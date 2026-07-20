package com.jsh.erp;

import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.service.DepotHeadService;
import com.jsh.erp.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepotHeadServiceCustomerPermissionTest {

    @Mock
    private UserService userService;

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
}
