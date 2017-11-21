package com.example.joseph.bluetoothchat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_DISCOVERABLE = 2;
    private static final String TAG = "MainActivity";
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 3;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean supported = true;
    private Set<BluetoothDevice> pairedDevices;
    private Button btnDiscoverable;
    private MyReceiver myReceiver;
    private RecyclerView rvDevices;
    private DeviceListAdapter deviceListAdapter;
    private Button btnScan;
    private boolean scanning;

    private BluetoothChatService mChatService = null;
    private String mConnectedDeviceName = null;

    private Activity activity;

    // TODO: 11/20/17 listen for ACTION_STATE_CHANGED broadcast intent, which the system broadcasts whenever the Bluetooth state changes

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        activity = this;

        scanning = false;

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTSupport();

        rvDevices = findViewById(R.id.rvDevices);
        deviceListAdapter = new DeviceListAdapter(this);
        rvDevices.setLayoutManager(new LinearLayoutManager(this));
        rvDevices.setAdapter(deviceListAdapter);

        btnDiscoverable = findViewById(R.id.btnDiscoverable);
        btnDiscoverable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mChatService = new BluetoothChatService(activity, mHandler);
                mChatService.start();
                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE);
            }
        });

        btnScan = findViewById(R.id.btnScan);
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(supported){
                    if(!scanning) {
                        checkPermission();
                    }else{
                        Toast.makeText(MainActivity.this, "still scanning", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(MainActivity.this, "Bluetooth not supported on this device", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Register for broadcasts when a device is discovered.
        myReceiver = new MyReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(myReceiver, filter);

    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "onCreate: not granted");

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                Log.d(TAG, "checkPermission: permission rationale");


            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);

                Log.d(TAG, "checkPermission: requesting permission");
            }
        } else {
            Log.d(TAG, "onCreate: already granted");
            checkBTEnabled();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Log.d(TAG, "onRequestPermissionsResult: granted");
                    checkBTEnabled();

                } else {

                    Log.d(TAG, "onRequestPermissionsResult: permission denied");
                }
                return;
            }

        }
    }

    public void checkBTSupport(){
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            supported = false;
        }
    }

    public void checkBTEnabled(){
        if (!mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "checkBTEnabled: requesting BT");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }else{
            Log.d(TAG, "checkBTEnabled: BT already enabled");
            mChatService = new BluetoothChatService(this, mHandler);
            mChatService.start();
            getDevices();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case REQUEST_ENABLE_BT:

                if(resultCode == RESULT_OK){
                    Log.d(TAG, "onActivityResult: BT enabled");
                    // Initialize the BluetoothChatService to perform bluetooth connections
                    mChatService = new BluetoothChatService(this, mHandler);
                    mChatService.start();
                    getDevices();
                }else{
                    // RESULT_CANCEL
                    Log.d(TAG, "onActivityResult: canceled");
                }

                break;

            case REQUEST_DISCOVERABLE:

                if(resultCode == RESULT_CANCELED){
                    Log.d(TAG, "onActivityResult: canceled");
                }else{
                    Log.d(TAG, "onActivityResult: discoverable for " + resultCode + " seconds");
                }

                break;
        }
    }

    public void getDevices(){
        getPairedDevices();
        Log.d(TAG, "getDevices: starting discovery");
        mBluetoothAdapter.startDiscovery();
    }

    public void getPairedDevices(){
        pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.d(TAG, "getPairedDevices: deviceName: " + deviceName);
                Log.d(TAG, "getPairedDevices: deviceHardwareAddress: " + deviceHardwareAddress);
                deviceListAdapter.addDevice(device);
            }
        }else{
            Log.d(TAG, "getPairedDevices: no paired devices");
        }
    }

    public class MyReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            switch (action) {
                case BluetoothDevice.ACTION_FOUND:

                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String deviceName = device.getName();
                    String deviceHardwareAddress = device.getAddress(); // MAC address
                    Log.d(TAG, "onReceive: " + deviceName + " " + deviceHardwareAddress);

                    deviceListAdapter.addDevice(device);

                    break;

                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:

                    Log.d(TAG, "onReceive: discovery started");

                    scanning = true;

                    break;

                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:

                    Log.d(TAG, "onReceive: discovery finished");

                    scanning = false;

                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Don't forget to unregister the ACTION_FOUND receiver.
        Log.d(TAG, "onDestroy: ");
//        mBluetoothAdapter.cancelDiscovery();
        unregisterReceiver(myReceiver);
        mChatService.stop();
    }


    public void connectDevice(BluetoothDevice device) {
        // Get the device MAC address
//        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
//        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, true);
    }


    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
//                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
//                            mConversationArrayAdapter.clear();
                            Log.d(TAG, "handleMessage: connected to: " + mConnectedDeviceName);
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
//                            setStatus(R.string.title_connecting);
                            Log.d(TAG, "handleMessage: connecting...");
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
//                            setStatus(R.string.title_not_connected);
                            Log.d(TAG, "handleMessage: not connected");
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
//                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    Log.d(TAG, "handleMessage: Me: " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
//                    mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    Log.d(TAG, "handleMessage: " + readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

}
