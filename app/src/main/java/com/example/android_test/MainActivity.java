/*
    Campbell Maxwell
    November 2019 - Present
 */

package com.example.android_test;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

public class MainActivity extends AppCompatActivity {

    // Buttons and other interactive objects
    private Button button_Forward, button_Reverse, button_Left, button_Right, button_Exit, button_Connect;
    private Switch switch_Control;

    // Global variables and objects
    BLEConnector bleConnector;                          // BLE object to manage BLE HM-10 connection
    private int lastAction = -1;                        // Used to help identify button down and button up based on the last button action

    // Constant variables
    private final byte FORWARD = 2;                     // Used on robot for motor start position for forwards
    private final byte REVERSE = 6;                     // Used for motor to go in reverse direction
    private final byte STOP = 4;                        // This will stop robot movement
    private final byte REMOTE = 1;                      // Tell robot it is to be controlled from android until sent '0'
    private final byte SELF = 0;                        // Tell robot to switch to AI routine until sent '1'

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
        enableButtons(true);

        /* UI Events */
        button_Exit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                packageDirection(SELF, SELF);
                bluetoothFinish();
            }
        });
        button_Connect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                bleConnector.startScanning(true);
            }
        });
        switch_Control.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    packageDirection(REMOTE, REMOTE);
                    enableButtons(true);
                } else {
                    packageDirection(SELF, SELF);
                    enableButtons(false);
                }
            }
        });
        button_Forward.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View view, MotionEvent moEvent) {
                if (moEvent.getAction() == MotionEvent.ACTION_DOWN && lastAction != MotionEvent.ACTION_DOWN) {
                    lastAction = MotionEvent.ACTION_DOWN;
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
                    lastAction = MotionEvent.ACTION_DOWN;
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
                    lastAction = MotionEvent.ACTION_DOWN;
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
                    lastAction = MotionEvent.ACTION_DOWN;
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
    @Override
    protected void onStart() {
        super.onStart();
        bluetoothSetup();
    }
    @Override
    protected void onResume() {
        super.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
    }
    @Override
    protected void onStop() {
        super.onStop();
        //bluetoothFinish();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * Initial setup of Bluetooth BLE including enabling of permissions and Bluetooth Adapter if disabled. Creates instance of
     * BLEConnector object to manage detection, connection and data transfer to and from our HM-10 module
     */
    private void bluetoothSetup() {
        int permissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!btAdapter.isEnabled()) {
            Intent btIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(btIntent, 1);
        }
        bleConnector = new BLEConnector(btAdapter, this, findViewById(R.id.switch_Control));
    }

    /**
     * Puts the values into byte array for transfer to robot. Control switch will override using 0,0 to relieve or 1,1 to regain control
     * @param leftMotor The movement value for the direction of the left motor on the Arduino board
     * @param rightMotor The movement value for the direction of the right motor on the Arduino board
     */
    private void packageDirection(byte leftMotor, byte rightMotor) {
        byte[] outgoing = new byte[2];  // Could refactor to a single byte and use bitwise commands on robot to mask direction command
        outgoing[0] = leftMotor;
        outgoing[1] = rightMotor;
        if (bleConnector.isInitialized(BLEConnections.BLUETOOTH_CHARACTERISTIC)) {
            bleConnector.writeToCharacteristic(outgoing);
        }
    }

    /**
     * Remove connections and reset Activity
     */
    private void bluetoothFinish() {
        bleConnector.finishBLE();
        bleConnector = null;
        finishAndRemoveTask();
    }

    /**
     * Show all buttons as active or inactive and disabled
     * @param bool TRUE will enable all buttons, FALSE will disable and deactivate them
     */
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

}
