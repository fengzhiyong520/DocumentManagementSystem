package com.dms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dms.entity.ProcessDefinition;
import org.apache.ibatis.annotations.Mapper;

/**
 * 流程定义Mapper
 */
@Mapper
public interface ProcessDefinitionMapper extends BaseMapper<ProcessDefinition> {
}

