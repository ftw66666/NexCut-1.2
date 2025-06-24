package com.example.opencv.http;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;

public class ProgressRequestBody extends RequestBody {
    private final RequestBody originalRequestBody;
    private final ProgressListener progressListener;

    public ProgressRequestBody(RequestBody originalRequestBody, ProgressListener progressListener) {
        this.originalRequestBody = originalRequestBody;
        this.progressListener = progressListener;
    }

    @Override
    public MediaType contentType() {
        return originalRequestBody.contentType();
    }

    @Override
    public long contentLength() throws IOException {
        return originalRequestBody.contentLength();
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        // 包装 Sink 以统计写入字节数
        CountingSink countingSink = new CountingSink(sink);
        BufferedSink bufferedSink = Okio.buffer(countingSink);

        // 写入原始数据
        originalRequestBody.writeTo(bufferedSink);
        bufferedSink.flush();
    }

    // 自定义 Sink 用于统计进度
    private class CountingSink extends ForwardingSink {
        private long bytesWritten = 0;

        public CountingSink(Sink delegate) {
            super(delegate);
        }

        @Override
        public void write(Buffer source, long byteCount) throws IOException {
            super.write(source, byteCount);
            bytesWritten += byteCount;
            // 回调进度
            progressListener.onProgress(bytesWritten, contentLength());
        }


    }


    public interface ProgressListener {
        void onProgress(long bytesRead, long contentLength);
    }
}