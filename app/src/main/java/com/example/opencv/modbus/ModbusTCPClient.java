package com.example.opencv.modbus;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.example.opencv.Constant;
import com.example.opencv.Utils.ProgressBarUtils;
import com.example.opencv.device.InfoService;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


public class ModbusTCPClient {
    private static final ModbusTCPClient INSTANCE = new ModbusTCPClient();
    private static final String TAG = "ModbusTCPClient";
    public final AtomicBoolean isConnected = new AtomicBoolean(false);
    public final AtomicBoolean isFiletransport = new AtomicBoolean(false);
    private final AtomicInteger transactionId = new AtomicInteger(0);
    private final Object requesrlock = new Object();
    private final Object responserlock = new Object();
    public List<Integer> deviceInfo = new CopyOnWriteArrayList<>();
    public List<Integer> AxisInfo = new CopyOnWriteArrayList<>();
    private int unitId = 0;
    public String ConnectDeviceId = "";
    private Socket socket;
    private BufferedInputStream inputStream;
    private BufferedOutputStream outputStream;
    private String LastHost = "";
    private int LastPort = 0;

    /**
     * 创建一个ModbusTCPClient实例
     */
    private ModbusTCPClient() {

    }

    /**
     * 获取ModbusTCPClient的实例
     *
     * @return ModbusTCPClient的实例
     */
    public static ModbusTCPClient getInstance() {
        return INSTANCE;
    }

    /**
     * 连接Modbus TCP设备
     *
     * @param host   Modbus TCP设备的IP地址
     * @param port   Modbus TCP设备的端口号
     * @param unitId Modbus TCP设备的单元ID
     * @throws ModbusException 连接失败时抛出
     */
    public void connect(String host, int port, int unitId) throws ModbusException {
        connect(5000, host, port, unitId);
    }

    /**
     * 连接Modbus TCP设备
     *
     * @param timeout 连接超时时间（毫秒）
     * @param host    Modbus TCP设备的IP地址
     * @param port    Modbus TCP设备的端口号
     * @param unitId  Modbus TCP设备的单元ID
     * @throws ModbusException 连接失败时抛出
     */
    public void connect(int timeout, String host, int port, int unitId) throws ModbusException {
        this.unitId = unitId;
        synchronized (requesrlock) {
            try {
                if (isConnected.get()) {
                    // 如果不是同一个设备，断开当前连接
                    disconnect();
                }
                // 创建一个新的Socket对象
                socket = new Socket(host, port);
                socket.setSoTimeout(timeout);
                socket.setSendBufferSize(1024 * 1024);
                socket.setTcpNoDelay(true);
                socket.setReuseAddress(true);
                inputStream = new BufferedInputStream(socket.getInputStream());
                outputStream = new BufferedOutputStream(socket.getOutputStream());
                LastHost = host;
                LastPort = port;
                isConnected.set(true);
            } catch (IOException e) {
                disconnect();
                throw new ModbusException("Connection failed: " + e.getMessage());
            }
        }
    }

    /**
     * 断开Modbus TCP连接
     */
    public void disconnect() {
        synchronized (requesrlock) {
            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Disconnect error: " + e.getMessage());
            }
            // 将连接状态设置为false
            isConnected.set(false);
        }
    }

    /**
     * 检查Modbus TCP连接是否已经建立
     *
     * @throws ModbusException 如果连接还没有建立
     */
    private void validateConnection() throws ModbusException {
        if (!isConnected.get()) {
            throw new ModbusException("Not connected");
        }
    }

    /**
     * 从流中读取expectedSize个字节
     *
     * @param in           输入流
     * @param expectedSize 期望的字节数
     * @return 读取的字节
     * @throws IOException 读取时发生IO错误
     */
    public static byte[] readBytes(BufferedInputStream in, int expectedSize) throws IOException {
        // 创建一个字节数组，用于存储读取的字节
        byte[] data = new byte[expectedSize];
        int totalRead = 0; // 记录已经读取的字节数

        // 读取流，直到读取了expectedSize个字节
        while (totalRead < expectedSize) {
            int remaining = expectedSize - totalRead; // 剩余需要读取的字节数
            // 从流中读取数据到数组的指定位置
            int bytesRead = in.read(data, totalRead, remaining);
            if (bytesRead == -1) {
                // 流已结束，但未读取足够数据
                throw new IOException("Unexpected end of stream");
            }
            totalRead += bytesRead;
        }
        return data;
    }

    /**
     * 读取Modbus TCP响应
     *
     * @param expectedFunction 读取的Modbus函数码
     * @return 读取的寄存器值
     * @throws IOException     读取时发生IO错误
     * @throws ModbusException 读取的响应格式错误
     */
    private List<Integer> Response(int expectedFunction) throws ModbusException, IOException {
        synchronized (responserlock) {
            try {
                byte[] header = readBytes(inputStream, ModBuscode.MbapFrameLen);
                List<Integer> data = new ArrayList<>();
                try {
                    List<Integer> mbapHeader = ModBuscode.decodeMbapFrame(header);
                    byte[] pdu = readBytes(inputStream, mbapHeader.get(2) - ModBuscode.UnitIdLen);
                    switch (expectedFunction) {
                        case ModBuscode.ReadFunCode:
                            data = ModBuscode.decodeReadReg(pdu);
                            break;
                        case ModBuscode.WriteFunCode:
                            data = ModBuscode.decodeWriteReg(pdu);
                            break;
                        case ModBuscode.FileFunCode:
                            data = ModBuscode.decodeFileTransport(pdu);
                            break;
                    }
                } catch (ModBuscode.ModbusFrameException e) {
                    throw new ModbusException(e.getMessage());
                }
                return data;
            } catch (IOException e) {
                throw new IOException(e.getMessage());
            }
        }
    }

    /**
     * 读取Modbus TCP寄存器
     *
     * @param startAddress 读取的寄存器起始地址
     * @param quantity     读取寄存器的数量
     * @return 读取的寄存器值
     * @throws IOException     读取时发生IO错误
     * @throws ModbusException 读取的响应格式错误
     */
    public List<Integer> readReg(int startAddress, int quantity, int retryCount) throws ModbusException {
        validateConnection();
        byte[] request;
        try {
            request = ModBuscode.encodeReadReg(transactionId.incrementAndGet(), unitId, startAddress, quantity);
        } catch (ModBuscode.ModbusFrameException e) {
            throw new ModbusException(e.getMessage());
        }
        synchronized (requesrlock) {
            try {
                outputStream.write(request);
                outputStream.flush();
                return Response(ModBuscode.ReadFunCode);
            } catch (IOException e) {
                if (retryCount < 3) { // 限制最多重试3次
                    if (Reconnect()) {
                        // 重试时递增计数器
                        return readReg(startAddress, quantity, retryCount + 1);
                    } else {
                        disconnect();
                        throw new ModbusException("重连失败: " + e.getMessage());
                    }
                } else {
                    disconnect();
                    throw new ModbusException("超过最大重试次数(3次): " + e.getMessage());
                }
            }
        }
    }

    public List<Integer> readReg(int startAddress, int quantity) throws ModbusException {
        // 外部调用入口，初始化重试次数为0
        return readReg(startAddress, quantity, 0);
    }

    public boolean Reconnect() {
        disconnect();
        Log.d("fileaddr", "进入重连");
        int MaxRetry = 3;
        int retryCount = 0;
        while (retryCount < MaxRetry) {
            try {
                connect(LastHost, LastPort, unitId);
                if (isConnected.get() == true) {
                    return true;
                }
            } catch (ModbusException e) {
                Log.d("fileaddr", "重连失败: " + e.getMessage());
            }
            // 增加延迟避免高频重试
            try {
                Thread.sleep(500); // 间隔1秒
            } catch (InterruptedException ignored) {
            }
            retryCount++;
        }
        return false;
    }


    /**
     * 验证写寄存器的响应
     *
     * @param data         响应数据，包含起始地址和寄存器数量
     * @param startAddress 写操作的起始地址
     * @param quantity     写入的寄存器数量
     * @throws ModbusException 如果起始地址或数量不匹配
     */
    private void validateWriteResponse(List<Integer> data, int startAddress, int quantity)
            throws ModbusException {
        if (data.get(0) != startAddress || data.get(1) != quantity) {
            throw new ModbusException("Write validation failed");
        }
    }

    /**
     * 写Modbus寄存器
     *
     * @param startAddr 写寄存器的起始地址
     * @param values    写寄存器的值
     * @throws ModbusException 如果通信错误或写寄存器的响应格式错误
     */
    public void writeReg(int startAddr, List<Integer> values, int retryCount) throws ModbusException {
        validateConnection();
        byte[] request;
        try {
            request = ModBuscode.encodeWriteReg(transactionId.incrementAndGet(), unitId, startAddr, values);
        } catch (ModBuscode.ModbusFrameException e) {
            throw new ModbusException(e.getMessage());
        }
        synchronized (requesrlock) {
            try {
                outputStream.write(request);
                outputStream.flush();
                List<Integer> response = Response(ModBuscode.WriteFunCode);
                validateWriteResponse(response, startAddr, values.size());
            } catch (IOException e) {
                if (retryCount < 3) { // 限制最多重试3次
                    if (Reconnect()) {
                        // 重试时递增计数器
                        writeReg(startAddr, values, retryCount + 1);
                    } else {
                        disconnect();
                        throw new ModbusException("重连失败: " + e.getMessage());
                    }
                } else {
                    disconnect();
                    throw new ModbusException("超过最大重试次数(3次): " + e.getMessage());
                }
            }
        }
    }

    public void writeReg(int startAddr, List<Integer> values) throws ModbusException {
        // 外部调用入口，初始化重试次数为0
        writeReg(startAddr, values, 0);
    }

    private void validateFileTransportResponse(List<Integer> data, int fileAddress,
                                               int quantity) throws ModbusException {
        if (data.get(0) != fileAddress || data.get(1) != quantity) {
            throw new ModbusException("FileTransport validation failed");
        }
    }

    /**
     * 写Modbus TCP文件传输
     *
     * @param fileAddr 写文件传输的起始地址
     * @param byteData 写入的文件数据
     * @throws ModbusException 如果通信错误或写文件传输的响应格式错误
     */
    public void FileTransportByte(int fileAddr, byte[] byteData, int retryCount) throws ModbusException {
        synchronized (requesrlock) {
        validateConnection();
        byte[] request;
        try {
            request = ModBuscode.encodeFileTransport(transactionId.incrementAndGet(), unitId, fileAddr, byteData);
        } catch (ModBuscode.ModbusFrameException e) {
            throw new ModbusException(e.getMessage());
        }
            try {
                outputStream.write(request);
                outputStream.flush();
                List<Integer> response = Response(ModBuscode.FileFunCode);
                validateFileTransportResponse(response, fileAddr, byteData.length);
                //Log.d("fileaddr", "fileaddr"+Integer.toUnsignedString(fileAddr));
            } catch (IOException e) {
                if (retryCount < 3) { // 限制最多重试3次
                    if (Reconnect()) {
                        // 重试时递增计数器
                        FileTransportByte(fileAddr, byteData, retryCount + 1);
                    } else {
                        disconnect();
                        throw new ModbusException("重连失败: " + e.getMessage());
                    }
                } else {
                    disconnect();
                    throw new ModbusException("超过最大重试次数(3次): " + e.getMessage());
                }
            }
        }
    }

    public void FileTransportByte(int fileAddr, byte[] byteData) throws ModbusException {
        // 外部调用入口，初始化重试次数为0
        FileTransportByte(fileAddr, byteData, 0);
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
        // 读取Modbus设备信息寄存器
        List<Integer> readData = readReg(Constant.DeviceStartAddr, Constant.DeviceRegCount);
        // 合并高低字节
        readData = mergeList(readData);
        return readData;
    }

    public List<Integer> ReadMachineInfo() throws ModbusException {
        // 读取Modbus机床信息寄存器
        List<Integer> readData = readReg(Constant.MachineAddr, Constant.MachineRegCount);
        // 合并高低字节
        readData = mergeList(readData);
        return readData;
    }

    public List<Integer> ReadAxisInfo() throws ModbusException {
        // 读取Modbus轴信息寄存器
        List<Integer> readData = readReg(Constant.AxisStartAddr, Constant.AxisRegCount);
        // 合并高低字节
        readData = mergeList(readData);
        return readData;
    }

    public void ControlStop() throws ModbusException {
        List<Integer> value = new ArrayList<>();
        value.add(Constant.Stop);
        value = byteToint(value);
        writeReg(Constant.CommandAddr, value);
    }

    public void ControlBack() throws ModbusException {
        List<Integer> value = new ArrayList<>();
        value.add(Constant.Back);
        value = byteToint(value);
        writeReg(Constant.CommandAddr, value);
    }

    public void ControlBackZero() throws ModbusException {
        List<Integer> value = new ArrayList<>();
        value.add(Constant.BackZero);
        value = byteToint(value);
        writeReg(Constant.CommandAddr, value);
    }

    public void ControlAxisRun(int AxisId, int Speed, int Dir) throws ModbusException {
        List<Integer> value = new ArrayList<>();
        value.add(Constant.AxisRun);
        value.add(AxisId);
        value.add(Speed);
        value.add(Dir);
        value = byteToint(value);
        writeReg(Constant.CommandAddr, value);
    }

    public void ControlDO(int DOId, int DOValue) throws ModbusException {
        List<Integer> value = new ArrayList<>();
        value.add(Constant.DO);
        value.add(DOId);
        value.add(DOValue);
        value = byteToint(value);
        writeReg(Constant.CommandAddr, value);
    }

    public void ControlDA(int DAId, int DAValue) throws ModbusException {
        List<Integer> value = new ArrayList<>();
        value.add(Constant.DA);
        value.add(DAId);
        value.add(DAValue);
        value = byteToint(value);
        writeReg(Constant.CommandAddr, value);
    }

    public void ControlFileStart(String Filename, String MD5) throws ModbusException {
        try {
            List<Integer> value = new ArrayList<>();
            value.add(Constant.FileStart);
            byte[] bytes = Filename.getBytes("GBK");
            for (int i = 0; i < bytes.length; i++) {
                value.add((int) bytes[i]);
            }
//            for (int i = 0; i < Filename.length(); i++) {
//                value.add((int) Filename.charAt(i));
//            }
            value.add((int) '&');
            for (int i = 0; i < MD5.length(); i++) {
                value.add((int) MD5.charAt(i));
            }
            value = byteToint(value);
            writeReg(Constant.CommandAddr, value);
        } catch (
                UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public void ControlFileStop() throws ModbusException {
        List<Integer> value = new ArrayList<>();
        value.add(Constant.FileStop);
        value = byteToint(value);
        writeReg(Constant.CommandAddr, value);
    }

    public void ControlFTC() throws ModbusException {
        List<Integer> value = new ArrayList<>();
        value.add(Constant.Ftc);
        value = byteToint(value);
        writeReg(Constant.CommandAddr, value);
    }

    public void ControlWorkBroder() throws ModbusException {
        List<Integer> value = new ArrayList<>();
        value.add(Constant.WorkBroder);
        value = byteToint(value);
        writeReg(Constant.CommandAddr, value);
    }

    public void ControlOfflineProcess(int val) throws ModbusException {
        List<Integer> value = new ArrayList<>();
        value.add(Constant.OfflineProcess);
        value.add(val);
        value = byteToint(value);
        writeReg(Constant.CommandAddr, value);
    }

    public void FileTransport(final int fileAddr, File file, Context context) throws
            ModbusException {
        int bufferSize = 900;
        AtomicInteger totalBytesSent = new AtomicInteger(0);
        android.os.Handler handler = new Handler(Looper.getMainLooper());
        ProgressBarUtils progressHelper = new ProgressBarUtils();
        //File file = new File(context.getFilesDir(), "largefile.bin");
        try (BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file))) {
            String filename = file.getName();
            String md5String = md5Hex(fis);
            ControlFileStart(filename, md5String);
            byte[] buffer = new byte[bufferSize];
            int bytesRead;
            BufferedInputStream fis1 = new BufferedInputStream(new FileInputStream(file));
            handler.post(new Runnable() {
                @Override
                public void run() {
                    progressHelper.showProgressDialog(context); // 显示对话框
                }
            });
            while ((bytesRead = fis1.read(buffer)) != -1) {
                // 判断是否为最后一次读取（可能不足缓冲区大小）
                byte[] aligendBytes = new byte[bytesRead];
                System.arraycopy(buffer, 0, aligendBytes, 0, bytesRead);

                int finalBytesRead = bytesRead;
                FileTransportByte(fileAddr + totalBytesSent.get(), aligendBytes);
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
            ControlFileStop();
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
            Log.d("fileaddr", "ModbusException" + e.getMessage());
            throw new ModbusException(e.getMessage());
        } catch (IOException e) {
            Log.d("fileaddr", "IOException" + e.getMessage());
            throw new ModbusException(e.getMessage());
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

    public void onWriteFailed(Context context, String error) {
        Toast toast = Toast.makeText(context, error + ".请检查连接是否正常", Toast.LENGTH_SHORT);
        toast.show();
    }

    public void onFileFailed(Context context, String error) {
        Toast toast = Toast.makeText(context, "文件传输错误." + error + ".请检查连接是否正常", Toast.LENGTH_SHORT);
        toast.show();
    }

    public interface ModbusCallback<T> {
        void onSuccess(T result);

        void onError(String error);
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
