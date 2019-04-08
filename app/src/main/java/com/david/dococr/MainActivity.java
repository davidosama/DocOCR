package com.david.dococr;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

//    TextView textView;
    EditText editText;
    Button btnExportPDF;

    static public MainActivity instance;
    static public SharedPreferences sharedPreferences;
    static public SharedPreferences.Editor sharedPreferenceEditor;

    final String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};


    static private final int allPermissions = 1;
    static private final int CameraRequestPermissionCode = 1;
    static private final int StorageRequestPermissionCode = 1;
    static private final int PICK_IMAGE_MULTIPLE = 5;

    static private int docNum = 0;
    static private int maxDocNum = 0;
    ArrayList<Uri> imagesUriArrayList;
    ArrayList<String> textArray;

    Uri uri;
    OCR ocr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        instance = this;

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openCamOrGallery(1);
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        sharedPreferences = getSharedPreferences("MySharedPreference",MODE_PRIVATE);
        sharedPreferenceEditor = getSharedPreferences("MySharedPreference",MODE_PRIVATE).edit();

        imagesUriArrayList = new ArrayList<Uri>();
        textArray = new ArrayList<String>();

        btnExportPDF = findViewById(R.id.buttonExportPDF);
        btnExportPDF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = editText.getText().toString();

                if(text.equals("")){
                    Toast.makeText(MainActivity.this, "Please type something or use the OCR.", Toast.LENGTH_LONG).show();
                }
                else {
                    try {
                        generatePDF(text);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (DocumentException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        editText = findViewById(R.id.editText);
        editText.setMovementMethod(new ScrollingMovementMethod());
        registerForContextMenu(editText);
        ocr = new OCR(MainActivity.this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Toast.makeText(MainActivity.this, "Dummy item, not implemented yet.", Toast.LENGTH_LONG).show();
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_scan) {
            openCamOrGallery(1);
        } else if (id == R.id.nav_gallery) {
            openCamOrGallery(2);
        } else if (id == R.id.nav_slideshow) {
            Toast.makeText(MainActivity.this, "Dummy item, not implemented yet.", Toast.LENGTH_LONG).show();
        } else if (id == R.id.nav_manage) {
            Toast.makeText(MainActivity.this, "Dummy item, not implemented yet.", Toast.LENGTH_LONG).show();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void openCamOrGallery(int option)
    {
        int permissionCheckCamera = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA);
        int permissionCheckStorage = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if(permissionCheckCamera == PackageManager.PERMISSION_GRANTED && permissionCheckStorage == PackageManager.PERMISSION_GRANTED){
            if(option == 1){
                CameraOpen();
            }
            else{
                GalleryOpen();
            }
        }
        else
            RequestRuntimePermission();

    }

    private void RequestRuntimePermission() {
        ActivityCompat.requestPermissions(this, permissions, allPermissions);

//        if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.CAMERA)){
//            Toast.makeText(this,"We need camera access to be able to scan documents",Toast.LENGTH_SHORT).show();
//        }
//        else {
//            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.CAMERA}, CameraRequestPermissionCode);
//        }
//
//        if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
//            Toast.makeText(this, "We need write to storage permission to export to file.", Toast.LENGTH_LONG).show();
//        }
//        else {
//            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, StorageRequestPermissionCode);
//        }

    }

    private void GalleryOpen(){
        imagesUriArrayList.clear();
        textArray.clear();

        maxDocNum = 0;
        docNum = 0;

        //multiple images
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_MULTIPLE);

    }

    private void CameraOpen() {
        imagesUriArrayList.clear();
        textArray.clear();

        maxDocNum = 1;
        docNum = 1;
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .start(this);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE_MULTIPLE) {
                if (data.getClipData() != null && data.getClipData().getItemCount() > 0) {
                    for(int i = 0; i < data.getClipData().getItemCount(); i++){
                        imagesUriArrayList.add(data.getClipData().getItemAt(i).getUri());
                    }

                    for(int i=0; i < imagesUriArrayList.size(); i++){
                        CropImage.activity(imagesUriArrayList.get(i))
                                .setGuidelines(CropImageView.Guidelines.ON)
                                .start(this);
                    }

                    maxDocNum = imagesUriArrayList.size()-1;
                }
                else {
                    // no image selected
                    Toast.makeText(MainActivity.this, "No image selected.", Toast.LENGTH_LONG).show();
                }

            }
            else if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                try{
                    textArray.add(ocr.ExtractEnglishText(MediaStore.Images.Media.getBitmap(this.getContentResolver(), result.getUri())));

                    if(docNum == maxDocNum){
                        for(String s : textArray){
                            editText.append(s+"\n");
                        }
                    }

                    docNum++;
                }
                catch (IOException e){
                    e.printStackTrace();
                }
            }
        }

    }

    private void generatePDF(String text) throws IOException, DocumentException {
        Calendar cal = Calendar. getInstance();
        Date date = cal.getTime();
        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        String formattedDate = dateFormat. format(date);


        Document document = new Document();

        // Location to save
        String filename = Environment.getExternalStorageDirectory().getPath() + "/DocOCR " + formattedDate + ".pdf";

        File file = new File(filename);
        if(!file.exists()){
            file.createNewFile();
        }
        PdfWriter.getInstance(document, new FileOutputStream(file.getAbsoluteFile()));

        // Open to write
        document.open();

        // Document Settings
        document.setPageSize(PageSize.A4);
        document.addCreationDate();
        document.addCreator("DocOCR");

        document.add(new Paragraph(text));
        document.add(new Paragraph(""));

        document.close();

        Toast.makeText(MainActivity.this, "PDF saved at "+filename, Toast.LENGTH_LONG).show();
    }

//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
//            CropImage.ActivityResult result = CropImage.getActivityResult(data);
//            if (resultCode == RESULT_OK) {
//                uri = result.getUri();
//                try {
//                    //for English text
//                    String text = ocr.ExtractEnglishText(MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri));
//
//                    //for Arabic text
////                    String text = ocr.ExtractArabicText(MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri));
//                    textView.setText(text);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }
}
