// Campbell Maxwell
// November 2019 - Present

// TODO: Lots of refactoring, break out some classes etc

package com.example.android_test;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
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

import java.util.List;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    // Buttons and other interactive objects
    private Button button_Forward, button_Reverse, button_Left, button_Right, button_Exit, button_Connect;
    private Switch switch_Control;

    // Global variables and objects
    private BluetoothAdapter btAdapter;                 // BT Adapter object for use of BT (parent)
    private BluetoothLeScanner btScanner;               // BT object inherited from Adapter for BLE scans
    private int lastAction = -1;                        // Used to help identify button down and button up
    private boolean notScanning = true;                 // Stop unwanted button presses from bricking phone
    private BluetoothDevice btDevice_HMSoft = null;
    private BluetoothGatt btGatt;
    private BluetoothGattCharacteristic btGattCharacteristic = null;


    // Constant variables
    private final byte FORWARD = 3;                             // 3 is used on robot for motor start position for forwards
    private final byte REVERSE = 5;                             // 5 is used for motor to go in reverse direction
    private final byte STOP = 4;                                // This will stop steppers from rotating
    private final byte REMOTE = 1;                              // Send this byte to tell robot it is to be controlled from android
    private final byte SELF = 0;                                // Tell robot to switch to AI routine until toggled 1
    private final String ROBOT_NAME = "HMSOFT";                 // Name of bluetooth module connected to robot
    private final String ROBOT_MAC = "18:62:E4:3E:79:B2";       // MAC of above
    private final long SCAN_DURATION = 5000;                    // The BLE discovery scan duration length
    //private final long UUID_MSB = 0x0000ffe100001000L;          // First 64 bits of custom char. UUID
    //private final long UUID_LSB = 0x800000805f9b34fbL;          // Last 64 bits of custom char. UUID
    private final UUID CUSTOM_CHARACTERISTIC = new UUID(0x0000ffe100001000L, 0x800000805f9b34fbL);
    //private final long UUID_MSB_DESC = 0x0000290200001000L;
    //private final long UUID_LSB_DESC = 0x800000805f9b34fbL;
    private final UUID CUSTOM_DESCRIPTOR = new UUID(0x0000290200001000L, 0x800000805f9b34fbL);

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
                //finishAndRemoveTask();
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
    
    // Callback object for newLEScan to pass information from BT device about advertising devices
    final ScanCallback newCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (btDevice_HMSoft == null) {
                        boolean resultCheck = false;
                        try {
                            resultCheck = result.getDevice().getName().equalsIgnoreCase(ROBOT_NAME);
                        } catch (Exception e) {
                            return;
                        }
                        if (resultCheck) {
                            btDevice_HMSoft = result.getDevice();
                            displayDiscoveredConnection(result.getDevice().getName());
                            newLEScan(false);
                            connectDevice();
                        }
                    }
                }
            });
        }
    };
    final BluetoothGattCallback btGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                Snackbar.make(findViewById(R.id.switch_Control), "Connection Success!", Snackbar.LENGTH_SHORT).show();
                btGatt.discoverServices();
            }
            else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Snackbar.make(findViewById(R.id.switch_Control), "Device Disconnected", Snackbar.LENGTH_SHORT).show();
                newLEScan(true);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                getCharacter();
            }
            else {    // For this device we know there are services present, otherwise possible perpetual loop
                btGatt.discoverServices();
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (characteristic.equals(btGattCharacteristic)) {
                String s = new String(btGattCharacteristic.getValue());
                Snackbar.make(findViewById(R.id.switch_Control), s, Snackbar.LENGTH_SHORT).show();
            }
        }
    };
    // Initial setup of bluetooth to activate it ready to scan
    private void bluetoothSetup() {
        //btAdapter = BluetoothAdapter.getDefaultAdapter();
        int permissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        // TODO: Protection for a null return
        btAdapter = bluetoothManager.getAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();
        if (!btAdapter.isEnabled()) {
            Intent btIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(btIntent, 1);
        }
    }
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
                        if (!notScanning) {
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
    // Connect to GATT of device (check has already been performed to ensure it is the correct device)
    private void connectDevice() {
        Snackbar.make(findViewById(R.id.button_Connect), "Connecting...", Snackbar.LENGTH_SHORT).show();
        btGatt = btDevice_HMSoft.connectGatt(this, false, btGattCallback);
    }
    // Method to get specific characteristic shared with HM-10 device using already known UUID
    // After retrieval it gets specific descriptor used to enable notifications and enables it along with
    //  enabling them via the bluetoothGatt object
    private void getCharacter() {
        List<BluetoothGattService> serviceList = btGatt.getServices(); // get list of services on HM-10
        for (BluetoothGattService btgs : serviceList) {     // for each service in list...
            btGattCharacteristic = btgs.getCharacteristic(CUSTOM_CHARACTERISTIC);
            if (btGattCharacteristic != null) {
                break;
            }
        }
        // TODO: Some protection for null return
        BluetoothGattDescriptor descriptor = btGattCharacteristic.getDescriptor(CUSTOM_DESCRIPTOR);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        btGatt.writeDescriptor(descriptor);
        btGatt.setCharacteristicNotification(btGattCharacteristic, true);
    }
    // Puts the values into byte array for transfer to robot. Control toggle will override using 0,0 to relieve or 1,1 to regain control
    private void packageDirection(byte leftMotor, byte rightMotor) {
        byte[] outgoing = new byte[2];
        outgoing[0] = leftMotor;
        outgoing[1] = rightMotor;
        // TODO: More protection here eg check for btGatt connection
        if (btGattCharacteristic != null) {
            transferData(outgoing);
        }
    }
    // Put data into characteristic and write to device
    private void transferData(String s) {
        btGattCharacteristic.setValue(s);
        btGatt.writeCharacteristic(btGattCharacteristic);
    }
    private void transferData(byte[] bb) {
        btGattCharacteristic.setValue(bb);
        btGatt.writeCharacteristic(btGattCharacteristic);
        // FOR DEBUGGING:
        System.out.println("Data written to characteristic:");
        System.out.println(bb[0] + "  " + bb[1]);
    }

    // Quick display of callback results
    private void displayDiscoveredConnection(String s) {
        Snackbar.make(findViewById(R.id.button_Connect), s, Snackbar.LENGTH_SHORT).show();
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

    // Method to get all UUIDs for service / characteristic / descriptors and write to system.out
    private void getAllUUID() {
        List<BluetoothGattService> serviceList = btGatt.getServices(); // get list of services on HM-10
        for (BluetoothGattService btgs : serviceList) {     // for each service in list...
            System.out.println("Service UUID: " + btgs.getUuid());
            System.out.println("Characteristic UUID's for this Service:");
            List<BluetoothGattCharacteristic> characteristicList = btgs.getCharacteristics();   // get list of charac. from each service
            for (BluetoothGattCharacteristic btgc : characteristicList) {       //for each charac....
                System.out.println(btgc.getUuid()); // print UUID of each charac. to console
                List<BluetoothGattDescriptor> descriptorList = btGattCharacteristic.getDescriptors();
                if (!descriptorList.isEmpty()) {
                    for (BluetoothGattDescriptor btgd : descriptorList) {
                        System.out.println(btgd.getUuid());
                    }
                }
            }
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
    // Start BLE discovery scan using newer (API 21) library
   /* private void newLEScan() {
        if (notScanning) {
            notScanning = false;
            Snackbar.make(findViewById(R.id.switch_Control), "Starting Scan...", Snackbar.LENGTH_SHORT).show();
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    btScanner.stopScan(newCallback);
                    Snackbar.make(findViewById(R.id.switch_Control), "Scan Complete", Snackbar.LENGTH_SHORT).show();
                    notScanning = true;

                }
            }, SCAN_DURATION);
            btScanner.startScan(newCallback);
        }*/
}
