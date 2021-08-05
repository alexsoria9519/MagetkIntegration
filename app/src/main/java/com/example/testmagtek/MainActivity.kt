package com.example.testmagtek

import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.magtek.mobile.android.mtlib.MTConnectionType
import com.magtek.mobile.android.mtlib.MTSCRA

class MainActivity : AppCompatActivity() {

    val CONFIGWS_URL = "https://deviceconfig.magensa.net/service.asmx" //Production URL
    private val CONFIGWS_USERNAME = "MAG905840514"
    private val CONFIGWS_PASSWORD = "UCP!h@Db#oviw7"
    private val CONFIGWS_READERTYPE = 0
    private val CONFIGWS_TIMEOUT = 10000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sdk = MTSCRA(this, Handler())

    }

    fun connectionDevice(sdk: MTSCRA){
        sdk.setConnectionType(MTConnectionType.USB)
//        sdk.setConnectionRetry(true) // this option is for Bluetooth Connection and recomended true flag
        sdk.setAddress("127.0.0.1")

        // ToDO implementation of services USB/Bluetooh


    }


}