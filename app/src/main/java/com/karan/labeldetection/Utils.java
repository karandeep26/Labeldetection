package com.karan.labeldetection;

import android.graphics.Bitmap;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Created by stpl on 3/27/2017.
 */

public class Utils {
    static public Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {
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
    static public  JsonObject getJson(String value) {
        JsonObject mainJson = new JsonObject();
        JsonObject images = new JsonObject();
        JsonObject content = new JsonObject();
        content.addProperty("content", value);
        images.add("image", content);
        JsonArray featureArray = new JsonArray();
        JsonObject type = new JsonObject();
        type.addProperty("type", "LABEL_DETECTION");
        type.addProperty("maxResults", 20);
        featureArray.add(type);
        JsonArray requests = new JsonArray();
        requests.add(images);
        JsonObject features = new JsonObject();
        features.add("features", featureArray);
        images.add("features", featureArray);
        JsonArray request = new JsonArray();
        request.add(images);
        mainJson.add("requests", request);
        return mainJson;
    }
}
