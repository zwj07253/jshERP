package com.jsh.erp.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.MaterialAttribute;
import com.jsh.erp.datasource.entities.MaterialAttributeExample;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.MaterialAttributeMapper;
import com.jsh.erp.datasource.mappers.MaterialAttributeMapperEx;
import com.jsh.erp.datasource.mappers.MaterialMapperEx;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.exception.JshException;
import com.jsh.erp.utils.PageUtils;
import com.jsh.erp.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class MaterialAttributeService {
    private static final String MATERIAL_ATTRIBUTE_URL = "/material/material_attribute";
    private static final String EDIT_BUTTON_CODE = "1";
    private Logger logger = LoggerFactory.getLogger(MaterialAttributeService.class);

    @Resource
    private LogService logService;

    @Resource
    private MaterialAttributeMapper materialAttributeMapper;

    @Resource
    private MaterialAttributeMapperEx materialAttributeMapperEx;
    @Resource
    private MaterialMapperEx materialMapperEx;
    @Resource
    private UserService userService;

    public MaterialAttribute getMaterialAttribute(long id)throws Exception {
        return getInfoById(id);
    }

    public List<MaterialAttribute> getMaterialAttribute() throws Exception{
        MaterialAttributeExample example = new MaterialAttributeExample();
        example.createCriteria().andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        example.setOrderByClause("id desc");
        List<MaterialAttribute> list=null;
        try{
            list=materialAttributeMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<MaterialAttribute> select(String attributeName, String attributeValue) throws Exception{
        List<MaterialAttribute> list = new ArrayList<>();
        try{
            PageUtils.startPage();
            list = materialAttributeMapperEx.selectByConditionMaterialAttribute(attributeName, attributeValue);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int insertMaterialAttribute(JSONObject obj, HttpServletRequest request)throws Exception {
        checkEditPermission();
        MaterialAttribute m = buildMaterialAttribute(obj, false);
        validateMaterialAttribute(m, null);
        try{
            materialAttributeMapper.insertSelective(m);
            logService.insertLog("商品属性",
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_ADD).append(m.getAttributeName()).toString(), request);
            return 1;
        }
        catch (BusinessRunTimeException ex) {
            throw new BusinessRunTimeException(ex.getCode(), ex.getMessage());
        }
        catch(Exception e){
            JshException.writeFail(logger, e);
            return 0;
        }
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int updateMaterialAttribute(JSONObject obj, HttpServletRequest request) throws Exception{
        checkEditPermission();
        MaterialAttribute materialAttribute = buildMaterialAttribute(obj, true);
        MaterialAttribute existing = materialAttribute.getId() == null ? null : getInfoById(materialAttribute.getId());
        if (existing == null) {
            throw invalidAttribute("商品属性不存在或已删除");
        }
        requireTenantOwnership(existing);
        validateMaterialAttribute(materialAttribute, existing);
        try{
            materialAttributeMapper.updateByPrimaryKeySelective(materialAttribute);
            logService.insertLog("商品属性",
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append(materialAttribute.getAttributeName()).toString(), request);
            return 1;
        }catch(Exception e){
            JshException.writeFail(logger, e);
            return 0;
        }
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int deleteMaterialAttribute(Long id, HttpServletRequest request)throws Exception {
        checkEditPermission();
        MaterialAttribute existing = getInfoById(id);
        if (existing != null) {
            requireTenantOwnership(existing);
        }
        return batchDeleteMaterialAttributeByIds(id.toString());
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteMaterialAttribute(String ids, HttpServletRequest request)throws Exception {
        checkEditPermission();
        return batchDeleteMaterialAttributeByIds(ids);
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    private int batchDeleteMaterialAttributeByIds(String ids) throws Exception{
        List<Long> idList = StringUtil.strToLongList(ids);
        ensureAttributesNotInUse(idList);
        requireBatchTenantOwnership(idList);
        String [] idArray = idList.stream().map(String::valueOf).toArray(String[]::new);
        try{
            return materialAttributeMapperEx.batchDeleteMaterialAttributeByIds(idArray);
        }catch(Exception e){
            JshException.writeFail(logger, e);
            return 0;
        }
    }

    private void requireTenantOwnership(MaterialAttribute record) throws Exception {
        if (record == null) return;
        User currentUser = userService.getCurrentUser();
        Long currentTenantId = currentUser == null ? null : currentUser.getTenantId();
        if (currentTenantId != null && !currentTenantId.equals(record.getTenantId())) {
            throw invalidAttribute("商品属性不存在或已删除");
        }
    }

    private void requireBatchTenantOwnership(List<Long> idList) throws Exception {
        if (idList == null || idList.isEmpty()) return;
        User currentUser = userService.getCurrentUser();
        Long currentTenantId = currentUser == null ? null : currentUser.getTenantId();
        if (currentTenantId == null) return;
        for (Long id : idList) {
            MaterialAttribute record = getInfoById(id);
            if (record != null && !currentTenantId.equals(record.getTenantId())) {
                throw invalidAttribute("商品属性不存在或已删除");
            }
        }
    }

    public int checkIsNameExist(Long id, String name)throws Exception {
        name = name == null ? null : name.trim();
        MaterialAttributeExample example = new MaterialAttributeExample();
        example.createCriteria().andIdNotEqualTo(id).andAttributeNameEqualTo(name).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<MaterialAttribute> list =null;
        try{
            list = materialAttributeMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list==null?0:list.size();
    }

    public JSONArray getValueArrById(Long id) {
        JSONArray valueArr = new JSONArray();
        MaterialAttribute ma = getInfoById(id);
        if(ma!=null) {
            String value = ma.getAttributeValue();
            if(StringUtil.isNotEmpty(value)){
                String[] arr = value.split("\\|");
                for(String v: arr) {
                    JSONObject item = new JSONObject();
                    item.put("value",v);
                    item.put("name",v);
                    valueArr.add(item);
                }
            }
        }
        return valueArr;
    }

    public MaterialAttribute getInfoById(Long id) {
        MaterialAttributeExample example = new MaterialAttributeExample();
        example.createCriteria().andIdEqualTo(id).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<MaterialAttribute> list = materialAttributeMapper.selectByExample(example);
        if(list!=null && list.size()>0) {
            return list.get(0);
        } else {
            return null;
        }
    }

    private MaterialAttribute buildMaterialAttribute(JSONObject obj, boolean update) {
        MaterialAttribute attribute = new MaterialAttribute();
        if (update) {
            attribute.setId(obj.getLong("id"));
        }
        attribute.setAttributeName(obj.getString("attributeName"));
        attribute.setAttributeValue(obj.getString("attributeValue"));
        attribute.setDeleteFlag(update ? null : BusinessConstants.DELETE_FLAG_EXISTS);
        return attribute;
    }

    private void validateMaterialAttribute(MaterialAttribute attribute, MaterialAttribute existing) throws Exception {
        String name = StringUtil.toNull(attribute.getAttributeName());
        if (name == null || name.length() > 50) {
            throw invalidAttribute("属性名称不能为空且不能超过50个字符");
        }
        String normalizedValue = normalizeAttributeValue(attribute.getAttributeValue());
        if (StringUtil.isEmpty(normalizedValue) || normalizedValue.length() > 500) {
            throw invalidAttribute("属性值不能为空且不能超过500个字符");
        }
        attribute.setAttributeName(name);
        attribute.setAttributeValue(normalizedValue);
        if (checkIsNameExist(attribute.getId() == null ? 0L : attribute.getId(), name) > 0) {
            throw invalidAttribute("属性名称已存在");
        }
        if (existing != null && !normalizedValue.equals(existing.getAttributeValue())) {
            ensureAttributesNotInUse(java.util.Collections.singletonList(existing.getId()));
        }
    }

    private String normalizeAttributeValue(String value) {
        Set<String> values = new LinkedHashSet<>();
        if (value != null) {
            for (String item : value.split("\\|")) {
                String normalized = StringUtil.toNull(item);
                if (normalized != null) {
                    values.add(normalized);
                }
            }
        }
        return String.join("|", values);
    }

    private void ensureAttributesNotInUse(List<Long> ids) {
        if (ids != null && !ids.isEmpty() && materialMapperEx.getCountByMaterialAttributeIds(ids) > 0) {
            throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_ATTRIBUTE_IN_USE_CODE,
                    ExceptionConstants.MATERIAL_ATTRIBUTE_IN_USE_MSG);
        }
    }

    private void checkEditPermission() throws Exception {
        User user = userService.getCurrentUser();
        if (!userService.hasButtonPermission(user == null ? null : user.getId(), MATERIAL_ATTRIBUTE_URL, EDIT_BUTTON_CODE)) {
            throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_PERMISSION_CODE,
                    ExceptionConstants.MATERIAL_PERMISSION_MSG);
        }
    }

    private BusinessRunTimeException invalidAttribute(String reason) {
        return new BusinessRunTimeException(ExceptionConstants.MATERIAL_ATTRIBUTE_INVALID_CODE,
                String.format(ExceptionConstants.MATERIAL_ATTRIBUTE_INVALID_MSG, reason));
    }
}
