package com.jsh.erp.service;

import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.datasource.entities.OrgaUserRel;
import com.jsh.erp.datasource.entities.OrgaUserRelExample;
import com.jsh.erp.datasource.entities.Organization;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.OrgaUserRelMapper;
import com.jsh.erp.datasource.mappers.OrgaUserRelMapperEx;
import com.jsh.erp.exception.JshException;
import com.jsh.erp.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Description
 *
 * @Author: cjl
 * @Date: 2019/3/11 18:11
 */
@Service
public class OrgaUserRelService {
    private Logger logger = LoggerFactory.getLogger(OrganizationService.class);

    @Resource
    private OrgaUserRelMapper orgaUserRelMapper;
    @Resource
    private OrgaUserRelMapperEx orgaUserRelMapperEx;
    @Resource
    private UserService userService;
    @Resource
    private OrganizationService organizationService;
    @Resource
    private LogService logService;

    public OrgaUserRel getOrgaUserRel(long id) throws Exception{
        return orgaUserRelMapper.selectByPrimaryKey(id);
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int insertOrgaUserRel(JSONObject obj, HttpServletRequest request) throws Exception{
        OrgaUserRel orgaUserRel = JSONObject.parseObject(obj.toJSONString(), OrgaUserRel.class);
        int result=0;
        try{
            result=orgaUserRelMapper.insertSelective(orgaUserRel);
            logService.insertLog("用户与部门关系", BusinessConstants.LOG_OPERATION_TYPE_ADD, request);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int updateOrgaUserRel(JSONObject obj, HttpServletRequest request) throws Exception{
        OrgaUserRel orgaUserRel = JSONObject.parseObject(obj.toJSONString(), OrgaUserRel.class);
        int result=0;
        try{
            result=orgaUserRelMapper.updateByPrimaryKeySelective(orgaUserRel);
            logService.insertLog("用户与部门关系",
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append(orgaUserRel.getId()).toString(), request);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int deleteOrgaUserRel(Long id, HttpServletRequest request)throws Exception {
        int result=0;
        try{
            result=orgaUserRelMapper.deleteByPrimaryKey(id);
            logService.insertLog("用户与部门关系",
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_DELETE).append(id).toString(), request);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteOrgaUserRel(String ids, HttpServletRequest request)throws Exception {
        List<Long> idList = StringUtil.strToLongList(ids);
        OrgaUserRelExample example = new OrgaUserRelExample();
        example.createCriteria().andIdIn(idList);
        int result=0;
        try{
            result=orgaUserRelMapper.deleteByExample(example);
            logService.insertLog("用户与部门关系", "批量删除,id集:" + ids, request);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }
    /**
     * create by: cjl
     * description:
     *  新增部门用户关联关系,反显id
     * create time: 2019/3/12 9:40
     * @Param: orgaUserRel
     * @return void
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public OrgaUserRel addOrgaUserRel(OrgaUserRel orgaUserRel) throws Exception{
        validateRelation(orgaUserRel, false);
        Date date = new Date();
        User userInfo=userService.getCurrentUser();
        //创建时间
        if(orgaUserRel.getCreateTime()==null){
            orgaUserRel.setCreateTime(date);
        }
        //创建人
        if(orgaUserRel.getCreator()==null){
            orgaUserRel.setCreator(userInfo==null?null:userInfo.getId());
        }
        //更新时间
        if(orgaUserRel.getUpdateTime()==null){
            orgaUserRel.setUpdateTime(date);
        }
        //更新人
        if(orgaUserRel.getUpdater()==null){
            orgaUserRel.setUpdater(userInfo==null?null:userInfo.getId());
        }
        orgaUserRel.setDeleteFlag(BusinessConstants.DELETE_FLAG_EXISTS);
        int result=0;
        try{
            result=orgaUserRelMapperEx.addOrgaUserRel(orgaUserRel);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        if(result>0){
            return orgaUserRel;
        }
        return null;
    }
    /**
     * create by: cjl
     * description:
     *  更新部门用户关联关系
     * create time: 2019/3/12 9:40
     * @Param: orgaUserRel
     * @return void
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public OrgaUserRel updateOrgaUserRel(OrgaUserRel orgaUserRel) throws Exception{
        validateRelation(orgaUserRel, true);
        User userInfo=userService.getCurrentUser();
        //更新时间
        if(orgaUserRel.getUpdateTime()==null){
            orgaUserRel.setUpdateTime(new Date());
        }
        //更新人
        if(orgaUserRel.getUpdater()==null){
            orgaUserRel.setUpdater(userInfo==null?null:userInfo.getId());
        }
        int result=0;
        try{
            result=orgaUserRelMapperEx.updateOrgaUserRel(orgaUserRel);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        if(result>0){
            return orgaUserRel;
        }
        return null;
    }

    /**
     * 根据用户id获取用户id列表
     * @param userId
     * @return
     * @throws Exception
     */
    public String getUserIdListByUserId(Long userId) throws Exception{
        String users = "";
        OrgaUserRelExample example = new OrgaUserRelExample();
        example.createCriteria().andUserIdEqualTo(userId)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<OrgaUserRel> list = orgaUserRelMapper.selectByExample(example);
        if(list!=null && list.size()>0) {
            OrgaUserRel our = list.get(0);
            List<Long> userIdList = getUserIdListByOrgId(our.getOrgaId());
            for(Long u: userIdList){
                users = users + u + ",";
            }
            if(users.length()>0){
                users = users.substring(0,users.length()-1);
            }
        }
        return users;
    }

    /**
     * 根据组织id获取所属的用户id列表（包含组织的递归）
     * @param orgId
     * @return
     */
    public List<Long> getUserIdListByOrgId(Long orgId) {
        List<Long> orgIdList = organizationService.getOrgIdByParentId(orgId);
        List<Long> userIdList = new ArrayList<Long>();
        OrgaUserRelExample example = new OrgaUserRelExample();
        if(orgIdList!=null && orgIdList.size()>0) {
            example.createCriteria().andOrgaIdIn(orgIdList).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        } else {
            return userIdList;
        }
        List<OrgaUserRel> list = orgaUserRelMapper.selectByExample(example);
        if(list!=null && list.size()>0) {
            for(OrgaUserRel our: list) {
                if(our.getUserId() != null) {
                    userIdList.add(our.getUserId());
                }
            }
        }
        return new ArrayList<>(new LinkedHashSet<>(userIdList));
    }

    private void validateRelation(OrgaUserRel relation, boolean update) throws Exception {
        if(relation == null || relation.getUserId() == null) {
            throw invalidRelation();
        }
        if(relation.getOrgaId() != null) {
            Organization organization = organizationService.getOrganization(relation.getOrgaId());
            User currentUser = userService.getCurrentUser();
            Long currentTenantId = currentUser == null ? null : currentUser.getTenantId();
            if(organization == null || BusinessConstants.DELETE_FLAG_DELETED.equals(organization.getDeleteFlag())
                    || (currentTenantId != null && currentTenantId != 0L
                    && !Objects.equals(currentTenantId, organization.getTenantId()))) {
                throw invalidRelation();
            }
        }
        if(update) {
            OrgaUserRel existing = relation.getId() == null ? null : orgaUserRelMapper.selectByPrimaryKey(relation.getId());
            if(existing == null || BusinessConstants.DELETE_FLAG_DELETED.equals(existing.getDeleteFlag())
                    || !Objects.equals(existing.getUserId(), relation.getUserId())) {
                throw invalidRelation();
            }
        } else {
            OrgaUserRelExample example = new OrgaUserRelExample();
            example.createCriteria().andUserIdEqualTo(relation.getUserId())
                    .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
            if(orgaUserRelMapper.countByExample(example) > 0) {
                throw invalidRelation();
            }
        }
    }

    private com.jsh.erp.exception.BusinessRunTimeException invalidRelation() {
        return new com.jsh.erp.exception.BusinessRunTimeException(
                com.jsh.erp.constants.ExceptionConstants.ORGANIZATION_INVALID_CODE,
                String.format(com.jsh.erp.constants.ExceptionConstants.ORGANIZATION_INVALID_MSG, "用户部门关系不合法"));
    }
}
