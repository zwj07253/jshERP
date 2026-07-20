package com.jsh.erp.service;

import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.base.PageDomain;
import com.jsh.erp.base.TableSupport;
import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.*;
import com.jsh.erp.datasource.mappers.*;
import com.jsh.erp.datasource.vo.AccountVo4InOutList;
import com.jsh.erp.datasource.vo.AccountVo4List;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.exception.JshException;
import com.jsh.erp.utils.PageUtils;
import com.jsh.erp.utils.StringUtil;
import com.jsh.erp.utils.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;

@Service
public class AccountService {
    private Logger logger = LoggerFactory.getLogger(AccountService.class);

    @Resource
    private AccountMapper accountMapper;

    @Resource
    private AccountMapperEx accountMapperEx;

    @Resource
    private DepotHeadMapper depotHeadMapper;
    @Resource
    private DepotHeadMapperEx depotHeadMapperEx;

    @Resource
    private AccountHeadMapper accountHeadMapper;
    @Resource
    private AccountHeadMapperEx accountHeadMapperEx;

    @Resource
    private AccountItemMapper accountItemMapper;
    @Resource
    private AccountItemMapperEx accountItemMapperEx;
    @Resource
    private LogService logService;
    @Resource
    private UserService userService;
    @Resource
    private SystemConfigService systemConfigService;

    public Account getAccount(long id) throws Exception{
        return accountMapper.selectByPrimaryKey(id);
    }

    public void checkAccountReportPermission() throws Exception {
        User currentUser = userService.getCurrentUser();
        Long userId = currentUser == null ? null : currentUser.getId();
        if(!userService.hasFunctionPermission(userId, "/report/account_report")) {
            throw new BusinessRunTimeException(ExceptionConstants.ACCOUNT_REPORT_PERMISSION_CODE,
                    ExceptionConstants.ACCOUNT_REPORT_PERMISSION_MSG);
        }
    }

    public Account checkAccountForReport(Long accountId) throws Exception {
        Account account = accountId == null ? null : accountMapper.selectByPrimaryKey(accountId);
        if(account == null || BusinessConstants.DELETE_FLAG_DELETED.equals(account.getDeleteFlag())) {
            throw new BusinessRunTimeException(ExceptionConstants.ACCOUNT_REPORT_ACCOUNT_FAILED_CODE,
                    ExceptionConstants.ACCOUNT_REPORT_ACCOUNT_FAILED_MSG);
        }
        return account;
    }

    public List<Account> getAccountListByIds(String ids)throws Exception {
        List<Long> idList = StringUtil.strToLongList(ids);
        List<Account> list = new ArrayList<>();
        try{
            AccountExample example = new AccountExample();
            example.createCriteria().andIdIn(idList);
            list = accountMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<Account> getAccount() throws Exception{
        List<Account> list=null;
        try{
            AccountExample example = new AccountExample();
            example.createCriteria().andEnabledEqualTo(true).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
            example.setOrderByClause("sort asc, id desc");
            list=accountMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<Account> getAccountByParam(String name, String serialNo) throws Exception{
        List<Account> list=null;
        try{
            list=accountMapperEx.getAccountByParam(name, serialNo);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<AccountVo4List> select(String name, String serialNo, String remark) throws Exception{
        PageUtils.startPage();
        List<AccountVo4List> list = accountMapperEx.selectByConditionAccount(name, serialNo, remark);
        PageDomain pageDomain = TableSupport.buildPageRequest();
        int offset = (pageDomain.getCurrentPage() - 1) * pageDomain.getPageSize();
        fillAccountBalances(list, name, serialNo, offset, pageDomain.getPageSize());
        return list;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int insertAccount(JSONObject obj, HttpServletRequest request)throws Exception {
        Account account = JSONObject.parseObject(obj.toJSONString(), Account.class);
        if(account.getInitialAmount() == null) {
            account.setInitialAmount(BigDecimal.ZERO);
        }
        List<Account> accountList = getAccountByParam(null, null);
        if(accountList.size() == 0) {
            account.setIsDefault(true);
        } else {
            account.setIsDefault(false);
        }
        account.setEnabled(true);
        int result=0;
        try{
            result = accountMapper.insertSelective(account);
            logService.insertLog("账户",
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_ADD).append(account.getName()).toString(), request);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int updateAccount(JSONObject obj, HttpServletRequest request)throws Exception {
        Account account = JSONObject.parseObject(obj.toJSONString(), Account.class);
        int result=0;
        try{
            result = accountMapper.updateByPrimaryKeySelective(account);
            logService.insertLog("账户",
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append(account.getName()).toString(), request);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int deleteAccount(Long id, HttpServletRequest request) throws Exception{
        return batchDeleteAccountByIds(id.toString());
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteAccount(String ids, HttpServletRequest request)throws Exception {
        return batchDeleteAccountByIds(ids);
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteAccountByIds(String ids) throws Exception{
        int result=0;
        String [] idArray=ids.split(",");
        //校验财务主表	jsh_accounthead
        List<AccountHead> accountHeadList=null;
        try{
            accountHeadList = accountHeadMapperEx.getAccountHeadListByAccountIds(idArray);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        if(accountHeadList!=null&&accountHeadList.size()>0){
            logger.error("异常码[{}],异常提示[{}],参数,AccountIds[{}]",
                    ExceptionConstants.DELETE_FORCE_CONFIRM_CODE,ExceptionConstants.DELETE_FORCE_CONFIRM_MSG,ids);
            throw new BusinessRunTimeException(ExceptionConstants.DELETE_FORCE_CONFIRM_CODE,
                    ExceptionConstants.DELETE_FORCE_CONFIRM_MSG);
        }
        //校验财务子表	jsh_accountitem
        List<AccountItem> accountItemList=null;
        try{
            accountItemList = accountItemMapperEx.getAccountItemListByAccountIds(idArray);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        if(accountItemList!=null&&accountItemList.size()>0){
            logger.error("异常码[{}],异常提示[{}],参数,AccountIds[{}]",
                    ExceptionConstants.DELETE_FORCE_CONFIRM_CODE,ExceptionConstants.DELETE_FORCE_CONFIRM_MSG,ids);
            throw new BusinessRunTimeException(ExceptionConstants.DELETE_FORCE_CONFIRM_CODE,
                    ExceptionConstants.DELETE_FORCE_CONFIRM_MSG);
        }
        //校验单据主表	jsh_depot_head
        List<DepotHead> depotHeadList =null;
        try{
            depotHeadList = depotHeadMapperEx.getDepotHeadListByAccountIds(idArray);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        if(depotHeadList!=null&&depotHeadList.size()>0){
            logger.error("异常码[{}],异常提示[{}],参数,AccountIds[{}]",
                    ExceptionConstants.DELETE_FORCE_CONFIRM_CODE,ExceptionConstants.DELETE_FORCE_CONFIRM_MSG,ids);
            throw new BusinessRunTimeException(ExceptionConstants.DELETE_FORCE_CONFIRM_CODE,
                    ExceptionConstants.DELETE_FORCE_CONFIRM_MSG);
        }
        //记录日志
        StringBuffer sb = new StringBuffer();
        sb.append(BusinessConstants.LOG_OPERATION_TYPE_DELETE);
        List<Account> list = getAccountListByIds(ids);
        for(Account account: list){
            sb.append("[").append(account.getName()).append("]");
        }
        logService.insertLog("账户", sb.toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        User userInfo=userService.getCurrentUser();
        //校验通过执行删除操作
        try{
            result = accountMapperEx.batchDeleteAccountByIds(new Date(),userInfo==null?null:userInfo.getId(),idArray);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    public int checkIsNameExist(Long id, String name)throws Exception {
        AccountExample example = new AccountExample();
        example.createCriteria().andIdNotEqualTo(id).andNameEqualTo(name).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<Account> list=null;
        try{
            list = accountMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list==null?0:list.size();
    }

    public List<Account> findBySelect()throws Exception {
        AccountExample example = new AccountExample();
        example.createCriteria().andEnabledEqualTo(true).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        example.setOrderByClause("sort asc, id desc");
        List<Account> list=null;
        try{
            list = accountMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    /**
     * 单个账户的金额求和-入库和出库
     * @return
     */
    public BigDecimal getAccountSum(Long accountId, String beginTime, String endTime, Boolean forceFlag) {
        return accountMapperEx.getAccountSum(accountId, beginTime, endTime, forceFlag);
    }

    /**
     * 单个账户的金额求和-收入、支出、转账的单据表头的合计
     * @return
     */
    public BigDecimal getAccountSumByHead(Long accountId, String beginTime, String endTime, Boolean forceFlag) {
        return accountMapperEx.getAccountSumByHead(accountId, beginTime, endTime, forceFlag);
    }

    /**
     * 单个账户的金额求和-收款、付款、转账、收预付款的单据明细的合计
     * @return
     */
    public BigDecimal getAccountSumByDetail(Long accountId, String beginTime, String endTime, Boolean forceFlag) {
        return accountMapperEx.getAccountSumByDetail(accountId, beginTime, endTime, forceFlag);
    }

    /**
     * 单个账户的金额求和-多账户的明细合计
     * @return
     */
    public BigDecimal getManyAccountSum(Long accountId, String beginTime, String endTime, Boolean forceFlag) {
        BigDecimal accountSum = BigDecimal.ZERO;
        List<DepotHead> dataList = accountMapperEx.getManyAccountSum(accountId, beginTime, endTime, forceFlag);
        if (dataList != null) {
            for (DepotHead depotHead : dataList) {
                if(depotHead != null) {
                    accountSum = accountSum.add(getAccountAmountFromLists(accountId,
                            depotHead.getAccountIdList(), depotHead.getAccountMoneyList()));
                }
            }
        }
        return accountSum;
    }

    /**
     * 单个账户的金额求和-多账户的明细合计(格式化)
     * @return
     */
    public BigDecimal getManyAccountSumParse(Long accountId, List<DepotHead> manyAmountList) {
        BigDecimal accountSum = BigDecimal.ZERO;
        if (manyAmountList != null) {
            for (DepotHead depotHead : manyAmountList) {
                accountSum = accountSum.add(getAccountAmountFromLists(accountId,
                        depotHead.getAccountIdList(), depotHead.getAccountMoneyList()));
            }
        }
        return accountSum;
    }

    public List<AccountVo4InOutList> findAccountInOutList(Long accountId, String number, String beginTime, String endTime,
                                                           Boolean forceFlag) throws Exception{
        Account account = checkAccountForReport(accountId);
        List<AccountVo4InOutList> allEntries = accountMapperEx.findAccountInOutList(accountId, forceFlag);
        BigDecimal runningBalance = zeroIfNull(account.getInitialAmount());
        for(int index = allEntries.size() - 1; index >= 0; index--) {
            AccountVo4InOutList entry = allEntries.get(index);
            BigDecimal changeAmount = entry.getChangeAmount();
            if(StringUtil.isNotEmpty(entry.getaList())) {
                changeAmount = getAccountAmountFromLists(accountId, entry.getaList(), entry.getAmList());
                entry.setChangeAmount(changeAmount);
            }
            runningBalance = runningBalance.add(zeroIfNull(changeAmount));
            entry.setBalance(runningBalance);
        }
        List<AccountVo4InOutList> filteredEntries = new ArrayList<>();
        for(AccountVo4InOutList entry : allEntries) {
            if(StringUtil.isNotEmpty(number) && (entry.getNumber() == null || !entry.getNumber().contains(number))) {
                continue;
            }
            if(StringUtil.isNotEmpty(beginTime) && entry.getOperTime().compareTo(beginTime) < 0) {
                continue;
            }
            if(StringUtil.isNotEmpty(endTime) && entry.getOperTime().compareTo(endTime) > 0) {
                continue;
            }
            filteredEntries.add(entry);
        }
        return filteredEntries;
    }

    private BigDecimal getAccountAmountFromLists(Long accountId, String accountIdList, String accountMoneyList) {
        if(accountId == null || StringUtil.isEmpty(accountIdList) || StringUtil.isEmpty(accountMoneyList)) {
            return BigDecimal.ZERO;
        }
        String[] accountIds = accountIdList.split(",");
        String[] accountAmounts = accountMoneyList.split(",");
        for(int index = 0; index < accountIds.length; index++) {
            if(accountId.toString().equals(accountIds[index].trim()) && index < accountAmounts.length) {
                try {
                    return new BigDecimal(accountAmounts[index].trim());
                } catch (NumberFormatException e) {
                    logger.warn("忽略非法的多账户金额，accountId={}, amount={}", accountId, accountAmounts[index]);
                    return BigDecimal.ZERO;
                }
            }
        }
        return BigDecimal.ZERO;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int updateIsDefault(Long accountId) throws Exception{
        int result=0;
        try{
            //全部取消默认
            Account allAccount = new Account();
            allAccount.setIsDefault(false);
            AccountExample allExample = new AccountExample();
            allExample.createCriteria();
            accountMapper.updateByExampleSelective(allAccount, allExample);
            //给指定账户设为默认
            Account account = new Account();
            account.setIsDefault(true);
            AccountExample example = new AccountExample();
            example.createCriteria().andIdEqualTo(accountId);
            accountMapper.updateByExampleSelective(account, example);
            logService.insertLog("账户",BusinessConstants.LOG_OPERATION_TYPE_EDIT+accountId,
                    ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
            result = 1;
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    public Map<Long,String> getAccountMap() throws Exception {
        List<Account> accountList = getAccount();
        Map<Long,String> accountMap = new HashMap<>();
        for(Account account : accountList){
            accountMap.put(account.getId(), account.getName());
        }
        return accountMap;
    }

    public String getAccountStrByIdAndMoney(Map<Long,String> accountMap, String accountIdList, String accountMoneyList){
        StringBuffer sb = new StringBuffer();
        List<Long> idList = StringUtil.strToLongList(accountIdList);
        List<BigDecimal> moneyList = StringUtil.strToBigDecimalList(accountMoneyList);
        for (int i = 0; i < idList.size(); i++) {
            Long id = idList.get(i);
            BigDecimal money =  moneyList.get(i).abs();
            sb.append(accountMap.get(id) + "(" + money + "元) ");
        }
        return sb.toString();
    }

    public List<AccountVo4List> listWithBalance(String name, String serialNo) throws Exception {
        PageUtils.startPage();
        List<AccountVo4List> list = accountMapperEx.selectByConditionAccount(name, serialNo, null);
        PageDomain pageDomain = TableSupport.buildPageRequest();
        int offset = (pageDomain.getCurrentPage() - 1) * pageDomain.getPageSize();
        fillAccountBalances(list, name, serialNo, offset, pageDomain.getPageSize());
        return list;
    }

    public Map<String, Object> getStatistics(String name, String serialNo) throws Exception {
        Map<String, Object> map = new HashMap<>();
        List<AccountVo4List> list = accountMapperEx.selectByConditionAccount(name, serialNo, null);
        fillAccountBalances(list, name, serialNo, null, null);
        BigDecimal allMonthAmount = BigDecimal.ZERO;
        BigDecimal allCurrentAmount = BigDecimal.ZERO;
        for (AccountVo4List account : list) {
            allMonthAmount = allMonthAmount.add(new BigDecimal(account.getThisMonthAmount()));
            allCurrentAmount = allCurrentAmount.add(zeroIfNull(account.getCurrentAmount()));
        }
        map.put("allMonthAmount", priceFormat(allMonthAmount));  //本月净发生额
        map.put("allCurrentAmount", priceFormat(allCurrentAmount));  //当前总金额
        return map;
    }

    private void fillAccountBalances(List<AccountVo4List> accounts, String name, String serialNo,
                                     Integer offset, Integer rows) throws Exception {
        if(accounts == null || accounts.isEmpty()) {
            return;
        }
        String timeStr = Tools.getCurrentMonth();
        String beginTime = Tools.firstDayOfMonth(timeStr) + BusinessConstants.DAY_FIRST_TIME;
        String endTime = Tools.lastDayOfMonth(timeStr) + BusinessConstants.DAY_LAST_TIME;
        Boolean forceFlag = systemConfigService.getForceApprovalFlag();
        Map<Long, AccountVo4Sum> monthSums = toAccountSumMap(accountMapperEx.getAccountSumByParam(
                name, serialNo, beginTime, endTime, forceFlag, offset, rows));
        Map<Long, AccountVo4Sum> currentSums = toAccountSumMap(accountMapperEx.getAccountSumByParam(
                name, serialNo, null, null, forceFlag, offset, rows));
        List<DepotHead> monthManyAmounts = accountMapperEx.getManyAccountSumByParam(beginTime, endTime, forceFlag);
        List<DepotHead> currentManyAmounts = accountMapperEx.getManyAccountSumByParam(null, null, forceFlag);
        for(AccountVo4List account : accounts) {
            AccountVo4Sum month = monthSums.get(account.getId());
            AccountVo4Sum current = currentSums.get(account.getId());
            BigDecimal monthAmount = sumAccountAmounts(month)
                    .add(getManyAccountSumParse(account.getId(), monthManyAmounts));
            BigDecimal currentAmount = sumAccountAmounts(current)
                    .add(getManyAccountSumParse(account.getId(), currentManyAmounts))
                    .add(zeroIfNull(account.getInitialAmount()));
            account.setThisMonthAmount(priceFormat(monthAmount));
            account.setCurrentAmount(currentAmount);
        }
    }

    private Map<Long, AccountVo4Sum> toAccountSumMap(List<AccountVo4Sum> sums) {
        Map<Long, AccountVo4Sum> result = new HashMap<>();
        if(sums != null) {
            for(AccountVo4Sum sum : sums) {
                result.put(sum.getId(), sum);
            }
        }
        return result;
    }

    private BigDecimal sumAccountAmounts(AccountVo4Sum sum) {
        if(sum == null) {
            return BigDecimal.ZERO;
        }
        return zeroIfNull(sum.getAccountSum())
                .add(zeroIfNull(sum.getAccountSumByHead()))
                .add(zeroIfNull(sum.getAccountSumByDetail()));
    }

    private BigDecimal zeroIfNull(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    /**
     * 价格格式化
     * @param price
     * @return
     */
    private String priceFormat(BigDecimal price) {
        String priceFmt = "0";
        DecimalFormat df = new DecimalFormat(".##");
        if ((price.compareTo(BigDecimal.ZERO))!=0) {
            priceFmt = df.format(price);
        }
        return priceFmt;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchSetStatus(Boolean status, String ids)throws Exception {
        logService.insertLog("账户",
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_ENABLED).toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        List<Long> accountIds = StringUtil.strToLongList(ids);
        Account account = new Account();
        account.setEnabled(status);
        AccountExample example = new AccountExample();
        example.createCriteria().andIdIn(accountIds);
        int result=0;
        try{
            result = accountMapper.updateByExampleSelective(account, example);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }
}
