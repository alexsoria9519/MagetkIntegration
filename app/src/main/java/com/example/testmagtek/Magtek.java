package com.example.testmagtek;

import android.content.Context;
import android.os.Handler;

import com.magtek.mobile.android.mtlib.MTSCRA;

public class Magtek {

    private Context context;
    private Handler handler;

    public Magtek(Context context, Handler handler) {
        this.context = context;
        this.handler = handler;
    }

    public static final String CONFIGWS_URL = "https://deviceconfig.magensa.net/service.asmx";//Production URL
    private static final String CONFIGWS_USERNAME = "MAG905840514";
    private static final String CONFIGWS_PASSWORD = "UCP!h@Db#oviw7";
    private static final int CONFIGWS_READERTYPE = 0;
    private static final int CONFIGWS_TIMEOUT = 10000;



}
