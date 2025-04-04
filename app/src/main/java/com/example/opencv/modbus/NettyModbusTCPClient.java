package com.example.opencv.modbus;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.example.opencv.Constant;
import com.example.opencv.Utils.ProgressBarUtils;
import com.example.opencv.device.InfoService;


import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.socket.SocketChannel;

import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;

public class NettyModbusTCPClient {
    private static final NettyModbusTCPClient INSTANCE = new NettyModbusTCPClient();
    private static final String TAG = "NettyModbusTCPClient";
    public List<Integer> deviceInfo = new CopyOnWriteArrayList<>();
    public final AtomicBoolean isConnected = new AtomicBoolean(false);
    public final AtomicBoolean isFiletransport = new AtomicBoolean(false);
    private final AtomicInteger transactionId = new AtomicInteger(0);

    // Netty 相关组件
    private NioEventLoopGroup group;
    private Channel channel;
    private ModbusClientHandler modbusHandler;

    private int unitId = 0;
    public String ConnectDeviceId = "";

    private NettyModbusTCPClient() {
    }

    public static NettyModbusTCPClient getInstance() {
        return INSTANCE;
    }

    /**
     * 使用 Netty 连接 Modbus TCP 设备
     */
    public void connect(String host, int port, int unitId, Context context) throws ModbusException {
        this.unitId = unitId;
        if (isConnected.get()) {
            // 如果已连接，则判断是否为同一设备，否则断开重连
            if (channel != null && channel.remoteAddress().toString().contains(host) && channel.remoteAddress().toString().contains(String.valueOf(port))) {
                return;
            }
            disconnect();
        }
        group = new NioEventLoopGroup();
        modbusHandler = new ModbusClientHandler();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group).channel(NioSocketChannel.class)
                // 根据需要设置缓冲区、NoDelay 等选项
                .option(ChannelOption.SO_RCVBUF, 1024 * 1024).option(ChannelOption.TCP_NODELAY, true).option(ChannelOption.SO_KEEPALIVE, true).handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        // 添加自定义解码器：将 ByteBuf 按照 MBAP 帧切割
                        ch.pipeline().addLast(new ModbusFrameDecoder());
                        // 添加业务处理器
                        ch.pipeline().addLast(modbusHandler);
                    }
                });
        try {
            ChannelFuture future = bootstrap.connect(host, port).sync();
            channel = future.channel();
            isConnected.set(true);
        } catch (InterruptedException e) {
            disconnect();
            throw new ModbusException("Connection failed: " + e.getMessage());
        }
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        try {
            if (channel != null) {
                channel.close().sync();
            }
            if (group != null) {
                group.shutdownGracefully().sync();
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Disconnect error: " + e.getMessage());
        }
        isConnected.set(false);
    }

    /**
     * 校验连接状态
     */
    private void validateConnection() throws ModbusException {
        if (!isConnected.get() || channel == null || !channel.isActive()) {
            throw new ModbusException("Not connected");
        }
    }

    /**
     * 发送请求并同步等待响应（超时可自行设定）
     */
    private byte[] sendRequest(byte[] request) throws ModbusException {
        validateConnection();
        try {
            // 发送请求
            ChannelFuture writeFuture = channel.writeAndFlush(Unpooled.wrappedBuffer(request));
            writeFuture.sync();
            // 同步等待响应，这里设置等待 5 秒
            byte[] response = modbusHandler.getResponse(2, TimeUnit.SECONDS);
            if (response == null) {
                throw new ModbusException("Response timeout");
            }
            return response;
        } catch (InterruptedException e) {
            disconnect();
            throw new ModbusException("Communication error: " + e.getMessage());
        }
    }

    public List<Integer> readReg(int startAddress, int quantity) throws ModbusException {
        byte[] request;
        try {
            request = ModBuscode.encodeReadReg(transactionId.incrementAndGet(), unitId, startAddress, quantity);
        } catch (ModBuscode.ModbusFrameException e) {
            throw new ModbusException(e.getMessage());
        }
        byte[] responseBytes = sendRequest(request);
        // 解析响应：先解析 MBAP header，再解析 PDU
        // 这里简化处理，假设 ModBuscode.decodeReadReg 能处理整个 PDU 部分
        try {
            // 跳过 MBAP header（7字节），实际根据 ModBuscode 的实现可能需要调整
            byte[] pdu = new byte[responseBytes.length - 7];
            System.arraycopy(responseBytes, 7, pdu, 0, pdu.length);
            return ModBuscode.decodeReadReg(pdu);
        } catch (ModBuscode.ModbusFrameException e) {
            throw new ModbusException(e.getMessage());
        }
    }

    public void writeReg(final int startAddr, List<Integer> values) throws ModbusException {
        byte[] request;
        try {
            request = ModBuscode.encodeWriteReg(transactionId.incrementAndGet(), unitId, startAddr, values);
        } catch (ModBuscode.ModbusFrameException e) {
            throw new ModbusException(e.getMessage());
        }

        byte[] responseBytes = sendRequest(request);
        try {
            // 同样解析响应 PDU 部分（跳过 MBAP header）
            byte[] pdu = new byte[responseBytes.length - 7];
            System.arraycopy(responseBytes, 7, pdu, 0, pdu.length);
            List<Integer> response = ModBuscode.decodeWriteReg(pdu);
            // 校验写响应
            validateWriteResponse(response, startAddr, values.size());
        } catch (ModBuscode.ModbusFrameException e) {
            throw new ModbusException(e.getMessage());
        }
    }

    private void validateWriteResponse(List<Integer> data, int startAddress, int quantity) throws ModbusException {
        if (data.get(0) != startAddress || data.get(1) != quantity) {
            throw new ModbusException("Write validation failed");
        }
    }

    public void FileTransportByte(final int fileAddr, byte[] byteData) throws ModbusException {
        byte[] request;
        try {
            request = ModBuscode.encodeFileTransport(transactionId.incrementAndGet(), unitId, fileAddr, byteData);
        } catch (ModBuscode.ModbusFrameException e) {
            throw new ModbusException(e.getMessage());
        }
        byte[] responseBytes = sendRequest(request);
        try {
            byte[] pdu = new byte[responseBytes.length - 7];
            System.arraycopy(responseBytes, 7, pdu, 0, pdu.length);
            List<Integer> response = ModBuscode.decodeFileTransport(pdu);
            validateFileTransportResponse(response, fileAddr, byteData.length);
        } catch (ModBuscode.ModbusFrameException e) {
            throw new ModbusException(e.getMessage());
        }
    }

    private void validateFileTransportResponse(List<Integer> data, int fileAddress, int quantity) throws ModbusException {
        if (data.get(0) != fileAddress || data.get(1) != quantity) {
            throw new ModbusException("FileTransport validation failed");
        }
    }

    /**
     * 将一个2字节数据拆分为4字节，按照低字节在前，高字节在后
     * <p>
     * 例如，输入为[0x1234]，那么输出将是[0x34, 0x12]
     *
     * @param value 要拆分的2字节数据
     * @return 拆分后的4字节数据
     */
    public static List<Integer> byteToint(List<Integer> value) {
        List<Integer> values = new ArrayList<>();
        for (int i = 0; i < value.size(); i++) {
            int val = value.get(i);
            // 提取高16位（前2字节）
            values.add(val & 0xFFFF);
            values.add(val >> 16 & 0xFFFF);
            // 提取低16位（后2字节）
        }
        return values;
    }

    /**
     * 合并一个由高低字节组成的List为一个完整的List
     * <p>
     * 例如，输入为[0x34, 0x12, 0x78, 0x56]，那么输出将是[0x1234, 0x5678]
     *
     * @param list 要合并的List
     * @return 合并后的List
     */
    private List<Integer> mergeList(List<Integer> list) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += 2) {
            int val = list.get(i) & 0xFFFF;
            val |= list.get(i + 1) << 16;
            result.add(val);
        }
        return result;
    }

    /**
     * 读取Modbus设备信息
     *
     * @return 读取的设备信息
     * @throws ModbusException 如果通信错误或读取的响应格式错误
     */
    public List<Integer> ReadDeviceInfo() throws ModbusException {
        try {
            // 读取Modbus设备信息寄存器
            List<Integer> readData = readReg(Constant.DeviceStartAddr, Constant.DeviceRegCount);
            // 合并高低字节
            readData = mergeList(readData);
            return readData;
        } catch (ModbusException e) {
            throw new ModbusException("Communication error: " + e.getMessage());
        }
    }

    public void ControlStop() throws ModbusException {
        try {
            List<Integer> value = new ArrayList<>();
            value.add(Constant.Stop);
            value = byteToint(value);
            writeReg(Constant.CommandAddr, value);
        } catch (ModbusException e) {
            throw new ModbusException("Communication error: " + e.getMessage());
        }
    }

    public void ControlBack() throws ModbusException {
        try {
            List<Integer> value = new ArrayList<>();
            value.add(Constant.Back);
            value = byteToint(value);
            writeReg(Constant.CommandAddr, value);
        } catch (ModbusException e) {
            throw new ModbusException("Communication error: " + e.getMessage());
        }
    }

    public void ControlAxisRun(int AxisId, int Speed, int Dir) throws ModbusException {
        try {
            List<Integer> value = new ArrayList<>();
            value.add(Constant.AxisRun);
            value.add(AxisId);
            value.add(Speed);
            value.add(Dir);
            value = byteToint(value);
            writeReg(Constant.CommandAddr, value);
        } catch (ModbusException e) {
            throw new ModbusException("Communication error: " + e.getMessage());
        }
    }

    public void ControlDO(int DOId, int DOValue) throws ModbusException {
        try {
            List<Integer> value = new ArrayList<>();
            value.add(Constant.DO);
            value.add(DOId);
            value.add(DOValue);
            value = byteToint(value);
            writeReg(Constant.CommandAddr, value);
        } catch (ModbusException e) {
            throw new ModbusException("Communication error: " + e.getMessage());
        }
    }

    public void ControlDA(int DAId, int DAValue) throws ModbusException {
        try {
            List<Integer> value = new ArrayList<>();
            value.add(Constant.DA);
            value.add(DAId);
            value.add(DAValue);
            value = byteToint(value);
            writeReg(Constant.CommandAddr, value);
        } catch (ModbusException e) {
            throw new ModbusException("Communication error: " + e.getMessage());
        }
    }

    public void ControlFileStart(String Filename, String MD5) throws ModbusException {
        try {
            List<Integer> value = new ArrayList<>();
            value.add(Constant.FileStart);
            for (int i = 0; i < Filename.length(); i++) {
                value.add((int) Filename.charAt(i));
            }
            value.add((int) '&');
            for (int i = 0; i < MD5.length(); i++) {
                value.add((int) MD5.charAt(i));
            }
            value = byteToint(value);
            writeReg(Constant.CommandAddr, value);
        } catch (ModbusException e) {
            throw new ModbusException("Communication error: " + e.getMessage());
        }
    }

    public void ControlFileStop() throws ModbusException {
        try {
            List<Integer> value = new ArrayList<>();
            value.add(Constant.FileStop);
            value = byteToint(value);
            writeReg(Constant.CommandAddr, value);
        } catch (ModbusException e) {
            throw new ModbusException("Communication error: " + e.getMessage());
        }
    }

    public void ControlFTC() throws ModbusException {
        try {
            List<Integer> value = new ArrayList<>();
            value.add(Constant.Ftc);
            value = byteToint(value);
            writeReg(Constant.CommandAddr, value);
        } catch (ModbusException e) {
            throw new ModbusException("Communication error: " + e.getMessage());
        }
    }

    public void FileTransport(final int fileAddr, File file, Context context) throws ModbusException {
        int bufferSize = 1024;
        AtomicLong totalBytesSent = new AtomicLong(0);
        android.os.Handler handler = new Handler(Looper.getMainLooper());
        ProgressBarUtils progressHelper = new ProgressBarUtils();
        //File file = new File(context.getFilesDir(), "largefile.bin");
        try (BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file))) {
            String filename = file.getName();
            String md5String = md5Hex(fis);
            ControlFileStart(filename, md5String);
            isFiletransport.set(true);
            byte[] buffer = new byte[bufferSize];
            int bytesRead;
            BufferedInputStream fis1 = new BufferedInputStream(new FileInputStream(file));
            handler.post(new Runnable() {
                @Override
                public void run() {
                    progressHelper.showProgressDialog(context); // 显示对话框
                }
            });
            Log.d("TCPtest", "文件传输开始");
            while ((bytesRead = fis1.read(buffer)) != -1) {
                // 判断是否为最后一次读取（可能不足缓冲区大小）
                byte[] aligendBytes = new byte[bytesRead];
                System.arraycopy(buffer, 0, aligendBytes, 0, bytesRead);
                int finalBytesRead = bytesRead;
                try {
                    FileTransportByte(fileAddr, aligendBytes);
                } catch (ModbusException e) {
                    //throw new RuntimeException(e);
                }
                totalBytesSent.addAndGet(finalBytesRead);
                // 计算进度
                int progress = (int) ((totalBytesSent.get() * 100) / file.length());
                // 更新对话框进度
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        progressHelper.updateProgress(progress);
                    }
                });
            }
            Log.d("TCPtest", "文件传输结束");
            ControlFileStop();
            isFiletransport.set(false);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    progressHelper.dismissDialog();
                }
            });
        } catch (ModbusException e) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    progressHelper.dismissDialog();
                }
            });
            throw new ModbusException("Communication error: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onConnected(Context context, String name) {
        Toast toast = Toast.makeText(context, name + "连接成功", Toast.LENGTH_SHORT);
        toast.show();
    }

    public void onConnectionFailed(Context context, String name) {
        Toast toast = Toast.makeText(context, name + "连接失败", Toast.LENGTH_SHORT);
        toast.show();
    }


    public void onReadFailed(Context context) {
        Toast toast = Toast.makeText(context, "读取错误，请检查连接是否正常", Toast.LENGTH_SHORT);
        toast.show();
    }

    public void onWriteFailed(Context context) {
        Toast toast = Toast.makeText(context, "写入错误，请检查连接是否正常", Toast.LENGTH_SHORT);
        toast.show();
    }

    public void onFileFailed(Context context) {
        Toast toast = Toast.makeText(context, "文件传输错误，请检查连接是否正常", Toast.LENGTH_SHORT);
        toast.show();
    }

    public static class ModbusException extends Exception {
        public ModbusException(String message) {
            super(message);
        }

        public static String getExceptionMessage(int code) {
            switch (code) {
                case 0x01:
                    return "Illegal Function";
                case 0x02:
                    return "Illegal Data Address";
                case 0x03:
                    return "Illegal Data Value";
                case 0x04:
                    return "Server Device Failure";
                default:
                    return "Unknown error (" + code + ")";
            }
        }
    }
}
