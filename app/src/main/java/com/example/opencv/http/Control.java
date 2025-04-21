package com.example.opencv.http;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.widget.Switch;
import android.widget.Toast;

import com.example.opencv.Utils.ProgressBar2Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Response;

public class Control {
    ApiClient apiClient = ApiClient.getInstance();

    public boolean VailadContect(Context context) {
        Handler handler = new Handler(Looper.getMainLooper());
        if (apiClient.isConnected.get()) {
            return true;
        } else {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, "未连接", Toast.LENGTH_SHORT).show();
                }
            });
            return false;
        }
    }

    public void FileTransfer(File selectedFile, Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Handler handler = new Handler(Looper.getMainLooper());
                if (VailadContect(context)) {
                    ProgressBar2Utils progressBar2Utils = new ProgressBar2Utils();
                    try {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                progressBar2Utils.showProgressDialog(context, selectedFile.getName());
                            }
                        });
                        Response response = apiClient.uploadGCode(selectedFile);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                progressBar2Utils.dismissDialog();
                            }
                        });
                        if (!response.isSuccessful()) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, "上传失败", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        else{
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, "上传成功", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (IOException e) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                progressBar2Utils.dismissDialog();
                            }
                        });
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "上传失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                        throw new RuntimeException(e);
                    }
                }
            }
        }).start();
    }

    public void GetMachineInfo() {
        if (apiClient.isConnected.get()) {
            try {
                Response response = apiClient.getMachineInfo();
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    MachineInfoResponse machineInfoResponse = apiClient.gson.fromJson(json, MachineInfoResponse.class);
                    apiClient.machineInfo.updateFrom(machineInfoResponse);
                    apiClient.isInfo.set(true);
                } else {
                    apiClient.isConnected.set(false);
                    apiClient.isInfo.set(false);
                }
            } catch (IOException e) {
                apiClient.isConnected.set(false);
                apiClient.isInfo.set(false);
            }
        }
    }

    public void MoveAxis(int index, int distance, int speed, Context context) {
        Handler handler = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (VailadContect(context)) {
                    try {
                        Response response = apiClient.moveAxis(index, distance, speed);
                        GenericResponse genericResponse = apiClient.gson.fromJson(response.body().string(), GenericResponse.class);
                        if (response.isSuccessful()) {
                            if (!genericResponse.isState()) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(context, "移动失败." + genericResponse.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, "移动失败", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (IOException e) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "移动失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        }).start();
    }

    public void SystemOrigin(Context context) {
        Handler handler = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (VailadContect(context)) {
                    try {
                        Response response = apiClient.systemOrigin();
                        GenericResponse genericResponse = apiClient.gson.fromJson(response.body().string(), GenericResponse.class);
                        if (response.isSuccessful()) {
                            if (!genericResponse.isState()) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(context, "回原失败." + genericResponse.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, "回原失败", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (IOException e) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "回原失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        }).start();
    }

    public void Zero(Context context) {
        Handler handler = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (VailadContect(context)) {
                    try {
                        Response response = apiClient.zeroReturn();
                        GenericResponse genericResponse = apiClient.gson.fromJson(response.body().string(), GenericResponse.class);
                        if (response.isSuccessful()) {
                            if (!genericResponse.isState()) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(context, "回零失败." + genericResponse.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, "回零失败", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (IOException e) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "回零失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        }).start();
    }

    public FileListResponse GetLocalFileList(Context context) {
        Handler handler = new Handler(Looper.getMainLooper());
        if (VailadContect(context)) {
            try {
                Response response = apiClient.getLocalFiles();
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    FileListResponse fileListResponse = apiClient.gson.fromJson(json, FileListResponse.class);
                    return fileListResponse;
                } else {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "获取失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (IOException e) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "获取失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
        return null;
    }

    public void LoadFile(Context context) {
        Handler handler = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {

                FileListResponse localFile = GetLocalFileList(context);
                if (localFile == null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "获取文件失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                }
                List<String> files = new ArrayList<>();
                if (localFile.getEnc() != null) {
                    files.addAll(localFile.getEnc());
                }
                if (localFile.getNc() != null) {
                    files.addAll(localFile.getNc());
                }
                if (files.isEmpty()) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "本地没有文件", Toast.LENGTH_SHORT).show();
                        }
                    });

                } else {
                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            builder.setTitle("本地文件列表");
                            builder.setItems(files.toArray(new String[0]), (dialog, which) -> {
                                String File = files.get(which);
                                // 创建第二个AlertDialog
                                AlertDialog.Builder secondDialogBuilder = new AlertDialog.Builder(context);
                                secondDialogBuilder.setMessage("是否选择加载: " + File);
                                secondDialogBuilder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (VailadContect(context)) {
                                                    try {
                                                        Response response = apiClient.loadFile(File);
                                                        GenericResponse genericResponse = apiClient.gson.fromJson(response.body().string(), GenericResponse.class);
                                                        if (response.isSuccessful()) {
                                                            if (!genericResponse.isState()) {
                                                                handler.post(new Runnable() {
                                                                    @Override
                                                                    public void run() {
                                                                        Toast.makeText(context, "加载失败." + genericResponse.getMessage(), Toast.LENGTH_SHORT).show();
                                                                    }
                                                                });
                                                            }
                                                        } else {
                                                            handler.post(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    Toast.makeText(context, "加载失败", Toast.LENGTH_SHORT).show();
                                                                }
                                                            });
                                                        }
                                                    } catch (IOException e) {
                                                        handler.post(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                Toast.makeText(context, "加载失败", Toast.LENGTH_SHORT).show();
                                                            }
                                                        });
                                                    }
                                                }
                                            }
                                        }).start();
                                    }
                                });
                                secondDialogBuilder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                                // 显示第二个对话框
                                secondDialogBuilder.show();
                            });
                            builder.show();
                        }
                    });
                }
            }
        }).start();
    }

    public void Start(Context context, boolean isEmpty, int speed) {
        Handler handler = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (VailadContect(context)) {
                    try {
                        Response response = apiClient.startProcess(isEmpty, speed);
                        GenericResponse genericResponse = apiClient.gson.fromJson(response.body().string(), GenericResponse.class);
                        if (response.isSuccessful()) {
                            if (!genericResponse.isState()) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(context, "启动失败." + genericResponse.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, "启动失败", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (IOException e) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "启动失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        }).start();
    }

    public void Stop(Context context) {
        Handler handler = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (VailadContect(context)) {
                    try {
                        Response response = apiClient.stopProcess();
                        GenericResponse genericResponse = apiClient.gson.fromJson(response.body().string(), GenericResponse.class);
                        if (response.isSuccessful()) {
                            if (!genericResponse.isState()) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(context, "停止失败." + genericResponse.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, "停止失败", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (IOException e) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "停止失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        }).start();
    }

    public void Pause(Context context) {
        Handler handler = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (VailadContect(context)) {
                    try {
                        Response response = apiClient.pauseProcess();
                        GenericResponse genericResponse = apiClient.gson.fromJson(response.body().string(), GenericResponse.class);
                        if (response.isSuccessful()) {
                            if (!genericResponse.isState()) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(context, "暂停失败." + genericResponse.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, "暂停失败", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (IOException e) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "暂停失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        }).start();
    }

    public WarningResponse GetWarning(Context context) {
        Handler handler = new Handler(Looper.getMainLooper());
        if (VailadContect(context)) {
            try {
                Response response = apiClient.getWarningStatus();
                if (response.isSuccessful()) {
                    WarningResponse warningResponse = apiClient.gson.fromJson(response.body().string(), WarningResponse.class);
                    return warningResponse;
                } else {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "获取失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (IOException e) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "获取失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
        return null;
    }

    public void ClearWarning(Context context) {
        Handler handler = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (VailadContect(context)) {
                    try {
                        Response response = apiClient.clearAlarm();
                        GenericResponse genericResponse = apiClient.gson.fromJson(response.body().string(), GenericResponse.class);
                        if (response.isSuccessful()) {
                            if (!genericResponse.isState()) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(context, "清除失败." + genericResponse.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, "清除失败", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (IOException e) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "清除失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        }).start();
    }

    public void SetDA(Context context, int index, float value) {
        Handler handler = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (VailadContect(context)) {
                    try {
                        Response response = apiClient.setDA(index, value);
                        GenericResponse genericResponse = apiClient.gson.fromJson(response.body().string(), GenericResponse.class);
                        if (response.isSuccessful()) {
                            if (!genericResponse.isState()) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(context, "设置DA失败." + genericResponse.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, "设置DA失败", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (IOException e) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "设置DA失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        }).start();
    }

    public void SetDO(Context context, int index, boolean value, Switch switch1) {
        Handler handler = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (VailadContect(context)) {
                    try {
                        Response response = apiClient.setDO(index, value);
                        GenericResponse genericResponse = apiClient.gson.fromJson(response.body().string(), GenericResponse.class);
                        if (response.isSuccessful()) {
                            if (!genericResponse.isState()) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(context, "设置DO失败." + genericResponse.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                                switch1.setChecked(!value);
                            }
                        } else {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, "设置DO失败", Toast.LENGTH_SHORT).show();
                                }
                            });
                            switch1.setChecked(!value);
                        }
                    } catch (IOException e) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "设置DO失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                        switch1.setChecked(!value);
                    }
                }
            }
        }).start();
    }


    public void FTCCalibration(Context context) {
        Handler handler = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (VailadContect(context)) {
                    try {
                        Response response = apiClient.calibrateFTC();
                        GenericResponse genericResponse = apiClient.gson.fromJson(response.body().string(), GenericResponse.class);
                        if (response.isSuccessful()) {
                            if (!genericResponse.isState()) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(context, "FTC标定失败." + genericResponse.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, "FTC标定失败", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (IOException e) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "FTC标定失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        }).start();
    }

    public void FTCFollow(Context context) {
        Handler handler = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (VailadContect(context)) {
                    try {
                        Response response = apiClient.enableFollowing();
                        GenericResponse genericResponse = apiClient.gson.fromJson(response.body().string(), GenericResponse.class);
                        if (response.isSuccessful()) {
                            if (!genericResponse.isState()) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(context, "FTC跟随失败." + genericResponse.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, "FTC跟随失败", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (IOException e) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "FTC跟随失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        }).start();
    }
}