package com.example.opencv.Utils;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.opencv.R;

public class ProgressBarUtils {
    private AlertDialog progressDialog;
    private ProgressBar progressBar;
    private TextView tvProgress;

    // 显示对话框
    public void showProgressDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.file_processbar, null);

        progressBar = dialogView.findViewById(R.id.progressBar);
        tvProgress = dialogView.findViewById(R.id.tvProgress);

        builder.setView(dialogView)
                .setTitle("文件传输中...")
                .setCancelable(false); // 禁止点击外部关闭

        progressDialog = builder.create();
        progressDialog.show();
    }

    // 显示对话框
    public void showProgressDialog(Context context, String title) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.file_processbar, null);

        progressBar = dialogView.findViewById(R.id.progressBar);
        tvProgress = dialogView.findViewById(R.id.tvProgress);

        builder.setView(dialogView)
                .setTitle(title)
                .setCancelable(false); // 禁止点击外部关闭

        progressDialog = builder.create();
        progressDialog.show();
    }

    // 更新进度
    public void updateProgress(int progress) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressBar.setProgress(progress);
            tvProgress.setText(progress + "%");
        }
    }

    // 关闭对话框
    public void dismissDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }


}
