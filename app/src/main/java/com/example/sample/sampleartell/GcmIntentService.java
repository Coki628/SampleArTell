package com.example.sample.sampleartell;

import android.app.IntentService;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GcmIntentService extends IntentService {
    public static final int NOTIFICATION_ID = 1;
    private Context context;
    private PowerManager.WakeLock wakelock;
    private Handler handler;

    public GcmIntentService(String name) {
        super(name);
    }

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        context = getApplicationContext();
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        String messageType = gcm.getMessageType(intent);

        if(!extras.isEmpty()) {        //受け取ったBundleが空じゃなければ処理する

            Log.d("LOG", "messageType(message): " + messageType + ",body:" + extras.toString());

            String full_msg = extras.getString("message");
            String tag = extras.getString("tag");

            //スリープ状態から復帰する(特にAPI21以上はDialog出さないから、ここでwakelockやって明るくする)
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

            //キーロック時かつAPI20以前ならDialog表示
            KeyguardManager keyguard = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if(keyguard.inKeyguardRestrictedInputMode() && Build.VERSION.SDK_INT <= 20){
                //受信時にDialogを呼ぶIntent
                Intent dialog_intent = new Intent(context, AlertDialogActivity.class);     //このIntentでDialogActivity呼ぶ
                dialog_intent.putExtra("GIStoADA", full_msg);                             //通知Dialogで使うメッセージ全文
                Log.d("A",full_msg);
                dialog_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK                    //スタックに残っていても新しくタスクを起動させる
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP);          //呼び出すActivityより後で起動したActivityを全部クリアして起動させる
                context.startActivity(dialog_intent);
            }

            //メッセージの表示する文字数を調整
            String message;
            int first_sep = full_msg.indexOf(System.getProperty("line.separator"));//最初の改行場所を取得
            if (first_sep != -1 && first_sep < 20) {              //改行が20文字目以内にあったら
                message = full_msg.substring(0, first_sep);   //そこから削って
                message += " …";                                //…ってやる
            } else if (full_msg.length() > 20) {             //改行なければ20文字まで表示
                message = full_msg.substring(0, 20);          //21文字以上ある時は削って
                message += " …";                                //…ってやる
            } else {
                message = full_msg;                           //20文字以内ならそのまま表示
            }
            //メッセージのDB格納準備
            Date date = new Date();                                                             //現在の時刻を取得
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy'/'MM'/'dd' 'HH':'mm':'ss", Locale.JAPAN);  //表示形式を設定
            String time = sdf.format(date);
            SQLiteDatabase db;
            MyOpenHelper helper = new MyOpenHelper(this);
            db = helper.getWritableDatabase();
            //重複がないかの確認
            try {   //初回(DBが空っぽ)だと例外出すからtry/catchしとく
                Cursor c = db.rawQuery("SELECT * FROM notification ORDER BY _id DESC;", null);
                c.moveToFirst();
                String last_msg = c.getString(c.getColumnIndex("message"));     //先頭行(直前)のメッセージを取得
                c.close();
                if(last_msg.equals(full_msg)) {
                    db.close();
                    return;                 //同じメッセージならここで処理終わり
                }
            } catch(CursorIndexOutOfBoundsException e) {
                //何もしない
            }
            //問題なければDBに追記
            db.execSQL("INSERT INTO notification( read_flag, message, recv_time ) " +
                    "VALUES( 0, '" + full_msg + "', '" + time + "' );");
            db.close();

            sendNotification(message, tag);

            //MainActivityが実行中ならリロード
            if(MyApplication.isActivityVisible()) {
                Intent reload_intent = new Intent(getApplicationContext(), MainActivity.class);
                reload_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);  //スタックに残っていても新しくタスクを起動させる
                startActivity(reload_intent);
            }
        }
        //Wakelockの解除処理
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    //通知領域にメッセージを通知する
    private void sendNotification(String message, String tag) {

        //通知をタップした時に発行されるIntent
        Intent intent = new Intent(context, MainActivity.class);        //このIntentはタップ時にMainActivityを呼ぶ
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);    //呼び出すActivityより後で起動したActivityを全部クリアして起動させる
        PendingIntent pi = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        //第1引数が起動元のcontextで第3引数が起動先のActivityを呼ぶintent

        String appName = getResources().getString(R.string.app_name);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentTitle(appName)
                .setContentText(message)                  //通知バーに出る方は短縮メッセージ
                .setTicker(appName + ": " + message)     //ティッカーも
                .setSound(defaultSoundUri)
                .setVibrate(new long[] { 0, 200, 200, 200 }) //開始時間、振動、休止、振動～
                .setLights(Color.GREEN, 1000, 1000)
//                .setDefaults(Notification.DEFAULT_ALL)    //音、バイブ、LEDをデフォルトに
                .setContentIntent(pi)
                .setAutoCancel(true); //タップするとキャンセル(消える);

        if(Build.VERSION.SDK_INT <= 20) {    //APIレベル20以前の機種の場合の処理
            builder.setSmallIcon(R.mipmap.ar3);

        } else {                        //APIレベル21(5.0(Lollipop))以降の機種の場合の処理
            builder.setSmallIcon(R.mipmap.ar3a)                     //透過png
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ar3))
                    .setCategory(Notification.CATEGORY_SERVICE)     //バックグラウンドで動く
                    .setPriority(Notification.PRIORITY_HIGH)        //ティッカーみたいの出す
                    .setVisibility(Notification.VISIBILITY_PUBLIC);  //ロック画面でも表示
        }

        NotificationManager manager = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
        manager.notify(tag, NOTIFICATION_ID, builder.build());
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
