package com.andreas.personalcloudclient;

import android.app.Application;
import com.orhanobut.hawk.Hawk;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize the Hawk library here.
        Hawk.init(this).build();
    }
}
