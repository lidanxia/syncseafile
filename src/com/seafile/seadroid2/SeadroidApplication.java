package com.seafile.seadroid2;

import android.app.Application;
import android.content.Context;

public class SeadroidApplication extends Application
{

    public static Context context;

    public void onCreate() 
    {
        super.onCreate();
        SeadroidApplication.context = getApplicationContext();
    }

    public static Context getAppContext()
    {
        return SeadroidApplication.context;
    }

}