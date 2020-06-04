package com.study.httpFileServer;


import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class ConsoleServer {
    private static Logger logger;

    public static void main(String[] args) throws IOException, InitServerPortException, InterruptedException {
        if (args != null && args.length > 1 && args[1] != null) {
            Main.initLog4j(args[1]);
        }
        logger = org.apache.log4j.Logger.getLogger(ConsoleServer.class);
        Properties properties = null;
        if (args != null && args.length > 0 && args[0] != null) {
            try {
                properties = initConfig(args[0]);
            } catch (IOException e) {
                e.printStackTrace();
                logger.error(e.getMessage(), e);
                logger.error("缺少配置文件");
            }
        }
        if (properties == null) {
            logger.error("缺少配置文件");
            System.exit(0);
        }
        String rootPath = properties.getProperty("rootPath");
        if (rootPath == null) {
            logger.info("没有rootPath这个配置，将使用当前工作目录作为根目录");
        }
        File file = new File(rootPath);
        if (!file.exists() || !file.isDirectory()) {
            logger.error(rootPath +":不是一个有效的文件夹");
            logger.info("rootPath:"+rootPath+">无效，将使用当前工作目录作为根目录");
        }
        String ports = properties.getProperty("ports");
        if (ports == null) {
            logger.info("没有配置端口");
            System.exit(0);
        }
        String[] portStrings = ports.split(",");
        List<Integer> portList = new ArrayList<>();
        for (String port : portStrings) {
            try {
                portList.add(Integer.parseInt(port));
            } catch (NumberFormatException e) {
                logger.error(e.getMessage(), e);
                System.exit(0);
            }
        }
        if (portList.size() == 0) {
            logger.error("没有可用端口");
            System.exit(0);
        }
        new HttpFileServer().init(portList, rootPath);
    }

    public static Properties initConfig(String fileName) throws IOException {
        File file = new File(fileName);
        Properties properties = null;
        if (file.exists() && file.isFile()) {
            properties = createPropertiesFromFile(file);
            return properties;
        }
        file = new File("conf/server.properties");
        if (file.exists() && file.isFile()) {
            properties = new Properties();
            InputStream in = new FileInputStream(file);
            properties.load(in);
            in.close();
            return properties;
        }
        properties = new Properties();
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("server.properties");
        properties.load(in);
        in.close();
        return properties;
    }

    public static Properties createPropertiesFromFile(File file) throws IOException {
        Properties properties = new Properties();
        InputStream in = new FileInputStream(file);
        if (in != null) {
            properties.load(in);
        }
        return properties;
    }

}
