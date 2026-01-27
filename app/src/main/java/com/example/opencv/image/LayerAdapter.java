package com.example.opencv.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.opencv.R;

import java.util.List;

public class LayerAdapter extends RecyclerView.Adapter<LayerAdapter.LayerViewHolder> {
    private List<LayerPreviewActivity.LayerParam> layerParams;

    public LayerAdapter(List<LayerPreviewActivity.LayerParam> layerParams) {
        this.layerParams = layerParams;
    }

    @NonNull
    @Override
    public LayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layer_card, parent, false);
        return new LayerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LayerViewHolder holder, int position) {
        LayerPreviewActivity.LayerParam layer = layerParams.get(position);
        Bitmap bitmap = BitmapFactory.decodeFile(layer.filePath);
        if (bitmap != null) holder.imgPreview.setImageBitmap(bitmap);
        holder.tvType.setText("打印方式: " + layer.printingMethod);

        holder.paramContainer.removeAllViews();
        ViewGroup container = holder.paramContainer;
        container.removeAllViews();

        String method = layer.printingMethod == null ? "" : layer.printingMethod.toLowerCase();
        if ("scan".equals(method)) {
            // 线密度
            TextView tvRho = new TextView(container.getContext());
            tvRho.setText("线密度:");
            EditText etRho = new EditText(container.getContext());
            etRho.setInputType(InputType.TYPE_CLASS_NUMBER);
            etRho.setText(String.valueOf(layer.rho));
            etRho.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    try {
                        int val = Integer.parseInt(etRho.getText().toString());
                        layer.rho = val;
                    } catch (Exception ignore) {}
                }
            });
            container.addView(tvRho);
            container.addView(etRho);

            // 半调网屏
            TextView tvHalftone = new TextView(container.getContext());
            tvHalftone.setText("使用半调网屏:");
            Switch swHalftone = new Switch(container.getContext());
            swHalftone.setChecked(layer.isHalftone);
            swHalftone.setOnCheckedChangeListener((buttonView, isChecked) -> layer.isHalftone = isChecked);
            container.addView(tvHalftone);
            container.addView(swHalftone);
        } else if ("vector".equals(method) || "engrave".equals(method) || "雕刻".equals(method)) {
            // 激光功率
            TextView tvPower = new TextView(container.getContext());
            tvPower.setText("激光功率:");
            EditText etPower = new EditText(container.getContext());
            etPower.setInputType(InputType.TYPE_CLASS_NUMBER);
            etPower.setText(String.valueOf(layer.laserPower));
            etPower.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    try {
                        int val = Integer.parseInt(etPower.getText().toString());
                        layer.laserPower = val;
                    } catch (Exception ignore) {}
                }
            });
            container.addView(tvPower);
            container.addView(etPower);
        }
    }

    @Override
    public int getItemCount() {
        return layerParams.size();
    }

    static class LayerViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPreview;
        TextView tvType;
        LinearLayout paramContainer;
        public LayerViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPreview = itemView.findViewById(R.id.imgPreview);
            tvType = itemView.findViewById(R.id.tvType);
            paramContainer = itemView.findViewById(R.id.paramContainer);
        }
    }
}
