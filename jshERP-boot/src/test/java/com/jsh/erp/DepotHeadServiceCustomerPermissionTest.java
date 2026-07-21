package com.jsh.erp;

import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.vo.SupplierSimple;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.service.DepotHeadService;
import com.jsh.erp.service.SupplierService;
import com.jsh.erp.service.SystemConfigService;
import com.jsh.erp.service.UserBusinessService;
import com.jsh.erp.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepotHeadServiceCustomerPermissionTest {

    @Mock
    private UserService userService;

    @Mock
    private UserBusinessService userBusinessService;

    @Mock
    private SupplierService supplierService;

    @Mock
    private SystemConfigService systemConfigService;

    @InjectMocks
    private DepotHeadService depotHeadService;

    @Test
    void adminCanViewAllCustomersInSalesReport() throws Exception {
        User admin = new User();
        admin.setLoginName("admin");
        when(userService.getCurrentUser()).thenReturn(admin);

        assertNull(depotHeadService.getOrganArray("销售", ""));
    }

    @Test
    void rejectsUserWithoutInDetailReportPermission() throws Exception {
        User user = new User();
        user.setId(101L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasFunctionPermission(101L, "/report/in_detail")).thenReturn(false);

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> depotHeadService.checkInOutDetailReportPermission("入库"));

        assertEquals(ExceptionConstants.DEPOT_HEAD_IN_DETAIL_REPORT_PERMISSION_CODE, exception.getCode());
    }

    @Test
    void allowsUserWithInDetailReportPermission() throws Exception {
        User user = new User();
        user.setId(101L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasFunctionPermission(101L, "/report/in_detail")).thenReturn(true);

        assertDoesNotThrow(() -> depotHeadService.checkInOutDetailReportPermission("入库"));
    }

    @Test
    void rejectsUserWithoutOutDetailReportPermission() throws Exception {
        User user = new User();
        user.setId(101L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasFunctionPermission(101L, "/report/out_detail")).thenReturn(false);

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> depotHeadService.checkInOutDetailReportPermission("出库"));

        assertEquals(ExceptionConstants.DEPOT_HEAD_OUT_DETAIL_REPORT_PERMISSION_CODE, exception.getCode());
    }

    @Test
    void allowsUserWithOutDetailReportPermission() throws Exception {
        User user = new User();
        user.setId(101L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasFunctionPermission(101L, "/report/out_detail")).thenReturn(true);

        assertDoesNotThrow(() -> depotHeadService.checkInOutDetailReportPermission("出库"));
    }

    @Test
    void rejectsUserWithoutAllocationDetailReportPermission() throws Exception {
        User user = new User();
        user.setId(101L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasFunctionPermission(101L, "/report/allocation_detail")).thenReturn(false);

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> depotHeadService.checkAllocationDetailReportPermission());

        assertEquals(ExceptionConstants.DEPOT_HEAD_ALLOCATION_DETAIL_REPORT_PERMISSION_CODE, exception.getCode());
    }

    @Test
    void allowsUserWithAllocationDetailReportPermission() throws Exception {
        User user = new User();
        user.setId(101L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasFunctionPermission(101L, "/report/allocation_detail")).thenReturn(true);

        assertDoesNotThrow(() -> depotHeadService.checkAllocationDetailReportPermission());
    }

    @Test
    void rejectsInvalidMaterialCountType() {
        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> depotHeadService.checkInOutMaterialCountReportPermission("其它"));

        assertEquals(ExceptionConstants.DEPOT_HEAD_MATERIAL_COUNT_TYPE_INVALID_CODE, exception.getCode());
    }

    @Test
    void rejectsUserWithoutInMaterialCountReportPermission() throws Exception {
        User user = new User();
        user.setId(101L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasFunctionPermission(101L, "/report/in_material_count")).thenReturn(false);

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> depotHeadService.checkInOutMaterialCountReportPermission("入库"));

        assertEquals(ExceptionConstants.DEPOT_HEAD_IN_MATERIAL_COUNT_REPORT_PERMISSION_CODE, exception.getCode());
    }

    @Test
    void allowsUserWithInMaterialCountReportPermission() throws Exception {
        User user = new User();
        user.setId(101L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasFunctionPermission(101L, "/report/in_material_count")).thenReturn(true);

        assertDoesNotThrow(() -> depotHeadService.checkInOutMaterialCountReportPermission("入库"));
    }

    @Test
    void rejectsUserWithoutOutMaterialCountReportPermission() throws Exception {
        User user = new User();
        user.setId(101L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasFunctionPermission(101L, "/report/out_material_count")).thenReturn(false);

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> depotHeadService.checkInOutMaterialCountReportPermission("出库"));

        assertEquals(ExceptionConstants.DEPOT_HEAD_OUT_MATERIAL_COUNT_REPORT_PERMISSION_CODE, exception.getCode());
    }

    @Test
    void allowsUserWithOutMaterialCountReportPermission() throws Exception {
        User user = new User();
        user.setId(101L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasFunctionPermission(101L, "/report/out_material_count")).thenReturn(true);

        assertDoesNotThrow(() -> depotHeadService.checkInOutMaterialCountReportPermission("出库"));
    }

    @Test
    void rejectsInvalidStatementAccountType() {
        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> depotHeadService.checkStatementAccountPermission("会员"));

        assertEquals(ExceptionConstants.DEPOT_HEAD_STATEMENT_ACCOUNT_TYPE_INVALID_CODE, exception.getCode());
    }

    @Test
    void allowsCustomerAccountReportPermission() throws Exception {
        User user = user(101L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasFunctionPermission(101L, "/report/customer_account")).thenReturn(true);

        assertDoesNotThrow(() -> depotHeadService.checkStatementAccountPermission("客户"));
    }

    @Test
    void allowsMoneyInPermissionForSharedCustomerAccountEndpoint() throws Exception {
        User user = user(101L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasFunctionPermission(101L, "/report/customer_account")).thenReturn(false);
        when(userService.hasFunctionPermission(101L, "/financial/money_in")).thenReturn(true);

        assertDoesNotThrow(() -> depotHeadService.checkStatementAccountPermission("客户"));
    }

    @Test
    void rejectsCustomerAccountWithoutReportOrMoneyInPermission() throws Exception {
        User user = user(101L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasFunctionPermission(101L, "/report/customer_account")).thenReturn(false);
        when(userService.hasFunctionPermission(101L, "/financial/money_in")).thenReturn(false);

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> depotHeadService.checkStatementAccountPermission("客户"));

        assertEquals(ExceptionConstants.DEPOT_HEAD_CUSTOMER_ACCOUNT_PERMISSION_CODE, exception.getCode());
    }

    @Test
    void allowsVendorAccountReportPermission() throws Exception {
        User user = user(101L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasFunctionPermission(101L, "/report/vendor_account")).thenReturn(true);

        assertDoesNotThrow(() -> depotHeadService.checkStatementAccountPermission("供应商"));
    }

    @Test
    void allowsMoneyOutPermissionForSharedVendorAccountEndpoint() throws Exception {
        User user = user(101L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasFunctionPermission(101L, "/report/vendor_account")).thenReturn(false);
        when(userService.hasFunctionPermission(101L, "/financial/money_out")).thenReturn(true);

        assertDoesNotThrow(() -> depotHeadService.checkStatementAccountPermission("供应商"));
    }

    @Test
    void rejectsVendorAccountWithoutReportOrMoneyOutPermission() throws Exception {
        User user = user(101L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasFunctionPermission(101L, "/report/vendor_account")).thenReturn(false);
        when(userService.hasFunctionPermission(101L, "/financial/money_out")).thenReturn(false);

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> depotHeadService.checkStatementAccountPermission("供应商"));

        assertEquals(ExceptionConstants.DEPOT_HEAD_VENDOR_ACCOUNT_PERMISSION_CODE, exception.getCode());
    }

    @Test
    void rejectsInvalidDebtListType() {
        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> depotHeadService.checkDebtListPermission("出库", "零售", 107L));

        assertEquals(ExceptionConstants.DEPOT_HEAD_DEBT_LIST_TYPE_INVALID_CODE, exception.getCode());
    }

    @Test
    void rejectsDebtListForUnauthorizedCustomer() throws Exception {
        User user = user(101L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasFunctionPermission(101L, "/report/customer_account")).thenReturn(true);
        when(userBusinessService.getUBValueByTypeAndKeyId("UserCustomer", "101")).thenReturn("[107]");
        SupplierSimple customer = new SupplierSimple();
        customer.setId(107L);
        when(supplierService.getAllCustomer()).thenReturn(Collections.singletonList(customer));
        when(systemConfigService.getCustomerFlag()).thenReturn(true);

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> depotHeadService.checkDebtListPermission("出库", "销售", 108L));

        assertEquals(ExceptionConstants.DEPOT_HEAD_CUSTOMER_DATA_PERMISSION_CODE, exception.getCode());
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setLoginName("user" + id);
        return user;
    }
}
