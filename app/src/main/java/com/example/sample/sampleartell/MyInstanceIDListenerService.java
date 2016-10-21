package com.example.sample.sampleartell;

import com.google.android.gms.iid.InstanceIDListenerService;

//GCMサーバーのトークンの変更を検知して更新するクラス
public class MyInstanceIDListenerService extends InstanceIDListenerService {
    @Override
    public void onTokenRefresh() {
        super.onTokenRefresh();

    }
}