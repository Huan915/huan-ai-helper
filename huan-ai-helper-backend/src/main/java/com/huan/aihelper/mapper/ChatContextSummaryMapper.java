package com.huan.aihelper.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huan.aihelper.model.entity.ChatContextSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ChatContextSummaryMapper extends BaseMapper<ChatContextSummary> {

    @Select("SELECT * FROM chat_context_summary WHERE conversation_id = #{conversationId} AND is_deleted = 0 ORDER BY seq_end DESC LIMIT 1")
    ChatContextSummary selectLatest(@Param("conversationId") Long conversationId);


}
