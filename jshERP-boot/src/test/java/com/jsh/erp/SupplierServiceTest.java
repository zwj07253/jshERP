package com.jsh.erp;

import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.Supplier;
import com.jsh.erp.datasource.entities.SupplierExample;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.AccountHeadMapperEx;
import com.jsh.erp.datasource.mappers.AccountItemMapperEx;
import com.jsh.erp.datasource.mappers.DepotHeadMapperEx;
import com.jsh.erp.datasource.mappers.SupplierMapper;
import com.jsh.erp.datasource.mappers.SupplierMapperEx;
import com.jsh.erp.datasource.vo.DepotHeadVo4StatementAccount;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.service.DepotHeadService;
import com.jsh.erp.service.LogService;
import com.jsh.erp.service.SupplierService;
import com.jsh.erp.service.UserBusinessService;
import com.jsh.erp.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock private SupplierMapper supplierMapper;
    @Mock private SupplierMapperEx supplierMapperEx;
    @Mock private UserService userService;
    @Mock private LogService logService;
    @Mock private AccountHeadMapperEx accountHeadMapperEx;
    @Mock private DepotHeadMapperEx depotHeadMapperEx;
    @Mock private AccountItemMapperEx accountItemMapperEx;
    @Mock private DepotHeadService depotHeadService;
    @Mock private UserBusinessService userBusinessService;

    @InjectMocks private SupplierService supplierService;

    @Test
    void rejectsWriteWithoutPermission() throws Exception {
        User user = new User();
        user.setId(101L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasButtonPermission(101L, "/system/vendor", "1")).thenReturn(false);

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> supplierService.insertSupplier(validBody(), null));

        assertEquals(ExceptionConstants.SUPPLIER_PERMISSION_CODE, exception.getCode());
        verify(supplierMapper, never()).insertSelective(any());
    }

    @Test
    void readsLegacyRecordWithNullDeleteFlag() throws Exception {
        Supplier supplier = new Supplier();
        supplier.setId(10L);
        supplier.setSupplier("Legacy Supplier");
        when(supplierMapperEx.getInfoById(10L)).thenReturn(supplier);

        assertEquals(supplier, supplierService.getSupplier(10L));
    }

    @Test
    void insertUsesWhitelistAndClearsProtectedFields() throws Exception {
        User user = new User();
        user.setId(101L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasButtonPermission(101L, "/system/vendor", "1")).thenReturn(true);
        when(supplierMapper.selectByExample(any(SupplierExample.class))).thenReturn(Collections.emptyList());
        when(supplierMapper.insertSelective(any(Supplier.class))).thenReturn(1);

        JSONObject body = validBody();
        body.put("id", 999L);
        body.put("tenantId", 888L);
        body.put("deleteFlag", "1");
        body.put("enabled", false);
        body.put("creator", 777L);

        supplierService.insertSupplier(body, null);

        ArgumentCaptor<Supplier> captor = ArgumentCaptor.forClass(Supplier.class);
        verify(supplierMapper).insertSelective(captor.capture());
        Supplier inserted = captor.getValue();
        assertNull(inserted.getId());
        assertNull(inserted.getTenantId());
        assertNull(inserted.getDeleteFlag());
        assertEquals(Boolean.TRUE, inserted.getEnabled());
        assertEquals(101L, inserted.getCreator());
    }

    @Test
    void loadsCustomerBalancesInOneBatch() throws Exception {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        Supplier first = customer(10L, "Customer A");
        Supplier second = customer(20L, "Customer B");
        when(supplierMapperEx.selectByConditionSupplier(nullable(String.class), eq("客户"),
                nullable(String.class), nullable(String.class), nullable(String.class), nullable(String[].class)))
                .thenReturn(Arrays.asList(first, second));
        DepotHeadVo4StatementAccount statement = new DepotHeadVo4StatementAccount();
        statement.setId(10L);
        statement.setDebtMoney(new java.math.BigDecimal("15.50"));
        when(depotHeadService.getStatementAccount(anyString(), anyString(), isNull(), any(String[].class),
                eq(1), eq("客户"), eq("出库"), eq("销售"), eq("入库"), eq("销售退货"),
                eq("收款"), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(Collections.singletonList(statement));

        supplierService.select(null, "客户", null, null, null);

        assertEquals(new java.math.BigDecimal("15.50"), first.getAllNeedGet());
        assertEquals(java.math.BigDecimal.ZERO, second.getAllNeedGet());
        verify(depotHeadService).getStatementAccount(anyString(), anyString(), isNull(), any(String[].class),
                eq(1), eq("客户"), eq("出库"), eq("销售"), eq("入库"), eq("销售退货"),
                eq("收款"), isNull(), isNull(), isNull(), isNull());
        RequestContextHolder.resetRequestAttributes();
    }

    private Supplier customer(Long id, String name) {
        Supplier supplier = new Supplier();
        supplier.setId(id);
        supplier.setSupplier(name);
        supplier.setType("客户");
        return supplier;
    }

    private JSONObject validBody() {
        JSONObject body = new JSONObject();
        body.put("supplier", "Supplier A");
        body.put("type", "供应商");
        body.put("beginNeedPay", 10);
        body.put("taxRate", 13);
        body.put("sort", "1");
        return body;
    }
}
