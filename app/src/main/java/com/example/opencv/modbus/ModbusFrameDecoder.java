package com.example.opencv.modbus;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class ModbusFrameDecoder extends ByteToMessageDecoder {
    // MBAP Header 固定长度为7字节（2字节事务标识 + 2字节协议标识 + 2字节长度 + 1字节单元标识）
    private static final int MBAP_HEADER_LENGTH = 7;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < MBAP_HEADER_LENGTH) {
            return;
        }
        in.markReaderIndex();
        // 跳过前4个字节（事务ID和协议ID）
        in.skipBytes(4);
        // 读取长度字段（2字节），表示后面字节数（包括 unitId）
        int length = in.readUnsignedShort();
        // 判断剩余字节是否足够
        if (in.readableBytes() < length) {
            in.resetReaderIndex();
            return;
        }
        // 将整个帧（MBAP header + 后续数据）读取出来
        in.resetReaderIndex();
        int frameLength = MBAP_HEADER_LENGTH + length - 1; // 因为 MBAP header 中 length 包含 unitId
        ByteBuf frame = in.readBytes(frameLength);
        out.add(frame);
    }
}

