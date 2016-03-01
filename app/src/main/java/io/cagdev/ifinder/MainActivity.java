package io.cagdev.ifinder;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "iFinder";
    private BluetoothAdapter mBluetoothAdapter;
    private static final long SCAN_PERIOD = 120000;
    private Handler mHandler;
    private boolean mScanning;
    private BluetoothGatt mGatt;

    public static final UUID UUID_SERVICE_MILI = UUID.fromString("0000fee0-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHAR_BATTERY = UUID.fromString("0000ff0c-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHAR_VIBRATION = UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHAR_HEARTRATE = UUID.fromString("00002a39-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_SERVICE_VIBRATION = UUID.fromString("00001802-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_SERVICE_HEARTRATE = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_NOTIFICATION_HEARTRATE = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_DESCRIPTOR_UPDATE_NOTIFICATION = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

       /* FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/

        mHandler = new Handler();
        BlueToothDirect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();
/*
                    BluetoothGattService bserive = mGatt.getService(UUID_SERVICE_MILI);
                    if (bserive != null) {
                        Log.i(TAG, "BluetoothGattCharacteristic ");
                    }*/


                    /*BluetoothGattCharacteristic chara = gatt.getService(UUID_SERVICE_MILI).getCharacteristic(UUID_CHAR_BATTERY);
                    if (chara != null) {
                        Log.i(TAG, "BluetoothGattCharacteristic ");
                    }*/

                   /* byte[] VIBRATION_WITH_LED = {1};
                    byte[] ENABLE_REALTIME_STEPS_NOTIFY = {3, 1};

                    BluetoothGattCharacteristic chara = gatt.getService(UUID.fromString("0000fee0-0000-1000-8000-00805f9b34fb")).getCharacteristic(UUID.fromString("0000ff05-0000-1000-8000-00805f9b34fb"));
                    if (chara != null) {
                        Log.i("getService", "ENABLE_REALTIME_STEPS_NOTIFY");
                        chara.setValue(ENABLE_REALTIME_STEPS_NOTIFY);
                    }*/
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }
        }
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();
            Log.i("onServicesDiscovered", services.toString());

            final BluetoothGattService gattService = gatt
                    .getService(UUID_SERVICE_HEARTRATE);
            if (gattService != null) {
                Log.i(TAG, "onServicesDiscovered UUID_SERVICE_HEARTRATE");
                BluetoothGattCharacteristic bch = gattService.getCharacteristic(UUID_NOTIFICATION_HEARTRATE);
                if (bch != null) {
                    Log.i(TAG, "BluetoothGattCharacteristic UUID_NOTIFICATION_HEARTRATE");
                   // gatt.readCharacteristic(bch);
                    byte[] VIBRATION_WITH_LED = {1};
                    byte[] START_HEART_RATE_SCAN = {21, 2, 1};

                    gatt.setCharacteristicNotification(bch, true);

                    BluetoothGattDescriptor descriptor = bch.getDescriptor(UUID_DESCRIPTOR_UPDATE_NOTIFICATION);
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

                    //bch.setValue(START_HEART_RATE_SCAN);
                    //gatt.writeCharacteristic(bch);

                    gatt.writeDescriptor(descriptor);

                    Log.i(TAG, "BluetoothGattCharacteristic writeDescriptor OK");

                }

               /* BluetoothGattCharacteristic bch2 = gattService.getCharacteristic(UUID_CHAR_HEARTRATE);
                if (bch2 != null) {
                    Log.i(TAG, "BluetoothGattCharacteristic UUID_CHAR_HEARTRATE");
                    byte[] START_HEART_RATE_SCAN = {21, 2, 1};
                    bch2.setValue(START_HEART_RATE_SCAN);
                    gatt.writeCharacteristic(bch2);
                }*/

            }

           /* gatt.readCharacteristic(services.get(1).getCharacteristics().get
                    (0));*/
        }



        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, int status) {
            Log.i("onCharacteristicRead", characteristic.toString());

            //Log.d(TAG, "getBatteryInfo result " + Arrays.toString(characteristic.getValue()));

            //gatt.disconnect();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.i("onCharacteristicChanged", characteristic.toString());

            byte[] data = characteristic.getValue();

            if (data.length == 2 && data[0] == 6) {
                int heartRate = data[1] & 0xFF;

                Log.d(TAG, "heartRate : " + heartRate);
            }


        }
    };



    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    Log.i(TAG, "rssi : " + rssi + " , " + device.toString());

                   // mScanning = false;
                    //mBluetoothAdapter.stopLeScan(mLeScanCallback);

                    //mGatt = device.connectGatt(getApplicationContext(), false, gattCallback);
                        /*runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mLeDeviceListAdapter.addDevice(device);
                                mLeDeviceListAdapter.notifyDataSetChanged();
                            }
                        });*/
                }};

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);

                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }

    }

    private void connectToDevice( String deviceAddress) {

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddress);

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {

                if (device != null) {
                    mGatt = device.connectGatt(getApplicationContext(), true, gattCallback);
                    scanLeDevice(false);// will stop after first device detection
                }
            }
        });
    }

    private void BlueToothDirect() {

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();


        //C8:0F:10:11:FE:24 -> new
       /* List<BluetoothDevice> devices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
        for(BluetoothDevice device : devices) {
            if(device.getType() == BluetoothDevice.DEVICE_TYPE_LE) {
                Log.i(TAG, "device : " + device.toString());
            }
        }*/





        connectToDevice("C8:0F:10:11:FE:24");
        //mBluetoothAdapter.startLeScan(mLeScanCallback);
        //scanLeDevice(true);
    }
}
