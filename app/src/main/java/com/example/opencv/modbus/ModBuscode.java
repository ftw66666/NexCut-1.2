package com.example.opencv.modbus;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class ModBuscode {
    public static final int ReadFunCode = 0x03;
    public static final int WriteFunCode = 0x10;
    public static final int FileFunCode = 0x26;
    public static final int ErrorFunCode = 0x80;
    public static final int MbapFrameLen = 7;
    public static final int MbapCountLen = 2;
    public static final int UnitIdLen = 1;
    public static final int FunCodeLen = 1;
    public static final int RegAddrLen = 2;
    public static final int RegDataLen = 2;
    public static final int RegCountLen = 2;
    public static final int ReadResponseCountLen = 1;
    public static final int WriteRequestCountLen = 1;
    public static final int FileAddrLen = 4;
    public static final int FileDataCountLen = 2;
    public static final int FileDataLen = 1;

    /**
     * Modbus TCP帧解析异常
     */
    public static class ModbusFrameException extends Exception {
        /**
         * 构造函数
         *
         * @param message 异常信息
         */
        public ModbusFrameException(String message) {
            super(message);
        }
    }

    /**
     * 生成Modbus TCP MBAP头
     *
     * @param transactionId int 事务ID
     * @param unitId        int 单元ID
     * @param pduLength     int pdu的长度
     * @return byte[] 生成MBAP头
     */
    private static byte[] encodeMbapFrame(int transactionId, int unitId, int pduLength) {
        ByteBuffer buffer = ByteBuffer.allocate(MbapFrameLen)
                .order(ByteOrder.BIG_ENDIAN)
                .putShort((short) transactionId) // 事务ID
                .putShort((short) 0x0000)        // 协议标识符
                .putShort((short) (pduLength + 1)) // 长度（包含单元ID）
                .put((byte) unitId);            // 单元ID
        return buffer.array();
    }

    /**
     * 解析Modbus TCP MBAP头
     *
     * @param frame byte[] modbus tcp帧
     * @return List<Integer> MBAP头中的事务ID、单元ID、长度
     * @throws ModbusFrameException 如果帧太短
     */
    public static List<Integer> decodeMbapFrame(byte[] frame) throws ModbusFrameException {
        if (frame.length < MbapFrameLen) {
            throw new ModbusFrameException("Frame too short (" + frame.length + " bytes)");
        }
        ByteBuffer buffer = ByteBuffer.wrap(frame).order(ByteOrder.BIG_ENDIAN);
        List<Integer> mbapHeader = new ArrayList<>();
        mbapHeader.add(buffer.getShort() & 0xFFFF); // 事务ID
        mbapHeader.add(buffer.get(6) & 0xFF); // 单元ID
        mbapHeader.add(buffer.getShort(4) & 0xFFFF); // 长度（包含单元ID）
        return mbapHeader;
    }

    /**
     * 检查Modbus返回值是否包含异常信息
     *
     * @param pdu byte Modbus返回的函数码
     * @throws ModbusFrameException 如果包含异常信息
     */
    private static void checkException(byte[] pdu) throws ModbusFrameException {
        if ((pdu[0] & ErrorFunCode) != 0) {
            String temp = "";
            switch (pdu[1]) {
                case 0x01:
                    temp = "非法功能码";
                    break;
                case 0x02:
                    temp = "非法数据地址";
                    break;
                case 0x03:
                    temp = "非法数据值";
                    break;
                case 0x04:
                    temp = "文件信息错误";
                    break;
                case 0x05:
                    temp = "文件存储错误";
                    break;
                default:
                    break;
            }
            throw new ModbusFrameException(temp);
        }
    }

    /**
     * 生成Modbus读取寄存器的PDU
     *
     * @param transactionId int 事务ID
     * @param unitId        int 单元ID
     * @param regAddr       int 寄存器地址
     * @param quantity      int 读取的寄存器数量
     * @return byte[] 生成的PDU
     * @throws ModbusFrameException 如果寄存器数量超出范围
     */
    public static byte[] encodeReadReg(
            int transactionId,
            int unitId,
            int regAddr,
            int quantity
    ) throws ModbusFrameException {
        if (quantity < 1 || quantity > 1200) {
            throw new ModbusFrameException("Quantity out of range (1-1200)");
        }

        ByteBuffer pdu = ByteBuffer.allocate(FunCodeLen + RegAddrLen + RegCountLen)
                .put((byte) ReadFunCode) // 读取寄存器函数码
                .putShort((short) regAddr) // 寄存器地址
                .putShort((short) quantity); // 读取的寄存器数量

        return ByteBuffer.allocate(MbapFrameLen + FunCodeLen + RegAddrLen + RegCountLen)
                .put(encodeMbapFrame(transactionId, unitId, FunCodeLen + RegAddrLen + RegCountLen))
                .put(pdu.array())
                .array();
    }

    /**
     * 解析Modbus读取寄存器的PDU
     *
     * @param pdu byte[] PDU
     * @return List<Integer> 读取的寄存器值
     * @throws ModbusFrameException 如果PDU为空或寄存器数量不匹配
     */
    public static List<Integer> decodeReadReg(byte[] pdu) throws ModbusFrameException {
        if (pdu.length < 1) throw new ModbusFrameException("Empty PDU");
        // 检查Modbus返回值是否包含异常信息
        checkException(pdu);
        if ((pdu[0] & 0xFF) != ReadFunCode) {
            throw new ModbusFrameException("Unexpected function code");
        }
        int byteCount = pdu[1] & 0xFF;
        if (pdu.length != FunCodeLen + ReadResponseCountLen + byteCount) {
            throw new ModbusFrameException("Byte count不匹配");
        }
        List<Integer> registers = new ArrayList<>();
        for (int i = 0; i < byteCount; i += RegDataLen) {
            int pos = FunCodeLen + ReadResponseCountLen + i;
            int data = 0;
            for (int j = 0; j < RegDataLen; j++) {
                // 读取寄存器值
                data += (0xFF & pdu[pos + j]) << ((RegDataLen - j - 1) * 8);
            }
            registers.add(data);
        }
        return registers;
    }

    /**
     * 编码Modbus写寄存器的PDU
     *
     * @param transactionId int 事务ID
     * @param unitId        int 单元ID
     * @param regAddr       int 寄存器地址
     * @param values        List<Integer> 寄存器值
     * @return byte[]        PDU
     * @throws ModbusFrameException 如果寄存器值数量不在1-1200范围内
     */
    public static byte[] encodeWriteReg(
            int transactionId,
            int unitId,
            int regAddr,
            List<Integer> values
    ) throws ModbusFrameException {
        if (values.isEmpty() || values.size() > 1200) {
            throw new ModbusFrameException("Values count out of range (1-1200)");
        }
        // 分配PDU缓存
        ByteBuffer pdu = ByteBuffer.allocate(
                FunCodeLen + RegAddrLen + RegCountLen + WriteRequestCountLen + values.size() * RegDataLen);
        // 依次写入PDU
        pdu.put((byte) WriteFunCode) // 函数码
                .putShort((short) regAddr) // 寄存器地址
                .putShort((short) values.size()) // 寄存器数量
                .put((byte) (values.size() * RegDataLen)); // 字节数量

        for (int value : values) {
            pdu.putShort((short) value); // 寄存器值
        }

        // 生成MBAP头
        return ByteBuffer.allocate(MbapFrameLen + pdu.position())
                .put(encodeMbapFrame(transactionId, unitId, pdu.position())) // MBAP头
                .put(pdu.array(), 0, pdu.position()) // PDU
                .array();
    }

    /**
     * 解码Modbus写寄存器的PDU
     *
     * @param pdu byte[] PDU
     * @return List<Integer> 解码后的寄存器地址和数量
     * @throws ModbusFrameException 如果PDU太短或寄存器数量不在1-1200范围内
     */
    public static List<Integer> decodeWriteReg(byte[] pdu) throws ModbusFrameException {
        if (pdu.length < 1) throw new ModbusFrameException("Empty PDU");
        checkException(pdu);
        if ((pdu[0] & 0xFF) != WriteFunCode) {
            throw new ModbusFrameException("Unexpected function code");
        }
        int regAddr = 0;
        int quantity = 0;
        List<Integer> result = new ArrayList<>();
        // 寄存器地址
        for (int i = 0; i < RegAddrLen; i++) {
            regAddr |= (pdu[FunCodeLen + i] << (8 * (RegAddrLen - 1 - i)));
        }
        // 寄存器数量
        for (int i = RegCountLen - 1; i >= 0; i--) {
            quantity |= (pdu[FunCodeLen + RegAddrLen + i] << (8 * (RegDataLen - 1 - i)));
        }
        result.add(regAddr);
        result.add(quantity);
        return result;
    }


    /**
     * 读取寄存器的PDU
     *
     * @param transactionId int  ID
     * @param unitId        int  ID
     * @param FileAddress   int  读取的文件地址
     * @param fileData      byte[]  读取的文件数据
     * @return byte[]  读取寄存器的PDU
     * @throws ModbusFrameException PDU
     */
    public static byte[] encodeFileTransport(
            int transactionId,
            int unitId,
            int FileAddress,
            byte[] fileData
    ) throws ModbusFrameException {
        if (fileData.length < 1 || fileData.length > 1400) {
            throw new ModbusFrameException("fileData  1-1400");
        }
        ByteBuffer pdu = ByteBuffer.allocate(FunCodeLen + FileAddrLen + FileDataCountLen + fileData.length * FileDataLen)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put((byte) FileFunCode)
                .putInt(FileAddress)
                .putShort((short) fileData.length);
        //  fileData  PDU
        for (byte byteData : fileData) {
            pdu.put(byteData);
        }
        //  MBAP
        return ByteBuffer.allocate(MbapFrameLen + pdu.position())
                .put(encodeMbapFrame(transactionId, unitId, pdu.position()))
                .put(pdu.array(), 0, pdu.position())
                .array();
    }

    /**
     * 读取Modbus文件传输的PDU
     *
     * @param pdu byte[]  读取的PDU
     * @return List<Integer>  读取的文件地址和数量
     * @throws ModbusFrameException PDU
     */
    public static List<Integer> decodeFileTransport(byte[] pdu) throws ModbusFrameException {
        if (pdu.length < 1) throw new ModbusFrameException("Empty PDU");
        checkException(pdu);
        if ((pdu[0] & 0xFF) != FileFunCode) {
            throw new ModbusFrameException("Unexpected function code");
        }
        int fileAddress = 0;
        int quantity = 0;
        List<Integer> result = new ArrayList<>();
        //  PDU  fileAddress
        for (int i = 0; i < FileAddrLen; i++) {
            fileAddress |= ((pdu[FunCodeLen + i] & 0xFF) << (8 * i));
        }
        //  PDU  quantity
        for (int i = 0; i < FileDataCountLen; i++) {
            quantity += ((pdu[FunCodeLen + FileAddrLen + i] & 0xFF) << (8 * i));
        }
        result.add(fileAddress);
        result.add(quantity);
        return result;
    }
}


