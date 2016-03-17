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
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private Button button1;
    private Button button2;
    private Button button3;
    private Button button4;
    private TextView textView;

    private static final String TAG = "iFinder";
    private BluetoothAdapter mBluetoothAdapter;
    private static final long SCAN_PERIOD = 120000;
    private Handler mHandler;
    private boolean mScanning;
    private BluetoothGatt mGatt = null;
    private BluetoothGattService mGattService;
    private String lastcommand ;
    private Handler messageHandler;
    private int mincount = 0;
    private Timer timer;
    static TextToSpeech tts = null;

    public static final UUID UUID_SERVICE_MILI = UUID.fromString("0000fee0-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHAR_BATTERY = UUID.fromString("0000ff0c-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHAR_VIBRATION = UUID.fromString("00002a06-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHAR_HEARTRATE = UUID.fromString("00002a39-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_SERVICE_VIBRATION = UUID.fromString("00001802-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_SERVICE_HEARTRATE = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_NOTIFICATION_HEARTRATE = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_DESCRIPTOR_UPDATE_NOTIFICATION = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public static final UUID UUID_CHAR_REALTIME_STEPS = UUID.fromString("0000ff06-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHAR_ACTIVITY_DATA = UUID.fromString("0000ff07-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHAR_CONTROL_POINT = UUID.fromString("0000ff05-0000-1000-8000-00805f9b34fb");
    public static final byte MODE_REGULAR_DATA_LEN_MINUTE = 0x1;
    public static final int BytesPerMinute = 4;
    public static final byte COMMAND_CONFIRM_ACTIVITY_DATA_TRANSFER_COMPLETE = 0xa;
    public static final byte COMMAND_FETCH_DATA = 0x6;

    private static boolean ENABLE_NOTIFICATION = false;
    private static boolean ENABLE_HEARTNOTIFICATION = false;

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.getDefault());
            //tts.setLanguage(Locale.CHINESE);


        } else {
            Log.e("TTS", "Initialization failed");
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        tts = new TextToSpeech(this, this);

        button1 = (Button)findViewById(R.id.button);
        button2 = (Button)findViewById(R.id.button2);
        button3 = (Button)findViewById(R.id.button3);
        button4 = (Button)findViewById(R.id.button4);
        textView = (TextView) findViewById(R.id.textView);
        textView.setMovementMethod(new ScrollingMovementMethod());

        messageHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.obj == null) {
                    textView.setText("");
                } else {
                    textView.append(msg.obj.toString() + "\r\n");
                }
            }
        };

        mHandler = new Handler();

        button1.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                textView.setText("");
                mincount = 0;
                lastcommand = "writeChar";
                BlueToothDirect();
            }

        });

        button2.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                textView.setText("");

                if (mGattService != null && mGatt != null) {
                    BluetoothGattCharacteristic bchnotify = mGattService.getCharacteristic(UUID_CHAR_ACTIVITY_DATA);
                    if (bchnotify != null ) {
                        mGatt.setCharacteristicNotification(bchnotify, false);
                        BluetoothGattDescriptor descriptorfalse = bchnotify.getDescriptor(UUID_DESCRIPTOR_UPDATE_NOTIFICATION);
                        descriptorfalse.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        mGatt.writeDescriptor(descriptorfalse);
                    }
                }

                mincount = 0;
                lastcommand = "readActivity";
                BlueToothDirect();
            }

        });


        button3.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                //textView.setText("");
               // mincount = 0;
                lastcommand = "testheart";
                BlueToothDirect();
            }

        });

        button4.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                textView.setText("");


              /*  mincount = 0;
              if (mGatt != null)
               mGatt.disconnect();*/
            }

        });
       /* FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/


        timer = new Timer();
        // This timer task will be executed every 1 sec.
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                lastcommand = "testheart";
                BlueToothDirect();
            }
        }, 0, 1000 * 60 * 5);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        //Close the Text to Speech Library

        if(tts != null) {
            tts.stop();
            tts.shutdown();
            Log.i(TAG, "TTS Destroyed");
        }

        super.onDestroy();
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

                    BluetoothGattCharacteristic chara = gatt.getService(UUID_SERVICE_MILI).getCharacteristic(UUID_CHAR_CONTROL_POINT);
                    if (chara != null) {
                        Log.i("getService", "ENABLE_REALTIME_STEPS_NOTIFY");
                        chara.setValue(ENABLE_REALTIME_STEPS_NOTIFY);
                    }*/
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    ENABLE_HEARTNOTIFICATION = false;
                    ENABLE_NOTIFICATION = false;
                    setText("STATE_DISCONNECTED");
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
                    .getService(UUID_SERVICE_MILI);
            if (gattService != null) {
                Log.i(TAG, "onServicesDiscovered UUID_SERVICE_MILI");

                mGattService = gattService;

               /* BluetoothGattCharacteristic readact = gattService.getCharacteristic(UUID_CHAR_ACTIVITY_DATA);
                if (readact != null) {
                    gatt.readCharacteristic(readact);
                }*/

                if (lastcommand.equals("writeChar")) {
                    BluetoothGattCharacteristic control = gattService.getCharacteristic(UUID_CHAR_CONTROL_POINT);
                    if (control != null) {
                        byte[] GET_ACT = {6};
                        control.setValue(GET_ACT);
                        gatt.writeCharacteristic(control);
                        // gatt.readCharacteristic(control);
                        return;
                    }
                }

                if (lastcommand.equals("stopsync")) {
                    BluetoothGattCharacteristic control = gattService.getCharacteristic(UUID_CHAR_CONTROL_POINT);
                    if (control != null) {
                        byte[] STOP_ACT = new byte[]{0x11};
                        control.setValue(STOP_ACT);
                        gatt.writeCharacteristic(control);
                        ENABLE_NOTIFICATION = false;

                        /*gatt.setCharacteristicNotification(control, false);
                        BluetoothGattDescriptor descriptor = control.getDescriptor(UUID_DESCRIPTOR_UPDATE_NOTIFICATION);
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);*/
                        return;
                    }
                }

                if (lastcommand.equals("testheart")) {

                    final BluetoothGattService gattServicehr = gatt
                            .getService(UUID_SERVICE_HEARTRATE);
                    if (gattServicehr != null) {
                        Log.i(TAG, "onServicesDiscovered UUID_SERVICE_HEARTRATE");

                        if (!ENABLE_HEARTNOTIFICATION) {
                            ENABLE_HEARTNOTIFICATION = true;
                            BluetoothGattCharacteristic bch = gattServicehr.getCharacteristic(UUID_NOTIFICATION_HEARTRATE);
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
                        } else {

                            BluetoothGattCharacteristic bch2 = gattServicehr.getCharacteristic(UUID_CHAR_HEARTRATE);
                            if (bch2 != null) {
                                Log.i(TAG, "BluetoothGattCharacteristic UUID_CHAR_HEARTRATE");
                                byte[] START_HEART_RATE_SCAN = {21, 2, 1};
                                bch2.setValue(START_HEART_RATE_SCAN);
                                gatt.writeCharacteristic(bch2);
                            }
                        }

                }

                    return;

            }


                //BluetoothGattCharacteristic bch = gattService.getCharacteristic(UUID_CHAR_CONTROL_POINT);
               // BluetoothGattCharacteristic bch = gattService.getCharacteristic(UUID_CHAR_REALTIME_STEPS);

                if (!ENABLE_NOTIFICATION) {
                    BluetoothGattCharacteristic bch = gattService.getCharacteristic(UUID_CHAR_ACTIVITY_DATA);
                    if (bch != null) {
                        Log.i(TAG, "BluetoothGattCharacteristic UUID_CHAR_CONTROL_POINT");


                        /*BluetoothGattCharacteristic bchnotify = gattService.getCharacteristic(UUID_CHAR_ACTIVITY_DATA);
                        if (bchnotify != null) {
                            gatt.setCharacteristicNotification(bchnotify, false);
                            BluetoothGattDescriptor descriptorfalse = bchnotify.getDescriptor(UUID_DESCRIPTOR_UPDATE_NOTIFICATION);
                            descriptorfalse.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            gatt.writeDescriptor(descriptorfalse);
                        }*/


                        ENABLE_NOTIFICATION = true;
                        byte[] VIBRATION_WITH_LED = {1};
                        byte[] START_HEART_RATE_SCAN = {21, 2, 1};
                        //byte[] SYNC = {11};
                        // byte[] GET_ACT = {18,1};
                        byte[] GET_ACT = {6};
                        byte[] fetch = new byte[]{COMMAND_FETCH_DATA};
                        byte[] ENABLE_REALTIME_STEPS_NOTIFY = {3, 1};

                        // bch.setValue(GET_ACT);
                        //  gatt.writeCharacteristic(bch);

                        // gatt.readCharacteristic(bch);


                        gatt.setCharacteristicNotification(bch, true);
                        BluetoothGattDescriptor descriptor = bch.getDescriptor(UUID_DESCRIPTOR_UPDATE_NOTIFICATION);
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);




                        // bch.setValue(ENABLE_REALTIME_STEPS_NOTIFY);
                        //gatt.readCharacteristic(bch);
                        //gatt.writeCharacteristic(bch);
                        //Log.i(TAG, "BluetoothGattCharacteristic writeCharacteristic OK");


                        //Log.i(TAG, "BluetoothGattCharacteristic writeDescriptor OK");


                    }
                }

            }
        }

           /* final BluetoothGattService gattService = gatt
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

                BluetoothGattCharacteristic bch2 = gattService.getCharacteristic(UUID_CHAR_HEARTRATE);
                if (bch2 != null) {
                    Log.i(TAG, "BluetoothGattCharacteristic UUID_CHAR_HEARTRATE");
                    byte[] START_HEART_RATE_SCAN = {21, 2, 1};
                    bch2.setValue(START_HEART_RATE_SCAN);
                    gatt.writeCharacteristic(bch2);
                }

            }

           // gatt.readCharacteristic(services.get(1).getCharacteristics().get(0));
        } */



        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, int status) {
            Log.i("onCharacteristicRead", characteristic.toString());

            byte[] data = characteristic.getValue();

            Log.i("onCharacteristicRead length", String.valueOf(data.length));

            if (data.length == 4) {
                int steps = data[3] << 24 | (data[2] & 0xFF) << 16 | (data[1] & 0xFF) << 8 | (data[0] & 0xFF);
                Log.d(TAG, "steps : " + steps);
            }

            if (data.length == 11) {
               // int steps = data[3] << 24 | (data[2] & 0xFF) << 16 | (data[1] & 0xFF) << 8 | (data[0] & 0xFF);
                //Log.d(TAG, "steps : " + steps);
                int dataType = data[0];

                GregorianCalendar timestamp = rawBytesToCalendar(data, 1);

                int totalDataToRead = (data[7] & 0xff) | ((data[8] & 0xff) << 8);
                totalDataToRead *= (dataType == MODE_REGULAR_DATA_LEN_MINUTE) ? BytesPerMinute : 1;
                Log.d(TAG, "totalDataToRead : " + totalDataToRead);

                // counter of this data block
                int dataUntilNextHeader = (data[9] & 0xff) | ((data[10] & 0xff) << 8);
                dataUntilNextHeader *= (dataType == MODE_REGULAR_DATA_LEN_MINUTE) ? BytesPerMinute : 1;
                Log.d(TAG, "counter data block : " + dataUntilNextHeader);

                Log.d(TAG,"total data to read: " + totalDataToRead + " len: " + (totalDataToRead / BytesPerMinute) + " minute(s)");

                Log.d(TAG,"data to read until next header: " + dataUntilNextHeader + " len: " + String.valueOf((dataUntilNextHeader / BytesPerMinute)) + " minute(s)");
                Log.d(TAG, "TIMESTAMP: " + DateFormat.getDateTimeInstance().format(timestamp.getTime()) + " magic byte: " + dataUntilNextHeader);

                if (dataUntilNextHeader == 0) {
                    mincount = 0;

                    BluetoothGattCharacteristic control = mGattService.getCharacteristic(UUID_CHAR_CONTROL_POINT);
                    if (control != null) {
                        byte[] STOP_ACT = new byte[]{0x11};
                        control.setValue(STOP_ACT);
                        gatt.writeCharacteristic(control);
                        ENABLE_NOTIFICATION = false;

                        /*BluetoothGattCharacteristic bchnotify = mGattService.getCharacteristic(UUID_CHAR_ACTIVITY_DATA);
                        if (bchnotify != null) {
                            gatt.setCharacteristicNotification(bchnotify, false);
                            BluetoothGattDescriptor descriptorfalse = bchnotify.getDescriptor(UUID_DESCRIPTOR_UPDATE_NOTIFICATION);
                            descriptorfalse.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            gatt.writeDescriptor(descriptorfalse);
                        }*/

                        Log.d(TAG, "magic bytes Zero Stop Notification");
                    }

                }


                byte[] ackTime = calendarToRawBytes(timestamp);

                /*byte[] ackChecksum = new byte[]{
                        (byte) (dataUntilNextHeader & 0xff),
                        (byte) (0xff & (dataUntilNextHeader >> 8))
                };*/

                byte[] ackChecksum = new byte[]{
                        (byte) (~dataUntilNextHeader & 0xff),
                        (byte) (0xff & (~dataUntilNextHeader >> 8))
                };

                byte[] ack = new byte[]{
                        COMMAND_CONFIRM_ACTIVITY_DATA_TRANSFER_COMPLETE,
                        ackTime[0],
                        ackTime[1],
                        ackTime[2],
                        ackTime[3],
                        ackTime[4],
                        ackTime[5],
                        ackChecksum[0],
                        ackChecksum[1]
                };




              // BluetoothGattCharacteristic bch = mGattService.getCharacteristic(UUID_CHAR_ACTIVITY_DATA);
              // bch.setValue(ack);
              // gatt.readCharacteristic(bch);
                /*if (mGattService != null) {
                    Log.i(TAG, "onServicesDiscovered UUID_SERVICE_MILI");

                    BluetoothGattCharacteristic bch = mGattService.getCharacteristic(UUID_CHAR_CONTROL_POINT);
                    if (bch != null) {
                        byte[] GET_ACT = {6};
                        bch.setValue(GET_ACT);
                        Log.i(TAG, "BluetoothGattCharacteristic UUID_CHAR_CONTROL_POINT");
                        //gatt.readCharacteristic(bch);
                        gatt.writeCharacteristic(bch);
                    }
                }*/


            }

            //Log.d(TAG, "getBatteryInfo result " + Arrays.toString(characteristic.getValue()));

            //gatt.disconnect();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.i("onCharacteristicChanged", characteristic.toString());

            byte[] data = characteristic.getValue();

            /*if (data.length == 4) {
                int steps = data[3] << 24 | (data[2] & 0xFF) << 16 | (data[1] & 0xFF) << 8 | (data[0] & 0xFF);
                Log.d(TAG, "steps : " + steps);
            }

            if (data.length == 2 && data[0] == 6) {
                int heartRate = data[1] & 0xFF;

                Log.d(TAG, "heartRate : " + heartRate);
            }*/

            if (data.length == 2 && data[0] == 6) {
                int heartRate = data[1] & 0xFF;

                Format formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String s = formatter.format(new Date());

                String outtext = s + " heartRate : " + heartRate;
                setText(outtext);
                Log.d(TAG, outtext);


                tts.speak("心跳 " + String.valueOf(heartRate), TextToSpeech.QUEUE_FLUSH, null);
                //ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
                //toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 220);

                /*if (mGattService != null && mGatt != null) {
                    BluetoothGattCharacteristic bchnotify = mGattService.getCharacteristic(UUID_NOTIFICATION_HEARTRATE);
                    if (bchnotify != null ) {
                        ENABLE_HEARTNOTIFICATION = false;
                        mGatt.setCharacteristicNotification(bchnotify, false);
                        BluetoothGattDescriptor descriptorfalse = bchnotify.getDescriptor(UUID_DESCRIPTOR_UPDATE_NOTIFICATION);
                        descriptorfalse.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                        mGatt.writeDescriptor(descriptorfalse);
                    }
                }*/


                mGatt.disconnect();

                return;
            }

            if (data.length == 11 ) {
                Log.d(TAG, "activity data meta  ");

                int dataType = data[0];

                GregorianCalendar timestamp = rawBytesToCalendar(data, 1);

                int totalDataToRead = (data[7] & 0xff) | ((data[8] & 0xff) << 8);
                totalDataToRead *= (dataType == MODE_REGULAR_DATA_LEN_MINUTE) ? BytesPerMinute : 1;
                Log.d(TAG, "totalDataToRead : " + totalDataToRead);

                // counter of this data block
                int dataUntilNextHeader = (data[9] & 0xff) | ((data[10] & 0xff) << 8);
                dataUntilNextHeader *= (dataType == MODE_REGULAR_DATA_LEN_MINUTE) ? BytesPerMinute : 1;
                Log.d(TAG, "counter data block : " + dataUntilNextHeader);

                Log.d(TAG,"total data to read: " + totalDataToRead + " len: " + (totalDataToRead / BytesPerMinute) + " minute(s)");

                Log.d(TAG, "data to read until next header: " + dataUntilNextHeader + " len: " + String.valueOf((dataUntilNextHeader / BytesPerMinute)) + " minute(s)");

                String outtext = "TIMESTAMP: " + DateFormat.getDateTimeInstance().format(timestamp.getTime()) + " magic byte: " + dataUntilNextHeader;
                setText(outtext);
                Log.d(TAG, outtext);

                if (dataUntilNextHeader == 0) {
                    mincount = 0;

                    BluetoothGattCharacteristic control = mGattService.getCharacteristic(UUID_CHAR_CONTROL_POINT);
                    if (control != null) {
                        byte[] STOP_ACT = new byte[]{0x11};
                        control.setValue(STOP_ACT);
                        gatt.writeCharacteristic(control);
                        ENABLE_NOTIFICATION = false;

                        Log.d(TAG, "magic bytes Zero Stop Notification");
                    }

                }


            } else  {
                Log.d(TAG, "activity data raw  ");

                for (int i = 0; i < data.length; i += BytesPerMinute) {
                    mincount ++;
                    String outtext = "[" + mincount + "] category:" + data[i] + ",intensity:" + (data[i + 1]& 0xff) + ",steps:" + (data[i + 2]& 0xff) + ",heartrate:" + (data[i + 3] & 0xff);
                    setText(outtext);
                    Log.d(TAG,outtext );
                }
            }




        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.i("onDescriptorWrite", descriptor.toString());

            setText("onDescriptorWrite:" + descriptor.toString());
          /*  BluetoothGattCharacteristic bch = mGattService.getCharacteristic(UUID_CHAR_ACTIVITY_DATA);
            if (bch != null) {
                Log.i(TAG, "BluetoothGattCharacteristic UUID_CHAR_CONTROL_POINT");

                gatt.setCharacteristicNotification(bch, true);
                BluetoothGattDescriptor descriptornoti = bch.getDescriptor(UUID_DESCRIPTOR_UPDATE_NOTIFICATION);
                descriptornoti.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptornoti);

            }*/

            if (!lastcommand.equals("testheart")) {
                BluetoothGattCharacteristic bchg = mGattService.getCharacteristic(UUID_CHAR_CONTROL_POINT);
                if (bchg != null) {
                    byte[] GET_ACT = {6};
                    bchg.setValue(GET_ACT);
                    gatt.writeCharacteristic(bchg);
                }
            } else {

                final BluetoothGattService gattServicehr = gatt
                        .getService(UUID_SERVICE_HEARTRATE);
                if (gattServicehr != null) {
                    BluetoothGattCharacteristic bch2 = gattServicehr.getCharacteristic(UUID_CHAR_HEARTRATE);
                    if (bch2 != null) {
                        Log.i(TAG, "BluetoothGattCharacteristic UUID_CHAR_HEARTRATE");
                        byte[] START_HEART_RATE_SCAN = {21, 2, 1};
                        bch2.setValue(START_HEART_RATE_SCAN);
                        gatt.writeCharacteristic(bch2);
                    }
                }

            }
        }
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i("onCharacteristicWrite", characteristic.toString());

            setText("onCharacteristicWrite:" + characteristic.toString());

           // BluetoothGattCharacteristic bch = mGattService.getCharacteristic(UUID_CHAR_ACTIVITY_DATA);

            //gatt.readCharacteristic(bch);

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
                   // scanLeDevice(false);// will stop after first device detection
                }
            }
        });
    }

    private GregorianCalendar rawBytesToCalendar(byte[] value, int offset) {
        if (value.length - offset >= 6) {
            GregorianCalendar timestamp = new GregorianCalendar(
                    value[offset] + 2000,
                    value[offset + 1],
                    value[offset + 2],
                    value[offset + 3],
                    value[offset + 4],
                    value[offset + 5]);

            return timestamp;
        }

        return new GregorianCalendar();
    }

    private byte[] calendarToRawBytes(Calendar timestamp) {
        return new byte[]{
                (byte) (timestamp.get(Calendar.YEAR) - 2000),
                (byte) timestamp.get(Calendar.MONTH),
                (byte) timestamp.get(Calendar.DATE),
                (byte) timestamp.get(Calendar.HOUR_OF_DAY),
                (byte) timestamp.get(Calendar.MINUTE),
                (byte) timestamp.get(Calendar.SECOND)
        };
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

    private void setText(String text) {
        Message msg = Message.obtain(messageHandler);
        msg.obj = text;
        messageHandler.sendMessage(msg);
    }
}
