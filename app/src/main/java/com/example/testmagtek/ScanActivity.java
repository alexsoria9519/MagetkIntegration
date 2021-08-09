package com.example.testmagtek;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.magtek.mobile.android.mtlib.MTDeviceConstants;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.UUID;

public class ScanActivity extends ListActivity {

    private DeviceListAdapter mDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private int mSelected;

    private String[] mUSBDeviceAddressList;

    private Menu mMenu;

    private UUID mServiceUuid;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    private static final int PERMISSIONS_REQUEST = 2001;

    public static final String EXTRAS_CONNECTION_TYPE = "CONNECTION_TYPE";
    public static String EXTRAS_CONNECTION_VALUE = "BLUETOOTH";

//    private Button button_ble;
//    private Button button_ble_emv;
//    private Button button_ble_emvt;

    private CustomScanCallback mScanCallback = null;

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    if (device.getType() != BluetoothDevice.DEVICE_TYPE_LE) {
                        mDeviceListAdapter.addDevice(device);
                        mDeviceListAdapter.notifyDataSetChanged();
                    }
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                mScanning = false;
                invalidateOptionsMenu();
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_scan);
//

        Intent intent = getIntent();
        String option = intent.getStringExtra(EXTRAS_CONNECTION_TYPE);
        Log.e("EXTRAS_CONNECTION_TYPE", option);
        EXTRAS_CONNECTION_VALUE = option;

        // Ask for permission
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN,
                                Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST);
            }
        }

//        //Change the name of the project
//        try {
//            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
//            String strAppVersion = pInfo.versionName;
//
//            getActionBar().setTitle(getResources().getText(R.string.app_name) + " " + strAppVersion);
//        } catch (Exception ex) {
//            getActionBar().setTitle(R.string.app_name);
//        }

        mHandler = new Handler();

        mScanning = false;

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        mBluetoothAdapter = bluetoothManager.getAdapter();


        if (mBluetoothAdapter == null) {
            Log.d("mBluetoothAdapter", "Bluetooth does not support");
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_LONG).show();
        } else {
            Log.e("mBluetoothAdapter", "Bluetooth is supporting");
//            scanBluetoothDevice(true);
        }


    }

    @Override
    protected void onResume() {
        super.onResume();

//        selectBLEEMV();
        Log.e("EXTRAS_CONNECTION_VALUE", EXTRAS_CONNECTION_VALUE);
        selectedOption(EXTRAS_CONNECTION_VALUE);
    }

    private void selectedOption(String dataOption) {
        switch (dataOption) {
            case "BLE":
                selectBLE();
                break;
            case "EMV":
                selectBLEEMV();
                break;
            case "EMVT":
                selectBLEEMVT();
                break;
            case "BLUETOOTH":
                selectBluetooth();
                break;
        }
    }

    private void selectBLEEMV() {
        stopScanning();
        //mSelected = R.id.menu_ble_emv;
        mSelected = 1;
        //getActionBar().setSubtitle(R.string.title_ble_emv_devices);

        mDeviceListAdapter = new DeviceListAdapter();
        setListAdapter(mDeviceListAdapter);
        scanLeDevice(true);
    }

    private void selectBLE() {
        stopScanning();

//        mSelected = R.id.menu_ble;
        mSelected = 0;
//        getActionBar().setSubtitle(R.string.title_ble_devices);

        mDeviceListAdapter = new DeviceListAdapter();
        setListAdapter(mDeviceListAdapter);
        scanLeDevice(true);
    }

    private void selectBLEEMVT() {
        stopScanning();

        //mSelected = R.id.menu_ble_emvt;
        mSelected = 2;

//        getActionBar().setSubtitle(R.string.title_ble_emv_t_devices);

        mDeviceListAdapter = new DeviceListAdapter();
        setListAdapter(mDeviceListAdapter);
        scanLeDevice(true);
    }


    private void scanLeDevice(final boolean enable) {
        if (!mBluetoothAdapter.isEnabled()) {
            return;
        }

        stopScanning();
        if (enable) {

            Toast.makeText(this, "Scanning " + EXTRAS_CONNECTION_VALUE, Toast.LENGTH_LONG).show();

            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopScanning();
                    invalidateOptionsMenu();
                    scanFinished("Scan Finished");
                }
            }, SCAN_PERIOD);

            mScanning = true;

            if (android.os.Build.VERSION.SDK_INT >= 21) {
                BluetoothLeScanner leScanner = mBluetoothAdapter.getBluetoothLeScanner();

                if (leScanner != null) {
                    if (mScanCallback == null) {
                        mScanCallback = new CustomScanCallback();
                    }

                    leScanner.startScan(mScanCallback);
                }
            } else {
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            }

            if (mSelected == 1) // 1 menu_ble_emv
            {
                mServiceUuid = MTDeviceConstants.UUID_SCRA_BLE_EMV_DEVICE_READER_SERVICE;
            } else if (mSelected == 2) //2 menu_ble_emvt
            {
                mServiceUuid = MTDeviceConstants.UUID_SCRA_BLE_EMV_T_DEVICE_READER_SERVICE;
            } else // BLE 0
            {
                mServiceUuid = MTDeviceConstants.UUID_SCRA_BLE_DEVICE_READER_SERVICE;
            }

        }

        invalidateOptionsMenu();
    }

    private void scanFinished(String data){
        Toast.makeText(this, data , Toast.LENGTH_LONG).show();
    }

    private void selectBluetooth() {
        stopScanning();

//        mSelected = R.id.menu_bluetooth;
        mSelected = 4;
//
//        if (mMenu != null)
//        {
//            mMenu.findItem(R.id.menu_bluetooth).setVisible(false);
//            mMenu.findItem(R.id.menu_ble).setVisible(true);
//            mMenu.findItem(R.id.menu_ble_emv).setVisible(true);
//            mMenu.findItem(R.id.menu_ble_emvt).setVisible(true);
//            mMenu.findItem(R.id.menu_usb).setVisible(true);
//            mMenu.findItem(R.id.menu_serial).setVisible(true);
//        }
//
//        getActionBar().setSubtitle(R.string.title_bluetooth_devices);

        // Initializes list view adapter.
        mDeviceListAdapter = new DeviceListAdapter();
        setListAdapter(mDeviceListAdapter);
        scanBluetoothDevice(true);
    }


    // this is the class for devices list
    private class DeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mDevices;
        private LayoutInflater mInflator;

        public DeviceListAdapter() {
            super();
            mDevices = new ArrayList<BluetoothDevice>();
            mInflator = ScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if (!mDevices.contains(device)) {
                mDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mDevices.get(position);
        }

        public void clear() {
            mDevices.clear();
        }

        @Override
        public int getCount() {
            return mDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            try {
                BluetoothDevice device = mDevices.get(i);
                final String deviceName = device.getName();
                if (deviceName != null && deviceName.length() > 0)
                    viewHolder.deviceName.setText(deviceName);
                else
                    viewHolder.deviceName.setText(R.string.unknown_device);
                viewHolder.deviceAddress.setText(device.getAddress());
            } catch (Exception ex) {

            }

            return view;
        }
    }

    private void stopScanning() {
        if (mScanning) {
            if (mBluetoothAdapter != null) {
//                if (R.id.menu_bluetooth == mSelected)
                if (mSelected == 4) {
                    mBluetoothAdapter.cancelDiscovery();
                } else {
                    if (android.os.Build.VERSION.SDK_INT >= 21) {
                        BluetoothLeScanner leScanner = mBluetoothAdapter.getBluetoothLeScanner();

                        if ((leScanner != null) && (mScanCallback != null)) {
                            leScanner.stopScan(mScanCallback);
                        }
                    } else {
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    }

                }
            }

            mScanning = false;
        }
    }


    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            boolean found = false;

            if (scanRecord != null) {
                List<UUID> uuidList = parseUUIDs(scanRecord);

                ListIterator<UUID> uuidListIt = uuidList.listIterator();

                while (uuidListIt.hasNext()) {
                    UUID scanUuid = uuidListIt.next();

                    if (scanUuid.compareTo(mServiceUuid) == 0) {
                        found = true;
                    }
                }
            }

            if (found) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mDeviceListAdapter.addDevice(device);
                        mDeviceListAdapter.notifyDataSetChanged();
                    }
                });
            }
        }
    };

    private class CustomScanCallback extends ScanCallback {
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.e("onBatchScanResults ", "data");
            if (results != null) {
                ListIterator<ScanResult> resultsIt = results.listIterator();

                while (resultsIt.hasNext()) {
                    ScanResult result = resultsIt.next();

                    processScanResult(result);
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("onBatchScanResults ", String.valueOf(errorCode));
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            processScanResult(result);
        }

        private void processScanResult(ScanResult result) {

            Log.e("processScanResult ", result.toString());
            if (android.os.Build.VERSION.SDK_INT < 21) {
                return;
            }

            boolean found = false;

            if (result != null) {
                ScanRecord scanRecord = result.getScanRecord();
                final BluetoothDevice device = result.getDevice();

                if (scanRecord != null) {
                    List<UUID> uuidList = parseUUIDs(scanRecord.getBytes());

                    ListIterator<UUID> uuidListIt = uuidList.listIterator();

                    while (uuidListIt.hasNext()) {
                        UUID scanUuid = uuidListIt.next();

                        if (scanUuid.compareTo(mServiceUuid) == 0) {
                            found = true;
                        }
                    }
                }

                if (found && (device != null)) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mDeviceListAdapter.addDevice(device);
                            mDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }

            }
        }
    }

    ;

    private static List<UUID> parseUUIDs(final byte[] advertisedData) {
        List<UUID> uuids = new ArrayList<UUID>();

        Log.e("processScanResult ", "parseUUIDs");

        int offset = 0;
        while (offset < (advertisedData.length - 2)) {
            int len = advertisedData[offset++];
            if (len == 0)
                break;

            int type = advertisedData[offset++];
            switch (type) {
                case 0x06:// Partial list of 128-bit UUIDs
                case 0x07:// Complete list of 128-bit UUIDs
                    // Loop through the advertised 128-bit UUID's.
                    while (len >= 16) {
                        try {
                            // Wrap the advertised bits and order them.
                            ByteBuffer buffer = ByteBuffer.wrap(advertisedData,
                                    offset++, 16).order(ByteOrder.LITTLE_ENDIAN);
                            long mostSignificantBit = buffer.getLong();
                            long leastSignificantBit = buffer.getLong();
                            uuids.add(new UUID(leastSignificantBit,
                                    mostSignificantBit));
                        } catch (IndexOutOfBoundsException e) {
                            continue;
                        } finally {
                            offset += 15;
                            len -= 16;
                        }
                    }
                    break;
                default:
                    offset += (len - 1);
                    break;
            }
        }

        return uuids;
    }

    private void scanBluetoothDevice(final boolean enable) {
        if (!mBluetoothAdapter.isEnabled()) {
            return;
        }

        if (enable) {
            // Get a set of currently paired devices
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

            if (pairedDevices.size() > 0) {

                for (BluetoothDevice device : pairedDevices) {
                    if (device.getType() != BluetoothDevice.DEVICE_TYPE_LE) {
                        mDeviceListAdapter.addDevice(device);
                    }
                }
            }

            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.cancelDiscovery();
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startDiscovery();


        } else {
            mScanning = false;
            mBluetoothAdapter.cancelDiscovery();
        }

        invalidateOptionsMenu();
    }

    @Override
    protected void onPause()
    {
        super.onPause();

//        if (R.id.menu_bluetooth == mSelected)
        if(mSelected == 4)
        {

        }
        else
        {
            scanLeDevice(false);
            mDeviceListAdapter.clear();
            mDeviceListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy()
    {
        Log.e("Destroy " , "This is a destroy");
        this.unregisterReceiver(mReceiver);

        super.onDestroy();
    }




}