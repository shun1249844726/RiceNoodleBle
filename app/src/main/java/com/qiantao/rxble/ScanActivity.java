package com.qiantao.rxble;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;


import com.qiantao.rxble.ble.MTBeacon;
import com.qiantao.rxble.ble.RxBle;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.functions.Action1;
;import static com.qiantao.rxble.Tools.mRxBle;

/**
 * Created by xushun on 2017/1/7.
 */

public class ScanActivity extends AppCompatActivity {
    private static final int REQUEST_CODE = 1;
    @BindView(R.id.ble_listview)
    ListView bleListview;



    private static final String TAG = ScanActivity.class.getSimpleName();

//    private RxBle mRxBle;

    private StringBuffer mStringBuffer;
    private ProgressDialog pd;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        ButterKnife.bind(this);
        initView();
        System.out.println("oncreat");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                requestMultiplePermissions();
            }
        }

    }
    @Override
    protected void onPause() {
        super.onPause();
        scan_flag = false;
        System.out.println("onPause");
        mRxBle.scanBleDevices(false);
        search_timer.removeMessages(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        System.out.println("onresume");
        scan_flag = true;

        mStringBuffer = new StringBuffer();
        mRxBle = mRxBle.getInstance().setTargetDevice("xushun");
        mRxBle.openBle(this);
        mRxBle.scanBleDevices(true);
        mRxBle.receiveData().subscribe(new Action1<String>() {
            @Override
            public void call(String receiveData) {

            }
        });
        mRxBle.setScanListener(new RxBle.BleScanListener() {
            @Override
            public void onBleScan(BluetoothDevice bleDevice, int rssi, byte[] scanRecord) {
                System.out.println("name:" + bleDevice.getName() + ";");

                int i = 0;
                // 检查是否是搜索过的设备，并且更新
                for (i = 0; i < scan_devices.size(); i++) {
                    if (0 == bleDevice.getAddress().compareTo(
                            scan_devices.get(i).GetDevice().getAddress())) {
                        scan_devices.get(i).ReflashInf(bleDevice, rssi, scanRecord); // 更新信息
                        return;
                    }
                }

                // 增加新设备
                scan_devices.add(new MTBeacon(bleDevice, rssi, scanRecord));

//                if (bleDevice.getName().contains("xushun")) {
//                    System.out.println("conn");
//                    mRxBle.connectDevice(bleDevice);
//                }
            }
        });

        search_timer.sendEmptyMessageDelayed(0, 500);

    }



    // 开始扫描
    private int scan_timer_select = 0;
    private boolean scan_flag = true;
    private Handler search_timer = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            search_timer.sendEmptyMessageDelayed(0, 500);
            if (!scan_flag) {
                return;
            }
            // 扫描时间调度
            switch (scan_timer_select) {
                case 1:
                    mRxBle.scanBleDevices(true);
                    System.out.println("123");
                    break;
                case 3: // 停止扫描(结算)
                    mRxBle.scanBleDevices(false);

                    for (int i = 0; i < scan_devices.size(); ) { // 防抖
                        if (scan_devices.get(i).CheckSearchcount() > 2) {
                            scan_devices.remove(i);
                        } else {
                            i++;
                        }
                    }
                    scan_devices_dis.clear(); // 显示出来
                    for (MTBeacon device : scan_devices) {
                        scan_devices_dis.add(device);
                    }
                    list_adapter.notifyDataSetChanged();

                    break;

                default:
                    break;
            }
            scan_timer_select = (scan_timer_select + 1) % 4;
        }

    };

    // 初始化控件
    private LayoutInflater mInflater;
    private ListView ble_listview;
    private List<MTBeacon> scan_devices = new ArrayList<MTBeacon>();
    private List<MTBeacon> scan_devices_dis = new ArrayList<MTBeacon>();
    private BaseAdapter list_adapter = new BaseAdapter() {
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (null == convertView) {
                convertView = mInflater.inflate(R.layout.item_device, null);
            }

            TextView device_name_txt = (TextView) convertView
                    .findViewById(R.id.device_name_txt);
            TextView device_rssi_txt = (TextView) convertView
                    .findViewById(R.id.device_rssi_txt);
            TextView device_mac_txt = (TextView) convertView
                    .findViewById(R.id.device_mac_txt);

            device_name_txt.setText(getItem(position).GetDevice().getName());
            device_mac_txt.setText("Mac: "
                    + getItem(position).GetDevice().getAddress());
            device_rssi_txt.setText("Rssi: "
                    + getItem(position).GetAveragerssi());

            return convertView;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public MTBeacon getItem(int position) {
            return scan_devices_dis.get(position);
        }

        @Override
        public int getCount() {
            return scan_devices_dis.size();
        }
    };

    private void initView() {
        mInflater = LayoutInflater.from(this);
        ble_listview = (ListView) findViewById(R.id.ble_listview);

        ble_listview.setAdapter(list_adapter);
        ble_listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                BluetoothDevice device = scan_devices.get(position).GetDevice();
                mRxBle.connectDevice(device);
                getDefaultName();

//                Intent intent = new Intent(ScanActivity.this, MainActivity.class);
//                startActivity(intent);

//                Intent intent = new Intent(ScanActivity.this,UploadActivity.class);
//                intent.putExtra("device", scan_devices_dis.get(position).GetDevice());
//                myConnection = connection;
//                setResult(RESULT_OK, intent);
//                ScanActivity.this.finish();

//                MAC = scan_devices_dis.get(position).GetDevice().getAddress();
//                System.out.println(scan_devices_dis.get(position).GetDevice().toString());
//                connect(scan_devices_dis.get(position).GetDevice());

            }
        });
    }

    private void getDefaultName() {
        // 开启一个缓冲对话框
        pd = new ProgressDialog(this);
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pd.setTitle("正在加载...");
        pd.setMessage("正在连接");
        pd.show();
        new readNameThread().start();
    }
    private class readNameThread extends Thread {
        @Override
        public void run() {
            super.run();
            Message msg = reflashDialogMessage.obtainMessage();
            Bundle b = new Bundle();
            msg.setData(b);


            try {
                //for (int i = 0; i < 10; i++) {
                while (true) {
                    for (int j = 0; j < 100; j++) {
                        System.out.println("连接状态：" + mRxBle.isConnected());

                        if (mRxBle.isConnected()) {
                            break;
                        }
                        readNameThread.sleep(100);
                    }
                    if (mRxBle.isConnected()) {
                        b.putString("msg", "连接成功");
                        reflashDialogMessage.sendMessage(msg);
                        break;
                    }
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            dis_services_handl.sendEmptyMessage(0);

            Intent intent = new Intent(ScanActivity.this, MainActivity.class);
            startActivity(intent);

        }
    }

    private Handler dis_services_handl = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            list_adapter.notifyDataSetChanged();
            pd.dismiss();
        }
    };
    private Handler reflashDialogMessage = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle b = msg.getData();
            pd.setMessage(b.getString("msg"));
        }
    };
    private void requestMultiplePermissions() {
        String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            int grantResult = grantResults[0];
            boolean granted = grantResult == PackageManager.PERMISSION_GRANTED;
            Log.i("TAG", "onRequestPermissionsResult granted=" + granted);
        }
    }

}
