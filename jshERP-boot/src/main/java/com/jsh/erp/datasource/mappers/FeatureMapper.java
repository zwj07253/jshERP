package com.jsh.erp.datasource.mappers;

import com.jsh.erp.datasource.entities.Feature;
import com.jsh.erp.datasource.entities.FeatureExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface FeatureMapper {
    long countByExample(FeatureExample example);

    int deleteByExample(FeatureExample example);

    int deleteByPrimaryKey(Long id);

    int insert(Feature record);

    int insertSelective(Feature record);

    List<Feature> selectByExample(FeatureExample example);

    Feature selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") Feature record, @Param("example") FeatureExample example);

    int updateByExample(@Param("record") Feature record, @Param("example") FeatureExample example);

    int updateByPrimaryKeySelective(Feature record);

    int updateByPrimaryKey(Feature record);
}
