package com.xy.lucky.auth.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.xy.lucky.auth.security.config.QRCodeProperties;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 生成二维码图像类
 *
 * @author songzixian
 */
public class QRCodeUtil {

    private static QRCodeProperties qrCodeProperties = new QRCodeProperties();

    /**
     * 设置二维码配置
     *
     * @param config 配置对象
     */
    public static void setQRCodeConfig(QRCodeProperties config) {
        qrCodeProperties = config;
    }

    /**
     * 生成二维码图像并保存为文件
     *
     * @param text     要编码的文本
     * @param width    二维码图像宽度
     * @param height   二维码图像高度
     * @param filePath 文件存储路径
     */
    public static void generateQRCode(String text, int width, int height, String filePath) {
        generateQRCode(text, width, height, filePath, null);
    }
    
    /**
     * 生成带Logo的二维码图像并保存为文件
     *
     * @param text     要编码的文本
     * @param width    二维码图像宽度
     * @param height   二维码图像高度
     * @param filePath 文件存储路径
     * @param logoPath Logo图片路径
     */
    public static void generateQRCode(String text, int width, int height, String filePath, String logoPath) {
        try {
            BitMatrix bitMatrix = generateBitMatrix(text, width, height);
            
            // 将二维码矩阵信息转换为图像
            BufferedImage image = bitMatrixToImage(bitMatrix);
            
            // 如果提供了Logo路径，则在二维码中央添加Logo
            if (logoPath != null && !logoPath.isEmpty()) {
                image = addLogoToQRCode(image, logoPath,
                        qrCodeProperties.getLogo().getWidth(),
                        qrCodeProperties.getLogo().getHeight(),
                        qrCodeProperties.getLogo().getOpacity());
            }
            
            // 将二维码保存成文件
            File qrCodeFile = new File(filePath);
            String format = getFileExtension(filePath);
            ImageIO.write(image, format, qrCodeFile);
            
            System.out.println("二维码生成成功: " + filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 生成二维码图像并保存为文件（使用默认尺寸）
     *
     * @param text     要编码的文本
     * @param filePath 文件存储路径
     */
    public static void generateQRCode(String text, String filePath) {
        generateQRCode(text, qrCodeProperties.getDefaultWidth(), qrCodeProperties.getDefaultHeight(), filePath,
                qrCodeProperties.getLogo().getPath());
    }

    /**
     * 生成二维码图像并返回 BufferedImage 对象
     *
     * @param text   要编码的文本
     * @param width  二维码图像宽度
     * @param height 二维码图像高度
     * @return BufferedImage 对象
     */
    public static BufferedImage generateQRCodeImage(String text, int width, int height) {
        return generateQRCodeImage(text, width, height, null);
    }
    
    /**
     * 生成带Logo的二维码图像并返回 BufferedImage 对象
     *
     * @param text     要编码的文本
     * @param width    二维码图像宽度
     * @param height   二维码图像高度
     * @param logoPath Logo图片路径
     * @return BufferedImage 对象
     */
    public static BufferedImage generateQRCodeImage(String text, int width, int height, String logoPath) {
        try {
            BitMatrix bitMatrix = generateBitMatrix(text, width, height);
            BufferedImage image = bitMatrixToImage(bitMatrix);
            
            // 如果提供了Logo路径，则在二维码中央添加Logo
            if (logoPath != null && !logoPath.isEmpty()) {
                image = addLogoToQRCode(image, logoPath,
                        qrCodeProperties.getLogo().getWidth(),
                        qrCodeProperties.getLogo().getHeight(),
                        qrCodeProperties.getLogo().getOpacity());
            }
            
            return image;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 生成二维码图像并返回 BufferedImage 对象（使用默认尺寸）
     *
     * @param text 要编码的文本
     * @return BufferedImage 对象
     */
    public static BufferedImage generateQRCodeImage(String text) {
        return generateQRCodeImage(text, qrCodeProperties.getDefaultWidth(), qrCodeProperties.getDefaultHeight(),
                qrCodeProperties.getLogo().getPath());
    }

    /**
     * 生成二维码图像并返回字节数组
     *
     * @param text   要编码的文本
     * @param width  二维码图像宽度
     * @param height 二维码图像高度
     * @return 生成的二维码图像的字节数组
     */
    public static byte[] generateQRCodeBytes(String text, int width, int height) {
        return generateQRCodeBytes(text, width, height, qrCodeProperties.getFormat(), null);
    }
    
    /**
     * 生成带Logo的二维码图像并返回字节数组
     *
     * @param text     要编码的文本
     * @param width    二维码图像宽度
     * @param height   二维码图像高度
     * @param format   图片格式
     * @param logoPath Logo图片路径
     * @return 生成的二维码图像的字节数组
     */
    public static byte[] generateQRCodeBytes(String text, int width, int height, String format, String logoPath) {
        try {
            BitMatrix bitMatrix = generateBitMatrix(text, width, height);
            BufferedImage image = bitMatrixToImage(bitMatrix);
            
            // 如果提供了Logo路径，则在二维码中央添加Logo
            if (logoPath != null && !logoPath.isEmpty()) {
                image = addLogoToQRCode(image, logoPath,
                        qrCodeProperties.getLogo().getWidth(),
                        qrCodeProperties.getLogo().getHeight(),
                        qrCodeProperties.getLogo().getOpacity());
            }
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, format, outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 生成二维码图像并返回字节数组（使用默认尺寸）
     *
     * @param text 要编码的文本
     * @return 生成的二维码图像的字节数组
     */
    public static byte[] generateQRCodeBytes(String text) {
        return generateQRCodeBytes(text, qrCodeProperties.getDefaultWidth(), qrCodeProperties.getDefaultHeight());
    }
    
    /**
     * 生成二维码图像并返回字节数组（使用默认尺寸）
     *
     * @param text   要编码的文本
     * @param format 图片格式
     * @return 生成的二维码图像的字节数组
     */
    public static byte[] generateQRCodeBytes(String text, String format) {
        return generateQRCodeBytes(text, qrCodeProperties.getDefaultWidth(), qrCodeProperties.getDefaultHeight(), format,
                qrCodeProperties.getLogo().getPath());
    }

    /**
     * 生成二维码图像并返回 Base64 编码字符串
     *
     * @param text   要编码的文本
     * @param width  二维码图像宽度
     * @param height 二维码图像高度
     * @return Base64 编码的图像字符串
     */
    public static String generateQRCodeBase64(String text, int width, int height) {
        return generateQRCodeBase64(text, width, height, qrCodeProperties.getFormat(), null);
    }
    
    /**
     * 生成带Logo的二维码图像并返回 Base64 编码字符串
     *
     * @param text     要编码的文本
     * @param width    二维码图像宽度
     * @param height   二维码图像高度
     * @param format   图片格式
     * @param logoPath Logo图片路径
     * @return Base64 编码的图像字符串
     */
    public static String generateQRCodeBase64(String text, int width, int height, String format, String logoPath) {
        try {
            byte[] imageBytes = generateQRCodeBytes(text, width, height, format, logoPath);
            if (imageBytes != null) {
                return "data:image/" + format + ";base64," + Base64.getEncoder().encodeToString(imageBytes);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * 生成二维码图像并返回 Base64 编码字符串（使用默认尺寸）
     *
     * @param text 要编码的文本
     * @return Base64 编码的图像字符串
     */
    public static String generateQRCodeBase64(String text) {
        return generateQRCodeBase64(text, qrCodeProperties.getDefaultWidth(), qrCodeProperties.getDefaultHeight());
    }
    
    /**
     * 生成二维码图像并返回 Base64 编码字符串（使用默认尺寸）
     *
     * @param text   要编码的文本
     * @param format 图片格式
     * @return Base64 编码的图像字符串
     */
    public static String generateQRCodeBase64(String text, String format) {
        return generateQRCodeBase64(text, qrCodeProperties.getDefaultWidth(), qrCodeProperties.getDefaultHeight(), format,
                qrCodeProperties.getLogo().getPath());
    }

    /**
     * 生成二维码位矩阵
     *
     * @param text   要编码的文本
     * @param width  二维码图像宽度
     * @param height 二维码图像高度
     * @return BitMatrix 对象
     * @throws Exception 生成过程中可能抛出的异常
     */
    private static BitMatrix generateBitMatrix(String text, int width, int height) throws Exception {
        // 定义二维码参数
        Map<EncodeHintType, Object> hints = new HashMap<>();
        // 设置字符编码
        hints.put(EncodeHintType.CHARACTER_SET, qrCodeProperties.getCharset());
        // 错误纠正级别
        hints.put(EncodeHintType.ERROR_CORRECTION, getErrorCorrectionLevel(qrCodeProperties.getErrorCorrectionLevel()));
        // 二维码边距
        hints.put(EncodeHintType.MARGIN, qrCodeProperties.getMargin());

        // 使用QRCodeWriter生成二维码矩阵信息
        MultiFormatWriter writer = new MultiFormatWriter();
        return writer.encode(text, BarcodeFormat.QR_CODE, width, height, hints);
    }

    /**
     * 将二维码位矩阵转换为图像
     *
     * @param bitMatrix 二维码位矩阵
     * @return BufferedImage 对象
     */
    private static BufferedImage bitMatrixToImage(BitMatrix bitMatrix) {
        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, bitMatrix.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
            }
        }
        return image;
    }

    /**
     * 根据字符串获取错误纠正级别
     *
     * @param level 纠错级别字符串 (L, M, Q, H)
     * @return ErrorCorrectionLevel 对象
     */
    private static com.google.zxing.qrcode.decoder.ErrorCorrectionLevel getErrorCorrectionLevel(String level) {
        switch (level.toUpperCase()) {
            case "L":
                return com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.L;
            case "M":
                return com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M;
            case "Q":
                return com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.Q;
            case "H":
            default:
                return com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H;
        }
    }
    
    /**
     * 从文件路径获取文件扩展名
     *
     * @param filePath 文件路径
     * @return 文件扩展名
     */
    private static String getFileExtension(String filePath) {
        String format = qrCodeProperties.getFormat();
        if (filePath != null && filePath.lastIndexOf(".") != -1) {
            format = filePath.substring(filePath.lastIndexOf(".") + 1);
        }
        return format;
    }
    
    /**
     * 在二维码中央添加Logo图片
     *
     * @param qrImage   二维码图片
     * @param logoPath  Logo图片路径 (可以是文件系统路径或resources目录下的路径)
     * @param logoWidth Logo宽度
     * @param logoHeight Logo高度
     * @param opacity   Logo透明度
     * @return 添加Logo后的二维码图片
     * @throws IOException 读取Logo图片时可能抛出的异常
     */
    private static BufferedImage addLogoToQRCode(BufferedImage qrImage, String logoPath, 
            Integer logoWidth, Integer logoHeight, Float opacity) throws IOException {
        BufferedImage logoImage = null;
        
        // 首先尝试从resources目录加载
        InputStream resourceStream = QRCodeUtil.class.getClassLoader().getResourceAsStream(logoPath);
        if (resourceStream != null) {
            logoImage = ImageIO.read(resourceStream);
        } else {
            // 如果resources中没有，则尝试从文件系统加载
            File logoFile = new File(logoPath);
            if (logoFile.exists()) {
                logoImage = ImageIO.read(logoFile);
            }
        }
        
        // 如果Logo图片不存在，则返回原二维码图片
        if (logoImage == null) {
            System.err.println("Logo文件不存在: " + logoPath);
            return qrImage;
        }
        
        // 计算Logo在二维码中央的位置
        int qrWidth = qrImage.getWidth();
        int qrHeight = qrImage.getHeight();
        int x = (qrWidth - logoWidth) / 2;
        int y = (qrHeight - logoHeight) / 2;
        
        // 创建Graphics2D对象进行绘制
        Graphics2D g = qrImage.createGraphics();
        
        // 设置透明度
        if (opacity >= 0 && opacity <= 1) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        }
        
        // 绘制Logo
        g.drawImage(logoImage, x, y, logoWidth, logoHeight, null);
        g.dispose();
        
        return qrImage;
    }
}