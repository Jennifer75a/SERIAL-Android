//**************************************************
//     Copyright (c) 2014 Laird Technologies.
//     Copyright (c) 2015 Jennifer AUBINAIS.
//**************************************************
package com.ja.serial;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;

import com.lairdtech.bt.BluetoothAdapterWrapperCallback;
import com.ja.serial.serialdevice.SerialManager;
import com.ja.serial.serialdevice.SerialManagerUiCallback;
import com.lairdtech.bt.BluetoothAdapterWrapper;
import com.lairdtech.bt.ble.BleDeviceBase;
import com.lairdtech.misc.DebugWrapper;


public class MainActivity extends Activity implements SerialManagerUiCallback, BluetoothAdapterWrapperCallback {

    private Button mBtnSend;
    private EditText mValueVspInputEt;
    private ScrollView mScrollViewVspOut;
    private TextView mValueVspOutTv;

    private SerialManager mSerialManager;

    private boolean isPrefClearTextAfterSending = false;

    private static final int ENABLE_BT_REQUEST_ID = 1;
    private final static String TAG = "Base Activity";
    protected Activity mActivity;

    protected TextView mValueName;
    protected TextView mValueRSSI;
    protected TextView mValueBattery;
    protected Button mBtnScan;

    protected BluetoothAdapterWrapper mBluetoothAdapterWrapper;
    protected Dialog mDialogFoundDevices;
    private Dialog mDialogAbout;
    private View mViewAbout;

    protected ListFoundDevicesHandler mListFoundDevicesHandler = null;

    private BleDeviceBase mBleDeviceBase;

    protected SharedPreferences mSharedPreferences;
    protected boolean isInNewScreen = false;
    protected boolean isPrefRunInBackground = true;
    protected boolean isPrefPeriodicalScan = true;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        mActivity = this;
        setContentView(R.layout.activity_main);
        super.onCreate(savedInstanceState);

        mSerialManager = new SerialManager(this, this);
        setBleDeviceBase(mSerialManager.getVSPDevice());

        mBluetoothAdapterWrapper = new BluetoothAdapterWrapper(this,this);

        mBtnScan = (Button) findViewById(R.id.btnScan);
        mBtnSend = (Button) findViewById(R.id.btnSend);
        mScrollViewVspOut = (ScrollView) findViewById(R.id.scrollViewVspOut);
        mValueVspInputEt = (EditText) findViewById(R.id.valueVspInputEt);
        mValueVspOutTv = (TextView) findViewById(R.id.valueVspOutTv);

        mListFoundDevicesHandler = new ListFoundDevicesHandler(this);
        initialiseDialogFoundDevices("VSP");
        mActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        setListeners();

    }


    @Override
    protected void onPause(){
        super.onPause();

        if(isInNewScreen == true
                || isPrefRunInBackground == true){
            // let the app run normally in the background
        } else{
            // stop scanning or disconnect if we are connected
            if(mBluetoothAdapterWrapper.isBleScanning()){
                mBluetoothAdapterWrapper.stopBleScan();

            } else if(getBleDeviceBase().isConnecting()
                    || getBleDeviceBase().isConnected()){
                getBleDeviceBase().disconnect();
            }
        }
    }

    /*
     * *************************************
     * SerialManagerUiCallback
     * *************************************
     */
    @Override
    public void onUiConnected(BluetoothGatt gatt) {
        uiInvalidateBtnState();
    }

    @Override
    public void onUiDisconnect(BluetoothGatt gatt) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                mBtnSend.setEnabled(false);
                mValueVspInputEt.setEnabled(false);
            }
        });
        uiInvalidateBtnState();
    }

    @Override
    public void onUiConnectionFailure(
            final BluetoothGatt gatt){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                mBtnSend.setEnabled(false);
                mValueVspInputEt.setEnabled(false);
            }
        });
        uiInvalidateBtnState();
    }

    @Override
    public void onUiBatteryReadSuccess(String result) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onUiReadRemoteRssiSuccess(int rssi) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onUiBonded() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onUiVspServiceNotFound(BluetoothGatt gatt) {
        runOnUiThread(new Runnable(){
            @Override
            public void run() {

                mBtnSend.setEnabled(false);
                mValueVspInputEt.setEnabled(false);
            }
        });
    }

    @Override
    public void onUiVspRxTxCharsNotFound(BluetoothGatt gatt) {
        runOnUiThread(new Runnable(){
            @Override
            public void run() {

                mBtnSend.setEnabled(false);
                mValueVspInputEt.setEnabled(false);
            }
        });
    }

    @Override
    public void onUiVspRxTxCharsFound(BluetoothGatt gatt) {
        runOnUiThread(new Runnable(){
            @Override
            public void run() {

                mBtnSend.setEnabled(true);
                mValueVspInputEt.setEnabled(true);
            }
        });
    }

    @Override
    public void onUiSendDataSuccess(
            final String dataSend) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mValueVspOutTv.append(dataSend);
                mScrollViewVspOut.smoothScrollTo(0, mValueVspOutTv.getBottom());
            }
        });
    }

    @Override
    public void onUiReceiveData(final String dataReceived) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mValueVspOutTv.append(dataReceived);
                mScrollViewVspOut.smoothScrollTo(0, mValueVspOutTv.getBottom());
            }
        });
    }

    @Override
    public void onUiUploaded() {

        mBtnSend.setEnabled(true);
        mValueVspInputEt.setEnabled(true);
    }

    /**
     * Set the bleDevice base to the specific device manager for each application within the toolkit.
     * This ensures it is not null
     * @param bleDeviceBase
     */
    protected void setBleDeviceBase(BleDeviceBase bleDeviceBase){
        mBleDeviceBase = bleDeviceBase;
    }

    protected BleDeviceBase getBleDeviceBase(){
        return mBleDeviceBase;
    }

    /**
     * used for setting handler and adapter for the dialog listView.
     */
    protected void setAdapters() {
        //setting handler and adapter for the dialog list view
        mListFoundDevicesHandler = new ListFoundDevicesHandler(this);
    }

    /**
     * used to set onClickListener for the generic scan button.
     */
    protected void setListeners() {
        // set onClickListener for the scan button
        mBtnScan.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(v.getId())
                {
                    case R.id.btnScan:
                    {
                        if(mBluetoothAdapterWrapper.isEnabled() == false){
                            DebugWrapper.errorMsg("Bluetooth must be on to start scanning.", TAG, DebugWrapper.getDebugMessageVisibility() );
                            DebugWrapper.toastMsg(mActivity, "Bluetooth must be on to start scanning.");
                            return;
                        }else if(mBleDeviceBase.isConnected() == false
                                && mBleDeviceBase.isConnecting() == false){

                            // do a scan operation
                            if(isPrefPeriodicalScan == true){
                                mBluetoothAdapterWrapper.startBleScanPeriodically();
                            } else{
                                mBluetoothAdapterWrapper.startBleScan();
                            }

                            mDialogFoundDevices.show();

                        } else if(mBleDeviceBase.isConnected() == true){
                            mBleDeviceBase.disconnect();

                        } else if(mBleDeviceBase.isConnecting() == true){
                            DebugWrapper.toastMsg(mActivity, "Wait for connection!");
                        }
                        uiInvalidateBtnState();

                        break;
                    }
                }
            }
        });
        mBtnSend.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(v.getId())
                {
                    case R.id.btnSend:
                    {
                        String data = mValueVspInputEt.getText().toString();
                        if(data != null){
                            mBtnSend.setEnabled(false);
                            mValueVspInputEt.setEnabled(false);
                            if(mValueVspOutTv.getText().length() <= 0){
                                mValueVspOutTv.append(">");
                            } else{
                                mValueVspOutTv.append("\n\n>");
                            }

                            mSerialManager.startDataTransfer(data + "\r");

                            InputMethodManager inputManager = (InputMethodManager)
                                    getSystemService(Context.INPUT_METHOD_SERVICE);

                            inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                                    InputMethodManager.HIDE_NOT_ALWAYS);

                            if(isPrefClearTextAfterSending == true){
                                mValueVspInputEt.setText("");
                            } else{
                                // do not clear the text from the editText
                            }
                        }
                        break;
                    }
                }
            }
        });

    }

    /**
     * Initialize the dialog for the devices found from a BLE scan.
     * @param title
     */
    protected void initialiseDialogFoundDevices(String title) {
		/*
		 * create/set dialog ListView
		 */
        ListView mLvFoundDevices = new ListView(mActivity);
        mLvFoundDevices.setAdapter(mListFoundDevicesHandler);
        mLvFoundDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
                final BluetoothDevice device = mListFoundDevicesHandler.getDevice(position);
                if(device == null) return;

                mBluetoothAdapterWrapper.stopBleScan();
                mDialogFoundDevices.dismiss();
                mBleDeviceBase.connect(device, false);
                uiInvalidateBtnState();

            }
        });

		/*
		 * create and initialise Dialog
		 */
        mDialogFoundDevices = new Dialog(this);
        mDialogFoundDevices.setContentView(mLvFoundDevices);
        mDialogFoundDevices.setTitle("Select a "+ title +" device");
        mDialogFoundDevices.setCanceledOnTouchOutside(false);
        mDialogFoundDevices.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface arg0) {
                mBluetoothAdapterWrapper.stopBleScan();
                invalidateOptionsMenu();

            }
        });

        mDialogFoundDevices.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface arg0) {
                mListFoundDevicesHandler.clearList();
            }
        });
    }

    /**
     * Add the found devices to a listView in the dialog.
     * @param device
     * @param rssi
     * @param scanRecord
     */
    protected void handleFoundDevice(final BluetoothDevice device,
                                     final int rssi,
                                     final byte[] scanRecord){
        //adds found devices to list view
        runOnUiThread(new Runnable(){
            @Override
            public void run() {
                mListFoundDevicesHandler.addDevice(device, rssi, scanRecord);
                mListFoundDevicesHandler.notifyDataSetChanged();
            }
        });
    }

    protected void onResume() {
        super.onResume();
        loadPref();
        isInNewScreen = false;
        //check that BT is enabled as use could have turned it off during the onPause.
        if (mBluetoothAdapterWrapper.isEnabled() == false){
            Intent enableBTIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBTIntent, ENABLE_BT_REQUEST_ID);
        }
    }

    public void onBackPressed(){
        if(mBleDeviceBase.isConnected() == true){
            mBleDeviceBase.disconnect();
            invalidateOptionsMenu();
        }else{
            finish();
        }
    }


    protected void invalidateUI() {

        invalidateOptionsMenu();
    }


    /**
     * invalidate the scan button state
     */
    protected void uiInvalidateBtnState(){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if(mBleDeviceBase.isConnected() == false
                        && mBleDeviceBase.isConnecting() == false){
                    mBtnScan.setText(R.string.btn_scan);

                } else if(mBleDeviceBase.isConnected() == true){
                    mBtnScan.setText(R.string.btn_disconnect);

                } else if(mBleDeviceBase.isConnecting() == true){
                    mBtnScan.setText(R.string.btn_connecting);
                }

                invalidateOptionsMenu();
            }
        });
    }


	/*
	 *************************************
	 * Bluetooth adapter callbacks
	 * ***********************************
	 */

    @Override
    public void onBleStopScan() {
        // dismiss' dialog if no devices are found.
        if(mListFoundDevicesHandler.getCount() <= 0){
            mDialogFoundDevices.dismiss();
        }
        uiInvalidateBtnState();
    }

    @Override
    public void onBleDeviceFound(BluetoothDevice device, int rssi,
                                 byte[] scanRecord) {
        handleFoundDevice(device, rssi, scanRecord);
    }

    @Override
    public void onDiscoveryStop() {
        //NOT NEEDED
    }

    @Override
    public void onDiscoveryDeviceFound(BluetoothDevice device, int rssi) {
        //NOT NEEDED
    }

    protected void loadPref() {
        //isPrefRunInBackground = mSharedPreferences.getBoolean("pref_run_in_background", true);
        //isPrefPeriodicalScan = mSharedPreferences.getBoolean("pref_periodical_scan", true);
    }

}