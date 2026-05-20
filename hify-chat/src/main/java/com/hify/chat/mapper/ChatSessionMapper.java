package com.hify.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hify.chat.entity.ChatSessionEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSessionEntity> {
}
