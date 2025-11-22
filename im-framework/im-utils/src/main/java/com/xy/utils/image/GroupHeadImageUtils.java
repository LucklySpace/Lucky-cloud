package com.xy.utils.image;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * 参考文章 url: https://blog.csdn.net/weixin_41712724/article/details/103706259
 * 群头像生成工具类
 */
public class GroupHeadImageUtils {
    /**
     * 图片宽度
     */
    private final Integer PIC_WIDTH = 422;
    /**
     * 图片高度
     */
    private final Integer PIC_HEIGHT = 422;
    /**
     * 空白宽度
     */
    private final Integer PIC_SPACE = 14;
    /***  思考笔记  可以跳过
     *  2 和 4 的区别  向下偏移量          0.5 LUMP_WIDTH+  0.5 PIC_SPACE
     *  3 和 4 的区别 第一张图向右偏移量   0.5 LUMP_WIDTH + 0.5 PIC_SPACE
     *  5 和 6 的区别 第一张图向右偏移量   0.5 LUMP_WIDTH+ 0.5 PIC_SPACE
     *  6 和 9 的区别 向下偏移量          0.5 LUMP_WIDTH+ 0.5 PIC_SPACE
     *  7 和 9 的区别 第一张图向右偏移量  LUMP_WIDTH+PIC_SPACE
     *  8 和 9 的区别 第一张图向右偏移量  0.5 LUMP_WIDTH+ 0.5 PIC_SPACE
     */

    // 围边使用的灰色
    private final int[] COLOR_GREY_BGR = new int[]{230, 230, 230};
    /**
     * 小图片宽度
     */
    private Double LUMP_WIDTH = null;
    /**
     * 小图片起始点横坐标
     */
    private Double LUMP_POINT_X = null;
    /**
     * 小图片起始点纵坐标
     */
    private Double LUMP_POINT_Y = null;
    //校对数组使用下标
    private int flg = 0;

    /**
     * @param pics     图片列表
     * @param fileName 存储图片名称（不包含路径）
     * @return 返回一个空文件对象
     */
    public File getCombinationOfhead(List<String> pics, String fileName) {

        List<BufferedImage> bufferedImages = new ArrayList<>();

        // BufferedImage.TYPE_INT_RGB可以自己定义可查看API
        BufferedImage outImage = new BufferedImage(PIC_WIDTH, PIC_HEIGHT, BufferedImage.TYPE_INT_RGB);

        Graphics2D gra = outImage.createGraphics();
        //设置背景为蓝灰色
        gra.setColor(toColor(COLOR_GREY_BGR));
        //填满图片
        gra.fillRect(0, 0, PIC_WIDTH, PIC_HEIGHT);

        // 开始拼凑 根据图片的数量判断该生成哪种样式组合头像

        Integer size = pics.size();//图片数量
        Integer sqrt = (int) Math.ceil(Math.sqrt(size));//宽度  一行几张图片
        //计算出 单张图片宽度
        LUMP_WIDTH = (PIC_WIDTH - ((sqrt + 1.0) * PIC_SPACE)) / sqrt;

        System.out.println(LUMP_WIDTH);

        // 压缩图片所有的图片生成尺寸同意的 为 125*125
        for (int i = 0; i < pics.size(); i++) {
            BufferedImage resize2 = resize2(pics.get(i), LUMP_WIDTH.intValue(), LUMP_WIDTH.intValue(), true);
            bufferedImages.add(resize2);
        }

        //缺几个满伍
        int lack = 0;
        //计算起始点坐标
        if (size < sqrt * (sqrt - 1)) {//少一行 不满伍
            //缺几个满伍
            lack = sqrt * (sqrt - 1) - size;
            //向右边偏移量
            LUMP_POINT_X = PIC_SPACE.doubleValue() + lack * (LUMP_WIDTH + PIC_SPACE) / 2;
            //向下偏移量
            LUMP_POINT_Y = PIC_SPACE.doubleValue() + LUMP_WIDTH / 2.;
        } else if (size == sqrt * (sqrt - 1)) {//满伍少一行
            //向右边偏移量
            LUMP_POINT_X = PIC_SPACE.doubleValue();
            //向下偏移量
            LUMP_POINT_Y = PIC_SPACE.doubleValue() + LUMP_WIDTH / 2.;
        } else if (size < sqrt * sqrt) {//不满伍
            //缺几个满伍
            lack = sqrt * sqrt - size;
            //向右边偏移量
            LUMP_POINT_X = PIC_SPACE.doubleValue() + lack * (LUMP_WIDTH + PIC_SPACE) / 2;
            LUMP_POINT_Y = PIC_SPACE.doubleValue();
        } else if (size == sqrt * sqrt) {//满伍
            LUMP_POINT_X = PIC_SPACE.doubleValue();
            LUMP_POINT_Y = PIC_SPACE.doubleValue();
        }

        int line = lack == 0 ? -1 : 0; //第几行图片
        int row = 0; //第几列图片
        for (int i = 0; i < bufferedImages.size(); i++) {
            if ((i + lack) % sqrt == 0) {
                line++;
                row = 0;
            }
            if (line == 0) {
                gra.drawImage(bufferedImages.get(i), LUMP_POINT_X.intValue() + (row++ * (PIC_SPACE + LUMP_WIDTH.intValue()))
                        , LUMP_POINT_Y.intValue(), null);
            } else {
                gra.drawImage(bufferedImages.get(i), PIC_SPACE + (row++ * (PIC_SPACE + LUMP_WIDTH.intValue()))
                        , LUMP_POINT_Y.intValue() + (line * (PIC_SPACE + LUMP_WIDTH.intValue())), null);
            }
        }

        // 创建一个临时文件
        File tempFile = null;
        try {
            tempFile = File.createTempFile(fileName, ".png");
            // 将图片写入文件
            if (tempFile != null) {
                ImageIO.write(outImage, "jpg", tempFile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tempFile;
    }


    /**
     * @param pics     图片列表
     * @param path     存储路径
     * @param fileName 存储图片名称
     * @return 成功 OR 失败
     */
    public boolean getCombinationOfhead(List<String> pics, String path, String fileName) {
        List<BufferedImage> bufferedImages = new ArrayList<BufferedImage>();

        // BufferedImage.TYPE_INT_RGB可以自己定义可查看API
        BufferedImage outImage = new BufferedImage(PIC_WIDTH, PIC_HEIGHT, BufferedImage.TYPE_INT_RGB);

        Graphics2D gra = outImage.createGraphics();
        //设置背景为蓝灰色
        gra.setColor(toColor(COLOR_GREY_BGR));
        //填满图片
        gra.fillRect(0, 0, PIC_WIDTH, PIC_HEIGHT);

        // 开始拼凑 根据图片的数量判断该生成哪种样式组合头像

        Integer size = pics.size();//图片数量
        Integer sqrt = (int) Math.ceil(Math.sqrt(size));//宽度  一行几张图片
        //计算出 单张图片宽度
        LUMP_WIDTH = (PIC_WIDTH - ((sqrt + 1.0) * PIC_SPACE)) / sqrt;

        System.out.println(LUMP_WIDTH);

        // 压缩图片所有的图片生成尺寸同意的 为 125*125
        for (int i = 0; i < pics.size(); i++) {
            BufferedImage resize2 = resize2(pics.get(i), LUMP_WIDTH.intValue(), LUMP_WIDTH.intValue(), true);
            bufferedImages.add(resize2);
        }

        //缺几个满伍
        int lack = 0;
        //计算起始点坐标
        if (size < sqrt * (sqrt - 1)) {//少一行 不满伍
            //缺几个满伍
            lack = sqrt * (sqrt - 1) - size;
            //向右边偏移量
            LUMP_POINT_X = PIC_SPACE.doubleValue() + lack * (LUMP_WIDTH + PIC_SPACE) / 2;
            //向下偏移量
            LUMP_POINT_Y = PIC_SPACE.doubleValue() + LUMP_WIDTH / 2.;
        } else if (size == sqrt * (sqrt - 1)) {//满伍少一行
            //向右边偏移量
            LUMP_POINT_X = PIC_SPACE.doubleValue();
            //向下偏移量
            LUMP_POINT_Y = PIC_SPACE.doubleValue() + LUMP_WIDTH / 2.;
        } else if (size < sqrt * sqrt) {//不满伍
            //缺几个满伍
            lack = sqrt * sqrt - size;
            //向右边偏移量
            LUMP_POINT_X = PIC_SPACE.doubleValue() + lack * (LUMP_WIDTH + PIC_SPACE) / 2;
            LUMP_POINT_Y = PIC_SPACE.doubleValue();
        } else if (size == sqrt * sqrt) {//满伍
            LUMP_POINT_X = PIC_SPACE.doubleValue();
            LUMP_POINT_Y = PIC_SPACE.doubleValue();
        }

        int line = lack == 0 ? -1 : 0; //第几行图片
        int row = 0; //第几列图片
        for (int i = 0; i < bufferedImages.size(); i++) {
            if ((i + lack) % sqrt == 0) {
                line++;
                row = 0;
            }
            if (line == 0) {
                gra.drawImage(bufferedImages.get(i), LUMP_POINT_X.intValue() + (row++ * (PIC_SPACE + LUMP_WIDTH.intValue()))
                        , LUMP_POINT_Y.intValue(), null);
            } else {
                gra.drawImage(bufferedImages.get(i), PIC_SPACE + (row++ * (PIC_SPACE + LUMP_WIDTH.intValue()))
                        , LUMP_POINT_Y.intValue() + (line * (PIC_SPACE + LUMP_WIDTH.intValue())), null);
            }
        }

        File file = new File(path + fileName + ".png");
        //文件如果存在先删除，再创建
        try {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
                if (file.exists()) {
                    file.delete();
                    if (!file.createNewFile()) {
                        System.out.println("创建失败！");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //将图片写到文件
        try {
            return ImageIO.write(outImage, "png", file);
        } catch (IOException e) {
            return false;
        }
    }


    /**
     * 图片缩放
     *
     * @param picPath 本地或网络图片路径
     * @param height  缩放后高度
     * @param width   缩放后宽度
     * @param fill    是否填充灰色
     * @return BufferedImage
     */
    public BufferedImage resize2(String picPath, Integer height, Integer width, boolean fill) {
        try {
            BufferedImage imageBuff = null;
            if (picPath.indexOf("https://") == 0 || picPath.indexOf("http://") == 0) { //简单判断是网络图片还是本地图片
                imageBuff = ImageIO.read(new URL(picPath));
            } else {
                imageBuff = ImageIO.read(new File(picPath));
            }

            Image itemp = imageBuff.getScaledInstance(width, height, Image.SCALE_SMOOTH);

            double ratio = 0; // 缩放比例
            // 计算比例
            if ((imageBuff.getHeight() > height) || (imageBuff.getWidth() > width)) {
                if (imageBuff.getHeight() > imageBuff.getWidth()) {
                    ratio = height.doubleValue() / imageBuff.getHeight();
                } else {
                    ratio = width.doubleValue() / imageBuff.getWidth();
                }
                AffineTransformOp op = new AffineTransformOp(AffineTransform.getScaleInstance(ratio, ratio), null);

                itemp = op.filter(imageBuff, null);
            }

            if (fill) {
                BufferedImage image = new BufferedImage(width, height,
                        BufferedImage.TYPE_INT_RGB);

                Graphics2D g = image.createGraphics();

                g.setColor(toColor(COLOR_GREY_BGR));

                g.fillRect(0, 0, width, height);

                if (width == itemp.getWidth(null)) {
                    g.drawImage(itemp, 0, (height - itemp.getHeight(null)) / 2,
                            itemp.getWidth(null), itemp.getHeight(null),
                            Color.white, null);
                } else {
                    g.drawImage(itemp, (width - itemp.getWidth(null)) / 2, 0,
                            itemp.getWidth(null), itemp.getHeight(null),
                            Color.white, null);
                }
                g.dispose();
                itemp = image;
            }
            return (BufferedImage) itemp;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param colorRoot 颜色索引
     * @return 颜色
     * @toColor 颜色索引转为颜色
     */
    private Color toColor(int[] colorRoot) {
        if (colorRoot.length >= 3) {
            return new Color(colorRoot[0], colorRoot[1], colorRoot[2]);
        } else {
            return null;
        }
    }
}

