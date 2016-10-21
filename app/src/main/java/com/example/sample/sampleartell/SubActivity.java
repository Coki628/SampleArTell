package com.example.sample.sampleartell;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class SubActivity extends Activity implements OnClickListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub);

        //Mainから要素を受け取って表示
        Intent intent = getIntent();
        String message = intent.getStringExtra("message");
        TextView viewMessage = (TextView)this.findViewById(R.id.full_message);
        viewMessage.setText(message);
        String time = intent.getStringExtra("time");
        TextView viewTime = (TextView)this.findViewById(R.id.time);
        viewTime.setText(time);

        Button ok_btn = (Button)this.findViewById(R.id.ok_btn);
        ok_btn.setOnClickListener(this);
    }

    //OKボタンが押されたらメイン画面へ戻る
    @Override
    public void onClick( View v ) {
        //本体バックボタンを押した時と同じ挙動にする
        try {
            Runtime.getRuntime().exec("input keyevent " + KeyEvent.KEYCODE_BACK);
        } catch (IOException e) {
            e.printStackTrace();
            dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
            dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));
        }
    }
}
