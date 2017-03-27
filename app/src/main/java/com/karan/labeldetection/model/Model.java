package com.karan.labeldetection.model;

/**
 * Created by stpl on 3/24/2017.
 */

public class Model {
    private String tag;
    private boolean isSelected;

    public Model(String tag, boolean isSelected) {
        this.tag = tag;
        this.isSelected = isSelected;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }
}
