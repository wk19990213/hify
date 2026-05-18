package com.hify.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hify.common.entity.DemoItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DemoItemMapper extends BaseMapper<DemoItem> {
}
