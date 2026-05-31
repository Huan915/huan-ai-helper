package com.huan.mcpserver.tools;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ImageSearchToolTest {

    @Resource
    private ImageSearchTool imageSearchTool;

    @Test
    void setImageSearchTool(){
        List<String> computer = imageSearchTool.searchMediumImages("computer");
        Assertions.assertNotNull(imageSearchTool);
    }
}