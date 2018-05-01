package com.keyshon.ad_001;

public interface Classifier {
    String name();

    Classification recognize(final float[] data);
}
