package com.example.sample.sampleartell;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

//アダプターをカスタムする
public class MyAdapter extends BaseAdapter {
    Context context;
    LayoutInflater myInflater;
    ArrayList<HashMap<String, String>> list;

    //コンストラクタでnew時に呼ばれる処理を書いとく
    public MyAdapter(Context context, ArrayList<HashMap<String, String>> list) {
        this.context = context;
        this.myInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.list = list;   //引数で受け取ったlistを入れる
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    //リストビュー1行ごとに呼ばれる
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //ビューがあれば再利用する処理
        if (convertView == null) {
            convertView = myInflater.inflate(R.layout.listview_row, parent, false);
        }
        //各テキストをレイアウトと繋ぐ
        TextView message = (TextView) convertView.findViewById(R.id.message);
        message.setText(list.get(position).get("message"));
        TextView read = (TextView) convertView.findViewById(R.id.read_flag);
        read.setText(list.get(position).get("read"));
        TextView time = (TextView) convertView.findViewById(R.id.recv_time);
        time.setText(list.get(position).get("time"));
        //未読・既読で色を変える(既読非表示につき不要となった)
/*        if (list.get(position).get("read").equals("未読")) {
            read.setTextColor(Color.BLUE);
        } else {
            read.setTextColor(Color.rgb(15, 15, 15));
        }*/
        return convertView;
    }
}