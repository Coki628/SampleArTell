package com.example.sample.sampleartell;

import android.app.AlertDialog;
import android.app.Dialog;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;

public class AlertDialogFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        //AlertDialogActivityから受け取るメッセージ
        Bundle bundle = getArguments();
        String message = bundle.getString("ADAtoADF");
        Log.d("C",message);

        //起動するダイアログ
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        Dialog dialog = builder.setTitle("ArTellメッセージの受信")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .setCancelable(false)
                .create();

        return dialog;
    }
    //Dialog閉じる時にAlertDialogActivityも同時に終了させる
    @Override
    public void onStop() {
        super.onStop();
        getActivity().finish();
    }
}