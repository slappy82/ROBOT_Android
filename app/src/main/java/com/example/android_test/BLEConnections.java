/*
    Campbell Maxwell
    January 2020 - Present
 */

package com.example.android_test;
import java.util.UUID;

public interface BLEConnections {

    /**
     * Name value of the HM-10 module
     */
    String ROBOT_NAME = "HMSOFT";

    /**
     * MAC address of the HM-10 module
     */
    String ROBOT_MAC = "18:62:E4:3E:79:B2";

    /**
     * UUID for the custom characteristic used by the HM-10 module for data transfer
     */
    UUID CUSTOM_CHARACTERISTIC = new UUID(0x0000ffe100001000L, 0x800000805f9b34fbL);

    /**
     * UUID for the descriptor used in the default characteristic of the HM-10 module. Used to enable notifications
     */
    UUID CUSTOM_DESCRIPTOR = new UUID(0x0000290200001000L, 0x800000805f9b34fbL);

    /**
     * Constant integer for use as a parameter with isInitialized method
     */
    int BLUETOOTH_ADAPTER = 0;

    /**
     * Constant integer for use as a parameter with isInitialized method
     */
    int BLUETOOTH_SCANNER = 1;

    /**
     * Constant integer for use as a parameter with isInitialized method
     */
    int BLUETOOTH_DEVICE = 2;

    /**
     * Constant integer for use as a parameter with isInitialized method
     */
    int BLUETOOTH_GATT = 3;

    /**
     * Constant integer for use as a parameter with isInitialized method
     */
    int BLUETOOTH_CHARACTERISTIC = 4;

    /**
     * Initiates a scan for advertising BLE devices. If the HM-10 module is detected it will attempt to make a connection
     * @param isStarting This method call is TRUE to start a new scan and FALSE to end a currently active scan
     */
    void startScanning(boolean isStarting);

    /**
     * Checks to see if the appropriate Bluetooth object has been initialized. Use constant members of class as values
     * @param bleType Takes an integer value (use the constant static class members) to select a Bluetooth object
     * @return Returns TRUE if the selected Bluetooth object is initialized and FALSE if not
     */
    boolean isInitialized(int bleType);

    /**
     * Writes the data contained in the byte array parameter to the current HM-10 characteristic
     * @param dataBlock Takes a byte array to be written to the default HM-10 characteristic
     */
    void writeToCharacteristic(byte[] dataBlock);

    /**
     * Writes the data contained in the String parameter to the current HM-10 characteristic
     * @param dataString Takes a String to be written to the default HM-10 characteristic
     */
    void writeToCharacteristic(String dataString);

    /**
     * Reads and returns data from the currently selected HM-10 characteristic
     * @return Returns a String containing the data in the current HM-10 characteristic
     */
    String readFromCharacteristic();

    /**
     * Closes connections and frees Bluetooth object resources ready for destructor call
     */
    void finishBLE();
}
