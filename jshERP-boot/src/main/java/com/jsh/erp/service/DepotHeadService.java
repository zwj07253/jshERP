package com.jsh.erp.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.*;
import com.jsh.erp.datasource.mappers.DepotHeadMapper;
import com.jsh.erp.datasource.mappers.DepotHeadMapperEx;
import com.jsh.erp.datasource.mappers.DepotItemMapperEx;
import com.jsh.erp.datasource.vo.*;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.exception.JshException;
import com.jsh.erp.utils.ExcelUtils;
import com.jsh.erp.utils.PageUtils;
import com.jsh.erp.utils.StringUtil;
import com.jsh.erp.utils.Tools;
import jxl.Workbook;
import jxl.write.WritableWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

import static com.jsh.erp.utils.Tools.getCenternTime;
import static com.jsh.erp.utils.Tools.getNow3;

@Service
public class DepotHeadService {
    private Logger logger = LoggerFactory.getLogger(DepotHeadService.class);
    private static final String RETAIL_OUT_URL = "/bill/retail_out";
    private static final String RETAIL_BACK_URL = "/bill/retail_back";
    private static final String PURCHASE_APPLY_URL = "/bill/purchase_apply";
    private static final String PURCHASE_ORDER_URL = "/bill/purchase_order";
    private static final String PURCHASE_IN_URL = "/bill/purchase_in";
    private static final String PURCHASE_RETURN_URL = "/bill/purchase_back";
    private static final String SALES_ORDER_URL = "/bill/sale_order";
    private static final String SALES_OUT_URL = "/bill/sale_out";
    private static final String SALES_RETURN_URL = "/bill/sale_back";
    private static final String OTHER_IN_URL = "/bill/other_in";
    private static final String OTHER_OUT_URL = "/bill/other_out";
    private static final String TRANSFER_OUT_URL = "/bill/allocation_out";
    private static final String ASSEMBLE_URL = "/bill/assemble";
    private static final String DISASSEMBLE_URL = "/bill/disassemble";

    @Resource
    private DepotHeadMapper depotHeadMapper;
    @Resource
    private DepotHeadMapperEx depotHeadMapperEx;
    @Resource
    private UserService userService;
    @Resource
    private RoleService roleService;
    @Resource
    private DepotService depotService;
    @Resource
    DepotItemService depotItemService;
    @Resource
    private SupplierService supplierService;
    @Resource
    private MaterialExtendService materialExtendService;
    @Resource
    private UserBusinessService userBusinessService;
    @Resource
    private SystemConfigService systemConfigService;
    @Resource
    private SerialNumberService serialNumberService;
    @Resource
    private OrgaUserRelService orgaUserRelService;
    @Resource
    private PersonService personService;
    @Resource
    private AccountService accountService;
    @Resource
    private AccountHeadService accountHeadService;
    @Resource
    private AccountItemService accountItemService;
    @Resource
    private SequenceService sequenceService;
    @Resource
    private RedisService redisService;
    @Resource
    DepotItemMapperEx depotItemMapperEx;
    @Resource
    private LogService logService;

    @Value(value="${file.exportTmp}")
    private String fileExportTmp;

    public DepotHead getDepotHead(long id)throws Exception {
        DepotHead result=null;
        try{
            result=depotHeadMapper.selectByPrimaryKey(id);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    public List<DepotHead> getDepotHead()throws Exception {
        DepotHeadExample example = new DepotHeadExample();
        example.createCriteria().andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<DepotHead> list=null;
        try{
            list=depotHeadMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<DepotHeadVo4List> select(String type, String subType, String hasDebt, String hasLastDebt, String status, String purchaseStatus, String number, String linkApply, String linkNumber,
           String beginTime, String endTime, String materialParam, Long organId, Long creator, Long depotId, Long accountId, String salesMan, String remark) throws Exception {
        List<DepotHeadVo4List> list = new ArrayList<>();
        try{
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            Long userId = userService.getUserId(request);
            String priceLimit = userService.getRoleTypeByUserId(userId).getPriceLimit();
            String billCategory = getBillCategory(subType);
            String [] depotArray = getDepotArray(subType);
            String [] creatorArray = getCreatorArray();
            String [] statusArray = StringUtil.isNotEmpty(status) ? status.split(",") : null;
            String [] purchaseStatusArray = StringUtil.isNotEmpty(purchaseStatus) ? purchaseStatus.split(",") : null;
            String [] organArray = getOrganArray(subType, purchaseStatus);
            //以销定购，查看全部数据
            creatorArray = StringUtil.isNotEmpty(purchaseStatus) ? null: creatorArray;
            Map<Long,String> personMap = personService.getPersonMap();
            Map<Long,String> accountMap = accountService.getAccountMap();
            beginTime = Tools.parseDayToTime(beginTime,BusinessConstants.DAY_FIRST_TIME);
            endTime = Tools.parseDayToTime(endTime,BusinessConstants.DAY_LAST_TIME);
            PageUtils.startPage();
            list = depotHeadMapperEx.selectByConditionDepotHead(type, subType, creatorArray, hasDebt, hasLastDebt,
                    statusArray, purchaseStatusArray, number, linkApply, linkNumber, beginTime, endTime,
                    materialParam, organId, organArray, creator, depotId, depotArray, accountId, salesMan, remark);
            if (null != list) {
                List<Long> idList = new ArrayList<>();
                List<String> numberList = new ArrayList<>();
                for (DepotHeadVo4List dh : list) {
                    idList.add(dh.getId());
                    numberList.add(dh.getNumber());
                }
                //通过批量查询去构造map
                Map<String,BigDecimal> finishDepositMap = getFinishDepositMapByNumberList(numberList);
                Map<Long,BigDecimal> financialBillPriceMap = getFinancialBillPriceMapByBillIdList(idList);
                Map<String,Integer> billSizeMap = getBillSizeMapByLinkNumberList(numberList);
                Map<Long,String> materialsListMap = findMaterialsListMapByHeaderIdList(idList);
                Map<Long,BigDecimal> materialCountListMap = getMaterialCountListMapByHeaderIdList(idList);
                for (DepotHeadVo4List dh : list) {
                    if(accountMap!=null && StringUtil.isNotEmpty(dh.getAccountIdList()) && StringUtil.isNotEmpty(dh.getAccountMoneyList())) {
                        String accountStr = accountService.getAccountStrByIdAndMoney(accountMap, dh.getAccountIdList(), dh.getAccountMoneyList());
                        dh.setAccountName(accountStr);
                    }
                    if(dh.getAccountIdList() != null) {
                        String accountidlistStr = dh.getAccountIdList().replace("[", "").replace("]", "").replaceAll("\"", "");
                        dh.setAccountIdList(accountidlistStr);
                    }
                    if(dh.getAccountMoneyList() != null) {
                        String accountmoneylistStr = dh.getAccountMoneyList().replace("[", "").replace("]", "").replaceAll("\"", "");
                        dh.setAccountMoneyList(accountmoneylistStr);
                    }
                    if(dh.getChangeAmount() != null) {
                        dh.setChangeAmount(roleService.parseBillPriceByLimit(dh.getChangeAmount().abs(), billCategory, priceLimit, request));
                    } else {
                        dh.setChangeAmount(BigDecimal.ZERO);
                    }
                    if(dh.getTotalPrice() != null) {
                        BigDecimal lastTotalPrice = BusinessConstants.SUB_TYPE_CHECK_ENTER.equals(dh.getSubType())||
                                BusinessConstants.SUB_TYPE_REPLAY.equals(dh.getSubType())?dh.getTotalPrice():dh.getTotalPrice().abs();
                        dh.setTotalPrice(roleService.parseBillPriceByLimit(lastTotalPrice, billCategory, priceLimit, request));
                    }
                    BigDecimal discountLastMoney = dh.getDiscountLastMoney()!=null?dh.getDiscountLastMoney():BigDecimal.ZERO;
                    dh.setDiscountLastMoney(roleService.parseBillPriceByLimit(discountLastMoney, billCategory, priceLimit, request));
                    BigDecimal backAmount = dh.getBackAmount()!=null?dh.getBackAmount():BigDecimal.ZERO;
                    dh.setBackAmount(roleService.parseBillPriceByLimit(backAmount, billCategory, priceLimit, request));
                    if(dh.getDeposit() == null) {
                        dh.setDeposit(BigDecimal.ZERO);
                    } else {
                        dh.setDeposit(roleService.parseBillPriceByLimit(dh.getDeposit(), billCategory, priceLimit, request));
                    }
                    //已经完成的欠款
                    if(finishDepositMap!=null) {
                        BigDecimal finishDeposit = finishDepositMap.get(dh.getNumber()) != null ? finishDepositMap.get(dh.getNumber()) : BigDecimal.ZERO;
                        dh.setFinishDeposit(roleService.parseBillPriceByLimit(finishDeposit, billCategory, priceLimit, request));
                    }
                    //欠款计算
                    BigDecimal otherMoney = dh.getOtherMoney()!=null?dh.getOtherMoney():BigDecimal.ZERO;
                    BigDecimal deposit = dh.getDeposit()!=null?dh.getDeposit():BigDecimal.ZERO;
                    BigDecimal changeAmount = dh.getChangeAmount()!=null?dh.getChangeAmount():BigDecimal.ZERO;
                    BigDecimal debt = discountLastMoney.add(otherMoney).subtract((deposit.add(changeAmount)));
                    dh.setDebt(roleService.parseBillPriceByLimit(debt, billCategory, priceLimit, request));
                    //最终欠款的金额
                    //TODO 暂时还是先通过计算获取，半年后改成从数据库直接去读last_dept time:20260601
                    if(financialBillPriceMap!=null) {
                        BigDecimal financialBillPrice = financialBillPriceMap.get(dh.getId())!=null?financialBillPriceMap.get(dh.getId()):BigDecimal.ZERO;
                        BigDecimal lastDebt = debt.subtract(financialBillPrice);
                        dh.setLastDebt(roleService.parseBillPriceByLimit(lastDebt, billCategory, priceLimit, request));
                    }
                    //是否有退款单
                    if(billSizeMap!=null) {
                        Integer billListSize = billSizeMap.get(dh.getNumber());
                        dh.setHasBackFlag(billListSize!=null && billListSize>0);
                    }
                    if(StringUtil.isNotEmpty(dh.getSalesMan())) {
                        dh.setSalesManStr(personService.getPersonByMapAndIds(personMap,dh.getSalesMan()));
                    }
                    if(dh.getOperTime() != null) {
                        dh.setOperTimeStr(getCenternTime(dh.getOperTime()));
                    }
                    //商品信息简述
                    if(materialsListMap!=null) {
                        dh.setMaterialsList(materialsListMap.get(dh.getId()));
                    }
                    //商品总数量
                    if(materialCountListMap!=null) {
                        dh.setMaterialCount(materialCountListMap.get(dh.getId()));
                    }
                    //以销定购的情况（不能显示销售单据的金额和客户名称）
                    if(StringUtil.isNotEmpty(purchaseStatus)) {
                        dh.setOrganName("****");
                        dh.setTotalPrice(null);
                        dh.setDiscountLastMoney(null);
                    }
                }
            }
        } catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    /**
     * 根据单据类型获取仓库数组
     * @param subType
     * @return
     * @throws Exception
     */
    public String[] getDepotArray(String subType) throws Exception {
        String [] depotArray = null;
        if(!BusinessConstants.SUB_TYPE_PURCHASE_APPLY.equals(subType)
                && !BusinessConstants.SUB_TYPE_PURCHASE_ORDER.equals(subType)
                && !BusinessConstants.SUB_TYPE_SALES_ORDER.equals(subType)) {
            if (isCurrentUserAdmin()) {
                return null;
            }
            String depotIds = depotService.findDepotStrByCurrentUser();
            //开启仓库权限但当前用户未分配仓库时，必须返回一个不可能命中的值，不能退化成“不过滤”。
            depotArray = StringUtil.isNotEmpty(depotIds) ? depotIds.split(",") : new String[]{"-1"};
        }
        return depotArray;
    }

    /**
     * 根据角色类型获取操作员数组
     * @return
     * @throws Exception
     */
    public String[] getCreatorArray() throws Exception {
        String creator = getCreatorByCurrentUser();
        String [] creatorArray=null;
        if(StringUtil.isNotEmpty(creator)){
            creatorArray = creator.split(",");
        }
        return creatorArray;
    }

    /**
     * 根据角色类型获取操作员数组
     * @param organizationId
     * @return
     * @throws Exception
     */
    public String[] getCreatorArrayByOrg(Long organizationId) throws Exception {
        List<Long> userIdList = orgaUserRelService.getUserIdListByOrgId(organizationId);
        if(userIdList.size()>0) {
            List<String> userIdStrList = userIdList.stream().map(Object::toString).collect(Collectors.toList());
            return StringUtil.listToStringArray(userIdStrList);
        } else {
            return "-1".split(",");
        }
    }

    /**
     * 获取部门数组
     * @return
     */
    public String[] getOrganArray(String subType, String purchaseStatus) throws Exception {
        String [] organArray = null;
        String type = "UserCustomer";
        if (isCurrentUserAdmin()) {
            return null;
        }
        Long userId = userService.getCurrentUser().getId();
        //获取权限信息
        String ubValue = userBusinessService.getUBValueByTypeAndKeyId(type, userId.toString());
        List<SupplierSimple> supplierList = supplierService.getAllCustomer();
        if(BusinessConstants.SUB_TYPE_SALES_ORDER.equals(subType) || BusinessConstants.SUB_TYPE_SALES.equals(subType)
                ||BusinessConstants.SUB_TYPE_SALES_RETURN.equals(subType) ) {
            //采购订单里面选择销售订单的时候不要过滤
            if(StringUtil.isEmpty(purchaseStatus)) {
                if (null != supplierList && supplierList.size() > 0) {
                    boolean customerFlag = systemConfigService.getCustomerFlag();
                    List<String> organList = new ArrayList<>();
                    for (SupplierSimple supplier : supplierList) {
                        boolean flag = ubValue.contains("[" + supplier.getId().toString() + "]");
                        if (!customerFlag || flag) {
                            organList.add(supplier.getId().toString());
                        }
                    }
                    if(organList.size() > 0) {
                        organArray = StringUtil.listToStringArray(organList);
                    } else if (customerFlag) {
                        organArray = new String[]{"-1"};
                    }
                }
            }
        }
        return organArray;
    }

    /**
     * 根据角色类型获取操作员
     * @return
     * @throws Exception
     */
    public String getCreatorByCurrentUser() throws Exception {
        String creator = "";
        User user = userService.getCurrentUser();
        String roleType = userService.getRoleTypeByUserId(user.getId()).getType(); //角色类型
        if(BusinessConstants.ROLE_TYPE_PRIVATE.equals(roleType)) {
            creator = user.getId().toString();
        } else if(BusinessConstants.ROLE_TYPE_THIS_ORG.equals(roleType)) {
            creator = orgaUserRelService.getUserIdListByUserId(user.getId());
        }
        return creator;
    }

    public Map<String, BigDecimal> getFinishDepositMapByNumberList(List<String> numberList) {
        Map<String,BigDecimal> finishDepositMap = new HashMap<>();
        if(numberList.size()>0) {
            List<FinishDepositVo> list = depotHeadMapperEx.getFinishDepositByNumberList(numberList);
            if(list!=null && list.size()>0) {
                for (FinishDepositVo finishDepositVo : list) {
                    if(finishDepositVo!=null) {
                        finishDepositMap.put(finishDepositVo.getNumber(), finishDepositVo.getFinishDeposit());
                    }
                }
            }
        }
        return finishDepositMap;
    }

    public Map<String, Integer> getBillSizeMapByLinkNumberList(List<String> numberList) throws Exception {
        Map<String, Integer> billListMap = new HashMap<>();
        if(numberList.size()>0) {
            List<DepotHead> list = getBillListByLinkNumberList(numberList);
            if(list!=null && list.size()>0) {
                for (DepotHead depotHead : list) {
                    if(depotHead!=null) {
                        billListMap.put(depotHead.getLinkNumber(), list.size());
                    }
                }
            }
        }
        return billListMap;
    }

    public Map<Long, BigDecimal> getFinancialBillPriceMapByBillIdList(List<Long> idList) {
        Map<Long, BigDecimal> billListMap = new HashMap<>();
        if(!idList.isEmpty()) {
            List<AccountItem> list = accountHeadService.getFinancialBillPriceByBillIdList(idList);
            if(list!=null && !list.isEmpty()) {
                for (AccountItem accountItem : list) {
                    if(accountItem!=null && accountItem.getEachAmount()!=null) {
                        billListMap.put(accountItem.getBillId(), accountItem.getEachAmount().abs());
                    }
                }
            }
        }
        return billListMap;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int insertDepotHead(JSONObject obj, HttpServletRequest request)throws Exception {
        DepotHead depotHead = JSONObject.parseObject(obj.toJSONString(), DepotHead.class);
        depotHead.setCreateTime(new Timestamp(System.currentTimeMillis()));
        depotHead.setStatus(BusinessConstants.BILLS_STATUS_UN_AUDIT);
        int result=0;
        try{
            result=depotHeadMapper.insert(depotHead);
            logService.insertLog("单据", BusinessConstants.LOG_OPERATION_TYPE_ADD, request);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int updateDepotHead(JSONObject obj, HttpServletRequest request) throws Exception{
        DepotHead depotHead = JSONObject.parseObject(obj.toJSONString(), DepotHead.class);
        DepotHead dh=null;
        try{
            dh = depotHeadMapper.selectByPrimaryKey(depotHead.getId());
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        depotHead.setStatus(dh.getStatus());
        depotHead.setCreateTime(dh.getCreateTime());
        int result=0;
        try{
            result = depotHeadMapper.updateByPrimaryKey(depotHead);
            logService.insertLog("单据",
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append(depotHead.getId()).toString(), request);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int deleteDepotHead(Long id, HttpServletRequest request)throws Exception {
        return batchDeleteBillByIds(id.toString());
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteDepotHead(String ids, HttpServletRequest request)throws Exception {
        return batchDeleteBillByIds(ids);
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteBillByIds(String ids)throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(BusinessConstants.LOG_OPERATION_TYPE_DELETE);
        List<DepotHead> dhList = getDepotHeadListByIds(ids);
        for(DepotHead depotHead: dhList){
            checkBillButtonPermission(depotHead, "1", "新增、编辑或删除");
            checkPurchaseBillDataPermission(depotHead);
            checkPurchaseInboundHasNoReturn(depotHead, "删除");
            checkSalesOrderHasNoOutbound(depotHead, "删除");
            checkSalesOutboundHasNoDownstream(depotHead, "删除");
            checkSalesReturnHasNoFinancial(depotHead, "删除");
            //只有未审核的单据才能被删除
            if(!"0".equals(depotHead.getStatus())) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_UN_AUDIT_DELETE_FAILED_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_UN_AUDIT_DELETE_FAILED_MSG));
            }
        }
        Set<Long> prepaidMemberIds = new TreeSet<>();
        for(DepotHead depotHead : dhList) {
            if(isPrepaidRetailBill(depotHead) && depotHead.getOrganId() != null) {
                prepaidMemberIds.add(depotHead.getOrganId());
            }
        }
        lockPrepaidMembers(prepaidMemberIds);
        for(DepotHead depotHead: dhList){
            sb.append("[").append(depotHead.getNumber()).append("]");
            User userInfo = userService.getCurrentUser();
            //删除入库单据，先校验序列号是否出库，如果未出库则同时删除序列号，如果已出库则不能删除单据
            if (BusinessConstants.DEPOTHEAD_TYPE_IN.equals(depotHead.getType())) {
                List<DepotItem> depotItemList = depotItemMapperEx.findDepotItemListBydepotheadId(depotHead.getId(), BusinessConstants.ENABLE_SERIAL_NUMBER_ENABLED);
                if (depotItemList != null && depotItemList.size() > 0) {
                    //单据明细里面存在序列号商品
                    int serialNumberSellCount = depotHeadMapperEx.getSerialNumberBySell(depotHead.getNumber());
                    if (serialNumberSellCount > 0) {
                        //已出库则不能删除单据
                        throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_SERIAL_IS_SELL_CODE,
                                String.format(ExceptionConstants.DEPOT_HEAD_SERIAL_IS_SELL_MSG, depotHead.getNumber()));
                    } else {
                        //删除序列号
                        SerialNumberExample example = new SerialNumberExample();
                        example.createCriteria().andInBillNoEqualTo(depotHead.getNumber());
                        serialNumberService.deleteByExample(example);
                    }
                }
            }
            //删除出库数据回收序列号
            if (BusinessConstants.DEPOTHEAD_TYPE_OUT.equals(depotHead.getType())
                    && !BusinessConstants.SUB_TYPE_TRANSFER.equals(depotHead.getSubType())) {
                //查询单据子表列表
                List<DepotItem> depotItemList = depotItemMapperEx.findDepotItemListBydepotheadId(depotHead.getId(), BusinessConstants.ENABLE_SERIAL_NUMBER_ENABLED);
                /**回收序列号*/
                if (depotItemList != null && depotItemList.size() > 0) {
                    for (DepotItem depotItem : depotItemList) {
                        //BasicNumber=OperNumber*ratio
                        serialNumberService.cancelSerialNumber(depotItem.getMaterialId(), depotHead.getNumber(), (depotItem.getBasicNumber() == null ? 0 : depotItem.getBasicNumber()).intValue(), userInfo);
                    }
                }
            }
            List<DepotItem> list = depotItemService.getListByHeaderId(depotHead.getId());
            //删除单据子表数据
            depotItemMapperEx.batchDeleteDepotItemByDepotHeadIds(new Long[]{depotHead.getId()});
            //删除单据主表信息
            batchDeleteDepotHeadByIds(depotHead.getId().toString());
            //将关联的单据置为审核状态-针对采购入库、销售出库、盘点复盘、其它入库、其它出库
            if(StringUtil.isNotEmpty(depotHead.getLinkNumber())){
                if((BusinessConstants.DEPOTHEAD_TYPE_IN.equals(depotHead.getType()) &&
                        BusinessConstants.SUB_TYPE_PURCHASE.equals(depotHead.getSubType()))
                        || (BusinessConstants.DEPOTHEAD_TYPE_OUT.equals(depotHead.getType()) &&
                        BusinessConstants.SUB_TYPE_SALES.equals(depotHead.getSubType()))
                        || (BusinessConstants.DEPOTHEAD_TYPE_OTHER.equals(depotHead.getType()) &&
                        BusinessConstants.SUB_TYPE_REPLAY.equals(depotHead.getSubType()))
                        || (BusinessConstants.DEPOTHEAD_TYPE_IN.equals(depotHead.getType()) &&
                        BusinessConstants.SUB_TYPE_OTHER.equals(depotHead.getSubType()))
                        || (BusinessConstants.DEPOTHEAD_TYPE_OUT.equals(depotHead.getType()) &&
                        BusinessConstants.SUB_TYPE_OTHER.equals(depotHead.getSubType()))) {
                    String status = null;
                    if (BusinessConstants.SUB_TYPE_PURCHASE.equals(depotHead.getSubType())) {
                        status = depotItemService.getBillStatusByParam(depotHead, depotHead.getLinkNumber(), "normal");
                    } else if (BusinessConstants.SUB_TYPE_SALES.equals(depotHead.getSubType())) {
                        recalculateSalesOrderStatus(depotHead.getLinkNumber());
                    } else if (BusinessConstants.SUB_TYPE_OTHER.equals(depotHead.getSubType())) {
                        status = depotItemService.getBillStatusByParam(depotHead, depotHead.getLinkNumber(), "normal");
                    } else {
                        status = BusinessConstants.BILLS_STATUS_AUDIT;
                        //查询除当前单据之外的关联单据列表
                        List<DepotHead> exceptCurrentList = getListByLinkNumberExceptCurrent(depotHead.getLinkNumber(), depotHead.getNumber(), depotHead.getType());
                        if(exceptCurrentList!=null && exceptCurrentList.size()>0) {
                            status = BusinessConstants.BILLS_STATUS_SKIPING;
                        }
                    }
                    if (status != null) {
                        DepotHead dh = new DepotHead();
                        dh.setStatus(status);
                        DepotHeadExample example = new DepotHeadExample();
                        example.createCriteria().andNumberEqualTo(depotHead.getLinkNumber());
                        depotHeadMapper.updateByExampleSelective(dh, example);
                    }
                }
            }
            //将关联的单据置为审核状态-针对请购单转采购订单的情况
            if(StringUtil.isNotEmpty(depotHead.getLinkApply())){
                if(BusinessConstants.DEPOTHEAD_TYPE_OTHER.equals(depotHead.getType()) &&
                        BusinessConstants.SUB_TYPE_PURCHASE_ORDER.equals(depotHead.getSubType())) {
                    String status = depotItemService.getBillStatusByParam(depotHead, depotHead.getLinkApply(), "apply");
                    DepotHead dh = new DepotHead();
                    dh.setStatus(status);
                    DepotHeadExample example = new DepotHeadExample();
                    example.createCriteria().andNumberEqualTo(depotHead.getLinkApply());
                    depotHeadMapper.updateByExampleSelective(dh, example);
                }
            }
            //将关联的销售订单单据置为未采购状态-针对销售订单转采购订单的情况
            if(StringUtil.isNotEmpty(depotHead.getLinkNumber())){
                if(BusinessConstants.DEPOTHEAD_TYPE_OTHER.equals(depotHead.getType()) &&
                        BusinessConstants.SUB_TYPE_PURCHASE_ORDER.equals(depotHead.getSubType())) {
                    DepotHead dh = new DepotHead();
                    String status = depotItemService.getBillStatusByParam(depotHead, depotHead.getLinkNumber(), "normal");
                    dh.setPurchaseStatus(BusinessConstants.BILLS_STATUS_AUDIT.equals(status)
                            ? BusinessConstants.PURCHASE_STATUS_UN_AUDIT : status);
                    DepotHeadExample example = new DepotHeadExample();
                    example.createCriteria().andNumberEqualTo(depotHead.getLinkNumber());
                    depotHeadMapper.updateByExampleSelective(dh, example);
                }
            }
            //零售出库或零售退货使用预付款时，重新计算会员预付款
            if (isPrepaidRetailBill(depotHead) && depotHead.getOrganId() != null) {
                supplierService.updateAdvanceIn(depotHead.getOrganId());
            }
            for (DepotItem depotItem : list) {
                //更新当前库存
                depotItemService.updateCurrentStock(depotItem);
                //更新当前成本价
                depotItemService.updateCurrentUnitPrice(depotItem);
            }
        }
        //路径列表
        List<String> pathList = new ArrayList<>();
        for(DepotHead depotHead: dhList){
            if(StringUtil.isNotEmpty(depotHead.getFileName())) {
                pathList.add(depotHead.getFileName());
            }
        }
        //逻辑删除文件
        systemConfigService.deleteFileByPathList(pathList);
        logService.insertLog("单据", sb.toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        return 1;
    }

    /**
     * 删除单据主表信息
     * @param ids
     * @return
     * @throws Exception
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteDepotHeadByIds(String ids)throws Exception {
        User userInfo=userService.getCurrentUser();
        String [] idArray=ids.split(",");
        int result=0;
        try{
            result = depotHeadMapperEx.batchDeleteDepotHeadByIds(new Date(),userInfo==null?null:userInfo.getId(),idArray);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    public List<DepotHead> getDepotHeadListByIds(String ids)throws Exception {
        List<Long> idList = StringUtil.strToLongList(ids);
        List<DepotHead> list = new ArrayList<>();
        try{
            DepotHeadExample example = new DepotHeadExample();
            example.createCriteria().andIdIn(idList);
            list = depotHeadMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    /**
     * 校验单据编号是否存在
     * @param id
     * @param number
     * @return
     * @throws Exception
     */
    public int checkIsBillNumberExist(Long id, String number)throws Exception {
        DepotHeadExample example = new DepotHeadExample();
        example.createCriteria().andIdNotEqualTo(id).andNumberEqualTo(number).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<DepotHead> list = null;
        try{
            list = depotHeadMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list==null?0:list.size();
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchForceClose(String ids, HttpServletRequest request) throws Exception {
        int result = 0;
        StringBuilder billNoStr = new StringBuilder();
        List<Long> idList = StringUtil.strToLongList(ids);
        List<DepotHead> depotHeadList = new ArrayList<>();
        for(Long id: idList) {
            DepotHead depotHead = getDepotHead(id);
            if (depotHead == null) {
                throw new BusinessRunTimeException(ExceptionConstants.DATA_READ_FAIL_CODE,
                        ExceptionConstants.DATA_READ_FAIL_MSG);
            }
            checkBillButtonPermission(depotHead, "1", "强制结单");
            checkPurchaseBillDataPermission(depotHead);
            //状态里面不包含部分不能强制结单
            if(!"3".equals(depotHead.getStatus())) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_FORCE_CLOSE_FAILED_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_FORCE_CLOSE_FAILED_MSG, depotHead.getNumber()));
            } else {
                billNoStr.append(depotHead.getNumber()).append(" ");
                depotHeadList.add(depotHead);
            }
        }
        if(!depotHeadList.isEmpty()) {
            for (DepotHead original : depotHeadList) {
                DepotHead update = new DepotHead();
                update.setId(original.getId());
                update.setStatus("2");
                String remark = StringUtil.isNotEmpty(original.getRemark())
                        ? original.getRemark() + "[强制结单]" : "[强制结单]";
                update.setRemark(remark);
                result += depotHeadMapper.updateByPrimaryKeySelective(update);
            }
            //记录日志
            String billNos = billNoStr.toString();
            if(StringUtil.isNotEmpty(billNos)) {
                logService.insertLog("单据", "强制结单：" + billNos,
                        ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
            }
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchForceClosePurchase(String ids, HttpServletRequest request) throws Exception {
        int result = 0;
        StringBuilder billNoStr = new StringBuilder();
        List<Long> idList = StringUtil.strToLongList(ids);
        List<DepotHead> depotHeadList = new ArrayList<>();
        for(Long id: idList) {
            DepotHead depotHead = getDepotHead(id);
            if (depotHead == null) {
                throw new BusinessRunTimeException(ExceptionConstants.DATA_READ_FAIL_CODE,
                        ExceptionConstants.DATA_READ_FAIL_MSG);
            }
            checkBillButtonPermission(depotHead, "1", "强制结单");
            checkPurchaseBillDataPermission(depotHead);
            //状态里面不包含部分不能强制结单
            if(!"3".equals(depotHead.getPurchaseStatus())) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_FORCE_CLOSE_FAILED_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_FORCE_CLOSE_FAILED_MSG, depotHead.getNumber()));
            } else {
                billNoStr.append(depotHead.getNumber()).append(" ");
                depotHeadList.add(depotHead);
            }
        }
        if(!depotHeadList.isEmpty()) {
            for (DepotHead original : depotHeadList) {
                DepotHead update = new DepotHead();
                update.setId(original.getId());
                update.setPurchaseStatus("2");
                String remark = StringUtil.isNotEmpty(original.getRemark())
                        ? original.getRemark() + "[强制结单-以销定购]" : "[强制结单-以销定购]";
                update.setRemark(remark);
                result += depotHeadMapper.updateByPrimaryKeySelective(update);
            }
            //记录日志
            String billNos = billNoStr.toString();
            if(StringUtil.isNotEmpty(billNos)) {
                logService.insertLog("单据", "强制结单-以销定购：" + billNos,
                        ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
            }
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchSetLastDebt(String ids, HttpServletRequest request) throws Exception {
        int result = 0;
        StringBuilder billNoStr = new StringBuilder();
        List<Long> idList = StringUtil.strToLongList(ids);
        for(Long id: idList) {
            DepotHead dh = getDepotHead(id);
            checkBillButtonPermission(dh, "1", "修正欠款");
            checkPurchaseBillDataPermission(dh);
            BigDecimal debt = getDebtByBill(dh);
            if(debt.compareTo(BigDecimal.ZERO)!=0) {
                //更新最终欠款
                updateLastDebtByBillId(debt, id);
                billNoStr.append(dh.getNumber()).append(" ");
            }
            result = 1;
        }
        //记录日志
        String billNos = billNoStr.toString();
        if(StringUtil.isNotEmpty(billNos)) {
            logService.insertLog("单据", "修正最终欠款：" + billNos,
                    ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        }
        return result;
    }

    /**
     * 获取单据的本次欠款
     * @param dh
     * @return
     */
    public BigDecimal getDebtByBill(DepotHead dh) {
        BigDecimal discountLastMoney = dh.getDiscountLastMoney()!=null? dh.getDiscountLastMoney():BigDecimal.ZERO;
        BigDecimal otherMoney = dh.getOtherMoney()!=null? dh.getOtherMoney():BigDecimal.ZERO;
        BigDecimal deposit = dh.getDeposit()!=null? dh.getDeposit():BigDecimal.ZERO;
        BigDecimal changeAmount = dh.getChangeAmount()!=null? dh.getChangeAmount().abs():BigDecimal.ZERO;
        //本次欠款
        return discountLastMoney.add(otherMoney).subtract((deposit.add(changeAmount)));
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchSetStatus(String status, String depotHeadIDs)throws Exception {
        int result = 0;
        boolean forceApprovalFlag = systemConfigService.getForceApprovalFlag();
        boolean minusStockFlag = systemConfigService.getMinusStockFlag();
        boolean inOutManageFlag = systemConfigService.getInOutManageFlag();
        List<Long> dhIds = new ArrayList<>();
        List<String> noList = new ArrayList<>();
        Set<String> salesOrderNumbers = new HashSet<>();
        List<DepotHead> stockCheckHeadList = new ArrayList<>();
        List<DepotHead> assembleStockCheckHeadList = new ArrayList<>();
        List<DepotHead> disassembleStockCheckHeadList = new ArrayList<>();
        Set<Long> prepaidMemberIds = new TreeSet<>();
        List<DepotHead> prepaidAuditBills = new ArrayList<>();
        List<Long> ids = StringUtil.strToLongList(depotHeadIDs);
        for(Long id: ids) {
            DepotHead depotHead = getDepotHead(id);
            checkPurchaseBillDataPermission(depotHead);
            if(isPrepaidRetailBill(depotHead) && depotHead.getOrganId() != null) {
                prepaidMemberIds.add(depotHead.getOrganId());
                if(BusinessConstants.BILLS_STATUS_AUDIT.equals(status)) {
                    prepaidAuditBills.add(depotHead);
                }
            }
            if (isSalesOutbound(depotHead) && StringUtil.isNotEmpty(depotHead.getLinkNumber())) {
                salesOrderNumbers.add(depotHead.getLinkNumber());
            }
            if("0".equals(status)){
                checkBillButtonPermission(depotHead, "7", "反审核");
                checkPurchaseInboundHasNoReturn(depotHead, "反审核");
                checkSalesOrderHasNoOutbound(depotHead, "反审核");
                checkSalesOutboundHasNoDownstream(depotHead, "反审核");
                checkSalesReturnHasNoFinancial(depotHead, "反审核");
                //进行反审核操作
                if("1".equals(depotHead.getStatus())
                        && (isPurchaseReturn(depotHead) || "0".equals(depotHead.getPurchaseStatus()))) {
                    dhIds.add(id);
                    noList.add(depotHead.getNumber());
                } else if("2".equals(depotHead.getPurchaseStatus())) {
                    throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_STATUS_TWO_CODE,
                            String.format(ExceptionConstants.DEPOT_HEAD_PURCHASE_STATUS_TWO_MSG));
                } else if("3".equals(depotHead.getPurchaseStatus())) {
                    throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_STATUS_THREE_CODE,
                            String.format(ExceptionConstants.DEPOT_HEAD_PURCHASE_STATUS_THREE_MSG));
                } else {
                    throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_AUDIT_TO_UN_AUDIT_FAILED_CODE,
                            String.format(ExceptionConstants.DEPOT_HEAD_AUDIT_TO_UN_AUDIT_FAILED_MSG));
                }
            } else if("1".equals(status)){
                checkBillButtonPermission(depotHead, "2", "审核");
                //进行审核操作
                if("0".equals(depotHead.getStatus())) {
                    validateSalesOutboundBeforeAudit(depotHead);
                    validateSalesReturnBeforeAudit(depotHead);
                    validateOtherInboundBeforeAudit(depotHead);
                    validateOtherOutboundBeforeAudit(depotHead);
                    validateTransferOutboundBeforeAudit(depotHead);
                    validateAssembleBeforeAudit(depotHead);
                    validateDisassembleBeforeAudit(depotHead);
                    if (isAssemble(depotHead)) {
                        depotItemService.refreshAssembleCost(depotHead);
                    }
                    if (isDisassemble(depotHead) && forceApprovalFlag) {
                        depotItemService.refreshDisassembleCost(depotHead);
                    }
                    dhIds.add(id);
                    noList.add(depotHead.getNumber());
                } else {
                    throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_UN_AUDIT_TO_AUDIT_FAILED_CODE,
                            String.format(ExceptionConstants.DEPOT_HEAD_UN_AUDIT_TO_AUDIT_FAILED_MSG));
                }
            }
            // 开启强审核，并且没有开启负库存：
            // 1、开启出入库管理，销售出库和采购退货单据审核的时候不做校验，其它出库做校验；
            // 2、未开启出入库管理，销售出库和采购退货单据审核的时候做校验，其它出库不做校验。
            if("1".equals(status)) {
                if(forceApprovalFlag && !minusStockFlag) {
                    boolean retailOut = BusinessConstants.DEPOTHEAD_TYPE_OUT.equals(depotHead.getType())
                            && BusinessConstants.SUB_TYPE_RETAIL.equals(depotHead.getSubType());
                    boolean otherOut = isOtherOutbound(depotHead);
                    boolean transferOut = isTransferOutbound(depotHead);
                    boolean assemble = isAssemble(depotHead);
                    boolean disassemble = isDisassemble(depotHead);
                    if (assemble) {
                        assembleStockCheckHeadList.add(depotHead);
                    }
                    if (disassemble) {
                        disassembleStockCheckHeadList.add(depotHead);
                    }
                    if(inOutManageFlag) {
                        if(retailOut || otherOut || transferOut) {
                            //校验单据中的商品库存是否不足
                            stockCheckHeadList.add(depotHead);
                        }
                    } else {
                        if(retailOut || otherOut || transferOut
                                || ("出库".equals(depotHead.getType()) && "销售".equals(depotHead.getSubType()))
                                || ("出库".equals(depotHead.getType()) && "采购退货".equals(depotHead.getSubType()))) {
                            //校验单据中的商品库存是否不足
                            stockCheckHeadList.add(depotHead);
                        }
                    }
                }
            }
        }
        lockPrepaidMembers(prepaidMemberIds);
        if(BusinessConstants.BILLS_STATUS_AUDIT.equals(status)) {
            validatePrepaidBillsBeforeAudit(prepaidAuditBills);
        }
        List<DepotHead> allStockChangeHeads = new ArrayList<>();
        allStockChangeHeads.addAll(stockCheckHeadList);
        allStockChangeHeads.addAll(assembleStockCheckHeadList);
        allStockChangeHeads.addAll(disassembleStockCheckHeadList);
        depotItemService.lockMaterialsForStockChange(allStockChangeHeads);
        if (!stockCheckHeadList.isEmpty()) {
            depotItemService.checkMaterialStock(stockCheckHeadList);
        }
        if (!assembleStockCheckHeadList.isEmpty()) {
            depotItemService.checkAssembleMaterialStock(assembleStockCheckHeadList);
        }
        if (!disassembleStockCheckHeadList.isEmpty()) {
            depotItemService.checkDisassembleMaterialStock(disassembleStockCheckHeadList);
        }
        if(!dhIds.isEmpty()) {
            DepotHead depotHead = new DepotHead();
            depotHead.setStatus(status);
            DepotHeadExample example = new DepotHeadExample();
            example.createCriteria().andIdIn(dhIds);
            result = depotHeadMapper.updateByExampleSelective(depotHead, example);
            //更新当前库存
            if(systemConfigService.getForceApprovalFlag()) {
                for(Long dhId: dhIds) {
                    List<DepotItem> list = depotItemService.getListByHeaderId(dhId);
                    for (DepotItem depotItem : list) {
                        depotItemService.updateCurrentStock(depotItem);
                        depotItemService.updateCurrentUnitPrice(depotItem);
                    }
                }
            }
            for (String salesOrderNumber : salesOrderNumbers) {
                recalculateSalesOrderStatus(salesOrderNumber);
            }
            refreshPrepaidMembers(prepaidMemberIds);
            //记录日志
            if(!noList.isEmpty() && ("0".equals(status) || "1".equals(status))) {
                String statusStr = status.equals("1")?"[审核]":"[反审核]";
                logService.insertLog("单据",
                        new StringBuffer(statusStr).append(String.join(", ", noList)).toString(),
                        ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
            }
        }
        return result;
    }

    public Map<Long,String> findMaterialsListMapByHeaderIdList(List<Long> idList)throws Exception {
        Map<Long,String> materialsListMap = new HashMap<>();
        if(idList.size()>0) {
            List<MaterialsListVo> list = depotHeadMapperEx.findMaterialsListMapByHeaderIdList(idList);
            for (MaterialsListVo materialsListVo : list) {
                String materialsList = materialsListVo.getMaterialsList();
                if(StringUtil.isNotEmpty(materialsList)) {
                    materialsList = materialsList.replace(",","，");
                }
                materialsListMap.put(materialsListVo.getHeaderId(), materialsList);
            }
        }
        return materialsListMap;
    }

    public Map<Long,BigDecimal> getMaterialCountListMapByHeaderIdList(List<Long> idList)throws Exception {
        Map<Long,BigDecimal> materialCountListMap = new HashMap<>();
        if(idList.size()>0) {
            List<MaterialCountVo> list = depotHeadMapperEx.getMaterialCountListByHeaderIdList(idList);
            for(MaterialCountVo materialCountVo : list){
                materialCountListMap.put(materialCountVo.getHeaderId(), materialCountVo.getMaterialCount());
            }
        }
        return materialCountListMap;
    }

    public List<DepotHeadVo4InDetail> findInOutDetail(String beginTime, String endTime, String type, String[] creatorArray,
                                                      String[] organArray, List<Long> categoryList, Boolean forceFlag, Boolean inOutManageFlag,
                                                      String materialParam, List<Long> depotList, Integer oId, String number,
                                                      Long creator, String remark, String column, String order, Integer offset, Integer rows) throws Exception{
        List<DepotHeadVo4InDetail> list = null;
        try{
            list =depotHeadMapperEx.findInOutDetail(beginTime, endTime, type, creatorArray, organArray, categoryList, forceFlag, inOutManageFlag,
                    materialParam, depotList, oId, number, creator, remark, column, order, offset, rows);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public void checkInOutDetailReportPermission(String type) throws Exception {
        User currentUser = userService.getCurrentUser();
        Long userId = currentUser == null ? null : currentUser.getId();
        String reportUrl = "出库".equals(type) ? "/report/out_detail" : "/report/in_detail";
        if(!userService.hasFunctionPermission(userId, reportUrl)) {
            if("出库".equals(type)) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_OUT_DETAIL_REPORT_PERMISSION_CODE,
                        ExceptionConstants.DEPOT_HEAD_OUT_DETAIL_REPORT_PERMISSION_MSG);
            }
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_IN_DETAIL_REPORT_PERMISSION_CODE,
                    ExceptionConstants.DEPOT_HEAD_IN_DETAIL_REPORT_PERMISSION_MSG);
        }
    }

    public void checkAllocationDetailReportPermission() throws Exception {
        User currentUser = userService.getCurrentUser();
        Long userId = currentUser == null ? null : currentUser.getId();
        if (!userService.hasFunctionPermission(userId, "/report/allocation_detail")) {
            throw new BusinessRunTimeException(
                    ExceptionConstants.DEPOT_HEAD_ALLOCATION_DETAIL_REPORT_PERMISSION_CODE,
                    ExceptionConstants.DEPOT_HEAD_ALLOCATION_DETAIL_REPORT_PERMISSION_MSG);
        }
    }

    public int findInOutDetailCount(String beginTime, String endTime, String type, String[] creatorArray,
                                    String[] organArray, List<Long> categoryList, Boolean forceFlag, Boolean inOutManageFlag, String materialParam, List<Long> depotList, Integer oId, String number,
                                    Long creator, String remark) throws Exception{
        int result = 0;
        try{
            result =depotHeadMapperEx.findInOutDetailCount(beginTime, endTime, type, creatorArray, organArray, categoryList, forceFlag, inOutManageFlag,
                    materialParam, depotList, oId, number, creator, remark);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    public DepotHeadVo4InDetail findInOutDetailStatistic(String beginTime, String endTime, String type, String [] creatorArray,
                                                      String [] organArray, List<Long> categoryList, Boolean forceFlag, Boolean inOutManageFlag,
                                                      String materialParam, List<Long> depotList, Integer oId, String number,
                                                      Long creator, String remark) throws Exception{
        DepotHeadVo4InDetail item = new DepotHeadVo4InDetail();
        try{
            List<DepotHeadVo4InDetail> list =depotHeadMapperEx.findInOutDetailStatistic(beginTime, endTime, type, creatorArray, organArray, categoryList, forceFlag, inOutManageFlag,
                    materialParam, depotList, oId, number, creator, remark);
            if(list.size()>0) {
                item.setOperNumber(list.get(0).getOperNumber());
                item.setAllPrice(list.get(0).getAllPrice());
            }
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return item;
    }

    public List<DepotHeadVo4InOutMCount> findInOutMaterialCount(String beginTime, String endTime, String type, List<Long> categoryList,
                                                                Boolean forceFlag, Boolean inOutManageFlag, String materialParam,
                                                                List<Long> depotList, Long organizationId, Integer oId, String column, String order,
                                                                Integer offset, Integer rows)throws Exception {
        List<DepotHeadVo4InOutMCount> list = null;
        try{
            String [] creatorArray = getCreatorArray();
            if(creatorArray == null && organizationId != null) {
                creatorArray = getCreatorArrayByOrg(organizationId);
            }
            String subType = "出库".equals(type)? "销售" : "";
            String [] organArray = getOrganArray(subType, "");
            list =depotHeadMapperEx.findInOutMaterialCount(beginTime, endTime, type, categoryList, forceFlag, inOutManageFlag, materialParam, depotList, oId,
                    creatorArray, organArray, column, order, offset, rows);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public int findInOutMaterialCountTotal(String beginTime, String endTime, String type, List<Long> categoryList,
                                           Boolean forceFlag, Boolean inOutManageFlag, String materialParam,
                                           List<Long> depotList, Long organizationId, Integer oId)throws Exception {
        int result = 0;
        try{
            String [] creatorArray = getCreatorArray();
            if(creatorArray == null && organizationId != null) {
                creatorArray = getCreatorArrayByOrg(organizationId);
            }
            String subType = "出库".equals(type)? "销售" : "";
            String [] organArray = getOrganArray(subType, "");
            result =depotHeadMapperEx.findInOutMaterialCountTotal(beginTime, endTime, type, categoryList, forceFlag, inOutManageFlag, materialParam, depotList, oId,
                    creatorArray, organArray);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    public DepotHeadVo4InOutMCount findInOutMaterialCountStatistic(String beginTime, String endTime, String type, List<Long> categoryList,
                                                                Boolean forceFlag, Boolean inOutManageFlag, String materialParam,
                                                                List<Long> depotList, Long organizationId, Integer oId) throws Exception {
        DepotHeadVo4InOutMCount item = new DepotHeadVo4InOutMCount();
        try{
            String [] creatorArray = getCreatorArray();
            if(creatorArray == null && organizationId != null) {
                creatorArray = getCreatorArrayByOrg(organizationId);
            }
            String subType = "出库".equals(type)? "销售" : "";
            String [] organArray = getOrganArray(subType, "");
            List<DepotHeadVo4InOutMCount> list = depotHeadMapperEx.findInOutMaterialCountStatistic(beginTime, endTime, type, categoryList,
                    forceFlag, inOutManageFlag, materialParam, depotList, oId, creatorArray, organArray);
            if(list.size()>0) {
                item.setNumSum(list.get(0).getNumSum());
                item.setPriceSum(list.get(0).getPriceSum());
            }
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return item;
    }

    public List<DepotHeadVo4InDetail> findAllocationDetail(String beginTime, String endTime, String subType, String number,
                            String [] creatorArray, List<Long> categoryList, Boolean forceFlag, String materialParam, List<Long> depotList, List<Long> depotFList,
                            String remark, String column, String order, Integer offset, Integer rows) throws Exception{
        List<DepotHeadVo4InDetail> list = null;
        try{
            list =depotHeadMapperEx.findAllocationDetail(beginTime, endTime, subType, number, creatorArray, categoryList, forceFlag,
                    materialParam, depotList, depotFList, remark, column, order, offset, rows);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public int findAllocationDetailCount(String beginTime, String endTime, String subType, String number,
                            String [] creatorArray, List<Long> categoryList, Boolean forceFlag, String materialParam, List<Long> depotList,  List<Long> depotFList,
                            String remark) throws Exception{
        int result = 0;
        try{
            result =depotHeadMapperEx.findAllocationDetailCount(beginTime, endTime, subType, number, creatorArray, categoryList, forceFlag,
                    materialParam, depotList, depotFList, remark);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    public DepotHeadVo4InDetail findAllocationStatistic(String beginTime, String endTime, String subType, String number,
                                                        String [] creatorArray, List<Long> categoryList, Boolean forceFlag, String materialParam, List<Long> depotList, List<Long> depotFList,
                                                        String remark) throws Exception{
        DepotHeadVo4InDetail item = new DepotHeadVo4InDetail();
        try{
            List<DepotHeadVo4InDetail> list =depotHeadMapperEx.findAllocationStatistic(beginTime, endTime, subType, number, creatorArray, categoryList, forceFlag,
                    materialParam, depotList, depotFList, remark);
            if(list.size()>0) {
                item.setOperNumber(list.get(0).getOperNumber());
                item.setAllPrice(list.get(0).getAllPrice());
            }
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return item;
    }

    public List<DepotHeadVo4StatementAccount> getStatementAccount(String beginTime, String endTime, Integer organId, String [] organArray,
                                                                  Integer hasDebt, String supplierType, String type, String subType, String typeBack,
                                                                  String subTypeBack, String billType, Integer offset, Integer rows) {
        List<DepotHeadVo4StatementAccount> list = null;
        try{
            list = depotHeadMapperEx.getStatementAccount(beginTime, endTime, organId, organArray, hasDebt, supplierType, type, subType,typeBack, subTypeBack, billType, offset, rows);
        } catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public int getStatementAccountCount(String beginTime, String endTime, Integer organId, String [] organArray,
                                        Integer hasDebt, String supplierType, String type, String subType, String typeBack, String subTypeBack, String billType) {
        int result = 0;
        try{
            result = depotHeadMapperEx.getStatementAccountCount(beginTime, endTime, organId, organArray, hasDebt, supplierType, type, subType,typeBack, subTypeBack, billType);
        } catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    public List<DepotHeadVo4StatementAccount> getStatementAccountTotalPay(String beginTime, String endTime, Integer organId, String [] organArray,
                                                                          Integer hasDebt, String supplierType, String type, String subType,
                                                                          String typeBack, String subTypeBack, String billType) {
        List<DepotHeadVo4StatementAccount> list = null;
        try{
            list = depotHeadMapperEx.getStatementAccountTotalPay(beginTime, endTime, organId, organArray, hasDebt, supplierType, type, subType,typeBack, subTypeBack, billType);
        } catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public int getNeedCount(String supplierType) throws Exception {
        String type = "";
        String subType = "";
        String typeBack = "";
        String subTypeBack = "";
        String billType = "";
        if (("供应商").equals(supplierType)) {
            type = "入库";
            subType = "采购";
            typeBack = "出库";
            subTypeBack = "采购退货";
            billType = "付款";
        } else if (("客户").equals(supplierType)) {
            type = "出库";
            subType = "销售";
            typeBack = "入库";
            subTypeBack = "销售退货";
            billType = "收款";
        }
        String beginTime = Tools.parseDayToTime(Tools.getYearBegin(), BusinessConstants.DAY_FIRST_TIME);
        String endTime = Tools.getCenternTime(new Date());
        String [] organArray = getOrganArray(subType, "");
        return getStatementAccountCount(beginTime, endTime, null, organArray,
                1, supplierType, type, subType,typeBack, subTypeBack, billType);
    }

    public List<DepotHeadVo4List> getDetailByNumber(String number, HttpServletRequest request)throws Exception {
        List<DepotHeadVo4List> resList = new ArrayList<>();
        try{
            Long userId = userService.getUserId(request);
            String priceLimit = userService.getRoleTypeByUserId(userId).getPriceLimit();
            Map<Long,String> personMap = personService.getPersonMap();
            Map<Long,String> accountMap = accountService.getAccountMap();
            List<DepotHeadVo4List> list = depotHeadMapperEx.getDetailByNumber(number);
            if (null != list) {
                List<Long> idList = new ArrayList<>();
                List<String> numberList = new ArrayList<>();
                for (DepotHeadVo4List dh : list) {
                    idList.add(dh.getId());
                    numberList.add(dh.getNumber());
                }
                //通过批量查询去构造map
                Map<String,Integer> billSizeMap = getBillSizeMapByLinkNumberList(numberList);
                Map<Long,String> materialsListMap = findMaterialsListMapByHeaderIdList(idList);
                Map<Long,BigDecimal> materialCountListMap = getMaterialCountListMapByHeaderIdList(idList);
                DepotHeadVo4List dh = list.get(0);
                checkPurchaseBillDataPermission(getDepotHead(dh.getId()));
                String billCategory = getBillCategory(dh.getSubType());
                if(accountMap!=null && StringUtil.isNotEmpty(dh.getAccountIdList()) && StringUtil.isNotEmpty(dh.getAccountMoneyList())) {
                    String accountStr = accountService.getAccountStrByIdAndMoney(accountMap, dh.getAccountIdList(), dh.getAccountMoneyList());
                    dh.setAccountName(accountStr);
                }
                if(dh.getAccountIdList() != null) {
                    String accountidlistStr = dh.getAccountIdList().replace("[", "").replace("]", "").replaceAll("\"", "");
                    dh.setAccountIdList(accountidlistStr);
                }
                if(dh.getAccountMoneyList() != null) {
                    String accountmoneylistStr = dh.getAccountMoneyList().replace("[", "").replace("]", "").replaceAll("\"", "");
                    dh.setAccountMoneyList(accountmoneylistStr);
                }
                if(dh.getChangeAmount() != null) {
                    dh.setChangeAmount(roleService.parseBillPriceByLimit(dh.getChangeAmount().abs(), billCategory, priceLimit, request));
                } else {
                    dh.setChangeAmount(BigDecimal.ZERO);
                }
                if(dh.getTotalPrice() != null) {
                    dh.setTotalPrice(roleService.parseBillPriceByLimit(dh.getTotalPrice().abs(), billCategory, priceLimit, request));
                }
                BigDecimal discountLastMoney = dh.getDiscountLastMoney()!=null?dh.getDiscountLastMoney():BigDecimal.ZERO;
                dh.setDiscountLastMoney(roleService.parseBillPriceByLimit(discountLastMoney, billCategory, priceLimit, request));
                BigDecimal backAmount = dh.getBackAmount()!=null?dh.getBackAmount():BigDecimal.ZERO;
                dh.setBackAmount(roleService.parseBillPriceByLimit(backAmount, billCategory, priceLimit, request));
                if(dh.getDeposit() == null) {
                    dh.setDeposit(BigDecimal.ZERO);
                } else {
                    dh.setDeposit(roleService.parseBillPriceByLimit(dh.getDeposit(), billCategory, priceLimit, request));
                }
                //欠款计算
                BigDecimal otherMoney = dh.getOtherMoney()!=null?dh.getOtherMoney():BigDecimal.ZERO;
                BigDecimal deposit = dh.getDeposit()!=null?dh.getDeposit():BigDecimal.ZERO;
                BigDecimal changeAmount = dh.getChangeAmount()!=null?dh.getChangeAmount():BigDecimal.ZERO;
                BigDecimal debt = discountLastMoney.add(otherMoney).subtract((deposit.add(changeAmount)));
                dh.setDebt(roleService.parseBillPriceByLimit(debt, billCategory, priceLimit, request));
                //是否有退款单
                if(billSizeMap!=null) {
                    Integer billListSize = billSizeMap.get(dh.getNumber());
                    dh.setHasBackFlag(billListSize!=null && billListSize>0);
                }
                if(StringUtil.isNotEmpty(dh.getSalesMan())) {
                    dh.setSalesManStr(personService.getPersonByMapAndIds(personMap,dh.getSalesMan()));
                }
                if(dh.getOperTime() != null) {
                    dh.setOperTimeStr(getCenternTime(dh.getOperTime()));
                }
                //商品信息简述
                if(materialsListMap!=null) {
                    dh.setMaterialsList(materialsListMap.get(dh.getId()));
                }
                //商品总数量
                if(materialCountListMap!=null) {
                    dh.setMaterialCount(materialCountListMap.get(dh.getId()));
                }
                User creatorUser = userService.getUser(dh.getCreator());
                if(creatorUser!=null) {
                    dh.setCreatorName(creatorUser.getUsername());
                }
                resList.add(dh);
            }
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return resList;
    }

    /**
     * 查询除当前单据之外的关联单据列表
     * @param linkNumber
     * @param number
     * @return
     * @throws Exception
     */
    public List<DepotHead> getListByLinkNumberExceptCurrent(String linkNumber, String number, String type)throws Exception {
        DepotHeadExample example = new DepotHeadExample();
        example.createCriteria().andLinkNumberEqualTo(linkNumber).andNumberNotEqualTo(number).andTypeEqualTo(type)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        return depotHeadMapper.selectByExample(example);
    }

    /**
     * 根据已审核的销售出库明细重新计算销售订单的销售进度。
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void recalculateSalesOrderStatus(String orderNumber) throws Exception {
        if (StringUtil.isEmpty(orderNumber)) {
            return;
        }
        DepotHead sourceHead = depotHeadMapperEx.lockDepotHeadByNumber(orderNumber);
        if (!isSalesOrder(sourceHead)) {
            return;
        }
        List<DepotItem> sourceItems = depotItemService.getListByHeaderId(sourceHead.getId());
        List<DepotItemVo4MaterialAndSum> shippedItems = depotItemMapperEx.getAuditedSalesOutboundBasicSum(orderNumber);
        Map<Long, BigDecimal> shippedMap = new HashMap<>();
        if (shippedItems != null) {
            for (DepotItemVo4MaterialAndSum shippedItem : shippedItems) {
                shippedMap.put(shippedItem.getMaterialExtendId(), shippedItem.getOperNumber());
            }
        }
        boolean hasShipment = shippedMap.values().stream()
                .anyMatch(value -> value != null && value.compareTo(BigDecimal.ZERO) > 0);
        boolean completed = hasShipment && sourceItems != null && !sourceItems.isEmpty();
        if (completed) {
            for (DepotItem sourceItem : sourceItems) {
                BigDecimal ordered = sourceItem.getBasicNumber() == null
                        ? sourceItem.getOperNumber() : sourceItem.getBasicNumber();
                BigDecimal shipped = shippedMap.getOrDefault(sourceItem.getId(), BigDecimal.ZERO);
                if (ordered == null || ordered.compareTo(BigDecimal.ZERO) <= 0 || shipped.compareTo(ordered) < 0) {
                    completed = false;
                    break;
                }
            }
        }
        String status = !hasShipment ? BusinessConstants.BILLS_STATUS_AUDIT
                : completed ? BusinessConstants.BILLS_STATUS_SKIPED : BusinessConstants.BILLS_STATUS_SKIPING;
        DepotHead updateHead = new DepotHead();
        updateHead.setId(sourceHead.getId());
        updateHead.setStatus(status);
        depotHeadMapper.updateByPrimaryKeySelective(updateHead);
    }

    /**
     * 查询除当前单据之外的关联单据列表
     * @param linkApply
     * @param number
     * @return
     * @throws Exception
     */
    public List<DepotHead> getListByLinkApplyExceptCurrent(String linkApply, String number, String type)throws Exception {
        DepotHeadExample example = new DepotHeadExample();
        example.createCriteria().andLinkApplyEqualTo(linkApply).andNumberNotEqualTo(number).andTypeEqualTo(type)
                .andSubTypeEqualTo(BusinessConstants.SUB_TYPE_PURCHASE_ORDER)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        return depotHeadMapper.selectByExample(example);
    }

    /**
     * 根据原单号查询关联的单据列表(批量)
     * @param linkNumberList
     * @return
     * @throws Exception
     */
    public List<DepotHead> getBillListByLinkNumberList(List<String> linkNumberList)throws Exception {
        if(linkNumberList!=null && linkNumberList.size()>0) {
            DepotHeadExample example = new DepotHeadExample();
            example.createCriteria().andLinkNumberIn(linkNumberList).andSubTypeLike("退货").andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
            return depotHeadMapper.selectByExample(example);
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * 根据原单号查询关联的单据列表
     * @param linkNumber
     * @return
     * @throws Exception
     */
    public List<DepotHead> getBillListByLinkNumber(String linkNumber)throws Exception {
        DepotHeadExample example = new DepotHeadExample();
        example.createCriteria().andLinkNumberEqualTo(linkNumber).andSubTypeLike("退货").andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<DepotHead> result = depotHeadMapper.selectByExample(example);
        for (DepotHead depotHead : result) {
            checkPurchaseBillDataPermission(depotHead);
        }
        return result;
    }

    /**
     * 新增单据主表及单据子表信息
     * @param beanJson
     * @param rows
     * @param request
     * @throws Exception
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void addDepotHeadAndDetail(String beanJson, String rows,
                                      HttpServletRequest request) throws Exception {
        /**处理单据主表数据*/
        JSONObject headJson = JSONObject.parseObject(beanJson);
        DepotHead depotHead = headJson.toJavaObject(DepotHead.class);
        validateDepotHeadBusinessType(depotHead);
        validatePurchaseInboundSubmittedState(depotHead, null);
        validatePurchaseReturnSubmittedState(depotHead);
        validateSalesSubmittedState(depotHead, null);
        validateOtherSubmittedState(depotHead, null);
        validateTransferSubmittedState(depotHead, null);
        validateAssembleSubmittedState(depotHead, null);
        validateDisassembleSubmittedState(depotHead, null);
        checkBillButtonPermission(depotHead, "1", "新增");
        if (BusinessConstants.BILLS_STATUS_AUDIT.equals(depotHead.getStatus())) {
            checkBillButtonPermission(depotHead, "2", "审核");
        }
        rows = validateAndNormalizeBill(depotHead, headJson, rows, null);
        //判断用户是否已经登录过，登录过不再处理
        User userInfo=userService.getCurrentUser();
        //通过redis去校验重复
        checkExistByRedis(userInfo, depotHead, "add");
        //校验单号是否重复
        if(checkIsBillNumberExist(0L, depotHead.getNumber())>0) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_BILL_NUMBER_EXIST_CODE,
                    String.format(ExceptionConstants.DEPOT_HEAD_BILL_NUMBER_EXIST_MSG));
        }
        //校验是否同时录入关联请购单号和关联订单号
        if(StringUtil.isNotEmpty(depotHead.getLinkNumber()) && StringUtil.isNotEmpty(depotHead.getLinkApply())) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_ITEM_EXIST_REPEAT_NO_FAILED_CODE,
                    String.format(ExceptionConstants.DEPOT_ITEM_EXIST_REPEAT_NO_FAILED_MSG));
        }
        String subType = depotHead.getSubType();
        //结算账户校验
        if("采购".equals(subType) || "采购退货".equals(subType) || "销售".equals(subType) || "销售退货".equals(subType)) {
            if (StringUtil.isEmpty(depotHead.getAccountIdList()) && depotHead.getAccountId() == null) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ACCOUNT_FAILED_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_ACCOUNT_FAILED_MSG));
            }
        }
        depotHead.setCreator(userInfo==null?null:userInfo.getId());
        depotHead.setCreateTime(new Timestamp(System.currentTimeMillis()));
        if(StringUtil.isEmpty(depotHead.getStatus())) {
            depotHead.setStatus(BusinessConstants.BILLS_STATUS_UN_AUDIT);
        }
        depotHead.setPurchaseStatus(BusinessConstants.BILLS_STATUS_UN_AUDIT);
        depotHead.setPayType(depotHead.getPayType()==null?"现付":depotHead.getPayType());
        validatePrepaidBalance(depotHead, null);
        if(StringUtil.isNotEmpty(depotHead.getAccountIdList())){
            depotHead.setAccountIdList(depotHead.getAccountIdList().replace("[", "").replace("]", "").replaceAll("\"", ""));
        }
        if(StringUtil.isNotEmpty(depotHead.getAccountMoneyList())) {
            //校验多账户的结算金额
            String accountMoneyList = depotHead.getAccountMoneyList().replace("[", "").replace("]", "").replaceAll("\"", "");
            BigDecimal sum = StringUtil.getArrSum(accountMoneyList.split(","));
            BigDecimal manyAccountSum = sum.abs();
            if(manyAccountSum.compareTo(depotHead.getChangeAmount().abs())!=0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_MANY_ACCOUNT_FAILED_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_MANY_ACCOUNT_FAILED_MSG));
            }
            depotHead.setAccountMoneyList(accountMoneyList);
        }
        //校验累计扣除订金是否超出订单中的金额
        if(depotHead.getDeposit()!=null && StringUtil.isNotEmpty(depotHead.getLinkNumber())) {
            BigDecimal finishDeposit = depotHeadMapperEx.getFinishDepositByNumberExceptCurrent(depotHead.getLinkNumber(), depotHead.getNumber());
            //订单中的订金金额
            BigDecimal changeAmount = getDepotHead(depotHead.getLinkNumber()).getChangeAmount();
            if(changeAmount!=null) {
                BigDecimal preDeposit = changeAmount.abs();
                if(depotHead.getDeposit().add(finishDeposit).compareTo(preDeposit)>0) {
                    throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_DEPOSIT_OVER_PRE_CODE,
                            String.format(ExceptionConstants.DEPOT_HEAD_DEPOSIT_OVER_PRE_MSG));
                }
            }
        }
        //校验附件的数量
        if(StringUtil.isNotEmpty(depotHead.getFileName())) {
            String[] fileArr = depotHead.getFileName().split(",");
            if(fileArr.length>4) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_FILE_NUM_LIMIT_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_FILE_NUM_LIMIT_MSG, 4));
            }
        }
        depotHeadMapper.insertSelective(depotHead);
        /**入库和出库处理预付款信息*/
        if(isPrepaidRetailBill(depotHead)){
            if(depotHead.getOrganId()!=null) {
                supplierService.updateAdvanceIn(depotHead.getOrganId());
            }
        }
        //根据单据编号查询单据id
        DepotHeadExample dhExample = new DepotHeadExample();
        dhExample.createCriteria().andNumberEqualTo(depotHead.getNumber()).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<DepotHead> list = depotHeadMapper.selectByExample(dhExample);
        if(list!=null) {
            Long headId = list.get(0).getId();
            /**入库和出库处理单据子表信息*/
            depotItemService.saveDetials(rows,headId, "add",request);
            /**更新最终欠款*/
            updateLastDebtByBillId(depotHead.getDebt(), headId);
        }
        String statusStr = depotHead.getStatus().equals("1")?"[审核]":"";
        logService.insertLog("单据",
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_ADD).append(depotHead.getNumber()).append(statusStr).toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
    }

    /**
     * 更新单据主表及单据子表信息
     * @param beanJson
     * @param rows
     * @param request
     * @throws Exception
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void updateDepotHeadAndDetail(String beanJson, String rows,HttpServletRequest request)throws Exception {
        /**更新单据主表信息*/
        JSONObject headJson = JSONObject.parseObject(beanJson);
        DepotHead depotHead = headJson.toJavaObject(DepotHead.class);
        DepotHead previousDepotHead = getDepotHead(depotHead.getId());
        if (previousDepotHead == null) {
            throw new BusinessRunTimeException(ExceptionConstants.DATA_READ_FAIL_CODE,
                    ExceptionConstants.DATA_READ_FAIL_MSG);
        }
        if (!Objects.equals(previousDepotHead.getType(), depotHead.getType())
                || !Objects.equals(previousDepotHead.getSubType(), depotHead.getSubType())) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_BILL_TYPE_CHANGE_CODE,
                    ExceptionConstants.DEPOT_HEAD_BILL_TYPE_CHANGE_MSG);
        }
        validateDepotHeadBusinessType(depotHead);
        //编辑接口只允许处理未审核单据，且采购入库的完成状态只能由后续业务回写。
        if(!BusinessConstants.BILLS_STATUS_UN_AUDIT.equals(previousDepotHead.getStatus())) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_BILL_CANNOT_EDIT_CODE,
                    ExceptionConstants.DEPOT_HEAD_BILL_CANNOT_EDIT_MSG);
        }
        validatePurchaseInboundSubmittedState(depotHead, previousDepotHead);
        validatePurchaseReturnSubmittedState(depotHead);
        validateSalesSubmittedState(depotHead, previousDepotHead);
        validateOtherSubmittedState(depotHead, previousDepotHead);
        validateTransferSubmittedState(depotHead, previousDepotHead);
        validateAssembleSubmittedState(depotHead, previousDepotHead);
        validateDisassembleSubmittedState(depotHead, previousDepotHead);
        checkPurchaseBillDataPermission(previousDepotHead);
        checkPurchaseInboundHasNoReturn(previousDepotHead, "编辑");
        if (isPurchaseInbound(previousDepotHead)
                && !Objects.equals(previousDepotHead.getNumber(), depotHead.getNumber())) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_IN_NUMBER_CHANGE_CODE,
                    ExceptionConstants.DEPOT_HEAD_PURCHASE_IN_NUMBER_CHANGE_MSG);
        }
        if (isPurchaseReturn(previousDepotHead)
                && !Objects.equals(previousDepotHead.getNumber(), depotHead.getNumber())) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_RETURN_LINK_CHANGE_CODE,
                    ExceptionConstants.DEPOT_HEAD_PURCHASE_RETURN_LINK_CHANGE_MSG);
        }
        checkBillButtonPermission(previousDepotHead, "1", "编辑");
        if (BusinessConstants.BILLS_STATUS_AUDIT.equals(depotHead.getStatus())) {
            checkBillButtonPermission(previousDepotHead, "2", "审核");
        }
        rows = validateAndNormalizeBill(depotHead, headJson, rows, previousDepotHead);
        //判断用户是否已经登录过，登录过不再处理
        User userInfo=userService.getCurrentUser();
        //通过redis去校验重复
        checkExistByRedis(userInfo, depotHead, "update");
        //校验单号是否重复
        if(checkIsBillNumberExist(depotHead.getId(), depotHead.getNumber())>0) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_BILL_NUMBER_EXIST_CODE,
                    String.format(ExceptionConstants.DEPOT_HEAD_BILL_NUMBER_EXIST_MSG));
        }
        //校验是否同时录入关联请购单号和关联订单号
        if(StringUtil.isNotEmpty(depotHead.getLinkNumber()) && StringUtil.isNotEmpty(depotHead.getLinkApply())) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_ITEM_EXIST_REPEAT_NO_FAILED_CODE,
                    String.format(ExceptionConstants.DEPOT_ITEM_EXIST_REPEAT_NO_FAILED_MSG));
        }
        String subType = depotHead.getSubType();
        //结算账户校验
        if("采购".equals(subType) || "采购退货".equals(subType) || "销售".equals(subType) || "销售退货".equals(subType)) {
            if (StringUtil.isEmpty(depotHead.getAccountIdList()) && depotHead.getAccountId() == null) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ACCOUNT_FAILED_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_ACCOUNT_FAILED_MSG));
            }
        }
        if(StringUtil.isNotEmpty(depotHead.getAccountIdList())){
            depotHead.setAccountIdList(depotHead.getAccountIdList().replace("[", "").replace("]", "").replaceAll("\"", ""));
        }
        if(StringUtil.isNotEmpty(depotHead.getAccountMoneyList())) {
            //校验多账户的结算金额
            String accountMoneyList = depotHead.getAccountMoneyList().replace("[", "").replace("]", "").replaceAll("\"", "");
            BigDecimal sum = StringUtil.getArrSum(accountMoneyList.split(","));
            BigDecimal manyAccountSum = sum.abs();
            if(manyAccountSum.compareTo(depotHead.getChangeAmount().abs())!=0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_MANY_ACCOUNT_FAILED_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_MANY_ACCOUNT_FAILED_MSG));
            }
            depotHead.setAccountMoneyList(accountMoneyList);
        }
        //校验累计扣除订金是否超出订单中的金额
        if(depotHead.getDeposit()!=null && StringUtil.isNotEmpty(depotHead.getLinkNumber())) {
            BigDecimal finishDeposit = depotHeadMapperEx.getFinishDepositByNumberExceptCurrent(depotHead.getLinkNumber(), depotHead.getNumber());
            //订单中的订金金额
            BigDecimal changeAmount = getDepotHead(depotHead.getLinkNumber()).getChangeAmount();
            if(changeAmount!=null) {
                BigDecimal preDeposit = changeAmount.abs();
                if(depotHead.getDeposit().add(finishDeposit).compareTo(preDeposit)>0) {
                    throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_DEPOSIT_OVER_PRE_CODE,
                            String.format(ExceptionConstants.DEPOT_HEAD_DEPOSIT_OVER_PRE_MSG));
                }
            }
        }
        //校验附件的数量
        if(StringUtil.isNotEmpty(depotHead.getFileName())) {
            String[] fileArr = depotHead.getFileName().split(",");
            if(fileArr.length>4) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_FILE_NUM_LIMIT_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_FILE_NUM_LIMIT_MSG, 4));
            }
        }
        Set<Long> prepaidMemberIds = new TreeSet<>();
        if(isPrepaidRetailBill(previousDepotHead) && previousDepotHead.getOrganId() != null) {
            prepaidMemberIds.add(previousDepotHead.getOrganId());
        }
        if(isPrepaidRetailBill(depotHead) && depotHead.getOrganId() != null) {
            prepaidMemberIds.add(depotHead.getOrganId());
        }
        lockPrepaidMembers(prepaidMemberIds);
        validatePrepaidBalance(depotHead, previousDepotHead);
        // 以下字段由服务端维护，编辑接口不得通过请求体覆盖。
        depotHead.setCreator(previousDepotHead.getCreator());
        depotHead.setCreateTime(previousDepotHead.getCreateTime());
        depotHead.setTenantId(previousDepotHead.getTenantId());
        depotHead.setDeleteFlag(previousDepotHead.getDeleteFlag());
        depotHead.setSource(previousDepotHead.getSource());
        depotHead.setDefaultNumber(previousDepotHead.getDefaultNumber());
        depotHead.setLastDebt(previousDepotHead.getLastDebt());
        depotHeadMapper.updateByPrimaryKeySelective(depotHead);
        //如果存在多账户结算需要将原账户的id置空
        if(StringUtil.isNotEmpty(depotHead.getAccountIdList())) {
            depotHeadMapperEx.setAccountIdToNull(depotHead.getId());
        }
        refreshPrepaidBalanceAfterUpdate(previousDepotHead, depotHead);
        /**入库和出库处理单据子表信息*/
        depotItemService.saveDetials(rows,depotHead.getId(), "update",request);
        /**更新最终欠款*/
        updateLastDebtByBillId(depotHead.getDebt(), depotHead.getId());
        String statusStr = depotHead.getStatus().equals("1")?"[审核]":"";
        logService.insertLog("单据",
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append(depotHead.getNumber()).append(statusStr).toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
    }

    /**
     * 通过redis去校验重复
     * @param userInfo
     * @param depotHead
     */
    private void checkExistByRedis(User userInfo, DepotHead depotHead, String operation) {
        String keyNo = userInfo.getLoginName() + "_" + operation + "_" + depotHead.getNumber();
        String keyValue = redisService.getCacheObject(keyNo);
        if(StringUtil.isNotEmpty(keyValue)) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_SUBMIT_REPEAT_FAILED_CODE,
                    String.format(ExceptionConstants.DEPOT_HEAD_SUBMIT_REPEAT_FAILED_MSG));
        } else {
            redisService.storageKeyWithTime(keyNo, depotHead.getNumber(), 2L);
        }
    }

    private void checkBillButtonPermission(DepotHead depotHead, String buttonCode, String operationName) throws Exception {
        if (depotHead == null) {
            return;
        }
        String url;
        String billName;
        if (BusinessConstants.DEPOTHEAD_TYPE_OUT.equals(depotHead.getType())
                && BusinessConstants.SUB_TYPE_RETAIL.equals(depotHead.getSubType())) {
            url = RETAIL_OUT_URL;
            billName = "零售出库";
        } else if (BusinessConstants.DEPOTHEAD_TYPE_IN.equals(depotHead.getType())
                && BusinessConstants.SUB_TYPE_RETAIL_RETURN.equals(depotHead.getSubType())) {
            url = RETAIL_BACK_URL;
            billName = "零售退货";
        } else if (BusinessConstants.DEPOTHEAD_TYPE_OTHER.equals(depotHead.getType())
                && BusinessConstants.SUB_TYPE_PURCHASE_APPLY.equals(depotHead.getSubType())) {
            url = PURCHASE_APPLY_URL;
            billName = "请购单";
        } else if (BusinessConstants.DEPOTHEAD_TYPE_OTHER.equals(depotHead.getType())
                && BusinessConstants.SUB_TYPE_PURCHASE_ORDER.equals(depotHead.getSubType())) {
            url = PURCHASE_ORDER_URL;
            billName = "采购订单";
        } else if (isPurchaseInbound(depotHead)) {
            url = PURCHASE_IN_URL;
            billName = "采购入库";
        } else if (isPurchaseReturn(depotHead)) {
            url = PURCHASE_RETURN_URL;
            billName = "采购退货";
        } else if (isSalesOrder(depotHead)) {
            url = SALES_ORDER_URL;
            billName = "销售订单";
        } else if (isSalesOutbound(depotHead)) {
            url = SALES_OUT_URL;
            billName = "销售出库";
        } else if (isSalesReturn(depotHead)) {
            url = SALES_RETURN_URL;
            billName = "销售退货";
        } else if (isOtherInbound(depotHead)) {
            url = OTHER_IN_URL;
            billName = "其它入库";
        } else if (isOtherOutbound(depotHead)) {
            url = OTHER_OUT_URL;
            billName = "其它出库";
        } else if (isTransferOutbound(depotHead)) {
            url = TRANSFER_OUT_URL;
            billName = "调拨出库";
        } else if (isAssemble(depotHead)) {
            url = ASSEMBLE_URL;
            billName = "组装单";
        } else if (isDisassemble(depotHead)) {
            url = DISASSEMBLE_URL;
            billName = "拆卸单";
        } else {
            return;
        }
        User currentUser = userService.getCurrentUser();
        Long userId = currentUser == null ? null : currentUser.getId();
        if (!userService.hasButtonPermission(userId, url, buttonCode)) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_RETAIL_PERMISSION_CODE,
                    String.format(ExceptionConstants.DEPOT_HEAD_RETAIL_PERMISSION_MSG, billName, operationName));
        }
    }

    private boolean isPurchaseInbound(DepotHead depotHead) {
        return depotHead != null
                && BusinessConstants.DEPOTHEAD_TYPE_IN.equals(depotHead.getType())
                && BusinessConstants.SUB_TYPE_PURCHASE.equals(depotHead.getSubType());
    }

    private boolean isPurchaseReturn(DepotHead depotHead) {
        return depotHead != null
                && BusinessConstants.DEPOTHEAD_TYPE_OUT.equals(depotHead.getType())
                && BusinessConstants.SUB_TYPE_PURCHASE_RETURN.equals(depotHead.getSubType());
    }

    private boolean isSalesOrder(DepotHead depotHead) {
        return depotHead != null
                && BusinessConstants.DEPOTHEAD_TYPE_OTHER.equals(depotHead.getType())
                && BusinessConstants.SUB_TYPE_SALES_ORDER.equals(depotHead.getSubType());
    }

    private boolean isSalesOutbound(DepotHead depotHead) {
        return depotHead != null
                && BusinessConstants.DEPOTHEAD_TYPE_OUT.equals(depotHead.getType())
                && BusinessConstants.SUB_TYPE_SALES.equals(depotHead.getSubType());
    }

    private boolean isSalesReturn(DepotHead depotHead) {
        return depotHead != null
                && BusinessConstants.DEPOTHEAD_TYPE_IN.equals(depotHead.getType())
                && BusinessConstants.SUB_TYPE_SALES_RETURN.equals(depotHead.getSubType());
    }

    private boolean isOtherInbound(DepotHead depotHead) {
        return depotHead != null
                && BusinessConstants.DEPOTHEAD_TYPE_IN.equals(depotHead.getType())
                && BusinessConstants.SUB_TYPE_OTHER.equals(depotHead.getSubType());
    }

    private boolean isOtherOutbound(DepotHead depotHead) {
        return depotHead != null
                && BusinessConstants.DEPOTHEAD_TYPE_OUT.equals(depotHead.getType())
                && BusinessConstants.SUB_TYPE_OTHER.equals(depotHead.getSubType());
    }

    private boolean isTransferOutbound(DepotHead depotHead) {
        return depotHead != null
                && BusinessConstants.DEPOTHEAD_TYPE_OUT.equals(depotHead.getType())
                && BusinessConstants.SUB_TYPE_TRANSFER.equals(depotHead.getSubType());
    }

    private boolean isAssemble(DepotHead depotHead) {
        return depotHead != null
                && BusinessConstants.DEPOTHEAD_TYPE_OTHER.equals(depotHead.getType())
                && BusinessConstants.SUB_TYPE_ASSEMBLE.equals(depotHead.getSubType());
    }

    private boolean isDisassemble(DepotHead depotHead) {
        return depotHead != null
                && BusinessConstants.DEPOTHEAD_TYPE_OTHER.equals(depotHead.getType())
                && BusinessConstants.SUB_TYPE_DISASSEMBLE.equals(depotHead.getSubType());
    }

    private boolean isCurrentUserAdmin() throws Exception {
        User currentUser = userService.getCurrentUser();
        return currentUser != null && "admin".equals(currentUser.getLoginName());
    }

    /**
     * 采购入库的状态只允许由“保存”或“保存并审核”提交，完成/部分完成状态由后续单据回写。
     */
    private void validatePurchaseInboundSubmittedState(DepotHead depotHead, DepotHead previousDepotHead) {
        if (!isPurchaseInbound(depotHead)) {
            return;
        }
        String status = StringUtil.isEmpty(depotHead.getStatus())
                ? BusinessConstants.BILLS_STATUS_UN_AUDIT : depotHead.getStatus();
        if (!BusinessConstants.BILLS_STATUS_UN_AUDIT.equals(status)
                && !BusinessConstants.BILLS_STATUS_AUDIT.equals(status)) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_IN_STATUS_CODE,
                    ExceptionConstants.DEPOT_HEAD_PURCHASE_IN_STATUS_MSG);
        }
        depotHead.setStatus(status);
        depotHead.setPurchaseStatus(previousDepotHead == null
                ? BusinessConstants.PURCHASE_STATUS_UN_AUDIT : previousDepotHead.getPurchaseStatus());
    }

    private void validatePurchaseReturnSubmittedState(DepotHead depotHead) {
        if (!isPurchaseReturn(depotHead)) {
            return;
        }
        String status = StringUtil.isEmpty(depotHead.getStatus())
                ? BusinessConstants.BILLS_STATUS_UN_AUDIT : depotHead.getStatus();
        if (!BusinessConstants.BILLS_STATUS_UN_AUDIT.equals(status)
                && !BusinessConstants.BILLS_STATUS_AUDIT.equals(status)) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_RETURN_STATUS_CODE,
                    ExceptionConstants.DEPOT_HEAD_PURCHASE_RETURN_STATUS_MSG);
        }
        depotHead.setStatus(status);
        //采购退货的实际出库进度使用status维护，purchaseStatus不允许由客户端写入。
        depotHead.setPurchaseStatus(BusinessConstants.PURCHASE_STATUS_UN_AUDIT);
    }

    private void validateSalesSubmittedState(DepotHead depotHead, DepotHead previousDepotHead) {
        if (!isSalesOrder(depotHead) && !isSalesOutbound(depotHead) && !isSalesReturn(depotHead)) {
            return;
        }
        String status = StringUtil.isEmpty(depotHead.getStatus())
                ? BusinessConstants.BILLS_STATUS_UN_AUDIT : depotHead.getStatus();
        if (!BusinessConstants.BILLS_STATUS_UN_AUDIT.equals(status)
                && !BusinessConstants.BILLS_STATUS_AUDIT.equals(status)) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_SALES_STATUS_CODE,
                    ExceptionConstants.DEPOT_HEAD_SALES_STATUS_MSG);
        }
        if (previousDepotHead != null) {
            boolean identityChanged = !Objects.equals(previousDepotHead.getType(), depotHead.getType())
                    || !Objects.equals(previousDepotHead.getSubType(), depotHead.getSubType())
                    || !Objects.equals(previousDepotHead.getNumber(), depotHead.getNumber());
            boolean linkChanged = (isSalesOutbound(previousDepotHead) || isSalesReturn(previousDepotHead))
                    && !Objects.equals(normalizeLink(previousDepotHead.getLinkNumber()),
                    normalizeLink(depotHead.getLinkNumber()));
            if (identityChanged || linkChanged) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_SALES_LINK_CHANGE_CODE,
                        ExceptionConstants.DEPOT_HEAD_SALES_LINK_CHANGE_MSG);
            }
            depotHead.setPurchaseStatus(previousDepotHead.getPurchaseStatus());
        } else {
            depotHead.setPurchaseStatus(BusinessConstants.PURCHASE_STATUS_UN_AUDIT);
        }
        depotHead.setStatus(status);
    }

    private void validateOtherSubmittedState(DepotHead depotHead, DepotHead previousDepotHead) {
        if (!isOtherInbound(depotHead) && !isOtherOutbound(depotHead)) {
            return;
        }
        String status = StringUtil.isEmpty(depotHead.getStatus())
                ? BusinessConstants.BILLS_STATUS_UN_AUDIT : depotHead.getStatus();
        if (!BusinessConstants.BILLS_STATUS_UN_AUDIT.equals(status)
                && !BusinessConstants.BILLS_STATUS_AUDIT.equals(status)) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_OTHER_STATUS_CODE,
                    ExceptionConstants.DEPOT_HEAD_OTHER_STATUS_MSG);
        }
        if (previousDepotHead != null) {
            boolean identityChanged = !Objects.equals(previousDepotHead.getNumber(), depotHead.getNumber())
                    || !Objects.equals(normalizeLink(previousDepotHead.getLinkNumber()),
                    normalizeLink(depotHead.getLinkNumber()));
            if (identityChanged) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_OTHER_LINK_CHANGE_CODE,
                        ExceptionConstants.DEPOT_HEAD_OTHER_LINK_CHANGE_MSG);
            }
            depotHead.setPurchaseStatus(previousDepotHead.getPurchaseStatus());
        } else {
            depotHead.setPurchaseStatus(BusinessConstants.PURCHASE_STATUS_UN_AUDIT);
        }
        depotHead.setStatus(status);
    }

    private void validateTransferSubmittedState(DepotHead depotHead, DepotHead previousDepotHead) {
        if (!isTransferOutbound(depotHead)) {
            return;
        }
        String status = StringUtil.isEmpty(depotHead.getStatus())
                ? BusinessConstants.BILLS_STATUS_UN_AUDIT : depotHead.getStatus();
        if (!BusinessConstants.BILLS_STATUS_UN_AUDIT.equals(status)
                && !BusinessConstants.BILLS_STATUS_AUDIT.equals(status)) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_TRANSFER_STATUS_CODE,
                    ExceptionConstants.DEPOT_HEAD_TRANSFER_STATUS_MSG);
        }
        if (previousDepotHead != null
                && !Objects.equals(previousDepotHead.getNumber(), depotHead.getNumber())) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_TRANSFER_NUMBER_CHANGE_CODE,
                    ExceptionConstants.DEPOT_HEAD_TRANSFER_NUMBER_CHANGE_MSG);
        }
        depotHead.setStatus(status);
        depotHead.setPurchaseStatus(previousDepotHead == null
                ? BusinessConstants.PURCHASE_STATUS_UN_AUDIT : previousDepotHead.getPurchaseStatus());
    }

    private void validateAssembleSubmittedState(DepotHead depotHead, DepotHead previousDepotHead) {
        if (!isAssemble(depotHead)) {
            return;
        }
        String status = StringUtil.isEmpty(depotHead.getStatus())
                ? BusinessConstants.BILLS_STATUS_UN_AUDIT : depotHead.getStatus();
        if (!BusinessConstants.BILLS_STATUS_UN_AUDIT.equals(status)
                && !BusinessConstants.BILLS_STATUS_AUDIT.equals(status)) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ASSEMBLE_STATUS_CODE,
                    ExceptionConstants.DEPOT_HEAD_ASSEMBLE_STATUS_MSG);
        }
        if (previousDepotHead != null
                && !Objects.equals(previousDepotHead.getNumber(), depotHead.getNumber())) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ASSEMBLE_NUMBER_CHANGE_CODE,
                    ExceptionConstants.DEPOT_HEAD_ASSEMBLE_NUMBER_CHANGE_MSG);
        }
        depotHead.setStatus(status);
        depotHead.setPurchaseStatus(previousDepotHead == null
                ? BusinessConstants.PURCHASE_STATUS_UN_AUDIT : previousDepotHead.getPurchaseStatus());
    }

    private void validateDisassembleSubmittedState(DepotHead depotHead, DepotHead previousDepotHead) {
        if (!isDisassemble(depotHead)) {
            return;
        }
        String status = StringUtil.isEmpty(depotHead.getStatus())
                ? BusinessConstants.BILLS_STATUS_UN_AUDIT : depotHead.getStatus();
        if (!BusinessConstants.BILLS_STATUS_UN_AUDIT.equals(status)
                && !BusinessConstants.BILLS_STATUS_AUDIT.equals(status)) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_STATUS_CODE,
                    ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_STATUS_MSG);
        }
        if (previousDepotHead != null
                && !Objects.equals(previousDepotHead.getNumber(), depotHead.getNumber())) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_NUMBER_CHANGE_CODE,
                    ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_NUMBER_CHANGE_MSG);
        }
        depotHead.setStatus(status);
        depotHead.setPurchaseStatus(previousDepotHead == null
                ? BusinessConstants.PURCHASE_STATUS_UN_AUDIT : previousDepotHead.getPurchaseStatus());
    }

    /**
     * 对直接按ID读取或操作的采购、销售和其它出入库单据补充操作员、往来单位和仓库数据权限。
     */
    public void checkPurchaseBillDataPermission(DepotHead depotHead) throws Exception {
        boolean purchaseInbound = isPurchaseInbound(depotHead);
        boolean purchaseReturn = isPurchaseReturn(depotHead);
        boolean salesOrder = isSalesOrder(depotHead);
        boolean salesOutbound = isSalesOutbound(depotHead);
        boolean salesReturn = isSalesReturn(depotHead);
        boolean otherStockBill = isOtherInbound(depotHead) || isOtherOutbound(depotHead);
        boolean transferOutbound = isTransferOutbound(depotHead);
        boolean assemble = isAssemble(depotHead);
        boolean disassemble = isDisassemble(depotHead);
        if (!purchaseInbound && !purchaseReturn && !salesOrder && !salesOutbound && !salesReturn
                && !otherStockBill && !transferOutbound && !assemble && !disassemble) {
            return;
        }
        if (isCurrentUserAdmin()) {
            return;
        }
        String[] creatorArray = getCreatorArray();
        if (creatorArray != null && (depotHead.getCreator() == null
                || Arrays.stream(creatorArray).noneMatch(depotHead.getCreator().toString()::equals))) {
            if (transferOutbound || assemble || disassemble) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_DATA_PERMISSION_CODE,
                        ExceptionConstants.DEPOT_DATA_PERMISSION_MSG);
            }
            throwBillDataPermissionException(purchaseReturn, salesOrder || salesOutbound || salesReturn, otherStockBill);
        }
        if (salesOrder || salesOutbound || salesReturn) {
            checkSalesCustomerPermission(depotHead.getOrganId());
            if (salesOrder) {
                return;
            }
        }
        JSONArray depotArray = depotService.findDepotByCurrentUser();
        Set<Long> allowedDepotIds = new HashSet<>();
        for (Object depotObject : depotArray) {
            allowedDepotIds.add(JSONObject.parseObject(depotObject.toString()).getLong("id"));
        }
        List<DepotItem> detailList = depotItemService.getListByHeaderId(depotHead.getId());
        for (DepotItem depotItem : detailList) {
            boolean sourceDepotDenied = depotItem.getDepotId() == null
                    || !allowedDepotIds.contains(depotItem.getDepotId());
            boolean targetDepotDenied = transferOutbound && (depotItem.getAnotherDepotId() == null
                    || !allowedDepotIds.contains(depotItem.getAnotherDepotId()));
            if (sourceDepotDenied || targetDepotDenied) {
                if (transferOutbound || assemble || disassemble) {
                    throw new BusinessRunTimeException(ExceptionConstants.DEPOT_DATA_PERMISSION_CODE,
                            ExceptionConstants.DEPOT_DATA_PERMISSION_MSG);
                }
                throwBillDataPermissionException(purchaseReturn, salesOutbound || salesReturn, otherStockBill);
            }
        }
    }

    private void throwBillDataPermissionException(boolean purchaseReturn, boolean salesBill, boolean otherStockBill) {
        if (otherStockBill) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_OTHER_DATA_PERMISSION_CODE,
                    ExceptionConstants.DEPOT_HEAD_OTHER_DATA_PERMISSION_MSG);
        }
        if (salesBill) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_SALES_DATA_PERMISSION_CODE,
                    ExceptionConstants.DEPOT_HEAD_SALES_DATA_PERMISSION_MSG);
        }
        if (purchaseReturn) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_RETURN_DATA_PERMISSION_CODE,
                    ExceptionConstants.DEPOT_HEAD_PURCHASE_RETURN_DATA_PERMISSION_MSG);
        }
        throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_IN_DATA_PERMISSION_CODE,
                ExceptionConstants.DEPOT_HEAD_PURCHASE_IN_DATA_PERMISSION_MSG);
    }

    private void checkPurchaseInboundHasNoReturn(DepotHead depotHead, String operationName) {
        if (!isPurchaseInbound(depotHead) || StringUtil.isEmpty(depotHead.getNumber())) {
            return;
        }
        DepotHeadExample example = new DepotHeadExample();
        example.createCriteria().andLinkNumberEqualTo(depotHead.getNumber())
                .andTypeEqualTo(BusinessConstants.DEPOTHEAD_TYPE_OUT)
                .andSubTypeEqualTo(BusinessConstants.SUB_TYPE_PURCHASE_RETURN)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        if (!depotHeadMapper.selectByExample(example).isEmpty()) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_IN_HAS_RETURN_CODE,
                    String.format(ExceptionConstants.DEPOT_HEAD_PURCHASE_IN_HAS_RETURN_MSG, operationName));
        }
    }

    private void checkSalesOrderHasNoOutbound(DepotHead depotHead, String operationName) {
        if (!isSalesOrder(depotHead) || StringUtil.isEmpty(depotHead.getNumber())) {
            return;
        }
        DepotHeadExample example = new DepotHeadExample();
        example.createCriteria().andLinkNumberEqualTo(depotHead.getNumber())
                .andTypeEqualTo(BusinessConstants.DEPOTHEAD_TYPE_OUT)
                .andSubTypeEqualTo(BusinessConstants.SUB_TYPE_SALES)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        if (!depotHeadMapper.selectByExample(example).isEmpty()) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_SALES_ORDER_HAS_OUTBOUND_CODE,
                    String.format(ExceptionConstants.DEPOT_HEAD_SALES_ORDER_HAS_OUTBOUND_MSG, operationName));
        }
    }

    private void checkSalesOutboundHasNoDownstream(DepotHead depotHead, String operationName) {
        if (!isSalesOutbound(depotHead) || depotHead.getId() == null) {
            return;
        }
        DepotHeadExample returnExample = new DepotHeadExample();
        returnExample.createCriteria().andLinkNumberEqualTo(depotHead.getNumber())
                .andTypeEqualTo(BusinessConstants.DEPOTHEAD_TYPE_IN)
                .andSubTypeEqualTo(BusinessConstants.SUB_TYPE_SALES_RETURN)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        if (!depotHeadMapper.selectByExample(returnExample).isEmpty()) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_SALES_OUT_HAS_RETURN_CODE,
                    String.format(ExceptionConstants.DEPOT_HEAD_SALES_OUT_HAS_RETURN_MSG, operationName));
        }
        if (!accountHeadService.getFinancialBillNoByBillId(depotHead.getId()).isEmpty()) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_SALES_OUT_HAS_FINANCIAL_CODE,
                    String.format(ExceptionConstants.DEPOT_HEAD_SALES_OUT_HAS_FINANCIAL_MSG, operationName));
        }
    }

    private void checkSalesReturnHasNoFinancial(DepotHead depotHead, String operationName) {
        if (!isSalesReturn(depotHead) || depotHead.getId() == null) {
            return;
        }
        if (!accountHeadService.getFinancialBillNoByBillId(depotHead.getId()).isEmpty()) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_SALES_RETURN_HAS_FINANCIAL_CODE,
                    String.format(ExceptionConstants.DEPOT_HEAD_SALES_RETURN_HAS_FINANCIAL_MSG, operationName));
        }
    }

    private void validateDepotHeadBusinessType(DepotHead depotHead) {
        if (depotHead == null) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_BILL_TYPE_INVALID_CODE,
                    ExceptionConstants.DEPOT_HEAD_BILL_TYPE_INVALID_MSG);
        }
        String type = depotHead.getType();
        String subType = depotHead.getSubType();
        boolean valid = false;
        if (BusinessConstants.DEPOTHEAD_TYPE_IN.equals(type)) {
            valid = BusinessConstants.SUB_TYPE_PURCHASE.equals(subType)
                    || BusinessConstants.SUB_TYPE_SALES_RETURN.equals(subType)
                    || BusinessConstants.SUB_TYPE_RETAIL_RETURN.equals(subType)
                    || BusinessConstants.SUB_TYPE_OTHER.equals(subType);
        } else if (BusinessConstants.DEPOTHEAD_TYPE_OUT.equals(type)) {
            valid = BusinessConstants.SUB_TYPE_SALES.equals(subType)
                    || BusinessConstants.SUB_TYPE_PURCHASE_RETURN.equals(subType)
                    || BusinessConstants.SUB_TYPE_RETAIL.equals(subType)
                    || BusinessConstants.SUB_TYPE_OTHER.equals(subType)
                    || BusinessConstants.SUB_TYPE_TRANSFER.equals(subType);
        } else if (BusinessConstants.DEPOTHEAD_TYPE_OTHER.equals(type)) {
            valid = BusinessConstants.SUB_TYPE_PURCHASE_APPLY.equals(subType)
                    || BusinessConstants.SUB_TYPE_PURCHASE_ORDER.equals(subType)
                    || BusinessConstants.SUB_TYPE_SALES_ORDER.equals(subType)
                    || BusinessConstants.SUB_TYPE_ASSEMBLE.equals(subType)
                    || BusinessConstants.SUB_TYPE_DISASSEMBLE.equals(subType);
        }
        if (!valid) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_BILL_TYPE_INVALID_CODE,
                    ExceptionConstants.DEPOT_HEAD_BILL_TYPE_INVALID_MSG);
        }
    }

    private String validateAndNormalizeBill(DepotHead depotHead, JSONObject headJson, String rows,
                                             DepotHead previousDepotHead) throws Exception {
        if (BusinessConstants.DEPOTHEAD_TYPE_OTHER.equals(depotHead.getType())
                && BusinessConstants.SUB_TYPE_PURCHASE_APPLY.equals(depotHead.getSubType())) {
            return validateAndNormalizePurchaseApply(depotHead, rows);
        }
        if (BusinessConstants.DEPOTHEAD_TYPE_OTHER.equals(depotHead.getType())
                && BusinessConstants.SUB_TYPE_PURCHASE_ORDER.equals(depotHead.getSubType())) {
            validatePurchaseLinkImmutable(depotHead, previousDepotHead);
            validatePurchaseSupplier(depotHead);
            if (StringUtil.isNotEmpty(depotHead.getLinkApply())) {
                rows = validateAndNormalizePurchaseOrderFromApply(depotHead, rows, previousDepotHead);
            } else if (StringUtil.isNotEmpty(depotHead.getLinkNumber())) {
                rows = validateAndNormalizePurchaseOrderFromSales(depotHead, rows, previousDepotHead);
            } else {
                rows = clearPurchaseRowLinks(rows);
            }
            return normalizePurchaseFinancialFields(depotHead, rows, true);
        }
        if (BusinessConstants.DEPOTHEAD_TYPE_IN.equals(depotHead.getType())
                && BusinessConstants.SUB_TYPE_PURCHASE.equals(depotHead.getSubType())) {
            validatePurchaseInboundLinkImmutable(depotHead, previousDepotHead);
            validatePurchaseSupplier(depotHead);
            if (StringUtil.isNotEmpty(depotHead.getLinkNumber())) {
                rows = validateAndNormalizePurchaseInbound(depotHead, rows, previousDepotHead);
            } else {
                rows = clearPurchaseRowLinks(rows);
            }
            return normalizePurchaseFinancialFields(depotHead, rows, false);
        }
        if (isPurchaseReturn(depotHead)) {
            validatePurchaseReturnLinkImmutable(depotHead, previousDepotHead);
            validatePurchaseSupplier(depotHead);
            if (StringUtil.isEmpty(depotHead.getLinkNumber())) {
                rows = clearPurchaseRowLinks(rows);
            }
            return validateAndNormalizePurchaseReturn(depotHead, rows, previousDepotHead);
        }
        if (BusinessConstants.DEPOTHEAD_TYPE_OUT.equals(depotHead.getType())
                && BusinessConstants.SUB_TYPE_RETAIL.equals(depotHead.getSubType())) {
            return validateAndNormalizeRetailOut(depotHead, headJson, rows);
        }
        if (BusinessConstants.DEPOTHEAD_TYPE_IN.equals(depotHead.getType())
                && BusinessConstants.SUB_TYPE_RETAIL_RETURN.equals(depotHead.getSubType())) {
            return validateAndNormalizeRetailReturn(depotHead, headJson, rows, previousDepotHead);
        }
        if (isSalesOrder(depotHead)) {
            validateSalesCustomer(depotHead);
            depotHead.setLinkNumber(null);
            depotHead.setLinkApply(null);
            rows = normalizeSalesFinancialFields(depotHead, rows, true);
            validateSalesSettlementAccounts(depotHead, false);
            return rows;
        }
        if (isSalesOutbound(depotHead)) {
            validateSalesCustomer(depotHead);
            if (StringUtil.isNotEmpty(depotHead.getLinkNumber())) {
                rows = validateAndNormalizeSalesOutboundSource(depotHead, rows, previousDepotHead);
            } else {
                rows = clearSalesRowLinks(rows);
            }
            rows = normalizeSalesFinancialFields(depotHead, rows, false);
            validateSalesSettlementAccounts(depotHead, true);
            return rows;
        }
        if (isSalesReturn(depotHead)) {
            validateSalesCustomer(depotHead);
            rows = validateAndNormalizeSalesReturn(depotHead, rows, previousDepotHead);
            validateSalesReturnSettlementAccounts(depotHead, true);
            return rows;
        }
        if (isOtherInbound(depotHead)) {
            depotHead.setLinkApply(null);
            if (StringUtil.isNotEmpty(depotHead.getLinkNumber())) {
                rows = validateAndNormalizeOtherInboundSource(depotHead, rows, previousDepotHead);
            } else {
                depotHead.setLinkNumber(null);
                rows = clearOtherInboundRowLinks(rows);
            }
            return normalizeOtherInboundFinancialFields(depotHead, rows);
        }
        if (isOtherOutbound(depotHead)) {
            depotHead.setLinkApply(null);
            if (StringUtil.isNotEmpty(depotHead.getLinkNumber())) {
                rows = validateAndNormalizeOtherOutboundSource(depotHead, rows, previousDepotHead);
            } else {
                depotHead.setLinkNumber(null);
                rows = clearOtherOutboundRowLinks(rows);
            }
            return normalizeOtherOutboundFinancialFields(depotHead, rows);
        }
        if (isTransferOutbound(depotHead)) {
            return validateAndNormalizeTransferOutbound(depotHead, rows);
        }
        if (isAssemble(depotHead)) {
            return validateAndNormalizeAssemble(depotHead, rows);
        }
        if (isDisassemble(depotHead)) {
            return validateAndNormalizeDisassemble(depotHead, rows);
        }
        return rows;
    }

    private String validateAndNormalizeAssemble(DepotHead depotHead, String rows) throws Exception {
        JSONArray detailArray = JSONArray.parseArray(rows);
        if (detailArray == null || detailArray.size() < 2) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ASSEMBLE_STRUCTURE_CODE,
                    ExceptionConstants.DEPOT_HEAD_ASSEMBLE_STRUCTURE_MSG);
        }
        String combinationBarCode = null;
        Set<String> componentBarCodes = new HashSet<>();
        for (int index = 0; index < detailArray.size(); index++) {
            JSONObject detail = JSONObject.parseObject(detailArray.get(index).toString());
            String expectedMaterialType = index == 0 ? "组合件" : "普通子件";
            if (!expectedMaterialType.equals(detail.getString("mType"))) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ASSEMBLE_STRUCTURE_CODE,
                        ExceptionConstants.DEPOT_HEAD_ASSEMBLE_STRUCTURE_MSG);
            }
            String barCode = detail.getString("barCode");
            if (index == 0) {
                combinationBarCode = barCode;
            } else {
                componentBarCodes.add(barCode);
            }
            Long depotId = detail.getLong("depotId");
            if (depotId == null) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_DEPOT_FAILED_CODE,
                        ExceptionConstants.DEPOT_HEAD_DEPOT_FAILED_MSG);
            }
            depotService.parseDepotList(depotId);
            BigDecimal quantity = detail.getBigDecimal("operNumber");
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_MSG, barCode));
            }
            BigDecimal unitPrice = detail.getBigDecimal("unitPrice");
            unitPrice = unitPrice == null ? BigDecimal.ZERO : unitPrice;
            if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ASSEMBLE_AMOUNT_CODE,
                        ExceptionConstants.DEPOT_HEAD_ASSEMBLE_AMOUNT_MSG);
            }
            BigDecimal allPrice = quantity.multiply(unitPrice).setScale(2, BigDecimal.ROUND_HALF_UP);
            detail.put("unitPrice", unitPrice);
            detail.put("allPrice", allPrice);
            detail.put("taxRate", BigDecimal.ZERO);
            detail.put("taxMoney", BigDecimal.ZERO);
            detail.put("taxLastMoney", allPrice);
            detail.put("taxUnitPrice", unitPrice.setScale(4, BigDecimal.ROUND_HALF_UP));
            detail.remove("linkId");
            detail.remove("preNumber");
            detail.remove("finishNumber");
            detail.remove("snList");
            detail.remove("batchNumber");
            detail.remove("expirationDate");
            detailArray.set(index, detail);
        }
        if (StringUtil.isNotEmpty(combinationBarCode) && componentBarCodes.contains(combinationBarCode)) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ASSEMBLE_SAME_MATERIAL_CODE,
                    String.format(ExceptionConstants.DEPOT_HEAD_ASSEMBLE_SAME_MATERIAL_MSG, combinationBarCode));
        }
        normalizeAssembleHeadFields(depotHead);
        return detailArray.toJSONString();
    }

    private void normalizeAssembleHeadFields(DepotHead depotHead) {
        //组装和拆卸的最终成本由明细服务按实时库存成本计算并回写。
        depotHead.setTotalPrice(BigDecimal.ZERO);
        depotHead.setOrganId(null);
        depotHead.setAccountId(null);
        depotHead.setAccountIdList(null);
        depotHead.setAccountMoneyList(null);
        depotHead.setDiscount(BigDecimal.ZERO);
        depotHead.setDiscountMoney(BigDecimal.ZERO);
        depotHead.setDiscountLastMoney(BigDecimal.ZERO);
        depotHead.setOtherMoney(BigDecimal.ZERO);
        depotHead.setDeposit(BigDecimal.ZERO);
        depotHead.setChangeAmount(BigDecimal.ZERO);
        depotHead.setBackAmount(BigDecimal.ZERO);
        depotHead.setDebt(BigDecimal.ZERO);
        depotHead.setLastDebt(BigDecimal.ZERO);
        depotHead.setLinkNumber(null);
        depotHead.setLinkApply(null);
        depotHead.setPayType("");
    }

    private String validateAndNormalizeDisassemble(DepotHead depotHead, String rows) throws Exception {
        JSONArray detailArray = JSONArray.parseArray(rows);
        if (detailArray == null || detailArray.size() < 2) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_STRUCTURE_CODE,
                    ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_STRUCTURE_MSG);
        }
        String combinationBarCode = null;
        Set<String> componentBarCodes = new HashSet<>();
        for (int index = 0; index < detailArray.size(); index++) {
            JSONObject detail = JSONObject.parseObject(detailArray.get(index).toString());
            String expectedMaterialType = index == 0 ? "组合件" : "普通子件";
            if (!expectedMaterialType.equals(detail.getString("mType"))) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_STRUCTURE_CODE,
                        ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_STRUCTURE_MSG);
            }
            String barCode = detail.getString("barCode");
            if (index == 0) {
                combinationBarCode = barCode;
            } else {
                componentBarCodes.add(barCode);
            }
            Long depotId = detail.getLong("depotId");
            if (depotId == null) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_DEPOT_FAILED_CODE,
                        ExceptionConstants.DEPOT_HEAD_DEPOT_FAILED_MSG);
            }
            depotService.parseDepotList(depotId);
            BigDecimal quantity = detail.getBigDecimal("operNumber");
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_MSG, barCode));
            }
            BigDecimal unitPrice = detail.getBigDecimal("unitPrice");
            if (unitPrice != null && unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_AMOUNT_CODE,
                        ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_AMOUNT_MSG);
            }
            //客户端成本仅用于录入展示，持久化前会按实时库存成本重新计算。
            detail.put("unitPrice", BigDecimal.ZERO);
            detail.put("allPrice", BigDecimal.ZERO);
            detail.put("taxRate", BigDecimal.ZERO);
            detail.put("taxMoney", BigDecimal.ZERO);
            detail.put("taxLastMoney", BigDecimal.ZERO);
            detail.put("taxUnitPrice", BigDecimal.ZERO);
            detail.remove("linkId");
            detail.remove("preNumber");
            detail.remove("finishNumber");
            detail.remove("snList");
            detail.remove("batchNumber");
            detail.remove("expirationDate");
            detailArray.set(index, detail);
        }
        if (StringUtil.isNotEmpty(combinationBarCode) && componentBarCodes.contains(combinationBarCode)) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_SAME_MATERIAL_CODE,
                    String.format(ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_SAME_MATERIAL_MSG, combinationBarCode));
        }
        normalizeAssembleHeadFields(depotHead);
        return detailArray.toJSONString();
    }

    private void validateAssembleBeforeAudit(DepotHead depotHead) throws Exception {
        if (!isAssemble(depotHead)) {
            return;
        }
        List<DepotItem> detailList = depotItemService.getListByHeaderId(depotHead.getId());
        if (detailList == null || detailList.size() < 2) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ASSEMBLE_STRUCTURE_CODE,
                    ExceptionConstants.DEPOT_HEAD_ASSEMBLE_STRUCTURE_MSG);
        }
        int combinationCount = 0;
        Long combinationMaterialExtendId = null;
        Set<Long> componentMaterialExtendIds = new HashSet<>();
        for (DepotItem detail : detailList) {
            if ("组合件".equals(detail.getMaterialType())) {
                combinationCount++;
                combinationMaterialExtendId = detail.getMaterialExtendId();
            } else if ("普通子件".equals(detail.getMaterialType())) {
                componentMaterialExtendIds.add(detail.getMaterialExtendId());
            } else {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ASSEMBLE_STRUCTURE_CODE,
                        ExceptionConstants.DEPOT_HEAD_ASSEMBLE_STRUCTURE_MSG);
            }
            if (detail.getDepotId() == null) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_DEPOT_FAILED_CODE,
                        ExceptionConstants.DEPOT_HEAD_DEPOT_FAILED_MSG);
            }
            depotService.parseDepotList(detail.getDepotId());
            if (detail.getOperNumber() == null || detail.getOperNumber().compareTo(BigDecimal.ZERO) <= 0
                    || detail.getBasicNumber() == null || detail.getBasicNumber().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_MSG,
                                detail.getMaterialExtendId()));
            }
            if (detail.getLinkId() != null || StringUtil.isNotEmpty(detail.getSnList())
                    || StringUtil.isNotEmpty(detail.getBatchNumber())) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ASSEMBLE_AMOUNT_CODE,
                        ExceptionConstants.DEPOT_HEAD_ASSEMBLE_AMOUNT_MSG);
            }
        }
        if (combinationCount != 1 || componentMaterialExtendIds.isEmpty()) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ASSEMBLE_STRUCTURE_CODE,
                    ExceptionConstants.DEPOT_HEAD_ASSEMBLE_STRUCTURE_MSG);
        }
        if (combinationMaterialExtendId != null && componentMaterialExtendIds.contains(combinationMaterialExtendId)) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ASSEMBLE_SAME_MATERIAL_CODE,
                    String.format(ExceptionConstants.DEPOT_HEAD_ASSEMBLE_SAME_MATERIAL_MSG,
                            combinationMaterialExtendId));
        }
        if (StringUtil.isNotEmpty(depotHead.getLinkNumber()) || StringUtil.isNotEmpty(depotHead.getLinkApply())
                || depotHead.getOrganId() != null || depotHead.getAccountId() != null) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ASSEMBLE_AMOUNT_CODE,
                    ExceptionConstants.DEPOT_HEAD_ASSEMBLE_AMOUNT_MSG);
        }
    }

    private void validateDisassembleBeforeAudit(DepotHead depotHead) throws Exception {
        if (!isDisassemble(depotHead)) {
            return;
        }
        List<DepotItem> detailList = depotItemService.getListByHeaderId(depotHead.getId());
        if (detailList == null || detailList.size() < 2) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_STRUCTURE_CODE,
                    ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_STRUCTURE_MSG);
        }
        int combinationCount = 0;
        Long combinationMaterialExtendId = null;
        Set<Long> componentMaterialExtendIds = new HashSet<>();
        for (int index = 0; index < detailList.size(); index++) {
            DepotItem detail = detailList.get(index);
            String expectedMaterialType = index == 0 ? "组合件" : "普通子件";
            if (!expectedMaterialType.equals(detail.getMaterialType())) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_STRUCTURE_CODE,
                        ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_STRUCTURE_MSG);
            }
            if ("组合件".equals(detail.getMaterialType())) {
                combinationCount++;
                combinationMaterialExtendId = detail.getMaterialExtendId();
            } else {
                componentMaterialExtendIds.add(detail.getMaterialExtendId());
            }
            if (detail.getDepotId() == null) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_DEPOT_FAILED_CODE,
                        ExceptionConstants.DEPOT_HEAD_DEPOT_FAILED_MSG);
            }
            depotService.parseDepotList(detail.getDepotId());
            if (detail.getOperNumber() == null || detail.getOperNumber().compareTo(BigDecimal.ZERO) <= 0
                    || detail.getBasicNumber() == null || detail.getBasicNumber().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_MSG,
                                detail.getMaterialExtendId()));
            }
            if (detail.getLinkId() != null || StringUtil.isNotEmpty(detail.getSnList())
                    || StringUtil.isNotEmpty(detail.getBatchNumber())) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_AMOUNT_CODE,
                        ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_AMOUNT_MSG);
            }
        }
        if (combinationCount != 1 || componentMaterialExtendIds.isEmpty()) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_STRUCTURE_CODE,
                    ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_STRUCTURE_MSG);
        }
        if (combinationMaterialExtendId != null && componentMaterialExtendIds.contains(combinationMaterialExtendId)) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_SAME_MATERIAL_CODE,
                    String.format(ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_SAME_MATERIAL_MSG,
                            combinationMaterialExtendId));
        }
        if (StringUtil.isNotEmpty(depotHead.getLinkNumber()) || StringUtil.isNotEmpty(depotHead.getLinkApply())
                || depotHead.getOrganId() != null || depotHead.getAccountId() != null) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_AMOUNT_CODE,
                    ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_AMOUNT_MSG);
        }
    }

    private String validateAndNormalizeTransferOutbound(DepotHead depotHead, String rows) throws Exception {
        JSONArray detailArray = JSONArray.parseArray(rows);
        if (detailArray == null || detailArray.isEmpty()) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ROW_FAILED_CODE,
                    ExceptionConstants.DEPOT_HEAD_ROW_FAILED_MSG);
        }
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (int detailIndex = 0; detailIndex < detailArray.size(); detailIndex++) {
            JSONObject detail = JSONObject.parseObject(detailArray.get(detailIndex).toString());
            String barCode = detail.getString("barCode");
            Long depotId = detail.getLong("depotId");
            Long anotherDepotId = detail.getLong("anotherDepotId");
            BigDecimal quantity = detail.getBigDecimal("operNumber");
            BigDecimal unitPrice = detail.getBigDecimal("unitPrice");
            if (depotId == null) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_DEPOT_FAILED_CODE,
                        ExceptionConstants.DEPOT_HEAD_DEPOT_FAILED_MSG);
            }
            if (anotherDepotId == null) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ANOTHER_DEPOT_FAILED_CODE,
                        ExceptionConstants.DEPOT_HEAD_ANOTHER_DEPOT_FAILED_MSG);
            }
            if (Objects.equals(depotId, anotherDepotId)) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ANOTHER_DEPOT_EQUAL_FAILED_CODE,
                        ExceptionConstants.DEPOT_HEAD_ANOTHER_DEPOT_EQUAL_FAILED_MSG);
            }
            //同时校验仓库存在、启用状态及当前用户对调出/调入仓库的数据权限。
            depotService.parseDepotList(depotId);
            depotService.parseDepotList(anotherDepotId);
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_MSG, barCode));
            }
            unitPrice = unitPrice == null ? BigDecimal.ZERO : unitPrice;
            if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_TRANSFER_AMOUNT_CODE,
                        ExceptionConstants.DEPOT_HEAD_TRANSFER_AMOUNT_MSG);
            }
            BigDecimal allPrice = quantity.multiply(unitPrice).setScale(2, BigDecimal.ROUND_HALF_UP);
            detail.put("unitPrice", unitPrice);
            detail.put("allPrice", allPrice);
            detail.put("taxRate", BigDecimal.ZERO);
            detail.put("taxMoney", BigDecimal.ZERO);
            detail.put("taxLastMoney", allPrice);
            detail.put("taxUnitPrice", unitPrice.setScale(4, BigDecimal.ROUND_HALF_UP));
            detail.remove("linkId");
            detail.remove("preNumber");
            detail.remove("finishNumber");
            detail.remove("snList");
            detail.remove("batchNumber");
            detail.remove("expirationDate");
            detailArray.set(detailIndex, detail);
            totalPrice = totalPrice.add(allPrice);
        }
        depotHead.setTotalPrice(totalPrice.setScale(2, BigDecimal.ROUND_HALF_UP));
        depotHead.setOrganId(null);
        depotHead.setAccountId(null);
        depotHead.setAccountIdList(null);
        depotHead.setAccountMoneyList(null);
        depotHead.setDiscount(BigDecimal.ZERO);
        depotHead.setDiscountMoney(BigDecimal.ZERO);
        depotHead.setDiscountLastMoney(BigDecimal.ZERO);
        depotHead.setOtherMoney(BigDecimal.ZERO);
        depotHead.setDeposit(BigDecimal.ZERO);
        depotHead.setChangeAmount(BigDecimal.ZERO);
        depotHead.setBackAmount(BigDecimal.ZERO);
        depotHead.setDebt(BigDecimal.ZERO);
        depotHead.setLastDebt(BigDecimal.ZERO);
        depotHead.setLinkNumber(null);
        depotHead.setLinkApply(null);
        depotHead.setPayType("");
        return detailArray.toJSONString();
    }

    private void validateTransferOutboundBeforeAudit(DepotHead depotHead) throws Exception {
        if (!isTransferOutbound(depotHead)) {
            return;
        }
        List<DepotItem> detailList = depotItemService.getListByHeaderId(depotHead.getId());
        if (detailList == null || detailList.isEmpty()) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ROW_FAILED_CODE,
                    ExceptionConstants.DEPOT_HEAD_ROW_FAILED_MSG);
        }
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (DepotItem detail : detailList) {
            if (detail.getDepotId() == null || detail.getAnotherDepotId() == null
                    || Objects.equals(detail.getDepotId(), detail.getAnotherDepotId())) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ANOTHER_DEPOT_FAILED_CODE,
                        ExceptionConstants.DEPOT_HEAD_ANOTHER_DEPOT_FAILED_MSG);
            }
            depotService.parseDepotList(detail.getDepotId());
            depotService.parseDepotList(detail.getAnotherDepotId());
            BigDecimal quantity = detail.getOperNumber();
            BigDecimal unitPrice = detail.getUnitPrice() == null ? BigDecimal.ZERO : detail.getUnitPrice();
            BigDecimal allPrice = detail.getAllPrice() == null ? BigDecimal.ZERO : detail.getAllPrice();
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_MSG,
                                detail.getMaterialExtendId()));
            }
            BigDecimal expectedPrice = quantity.multiply(unitPrice).setScale(2, BigDecimal.ROUND_HALF_UP);
            if (unitPrice.compareTo(BigDecimal.ZERO) < 0 || allPrice.compareTo(expectedPrice) != 0
                    || detail.getLinkId() != null) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_TRANSFER_AMOUNT_CODE,
                        ExceptionConstants.DEPOT_HEAD_TRANSFER_AMOUNT_MSG);
            }
            totalPrice = totalPrice.add(expectedPrice);
        }
        BigDecimal headTotalPrice = depotHead.getTotalPrice() == null
                ? BigDecimal.ZERO : depotHead.getTotalPrice();
        if (headTotalPrice.compareTo(totalPrice.setScale(2, BigDecimal.ROUND_HALF_UP)) != 0
                || StringUtil.isNotEmpty(depotHead.getLinkNumber())
                || StringUtil.isNotEmpty(depotHead.getLinkApply())) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_TRANSFER_AMOUNT_CODE,
                    ExceptionConstants.DEPOT_HEAD_TRANSFER_AMOUNT_MSG);
        }
    }

    private String clearOtherInboundRowLinks(String rows) {
        JSONArray detailArray = JSONArray.parseArray(rows);
        if (detailArray == null || detailArray.isEmpty()) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ROW_FAILED_CODE,
                    ExceptionConstants.DEPOT_HEAD_ROW_FAILED_MSG);
        }
        for (int detailIndex = 0; detailIndex < detailArray.size(); detailIndex++) {
            JSONObject detail = JSONObject.parseObject(detailArray.get(detailIndex).toString());
            detail.remove("linkId");
            detail.remove("preNumber");
            detail.remove("finishNumber");
            detailArray.set(detailIndex, detail);
        }
        return detailArray.toJSONString();
    }

    private String validateAndNormalizeOtherInboundSource(DepotHead depotHead, String rows,
                                                            DepotHead previousDepotHead) throws Exception {
        DepotHead sourceHead = depotHeadMapperEx.lockDepotHeadByNumber(depotHead.getLinkNumber());
        boolean updatingCurrentAssociation = previousDepotHead != null
                && Objects.equals(normalizeLink(previousDepotHead.getLinkNumber()),
                normalizeLink(depotHead.getLinkNumber()));
        boolean sourceTypeValid = sourceHead != null
                && BusinessConstants.DEPOTHEAD_TYPE_IN.equals(sourceHead.getType())
                && (BusinessConstants.SUB_TYPE_PURCHASE.equals(sourceHead.getSubType())
                || BusinessConstants.SUB_TYPE_SALES_RETURN.equals(sourceHead.getSubType()));
        boolean sourceStatusValid = sourceHead != null
                && (BusinessConstants.BILLS_STATUS_AUDIT.equals(sourceHead.getStatus())
                || BusinessConstants.BILLS_STATUS_SKIPING.equals(sourceHead.getStatus())
                || (updatingCurrentAssociation && BusinessConstants.BILLS_STATUS_SKIPED.equals(sourceHead.getStatus())));
        if (!sourceTypeValid || !sourceStatusValid) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_OTHER_IN_SOURCE_CODE,
                    ExceptionConstants.DEPOT_HEAD_OTHER_IN_SOURCE_MSG);
        }
        checkPurchaseBillDataPermission(sourceHead);
        depotHead.setOrganId(sourceHead.getOrganId());

        JSONArray detailArray = JSONArray.parseArray(rows);
        if (detailArray == null || detailArray.isEmpty()) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ROW_FAILED_CODE,
                    ExceptionConstants.DEPOT_HEAD_ROW_FAILED_MSG);
        }
        Long currentHeaderId = previousDepotHead == null ? null : previousDepotHead.getId();
        Map<Long, BigDecimal> currentReceivedBasicMap = new HashMap<>();
        for (int detailIndex = 0; detailIndex < detailArray.size(); detailIndex++) {
            JSONObject detail = JSONObject.parseObject(detailArray.get(detailIndex).toString());
            String barCode = detail.getString("barCode");
            BigDecimal quantity = detail.getBigDecimal("operNumber");
            Long linkId = detail.getLong("linkId");
            DepotItem sourceItem = linkId == null ? null : depotItemMapperEx.lockDepotItemById(linkId);
            MaterialExtend materialExtend = StringUtil.isEmpty(barCode)
                    ? null : materialExtendService.getInfoByBarCode(barCode);
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0
                    || sourceItem == null || !Objects.equals(sourceItem.getHeaderId(), sourceHead.getId())
                    || materialExtend == null
                    || !Objects.equals(materialExtend.getId(), sourceItem.getMaterialExtendId())
                    || sourceItem.getOperNumber() == null || sourceItem.getOperNumber().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_OTHER_IN_DETAIL_CODE,
                        ExceptionConstants.DEPOT_HEAD_OTHER_IN_DETAIL_MSG);
            }
            BigDecimal sourceBasicNumber = sourceItem.getBasicNumber() == null
                    ? sourceItem.getOperNumber() : sourceItem.getBasicNumber();
            BigDecimal unitRatio = sourceBasicNumber.divide(sourceItem.getOperNumber(), 10, BigDecimal.ROUND_HALF_UP);
            BigDecimal currentBasicNumber = quantity.multiply(unitRatio);
            BigDecimal alreadyReceived = depotItemMapperEx.getOtherInboundReceivedBasicNumber(linkId, currentHeaderId);
            BigDecimal currentReceived = currentReceivedBasicMap.getOrDefault(linkId, BigDecimal.ZERO)
                    .add(currentBasicNumber);
            currentReceivedBasicMap.put(linkId, currentReceived);
            if (!systemConfigService.getOverLinkBillFlag()
                    && alreadyReceived.add(currentReceived).compareTo(sourceBasicNumber) > 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_OTHER_IN_OVER_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_OTHER_IN_OVER_MSG, barCode));
            }
            detail.put("linkId", sourceItem.getId());
            detail.put("unit", sourceItem.getMaterialUnit());
            detail.put("depotId", sourceItem.getDepotId());
            detail.put("preNumber", sourceItem.getOperNumber());
            BigDecimal finishNumber = unitRatio.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                    : alreadyReceived.divide(unitRatio, 10, BigDecimal.ROUND_HALF_UP);
            detail.put("finishNumber", finishNumber);
            detail.put("unitPrice", BigDecimal.ZERO);
            detail.put("allPrice", BigDecimal.ZERO);
            detail.put("taxRate", BigDecimal.ZERO);
            detail.put("taxMoney", BigDecimal.ZERO);
            detail.put("taxLastMoney", BigDecimal.ZERO);
            detail.put("taxUnitPrice", BigDecimal.ZERO);
            detailArray.set(detailIndex, detail);
        }
        return detailArray.toJSONString();
    }

    private void validateOtherInboundBeforeAudit(DepotHead depotHead) throws Exception {
        if (!isOtherInbound(depotHead) || StringUtil.isEmpty(depotHead.getLinkNumber())) {
            return;
        }
        List<DepotItemVo4WithInfoEx> itemList = depotItemService.getDetailList(depotHead.getId());
        JSONArray rowArray = new JSONArray();
        for (DepotItemVo4WithInfoEx item : itemList) {
            JSONObject row = JSONObject.parseObject(JSONObject.toJSONString(item));
            row.put("unit", item.getMaterialUnit());
            rowArray.add(row);
        }
        validateAndNormalizeOtherInboundSource(depotHead, rowArray.toJSONString(), depotHead);
    }

    private String normalizeOtherInboundFinancialFields(DepotHead depotHead, String rows) {
        JSONArray detailArray = JSONArray.parseArray(rows);
        if (detailArray == null || detailArray.isEmpty()) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ROW_FAILED_CODE,
                    ExceptionConstants.DEPOT_HEAD_ROW_FAILED_MSG);
        }
        boolean linked = StringUtil.isNotEmpty(depotHead.getLinkNumber());
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (int detailIndex = 0; detailIndex < detailArray.size(); detailIndex++) {
            JSONObject detail = JSONObject.parseObject(detailArray.get(detailIndex).toString());
            String barCode = detail.getString("barCode");
            BigDecimal quantity = detail.getBigDecimal("operNumber");
            BigDecimal unitPrice = linked ? BigDecimal.ZERO : detail.getBigDecimal("unitPrice");
            unitPrice = unitPrice == null ? BigDecimal.ZERO : unitPrice;
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_MSG, barCode));
            }
            if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_OTHER_IN_AMOUNT_CODE,
                        ExceptionConstants.DEPOT_HEAD_OTHER_IN_AMOUNT_MSG);
            }
            BigDecimal allPrice = quantity.multiply(unitPrice).setScale(2, BigDecimal.ROUND_HALF_UP);
            detail.put("unitPrice", unitPrice);
            detail.put("allPrice", allPrice);
            detail.put("taxRate", BigDecimal.ZERO);
            detail.put("taxMoney", BigDecimal.ZERO);
            detail.put("taxLastMoney", allPrice);
            detail.put("taxUnitPrice", quantity.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                    : allPrice.divide(quantity, 4, BigDecimal.ROUND_HALF_UP));
            detailArray.set(detailIndex, detail);
            totalPrice = totalPrice.add(allPrice);
        }
        depotHead.setTotalPrice(totalPrice.setScale(2, BigDecimal.ROUND_HALF_UP).negate());
        depotHead.setDiscount(BigDecimal.ZERO);
        depotHead.setDiscountMoney(BigDecimal.ZERO);
        depotHead.setDiscountLastMoney(BigDecimal.ZERO);
        depotHead.setOtherMoney(BigDecimal.ZERO);
        depotHead.setDeposit(BigDecimal.ZERO);
        depotHead.setChangeAmount(BigDecimal.ZERO);
        depotHead.setDebt(BigDecimal.ZERO);
        depotHead.setBackAmount(BigDecimal.ZERO);
        depotHead.setAccountId(null);
        depotHead.setAccountIdList(null);
        depotHead.setAccountMoneyList(null);
        return detailArray.toJSONString();
    }

    private String clearOtherOutboundRowLinks(String rows) {
        JSONArray detailArray = JSONArray.parseArray(rows);
        if (detailArray == null || detailArray.isEmpty()) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ROW_FAILED_CODE,
                    ExceptionConstants.DEPOT_HEAD_ROW_FAILED_MSG);
        }
        for (int detailIndex = 0; detailIndex < detailArray.size(); detailIndex++) {
            JSONObject detail = JSONObject.parseObject(detailArray.get(detailIndex).toString());
            detail.remove("linkId");
            detail.remove("preNumber");
            detail.remove("finishNumber");
            detailArray.set(detailIndex, detail);
        }
        return detailArray.toJSONString();
    }

    private String validateAndNormalizeOtherOutboundSource(DepotHead depotHead, String rows,
                                                              DepotHead previousDepotHead) throws Exception {
        DepotHead sourceHead = depotHeadMapperEx.lockDepotHeadByNumber(depotHead.getLinkNumber());
        boolean updatingCurrentAssociation = previousDepotHead != null
                && Objects.equals(normalizeLink(previousDepotHead.getLinkNumber()),
                normalizeLink(depotHead.getLinkNumber()));
        boolean sourceTypeValid = sourceHead != null
                && BusinessConstants.DEPOTHEAD_TYPE_OUT.equals(sourceHead.getType())
                && (BusinessConstants.SUB_TYPE_SALES.equals(sourceHead.getSubType())
                || BusinessConstants.SUB_TYPE_PURCHASE_RETURN.equals(sourceHead.getSubType()));
        boolean sourceStatusValid = sourceHead != null
                && (BusinessConstants.BILLS_STATUS_AUDIT.equals(sourceHead.getStatus())
                || BusinessConstants.BILLS_STATUS_SKIPING.equals(sourceHead.getStatus())
                || (updatingCurrentAssociation
                && BusinessConstants.BILLS_STATUS_SKIPED.equals(sourceHead.getStatus())));
        if (!sourceTypeValid || !sourceStatusValid) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_OTHER_OUT_SOURCE_CODE,
                    ExceptionConstants.DEPOT_HEAD_OTHER_OUT_SOURCE_MSG);
        }
        checkPurchaseBillDataPermission(sourceHead);
        JSONArray detailArray = JSONArray.parseArray(rows);
        if (detailArray == null || detailArray.isEmpty()) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ROW_FAILED_CODE,
                    ExceptionConstants.DEPOT_HEAD_ROW_FAILED_MSG);
        }
        depotHead.setLinkNumber(sourceHead.getNumber());
        depotHead.setOrganId(sourceHead.getOrganId());
        Long currentHeaderId = previousDepotHead == null ? null : previousDepotHead.getId();
        Map<Long, BigDecimal> currentIssuedBasicMap = new HashMap<>();
        for (int detailIndex = 0; detailIndex < detailArray.size(); detailIndex++) {
            JSONObject detail = JSONObject.parseObject(detailArray.get(detailIndex).toString());
            String barCode = detail.getString("barCode");
            BigDecimal quantity = detail.getBigDecimal("operNumber");
            Long linkId = detail.getLong("linkId");
            DepotItem sourceItem = linkId == null ? null : depotItemMapperEx.lockDepotItemById(linkId);
            MaterialExtend materialExtend = StringUtil.isEmpty(barCode)
                    ? null : materialExtendService.getInfoByBarCode(barCode);
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0
                    || sourceItem == null || !Objects.equals(sourceItem.getHeaderId(), sourceHead.getId())
                    || materialExtend == null
                    || !Objects.equals(materialExtend.getId(), sourceItem.getMaterialExtendId())
                    || sourceItem.getOperNumber() == null || sourceItem.getOperNumber().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_OTHER_OUT_DETAIL_CODE,
                        ExceptionConstants.DEPOT_HEAD_OTHER_OUT_DETAIL_MSG);
            }
            BigDecimal sourceBasicNumber = sourceItem.getBasicNumber() == null
                    ? sourceItem.getOperNumber() : sourceItem.getBasicNumber();
            BigDecimal unitRatio = sourceBasicNumber.divide(sourceItem.getOperNumber(), 10, BigDecimal.ROUND_HALF_UP);
            BigDecimal currentBasicNumber = quantity.multiply(unitRatio);
            BigDecimal alreadyIssued = depotItemMapperEx.getOtherOutboundIssuedBasicNumber(linkId, currentHeaderId);
            BigDecimal currentIssued = currentIssuedBasicMap.getOrDefault(linkId, BigDecimal.ZERO)
                    .add(currentBasicNumber);
            currentIssuedBasicMap.put(linkId, currentIssued);
            if (!systemConfigService.getOverLinkBillFlag()
                    && alreadyIssued.add(currentIssued).compareTo(sourceBasicNumber) > 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_OTHER_OUT_OVER_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_OTHER_OUT_OVER_MSG, barCode));
            }
            detail.put("linkId", sourceItem.getId());
            detail.put("unit", sourceItem.getMaterialUnit());
            detail.put("depotId", sourceItem.getDepotId());
            detail.put("preNumber", sourceItem.getOperNumber());
            BigDecimal finishNumber = unitRatio.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                    : alreadyIssued.divide(unitRatio, 10, BigDecimal.ROUND_HALF_UP);
            detail.put("finishNumber", finishNumber);
            detail.put("unitPrice", BigDecimal.ZERO);
            detail.put("allPrice", BigDecimal.ZERO);
            detail.put("taxRate", BigDecimal.ZERO);
            detail.put("taxMoney", BigDecimal.ZERO);
            detail.put("taxLastMoney", BigDecimal.ZERO);
            detail.put("taxUnitPrice", BigDecimal.ZERO);
            detailArray.set(detailIndex, detail);
        }
        return detailArray.toJSONString();
    }

    private void validateOtherOutboundBeforeAudit(DepotHead depotHead) throws Exception {
        if (!isOtherOutbound(depotHead) || StringUtil.isEmpty(depotHead.getLinkNumber())) {
            return;
        }
        List<DepotItemVo4WithInfoEx> itemList = depotItemService.getDetailList(depotHead.getId());
        JSONArray rowArray = new JSONArray();
        for (DepotItemVo4WithInfoEx item : itemList) {
            JSONObject row = JSONObject.parseObject(JSONObject.toJSONString(item));
            row.put("unit", item.getMaterialUnit());
            rowArray.add(row);
        }
        validateAndNormalizeOtherOutboundSource(depotHead, rowArray.toJSONString(), depotHead);
    }

    private String normalizeOtherOutboundFinancialFields(DepotHead depotHead, String rows) {
        JSONArray detailArray = JSONArray.parseArray(rows);
        if (detailArray == null || detailArray.isEmpty()) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ROW_FAILED_CODE,
                    ExceptionConstants.DEPOT_HEAD_ROW_FAILED_MSG);
        }
        boolean linked = StringUtil.isNotEmpty(depotHead.getLinkNumber());
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (int detailIndex = 0; detailIndex < detailArray.size(); detailIndex++) {
            JSONObject detail = JSONObject.parseObject(detailArray.get(detailIndex).toString());
            String barCode = detail.getString("barCode");
            BigDecimal quantity = detail.getBigDecimal("operNumber");
            BigDecimal unitPrice = linked ? BigDecimal.ZERO : detail.getBigDecimal("unitPrice");
            unitPrice = unitPrice == null ? BigDecimal.ZERO : unitPrice;
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_MSG, barCode));
            }
            if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_OTHER_OUT_AMOUNT_CODE,
                        ExceptionConstants.DEPOT_HEAD_OTHER_OUT_AMOUNT_MSG);
            }
            BigDecimal allPrice = quantity.multiply(unitPrice).setScale(2, BigDecimal.ROUND_HALF_UP);
            detail.put("unitPrice", unitPrice);
            detail.put("allPrice", allPrice);
            detail.put("taxRate", BigDecimal.ZERO);
            detail.put("taxMoney", BigDecimal.ZERO);
            detail.put("taxLastMoney", allPrice);
            detail.put("taxUnitPrice", quantity.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                    : allPrice.divide(quantity, 4, BigDecimal.ROUND_HALF_UP));
            detailArray.set(detailIndex, detail);
            totalPrice = totalPrice.add(allPrice);
        }
        depotHead.setTotalPrice(totalPrice.setScale(2, BigDecimal.ROUND_HALF_UP));
        depotHead.setDiscount(BigDecimal.ZERO);
        depotHead.setDiscountMoney(BigDecimal.ZERO);
        depotHead.setDiscountLastMoney(BigDecimal.ZERO);
        depotHead.setOtherMoney(BigDecimal.ZERO);
        depotHead.setDeposit(BigDecimal.ZERO);
        depotHead.setChangeAmount(BigDecimal.ZERO);
        depotHead.setDebt(BigDecimal.ZERO);
        depotHead.setBackAmount(BigDecimal.ZERO);
        depotHead.setAccountId(null);
        depotHead.setAccountIdList(null);
        depotHead.setAccountMoneyList(null);
        return detailArray.toJSONString();
    }

    private void validatePurchaseReturnLinkImmutable(DepotHead depotHead, DepotHead previousDepotHead) {
        if (previousDepotHead != null
                && !Objects.equals(StringUtil.toNull(previousDepotHead.getLinkNumber()),
                StringUtil.toNull(depotHead.getLinkNumber()))) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_RETURN_LINK_CHANGE_CODE,
                    ExceptionConstants.DEPOT_HEAD_PURCHASE_RETURN_LINK_CHANGE_MSG);
        }
    }

    private String validateAndNormalizePurchaseReturn(DepotHead depotHead, String rows,
                                                        DepotHead previousDepotHead) throws Exception {
        JSONArray detailArray = JSONArray.parseArray(rows);
        if (detailArray == null || detailArray.isEmpty()) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ROW_FAILED_CODE,
                    ExceptionConstants.DEPOT_HEAD_ROW_FAILED_MSG);
        }
        DepotHead sourceHead = null;
        if (StringUtil.isNotEmpty(depotHead.getLinkNumber())) {
            sourceHead = getDepotHead(depotHead.getLinkNumber());
            if (sourceHead == null || sourceHead.getId() == null || !isPurchaseInbound(sourceHead)
                    || !(BusinessConstants.BILLS_STATUS_AUDIT.equals(sourceHead.getStatus())
                    || BusinessConstants.BILLS_STATUS_SKIPED.equals(sourceHead.getStatus())
                    || BusinessConstants.BILLS_STATUS_SKIPING.equals(sourceHead.getStatus()))) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_RETURN_SOURCE_CODE,
                        ExceptionConstants.DEPOT_HEAD_PURCHASE_RETURN_SOURCE_MSG);
            }
            checkPurchaseBillDataPermission(sourceHead);
            if (!Objects.equals(sourceHead.getOrganId(), depotHead.getOrganId())) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_RETURN_SOURCE_CODE,
                        ExceptionConstants.DEPOT_HEAD_PURCHASE_RETURN_SOURCE_MSG);
            }
        }

        boolean priceIncludesTax = systemConfigService.getMaterialPriceTaxFlag();
        boolean physicalOutbound = !systemConfigService.getInOutManageFlag();
        Long currentHeaderId = previousDepotHead == null ? null : previousDepotHead.getId();
        Map<Long, BigDecimal> currentReturnBasicNumberMap = new HashMap<>();
        BigDecimal totalPrice = BigDecimal.ZERO;
        BigDecimal totalTaxLastMoney = BigDecimal.ZERO;
        for (int detailIndex = 0; detailIndex < detailArray.size(); detailIndex++) {
            JSONObject detail = JSONObject.parseObject(detailArray.get(detailIndex).toString());
            String barCode = detail.getString("barCode");
            MaterialExtend materialExtend = StringUtil.isEmpty(barCode)
                    ? null : materialExtendService.getInfoByBarCode(barCode);
            BigDecimal quantity = detail.getBigDecimal("operNumber");
            if (materialExtend == null) {
                throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_BARCODE_IS_NOT_EXIST_CODE,
                        String.format(ExceptionConstants.MATERIAL_BARCODE_IS_NOT_EXIST_MSG, barCode));
            }
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_MSG, barCode));
            }

            BigDecimal unitPrice;
            BigDecimal taxRate;
            if (sourceHead != null) {
                Long linkId = detail.getLong("linkId");
                DepotItem sourceItem = linkId == null ? null : depotItemMapperEx.lockDepotItemById(linkId);
                if (sourceItem == null || !Objects.equals(sourceItem.getHeaderId(), sourceHead.getId())
                        || !Objects.equals(sourceItem.getMaterialExtendId(), materialExtend.getId())
                        || sourceItem.getOperNumber() == null
                        || sourceItem.getOperNumber().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_RETURN_DETAIL_CODE,
                            ExceptionConstants.DEPOT_HEAD_PURCHASE_RETURN_DETAIL_MSG);
                }
                BigDecimal sourceBasicNumber = sourceItem.getBasicNumber() == null
                        ? sourceItem.getOperNumber() : sourceItem.getBasicNumber();
                if (sourceBasicNumber.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_RETURN_DETAIL_CODE,
                            ExceptionConstants.DEPOT_HEAD_PURCHASE_RETURN_DETAIL_MSG);
                }
                BigDecimal unitRatio = sourceBasicNumber.divide(sourceItem.getOperNumber(), 12,
                        BigDecimal.ROUND_HALF_UP);
                BigDecimal requestedBasicNumber = quantity.multiply(unitRatio);
                BigDecimal alreadyReturnedBasicNumber = depotItemMapperEx
                        .getPurchaseReturnReturnedBasicNumber(linkId, currentHeaderId);
                BigDecimal currentRequestedBasicNumber = currentReturnBasicNumberMap
                        .getOrDefault(linkId, BigDecimal.ZERO).add(requestedBasicNumber);
                currentReturnBasicNumberMap.put(linkId, currentRequestedBasicNumber);
                if (!systemConfigService.getOverLinkBillFlag()
                        && alreadyReturnedBasicNumber.add(currentRequestedBasicNumber)
                        .compareTo(sourceBasicNumber) > 0) {
                    throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_RETURN_OVER_CODE,
                            String.format(ExceptionConstants.DEPOT_HEAD_PURCHASE_RETURN_OVER_MSG, barCode));
                }

                detail.put("unit", sourceItem.getMaterialUnit());
                detail.put("batchNumber", sourceItem.getBatchNumber());
                detail.put("expirationDate", sourceItem.getExpirationDate());
                detail.put("preNumber", sourceItem.getOperNumber());
                detail.put("finishNumber", alreadyReturnedBasicNumber.divide(unitRatio, 6,
                        BigDecimal.ROUND_HALF_UP));
                Long depotId = detail.getLong("depotId");
                serialNumberService.validatePurchaseReturnSerialNumbers(materialExtend.getMaterialId(), depotId,
                        depotHead.getNumber(), detail.getString("snList"), sourceItem.getSnList(), physicalOutbound);
                unitPrice = sourceItem.getUnitPrice() == null ? BigDecimal.ZERO : sourceItem.getUnitPrice();
                taxRate = sourceItem.getTaxRate() == null ? BigDecimal.ZERO : sourceItem.getTaxRate();
            } else {
                unitPrice = detail.getBigDecimal("unitPrice");
                taxRate = detail.getBigDecimal("taxRate");
                taxRate = taxRate == null ? BigDecimal.ZERO : taxRate;
            }
            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0
                    || taxRate.compareTo(BigDecimal.ZERO) < 0
                    || taxRate.compareTo(new BigDecimal("100")) > 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_RETURN_AMOUNT_CODE,
                        ExceptionConstants.DEPOT_HEAD_PURCHASE_RETURN_AMOUNT_MSG);
            }

            BigDecimal allPrice = quantity.multiply(unitPrice).setScale(2, BigDecimal.ROUND_HALF_UP);
            BigDecimal taxMoney;
            BigDecimal taxLastMoney;
            if (priceIncludesTax) {
                BigDecimal realAllPrice = allPrice.divide(BigDecimal.ONE.add(taxRate.movePointLeft(2)), 2,
                        BigDecimal.ROUND_HALF_UP);
                taxMoney = realAllPrice.multiply(taxRate).movePointLeft(2)
                        .setScale(2, BigDecimal.ROUND_HALF_UP);
                taxLastMoney = allPrice;
            } else {
                taxMoney = allPrice.multiply(taxRate).movePointLeft(2)
                        .setScale(2, BigDecimal.ROUND_HALF_UP);
                taxLastMoney = allPrice.add(taxMoney).setScale(2, BigDecimal.ROUND_HALF_UP);
            }
            detail.put("unitPrice", unitPrice);
            detail.put("allPrice", allPrice);
            detail.put("taxRate", taxRate);
            detail.put("taxMoney", taxMoney);
            detail.put("taxLastMoney", taxLastMoney);
            detail.put("taxUnitPrice", taxLastMoney.divide(quantity, 4, BigDecimal.ROUND_HALF_UP));
            detailArray.set(detailIndex, detail);
            totalPrice = totalPrice.add(allPrice);
            totalTaxLastMoney = totalTaxLastMoney.add(taxLastMoney);
        }

        totalPrice = totalPrice.setScale(2, BigDecimal.ROUND_HALF_UP);
        totalTaxLastMoney = totalTaxLastMoney.setScale(2, BigDecimal.ROUND_HALF_UP);
        BigDecimal discountMoney = depotHead.getDiscountMoney();
        BigDecimal discount = depotHead.getDiscount();
        if (discountMoney != null) {
            if (discountMoney.compareTo(BigDecimal.ZERO) < 0
                    || discountMoney.compareTo(totalTaxLastMoney) > 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_RETURN_AMOUNT_CODE,
                        ExceptionConstants.DEPOT_HEAD_PURCHASE_RETURN_AMOUNT_MSG);
            }
            discountMoney = discountMoney.setScale(2, BigDecimal.ROUND_HALF_UP);
            discount = totalTaxLastMoney.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                    : discountMoney.multiply(new BigDecimal("100"))
                    .divide(totalTaxLastMoney, 2, BigDecimal.ROUND_HALF_UP);
        } else {
            discount = discount == null ? BigDecimal.ZERO : discount;
            if (discount.compareTo(BigDecimal.ZERO) < 0
                    || discount.compareTo(new BigDecimal("100")) > 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_RETURN_AMOUNT_CODE,
                        ExceptionConstants.DEPOT_HEAD_PURCHASE_RETURN_AMOUNT_MSG);
            }
            discountMoney = totalTaxLastMoney.multiply(discount).movePointLeft(2)
                    .setScale(2, BigDecimal.ROUND_HALF_UP);
        }
        BigDecimal discountLastMoney = totalTaxLastMoney.subtract(discountMoney)
                .setScale(2, BigDecimal.ROUND_HALF_UP);
        BigDecimal otherMoney = depotHead.getOtherMoney() == null
                ? BigDecimal.ZERO : depotHead.getOtherMoney();
        BigDecimal refund = depotHead.getChangeAmount() == null
                ? BigDecimal.ZERO : depotHead.getChangeAmount();
        BigDecimal refundable = discountLastMoney.add(otherMoney).setScale(2, BigDecimal.ROUND_HALF_UP);
        if (otherMoney.compareTo(BigDecimal.ZERO) < 0 || refund.compareTo(BigDecimal.ZERO) < 0
                || refund.compareTo(refundable) > 0) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_RETURN_AMOUNT_CODE,
                    ExceptionConstants.DEPOT_HEAD_PURCHASE_RETURN_AMOUNT_MSG);
        }
        depotHead.setTotalPrice(totalPrice);
        depotHead.setDiscount(discount);
        depotHead.setDiscountMoney(discountMoney);
        depotHead.setDiscountLastMoney(discountLastMoney);
        depotHead.setOtherMoney(otherMoney);
        depotHead.setDeposit(BigDecimal.ZERO);
        depotHead.setChangeAmount(refund.setScale(2, BigDecimal.ROUND_HALF_UP));
        depotHead.setDebt(refundable.subtract(refund).setScale(2, BigDecimal.ROUND_HALF_UP));
        depotHead.setBackAmount(BigDecimal.ZERO);
        return detailArray.toJSONString();
    }

    private String validateAndNormalizePurchaseApply(DepotHead depotHead, String rows) throws Exception {
        JSONArray detailArray = JSONArray.parseArray(rows);
        if (detailArray == null || detailArray.isEmpty()) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ROW_FAILED_CODE,
                    ExceptionConstants.DEPOT_HEAD_ROW_FAILED_MSG);
        }
        for (int detailIndex = 0; detailIndex < detailArray.size(); detailIndex++) {
            JSONObject detail = JSONObject.parseObject(detailArray.get(detailIndex).toString());
            String barCode = detail.getString("barCode");
            MaterialExtend materialExtend = StringUtil.isEmpty(barCode)
                    ? null : materialExtendService.getInfoByBarCode(barCode);
            if (materialExtend == null) {
                throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_BARCODE_IS_NOT_EXIST_CODE,
                        String.format(ExceptionConstants.MATERIAL_BARCODE_IS_NOT_EXIST_MSG, barCode));
            }
            BigDecimal quantity = detail.getBigDecimal("operNumber");
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_MSG, barCode));
            }
            detail.put("unit", materialExtend.getCommodityUnit());
            detail.put("unitPrice", BigDecimal.ZERO);
            detail.put("taxUnitPrice", BigDecimal.ZERO);
            detail.put("allPrice", BigDecimal.ZERO);
            detail.put("taxRate", BigDecimal.ZERO);
            detail.put("taxMoney", BigDecimal.ZERO);
            detail.put("taxLastMoney", BigDecimal.ZERO);
            detailArray.set(detailIndex, detail);
        }
        depotHead.setOrganId(null);
        depotHead.setAccountId(null);
        depotHead.setAccountIdList(null);
        depotHead.setAccountMoneyList(null);
        depotHead.setChangeAmount(BigDecimal.ZERO);
        depotHead.setBackAmount(BigDecimal.ZERO);
        depotHead.setTotalPrice(BigDecimal.ZERO);
        depotHead.setDiscount(BigDecimal.ZERO);
        depotHead.setDiscountMoney(BigDecimal.ZERO);
        depotHead.setDiscountLastMoney(BigDecimal.ZERO);
        depotHead.setOtherMoney(BigDecimal.ZERO);
        depotHead.setDeposit(BigDecimal.ZERO);
        depotHead.setDebt(BigDecimal.ZERO);
        return detailArray.toJSONString();
    }

    private void validatePurchaseLinkImmutable(DepotHead depotHead, DepotHead previousDepotHead) {
        if (previousDepotHead == null) {
            return;
        }
        if (!Objects.equals(normalizeLink(previousDepotHead.getLinkApply()), normalizeLink(depotHead.getLinkApply()))
                || !Objects.equals(normalizeLink(previousDepotHead.getLinkNumber()), normalizeLink(depotHead.getLinkNumber()))) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_LINK_CHANGE_CODE,
                    ExceptionConstants.DEPOT_HEAD_PURCHASE_LINK_CHANGE_MSG);
        }
    }

    private void validatePurchaseInboundLinkImmutable(DepotHead depotHead, DepotHead previousDepotHead) {
        if (previousDepotHead != null
                && !Objects.equals(normalizeLink(previousDepotHead.getLinkNumber()), normalizeLink(depotHead.getLinkNumber()))) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_LINK_CHANGE_CODE,
                    ExceptionConstants.DEPOT_HEAD_PURCHASE_LINK_CHANGE_MSG);
        }
    }

    private String normalizeLink(String linkNumber) {
        return StringUtil.isEmpty(linkNumber) ? null : linkNumber;
    }

    private String clearPurchaseRowLinks(String rows) {
        JSONArray detailArray = JSONArray.parseArray(rows);
        if (detailArray == null || detailArray.isEmpty()) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ROW_FAILED_CODE,
                    ExceptionConstants.DEPOT_HEAD_ROW_FAILED_MSG);
        }
        for (int detailIndex = 0; detailIndex < detailArray.size(); detailIndex++) {
            JSONObject detail = JSONObject.parseObject(detailArray.get(detailIndex).toString());
            detail.remove("linkId");
            detail.remove("preNumber");
            detail.remove("finishNumber");
            detailArray.set(detailIndex, detail);
        }
        return detailArray.toJSONString();
    }

    private void validatePurchaseSupplier(DepotHead depotHead) throws Exception {
        Supplier supplier = depotHead.getOrganId() == null ? null : supplierService.getSupplier(depotHead.getOrganId());
        if (supplier == null || supplier.getId() == null
                || !"供应商".equals(supplier.getType())
                || !Boolean.TRUE.equals(supplier.getEnabled())
                || BusinessConstants.DELETE_FLAG_DELETED.equals(supplier.getDeleteFlag())) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_SUPPLIER_CODE,
                    ExceptionConstants.DEPOT_HEAD_PURCHASE_SUPPLIER_MSG);
        }
    }

    private String validateAndNormalizePurchaseOrderFromApply(DepotHead depotHead, String rows,
                                                               DepotHead previousDepotHead) throws Exception {
        DepotHead sourceHead = getDepotHead(depotHead.getLinkApply());
        boolean updatingCurrentAssociation = previousDepotHead != null
                && Objects.equals(previousDepotHead.getLinkApply(), depotHead.getLinkApply());
        boolean sourceStatusValid = sourceHead != null
                && (BusinessConstants.BILLS_STATUS_AUDIT.equals(sourceHead.getStatus())
                || BusinessConstants.BILLS_STATUS_SKIPING.equals(sourceHead.getStatus())
                || (updatingCurrentAssociation && BusinessConstants.BILLS_STATUS_SKIPED.equals(sourceHead.getStatus())));
        if (sourceHead == null || sourceHead.getId() == null
                || !BusinessConstants.DEPOTHEAD_TYPE_OTHER.equals(sourceHead.getType())
                || !BusinessConstants.SUB_TYPE_PURCHASE_APPLY.equals(sourceHead.getSubType())
                || !sourceStatusValid) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_APPLY_SOURCE_CODE,
                    ExceptionConstants.DEPOT_HEAD_PURCHASE_APPLY_SOURCE_MSG);
        }

        JSONArray detailArray = JSONArray.parseArray(rows);
        if (detailArray == null || detailArray.isEmpty()) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ROW_FAILED_CODE,
                    ExceptionConstants.DEPOT_HEAD_ROW_FAILED_MSG);
        }
        Long currentHeaderId = previousDepotHead == null ? null : previousDepotHead.getId();
        Map<Long, BigDecimal> currentAppliedNumberMap = new HashMap<>();
        for (int detailIndex = 0; detailIndex < detailArray.size(); detailIndex++) {
            JSONObject detail = JSONObject.parseObject(detailArray.get(detailIndex).toString());
            String barCode = detail.getString("barCode");
            BigDecimal quantity = detail.getBigDecimal("operNumber");
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_MSG, barCode));
            }
            Long linkId = detail.getLong("linkId");
            DepotItem sourceItem = linkId == null ? null : depotItemMapperEx.lockDepotItemById(linkId);
            MaterialExtend materialExtend = StringUtil.isEmpty(barCode)
                    ? null : materialExtendService.getInfoByBarCode(barCode);
            if (sourceItem == null || !Objects.equals(sourceItem.getHeaderId(), sourceHead.getId())
                    || materialExtend == null
                    || !Objects.equals(materialExtend.getId(), sourceItem.getMaterialExtendId())
                    || sourceItem.getOperNumber() == null
                    || sourceItem.getOperNumber().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_APPLY_DETAIL_CODE,
                        ExceptionConstants.DEPOT_HEAD_PURCHASE_APPLY_DETAIL_MSG);
            }

            BigDecimal alreadyApplied = depotItemMapperEx.getPurchaseOrderAppliedNumber(linkId, currentHeaderId);
            BigDecimal currentApplied = currentAppliedNumberMap.getOrDefault(linkId, BigDecimal.ZERO).add(quantity);
            currentAppliedNumberMap.put(linkId, currentApplied);
            if (!systemConfigService.getOverLinkBillFlag()
                    && alreadyApplied.add(currentApplied).compareTo(sourceItem.getOperNumber()) > 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_APPLY_OVER_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_PURCHASE_APPLY_OVER_MSG, barCode));
            }

            detail.put("linkId", sourceItem.getId());
            detail.put("unit", sourceItem.getMaterialUnit());
            detail.put("preNumber", sourceItem.getOperNumber());
            detail.put("finishNumber", alreadyApplied);
            detailArray.set(detailIndex, detail);
        }
        return detailArray.toJSONString();
    }

    private String validateAndNormalizePurchaseOrderFromSales(DepotHead depotHead, String rows,
                                                               DepotHead previousDepotHead) throws Exception {
        DepotHead sourceHead = getDepotHead(depotHead.getLinkNumber());
        boolean updatingCurrentAssociation = previousDepotHead != null
                && Objects.equals(previousDepotHead.getLinkNumber(), depotHead.getLinkNumber());
        boolean sourceStatusValid = sourceHead != null
                && (BusinessConstants.BILLS_STATUS_AUDIT.equals(sourceHead.getStatus())
                || BusinessConstants.BILLS_STATUS_SKIPING.equals(sourceHead.getStatus())
                || (updatingCurrentAssociation && BusinessConstants.BILLS_STATUS_SKIPED.equals(sourceHead.getStatus())));
        boolean purchaseStatusValid = sourceHead != null
                && (BusinessConstants.PURCHASE_STATUS_UN_AUDIT.equals(sourceHead.getPurchaseStatus())
                || BusinessConstants.PURCHASE_STATUS_SKIPING.equals(sourceHead.getPurchaseStatus())
                || (updatingCurrentAssociation
                && BusinessConstants.PURCHASE_STATUS_SKIPED.equals(sourceHead.getPurchaseStatus())));
        if (sourceHead == null || sourceHead.getId() == null
                || !BusinessConstants.DEPOTHEAD_TYPE_OTHER.equals(sourceHead.getType())
                || !BusinessConstants.SUB_TYPE_SALES_ORDER.equals(sourceHead.getSubType())
                || !sourceStatusValid || !purchaseStatusValid) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_SALES_SOURCE_CODE,
                    ExceptionConstants.DEPOT_HEAD_PURCHASE_SALES_SOURCE_MSG);
        }

        JSONArray detailArray = JSONArray.parseArray(rows);
        if (detailArray == null || detailArray.isEmpty()) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ROW_FAILED_CODE,
                    ExceptionConstants.DEPOT_HEAD_ROW_FAILED_MSG);
        }
        Long currentHeaderId = previousDepotHead == null ? null : previousDepotHead.getId();
        Map<Long, BigDecimal> currentAppliedNumberMap = new HashMap<>();
        for (int detailIndex = 0; detailIndex < detailArray.size(); detailIndex++) {
            JSONObject detail = JSONObject.parseObject(detailArray.get(detailIndex).toString());
            String barCode = detail.getString("barCode");
            BigDecimal quantity = detail.getBigDecimal("operNumber");
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_MSG, barCode));
            }
            Long linkId = detail.getLong("linkId");
            DepotItem sourceItem = linkId == null ? null : depotItemMapperEx.lockDepotItemById(linkId);
            MaterialExtend materialExtend = StringUtil.isEmpty(barCode)
                    ? null : materialExtendService.getInfoByBarCode(barCode);
            if (sourceItem == null || !Objects.equals(sourceItem.getHeaderId(), sourceHead.getId())
                    || materialExtend == null
                    || !Objects.equals(materialExtend.getId(), sourceItem.getMaterialExtendId())
                    || sourceItem.getOperNumber() == null
                    || sourceItem.getOperNumber().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_SALES_DETAIL_CODE,
                        ExceptionConstants.DEPOT_HEAD_PURCHASE_SALES_DETAIL_MSG);
            }
            BigDecimal alreadyApplied = depotItemMapperEx.getPurchaseOrderAppliedNumber(linkId, currentHeaderId);
            BigDecimal currentApplied = currentAppliedNumberMap.getOrDefault(linkId, BigDecimal.ZERO).add(quantity);
            currentAppliedNumberMap.put(linkId, currentApplied);
            if (!systemConfigService.getOverLinkBillFlag()
                    && alreadyApplied.add(currentApplied).compareTo(sourceItem.getOperNumber()) > 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_SALES_OVER_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_PURCHASE_SALES_OVER_MSG, barCode));
            }
            detail.put("linkId", sourceItem.getId());
            detail.put("unit", sourceItem.getMaterialUnit());
            detail.put("preNumber", sourceItem.getOperNumber());
            detail.put("finishNumber", alreadyApplied);
            detailArray.set(detailIndex, detail);
        }
        return detailArray.toJSONString();
    }

    private String validateAndNormalizePurchaseInbound(DepotHead depotHead, String rows,
                                                        DepotHead previousDepotHead) throws Exception {
        DepotHead sourceHead = getDepotHead(depotHead.getLinkNumber());
        boolean updatingCurrentAssociation = previousDepotHead != null
                && Objects.equals(previousDepotHead.getLinkNumber(), depotHead.getLinkNumber());
        boolean sourceStatusValid = sourceHead != null
                && (BusinessConstants.BILLS_STATUS_AUDIT.equals(sourceHead.getStatus())
                || BusinessConstants.BILLS_STATUS_SKIPING.equals(sourceHead.getStatus())
                || (updatingCurrentAssociation && BusinessConstants.BILLS_STATUS_SKIPED.equals(sourceHead.getStatus())));
        if (sourceHead == null || sourceHead.getId() == null
                || !BusinessConstants.DEPOTHEAD_TYPE_OTHER.equals(sourceHead.getType())
                || !BusinessConstants.SUB_TYPE_PURCHASE_ORDER.equals(sourceHead.getSubType())
                || !sourceStatusValid) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_IN_SOURCE_CODE,
                    ExceptionConstants.DEPOT_HEAD_PURCHASE_IN_SOURCE_MSG);
        }
        if (!Objects.equals(sourceHead.getOrganId(), depotHead.getOrganId())) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_SUPPLIER_CODE,
                    ExceptionConstants.DEPOT_HEAD_PURCHASE_SUPPLIER_MSG);
        }

        JSONArray detailArray = JSONArray.parseArray(rows);
        if (detailArray == null || detailArray.isEmpty()) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ROW_FAILED_CODE,
                    ExceptionConstants.DEPOT_HEAD_ROW_FAILED_MSG);
        }
        Long currentHeaderId = previousDepotHead == null ? null : previousDepotHead.getId();
        Map<Long, BigDecimal> currentReceivedNumberMap = new HashMap<>();
        for (int detailIndex = 0; detailIndex < detailArray.size(); detailIndex++) {
            JSONObject detail = JSONObject.parseObject(detailArray.get(detailIndex).toString());
            String barCode = detail.getString("barCode");
            BigDecimal quantity = detail.getBigDecimal("operNumber");
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_MSG, barCode));
            }
            Long linkId = detail.getLong("linkId");
            DepotItem sourceItem = linkId == null ? null : depotItemMapperEx.lockDepotItemById(linkId);
            MaterialExtend materialExtend = StringUtil.isEmpty(barCode)
                    ? null : materialExtendService.getInfoByBarCode(barCode);
            if (sourceItem == null || !Objects.equals(sourceItem.getHeaderId(), sourceHead.getId())
                    || materialExtend == null
                    || !Objects.equals(materialExtend.getId(), sourceItem.getMaterialExtendId())
                    || sourceItem.getOperNumber() == null
                    || sourceItem.getOperNumber().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_IN_DETAIL_CODE,
                        ExceptionConstants.DEPOT_HEAD_PURCHASE_IN_DETAIL_MSG);
            }
            BigDecimal alreadyReceived = depotItemMapperEx.getPurchaseInboundReceivedNumber(linkId, currentHeaderId);
            BigDecimal currentReceived = currentReceivedNumberMap.getOrDefault(linkId, BigDecimal.ZERO).add(quantity);
            currentReceivedNumberMap.put(linkId, currentReceived);
            if (!systemConfigService.getOverLinkBillFlag()
                    && alreadyReceived.add(currentReceived).compareTo(sourceItem.getOperNumber()) > 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_IN_OVER_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_PURCHASE_IN_OVER_MSG, barCode));
            }
            detail.put("linkId", sourceItem.getId());
            detail.put("unit", sourceItem.getMaterialUnit());
            detail.put("unitPrice", sourceItem.getUnitPrice() == null ? BigDecimal.ZERO : sourceItem.getUnitPrice());
            detail.put("taxRate", sourceItem.getTaxRate() == null ? BigDecimal.ZERO : sourceItem.getTaxRate());
            detail.put("preNumber", sourceItem.getOperNumber());
            detail.put("finishNumber", alreadyReceived);
            detailArray.set(detailIndex, detail);
        }
        return detailArray.toJSONString();
    }

    private String normalizePurchaseFinancialFields(DepotHead depotHead, String rows, boolean purchaseOrder)
            throws Exception {
        JSONArray detailArray = JSONArray.parseArray(rows);
        if (detailArray == null || detailArray.isEmpty()) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ROW_FAILED_CODE,
                    ExceptionConstants.DEPOT_HEAD_ROW_FAILED_MSG);
        }
        boolean priceIncludesTax = systemConfigService.getMaterialPriceTaxFlag();
        BigDecimal totalPrice = BigDecimal.ZERO;
        BigDecimal totalTaxLastMoney = BigDecimal.ZERO;
        for (int detailIndex = 0; detailIndex < detailArray.size(); detailIndex++) {
            JSONObject detail = JSONObject.parseObject(detailArray.get(detailIndex).toString());
            String barCode = detail.getString("barCode");
            BigDecimal quantity = detail.getBigDecimal("operNumber");
            BigDecimal unitPrice = detail.getBigDecimal("unitPrice");
            BigDecimal taxRate = detail.getBigDecimal("taxRate");
            taxRate = taxRate == null ? BigDecimal.ZERO : taxRate;
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_MSG, barCode));
            }
            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0
                    || taxRate.compareTo(BigDecimal.ZERO) < 0 || taxRate.compareTo(new BigDecimal("100")) > 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_AMOUNT_CODE,
                        ExceptionConstants.DEPOT_HEAD_PURCHASE_AMOUNT_MSG);
            }
            BigDecimal allPrice = quantity.multiply(unitPrice).setScale(2, BigDecimal.ROUND_HALF_UP);
            BigDecimal taxMoney;
            BigDecimal taxLastMoney;
            if (priceIncludesTax) {
                BigDecimal divisor = BigDecimal.ONE.add(taxRate.movePointLeft(2));
                BigDecimal realAllPrice = allPrice.divide(divisor, 2, BigDecimal.ROUND_HALF_UP);
                taxMoney = realAllPrice.multiply(taxRate).movePointLeft(2)
                        .setScale(2, BigDecimal.ROUND_HALF_UP);
                taxLastMoney = allPrice;
            } else {
                taxMoney = allPrice.multiply(taxRate).movePointLeft(2)
                        .setScale(2, BigDecimal.ROUND_HALF_UP);
                taxLastMoney = allPrice.add(taxMoney).setScale(2, BigDecimal.ROUND_HALF_UP);
            }
            detail.put("allPrice", allPrice);
            detail.put("taxRate", taxRate);
            detail.put("taxMoney", taxMoney);
            detail.put("taxLastMoney", taxLastMoney);
            detail.put("taxUnitPrice", taxLastMoney.divide(quantity, 4, BigDecimal.ROUND_HALF_UP));
            detailArray.set(detailIndex, detail);
            totalPrice = totalPrice.add(allPrice);
            totalTaxLastMoney = totalTaxLastMoney.add(taxLastMoney);
        }
        totalPrice = totalPrice.setScale(2, BigDecimal.ROUND_HALF_UP);
        totalTaxLastMoney = totalTaxLastMoney.setScale(2, BigDecimal.ROUND_HALF_UP);

        BigDecimal discountMoney = depotHead.getDiscountMoney();
        BigDecimal discount = depotHead.getDiscount();
        if (discountMoney != null) {
            if (discountMoney.compareTo(BigDecimal.ZERO) < 0 || discountMoney.compareTo(totalTaxLastMoney) > 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_AMOUNT_CODE,
                        ExceptionConstants.DEPOT_HEAD_PURCHASE_AMOUNT_MSG);
            }
            discountMoney = discountMoney.setScale(2, BigDecimal.ROUND_HALF_UP);
            discount = totalTaxLastMoney.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                    : discountMoney.multiply(new BigDecimal("100"))
                    .divide(totalTaxLastMoney, 2, BigDecimal.ROUND_HALF_UP);
        } else {
            discount = discount == null ? BigDecimal.ZERO : discount;
            if (discount.compareTo(BigDecimal.ZERO) < 0 || discount.compareTo(new BigDecimal("100")) > 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_AMOUNT_CODE,
                        ExceptionConstants.DEPOT_HEAD_PURCHASE_AMOUNT_MSG);
            }
            discountMoney = totalTaxLastMoney.multiply(discount).movePointLeft(2)
                    .setScale(2, BigDecimal.ROUND_HALF_UP);
        }
        BigDecimal discountLastMoney = totalTaxLastMoney.subtract(discountMoney)
                .setScale(2, BigDecimal.ROUND_HALF_UP);
        depotHead.setTotalPrice(totalPrice.negate());
        depotHead.setDiscount(discount);
        depotHead.setDiscountMoney(discountMoney);
        depotHead.setDiscountLastMoney(discountLastMoney);
        depotHead.setBackAmount(BigDecimal.ZERO);

        BigDecimal payment = depotHead.getChangeAmount() == null
                ? BigDecimal.ZERO : depotHead.getChangeAmount().abs().setScale(2, BigDecimal.ROUND_HALF_UP);
        if (purchaseOrder) {
            if (payment.compareTo(discountLastMoney) > 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_AMOUNT_CODE,
                        ExceptionConstants.DEPOT_HEAD_PURCHASE_AMOUNT_MSG);
            }
            if (payment.compareTo(BigDecimal.ZERO) > 0
                    && depotHead.getAccountId() == null && StringUtil.isEmpty(depotHead.getAccountIdList())) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ACCOUNT_FAILED_CODE,
                        ExceptionConstants.DEPOT_HEAD_ACCOUNT_FAILED_MSG);
            }
            depotHead.setOtherMoney(BigDecimal.ZERO);
            depotHead.setDeposit(BigDecimal.ZERO);
            depotHead.setDebt(BigDecimal.ZERO);
        } else {
            BigDecimal otherMoney = depotHead.getOtherMoney() == null ? BigDecimal.ZERO : depotHead.getOtherMoney();
            BigDecimal deposit = depotHead.getDeposit() == null ? BigDecimal.ZERO : depotHead.getDeposit();
            if (otherMoney.compareTo(BigDecimal.ZERO) < 0 || deposit.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_AMOUNT_CODE,
                        ExceptionConstants.DEPOT_HEAD_PURCHASE_AMOUNT_MSG);
            }
            if (StringUtil.isEmpty(depotHead.getLinkNumber()) && deposit.compareTo(BigDecimal.ZERO) > 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_IN_DEPOSIT_SOURCE_CODE,
                        ExceptionConstants.DEPOT_HEAD_PURCHASE_IN_DEPOSIT_SOURCE_MSG);
            }
            BigDecimal payable = discountLastMoney.add(otherMoney).subtract(deposit)
                    .setScale(2, BigDecimal.ROUND_HALF_UP);
            if (payable.compareTo(BigDecimal.ZERO) < 0 || payment.compareTo(payable) > 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_AMOUNT_CODE,
                        ExceptionConstants.DEPOT_HEAD_PURCHASE_AMOUNT_MSG);
            }
            depotHead.setOtherMoney(otherMoney);
            depotHead.setDeposit(deposit);
            depotHead.setDebt(payable.subtract(payment).setScale(2, BigDecimal.ROUND_HALF_UP));
        }
        depotHead.setChangeAmount(payment.negate());
        return detailArray.toJSONString();
    }

    private void validateSalesCustomer(DepotHead depotHead) throws Exception {
        Supplier customer = depotHead.getOrganId() == null ? null : supplierService.getSupplier(depotHead.getOrganId());
        if (customer == null || customer.getId() == null
                || !"客户".equals(customer.getType())
                || !Boolean.TRUE.equals(customer.getEnabled())
                || BusinessConstants.DELETE_FLAG_DELETED.equals(customer.getDeleteFlag())) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_SALES_CUSTOMER_CODE,
                    ExceptionConstants.DEPOT_HEAD_SALES_CUSTOMER_MSG);
        }
        checkSalesCustomerPermission(depotHead.getOrganId());
    }

    private void validateSalesSettlementAccounts(DepotHead depotHead, boolean accountRequired) throws Exception {
        String accountIdList = normalizeListValue(depotHead.getAccountIdList());
        String accountMoneyList = normalizeListValue(depotHead.getAccountMoneyList());
        depotHead.setAccountIdList(StringUtil.isEmpty(accountIdList) ? null : accountIdList);
        depotHead.setAccountMoneyList(StringUtil.isEmpty(accountMoneyList) ? null : accountMoneyList);

        if (StringUtil.isNotEmpty(accountIdList)) {
            if (StringUtil.isEmpty(accountMoneyList)) {
                throwSalesAccountInvalid();
            }
            String[] idArray = accountIdList.split(",", -1);
            String[] moneyArray = accountMoneyList.split(",", -1);
            if (idArray.length != moneyArray.length) {
                throwSalesAccountInvalid();
            }
            Set<Long> accountIds = new HashSet<>();
            for (int index = 0; index < idArray.length; index++) {
                try {
                    Long accountId = Long.valueOf(idArray[index].trim());
                    if (!accountIds.add(accountId)) {
                        throwSalesAccountInvalid();
                    }
                    validateEnabledAccount(accountId);
                    BigDecimal accountMoney = new BigDecimal(moneyArray[index].trim());
                    if (accountMoney.compareTo(BigDecimal.ZERO) < 0) {
                        throwSalesAccountInvalid();
                    }
                } catch (NumberFormatException exception) {
                    throwSalesAccountInvalid();
                }
            }
            // 多账户结算以列表为准，避免同一笔款项同时落入单账户和多账户。
            depotHead.setAccountId(null);
        } else {
            if (StringUtil.isNotEmpty(accountMoneyList)) {
                throwSalesAccountInvalid();
            }
            if (depotHead.getAccountId() != null) {
                validateEnabledAccount(depotHead.getAccountId());
            } else if (accountRequired) {
                throwSalesAccountInvalid();
            }
        }
    }

    private void validateSalesReturnSettlementAccounts(DepotHead depotHead, boolean accountRequired) throws Exception {
        String accountIdList = normalizeListValue(depotHead.getAccountIdList());
        String accountMoneyList = normalizeListValue(depotHead.getAccountMoneyList());
        depotHead.setAccountIdList(StringUtil.isEmpty(accountIdList) ? null : accountIdList);
        depotHead.setAccountMoneyList(StringUtil.isEmpty(accountMoneyList) ? null : accountMoneyList);
        BigDecimal refund = depotHead.getChangeAmount() == null
                ? BigDecimal.ZERO : depotHead.getChangeAmount().abs().setScale(2, BigDecimal.ROUND_HALF_UP);

        if (StringUtil.isNotEmpty(accountIdList)) {
            if (StringUtil.isEmpty(accountMoneyList)) {
                throwSalesAccountInvalid();
            }
            String[] idArray = accountIdList.split(",", -1);
            String[] moneyArray = accountMoneyList.split(",", -1);
            if (idArray.length != moneyArray.length) {
                throwSalesAccountInvalid();
            }
            Set<Long> accountIds = new HashSet<>();
            BigDecimal accountMoneyTotal = BigDecimal.ZERO;
            for (int index = 0; index < idArray.length; index++) {
                try {
                    Long accountId = Long.valueOf(idArray[index].trim());
                    if (!accountIds.add(accountId)) {
                        throwSalesAccountInvalid();
                    }
                    validateEnabledAccount(accountId);
                    BigDecimal accountMoney = new BigDecimal(moneyArray[index].trim());
                    if (accountMoney.compareTo(BigDecimal.ZERO) > 0) {
                        throwSalesAccountInvalid();
                    }
                    accountMoneyTotal = accountMoneyTotal.add(accountMoney.abs());
                } catch (NumberFormatException exception) {
                    throwSalesAccountInvalid();
                }
            }
            if (accountMoneyTotal.setScale(2, BigDecimal.ROUND_HALF_UP).compareTo(refund) != 0) {
                throwSalesAccountInvalid();
            }
            depotHead.setAccountId(null);
        } else {
            if (StringUtil.isNotEmpty(accountMoneyList)) {
                throwSalesAccountInvalid();
            }
            if (depotHead.getAccountId() != null) {
                validateEnabledAccount(depotHead.getAccountId());
            } else if (accountRequired) {
                throwSalesAccountInvalid();
            }
        }
    }

    private String normalizeListValue(String value) {
        return value == null ? null : value.replace("[", "").replace("]", "")
                .replace("\"", "").replace(" ", "").trim();
    }

    private void validateEnabledAccount(Long accountId) throws Exception {
        Account account = accountId == null ? null : accountService.getAccount(accountId);
        if (account == null || !Boolean.TRUE.equals(account.getEnabled())
                || BusinessConstants.DELETE_FLAG_DELETED.equals(account.getDeleteFlag())) {
            throwSalesAccountInvalid();
        }
    }

    private void throwSalesAccountInvalid() {
        throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_SALES_ACCOUNT_INVALID_CODE,
                ExceptionConstants.DEPOT_HEAD_SALES_ACCOUNT_INVALID_MSG);
    }

    private void checkSalesCustomerPermission(Long customerId) throws Exception {
        if (isCurrentUserAdmin() || !systemConfigService.getCustomerFlag()) {
            return;
        }
        User currentUser = userService.getCurrentUser();
        String customerPermission = currentUser == null ? null
                : userBusinessService.getUBValueByTypeAndKeyId("UserCustomer", currentUser.getId().toString());
        if (customerId == null || StringUtil.isEmpty(customerPermission)
                || !customerPermission.contains("[" + customerId + "]")) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_SALES_DATA_PERMISSION_CODE,
                    ExceptionConstants.DEPOT_HEAD_SALES_DATA_PERMISSION_MSG);
        }
    }

    private String clearSalesRowLinks(String rows) {
        JSONArray detailArray = JSONArray.parseArray(rows);
        if (detailArray == null || detailArray.isEmpty()) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ROW_FAILED_CODE,
                    ExceptionConstants.DEPOT_HEAD_ROW_FAILED_MSG);
        }
        for (int detailIndex = 0; detailIndex < detailArray.size(); detailIndex++) {
            JSONObject detail = JSONObject.parseObject(detailArray.get(detailIndex).toString());
            detail.remove("linkId");
            detail.remove("preNumber");
            detail.remove("finishNumber");
            detailArray.set(detailIndex, detail);
        }
        return detailArray.toJSONString();
    }

    private String validateAndNormalizeSalesOutboundSource(DepotHead depotHead, String rows,
                                                            DepotHead previousDepotHead) throws Exception {
        DepotHead sourceHead = depotHeadMapperEx.lockDepotHeadByNumber(depotHead.getLinkNumber());
        boolean sourceStatusValid = sourceHead != null
                && (BusinessConstants.BILLS_STATUS_AUDIT.equals(sourceHead.getStatus())
                || BusinessConstants.BILLS_STATUS_SKIPING.equals(sourceHead.getStatus()));
        if (sourceHead == null || sourceHead.getId() == null || !isSalesOrder(sourceHead)
                || !sourceStatusValid || !Objects.equals(sourceHead.getOrganId(), depotHead.getOrganId())) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_SALES_OUT_SOURCE_CODE,
                    ExceptionConstants.DEPOT_HEAD_SALES_OUT_SOURCE_MSG);
        }
        checkPurchaseBillDataPermission(sourceHead);

        JSONArray detailArray = JSONArray.parseArray(rows);
        if (detailArray == null || detailArray.isEmpty()) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ROW_FAILED_CODE,
                    ExceptionConstants.DEPOT_HEAD_ROW_FAILED_MSG);
        }
        Long currentHeaderId = previousDepotHead == null ? null : previousDepotHead.getId();
        Map<Long, BigDecimal> currentShippedBasicMap = new HashMap<>();
        for (int detailIndex = 0; detailIndex < detailArray.size(); detailIndex++) {
            JSONObject detail = JSONObject.parseObject(detailArray.get(detailIndex).toString());
            String barCode = detail.getString("barCode");
            BigDecimal quantity = detail.getBigDecimal("operNumber");
            Long linkId = detail.getLong("linkId");
            DepotItem sourceItem = linkId == null ? null : depotItemMapperEx.lockDepotItemById(linkId);
            MaterialExtend materialExtend = StringUtil.isEmpty(barCode)
                    ? null : materialExtendService.getInfoByBarCode(barCode);
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0
                    || sourceItem == null || !Objects.equals(sourceItem.getHeaderId(), sourceHead.getId())
                    || materialExtend == null
                    || !Objects.equals(materialExtend.getId(), sourceItem.getMaterialExtendId())
                    || sourceItem.getOperNumber() == null
                    || sourceItem.getOperNumber().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_SALES_OUT_DETAIL_CODE,
                        ExceptionConstants.DEPOT_HEAD_SALES_OUT_DETAIL_MSG);
            }

            BigDecimal sourceBasicNumber = sourceItem.getBasicNumber() == null
                    ? sourceItem.getOperNumber() : sourceItem.getBasicNumber();
            BigDecimal submittedBasicNumber = quantity.multiply(sourceBasicNumber)
                    .divide(sourceItem.getOperNumber(), 6, BigDecimal.ROUND_HALF_UP);
            BigDecimal alreadyShipped = depotItemMapperEx.getSalesOutboundShippedBasicNumber(linkId, currentHeaderId);
            BigDecimal currentShipped = currentShippedBasicMap.getOrDefault(linkId, BigDecimal.ZERO)
                    .add(submittedBasicNumber);
            currentShippedBasicMap.put(linkId, currentShipped);
            if (!systemConfigService.getOverLinkBillFlag()
                    && alreadyShipped.add(currentShipped).compareTo(sourceBasicNumber) > 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_SALES_OUT_OVER_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_SALES_OUT_OVER_MSG, barCode));
            }

            BigDecimal finishNumber = sourceBasicNumber.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                    : alreadyShipped.multiply(sourceItem.getOperNumber())
                    .divide(sourceBasicNumber, 6, BigDecimal.ROUND_HALF_UP);
            detail.put("linkId", sourceItem.getId());
            detail.put("unit", sourceItem.getMaterialUnit());
            detail.put("unitPrice", sourceItem.getUnitPrice());
            detail.put("taxRate", sourceItem.getTaxRate() == null ? BigDecimal.ZERO : sourceItem.getTaxRate());
            detail.put("preNumber", sourceItem.getOperNumber());
            detail.put("finishNumber", finishNumber);
            detailArray.set(detailIndex, detail);
        }
        return detailArray.toJSONString();
    }

    /**
     * 草稿保存后源销售订单可能发生状态或内容变化，审核时必须基于数据库中的最新数据再次校验。
     */
    private void validateSalesOutboundBeforeAudit(DepotHead depotHead) throws Exception {
        if (!isSalesOutbound(depotHead)) {
            return;
        }
        validateSalesCustomer(depotHead);
        validateSalesSettlementAccounts(depotHead, true);
        if (StringUtil.isEmpty(depotHead.getLinkNumber())) {
            return;
        }
        JSONArray detailArray = new JSONArray();
        List<DepotItem> detailList = depotItemService.getListByHeaderId(depotHead.getId());
        for (DepotItem depotItem : detailList) {
            MaterialExtend materialExtend = depotItem.getMaterialExtendId() == null ? null
                    : materialExtendService.getMaterialExtend(depotItem.getMaterialExtendId());
            JSONObject detail = new JSONObject();
            detail.put("barCode", materialExtend == null ? null : materialExtend.getBarCode());
            detail.put("operNumber", depotItem.getOperNumber());
            detail.put("linkId", depotItem.getLinkId());
            detailArray.add(detail);
        }
        validateAndNormalizeSalesOutboundSource(depotHead, detailArray.toJSONString(), depotHead);

        BigDecimal deposit = depotHead.getDeposit() == null ? BigDecimal.ZERO : depotHead.getDeposit();
        DepotHead sourceHead = getDepotHead(depotHead.getLinkNumber());
        BigDecimal orderDeposit = sourceHead == null || sourceHead.getChangeAmount() == null
                ? BigDecimal.ZERO : sourceHead.getChangeAmount().abs();
        BigDecimal usedDeposit = depotHeadMapperEx.getFinishDepositByNumberExceptCurrent(
                depotHead.getLinkNumber(), depotHead.getNumber());
        if (deposit.add(usedDeposit).compareTo(orderDeposit) > 0) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_DEPOSIT_OVER_PRE_CODE,
                    ExceptionConstants.DEPOT_HEAD_DEPOSIT_OVER_PRE_MSG);
        }
    }

    /**
     * 销售退货草稿审核前，按数据库中的最新销售出库、累计退货和结算数据重新校验。
     */
    private void validateSalesReturnBeforeAudit(DepotHead depotHead) throws Exception {
        if (!isSalesReturn(depotHead)) {
            return;
        }
        validateSalesCustomer(depotHead);
        JSONArray detailArray = new JSONArray();
        List<DepotItem> detailList = depotItemService.getListByHeaderId(depotHead.getId());
        for (DepotItem depotItem : detailList) {
            MaterialExtend materialExtend = depotItem.getMaterialExtendId() == null ? null
                    : materialExtendService.getMaterialExtend(depotItem.getMaterialExtendId());
            JSONObject detail = new JSONObject();
            detail.put("barCode", materialExtend == null ? null : materialExtend.getBarCode());
            detail.put("unit", depotItem.getMaterialUnit());
            detail.put("depotId", depotItem.getDepotId());
            detail.put("operNumber", depotItem.getOperNumber());
            detail.put("unitPrice", depotItem.getUnitPrice());
            detail.put("taxRate", depotItem.getTaxRate());
            detail.put("linkId", depotItem.getLinkId());
            detail.put("snList", depotItem.getSnList());
            detailArray.add(detail);
        }
        validateAndNormalizeSalesReturn(depotHead, detailArray.toJSONString(), depotHead);
        validateSalesReturnSettlementAccounts(depotHead, true);
    }

    private String normalizeSalesFinancialFields(DepotHead depotHead, String rows, boolean salesOrder)
            throws Exception {
        JSONArray detailArray = JSONArray.parseArray(rows);
        if (detailArray == null || detailArray.isEmpty()) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ROW_FAILED_CODE,
                    ExceptionConstants.DEPOT_HEAD_ROW_FAILED_MSG);
        }
        boolean priceIncludesTax = systemConfigService.getMaterialPriceTaxFlag();
        BigDecimal totalPrice = BigDecimal.ZERO;
        BigDecimal totalTaxLastMoney = BigDecimal.ZERO;
        for (int detailIndex = 0; detailIndex < detailArray.size(); detailIndex++) {
            JSONObject detail = JSONObject.parseObject(detailArray.get(detailIndex).toString());
            String barCode = detail.getString("barCode");
            BigDecimal quantity = detail.getBigDecimal("operNumber");
            BigDecimal unitPrice = detail.getBigDecimal("unitPrice");
            BigDecimal taxRate = detail.getBigDecimal("taxRate");
            taxRate = taxRate == null ? BigDecimal.ZERO : taxRate;
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_MSG, barCode));
            }
            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0
                    || taxRate.compareTo(BigDecimal.ZERO) < 0 || taxRate.compareTo(new BigDecimal("100")) > 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_SALES_AMOUNT_CODE,
                        ExceptionConstants.DEPOT_HEAD_SALES_AMOUNT_MSG);
            }
            BigDecimal allPrice = quantity.multiply(unitPrice).setScale(2, BigDecimal.ROUND_HALF_UP);
            BigDecimal taxMoney;
            BigDecimal taxLastMoney;
            if (priceIncludesTax) {
                BigDecimal divisor = BigDecimal.ONE.add(taxRate.movePointLeft(2));
                BigDecimal realAllPrice = allPrice.divide(divisor, 2, BigDecimal.ROUND_HALF_UP);
                taxMoney = realAllPrice.multiply(taxRate).movePointLeft(2)
                        .setScale(2, BigDecimal.ROUND_HALF_UP);
                taxLastMoney = allPrice;
            } else {
                taxMoney = allPrice.multiply(taxRate).movePointLeft(2)
                        .setScale(2, BigDecimal.ROUND_HALF_UP);
                taxLastMoney = allPrice.add(taxMoney).setScale(2, BigDecimal.ROUND_HALF_UP);
            }
            detail.put("allPrice", allPrice);
            detail.put("taxRate", taxRate);
            detail.put("taxMoney", taxMoney);
            detail.put("taxLastMoney", taxLastMoney);
            detail.put("taxUnitPrice", taxLastMoney.divide(quantity, 4, BigDecimal.ROUND_HALF_UP));
            detailArray.set(detailIndex, detail);
            totalPrice = totalPrice.add(allPrice);
            totalTaxLastMoney = totalTaxLastMoney.add(taxLastMoney);
        }
        totalPrice = totalPrice.setScale(2, BigDecimal.ROUND_HALF_UP);
        totalTaxLastMoney = totalTaxLastMoney.setScale(2, BigDecimal.ROUND_HALF_UP);

        BigDecimal discountMoney = depotHead.getDiscountMoney();
        BigDecimal discount = depotHead.getDiscount();
        if (discountMoney != null) {
            if (discountMoney.compareTo(BigDecimal.ZERO) < 0 || discountMoney.compareTo(totalTaxLastMoney) > 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_SALES_AMOUNT_CODE,
                        ExceptionConstants.DEPOT_HEAD_SALES_AMOUNT_MSG);
            }
            discountMoney = discountMoney.setScale(2, BigDecimal.ROUND_HALF_UP);
            discount = totalTaxLastMoney.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                    : discountMoney.multiply(new BigDecimal("100"))
                    .divide(totalTaxLastMoney, 2, BigDecimal.ROUND_HALF_UP);
        } else {
            discount = discount == null ? BigDecimal.ZERO : discount;
            if (discount.compareTo(BigDecimal.ZERO) < 0 || discount.compareTo(new BigDecimal("100")) > 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_SALES_AMOUNT_CODE,
                        ExceptionConstants.DEPOT_HEAD_SALES_AMOUNT_MSG);
            }
            discountMoney = totalTaxLastMoney.multiply(discount).movePointLeft(2)
                    .setScale(2, BigDecimal.ROUND_HALF_UP);
        }
        BigDecimal discountLastMoney = totalTaxLastMoney.subtract(discountMoney)
                .setScale(2, BigDecimal.ROUND_HALF_UP);
        BigDecimal received = depotHead.getChangeAmount() == null
                ? BigDecimal.ZERO : depotHead.getChangeAmount();
        if (received.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_SALES_AMOUNT_CODE,
                    ExceptionConstants.DEPOT_HEAD_SALES_AMOUNT_MSG);
        }
        received = received.setScale(2, BigDecimal.ROUND_HALF_UP);
        depotHead.setTotalPrice(totalPrice);
        depotHead.setDiscount(discount);
        depotHead.setDiscountMoney(discountMoney);
        depotHead.setDiscountLastMoney(discountLastMoney);
        depotHead.setBackAmount(BigDecimal.ZERO);

        if (salesOrder) {
            if (received.compareTo(discountLastMoney) > 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_SALES_AMOUNT_CODE,
                        ExceptionConstants.DEPOT_HEAD_SALES_AMOUNT_MSG);
            }
            if (received.compareTo(BigDecimal.ZERO) > 0
                    && depotHead.getAccountId() == null && StringUtil.isEmpty(depotHead.getAccountIdList())) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ACCOUNT_FAILED_CODE,
                        ExceptionConstants.DEPOT_HEAD_ACCOUNT_FAILED_MSG);
            }
            depotHead.setOtherMoney(BigDecimal.ZERO);
            depotHead.setDeposit(BigDecimal.ZERO);
            depotHead.setDebt(BigDecimal.ZERO);
        } else {
            BigDecimal otherMoney = depotHead.getOtherMoney() == null ? BigDecimal.ZERO : depotHead.getOtherMoney();
            BigDecimal deposit = depotHead.getDeposit() == null ? BigDecimal.ZERO : depotHead.getDeposit();
            if (otherMoney.compareTo(BigDecimal.ZERO) < 0 || deposit.compareTo(BigDecimal.ZERO) < 0
                    || (StringUtil.isEmpty(depotHead.getLinkNumber()) && deposit.compareTo(BigDecimal.ZERO) > 0)) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_SALES_AMOUNT_CODE,
                        ExceptionConstants.DEPOT_HEAD_SALES_AMOUNT_MSG);
            }
            BigDecimal receivable = discountLastMoney.add(otherMoney).subtract(deposit)
                    .setScale(2, BigDecimal.ROUND_HALF_UP);
            if (receivable.compareTo(BigDecimal.ZERO) < 0 || received.compareTo(receivable) > 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_SALES_AMOUNT_CODE,
                        ExceptionConstants.DEPOT_HEAD_SALES_AMOUNT_MSG);
            }
            depotHead.setOtherMoney(otherMoney);
            depotHead.setDeposit(deposit);
            depotHead.setDebt(receivable.subtract(received).setScale(2, BigDecimal.ROUND_HALF_UP));
        }
        depotHead.setChangeAmount(received);
        return detailArray.toJSONString();
    }

    private String validateAndNormalizeSalesReturn(DepotHead depotHead, String rows,
                                                    DepotHead previousDepotHead) throws Exception {
        DepotHead sourceHead = null;
        if (StringUtil.isNotEmpty(depotHead.getLinkNumber())) {
            sourceHead = depotHeadMapperEx.lockDepotHeadByNumber(depotHead.getLinkNumber());
            boolean sourceStatusValid = sourceHead != null
                    && (BusinessConstants.BILLS_STATUS_AUDIT.equals(sourceHead.getStatus())
                    || BusinessConstants.BILLS_STATUS_SKIPED.equals(sourceHead.getStatus())
                    || BusinessConstants.BILLS_STATUS_SKIPING.equals(sourceHead.getStatus()));
            if (sourceHead == null || sourceHead.getId() == null || !isSalesOutbound(sourceHead)
                    || !sourceStatusValid || !Objects.equals(sourceHead.getOrganId(), depotHead.getOrganId())) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_SALES_RETURN_SOURCE_CODE,
                        ExceptionConstants.DEPOT_HEAD_SALES_RETURN_SOURCE_MSG);
            }
            checkPurchaseBillDataPermission(sourceHead);
        } else {
            rows = clearSalesRowLinks(rows);
        }

        JSONArray detailArray = JSONArray.parseArray(rows);
        if (detailArray == null || detailArray.isEmpty()) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ROW_FAILED_CODE,
                    ExceptionConstants.DEPOT_HEAD_ROW_FAILED_MSG);
        }
        boolean priceIncludesTax = systemConfigService.getMaterialPriceTaxFlag();
        Long currentHeaderId = previousDepotHead == null ? null : previousDepotHead.getId();
        Map<Long, BigDecimal> currentReturnedBasicMap = new HashMap<>();
        BigDecimal totalPrice = BigDecimal.ZERO;
        BigDecimal totalTaxLastMoney = BigDecimal.ZERO;
        for (int detailIndex = 0; detailIndex < detailArray.size(); detailIndex++) {
            JSONObject detail = JSONObject.parseObject(detailArray.get(detailIndex).toString());
            String barCode = detail.getString("barCode");
            MaterialExtend materialExtend = StringUtil.isEmpty(barCode)
                    ? null : materialExtendService.getInfoByBarCode(barCode);
            BigDecimal quantity = detail.getBigDecimal("operNumber");
            if (materialExtend == null) {
                throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_BARCODE_IS_NOT_EXIST_CODE,
                        String.format(ExceptionConstants.MATERIAL_BARCODE_IS_NOT_EXIST_MSG, barCode));
            }
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_MSG, barCode));
            }

            BigDecimal unitPrice;
            BigDecimal taxRate;
            if (sourceHead != null) {
                Long linkId = detail.getLong("linkId");
                DepotItem sourceItem = linkId == null ? null : depotItemMapperEx.lockDepotItemById(linkId);
                if (sourceItem == null || !Objects.equals(sourceItem.getHeaderId(), sourceHead.getId())
                        || !Objects.equals(sourceItem.getMaterialExtendId(), materialExtend.getId())
                        || sourceItem.getOperNumber() == null
                        || sourceItem.getOperNumber().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_SALES_RETURN_DETAIL_CODE,
                            ExceptionConstants.DEPOT_HEAD_SALES_RETURN_DETAIL_MSG);
                }
                BigDecimal sourceBasicNumber = sourceItem.getBasicNumber() == null
                        ? sourceItem.getOperNumber() : sourceItem.getBasicNumber();
                if (sourceBasicNumber.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_SALES_RETURN_DETAIL_CODE,
                            ExceptionConstants.DEPOT_HEAD_SALES_RETURN_DETAIL_MSG);
                }
                BigDecimal unitRatio = sourceBasicNumber.divide(sourceItem.getOperNumber(), 12,
                        BigDecimal.ROUND_HALF_UP);
                BigDecimal requestedBasicNumber = quantity.multiply(unitRatio);
                BigDecimal alreadyReturnedBasicNumber = depotItemMapperEx
                        .getSalesReturnReturnedBasicNumber(linkId, currentHeaderId);
                BigDecimal currentReturnedBasicNumber = currentReturnedBasicMap
                        .getOrDefault(linkId, BigDecimal.ZERO).add(requestedBasicNumber);
                currentReturnedBasicMap.put(linkId, currentReturnedBasicNumber);
                if (!systemConfigService.getOverLinkBillFlag()
                        && alreadyReturnedBasicNumber.add(currentReturnedBasicNumber)
                        .compareTo(sourceBasicNumber) > 0) {
                    throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_SALES_RETURN_OVER_CODE,
                            String.format(ExceptionConstants.DEPOT_HEAD_SALES_RETURN_OVER_MSG, barCode));
                }
                validateSalesReturnSerialNumber(detail.getString("snList"), sourceItem.getSnList());
                detail.put("linkId", sourceItem.getId());
                detail.put("unit", sourceItem.getMaterialUnit());
                detail.put("batchNumber", sourceItem.getBatchNumber());
                detail.put("expirationDate", sourceItem.getExpirationDate());
                detail.put("preNumber", sourceItem.getOperNumber());
                detail.put("finishNumber", alreadyReturnedBasicNumber.divide(unitRatio, 6,
                        BigDecimal.ROUND_HALF_UP));
                unitPrice = sourceItem.getUnitPrice() == null ? BigDecimal.ZERO : sourceItem.getUnitPrice();
                taxRate = sourceItem.getTaxRate() == null ? BigDecimal.ZERO : sourceItem.getTaxRate();
            } else {
                unitPrice = detail.getBigDecimal("unitPrice");
                taxRate = detail.getBigDecimal("taxRate");
                taxRate = taxRate == null ? BigDecimal.ZERO : taxRate;
            }
            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0
                    || taxRate.compareTo(BigDecimal.ZERO) < 0
                    || taxRate.compareTo(new BigDecimal("100")) > 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_SALES_AMOUNT_CODE,
                        ExceptionConstants.DEPOT_HEAD_SALES_AMOUNT_MSG);
            }

            BigDecimal allPrice = quantity.multiply(unitPrice).setScale(2, BigDecimal.ROUND_HALF_UP);
            BigDecimal taxMoney;
            BigDecimal taxLastMoney;
            if (priceIncludesTax) {
                BigDecimal realAllPrice = allPrice.divide(BigDecimal.ONE.add(taxRate.movePointLeft(2)), 2,
                        BigDecimal.ROUND_HALF_UP);
                taxMoney = realAllPrice.multiply(taxRate).movePointLeft(2)
                        .setScale(2, BigDecimal.ROUND_HALF_UP);
                taxLastMoney = allPrice;
            } else {
                taxMoney = allPrice.multiply(taxRate).movePointLeft(2)
                        .setScale(2, BigDecimal.ROUND_HALF_UP);
                taxLastMoney = allPrice.add(taxMoney).setScale(2, BigDecimal.ROUND_HALF_UP);
            }
            detail.put("unitPrice", unitPrice);
            detail.put("allPrice", allPrice);
            detail.put("taxRate", taxRate);
            detail.put("taxMoney", taxMoney);
            detail.put("taxLastMoney", taxLastMoney);
            detail.put("taxUnitPrice", taxLastMoney.divide(quantity, 4, BigDecimal.ROUND_HALF_UP));
            detailArray.set(detailIndex, detail);
            totalPrice = totalPrice.add(allPrice);
            totalTaxLastMoney = totalTaxLastMoney.add(taxLastMoney);
        }

        totalPrice = totalPrice.setScale(2, BigDecimal.ROUND_HALF_UP);
        totalTaxLastMoney = totalTaxLastMoney.setScale(2, BigDecimal.ROUND_HALF_UP);
        BigDecimal discount;
        BigDecimal discountMoney;
        if (sourceHead != null) {
            discount = sourceHead.getDiscount() == null ? BigDecimal.ZERO : sourceHead.getDiscount();
            discountMoney = totalTaxLastMoney.multiply(discount).movePointLeft(2)
                    .setScale(2, BigDecimal.ROUND_HALF_UP);
        } else if (depotHead.getDiscountMoney() != null) {
            discountMoney = depotHead.getDiscountMoney();
            if (discountMoney.compareTo(BigDecimal.ZERO) < 0
                    || discountMoney.compareTo(totalTaxLastMoney) > 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_SALES_AMOUNT_CODE,
                        ExceptionConstants.DEPOT_HEAD_SALES_AMOUNT_MSG);
            }
            discountMoney = discountMoney.setScale(2, BigDecimal.ROUND_HALF_UP);
            discount = totalTaxLastMoney.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                    : discountMoney.multiply(new BigDecimal("100"))
                    .divide(totalTaxLastMoney, 2, BigDecimal.ROUND_HALF_UP);
        } else {
            discount = depotHead.getDiscount() == null ? BigDecimal.ZERO : depotHead.getDiscount();
            if (discount.compareTo(BigDecimal.ZERO) < 0
                    || discount.compareTo(new BigDecimal("100")) > 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_SALES_AMOUNT_CODE,
                        ExceptionConstants.DEPOT_HEAD_SALES_AMOUNT_MSG);
            }
            discountMoney = totalTaxLastMoney.multiply(discount).movePointLeft(2)
                    .setScale(2, BigDecimal.ROUND_HALF_UP);
        }
        if (discount.compareTo(BigDecimal.ZERO) < 0 || discount.compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_SALES_AMOUNT_CODE,
                    ExceptionConstants.DEPOT_HEAD_SALES_AMOUNT_MSG);
        }
        BigDecimal discountLastMoney = totalTaxLastMoney.subtract(discountMoney)
                .setScale(2, BigDecimal.ROUND_HALF_UP);
        BigDecimal otherMoney = depotHead.getOtherMoney() == null
                ? BigDecimal.ZERO : depotHead.getOtherMoney();
        BigDecimal refund = depotHead.getChangeAmount() == null
                ? BigDecimal.ZERO : depotHead.getChangeAmount().abs().setScale(2, BigDecimal.ROUND_HALF_UP);
        BigDecimal refundable = discountLastMoney.add(otherMoney).setScale(2, BigDecimal.ROUND_HALF_UP);
        if (otherMoney.compareTo(BigDecimal.ZERO) < 0 || refundable.compareTo(BigDecimal.ZERO) < 0
                || refund.compareTo(refundable) > 0) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_SALES_AMOUNT_CODE,
                    ExceptionConstants.DEPOT_HEAD_SALES_AMOUNT_MSG);
        }
        depotHead.setTotalPrice(totalPrice.negate());
        depotHead.setDiscount(discount);
        depotHead.setDiscountMoney(discountMoney);
        depotHead.setDiscountLastMoney(discountLastMoney);
        depotHead.setOtherMoney(otherMoney);
        depotHead.setDeposit(BigDecimal.ZERO);
        depotHead.setChangeAmount(refund.negate());
        depotHead.setDebt(refundable.subtract(refund).setScale(2, BigDecimal.ROUND_HALF_UP));
        depotHead.setBackAmount(BigDecimal.ZERO);
        return detailArray.toJSONString();
    }

    private void validateSalesReturnSerialNumber(String returnSnList, String sourceSnList) {
        if (StringUtil.isEmpty(returnSnList)) {
            return;
        }
        Set<String> sourceSnSet = StringUtil.isEmpty(sourceSnList) ? Collections.emptySet()
                : Arrays.stream(sourceSnList.split(","))
                .map(String::trim).filter(StringUtil::isNotEmpty).collect(Collectors.toSet());
        for (String serialNumber : returnSnList.split(",")) {
            String value = serialNumber.trim();
            if (StringUtil.isNotEmpty(value) && !sourceSnSet.contains(value)) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_SALES_RETURN_SERIAL_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_SALES_RETURN_SERIAL_MSG, value));
            }
        }
    }

    private String validateAndNormalizeRetailOut(DepotHead depotHead, JSONObject headJson, String rows) {
        JSONArray detailArray = JSONArray.parseArray(rows);
        if (detailArray == null || detailArray.isEmpty()) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ROW_FAILED_CODE,
                    ExceptionConstants.DEPOT_HEAD_ROW_FAILED_MSG);
        }
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (int detailIndex = 0; detailIndex < detailArray.size(); detailIndex++) {
            JSONObject detail = JSONObject.parseObject(detailArray.get(detailIndex).toString());
            String barCode = detail.getString("barCode");
            BigDecimal quantity = detail.getBigDecimal("operNumber");
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_MSG, barCode));
            }
            BigDecimal unitPrice = detail.getBigDecimal("unitPrice");
            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_RETAIL_AMOUNT_MISMATCH_CODE,
                        ExceptionConstants.DEPOT_HEAD_RETAIL_AMOUNT_MISMATCH_MSG);
            }
            BigDecimal allPrice = quantity.multiply(unitPrice).setScale(2, BigDecimal.ROUND_HALF_UP);
            detail.put("allPrice", allPrice);
            detailArray.set(detailIndex, detail);
            totalPrice = totalPrice.add(allPrice);
        }
        totalPrice = totalPrice.setScale(2, BigDecimal.ROUND_HALF_UP);
        depotHead.setTotalPrice(totalPrice);
        depotHead.setChangeAmount(totalPrice);

        String payType = StringUtil.isEmpty(depotHead.getPayType())
                ? BusinessConstants.PAY_TYPE_BY_CASH : depotHead.getPayType();
        depotHead.setPayType(payType);
        if (!BusinessConstants.PAY_TYPE_BY_CASH.equals(payType)
                && !BusinessConstants.PAY_TYPE_PREPAID.equals(payType)) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_RETAIL_PAY_TYPE_CODE,
                    ExceptionConstants.DEPOT_HEAD_RETAIL_PAY_TYPE_MSG);
        }
        if (BusinessConstants.PAY_TYPE_PREPAID.equals(payType) && depotHead.getOrganId() == null) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_RETAIL_MEMBER_REQUIRED_CODE,
                    ExceptionConstants.DEPOT_HEAD_RETAIL_MEMBER_REQUIRED_MSG);
        }
        if (depotHead.getAccountId() == null && StringUtil.isEmpty(depotHead.getAccountIdList())) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ACCOUNT_FAILED_CODE,
                    ExceptionConstants.DEPOT_HEAD_ACCOUNT_FAILED_MSG);
        }

        BigDecimal getAmount = headJson.getBigDecimal("getAmount");
        if (getAmount != null) {
            if (getAmount.compareTo(totalPrice) < 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_RETAIL_RECEIPT_LACK_CODE,
                        ExceptionConstants.DEPOT_HEAD_RETAIL_RECEIPT_LACK_MSG);
            }
            depotHead.setBackAmount(getAmount.subtract(totalPrice).setScale(2, BigDecimal.ROUND_HALF_UP));
        } else if (depotHead.getBackAmount() != null && depotHead.getBackAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_RETAIL_RECEIPT_LACK_CODE,
                    ExceptionConstants.DEPOT_HEAD_RETAIL_RECEIPT_LACK_MSG);
        } else if (depotHead.getBackAmount() == null) {
            depotHead.setBackAmount(BigDecimal.ZERO);
        }
        return detailArray.toJSONString();
    }

    private String validateAndNormalizeRetailReturn(DepotHead depotHead, JSONObject headJson, String rows,
                                                      DepotHead previousDepotHead) throws Exception {
        JSONArray detailArray = JSONArray.parseArray(rows);
        if (detailArray == null || detailArray.isEmpty()) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ROW_FAILED_CODE,
                    ExceptionConstants.DEPOT_HEAD_ROW_FAILED_MSG);
        }

        DepotHead sourceHead = null;
        if (StringUtil.isNotEmpty(depotHead.getLinkNumber())) {
            sourceHead = getDepotHead(depotHead.getLinkNumber());
            if (sourceHead == null || sourceHead.getId() == null
                    || !BusinessConstants.DEPOTHEAD_TYPE_OUT.equals(sourceHead.getType())
                    || !BusinessConstants.SUB_TYPE_RETAIL.equals(sourceHead.getSubType())
                    || !BusinessConstants.BILLS_STATUS_AUDIT.equals(sourceHead.getStatus())) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_RETAIL_RETURN_SOURCE_CODE,
                        ExceptionConstants.DEPOT_HEAD_RETAIL_RETURN_SOURCE_MSG);
            }
            if (!Objects.equals(sourceHead.getOrganId(), depotHead.getOrganId())) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_RETAIL_RETURN_MEMBER_CODE,
                        ExceptionConstants.DEPOT_HEAD_RETAIL_RETURN_MEMBER_MSG);
            }
            depotHead.setPayType(BusinessConstants.PAY_TYPE_PREPAID.equals(sourceHead.getPayType())
                    ? BusinessConstants.PAY_TYPE_PREPAID : BusinessConstants.PAY_TYPE_BY_CASH);
        }

        Long currentHeaderId = previousDepotHead == null ? null : previousDepotHead.getId();
        Map<Long, BigDecimal> currentReturnNumberMap = new HashMap<>();
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (int detailIndex = 0; detailIndex < detailArray.size(); detailIndex++) {
            JSONObject detail = JSONObject.parseObject(detailArray.get(detailIndex).toString());
            String barCode = detail.getString("barCode");
            BigDecimal quantity = detail.getBigDecimal("operNumber");
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_MSG, barCode));
            }

            BigDecimal unitPrice;
            if (sourceHead != null) {
                Long linkId = detail.getLong("linkId");
                DepotItem sourceItem = linkId == null ? null : depotItemMapperEx.lockDepotItemById(linkId);
                MaterialExtend materialExtend = StringUtil.isEmpty(barCode)
                        ? null : materialExtendService.getInfoByBarCode(barCode);
                if (sourceItem == null || !Objects.equals(sourceItem.getHeaderId(), sourceHead.getId())
                        || materialExtend == null
                        || !Objects.equals(materialExtend.getId(), sourceItem.getMaterialExtendId())
                        || sourceItem.getOperNumber() == null
                        || sourceItem.getOperNumber().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_RETAIL_RETURN_DETAIL_CODE,
                            ExceptionConstants.DEPOT_HEAD_RETAIL_RETURN_DETAIL_MSG);
                }

                validateRetailReturnSerialNumber(detail.getString("snList"), sourceItem.getSnList());
                BigDecimal alreadyReturned = depotItemMapperEx.getReturnedOperNumber(linkId, currentHeaderId);
                BigDecimal currentReturned = currentReturnNumberMap.getOrDefault(linkId, BigDecimal.ZERO).add(quantity);
                currentReturnNumberMap.put(linkId, currentReturned);
                if (!systemConfigService.getOverLinkBillFlag()
                        && alreadyReturned.add(currentReturned).compareTo(sourceItem.getOperNumber()) > 0) {
                    throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_RETAIL_RETURN_OVER_CODE,
                            String.format(ExceptionConstants.DEPOT_HEAD_RETAIL_RETURN_OVER_MSG, barCode));
                }

                unitPrice = sourceItem.getUnitPrice() == null ? BigDecimal.ZERO : sourceItem.getUnitPrice();
                detail.put("unitPrice", unitPrice);
                detail.put("unit", sourceItem.getMaterialUnit());
                detail.put("batchNumber", sourceItem.getBatchNumber());
                detail.put("expirationDate", sourceItem.getExpirationDate());
                detail.put("preNumber", sourceItem.getOperNumber());
                detail.put("finishNumber", alreadyReturned);
            } else {
                unitPrice = detail.getBigDecimal("unitPrice");
                if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                    throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_RETAIL_AMOUNT_MISMATCH_CODE,
                            ExceptionConstants.DEPOT_HEAD_RETAIL_AMOUNT_MISMATCH_MSG);
                }
            }

            BigDecimal allPrice = quantity.multiply(unitPrice).setScale(2, BigDecimal.ROUND_HALF_UP);
            detail.put("allPrice", allPrice);
            detailArray.set(detailIndex, detail);
            totalPrice = totalPrice.add(allPrice);
        }

        totalPrice = totalPrice.setScale(2, BigDecimal.ROUND_HALF_UP);
        depotHead.setTotalPrice(totalPrice.negate());
        depotHead.setChangeAmount(totalPrice.negate());
        String payType = StringUtil.isEmpty(depotHead.getPayType())
                ? BusinessConstants.PAY_TYPE_BY_CASH : depotHead.getPayType();
        depotHead.setPayType(payType);
        if (!BusinessConstants.PAY_TYPE_BY_CASH.equals(payType)
                && !BusinessConstants.PAY_TYPE_PREPAID.equals(payType)) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_RETAIL_PAY_TYPE_CODE,
                    ExceptionConstants.DEPOT_HEAD_RETAIL_PAY_TYPE_MSG);
        }
        if (BusinessConstants.PAY_TYPE_PREPAID.equals(payType)) {
            if (depotHead.getOrganId() == null) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_RETAIL_MEMBER_REQUIRED_CODE,
                        ExceptionConstants.DEPOT_HEAD_RETAIL_MEMBER_REQUIRED_MSG);
            }
            depotHead.setAccountId(null);
            depotHead.setAccountIdList(null);
            depotHead.setAccountMoneyList(null);
            depotHead.setBackAmount(BigDecimal.ZERO);
        } else {
            if (depotHead.getAccountId() == null && StringUtil.isEmpty(depotHead.getAccountIdList())) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ACCOUNT_FAILED_CODE,
                        ExceptionConstants.DEPOT_HEAD_ACCOUNT_FAILED_MSG);
            }
            BigDecimal getAmount = headJson.getBigDecimal("getAmount");
            if (getAmount == null || getAmount.compareTo(totalPrice) < 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_RETAIL_RETURN_REFUND_CODE,
                        ExceptionConstants.DEPOT_HEAD_RETAIL_RETURN_REFUND_MSG);
            }
            depotHead.setBackAmount(getAmount.subtract(totalPrice).setScale(2, BigDecimal.ROUND_HALF_UP));
        }
        return detailArray.toJSONString();
    }

    private void validateRetailReturnSerialNumber(String returnSnList, String sourceSnList) {
        if (StringUtil.isEmpty(returnSnList)) {
            return;
        }
        Set<String> sourceSnSet = StringUtil.isEmpty(sourceSnList) ? Collections.emptySet()
                : Arrays.stream(sourceSnList.split(","))
                .map(String::trim).filter(StringUtil::isNotEmpty).collect(Collectors.toSet());
        for (String serialNumber : returnSnList.split(",")) {
            String value = serialNumber.trim();
            if (StringUtil.isNotEmpty(value) && !sourceSnSet.contains(value)) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_RETAIL_RETURN_SN_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_RETAIL_RETURN_SN_MSG, value));
            }
        }
    }

    private void validatePrepaidBalance(DepotHead depotHead, DepotHead previousDepotHead) {
        if (!isPrepaidRetailBill(depotHead) || depotHead.getOrganId() == null) {
            return;
        }
        Supplier supplier = supplierService.lockSupplier(depotHead.getOrganId());
        if (supplier == null || !"会员".equals(supplier.getType())
                || !Boolean.TRUE.equals(supplier.getEnabled())
                || BusinessConstants.DELETE_FLAG_DELETED.equals(supplier.getDeleteFlag())) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_RETAIL_MEMBER_REQUIRED_CODE,
                    ExceptionConstants.DEPOT_HEAD_RETAIL_MEMBER_REQUIRED_MSG);
        }
        if (isPrepaidRetailReturn(depotHead)) {
            return;
        }
        BigDecimal availableAdvance = supplierService.calculateAdvanceIn(depotHead.getOrganId());
        if (previousDepotHead != null
                && isPrepaidRetailOut(previousDepotHead)
                && BusinessConstants.BILLS_STATUS_AUDIT.equals(previousDepotHead.getStatus())
                && Objects.equals(previousDepotHead.getOrganId(), depotHead.getOrganId())) {
            availableAdvance = availableAdvance.add(previousDepotHead.getTotalPrice() == null
                    ? BigDecimal.ZERO : previousDepotHead.getTotalPrice());
        }
        if (availableAdvance.compareTo(depotHead.getTotalPrice()) < 0) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_MEMBER_PAY_LACK_CODE,
                    ExceptionConstants.DEPOT_HEAD_MEMBER_PAY_LACK_MSG);
        }
    }

    private void lockPrepaidMembers(Set<Long> memberIds) {
        for(Long memberId : memberIds) {
            supplierService.lockSupplier(memberId);
        }
    }

    private void validatePrepaidBillsBeforeAudit(List<DepotHead> prepaidBills) {
        Map<Long, BigDecimal> amountByMember = new HashMap<>();
        for(DepotHead depotHead : prepaidBills) {
            Supplier member = supplierService.lockSupplier(depotHead.getOrganId());
            if(member == null || !"会员".equals(member.getType())
                    || !Boolean.TRUE.equals(member.getEnabled())
                    || BusinessConstants.DELETE_FLAG_DELETED.equals(member.getDeleteFlag())) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_RETAIL_MEMBER_REQUIRED_CODE,
                        ExceptionConstants.DEPOT_HEAD_RETAIL_MEMBER_REQUIRED_MSG);
            }
            BigDecimal totalPrice = depotHead.getTotalPrice() == null
                    ? BigDecimal.ZERO : depotHead.getTotalPrice();
            amountByMember.merge(depotHead.getOrganId(), totalPrice, BigDecimal::add);
        }
        for(Map.Entry<Long, BigDecimal> entry : amountByMember.entrySet()) {
            if(entry.getValue().compareTo(BigDecimal.ZERO) > 0
                    && supplierService.calculateAdvanceIn(entry.getKey()).compareTo(entry.getValue()) < 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_MEMBER_PAY_LACK_CODE,
                        ExceptionConstants.DEPOT_HEAD_MEMBER_PAY_LACK_MSG);
            }
        }
    }

    private void refreshPrepaidMembers(Set<Long> memberIds) throws Exception {
        for(Long memberId : memberIds) {
            supplierService.updateAdvanceIn(memberId);
        }
    }

    private void refreshPrepaidBalanceAfterUpdate(DepotHead previousDepotHead, DepotHead depotHead) throws Exception {
        Set<Long> memberIds = new TreeSet<>();
        if (isPrepaidRetailBill(previousDepotHead) && previousDepotHead.getOrganId() != null) {
            memberIds.add(previousDepotHead.getOrganId());
        }
        if (isPrepaidRetailBill(depotHead) && depotHead.getOrganId() != null) {
            memberIds.add(depotHead.getOrganId());
        }
        for (Long memberId : memberIds) {
            supplierService.updateAdvanceIn(memberId);
        }
    }

    private boolean isPrepaidRetailOut(DepotHead depotHead) {
        return depotHead != null
                && BusinessConstants.DEPOTHEAD_TYPE_OUT.equals(depotHead.getType())
                && BusinessConstants.SUB_TYPE_RETAIL.equals(depotHead.getSubType())
                && BusinessConstants.PAY_TYPE_PREPAID.equals(depotHead.getPayType());
    }

    private boolean isPrepaidRetailReturn(DepotHead depotHead) {
        return depotHead != null
                && BusinessConstants.DEPOTHEAD_TYPE_IN.equals(depotHead.getType())
                && BusinessConstants.SUB_TYPE_RETAIL_RETURN.equals(depotHead.getSubType())
                && BusinessConstants.PAY_TYPE_PREPAID.equals(depotHead.getPayType());
    }

    private boolean isPrepaidRetailBill(DepotHead depotHead) {
        return isPrepaidRetailOut(depotHead) || isPrepaidRetailReturn(depotHead);
    }

    /**
     * 更新最终欠款
     * @param billId
     * @return
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void updateLastDebtByBillId(BigDecimal debt, Long billId) throws Exception {
        BigDecimal financialBillPrice = accountHeadService.getFinancialBillPriceByBillId(billId);
        if(debt!=null && financialBillPrice!=null) {
            DepotHead dh = new DepotHead();
            dh.setId(billId);
            dh.setLastDebt(debt.subtract(financialBillPrice));
            depotHeadMapper.updateByPrimaryKeySelective(dh);
        }
    }

    public Map<String, Object> getBuyAndSaleStatistics(String today, String monthFirstDay, String yesterdayBegin, String yesterdayEnd,
                                                       String yearBegin, String yearEnd, HttpServletRequest request) throws Exception {
        Long userId = userService.getUserId(request);
        String priceLimit = userService.getRoleTypeByUserId(userId).getPriceLimit();
        Boolean forceFlag = systemConfigService.getForceApprovalFlag();
        String[] creatorArray = getCreatorArray();
        List<InOutPriceVo> inOutPriceVoList = depotHeadMapperEx.getBuyAndSaleStatisticsList(yearBegin, yearEnd, creatorArray, forceFlag);

        String[] periods = {"today", "month", "yesterday", "year"};
        String[] types = {"Buy", "BuyBack", "Sale", "SaleBack", "RetailSale", "RetailSaleBack"};

        Map<String, BigDecimal> statistics = new HashMap<>();

        // 初始化 statistics Map
        for (String period : periods) {
            for (String type : types) {
                statistics.put(period + type, BigDecimal.ZERO);
            }
        }

        Date todayDate = Tools.strToDate(today);
        Date monthFirstDate = Tools.strToDate(monthFirstDay);
        Date yesterdayStartDate = Tools.strToDate(yesterdayBegin);
        Date yesterdayEndDate = Tools.strToDate(yesterdayEnd);
        Date yearStartDate = Tools.strToDate(yearBegin);
        Date yearEndDate = Tools.strToDate(yearEnd);

        for (InOutPriceVo item : inOutPriceVoList) {
            Date operTime = item.getOperTime();
            BigDecimal discountLastMoney = item.getDiscountLastMoney() != null ? item.getDiscountLastMoney() : BigDecimal.ZERO;
            BigDecimal totalPrice = item.getTotalPrice();
            BigDecimal totalPriceAbs = totalPrice != null ? totalPrice.abs() : BigDecimal.ZERO;

            if (isWithinRange(operTime, todayDate, Tools.strToDate(getNow3()))) {
                updateStatistics(statistics, item, "today", discountLastMoney, totalPriceAbs);
            }

            if (isWithinRange(operTime, monthFirstDate, Tools.strToDate(getNow3()))) {
                updateStatistics(statistics, item, "month", discountLastMoney, totalPriceAbs);
            }

            if (isWithinRange(operTime, yesterdayStartDate, yesterdayEndDate)) {
                updateStatistics(statistics, item, "yesterday", discountLastMoney, totalPriceAbs);
            }

            if (isWithinRange(operTime, yearStartDate, yearEndDate)) {
                updateStatistics(statistics, item, "year", discountLastMoney, totalPriceAbs);
            }
        }

        Map<String, Object> result = new HashMap<>();
        for (String period : periods) {
            result.put(period + "Buy", roleService.parseHomePriceByLimit(statistics.get(period + "Buy").subtract(statistics.get(period + "BuyBack")), "buy", priceLimit, "***", request));
            result.put(period + "Sale", roleService.parseHomePriceByLimit(statistics.get(period + "Sale").subtract(statistics.get(period + "SaleBack")), "sale", priceLimit, "***", request));
            result.put(period + "RetailSale", roleService.parseHomePriceByLimit(statistics.get(period + "RetailSale").subtract(statistics.get(period + "RetailSaleBack")), "retail", priceLimit, "***", request));
        }

        return result;
    }

    private boolean isWithinRange(Date operTime, Date startDate, Date endDate) {
        return operTime.compareTo(startDate) >= 0 && operTime.compareTo(endDate) <= 0;
    }

    private void updateStatistics(Map<String, BigDecimal> statistics, InOutPriceVo item, String period, BigDecimal discountLastMoney, BigDecimal totalPriceAbs) {
        switch (item.getType()) {
            case "入库":
                switch (item.getSubType()) {
                    case "采购":
                        statistics.put(period + "Buy", statistics.get(period + "Buy").add(discountLastMoney));
                        break;
                    case "销售退货":
                        statistics.put(period + "SaleBack", statistics.get(period + "SaleBack").add(discountLastMoney));
                        break;
                    case "零售退货":
                        statistics.put(period + "RetailSaleBack", statistics.get(period + "RetailSaleBack").add(totalPriceAbs));
                        break;
                }
                break;
            case "出库":
                switch (item.getSubType()) {
                    case "采购退货":
                        statistics.put(period + "BuyBack", statistics.get(period + "BuyBack").add(discountLastMoney));
                        break;
                    case "销售":
                        statistics.put(period + "Sale", statistics.get(period + "Sale").add(discountLastMoney));
                        break;
                    case "零售":
                        statistics.put(period + "RetailSale", statistics.get(period + "RetailSale").add(totalPriceAbs));
                        break;
                }
                break;
        }
    }


    public DepotHead getDepotHead(String number)throws Exception {
        DepotHead depotHead = new DepotHead();
        try{
            DepotHeadExample example = new DepotHeadExample();
            example.createCriteria().andNumberEqualTo(number).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
            List<DepotHead> list = depotHeadMapper.selectByExample(example);
            if(null!=list && list.size()>0) {
                depotHead = list.get(0);
            }
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return depotHead;
    }

    public List<DepotHeadVo4List> debtList(Long organId, String type, String subType, String materialParam, String number, String beginTime, String endTime,
                                           String status, Integer offset, Integer rows) {
        List<DepotHeadVo4List> resList = new ArrayList<>();
        try{
            String depotIds = depotService.findDepotStrByCurrentUser();
            String [] depotArray=StringUtil.isNotEmpty(depotIds) ? depotIds.split(",") : null;
            String [] creatorArray = getCreatorArray();
            beginTime = Tools.parseDayToTime(beginTime,BusinessConstants.DAY_FIRST_TIME);
            endTime = Tools.parseDayToTime(endTime,BusinessConstants.DAY_LAST_TIME);
            List<DepotHeadVo4List> list=depotHeadMapperEx.debtList(organId, type, subType, creatorArray, status, number,
                    beginTime, endTime, materialParam, depotArray, offset, rows);
            if (null != list) {
                resList = parseDebtBillList(list);
            }
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return resList;
    }

    public int debtListCount(Long organId, String type, String subType, String materialParam, String number, String beginTime, String endTime,
                              String status) {
        int total = 0;
        try {
            String depotIds = depotService.findDepotStrByCurrentUser();
            String[] depotArray = StringUtil.isNotEmpty(depotIds) ? depotIds.split(",") : null;
            String[] creatorArray = getCreatorArray();
            beginTime = Tools.parseDayToTime(beginTime, BusinessConstants.DAY_FIRST_TIME);
            endTime = Tools.parseDayToTime(endTime, BusinessConstants.DAY_LAST_TIME);
            total = depotHeadMapperEx.debtListCount(organId, type, subType, creatorArray, status, number,
                    beginTime, endTime, materialParam, depotArray);
        } catch(Exception e){
            JshException.readFail(logger, e);
        }
        return total;
    }

    public void debtExport(Long organId, String materialParam, String number, String type, String subType,
                           String beginTime, String endTime, String status, String mpList,
                           HttpServletRequest request, HttpServletResponse response) {
        try {
            Long userId = userService.getUserId(request);
            String priceLimit = userService.getRoleTypeByUserId(userId).getPriceLimit();
            String billCategory = getBillCategory(subType);
            String depotIds = depotService.findDepotStrByCurrentUser();
            String[] depotArray = StringUtil.isNotEmpty(depotIds) ? depotIds.split(",") : null;
            String[] creatorArray = getCreatorArray();
            status = StringUtil.isNotEmpty(status) ? status : null;
            beginTime = Tools.parseDayToTime(beginTime, BusinessConstants.DAY_FIRST_TIME);
            endTime = Tools.parseDayToTime(endTime, BusinessConstants.DAY_LAST_TIME);
            List<DepotHeadVo4List> dhList = new ArrayList<>();
            List<DepotHeadVo4List> list = depotHeadMapperEx.debtList(organId, type, subType, creatorArray, status, number,
                    beginTime, endTime, materialParam, depotArray, null, null);
            if (null != list) {
                dhList = parseDebtBillList(list);
            }
            //生成Excel文件
            String fileName = "单据信息";
            File file = new File(fileExportTmp + fileName);
            WritableWorkbook wtwb = Workbook.createWorkbook(file);
            String oneTip = "";
            String sheetOneStr = "";
            if("采购".equals(subType)) {
                oneTip = "供应商对账列表";
                sheetOneStr = "供应商,单据编号,关联单据,商品信息,单据日期,操作员,单据金额,本单欠款,已付欠款,待付欠款,备注";
            } else if("出库".equals(type) && "销售".equals(subType)) {
                oneTip = "客户对账列表";
                sheetOneStr = "客户,单据编号,关联单据,商品信息,单据日期,操作员,单据金额,本单欠款,已收欠款,待收欠款,备注";
            }
            if(StringUtil.isNotEmpty(beginTime) && StringUtil.isNotEmpty(endTime)) {
                oneTip = oneTip + "（" + beginTime + "至" + endTime + "）";
            }
            List<String> sheetOneList = StringUtil.strToStringList(sheetOneStr);
            String[] sheetOneArr = StringUtil.listToStringArray(sheetOneList);
            List<Long> idList = new ArrayList<>();
            List<String[]> billList = new ArrayList<>();
            Map<Long, BillListCacheVo> billListCacheVoMap = new HashMap<>();
            for (DepotHeadVo4List dh : dhList) {
                idList.add(dh.getId());
                BillListCacheVo billListCacheVo = new BillListCacheVo();
                billListCacheVo.setNumber(dh.getNumber());
                billListCacheVo.setOrganName(dh.getOrganName());
                billListCacheVo.setOperTimeStr(getCenternTime(dh.getOperTime()));
                billListCacheVoMap.put(dh.getId(), billListCacheVo);
                String[] objs = new String[sheetOneArr.length];
                objs[0] = dh.getOrganName();
                objs[1] = dh.getNumber();
                objs[2] = dh.getLinkNumber();
                objs[3] = dh.getMaterialsList();
                objs[4] = dh.getOperTimeStr();
                objs[5] = dh.getUserName();
                BigDecimal discountLastMoney = dh.getDiscountLastMoney() == null ? BigDecimal.ZERO : dh.getDiscountLastMoney();
                BigDecimal otherMoney = dh.getOtherMoney() == null ? BigDecimal.ZERO : dh.getOtherMoney();
                BigDecimal deposit = dh.getDeposit() == null ? BigDecimal.ZERO : dh.getDeposit();
                objs[6] = parseDecimalToStr(discountLastMoney.add(otherMoney).subtract(deposit), 2);
                objs[7] = parseDecimalToStr(dh.getNeedDebt(), 2);
                objs[8] = parseDecimalToStr(dh.getFinishDebt(), 2);
                objs[9] = parseDecimalToStr(dh.getDebt(), 2);
                objs[10] = dh.getRemark();
                billList.add(objs);
            }
            ExcelUtils.exportObjectsManySheet(wtwb, oneTip, sheetOneArr, "单据列表", 0, billList);
            //导出明细数据
            if(idList.size()>0) {
                List<DepotItemVo4WithInfoEx> dataList = depotItemMapperEx.getBillDetailListByIds(idList);
                String twoTip = "";
                String sheetTwoStr = "";
                if ("采购".equals(subType)) {
                    twoTip = "供应商单据明细";
                    sheetTwoStr = "供应商,单据编号,单据日期,仓库名称,条码,名称,规格,型号,颜色,品牌,制造商," + mpList + ",单位,序列号,批号,有效期,多属性,数量,单价,金额,税率(%),税额,价税合计,重量,备注";
                } else if ("销售".equals(subType)) {
                    twoTip = "客户单据明细";
                    sheetTwoStr = "客户,单据编号,单据日期,仓库名称,条码,名称,规格,型号,颜色,品牌,制造商," + mpList + ",单位,序列号,批号,有效期,多属性,数量,单价,金额,税率(%),税额,价税合计,重量,备注";
                }
                if (StringUtil.isNotEmpty(beginTime) && StringUtil.isNotEmpty(endTime)) {
                    twoTip = twoTip + "（" + beginTime + "至" + endTime + "）";
                }
                List<String> sheetTwoList = StringUtil.strToStringList(sheetTwoStr);
                String[] sheetTwoArr = StringUtil.listToStringArray(sheetTwoList);
                List<String[]> billDetail = new ArrayList<>();
                for (DepotItemVo4WithInfoEx diEx : dataList) {
                    String[] objs = new String[sheetTwoArr.length];
                    BillListCacheVo billListCacheVo = billListCacheVoMap.get(diEx.getHeaderId());
                    objs[0] = billListCacheVo != null ? billListCacheVo.getOrganName() : "";
                    objs[1] = billListCacheVo != null ? billListCacheVo.getNumber() : "";
                    objs[2] = billListCacheVo != null ? billListCacheVo.getOperTimeStr() : "";
                    objs[3] = diEx.getDepotId() == null ? "" : diEx.getDepotName();
                    objs[4] = diEx.getBarCode();
                    objs[5] = diEx.getMName();
                    objs[6] = diEx.getMStandard();
                    objs[7] = diEx.getMModel();
                    objs[8] = diEx.getMColor();
                    objs[9] = diEx.getBrand();
                    objs[10] = diEx.getMMfrs();
                    objs[11] = diEx.getMOtherField1();
                    objs[12] = diEx.getMOtherField2();
                    objs[13] = diEx.getMOtherField3();
                    objs[14] = diEx.getMaterialUnit();
                    objs[15] = diEx.getSnList();
                    objs[16] = diEx.getBatchNumber();
                    objs[17] = Tools.parseDateToStr(diEx.getExpirationDate());
                    objs[18] = diEx.getSku();
                    objs[19] = parseDecimalToStr(diEx.getOperNumber(), 2);
                    objs[20] = parseDecimalToStr(roleService.parseBillPriceByLimit(diEx.getUnitPrice(), billCategory, priceLimit, request), 2);
                    objs[21] = parseDecimalToStr(roleService.parseBillPriceByLimit(diEx.getAllPrice(), billCategory, priceLimit, request), 2);
                    objs[22] = parseDecimalToStr(roleService.parseBillPriceByLimit(diEx.getTaxRate(), billCategory, priceLimit, request), 2);
                    objs[23] = parseDecimalToStr(roleService.parseBillPriceByLimit(diEx.getTaxMoney(), billCategory, priceLimit, request), 2);
                    objs[24] = parseDecimalToStr(roleService.parseBillPriceByLimit(diEx.getTaxLastMoney(), billCategory, priceLimit, request), 2);
                    BigDecimal allWeight = diEx.getBasicNumber() == null || diEx.getWeight() == null ? BigDecimal.ZERO : diEx.getBasicNumber().multiply(diEx.getWeight());
                    objs[25] = parseDecimalToStr(allWeight, 2);
                    objs[26] = diEx.getRemark();
                    billDetail.add(objs);
                }
                ExcelUtils.exportObjectsManySheet(wtwb, twoTip, sheetTwoArr, "单据明细", 1, billDetail);
            }
            wtwb.write();
            wtwb.close();
            ExcelUtils.downloadExcel(file, file.getName(), response);
        } catch(Exception e){
            JshException.readFail(logger, e);
        }
    }

    public List<DepotHeadVo4List> parseDebtBillList(List<DepotHeadVo4List> list) throws Exception {
        List<Long> idList = new ArrayList<>();
        List<DepotHeadVo4List> dhList = new ArrayList<>();
        for (DepotHeadVo4List dh : list) {
            idList.add(dh.getId());
        }
        //通过批量查询去构造map
        Map<Long,String> materialsListMap = findMaterialsListMapByHeaderIdList(idList);
        for (DepotHeadVo4List dh : list) {
            if(dh.getChangeAmount() != null) {
                dh.setChangeAmount(dh.getChangeAmount().abs());
            }
            if(dh.getTotalPrice() != null) {
                dh.setTotalPrice(dh.getTotalPrice().abs());
            }
            if(dh.getDeposit() == null) {
                dh.setDeposit(BigDecimal.ZERO);
            }
            if(dh.getOperTime() != null) {
                dh.setOperTimeStr(getCenternTime(dh.getOperTime()));
            }
            BigDecimal discountLastMoney = dh.getDiscountLastMoney()!=null?dh.getDiscountLastMoney():BigDecimal.ZERO;
            BigDecimal otherMoney = dh.getOtherMoney()!=null?dh.getOtherMoney():BigDecimal.ZERO;
            BigDecimal deposit = dh.getDeposit()!=null?dh.getDeposit():BigDecimal.ZERO;
            BigDecimal changeAmount = dh.getChangeAmount()!=null?dh.getChangeAmount().abs():BigDecimal.ZERO;
            //本单欠款(如果退货则为负数)
            dh.setNeedDebt(discountLastMoney.add(otherMoney).subtract(deposit.add(changeAmount)));
            if(BusinessConstants.SUB_TYPE_PURCHASE_RETURN.equals(dh.getSubType()) || BusinessConstants.SUB_TYPE_SALES_RETURN.equals(dh.getSubType())) {
                dh.setNeedDebt(BigDecimal.ZERO.subtract(dh.getNeedDebt()));
            }
            BigDecimal needDebt = dh.getNeedDebt()!=null?dh.getNeedDebt():BigDecimal.ZERO;
            BigDecimal finishDebt = accountItemService.getEachAmountByBillId(dh.getId());
            finishDebt = finishDebt!=null?finishDebt:BigDecimal.ZERO;
            //已收欠款
            dh.setFinishDebt(finishDebt);
            //待收欠款
            dh.setDebt(needDebt.subtract(finishDebt));
            //商品信息简述
            if(materialsListMap!=null) {
                dh.setMaterialsList(materialsListMap.get(dh.getId()));
            }
            dhList.add(dh);
        }
        return dhList;
    }

    public String getBillCategory(String subType) {
        if(subType == null) {
            return "buy";
        }
        if(subType.equals("零售") || subType.equals("零售退货")) {
            return "retail";
        } else if(subType.equals("销售订单") || subType.equals("销售") || subType.equals("销售退货")) {
            return "sale";
        } else {
            return "buy";
        }
    }

    /**
     * 格式化金额样式
     * @param decimal
     * @param num
     * @return
     */
    private String parseDecimalToStr(BigDecimal decimal, Integer num) {
        return decimal == null ? "" : decimal.setScale(num, BigDecimal.ROUND_HALF_UP).toString();
    }

    private String parseStatusToStr(String status, String type) {
        if(StringUtil.isNotEmpty(status)) {
            if("purchase".equals(type)) {
                switch (status) {
                    case "2":
                        return "完成采购";
                    case "3":
                        return "部分采购";
                }
            } else if("sale".equals(type)) {
                switch (status) {
                    case "2":
                        return "完成销售";
                    case "3":
                        return "部分销售";
                }
            }
            switch (status) {
                case "0":
                    return "未审核";
                case "1":
                    return "已审核";
                case "9":
                    return "审核中";
            }
        }
        return "";
    }

    public List<DepotHeadVo4List> waitBillList(String number, String materialParam, String type, String subType,
                                               String beginTime, String endTime, String status, int offset, int rows) {
        List<DepotHeadVo4List> resList = new ArrayList<>();
        try{
            String [] depotArray = getDepotArray("其它");
            //给仓管可以看全部的单据（此时可以通过分配仓库去控制权限）
            String [] creatorArray = null;
            String [] subTypeArray = StringUtil.isNotEmpty(subType) ? subType.split(",") : null;
            String [] statusArray = StringUtil.isNotEmpty(status) ? status.split(",") : null;
            Map<Long,String> accountMap = accountService.getAccountMap();
            beginTime = Tools.parseDayToTime(beginTime,BusinessConstants.DAY_FIRST_TIME);
            endTime = Tools.parseDayToTime(endTime,BusinessConstants.DAY_LAST_TIME);
            List<DepotHeadVo4List> list = depotHeadMapperEx.waitBillList(type, subTypeArray, creatorArray, statusArray, number, beginTime, endTime,
                    materialParam, depotArray, offset, rows);
            if (null != list) {
                List<Long> idList = new ArrayList<>();
                for (DepotHeadVo4List dh : list) {
                    idList.add(dh.getId());
                }
                //通过批量查询去构造map
                Map<Long,String> materialsListMap = findMaterialsListMapByHeaderIdList(idList);
                Map<Long,BigDecimal> materialCountListMap = getMaterialCountListMapByHeaderIdList(idList);
                for (DepotHeadVo4List dh : list) {
                    if(accountMap!=null && StringUtil.isNotEmpty(dh.getAccountIdList()) && StringUtil.isNotEmpty(dh.getAccountMoneyList())) {
                        String accountStr = accountService.getAccountStrByIdAndMoney(accountMap, dh.getAccountIdList(), dh.getAccountMoneyList());
                        dh.setAccountName(accountStr);
                    }
                    if(dh.getOperTime() != null) {
                        dh.setOperTimeStr(getCenternTime(dh.getOperTime()));
                    }
                    //商品信息简述
                    if(materialsListMap!=null) {
                        dh.setMaterialsList(materialsListMap.get(dh.getId()));
                    }
                    //商品总数量
                    if(materialCountListMap!=null) {
                        dh.setMaterialCount(materialCountListMap.get(dh.getId()));
                    }
                    resList.add(dh);
                }
            }
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return resList;
    }

    public Long waitBillCount(String number, String materialParam, String type, String subType,
                             String beginTime, String endTime, String status) {
        Long result=null;
        try{
            String [] depotArray = getDepotArray("其它");
            //给仓管可以看全部的单据（此时可以通过分配仓库去控制权限）
            String [] creatorArray = null;
            String [] subTypeArray = StringUtil.isNotEmpty(subType) ? subType.split(",") : null;
            String [] statusArray = StringUtil.isNotEmpty(status) ? status.split(",") : null;
            beginTime = Tools.parseDayToTime(beginTime,BusinessConstants.DAY_FIRST_TIME);
            endTime = Tools.parseDayToTime(endTime,BusinessConstants.DAY_LAST_TIME);
            result=depotHeadMapperEx.waitBillCount(type, subTypeArray, creatorArray, statusArray, number, beginTime, endTime,
                    materialParam, depotArray);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void batchAddDepotHeadAndDetail(String ids, HttpServletRequest request) throws Exception {
        List<Long> sourceIds = StringUtil.strToLongList(ids);
        if (sourceIds == null || sourceIds.isEmpty()) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_OTHER_BATCH_SOURCE_CODE,
                    ExceptionConstants.DEPOT_HEAD_OTHER_BATCH_SOURCE_MSG);
        }
        sourceIds = sourceIds.stream().distinct().sorted().collect(Collectors.toList());
        StringBuilder sb = new StringBuilder();
        User userInfo=userService.getCurrentUser();
        for(Long sourceId : sourceIds) {
            //按固定顺序锁定来源单，避免并发请求重复转单或多单据请求互相死锁。
            DepotHead sourceHead = depotHeadMapperEx.lockDepotHeadById(sourceId);
            boolean inboundSource = sourceHead != null
                    && BusinessConstants.DEPOTHEAD_TYPE_IN.equals(sourceHead.getType())
                    && (BusinessConstants.SUB_TYPE_PURCHASE.equals(sourceHead.getSubType())
                    || BusinessConstants.SUB_TYPE_SALES_RETURN.equals(sourceHead.getSubType()));
            boolean outboundSource = sourceHead != null
                    && BusinessConstants.DEPOTHEAD_TYPE_OUT.equals(sourceHead.getType())
                    && (BusinessConstants.SUB_TYPE_PURCHASE_RETURN.equals(sourceHead.getSubType())
                    || BusinessConstants.SUB_TYPE_SALES.equals(sourceHead.getSubType()));
            if (!inboundSource && !outboundSource) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_OTHER_BATCH_SOURCE_CODE,
                        ExceptionConstants.DEPOT_HEAD_OTHER_BATCH_SOURCE_MSG);
            }
            checkPurchaseBillDataPermission(sourceHead);
            DepotHead depotHead = JSONObject.parseObject(JSONObject.toJSONString(sourceHead), DepotHead.class);
            depotHead.setSubType(BusinessConstants.SUB_TYPE_OTHER);
            checkBillButtonPermission(depotHead, "1", "批量新增");
            String prefixNo = inboundSource ? "QTRK" : "QTCK";
            //关联单据单号
            String oldNumber = sourceHead.getNumber();
            //校验单据最新状态不能进行批量操作
            if(!BusinessConstants.BILLS_STATUS_AUDIT.equals(sourceHead.getStatus())) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_ITEM_EXIST_NEW_STATUS_FAILED_CODE,
                        String.format(ExceptionConstants.DEPOT_ITEM_EXIST_NEW_STATUS_FAILED_MSG, oldNumber, sourceHead.getType()));
            }
            depotHead.setLinkNumber(oldNumber);
            //给单号重新赋值
            String number = prefixNo + sequenceService.buildOnlyNumber();
            depotHead.setNumber(number);
            depotHead.setDefaultNumber(number);
            depotHead.setOperTime(new Date());
            depotHead.setChangeAmount(BigDecimal.ZERO);
            depotHead.setTotalPrice(BigDecimal.ZERO);
            depotHead.setDiscountLastMoney(BigDecimal.ZERO);
            depotHead.setCreator(userInfo==null?null:userInfo.getId());
            depotHead.setCreateTime(new Timestamp(System.currentTimeMillis()));
            depotHead.setOrganId(null);
            depotHead.setAccountId(null);
            depotHead.setAccountIdList(null);
            depotHead.setAccountMoneyList(null);
            depotHead.setFileName(null);
            depotHead.setSalesMan(null);
            depotHead.setStatus("0");
            depotHead.setPurchaseStatus(BusinessConstants.PURCHASE_STATUS_UN_AUDIT);
            depotHead.setDeleteFlag(BusinessConstants.DELETE_FLAG_EXISTS);
            depotHead.setTenantId(null);
            //查询明细
            List<DepotItemVo4WithInfoEx> itemList = depotItemService.getDetailList(sourceHead.getId());
            depotHead.setId(null);
            JSONArray rowArr = new JSONArray();
            for(DepotItemVo4WithInfoEx item: itemList) {
                if("1".equals(item.getEnableSerialNumber())) {
                    throw new BusinessRunTimeException(ExceptionConstants.DEPOT_ITEM_EXIST_SERIAL_NUMBER_FAILED_CODE,
                            String.format(ExceptionConstants.DEPOT_ITEM_EXIST_SERIAL_NUMBER_FAILED_MSG, oldNumber));
                }
                if("1".equals(item.getEnableBatchNumber())) {
                    throw new BusinessRunTimeException(ExceptionConstants.DEPOT_ITEM_EXIST_BATCH_NUMBER_FAILED_CODE,
                            String.format(ExceptionConstants.DEPOT_ITEM_EXIST_BATCH_NUMBER_FAILED_MSG, oldNumber));
                }
                item.setUnitPrice(BigDecimal.ZERO);
                item.setAllPrice(BigDecimal.ZERO);
                item.setLinkId(item.getId());
                item.setTenantId(null);
                String itemStr = JSONObject.toJSONString(item);
                JSONObject itemObj = JSONObject.parseObject(itemStr);
                itemObj.put("unit", itemObj.getString("materialUnit"));
                rowArr.add(itemObj.toJSONString());
            }
            String rows = rowArr.toJSONString();
            validateDepotHeadBusinessType(depotHead);
            validateOtherSubmittedState(depotHead, null);
            if (inboundSource) {
                rows = validateAndNormalizeOtherInboundSource(depotHead, rows, null);
                rows = normalizeOtherInboundFinancialFields(depotHead, rows);
            } else {
                rows = validateAndNormalizeOtherOutboundSource(depotHead, rows, null);
                rows = normalizeOtherOutboundFinancialFields(depotHead, rows);
            }
            //新增其它入库单或其它出库单
            sb.append("[").append(depotHead.getNumber()).append("]");
            depotHeadMapper.insertSelective(depotHead);
            //根据单据编号查询单据id
            DepotHeadExample dhExample = new DepotHeadExample();
            dhExample.createCriteria().andNumberEqualTo(depotHead.getNumber()).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
            List<DepotHead> list = depotHeadMapper.selectByExample(dhExample);
            if(list!=null) {
                Long headId = list.get(0).getId();
                /**入库和出库处理单据子表信息*/
                depotItemService.saveDetials(rows, headId, "add", request);
            }
        }
        logService.insertLog("单据",
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_BATCH_ADD).append(sb).toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
    }

    /**
     * 快捷编辑单据
     * @param id 单据id
     * @param remark 备注内容
     * @param request 请求对象
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void quickEditDepotHead(Long id, String remark, HttpServletRequest request) throws Exception {
        try {
            // 查询单据获取编号
            DepotHead oldDepotHead = depotHeadMapper.selectByPrimaryKey(id);
            if (oldDepotHead == null) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_EDIT_FAILED_CODE,
                        ExceptionConstants.DEPOT_HEAD_EDIT_FAILED_MSG);
            }
            checkBillButtonPermission(oldDepotHead, "1", "编辑备注");
            checkPurchaseBillDataPermission(oldDepotHead);
            DepotHead depotHead = new DepotHead();
            depotHead.setId(id);
            depotHead.setRemark(remark);
            depotHeadMapper.updateByPrimaryKeySelective(depotHead);
            // 记录日志，使用单据编号
            String oldRemark = StringUtil.isNotEmpty(oldDepotHead.getRemark())? "，原备注为:"+oldDepotHead.getRemark():"";
            logService.insertLog("单据",
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append("备注，单据编号:")
                            .append(oldDepotHead.getNumber()).append(oldRemark).toString(),
                    request);
        } catch (BusinessRunTimeException e) {
            throw e;
        } catch (Exception e) {
            JshException.writeFail(logger, e);
        }
    }
}
