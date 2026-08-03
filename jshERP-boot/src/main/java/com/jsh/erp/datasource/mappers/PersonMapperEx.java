package com.jsh.erp.datasource.mappers;

import com.jsh.erp.datasource.entities.Person;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PersonMapperEx {

    List<Person> selectByConditionPerson(
            @Param("name") String name,
            @Param("type") String type);

    int batchDeletePersonByIds(@Param("ids") String ids[]);

    Object lockPersonWrite(@Param("tenantId") Long tenantId);
}
