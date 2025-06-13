package com.xy.proxy.nginx;

import com.xy.proxy.nacos.NacosTemplate;
import com.xy.spring.annotations.core.Autowired;
import com.xy.spring.annotations.core.Component;
import com.xy.spring.annotations.core.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/**
 * NginxTemplate
 * <p>
 * 自动管理 Nginx：安装、配置替换、启动/重载、健康检查、动态 upstream 更新
 */
@Slf4j(topic = "nginx")
@Component
public class NginxTemplate {

    @Autowired
    private NacosTemplate nacosTemplate;

    // 长连接服务名
    private static final String SERVICE_NAME = "im-connect";

    // 节点配置文件名称
    private static final String UPSTREAM_NAME = "netty_upstream";

    // nginx健康状态监测地址
    private static final String STATUS_URL = "http://127.0.0.1:81/status";

    // 定时任务检查时间
    private static final int CHECK_INTERVAL_SEC = 30;

    //判断系统类型
    private final boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

    //nginx地址
    private final Path nginxHome = isWindows
            ? Paths.get("D:/Program Files/Nginx/nginx-1.27.4")
            : Paths.get("/usr/local/nginx");

    // 执行运行程序名称
    private final Path nginxExe = nginxHome.resolve(isWindows ? "nginx.exe" : "sbin/nginx");

    // 节点配置文件地址
    private final Path upstreamConf = nginxHome.resolve("conf/conf.d/netty_upstream.conf");

    // 上次记录节点列表
    private List<String> lastNodeConfig = new ArrayList<>();

    // 定时任务
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    public void init() {
        try {

            ensureNginxPrepared();

            startNginxIfNeeded();

            verifyHealth();

            updateUpstream();

            scheduler.scheduleAtFixedRate(this::task, CHECK_INTERVAL_SEC, CHECK_INTERVAL_SEC, TimeUnit.SECONDS);

        } catch (Exception e) {
            log.error("NginxTemplate 初始化失败", e);
        }
    }

    /**
     * 开启定时任务
     */
    private void task() {
        try {
            verifyHealth();
            updateUpstream();
        } catch (Exception e) {
            log.error("NginxTemplate 定时任务失败", e);
        }
    }

    /**
     * 确保 Nginx 安装目录及配置存在
     */
    private void ensureNginxPrepared() throws IOException {
        if (!Files.exists(nginxHome)) {
            if (!isWindows) {
                throw new IllegalStateException("请在 Linux 上手动安装 Nginx 到 " + nginxHome);
            }
            log.info("未检测到 Nginx，开始解压安装...");
            try (InputStream zip = getClass().getClassLoader().getResourceAsStream("nginx/nginx-windows.zip")) {
                if (zip == null) throw new FileNotFoundException("未找到 nginx-windows.zip");
                unzip(zip, nginxHome);
                log.info("Nginx 解压完成");
            }
        }
        // 替换主配置
        String cfgRes = "nginx/nginx.conf";
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(cfgRes)) {
            if (in == null) throw new FileNotFoundException("未找到配置文件: " + cfgRes);
            Files.copy(in, nginxHome.resolve("conf/nginx.conf"), StandardCopyOption.REPLACE_EXISTING);
            log.info("主配置已替换");
        }
    }

    /**
     * 启动 Nginx（如果未运行）
     */
    private void startNginxIfNeeded() throws IOException, InterruptedException {
        if (isRunning()) {
            log.info("Nginx 已在运行");
            return;
        }
        log.info("启动 Nginx...");
        runCommand(nginxExe.toString());
        log.info("Nginx 启动完成");
    }

    /**
     * 更新 upstream 配置并 reload
     */
    private void updateUpstream() throws IOException, InterruptedException {
        if (nacosTemplate.getNamingService() == null) {
            nacosTemplate.registerNacos();
        }
        List<String> nodes = nacosTemplate.getAllInstances(SERVICE_NAME).stream()
                .map(i -> i.getIp() + ":" + i.getPort())
                .collect(Collectors.toList());
        if (nodes.isEmpty()) {
            log.warn("无可用实例，跳过 upstream 更新");
            return;
        }

        if(!lastNodeConfig.isEmpty()&&lastNodeConfig.equals(nodes)){
            log.warn("节点无变化，无需更新配置");
            return;
        }else{
            log.info("节点列表：{}",nodes);
        }

        // 记录当前节点列表  用于下次比对
        lastNodeConfig = nodes;

        // 生成配置文件内容
        StringBuilder sb = new StringBuilder("# auto-generated\nupstream ").append(UPSTREAM_NAME).append(" {\n").append("hash $arg_uid consistent;\n");
        nodes.forEach(n -> sb.append("    server ").append(n).append(" max_fails=3 fail_timeout=30s;\n"));
        sb.append('}');

        // 创建或刷新配置文件
        Files.createDirectories(upstreamConf.getParent());
        Files.write(upstreamConf, sb.toString().getBytes(StandardCharsets.UTF_8));
        log.info("upstream 生成完成: {}", upstreamConf);

        // 重载
        reload();

        log.info("Nginx reload 完成");
    }

    /**
     * nginx 健康检查 stub_status
     */
    private void verifyHealth() {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(STATUS_URL).openConnection();
            c.setConnectTimeout(1000);
            c.setReadTimeout(1000);
            if (c.getResponseCode() != 200) throw new IOException("状态码: " + c.getResponseCode());
            log.info("Nginx 健康");
        } catch (Exception e) {
            throw new RuntimeException("Nginx 健康检查失败", e);
        }
    }

    /**
     * nginx 执行重载
     */
    private void reload() throws IOException, InterruptedException {
        String cmd = isWindows ? nginxExe.toString() : "nginx";
        runCommand(cmd, "-s", "reload");
    }

    /**
     * 通用命令执行
     */
    private void runCommand(String... cmd) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(nginxHome.toFile());
        // 合并标准错误到标准输出
        pb.redirectErrorStream(true);

        Process process = pb.start();
        // 异步消费输出，防止阻塞
        Thread stdoutReader = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    log.debug("[nginx] {}", line);
                }
            } catch (IOException e) {
                log.warn("读取命令输出时出错", e);
            }
        });
        stdoutReader.setDaemon(true);
        stdoutReader.start();

        // 等待执行完成，带超时
        boolean finished = process.waitFor(10, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            //throw new RuntimeException("命令执行超时: " + String.join(" ", cmd));
        }
        int code = process.exitValue();
        if (code != 0) {
            //throw new RuntimeException("命令执行失败（退出码=" + code + "）: " + String.join(" ", cmd));
        }
    }

    /**
     * 解压 ZIP
     */
    private void unzip(InputStream in, Path target) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(in)) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                Path p = target.resolve(e.getName());
                if (e.isDirectory()) Files.createDirectories(p);
                else {
                    Files.createDirectories(p.getParent());
                    Files.copy(zis, p, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }

    /**
     * 判断 Nginx 是否在运行
     */
    private boolean isRunning() {
        try {
            Process p = isWindows
                    ? new ProcessBuilder("tasklist").start()
                    : new ProcessBuilder("pgrep", "nginx").start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                return r.lines().anyMatch(l -> isWindows ? l.toLowerCase().contains("nginx.exe") : !l.isBlank());
            }
        } catch (IOException e) {
            log.warn("检测运行状态失败", e);
            return false;
        }
    }
}


