package com.study.httpFileServer;

import org.apache.log4j.PropertyConfigurator;

import java.io.File;

public class Main {
    public static void main(String[] args) throws InitServerPortException, InterruptedException {
        if (args != null && args.length > 0 && args[0] != null) {
            initLog4j(args[0]);
        }
        new HttpFileServerFrame();
    }

    public static void initLog4j(String fileName) {
        File file = new File(fileName);
        if (file.exists() && file.isFile()) {
            PropertyConfigurator.configure(fileName);
            return;
        }
        file = new File("conf/server-log.properties");
        if (file.exists() && file.isFile()) {
            PropertyConfigurator.configure(file.getAbsolutePath());
        }
    }
}
