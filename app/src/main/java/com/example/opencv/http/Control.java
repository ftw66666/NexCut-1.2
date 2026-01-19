package com.example.opencv.http;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Switch;
import android.widget.Toast;

import com.example.opencv.R;
import com.example.opencv.Utils.ProgressBar2Utils;
import com.example.opencv.Utils.ProgressBarUtils;

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
                    Toast.makeText(context, R.string.device_status_disconnected, Toast.LENGTH_SHORT).show();
                }
            });
            return false;
        }
    }

    public void Login(Context context, String username, String password) {
        Handler handler = new Handler(Looper.getMainLooper());
        try {
            Response response = apiClient.login(username, password);
            if (response.isSuccessful()) {
                GenericResponse genericResponse = apiClient.gson.fromJson(response.body().string(), GenericResponse.class);
                if (!genericResponse.isState()) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            new AlertDialog.Builder(context)
                                    .setMessage(R.string.user_problem)
                                    .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Logout(context, true);
                                        }
                                    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    })
                                    .show();
                        }
                    });
                } else {
                    apiClient.uuid = genericResponse.getMessage();
                    apiClient.isConnected.set(true);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, R.string.Login_Successful, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } else {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, R.string.Login_failed, Toast.LENGTH_SHORT).show();

                    }
                });
            }
        } catch (IOException e) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, R.string.Login_failed, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public void Logout(Context context, boolean force) {
        Handler handler = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 如果BASE_URL为空，什么也不做
                    if (apiClient.BASE_URL.length() == 0 || apiClient.BASE_URL.toString().trim().isEmpty()) {
                        return;
                    }
                    Response response = apiClient.logout(force);
                    if (response.isSuccessful()) {
                        apiClient.isConnected.set(false);
                        Log.d("ExitMonitor", "注销成功(Cancellation Successful)");
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, R.string.Logout_successful, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, R.string.Logout_fail, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (IOException e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, R.string.Logout_fail, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();

    }

//    public void FileTransfer(File selectedFile, Context context) {
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                Handler handler = new Handler(Looper.getMainLooper());
//                if (VailadContect(context)) {
//                    ProgressBar2Utils progressBar2Utils = new ProgressBar2Utils();
//                    try {
//                        handler.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                progressBar2Utils.showProgressDialog(context, selectedFile.getName());
//                            }
//                        });
//                        Response response = apiClient.uploadGCode(selectedFile);
//                        handler.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                progressBar2Utils.dismissDialog();
//                            }
//                        });
//                        if (!response.isSuccessful()) {
//                            handler.post(new Runnable() {
//                                @Override
//                                public void run() {
//                                    Toast.makeText(context, "上传失败", Toast.LENGTH_SHORT).show();
//                                }
//                            });
//                        } else {
//                            handler.post(new Runnable() {
//                                @Override
//                                public void run() {
//                                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
//                                    builder.setMessage("是否加载" + selectedFile.getName() + "?");
//                                    builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
//                                        @Override
//                                        public void onClick(DialogInterface dialog, int which) {
//                                            LoadFile(selectedFile.getName(), context);
//                                        }
//                                    });
//                                    builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
//                                        @Override
//                                        public void onClick(DialogInterface dialog, int which) {
//                                            dialog.dismiss();
//                                        }
//                                    });
//                                    builder.show();
//                                }
//                            });
//                        }
//                    } catch (IOException e) {
//                        handler.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                progressBar2Utils.dismissDialog();
//                            }
//                        });
//                        handler.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                Toast.makeText(context, "上传失败", Toast.LENGTH_SHORT).show();
//                            }
//                        });
//                        throw new RuntimeException(e);
//                    }
//                }
//            }
//        }).start();
//    }

    public void FileTransfer(File selectedFile, Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Handler handler = new Handler(Looper.getMainLooper());
                ProgressBarUtils progressBarUtils = new ProgressBarUtils();
                if (VailadContect(context)) {
                    try {
                        ProgressRequestBody.ProgressListener listener = new ProgressRequestBody.ProgressListener() {
                            @Override
                            public void onProgress(long bytesRead, long contentLength) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        progressBarUtils.updateProgress((int) (bytesRead * 100 / contentLength));
                                    }
                                });
                            }
                        };
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                progressBarUtils.showProgressDialog(context);
                            }
                        });

                        Response response = apiClient.uploadGCode(selectedFile, listener);
                        if (!response.isSuccessful()) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            progressBarUtils.dismissDialog();
                                        }
                                    });
                                    Toast.makeText(context, R.string.Upload_failed, Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    progressBarUtils.dismissDialog();
                                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                    builder.setMessage(R.string.Load_or_not + selectedFile.getName() + "?");
                                    builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            LoadFile(selectedFile.getName(), context);
                                        }
                                    });
                                    builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    });
                                    builder.show();
                                }
                            });
                        }

                    } catch (IOException e) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                progressBarUtils.dismissDialog();
                                Toast.makeText(context, R.string.Upload_failed, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        }).start();
    }

    public void GetMachineInfo(Context context) {
        Handler handler = new Handler(Looper.getMainLooper());
        if (apiClient.isConnected.get()) {
            try {
                Response response = apiClient.getMachineInfo();
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    MachineInfoResponse machineInfoResponse = apiClient.gson.fromJson(json, MachineInfoResponse.class);
                    if (machineInfoResponse.getFtc() == null || machineInfoResponse.getMc() == null) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, R.string.Connection_lost, Toast.LENGTH_SHORT).show();
                            }
                        });
                        apiClient.isConnected.set(false);
                        apiClient.isInfo.set(false);
                    } else {
                        apiClient.machineInfo.updateFrom(machineInfoResponse);
                        apiClient.isInfo.set(true);
                    }
                } else {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, R.string.Connection_lost, Toast.LENGTH_SHORT).show();
                        }
                    });
                    apiClient.isConnected.set(false);
                    apiClient.isInfo.set(false);
                }
            } catch (IOException e) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, R.string.Connection_lost, Toast.LENGTH_SHORT).show();
                    }
                });
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
                                        Toast.makeText(context, R.string.Move_failed+"."+ genericResponse.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, R.string.Move_failed, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (IOException e) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, R.string.Move_failed, Toast.LENGTH_SHORT).show();
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
                                        Toast.makeText(context, R.string.Return_failed +"."+ genericResponse.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, R.string.Return_failed, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (IOException e) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, R.string.Return_failed, Toast.LENGTH_SHORT).show();
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
                                        Toast.makeText(context, R.string.Homing_failed+"." + genericResponse.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, R.string.Homing_failed, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (IOException e) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, R.string.Homing_failed, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        }).start();
    }

    public void Border(Context context, int speed) {
        Handler handler = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (VailadContect(context)) {
                    try {
                        Response response = apiClient.border(speed);
                        GenericResponse genericResponse = apiClient.gson.fromJson(response.body().string(), GenericResponse.class);
                        if (response.isSuccessful()) {
                            if (!genericResponse.isState()) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(context, R.string.Failed_to_walk_along_the_edge+"." + genericResponse.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, R.string.Failed_to_walk_along_the_edge, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (IOException e) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, R.string.Failed_to_walk_along_the_edge, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        }).start();
    }

    public void AxisStop(Context context) {
        Handler handler = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (VailadContect(context)) {
                    try {
                        Response response = apiClient.axisStop();
                        GenericResponse genericResponse = apiClient.gson.fromJson(response.body().string(), GenericResponse.class);
                        if (response.isSuccessful()) {
                            if (!genericResponse.isState()) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(context, R.string.Singleaxis_stop_faile+"."+ genericResponse.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, R.string.Singleaxis_stop_faile, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (IOException e) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, R.string.Singleaxis_stop_faile, Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(context, R.string.Failed_to_retrieve, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (IOException e) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, R.string.Failed_to_retrieve, Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(context, R.string.Failed_to_get_the_file, Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(context, R.string.No_local_files, Toast.LENGTH_SHORT).show();
                        }
                    });

                } else {
                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            builder.setTitle(R.string.Local_File_List);
                            builder.setItems(files.toArray(new String[0]), (dialog, which) -> {
                                String File = files.get(which);
                                // 创建第二个AlertDialog
                                AlertDialog.Builder secondDialogBuilder = new AlertDialog.Builder(context);
                                secondDialogBuilder.setMessage(R.string.Load + File);
                                secondDialogBuilder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
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
                                                                        Toast.makeText(context, R.string.Loading_failed+"." + genericResponse.getMessage(), Toast.LENGTH_SHORT).show();
                                                                    }
                                                                });
                                                            }
                                                        } else {
                                                            handler.post(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    Toast.makeText(context, R.string.Loading_failed, Toast.LENGTH_SHORT).show();
                                                                }
                                                            });
                                                        }
                                                    } catch (
                                                            IOException e) {
                                                        handler.post(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                Toast.makeText(context, R.string.Loading_failed, Toast.LENGTH_SHORT).show();
                                                            }
                                                        });
                                                    }
                                                }
                                            }
                                        }).start();
                                    }
                                });
                                secondDialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                                // 显示第二个对话框
                                secondDialogBuilder.show();
                            });
                            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                            builder.show();
                        }
                    });
                }
            }
        }).start();
    }

    public void LoadFile(String selectedFileName, Context context) {
        Handler handler = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (VailadContect(context)) {
                    try {
                        Response response = apiClient.loadFile(selectedFileName);
                        GenericResponse genericResponse = apiClient.gson.fromJson(response.body().string(), GenericResponse.class);
                        if (response.isSuccessful()) {
                            if (!genericResponse.isState()) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(context, R.string.Loading_failed+"." + genericResponse.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, R.string.Loading_failed, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (IOException e) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, R.string.Loading_failed, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
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
                                        Toast.makeText(context, R.string.Startup_failed+"." + genericResponse.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, R.string.Startup_failed, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (IOException e) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, R.string.Startup_failed, Toast.LENGTH_SHORT).show();
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
                                        Toast.makeText(context, R.string.Stop_failure+"." + genericResponse.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, R.string.Stop_failure, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (IOException e) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, R.string.Stop_failure, Toast.LENGTH_SHORT).show();
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
                                        Toast.makeText(context, R.string.Stop_failure+"." + genericResponse.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, R.string.Stop_failure, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (IOException e) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, R.string.Stop_failure, Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(context, R.string.Failed_to_retrieve, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (IOException e) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, R.string.Failed_to_retrieve, Toast.LENGTH_SHORT).show();
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
                                        Toast.makeText(context, R.string.Clear_failed+"." + genericResponse.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, R.string.Clear_failed, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (IOException e) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, R.string.Clear_failed, Toast.LENGTH_SHORT).show();
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
                                        Toast.makeText(context, R.string.Failed_to_set_DA+"."+ genericResponse.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, R.string.Failed_to_set_DA, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (IOException e) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, R.string.Failed_to_set_DA, Toast.LENGTH_SHORT).show();
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
                                        Toast.makeText(context, R.string.Failed_to_set_DO+"." + genericResponse.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                                switch1.setChecked(!value);
                            }
                        } else {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, R.string.Failed_to_set_DO, Toast.LENGTH_SHORT).show();
                                }
                            });
                            switch1.setChecked(!value);
                        }
                    } catch (IOException e) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, R.string.Failed_to_set_DO, Toast.LENGTH_SHORT).show();
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
                                        Toast.makeText(context, R.string.FTC_calibration_failed+"." + genericResponse.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, R.string.FTC_calibration_failed, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (IOException e) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, R.string.FTC_calibration_failed, Toast.LENGTH_SHORT).show();
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
                                        Toast.makeText(context, R.string.FTC_follow_failure+"." + genericResponse.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, R.string.FTC_follow_failure, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (IOException e) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, R.string.FTC_follow_failure, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        }).start();
    }

    public void FTCStop(Context context) {
        Handler handler = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (VailadContect(context)) {
                    try {
                        Response response = apiClient.FTCStop();
                        GenericResponse genericResponse = apiClient.gson.fromJson(response.body().string(), GenericResponse.class);
                        if (response.isSuccessful()) {
                            if (!genericResponse.isState()) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(context, R.string.FTC_failure_to_stop+"." + genericResponse.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, R.string.FTC_failure_to_stop, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (IOException e) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, R.string.FTC_failure_to_stop, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        }).start();
    }

    public void FTCMove(Context context, int distance, int speed) {
        Handler handler = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (VailadContect(context)) {
                    try {
                        Response response = apiClient.FTCMove(distance, speed);
                        GenericResponse genericResponse = apiClient.gson.fromJson(response.body().string(), GenericResponse.class);
                        if (response.isSuccessful()) {
                            if (!genericResponse.isState()) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(context, R.string.FTC_move_failure+"." + genericResponse.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, R.string.FTC_move_failure, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (IOException e) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, R.string.FTC_move_failure, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        }).start();
    }
}