/*****************************************************************************
* Copyright (c) 2014 Laird Technologies. All Rights Reserved.
* 
* The information contained herein is property of Laird Technologies.
* Licensees are granted free, non-transferable use of the information. NO WARRANTY of ANY KIND is provided. 
* This heading must NOT be removed from the file.
******************************************************************************/

package com.ja.serial.bases;

import android.bluetooth.BluetoothGatt;

public interface BaseActivityUiCallback {

    public void onUiConnected(
           final BluetoothGatt gatt);
    
    public void onUiDisconnect(
            final BluetoothGatt gatt);
    
    public void onUiConnectionFailure(
    		final BluetoothGatt gatt);
    
    public void onUiBatteryReadSuccess(
    		final String result);
    
    public void onUiReadRemoteRssiSuccess(final int rssi);

    public void onUiBonded();//TODO am i using this? 
    
}
