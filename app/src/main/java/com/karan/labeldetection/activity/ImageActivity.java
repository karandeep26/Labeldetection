package com.karan.labeldetection.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.beloo.widget.chipslayoutmanager.ChipsLayoutManager;
import com.bumptech.glide.Glide;
import com.karan.labeldetection.R;
import com.karan.labeldetection.RecyclerItemClickListener;
import com.karan.labeldetection.SpacingItemDecoration;
import com.karan.labeldetection.Utils;
import com.karan.labeldetection.adapter.RecyclerViewAdapter;
import com.karan.labeldetection.model.JsonResponse;
import com.karan.labeldetection.model.Model;
import com.karan.labeldetection.network.ApiClient;
import com.karan.labeldetection.network.LabelService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
    ProgressDialog progressDialog;
    private static final String CLOUD_VISION_API_KEY = "AIzaSyCIySCvpq6lgJyhsdVtXkTFua3iKcegKuk";
    RecyclerViewAdapter gridViewAdapter;
    RecyclerView recyclerView;
    LabelService labelService;
    Uri uri;
    Set<String> textToCopy = new HashSet<>();
    ImageView imageView;
    TextView selectAll;
    ArrayList<Model> data = new ArrayList<>();
    private Button cancel, share;
    private LinearLayout linearLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        getWindow().setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.8),
                (int) (getResources().getDisplayMetrics().heightPixels * 0.8));
        findViewByIds();
        setClickListeners();
        setFinishOnTouchOutside(false);
        progressDialog = new ProgressDialog(ImageActivity.this);
        progressDialog.setCancelable(false);
        selectAll.setSelected(false);
        uri = getIntent().getParcelableExtra("data");
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        int height = (int) (displayMetrics.heightPixels * 0.8);
        recyclerView.getLayoutParams().height = (int) (height - height * 0.6);
        imageView.getLayoutParams().height = (int) (height - height * 0.5);
        linearLayout.getLayoutParams().height = (int) (height - height * height * 0.9);
        Glide.with(this).load(uri).asBitmap().centerCrop().into(imageView);
        labelService = ApiClient.getClient().create(LabelService.class);
        try {
            progressDialog.show();
            long start=System.currentTimeMillis();
            Bitmap bitmap = Utils.scaleBitmapDown(MediaStore.Images
                    .Media.getBitmap(getContentResolver(), uri), 1200);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.WEBP, 100, byteArrayOutputStream);
            String encodedImage = Base64.encodeToString(byteArrayOutputStream.toByteArray(),
                    Base64.URL_SAFE);
            Log.d("TAG",System.currentTimeMillis()-start+"");
            fetchLabels(Utils.getJson(encodedImage).toString());
            gridViewAdapter = new RecyclerViewAdapter(data, ImageActivity.this);
            recyclerView.setLayoutManager(getLayoutManager());
            recyclerView.addItemDecoration(new SpacingItemDecoration(getResources()
                    .getDimensionPixelOffset(R.dimen.item_space),
                    getResources().getDimensionPixelOffset(R.dimen.item_space)));
            recyclerView.setAdapter(gridViewAdapter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Disposable fetchLabels(String request) {
        RequestBody body =
                RequestBody.create(MediaType.parse("text/plain"), request);
        Observable<retrofit2.Response<JsonResponse>> responseObservable = labelService
                .fetchLabels(body);
        return responseObservable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers
                .mainThread())
                .flatMap(jsonResponseResponse -> {
                    if (jsonResponseResponse.isSuccessful()) {
                        return Observable.just(jsonResponseResponse.body().getResponses());
                    }
                    throw new Exception(jsonResponseResponse.errorBody().string());
                })
                .flatMapIterable(list -> list)
                .flatMap(response -> Observable.just(response.getLabelAnnotations()))
                .flatMapIterable(labelAnnotations -> labelAnnotations)
                .subscribe(labelAnnotations -> {
                    data.add(new Model("#" + labelAnnotations.getDescription().replaceAll("\\s+",
                            ""), false));
                    gridViewAdapter.notifyItemInserted(data.size() - 1);
                }, throwable -> progressDialog.dismiss(), () -> progressDialog.dismiss());

    }

    ChipsLayoutManager getLayoutManager() {
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
                //you are able to break row due to your conditions. Row breaker should return
                // true for that views

                //a layoutOrientation of layout manager, could be VERTICAL OR HORIZONTAL.
                // HORIZONTAL by default
                .setOrientation(ChipsLayoutManager.HORIZONTAL)
                // row strategy for views in completed row, could be STRATEGY_DEFAULT,
                // STRATEGY_FILL_VIEW,
                //STRATEGY_FILL_SPACE or STRATEGY_CENTER
                .setRowStrategy(ChipsLayoutManager.STRATEGY_DEFAULT)
                // whether strategy is applied to last row. FALSE by default
                .withLastRow(true)
                .build();
    }

    private void createInstagramIntent(String type) {

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

    private boolean verifyInstagram() {
        boolean installed;

        try {
            getPackageManager().getApplicationInfo("com.instagram.android", 0);
            installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            installed = false;
        }
        return installed;
    }

    private void findViewByIds() {
        imageView = (ImageView) findViewById(R.id.image_view);
        selectAll = (TextView) findViewById(R.id.select);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        cancel = (Button) findViewById(R.id.cancel);
        share = (Button) findViewById(R.id.share);
        linearLayout = (LinearLayout) findViewById(R.id.buttonPanel);


    }

    private void setClickListeners() {
        cancel.setOnClickListener(v -> finish());
        share.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context
                    .CLIPBOARD_SERVICE);
            StringBuilder stringBuilder = new StringBuilder();
            for(String s:textToCopy){
                stringBuilder.append(s);
                stringBuilder.append(" ");
            }
            ClipData clip = ClipData.newPlainText("Copied", stringBuilder.toString());
            clipboard.setPrimaryClip(clip);
            if (verifyInstagram()) {
                android.app.AlertDialog.Builder alertDialog =new android.app.AlertDialog.Builder(this);
                alertDialog.setTitle("Instagram Caption Copied!")
                        .setMessage("Selected tags have been copied.");
                alertDialog.setPositiveButton("Okay,Got It!",
                        (dialog, which) -> createInstagramIntent("image/*"));
                alertDialog.setCancelable(false);
                alertDialog.show();
            }
            else{
                Toast.makeText(this,"Instagram not installed",Toast.LENGTH_SHORT).show();
            }
        });
        recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(this,
                (view, position) -> {
                    TextView textView = (TextView) view.findViewById(R.id.tag);
                    if (data.get(position).isSelected()) {
                        data.get(position).setSelected(false);
                        textToCopy.remove(data.get(position).getTag());
                        textView.setBackground(ContextCompat.getDrawable(textView.getContext(), R
                                .drawable.shape_unselected));
                    } else {
                        data.get(position).setSelected(true);
                        textToCopy.add(data.get(position).getTag());
                        textView.setBackground(ContextCompat.getDrawable(textView.getContext(), R
                                .drawable.shape_selected));
                    }
                    if (selectAll.isSelected()) {
                        selectAll.setSelected(false);
                        selectAll.setTextColor(Color.parseColor("#8C000000"));
                    }
                }));
        selectAll.setOnClickListener(v -> {
            if (!selectAll.isSelected()) {
                selectAll.setTextColor(Color.parseColor("#D9000000"));
                selectAll.setSelected(true);
            } else {
                selectAll.setTextColor(Color.parseColor("#8C000000"));
                selectAll.setSelected(false);
            }
            for(Model m:data){
                m.setSelected(selectAll.isSelected());
                if (selectAll.isSelected()) {
                    textToCopy.add(m.getTag());
                }
            }
            gridViewAdapter.notifyDataSetChanged();
        });
    }


}
