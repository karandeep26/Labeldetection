package com.karan.labeldetection;

import java.util.ArrayList;

/**
 * Created by stpl on 3/23/2017.
 */

public class Response {
    private ArrayList<LabelAnnotations> labelAnnotations;

    public Response(ArrayList<LabelAnnotations> labelAnnotations) {
        this.labelAnnotations = labelAnnotations;
    }

    public ArrayList<LabelAnnotations> getLabelAnnotations() {
        return labelAnnotations;
    }

    public static class LabelAnnotations{
        String mid,description,score;

        public LabelAnnotations(String mid, String description, String score) {
            this.mid = mid;
            this.description = description;
            this.score = score;
        }

        public String getMid() {
            return mid;
        }

        public String getDescription() {
            return description;
        }

        public String getScore() {
            return score;
        }
    }
}
