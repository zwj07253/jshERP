package com.jsh.erp.service;

import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.*;
import com.jsh.erp.datasource.mappers.*;
import com.jsh.erp.datasource.vo.DepotHeadVo4StatementAccount;
import com.jsh.erp.datasource.vo.SupplierSimple;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.exception.JshException;
import com.jsh.erp.utils.*;
import jxl.Sheet;
import jxl.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.math.BigDecimal;
import java.util.*;


@Service
public class SupplierService {
    private static final Set<String> SUPPORTED_TYPES = new HashSet<>(Arrays.asList("供应商", "客户", "会员"));
    private static final String EDIT_BUTTON_CODE = "1";

    private Logger logger = LoggerFactory.getLogger(SupplierService.class);

    @Resource
    private SupplierMapper supplierMapper;

    @Resource
    private SupplierMapperEx supplierMapperEx;
    @Resource
    private LogService logService;
    @Resource
    private UserService userService;
    @Resource
    private AccountHeadMapperEx accountHeadMapperEx;
    @Resource
    private DepotHeadMapperEx depotHeadMapperEx;
    @Resource
    private AccountItemMapperEx accountItemMapperEx;
    @Resource
    private DepotHeadService depotHeadService;
    @Resource
    private UserBusinessService userBusinessService;
    @Resource
    private UserBusinessMapper userBusinessMapper;

    @Value(value="${file.exportTmp}")
    private String fileExportTmp;

    public Supplier getSupplier(long id)throws Exception {
        Supplier result=null;
        try{
            result = supplierMapperEx.getInfoById(id);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    public Supplier lockSupplier(Long id) {
        return supplierMapperEx.lockById(id);
    }

    public BigDecimal calculateAdvanceIn(Long supplierId) {
        BigDecimal financialAllPrice = accountHeadMapperEx.getFinancialAllPriceByOrganId(supplierId);
        BigDecimal billAllPrice = depotHeadMapperEx.getBillAllPriceByOrganId(supplierId);
        return financialAllPrice.subtract(billAllPrice);
    }

    public List<Supplier> getSupplierListByIds(String ids)throws Exception {
        List<Long> idList = StringUtil.strToLongList(ids);
        List<Supplier> list = new ArrayList<>();
        try{
            for (Long id : idList) {
                Supplier supplier = supplierMapperEx.getInfoById(id);
                if (supplier != null) list.add(supplier);
            }
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<Supplier> getSupplier()throws Exception {
        SupplierExample example = new SupplierExample();
        example.createCriteria().andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<Supplier> list=null;
        try{
            list=supplierMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<Supplier> select(String supplier, String type, String contacts, String phonenum, String telephone) throws Exception{
        List<Supplier> list = new ArrayList<>();
        try{
            String [] creatorArray = depotHeadService.getCreatorArray();
            PageUtils.startPage();
            list = supplierMapperEx.selectByConditionSupplier(supplier, type, contacts, phonenum, telephone, creatorArray);
            for(Supplier s : list) {
                Integer supplierId = s.getId().intValue();
                String beginTime = Tools.getYearBegin();
                String endTime = Tools.getCenternTime(new Date());
                BigDecimal sum = BigDecimal.ZERO;
                String supplierType = StringUtil.isEmpty(type) ? s.getType() : type;
                String inOutType = "";
                String subType = "";
                String typeBack = "";
                String subTypeBack = "";
                String billType = "";
                if (("供应商").equals(supplierType)) {
                    inOutType = "入库";
                    subType = "采购";
                    typeBack = "出库";
                    subTypeBack = "采购退货";
                    billType = "付款";
                } else if (("客户").equals(supplierType)) {
                    inOutType = "出库";
                    subType = "销售";
                    typeBack = "入库";
                    subTypeBack = "销售退货";
                    billType = "收款";
                }
                List<DepotHeadVo4StatementAccount> saList = depotHeadService.getStatementAccount(beginTime, endTime, supplierId, null,
                        1, supplierType, inOutType, subType, typeBack, subTypeBack, billType, null, null, null, null);
                if(saList != null && saList.size()>0) {
                    DepotHeadVo4StatementAccount item = saList.get(0);
                    //期初 = 起始期初金额+上期欠款金额-上期退货的欠款金额-上期收付款
                    BigDecimal preNeed = nvl(item.getBeginNeed()).add(nvl(item.getPreDebtMoney())).subtract(nvl(item.getPreReturnDebtMoney())).subtract(nvl(item.getPreBackMoney()));
                    item.setPreNeed(preNeed);
                    //实际欠款 = 本期欠款-本期退货的欠款金额
                    BigDecimal realDebtMoney = nvl(item.getDebtMoney()).subtract(nvl(item.getReturnDebtMoney()));
                    item.setDebtMoney(realDebtMoney);
                    //期末 = 期初+实际欠款-本期收款
                    BigDecimal allNeedGet = preNeed.add(realDebtMoney).subtract(nvl(item.getBackMoney()));
                    sum = sum.add(allNeedGet);
                }
                if(("客户").equals(s.getType())) {
                    s.setAllNeedGet(sum);
                } else if(("供应商").equals(s.getType())) {
                    s.setAllNeedPay(sum);
                }
            }
        } catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int insertSupplier(JSONObject obj, HttpServletRequest request)throws Exception {
        Supplier supplier = buildSupplier(obj, null);
        checkEditPermission(supplier.getType());
        validateSupplier(supplier, null);
        int result=0;
        try{
            supplier.setId(null);
            supplier.setTenantId(null);
            supplier.setDeleteFlag(null);
            supplier.setEnabled(true);
            User userInfo=userService.getCurrentUser();
            supplier.setCreator(userInfo==null?null:userInfo.getId());
            result=supplierMapper.insertSelective(supplier);
            //新增客户时给当前用户和租户自动授权
            setUserCustomerPermission(request, supplier);
            logService.insertLog("商家",
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_ADD).append(supplier.getSupplier()).toString(),request);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int updateSupplier(JSONObject obj, HttpServletRequest request)throws Exception {
        Long id = obj.getLong("id");
        Supplier existing = id == null ? null : getSupplier(id);
        if (existing == null) {
            throw new BusinessRunTimeException(ExceptionConstants.SUPPLIER_NOT_EXISTS_CODE,
                    ExceptionConstants.SUPPLIER_NOT_EXISTS_MSG);
        }
        checkEditPermission(existing.getType());
        Supplier supplier = buildSupplier(obj, existing);
        validateSupplier(supplier, existing);
        boolean protectedFieldChanged = !Objects.equals(existing.getSupplier(), supplier.getSupplier())
                || !Objects.equals(existing.getBeginNeedPay(), supplier.getBeginNeedPay())
                || !Objects.equals(existing.getBeginNeedGet(), supplier.getBeginNeedGet());
        if (protectedFieldChanged && isInUse(id)) {
            throw new BusinessRunTimeException(ExceptionConstants.SUPPLIER_IN_USE_CODE,
                    ExceptionConstants.SUPPLIER_IN_USE_MSG);
        }
        int result=0;
        try{
            supplier.setTenantId(null);
            supplier.setDeleteFlag(null);
            supplier.setCreator(null);
            supplier.setEnabled(null);
            supplier.setAdvanceIn(null);
            supplier.setAllNeedGet(null);
            supplier.setAllNeedPay(null);
            result=supplierMapper.updateByPrimaryKeySelective(supplier);
            logService.insertLog("商家",
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append(supplier.getSupplier()).toString(), request);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    private Supplier buildSupplier(JSONObject obj, Supplier existing) {
        Supplier supplier = existing == null ? new Supplier() : new Supplier();
        if (existing != null) {
            supplier.setId(existing.getId());
            supplier.setType(existing.getType());
            supplier.setSupplier(existing.getSupplier());
            supplier.setContacts(existing.getContacts());
            supplier.setTelephone(existing.getTelephone());
            supplier.setPhoneNum(existing.getPhoneNum());
            supplier.setEmail(existing.getEmail());
            supplier.setFax(existing.getFax());
            supplier.setBeginNeedPay(existing.getBeginNeedPay());
            supplier.setBeginNeedGet(existing.getBeginNeedGet());
            supplier.setTaxNum(existing.getTaxNum());
            supplier.setTaxRate(existing.getTaxRate());
            supplier.setBankName(existing.getBankName());
            supplier.setAccountNumber(existing.getAccountNumber());
            supplier.setAddress(existing.getAddress());
            supplier.setDescription(existing.getDescription());
            supplier.setSort(existing.getSort());
        }
        if (obj.containsKey("supplier")) supplier.setSupplier(obj.getString("supplier"));
        if (obj.containsKey("contacts")) supplier.setContacts(obj.getString("contacts"));
        if (obj.containsKey("telephone")) supplier.setTelephone(obj.getString("telephone"));
        if (obj.containsKey("phoneNum")) supplier.setPhoneNum(obj.getString("phoneNum"));
        if (obj.containsKey("email")) supplier.setEmail(obj.getString("email"));
        if (obj.containsKey("fax")) supplier.setFax(obj.getString("fax"));
        if (obj.containsKey("beginNeedPay")) supplier.setBeginNeedPay(obj.getBigDecimal("beginNeedPay"));
        if (obj.containsKey("beginNeedGet")) supplier.setBeginNeedGet(obj.getBigDecimal("beginNeedGet"));
        if (obj.containsKey("taxNum")) supplier.setTaxNum(obj.getString("taxNum"));
        if (obj.containsKey("taxRate")) supplier.setTaxRate(obj.getBigDecimal("taxRate"));
        if (obj.containsKey("bankName")) supplier.setBankName(obj.getString("bankName"));
        if (obj.containsKey("accountNumber")) supplier.setAccountNumber(obj.getString("accountNumber"));
        if (obj.containsKey("address")) supplier.setAddress(obj.getString("address"));
        if (obj.containsKey("description")) supplier.setDescription(obj.getString("description"));
        if (obj.containsKey("sort")) supplier.setSort(obj.getString("sort"));
        if (existing == null) supplier.setType(obj.getString("type"));
        return supplier;
    }

    private void validateSupplier(Supplier supplier, Supplier existing) throws Exception {
        if (supplier == null || StringUtil.isEmpty(supplier.getSupplier())
                || supplier.getSupplier().trim().length() < 2 || supplier.getSupplier().trim().length() > 60) {
            throw new BusinessRunTimeException(ExceptionConstants.SUPPLIER_INVALID_CODE,
                    String.format(ExceptionConstants.SUPPLIER_INVALID_MSG, "名称长度必须为2至60个字符"));
        }
        if (!SUPPORTED_TYPES.contains(supplier.getType())) {
            throw new BusinessRunTimeException(ExceptionConstants.SUPPLIER_INVALID_CODE,
                    String.format(ExceptionConstants.SUPPLIER_INVALID_MSG, "类型不支持"));
        }
        if (supplier.getBeginNeedPay() != null && supplier.getBeginNeedPay().signum() < 0
                || supplier.getBeginNeedGet() != null && supplier.getBeginNeedGet().signum() < 0) {
            throw new BusinessRunTimeException(ExceptionConstants.SUPPLIER_INVALID_CODE,
                    String.format(ExceptionConstants.SUPPLIER_INVALID_MSG, "期初金额不能为负数"));
        }
        if (supplier.getTaxRate() != null && (supplier.getTaxRate().signum() < 0
                || supplier.getTaxRate().compareTo(new BigDecimal("100")) > 0)) {
            throw new BusinessRunTimeException(ExceptionConstants.SUPPLIER_INVALID_CODE,
                    String.format(ExceptionConstants.SUPPLIER_INVALID_MSG, "税率必须在0至100之间"));
        }
        if (StringUtil.isNotEmpty(supplier.getSort())) {
            try { Integer.parseInt(supplier.getSort()); }
            catch (NumberFormatException e) {
                throw new BusinessRunTimeException(ExceptionConstants.SUPPLIER_INVALID_CODE,
                        String.format(ExceptionConstants.SUPPLIER_INVALID_MSG, "排序必须是整数"));
            }
        }
        SupplierExample example = new SupplierExample();
        example.createCriteria().andSupplierEqualTo(supplier.getSupplier().trim())
                .andTypeEqualTo(supplier.getType())
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<Supplier> duplicates = supplierMapper.selectByExample(example);
        for (Supplier duplicate : duplicates) {
            if (existing == null || !Objects.equals(existing.getId(), duplicate.getId())) {
                throw new BusinessRunTimeException(ExceptionConstants.SUPPLIER_INVALID_CODE,
                        String.format(ExceptionConstants.SUPPLIER_INVALID_MSG, "名称和类型已存在"));
            }
        }
    }

    private void checkEditPermission(String type) throws Exception {
        String url;
        if ("供应商".equals(type)) url = "/system/vendor";
        else if ("客户".equals(type)) url = "/system/customer";
        else if ("会员".equals(type)) url = "/system/member";
        else throw new BusinessRunTimeException(ExceptionConstants.SUPPLIER_INVALID_CODE,
                String.format(ExceptionConstants.SUPPLIER_INVALID_MSG, "类型不支持"));
        User user = userService.getCurrentUser();
        Long userId = user == null ? null : user.getId();
        if (!userService.hasButtonPermission(userId, url, EDIT_BUTTON_CODE)) {
            throw new BusinessRunTimeException(ExceptionConstants.SUPPLIER_PERMISSION_CODE,
                    ExceptionConstants.SUPPLIER_PERMISSION_MSG);
        }
    }

    private boolean isInUse(Long id) {
        String[] ids = new String[]{String.valueOf(id)};
        List<AccountHead> accountHeads = accountHeadMapperEx.getAccountHeadListByOrganIds(ids);
        List<DepotHead> depotHeads = depotHeadMapperEx.getDepotHeadListByOrganIds(ids);
        return accountHeads != null && !accountHeads.isEmpty()
                || depotHeads != null && !depotHeads.isEmpty();
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int deleteSupplier(Long id, HttpServletRequest request)throws Exception {
        return batchDeleteSupplierByIds(id.toString());
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteSupplier(String ids, HttpServletRequest request) throws Exception{
        return batchDeleteSupplierByIds(ids);
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteSupplierByIds(String ids)throws Exception {
        int result=0;
        List<Long> idList = new ArrayList<>(new LinkedHashSet<>(StringUtil.strToLongList(ids)));
        if (idList.isEmpty()) return 0;
        String[] idArray = idList.stream().map(String::valueOf).toArray(String[]::new);
        for (Long id : idList) {
            Supplier supplier = getSupplier(id);
            if (supplier == null) {
                throw new BusinessRunTimeException(ExceptionConstants.SUPPLIER_NOT_EXISTS_CODE,
                        ExceptionConstants.SUPPLIER_NOT_EXISTS_MSG);
            }
            checkEditPermission(supplier.getType());
            if (isInUse(id)) {
                throw new BusinessRunTimeException(ExceptionConstants.SUPPLIER_IN_USE_CODE,
                        ExceptionConstants.SUPPLIER_IN_USE_MSG);
            }
        }
        //校验财务主表	jsh_accounthead
        List<AccountHead> accountHeadList=null;
        try{
            accountHeadList = accountHeadMapperEx.getAccountHeadListByOrganIds(idArray);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        if(accountHeadList!=null&&accountHeadList.size()>0){
            logger.error("异常码[{}],异常提示[{}],参数,OrganIds[{}]",
                    ExceptionConstants.DELETE_FORCE_CONFIRM_CODE,ExceptionConstants.DELETE_FORCE_CONFIRM_MSG,ids);
            throw new BusinessRunTimeException(ExceptionConstants.DELETE_FORCE_CONFIRM_CODE,
                    ExceptionConstants.DELETE_FORCE_CONFIRM_MSG);
        }
        //校验单据主表	jsh_depot_head
        List<DepotHead> depotHeadList=null;
        try{
            depotHeadList = depotHeadMapperEx.getDepotHeadListByOrganIds(idArray);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        if(depotHeadList!=null&&depotHeadList.size()>0){
            logger.error("异常码[{}],异常提示[{}],参数,OrganIds[{}]",
                    ExceptionConstants.DELETE_FORCE_CONFIRM_CODE,ExceptionConstants.DELETE_FORCE_CONFIRM_MSG,ids);
            throw new BusinessRunTimeException(ExceptionConstants.DELETE_FORCE_CONFIRM_CODE,
                    ExceptionConstants.DELETE_FORCE_CONFIRM_MSG);
        }
        //记录日志
        StringBuffer sb = new StringBuffer();
        sb.append(BusinessConstants.LOG_OPERATION_TYPE_DELETE);
        List<Supplier> list = getSupplierListByIds(ids);
        for(Supplier supplier: list){
            sb.append("[").append(supplier.getSupplier()).append("]");
        }
        logService.insertLog("商家", sb.toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        User userInfo=userService.getCurrentUser();
        //校验通过执行删除操作
        try{
            result = supplierMapperEx.batchDeleteSupplierByIds(new Date(),userInfo==null?null:userInfo.getId(),idList);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    public int checkIsNameExist(Long id, String name)throws Exception {
        SupplierExample example = new SupplierExample();
        example.createCriteria().andIdNotEqualTo(id).andSupplierEqualTo(name).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<Supplier> list=null;
        try{
            list= supplierMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list==null?0:list.size();
    }

    public int checkIsNameAndTypeExist(Long id, String name, String type)throws Exception {
        name = name == null? "": name;
        SupplierExample example = new SupplierExample();
        example.createCriteria().andIdNotEqualTo(id).andSupplierEqualTo(name).andTypeEqualTo(type)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<Supplier> list=null;
        try{
            list= supplierMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list==null?0:list.size();
    }

    /**
     * 更新会员的预付款
     * @param supplierId
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void updateAdvanceIn(Long supplierId) throws Exception {
        if(lockSupplier(supplierId) == null) {
            throw new BusinessRunTimeException(ExceptionConstants.SUPPLIER_EDIT_FAILED_CODE,
                    ExceptionConstants.SUPPLIER_EDIT_FAILED_MSG);
        }
        Supplier supplier = new Supplier();
        supplier.setId(supplierId);
        supplier.setAdvanceIn(calculateAdvanceIn(supplierId));
        if(supplierMapper.updateByPrimaryKeySelective(supplier) != 1) {
            throw new BusinessRunTimeException(ExceptionConstants.SUPPLIER_EDIT_FAILED_CODE,
                    ExceptionConstants.SUPPLIER_EDIT_FAILED_MSG);
        }
    }

    public List<Supplier> findBySelectCus(String key, Long organId, Integer limit)throws Exception {
        List<Supplier> list=null;
        try{
            list = supplierMapperEx.findByTypeAndKey("客户", key, limit);
            if(organId!=null) {
                list = addOrganToList(list, organId, "客户");
            }
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<Supplier> findBySelectSup(String key, Long organId, Integer limit)throws Exception {
        List<Supplier> list=null;
        try{
            list = supplierMapperEx.findByTypeAndKey("供应商", key, limit);
            if(organId!=null) {
                list = addOrganToList(list, organId, "供应商");
            }
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<Supplier> findBySelectRetail(String key, Long organId, Integer limit)throws Exception {
        List<Supplier> list=null;
        try{
            list = supplierMapperEx.findByTypeAndKey("会员", key, limit);
            if(organId!=null) {
                list = addOrganToList(list, organId, "会员");
            }
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    /**
     * 给列表追加供应商信息
     * @param list
     * @param organId
     * @return
     */
    public List<Supplier> addOrganToList(List<Supplier> list, Long organId, String expectedType) {
        if (list == null) list = new ArrayList<>();
        boolean isExist = false;
        for(Supplier supplier: list) {
            if(supplier.getId().equals(organId)) {
                isExist = true;
            }
        }
        if(!isExist) {
            //列表里面不存在则追加
            Supplier info = supplierMapperEx.getInfoById(organId);
            if(info != null && expectedType.equals(info.getType()) && Boolean.TRUE.equals(info.getEnabled())) {
                list.add(info);
            }
        }
        return list;
    }

    public List<Supplier> findById(Long supplierId)throws Exception {
        SupplierExample example = new SupplierExample();
        example.createCriteria().andIdEqualTo(supplierId)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        example.setOrderByClause("sort asc, id desc");
        List<Supplier> list=null;
        try{
            list = supplierMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchSetStatus(Boolean status, String ids)throws Exception {
        if (status == null) {
            throw new BusinessRunTimeException(ExceptionConstants.SUPPLIER_INVALID_CODE,
                    String.format(ExceptionConstants.SUPPLIER_INVALID_MSG, "状态不能为空"));
        }
        logService.insertLog("商家",
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_ENABLED).toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        List<Long> supplierIds = StringUtil.strToLongList(ids);
        if (supplierIds.isEmpty()) return 0;
        for (Long id : supplierIds) {
            Supplier existing = getSupplier(id);
            if (existing == null) {
                throw new BusinessRunTimeException(ExceptionConstants.SUPPLIER_NOT_EXISTS_CODE,
                        ExceptionConstants.SUPPLIER_NOT_EXISTS_MSG);
            }
            checkEditPermission(existing.getType());
        }
        Supplier supplier = new Supplier();
        supplier.setEnabled(status);
        SupplierExample example = new SupplierExample();
        example.createCriteria().andIdIn(supplierIds)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        int result=0;
        try{
            result = supplierMapper.updateByExampleSelective(supplier, example);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    public List<Supplier> findUserCustomer()throws Exception{
        SupplierExample example = new SupplierExample();
        example.createCriteria().andTypeEqualTo("客户")
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        example.setOrderByClause("sort asc, id desc");
        List<Supplier> list=null;
        try{
            list = supplierMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<Supplier> findByAll(String supplier, String type, String phonenum, String telephone) throws Exception{
        List<Supplier> list=null;
        try{
            list = supplierMapperEx.findByAll(supplier, type, phonenum, telephone);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public Map<String, Object> getBeginNeedByOrganId(Long organId) throws Exception {
        Supplier supplier = getSupplier(organId);
        if (supplier == null) {
            throw new BusinessRunTimeException(ExceptionConstants.SUPPLIER_NOT_EXISTS_CODE,
                    ExceptionConstants.SUPPLIER_NOT_EXISTS_MSG);
        }
        Map<String, Object> map = new HashMap<>();
        BigDecimal needDebt = BigDecimal.ZERO;
        if("供应商".equals(supplier.getType())) {
            needDebt = supplier.getBeginNeedPay();
        } else if("客户".equals(supplier.getType())) {
            needDebt = supplier.getBeginNeedGet();
        }
        BigDecimal finishDebtValue = accountItemMapperEx.getFinishDebtByOrganId(organId);
        BigDecimal finishDebt = finishDebtValue == null ? BigDecimal.ZERO : finishDebtValue.abs();
        BigDecimal eachAmount = BigDecimal.ZERO;
        if(needDebt != null) {
            eachAmount = needDebt.subtract(finishDebt);
        }
        //应收欠款
        map.put("needDebt", needDebt);
        //已收欠款
        map.put("finishDebt", finishDebt);
        //本次收款
        map.put("eachAmount", eachAmount);
        return map;
    }

    /**
     * 校验文件格式
     * @param file
     */
    public void checkFileExt(MultipartFile file) {
        //文件扩展名只能为xls
        String fileName = file.getOriginalFilename();
        if(StringUtil.isNotEmpty(fileName)) {
            String fileExt = fileName.substring(fileName.lastIndexOf(".")+1);
            if(!"xls".equals(fileExt)) {
                throw new BusinessRunTimeException(ExceptionConstants.FILE_EXTENSION_ERROR_CODE,
                        ExceptionConstants.FILE_EXTENSION_ERROR_MSG);
            }
        }
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void importVendor(MultipartFile file, HttpServletRequest request) throws Exception{
        String type = "供应商";
        User userInfo = userService.getCurrentUser();
        Workbook workbook = Workbook.getWorkbook(file.getInputStream());
        Sheet src = workbook.getSheet(0);
        //'名称', '联系人', '手机号码', '联系电话', '电子邮箱', '传真', '期初应付', '纳税人识别号', '税率(%)', '开户行', '账号', '地址', '备注', '排序', '状态'
        List<Supplier> sList = new ArrayList<>();
        for (int i = 2; i < src.getRows(); i++) {
            String supplierName = ExcelUtils.getContent(src, i, 0);
            String enabled = ExcelUtils.getContent(src, i, 14);
            if(StringUtil.isNotEmpty(supplierName) && StringUtil.isNotEmpty(enabled)) {
                Supplier s = new Supplier();
                s.setType(type);
                s.setSupplier(supplierName);
                s.setContacts(ExcelUtils.getContent(src, i, 1));
                s.setTelephone(ExcelUtils.getContent(src, i, 2));
                s.setPhoneNum(ExcelUtils.getContent(src, i, 3));
                s.setEmail(ExcelUtils.getContent(src, i, 4));
                s.setFax(ExcelUtils.getContent(src, i, 5));
                s.setBeginNeedPay(parseBigDecimalEx(ExcelUtils.getContent(src, i, 6)));
                s.setTaxNum(ExcelUtils.getContent(src, i, 7));
                s.setTaxRate(parseBigDecimalEx(ExcelUtils.getContent(src, i, 8)));
                s.setBankName(ExcelUtils.getContent(src, i, 9));
                s.setAccountNumber(ExcelUtils.getContent(src, i, 10));
                s.setAddress(ExcelUtils.getContent(src, i, 11));
                s.setDescription(ExcelUtils.getContent(src, i, 12));
                s.setSort(ExcelUtils.getContent(src, i, 13));
                s.setCreator(userInfo==null?null:userInfo.getId());
                s.setEnabled("1".equals(enabled));
                sList.add(s);
            }
        }
        importExcel(sList, type, request);
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void importCustomer(MultipartFile file, HttpServletRequest request) throws Exception{
        String type = "客户";
        User userInfo = userService.getCurrentUser();
        Workbook workbook = Workbook.getWorkbook(file.getInputStream());
        Sheet src = workbook.getSheet(0);
        //'名称', '联系人', '手机号码', '联系电话', '电子邮箱', '传真', '期初应收', '纳税人识别号', '税率(%)', '开户行', '账号', '地址', '备注', '排序', '状态'
        List<Supplier> sList = new ArrayList<>();
        for (int i = 2; i < src.getRows(); i++) {
            String supplierName = ExcelUtils.getContent(src, i, 0);
            String enabled = ExcelUtils.getContent(src, i, 14);
            if(StringUtil.isNotEmpty(supplierName) && StringUtil.isNotEmpty(enabled)) {
                Supplier s = new Supplier();
                s.setType(type);
                s.setSupplier(supplierName);
                s.setContacts(ExcelUtils.getContent(src, i, 1));
                s.setTelephone(ExcelUtils.getContent(src, i, 2));
                s.setPhoneNum(ExcelUtils.getContent(src, i, 3));
                s.setEmail(ExcelUtils.getContent(src, i, 4));
                s.setFax(ExcelUtils.getContent(src, i, 5));
                s.setBeginNeedGet(parseBigDecimalEx(ExcelUtils.getContent(src, i, 6)));
                s.setTaxNum(ExcelUtils.getContent(src, i, 7));
                s.setTaxRate(parseBigDecimalEx(ExcelUtils.getContent(src, i, 8)));
                s.setBankName(ExcelUtils.getContent(src, i, 9));
                s.setAccountNumber(ExcelUtils.getContent(src, i, 10));
                s.setAddress(ExcelUtils.getContent(src, i, 11));
                s.setDescription(ExcelUtils.getContent(src, i, 12));
                s.setSort(ExcelUtils.getContent(src, i, 13));
                s.setCreator(userInfo==null?null:userInfo.getId());
                s.setEnabled("1".equals(enabled));
                sList.add(s);
            }
        }
        importExcel(sList, type, request);
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void importMember(MultipartFile file, HttpServletRequest request) throws Exception{
        String type = "会员";
        User userInfo = userService.getCurrentUser();
        Workbook workbook = Workbook.getWorkbook(file.getInputStream());
        Sheet src = workbook.getSheet(0);
        //'名称', '联系人', '手机号码', '联系电话', '电子邮箱', '备注', '排序', '状态'
        List<Supplier> sList = new ArrayList<>();
        for (int i = 2; i < src.getRows(); i++) {
            String supplierName = ExcelUtils.getContent(src, i, 0);
            String enabled = ExcelUtils.getContent(src, i, 7);
            if(StringUtil.isNotEmpty(supplierName) && StringUtil.isNotEmpty(enabled)) {
                Supplier s = new Supplier();
                s.setType(type);
                s.setSupplier(supplierName);
                s.setContacts(ExcelUtils.getContent(src, i, 1));
                s.setTelephone(ExcelUtils.getContent(src, i, 2));
                s.setPhoneNum(ExcelUtils.getContent(src, i, 3));
                s.setEmail(ExcelUtils.getContent(src, i, 4));
                s.setDescription(ExcelUtils.getContent(src, i, 5));
                s.setSort(ExcelUtils.getContent(src, i, 6));
                s.setCreator(userInfo==null?null:userInfo.getId());
                s.setEnabled("1".equals(enabled));
                sList.add(s);
            }
        }
        importExcel(sList, type, request);
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public BaseResponseInfo importExcel(List<Supplier> mList, String type, HttpServletRequest request) throws Exception {
        checkEditPermission(type);
        logService.insertLog(type,
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_IMPORT).append(mList.size()).append(BusinessConstants.LOG_DATA_UNIT).toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        BaseResponseInfo info = new BaseResponseInfo();
        Map<String, Object> data = new HashMap<>();
        for(Supplier supplier: mList) {
                supplier.setType(type);
                SupplierExample example = new SupplierExample();
                example.createCriteria().andSupplierEqualTo(supplier.getSupplier()).andTypeEqualTo(type).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
                List<Supplier> list= supplierMapper.selectByExample(example);
                Supplier existing = list.isEmpty() ? null : list.get(0);
                validateSupplier(supplier, existing);
                if (existing != null && (!Objects.equals(existing.getBeginNeedPay(), supplier.getBeginNeedPay())
                        || !Objects.equals(existing.getBeginNeedGet(), supplier.getBeginNeedGet())) && isInUse(existing.getId())) {
                    throw new BusinessRunTimeException(ExceptionConstants.SUPPLIER_IN_USE_CODE,
                            ExceptionConstants.SUPPLIER_IN_USE_MSG);
                }
                if(list.size() <= 0) {
                    supplier.setId(null);
                    supplier.setTenantId(null);
                    supplier.setDeleteFlag(null);
                    supplierMapper.insertSelective(supplier);
                    //新增客户时给当前用户和租户自动授权
                    setUserCustomerPermission(request, supplier);
                } else {
                    Supplier existing = list.get(0);
                    supplier.setId(existing.getId());
                    supplier.setCreator(null);
                    supplier.setTenantId(null);
                    supplier.setDeleteFlag(null);
                    supplier.setAdvanceIn(null);
                    supplier.setAllNeedGet(null);
                    supplier.setAllNeedPay(null);
                    supplierMapper.updateByPrimaryKeySelective(supplier);
                }
            }
        info.code = 200;
        data.put("message", "成功");
        info.data = data;
        return info;
    }

    public BigDecimal parseBigDecimalEx(String str)throws Exception{
        if(!StringUtil.isEmpty(str)) {
            return new BigDecimal(str);
        } else {
            return null;
        }
    }

    public File exportExcel(List<Supplier> dataList, String type) throws Exception {
        if("供应商".equals(type)) {
            return exportExcelVendorOrCustomer(dataList, type);
        } else if("客户".equals(type)) {
            return exportExcelVendorOrCustomer(dataList, type);
        } else {
            //会员
            String[] names = {"会员卡号*", "联系人", "手机号码", "联系电话", "电子邮箱", "备注", "排序", "状态*"};
            String title = "信息内容";
            List<Object[]> objects = new ArrayList<>();
            if (null != dataList) {
                for (Supplier s : dataList) {
                    Object[] objs = new Object[names.length];
                    objs[0] = s.getSupplier();
                    objs[1] = s.getContacts();
                    objs[2] = s.getTelephone();
                    objs[3] = s.getPhoneNum();
                    objs[4] = s.getEmail();
                    objs[5] = s.getDescription();
                    objs[6] = s.getSort();
                    objs[7] = s.getEnabled() ? "1" : "0";
                    objects.add(objs);
                }
            }
            return ExcelUtils.exportObjectsOneSheet(fileExportTmp, title, "*导入时本行内容请勿删除，切记！", names, title, objects);
        }
    }

    private File exportExcelVendorOrCustomer(List<Supplier> dataList, String type) throws Exception {
        String beginNeedStr = "";
        if("供应商".equals(type)) {
            beginNeedStr = "期初应付";
        } else if("客户".equals(type)) {
            beginNeedStr = "期初应收";
        }
        String[] names = {"名称*", "联系人", "手机号码", "联系电话", "电子邮箱", "传真", beginNeedStr,
                "纳税人识别号", "税率(%)", "开户行", "账号", "地址", "备注", "排序", "状态*"};
        String title = "信息内容";
        List<Object[]> objects = new ArrayList<>();
        if (null != dataList) {
            for (Supplier s : dataList) {
                Object[] objs = new Object[names.length];
                objs[0] = s.getSupplier();
                objs[1] = s.getContacts();
                objs[2] = s.getTelephone();
                objs[3] = s.getPhoneNum();
                objs[4] = s.getEmail();
                objs[5] = s.getFax();
                if(("客户").equals(s.getType())) {
                    objs[6] = s.getBeginNeedGet() == null? "" : s.getBeginNeedGet().setScale(2,BigDecimal.ROUND_HALF_UP);
                } else if(("供应商").equals(s.getType())) {
                    objs[6] = s.getBeginNeedPay() == null? "" : s.getBeginNeedPay().setScale(2,BigDecimal.ROUND_HALF_UP);
                }
                objs[7] = s.getTaxNum();
                objs[8] = s.getTaxRate() == null? "" : s.getTaxRate().setScale(2,BigDecimal.ROUND_HALF_UP);
                objs[9] = s.getBankName();
                objs[10] = s.getAccountNumber();
                objs[11] = s.getAddress();
                objs[12] = s.getDescription();
                objs[13] = s.getSort();
                objs[14] = s.getEnabled() ? "1" : "0";
                objects.add(objs);
            }
        }
        return ExcelUtils.exportObjectsOneSheet(fileExportTmp, title, "*导入时本行内容请勿删除，切记！", names, title, objects);
    }

    /**
     * 新增客户时给当前用户和租户自动授权
     * @param request
     * @param supplier
     * @throws Exception
     */
    private void setUserCustomerPermission(HttpServletRequest request, Supplier supplier) throws Exception {
        if("客户".equals(supplier.getType())) {
            User user = userService.getCurrentUser();
            Supplier sInfo = supplierMapperEx.getSupplierByNameAndType(supplier.getSupplier(), supplier.getType());
            String ubKey = "[" + sInfo.getId() + "]";
            //授权当前用户
            setPermissionByParam(user.getId(), ubKey);
            if(!user.getId().equals(user.getTenantId())) {
                //授权当前租户
                setPermissionByParam(user.getTenantId(), ubKey);
            }
        }
    }

    /**
     * 权限授权操作
     * @param userId
     * @param ubKey
     * @throws Exception
     */
    private void setPermissionByParam(Long userId, String ubKey) throws Exception {
        List<UserBusiness> ubList = userBusinessService.getBasicData(userId.toString(), "UserCustomer");
        if(ubList ==null || ubList.size() == 0) {
            JSONObject ubObj = new JSONObject();
            ubObj.put("type", "UserCustomer");
            ubObj.put("keyId", userId);
            ubObj.put("value", ubKey);
            UserBusiness userBusiness = JSONObject.parseObject(ubObj.toJSONString(), UserBusiness.class);
            userBusinessMapper.insertSelective(userBusiness);
        } else {
            UserBusiness ubInfo = ubList.get(0);
            JSONObject ubObj = new JSONObject();
            ubObj.put("id", ubInfo.getId());
            ubObj.put("type", ubInfo.getType());
            ubObj.put("keyId", ubInfo.getKeyId());
            ubObj.put("value", ubInfo.getValue() + ubKey);
            UserBusiness userBusiness = JSONObject.parseObject(ubObj.toJSONString(), UserBusiness.class);
            userBusinessMapper.updateByPrimaryKeySelective(userBusiness);
        }
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchSetAdvanceIn(String ids) throws Exception {
        int res = 0;
        Set<Long> idSet = new TreeSet<>(StringUtil.strToLongList(ids));
        for(Long sId: idSet) {
            Supplier supplier = lockSupplier(sId);
            if(supplier == null || !"会员".equals(supplier.getType())
                    || !Boolean.TRUE.equals(supplier.getEnabled())
                    || BusinessConstants.DELETE_FLAG_DELETED.equals(supplier.getDeleteFlag())) {
                throw new BusinessRunTimeException(ExceptionConstants.ACCOUNT_HEAD_ADVANCE_IN_ORGAN_FAILED_CODE,
                        ExceptionConstants.ACCOUNT_HEAD_ADVANCE_IN_ORGAN_FAILED_MSG);
            }
        }
        for(Long sId: idSet) {
            updateAdvanceIn(sId);
            res = 1;
        }
        return res;
    }

    public List<SupplierSimple> getAllCustomer() {
        return supplierMapperEx.getAllCustomer();
    }

    public Supplier getInfoByName(String name, String type) {
        return supplierMapperEx.getInfoByName(name, type);
    }
}
