package com.example.testmagtek;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.widget.EditText;
import android.widget.TextView;

import com.magtek.mobile.android.mtlib.MTConnectionState;
import com.magtek.mobile.android.mtlib.MTConnectionType;
import com.magtek.mobile.android.mtlib.MTDeviceFeatures;
import com.magtek.mobile.android.mtlib.MTSCRA;

public class MagtekActivity extends AppCompatActivity {


//    private final static String TAG = Magtek.class.getSimpleName();

    public static final String AUDIO_CONFIG_FILE = "MTSCRAAudioConfig.cfg";

    public static final String EXTRAS_CONNECTION_TYPE_VALUE_AUDIO = "Audio";
    public static final String EXTRAS_CONNECTION_TYPE_VALUE_BLE = "BLE";
    public static final String EXTRAS_CONNECTION_TYPE_VALUE_BLE_EMV = "BLEEMV";
    public static final String EXTRAS_CONNECTION_TYPE_VALUE_BLE_EMVT = "BLEEMVT";
    public static final String EXTRAS_CONNECTION_TYPE_VALUE_BLUETOOTH = "Bluetooth";
    public static final String EXTRAS_CONNECTION_TYPE_VALUE_USB = "USB";
    public static final String EXTRAS_CONNECTION_TYPE_VALUE_SERIAL = "Serial";
    public static final String EXTRAS_CONNECTION_TYPE_VALUE_AIDL = "AIDL";

    public static final String EXTRAS_CONNECTION_TYPE = "CONNECTION_TYPE";
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String EXTRAS_AUDIO_CONFIG_TYPE = "AUDIO_CONFIG_TYPE";

    public static final String CONFIGWS_URL = "https://deviceconfig.magensa.net/service.asmx";//Production URL
    private static final String CONFIGWS_USERNAME = "magtek";
    private static final String CONFIGWS_PASSWORD = "p@ssword";
    private static final int CONFIGWS_READERTYPE = 0;
    private static final int CONFIGWS_TIMEOUT = 10000;

    private static String SCRA_CONFIG_VERSION = "102.02";

    private Menu mMainMenu;

    private TextView mMessageTextView;
    private TextView mMessageTextView2;

    private TextView mAddressField;
    private TextView mConnectionStateField;

    private EditText mDataFields;

    private AlertDialog mSelectionDialog;
    private Handler mSelectionDialogController;

    private AudioManager m_audioManager;

    private int m_audioVolume;

    private boolean m_startTransactionActionPending;

    private boolean m_turnOffLEDPending;

    private int m_emvMessageFormat = 0;

    private boolean m_emvMessageFormatRequestPending = false;

    private MTSCRA m_scra;

    private MTConnectionType m_connectionType;
    private String m_deviceName;
    private String m_deviceAddress;
    private String m_audioConfigType;

    private Object m_syncEvent = null;
    private String m_syncData = "";

    private MTConnectionState m_connectionState = MTConnectionState.Disconnected;

//    private final HeadSetBroadCastReceiver m_headsetReceiver = new HeadSetBroadCastReceiver();
//
//    private final NoisyAudioStreamReceiver m_noisyAudioStreamReceiver = new NoisyAudioStreamReceiver();
//
//    private AlertDialog mTransactionDialog;
//    private String[] mTypes = new String[] {"Swipe", "Chip", "Contactless"};
//    private boolean[] mTypeChecked = new boolean[] {false, true, false};
//
//    private Handler m_scraHandler = new Handler(new SCRAHandlerCallback());




    protected void OnDeviceStateChanged(MTConnectionState deviceState)
    {
        setState(deviceState);
        updateDisplay();
        invalidateOptionsMenu();

        switch (deviceState)
        {
            case Disconnected:
//                if (m_connectionType == MTConnectionType.Audio)
//                {
//                    restoreVolume();
//                }
                break;
            case Connected:
                displayDeviceFeatures();
                if (m_connectionType == MTConnectionType.Audio)
                {
//                    setVolumeToMax();
                }
                else if ((m_connectionType == MTConnectionType.USB) && (m_scra.isDeviceEMV() == false))
                {
                    sendGetSecurityLevelCommand();	// Wake up swipe output channel for BulleT and Audio readers
                }

                clearMessage();
                clearMessage2();
                break;
            case Error:
                sendToDisplay("[Device State Error]");
                break;
            case Connecting:
                break;
            case Disconnecting:
                break;
        }
    }




    private void setState(MTConnectionState deviceState)
    {
        m_connectionState = deviceState;
        updateDisplay();
        invalidateOptionsMenu();
    }

    private void updateDisplay()
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if (m_connectionState == MTConnectionState.Connected)
                {
                    updateConnectionState(R.string.connected);
                }
                else if (m_connectionState == MTConnectionState.Connecting)
                {
                    updateConnectionState(R.string.connecting);
                }
                else if (m_connectionState == MTConnectionState.Disconnecting)
                {
                    updateConnectionState(R.string.disconnecting);
                }
                else if (m_connectionState == MTConnectionState.Disconnected)
                {
                    updateConnectionState(R.string.disconnected);
                }
            }
        });
    }





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_magtek);
    }


    private void displayDeviceFeatures()
    {
        if (m_scra != null)
        {
            MTDeviceFeatures features = m_scra.getDeviceFeatures();

            if (features != null)
            {
                StringBuilder infoSB = new StringBuilder();

                infoSB.append("[Device Features]\n");

                infoSB.append("Supported Types: " + (features.MSR ? "(MSR) ":"") + (features.Contact ? "(Contact) ":"") + (features.Contactless ? "(Contactless) ":"") + "\n");
                infoSB.append("MSR Power Saver: " + (features.MSRPowerSaver ? "Yes":"No") + "\n");
                infoSB.append("Battery Backed Clock: " + (features.BatteryBackedClock ? "Yes":"No"));

                sendToDisplay(infoSB.toString());
            }
        }
    }

    public void sendToDisplay(final String data)
    {
        if (data != null)
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    mDataFields.append(data + "\n");
                }
            });
        }
    }

    private void sendGetSecurityLevelCommand()
    {
        Handler delayHandler = new Handler();
        delayHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                int status = sendCommand("1500");

            }
        }, 1000);
    }

    public int sendCommand(String command)
    {
        int result = MTSCRA.SEND_COMMAND_ERROR;


        if (m_scra != null)
        {
            sendToDisplay("[Sending Command]");
            sendToDisplay(command);

            result = m_scra.sendCommandToDevice(command);
        }

        return result;
    }

    private void updateConnectionState(final int resourceId)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                mConnectionStateField.setText(resourceId);
            }
        });
    }
    private void clearMessage()
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                mMessageTextView.setText("");
            }
        });
    }

    private void clearMessage2()
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                mMessageTextView2.setText("");
            }
        });
    }

}