package com.huan.aihelper.common;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FileContentCache {

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public void put(String fileUrl, String parsedContent) {
        cache.put(fileUrl, parsedContent);
    }

    public String get(String fileUrl) {
        return cache.get(fileUrl);
    }

    public void remove(String fileUrl) {
        cache.remove(fileUrl);
    }

    public void cleanup() {
        if (cache.size() > 500) {
            cache.clear();
        }
    }
}
