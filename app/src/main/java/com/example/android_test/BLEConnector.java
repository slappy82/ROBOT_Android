/*
    Campbell Maxwell
    January 2020 - Present
 */

package com.example.android_test;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Handler;
import android.view.View;
import com.google.android.material.snackbar.Snackbar;
import java.util.List;


public class BLEConnector implements BLEConnections {
    // CLASS VARIABLES
    private Context _contextMainApp;                                     // For getting data from Main activity
    private Handler _handleMainUI;                                       // For starting new threads on MainUI
    private View _viewSnackBar;                                          // For SnackBar use (switch_control view)
    private BluetoothAdapter _btAdapter;                                 // BTAdapter interacts with the phone's BT transceiver
    private BluetoothLeScanner _btScanner = null;                        // Scanner object used to scan for advertising devices
    private BluetoothDevice _btDevice = null;                            // A BT device detected by the BT scanner
    private BluetoothGatt _btGatt = null;                                // The GATT server hosted by the BT Device
    private BluetoothGattCharacteristic _btGattCharacteristic = null;    // The object used to read and write data to and from hosted on BT Device
    private boolean notScanning;                                         // Return TRUE if there is no current scan being performed on any thread

    /**
     * Class Constructor method to initialize an instance of this class
     * @param btAdapt Passes the Bluetooth Adapter used by this phone to perform all BLE related actions
     * @param mainCon Passes the Context from the main activity
     * @param snackView Passes any permanent UI View for anchoring the SnackBar to for feedback to the user
     */
    BLEConnector(BluetoothAdapter btAdapt, @org.jetbrains.annotations.NotNull Context mainCon, View snackView) {
        _btAdapter = btAdapt;
        _contextMainApp = mainCon;
        _handleMainUI = new Handler(mainCon.getMainLooper());
        _viewSnackBar = snackView;
        notScanning = true;
        setupScanner();
    }
    // CLASS CALLBACK METHODS
    /**
     * Callback object for leScanner to pass information from BLE adapter about advertising devices
     */
    private final ScanCallback _leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            _handleMainUI.post(new Runnable() {
                @Override
                public void run() {
                    if (_btDevice == null) {
                        setupDevice(result);
                    }
                }
            });
        }
    };
    /**
     * Callback object for connection attempts to HM-10 GATT server. Reacts to state changes between phone and the HM-10
     */
    private final BluetoothGattCallback _btGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                Snackbar.make(_viewSnackBar, "Connection Success!", Snackbar.LENGTH_SHORT).show();
                _btGatt.discoverServices();
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Snackbar.make(_viewSnackBar, "Device Disconnected", Snackbar.LENGTH_SHORT).show();
            }
        }
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                setupCharacteristic();
            } else {    // For this device we know there are services present, otherwise possible perpetual loop
                _btGatt.discoverServices();
            }
        }
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (characteristic.equals(_btGattCharacteristic)) {
                String newDataString = readFromCharacteristic();                // Stored for use in main program later
                //Snackbar.make(_viewSnackBar, newDataString, Snackbar.LENGTH_SHORT).show();;
            }
        }
    };
    // CLASS METHODS
    /**
     * Receives the BLE Scanner object from the Bluetooth Adapter. If an exception is thrown (eg BT Adapter is still null)
     * it will close this class instance
     */
    private void setupScanner() {
        try {
            _btScanner = _btAdapter.getBluetoothLeScanner();
        } catch (Exception e) {
            System.out.println(e.toString());
            finishBLE();
        }
    }

    // Scans for advertising BLE devices. See BLEConnections interface for more information
    @Override
    public void startScanning(boolean isStarting) {
        if (isStarting) {
            if (notScanning) {
                notScanning = false;
                Snackbar.make(_viewSnackBar, "Starting Scan...", Snackbar.LENGTH_SHORT).show();
                Handler handler = new Handler();
                // The BLE discovery scan duration length
                long SCAN_DURATION = 5000;
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!notScanning) {
                            startScanning(false);
                        }
                    }
                }, SCAN_DURATION);
                _btScanner.startScan(_leScanCallback);
            }
        } else {
            _btScanner.stopScan(_leScanCallback);
            Snackbar.make(_viewSnackBar, "Scan Complete", Snackbar.LENGTH_SHORT).show();
            notScanning = true;
        }
    }

    /**
     * Checks a ScanResult object to see if it is the HM-10 device we want. Notifies the user and attempts to make a connection
     *
     * @param result Takes a ScanResult object to check for our HM-10 module
     */
    private void setupDevice(ScanResult result) {
        boolean resultCheck;
        try {
            resultCheck = result.getDevice().getName().equalsIgnoreCase(ROBOT_NAME);
        } catch (Exception e) {
            return;
        }
        if (resultCheck) {
            _btDevice = result.getDevice();
            Snackbar.make(_viewSnackBar, (result.getDevice().getName()), Snackbar.LENGTH_SHORT).show();
            startScanning(false);
            connectToDevice();
        }
    }

    /**
     * Connect to GATT server of peripheral device. Check has already been performed to ensure it is the correct device
     */
    private void connectToDevice() {
        if (_btDevice != null) {
            try {
                _btGatt = _btDevice.connectGatt(_contextMainApp, false, _btGattCallback);
            } catch (Exception e) {
                finishBLE();
            }
        } else {
            Snackbar.make(_viewSnackBar, "No Device To Connect With - Try Scanning again", Snackbar.LENGTH_SHORT).show();
        }
    }

    /**
     * Retrieves the custom characteristic shared with the HM-10 module using our stored UUID.
     * After retrieval it obtains the descriptor used to enable notifications for the characteristic and
     * enables it via the BluetoothGatt object
     */
    private void setupCharacteristic() {
        List<BluetoothGattService> serviceList = _btGatt.getServices(); // get list of services on HM-10
        for (BluetoothGattService btgs : serviceList) {     // for each service in list...
            _btGattCharacteristic = btgs.getCharacteristic(CUSTOM_CHARACTERISTIC);
            if (_btGattCharacteristic != null) {
                break;
            }
        }
        BluetoothGattDescriptor descriptor = _btGattCharacteristic != null ? _btGattCharacteristic.getDescriptor(CUSTOM_DESCRIPTOR) : null;
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            _btGatt.writeDescriptor(descriptor);
            _btGatt.setCharacteristicNotification(_btGattCharacteristic, true);
        }
    }

    // Checks if selected Bluetooth object is not null. See BLEConnections interface for more information
    @Override
    public boolean isInitialized(int bleType) {
        switch (bleType) {
            case BLUETOOTH_ADAPTER:
                return _btAdapter != null;
            case BLUETOOTH_SCANNER:
                return _btScanner != null;
            case BLUETOOTH_DEVICE:
                return _btDevice != null;
            case BLUETOOTH_GATT:
                return _btGatt != null;
            case BLUETOOTH_CHARACTERISTIC:
                return _btGattCharacteristic != null;
            default:
                return false;
        }
    }

    // Writes byte array to characteristic. See BLEConnections interface for more information
    @Override
    public void writeToCharacteristic(byte[] dataBlock) {
        _btGattCharacteristic.setValue(dataBlock);
        _btGatt.writeCharacteristic(_btGattCharacteristic);
        // FOR DEBUGGING:
        System.out.println("Data written to characteristic:");
        System.out.println(dataBlock[0] + "  " + dataBlock[1]);
    }

    // Writes String to characteristic. See BLEConnections interface for more information
    @Override
    public void writeToCharacteristic(String dataString) {
        _btGattCharacteristic.setValue(dataString);
        _btGatt.writeCharacteristic(_btGattCharacteristic);
    }

    // Reads from characteristic. See BLEConnections interface for more information
    @Override
    public String readFromCharacteristic() {
        String s = "";
        try {
            s = new String(_btGattCharacteristic.getValue());
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        return s;
    }

    // Finalizes this class instance. See BLEConnections interface for more information
    @Override
    public void finishBLE() {
        try {
            _btGatt.disconnect();
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        _btAdapter = null;
    }

    /**
     * Prints all UUIDs of Services, Characteristics and Descriptors to System.out maintaining their respective hierarchy
     */
    private void getAllUUID() {
        List<BluetoothGattService> serviceList = _btGatt.getServices(); // get list of services on HM-10
        for (BluetoothGattService btgs : serviceList) {     // for each service in list...
            System.out.println("Service UUID: " + btgs.getUuid());
            System.out.println("Characteristic UUID's for this Service:");
            List<BluetoothGattCharacteristic> characteristicList = btgs.getCharacteristics();   // get list of charac. from each service
            for (BluetoothGattCharacteristic btgc : characteristicList) {       //for each charac....
                System.out.println(btgc.getUuid()); // print UUID of each charac. to console
                List<BluetoothGattDescriptor> descriptorList = _btGattCharacteristic.getDescriptors();
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
