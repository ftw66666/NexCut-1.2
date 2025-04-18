package com.example.opencv.image;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.opencv.MainActivity;
import com.example.opencv.R;

import java.io.File;
import java.text.DecimalFormat;
import java.util.List;

public class GCodeFileAdapter extends BaseAdapter {
    private final List<File> files;
    private final Context context;
    private final LayoutInflater inflater;
    private final Runnable onDataChanged;

    public GCodeFileAdapter(Context context, List<File> files, Runnable onDataChanged) {
        this.context = context;
        this.files = files;
        this.inflater = LayoutInflater.from(context);
        this.onDataChanged = onDataChanged;
    }

    @Override
    public int getCount() {
        return files.size();
    }

    @Override
    public File getItem(int position) {
        return files.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = (convertView != null)
                ? convertView
                : inflater.inflate(R.layout.item_gcode_file, parent, false);

        TextView tvName = row.findViewById(R.id.tvFileName);
        TextView tvX    = row.findViewById(R.id.tvDeleteX);

        File file = getItem(position);
        String nameWithSize = file.getName() + " (" + readableFileSize(file.length()) + ")";
        tvName.setText(nameWithSize);

        // 点击文件行：打开确认对话框
        row.setOnClickListener(v -> {
            ((Activity) context).runOnUiThread(() -> {
                ((MainActivity) context).showConfirmationDialog(file);
            });
        });

        // 点击“×”删除
        tvX.setOnClickListener(v -> new AlertDialog.Builder(context)
                .setTitle("确认删除")
                .setMessage("确定要删除文件？\n" + file.getName())
                .setPositiveButton("删除", (dlg, which) -> {
                    if (file.delete()) {
                        files.remove(position);
                        notifyDataSetChanged();
                        onDataChanged.run();
                        Toast.makeText(context, "已删除：" + file.getName(),
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "删除失败：" + file.getName(),
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show());

        return row;
    }

    private String readableFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#")
                .format(size / Math.pow(1024, digitGroups))
                + " " + units[digitGroups];
    }


}
