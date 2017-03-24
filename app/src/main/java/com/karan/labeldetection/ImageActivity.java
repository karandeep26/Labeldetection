package com.karan.labeldetection;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.beloo.widget.chipslayoutmanager.ChipsLayoutManager;
import com.bumptech.glide.Glide;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.RequestBody;

public class ImageActivity extends Activity {
    Bitmap uploadBitmap;
    ProgressDialog progressDialog;
    private static final String CLOUD_VISION_API_KEY = "AIzaSyCIySCvpq6lgJyhsdVtXkTFua3iKcegKuk";
    RecyclerViewAdapter gridViewAdapter;
    RecyclerView recyclerView;
    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";
    LabelService labelService;
    Uri uri;
    Set<String> textToCopy=new HashSet<>();
    ArrayList<Model> data;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        getWindow().setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.8),
                (int) (getResources().getDisplayMetrics().heightPixels * 0.8));
        progressDialog = new ProgressDialog(ImageActivity.this);
        progressDialog.setCancelable(false);
        ImageView imageView = (ImageView) findViewById(R.id.image_view);
        TextView selectAll= (TextView) findViewById(R.id.select);
        selectAll.setSelected(false);
        selectAll.setOnClickListener(v -> {
            if(!selectAll.isSelected()) {
                selectAll.setTextColor(Color.parseColor("#D9000000"));
                selectAll.setSelected(true);
            }
            else{
                selectAll.setTextColor(Color.parseColor("#8C000000"));
                selectAll.setSelected(false);
            }
            for(Model m:data){
                m.setSelected(selectAll.isSelected());
                if(selectAll.isSelected()){
                    textToCopy.add(m.getTag());
                }
            }
            gridViewAdapter.notifyDataSetChanged();

        });
        uri = getIntent().getParcelableExtra("data");
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
        recyclerView= (RecyclerView) findViewById(R.id.recyclerView);
        int height=(int)(displayMetrics.heightPixels*0.8);
        recyclerView.getLayoutParams().height= (int) (height-height*0.6);
        imageView.getLayoutParams().height= (int) (height-height*0.5);
        LinearLayout linearLayout= (LinearLayout) findViewById(R.id.buttonPanel);
        linearLayout.getLayoutParams().height= (int) (height-height*height*0.9);
        Glide.with(this).load(uri)
                .asBitmap().centerCrop()
                .into(imageView);
        labelService=ApiClient.getClient().create(LabelService.class);
        Button cancel= (Button) findViewById(R.id.cancel);
        Button share= (Button) findViewById(R.id.share);
        cancel.setOnClickListener(v -> finish());
        share.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager)   getSystemService(Context.CLIPBOARD_SERVICE);
            StringBuilder stringBuilder=new StringBuilder();
            for(String s:textToCopy){
                stringBuilder.append(s);
                stringBuilder.append(" ");
            }
            ClipData clip = ClipData.newPlainText("Copied",stringBuilder.toString());
            clipboard.setPrimaryClip(clip);
            if(verifyInstagram()) {
                createInstagramIntent("image/*");
            }
        });
        recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(this,
                new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position, float x, float y) {
                if(data.get(position).isSelected()){
                    data.get(position).setSelected(false);
                    textToCopy.remove(data.get(position).getTag());
                }
                else{
                    data.get(position).setSelected(true);
                    textToCopy.add(data.get(position).getTag());
                }
                gridViewAdapter.notifyItemChanged(position);
                if(selectAll.isSelected()) {
                    selectAll.setSelected(false);
                    selectAll.setTextColor(Color.parseColor("#8C000000"));
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {

            }
        },recyclerView));
        try {
            Bitmap bitmap=scaleBitmapDown(MediaStore.Images.Media.getBitmap(getContentResolver(),uri),1200);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.WEBP, 100, byteArrayOutputStream);
            String encodedImage= Base64.encodeToString(byteArrayOutputStream.toByteArray(),Base64.URL_SAFE);
            String request=getJson(encodedImage).toString();
            fetchLabels(request);
            progressDialog.show();
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
    private Disposable fetchLabels(String request){
        RequestBody body =
                RequestBody.create(MediaType.parse("text/plain"), request);
        Observable<retrofit2.Response<JsonResponse>> responseObservable= labelService.fetchLabels(body);
        return responseObservable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(jsonResponse -> {
                    if (jsonResponse.isSuccessful()) {
                        data = new ArrayList<>();
                        JsonResponse response = jsonResponse.body();
                        ArrayList<Response> responseArrayList = response.getResponses();
                        for (Response res : responseArrayList) {
                            ArrayList<Response.LabelAnnotations> annotationsArrayList =
                                    res.getLabelAnnotations();
                            for (Response.LabelAnnotations annotations : annotationsArrayList) {
                                Log.d("Value",annotations.getDescription());
                                data.add(new Model("#"+annotations.getDescription().replaceAll("\\s+",""),false));
                            }
                        }
                        gridViewAdapter = new RecyclerViewAdapter(data, ImageActivity.this);
                        recyclerView.setLayoutManager(getLayoutManager());
                        recyclerView.addItemDecoration(new SpacingItemDecoration(getResources()
                                .getDimensionPixelOffset(R.dimen.item_space),
                                getResources().getDimensionPixelOffset(R.dimen.item_space)));
                        recyclerView.setAdapter(gridViewAdapter);


                    } else {
                        Log.d("error", jsonResponse.errorBody().string());
                    }
                }, throwable -> progressDialog.dismiss(), () -> progressDialog.dismiss());
    }
    ChipsLayoutManager getLayoutManager(){
        return ChipsLayoutManager.newBuilder(this)
                //set vertical gravity for all items in a row. Default = Gravity.CENTER_VERTICAL
                .setChildGravity(Gravity.TOP)
                //whether RecyclerView can scroll. TRUE by default
                .setScrollingEnabled(true)
                //set maximum views count in a particular row
                .setMaxViewsInRow(4)
                //set gravity resolver where you can determine gravity for item in position.
                //This method have priority over previous one
                .setGravityResolver(position -> Gravity.CENTER)
                //you are able to break row due to your conditions. Row breaker should return true for that views

                //a layoutOrientation of layout manager, could be VERTICAL OR HORIZONTAL. HORIZONTAL by default
                .setOrientation(ChipsLayoutManager.HORIZONTAL)
                // row strategy for views in completed row, could be STRATEGY_DEFAULT, STRATEGY_FILL_VIEW,
                //STRATEGY_FILL_SPACE or STRATEGY_CENTER
                .setRowStrategy(ChipsLayoutManager.STRATEGY_DEFAULT)
                // whether strategy is applied to last row. FALSE by default
                .withLastRow(true)
                .build();
    }
    private void createInstagramIntent(String type){

        // Create the new Intent using the 'Send' action.
        Intent share = new Intent(Intent.ACTION_SEND);

        // Set the MIME type
        share.setType(type);


        // Add the URI to the Intent.
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.setPackage("com.instagram.android");


        // Broadcast the Intent.
        startActivity(Intent.createChooser(share, "Share to"));
    }
    private boolean verifyInstagram(){
        boolean installed = false;

        try {
            ApplicationInfo info = getPackageManager().getApplicationInfo("com.instagram.android", 0);
            installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            installed = false;
        }
        return installed;
    }

}
