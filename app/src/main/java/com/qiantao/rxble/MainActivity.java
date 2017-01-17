package com.qiantao.rxble;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.qiantao.rxble.ble.RxBle;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import rx.functions.Action1;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.tv_receive)
    TextView sendTv;

    @BindView(R.id.et_send)
    EditText sendEt;

    private String mMsgSend;

    private static final String TAG = MainActivity.class.getSimpleName();

    private RxBle mRxBle;

    private StringBuffer mStringBuffer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        mStringBuffer = new StringBuffer();
        mRxBle = RxBle.getInstance().setTargetDevice("Test");//为名字
        mRxBle.openBle(this);
        mRxBle.scanBleDevices(true);
        mRxBle.receiveData().subscribe(new Action1<String>() {
            @Override
            public void call(String receiveData) {
                sendTv.setText(mStringBuffer.append(receiveData).append("\n"));
            }
        });
        mRxBle.setScanListener(new RxBle.BleScanListener() {
            @Override
            public void onBleScan(BluetoothDevice bleDevice, int rssi, byte[] scanRecord) {
                // Get list of devices and other information
            }
        });
    }

    @OnClick(R.id.btn_send)
    public void sendMessage(View view) {
        if (!TextUtils.isEmpty(mMsgSend)) {
            Log.d(TAG, "sendMessage: " + mMsgSend);
            mRxBle.sendData(mMsgSend, 0);
        }
    }

    @OnTextChanged(R.id.et_send)
    public void onTextChanged(CharSequence text) {
        mMsgSend = text.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRxBle.closeBle();
    }
}
