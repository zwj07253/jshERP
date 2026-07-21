package com.jsh.erp.datasource.mappers;

import com.jsh.erp.datasource.entities.MaterialCategory;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * Description
 *
 * @Author: cjl
 * @Date: 2019/2/18 17:23
 */
public interface MaterialCategoryMapperEx {
    List<MaterialCategory> selectByConditionMaterialCategory(
            @Param("name") String name,
            @Param("parentId") Integer parentId);

    List<MaterialCategory> getActiveMaterialCategoryList();

    Object lockMaterialCategoryWrite(@Param("tenantId") Long tenantId);

    int batchDeleteMaterialCategoryByIds(@Param("updateTime") Date updateTime, @Param("updater") Long updater,
                                         @Param("ids") List<Long> ids);

    int editMaterialCategory(MaterialCategory mc);

    List<MaterialCategory> getMaterialCategoryBySerialNo(@Param("serialNo") String serialNo, @Param("id") Long id);

    List<MaterialCategory> getMaterialCategoryListByCategoryIds(@Param("parentIds") List<Long> categoryIds);

}
