package com.jsh.erp.datasource.mappers;

import com.jsh.erp.datasource.entities.TenantFeatureMapping;
import com.jsh.erp.datasource.entities.TenantFeatureMappingExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface TenantFeatureMappingMapper {
    long countByExample(TenantFeatureMappingExample example);

    int deleteByExample(TenantFeatureMappingExample example);

    int deleteByPrimaryKey(Long id);

    int insert(TenantFeatureMapping record);

    int insertSelective(TenantFeatureMapping record);

    List<TenantFeatureMapping> selectByExample(TenantFeatureMappingExample example);

    TenantFeatureMapping selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") TenantFeatureMapping record, @Param("example") TenantFeatureMappingExample example);

    int updateByExample(@Param("record") TenantFeatureMapping record, @Param("example") TenantFeatureMappingExample example);

    int updateByPrimaryKeySelective(TenantFeatureMapping record);

    int updateByPrimaryKey(TenantFeatureMapping record);
}
