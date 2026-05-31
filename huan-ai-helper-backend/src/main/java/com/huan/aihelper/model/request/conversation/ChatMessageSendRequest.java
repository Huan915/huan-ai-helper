package com.huan.aihelper.model.request.conversation;

import lombok.Data;

import java.util.List;

@Data
public class ChatMessageSendRequest {

    private String content;

    private Boolean gain;

    private Boolean deepThinking;

    private List<AttachmentInfo> attachments;

    @Data
    public static class AttachmentInfo {
        private String name;
        private String type;
        private String url;
        private Long size;
        private String parsedContent;
    }
}
