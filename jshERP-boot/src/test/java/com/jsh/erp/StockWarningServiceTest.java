package com.jsh.erp;

import com.jsh.erp.datasource.entities.Depot;
import com.jsh.erp.datasource.entities.Material;
import com.jsh.erp.datasource.entities.MaterialCurrentStock;
import com.jsh.erp.datasource.entities.MaterialInitialStock;
import com.jsh.erp.datasource.entities.StockWarningStatus;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.entities.UserBusiness;
import com.jsh.erp.datasource.mappers.DepotMapper;
import com.jsh.erp.datasource.mappers.MaterialCurrentStockMapper;
import com.jsh.erp.datasource.mappers.MaterialInitialStockMapper;
import com.jsh.erp.datasource.mappers.MaterialMapper;
import com.jsh.erp.datasource.mappers.StockWarningStatusMapper;
import com.jsh.erp.datasource.mappers.UserMapper;
import com.jsh.erp.datasource.vo.StockWarningKey;
import com.jsh.erp.service.MsgService;
import com.jsh.erp.service.StockWarningService;
import com.jsh.erp.service.SystemConfigService;
import com.jsh.erp.service.UserBusinessService;
import com.jsh.erp.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockWarningServiceTest {
    @Mock private StockWarningStatusMapper stockWarningStatusMapper;
    @Mock private MaterialCurrentStockMapper materialCurrentStockMapper;
    @Mock private MaterialInitialStockMapper materialInitialStockMapper;
    @Mock private MaterialMapper materialMapper;
    @Mock private DepotMapper depotMapper;
    @Mock private UserMapper userMapper;
    @Mock private UserService userService;
    @Mock private UserBusinessService userBusinessService;
    @Mock private SystemConfigService systemConfigService;
    @Mock private MsgService msgService;

    @InjectMocks private StockWarningService stockWarningService;

    @Test
    void normalToLowWarningSendsOneMessage() throws Exception {
        prepareStock(new BigDecimal("9"), new BigDecimal("10"), new BigDecimal("100"));
        when(stockWarningStatusMapper.insertIfAbsent(
                anyLong(), anyLong(), anyLong(), anyString(), any(Boolean.class), any()))
                .thenReturn(1);
        User user = user(7L, "operator");
        when(userMapper.selectByExample(any())).thenReturn(Collections.singletonList(user));
        when(systemConfigService.getDepotFlag()).thenReturn(false);
        when(userService.hasFunctionPermission(7L, "/report/stock_warning_report"))
                .thenReturn(true);
        Material material = new Material();
        material.setName("Watch");
        when(materialMapper.selectByPrimaryKey(1L)).thenReturn(material);
        Depot depot = new Depot();
        depot.setName("Main");
        when(depotMapper.selectByPrimaryKey(2L)).thenReturn(depot);

        stockWarningService.checkAndNotifyStockWarnings(keys(), 9L);

        verify(msgService).insertSystemMsg(
                eq("\u5e93\u5b58\u9884\u8b66-\u4f4e\u5e93\u5b58"),
                anyString(), eq("stock_warning"), eq(7L), eq(9L));
    }

    @Test
    void activeLowWarningDoesNotSendDuplicateMessage() throws Exception {
        prepareStock(new BigDecimal("8"), new BigDecimal("10"), new BigDecimal("100"));
        when(stockWarningStatusMapper.insertIfAbsent(
                anyLong(), anyLong(), anyLong(), anyString(), any(Boolean.class), any()))
                .thenReturn(0);
        when(stockWarningStatusMapper.selectForUpdate(9L, 1L, 2L, "LOW"))
                .thenReturn(status(11L, true));
        when(stockWarningStatusMapper.selectForUpdate(9L, 1L, 2L, "HIGH"))
                .thenReturn(status(12L, false));

        stockWarningService.checkAndNotifyStockWarnings(keys(), 9L);

        verify(stockWarningStatusMapper).updateState(
                11L, true, new BigDecimal("8"), false);
        verify(msgService, never()).insertSystemMsg(
                anyString(), anyString(), anyString(), anyLong(), anyLong());
    }

    @Test
    void notifyUsersAreFilteredByWarehousePermission() throws Exception {
        User allowed = user(7L, "allowed");
        User denied = user(8L, "denied");
        when(userMapper.selectByExample(any())).thenReturn(List.of(allowed, denied));
        when(systemConfigService.getDepotFlag()).thenReturn(true);
        when(userService.hasFunctionPermission(anyLong(), eq("/report/stock_warning_report")))
                .thenReturn(true);
        when(userBusinessService.getBasicData("7", "UserDepot"))
                .thenReturn(Collections.singletonList(relation("[2][4]")));
        when(userBusinessService.getBasicData("8", "UserDepot"))
                .thenReturn(Collections.singletonList(relation("[3]")));

        List<Long> result = stockWarningService.getNotifyUserIds(2L, 9L);

        assertEquals(Collections.singletonList(7L), result);
    }

    private void prepareStock(BigDecimal current, BigDecimal low, BigDecimal high) {
        MaterialCurrentStock currentStock = new MaterialCurrentStock();
        currentStock.setCurrentNumber(current);
        when(materialCurrentStockMapper.selectByExample(any()))
                .thenReturn(Collections.singletonList(currentStock));
        MaterialInitialStock safeStock = new MaterialInitialStock();
        safeStock.setLowSafeStock(low);
        safeStock.setHighSafeStock(high);
        when(materialInitialStockMapper.selectByExample(any()))
                .thenReturn(Collections.singletonList(safeStock));
    }

    private HashSet<StockWarningKey> keys() {
        return new HashSet<>(Collections.singletonList(new StockWarningKey(1L, 2L)));
    }

    private StockWarningStatus status(Long id, boolean active) {
        StockWarningStatus status = new StockWarningStatus();
        status.setId(id);
        status.setActive(active);
        return status;
    }

    private User user(Long id, String loginName) {
        User user = new User();
        user.setId(id);
        user.setLoginName(loginName);
        return user;
    }

    private UserBusiness relation(String value) {
        UserBusiness relation = new UserBusiness();
        relation.setValue(value);
        return relation;
    }
}
