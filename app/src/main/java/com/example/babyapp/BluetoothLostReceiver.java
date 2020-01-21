package com.example.babyapp;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BluetoothLostReceiver extends BroadcastReceiver {

    HomeFragment homeFragment = null;

    public BluetoothLostReceiver(HomeFragment homeFragment){
        this.homeFragment = homeFragment;
    }

    // broadcast receiver for detecting when bluetooth device disconnects
    @Override
    public void onReceive(Context context, Intent intent) {
        if(BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(intent.getAction()))
            homeFragment.setIsConnected(false); // set the connection status to false when device connection is lost
    }
}
