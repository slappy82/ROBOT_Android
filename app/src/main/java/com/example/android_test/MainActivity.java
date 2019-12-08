// Campbell Maxwell
// November 2019 - Present

package com.example.android_test;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import com.google.android.material.snackbar.Snackbar;


public class MainActivity extends AppCompatActivity {

    // Buttons and other interactive objects
    private Button button_Forward, button_Reverse, button_Left, button_Right, button_Exit, button_Connect;
    private Switch switch_Control;

    // Global variables and objects
    private BluetoothAdapter btAdapter;                 // BT Adapter object for use of BT (parent)
    private BluetoothLeScanner btScanner;               // BT object inherited from Adapter for BLE scans
    private int lastAction = 0;                         // Used to help identify button down and button up
    private boolean notScanning = true;                 // Stop unwanted button presses from bricking phone
    private BluetoothDevice btDevice_HMSoft = null;

    // Constant variables
    private final byte FORWARD = 3;                     // 3 is used on robot for motor start position for forwards
    private final byte REVERSE = 5;                     // 5 is used for motor to go in reverse direction
    private final byte STOP = 4;                        // This will stop steppers from rotating
    private final byte REMOTE = 1;                      // Send this byte to tell robot it is to be controlled from android
    private final byte SELF = 0;                        // Tell robot to switch to AI routine until toggled 1
    private final String ROBOT_NAME = "HMSoft";         // Name of bluetooth module connected to robot
    private final String ROBOT_MAC = "18:62:E4:3E:79:B2";    // MAC of above
    private final long SCAN_DURATION = 5000;           // The BLE discovery scan duration length

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Link my buttons to their graphical object
        button_Exit = findViewById(R.id.button_Exit);
        button_Forward = findViewById(R.id.button_Forward);
        button_Reverse = findViewById(R.id.button_Reverse);
        button_Left = findViewById(R.id.buttonLeft);
        button_Right = findViewById(R.id.buttonRight);
        button_Connect = findViewById(R.id.button_Connect);
        switch_Control = findViewById(R.id.switch_Control);
        enableButtons(false);
        bluetoothSetup();

        // Click events for my buttons
        button_Exit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                tidyUp();
                System.exit(0);
            }
        });
        button_Connect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                //oldLEScan();
                newLEScan(true);
            }
        });
        switch_Control.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    packageDirection(REMOTE, REMOTE);
                    enableButtons(true);
                }
                else {
                    packageDirection(SELF, SELF);
                    enableButtons(false);
                }
            }
        });
        button_Forward.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View view, MotionEvent moEvent) {
                if (moEvent.getAction() == MotionEvent.ACTION_DOWN && lastAction != MotionEvent.ACTION_DOWN) {
                    lastAction  = MotionEvent.ACTION_DOWN;
                    packageDirection(FORWARD, FORWARD);
                    return true;
                }
                if (moEvent.getAction() == MotionEvent.ACTION_UP) {
                    lastAction = MotionEvent.ACTION_UP;
                    packageDirection(STOP, STOP);
                    return true;
                }
                return false;
            }
        });
        button_Reverse.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View view, MotionEvent moEvent) {
                if (moEvent.getAction() == MotionEvent.ACTION_DOWN && lastAction != MotionEvent.ACTION_DOWN) {
                    lastAction  = MotionEvent.ACTION_DOWN;
                    packageDirection(REVERSE, REVERSE);
                    return true;
                }
                if (moEvent.getAction() == MotionEvent.ACTION_UP) {
                    lastAction = MotionEvent.ACTION_UP;
                    packageDirection(STOP, STOP);
                    return true;
                }
                return false;
            }
        });
        button_Left.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View view, MotionEvent moEvent) {
                if (moEvent.getAction() == MotionEvent.ACTION_DOWN && lastAction != MotionEvent.ACTION_DOWN) {
                    lastAction  = MotionEvent.ACTION_DOWN;
                    packageDirection(REVERSE, FORWARD);
                    return true;
                }
                if (moEvent.getAction() == MotionEvent.ACTION_UP) {
                    lastAction = MotionEvent.ACTION_UP;
                    packageDirection(STOP, STOP);
                    return true;
                }
                return false;
            }
        });
        button_Right.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View view, MotionEvent moEvent) {
                if (moEvent.getAction() == MotionEvent.ACTION_DOWN && lastAction != MotionEvent.ACTION_DOWN) {
                    lastAction  = MotionEvent.ACTION_DOWN;
                    packageDirection(FORWARD, REVERSE);
                    return true;
                }
                if (moEvent.getAction() == MotionEvent.ACTION_UP) {
                    lastAction = MotionEvent.ACTION_UP;
                    packageDirection(STOP, STOP);
                    return true;
                }
                return false;
            }
        });
    }

    // Puts the values into byte array for transfer to robot. Control toggle will override using 0,0 to relieve or 1,1 to regain control
    private void packageDirection(byte leftMotor, byte rightMotor) {
           byte[] outgoing = new byte[2];
           outgoing[0] = leftMotor;
           outgoing[1] = rightMotor;
            //  TODO: Transfer packet via bluetooth adapter socket
    }
    // Callback object for newLEScan to pass information from BT device about advertising devices
    final ScanCallback newCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            //super.onScanResult(callbackType, result);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (result.getDevice().getName().equalsIgnoreCase(ROBOT_NAME)
                                    && result.getDevice().getAddress().equalsIgnoreCase(ROBOT_MAC) //){
                                    && !result.getDevice().equals(btDevice_HMSoft)) {
                        btDevice_HMSoft = result.getDevice();
                        displayDiscoveredConnection(result.getDevice().getName());
                        newLEScan(false);
                        connectDevice();
                    }
                }
            });
        }
    };
    // Start BLE discovery scan using newer (API 21) library
    private void newLEScan(boolean scanStarting) {
        if (scanStarting) {
            if (notScanning) {
                notScanning = false;
                Snackbar.make(findViewById(R.id.switch_Control), "Starting Scan...", Snackbar.LENGTH_SHORT).show();
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!notScanning) {             //    test for any active scan
                            newLEScan(false);
                        }
                    }
                }, SCAN_DURATION);
                btScanner.startScan(newCallback);
            }
        }
        else {
            btScanner.stopScan(newCallback);
            Snackbar.make(findViewById(R.id.switch_Control), "Scan Complete", Snackbar.LENGTH_SHORT).show();
            notScanning = true;
        }
    }
    // Quick display of callback results
    private void displayDiscoveredConnection(String s) {
        Snackbar.make(findViewById(R.id.button_Connect), s, Snackbar.LENGTH_SHORT).show();
    }
    // Connect to GATT of device (check has already been performed to ensure it is the correct device)
    private void connectDevice() {
        Snackbar.make(findViewById(R.id.button_Connect), "Connecting...", Snackbar.LENGTH_SHORT).show();
            // TODO - THEN figure out connection to GATT etc
    }
    // Remove connections and reset android
    private void tidyUp() {
        if (btAdapter.isEnabled()) {
            btAdapter.disable();
            btAdapter = null;
        }
        if (!notScanning) {
            //btAdapter.stopLeScan(oldCallback);
            btScanner.stopScan(newCallback);
        }
    }
    // Show all buttons as active or inactive and disabled
    private void enableButtons(boolean bool) {
        button_Forward.setClickable(bool);
        button_Forward.setEnabled(bool);
        button_Reverse.setClickable(bool);
        button_Reverse.setEnabled(bool);
        button_Left.setClickable(bool);
        button_Left.setEnabled(bool);
        button_Right.setClickable(bool);
        button_Right.setEnabled(bool);
    }

    // Initial setup of bluetooth to activate it ready to scan
    private void bluetoothSetup() {
        //btAdapter = BluetoothAdapter.getDefaultAdapter();
        int permissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = bluetoothManager.getAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();
        if (!btAdapter.isEnabled()) {
            Intent btIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(btIntent, 1);
        }
    }

    // Callback for BLE scanner for older android version - before API 21
    /*final BluetoothAdapter.LeScanCallback oldCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (device.getName().equalsIgnoreCase(ROBOT_NAME) && device.getAddress().equalsIgnoreCase(ROBOT_MAC)) {
                        CharSequence cs = device.getName();
                        displayDiscoveredConnection(cs);
                        //connectDevice();
                    }
                }
            });
        }
    };
    // This is for an older version of android - before API 21
    private void oldLEScan() {
        if (notScanning) {
            notScanning = false;
            Snackbar.make(findViewById(R.id.switch_Control), "Starting Scan...", Snackbar.LENGTH_SHORT).show();
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    btAdapter.stopLeScan(oldCallback);
                    Snackbar.make(findViewById(R.id.switch_Control), "Scan Complete", Snackbar.LENGTH_SHORT).show();
                    notScanning = true;
                }
            }, SCAN_DURATION);
            btAdapter.startLeScan(oldCallback);
        }
    }*/
}
