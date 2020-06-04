package com.study.httpFileServer;

import jdk.nashorn.internal.scripts.JO;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class HttpFileServerFrame {
    private JFrame frame;
    private JTextField rootPathField;
    private JTextField portsField;
    private JButton startBtn;
    private JButton stopBtn;
    private static final String title = "Http文件服务器";
    private HttpFileServer server;
    private static Logger logger = Logger.getLogger(HttpFileServerFrame.class);

    public HttpFileServerFrame() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame();
        frame.setBounds(100, 100, 538, 307);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle(title);
        frame.getContentPane().setLayout(null);

        JLabel label = new JLabel("http\u6587\u4EF6\u670D\u52A1\u5668");
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setFont(new Font("Microsoft JhengHei", Font.ITALIC, 25));
        label.setBounds(97, 30, 325, 31);
        frame.getContentPane().add(label);

        JLabel rootPathLabel = new JLabel("\u6839\u76EE\u5F55");
        rootPathLabel.setHorizontalAlignment(SwingConstants.CENTER);
        rootPathLabel.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 20));
        rootPathLabel.setBounds(47, 92, 119, 31);
        frame.getContentPane().add(rootPathLabel);

        rootPathField = new JTextField();
        rootPathField.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 15));
        rootPathField.setBounds(180, 92, 242, 30);
        frame.getContentPane().add(rootPathField);
        rootPathField.setColumns(10);

        JLabel lblNewLabel = new JLabel("\u76D1\u542C\u7AEF\u53E3(\u53EF\u4EE5\u7528\u9017\u53F7\u9694\u5F00\u591A\u4E2A)");
        lblNewLabel.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 18));
        lblNewLabel.setBounds(70, 158, 252, 29);
        frame.getContentPane().add(lblNewLabel);

        portsField = new JTextField();
        portsField.setFont(new Font("Microsoft JhengHei", Font.PLAIN, 15));
        portsField.setBounds(336, 160, 142, 31);
        frame.getContentPane().add(portsField);
        portsField.setColumns(10);

        startBtn = new JButton("开启服务器");
        startBtn.setFont(UIManager.getFont("ToolBar.font"));
        startBtn.setBounds(142, 214, 113, 27);
        frame.getContentPane().add(startBtn);

        stopBtn = new JButton("关闭服务器");
        stopBtn.setFont(UIManager.getFont("ToolBar.font"));
        stopBtn.setBounds(277, 214, 113, 27);
        frame.getContentPane().add(stopBtn);
        addEvent();
        frame.setVisible(true);
    }

    private void addEvent() {
        startBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String rootPath = rootPathField.getText();
                String[] portStrs = portsField.getText().split(",");
                List<Integer> ports = new ArrayList<>();
                for (String port : portStrs) {
                    ports.add(Integer.parseInt(port));
                }
                File file = new File(rootPath);
                if (!file.exists() || !file.isDirectory()) {
                    logger.error(rootPath + " :不是一个有效的文件夹");
                    JOptionPane.showMessageDialog(frame, rootPath + " :不是一个有效的文件夹,开启服务器失败");
                    return;
                }
                try {
                    server = new HttpFileServer();
                    server.init(ports, rootPath);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    frame.setTitle("开启服务器失败，请检查");
                    JOptionPane.showMessageDialog(frame,"开启服务器失败，请检查");
                    return;
                }
                frame.setTitle(title + "---开启中");
                JOptionPane.showMessageDialog(frame,title + "---开启中,监听端口:"+ports+" 服务器根目录为:"+rootPath);
            }
        });
        stopBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (server != null) {
                    server.release();
                    JOptionPane.showMessageDialog(frame,"关闭服务器成功");
                }
                frame.setTitle(title);

            }
        });
    }
}
