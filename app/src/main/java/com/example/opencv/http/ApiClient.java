package com.example.opencv.http;

import android.annotation.SuppressLint;

import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiClient {
    public StringBuffer BASE_URL = new StringBuffer();

    public AtomicBoolean isConnected = new AtomicBoolean(false);
    public AtomicBoolean isInfo = new AtomicBoolean(false);
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
    public final Gson gson = new Gson();
    public MachineInfo machineInfo = new MachineInfo();

    public String ConnectDeviceId = "";

    private static class Holder {
        static final ApiClient INSTANCE = new ApiClient();
    }

    public static ApiClient getInstance() {
        return Holder.INSTANCE;
    }

    // region Axis 接口
    public Response moveAxis(int index, int distance, int speed) throws IOException {
        @SuppressLint("DefaultLocale") String json = String.format("{\"Index\":%d,\"Distance\":%d,\"Speed\":%d}",
                index, distance, speed);
        return postRequest("/Axis/Move", json);
    }


    public Response systemOrigin() throws IOException {
        return postRequest("/Axis/SystemOrigin", "");
    }

    public Response zeroReturn() throws IOException {
        return postRequest("/Axis/Zero", "");
    }
    // endregion

    // region File 接口
    public Response uploadGCode(File file) throws IOException {
        MultipartBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(),
                        RequestBody.create(file, MediaType.parse("application/octet-stream")))
                .build();

        Request request = new Request.Builder()
                .url(BASE_URL + "/File/UploadGCode")
                .post(body)
                .build();

        return executeSync(request);
    }

    public Response getLocalFiles() throws IOException {
        return getRequest("/File/LocalList");
    }

    public Response loadFile(String fileName) throws IOException {
        String json = String.format("{\"fileName\":\"%s\"}", fileName);
        return postRequest("/File/LoadFile", json);
    }
    // endregion

    // region 加工控制
    public Response startProcess(boolean empty, int speed) throws IOException {
        @SuppressLint("DefaultLocale") String json = String.format("{\"Empty\":%s,\"Speed\":%d}", empty, speed);
        return postRequest("/Process/Start", json);
    }

    public Response stopProcess() throws IOException {
        return postRequest("/Process/Stop", "");
    }

    public Response pauseProcess() throws IOException {
        return postRequest("/Process/Parse", "");
    }
    // endregion

    // region 机床状态
    public Response getMachineInfo() throws IOException {
        return getRequest("/Machine/Info");
    }

    public Response getWarningStatus() throws IOException {
        return getRequest("/Machine/Warning");
    }

    public Response clearAlarm() throws IOException {
        return postRequest("/Machine/ClearAlarm", "");
    }
    // endregion

    // region IO 控制
    public Response setDA(int index, float value) throws IOException {
        @SuppressLint("DefaultLocale") String json = String.format("{\"Index\":%d,\"Value\":%.2f}", index, value);
        return postRequest("/IO/DA", json);
    }

    public Response setDO(int index, boolean enable) throws IOException {
        @SuppressLint("DefaultLocale") String json = String.format("{\"Index\":%d,\"Enable\":%b}", index, enable);
        return postRequest("/IO/DO", json);
    }
    // endregion

    // region FTC 接口
    public Response calibrateFTC() throws IOException {
        return postRequest("/FTC/Calibration", "");
    }

    public Response enableFollowing() throws IOException {
        return postRequest("/FTC/Follow", "");
    }
    // endregion

    // region 通用请求方法
    private Response getRequest(String path) throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + path)
                .get()
                .build();
        return executeSync(request);
    }

    private Response postRequest(String path, String jsonBody) throws IOException {
        RequestBody body = RequestBody.create(
                jsonBody,
                MediaType.parse("application/json")
        );
        Request request = new Request.Builder()
                .url(BASE_URL + path)
                .post(body)
                .build();
        return executeSync(request);
    }

    public Response executeSync(Request request) throws IOException {
        return client.newCall(request).execute();
    }
}
