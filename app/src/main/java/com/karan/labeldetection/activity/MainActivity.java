package com.karan.labeldetection.activity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.karan.labeldetection.AlertDialogManager;
import com.karan.labeldetection.BuildConfig;
import com.karan.labeldetection.ConnectionDetector;
import com.karan.labeldetection.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;
    private static final int MULTIPLE_PERMISSIONS = 123;
    private static final int REQUEST_TAKE_PHOTO = 5;
    String mCurrentPhotoPath;
    ConnectionDetector connectionDetector;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        connectionDetector=new ConnectionDetector(this);
        if(!connectionDetector.isConnectingToInternet()){
            AlertDialogManager alertDialogManager=new AlertDialogManager();
            alertDialogManager.showAlertDialog(this,"Connectivity Problem","Not Connected to Internet",false);
            return;
        }
        Button capture, select;
        capture = (Button) findViewById(R.id.capture);
        select = (Button) findViewById(R.id.select);
        select.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"),
                    PICK_IMAGE_REQUEST);
        });
        capture.setOnClickListener(v -> checkForPermissions());

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Intent intent = new Intent(this, ImageActivity.class);
        if (requestCode == PICK_IMAGE_REQUEST &&
                resultCode == RESULT_OK && data != null && data.getData() != null) {

            Uri uri = data.getData();
            intent.putExtra("data", uri);
            startActivity(intent);
        } else if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            galleryAddPic();
            Uri uri = Uri.fromFile(new File(mCurrentPhotoPath));
            Log.d("TAG", "file exist" + new File(mCurrentPhotoPath).length());
            intent.putExtra("data", uri);

            startActivity(intent);
        }
    }

    private void checkForPermissions() {
        ArrayList<String> permissionNeeded = new ArrayList<>();
        ArrayList<String> permissionList = new ArrayList<>();
        if (!addPermission(permissionList, Manifest.permission.CAMERA))
            permissionNeeded.add("camera");
        if (!addPermission(permissionList, Manifest.permission.WRITE_EXTERNAL_STORAGE))
            permissionNeeded.add("write");
        if (!addPermission(permissionList, Manifest.permission.READ_EXTERNAL_STORAGE))
            permissionNeeded.add("read");

        if (permissionNeeded.size() > 0) {
            permissionNotAvailable(permissionNeeded, permissionList);
        } else {
            dispatchTakePictureIntent();
        }

    }

    private boolean addPermission(List<String> permissionsList, String permission) {
        if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager
                .PERMISSION_GRANTED) {
            permissionsList.add(permission);
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                    permission))
                return false;
        }
        return true;
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    public void permissionNotAvailable(ArrayList<String> permissionNeeded,
                                       ArrayList<String> permissionList) {
        String message = "You need to grant access to " + permissionNeeded.get(0);
        for (String permission : permissionNeeded) {
            message = message + ", " + permission;
        }
        showMessageOKCancel(message,
                (dialog, which) -> ActivityCompat.requestPermissions(this, permissionList.toArray
                                (new String[permissionList.size()]),
                        MULTIPLE_PERMISSIONS));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case MULTIPLE_PERMISSIONS:
                Map<String, Integer> perms = new HashMap<>();

                perms.put(Manifest.permission.CAMERA, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.READ_EXTERNAL_STORAGE, PackageManager
                        .PERMISSION_GRANTED);
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager
                        .PERMISSION_GRANTED);

                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);

                if (perms.get(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                        PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                        PackageManager.PERMISSION_GRANTED) {
                    //Permissions are available,set up the screen now
                    dispatchTakePictureIntent();
                }
                // All Permissions Granted
                else {
                    // Permission Denied
                    Toast.makeText(MainActivity.this, "Some Permission is Denied", Toast
                            .LENGTH_SHORT)
                            .show();
                }

                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile;

            photoFile = getOutputMediaFile();

            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI;
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                    photoURI =
                            Uri.fromFile(photoFile);
                } else {
                    photoURI = FileProvider.getUriForFile(this,
                            BuildConfig.APPLICATION_ID + ".provider", photoFile);
                }
                takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private File getOutputMediaFile() {
        File mediaStorageDir = Environment.getExternalStoragePublicDirectory(Environment
                .DIRECTORY_DCIM);
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(new Date());
        String fileName = mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg";

        File mediaFile;
        mediaFile = new File(fileName);
        try {
            if (mediaFile.createNewFile()) {
                mCurrentPhotoPath = mediaFile.getAbsolutePath();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        return mediaFile;
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

}
