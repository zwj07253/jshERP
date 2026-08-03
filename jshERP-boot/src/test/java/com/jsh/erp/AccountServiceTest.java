package com.jsh.erp;

import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.Account;
import com.jsh.erp.datasource.entities.DepotHead;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.AccountHeadMapper;
import com.jsh.erp.datasource.mappers.AccountHeadMapperEx;
import com.jsh.erp.datasource.mappers.AccountItemMapper;
import com.jsh.erp.datasource.mappers.AccountItemMapperEx;
import com.jsh.erp.datasource.mappers.AccountMapper;
import com.jsh.erp.datasource.mappers.AccountMapperEx;
import com.jsh.erp.datasource.mappers.DepotHeadMapper;
import com.jsh.erp.datasource.mappers.DepotHeadMapperEx;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.service.AccountService;
import com.jsh.erp.service.LogService;
import com.jsh.erp.service.SystemConfigService;
import com.jsh.erp.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock private AccountMapper accountMapper;
    @Mock private AccountMapperEx accountMapperEx;
    @Mock private DepotHeadMapper depotHeadMapper;
    @Mock private DepotHeadMapperEx depotHeadMapperEx;
    @Mock private AccountHeadMapper accountHeadMapper;
    @Mock private AccountHeadMapperEx accountHeadMapperEx;
    @Mock private AccountItemMapper accountItemMapper;
    @Mock private AccountItemMapperEx accountItemMapperEx;
    @Mock private LogService logService;
    @Mock private UserService userService;
    @Mock private SystemConfigService systemConfigService;

    @InjectMocks private AccountService accountService;

    @Test
    void rejectsWriteWithoutAccountPermission() throws Exception {
        stubUser(false);

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> accountService.insertAccount(validAccount(), null));

        assertEquals(ExceptionConstants.ACCOUNT_PERMISSION_CODE, exception.getCode());
        verify(accountMapper, never()).insertSelective(any());
    }

    @Test
    void insertUsesOnlyAllowedFields() throws Exception {
        stubUser(true);
        JSONObject request = validAccount();
        request.put("id", 99L);
        request.put("tenantId", 88L);
        request.put("deleteFlag", "1");
        request.put("enabled", false);
        request.put("isDefault", false);
        request.put("currentAmount", "999999");
        when(accountMapper.selectByExample(any())).thenReturn(Collections.emptyList());
        when(accountMapper.insertSelective(any())).thenReturn(1);

        accountService.insertAccount(request, null);

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountMapper).insertSelective(captor.capture());
        Account inserted = captor.getValue();
        assertNull(inserted.getId());
        assertNull(inserted.getTenantId());
        assertNull(inserted.getDeleteFlag());
        assertNull(inserted.getCurrentAmount());
        assertEquals(true, inserted.getEnabled());
        assertEquals(true, inserted.getIsDefault());
    }

    @Test
    void updateCannotOverwriteSystemFields() throws Exception {
        stubUser(true);
        Account existing = account(10L, false);
        when(accountMapper.selectByPrimaryKey(10L)).thenReturn(existing);
        when(accountMapper.selectByExample(any())).thenReturn(Collections.emptyList());
        when(accountMapper.updateByPrimaryKeySelective(any())).thenReturn(1);
        JSONObject request = validAccount();
        request.put("id", 10L);
        request.put("tenantId", 88L);
        request.put("deleteFlag", "1");
        request.put("enabled", false);
        request.put("isDefault", true);
        request.put("currentAmount", "999999");

        accountService.updateAccount(request, null);

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountMapper).updateByPrimaryKeySelective(captor.capture());
        Account updated = captor.getValue();
        assertEquals(10L, updated.getId());
        assertNull(updated.getTenantId());
        assertNull(updated.getDeleteFlag());
        assertNull(updated.getEnabled());
        assertNull(updated.getIsDefault());
        assertNull(updated.getCurrentAmount());
    }

    @Test
    void rejectsDisablingDefaultAccount() throws Exception {
        stubUser(true);
        when(accountMapper.selectByExample(any())).thenReturn(Collections.singletonList(account(10L, true)));

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> accountService.batchSetStatus(false, "10"));

        assertEquals(ExceptionConstants.ACCOUNT_DEFAULT_OPERATION_CODE, exception.getCode());
        verify(accountMapper, never()).updateByExampleSelective(any(), any());
    }

    @Test
    void rejectsDeletingAccountReferencedByMultiAccountBill() throws Exception {
        stubUser(true);
        when(accountMapper.selectByExample(any())).thenReturn(Collections.singletonList(account(10L, false)));
        when(accountHeadMapperEx.getAccountHeadListByAccountIds(any())).thenReturn(Collections.emptyList());
        when(accountItemMapperEx.getAccountItemListByAccountIds(any())).thenReturn(Collections.emptyList());
        when(depotHeadMapperEx.getDepotHeadListByAccountIds(any()))
                .thenReturn(Collections.singletonList(new DepotHead()));

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> accountService.deleteAccount(10L, null));

        assertEquals(ExceptionConstants.ACCOUNT_IN_USE_CODE, exception.getCode());
        verify(accountMapperEx, never()).batchDeleteAccountByIds(any(), any(), any());
    }

    @Test
    void rejectsChangingInitialAmountAfterAccountIsUsed() throws Exception {
        stubUser(true);
        Account existing = account(10L, false);
        when(accountMapper.selectByPrimaryKey(10L)).thenReturn(existing);
        when(accountMapper.selectByExample(any())).thenReturn(Collections.emptyList());
        when(accountHeadMapperEx.getAccountHeadListByAccountIds(any()))
                .thenReturn(Collections.singletonList(new com.jsh.erp.datasource.entities.AccountHead()));
        JSONObject request = validAccount();
        request.put("id", 10L);
        request.put("initialAmount", "200");

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> accountService.updateAccount(request, null));

        assertEquals(ExceptionConstants.ACCOUNT_IN_USE_CODE, exception.getCode());
        verify(accountMapper, never()).updateByPrimaryKeySelective(any());
    }

    private JSONObject validAccount() {
        JSONObject request = new JSONObject();
        request.put("name", "现金账户");
        request.put("serialNo", "CASH-01");
        request.put("initialAmount", "100");
        request.put("sort", "1");
        request.put("remark", "测试账户");
        return request;
    }

    private void stubUser(boolean allowed) throws Exception {
        User user = new User();
        user.setId(101L);
        user.setTenantId(7L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasButtonPermission(101L, "/system/account", "1")).thenReturn(allowed);
    }

    private Account account(Long id, boolean isDefault) {
        Account account = new Account();
        account.setId(id);
        account.setName("现金账户");
        account.setInitialAmount(new BigDecimal("100"));
        account.setEnabled(true);
        account.setIsDefault(isDefault);
        account.setDeleteFlag("0");
        return account;
    }
}
