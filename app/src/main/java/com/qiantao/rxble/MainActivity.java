package com.qiantao.rxble;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.qiantao.rxble.Tools.mRxBle;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.tv_receive)
    TextView sendTv;

    @BindView(R.id.et_send)
    EditText sendEt;
    @BindView(R.id.amount_view_mode1)
    AmountView amountViewMode_A;
    @BindView(R.id.amount_view_mode2)
    AmountView amountViewMode_B;
    @BindView(R.id.tv_log)
    TextView logTv;

    private static int aModeNum = 0;
    private static int bModeNum = 0;

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        if (mRxBle.isConnected()){
            logTv.setText("连接正常！  ");
        }
        else {
            logTv.setText("未连接设备！");
        }

        preferences = getSharedPreferences("MODENUMINFO", MODE_PRIVATE);
        editor = preferences.edit();

        aModeNum = preferences.getInt("AMODENUM",0);
        bModeNum = preferences.getInt("BMODENUM",0);

        amountViewMode_A.setGoods_storage(255);
        amountViewMode_B.setGoods_storage(255);

        amountViewMode_A.setCurentGoodsNum(aModeNum);
        amountViewMode_B.setCurentGoodsNum(bModeNum);

        amountViewMode_A.setOnAmountChangeListener(new AmountView.OnAmountChangeListener() {
            @Override
            public void onAmountChange(View view, int amount) {
                aModeNum = amount;
                System.out.println("a:   "+ view.getId() + "    "+aModeNum);
                BleSend(1,amount);
            }
        });
        amountViewMode_B.setOnAmountChangeListener(new AmountView.OnAmountChangeListener() {
            @Override
            public void onAmountChange(View view, int amount) {
                bModeNum = amount;
                System.out.println("b:   "+ view.getId() + "    "+aModeNum);

                BleSend(2,amount);
            }
        });
    }
    private void BleSend(int mode,int num) {
        if (mRxBle.isConnected()){
            switch (mode){
                case 1:
                    mRxBle.sendData("A:"+num+";");
                    break;
                case 2:
                    mRxBle.sendData("B:"+num+";");
                    break;
                default:
                    break;
            }
        }
        else {
            logTv.setText("未连接设备！");
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRxBle.closeBle();

    }

    @Override
    protected void onPause() {
        super.onPause();

        editor.clear();
        editor.putInt("AMODENUM", aModeNum);
        editor.putInt("BMODENUM", bModeNum);
        editor.commit();

        System.out.println("onPause:"+aModeNum +"     "+ bModeNum);

    }
}