package com.jsh.erp.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.Supplier;
import com.jsh.erp.datasource.entities.Depot;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.entities.UserBusiness;
import com.jsh.erp.datasource.entities.UserBusinessExample;
import com.jsh.erp.datasource.mappers.UserBusinessMapper;
import com.jsh.erp.datasource.mappers.UserBusinessMapperEx;
import com.jsh.erp.datasource.mappers.SupplierMapperEx;
import com.jsh.erp.datasource.mappers.DepotMapper;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.exception.JshException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

@Service
public class UserBusinessService {
    private Logger logger = LoggerFactory.getLogger(UserBusinessService.class);

    @Resource
    private UserBusinessMapper userBusinessMapper;
    @Resource
    private UserBusinessMapperEx userBusinessMapperEx;
    @Resource
    private LogService logService;
    @Resource
    private UserService userService;
    @Resource
    private SupplierMapperEx supplierMapperEx;
    @Resource
    private DepotMapper depotMapper;

    public UserBusiness getUserBusiness(long id)throws Exception {
        UserBusiness result=null;
        try{
            result=userBusinessMapper.selectByPrimaryKey(id);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    public List<UserBusiness> getUserBusiness()throws Exception {
        UserBusinessExample example = new UserBusinessExample();
        example.createCriteria().andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<UserBusiness> list=null;
        try{
            list=userBusinessMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int insertUserBusiness(JSONObject obj, HttpServletRequest request) throws Exception {
        UserBusiness requested = JSONObject.parseObject(obj.toJSONString(), UserBusiness.class);
        UserBusiness userBusiness = writableRelation(requested);
        validateCustomerRelation(userBusiness, null);
        int result=0;
        try{
            String value = userBusiness.getValue();
            String newValue = value.replaceAll(",","\\]\\[");
            newValue = newValue.replaceAll("\\[0\\]","").replaceAll("\\[\\]","");
            userBusiness.setValue(newValue);
            result=userBusinessMapper.insertSelective(userBusiness);
            logService.insertLog("关联关系", BusinessConstants.LOG_OPERATION_TYPE_ADD, request);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int updateUserBusiness(JSONObject obj, HttpServletRequest request) throws Exception {
        UserBusiness requested = JSONObject.parseObject(obj.toJSONString(), UserBusiness.class);
        UserBusiness userBusiness = writableRelation(requested);
        UserBusiness existing = userBusiness.getId() == null ? null
                : userBusinessMapper.selectByPrimaryKey(userBusiness.getId());
        if (existing == null || !existing.getType().equals(userBusiness.getType())
                || !existing.getKeyId().equals(userBusiness.getKeyId())) {
            throw invalidRelation("用户关系记录不存在或不匹配");
        }
        validateCustomerRelation(userBusiness, existing);
        int result=0;
        try{
            String value = userBusiness.getValue();
            String newValue = value.replaceAll(",","\\]\\[");
            newValue = newValue.replaceAll("\\[0\\]","").replaceAll("\\[\\]","");
            userBusiness.setValue(newValue);
            result=userBusinessMapper.updateByPrimaryKeySelective(userBusiness);
            logService.insertLog("关联关系", BusinessConstants.LOG_OPERATION_TYPE_EDIT, request);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int deleteUserBusiness(Long id, HttpServletRequest request)throws Exception {
        return batchDeleteUserBusinessByIds(id.toString());
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteUserBusiness(String ids, HttpServletRequest request)throws Exception {
        return batchDeleteUserBusinessByIds(ids);
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteUserBusinessByIds(String ids) throws Exception{
        logService.insertLog("关联关系",
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_DELETE).append(ids).toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        User userInfo=userService.getCurrentUser();
        String [] idArray=ids.split(",");
        int result=0;
        try{
            result=  userBusinessMapperEx.batchDeleteUserBusinessByIds(new Date(),userInfo==null?null:userInfo.getId(),idArray);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    public int checkIsNameExist(Long id, String name)throws Exception {
        return 1;
    }

    public List<UserBusiness> getBasicData(String keyId, String type)throws Exception{
        List<UserBusiness> list=null;
        try{
            list= userBusinessMapperEx.getBasicDataByKeyIdAndType(keyId, type);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public String getUBValueByTypeAndKeyId(String type, String keyId) throws Exception {
        String ubValue = "";
        List<UserBusiness> ubList = getBasicData(keyId, type);
        if(ubList!=null && ubList.size()>0) {
            ubValue = ubList.get(0).getValue();
        }
        return ubValue;
    }

    public Long checkIsValueExist(String type, String keyId)throws Exception {
        UserBusinessExample example = new UserBusinessExample();
        example.createCriteria().andTypeEqualTo(type).andKeyIdEqualTo(keyId)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<UserBusiness> list=null;
        try{
            list= userBusinessMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        Long id = null;
        if(list!=null&&list.size() > 0) {
            id = list.get(0).getId();
        }
        return id;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int updateBtnStr(String keyId, String type, String btnStr) throws Exception{
        logService.insertLog("关联关系",
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append("角色的按钮权限").toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        UserBusiness userBusiness = new UserBusiness();
        userBusiness.setBtnStr(btnStr);
        UserBusinessExample example = new UserBusinessExample();
        example.createCriteria().andKeyIdEqualTo(keyId).andTypeEqualTo(type);
        int result=0;
        try{
            result=  userBusinessMapper.updateByExampleSelective(userBusiness, example);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    public List<Long> getUBKeyIdByTypeAndOneValue(String type, String oneValue) {
        return userBusinessMapperEx.getUBKeyIdByTypeAndOneValue(type, oneValue);
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int updateOneValueByKeyIdAndType(String type, JSONArray keyIdArr, String oneValue) throws Exception {
        if (!("UserCustomer".equals(type) || "UserDepot".equals(type)) || keyIdArr == null) {
            throw invalidRelation("用户授权参数不合法");
        }
        if ("UserCustomer".equals(type)) {
            validateCustomerId(oneValue);
        } else {
            validateDepotId(oneValue);
        }
        Set<String> validatedUserIds = new HashSet<>();
        for (Object keyIdObj : keyIdArr) {
            String keyId = keyIdObj == null ? null : keyIdObj.toString();
            validateUserId(keyId);
            validatedUserIds.add(keyId);
        }
        int res = 0;
        try {
            Map<String, String> keyIdMap = new HashMap<>();
            List<UserBusiness> oldUbList = userBusinessMapperEx.getOldListByType(type);
            for(String keyId: validatedUserIds) {
                keyIdMap.put(keyId, keyId);
                List<UserBusiness> ubList = userBusinessMapperEx.getBasicDataByKeyIdAndType(keyId, type);
                if(ubList.size()>0) {
                    String valueStr = ubList.get(0).getValue();
                    Boolean flag = valueStr.contains("[" + oneValue + "]");
                    if(flag) {
                        //存在则忽略
                    } else {
                        //不存在则追加并更新
                        valueStr = valueStr + "[" + oneValue + "]";
                        UserBusiness userBusiness = new UserBusiness();
                        userBusiness.setId(ubList.get(0).getId());
                        userBusiness.setValue(valueStr);
                        userBusinessMapper.updateByPrimaryKeySelective(userBusiness);
                    }
                } else {
                    //新增数据
                    UserBusiness userBusiness = new UserBusiness();
                    userBusiness.setType(type);
                    userBusiness.setKeyId(keyId);
                    userBusiness.setValue("[" + oneValue + "]");
                    userBusinessMapper.insertSelective(userBusiness);
                }
            }
            //检查被移除的keyId
            for(UserBusiness item: oldUbList) {
                String oldValue = item.getValue();
                String oldkeyId = item.getKeyId();
                if(keyIdMap.get(oldkeyId) == null) {
                    //处理被删除的keyId
                    String valueStr = "[" + oneValue + "]";
                    if(oldValue.equals(valueStr)) {
                        //说明value里面只有一条数据，需要进行逻辑删除
                        UserBusiness userBusiness = new UserBusiness();
                        userBusiness.setId(item.getId());
                        userBusiness.setDeleteFlag("1");
                        userBusinessMapper.updateByPrimaryKeySelective(userBusiness);
                    } else {
                        //多条进行替换后再更新
                        String newValue = oldValue.replace(valueStr, "");
                        UserBusiness userBusiness = new UserBusiness();
                        userBusiness.setId(item.getId());
                        userBusiness.setValue(newValue);
                        userBusinessMapper.updateByPrimaryKeySelective(userBusiness);
                    }
                }
            }
            res = 1;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
        return res;
    }

    public void validateCustomerId(String customerId) throws Exception {
        Long id = parsePositiveId(customerId, "客户ID不合法");
        Supplier supplier = supplierMapperEx.getInfoById(id);
        if (supplier == null || !"客户".equals(supplier.getType()) || !Boolean.TRUE.equals(supplier.getEnabled())) {
            throw invalidRelation("客户不存在或已停用");
        }
    }

    public void validateDepotId(String depotId) throws Exception {
        Long id = parsePositiveId(depotId, "仓库ID不合法");
        Depot depot = depotMapper.selectByPrimaryKey(id);
        if (depot == null || !Boolean.TRUE.equals(depot.getEnabled())
                || BusinessConstants.DELETE_FLAG_DELETED.equals(depot.getDeleteFlag())) {
            throw invalidRelation("仓库不存在或已停用");
        }
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int removeOneValueByType(String type, String oneValue) {
        if (!"UserDepot".equals(type)) {
            throw invalidRelation("用户授权类型不合法");
        }
        String valueToken = "[" + oneValue + "]";
        int changed = 0;
        for (UserBusiness item : userBusinessMapperEx.getOldListByType(type)) {
            String oldValue = item.getValue();
            if (oldValue == null || !oldValue.contains(valueToken)) {
                continue;
            }
            UserBusiness update = new UserBusiness();
            update.setId(item.getId());
            if (oldValue.equals(valueToken)) {
                update.setDeleteFlag(BusinessConstants.DELETE_FLAG_DELETED);
            } else {
                update.setValue(oldValue.replace(valueToken, ""));
            }
            changed += userBusinessMapper.updateByPrimaryKeySelective(update);
        }
        return changed;
    }

    private void validateCustomerRelation(UserBusiness relation, UserBusiness existing) throws Exception {
        if (!("UserCustomer".equals(relation.getType()) || "UserDepot".equals(relation.getType()))) {
            return;
        }
        if (existing != null && (!relation.getType().equals(existing.getType())
                || !existing.getKeyId().equals(relation.getKeyId()))) {
            throw invalidRelation("用户授权记录不匹配");
        }
        if (relation.getId() != null && existing == null) {
            throw invalidRelation("用户授权记录不存在");
        }
        validateUserId(relation.getKeyId());
        String value = relation.getValue();
        if (value == null) {
            throw invalidRelation("客户授权值不能为空");
        }
        String normalized = value.replace(",", "][").replace("[0]", "").replace("[]", "");
        if (!normalized.isEmpty()) {
            for (String customerId : normalized.replace("[", "").split("]")) {
                if (!customerId.isEmpty()) {
                    if ("UserCustomer".equals(relation.getType())) {
                        validateCustomerId(customerId);
                    } else {
                        validateDepotId(customerId);
                    }
                }
            }
        }
    }

    private UserBusiness writableRelation(UserBusiness source) {
        UserBusiness target = new UserBusiness();
        target.setId(source.getId());
        target.setType(source.getType());
        target.setKeyId(source.getKeyId());
        target.setValue(source.getValue());
        return target;
    }

    private void validateUserId(String userId) throws Exception {
        Long id = parsePositiveId(userId, "用户ID不合法");
        if (userService.getUser(id) == null) {
            throw invalidRelation("用户不存在");
        }
    }

    private Long parsePositiveId(String value, String message) {
        try {
            Long id = Long.valueOf(value);
            if (id <= 0) {
                throw invalidRelation(message);
            }
            return id;
        } catch (NumberFormatException e) {
            throw invalidRelation(message);
        }
    }

    private BusinessRunTimeException invalidRelation(String detail) {
        return new BusinessRunTimeException(ExceptionConstants.SUPPLIER_INVALID_CODE,
                String.format(ExceptionConstants.SUPPLIER_INVALID_MSG, detail));
    }
}
