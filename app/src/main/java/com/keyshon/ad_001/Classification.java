package com.keyshon.ad_001;

public class Classification {
    private float conf;
    private String label;

    Classification() {
        this.conf = -3.0F;
        this.label = null;
    }

    void update(float conf, String label) {
        this.conf = conf;
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public float getConf() {
        return conf;
    }
}
