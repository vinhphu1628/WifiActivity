package com.example.xfoodz.wifiactivity;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity implements View.OnClickListener {
    private Handler mWifi = new Handler();
    private Handler mTime = new Handler();
    private Handler mScan = new Handler();
    private int level = 0;
    private Context context = this;
    private ImageView mImageView;
    private TextView date;
    private TextView time;
    private TextView wifiState;

    private int idleCount = 0;
    private Handler mIdle = new Handler();
    private boolean idle = false;

    private boolean standby = false;

    private WifiManager wifiManager;
    private WifiInfo wifiInfo;
    private ListView listWifi;
    private Button buttonFlush;
    private Button buttonEnable;
    private ImageView buttonBack;
    private int size = 0;
    private List<ScanResult> results;

    private ArrayList<String> arraylist = new ArrayList<>();

    private boolean check = false;
    private TimerTask connecting_;
    private Timer timer;
    private int i = 0;
    private int curID = 0;

    private WifiAdapter adapter;

    private BroadcastReceiver rv;
    private ArrayList<String> signal = new ArrayList<>();

    private boolean isDialogShow = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        mTime.post(mTimeRunnable);

        mImageView = findViewById(R.id.imageWifiState);
        wifiState = findViewById(R.id.wifiState);

        buttonFlush = findViewById(R.id.buttonFlush);
        buttonFlush.setOnClickListener(this);
        buttonEnable = findViewById(R.id.buttonEnable);
        buttonEnable.setOnClickListener(this);
        buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(this);

        listWifi = findViewById(R.id.listWifi);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            buttonEnable.setText("Enable");
        } else {
            buttonEnable.setText("Disable");
        }
//        if (!wifiManager.isWifiEnabled()) {
//            wifiManager.setWifiEnabled(true);
//        }

        results = wifiManager.getScanResults();
        adapter = new WifiAdapter(this, arraylist, signal);
        listWifi.setAdapter(adapter);

        mScan.post(mScanRunnable);

        rv = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                results = wifiManager.getScanResults();
                size = results.size();
                unregisterReceiver(rv);
            }
        };
        registerReceiver(rv, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();

        listWifi.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (wifiManager.isWifiEnabled()) {
                    boolean log = true;
                    List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
                    for (WifiConfiguration j : list) {
                        if (j.SSID.equals("\"" + arraylist.get(i) + "\"")) {
                            log = false;
                            option(j);
                            break;
                        }
                    }

                    if (log) {
                        if (signal.get(i).contains("LOCK")){
                            login(arraylist.get(i));
                        }
                        else {
                            for (WifiConfiguration j : list) {
                                wifiManager.disableNetwork(j.networkId);
                            }
                            WifiConfiguration wifiConfig = new WifiConfiguration();
                            String SSID = arraylist.get(i);
                            wifiConfig.SSID = String.format("\"%s\"", SSID);
                            int netId = wifiManager.addNetwork(wifiConfig);
                            curID = netId;
                            wifiManager.disconnect();
                            wifiManager.enableNetwork(netId, true);
                            wifiManager.reconnect();

                            check = true;
                        }
                    }
                }
                else Toast.makeText(context, "Wifi disabled!", Toast.LENGTH_SHORT).show();
            }
        });

        mWifi.post(mWifiRunnable);
        mIdle.post(mIdleRunnable);
        Toast.makeText(this, "Scanning...", Toast.LENGTH_LONG).show();
    }

    public void onClick(View view) {
        if (view == buttonFlush) {
            final Dialog flushDialog = new Dialog(this);
            flushDialog.setContentView(R.layout.wifi_flush);
            flushDialog.setCancelable(true);
            final Button agree = flushDialog.findViewById(R.id.buttonAgree);
            Button back = flushDialog.findViewById(R.id.buttonBack);
            EditText key = flushDialog.findViewById(R.id.txtAgree);
            agree.setEnabled(false);

            flushDialog.setTitle("Warning");
            flushDialog.show();
            isDialogShow = true;

            key.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    if(String.valueOf(charSequence).equals("123")) agree.setEnabled(true);
                    else agree.setEnabled(false);
                }

                @Override
                public void afterTextChanged(Editable editable) {

                }
            });

            agree.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
                    for (WifiConfiguration i : list) {
                        wifiManager.removeNetwork(i.networkId);
                    }
                    Toast.makeText(context, "Successfully flushed all configurations!", Toast.LENGTH_SHORT).show();
                    flushDialog.cancel();
                    isDialogShow = false;
                }
            });

            back.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    flushDialog.cancel();
                    isDialogShow = false;
                }
            });

        } else if (view == buttonEnable) {
            if (!wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(true);
                buttonEnable.setText("Disable");
                wifiState.setText("Enabled");
            } else {
                wifiManager.setWifiEnabled(false);
                buttonEnable.setText("Enable");
                wifiState.setText("Disabled");
            }
        } else if (view == buttonBack) {
            Intent LaunchIntent = context.getPackageManager().getLaunchIntentForPackage("com.example.xfoodz.home");
            if(LaunchIntent != null) {
                context.startActivity(LaunchIntent);
                finish();
                System.exit(0);
            }
        }
    }

    private void option(final WifiConfiguration conf) {
        final String SSID = conf.SSID;
        String BSSID = conf.BSSID;
        String networkId = String.valueOf(conf.networkId);
        final Dialog myDialog = new Dialog(this);
        myDialog.setContentView(R.layout.wifi_option);
        myDialog.setCancelable(true);
        Button forget = myDialog.findViewById(R.id.buttonForget);
        Button back = myDialog.findViewById(R.id.buttonBack);
        Button connect = myDialog.findViewById(R.id.buttonConnect);

        TextView txtSSID = myDialog.findViewById(R.id.txtSSID);
        TextView txtBSSID = myDialog.findViewById(R.id.txtBSSID);
        TextView txtNetID = myDialog.findViewById(R.id.networkID);
        txtSSID.setText("SSID: " + SSID);
        txtBSSID.setText("BSSID: " + BSSID);
        txtNetID.setText("NetworkID: " + networkId);

        myDialog.setTitle("Option");
        myDialog.show();
        isDialogShow = true;

        wifiInfo = wifiManager.getConnectionInfo();
        if(wifiInfo.getNetworkId() == conf.networkId) connect.setVisibility(View.GONE);
        else connect.setVisibility(View.VISIBLE);

        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
                for (WifiConfiguration i : list) {
                    wifiManager.disableNetwork(i.networkId);
                }
                int netId = conf.networkId;
                wifiManager.disconnect();
                wifiManager.enableNetwork(netId, true);
                wifiManager.reconnect();
                myDialog.cancel();
                isDialogShow = false;
            }
        });
        forget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (wifiManager.removeNetwork(conf.networkId))
                    Toast.makeText(context, "Successfully forget network!", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(context, "Failed to forget network!", Toast.LENGTH_SHORT).show();
                myDialog.cancel();
                isDialogShow = false;
            }
        });
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myDialog.cancel();
                isDialogShow = false;
            }
        });
    }

    private void login(final String SSID) {
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiInfo = wifiManager.getConnectionInfo();
        final Dialog myDialog = new Dialog(this);
        myDialog.setContentView(R.layout.wifi_login);
        myDialog.setCancelable(true);
        Button login = myDialog.findViewById(R.id.buttonLogin);
        Button back = myDialog.findViewById(R.id.buttonBack);

        myDialog.setTitle(SSID);
        myDialog.show();
        isDialogShow = true;

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText txtKey = myDialog.findViewById(R.id.txtKey);
                Log.d("ssid", SSID);
                Log.d("key", txtKey.getText().toString());

                List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
                for (WifiConfiguration i : list) {
                    wifiManager.disableNetwork(i.networkId);
                }

                WifiConfiguration wifiConfig = new WifiConfiguration();
                wifiConfig.SSID = String.format("\"%s\"", SSID);
                wifiConfig.preSharedKey = String.format("\"%s\"", txtKey.getText().toString());

                int netId = wifiManager.addNetwork(wifiConfig);
                curID = netId;
                wifiManager.disconnect();
                wifiManager.enableNetwork(netId, true);
                wifiManager.reconnect();

                check = true;
                myDialog.cancel();
                isDialogShow = false;
            }
        });
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myDialog.cancel();
                isDialogShow = false;
                return;
            }
        });
    }

    private Runnable mTimeRunnable = new Runnable() {
        @Override
        public void run() {
            date = findViewById(R.id.date);
            time = findViewById(R.id.time);
            SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
            date.setText(format.format(new Date()));
            format = new SimpleDateFormat("hh:mm");
            time.setText(format.format(new Date()));
            mTime.postDelayed(mTimeRunnable, 1000);
        }
    };

    private Runnable mWifiRunnable = new Runnable() {
        @Override
        public void run() {
            if(isEthernetConnected()){
                ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                wifiState.setText(activeNetwork.getExtraInfo());
            }
            else {
                wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                int numberOfLevels = 5;
                wifiInfo = wifiManager.getConnectionInfo();
                level = WifiManager.calculateSignalLevel(wifiInfo.getRssi(), numberOfLevels);

                if (check) {
                    if (wifiInfo.getSupplicantState() == SupplicantState.DISCONNECTED) {
                        if (i > 3) {
                            wifiState.setText("No connection");
                            Toast.makeText(context, "Fail to connect!", Toast.LENGTH_SHORT).show();
                            wifiManager.removeNetwork(curID);
                            check = false;
                            i = 0;
                        } else {
                            wifiState.setText("Checking...");
                            i++;
                        }
                    } else {
                        wifiState.setText("Connecting...");
                        connecting();
                        check = false;
                    }
                } else {
                    if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
                        int ipAddress = wifiInfo.getIpAddress();
                        String ipString = Formatter.formatIpAddress(ipAddress);
                        wifiState.setText(wifiInfo.getSSID() + " - " + ipString);
                        check = false;
                    } else wifiState.setText("No connection");
                }
            }
            if(!wifiManager.isWifiEnabled()) level = -1;
            if(isEthernetConnected()) level = -2;
            switch(level){
                case -2:
                    mImageView.setImageResource(R.drawable.ic_computer_black_24dp);
                    break;
                case -1:
                    mImageView.setImageResource(R.drawable.ic_signal_wifi_off_bar_black_48dp);
                    break;
                case 0:
                    wifiManager.reconnect();
                    mImageView.setImageResource(R.drawable.ic_signal_wifi_0_bar_black_48dp);
                    break;
                case 1:
                    mImageView.setImageResource(R.drawable.ic_signal_wifi_1_bar_black_48dp);
                    break;
                case 2:
                    mImageView.setImageResource(R.drawable.ic_signal_wifi_2_bar_black_48dp);
                    break;
                case 3:
                    mImageView.setImageResource(R.drawable.ic_signal_wifi_3_bar_black_48dp);
                    break;
                case 4:
                    mImageView.setImageResource(R.drawable.ic_signal_wifi_4_bar_black_48dp);
                    break;
                default:
                    mImageView.setImageResource(0);
            }
            mWifi.postDelayed(mWifiRunnable, 1000);
        }
    };

    public void connectTimerTask() {
        connecting_ = new TimerTask() {
            @Override
            public void run() {
                wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
                    connecting_.cancel();
                    stopTimer();

                } else {
                    wifiManager.reconnect();
                }
            }
        };
    }

    public void stopTimer(){
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    void connecting(){
        Timer timer = new Timer();
        connectTimerTask();
        timer.scheduleAtFixedRate(connecting_, 10000, 1000);
    }

    private Runnable mScanRunnable = new Runnable() {
        @Override
        public void run() {
        if(isDialogShow || check) mScan.postDelayed(mScanRunnable, 500);
        else {
            if (!wifiManager.isWifiEnabled()) {
                arraylist.clear();
                mScan.postDelayed(mScanRunnable, 500);
            } else {
                arraylist.clear();
                signal.clear();
                wifiManager.startScan();
                registerReceiver(rv, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                int i = 0;
                while (i < size) {
                    String item = results.get(i).SSID;
                    String s;
                    String capabilities = results.get(i).capabilities;

                    int level = WifiManager.calculateSignalLevel(results.get(i).level, 5);
                    s = String.valueOf(level);

                    if (capabilities.contains("PSK")) {
                        s = s + "LOCK";
                    } else if (capabilities.contains("WEP")) {
                        s = s + "LOCK";
                    } else if (capabilities.contains("EAP")) {
                        s = s + "LOCK";
                    }
                    signal.add(s);
                    arraylist.add(item);
                    adapter.notifyDataSetChanged();
                    i++;
                }
                mScan.postDelayed(mScanRunnable, 5000);
            }
        }
        }
    };

    private Runnable mIdleRunnable = new Runnable() {
        @Override
        public void run() {
            if(idleCount == 30) {
                idle = true;
                idleCount = 0;
                Intent LaunchIntent = context.getPackageManager().getLaunchIntentForPackage("com.example.xfoodz.home");
                if(LaunchIntent != null) {
                    context.startActivity(LaunchIntent);
                    finish();
                    System.exit(0);
                }
            }
            else {
                if(isDialogShow == true || check == true) idleCount = 0;
                else idleCount++;
            }
            if(!idle) mIdle.postDelayed(mIdleRunnable, 1000);
        }
    };

    private Boolean isNetworkAvailable() {
        ConnectivityManager cm
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    public Boolean isEthernetConnected(){
        if(isNetworkAvailable()){
            ConnectivityManager cm
                    = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            return (cm.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_ETHERNET);
        }
        return false;
    }

}