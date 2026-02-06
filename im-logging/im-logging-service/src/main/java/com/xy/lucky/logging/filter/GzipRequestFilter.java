//package com.xy.lucky.logging.filter;
//
//import jakarta.servlet.Filter;
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.ServletInputStream;
//import jakarta.servlet.ServletRequest;
//import jakarta.servlet.ServletResponse;
//import jakarta.servlet.ReadListener;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletRequestWrapper;
//import org.springframework.stereotype.Component;
//
//import java.io.*;
//import java.nio.charset.Charset;
//import java.util.Locale;
//
//@Component
//public class GzipRequestFilter implements Filter {
//    @Override
//    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
//            throws IOException, ServletException {
//        if (request instanceof HttpServletRequest httpRequest) {
//            String contentEncoding = httpRequest.getHeader("Content-Encoding");
//            boolean isGzip = contentEncoding != null && "gzip".equalsIgnoreCase(contentEncoding.trim());
//            boolean isJson = isJsonContent(httpRequest.getHeader("Content-Type"));
//            boolean isPost = "POST".equalsIgnoreCase(httpRequest.getMethod());
//            if (isGzip && isJson && isPost) {
//                chain.doFilter(new GzipHttpServletRequestWrapper(httpRequest), response);
//                return;
//            }
//        }
//        chain.doFilter(request, response);
//    }
//
//    private boolean isJsonContent(String contentType) {
//        if (contentType == null) return false;
//        String ct = contentType.toLowerCase(Locale.ROOT);
//        return ct.contains("application/json") || ct.contains("application/") && ct.contains("+json");
//    }
//
//    static class GzipHttpServletRequestWrapper extends HttpServletRequestWrapper {
//        private final byte[] cachedBody;
//
//        GzipHttpServletRequestWrapper(HttpServletRequest request) throws IOException {
//            super(request);
//            byte[] rawBody = readAllBytes(request.getInputStream());
//            this.cachedBody = decompress(rawBody);
//        }
//
//        private byte[] decompress(byte[] rawBody) {
//            try (ByteArrayInputStream bais = new ByteArrayInputStream(rawBody);
//                 java.util.zip.GZIPInputStream gzip = new java.util.zip.GZIPInputStream(bais);
//                 ByteArrayOutputStream out = new ByteArrayOutputStream(rawBody.length)) {  // Initial capacity hint
//                byte[] buffer = new byte[8192];
//                int n;
//                while ((n = gzip.read(buffer)) > 0) {
//                    out.write(buffer, 0, n);
//                }
//                return out.toByteArray();
//            } catch (IOException e) {
//                // If decompression fails, return the raw body (perhaps it wasn't actually gzipped)
//                return rawBody;
//            }
//        }
//
//        private byte[] readAllBytes(ServletInputStream input) throws IOException {
//            try (ByteArrayOutputStream out = new ByteArrayOutputStream(8192)) {
//                byte[] buffer = new byte[8192];
//                int n;
//                while ((n = input.read(buffer)) > 0) {
//                    out.write(buffer, 0, n);
//                }
//                return out.toByteArray();
//            }
//        }
//
//        @Override
//        public ServletInputStream getInputStream() {
//            ByteArrayInputStream bais = new ByteArrayInputStream(cachedBody);
//            return new ServletInputStream() {
//                @Override
//                public boolean isFinished() {
//                    return bais.available() == 0;
//                }
//
//                @Override
//                public boolean isReady() {
//                    return true;
//                }
//
//                @Override
//                public void setReadListener(ReadListener readListener) {
//                    // No-op, as this is a simple in-memory stream
//                }
//
//                @Override
//                public int read() {
//                    return bais.read();
//                }
//            };
//        }
//
//        @Override
//        public int getContentLength() {
//            return cachedBody.length;
//        }
//
//        @Override
//        public long getContentLengthLong() {
//            return cachedBody.length;
//        }
//
//        public BufferedReader getReader() {
//            String encoding = getCharacterEncoding();
//            Charset charset = Charset.forName(encoding != null ? encoding : "UTF-8");
//            return new InputStreamReader(getInputStream(), charset);
//        }
//    }
//}