package com.blecentral;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();

    private BluetoothLeScanner bluetoothLeScanner;
    private boolean mScanning = false;
    private Thread scanningThread;
    private Handler handler;

    BluetoothDevice device;
    BluetoothGatt bluetoothGatt = null;
    BluetoothGattCharacteristic checkignOptionsCharacteristic = null;
    BluetoothGattCharacteristic workerCheckingCharacteristic = null;
    private static final long SCANNING_PERIOD = 10000;

    UUID workingTimeEvidenceServiceUUID = UUID.fromString("000012ab-0000-1000-8000-00805f9b34fb");
    UUID checkignOptionsCharacteristicUUID = UUID.fromString("000034cd-0000-1000-8000-00805f9b34fb");
    UUID workerCheckingCharacteristicUUID = UUID.fromString("000035cd-0000-1000-8000-00805f9b34fb");

    private Button buttonArrival;
    private Button buttonPause;
    private Button buttonOfficalExit;
    private Button buttonWorkEnd;
    private Button buttonConnect;
    private ProgressBar progressBar;

    final String  OPTION_CHECK_IN = "OPTION_CHECK_IN";
    final String  OPTION_CHECK_END = "OPTION_CHECK_END";
    final String  OPTION_CHECK_OFFICAL_EXIT = "OPTION_CHECK_OFFICAL_EXIT";
    final String  OPTION_CHECK_PAUSE = "OPTION_CHECK_PAUSE";

    private String userId = "hasjdkhsa18293";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        int permissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
        initViews();
        handler = new Handler();

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            if(areLocationServicesEnabled(this)) {

                if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                    Toast.makeText(this, "Ble not supported", Toast.LENGTH_SHORT).show();
                    finish();
                }
                bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
                setInitOptions();
            }
        }
    }

    private void initViews(){
        this.buttonArrival = (Button) findViewById(R.id.button);
        this.buttonPause = (Button) findViewById(R.id.button2);
        this.buttonOfficalExit = (Button) findViewById(R.id.button3);
        this.buttonWorkEnd = (Button) findViewById(R.id.button4);
        this.buttonConnect = (Button) findViewById(R.id.button_connect);
        this.progressBar = (ProgressBar) findViewById(R.id.progressBar);

        buttonArrival.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                workerCheckingCharacteristic.setValue(OPTION_CHECK_IN.getBytes());
                workerCheckingCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                bluetoothGatt.writeCharacteristic(workerCheckingCharacteristic);
            }
        });

        buttonPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                workerCheckingCharacteristic.setValue(OPTION_CHECK_PAUSE.getBytes());
                workerCheckingCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                bluetoothGatt.writeCharacteristic(workerCheckingCharacteristic);
            }
        });

        buttonOfficalExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                workerCheckingCharacteristic.setValue(OPTION_CHECK_OFFICAL_EXIT.getBytes());
                workerCheckingCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                bluetoothGatt.writeCharacteristic(workerCheckingCharacteristic);
            }
        });

        buttonWorkEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                workerCheckingCharacteristic.setValue(OPTION_CHECK_END.getBytes());
                workerCheckingCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                bluetoothGatt.writeCharacteristic(workerCheckingCharacteristic);
            }
        });

        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Click");
                scanBleDevices();
            }
        });
    }

    private void setCheckInOption(){
        buttonArrival.setVisibility(View.VISIBLE);
        buttonPause.setVisibility(View.INVISIBLE);
        buttonOfficalExit.setVisibility(View.INVISIBLE);
        buttonWorkEnd.setVisibility(View.INVISIBLE);
        buttonConnect.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.INVISIBLE);
    }

    private void setExitOptions(){
        buttonArrival.setVisibility(View.INVISIBLE);
        buttonPause.setVisibility(View.VISIBLE);
        buttonOfficalExit.setVisibility(View.VISIBLE);
        buttonWorkEnd.setVisibility(View.VISIBLE);
        buttonConnect.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.INVISIBLE);
    }

    private void setInitOptions(){
        buttonArrival.setVisibility(View.INVISIBLE);
        buttonPause.setVisibility(View.INVISIBLE);
        buttonOfficalExit.setVisibility(View.INVISIBLE);
        buttonWorkEnd.setVisibility(View.INVISIBLE);
        buttonConnect.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.INVISIBLE);
    }

    private void setScanningScreen(){
        buttonArrival.setVisibility(View.INVISIBLE);
        buttonPause.setVisibility(View.INVISIBLE);
        buttonOfficalExit.setVisibility(View.INVISIBLE);
        buttonWorkEnd.setVisibility(View.INVISIBLE);
        buttonConnect.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
    }

    private void startScanning(){
        ScanFilter scanFilter = new ScanFilter.Builder()
                .setDeviceName("RPI")
                .build();
        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .build();
        bluetoothLeScanner.startScan(Arrays.asList(scanFilter), scanSettings, leScanCallback);
        mScanning = true;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setScanningScreen();
            }
        });
    }

    private Runnable scanRunnable = new Runnable() {
        @Override
        public void run() {
            startScanning();
        }
    };

    private void scanBleDevices(){
        if(mScanning){
            return;
        }

        scanningThread = new Thread(scanRunnable);
        scanningThread.start();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(mScanning) {
                    stopScanning();
                    Toast.makeText(getApplicationContext(), "Could not check you in", Toast.LENGTH_LONG).show();
                    setInitOptions();
                }
            }
        }, SCANNING_PERIOD);

    }

    private void stopScanning(){
        bluetoothLeScanner.stopScan(leScanCallback);
        mScanning = false;
        Log.i(TAG, "Stopped scanning");
    }

    private ScanCallback leScanCallback =
        new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                if(result.getDevice().getName() != null && result.getDevice().getName().equals("RPI")){
                    stopScanning();
                    device = result.getDevice();
                    device.connectGatt(getApplicationContext(),false ,gattCallback);
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.i(TAG, "Scan failed");
            }
        };

    public boolean areLocationServicesEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        try {
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private final BluetoothGattCallback gattCallback =
        new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if(status == BluetoothGatt.GATT_SUCCESS) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.i(TAG, "Connected to GATT server.");
                        Log.i(TAG, "Attempting to start service discovery:" + gatt.discoverServices());
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.w(TAG, "Successfully disconnected from $deviceAddress");
                        gatt.close();
                    }
                }else{
                    Log.i(TAG, "Cant connect");
                    gatt.close();
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                List<BluetoothGattService> gattServiceList = gatt.getServices();
                BluetoothGattService targetService = null;
                for(BluetoothGattService gattService : gattServiceList){
                    Log.i(TAG, gattService.getUuid().toString());
                    if (workingTimeEvidenceServiceUUID.equals(gattService.getUuid())) {
                        targetService = gattService;
                    }
                }

                if (targetService != null) {
                    bluetoothGatt = gatt;
                    List<BluetoothGattCharacteristic> characteristics =  targetService.getCharacteristics();
                    for(BluetoothGattCharacteristic characteristic : characteristics){
                        if (checkignOptionsCharacteristicUUID.equals(characteristic.getUuid())) {
                            checkignOptionsCharacteristic = characteristic;
                        }
                        if (workerCheckingCharacteristicUUID.equals(characteristic.getUuid())) {
                            workerCheckingCharacteristic = characteristic;
                        }
                    }

                    if (checkignOptionsCharacteristic != null && workerCheckingCharacteristic != null) {
                        checkignOptionsCharacteristic.setValue(userId.getBytes());
                        checkignOptionsCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                        gatt.writeCharacteristic(checkignOptionsCharacteristic);
                    } else {
                        gatt.close();
                    }
                }
                else {
                    bluetoothGatt = null;
                    gatt.close();
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                if(characteristic.getUuid().equals(checkignOptionsCharacteristicUUID)){
                    String s = new String(characteristic.getValue(), StandardCharsets.UTF_8);
                    if (s.equals(OPTION_CHECK_IN)) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setCheckInOption();
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setExitOptions();
                            }
                        });
                    }
                }
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (characteristic.getUuid().equals(checkignOptionsCharacteristicUUID)) {
                        Log.i(TAG, "Response: " + new String(characteristic.getValue(),StandardCharsets.UTF_8));
                        gatt.readCharacteristic(checkignOptionsCharacteristic);
                    } else if (characteristic.getUuid().equals(workerCheckingCharacteristicUUID)) {
                        String s = new String(characteristic.getValue(), StandardCharsets.UTF_8);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(),"CHECK IN",Toast.LENGTH_LONG).show();
                                setInitOptions();
                            }
                        });
                        gatt.close();
                    }
                } else {
                    gatt.close();
                }
            }

            @Override
            public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
                super.onReliableWriteCompleted(gatt, status);
            }
        };
}
