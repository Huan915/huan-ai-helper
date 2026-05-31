package com.huan.aihelper.chatmemory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import lombok.extern.slf4j.Slf4j;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FileBasedChatMemory implements ChatMemory {

    private final String MEMORY_DIR;
    private final int maxMessages;
    private static final Kryo kryo = new Kryo();

    static {
        kryo.setRegistrationRequired(false);
        // 设置实例化策略
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
    }

    /**
     * 构造函数（使用默认最大消息数 10）
     */
    public FileBasedChatMemory(String MEMORY_DIR) {
        this(MEMORY_DIR, 10);
    }

    /**
     * 构造函数（可指定最大消息数）
     * @param MEMORY_DIR 存储目录
     * @param maxMessages 最大消息数量
     */
    public FileBasedChatMemory(String MEMORY_DIR, int maxMessages) {
        this.MEMORY_DIR = MEMORY_DIR;
        this.maxMessages = maxMessages;
        File baseDir = new File(MEMORY_DIR);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        if (conversationId == null || messages == null || messages.isEmpty()) {
            return;
        }

        synchronized (conversationId.intern()) {
            List<Message> existingMessages = loadFromFile(conversationId);
            existingMessages.addAll(messages);
            
            // 如果消息数量超过限制，保留最新的 maxMessages 条消息
            if (existingMessages.size() > maxMessages) {
                int fromIndex = existingMessages.size() - maxMessages;
                existingMessages = existingMessages.subList(fromIndex, existingMessages.size());
                log.debug("消息数量 {} 超过限制 {}, 截断保留最新 {} 条消息", 
                    existingMessages.size() + fromIndex, maxMessages, maxMessages);
            }
            
            saveToFile(conversationId, existingMessages);
        }
    }

    @Override
    public List<Message> get(String conversationId) {

        return loadFromFile(conversationId);
    }

    @Override
    public void clear(String conversationId) {
        if (conversationId == null) {
            return;
        }

        synchronized (conversationId.intern()) {
            Path filePath = Paths.get(MEMORY_DIR, conversationId + ".kryo");
            try {
                Files.deleteIfExists(filePath);
                log.info("已清除会话 {} 的记忆并删除文件", conversationId);
            } catch (IOException e) {
                log.error("删除会话文件失败: {}", filePath, e);
            }
        }
    }

    private List<Message> loadFromFile(String conversationId) {
        Path filePath = Paths.get(MEMORY_DIR, conversationId + ".kryo");

        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }

        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             Input input = new Input(fis)) {

            @SuppressWarnings("unchecked")
            List<Message> messages = kryo.readObject(input, ArrayList.class);
            log.debug("从文件加载 {} 条消息,会话ID: {}", messages.size(), conversationId);
            return messages;

        } catch (Exception e) {
            log.error("从文件加载消息失败,会话ID: {},将返回空列表", conversationId, e);
            return new ArrayList<>();
        }
    }

    private void saveToFile(String conversationId, List<Message> messages) {
        Path filePath = Paths.get(MEMORY_DIR, conversationId + ".kryo");

        try (FileOutputStream fos = new FileOutputStream(filePath.toFile());
             Output output = new Output(fos)) {

            kryo.writeObject(output, messages);
            output.flush();
            log.debug("已保存 {} 条消息到文件,会话ID: {}", messages.size(), conversationId);

        } catch (Exception e) {
            log.error("保存消息到文件失败,会话ID: {}", conversationId, e);
            throw new RuntimeException("保存对话记忆失败", e);
        }
    }
}