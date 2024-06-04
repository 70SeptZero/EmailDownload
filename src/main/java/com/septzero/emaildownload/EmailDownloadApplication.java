package com.septzero.emaildownload;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import javax.mail.search.FlagTerm;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Scanner;

@SpringBootApplication
public class EmailDownloadApplication {

    public static void main(String[] args) throws MessagingException, IOException {
        //输入显示
        Scanner scanner = new Scanner(System.in);
//
        System.out.print("输入IMAP服务地址: ");
        String imapServer = scanner.nextLine();

        System.out.print("邮箱账号: ");
        String email = scanner.nextLine();

        System.out.print("邮箱密码（未隐藏）: ");
        String password = scanner.nextLine();

        Properties properties = new Properties();
        properties.setProperty("mail.store.protocol", "imaps");
        Session session = Session.getDefaultInstance(properties, null);

        Store store = session.getStore("imaps");
        store.connect(imapServer, email, password);

        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_WRITE);

        System.out.println("登录成功，正在获取邮件。。。。。。");

        Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false)); // 获取所有未读邮件

        //检测输出文件夹是否存在
        File downloadDir = new File("../download");
        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }

        for (Message msg : messages) {
            if (msg instanceof MimeMessage) {
                //获取邮件时间
                Date date = msg.getSentDate();
                Date runDate = new Date();
                // 创建一个 SimpleDateFormat 文件夹名-文件名
                SimpleDateFormat sdfDate = new SimpleDateFormat("yyyyMMdd");
                SimpleDateFormat sdfTime = new SimpleDateFormat("yyyyMMddHHmmss");
                // 使用 SimpleDateFormat 转字符串
                String msgDateTime = sdfDate.format(runDate);
                String msgHmsTime = sdfTime.format(date);


                MimeMessage mimeMessage = (MimeMessage) msg;
                Address[] fromAddresses = mimeMessage.getFrom();
                System.out.println("From: " + fromAddresses[0]);

                Multipart multipart = (Multipart) mimeMessage.getContent();
                //检测输出文件夹是否存在
                File dateDir = new File("../download/" + msgDateTime);
                if (!dateDir.exists()) {
                    dateDir.mkdirs();
                }
                for (int i = 0; i < multipart.getCount(); i++) {
                    BodyPart bodyPart = multipart.getBodyPart(i);
                    if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
                        String fileName = bodyPart.getFileName();
                        String encodedFileName = URLEncoder.encode(fileName, "UTF-8");
                        String decodedFileName = URLDecoder.decode(encodedFileName, "UTF-8");
                        String finalFileName = MimeUtility.decodeText(decodedFileName);
                        String splFileName = msgHmsTime + "_" + finalFileName;
                        System.out.println("附件: " + splFileName + " 下载中。。。。。。");
                        String downloadPath = dateDir + "/" + splFileName;
                        File file = new File(downloadPath);
                        Path testPath = file.toPath().toAbsolutePath();
                        System.out.println("当前下载路径：" + testPath);
                        FileOutputStream outputStream = new FileOutputStream(file);
                        IOUtils.copy(bodyPart.getInputStream(), outputStream);
                        outputStream.close();
                        System.out.println("附件: " + splFileName + " 下载完成");
                    }
                }

                // 标记该邮件为已读
                msg.setFlag(Flags.Flag.SEEN, true);
            }
        }

        inbox.close(false);
        store.close();
    }

}
