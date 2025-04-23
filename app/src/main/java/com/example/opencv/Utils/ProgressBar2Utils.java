package com.example.opencv.Utils;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.example.opencv.R;

public class ProgressBar2Utils {
    private AlertDialog progressDialog;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public void showProgressDialog(Context context, String filename) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.progressbardialog, null);


        TextView textView = dialogView.findViewById(R.id.textView);

        builder.setView(dialogView)
                .setTitle(filename + "上传中")
                .setCancelable(false); // 禁止点击外部关闭

        progressDialog = builder.create();
        progressDialog.show();
    }

    public void dismissDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
