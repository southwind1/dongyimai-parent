package com.offcn.content.controller;

import com.offcn.entity.Result;
import com.offcn.util.FastDFSClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传Controller
 * @author Administrator
 *
 */
@RestController
public class UploadController {
    //文件服务器地址
    @Value("${FILE_SERVER_URL}")
    private String FILE_SERVER_UR;
    @RequestMapping("/upload")
    public Result upLoad(MultipartFile file){
//        取出文件的扩展名
        String originalFilename = file.getOriginalFilename();
        String extName = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
        try {
//            创建一个FastDFS客户端
            FastDFSClient fastDFSClient = new FastDFSClient("classpath:config\\fdfs_client.conf");
//            进行文件上传，获取文件上传后的地址
            String path = fastDFSClient.uploadFile(file.getBytes(), extName);
//          添加ip组成真正的地址
            String url = FILE_SERVER_UR +path;
            return new Result(true,url);

        }catch (Exception e){
            e.printStackTrace();
            return new Result(false,"上传失败");
        }
    }
}
