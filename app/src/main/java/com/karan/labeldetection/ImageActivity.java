package com.karan.labeldetection;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.GridView;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class ImageActivity extends AppCompatActivity {
    Bitmap uploadBitmap;
    ProgressDialog progressDialog;
    private static final String CLOUD_VISION_API_KEY = "AIzaSyCIySCvpq6lgJyhsdVtXkTFua3iKcegKuk";
    GridViewAdapter gridViewAdapter;
    GridView gridView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        progressDialog = new ProgressDialog(ImageActivity.this);
        progressDialog.setCancelable(false);
        ImageView imageView = (ImageView) findViewById(R.id.image_view);
        Uri uri = getIntent().getParcelableExtra("data");
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
        gridView= (GridView) findViewById(R.id.gridView);

        Glide.with(this).loadFromMediaStore(uri).asBitmap()
                .override(displayMetrics.widthPixels,displayMetrics.heightPixels)
                .listener(new RequestListener<Uri, Bitmap>() {
                    @Override
                    public boolean onException(Exception e, Uri model, Target<Bitmap> target,
                                               boolean isFirstResource) {
                        return false;
                    }
                    @Override
                    public boolean onResourceReady(Bitmap resource, Uri model, Target<Bitmap> target
                            , boolean isFromMemoryCache, boolean isFirstResource) {
                        uploadBitmap = resize(resource, 640, 480);
                        progressDialog.show();
                        http(resource);
                        return false;
                    }
                })
                .into(imageView);
    }

    private static Bitmap resize(Bitmap image, int maxWidth, int maxHeight) {
        if (maxHeight > 0 && maxWidth > 0) {
            int width = image.getWidth();
            int height = image.getHeight();
            float ratioBitmap = (float) width / (float) height;
            float ratioMax = (float) maxWidth / (float) maxHeight;

            int finalWidth = maxWidth;
            int finalHeight = maxHeight;
            if (ratioMax > 1) {
                finalWidth = (int) ((float) maxHeight * ratioBitmap);
            } else {
                finalHeight = (int) ((float) maxWidth / ratioBitmap);
            }
            image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true);
            return image;
        } else {
            return image;
        }
    }

    void http(Bitmap bitmap) {

        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
        VisionRequestInitializer initializer = new VisionRequestInitializer(CLOUD_VISION_API_KEY);
        Vision vision = new Vision.Builder(httpTransport, jsonFactory, null)
                .setVisionRequestInitializer(initializer)
                .build();

        // Create the image request

        AnnotateImageRequest imageRequest = new AnnotateImageRequest();
        Image image = new Image();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        image.encodeContent(stream.toByteArray());
        imageRequest.setImage(image);
        // Add the features we want
        Feature labelDetection = new Feature();
        labelDetection.setType("LABEL_DETECTION");
        labelDetection.setMaxResults(100);
        imageRequest.setFeatures(Collections.singletonList(labelDetection));

        // Batch and execute the request
        BatchAnnotateImagesRequest requestBatch = new BatchAnnotateImagesRequest();
        requestBatch.setRequests(Collections.singletonList(imageRequest));

            Observable<BatchAnnotateImagesResponse> responseObservable = Observable
                    .defer(() -> Observable.just(vision.images()
                            .annotate(requestBatch)
                            .setDisableGZipContent(true)
                            .execute()));
        long start=System.currentTimeMillis();
            responseObservable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                    .subscribe(batchAnnotateImagesResponse ->
                    {
                        Log.d("end time",""+(System.currentTimeMillis()-start));
                        gridViewAdapter=new GridViewAdapter(getLabels(batchAnnotateImagesResponse)
                                ,ImageActivity.this);
                        gridView.setAdapter(gridViewAdapter);
                    },
                    throwable -> progressDialog.dismiss(), () -> progressDialog.dismiss());


    }

    private Map<String, Float> convertResponseToMap(BatchAnnotateImagesResponse response) {
        Map<String, Float> annotations = new HashMap<>();

        // Convert response into a readable collection of annotations
        List<EntityAnnotation> labels = response.getResponses().get(0).getLabelAnnotations();
        if (labels != null) {
            for (EntityAnnotation label : labels) {
                annotations.put(label.getDescription(), label.getScore());
            }
        }
        return annotations;
    }
    private ArrayList<String> getLabels(BatchAnnotateImagesResponse response){
        ArrayList<String> data=new ArrayList<>();
        List<EntityAnnotation> labels = response.getResponses().get(0).getLabelAnnotations();
        if (labels != null) {
            for (EntityAnnotation label : labels) {
                data.add(label.getDescription());
            }
        }
        return data;
    }
}
