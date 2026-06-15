package com.example.controller;

import com.example.model.ImagePoint;
import com.example.service.ImageRecognitionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/image")
public class ImageController {

    @Autowired
    private ImageRecognitionService imageRecognitionService;

    /**
     * 图片上传并识别主体坐标
     * @param file 上传的图片文件
     * @return 四个角点的坐标
     */
    @PostMapping("/detect")
    public ResponseEntity<Map<String, Object>> detectImageSubject(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        buildResponse(false, "文件不能为空", null)
                );
            }

            String contentType = file.getContentType();
            if (!isValidImageType(contentType)) {
                return ResponseEntity.badRequest().body(
                        buildResponse(false, "只支持 JPG、PNG、BMP 等图片格式", null)
                );
            }

            ImagePoint corners = imageRecognitionService.detectImageSubject(file);
            
            return ResponseEntity.ok(
                    buildResponse(true, "检测成功", corners)
            );
        } catch (IOException e) {
            log.error("文件读取失败", e);
            return ResponseEntity.badRequest().body(
                    buildResponse(false, "文件读取失败: " + e.getMessage(), null)
            );
        } catch (Exception e) {
            log.error("图片识别失败", e);
            return ResponseEntity.badRequest().body(
                    buildResponse(false, "识别失败: " + e.getMessage(), null)
            );
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "OK"));
    }

    private boolean isValidImageType(String contentType) {
        if (contentType == null) {
            return false;
        }
        return contentType.startsWith("image/jpeg") ||
               contentType.startsWith("image/png") ||
               contentType.startsWith("image/bmp");
    }

    private Map<String, Object> buildResponse(boolean success, String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", message);
        response.put("data", data);
        return response;
    }
}