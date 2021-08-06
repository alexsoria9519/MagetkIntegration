package com.example.testmagtek;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.UUID;

public class ScanActivity extends AppCompatActivity {

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

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action))
            {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED)
                {
                    if (device.getType() != BluetoothDevice.DEVICE_TYPE_LE)
                    {
                        mDeviceListAdapter.addDevice(device);
                        mDeviceListAdapter.notifyDataSetChanged();
                    }
                }
                // When discovery is finished, change the Activity title
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
            {
                mScanning = false;
                invalidateOptionsMenu();
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        if (android.os.Build.VERSION.SDK_INT >= 23)
        {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(this,
                        new String[]{ Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN,
                                Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION }, PERMISSIONS_REQUEST);
            }
        }

        try
        {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String strAppVersion =  pInfo.versionName;

            getSupportActionBar().setTitle(getResources().getText(R.string.app_name) + " " + strAppVersion);
        }
        catch (Exception ex)
        {

            getSupportActionBar().setTitle(R.string.app_name);
        }

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

        if (mBluetoothAdapter == null)
        {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
        }


    }

    // this is the class for devices list
    private class DeviceListAdapter extends BaseAdapter
    {
        private ArrayList<BluetoothDevice> mDevices;
        private LayoutInflater mInflator;

        public DeviceListAdapter()
        {
            super();
            mDevices = new ArrayList<BluetoothDevice>();
            mInflator = ScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device)
        {
            if(!mDevices.contains(device))
            {
                mDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position)
        {
            return mDevices.get(position);
        }

        public void clear()
        {
            mDevices.clear();
        }

        @Override
        public int getCount()
        {
            return mDevices.size();
        }

        @Override
        public Object getItem(int i)
        {
            return mDevices.get(i);
        }

        @Override
        public long getItemId(int i)
        {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup)
        {
            ViewHolder viewHolder;
            if (view == null)
            {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            }
            else
            {
                viewHolder = (ViewHolder) view.getTag();
            }

            try
            {
                BluetoothDevice device = mDevices.get(i);
                final String deviceName = device.getName();
                if (deviceName != null && deviceName.length() > 0)
                    viewHolder.deviceName.setText(deviceName);
                else
                    viewHolder.deviceName.setText(R.string.unknown_device);
                viewHolder.deviceAddress.setText(device.getAddress());
            }
            catch (Exception ex)
            {

            }

            return view;
        }
    }






    static class ViewHolder
    {
        TextView deviceName;
        TextView deviceAddress;
    }
}