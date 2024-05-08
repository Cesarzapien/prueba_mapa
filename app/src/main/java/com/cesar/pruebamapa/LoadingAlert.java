package com.cesar.pruebamapa;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Handler;
import android.view.LayoutInflater;

public class LoadingAlert {

    private Activity activity;
    private AlertDialog dialog;
    private Handler handler;

    LoadingAlert(Activity myActivity) {
        activity = myActivity;
        handler = new Handler();
    }

    void startAlertDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        LayoutInflater inflater = activity.getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.dialog_charging, null));

        builder.setCancelable(true);

        dialog = builder.create();
        dialog.show();

        // Cerrar el diálogo después de 3 segundos
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                closeAlertDialog();
            }
        }, 1000); // 1000 milisegundos = 1 segundo
    }

    void closeAlertDialog(){
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
