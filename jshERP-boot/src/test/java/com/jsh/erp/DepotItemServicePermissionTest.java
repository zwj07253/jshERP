package com.jsh.erp;

import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.service.DepotItemService;
import com.jsh.erp.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepotItemServicePermissionTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private DepotItemService depotItemService;

    @Test
    void rejectsUserWithoutRetailReportPermission() throws Exception {
        User user = new User();
        user.setId(101L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasFunctionPermission(101L, "/report/retail_out_report")).thenReturn(false);

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> depotItemService.checkRetailReportPermission());

        assertEquals(ExceptionConstants.RETAIL_REPORT_PERMISSION_CODE, exception.getCode());
    }

    @Test
    void allowsUserWithRetailReportPermission() throws Exception {
        User user = new User();
        user.setId(101L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasFunctionPermission(101L, "/report/retail_out_report")).thenReturn(true);

        assertDoesNotThrow(() -> depotItemService.checkRetailReportPermission());
    }

    @Test
    void rejectsUserWithoutBuyReportPermission() throws Exception {
        User user = new User();
        user.setId(101L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasFunctionPermission(101L, "/report/buy_in_report")).thenReturn(false);

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> depotItemService.checkBuyReportPermission());

        assertEquals(ExceptionConstants.BUY_REPORT_PERMISSION_CODE, exception.getCode());
    }

    @Test
    void allowsUserWithBuyReportPermission() throws Exception {
        User user = new User();
        user.setId(101L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasFunctionPermission(101L, "/report/buy_in_report")).thenReturn(true);

        assertDoesNotThrow(() -> depotItemService.checkBuyReportPermission());
    }

    @Test
    void rejectsUserWithoutSaleReportPermission() throws Exception {
        User user = new User();
        user.setId(101L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasFunctionPermission(101L, "/report/sale_out_report")).thenReturn(false);

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> depotItemService.checkSaleReportPermission());

        assertEquals(ExceptionConstants.SALE_REPORT_PERMISSION_CODE, exception.getCode());
    }

    @Test
    void allowsUserWithSaleReportPermission() throws Exception {
        User user = new User();
        user.setId(101L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasFunctionPermission(101L, "/report/sale_out_report")).thenReturn(true);

        assertDoesNotThrow(() -> depotItemService.checkSaleReportPermission());
    }

    @Test
    void rejectsUserWithoutInOutStockReportPermission() throws Exception {
        User user = new User();
        user.setId(101L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasFunctionPermission(101L, "/report/in_out_stock_report")).thenReturn(false);

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> depotItemService.checkInOutStockReportPermission());

        assertEquals(ExceptionConstants.IN_OUT_STOCK_REPORT_PERMISSION_CODE, exception.getCode());
    }

    @Test
    void allowsUserWithInOutStockReportPermission() throws Exception {
        User user = new User();
        user.setId(101L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasFunctionPermission(101L, "/report/in_out_stock_report")).thenReturn(true);

        assertDoesNotThrow(() -> depotItemService.checkInOutStockReportPermission());
    }
}
