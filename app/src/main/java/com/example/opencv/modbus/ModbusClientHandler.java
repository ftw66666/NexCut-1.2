package com.example.opencv.modbus;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class ModbusClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private final BlockingQueue<byte[]> responseQueue = new ArrayBlockingQueue<>(1);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        byte[] bytes = new byte[msg.readableBytes()];
        msg.readBytes(bytes);
        // 将完整响应放入队列，供发送请求方法获取
        responseQueue.offer(bytes);
    }

    /**
     * 同步等待响应（可根据实际情况设置超时时间）
     */
    public byte[] getResponse(long timeout, TimeUnit unit) throws InterruptedException {
        return responseQueue.poll(timeout, unit);
    }
}

