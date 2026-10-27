package com.wedding.controller;

import com.wedding.annotation.RequireLogin;
import com.wedding.common.Result;
import com.wedding.exception.BusinessException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// 服务员:文件上传(路径 /api/files)。商户头像、相册、纠纷证据图后续都复用这里。
//
// 安全底线:文件名一律 UUID 重命名,原始文件名只用来取扩展名,绝不参与拼路径(防 ../../ 穿越)。
// 落盘位置 user.dir/uploads/yyyyMM/,由 WebMvcConfig 的 /uploads/** 映射成只读静态访问。
@RestController
@RequestMapping("/api/files")
public class FileController {

    // 扩展名白名单:故意不含 svg / html,那两类能被塞进脚本;mp4 / webm / mov 供案例视频用
    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp", "gif", "pdf", "mp4", "webm", "mov");
    // 视频单列一组:体积天然比图片大一个量级,和图片共用 5MB 上限等于传不了
    private static final Set<String> VIDEO_EXT = Set.of("mp4", "webm", "mov");
    // 图片 / 文档单文件上限 5MB,视频 50MB,与 application.yml 里的 multipart 限制保持一致
    private static final long MAX_SIZE = 5L * 1024 * 1024;
    private static final long MAX_VIDEO_SIZE = 50L * 1024 * 1024;

    // POST /api/files/upload → 上传一个文件,返回可直接访问的相对 url(1 档:登录后即可上传)
    @RequireLogin
    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        // 1. 空文件:表单没带 file 字段时 Spring 会直接 400,这里兜的是"带了字段但内容为空"
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        // 2. 扩展名白名单:取原始文件名最后一个点之后的部分,转小写再比
        String ext = getExtension(file.getOriginalFilename());
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException("只允许上传 jpg/png/webp/gif/pdf/mp4/webm 文件");
        }

        // 3. 大小上限,按类型分档。yml 里的 multipart 取两者中较大的 50MB,
        //    否则 Spring 在进方法前就按默认 1MB 拦掉,轮不到这里给准确提示
        boolean video = VIDEO_EXT.contains(ext);
        if (file.getSize() > (video ? MAX_VIDEO_SIZE : MAX_SIZE)) {
            throw new BusinessException(video ? "视频不能超过 50MB" : "文件不能超过 5MB");
        }

        // 按月分目录:一个目录堆几万个文件时列目录会明显变慢
        String month = new SimpleDateFormat("yyyyMM").format(new Date());
        File dir = new File(System.getProperty("user.dir") + File.separator + "uploads" + File.separator + month);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new BusinessException("上传目录创建失败");
        }

        // UUID 重命名:原始文件名可能带 ../ 、盘符或非法字符,一律不进路径
        String fileName = UUID.randomUUID().toString() + "." + ext;
        File target = new File(dir, fileName);
        try {
            file.transferTo(target);
        } catch (IOException e) {
            throw new BusinessException("文件保存失败");
        }

        // 返回相对路径,前端拼上 baseURL 即可访问;分隔符写死 / ,不跟操作系统走
        Map<String, String> data = new HashMap<>();
        data.put("url", "/uploads/" + month + "/" + fileName);
        return Result.success(data);
    }

    // 取扩展名:没有点、点在开头、点在结尾都当无扩展名,返回空串交给白名单判死
    private String getExtension(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            return "";
        }
        return originalFilename.substring(dot + 1).toLowerCase();
    }
}
