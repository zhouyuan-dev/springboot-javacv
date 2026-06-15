package com.example.service;

import com.example.model.ImagePoint;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Slf4j
@Service
public class ImageRecognitionService {

    static {
        // 加载 OpenCV 库
        nu.pattern.OpenCV.loadLocally();
    }

    /**
     * 处理上传的图片文件，识别主体部分的坐标
     * @param file 上传的图片文件
     * @return 四个角点的坐标
     */
    public ImagePoint detectImageSubject(MultipartFile file) throws IOException {
        // 1. 读取图片
        Mat image = readImageFromFile(file);
        if (image.empty()) {
            throw new IllegalArgumentException("无法读取图片，请检查文件格式");
        }

        try {
            // 2. 图片预处理
            Mat processed = preprocessImage(image);

            // 3. 检测边界
            Mat edges = detectEdges(processed);

            // 4. 查找轮廓
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            // 5. 找到最大的轮廓（主体部分）
            MatOfPoint largestContour = findLargestContour(contours);
            if (largestContour == null) {
                throw new RuntimeException("无法检测到主体部分");
            }

            // 6. 获取四个角点
            ImagePoint corners = getCornerPoints(largestContour);
            
            log.info("检测成功: 左上({},{}), 右上({},{}), 右下({},{}), 左下({},{})",
                    corners.getTopLeft().getX(), corners.getTopLeft().getY(),
                    corners.getTopRight().getX(), corners.getTopRight().getY(),
                    corners.getBottomRight().getX(), corners.getBottomRight().getY(),
                    corners.getBottomLeft().getX(), corners.getBottomLeft().getY());

            return corners;
        } finally {
            image.release();
        }
    }

    /**
     * 从 MultipartFile 读取图片
     */
    private Mat readImageFromFile(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        File tempFile = File.createTempFile("temp_", ".jpg");
        FileUtils.writeByteArrayToFile(tempFile, bytes);
        
        Mat image = Imgcodecs.imread(tempFile.getAbsolutePath());
        tempFile.delete();
        
        return image;
    }

    /**
     * 图片预处理：缩放、灰度化、模糊
     */
    private Mat preprocessImage(Mat image) {
        // 如果图片过大，进行缩放
        Mat resized = new Mat();
        if (image.width() > 1920 || image.height() > 1080) {
            double scale = Math.min(1920.0 / image.width(), 1080.0 / image.height());
            Imgproc.resize(image, resized, new Size(image.width() * scale, image.height() * scale));
        } else {
            resized = image.clone();
        }

        // 转换为灰度图
        Mat gray = new Mat();
        Imgproc.cvtColor(resized, gray, Imgproc.COLOR_BGR2GRAY);

        // 高斯模糊
        Mat blurred = new Mat();
        Imgproc.GaussianBlur(gray, blurred, new Size(5, 5), 0);

        resized.release();
        gray.release();

        return blurred;
    }

    /**
     * 边界检测
     */
    private Mat detectEdges(Mat image) {
        Mat edges = new Mat();
        Imgproc.Canny(image, edges, 100, 200);
        
        // 形态学操作：膨胀和腐蚀
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
        Mat dilated = new Mat();
        Imgproc.dilate(edges, dilated, kernel, new Point(-1, -1), 2);
        
        edges.release();
        kernel.release();

        return dilated;
    }

    /**
     * 查找最大的轮廓
     */
    private MatOfPoint findLargestContour(List<MatOfPoint> contours) {
        double maxArea = 0;
        MatOfPoint largestContour = null;

        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);
            if (area > maxArea && area > 5000) { // 最小面积限制
                maxArea = area;
                largestContour = contour;
            }
        }

        return largestContour;
    }

    /**
     * 获取轮廓的四个角点
     */
    private ImagePoint getCornerPoints(MatOfPoint contour) {
        // 使用外接矩形获取四个角点
        RotatedRect rotatedRect = Imgproc.minAreaRect(new MatOfPoint2f(contour.toArray()));
        Point[] points = rotatedRect.points();

        // 排序四个点
        Point[] sortedPoints = sortCornerPoints(points);

        return ImagePoint.builder()
                .topLeft(ImagePoint.Point.builder()
                        .x(sortedPoints[0].x)
                        .y(sortedPoints[0].y)
                        .build())
                .topRight(ImagePoint.Point.builder()
                        .x(sortedPoints[1].x)
                        .y(sortedPoints[1].y)
                        .build())
                .bottomRight(ImagePoint.Point.builder()
                        .x(sortedPoints[2].x)
                        .y(sortedPoints[2].y)
                        .build())
                .bottomLeft(ImagePoint.Point.builder()
                        .x(sortedPoints[3].x)
                        .y(sortedPoints[3].y)
                        .build())
                .build();
    }

    /**
     * 排序四个点：左上、右上、右下、左下
     */
    private Point[] sortCornerPoints(Point[] points) {
        // 计算中心点
        Point center = new Point(
                (points[0].x + points[1].x + points[2].x + points[3].x) / 4,
                (points[0].y + points[1].y + points[2].y + points[3].y) / 4
        );

        Point[] sorted = new Point[4];
        
        for (Point p : points) {
            if (p.x < center.x && p.y < center.y) {
                sorted[0] = p; // 左上
            } else if (p.x >= center.x && p.y < center.y) {
                sorted[1] = p; // 右上
            } else if (p.x >= center.x && p.y >= center.y) {
                sorted[2] = p; // 右下
            } else {
                sorted[3] = p; // 左下
            }
        }

        return sorted;
    }
}