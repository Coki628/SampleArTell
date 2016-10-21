package com.example.sample.sampleartell;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MyOpenHelper extends SQLiteOpenHelper {

    public MyOpenHelper(Context c){
        super(c, "artell.db", null, 1);
    }

    public void onCreate(SQLiteDatabase db) {       //データベースがまだなければこれが呼ばれる
        db.execSQL(
                "create table notification ( _id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "read_flag INTEGER, message TEXT, recv_time TEXT );"
        );
        //SQLiteの仕様で、booleanはintegerの0,1、日付はtextで表すみたい
/*        db.execSQL("insert into notification( read_flag, message, recv_time ) " +
                "values(0, 'メッセージ1 送信チェック　確認のため、少し長めの文章にします。', '2015/08/25 13:15:42' );");*/
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {     //バージョン情報が異なると呼ばれる
        db.execSQL("drop table notification;");
        onCreate(db);
    }
}
