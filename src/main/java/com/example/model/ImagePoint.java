package com.example.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImagePoint {
    /** 左上角 */
    private Point topLeft;
    /** 右上角 */
    private Point topRight;
    /** 右下角 */
    private Point bottomRight;
    /** 左下角 */
    private Point bottomLeft;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Point {
        private double x;
        private double y;
    }
}