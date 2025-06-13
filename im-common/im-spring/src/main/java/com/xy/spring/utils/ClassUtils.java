package com.xy.spring.utils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 类扫描工具：用于扫描指定包路径下的所有Class文件（支持文件系统和JAR包）
 */
public class ClassUtils {

    /**
     * 扫描指定包路径下的所有Class
     *
     * @param basePackage 要扫描的包路径（如：com.example）
     * @return 类集合（不包括接口和抽象类）
     * @throws ClassNotFoundException 当类加载失败时抛出
     * @throws RuntimeException       当包路径不存在或扫描过程出错时抛出
     */
    public static Set<Class<?>> scan(String basePackage) throws ClassNotFoundException {
        Set<Class<?>> classSet = new HashSet<>();

        // 将包路径转换为文件系统路径格式（com.example -> com/example）
        String path = basePackage.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        // 获取资源URL（可能指向目录或JAR条目）
        URL resource = classLoader.getResource(path);
        if (resource == null) {
            throw new RuntimeException("找不到包路径：" + path);
        }

        try {
            // 根据资源协议类型（file/jar）选择不同的处理方式
            String protocol = resource.getProtocol();
            if ("jar".equals(protocol)) {
                processJar(resource, basePackage, classLoader, classSet);
            } else if ("file".equals(protocol)) {
                processFileSystem(resource, basePackage, classSet);
            }
        } catch (IOException e) {
            throw new RuntimeException("扫描包路径失败：" + path, e);
        }

        return classSet;
    }

    /**
     * 处理JAR包中的类扫描
     *
     * @param resource    JAR资源URL
     * @param basePackage 要扫描的包路径
     * @param classLoader 类加载器
     * @param classSet    用于存储结果的集合
     */
    private static void processJar(URL resource, String basePackage, ClassLoader classLoader, Set<Class<?>> classSet)
            throws IOException, ClassNotFoundException {

        // 建立JAR连接并获取JAR文件对象（try-with-resources确保自动关闭）
        JarURLConnection jarConn = (JarURLConnection) resource.openConnection();
        try (JarFile jarFile = jarConn.getJarFile()) {
            // 将包路径转换为JAR内部路径格式（com.example -> com/example/）
            String basePath = basePackage.replace('.', '/') + "/";

            // 遍历JAR中的所有条目
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();

                // 筛选：路径匹配且是.class文件
                if (entryName.startsWith(basePath) && entryName.endsWith(".class")) {
                    String className = convertEntryToClassName(entryName);
                    try {
                        // 加载类并加入结果集
                        Class<?> clazz = classLoader.loadClass(className);
                        classSet.add(clazz);
                    } catch (ClassNotFoundException e) {
                        throw new ClassNotFoundException("加载类失败：" + className, e);
                    }
                }
            }
        }
    }



    /**
     * 处理文件系统中的类扫描
     *
     * @param resource    文件资源URL
     * @param basePackage 要扫描的包路径
     * @param classSet    用于存储结果的集合
     */
    private static void processFileSystem(URL resource, String basePackage, Set<Class<?>> classSet)
            throws ClassNotFoundException {

        // 解码URL路径（处理空格等特殊字符）
        String filePath = URLDecoder.decode(resource.getFile(), StandardCharsets.UTF_8);
        File directory = new File(filePath);
        if (!directory.exists()) return;

        // 递归扫描目录
        scanDirectory(basePackage, directory, classSet);
    }

    /**
     * 递归扫描文件目录
     *
     * @param currentPackage 当前包路径
     * @param directory      要扫描的目录
     * @param classSet       用于存储结果的集合
     */
    private static void scanDirectory(String currentPackage, File directory, Set<Class<?>> classSet)
            throws ClassNotFoundException {

        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            String fileName = file.getName();

            if (file.isDirectory()) {
                // 递归处理子目录（包路径累加）
                scanDirectory(currentPackage + "." + fileName, file, classSet);
            } else if (fileName.endsWith(".class")) {
                // 处理.class文件（移除后缀后加载类）
                String className = currentPackage + '.' + fileName.replace(".class", "");
                try {
                    Class<?> clazz = Class.forName(className);
                    classSet.add(clazz);
                } catch (ClassNotFoundException e) {
                    throw new ClassNotFoundException("加载类失败：" + className, e);
                }
            }
        }
    }

    /**
     * 将JAR条目路径转换为全限定类名
     *
     * @param entryName JAR条目路径（如：com/example/Test.class）
     * @return 类名（如：com.example.Test）
     */
    private static String convertEntryToClassName(String entryName) {
        // 替换路径分隔符   移除".class"后缀（6字符）
        return entryName.replace('/', '.')
                .substring(0, entryName.length() - 6);
    }

    /**
     * 使用当前线程上下文 ClassLoader 加载类。
     * 支持：
     * <ul>
     *   <li>原始类型："int", "boolean" 等</li>
     *   <li>一维及多维数组："java.lang.String[][]", "int[]" 等</li>
     *   <li>普通类：全限定名</li>
     * </ul>
     *
     * @param name 类名或数组描述
     * @return 对应 Class 对象
     * @throws ClassNotFoundException 无法找到类时抛出
     */
    public static Class<?> forName(String name) throws ClassNotFoundException {
        return forName(name, Thread.currentThread().getContextClassLoader());
    }

    /**
     * 使用指定 ClassLoader 加载类。
     *
     * @param name        类名或数组描述，如 "int[]"、"com.foo.Bar"
     * @param classLoader 用于加载的 ClassLoader，若为 null 则使用系统 ClassLoader
     * @return Class 对象
     * @throws ClassNotFoundException 加载失败时抛出
     */
    public static Class<?> forName(String name, ClassLoader classLoader) throws ClassNotFoundException {
        Objects.requireNonNull(name, "Class name must not be null");
        ClassLoader loader = (classLoader != null ? classLoader : ClassLoader.getSystemClassLoader());

        // 多维数组处理：检测末尾连续的 "[]"
        int dims = 0;
        while (name.endsWith("[]")) {
            dims++;
            name = name.substring(0, name.length() - 2);
        }

        Class<?> component;
        switch (name) {
            case "byte":    component = byte.class;    break;
            case "short":   component = short.class;   break;
            case "int":     component = int.class;     break;
            case "long":    component = long.class;    break;
            case "float":   component = float.class;   break;
            case "double":  component = double.class;  break;
            case "boolean": component = boolean.class; break;
            case "char":    component = char.class;    break;
            case "void":    component = void.class;    break;
            default:
                component = Class.forName(name, false, loader);
        }

        // 如果不是数组，直接返回
        if (dims == 0) {
            return component;
        }
        // 创建多维数组类型
        int[] dimensions = new int[dims];
        return Array.newInstance(component, dimensions).getClass();
    }

    /**
     * 检查类是否存在。
     *
     * @param name        类名或数组描述
     * @param classLoader 加载时使用的 ClassLoader
     * @return 如果存在则返回 true，否则 false
     */
    public static boolean isPresent(String name, ClassLoader classLoader) {
        try {
            forName(name, classLoader);
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    /**
     * 检查类是否存在，使用线程上下文 ClassLoader。
     */
    public static boolean isPresent(String name) {
        return isPresent(name, Thread.currentThread().getContextClassLoader());
    }

    /**
     * 将资源路径转换为类全限定名。
     * <p>示例："com/example/Foo.class" -> "com.example.Foo"</p>
     *
     * @param resourcePath 资源路径
     * @return 类名
     */
    public static String resourcePathToClassName(String resourcePath) {
        String path = resourcePath;
        if (path.endsWith(".class")) {
            path = path.substring(0, path.length() - 6);
        }
        return path.replace('/', '.');
    }

    /**
     * 将类全限定名转换为资源路径。
     * <p>示例："com.example.Foo" -> "com/example/Foo.class"</p>
     *
     * @param className 全限定类名
     * @return 资源路径
     */
    public static String classNameToResourcePath(String className) {
        Objects.requireNonNull(className, "Class name must not be null");
        return className.replace('.', '/') + ".class";
    }

    /**
     * 提取简单类名，包括内部类。
     * <p>示例："com.example.Outer$Inner" -> "Inner"</p>
     *
     * @param className 全限定类名
     * @return 简单名
     */
    public static String getShortName(String className) {
        Objects.requireNonNull(className, "Class name must not be null");
        int lastDot = className.lastIndexOf('.');
        int lastDollar = className.lastIndexOf('$');
        int pos = Math.max(lastDot, lastDollar);
        return className.substring(pos + 1);
    }

    /**
     * 获取对象的用户类，剥离常见代理（如 CGLIB）生成的子类。
     *
     * @param instance 对象实例
     * @return 用户实际定义的类
     */
    public static Class<?> getUserClass(Object instance) {
        Objects.requireNonNull(instance, "Instance must not be null");
        Class<?> clazz = instance.getClass();
        String name = clazz.getName();
        int idx = name.indexOf("$$");
        if (idx > 0 && clazz.getSuperclass() != null && !Object.class.equals(clazz.getSuperclass())) {
            return clazz.getSuperclass();
        }
        return clazz;
    }

    /**
     * 获取默认 ClassLoader，优先线程上下文 ClassLoader。
     *
     * @return 类加载器
     */
    public static ClassLoader getDefaultClassLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return (cl != null ? cl : ClassUtils.class.getClassLoader());
    }


}