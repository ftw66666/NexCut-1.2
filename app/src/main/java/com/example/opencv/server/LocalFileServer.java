package com.example.opencv.server;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import fi.iki.elonen.NanoHTTPD;

public class LocalFileServer extends NanoHTTPD {

    private final ContentResolver contentResolver;
    // 使用一个 Map 来存储已授权的 Uri
    private final Map<String, Uri> authorizedUris = new HashMap<>();

    public LocalFileServer(Context context, int port) {
        super(port);
        this.contentResolver = context.getContentResolver();
    }

    // 这是暴露给你的 Activity 的方法，用于授权一个 Uri
    public String authorizeUri(Uri uri) {
        // 生成一个唯一的 ID
        String fileId = UUID.randomUUID().toString();
        authorizedUris.put(fileId, uri);
        // 返回一个本地可访问的 URL
        return "http://127.0.0.1:" + getListeningPort() + "/" + fileId;
    }

    @Override
    public Response serve(IHTTPSession session) {
        // 获取请求路径中的 fileId (例如 /a1b2c3d4-...)
        String fileId = session.getUri().substring(1);

        // 检查这个 ID 是否已被授权
        if (authorizedUris.containsKey(fileId)) {
            try {
                Uri uri = authorizedUris.get(fileId);
                String mimeType = contentResolver.getType(uri);
                InputStream inputStream = contentResolver.openInputStream(uri);

                // 以流式响应返回，内存占用极小！
                return newChunkedResponse(Response.Status.OK, mimeType, inputStream);

            } catch (Exception e) {
                e.printStackTrace();
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Failed to serve file.");
            }
        } else {
            // 如果 ID 无效，返回 404 Not Found
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "File not found or not authorized.");
        }
    }
}