package com.jsh.erp.service;

import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.*;
import com.jsh.erp.datasource.mappers.FunctionMapper;
import com.jsh.erp.datasource.mappers.FunctionMapperEx;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.exception.JshException;
import com.jsh.erp.utils.PageUtils;
import com.jsh.erp.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Service
public class FunctionService {
    private Logger logger = LoggerFactory.getLogger(FunctionService.class);

    @Resource
    private FunctionMapper functionsMapper;

    @Resource
    private FunctionMapperEx functionMapperEx;

    @Resource
    private UserService userService;

    @Resource
    private UserBusinessService userBusinessService;

    @Resource
    private SystemConfigService systemConfigService;

    @Resource
    private LogService logService;

    public Function getFunction(long id)throws Exception {
        Function result=null;
        try{
            result=functionsMapper.selectByPrimaryKey(id);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    public List<Function> getFunctionListByIds(String ids)throws Exception {
        List<Long> idList = StringUtil.strToLongList(ids);
        List<Function> list = new ArrayList<>();
        try{
            FunctionExample example = new FunctionExample();
            example.createCriteria().andIdIn(idList);
            list = functionsMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<Function> getFunction()throws Exception {
        FunctionExample example = new FunctionExample();
        example.createCriteria().andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<Function> list=null;
        try{
            list=functionsMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<FunctionEx> select(String name, String type)throws Exception {
        List<FunctionEx> list=Collections.emptyList();
        try{
            if(BusinessConstants.DEFAULT_MANAGER.equals(userService.getCurrentUser().getLoginName())) {
                PageUtils.startPage();
                list = functionMapperEx.selectByConditionFunction(name, type);
            }
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int insertFunction(JSONObject obj, HttpServletRequest request)throws Exception {
        Function functions = JSONObject.parseObject(obj.toJSONString(), Function.class);
        int result=0;
        try{
            if(BusinessConstants.DEFAULT_MANAGER.equals(userService.getCurrentUser().getLoginName())) {
                validateParentNumber(functions.getNumber(), functions.getParentNumber());
                functions.setState(false);
                functions.setType("电脑版");
                result = functionsMapper.insertSelective(functions);
                logService.insertLog("功能",
                        new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_ADD).append(functions.getName()).toString(), request);
            }
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int updateFunction(JSONObject obj, HttpServletRequest request) throws Exception{
        Function functions = JSONObject.parseObject(obj.toJSONString(), Function.class);
        int result=0;
        try{
            if(BusinessConstants.DEFAULT_MANAGER.equals(userService.getCurrentUser().getLoginName())) {
                // 禁止修改已创建菜单的number
                if(functions.getId() != null) {
                    Function existing = functionsMapper.selectByPrimaryKey(functions.getId());
                    if(existing != null && existing.getNumber() != null
                            && !existing.getNumber().equals(functions.getNumber())) {
                        throw new BusinessRunTimeException(ExceptionConstants.FUNCTIONS_INVALID_CODE,
                                String.format(ExceptionConstants.FUNCTIONS_INVALID_MSG, "菜单编号不可修改"));
                    }
                }
                validateParentNumber(functions.getNumber(), functions.getParentNumber());
                result = functionsMapper.updateByPrimaryKeySelective(functions);
                logService.insertLog("功能",
                        new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append(functions.getName()).toString(), request);
            }
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int deleteFunction(Long id, HttpServletRequest request)throws Exception {
        return batchDeleteFunctionByIds(id.toString());
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteFunction(String ids, HttpServletRequest request)throws Exception {
        return batchDeleteFunctionByIds(ids);
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteFunctionByIds(String ids)throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(BusinessConstants.LOG_OPERATION_TYPE_DELETE);
        List<Function> list = getFunctionListByIds(ids);
        for(Function functions: list){
            sb.append("[").append(functions.getName()).append("]");
        }
        User userInfo=userService.getCurrentUser();
        String [] idArray=ids.split(",");
        int result=0;
        try{
            if(BusinessConstants.DEFAULT_MANAGER.equals(userService.getCurrentUser().getLoginName())) {
                // 检查是否存在子菜单
                for(Function function : list) {
                    if(hasChildren(function.getNumber())) {
                        throw new BusinessRunTimeException(ExceptionConstants.FUNCTIONS_IN_USE_CODE,
                                ExceptionConstants.FUNCTIONS_IN_USE_MSG);
                    }
                }
                // 检查是否被角色权限引用
                List<Long> functionIds = new ArrayList<>();
                for(Function function : list) {
                    functionIds.add(function.getId());
                }
                if(isReferencedByRoleFunctions(functionIds)) {
                    throw new BusinessRunTimeException(ExceptionConstants.FUNCTIONS_IN_USE_CODE,
                            ExceptionConstants.FUNCTIONS_IN_USE_MSG);
                }
                result = functionMapperEx.batchDeleteFunctionByIds(new Date(), userInfo == null ? null : userInfo.getId(), idArray);
                logService.insertLog("功能", sb.toString(),
                        ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
            }
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    public int checkIsNameExist(Long id, String name)throws Exception {
        FunctionExample example = new FunctionExample();
        example.createCriteria().andIdNotEqualTo(id).andNameEqualTo(name).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<Function> list=null;
        try{
            list = functionsMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list==null?0:list.size();
    }

    public int checkIsNumberExist(Long id, String number)throws Exception {
        FunctionExample example = new FunctionExample();
        example.createCriteria().andIdNotEqualTo(id).andNumberEqualTo(number).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<Function> list=null;
        try{
            list = functionsMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list==null?0:list.size();
    }

    /**
     * 根据编号查询单个菜单（未删除）
     */
    public Function getByNumber(String number) throws Exception {
        FunctionExample example = new FunctionExample();
        example.createCriteria().andNumberEqualTo(number).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<Function> list = functionsMapper.selectByExample(example);
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }

    /**
     * 校验父菜单编号的合法性：父菜单必须存在，且不能形成循环引用
     */
    private void validateParentNumber(String number, String parentNumber) {
        if(parentNumber == null || parentNumber.trim().isEmpty() || "0".equals(parentNumber)) {
            return;
        }
        try {
            Function parent = getByNumber(parentNumber);
            if(parent == null) {
                throw new BusinessRunTimeException(ExceptionConstants.FUNCTIONS_INVALID_CODE,
                        String.format(ExceptionConstants.FUNCTIONS_INVALID_MSG, "上级菜单不存在"));
            }
            // 沿父节点链向上遍历检测循环
            String current = parentNumber;
            int depth = 0;
            while(current != null && !"0".equals(current)) {
                if(current.equals(number)) {
                    throw new BusinessRunTimeException(ExceptionConstants.FUNCTIONS_INVALID_CODE,
                            String.format(ExceptionConstants.FUNCTIONS_INVALID_MSG, "菜单层级存在循环引用"));
                }
                Function node = getByNumber(current);
                if(node == null) break;
                current = node.getParentNumber();
                depth++;
                if(depth > 10) {
                    throw new BusinessRunTimeException(ExceptionConstants.FUNCTIONS_INVALID_CODE,
                            String.format(ExceptionConstants.FUNCTIONS_INVALID_MSG, "菜单层级超过最大深度"));
                }
            }
        } catch(BusinessRunTimeException e) {
            throw e;
        } catch(Exception e) {
            logger.error("校验父菜单编号异常", e);
        }
    }

    /**
     * 检查菜单是否存在未删除的子菜单
     */
    private boolean hasChildren(String number) throws Exception {
        FunctionExample example = new FunctionExample();
        example.createCriteria().andParentNumberEqualTo(number).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<Function> list = functionsMapper.selectByExample(example);
        return list != null && !list.isEmpty();
    }

    /**
     * 检查菜单是否被角色权限引用
     */
    private boolean isReferencedByRoleFunctions(List<Long> functionIds) throws Exception {
        for(Long funId : functionIds) {
            List<Long> roleIds = userBusinessService.getUBKeyIdByTypeAndOneValue("RoleFunctions", funId.toString());
            if(roleIds != null && !roleIds.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public List<Function> getRoleFunction(String pNumber)throws Exception {
        FunctionExample example = new FunctionExample();
        example.createCriteria().andEnabledEqualTo(true).andParentNumberEqualTo(pNumber)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        example.setOrderByClause("Sort");
        List<Function> list=null;
        try{
            list = functionsMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<Function> findRoleFunction(String pnumber, List<Long> funIdList)throws Exception{
        List<Function> list=null;
        try{
            Boolean multiLevelApprovalFlag = systemConfigService.getMultiLevelApprovalFlag();
            FunctionExample example = new FunctionExample();
            FunctionExample.Criteria criteria = example.createCriteria();
            criteria.andEnabledEqualTo(true).andParentNumberEqualTo(pnumber)
                    .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
            if("0".equals(pnumber)) {
                if(!multiLevelApprovalFlag) {
                    criteria.andUrlNotEqualTo("/workflow");
                }
            }
            if(funIdList!=null && funIdList.size()>0) {
                criteria.andIdIn(funIdList);
            }
            example.setOrderByClause("Sort");
            list =functionsMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<Function> findByIds(String functionsIds)throws Exception{
        List<Long> idList = StringUtil.strToLongList(functionsIds);
        FunctionExample example = new FunctionExample();
        example.createCriteria().andEnabledEqualTo(true).andIdIn(idList).andPushBtnIsNotNull().andPushBtnNotEqualTo("")
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        example.setOrderByClause("Sort asc");
        List<Function> list=null;
        try{
            list =functionsMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    /**
     * 从格式如 [1][2][5] 的值中解析所有ID
     */
    private List<Long> parseIdsFromBracketValue(String value) {
        List<Long> ids = new ArrayList<>();
        if(StringUtil.isNotEmpty(value)) {
            String normalized = value.replace("][", ",").replace("[", "").replace("]", "");
            if(StringUtil.isNotEmpty(normalized)) {
                for(String item : normalized.split(",")) {
                    try {
                        ids.add(Long.parseLong(item.trim()));
                    } catch(NumberFormatException e) {
                        // skip invalid
                    }
                }
            }
        }
        return ids;
    }

    /**
     * 获取当前用户所属的租户所拥有的功能id列表（支持多角色取并集）
     * @return
     */
    public List<Long> getCurrentTenantFunIdList() throws Exception {
        Set<Long> funIdSet = new HashSet<>();
        User userInfo = userService.getCurrentUser();
        // Platform administrators belong to the virtual tenant 0. There is no tenant
        // owner/role relation for tenant 0, and callers grant admin access separately.
        if(BusinessConstants.DEFAULT_MANAGER.equals(userInfo.getLoginName())) {
            return new ArrayList<>();
        }
        //获取当前用户所有的角色id
        List<UserBusiness> roleList = userBusinessService.getBasicData(userInfo.getTenantId().toString(), "UserRole");
        if(roleList!=null && roleList.size()>0){
            String value = roleList.get(0).getValue();
            List<Long> roleIds = parseIdsFromBracketValue(value);
            for(Long roleId : roleIds) {
                List<UserBusiness> funList = userBusinessService.getBasicData(roleId.toString(), "RoleFunctions");
                if(funList!=null && funList.size()>0){
                    String fc = funList.get(0).getValue();
                    funIdSet.addAll(parseIdsFromBracketValue(fc));
                }
            }
        }
        userService.getRoleTypeByUserId(userInfo.getTenantId());
        return new ArrayList<>(funIdSet);
    }

    /**
     * 获取当前用户所属的租户所拥有的功能id的map
     * @return
     */
    public Map<Long, Long> getCurrentTenantFunIdMap() throws Exception {
        Map<Long, Long> funIdMap = new HashMap<>();
        List<Long> list = getCurrentTenantFunIdList();
        if(list.size()>0) {
            for (Long funId : list) {
                funIdMap.put(funId, funId);
            }
            //把一级菜单的id全都赋值给租户,解决漏掉枝干id的问题
            List<Long> firstNodeIdList = functionMapperEx.getFirstNodeIdList();
            for (Long firstNodeId : firstNodeIdList) {
                funIdMap.put(firstNodeId, firstNodeId);
            }
            return funIdMap;
        } else {
            return null;
        }
    }

    /**
     * 获取当前用户所拥有的功能id列表（支持多角色取并集）
     * @return
     */
    public List<Long> getCurrentUserFunIdList() throws Exception {
        Set<Long> funIdSet = new HashSet<>();
        User userInfo = userService.getCurrentUser();
        //获取当前用户所有的角色id
        List<UserBusiness> roleList = userBusinessService.getBasicData(userInfo.getId().toString(), "UserRole");
        if(roleList!=null && roleList.size()>0){
            String value = roleList.get(0).getValue();
            List<Long> roleIds = parseIdsFromBracketValue(value);
            for(Long roleId : roleIds) {
                List<UserBusiness> funList = userBusinessService.getBasicData(roleId.toString(), "RoleFunctions");
                if(funList!=null && funList.size()>0){
                    String fc = funList.get(0).getValue();
                    funIdSet.addAll(parseIdsFromBracketValue(fc));
                }
            }
        }
        userService.getRoleTypeByUserId(userInfo.getId());
        return new ArrayList<>(funIdSet);
    }

    /**
     * 获取当前用户所拥有的功能id的map
     * @return
     */
    public Map<Long, Long> getCurrentUserFunIdMap() throws Exception {
        Map<Long, Long> funIdMap = new HashMap<>();
        List<Long> list = getCurrentUserFunIdList();
        if(list.size()>0) {
            for(Long funId: list) {
                funIdMap.put(funId, funId);
            }
            return funIdMap;
        } else {
            return null;
        }
    }
}
