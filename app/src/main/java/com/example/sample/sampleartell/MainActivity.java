package com.example.sample.sampleartell;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.squareup.okhttp.Authenticator;
import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends Activity implements OnItemClickListener, OnClickListener {

    //登録の送受信関係
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String SENDER_ID = "870639644421";                //デベロッパーコンソールで取得したセンダーID
    private GoogleCloudMessaging gcm;
    private String registrationId;
    private Context context;

    //DBとListView関係
    private SQLiteDatabase db;
    private ArrayList<HashMap<String, String>> list = new ArrayList<>();        //連想配列を入れる配列
    private ListView listview;
    private MyAdapter myadapter;
    private String[] full_msg;
    private String[] time;
    private String[] read;
    public static int FVP = 0;
    public static int y = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //登録処理
        context = getApplicationContext();
        if (checkPlayServices()) {                  //端末のGooglePlayServiceAPKの有無をチェック
            gcm = GoogleCloudMessaging.getInstance(context);
            registrationId = getRegistrationId(context);    //Preferenceから登録IDを取得

        } else {
            Log.i("TAG", "Google Play Services APKが見つかりません");
        }
        if ("".equals(registrationId)) {         //登録IDがまだなかったら登録する
            new AlertDialog.Builder(this)
                .setTitle("ArTellのID登録")
                .setMessage("ArTellメッセージの受信を許可しますか？")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //ネットワークの接続状態を確認
                        ConnectivityManager connMgr = (ConnectivityManager)
                                getSystemService(Context.CONNECTIVITY_SERVICE);
                        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                        if (networkInfo != null && networkInfo.isConnected()) {
                            register();
//                          Toast.makeText(context, "ID registered successfully!!", Toast.LENGTH_LONG).show();

                        } else {
                            // 電波なかったらこのダイアログ出す
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("ネットワーク接続不可")
                                    .setMessage("電波のある所でリトライして下さい。")
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            finish();
                                        }
                                    })
                                    .setCancelable(false)
                                    .show();
                        }
                    }
                })
                .setNegativeButton("キャンセル", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setCancelable(false)       //画面外タッチによるキャンセル阻止
                .show();
        }

        //DBの準備
        MyOpenHelper helper = new MyOpenHelper(this);
        db = helper.getWritableDatabase();
//        db.execSQL("delete from notification;");          //DB内容削除(デバッグ用)
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyApplication.activityResumed();
        checkPlayServices();
        //前面に出たとこで画面リロード
        dbToListView();          //ListView表示処理
    }

    @Override
    protected void onPause() {
        super.onPause();
        MyApplication.activityPaused();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        gcm.close();
        db.close();
    }

    //メイン画面で戻る押されたらアプリ終了(なんかリロードされたりするから)
    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getAction() == KeyEvent.ACTION_UP && e.getKeyCode() == KeyEvent.KEYCODE_BACK) { //バックボタンが離された時
            finish();
//            System.exit(RESULT_OK);     //finishで閉じない時あるからこっちもあり(でもこれでもたまに開き直る)
        }
        return super.dispatchKeyEvent(e);
    }
    //手動更新処理(気持ちの問題)
    @Override
    public void onClick(View v) {
        FVP = 0;
        y = 0;
        dbToListView();
        Toast.makeText(this, "リスト更新完了！", Toast.LENGTH_SHORT).show();
    }
    //リストがクリックされた時の処理
    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
//        Toast.makeText(this, String.valueOf(position), Toast.LENGTH_SHORT).show();    //positionは0～だった

        //listview数 - positionでdbのid列を出して、SQL文に入れられるようにString型に変換
        String dbId = String.valueOf(myadapter.getCount() - position);

        //クリックされたビューを既読にする(DB内のflagを1にする)
        db.execSQL("UPDATE notification SET read_flag= 1 WHERE _id = " + dbId + ";");

        //リストの現在位置を取得(初期位置は両方0)
        FVP = listview.getFirstVisiblePosition();
        y = listview.getChildAt(0).getTop();

        //詳細画面への遷移
        Intent intent = new Intent(this, SubActivity.class)
                .putExtra("message", full_msg[position])
                .putExtra("time", time[position]);
        startActivity(intent);
    }
    //ListView表示処理
    private void dbToListView() {
        //今あるリスト内容をクリア
        list.clear();
        //DBから配列にデータを格納
        Cursor c = db.rawQuery("SELECT * FROM notification ORDER BY _id DESC;", null);
        c.moveToFirst();
        full_msg = new String[c.getCount()];
        int[] flag = new int[c.getCount()];
        String[] message = new String[c.getCount()];
        time = new String[c.getCount()];
        read = new String[c.getCount()];
        for (int i = 0; i < c.getCount(); i++) {
            flag[i] = c.getInt(c.getColumnIndex("read_flag"));        //配列[0]が1行目
            full_msg[i] = c.getString(c.getColumnIndex("message"));
            time[i] = c.getString(c.getColumnIndex("recv_time"));
            c.moveToNext();
            //flagの0,1をreadの未読既読に
            if (flag[i] == 0) {
                read[i] = "未読";
            } else {
                read[i] = "";
            }
            //秒表示を除く処理(timeを16文字だけ表示)
            time[i] = time[i].substring(0, 16);
            //メッセージの表示する文字数を調整
            int first_sep = full_msg[i].indexOf(System.getProperty("line.separator"));//最初の改行場所を取得
            if (first_sep != -1 && first_sep < 20) {                       //改行が20文字目以内にあったら
                message[i] = full_msg[i].substring(0, first_sep);   //そこから削って
                message[i] += " …";                                       //…ってやる
            } else if (full_msg[i].length() > 20) {                //改行なければ20文字まで表示
                message[i] = full_msg[i].substring(0, 20);            //21文字以上ある時は削って
                message[i] += " …";                                      //…ってやる
            } else {
                message[i] = full_msg[i];                           //20文字以内ならそのまま表示
            }
            //ListViewに表示させる項目をdataに格納
            HashMap<String, String> map = new HashMap<>();              //HashMapはJavaの連想配列
            map.put("message", message[i]);                          //第1引数keyは添字(map[message]=メッセージ1みたくなる)
            map.put("read", read[i]);
            map.put("time", time[i]);
            list.add(map);
        }
//        Collections.reverse(list);                    //これでも逆順できる
        c.close();

        //AdapterでListの配列をListViewへ
        listview = (ListView) findViewById(R.id.ListView);
        myadapter = new MyAdapter(this, list);
        listview.setAdapter(myadapter);
        listview.setSelectionFromTop(FVP, y);       //記憶してあったリストの位置を取得
        listview.setOnItemClickListener(this);      //ListViewに対してはOnClickListenerじゃなくてこれを使う

    }

    //端末のGooglePlayServiceAPKの有無をチェック
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i("TAG", "Playサービスがサポートされていない端末です");
                finish();
            }
            return false;
        }
        return true;
    }

    //端末に保存されている登録IDの取得
    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.equals("")) {        //登録IDがまだなければ
            return "";                          //""を返す
        }
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            return "";
        }
        return registrationId;
    }

    //アプリケーションのバージョン情報を取得する
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("パッケージが見つかりません:" + e);
        }
    }

    //アプリのプリファレンスを取得する
    private SharedPreferences getGCMPreferences(Context context) {
        return getSharedPreferences(MainActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }

    //登録IDをプリファレンスで端末保存
    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGCMPreferences(context);
        int appVersion = getAppVersion(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.apply();
    }

    // 登録IDの登録処理
    private void register() {

        // くるくるを表示
        final ProgressDialog dialog = new ProgressDialog(MainActivity.this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage("登録中");
        dialog.show();

        //固有の端末識別番号の取得
        final String deviceId;
        TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        if(telephonyManager.getDeviceId() != null) {    //端末IDの有無(電話番号がないとnullらしい)
            deviceId = telephonyManager.getDeviceId();  //端末IDを取得
        } else {
            deviceId = android.os.Build.SERIAL;        //タブレット等は代わりにシリアル番号を取得
        }

        new AsyncTask<Void, Void, String>() {          //登録処理は非同期で
            @Override
            protected String doInBackground(Void... params) {

                String result = null;

                try {
                    /** GCMサーバーへの登録(登録IDの発行) */
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    InstanceID instanceID = InstanceID.getInstance(context);
                    registrationId = instanceID.getToken(SENDER_ID, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);

                    /** 登録IDをアプリサーバーに送る */
                    final MediaType TEXT = MediaType.parse("text/plain; charset=utf-8");              // リクエストボディを作る
                    final String BOUNDARY = String.valueOf(System.currentTimeMillis());

                    RequestBody requestBody = new MultipartBuilder(BOUNDARY)
                            .type(MultipartBuilder.FORM)
                            .addPart(
                                    Headers.of("Content-Disposition", "form-data; name='regId'"),   //ここのnameがphpの$_POST['regId']と連動
                                    RequestBody.create(TEXT, registrationId))                            //第2引数が送りたいテキスト
                            .addPart(                                                                      //2つ以上のデータも送れる
                                    Headers.of("Content-Disposition", "form-data; name='devId'"),
                                    RequestBody.create(TEXT, deviceId))
                            .build();
                    // リクエストオブジェクトを作って
                    Request request = new Request.Builder()
                            .url("http://searching4freedom.razor.jp/GCM/register.php")
                            .post(requestBody)
                            .build();
                    OkHttpClient client = new OkHttpClient();
                    //Basic認証に対応する
                    client.setAuthenticator(new Authenticator() {
                        @Override
                        public Request authenticate(Proxy proxy, Response response) throws IOException {
                            String credential = Credentials.basic("guest", "guest");        //ユーザー名, パスワード
                            return response.request().newBuilder().header("Authorization", credential).build();
                        }
                        @Override
                        public Request authenticateProxy(Proxy proxy, Response response) throws IOException {
                            return null;
                        }
                    });
                    // リクエストして結果を受け取る
                    Response response = client.newCall(request).execute();
                    result = response.body().string();

                } catch (IOException e) {
                    e.printStackTrace();
                }
                // 結果返す
                return result;
            }
            // 結果に応じた処理をUIスレッドで行う
            @Override
            protected void onPostExecute(String result) {
                Log.d("TAG", result);

                // APサーバー通信の成否で場合分け
                if(result.equals("save ok!")) {
                    // 登録IDを端末に保存
                    storeRegistrationId(context, registrationId);
                    // くるくるを消去
                    dialog.dismiss();

                    new AlertDialog.Builder(MainActivity.this)  //ここはcontextだとエラーになる
                            .setTitle("ArTellのID登録")
                            .setMessage("ID登録が完了しました！")
                            .setPositiveButton("OK", null)
                            .show();
                } else {
                    // APサーバーが落ちてた時の処理
                    Log.d("TAG", "サーバーエラー");
                    new AlertDialog.Builder(MainActivity.this)  //ここはcontextだとエラーになる
                            .setTitle("サーバーエラー")
                            .setMessage("時間を置いてリトライして下さい。")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })
                            .setCancelable(false)
                            .show();
                }
            }
        }.execute();
    }
}