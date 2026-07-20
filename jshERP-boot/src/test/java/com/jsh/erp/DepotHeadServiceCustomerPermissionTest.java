package com.jsh.erp;

import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.service.DepotHeadService;
import com.jsh.erp.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNull;
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
}
