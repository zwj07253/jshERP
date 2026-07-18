package com.jsh.erp.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.*;
import com.jsh.erp.datasource.mappers.AccountHeadMapper;
import com.jsh.erp.datasource.mappers.AccountHeadMapperEx;
import com.jsh.erp.datasource.mappers.AccountItemMapperEx;
import com.jsh.erp.datasource.mappers.AccountMapper;
import com.jsh.erp.datasource.mappers.DepotHeadMapperEx;
import com.jsh.erp.datasource.vo.AccountItemVo4List;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.exception.JshException;
import com.jsh.erp.utils.PageUtils;
import com.jsh.erp.utils.StringUtil;
import com.jsh.erp.utils.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.jsh.erp.utils.Tools.getCenternTime;

@Service
public class AccountHeadService {
    private Logger logger = LoggerFactory.getLogger(AccountHeadService.class);
    @Resource
    private AccountHeadMapper accountHeadMapper;
    @Resource
    private AccountHeadMapperEx accountHeadMapperEx;
    @Resource
    private OrgaUserRelService orgaUserRelService;
    @Resource
    private AccountItemService accountItemService;
    @Resource
    private UserService userService;
    @Resource
    private SupplierService supplierService;
    @Resource
    private SystemConfigService systemConfigService;
    @Resource
    private LogService logService;
    @Resource
    private AccountItemMapperEx accountItemMapperEx;
    @Resource
    private AccountMapper accountMapper;
    @Resource
    private InOutItemService inOutItemService;
    @Resource
    private DepotHeadMapperEx depotHeadMapperEx;

    private static final String ADVANCE_IN_URL = "/financial/advance_in";
    private static final String MONEY_IN_URL = "/financial/money_in";
    private static final String MONEY_OUT_URL = "/financial/money_out";
    private static final String GIRO_URL = "/financial/giro";
    private static final String INCOME_URL = "/financial/item_in";
    private static final String EXPENSE_URL = "/financial/item_out";

    public AccountHead getAccountHead(long id) throws Exception {
        AccountHead result=null;
        try{
            result=accountHeadMapper.selectByPrimaryKey(id);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    public AccountHead getAccountHeadWithPermission(long id) throws Exception {
        AccountHead accountHead = getAccountHead(id);
        if (accountHead != null && !BusinessConstants.DELETE_FLAG_DELETED.equals(accountHead.getDeleteFlag())) {
            checkAccountHeadReadPermission(accountHead);
            return accountHead;
        }
        return null;
    }

    public List<AccountHead> getAccountHeadListByIds(String ids)throws Exception {
        List<Long> idList = StringUtil.strToLongList(ids);
        List<AccountHead> list = new ArrayList<>();
        try{
            AccountHeadExample example = new AccountHeadExample();
            example.createCriteria().andIdIn(idList)
                    .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
            list = accountHeadMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<AccountHead> getAccountHead() throws Exception{
        AccountHeadExample example = new AccountHeadExample();
        List<AccountHead> list=null;
        try{
            list=accountHeadMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<AccountHeadVo4ListEx> select(String type, String billNo, String beginTime, String endTime,
                                             Long organId, Long creator, Long handsPersonId, Long accountId, String status,
                                             String remark, String number, Long inOutItemId) throws Exception{
        checkAccountHeadListPermission(type);
        List<AccountHeadVo4ListEx> list = new ArrayList<>();
        try{
            String [] creatorArray = getCreatorArray();
            beginTime = Tools.parseDayToTime(beginTime,BusinessConstants.DAY_FIRST_TIME);
            endTime = Tools.parseDayToTime(endTime,BusinessConstants.DAY_LAST_TIME);
            PageUtils.startPage();
            list = accountHeadMapperEx.selectByConditionAccountHead(type, creatorArray, billNo,
                    beginTime, endTime, organId, creator, handsPersonId, accountId, status, remark, number, inOutItemId);
            if (null != list) {
                for (AccountHeadVo4ListEx ah : list) {
                    if(ah.getChangeAmount() != null) {
                        if(BusinessConstants.TYPE_MONEY_IN.equals(ah.getType())) {
                            ah.setChangeAmount(ah.getChangeAmount());
                        } else if(BusinessConstants.TYPE_MONEY_OUT.equals(ah.getType())) {
                            ah.setChangeAmount(BigDecimal.ZERO.subtract(ah.getChangeAmount()));
                        } else {
                            ah.setChangeAmount(ah.getChangeAmount().abs());
                        }
                    }
                    if(ah.getTotalPrice() != null) {
                        if(BusinessConstants.TYPE_MONEY_IN.equals(ah.getType())) {
                            ah.setTotalPrice(ah.getTotalPrice());
                        } else if(BusinessConstants.TYPE_MONEY_OUT.equals(ah.getType())) {
                            ah.setTotalPrice(BigDecimal.ZERO.subtract(ah.getTotalPrice()));
                        } else {
                            ah.setTotalPrice(ah.getTotalPrice().abs());
                        }
                    }
                    if(ah.getBillTime() !=null) {
                        ah.setBillTimeStr(getCenternTime(ah.getBillTime()));
                    }
                }
            }
        } catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    /**
     * 根据角色类型获取操作员数组
     * @return
     * @throws Exception
     */
    public String[] getCreatorArray() throws Exception {
        String creator = "";
        User user = userService.getCurrentUser();
        String roleType = userService.getRoleTypeByUserId(user.getId()).getType(); //角色类型
        if(BusinessConstants.ROLE_TYPE_PRIVATE.equals(roleType)) {
            creator = user.getId().toString();
        } else if(BusinessConstants.ROLE_TYPE_THIS_ORG.equals(roleType)) {
            creator = orgaUserRelService.getUserIdListByUserId(user.getId());
        }
        String [] creatorArray=null;
        if(StringUtil.isNotEmpty(creator)){
            creatorArray = creator.split(",");
        }
        return creatorArray;
    }

    public void checkAccountHeadReadPermission(AccountHead accountHead) throws Exception {
        if(accountHead == null) {
            throw new BusinessRunTimeException(ExceptionConstants.ACCOUNT_HEAD_DATA_PERMISSION_CODE,
                    ExceptionConstants.ACCOUNT_HEAD_DATA_PERMISSION_MSG);
        }
        String url = getFinancialPageUrl(accountHead.getType());
        User currentUser = userService.getCurrentUser();
        Long userId = currentUser == null ? null : currentUser.getId();
        if(!userService.hasFunctionPermission(userId, url)) {
            throw new BusinessRunTimeException(ExceptionConstants.ACCOUNT_HEAD_PERMISSION_CODE,
                    String.format(ExceptionConstants.ACCOUNT_HEAD_PERMISSION_MSG,
                            getFinancialBillName(accountHead.getType()), "查看"));
        }
        String[] creatorArray = getCreatorArray();
        if(creatorArray != null && (accountHead.getCreator() == null
                || !Arrays.asList(creatorArray).contains(accountHead.getCreator().toString()))) {
            throw new BusinessRunTimeException(ExceptionConstants.ACCOUNT_HEAD_DATA_PERMISSION_CODE,
                    ExceptionConstants.ACCOUNT_HEAD_DATA_PERMISSION_MSG);
        }
    }

    private void checkAccountHeadListPermission(String type) throws Exception {
        String url = getFinancialPageUrl(type);
        User currentUser = userService.getCurrentUser();
        Long userId = currentUser == null ? null : currentUser.getId();
        if(!userService.hasFunctionPermission(userId, url)) {
            throw new BusinessRunTimeException(ExceptionConstants.ACCOUNT_HEAD_PERMISSION_CODE,
                    String.format(ExceptionConstants.ACCOUNT_HEAD_PERMISSION_MSG,
                            getFinancialBillName(type), "查看"));
        }
    }

    private void checkAccountHeadButtonPermission(String type, String buttonCode, String operationName) throws Exception {
        String url = getFinancialPageUrl(type);
        User currentUser = userService.getCurrentUser();
        Long userId = currentUser == null ? null : currentUser.getId();
        if(!userService.hasButtonPermission(userId, url, buttonCode)) {
            throw new BusinessRunTimeException(ExceptionConstants.ACCOUNT_HEAD_PERMISSION_CODE,
                    String.format(ExceptionConstants.ACCOUNT_HEAD_PERMISSION_MSG,
                            getFinancialBillName(type), operationName));
        }
    }

    private void validateFinancialType(String type) {
        getFinancialPageUrl(type);
    }

    private String getFinancialPageUrl(String type) {
        if(BusinessConstants.TYPE_ADVANCE_IN.equals(type)) {
            return ADVANCE_IN_URL;
        } else if(BusinessConstants.TYPE_MONEY_IN.equals(type)) {
            return MONEY_IN_URL;
        } else if(BusinessConstants.TYPE_MONEY_OUT.equals(type)) {
            return MONEY_OUT_URL;
        } else if(BusinessConstants.TYPE_GIRO.equals(type)) {
            return GIRO_URL;
        } else if(BusinessConstants.TYPE_INCOME.equals(type)) {
            return INCOME_URL;
        } else if(BusinessConstants.TYPE_EXPENSE.equals(type)) {
            return EXPENSE_URL;
        }
        throw new BusinessRunTimeException(ExceptionConstants.ACCOUNT_HEAD_TYPE_FAILED_CODE,
                ExceptionConstants.ACCOUNT_HEAD_TYPE_FAILED_MSG);
    }

    private String getFinancialBillName(String type) {
        if(BusinessConstants.TYPE_ADVANCE_IN.equals(type)) {
            return "会员预付款";
        } else if(BusinessConstants.TYPE_MONEY_IN.equals(type)) {
            return "收款单";
        } else if(BusinessConstants.TYPE_MONEY_OUT.equals(type)) {
            return "付款单";
        } else if(BusinessConstants.TYPE_GIRO.equals(type)) {
            return "转账单";
        } else if(BusinessConstants.TYPE_INCOME.equals(type)) {
            return "收入单";
        } else if(BusinessConstants.TYPE_EXPENSE.equals(type)) {
            return "支出单";
        }
        return "财务单据";
    }

    private void validateSaveStatus(String status) {
        if(!BusinessConstants.BILLS_STATUS_UN_AUDIT.equals(status)
                && !BusinessConstants.BILLS_STATUS_AUDIT.equals(status)) {
            throw new BusinessRunTimeException(ExceptionConstants.ACCOUNT_HEAD_STATUS_FAILED_CODE,
                    ExceptionConstants.ACCOUNT_HEAD_STATUS_FAILED_MSG);
        }
    }

    private void validateIncomeExpenseBill(AccountHead accountHead, String rows) throws Exception {
        boolean income = BusinessConstants.TYPE_INCOME.equals(accountHead.getType());
        boolean expense = BusinessConstants.TYPE_EXPENSE.equals(accountHead.getType());
        if(!income && !expense) {
            return;
        }
        int detailFailedCode = income ? ExceptionConstants.ACCOUNT_HEAD_INCOME_DETAIL_FAILED_CODE
                : ExceptionConstants.ACCOUNT_HEAD_EXPENSE_DETAIL_FAILED_CODE;
        String detailFailedMsg = income ? ExceptionConstants.ACCOUNT_HEAD_INCOME_DETAIL_FAILED_MSG
                : ExceptionConstants.ACCOUNT_HEAD_EXPENSE_DETAIL_FAILED_MSG;
        if(StringUtil.isEmpty(accountHead.getBillNo()) || accountHead.getBillTime() == null) {
            throw new BusinessRunTimeException(detailFailedCode, detailFailedMsg);
        }
        Account account = accountHead.getAccountId() == null
                ? null : accountMapper.selectByPrimaryKey(accountHead.getAccountId());
        if(account == null || !Boolean.TRUE.equals(account.getEnabled())
                || BusinessConstants.DELETE_FLAG_DELETED.equals(account.getDeleteFlag())) {
            throw new BusinessRunTimeException(
                    income ? ExceptionConstants.ACCOUNT_HEAD_ACCOUNT_FAILED_CODE
                            : ExceptionConstants.ACCOUNT_HEAD_EXPENSE_ACCOUNT_FAILED_CODE,
                    income ? ExceptionConstants.ACCOUNT_HEAD_ACCOUNT_FAILED_MSG
                            : ExceptionConstants.ACCOUNT_HEAD_EXPENSE_ACCOUNT_FAILED_MSG);
        }
        JSONArray rowArray;
        try {
            rowArray = JSONArray.parseArray(rows);
        } catch (Exception e) {
            throw new BusinessRunTimeException(detailFailedCode, detailFailedMsg);
        }
        if(rowArray == null || rowArray.isEmpty()) {
            throw new BusinessRunTimeException(ExceptionConstants.ACCOUNT_HEAD_ROW_FAILED_CODE,
                    ExceptionConstants.ACCOUNT_HEAD_ROW_FAILED_MSG);
        }
        BigDecimal detailTotal = BigDecimal.ZERO;
        for(Object rowValue : rowArray) {
            JSONObject row = JSONObject.parseObject(rowValue.toString());
            Long inOutItemId = row.getLong("inOutItemId");
            InOutItem inOutItem = inOutItemId == null ? null : inOutItemService.getInOutItem(inOutItemId);
            BigDecimal eachAmount = row.getBigDecimal("eachAmount");
            String expectedItemType = income ? BusinessConstants.TYPE_INCOME : BusinessConstants.TYPE_EXPENSE;
            if(inOutItem == null || !expectedItemType.equals(inOutItem.getType())
                    || !Boolean.TRUE.equals(inOutItem.getEnabled())
                    || BusinessConstants.DELETE_FLAG_DELETED.equals(inOutItem.getDeleteFlag())
                    || eachAmount == null || eachAmount.compareTo(BigDecimal.ZERO) <= 0
                    || hasValue(row, "accountId") || hasValue(row, "billId")
                    || hasValue(row, "billNumber") || hasValue(row, "needDebt")
                    || hasValue(row, "finishDebt")) {
                throw new BusinessRunTimeException(detailFailedCode, detailFailedMsg);
            }
            detailTotal = detailTotal.add(eachAmount);
        }
        BigDecimal totalPrice = accountHead.getTotalPrice();
        BigDecimal changeAmount = accountHead.getChangeAmount();
        BigDecimal discountMoney = accountHead.getDiscountMoney();
        boolean invalidAmount = totalPrice == null || changeAmount == null
                || (discountMoney != null && discountMoney.compareTo(BigDecimal.ZERO) != 0);
        if(!invalidAmount) {
            if(income) {
                invalidAmount = totalPrice.compareTo(BigDecimal.ZERO) <= 0
                        || changeAmount.compareTo(BigDecimal.ZERO) <= 0
                        || !sameMoney(totalPrice, detailTotal) || !sameMoney(changeAmount, detailTotal);
            } else {
                invalidAmount = totalPrice.compareTo(BigDecimal.ZERO) >= 0
                        || changeAmount.compareTo(BigDecimal.ZERO) >= 0
                        || !sameMoney(totalPrice.abs(), detailTotal) || !sameMoney(changeAmount.abs(), detailTotal);
            }
        }
        if(invalidAmount) {
            throw new BusinessRunTimeException(
                    income ? ExceptionConstants.ACCOUNT_HEAD_INCOME_AMOUNT_FAILED_CODE
                            : ExceptionConstants.ACCOUNT_HEAD_EXPENSE_AMOUNT_FAILED_CODE,
                    income ? ExceptionConstants.ACCOUNT_HEAD_INCOME_AMOUNT_FAILED_MSG
                            : ExceptionConstants.ACCOUNT_HEAD_EXPENSE_AMOUNT_FAILED_MSG);
        }
    }

    private String validateFinancialBill(AccountHead accountHead, String rows, Long excludeHeadId) throws Exception {
        validateIncomeExpenseBill(accountHead, rows);
        if(BusinessConstants.TYPE_MONEY_IN.equals(accountHead.getType())) {
            return validateMoneyInBill(accountHead, rows, excludeHeadId);
        }
        return rows;
    }

    private String validateMoneyInBill(AccountHead accountHead, String rows, Long excludeHeadId) throws Exception {
        if(StringUtil.isEmpty(accountHead.getBillNo()) || accountHead.getBillTime() == null) {
            throw new BusinessRunTimeException(ExceptionConstants.ACCOUNT_HEAD_MONEY_IN_DETAIL_FAILED_CODE,
                    ExceptionConstants.ACCOUNT_HEAD_MONEY_IN_DETAIL_FAILED_MSG);
        }
        Account account = accountHead.getAccountId() == null
                ? null : accountMapper.selectByPrimaryKey(accountHead.getAccountId());
        if(account == null || !Boolean.TRUE.equals(account.getEnabled())
                || BusinessConstants.DELETE_FLAG_DELETED.equals(account.getDeleteFlag())) {
            throw new BusinessRunTimeException(ExceptionConstants.ACCOUNT_HEAD_MONEY_IN_ACCOUNT_FAILED_CODE,
                    ExceptionConstants.ACCOUNT_HEAD_MONEY_IN_ACCOUNT_FAILED_MSG);
        }
        Supplier customer = accountHead.getOrganId() == null
                ? null : supplierService.lockSupplier(accountHead.getOrganId());
        if(customer == null || !"客户".equals(customer.getType()) || !Boolean.TRUE.equals(customer.getEnabled())
                || BusinessConstants.DELETE_FLAG_DELETED.equals(customer.getDeleteFlag())) {
            throw new BusinessRunTimeException(ExceptionConstants.ACCOUNT_HEAD_MONEY_IN_ORGAN_FAILED_CODE,
                    ExceptionConstants.ACCOUNT_HEAD_MONEY_IN_ORGAN_FAILED_MSG);
        }
        JSONArray rowArray;
        try {
            rowArray = JSONArray.parseArray(rows);
        } catch (Exception e) {
            throw new BusinessRunTimeException(ExceptionConstants.ACCOUNT_HEAD_MONEY_IN_DETAIL_FAILED_CODE,
                    ExceptionConstants.ACCOUNT_HEAD_MONEY_IN_DETAIL_FAILED_MSG);
        }
        if(rowArray == null || rowArray.isEmpty()) {
            throw new BusinessRunTimeException(ExceptionConstants.ACCOUNT_HEAD_ROW_FAILED_CODE,
                    ExceptionConstants.ACCOUNT_HEAD_ROW_FAILED_MSG);
        }
        BigDecimal detailTotal = BigDecimal.ZERO;
        Set<String> billNumbers = new HashSet<>();
        for(int rowIndex = 0; rowIndex < rowArray.size(); rowIndex++) {
            Object rowValue = rowArray.get(rowIndex);
            JSONObject row;
            String billNumber;
            BigDecimal eachAmount;
            try {
                row = JSONObject.parseObject(rowValue.toString());
                billNumber = row.getString("billNumber");
                eachAmount = row.getBigDecimal("eachAmount");
            } catch (Exception e) {
                throw new BusinessRunTimeException(ExceptionConstants.ACCOUNT_HEAD_MONEY_IN_DETAIL_FAILED_CODE,
                        ExceptionConstants.ACCOUNT_HEAD_MONEY_IN_DETAIL_FAILED_MSG);
            }
            if(StringUtil.isEmpty(billNumber) || !billNumbers.add(billNumber) || eachAmount == null
                    || hasValue(row, "accountId") || hasValue(row, "inOutItemId") || hasValue(row, "billId")) {
                throw new BusinessRunTimeException(ExceptionConstants.ACCOUNT_HEAD_MONEY_IN_DETAIL_FAILED_CODE,
                        ExceptionConstants.ACCOUNT_HEAD_MONEY_IN_DETAIL_FAILED_MSG);
            }
            if(eachAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRunTimeException(ExceptionConstants.ACCOUNT_HEAD_MONEY_IN_AMOUNT_FAILED_CODE,
                        ExceptionConstants.ACCOUNT_HEAD_MONEY_IN_AMOUNT_FAILED_MSG);
            }
            BigDecimal needDebt;
            BigDecimal finishDebt;
            if("QiChu".equals(billNumber)) {
                needDebt = customer.getBeginNeedGet() == null ? BigDecimal.ZERO : customer.getBeginNeedGet();
                finishDebt = accountHeadMapperEx.getAuditedOpeningAmount(customer.getId(),
                        BusinessConstants.TYPE_MONEY_IN, excludeHeadId);
            } else {
                DepotHead depotHead = depotHeadMapperEx.lockDepotHeadByNumber(billNumber);
                if(depotHead == null || !"出库".equals(depotHead.getType()) || !"销售".equals(depotHead.getSubType())
                        || !BusinessConstants.BILLS_STATUS_AUDIT.equals(depotHead.getStatus())
                        || !customer.getId().equals(depotHead.getOrganId())) {
                    throw new BusinessRunTimeException(ExceptionConstants.ACCOUNT_HEAD_MONEY_IN_DETAIL_FAILED_CODE,
                            ExceptionConstants.ACCOUNT_HEAD_MONEY_IN_DETAIL_FAILED_MSG);
                }
                BigDecimal discountLastMoney = depotHead.getDiscountLastMoney() == null
                        ? BigDecimal.ZERO : depotHead.getDiscountLastMoney();
                BigDecimal otherMoney = depotHead.getOtherMoney() == null ? BigDecimal.ZERO : depotHead.getOtherMoney();
                BigDecimal deposit = depotHead.getDeposit() == null ? BigDecimal.ZERO : depotHead.getDeposit();
                BigDecimal changeAmount = depotHead.getChangeAmount() == null
                        ? BigDecimal.ZERO : depotHead.getChangeAmount().abs();
                needDebt = discountLastMoney.add(otherMoney).subtract(deposit.add(changeAmount)).abs();
                finishDebt = accountHeadMapperEx.getAuditedFinancialAmountByBillId(depotHead.getId(),
                        BusinessConstants.TYPE_MONEY_IN, excludeHeadId);
            }
            if(finishDebt == null) {
                finishDebt = BigDecimal.ZERO;
            }
            BigDecimal remainingDebt = needDebt.subtract(finishDebt);
            if(needDebt.compareTo(BigDecimal.ZERO) <= 0 || remainingDebt.compareTo(BigDecimal.ZERO) <= 0
                    || eachAmount.compareTo(remainingDebt) > 0) {
                throw new BusinessRunTimeException(ExceptionConstants.ACCOUNT_HEAD_MONEY_IN_AMOUNT_FAILED_CODE,
                        ExceptionConstants.ACCOUNT_HEAD_MONEY_IN_AMOUNT_FAILED_MSG);
            }
            row.remove("accountId");
            row.remove("inOutItemId");
            row.remove("billId");
            row.put("needDebt", needDebt);
            row.put("finishDebt", finishDebt);
            rowArray.set(rowIndex, row);
            detailTotal = detailTotal.add(eachAmount);
        }
        BigDecimal totalPrice = accountHead.getTotalPrice();
        BigDecimal changeAmount = accountHead.getChangeAmount();
        BigDecimal discountMoney = accountHead.getDiscountMoney();
        boolean invalidAmount = totalPrice == null || changeAmount == null || discountMoney == null
                || totalPrice.compareTo(BigDecimal.ZERO) <= 0 || changeAmount.compareTo(BigDecimal.ZERO) <= 0
                || discountMoney.compareTo(BigDecimal.ZERO) < 0 || discountMoney.compareTo(totalPrice) >= 0
                || !sameMoney(totalPrice, detailTotal)
                || !sameMoney(changeAmount, totalPrice.subtract(discountMoney));
        if(invalidAmount) {
            throw new BusinessRunTimeException(ExceptionConstants.ACCOUNT_HEAD_MONEY_IN_AMOUNT_FAILED_CODE,
                    ExceptionConstants.ACCOUNT_HEAD_MONEY_IN_AMOUNT_FAILED_MSG);
        }
        return rowArray.toJSONString();
    }

    private void validatePersistedFinancialBill(AccountHead accountHead) throws Exception {
        if(!BusinessConstants.TYPE_INCOME.equals(accountHead.getType())
                && !BusinessConstants.TYPE_EXPENSE.equals(accountHead.getType())
                && !BusinessConstants.TYPE_MONEY_IN.equals(accountHead.getType())) {
            return;
        }
        List<AccountItemVo4List> detailList = accountItemService.getDetailList(accountHead.getId());
        JSONArray rows = new JSONArray();
        if(detailList != null) {
            for(AccountItemVo4List detail : detailList) {
                JSONObject row = new JSONObject();
                row.put("eachAmount", detail.getEachAmount());
                if(!BusinessConstants.TYPE_MONEY_IN.equals(accountHead.getType())) {
                    row.put("inOutItemId", detail.getInOutItemId());
                    row.put("accountId", detail.getAccountId());
                    row.put("billId", detail.getBillId());
                }
                if(StringUtil.isNotEmpty(detail.getBillNumber())) {
                    row.put("billNumber", detail.getBillNumber());
                } else if(BusinessConstants.TYPE_MONEY_IN.equals(accountHead.getType())) {
                    row.put("billNumber", "QiChu");
                }
                if(detail.getNeedDebt() != null && detail.getNeedDebt().compareTo(BigDecimal.ZERO) != 0) {
                    row.put("needDebt", detail.getNeedDebt());
                }
                if(detail.getFinishDebt() != null && detail.getFinishDebt().compareTo(BigDecimal.ZERO) != 0) {
                    row.put("finishDebt", detail.getFinishDebt());
                }
                rows.add(row);
            }
        }
        validateFinancialBill(accountHead, rows.toJSONString(), accountHead.getId());
    }

    private boolean hasValue(JSONObject row, String key) {
        Object value = row.get(key);
        return value != null && StringUtil.isNotEmpty(value.toString());
    }

    private boolean sameMoney(BigDecimal first, BigDecimal second) {
        return first.setScale(6, RoundingMode.HALF_UP)
                .compareTo(second.setScale(6, RoundingMode.HALF_UP)) == 0;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int insertAccountHead(JSONObject obj, HttpServletRequest request) throws Exception{
        AccountHead accountHead = JSONObject.parseObject(obj.toJSONString(), AccountHead.class);
        int result=0;
        try{
            User userInfo=userService.getCurrentUser();
            accountHead.setCreator(userInfo==null?null:userInfo.getId());
            result = accountHeadMapper.insertSelective(accountHead);
            logService.insertLog("财务单据",
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_ADD).append(accountHead.getBillNo()).toString(), request);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int updateAccountHead(JSONObject obj, HttpServletRequest request)throws Exception {
        AccountHead accountHead = JSONObject.parseObject(obj.toJSONString(), AccountHead.class);
        int result=0;
        try{
            result = accountHeadMapper.updateByPrimaryKeySelective(accountHead);
            logService.insertLog("财务单据",
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append(accountHead.getBillNo()).toString(), request);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int deleteAccountHead(Long id, HttpServletRequest request)throws Exception {
        return batchDeleteAccountHeadByIds(id.toString());
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteAccountHead(String ids, HttpServletRequest request)throws Exception {
        return batchDeleteAccountHeadByIds(ids);
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteAccountHeadByIds(String ids)throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(BusinessConstants.LOG_OPERATION_TYPE_DELETE);
        User userInfo=userService.getCurrentUser();
        String [] idArray=ids.split(",");
        List<AccountHead> list = getAccountHeadListByIds(ids);
        if(list.size() != idArray.length) {
            throw new BusinessRunTimeException(ExceptionConstants.ACCOUNT_HEAD_DATA_PERMISSION_CODE,
                    ExceptionConstants.ACCOUNT_HEAD_DATA_PERMISSION_MSG);
        }
        for(AccountHead accountHead: list){
            checkAccountHeadReadPermission(accountHead);
            checkAccountHeadButtonPermission(accountHead.getType(), "1", "删除");
            if(!"0".equals(accountHead.getStatus())) {
                throw new BusinessRunTimeException(ExceptionConstants.ACCOUNT_HEAD_UN_AUDIT_DELETE_FAILED_CODE,
                        String.format(ExceptionConstants.ACCOUNT_HEAD_UN_AUDIT_DELETE_FAILED_MSG));
            }
        }
        Set<Long> affectedBillIds = new HashSet<>(accountItemService.getBillIdsByHeaderIds(idArray));
        //删除主表
        accountItemMapperEx.batchDeleteAccountItemByHeadIds(new Date(),userInfo==null?null:userInfo.getId(),idArray);
        //删除子表
        int deleteResult = accountHeadMapperEx.batchDeleteAccountHeadByIds(
                new Date(),userInfo==null?null:userInfo.getId(),idArray);
        if(deleteResult != idArray.length) {
            throw new BusinessRunTimeException(ExceptionConstants.ACCOUNT_HEAD_DELETE_FAILED_CODE,
                    ExceptionConstants.ACCOUNT_HEAD_DELETE_FAILED_MSG);
        }
        //路径列表
        List<String> pathList = new ArrayList<>();
        for(AccountHead accountHead: list){
            sb.append("[").append(accountHead.getBillNo()).append("]");
            if(StringUtil.isNotEmpty(accountHead.getFileName())) {
                pathList.add(accountHead.getFileName());
            }
            if("收预付款".equals(accountHead.getType())){
                if (accountHead.getOrganId() != null) {
                    //更新会员预付款
                    supplierService.updateAdvanceIn(accountHead.getOrganId());
                }
            }
        }
        //逻辑删除文件
        systemConfigService.deleteFileByPathList(pathList);
        accountItemService.refreshLastDebtByBillIds(affectedBillIds);
        logService.insertLog("财务单据", sb.toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        return 1;
    }

    /**
     * 校验单据编号是否存在
     * @param id
     * @param billNo
     * @return
     * @throws Exception
     */
    public int checkIsBillNoExist(Long id, String billNo)throws Exception {
        AccountHeadExample example = new AccountHeadExample();
        example.createCriteria().andIdNotEqualTo(id).andBillNoEqualTo(billNo).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<AccountHead> list = null;
        try{
            list = accountHeadMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list==null?0:list.size();
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchSetStatus(String status, String accountHeadIds)throws Exception {
        if(!BusinessConstants.BILLS_STATUS_UN_AUDIT.equals(status)
                && !BusinessConstants.BILLS_STATUS_AUDIT.equals(status)) {
            throw new BusinessRunTimeException(ExceptionConstants.ACCOUNT_HEAD_STATUS_FAILED_CODE,
                    ExceptionConstants.ACCOUNT_HEAD_STATUS_FAILED_MSG);
        }
        int result = 0;
        List<Long> ahIds = new ArrayList<>();
        List<String> noList = new ArrayList<>();
        List<Long> ids = StringUtil.strToLongList(accountHeadIds);
        for(Long id: ids) {
            AccountHead accountHead = getAccountHead(id);
            if(accountHead == null || BusinessConstants.DELETE_FLAG_DELETED.equals(accountHead.getDeleteFlag())) {
                throw new BusinessRunTimeException(ExceptionConstants.ACCOUNT_HEAD_DATA_PERMISSION_CODE,
                        ExceptionConstants.ACCOUNT_HEAD_DATA_PERMISSION_MSG);
            }
            checkAccountHeadReadPermission(accountHead);
            if("0".equals(status)){
                checkAccountHeadButtonPermission(accountHead.getType(), "7", "反审核");
                //进行反审核操作
                if("1".equals(accountHead.getStatus())) {
                    ahIds.add(id);
                    noList.add(accountHead.getBillNo());
                } else {
                    throw new BusinessRunTimeException(ExceptionConstants.ACCOUNT_HEAD_AUDIT_TO_UN_AUDIT_FAILED_CODE,
                            String.format(ExceptionConstants.ACCOUNT_HEAD_AUDIT_TO_UN_AUDIT_FAILED_MSG));
                }
            } else if("1".equals(status)){
                checkAccountHeadButtonPermission(accountHead.getType(), "2", "审核");
                //进行审核操作
                if("0".equals(accountHead.getStatus())) {
                    validatePersistedFinancialBill(accountHead);
                    ahIds.add(id);
                    noList.add(accountHead.getBillNo());
                } else {
                    throw new BusinessRunTimeException(ExceptionConstants.ACCOUNT_HEAD_UN_AUDIT_TO_AUDIT_FAILED_CODE,
                            String.format(ExceptionConstants.ACCOUNT_HEAD_UN_AUDIT_TO_AUDIT_FAILED_MSG));
                }
            }
        }
        if(!ahIds.isEmpty()) {
            String[] affectedHeadIds = ahIds.stream().map(String::valueOf).toArray(String[]::new);
            Set<Long> affectedBillIds = new HashSet<>(accountItemService.getBillIdsByHeaderIds(affectedHeadIds));
            AccountHead accountHead = new AccountHead();
            accountHead.setStatus(status);
            AccountHeadExample example = new AccountHeadExample();
            String previousStatus = BusinessConstants.BILLS_STATUS_AUDIT.equals(status)
                    ? BusinessConstants.BILLS_STATUS_UN_AUDIT : BusinessConstants.BILLS_STATUS_AUDIT;
            example.createCriteria().andIdIn(ahIds).andStatusEqualTo(previousStatus)
                    .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
            result = accountHeadMapper.updateByExampleSelective(accountHead, example);
            if(result != ahIds.size()) {
                throw new BusinessRunTimeException(ExceptionConstants.ACCOUNT_HEAD_STATUS_FAILED_CODE,
                        ExceptionConstants.ACCOUNT_HEAD_STATUS_FAILED_MSG);
            }
            accountItemService.refreshLastDebtByBillIds(affectedBillIds);
            //记录日志
            if(!noList.isEmpty() && ("0".equals(status) || "1".equals(status))) {
                String statusStr = status.equals("1")?"[审核]":"[反审核]";
                logService.insertLog("财务单据",
                        new StringBuffer(statusStr).append(String.join(", ", noList)).toString(),
                        ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
            }
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void addAccountHeadAndDetail(String beanJson, String rows, HttpServletRequest request) throws Exception {
        AccountHead accountHead = JSONObject.parseObject(beanJson, AccountHead.class);
        validateFinancialType(accountHead.getType());
        checkAccountHeadButtonPermission(accountHead.getType(), "1", "新增");
        if(StringUtil.isEmpty(accountHead.getStatus())) {
            accountHead.setStatus(BusinessConstants.BILLS_STATUS_UN_AUDIT);
        }
        validateSaveStatus(accountHead.getStatus());
        if(BusinessConstants.BILLS_STATUS_AUDIT.equals(accountHead.getStatus())) {
            checkAccountHeadButtonPermission(accountHead.getType(), "2", "审核");
        }
        rows = validateFinancialBill(accountHead, rows, null);
        //校验单号是否重复
        if(checkIsBillNoExist(0L, accountHead.getBillNo())>0) {
            throw new BusinessRunTimeException(ExceptionConstants.ACCOUNT_HEAD_BILL_NO_EXIST_CODE,
                    String.format(ExceptionConstants.ACCOUNT_HEAD_BILL_NO_EXIST_MSG));
        }
        //校验付款账户和明细中的账户重复（转账单据）
        if(BusinessConstants.TYPE_GIRO.equals(accountHead.getType())) {
            JSONArray rowArr = JSONArray.parseArray(rows);
            if (null != rowArr && rowArr.size()>0) {
                for (int i = 0; i < rowArr.size(); i++) {
                    JSONObject object = JSONObject.parseObject(rowArr.getString(i));
                    if (object.get("accountId") != null && !object.get("accountId").equals("")) {
                        Long accoutId = object.getLong("accountId");
                        String accountName = accountMapper.selectByPrimaryKey(accoutId).getName();
                        if(accoutId.equals(accountHead.getAccountId())) {
                            throw new BusinessRunTimeException(ExceptionConstants.ACCOUNT_HEAD_ACCOUNT_REPEAT_CODE,
                                    String.format(ExceptionConstants.ACCOUNT_HEAD_ACCOUNT_REPEAT_MSG, accountName));
                        }
                    }
                }
            }
        }
        User userInfo=userService.getCurrentUser();
        accountHead.setCreator(userInfo==null?null:userInfo.getId());
        try {
            accountHeadMapper.insertSelective(accountHead);
        } catch (DuplicateKeyException e) {
            throw new BusinessRunTimeException(ExceptionConstants.ACCOUNT_HEAD_BILL_NO_EXIST_CODE,
                    ExceptionConstants.ACCOUNT_HEAD_BILL_NO_EXIST_MSG);
        }
        //根据单据编号查询单据id
        AccountHeadExample dhExample = new AccountHeadExample();
        dhExample.createCriteria().andBillNoEqualTo(accountHead.getBillNo()).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<AccountHead> list = accountHeadMapper.selectByExample(dhExample);
        if(list!=null && !list.isEmpty()) {
            Long headId = list.get(0).getId();
            String type = list.get(0).getType();
            /**处理单据子表信息*/
            accountItemService.saveDetials(rows, headId, type, request);
        }
        if("收预付款".equals(accountHead.getType())){
            //更新会员预付款
            supplierService.updateAdvanceIn(accountHead.getOrganId());
        }
        String statusStr = accountHead.getStatus().equals("1")?"[审核]":"";
        logService.insertLog("财务单据",
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_ADD).append(accountHead.getBillNo()).append(statusStr).toString(), request);
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void updateAccountHeadAndDetail(String beanJson, String rows, HttpServletRequest request) throws Exception {
        AccountHead accountHead = JSONObject.parseObject(beanJson, AccountHead.class);
        if(accountHead.getId() == null) {
            throw new BusinessRunTimeException(ExceptionConstants.ACCOUNT_HEAD_DATA_PERMISSION_CODE,
                    ExceptionConstants.ACCOUNT_HEAD_DATA_PERMISSION_MSG);
        }
        AccountHead previousAccountHead = getAccountHead(accountHead.getId());
        if(previousAccountHead == null || BusinessConstants.DELETE_FLAG_DELETED.equals(previousAccountHead.getDeleteFlag())) {
            throw new BusinessRunTimeException(ExceptionConstants.ACCOUNT_HEAD_DATA_PERMISSION_CODE,
                    ExceptionConstants.ACCOUNT_HEAD_DATA_PERMISSION_MSG);
        }
        checkAccountHeadReadPermission(previousAccountHead);
        checkAccountHeadButtonPermission(previousAccountHead.getType(), "1", "编辑");
        if(!BusinessConstants.BILLS_STATUS_UN_AUDIT.equals(previousAccountHead.getStatus())) {
            throw new BusinessRunTimeException(ExceptionConstants.ACCOUNT_HEAD_STATUS_FAILED_CODE,
                    ExceptionConstants.ACCOUNT_HEAD_STATUS_FAILED_MSG);
        }
        if(!previousAccountHead.getType().equals(accountHead.getType())) {
            throw new BusinessRunTimeException(ExceptionConstants.ACCOUNT_HEAD_TYPE_FAILED_CODE,
                    ExceptionConstants.ACCOUNT_HEAD_TYPE_FAILED_MSG);
        }
        if(StringUtil.isEmpty(accountHead.getStatus())) {
            accountHead.setStatus(BusinessConstants.BILLS_STATUS_UN_AUDIT);
        }
        validateSaveStatus(accountHead.getStatus());
        if(BusinessConstants.BILLS_STATUS_AUDIT.equals(accountHead.getStatus())) {
            checkAccountHeadButtonPermission(previousAccountHead.getType(), "2", "审核");
        }
        accountHead.setCreator(previousAccountHead.getCreator());
        rows = validateFinancialBill(accountHead, rows, previousAccountHead.getId());
        //校验单号是否重复
        if(checkIsBillNoExist(accountHead.getId(), accountHead.getBillNo())>0) {
            throw new BusinessRunTimeException(ExceptionConstants.ACCOUNT_HEAD_BILL_NO_EXIST_CODE,
                    String.format(ExceptionConstants.ACCOUNT_HEAD_BILL_NO_EXIST_MSG));
        }
        AccountHeadExample updateExample = new AccountHeadExample();
        updateExample.createCriteria().andIdEqualTo(previousAccountHead.getId())
                .andStatusEqualTo(BusinessConstants.BILLS_STATUS_UN_AUDIT)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        int updateResult;
        try {
            updateResult = accountHeadMapper.updateByExampleSelective(accountHead, updateExample);
        } catch (DuplicateKeyException e) {
            throw new BusinessRunTimeException(ExceptionConstants.ACCOUNT_HEAD_BILL_NO_EXIST_CODE,
                    ExceptionConstants.ACCOUNT_HEAD_BILL_NO_EXIST_MSG);
        }
        if(updateResult != 1) {
            throw new BusinessRunTimeException(ExceptionConstants.ACCOUNT_HEAD_EDIT_FAILED_CODE,
                    ExceptionConstants.ACCOUNT_HEAD_EDIT_FAILED_MSG);
        }
        /**处理单据子表信息*/
        accountItemService.saveDetials(rows, previousAccountHead.getId(), previousAccountHead.getType(), request);
        if("收预付款".equals(accountHead.getType())){
            //更新会员预付款
            supplierService.updateAdvanceIn(accountHead.getOrganId());
        }
        String statusStr = accountHead.getStatus().equals("1")?"[审核]":"";
        logService.insertLog("财务单据",
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append(accountHead.getBillNo()).append(statusStr).toString(), request);
    }

    public List<AccountHeadVo4ListEx> getDetailByNumber(String billNo)throws Exception {
        List<AccountHeadVo4ListEx> resList = new ArrayList<AccountHeadVo4ListEx>();
        List<AccountHeadVo4ListEx> list = null;
        try{
            list = accountHeadMapperEx.getDetailByNumber(billNo);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        if (null != list) {
            for (AccountHeadVo4ListEx ah : list) {
                checkAccountHeadReadPermission(ah);
                if(ah.getChangeAmount() != null) {
                    if(BusinessConstants.TYPE_MONEY_IN.equals(ah.getType())) {
                        ah.setChangeAmount(ah.getChangeAmount());
                    } else if(BusinessConstants.TYPE_MONEY_OUT.equals(ah.getType())) {
                        ah.setChangeAmount(BigDecimal.ZERO.subtract(ah.getChangeAmount()));
                    } else {
                        ah.setChangeAmount(ah.getChangeAmount().abs());
                    }
                }
                if(ah.getTotalPrice() != null) {
                    if(BusinessConstants.TYPE_MONEY_IN.equals(ah.getType())) {
                        ah.setTotalPrice(ah.getTotalPrice());
                    } else if(BusinessConstants.TYPE_MONEY_OUT.equals(ah.getType())) {
                        ah.setTotalPrice(BigDecimal.ZERO.subtract(ah.getTotalPrice()));
                    } else {
                        ah.setTotalPrice(ah.getTotalPrice().abs());
                    }
                }
                if(ah.getBillTime() !=null) {
                    ah.setBillTimeStr(getCenternTime(ah.getBillTime()));
                }
                resList.add(ah);
            }
        }
        return resList;
    }


    /**
     * 根据单据id获取对应的收付款金额
     * @param billId
     * @return
     */
    public BigDecimal getFinancialBillPriceByBillId(Long billId) {
        BigDecimal eachAmount = BigDecimal.ZERO;
        if(billId!=null) {
            List<Long> idList = new ArrayList<>();
            idList.add(billId);
            List<AccountItem> list = getFinancialBillPriceByBillIdList(idList);
            if(list!=null && !list.isEmpty()) {
                for (AccountItem accountItem : list) {
                    if(accountItem!=null && accountItem.getBillId()!=null) {
                        if(accountItem.getBillId().equals(billId)) {
                            eachAmount = accountItem.getEachAmount().abs();
                        }
                    }
                }
            }
        }
        return eachAmount;
    }

    public List<AccountItem> getFinancialBillPriceByBillIdList(List<Long> idList) {
        return accountHeadMapperEx.getFinancialBillPriceByBillIdList(idList);
    }

    public List<AccountHead> getFinancialBillNoByBillId(Long billId) {
        return accountHeadMapperEx.getFinancialBillNoByBillId(billId);
    }
}
