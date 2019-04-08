package com.david.dococr;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class OCR extends AppCompatActivity{

    static public TextRecognizer textRecognizer;
    static public TessBaseAPI tessBaseAPI;
    boolean tess_file_exists;

    OCR(Context context){
        initializeTextRecognizer(context);

//        initializeTesseract();

    }

    private void initializeTextRecognizer(Context context){
        textRecognizer = new TextRecognizer.Builder(context).build();
        if (!textRecognizer.isOperational()) {
//            Log.v("OCR_ERROR", "Detector dependencies are not yet available.");

            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
//            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
//            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;
//
//            if (hasLowStorage) {
//                Toast.makeText(context, "Low storage.", Toast.LENGTH_LONG).show();
//                Log.v("OCR_ERROR", "Low storage.");
//            }
//            else{
            Toast.makeText(context, "Text Recognizer NOT operational.", Toast.LENGTH_LONG).show();
            Log.v("OCR_ERROR", "Text Recognizer NOT operational.");
//            }
        }
    }

    private void initializeTesseract(){
        tess_file_exists = MainActivity.sharedPreferences.getBoolean("TessFileExists",false);

        if(!tess_file_exists){
            copyTessDataForTextRecognizor();
            MainActivity.sharedPreferenceEditor.putBoolean("TessFileExists",true);
            MainActivity.sharedPreferenceEditor.apply();
        }

        tessBaseAPI = new TessBaseAPI();
        tessBaseAPI.init(MainActivity.instance.getExternalFilesDir(null).getAbsolutePath(),"ara");
    }

    private void copyTessDataForTextRecognizor()
    {
        Runnable run = new Runnable() {
            @Override
            public void run() {
                AssetManager assetManager = MainActivity.instance.getAssets();
                OutputStream out =null;
                try {
                    InputStream inArabic = assetManager.open("ara.traineddata");
                    String tesspath = MainActivity.instance.getExternalFilesDir(null)+"/tessdata/";
                    File tessFolder = new File(tesspath);
                    if(!tessFolder.exists())
                        tessFolder.mkdir();
                    String tessData = tesspath+"/"+"ara.traineddata";
                    File tessFile = new File(tessData);
                    if(!tessFile.exists())
                    {
                        out = new FileOutputStream(tessData);
                        byte[] buffer = new byte[1024];
                        int read = inArabic.read(buffer);
                        while (read != -1) {
                            out.write(buffer, 0, read);
                            read = inArabic.read(buffer);
                        }
                        Log.v("OCR_ERROR", "Did finish copy tess file");
                    }
                    else
                        Log.v("OCR_ERROR", "tess file exist ");

                } catch (Exception e)
                {
                    Log.v("OCR_ERROR", "couldn't copy with the following error: "+e.toString());
                }finally {
                    try {
                        if(out!=null)
                            out.close();
                    }catch (Exception exx)
                    {

                    }
                }
            }
        };
        new Thread(run).start();
    }


    public String ExtractEnglishText(Bitmap bitmap){
        Frame frame = new Frame.Builder().setBitmap(bitmap).build();
        SparseArray<TextBlock> items = textRecognizer.detect(frame);

        StringBuilder text = new StringBuilder();

        for(int i=0; i<items.size(); i++){
            text.append(items.valueAt(i).getValue()+"\n");
        }

        return text.toString();
    }

    public String ExtractArabicText(Bitmap bitmap){
        tessBaseAPI.setImage(bitmap);
        return tessBaseAPI.getUTF8Text();
    }
}
