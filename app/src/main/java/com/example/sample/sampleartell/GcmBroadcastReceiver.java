package com.example.sample.sampleartell;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.content.WakefulBroadcastReceiver;

public class GcmBroadcastReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        //新着メッセージを受信したらリストを初期位置にする
        MainActivity.FVP = 0;
        MainActivity.y = 0;

        //受け取ったインテントの処理をGcmIntentServiceで行う
        ComponentName comp = new ComponentName(context.getPackageName(),
                GcmIntentService.class.getName());
        //サービスの起動。処理中スリープを制御
        startWakefulService(context, (intent.setComponent(comp)));
        setResultCode(Activity.RESULT_OK);
    }
}
