package com.huan.aihelper.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huan.aihelper.model.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    @Select("SELECT COALESCE(MAX(seq_no), 0) FROM chat_message WHERE conversation_id = #{conversationId} AND is_deleted = 0")
    int selectMaxSeqNo(@Param("conversationId") Long conversationId);

    @Select("SELECT * FROM chat_message WHERE conversation_id = #{conversationId} AND context_included = true AND is_deleted = 0 ORDER BY seq_no ASC")
    List<ChatMessage> selectInContext(@Param("conversationId") Long conversationId);
}
