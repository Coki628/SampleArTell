package com.example.sample.sampleartell;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class AlertDialogActivity extends FragmentActivity {

    private PowerManager.WakeLock wakelock;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //スリープ状態から復帰する
        wakelock = ((PowerManager) getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.FULL_WAKE_LOCK                    //CPU、Screen、KeyBoardが全部起きる
                        | PowerManager.ACQUIRE_CAUSES_WAKEUP               //WakeLock取得時にすぐに消えないように設定
                        | PowerManager.ON_AFTER_RELEASE, "disableLock");  //Release後は通常の設定時間に戻る(今回時間設定してないけど)
        wakelock.acquire();                                //↑デバッグ用のタグ
        //10秒でリリースする
        handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                wakelock.release();
            }
        }, 10000);

        //キーロック無視で表示させる処理
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        //背景は真っ黒に
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setBackgroundColor(Color.BLACK);
        setContentView(frameLayout);

        //Serviceからメッセージを受け取るIntent
        Intent intent = getIntent();
        String message = intent.getStringExtra("GIStoADA");
        Log.d("B",message);

        //受け取ったメッセージを包むBundle
        Bundle bundle = new Bundle();
        bundle.putString("ADAtoADF",message);

        AlertDialogFragment fragment = new AlertDialogFragment();   //フラグメントのインスタンス作って表示
        fragment.setArguments(bundle);                              //fragmentで使うメッセージを添付
        fragment.show(getSupportFragmentManager(), "alert_dialog");
    }
    //使い終わったらリリースする処理
    @Override
    public void onDestroy() {
        super.onDestroy();

        handler.removeCallbacksAndMessages(null);   //10秒経つ前にユーザーが触ったらhandlerをキャンセル
        try {       //既にwakelockがreleaseされてると例外出すからcatchしとく
            wakelock.release();
        } catch (Throwable th) {
            //何もしない
        }
    }
}