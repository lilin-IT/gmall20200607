package com.atguigu.gmall.manager;

import org.csource.common.MyException;
import org.csource.fastdfs.*;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallManageWebApplicationTests {

    @Test
    public void contextLoads() throws IOException, MyException {
        //配置fdfs的全局连接地址
        String tracker= GmallManageWebApplicationTests.class.getResource("/tracker.conf").getPath();
        ClientGlobal.init(tracker);
        TrackerClient trackerClient=new TrackerClient();
        //获取一个trackerServer实例
        TrackerServer trackerServer=trackerClient.getTrackerServer();
        //通过tracker获取一个storage链接客户端
        StorageClient storageClient=new StorageClient(trackerServer,null);
        String[] uploadInfos= storageClient.upload_file("C:/Users/94457/Desktop/abc.jpg","jpg",null);
        String url="http://192.168.32.135";
        for (String uploadInfo:uploadInfos){
            System.out.println(uploadInfo);
            url+="/"+uploadInfo;

        }
        System.out.println(url);
    }

}
