package com.jsh.erp;

import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.Depot;
import com.jsh.erp.datasource.entities.DepotItem;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.DepotItemMapperEx;
import com.jsh.erp.datasource.mappers.DepotMapper;
import com.jsh.erp.datasource.mappers.DepotMapperEx;
import com.jsh.erp.datasource.mappers.MaterialCurrentStockMapper;
import com.jsh.erp.datasource.mappers.MaterialCurrentStockMapperEx;
import com.jsh.erp.datasource.mappers.MaterialInitialStockMapper;
import com.jsh.erp.datasource.mappers.MaterialInitialStockMapperEx;
import com.jsh.erp.datasource.mappers.SerialNumberMapper;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.service.DepotService;
import com.jsh.erp.service.LogService;
import com.jsh.erp.service.SystemConfigService;
import com.jsh.erp.service.UserBusinessService;
import com.jsh.erp.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepotServiceTest {

    @Mock private DepotMapper depotMapper;
    @Mock private DepotMapperEx depotMapperEx;
    @Mock private UserService userService;
    @Mock private SystemConfigService systemConfigService;
    @Mock private UserBusinessService userBusinessService;
    @Mock private LogService logService;
    @Mock private DepotItemMapperEx depotItemMapperEx;
    @Mock private MaterialInitialStockMapperEx materialInitialStockMapperEx;
    @Mock private MaterialCurrentStockMapperEx materialCurrentStockMapperEx;
    @Mock private MaterialInitialStockMapper materialInitialStockMapper;
    @Mock private MaterialCurrentStockMapper materialCurrentStockMapper;
    @Mock private SerialNumberMapper serialNumberMapper;

    @InjectMocks private DepotService depotService;

    @Test
    void rejectsWriteWithoutDepotPermission() throws Exception {
        stubUser(false);

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> depotService.insertDepot(validDepot(), null));

        assertEquals(ExceptionConstants.DEPOT_PERMISSION_CODE, exception.getCode());
        verify(depotMapper, never()).insertSelective(any());
    }

    @Test
    void insertUsesOnlyAllowedFields() throws Exception {
        stubUser(true);
        JSONObject request = validDepot();
        request.put("id", 999L);
        request.put("tenantId", 888L);
        request.put("deleteFlag", "1");
        request.put("enabled", false);
        request.put("isDefault", false);
        request.put("type", 99);
        when(depotMapper.selectByExample(any())).thenReturn(Collections.emptyList());
        doAnswer(invocation -> {
            Depot depot = invocation.getArgument(0);
            depot.setId(10L);
            return 1;
        }).when(depotMapper).insertSelective(any());
        when(userService.getUserId(null)).thenReturn(101L);

        depotService.insertDepot(request, null);

        ArgumentCaptor<Depot> captor = ArgumentCaptor.forClass(Depot.class);
        verify(depotMapper).insertSelective(captor.capture());
        Depot inserted = captor.getValue();
        assertEquals(10L, inserted.getId());
        assertNull(inserted.getTenantId());
        assertNull(inserted.getDeleteFlag());
        assertEquals(0, inserted.getType());
        assertEquals(true, inserted.getEnabled());
        assertEquals(true, inserted.getIsDefault());
        verify(userBusinessService).updateOneValueByKeyIdAndType(eq("UserDepot"), any(), eq("10"));
    }

    @Test
    void rejectsDisablingDefaultDepot() throws Exception {
        stubUser(true);
        Depot depot = depot(10L, true);
        when(depotMapper.selectByExample(any())).thenReturn(Collections.singletonList(depot));

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> depotService.batchSetStatus(false, "10"));

        assertEquals(ExceptionConstants.DEPOT_DEFAULT_OPERATION_CODE, exception.getCode());
        verify(depotMapper, never()).updateByExampleSelective(any(), any());
    }

    @Test
    void updateCannotOverwriteSystemFields() throws Exception {
        stubUser(true);
        Depot existing = depot(10L, false);
        when(depotMapper.selectByExample(any()))
                .thenReturn(Collections.singletonList(existing), Collections.emptyList());
        JSONObject request = validDepot();
        request.put("id", 10L);
        request.put("tenantId", 888L);
        request.put("deleteFlag", "1");
        request.put("enabled", false);
        request.put("isDefault", true);
        when(depotMapper.updateByPrimaryKeySelective(any())).thenReturn(1);

        depotService.updateDepot(request, null);

        ArgumentCaptor<Depot> captor = ArgumentCaptor.forClass(Depot.class);
        verify(depotMapper).updateByPrimaryKeySelective(captor.capture());
        Depot updated = captor.getValue();
        assertNull(updated.getTenantId());
        assertNull(updated.getDeleteFlag());
        assertNull(updated.getEnabled());
        assertNull(updated.getIsDefault());
        assertNull(updated.getType());
        assertFalse(updated.getName().isEmpty());
    }

    @Test
    void rejectsDeletingDepotReferencedAsTransferDestination() throws Exception {
        stubUser(true);
        when(depotMapper.selectByExample(any())).thenReturn(Collections.singletonList(depot(10L, false)));
        when(depotItemMapperEx.getDepotItemListListByDepotIds(any()))
                .thenReturn(Collections.singletonList(new DepotItem()));

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> depotService.deleteDepot(10L, null));

        assertEquals(ExceptionConstants.DEPOT_IN_USE_CODE, exception.getCode());
        verify(depotMapperEx, never()).batchDeleteDepotByIds(any(), any(), any());
    }

    private JSONObject validDepot() {
        JSONObject request = new JSONObject();
        request.put("name", "Main Warehouse");
        request.put("address", "Shenzhen");
        request.put("warehousing", "1.25");
        request.put("truckage", "2.50");
        request.put("sort", "1");
        return request;
    }

    private void stubUser(boolean allowed) throws Exception {
        User user = new User();
        user.setId(101L);
        user.setTenantId(7L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasButtonPermission(101L, "/system/depot", "1")).thenReturn(allowed);
    }

    private Depot depot(Long id, boolean isDefault) {
        Depot depot = new Depot();
        depot.setId(id);
        depot.setName("Main Warehouse");
        depot.setType(0);
        depot.setEnabled(true);
        depot.setIsDefault(isDefault);
        depot.setDeleteFlag("0");
        return depot;
    }
}
