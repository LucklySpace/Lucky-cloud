package com.xy.lucky.ai.memory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class InFileChatMemory implements ChatMemory {

    private static final Kryo kryo = new Kryo();

    // 静态初始化块，在类被加载时只执行一次，用来初始化类的静态字段或做一些静态配置
    // 如果写在构造函数里，每创建一个对象就会重复配置一次，浪费资源，而且有可能在并发情况下产生线程安全风险，kryo 的行为应该在类级别上定义，而不是实例级别
    static {
        // 默认情况下，Kryo 要求先注册所有要序列化的类，否则报错。这行代码关闭了这个限制：即使没有预注册类也能序列化/反序列化。
        kryo.setRegistrationRequired(false);  // 不强制要求注册类
        // Kryo 默认有时不能创建没有无参构造器的对象，会抛异常。这行设置了一种“宽松”的策略，可以“跳过构造函数”创建对象，从而支持序列化像 JPA 实体等没有显式构造函数的类
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());  // 设置标准的实例化策略
    }

    private final String BASE_DIR;

    /**
     * 构造对象时，指定文件保存目录
     */
    public InFileChatMemory(String dir) {
        this.BASE_DIR = dir;
        File baseDir = new File(dir);
        if (!baseDir.exists())
            baseDir.mkdirs();  // 如果文件夹不存在，先创建文件夹
        // 方法名	        作用	                                    是否能创建多级目录
        // mkdir()	        创建当前指定的“单个目录”，父目录必须存在	        ❌ 不能
        // mkdirs()	        创建当前目录，并递归创建所有不存在的父目录	    ✅ 可以
        // eg：File dir = new File("a/b/c");
        // mkdir() 只能在 a/b 已经存在的情况下成功创建 c。
        // mkdirs() 会自动创建所有中间路径，不管 a、b 是否存在。
        // 推荐用 mkdirs()：更稳妥，尤其不确定父目录是否已存在时。
        // 用 mkdir()：适合能确保父目录已存在的场景。
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        List<Message> messageList = getOrCreateConversation(conversationId);
        messageList.addAll(messages);
        saveConversation(conversationId, messageList);
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        List<Message> messageList = getOrCreateConversation(conversationId);
        return messageList.stream()
                .skip(Math.max(0, messageList.size() - lastN))  // 最后 N 条：跳过 总数-N 条消息
                .toList();
    }

    @Override
    public void clear(String conversationId) {
        File file = getConversationFile(conversationId);
        if (file.exists())
            file.delete();
    }

    /**
     * 每个会话文件单独保存，根据会话id获取相应的会话文件
     */
    private File getConversationFile(String conversationId) {
        return new File(BASE_DIR, conversationId + ".kryo");
    }

    /**
     * 获取/创建会话消息列表
     */
    private List<Message> getOrCreateConversation(String conversationId) {
        File file = getConversationFile(conversationId);  // 得到会话文件
        List<Message> messages = new ArrayList<>();
        if (file.exists()) {
            // try-with-resources 语法，要求资源对象实现了 AutoCloseable 接口，在使用完后自动关闭，无需手动调用 close()，防止资源泄露
            try (Input input = new Input(new FileInputStream(file))) {
                messages = kryo.readObject(input, ArrayList.class);  // 反序列化，从文件中读取对象
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return messages;
    }

    /**
     * 保存会话消息列表
     */
    private void saveConversation(String conversationId, List<Message> messages) {
        File file = getConversationFile(conversationId);
        try (Output output = new Output(new FileOutputStream(file))) {
            kryo.writeObject(output, messages);  // 把 messages 对象序列化成二进制数据，并写入到 output 中，以便后续可以保存或传输
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}