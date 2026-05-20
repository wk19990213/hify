package com.hify.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hify.chat.entity.ChatMessageEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessageEntity> {
}
