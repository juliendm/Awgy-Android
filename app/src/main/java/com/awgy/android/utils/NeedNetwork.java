package com.awgy.android.utils;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class NeedNetwork {

    private List<String> mDidCallNetwork;

    private static final NeedNetwork mInstance = new NeedNetwork();

    public static NeedNetwork getInstance() {
        return mInstance;
    }

    private NeedNetwork () {
        mDidCallNetwork = new ArrayList<String>();
    }

    public boolean needNetworkForPinName(String pinName) {
        return !mDidCallNetwork.contains(pinName);
    }

    public void addDone(String pinName) {
        if (!mDidCallNetwork.contains(pinName)) {
            mDidCallNetwork.add(pinName);
        }
    }

    public void removeDone(String pinName) {
        if (mDidCallNetwork.contains(pinName)) {
            mDidCallNetwork.remove(pinName);
        }
    }

    public void clear() {
        mDidCallNetwork.clear();
    }


}

