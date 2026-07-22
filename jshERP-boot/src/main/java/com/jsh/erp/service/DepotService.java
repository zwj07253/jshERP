package com.jsh.erp.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.*;
import com.jsh.erp.datasource.mappers.*;
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
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class DepotService {
    private static final String DEPOT_URL = "/system/depot";
    private static final String MATERIAL_URL = "/material/material";
    private static final String USER_URL = "/system/user";
    private static final String EDIT_BUTTON_CODE = "1";
    private Logger logger = LoggerFactory.getLogger(DepotService.class);

    @Resource
    private DepotMapper depotMapper;
    @Resource
    private DepotMapperEx depotMapperEx;
    @Resource
    private UserService userService;
    @Resource
    private SystemConfigService systemConfigService;
    @Resource
    private UserBusinessService userBusinessService;
    @Resource
    private LogService logService;
    @Resource
    private DepotItemMapperEx depotItemMapperEx;
    @Resource
    private MaterialInitialStockMapperEx materialInitialStockMapperEx;
    @Resource
    private MaterialCurrentStockMapperEx materialCurrentStockMapperEx;
    @Resource
    private MaterialInitialStockMapper materialInitialStockMapper;
    @Resource
    private MaterialCurrentStockMapper materialCurrentStockMapper;
    @Resource
    private SerialNumberMapper serialNumberMapper;

    public Depot getDepot(long id)throws Exception {
        Depot result=null;
        try{
            DepotExample example = new DepotExample();
            example.createCriteria().andIdEqualTo(id)
                    .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
            List<Depot> list = depotMapper.selectByExample(example);
            if (list != null && !list.isEmpty()) {
                result = list.get(0);
            }
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    public List<Depot> getDepotListByIds(String ids)throws Exception {
        List<Long> idList = StringUtil.strToLongList(ids);
        DepotExample example = new DepotExample();
        example.createCriteria().andIdIn(idList)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        return depotMapper.selectByExample(example);
    }

    public List<Depot> getDepot()throws Exception {
        DepotExample example = new DepotExample();
        example.createCriteria().andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<Depot> list=null;
        try{
            list=depotMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<Depot> getAllList()throws Exception {
        DepotExample example = new DepotExample();
        example.createCriteria().andEnabledEqualTo(true).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        example.setOrderByClause("sort asc, id desc");
        List<Depot> list=null;
        try{
            list=depotMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<DepotEx> select(String name, Integer type, String remark)throws Exception {
        List<DepotEx> list=null;
        try{
            PageUtils.startPage();
            list=depotMapperEx.selectByConditionDepot(name, type, remark);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int insertDepot(JSONObject obj, HttpServletRequest request)throws Exception {
        checkEditPermission();
        lockDepotWrite();
        Depot depot = buildDepot(obj, null);
        validateDepot(depot);
        ensureDepotNameUnique(depot.getId(), depot.getName());
        int result=0;
        try{
            depot.setType(0);
            List<Depot> depotList = getDepot();
            if(depotList.size() == 0) {
                depot.setIsDefault(true);
            } else {
                depot.setIsDefault(false);
            }
            depot.setEnabled(true);
            result=depotMapper.insertSelective(depot);
            //新增仓库时给当前用户自动授权
            Long userId = userService.getUserId(request);
            Long depotId = depot.getId();
            if (depotId == null) {
                throw invalidDepot("新增仓库未生成有效ID");
            }
            JSONArray userIds = new JSONArray();
            userIds.add(userId);
            userBusinessService.updateOneValueByKeyIdAndType("UserDepot", userIds, depotId.toString());
            logService.insertLog("仓库",
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_ADD).append(depot.getName()).toString(), request);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int updateDepot(JSONObject obj, HttpServletRequest request) throws Exception{
        checkEditPermission();
        lockDepotWrite();
        Long id = obj.getLong("id");
        Depot existing = id == null ? null : getDepot(id);
        if (existing == null) {
            throw invalidDepot("仓库不存在或已删除");
        }
        Depot depot = buildDepot(obj, existing);
        validateDepot(depot);
        ensureDepotNameUnique(depot.getId(), depot.getName());
        int result=0;
        try{
            result= depotMapper.updateByPrimaryKeySelective(depot);
            logService.insertLog("仓库",
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append(depot.getName()).toString(), request);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int deleteDepot(Long id, HttpServletRequest request)throws Exception {
        checkEditPermission();
        return batchDeleteDepotByIds(id.toString());
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteDepot(String ids, HttpServletRequest request) throws Exception{
        checkEditPermission();
        return batchDeleteDepotByIds(ids);
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    private int batchDeleteDepotByIds(String ids)throws Exception {
        lockDepotWrite();
        int result=0;
        List<Long> depotIds = StringUtil.strToLongList(ids);
        if (depotIds.isEmpty()) {
            throw invalidDepot("请选择要删除的仓库");
        }
        String [] idArray = depotIds.stream().map(String::valueOf).toArray(String[]::new);
        List<Depot> activeDepots = getDepotListByIds(ids);
        if (activeDepots.size() != depotIds.size()) {
            throw invalidDepot("仓库不存在或已删除");
        }
        for (Depot depot : activeDepots) {
            if (Boolean.TRUE.equals(depot.getIsDefault())) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_DEFAULT_OPERATION_CODE,
                        ExceptionConstants.DEPOT_DEFAULT_OPERATION_MSG);
            }
        }
        //校验单据子表	jsh_depot_item
        List<DepotItem> depotItemList = depotItemMapperEx.getDepotItemListListByDepotIds(idArray);
        if(depotItemList != null && !depotItemList.isEmpty()){
            throwDepotInUse();
        }
        MaterialInitialStockExample initialExample = new MaterialInitialStockExample();
        initialExample.createCriteria().andDepotIdIn(depotIds).andNumberNotEqualTo(BigDecimal.ZERO)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        if (!materialInitialStockMapper.selectByExample(initialExample).isEmpty()) {
            throwDepotInUse();
        }
        MaterialCurrentStockExample currentExample = new MaterialCurrentStockExample();
        currentExample.createCriteria().andDepotIdIn(depotIds).andCurrentNumberNotEqualTo(BigDecimal.ZERO)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        if (!materialCurrentStockMapper.selectByExample(currentExample).isEmpty()) {
            throwDepotInUse();
        }
        SerialNumberExample serialExample = new SerialNumberExample();
        serialExample.createCriteria().andDepotIdIn(depotIds)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        if (serialNumberMapper.selectByExample(serialExample).stream()
                .anyMatch(serialNumber -> !"1".equals(serialNumber.getIsSell()))) {
            throwDepotInUse();
        }
        try{
            //记录日志
            StringBuffer sb = new StringBuffer();
            sb.append(BusinessConstants.LOG_OPERATION_TYPE_DELETE);
            List<Depot> list = getDepotListByIds(ids);
            for(Depot depot: list){
                sb.append("[").append(depot.getName()).append("]");
            }
            User userInfo=userService.getCurrentUser();
            //校验通过执行删除操作
            //删除仓库关联的商品的初始库存
            materialInitialStockMapperEx.batchDeleteByDepots(idArray);
            //删除仓库关联的商品的当前库存
            materialCurrentStockMapperEx.batchDeleteByDepots(idArray);
            for (Long depotId : depotIds) {
                userBusinessService.removeOneValueByType("UserDepot", depotId.toString());
            }
            //删除仓库
            result = depotMapperEx.batchDeleteDepotByIds(new Date(),userInfo==null?null:userInfo.getId(),idArray);
            //记录日志
            logService.insertLog("仓库", sb.toString(),
                    ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        } catch (Exception e) {
            JshException.writeFail(logger, e);
        }
        return result;
    }

    public int checkIsNameExist(Long id, String name)throws Exception {
        name = StringUtil.toNull(name);
        if (name == null) {
            return 0;
        }
        DepotExample example = new DepotExample();
        example.createCriteria().andIdNotEqualTo(id).andNameEqualTo(name).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<Depot> list=null;
        try{
            list= depotMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list==null?0:list.size();
    }

    public List<Depot> findUserDepot()throws Exception{
        DepotExample example = new DepotExample();
        example.createCriteria().andTypeEqualTo(0).andEnabledEqualTo(true)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        example.setOrderByClause("sort asc, id desc");
        List<Depot> list=null;
        try{
            list= depotMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int updateIsDefault(Long depotId) throws Exception{
        checkEditPermission();
        lockDepotWrite();
        Depot target = depotId == null ? null : getDepot(depotId);
        if (target == null || !Boolean.TRUE.equals(target.getEnabled())) {
            throw invalidDepot("只能将已启用的仓库设为默认仓库");
        }
        int result=0;
        try{
            //全部取消默认
            Depot allDepot = new Depot();
            allDepot.setIsDefault(false);
            DepotExample allExample = new DepotExample();
            allExample.createCriteria().andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
            depotMapper.updateByExampleSelective(allDepot, allExample);
            //给指定仓库设为默认
            Depot depot = new Depot();
            depot.setIsDefault(true);
            DepotExample example = new DepotExample();
            example.createCriteria().andIdEqualTo(depotId).andEnabledEqualTo(true)
                    .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
            result = depotMapper.updateByExampleSelective(depot, example);
            logService.insertLog("仓库",BusinessConstants.LOG_OPERATION_TYPE_EDIT+depotId,
                    ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    /**
     * 根据单个仓库查询
     * @param depotId
     * @return
     * @throws Exception
     */
    public List<Long> parseDepotList(Long depotId) throws Exception {
        Set<Long> allowedDepotIds = getAllowedDepotIds();
        List<Long> depotList = new ArrayList<>();
        if(depotId !=null) {
            if (!allowedDepotIds.contains(depotId)) {
                throwDepotDataPermissionException();
            }
            depotList.add(depotId);
        } else {
            depotList.addAll(allowedDepotIds);
        }
        return depotList.isEmpty() ? Collections.singletonList(-1L) : depotList;
    }

    /**
     * 根据多个仓库查询
     * @param depotIdArr
     * @return
     * @throws Exception
     */
    public List<Long> parseDepotListByArr(String[] depotIdArr) throws Exception {
        Set<Long> allowedDepotIds = getAllowedDepotIds();
        List<Long> depotList = new ArrayList<>();
        if(depotIdArr !=null) {
            for (int i = 0; i < depotIdArr.length; i++) {
                Long depotId = Long.parseLong(depotIdArr[i]);
                if (!allowedDepotIds.contains(depotId)) {
                    throwDepotDataPermissionException();
                }
                depotList.add(depotId);
            }
        } else {
            depotList.addAll(allowedDepotIds);
        }
        // Empty collections are omitted by mapper XML and would otherwise
        // become unrestricted queries. Keep the permission boundary closed.
        return depotList.isEmpty() ? Collections.singletonList(-1L) : depotList;
    }

    private Set<Long> getAllowedDepotIds() throws Exception {
        Set<Long> depotIds = new HashSet<>();
        User currentUser = userService.getCurrentUser();
        if (currentUser != null && "admin".equals(currentUser.getLoginName())) {
            for (Depot depot : findUserDepot()) {
                depotIds.add(depot.getId());
            }
            return depotIds;
        }
        JSONArray depotArr = findDepotByCurrentUser();
        for (Object obj : depotArr) {
            depotIds.add(JSONObject.parseObject(obj.toString()).getLong("id"));
        }
        return depotIds;
    }

    private void throwDepotDataPermissionException() {
        throw new BusinessRunTimeException(ExceptionConstants.DEPOT_DATA_PERMISSION_CODE,
                ExceptionConstants.DEPOT_DATA_PERMISSION_MSG);
    }

    public JSONArray findDepotByCurrentUser() throws Exception {
        JSONArray arr = new JSONArray();
        String type = "UserDepot";
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return arr;
        }
        Long userId = currentUser.getId();
        List<Depot> dataList = findUserDepot();
        //开始拼接json数据
        if (null != dataList) {
            boolean depotFlag = systemConfigService.getDepotFlag();
            if(depotFlag && !"admin".equals(currentUser.getLoginName())) {
                List<UserBusiness> list = userBusinessService.getBasicData(userId.toString(), type);
                if(list!=null && list.size()>0) {
                    String depotStr = list.get(0).getValue();
                    if(StringUtil.isNotEmpty(depotStr)){
                        depotStr = depotStr.replaceAll("\\[", "").replaceAll("]", ",");
                        String[] depotArr = depotStr.split(",");
                        for (Depot depot : dataList) {
                            for(String depotId: depotArr) {
                                if(StringUtil.isNotEmpty(depotId)
                                        && depot.getId().toString().equals(depotId.trim())){
                                    JSONObject item = new JSONObject();
                                    item.put("id", depot.getId());
                                    item.put("depotName", depot.getName());
                                    item.put("isDefault", depot.getIsDefault());
                                    arr.add(item);
                                }
                            }
                        }
                    }
                }
            } else {
                for (Depot depot : dataList) {
                    JSONObject item = new JSONObject();
                    item.put("id", depot.getId());
                    item.put("depotName", depot.getName());
                    item.put("isDefault", depot.getIsDefault());
                    arr.add(item);
                }
            }
        }
        return arr;
    }

    public List<Depot> getAllListByCurrentUser() throws Exception {
        Set<Long> allowedDepotIds = getAllowedDepotIds();
        if (allowedDepotIds.isEmpty()) {
            return Collections.emptyList();
        }
        DepotExample example = new DepotExample();
        example.createCriteria().andIdIn(new ArrayList<>(allowedDepotIds)).andEnabledEqualTo(true)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        example.setOrderByClause("sort asc, id desc");
        return depotMapper.selectByExample(example);
    }

    /**
     * 当前用户有权限使用的仓库列表的id，用逗号隔开
     * @return
     * @throws Exception
     */
    public String findDepotStrByCurrentUser() throws Exception {
        JSONArray arr =  findDepotByCurrentUser();
        StringBuffer sb = new StringBuffer();
        for(Object object: arr) {
            JSONObject obj = (JSONObject)object;
            sb.append(obj.getLong("id")).append(",");
        }
        String depotStr = sb.toString();
        if(StringUtil.isNotEmpty(depotStr)){
            depotStr = depotStr.substring(0, depotStr.length()-1);
        }
        return depotStr;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchSetStatus(Boolean status, String ids)throws Exception {
        checkEditPermission();
        lockDepotWrite();
        if (status == null) {
            throw invalidDepot("启用状态不能为空");
        }
        List<Long> depotIds = StringUtil.strToLongList(ids);
        if (depotIds.isEmpty()) {
            throw invalidDepot("请选择仓库");
        }
        if (Boolean.FALSE.equals(status)) {
            DepotExample defaultExample = new DepotExample();
            defaultExample.createCriteria().andIdIn(depotIds).andIsDefaultEqualTo(true)
                    .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
            if (!depotMapper.selectByExample(defaultExample).isEmpty()) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_DEFAULT_OPERATION_CODE,
                        ExceptionConstants.DEPOT_DEFAULT_OPERATION_MSG);
            }
        }
        logService.insertLog("仓库",
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_ENABLED).toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        Depot depot = new Depot();
        depot.setEnabled(status);
        DepotExample example = new DepotExample();
        example.createCriteria().andIdIn(depotIds)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        int result=0;
        try{
            result = depotMapper.updateByExampleSelective(depot, example);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    public void checkReadPermission() throws Exception {
        User currentUser = userService.getCurrentUser();
        Long userId = currentUser == null ? null : currentUser.getId();
        if (!userService.hasFunctionPermission(userId, DEPOT_URL)) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_PERMISSION_CODE,
                    ExceptionConstants.DEPOT_PERMISSION_MSG);
        }
    }

    public void checkEditPermission() throws Exception {
        User currentUser = userService.getCurrentUser();
        Long userId = currentUser == null ? null : currentUser.getId();
        if (!userService.hasButtonPermission(userId, DEPOT_URL, EDIT_BUTTON_CODE)) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_PERMISSION_CODE,
                    ExceptionConstants.DEPOT_PERMISSION_MSG);
        }
    }

    public void checkMaterialReadPermission() throws Exception {
        User currentUser = userService.getCurrentUser();
        Long userId = currentUser == null ? null : currentUser.getId();
        if (!userService.hasFunctionPermission(userId, MATERIAL_URL)) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_PERMISSION_CODE,
                    ExceptionConstants.DEPOT_PERMISSION_MSG);
        }
    }

    public void checkUserEditPermission() throws Exception {
        User currentUser = userService.getCurrentUser();
        Long userId = currentUser == null ? null : currentUser.getId();
        if (!userService.hasButtonPermission(userId, USER_URL, EDIT_BUTTON_CODE)) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_PERMISSION_CODE,
                    ExceptionConstants.DEPOT_PERMISSION_MSG);
        }
    }

    private Depot buildDepot(JSONObject obj, Depot existing) {
        Depot depot = new Depot();
        if (existing != null) {
            depot.setId(existing.getId());
        }
        depot.setName(StringUtil.toNull(obj.getString("name")));
        depot.setAddress(StringUtil.toNull(obj.getString("address")));
        depot.setWarehousing(obj.getBigDecimal("warehousing"));
        depot.setTruckage(obj.getBigDecimal("truckage"));
        depot.setSort(StringUtil.toNull(obj.getString("sort")));
        depot.setRemark(StringUtil.toNull(obj.getString("remark")));
        depot.setPrincipal(obj.getLong("principal"));
        return depot;
    }

    private void validateDepot(Depot depot) throws Exception {
        validateText(depot.getName(), 20, "仓库名称", true);
        validateText(depot.getAddress(), 50, "仓库地址", false);
        validateText(depot.getSort(), 10, "排序", false);
        validateText(depot.getRemark(), 100, "备注", false);
        if (depot.getSort() != null && !depot.getSort().matches("\\d+")) {
            throw invalidDepot("排序必须为非负整数");
        }
        validateAmount(depot.getWarehousing(), "仓储费");
        validateAmount(depot.getTruckage(), "搬运费");
        if (depot.getPrincipal() != null) {
            User principal = userService.getUser(depot.getPrincipal());
            if (principal == null || BusinessConstants.DELETE_FLAG_DELETED.equals(principal.getDeleteFlag())
                    || (principal.getStatus() != null && principal.getStatus() != 0)) {
                throw invalidDepot("负责人不存在或已停用");
            }
        }
    }

    private void validateText(String value, int maxLength, String label, boolean required) {
        if (required && StringUtil.isEmpty(value)) {
            throw invalidDepot(label + "不能为空");
        }
        if (value != null && value.length() > maxLength) {
            throw invalidDepot(label + "长度不能超过" + maxLength + "个字符");
        }
    }

    private void validateAmount(BigDecimal value, String label) {
        if (value == null) {
            return;
        }
        BigDecimal normalized = value.stripTrailingZeros();
        int scale = Math.max(0, normalized.scale());
        int integerDigits = Math.max(0, normalized.precision() - normalized.scale());
        if (value.compareTo(BigDecimal.ZERO) < 0 || scale > 6 || integerDigits > 18) {
            throw invalidDepot(label + "必须为非负数，且最多保留6位小数");
        }
    }

    private void ensureDepotNameUnique(Long id, String name) throws Exception {
        if (checkIsNameExist(id == null ? 0L : id, name) > 0) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_ALREADY_EXISTS_CODE,
                    ExceptionConstants.DEPOT_ALREADY_EXISTS_MSG);
        }
    }

    private void throwDepotInUse() {
        throw new BusinessRunTimeException(ExceptionConstants.DEPOT_IN_USE_CODE,
                ExceptionConstants.DEPOT_IN_USE_MSG);
    }

    private BusinessRunTimeException invalidDepot(String reason) {
        return new BusinessRunTimeException(ExceptionConstants.DEPOT_INVALID_CODE,
                String.format(ExceptionConstants.DEPOT_INVALID_MSG, reason));
    }

    private void lockDepotWrite() throws Exception {
        User currentUser = userService.getCurrentUser();
        Long tenantId = currentUser == null || currentUser.getTenantId() == null ? 0L : currentUser.getTenantId();
        depotMapperEx.lockDepotWrite(tenantId);
    }
}
