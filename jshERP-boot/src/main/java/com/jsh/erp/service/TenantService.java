package com.jsh.erp.service;

import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.Tenant;
import com.jsh.erp.datasource.entities.TenantEx;
import com.jsh.erp.datasource.entities.TenantExample;
import com.jsh.erp.datasource.entities.UserEx;
import com.jsh.erp.datasource.mappers.TenantMapper;
import com.jsh.erp.datasource.mappers.TenantMapperEx;
import com.jsh.erp.datasource.mappers.UserBusinessMapperEx;
import com.jsh.erp.datasource.mappers.UserMapperEx;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.exception.JshException;
import com.jsh.erp.utils.PageUtils;
import com.jsh.erp.utils.StringUtil;
import com.jsh.erp.utils.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;

@Service
public class TenantService {
    private Logger logger = LoggerFactory.getLogger(TenantService.class);

    @Resource
    private TenantMapper tenantMapper;

    @Resource
    private TenantMapperEx tenantMapperEx;

    @Resource
    private UserMapperEx userMapperEx;

    @Resource
    private UserBusinessMapperEx userBusinessMapperEx;

    @Resource
    private UserService userService;

    @Resource
    private LogService logService;

    @Resource
    private RedisService redisService;

    @Resource
    private com.jsh.erp.datasource.mappers.UserMapper userMapper;

    @Value("${manage.roleId}")
    private Integer manageRoleId;

    public Tenant getTenant(long id)throws Exception {
        Tenant result=null;
        try{
            result=tenantMapper.selectByPrimaryKey(id);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    public List<Tenant> getTenant()throws Exception {
        TenantExample example = new TenantExample();
        example.createCriteria().andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<Tenant> list=null;
        try{
            list=tenantMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<TenantEx> select(String loginName, String type, String enabled, String remark)throws Exception {
        List<TenantEx> list = Collections.emptyList();
        try{
            if(BusinessConstants.DEFAULT_MANAGER.equals(userService.getCurrentUser().getLoginName())) {
                PageUtils.startPage();
                list = tenantMapperEx.selectByConditionTenant(loginName, type, enabled, remark);
                if (null != list) {
                    for (TenantEx tenantEx : list) {
                        tenantEx.setCreateTimeStr(Tools.getCenternTime(tenantEx.getCreateTime()));
                        tenantEx.setExpireTimeStr(Tools.getCenternTime(tenantEx.getExpireTime()));
                    }
                }
            }
        } catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int insertTenant(JSONObject obj, HttpServletRequest request)throws Exception {
        UserEx ue = JSONObject.parseObject(obj.toJSONString(), UserEx.class);
        ue.setUsername(ue.getLoginName());
        userService.checkLoginName(ue); //检查登录名
        userService.registerUser(ue,manageRoleId,request);
        return 1;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int updateTenant(JSONObject obj, HttpServletRequest request)throws Exception {
        Tenant tenant = JSONObject.parseObject(obj.toJSONString(), Tenant.class);
        // 保护不可修改字段：tenantId 是整个租户数据隔离的边界，修改会导致所有业务表数据归属断裂
        tenant.setTenantId(null);
        tenant.setCreateTime(null);
        int result=0;
        try{
            if(BusinessConstants.DEFAULT_MANAGER.equals(userService.getCurrentUser().getLoginName())) {
                // 校验用户数量限制：不允许降低到当前启用用户数以下
                if (tenant.getUserNumLimit() != null && obj.get("tenantId") != null) {
                    Long tenantId = obj.getLong("tenantId");
                    int currentUserCount = userMapperEx.countActiveUsersByTenantId(tenantId);
                    if (tenant.getUserNumLimit() < currentUserCount) {
                        throw new BusinessRunTimeException(ExceptionConstants.TENANT_USER_LIMIT_UPDATE_CODE,
                                ExceptionConstants.TENANT_USER_LIMIT_UPDATE_MSG);
                    }
                }
                result = tenantMapper.updateByPrimaryKeySelective(tenant);
                //更新租户对应的角色
                if(obj.get("roleId")!=null) {
                    String ubValue = "[" + obj.getString("roleId") + "]";
                    userBusinessMapperEx.updateValueByTypeAndKeyId("UserRole", obj.getString("tenantId"), ubValue);
                }
            }
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int deleteTenant(Long id, HttpServletRequest request)throws Exception {
        int result=0;
        try{
            if(BusinessConstants.DEFAULT_MANAGER.equals(userService.getCurrentUser().getLoginName())) {
                result = tenantMapper.deleteByPrimaryKey(id);
            }
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteTenant(String ids, HttpServletRequest request)throws Exception {
        List<Long> idList = StringUtil.strToLongList(ids);
        TenantExample example = new TenantExample();
        example.createCriteria().andIdIn(idList);
        int result=0;
        try{
            if(BusinessConstants.DEFAULT_MANAGER.equals(userService.getCurrentUser().getLoginName())) {
                result = tenantMapper.deleteByExample(example);
            }
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    public int checkIsNameExist(Long id, String name)throws Exception {
        TenantExample example = new TenantExample();
        example.createCriteria().andIdNotEqualTo(id).andLoginNameEqualTo(name).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<Tenant> list=null;
        try{
            list= tenantMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list==null?0:list.size();
    }

    public Tenant getTenantByTenantId(long tenantId) {
        Tenant tenant = new Tenant();
        TenantExample example = new TenantExample();
        example.createCriteria().andTenantIdEqualTo(tenantId).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<Tenant> list = tenantMapper.selectByExample(example);
        if(list.size()>0) {
            tenant = list.get(0);
        }
        return tenant;
    }

    public Tenant getTenantByTenantCode(String tenantCode) {
        TenantExample example = new TenantExample();
        example.createCriteria().andTenantCodeEqualTo(tenantCode).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<Tenant> list = tenantMapper.selectByExample(example);
        if(list != null && list.size()>0) {
            return list.get(0);
        }
        return null;
    }

    public int batchSetStatus(Boolean status, String ids)throws Exception {
        int result=0;
        try{
            if(BusinessConstants.DEFAULT_MANAGER.equals(userService.getCurrentUser().getLoginName())) {
                String statusStr = "";
                if (status) {
                    statusStr = "批量启用";
                } else {
                    statusStr = "批量禁用";
                }
                logService.insertLog("用户",
                        new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append(ids).append("-").append(statusStr).toString(),
                        ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
                List<Long> idList = StringUtil.strToLongList(ids);
                Tenant tenant = new Tenant();
                tenant.setEnabled(status);
                TenantExample example = new TenantExample();
                example.createCriteria().andIdIn(idList);
                result = tenantMapper.updateByExampleSelective(tenant, example);
                // 禁用租户时，清除该租户所有用户的 Redis session，使已登录用户立即失效
                if (!status && result > 0) {
                    for (Long id : idList) {
                        Tenant t = tenantMapper.selectByPrimaryKey(id);
                        if (t != null && t.getTenantId() != null) {
                            deleteSessionsByTenantId(t.getTenantId());
                        }
                    }
                }
            }
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    /**
     * 清除指定租户下所有用户的 Redis session
     */
    private void deleteSessionsByTenantId(Long tenantId) {
        java.util.Set<String> tokens = redisService.redisTemplate.keys("*");
        for (String token : tokens) {
            if (redisService.redisTemplate.hasKey(token)
                    && redisService.redisTemplate.type(token) == org.springframework.data.redis.connection.DataType.HASH) {
                Object userIdValue = redisService.redisTemplate.opsForHash().get(token, "userId");
                if (userIdValue != null) {
                    try {
                        com.jsh.erp.datasource.entities.User user = userMapper.selectByPrimaryKey(Long.parseLong(userIdValue.toString()));
                        if (user != null && tenantId.equals(user.getTenantId())) {
                            redisService.redisTemplate.delete(token);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
    }
}
