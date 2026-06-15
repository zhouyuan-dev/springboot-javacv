# Spring Boot + JavaCV 图片识别项目

本项目实现了使用 Spring Boot 和 JavaCV 进行图片处理和主体识别的功能。

## 功能特性

✅ **功能1 - 读取图片**
- 支持 MultipartFile 上传
- 自动转换为 OpenCV Mat 对象
- 支持 JPG、PNG、BMP 等格式

✅ **功能2 - 识别主体坐标**
- Canny 边界检测
- 轮廓查找与分析
- 返回四个角点的绝对坐标（左上、右上、右下、左下）

## 技术栈

- Spring Boot 2.7.14
- JavaCV 1.5.9
- OpenCV 4.5.2
- Lombok
- Commons IO

## 项目结构

```
src/
├── main/
│   ├── java/com/example/
│   │   ├── Application.java           # 应用入口
│   │   ├── model/
│   │   │   └── ImagePoint.java        # 坐标数据模型
│   │   ├── service/
│   │   │   └── ImageRecognitionService.java  # 图片识别服务
│   │   └── controller/
│   │       └── ImageController.java   # REST API 控制器
│   └── resources/
│       └── application.yml            # 配置文件
└── pom.xml                            # Maven 依赖配置
```

## 快速开始

### 1. 构建项目

```bash
mvn clean install
```

### 2. 运行应用

```bash
mvn spring-boot:run
```

应用将在 `http://localhost:8080` 启动

### 3. API 调用

#### 上传图片并识别主体坐标

```bash
curl -X POST \
  -F "file=@/path/to/image.jpg" \
  http://localhost:8080/api/image/detect
```

#### 响应示例

```json
{
  "success": true,
  "message": "检测成功",
  "data": {
    "topLeft": {
      "x": 100.0,
      "y": 150.0
    },
    "topRight": {
      "x": 800.0,
      "y": 150.0
    },
    "bottomRight": {
      "x": 800.0,
      "y": 600.0
    },
    "bottomLeft": {
      "x": 100.0,
      "y": 600.0
    }
  }
}
```

#### 健康检查

```bash
curl http://localhost:8080/api/image/health
```

## 工作流程

1. **读取图片**: 将上传的 MultipartFile 转换为临时文件，使用 OpenCV 读取
2. **图片预处理**: 
   - 对过大的图片进行缩放
   - 转换为灰度图
   - 应用高斯模糊
3. **边界检测**: 使用 Canny 边界检测算法
4. **形态学操作**: 膨胀操作增强边界
5. **轮廓查找**: 查找所有轮廓并识别最大的轮廓（主体部分）
6. **角点提取**: 使用最小外接矩形获取四个角点
7. **点排序**: 将四个点按位置排序为左上、右上、右下、左下

## 配置说明

### application.yml

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 50MB          # 单个文件最大大小
      max-request-size: 50MB       # 请求最大大小

server:
  port: 8080                       # 服务器端口

logging:
  level:
    com.example: DEBUG             # 日志级别
```

## 依赖说明

| 依赖 | 版本 | 说明 |
|------|------|------|
| spring-boot-starter-web | 2.7.14 | Spring Web 框架 |
| javacv | 1.5.9 | JavaCV 核心库 |
| opencv | 4.5.2-1.5.9 | OpenCV 算法库 |
| opencv-platform | 4.5.2-1.5.9 | OpenCV 平台库 |
| lombok | 最新 | Java 注解处理库 |
| commons-io | 2.11.0 | IO 工具类 |

## 注意事项

1. **OpenCV 初始化**: 首次运行时需要下载 OpenCV 本地库，可能需要较长时间
2. **图片格式**: 仅支持 JPG、PNG、BMP 等常见图片格式
3. **文件大小**: 默认限制单个文件 50MB，可在 application.yml 中修改
4. **最小面积限制**: 轮廓最小面积设置为 5000 像素，可根据需求调整

## 常见问题

### Q: 为什么识别失败？
A: 可能原因：
- 图片格式不支持
- 图片中没有明确的主体轮廓
- 图片过于模糊或对比度不足

### Q: 如何优化识别精度？
A: 可以调整以下参数：
- Canny 阈值：`detectEdges()` 中的 100 和 200
- 最小面积限制：`findLargestContour()` 中的 5000
- 高斯模糊核大小：`preprocessImage()` 中的 (5, 5)

## 许可证

MIT