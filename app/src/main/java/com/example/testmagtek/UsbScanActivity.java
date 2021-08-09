package com.example.testmagtek;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;

public class UsbScanActivity extends ListActivity {

    private DeviceListAdapter mDeviceListAdapter; // Devices list for usb and

    private boolean mScanning;
    private Handler mHandler;
    private int mSelected;

    private String[] mUSBDeviceAddressList;

    private UUID mServiceUuid;

    private CustomScanCallback mScanCallback = null;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    private static final int PERMISSIONS_REQUEST = 2001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_usb_scan);

        selectUsbDevice();
    }


    private void selectUsbDevice() {
        ArrayList<String> selectionList = getUsbDeviceSelections();

        if (selectionList.size() < 1) {
            Toast.makeText(this, "Usb devices not found", Toast.LENGTH_LONG).show();
            Log.e("Data usb Scan", "Usb devices not found");
            return;
        } else {
            stopScanning();

            //mSelected = R.id.menu_usb;

            mUSBDeviceAddressList = selectionList.toArray(new String[selectionList.size()]);

            if (mUSBDeviceAddressList.length > 1) {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
                dialogBuilder.setTitle("Select USB Device:");

                dialogBuilder.setNegativeButton(R.string.value_cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });

                dialogBuilder.setItems(mUSBDeviceAddressList,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                String selection = mUSBDeviceAddressList[which];

                                String address = getUsbDeviceAddress(selection);

                                selectUSBDeviceAddress(address);
                            }
                        });

                dialogBuilder.show();
            } else {
                selectUSBDeviceAddress("");
            }
        }
    }

    private void selectUSBDeviceAddress(String address) {
        Log.d("Data Address Selected", address);
        Toast.makeText(this, address, Toast.LENGTH_LONG).show();


//        final Intent intent = new Intent(this, MagTekDemo.class);
//        intent.putExtra(MagTekDemo.EXTRAS_CONNECTION_TYPE, MagTekDemo.EXTRAS_CONNECTION_TYPE_VALUE_USB);
//        intent.putExtra(MagTekDemo.EXTRAS_DEVICE_NAME, "USB");
//        intent.putExtra(MagTekDemo.EXTRAS_DEVICE_ADDRESS, address);
//
//        startActivity(intent);
    }


    // This is the function that get the usb selections
    private ArrayList<String> getUsbDeviceSelections() {
        ArrayList<String> selectionList = new ArrayList<String>();

        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        if (usbManager != null) {
            HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();

            Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

            while (deviceIterator.hasNext()) {
                UsbDevice device = deviceIterator.next();

                if (device != null) {
                    if (device.getVendorId() == 0x0801) {
                        String name = device.getDeviceName();

                        if (android.os.Build.VERSION.SDK_INT >= 21) {
                            String dsn = device.getSerialNumber();

                            if ((dsn != null) && !dsn.isEmpty()) {
                                name = dsn;
                            }
                        }

                        selectionList.add(name);
                    }
                }
            }
        } else {
            Log.e("UsbManager", "UsbManager is null");
        }

        return selectionList;
    }


    ///// This are the classes for the scan devices process


    private class CustomScanCallback extends ScanCallback {
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
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
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            processScanResult(result);
        }

        private void processScanResult(ScanResult result) {
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


    //this are the clases for adapter devices list

    private class DeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mDevices;
        private LayoutInflater mInflator;

        public DeviceListAdapter() {
            super();
            mDevices = new ArrayList<BluetoothDevice>();
            mInflator = UsbScanActivity.this.getLayoutInflater();
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

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    private String getUsbDeviceAddress(String selection) {
        String address = selection;

        if (android.os.Build.VERSION.SDK_INT > 21) {
            UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

            if (usbManager != null) {
                HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();

                Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

                while (deviceIterator.hasNext()) {
                    UsbDevice device = deviceIterator.next();

                    if (device != null) {
                        String dsn = device.getSerialNumber();

                        if ((dsn != null) && !dsn.isEmpty()) {
                            if (selection.equalsIgnoreCase(dsn)) {
                                address = device.getDeviceName();
                                break;
                            }
                        }
                    }
                }
            }
        }

        return address;
    }

    private void stopScanning()
    {
        if (mScanning)
        {
//            if (mBluetoothAdapter != null)
//            {
//                if (R.id.menu_bluetooth == mSelected)
//                {
//                    mBluetoothAdapter.cancelDiscovery();
//                }
//                else
//                {
//                    if (android.os.Build.VERSION.SDK_INT >= 21)
//                    {
//                        BluetoothLeScanner leScanner = mBluetoothAdapter.getBluetoothLeScanner();
//
//                        if ((leScanner != null) && (mScanCallback != null))
//                        {
//                            leScanner.stopScan(mScanCallback);
//                        }
//                    }
//                    else
//                    {
//                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
//                    }
//
//                }
//            }

            mScanning = false;
        }
    }


}