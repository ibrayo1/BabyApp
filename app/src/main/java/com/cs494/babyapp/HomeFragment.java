package com.cs494.babyapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static android.app.Activity.RESULT_OK;

public class HomeFragment extends Fragment {

    // 00001101-0000-1000-8000-00805F9B34FB is the standard uuid for the hc-05 module
    // 00:14:03:06:81:42 is the module's mac address and H-C-2010-06-01 its the name
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mmSocket;
    private BluetoothDevice mmDevice;
    private InputStream mmInputStream;
    private Thread workerThread;
    private Thread connectThread;
    private BluetoothLostReceiver bluetoothLostReceiver;
    private byte[] readBuffer;
    private int readBufferPosition;
    private volatile boolean stopWorker = true;
    private volatile boolean isConnected = false;
    private final Handler handler = new Handler();

    // declare all the views that user interact withs
    private Button connect_btn;
    private LinearLayout statistics;
    private RelativeLayout loadingPanel;

    // declare all the placeholders that get updated from bluetooth
    private TextView bpm;
    private TextView respiration;
    private TextView oxygen;
    private TextView temperature;

    // text that is updated from setting
    private TextView temp_scale;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // declare all the views that user interact withs
        connect_btn = (Button) view.findViewById(R.id.connect_button);
        statistics = (LinearLayout) view.findViewById(R.id.statistics);
        loadingPanel = (RelativeLayout) view.findViewById(R.id.loadingPanel);

        // declare all the placeholders that get updated from bluetooth
        bpm = (TextView) view.findViewById(R.id.bpm);
        respiration = (TextView) view.findViewById(R.id.respiration);
        oxygen = (TextView) view.findViewById(R.id.oxygen);
        temperature = (TextView) view.findViewById(R.id.temperature);

        // get the scale text view
        temp_scale = view.findViewById(R.id.temperature_scale);

        /* for debug purposes */
        //connect_btn.setVisibility(View.GONE);
        //statistics.setVisibility(View.VISIBLE);

        // when user clicks the button connect to bluetooth device
        connect_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkBT();
                //findBT();
            }
        });

        return view;
    }

    @Override
    public void onDestroy() {
        try{ // close the input stream and bt socket
            closeBT();
        }
        catch (IOException e){e.printStackTrace();}

        // unregister broadcast receiver
        if(bluetoothLostReceiver != null)
            getActivity().unregisterReceiver(bluetoothLostReceiver);

        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == 0){
            if(resultCode == RESULT_OK){
                connect_btn.setVisibility(View.GONE);
                loadingPanel.setVisibility(View.VISIBLE);

                try {
                    connectBT();
                } catch (InterruptedException ie){
                    ie.printStackTrace();
                }
            }
        }
    }

    // setter method which sets the temperature scale
    public void setTempScale(String temperature_scale){
        if(temperature_scale.equals("Celsius")) temp_scale.setText("°C"); else temp_scale.setText("°F");
    }

    // method which sets the connection status of the bt device
    public void setIsConnected(boolean status){
        this.isConnected = status;
    }

    // this method checks to see if the phone supports bluetooth capabilites
    private void checkBT(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
            Toast.makeText(getContext(), "Device doesn't support bluetooth", Toast.LENGTH_SHORT).show();

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        } else {
            connect_btn.setVisibility(View.GONE);
            loadingPanel.setVisibility(View.VISIBLE);

            try {
                connectBT();
            } catch (InterruptedException ie){
                ie.printStackTrace();
            }
        }
    }

    // this method activates a thread which attempts to connect to the bluetooth device
    private void connectBT() throws InterruptedException{

        final byte delimiter = 10; //This is the ASCII code for a newline character
        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];

        connectThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // connect to this device via this mac address
                mmDevice = mBluetoothAdapter.getRemoteDevice("00:14:03:06:81:42");
                UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID

                // try to connect to the bluetooth
                try {
                    // Get a BluetoothSocket to connect with the given BluetoothDevice.
                    // MY_UUID is the app's UUID string, also used in the server code.
                    mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);

                    // Cancel discovery because it otherwise slows down the connection.
                    mBluetoothAdapter.cancelDiscovery();

                    // Connect to the remote device through the socket. This call blocks
                    // until it succeeds or throws an exception.
                    mmSocket.connect();

                    // get the input stream
                    mmInputStream = mmSocket.getInputStream();

                } catch (IOException ioe){ // post a toast message if failed to connect
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            connect_btn.setVisibility(View.VISIBLE);
                            loadingPanel.setVisibility(View.GONE);
                            Toast.makeText(getContext(), "Failed to Connect", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                // update the the ui if the bluetooth socket connects and start the worker thread
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(mmSocket.isConnected()){
                            loadingPanel.setVisibility(View.GONE);
                            statistics.setVisibility(View.VISIBLE); isConnected = true;
                            Toast.makeText(getContext(), "Connected", Toast.LENGTH_SHORT).show();

                            if(bluetoothLostReceiver == null){
                                bluetoothLostReceiver = new BluetoothLostReceiver(HomeFragment.this);
                                IntentFilter filter = new IntentFilter("android.bluetooth.device.action.ACL_DISCONNECTED");
                                getActivity().registerReceiver(bluetoothLostReceiver, filter);
                            }

                            workerThread.start();
                        }
                    }
                });

            }
        });

        workerThread = new Thread(new Runnable() {

            public void run()
            {
                // continuously read the data being sent via hc-05 while the device is connected
                while(!stopWorker && isConnected)
                {
                    try
                    {
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "UTF-8");
                                    readBufferPosition = 0;

                                    // update the ui thread with the data received through bluetooth
                                    handler.post(new Runnable() {

                                        public void run() //get the data
                                        {
                                            String [] res = data.split(",");

                                            // set the text views
                                            bpm.setText(res[0]);
                                            respiration.setText(res[1]);
                                            oxygen.setText(res[2]);
                                            temperature.setText(res[3]);
                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                        isConnected = false;
                        // if we reach here then that means the bluetooth module disconnected
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                connect_btn.setVisibility(View.VISIBLE);
                                statistics.setVisibility(View.INVISIBLE);
                                Toast.makeText(getContext(), "Disconnected", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }

                // if we reach here then that means the bluetooth module disconnected
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        connect_btn.setVisibility(View.VISIBLE);
                        statistics.setVisibility(View.INVISIBLE);
                        Toast.makeText(getContext(), "Disconnected", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        connectThread.start();

    }

    private void closeBT() throws IOException
    {
        stopWorker = true;
        isConnected = false;
        mmInputStream.close();
        mmSocket.close();
    }

}
