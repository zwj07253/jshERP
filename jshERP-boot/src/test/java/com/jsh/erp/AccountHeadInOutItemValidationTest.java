package com.jsh.erp;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.datasource.entities.Account;
import com.jsh.erp.datasource.entities.AccountHead;
import com.jsh.erp.datasource.entities.InOutItem;
import com.jsh.erp.datasource.mappers.AccountMapper;
import com.jsh.erp.datasource.vo.AccountItemVo4List;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.service.AccountHeadService;
import com.jsh.erp.service.AccountItemService;
import com.jsh.erp.service.InOutItemService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountHeadInOutItemValidationTest {

    @Mock
    private AccountMapper accountMapper;
    @Mock
    private AccountItemService accountItemService;
    @Mock
    private InOutItemService inOutItemService;

    @InjectMocks
    private AccountHeadService accountHeadService;

    @Test
    void allowsExistingDisabledItemButRejectsItForNewBill() throws Exception {
        Account account = new Account();
        account.setId(3L);
        account.setEnabled(true);
        account.setDeleteFlag("0");
        when(accountMapper.selectByPrimaryKey(3L)).thenReturn(account);

        InOutItem disabledItem = new InOutItem();
        disabledItem.setId(5L);
        disabledItem.setType("收入");
        disabledItem.setEnabled(false);
        disabledItem.setDeleteFlag("0");
        when(inOutItemService.getInOutItem(5L)).thenReturn(disabledItem);

        AccountItemVo4List existingDetail = new AccountItemVo4List();
        existingDetail.setInOutItemId(5L);
        when(accountItemService.getDetailList(9L)).thenReturn(Collections.singletonList(existingDetail));

        AccountHead head = new AccountHead();
        head.setType("收入");
        head.setBillNo("SR-1");
        head.setBillTime(new Date());
        head.setAccountId(3L);
        head.setTotalPrice(new BigDecimal("100"));
        head.setChangeAmount(new BigDecimal("100"));
        head.setDiscountMoney(BigDecimal.ZERO);

        JSONObject row = new JSONObject();
        row.put("inOutItemId", 5L);
        row.put("eachAmount", new BigDecimal("100"));
        String rows = new JSONArray(Collections.singletonList(row)).toJSONString();

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(accountHeadService,
                "validateFinancialBill", head, rows, 9L));
        assertThrows(BusinessRunTimeException.class, () -> ReflectionTestUtils.invokeMethod(accountHeadService,
                "validateFinancialBill", head, rows, null));
    }
}
