package com.karan.labeldetection;

import io.reactivex.Observable;
import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Created by stpl on 3/23/2017.
 */

public interface LabelService {
    @POST("v1/images:annotate?key=AIzaSyCIySCvpq6lgJyhsdVtXkTFua3iKcegKuk")
    Observable<retrofit2.Response<JsonResponse>> fetchLabels(@Body RequestBody json);
}
