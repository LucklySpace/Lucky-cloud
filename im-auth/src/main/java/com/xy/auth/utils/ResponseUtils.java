package com.xy.auth.utils;

import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URLEncoder;

/**
 * 使用response返回的工具类
 *
 * @author lyn
 */
@Slf4j
public class ResponseUtils {

    /**
     * 使用response输出JSON
     *
     * @param response 响应对象
     * @param result   需要输出的结果对象
     */
    public static void out(ServletResponse response, Object result) {

        // 检查response是否是HttpServletResponse类型
        if (!(response instanceof HttpServletResponse httpServletResponse)) {
            log.error("Response is not an instance of HttpServletResponse");
            return;
        }

        PrintWriter out = null;
        try {
            httpServletResponse.setCharacterEncoding("UTF-8");
            httpServletResponse.setContentType("application/json");

            // 确保在调用getWriter之前没有调用getOutputStream
            if (httpServletResponse.isCommitted()) {
                log.error("Response is already committed.");
                return;
            }
            out = httpServletResponse.getWriter();
            out.println(JsonUtil.toJSONString(result));
        } catch (IOException e) {
            log.error("输出JSON时发生错误", e);
        } finally {
            if (out != null) {
                try {
                    out.flush();
                    out.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * 将字符串渲染到客户端
     *
     * @param response 渲染对象
     * @param string   待渲染的字符串
     * @return null
     */
    public static String renderString(HttpServletResponse response, String string) {
        try {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");

            // 确保在调用getWriter之前没有调用getOutputStream
            if (response.isCommitted()) {
                log.error("Response is already committed.");
                return null;
            }

            response.getWriter().print(string);
        } catch (IOException e) {
            log.error("渲染字符串时发生错误", e);
        }
        return null;
    }

    /**
     * 使用response输出文件
     *
     * @param response
     * @param is       文件流
     * @param fileName 文件名
     */
    public static void outFile(HttpServletResponse response, InputStream is, String fileName) {
        // 设置强制下载不打开
        response.setContentType("application/force-download");

        // 设置文件名
        try {
            fileName = URLEncoder.encode(fileName, "UTF-8");
            response.addHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

            // 确保没有调用过getWriter
            if (response.isCommitted()) {
                log.error("Response is already committed.");
                return;
            }

            OutputStream outputStream = response.getOutputStream();
            BufferedInputStream bis = new BufferedInputStream(is);

            // 使用IOUtils将输入流写入到响应的输出流中
            IOUtils.copy(bis, outputStream);
            outputStream.flush();
        } catch (IOException e) {
            log.error("输出文件时发生错误", e);
        } finally {
            IOUtils.closeQuietly(is); // 安全关闭InputStream
        }
    }
}