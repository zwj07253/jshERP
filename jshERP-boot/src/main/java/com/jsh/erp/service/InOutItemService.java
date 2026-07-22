package com.jsh.erp.service;

import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.AccountItem;
import com.jsh.erp.datasource.entities.InOutItem;
import com.jsh.erp.datasource.entities.InOutItemExample;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.AccountItemMapperEx;
import com.jsh.erp.datasource.mappers.InOutItemMapper;
import com.jsh.erp.datasource.mappers.InOutItemMapperEx;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.exception.JshException;
import com.jsh.erp.utils.PageUtils;
import com.jsh.erp.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

@Service
public class InOutItemService {
    private Logger logger = LoggerFactory.getLogger(InOutItemService.class);
    private static final String IN_OUT_ITEM_URL = "/system/in_out_item";
    private static final String INCOME_URL = "/financial/item_in";
    private static final String EXPENSE_URL = "/financial/item_out";
    private static final String EDIT_BUTTON_CODE = "1";

    @Resource
    private InOutItemMapper inOutItemMapper;

    @Resource
    private InOutItemMapperEx inOutItemMapperEx;
    @Resource
    private UserService userService;
    @Resource
    private LogService logService;
    @Resource
    private AccountItemMapperEx accountItemMapperEx;

    public InOutItem getInOutItem(long id)throws Exception {
        InOutItem result=null;
        try{
            InOutItemExample example = new InOutItemExample();
            example.createCriteria().andIdEqualTo(id)
                    .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
            List<InOutItem> list = inOutItemMapper.selectByExample(example);
            result = list.isEmpty() ? null : list.get(0);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    public List<InOutItem> getInOutItemListByIds(String ids)throws Exception {
        List<Long> idList = StringUtil.strToLongList(ids);
        List<InOutItem> list = new ArrayList<>();
        try{
            InOutItemExample example = new InOutItemExample();
            example.createCriteria().andIdIn(idList)
                    .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
            list = inOutItemMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<InOutItem> getInOutItem()throws Exception {
        InOutItemExample example = new InOutItemExample();
        example.createCriteria().andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<InOutItem> list=null;
        try{
            list=inOutItemMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<InOutItem> select(String name, String type, String remark)throws Exception {
        List<InOutItem> list=null;
        try{
            PageUtils.startPage();
            list=inOutItemMapperEx.selectByConditionInOutItem(name, type, remark);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int insertInOutItem(JSONObject obj, HttpServletRequest request)throws Exception {
        checkEditPermission();
        lockInOutItemWrite();
        InOutItem inOutItem = buildInOutItem(obj, null);
        validateInOutItem(inOutItem);
        ensureUnique(inOutItem);
        int result=0;
        try{
            inOutItem.setEnabled(true);
            result=inOutItemMapper.insertSelective(inOutItem);
            logService.insertLog("收支项目",
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_ADD).append(inOutItem.getName()).toString(), request);
        }catch(DuplicateKeyException e) {
            throw duplicateName();
        }catch(BusinessRunTimeException e) {
            throw e;
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int updateInOutItem(JSONObject obj, HttpServletRequest request)throws Exception {
        checkEditPermission();
        lockInOutItemWrite();
        Long id = obj.getLong("id");
        InOutItem existing = id == null ? null : getInOutItem(id);
        if (existing == null) {
            throw invalidInOutItem("收支项目不存在");
        }
        InOutItem inOutItem = buildInOutItem(obj, existing);
        validateInOutItem(inOutItem);
        if (!Objects.equals(existing.getType(), inOutItem.getType()) && isInUse(existing.getId())) {
            throw new BusinessRunTimeException(ExceptionConstants.IN_OUT_ITEM_IN_USE_CODE,
                    ExceptionConstants.IN_OUT_ITEM_IN_USE_MSG);
        }
        ensureUnique(inOutItem);
        int result=0;
        try{
            result=inOutItemMapper.updateByPrimaryKeySelective(inOutItem);
            logService.insertLog("收支项目",
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append(inOutItem.getName()).toString(), request);
        }catch(DuplicateKeyException e) {
            throw duplicateName();
        }catch(BusinessRunTimeException e) {
            throw e;
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int deleteInOutItem(Long id, HttpServletRequest request)throws Exception {
        return batchDeleteInOutItemByIds(id.toString());
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteInOutItem(String ids, HttpServletRequest request)throws Exception {
        return batchDeleteInOutItemByIds(ids);
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteInOutItemByIds(String ids)throws Exception {
        checkEditPermission();
        lockInOutItemWrite();
        int result = 0;
        List<Long> idList = parseIds(ids);
        if (idList.isEmpty()) {
            return 0;
        }
        String [] idArray=idList.stream().map(String::valueOf).toArray(String[]::new);
        //校验财务子表	jsh_accountitem
        List<AccountItem> accountItemList=null;
        try{
            accountItemList=accountItemMapperEx.getAccountItemListByInOutItemIds(idArray);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        if(accountItemList!=null&&accountItemList.size()>0){
            logger.error("异常码[{}],异常提示[{}],参数,InOutItemIds[{}]",
                    ExceptionConstants.DELETE_FORCE_CONFIRM_CODE,ExceptionConstants.DELETE_FORCE_CONFIRM_MSG,ids);
            throw new BusinessRunTimeException(ExceptionConstants.DELETE_FORCE_CONFIRM_CODE,
                    ExceptionConstants.DELETE_FORCE_CONFIRM_MSG);
        }
        //校验通过执行删除操作
        StringBuffer sb = new StringBuffer();
        sb.append(BusinessConstants.LOG_OPERATION_TYPE_DELETE);
        List<InOutItem> list = getInOutItemListByIds(ids);
        for(InOutItem inOutItem: list){
            sb.append("[").append(inOutItem.getName()).append("]");
        }
        logService.insertLog("收支项目", sb.toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        User userInfo=userService.getCurrentUser();
        try{
            result=inOutItemMapperEx.batchDeleteInOutItemByIds(new Date(),userInfo==null?null:userInfo.getId(),idArray);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    public int checkIsNameAndTypeExist(Long id, String name, String type)throws Exception {
        id = id==null?0L:id;
        InOutItemExample example = new InOutItemExample();
        example.createCriteria().andIdNotEqualTo(id).andNameEqualTo(name).andTypeEqualTo(type).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<InOutItem> list = null;
        try{
            list=inOutItemMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }

        return list==null?0:list.size();
    }

    public int checkIsNameExist(Long id, String name)throws Exception {
        InOutItemExample example = new InOutItemExample();
        example.createCriteria().andIdNotEqualTo(id).andNameEqualTo(name).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<InOutItem> list = null;
        try{
            list=inOutItemMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }

        return list==null?0:list.size();
    }

    public List<InOutItem> findBySelect(String type)throws Exception {
        InOutItemExample example = new InOutItemExample();
        if ("in".equals(type)) {
            example.createCriteria().andTypeEqualTo("收入").andEnabledEqualTo(true)
                    .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        } else if ("out".equals(type)) {
            example.createCriteria().andTypeEqualTo("支出").andEnabledEqualTo(true)
                    .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        } else {
            throw invalidInOutItem("类型只能为in或out");
        }
        example.setOrderByClause("case when sort ~ '^[0-9]+$' then sort::bigint end asc nulls last, id desc");
        List<InOutItem> list = null;
        try{
            list=inOutItemMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchSetStatus(Boolean status, String ids)throws Exception {
        checkEditPermission();
        lockInOutItemWrite();
        if (status == null) {
            throw invalidInOutItem("状态不能为空");
        }
        List<Long> inOutItemIds = parseIds(ids);
        if (inOutItemIds.isEmpty()) {
            return 0;
        }
        logService.insertLog("收支项目",
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_ENABLED).toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        InOutItem inOutItem = new InOutItem();
        inOutItem.setEnabled(status);
        InOutItemExample example = new InOutItemExample();
        example.createCriteria().andIdIn(inOutItemIds)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        int result=0;
        try{
            result = inOutItemMapper.updateByExampleSelective(inOutItem, example);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    public void checkReadPermission() throws Exception {
        User currentUser = userService.getCurrentUser();
        Long userId = currentUser == null ? null : currentUser.getId();
        if (!userService.hasFunctionPermission(userId, IN_OUT_ITEM_URL)) {
            throw permissionDenied();
        }
    }

    public void checkEditPermission() throws Exception {
        User currentUser = userService.getCurrentUser();
        Long userId = currentUser == null ? null : currentUser.getId();
        if (!userService.hasButtonPermission(userId, IN_OUT_ITEM_URL, EDIT_BUTTON_CODE)) {
            throw permissionDenied();
        }
    }

    public void checkSelectPermission(String type) throws Exception {
        if (!"in".equals(type) && !"out".equals(type)) {
            throw invalidInOutItem("类型只能为in或out");
        }
        User currentUser = userService.getCurrentUser();
        Long userId = currentUser == null ? null : currentUser.getId();
        String financialUrl = "in".equals(type) ? INCOME_URL : EXPENSE_URL;
        if (!userService.hasFunctionPermission(userId, IN_OUT_ITEM_URL)
                && !userService.hasFunctionPermission(userId, financialUrl)) {
            throw permissionDenied();
        }
    }

    public void lockInOutItemWrite() throws Exception {
        User currentUser = userService.getCurrentUser();
        Long tenantId = currentUser == null || currentUser.getTenantId() == null ? 0L : currentUser.getTenantId();
        inOutItemMapperEx.lockInOutItemWrite(tenantId);
    }

    private InOutItem buildInOutItem(JSONObject obj, InOutItem existing) {
        InOutItem inOutItem = new InOutItem();
        if (existing != null) {
            inOutItem.setId(existing.getId());
        }
        inOutItem.setName(StringUtil.toNull(obj.getString("name")));
        inOutItem.setType(StringUtil.toNull(obj.getString("type")));
        inOutItem.setRemark(StringUtil.toNull(obj.getString("remark")));
        inOutItem.setSort(StringUtil.toNull(obj.getString("sort")));
        return inOutItem;
    }

    private void validateInOutItem(InOutItem inOutItem) {
        if (inOutItem.getName() == null || inOutItem.getName().length() > 50) {
            throw invalidInOutItem("名称不能为空且长度不能超过50个字符");
        }
        if (!BusinessConstants.TYPE_INCOME.equals(inOutItem.getType())
                && !BusinessConstants.TYPE_EXPENSE.equals(inOutItem.getType())) {
            throw invalidInOutItem("类型只能为收入或支出");
        }
        if (inOutItem.getRemark() != null && inOutItem.getRemark().length() > 100) {
            throw invalidInOutItem("备注长度不能超过100个字符");
        }
        if (inOutItem.getSort() != null && !inOutItem.getSort().matches("\\d{1,10}")) {
            throw invalidInOutItem("排序只能填写不超过10位的非负整数");
        }
    }

    private void ensureUnique(InOutItem inOutItem) throws Exception {
        if (checkIsNameAndTypeExist(inOutItem.getId(), inOutItem.getName(), inOutItem.getType()) > 0) {
            throw duplicateName();
        }
    }

    private boolean isInUse(Long id) {
        String[] ids = {String.valueOf(id)};
        List<AccountItem> items = accountItemMapperEx.getAccountItemListByInOutItemIds(ids);
        return items != null && !items.isEmpty();
    }

    private List<Long> parseIds(String ids) {
        if (StringUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        try {
            List<Long> parsed = new ArrayList<>(new LinkedHashSet<>(StringUtil.strToLongList(ids)));
            if (parsed.stream().anyMatch(id -> id == null || id <= 0)) {
                throw invalidInOutItem("项目编号不合法");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw invalidInOutItem("项目编号不合法");
        }
    }

    private BusinessRunTimeException duplicateName() {
        return new BusinessRunTimeException(ExceptionConstants.IN_OUT_ITEM_NAME_EXIST_FAILED_CODE,
                ExceptionConstants.IN_OUT_ITEM_NAME_EXIST_FAILED_MSG);
    }

    private BusinessRunTimeException permissionDenied() {
        return new BusinessRunTimeException(ExceptionConstants.IN_OUT_ITEM_PERMISSION_CODE,
                ExceptionConstants.IN_OUT_ITEM_PERMISSION_MSG);
    }

    private BusinessRunTimeException invalidInOutItem(String reason) {
        return new BusinessRunTimeException(ExceptionConstants.IN_OUT_ITEM_INVALID_CODE,
                String.format(ExceptionConstants.IN_OUT_ITEM_INVALID_MSG, reason));
    }
}
