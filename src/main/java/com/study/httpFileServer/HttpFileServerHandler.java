package com.study.httpFileServer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.CharsetUtil;
import org.apache.log4j.Logger;

import javax.activation.MimetypesFileTypeMap;
import java.io.*;
import java.net.URLDecoder;
import java.util.regex.Pattern;

public class HttpFileServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final String url;
    public static final Integer CHUNK_SIZE = 8192;
    private static Logger logger = Logger.getLogger(HttpFileServerHandler.class);

    public HttpFileServerHandler(String url) {
        this.url = url;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        cause.printStackTrace();
        if (ctx.channel().isActive())
            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }

    private static final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");

    private String sanitizeUri(String uri) {
        try {
            uri = URLDecoder.decode(uri, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            try {
                uri = URLDecoder.decode(uri, "ISO-8859-1");
            } catch (UnsupportedEncodingException e1) {
                throw new Error();
            }
        }


        if (!uri.startsWith("/"))
            return null;

        uri = uri.replace('/', File.separatorChar);
        if (uri.contains(File.separator + '.') || uri.contains('.' + File.separator)) {
            return null;
        }
        return url + File.separator + uri;
    }

    private static final Pattern ALLOWED_FILE_NAME = Pattern.compile("[\\w\\W]*");

    private static void sendListing(ChannelHandlerContext ctx, File dir, String uri) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
//        response.headers().set("CONNECT_TYPE", "text/html;charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html;charset=UTF-8");

        String dirPath = dir.getPath();
        StringBuilder buf = new StringBuilder();

        buf.append("<!DOCTYPE html>\r\n");
        buf.append("<html><head><title>");

        buf.append("目录:");
        buf.append(dirPath);
        buf.append("</title></head><body>\r\n");

        buf.append("<h3>");
        buf.append(" 目录：").append(dirPath);
        buf.append("</h3>\r\n");
        buf.append("<ul>");
        String nowUri = uri;
        if (!uri.equals("/") || !uri.equals("")) {
            uri = uri.substring(0, uri.lastIndexOf("/"));
            uri = uri.substring(0, uri.lastIndexOf("/") + 1);
            buf.append("<li><a href=\"" + uri + "\">....</a></li>\r\n");
        }

        for (File f : dir.listFiles()) {
            if (f.isHidden() || !f.canRead()) {
                continue;
            }
            String name = f.getName();
            if (!ALLOWED_FILE_NAME.matcher(name).matches()) {
                continue;
            }

            buf.append("<li><a href=\"");
            buf.append(name);
            buf.append("\">");
            buf.append(name);
            buf.append("</a></li>\r\n");
        }
        buf.append("</ul>");
        buf.append("</body></html>\r\n");

        ByteBuf buffer = Unpooled.copiedBuffer(buf, CharsetUtil.UTF_8);
        response.content().writeBytes(buffer);
        buffer.release();
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }


    private static void sendRedirect(ChannelHandlerContext ctx, String newUri) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND);
//        response.headers().set("LOCATIN", newUri);
        response.headers().set(HttpHeaderNames.LOCATION, newUri);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status,
                Unpooled.copiedBuffer("Failure: " + status.toString() + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html;charset=UTF-8");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private static void setContentTypeHeader(HttpResponse response, File file) {
        MimetypesFileTypeMap mimetypesFileTypeMap = new MimetypesFileTypeMap();
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, mimetypesFileTypeMap.getContentType(file.getPath()));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        if (!request.decoderResult().isSuccess()) {
            sendError(ctx, HttpResponseStatus.BAD_REQUEST); //请求不成功
            return;
        }
        if (request.method() != HttpMethod.GET) {
            sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED); //请求方法必须是get
            return;
        }

        final String uri = request.uri(); //获取请求的uri
        final String path = sanitizeUri(uri); //对请求的uri进行字符编码，设置为utf-8,并添加服务器前缀目录
        if (path == null) {//如果path不存在
            sendError(ctx, HttpResponseStatus.FORBIDDEN); //访问被拒绝
            return;
        }

        File file = new File(path); //获取到指定文件的路径
        if (file.isHidden() || !file.exists()) { //如果文件是隐藏的或者是不存在的
            sendError(ctx, HttpResponseStatus.NOT_FOUND);  //404 not found
            return;
        }
        if (file.isDirectory()) { //如果是目录，则遍历目录下每一个文件,并发送给客户端
            if (uri.endsWith("/")) {
                sendListing(ctx, file, uri); //拼接字符串
            } else {
                //这一步不是必要的，可以通过其他逻辑完成，当然这种方式也是可以的
                sendRedirect(ctx, uri + "/"); //重定向到uri加上一个 /也就是重新回到这里
            }
            return;
        }
        //如果不是文件也不是目录，则发送403 访问被拒绝
        if (!file.isFile()) {
            sendError(ctx, HttpResponseStatus.FORBIDDEN);
            return;
        }

        //文件下载操作
        downloadFile(request, ctx, file);

    }

    public void downloadFile(HttpRequest request, ChannelHandlerContext ctx, File file) throws IOException {
        //文件下载操作
        RandomAccessFile randomAccessFile = null;
        try {
            //创建一个RandomAccessFile文件流
            randomAccessFile = new RandomAccessFile(file, "r");
        } catch (FileNotFoundException e) {
            sendError(ctx, HttpResponseStatus.NOT_FOUND); //如果发生原创说明没有该文件
            return;
        }

        //获取文件总字节大小
        long fileLength = randomAccessFile.length();
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);//创建一个响应对象
        HttpUtil.setContentLength(response, fileLength); //使用httpUtil来设置响应大小
        //setContentLength(response, fileLength);
        setContentTypeHeader(response, file); //设置文件文件响应信息


        if (HttpUtil.isKeepAlive(request)) { //如果是活跃链接
            //保持连接状态
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }


        ctx.write(response);
        ChannelFuture sendFileFuture = null;
        //一次性读取写入8192个字节
        sendFileFuture = ctx.write(new ChunkedFile(randomAccessFile, 0, fileLength, CHUNK_SIZE), ctx.newProgressivePromise());
        sendFileFuture.addListener(new ChannelProgressiveFutureListener() {//添加监听

            @Override
            public void operationComplete(ChannelProgressiveFuture future)
                    throws Exception {
                logger.info("Transfer complete=>" + file);

            }

            @Override
            public void operationProgressed(ChannelProgressiveFuture future,
                                            long progress, long total) throws Exception {
                if (total < 0)
                    logger.debug("Transfer progress: " + progress);
                else
                    logger.debug("Transfer progress: " + progress + "/" + total);
            }
        });

        ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        if (!HttpUtil.isKeepAlive(request))
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);

    }
}
