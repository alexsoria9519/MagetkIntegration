package com.example.testmagtek

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.magtek.mobile.android.mtlib.MTConnectionType
import com.magtek.mobile.android.mtlib.MTSCRA

class MainActivity : AppCompatActivity() {

    private lateinit var usb: Button
    private lateinit var bluetooth: Button
    private lateinit var ble: Button
    private lateinit var bleEmv: Button
    private lateinit var bleEmvt: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        usb = findViewById(R.id.call_usb_con)
        bluetooth = findViewById(R.id.call_bluetooth_con)
        ble = findViewById(R.id.tes_ble)
        bleEmv = findViewById(R.id.tes_ble_emv)
        bleEmvt = findViewById(R.id.tes_ble_emvt)

        usb.setOnClickListener {
//            val intent = Intent(this, UsbScanActivity::class.java)
//            startActivity(intent)
            val intent = Intent(this, ScanActivity::class.java)
            intent.putExtra(ScanActivity.EXTRAS_CONNECTION_TYPE,"USB")
            startActivity(intent)
        }

        bluetooth.setOnClickListener {
            val intent = Intent(this, ScanActivity::class.java)
            intent.putExtra(ScanActivity.EXTRAS_CONNECTION_TYPE,"BLUETOOTH")
            startActivity(intent)
        }

        ble.setOnClickListener {
            val intent = Intent(this, ScanActivity::class.java)
            intent.putExtra(ScanActivity.EXTRAS_CONNECTION_TYPE,"BLE")
            startActivity(intent)
        }

        bleEmv.setOnClickListener {
            val intent = Intent(this, ScanActivity::class.java)
            intent.putExtra(ScanActivity.EXTRAS_CONNECTION_TYPE,"EMV")
            startActivity(intent)
        }

        bleEmvt.setOnClickListener {
            val intent = Intent(this, ScanActivity::class.java)
            intent.putExtra(ScanActivity.EXTRAS_CONNECTION_TYPE,"EMVT")
            startActivity(intent)
        }
    }


    fun connectionDevice(sdk: MTSCRA) {
        sdk.setConnectionType(MTConnectionType.USB)
//        sdk.setConnectionRetry(true) // this option is for Bluetooth Connection and recomended true flag
        sdk.setAddress("127.0.0.1")
    }


}