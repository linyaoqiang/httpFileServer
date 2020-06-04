package com.study.httpFileServer;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;

public class HttpFileServerChannelInitializer extends ChannelInitializer<SocketChannel> {
    private String path;
    public HttpFileServerChannelInitializer(String path){
        this.path=path;
    }
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {

        ch.pipeline().addLast("http-decoder", new HttpRequestDecoder());
        ch.pipeline().addLast("http-aggregator", new HttpObjectAggregator(65536));
        ch.pipeline().addLast("http-encoder", new HttpResponseEncoder());
        ch.pipeline().addLast("http-chunked", new ChunkedWriteHandler());
        ch.pipeline().addLast("fileServerHandler", new HttpFileServerHandler(path));
    }
}
