package com.atguigu.gmall.manager.util;

import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
//图片、音视频等文件上传
public class PmsUploadUtil {
    public static String uploadImage(MultipartFile multipartFile) {
        String imgUrl="http://192.168.32.135";
        //上传图片到服务器
        //配置fdfs的全局连接地址
        String tracker= PmsUploadUtil.class.getResource("/tracker.conf").getPath();//获取配置文件路径
        try {
            ClientGlobal.init(tracker);
        } catch (Exception e) {
            e.printStackTrace();
        }
        TrackerClient trackerClient=new TrackerClient();
        //获取一个trackerServer实例
        TrackerServer trackerServer= null;
        try {
            trackerServer = trackerClient.getTrackerServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //通过tracker获取一个storage链接客户端
        StorageClient storageClient=new StorageClient(trackerServer,null);
        try {
            //获取上传的二进制对象
            byte[] bytes= multipartFile.getBytes();
            //获取文件后缀名
            String originalFilename =multipartFile.getOriginalFilename();
            System.out.println("文件格式："+originalFilename);
            int i= originalFilename.lastIndexOf(".");
            String extName=originalFilename.substring(i+1);
            String[] uploadInfos = storageClient.upload_file(bytes,extName,null);

            for (String uploadInfo:uploadInfos){
                imgUrl+="/"+uploadInfo;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return imgUrl;
    }
}
