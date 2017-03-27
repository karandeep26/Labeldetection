package com.karan.labeldetection.model;

import java.util.ArrayList;

/**
 * Created by stpl on 3/23/2017.
 */

public class JsonResponse {
   private ArrayList<Response> responses;

    public JsonResponse(ArrayList<Response> responses) {
        this.responses = responses;
    }

    public ArrayList<Response> getResponses() {
        return responses;
    }
}
