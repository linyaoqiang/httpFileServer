package com.study.httpFileServer;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.log4j.Logger;

import java.util.List;


public class HttpFileServer {
    public static final String DEFAULT_DIR = System.getProperty("user.dir");
    private EventLoopGroup acceptGroup = new NioEventLoopGroup();
    private EventLoopGroup workerGroup = new NioEventLoopGroup();
    private ServerBootstrap bootstrap = new ServerBootstrap();
    private ChannelFuture future;
    private Logger logger = Logger.getLogger(HttpFileServer.class);

    public void init(List<Integer> ports, String dir) throws InitServerPortException, InterruptedException {
        if (ports == null) {
            throw new InitServerPortException("没有可用的监听端口");
        }
        bootstrap.group(acceptGroup, workerGroup);
        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.option(ChannelOption.SO_BACKLOG, 128);//配置TCP参数,这里是设置缓冲区
        dir = dir == null ? DEFAULT_DIR : dir;
        initHandler(dir);
        initPorts(ports);
        printServerInfo(ports, dir);
    }

    private void initHandler(String path) {
        bootstrap.childHandler(new HttpFileServerChannelInitializer(path));
    }

    private void initPorts(List<Integer> ports) throws InterruptedException {
        for (Integer port : ports) {
            bootstrap.bind(port).sync();
        }
    }

    private void printServerInfo(List<Integer> ports, String path) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("server started in port");
        for (Integer port : ports) {
            stringBuffer.append(" " + port);
        }
        stringBuffer.append("=>");
        stringBuffer.append("the root path :" + path);
        logger.info(stringBuffer.toString());
    }

    public void release() {
        acceptGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        logger.info("server stopped");
    }
}
