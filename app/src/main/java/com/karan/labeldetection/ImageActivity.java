package com.karan.labeldetection;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.GridView;
import android.widget.ImageView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.RequestBody;

public class ImageActivity extends AppCompatActivity {
    Bitmap uploadBitmap;
    ProgressDialog progressDialog;
    private static final String CLOUD_VISION_API_KEY = "AIzaSyCIySCvpq6lgJyhsdVtXkTFua3iKcegKuk";
    GridViewAdapter gridViewAdapter;
    GridView gridView;
    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";
    Uri uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        progressDialog = new ProgressDialog(ImageActivity.this);
        progressDialog.setCancelable(false);
        ImageView imageView = (ImageView) findViewById(R.id.image_view);
        uri = getIntent().getParcelableExtra("data");
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
        gridView= (GridView) findViewById(R.id.gridView);
        LabelService labelService=ApiClient.getClient().create(LabelService.class);
        try {
            Bitmap bitmap=scaleBitmapDown(MediaStore.Images.Media.getBitmap(getContentResolver(),uri),1200);
            imageView.setImageBitmap(bitmap);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.WEBP, 100, byteArrayOutputStream);
            String encodedImage= Base64.encodeToString(byteArrayOutputStream.toByteArray(),Base64.URL_SAFE);
            String request=getJson(encodedImage).toString();
            RequestBody body =
                    RequestBody.create(MediaType.parse("text/plain"), request);
            Observable<retrofit2.Response<JsonResponse>> responseObservable= labelService.fetchLabels(body);

            long start=System.currentTimeMillis();
            progressDialog.show();
            responseObservable.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(jsonResponse -> {
                        if (jsonResponse.isSuccessful()) {
                            ArrayList<String> data = new ArrayList<>();
                            JsonResponse response = jsonResponse.body();
                            ArrayList<Response> responseArrayList = response.getResponses();
                            for (Response res : responseArrayList) {
                                ArrayList<Response.LabelAnnotations> annotationsArrayList =
                                        res.getLabelAnnotations();
                                for (Response.LabelAnnotations annotations : annotationsArrayList) {
                                    data.add(annotations.getDescription());
                                }
                            }
                            GridViewAdapter gridViewAdapter = new GridViewAdapter(data, ImageActivity.this);
                            gridView.setAdapter(gridViewAdapter);
                        } else {
                            Log.d("error", jsonResponse.errorBody().string());
                        }
                    }, throwable -> progressDialog.dismiss(), () -> progressDialog.dismiss());


        } catch (IOException e) {
            e.printStackTrace();
        }


    }





    public Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }


    public byte[] readBytes(InputStream inputStream) throws IOException {
        // this dynamically extends to take the bytes you read
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

        // this is storage overwritten on each iteration with bytes
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        // we need to know how may bytes were read to write them to the byteBuffer
        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }

        // and then we can return your byte array.
        return byteBuffer.toByteArray();
    }
    JsonObject getJson(String value){
        JsonObject mainJson=new JsonObject();
        JsonObject images=new JsonObject();
        JsonObject content=new JsonObject();
        content.addProperty("content",value);
        images.add("image",content);

        JsonArray featureArray=new JsonArray();
        JsonObject type=new JsonObject();
        type.addProperty("type","LABEL_DETECTION");
        type.addProperty("maxResults",20);
        featureArray.add(type);
        JsonArray requests=new JsonArray();
        requests.add(images);
        JsonObject features=new JsonObject();
        features.add("features",featureArray);
        images.add("features",featureArray);
        JsonArray request=new JsonArray();
        request.add(images);
        mainJson.add("requests",request);
        return mainJson;
    }
}
