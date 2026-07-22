package com.jsh.erp.service;

import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.Organization;
import com.jsh.erp.datasource.entities.OrganizationExample;
import com.jsh.erp.datasource.entities.OrgaUserRelExample;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.OrganizationMapper;
import com.jsh.erp.datasource.mappers.OrganizationMapperEx;
import com.jsh.erp.datasource.mappers.OrgaUserRelMapper;
import com.jsh.erp.datasource.vo.TreeNode;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.exception.JshException;
import com.jsh.erp.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @Author: jsh
 */
@Service
public class OrganizationService {
    private Logger logger = LoggerFactory.getLogger(OrganizationService.class);
    private static final String ORGANIZATION_URL = "/system/organization";
    private static final String USER_URL = "/system/user";
    private static final String EDIT_BUTTON_CODE = "1";
    private static final Pattern SORT_PATTERN = Pattern.compile("^[0-9]{1,10}$");

    @Resource
    private OrganizationMapper organizationMapper;
    @Resource
    private OrganizationMapperEx organizationMapperEx;
    @Resource
    private OrgaUserRelMapper orgaUserRelMapper;
    @Resource
    private UserService userService;
    @Resource
    private LogService logService;

    public Organization getOrganization(long id) throws Exception {
        return organizationMapper.selectByPrimaryKey(id);
    }

    public List<Organization> getOrganizationListByIds(String ids)throws Exception {
        List<Long> idList = StringUtil.strToLongList(ids);
        List<Organization> list = new ArrayList<>();
        try{
            OrganizationExample example = new OrganizationExample();
            example.createCriteria().andIdIn(idList);
            list = organizationMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int insertOrganization(JSONObject obj, HttpServletRequest request)throws Exception {
        checkEditPermission();
        lockOrganizationWrite();
        Organization organization = buildOrganization(obj, null);
        validateOrganization(organization, null);
        ensureUnique(organization);
        organization.setCreateTime(new Date());
        organization.setUpdateTime(new Date());
        try{
            int result=organizationMapper.insertSelective(organization);
            logService.insertLog("部门",
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_ADD).append(organization.getOrgAbr()).toString(),request);
            return result;
        }catch(DuplicateKeyException e) {
            throw duplicateOrganization(e);
        }catch(BusinessRunTimeException e) {
            throw e;
        }catch(Exception e){
            JshException.writeFail(logger, e);
            return 0;
        }
    }
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int updateOrganization(JSONObject obj, HttpServletRequest request)throws Exception {
        checkEditPermission();
        lockOrganizationWrite();
        Long id = obj.getLong("id");
        Organization existing = id == null ? null : getActiveOrganization(id);
        if(existing == null) {
            throw invalidOrganization("部门不存在");
        }
        Organization organization = buildOrganization(obj, existing);
        validateOrganization(organization, id);
        ensureUnique(organization);
        organization.setUpdateTime(new Date());
        try{
            int result=organizationMapperEx.editOrganization(organization);
            logService.insertLog("部门",
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append(organization.getOrgAbr()).toString(), request);
            return result;
        }catch(DuplicateKeyException e) {
            throw duplicateOrganization(e);
        }catch(BusinessRunTimeException e) {
            throw e;
        }catch(Exception e){
            JshException.writeFail(logger, e);
            return 0;
        }
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int deleteOrganization(Long id, HttpServletRequest request)throws Exception {
        return batchDeleteOrganizationByIds(id.toString());
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteOrganization(String ids, HttpServletRequest request)throws Exception {
        return batchDeleteOrganizationByIds(ids);
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteOrganizationByIds(String ids) throws Exception{
        checkEditPermission();
        lockOrganizationWrite();
        List<Long> idList = parseIds(ids);
        if(idList.isEmpty()) {
            return 0;
        }
        String[] idArray = idList.stream().map(String::valueOf).toArray(String[]::new);
        List<Organization> selected = getActiveOrganizations(idList);
        if(selected.size() != idList.size()) {
            throw invalidOrganization("部门不存在或已删除");
        }
        StringBuffer sb = new StringBuffer();
        sb.append(BusinessConstants.LOG_OPERATION_TYPE_DELETE);
        for(Organization organization: selected){
            sb.append("[").append(organization.getOrgAbr()).append("]");
        }
        List <Organization> organList = organizationMapperEx.getOrganizationByParentIds(idArray);
        Set<Long> selectedIds = new HashSet<>(idList);
        boolean hasUnselectedChild = organList != null && organList.stream()
                .anyMatch(child -> !selectedIds.contains(child.getId()));
        if(hasUnselectedChild) {
            //如果存在子部门则不能删除
            logger.error("异常码[{}],异常提示[{}]",
                    ExceptionConstants.ORGANIZATION_CHILD_NOT_ALLOWED_DELETE_CODE,ExceptionConstants.ORGANIZATION_CHILD_NOT_ALLOWED_DELETE_MSG);
            throw new BusinessRunTimeException(ExceptionConstants.ORGANIZATION_CHILD_NOT_ALLOWED_DELETE_CODE,
                    ExceptionConstants.ORGANIZATION_CHILD_NOT_ALLOWED_DELETE_MSG);
        }
        OrgaUserRelExample relationExample = new OrgaUserRelExample();
        relationExample.createCriteria().andOrgaIdIn(idList)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        if(orgaUserRelMapper.countByExample(relationExample) > 0) {
            throw new BusinessRunTimeException(ExceptionConstants.ORGANIZATION_USER_NOT_ALLOWED_DELETE_CODE,
                    ExceptionConstants.ORGANIZATION_USER_NOT_ALLOWED_DELETE_MSG);
        }
        logService.insertLog("部门", sb.toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        User userInfo=userService.getCurrentUser();
        return organizationMapperEx.batchDeleteOrganizationByIds(
                new Date(),userInfo==null?null:userInfo.getId(),idArray);
    }

    public int checkIsNameExist(Long id, String name)throws Exception {
        checkEditPermission();
        if(name == null || name.trim().isEmpty()) {
            return 0;
        }
        OrganizationExample example = new OrganizationExample();
        OrganizationExample.Criteria criteria = example.createCriteria().andOrgAbrEqualTo(name.trim())
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        if(id != null && id > 0) {
            criteria.andIdNotEqualTo(id);
        }
        List<Organization> list=null;
        try{
            list= organizationMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list==null?0:list.size();
    }

    public List<TreeNode> getOrganizationTree(Long id)throws Exception {
        List<TreeNode> list=null;
        try{
            list=organizationMapperEx.getNodeTree(id);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public void checkReadPermission() throws Exception {
        User user = userService.getCurrentUser();
        if(!userService.hasFunctionPermission(user == null ? null : user.getId(), ORGANIZATION_URL)) {
            throw permissionDenied();
        }
    }

    public void checkSelectionPermission() throws Exception {
        User user = userService.getCurrentUser();
        Long userId = user == null ? null : user.getId();
        if(!userService.hasFunctionPermission(userId, ORGANIZATION_URL)
                && !userService.hasFunctionPermission(userId, USER_URL)) {
            throw permissionDenied();
        }
    }

    public void checkEditPermission() throws Exception {
        User user = userService.getCurrentUser();
        if(!userService.hasButtonPermission(user == null ? null : user.getId(), ORGANIZATION_URL, EDIT_BUTTON_CODE)) {
            throw permissionDenied();
        }
    }

    public List<Organization> findById(Long id) throws Exception{
        OrganizationExample example = new OrganizationExample();
        example.createCriteria().andIdEqualTo(id)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<Organization> list=null;
        try{
            list=organizationMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<Organization> findByParentId(Long parentId)throws Exception {
        List<Organization> list=null;
        if(parentId!=null){
            OrganizationExample example = new OrganizationExample();
            example.createCriteria().andIdEqualTo(parentId).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
            try{
                list=organizationMapper.selectByExample(example);
            }catch(Exception e){
                JshException.readFail(logger, e);
            }
        }
        return list;
    }

    public List<Organization> findByOrgNo(String orgNo)throws Exception {
        OrganizationExample example = new OrganizationExample();
        example.createCriteria().andOrgNoEqualTo(orgNo).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<Organization> list=null;
        try{
            list=organizationMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }
    /**
     * create by: cjl
     * description:
     *  检查部门编号是否已经存在
     * create time: 2019/3/7 10:01
     * @Param: orgNo
     * @return void
     */
    public void checkOrgNoIsExists(String orgNo,Long id)throws Exception {
        List<Organization> orgList=findByOrgNo(orgNo);
        if(orgList!=null&&orgList.size()>0){
            if(orgList.size()>1){
                logger.error("异常码[{}],异常提示[{}],参数,orgNo[{}]",
                        ExceptionConstants.ORGANIZATION_NO_ALREADY_EXISTS_CODE,ExceptionConstants.ORGANIZATION_NO_ALREADY_EXISTS_MSG,orgNo);
                //获取的数据条数大于1，部门编号已存在
                throw new BusinessRunTimeException(ExceptionConstants.ORGANIZATION_NO_ALREADY_EXISTS_CODE,
                        ExceptionConstants.ORGANIZATION_NO_ALREADY_EXISTS_MSG);
            }
            if(id!=null){
                if(!orgList.get(0).getId().equals(id)){
                    //数据条数等于1，但是和编辑的数据的id不相同
                    logger.error("异常码[{}],异常提示[{}],参数,orgNo[{}],id[{}]",
                            ExceptionConstants.ORGANIZATION_NO_ALREADY_EXISTS_CODE,ExceptionConstants.ORGANIZATION_NO_ALREADY_EXISTS_MSG,orgNo,id);
                    throw new BusinessRunTimeException(ExceptionConstants.ORGANIZATION_NO_ALREADY_EXISTS_CODE,
                            ExceptionConstants.ORGANIZATION_NO_ALREADY_EXISTS_MSG);
                }
            }else{
                logger.error("异常码[{}],异常提示[{}],参数,orgNo[{}]",
                        ExceptionConstants.ORGANIZATION_NO_ALREADY_EXISTS_CODE,ExceptionConstants.ORGANIZATION_NO_ALREADY_EXISTS_MSG,orgNo);
                //数据条数等于1，但此时是新增
                throw new BusinessRunTimeException(ExceptionConstants.ORGANIZATION_NO_ALREADY_EXISTS_CODE,
                        ExceptionConstants.ORGANIZATION_NO_ALREADY_EXISTS_MSG);
            }
        }

    }

    /**
     * 根据父级id递归获取子集组织id
     * @return
     */
    public List<Long> getOrgIdByParentId(Long orgId) {
        List<Long> idList = new ArrayList<>();
        OrganizationExample example = new OrganizationExample();
        example.createCriteria().andIdEqualTo(orgId).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<Organization> orgList = organizationMapper.selectByExample(example);
        if(orgList!=null && orgList.size()>0) {
            idList.add(orgId);
            getOrgIdByParentNo(idList, orgList.get(0).getId());
        }
        return idList;
    }

    /**
     * 根据组织编号递归获取下级编号
     * @param id
     * @return
     */
    public void getOrgIdByParentNo(List<Long> idList,Long id) {
        getOrgIdByParentNo(idList, id, new HashSet<>(idList));
    }

    private void getOrgIdByParentNo(List<Long> idList, Long id, Set<Long> visited) {
        OrganizationExample example = new OrganizationExample();
        example.createCriteria().andParentIdEqualTo(id).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<Organization> orgList = organizationMapper.selectByExample(example);
        if(orgList!=null && orgList.size()>0) {
            for(Organization o: orgList) {
                if(visited.add(o.getId())) {
                    idList.add(o.getId());
                    getOrgIdByParentNo(idList, o.getId(), visited);
                }
            }
        }
    }

    private Organization buildOrganization(JSONObject obj, Organization existing) throws Exception {
        Organization organization = new Organization();
        organization.setId(existing == null ? null : existing.getId());
        organization.setOrgAbr(readString(obj, "orgAbr", existing == null ? null : existing.getOrgAbr()));
        organization.setOrgNo(readString(obj, "orgNo", existing == null ? null : existing.getOrgNo()));
        organization.setSort(readString(obj, "sort", existing == null ? null : existing.getSort()));
        organization.setRemark(readString(obj, "remark", existing == null ? null : existing.getRemark()));
        organization.setParentId(obj.containsKey("parentId") ? obj.getLong("parentId")
                : existing == null ? null : existing.getParentId());
        User currentUser = userService.getCurrentUser();
        organization.setTenantId(existing == null
                ? (currentUser == null || currentUser.getTenantId() == null ? 0L : currentUser.getTenantId())
                : existing.getTenantId());
        organization.setDeleteFlag(BusinessConstants.DELETE_FLAG_EXISTS);
        organization.setCreateTime(existing == null ? null : existing.getCreateTime());
        return organization;
    }

    private String readString(JSONObject obj, String key, String fallback) {
        return obj.containsKey(key) ? trimToNull(obj.getString(key)) : fallback;
    }

    private String trimToNull(String value) {
        if(value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private void validateOrganization(Organization organization, Long currentId) throws Exception {
        if(organization.getOrgAbr() == null || organization.getOrgAbr().length() > 20) {
            throw invalidOrganization("名称不能为空且不能超过20个字符");
        }
        if(organization.getOrgNo() == null || organization.getOrgNo().length() > 20) {
            throw invalidOrganization("编号不能为空且不能超过20个字符");
        }
        if(organization.getSort() != null && !SORT_PATTERN.matcher(organization.getSort()).matches()) {
            throw invalidOrganization("排序只能是1至10位数字");
        }
        if(organization.getRemark() != null && organization.getRemark().length() > 500) {
            throw invalidOrganization("备注不能超过500个字符");
        }
        validateParentChain(organization, currentId);
    }

    private void validateParentChain(Organization organization, Long currentId) throws Exception {
        Long parentId = organization.getParentId();
        Set<Long> visited = new HashSet<>();
        while(parentId != null) {
            if(parentId <= 0 || Objects.equals(parentId, currentId) || !visited.add(parentId)) {
                throw invalidOrganization("上级部门层级不合法");
            }
            Organization parent = getActiveOrganization(parentId);
            if(parent == null || !Objects.equals(parent.getTenantId(), organization.getTenantId())) {
                throw invalidOrganization("上级部门不存在或不属于当前租户");
            }
            parentId = parent.getParentId();
        }
    }

    private void ensureUnique(Organization organization) {
        OrganizationExample nameExample = new OrganizationExample();
        OrganizationExample.Criteria nameCriteria = nameExample.createCriteria()
                .andOrgAbrEqualTo(organization.getOrgAbr())
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        OrganizationExample noExample = new OrganizationExample();
        OrganizationExample.Criteria noCriteria = noExample.createCriteria()
                .andOrgNoEqualTo(organization.getOrgNo())
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        if(organization.getId() != null) {
            nameCriteria.andIdNotEqualTo(organization.getId());
            noCriteria.andIdNotEqualTo(organization.getId());
        }
        if(organizationMapper.countByExample(nameExample) > 0) {
            throw new BusinessRunTimeException(ExceptionConstants.ORGANIZATION_NAME_ALREADY_EXISTS_CODE,
                    ExceptionConstants.ORGANIZATION_NAME_ALREADY_EXISTS_MSG);
        }
        if(organizationMapper.countByExample(noExample) > 0) {
            throw new BusinessRunTimeException(ExceptionConstants.ORGANIZATION_NO_ALREADY_EXISTS_CODE,
                    ExceptionConstants.ORGANIZATION_NO_ALREADY_EXISTS_MSG);
        }
    }

    private Organization getActiveOrganization(Long id) throws Exception {
        Organization organization = id == null ? null : getOrganization(id);
        return organization == null || BusinessConstants.DELETE_FLAG_DELETED.equals(organization.getDeleteFlag())
                ? null : organization;
    }

    private List<Organization> getActiveOrganizations(List<Long> ids) {
        if(ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        OrganizationExample example = new OrganizationExample();
        example.createCriteria().andIdIn(ids)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        return organizationMapper.selectByExample(example);
    }

    private List<Long> parseIds(String ids) {
        try {
            List<Long> result = new ArrayList<>(new LinkedHashSet<>(StringUtil.strToLongList(ids)));
            if(result.isEmpty() || result.stream().anyMatch(id -> id == null || id <= 0)) {
                throw invalidOrganization("部门ID不合法");
            }
            return result;
        } catch(BusinessRunTimeException e) {
            throw e;
        } catch(Exception e) {
            throw invalidOrganization("部门ID不合法");
        }
    }

    private void lockOrganizationWrite() throws Exception {
        User user = userService.getCurrentUser();
        organizationMapperEx.lockOrganizationWrite(user == null || user.getTenantId() == null ? 0L : user.getTenantId());
    }

    private BusinessRunTimeException duplicateOrganization(DuplicateKeyException e) {
        String message = e.getMessage() == null ? "" : e.getMessage();
        if(message.contains("org_no")) {
            return new BusinessRunTimeException(ExceptionConstants.ORGANIZATION_NO_ALREADY_EXISTS_CODE,
                    ExceptionConstants.ORGANIZATION_NO_ALREADY_EXISTS_MSG);
        }
        return new BusinessRunTimeException(ExceptionConstants.ORGANIZATION_NAME_ALREADY_EXISTS_CODE,
                ExceptionConstants.ORGANIZATION_NAME_ALREADY_EXISTS_MSG);
    }

    private BusinessRunTimeException permissionDenied() {
        return new BusinessRunTimeException(ExceptionConstants.ORGANIZATION_PERMISSION_CODE,
                ExceptionConstants.ORGANIZATION_PERMISSION_MSG);
    }

    private BusinessRunTimeException invalidOrganization(String reason) {
        return new BusinessRunTimeException(ExceptionConstants.ORGANIZATION_INVALID_CODE,
                String.format(ExceptionConstants.ORGANIZATION_INVALID_MSG, reason));
    }
}
