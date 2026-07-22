package com.jsh.erp;

import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.Account;
import com.jsh.erp.datasource.entities.DepotHead;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.service.AccountService;
import com.jsh.erp.service.DepotHeadService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepotHeadSettlementAccountTest {

    @Mock private AccountService accountService;
    @InjectMocks private DepotHeadService depotHeadService;

    @Test
    void acceptsValidSignedMultiAccountSettlement() throws Exception {
        when(accountService.getAccount(anyLong())).thenReturn(enabledAccount());
        DepotHead head = settlementHead("1,2", "-40,-60", "-100");

        assertDoesNotThrow(() -> invokeValidation(head, true));
        assertEquals(null, head.getAccountId());
        assertEquals("1,2", head.getAccountIdList());
    }

    @Test
    void rejectsDuplicateMultiAccountIds() {
        DepotHead head = settlementHead("1,1", "50,50", "100");

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> invokeValidation(head, true));

        assertEquals(ExceptionConstants.DEPOT_HEAD_SETTLEMENT_ACCOUNT_INVALID_CODE, exception.getCode());
    }

    @Test
    void rejectsMissingRequiredAccount() {
        DepotHead head = settlementHead(null, null, "100");

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> invokeValidation(head, true));

        assertEquals(ExceptionConstants.DEPOT_HEAD_SETTLEMENT_ACCOUNT_INVALID_CODE, exception.getCode());
    }

    private void invokeValidation(DepotHead head, boolean required) throws Exception {
        Method method = DepotHeadService.class.getDeclaredMethod(
                "validateSettlementAccounts", DepotHead.class, boolean.class);
        method.setAccessible(true);
        try {
            method.invoke(depotHeadService, head, required);
        } catch(InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if(cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw exception;
        }
    }

    private DepotHead settlementHead(String accountIds, String accountMoney, String changeAmount) {
        DepotHead head = new DepotHead();
        head.setAccountIdList(accountIds);
        head.setAccountMoneyList(accountMoney);
        head.setChangeAmount(new BigDecimal(changeAmount));
        return head;
    }

    private Account enabledAccount() {
        Account account = new Account();
        account.setEnabled(true);
        account.setDeleteFlag("0");
        return account;
    }
}
