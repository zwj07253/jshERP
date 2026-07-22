package com.jsh.erp.service;

import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.Role;
import com.jsh.erp.datasource.entities.RoleEx;
import com.jsh.erp.datasource.entities.RoleExample;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.RoleMapper;
import com.jsh.erp.datasource.mappers.RoleMapperEx;
import com.jsh.erp.datasource.mappers.UserBusinessMapperEx;
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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoleService {
    private Logger logger = LoggerFactory.getLogger(RoleService.class);
    @Resource
    private RoleMapper roleMapper;

    @Resource
    private RoleMapperEx roleMapperEx;
    @Resource
    private LogService logService;
    @Resource
    private UserService userService;
    @Resource
    private UserBusinessMapperEx userBusinessMapperEx;

    //超管的专用角色
    private static final Long MANAGE_ROLE_ID = 4L;
    private static final String ROLE_URL = "/system/role";
    private static final String USER_URL = "/system/user";
    private static final String EDIT_BUTTON_CODE = "1";
    private static final Set<String> ROLE_TYPES = new HashSet<>(Arrays.asList(
            BusinessConstants.ROLE_TYPE_PUBLIC,
            BusinessConstants.ROLE_TYPE_THIS_ORG,
            BusinessConstants.ROLE_TYPE_PRIVATE));

    public Role getRole(long id)throws Exception {
        Role result=null;
        try{
            result=roleMapper.selectByPrimaryKey(id);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    public List<Role> getRoleListByIds(String ids)throws Exception {
        List<Long> idList = StringUtil.strToLongList(ids);
        List<Role> list = new ArrayList<>();
        try{
            RoleExample example = new RoleExample();
            example.createCriteria().andIdIn(idList);
            list = roleMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<Role> allList()throws Exception {
        RoleExample example = new RoleExample();
        example.createCriteria().andEnabledEqualTo(true).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        example.setOrderByClause("sort asc, id desc");
        List<Role> list=null;
        try{
            list=roleMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<Role> tenantRoleList() {
        List<Role> list=null;
        try{
            if(BusinessConstants.DEFAULT_MANAGER.equals(userService.getCurrentUser().getLoginName())) {
                RoleExample example = new RoleExample();
                example.createCriteria().andEnabledEqualTo(true).andTenantIdIsNull().andIdNotEqualTo(MANAGE_ROLE_ID)
                        .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
                example.setOrderByClause("sort asc, id asc");
                list=roleMapper.selectByExample(example);
            }
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<RoleEx> select(String name, String description)throws Exception {
        checkReadPermission();
        List<RoleEx> list=null;
        try{
            PageUtils.startPage();
            list=roleMapperEx.selectByConditionRole(name, description);
            for(RoleEx roleEx: list) {
                String priceLimit = roleEx.getPriceLimit();
                if(StringUtil.isNotEmpty(priceLimit)) {
                    String priceLimitStr = priceLimit
                        .replace("1", "屏蔽首页采购价")
                        .replace("2", "屏蔽首页零售价")
                        .replace("3", "屏蔽首页销售价")
                        .replace("4", "屏蔽单据采购价")
                        .replace("5", "屏蔽单据零售价")
                        .replace("6", "屏蔽单据销售价")
                        .replace("7", "屏蔽库存成本价");
                    roleEx.setPriceLimitStr(priceLimitStr);
                }
            }
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int insertRole(JSONObject obj, HttpServletRequest request)throws Exception {
        checkEditPermission();
        lockRoleWrite();
        Role role = buildRole(obj, null);
        validateRole(role);
        ensureNameUnique(null, role.getName(), role.getTenantId());
        int result=0;
        try{
            result=roleMapper.insertSelective(role);
            logService.insertLog("角色",
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_ADD).append(role.getName()).toString(), request);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int updateRole(JSONObject obj, HttpServletRequest request) throws Exception{
        checkEditPermission();
        lockRoleWrite();
        Long id = obj.getLong("id");
        Role existing = id == null ? null : roleMapper.selectByPrimaryKey(id);
        if(existing == null || BusinessConstants.DELETE_FLAG_DELETED.equals(existing.getDeleteFlag())) {
            throw invalidRole("角色不存在或已删除");
        }
        Role role = buildRole(obj, existing);
        validateRole(role);
        ensureNameUnique(role.getId(), role.getName(), existing.getTenantId());
        int result=0;
        try{
            result=roleMapper.updateByPrimaryKeySelective(role);
            logService.insertLog("角色",
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append(role.getName()).toString(), request);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int deleteRole(Long id, HttpServletRequest request)throws Exception {
        checkEditPermission();
        return batchDeleteRoleByIds(id.toString());
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteRole(String ids, HttpServletRequest request) throws Exception{
        checkEditPermission();
        return batchDeleteRoleByIds(ids);
    }

    public int checkIsNameExist(Long id, String name) throws Exception{
        RoleExample example = new RoleExample();
        RoleExample.Criteria criteria = example.createCriteria().andIdNotEqualTo(id).andNameEqualTo(name)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        User currentUser = userService.getCurrentUser();
        Long tenantId = currentUser == null ? null : currentUser.getTenantId();
        if(tenantId == null || tenantId == 0L) {
            criteria.andTenantIdIsNull();
        } else {
            criteria.andTenantIdEqualTo(tenantId);
        }
        List<Role> list =null;
        try{
            list=roleMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list==null?0:list.size();
    }

    public List<Role> findUserRole()throws Exception{
        RoleExample example = new RoleExample();
        example.setOrderByClause("Id");
        example.createCriteria().andEnabledEqualTo(true).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<Role> list=null;
        try{
            list=roleMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }
    /**
     * create by: qiankunpingtai
     *  逻辑删除角色信息
     * create time: 2019/3/28 15:44
     * @Param: ids
     * @return int
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteRoleByIds(String ids) throws Exception{
        lockRoleWrite();
        List<Long> roleIds = parseIds(ids);
        if(roleIds.isEmpty()) {
            throw invalidRole("请选择要删除的角色");
        }
        if(roleIds.contains(MANAGE_ROLE_ID)) {
            throw invalidRole("系统管理员角色不能删除");
        }
        String normalizedIds = roleIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        List<Role> existingRoles = getRoleListByIds(normalizedIds);
        if(existingRoles.size() != roleIds.size()
                || existingRoles.stream().anyMatch(role -> BusinessConstants.DELETE_FLAG_DELETED.equals(role.getDeleteFlag()))) {
            throw invalidRole("角色不存在或已删除");
        }
        ensureRolesNotAssigned(roleIds, "删除");
        StringBuffer sb = new StringBuffer();
        sb.append(BusinessConstants.LOG_OPERATION_TYPE_DELETE);
        for(Role role: existingRoles){
            sb.append("[").append(role.getName()).append("]");
        }
        logService.insertLog("角色", sb.toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        User userInfo=userService.getCurrentUser();
        String [] idArray=roleIds.stream().map(String::valueOf).toArray(String[]::new);
        int result=0;
        try{
            result=roleMapperEx.batchDeleteRoleByIds(new Date(),userInfo==null?null:userInfo.getId(),idArray);
            userBusinessMapperEx.deleteRoleFunctionsByRoleIds(
                    roleIds.stream().map(String::valueOf).collect(Collectors.toList()));
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    public Role getRoleWithoutTenant(Long roleId) {
        return roleMapperEx.getRoleWithoutTenant(roleId);
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchSetStatus(Boolean status, String ids)throws Exception {
        checkEditPermission();
        lockRoleWrite();
        if(status == null) {
            throw invalidRole("角色状态不能为空");
        }
        List<Long> roleIds = parseIds(ids);
        if(roleIds.isEmpty()) {
            throw invalidRole("请选择要设置状态的角色");
        }
        if(!status && roleIds.contains(MANAGE_ROLE_ID)) {
            throw invalidRole("系统管理员角色不能禁用");
        }
        List<Role> roles = getRoleListByIds(roleIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        if(roles.size() != roleIds.size()
                || roles.stream().anyMatch(role -> BusinessConstants.DELETE_FLAG_DELETED.equals(role.getDeleteFlag()))) {
            throw invalidRole("角色不存在或已删除");
        }
        if(!status) {
            ensureRolesNotAssigned(roleIds, "禁用");
        }
        logService.insertLog("角色",
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_ENABLED).toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        Role role = new Role();
        role.setEnabled(status);
        RoleExample example = new RoleExample();
        example.createCriteria().andIdIn(roleIds);
        int result=0;
        try{
            result = roleMapper.updateByExampleSelective(role, example);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    public void checkReadPermission() throws Exception {
        User currentUser = userService.getCurrentUser();
        Long userId = currentUser == null ? null : currentUser.getId();
        if(!userService.hasFunctionPermission(userId, ROLE_URL)) {
            throw permissionDenied();
        }
    }

    public void checkSelectionPermission() throws Exception {
        User currentUser = userService.getCurrentUser();
        Long userId = currentUser == null ? null : currentUser.getId();
        if(!userService.hasFunctionPermission(userId, ROLE_URL)
                && !userService.hasFunctionPermission(userId, USER_URL)) {
            throw permissionDenied();
        }
    }

    public void checkAssignmentPermission() throws Exception {
        User currentUser = userService.getCurrentUser();
        Long userId = currentUser == null ? null : currentUser.getId();
        if(!userService.hasButtonPermission(userId, ROLE_URL, EDIT_BUTTON_CODE)
                && !userService.hasButtonPermission(userId, USER_URL, EDIT_BUTTON_CODE)) {
            throw permissionDenied();
        }
    }

    public void checkEditPermission() throws Exception {
        User currentUser = userService.getCurrentUser();
        Long userId = currentUser == null ? null : currentUser.getId();
        if(!userService.hasButtonPermission(userId, ROLE_URL, EDIT_BUTTON_CODE)) {
            throw permissionDenied();
        }
    }

    public Role requireActiveRole(Long roleId) throws Exception {
        Role role = roleId == null ? null : getRoleWithoutTenant(roleId);
        if(role == null || !Boolean.TRUE.equals(role.getEnabled())
                || BusinessConstants.DELETE_FLAG_DELETED.equals(role.getDeleteFlag())) {
            throw permissionDenied();
        }
        return role;
    }

    public Role requireManagedRole(Long roleId) throws Exception {
        Role role = roleId == null ? null : roleMapper.selectByPrimaryKey(roleId);
        if(role == null || BusinessConstants.DELETE_FLAG_DELETED.equals(role.getDeleteFlag())) {
            throw invalidRole("角色不存在或已删除");
        }
        return role;
    }

    public void validateAssignableUser(String userId) throws Exception {
        try {
            if(userService.getUser(Long.parseLong(userId)) == null) {
                throw invalidRole("用户不存在");
            }
        } catch(NumberFormatException e) {
            throw invalidRole("用户ID不合法");
        }
    }

    private Role buildRole(JSONObject obj, Role existing) throws Exception {
        Role role = new Role();
        if(existing != null) {
            role.setId(existing.getId());
        } else {
            User currentUser = userService.getCurrentUser();
            Long tenantId = currentUser == null ? null : currentUser.getTenantId();
            role.setTenantId(tenantId != null && tenantId == 0L ? null : tenantId);
            role.setEnabled(true);
            role.setDeleteFlag(BusinessConstants.DELETE_FLAG_EXISTS);
        }
        role.setName(obj.getString("name"));
        role.setType(obj.getString("type"));
        role.setPriceLimit(normalizePriceLimit(obj.getString("priceLimit")));
        role.setDescription(obj.getString("description"));
        role.setSort(obj.getString("sort"));
        return role;
    }

    private void validateRole(Role role) {
        if(StringUtil.isEmpty(role.getName()) || role.getName().length() < 2 || role.getName().length() > 30) {
            throw invalidRole("角色名称长度必须为2到30个字符");
        }
        if(!ROLE_TYPES.contains(role.getType())) {
            throw invalidRole("角色数据类型不合法");
        }
        if(role.getDescription() != null && role.getDescription().length() > 100) {
            throw invalidRole("角色备注不能超过100个字符");
        }
        if(role.getSort() != null && role.getSort().length() > 10) {
            throw invalidRole("角色排序不能超过10个字符");
        }
    }

    private String normalizePriceLimit(String priceLimit) {
        if(StringUtil.isEmpty(priceLimit)) {
            return null;
        }
        String compact = priceLimit.replace(",", "").replace(" ", "");
        Set<Character> values = new HashSet<>();
        for(char value : compact.toCharArray()) {
            if(value < '1' || value > '7') {
                throw invalidRole("价格屏蔽选项不合法");
            }
            values.add(value);
        }
        return values.stream().sorted().map(String::valueOf).collect(Collectors.joining(","));
    }

    private void ensureNameUnique(Long id, String name, Long tenantId) {
        RoleExample example = new RoleExample();
        RoleExample.Criteria criteria = example.createCriteria().andNameEqualTo(name)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        if(id != null) {
            criteria.andIdNotEqualTo(id);
        }
        if(tenantId == null) {
            criteria.andTenantIdIsNull();
        } else {
            criteria.andTenantIdEqualTo(tenantId);
        }
        if(roleMapper.countByExample(example) > 0) {
            throw invalidRole("角色名称已经存在");
        }
    }

    private void ensureRolesNotAssigned(List<Long> roleIds, String operation) {
        for(Long roleId : roleIds) {
            List<Long> userIds = userBusinessMapperEx.getUBKeyIdByTypeAndOneValue("UserRole", roleId.toString());
            if(userIds != null && !userIds.isEmpty()) {
                throw invalidRole("角色已分配给用户，不能" + operation);
            }
        }
    }

    private List<Long> parseIds(String ids) {
        try {
            return StringUtil.isEmpty(ids) ? new ArrayList<>() : StringUtil.strToLongList(ids);
        } catch(Exception e) {
            throw invalidRole("角色ID不合法");
        }
    }

    private void lockRoleWrite() throws Exception {
        User currentUser = userService.getCurrentUser();
        roleMapperEx.lockRoleWrite(currentUser == null ? 0L : currentUser.getTenantId());
    }

    private BusinessRunTimeException invalidRole(String detail) {
        return new BusinessRunTimeException(ExceptionConstants.SUPPLIER_INVALID_CODE,
                String.format(ExceptionConstants.SUPPLIER_INVALID_MSG, detail));
    }

    private BusinessRunTimeException permissionDenied() {
        return new BusinessRunTimeException(ExceptionConstants.SUPPLIER_PERMISSION_CODE,
                ExceptionConstants.SUPPLIER_PERMISSION_MSG);
    }

    /**
     * 根据权限进行屏蔽价格-首页
     * @param price
     * @param type
     * @return
     */
    public Object parseHomePriceByLimit(BigDecimal price, String type, String priceLimit, String emptyInfo, HttpServletRequest request) throws Exception {
        if(StringUtil.isNotEmpty(priceLimit)) {
            if("buy".equals(type) && priceLimit.contains("1")) {
                return emptyInfo;
            }
            if("retail".equals(type) && priceLimit.contains("2")) {
                return emptyInfo;
            }
            if("sale".equals(type) && priceLimit.contains("3")) {
                return emptyInfo;
            }
        }
        return price;
    }

    /**
     * 根据权限进行屏蔽价格-单据
     * @param price
     * @param billCategory
     * @param priceLimit
     * @param request
     * @return
     * @throws Exception
     */
    public BigDecimal parseBillPriceByLimit(BigDecimal price, String billCategory, String priceLimit, HttpServletRequest request) throws Exception {
        if(StringUtil.isNotEmpty(priceLimit)) {
            if("buy".equals(billCategory) && priceLimit.contains("4")) {
                return BigDecimal.ZERO;
            }
            if("retail".equals(billCategory) && priceLimit.contains("5")) {
                return BigDecimal.ZERO;
            }
            if("sale".equals(billCategory) && priceLimit.contains("6")) {
                return BigDecimal.ZERO;
            }
        }
        return price;
    }

    /**
     * 根据权限进行屏蔽成本价-库存报表
     * @param price
     * @param priceLimit
     * @param request
     * @return
     * @throws Exception
     */
    public BigDecimal parseStockPriceByLimit(BigDecimal price, String priceLimit, HttpServletRequest request) throws Exception {
        if(StringUtil.isNotEmpty(priceLimit)) {
            if(priceLimit.contains("7")) {
                return null;
            }
        }
        return price;
    }

    /**
     * 根据权限进行屏蔽价格-物料
     * @param price
     * @param type
     * @return
     */
    public Object parseMaterialPriceByLimit(BigDecimal price, String type, String emptyInfo, HttpServletRequest request) throws Exception {
        Long userId = userService.getUserId(request);
        String priceLimit = userService.getRoleTypeByUserId(userId).getPriceLimit();
        if(StringUtil.isNotEmpty(priceLimit)) {
            if("buy".equals(type) && priceLimit.contains("4")) {
                return emptyInfo;
            }
            if("retail".equals(type) && priceLimit.contains("5")) {
                return emptyInfo;
            }
            if("sale".equals(type) && priceLimit.contains("6")) {
                return emptyInfo;
            }
        }
        return price;
    }

    public String getCurrentPriceLimit(HttpServletRequest request) throws Exception {
        Long userId = userService.getUserId(request);
        return userService.getRoleTypeByUserId(userId).getPriceLimit();
    }
}
