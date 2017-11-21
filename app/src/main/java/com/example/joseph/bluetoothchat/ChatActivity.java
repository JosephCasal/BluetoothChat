package com.example.joseph.bluetoothchat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    BluetoothChatService mChatService = null;
    String mConnectedDeviceName = null;

    Activity activity;

    MyApplication application;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        application = (MyApplication)getApplication();


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }



}
