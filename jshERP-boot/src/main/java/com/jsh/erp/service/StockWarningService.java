package com.jsh.erp.service;

import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.datasource.entities.Depot;
import com.jsh.erp.datasource.entities.Material;
import com.jsh.erp.datasource.entities.MaterialCurrentStock;
import com.jsh.erp.datasource.entities.MaterialCurrentStockExample;
import com.jsh.erp.datasource.entities.MaterialInitialStock;
import com.jsh.erp.datasource.entities.MaterialInitialStockExample;
import com.jsh.erp.datasource.entities.StockWarningStatus;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.entities.UserBusiness;
import com.jsh.erp.datasource.entities.UserExample;
import com.jsh.erp.datasource.mappers.DepotMapper;
import com.jsh.erp.datasource.mappers.MaterialCurrentStockMapper;
import com.jsh.erp.datasource.mappers.MaterialInitialStockMapper;
import com.jsh.erp.datasource.mappers.MaterialMapper;
import com.jsh.erp.datasource.mappers.StockWarningStatusMapper;
import com.jsh.erp.datasource.mappers.UserMapper;
import com.jsh.erp.datasource.vo.StockWarningKey;
import com.jsh.erp.utils.StringUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
public class StockWarningService {
    private static final String WARNING_LOW = "LOW";
    private static final String WARNING_HIGH = "HIGH";
    private static final String MESSAGE_TYPE = "stock_warning";
    private static final String STOCK_WARNING_URL = "/report/stock_warning_report";

    @Resource
    private StockWarningStatusMapper stockWarningStatusMapper;
    @Resource
    private MaterialCurrentStockMapper materialCurrentStockMapper;
    @Resource
    private MaterialInitialStockMapper materialInitialStockMapper;
    @Resource
    private MaterialMapper materialMapper;
    @Resource
    private DepotMapper depotMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private UserService userService;
    @Resource
    private UserBusinessService userBusinessService;
    @Resource
    private SystemConfigService systemConfigService;
    @Resource
    private MsgService msgService;

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void checkAndNotifyStockWarnings(Set<StockWarningKey> affectedStocks, Long tenantId)
            throws Exception {
        if (tenantId == null || affectedStocks == null || affectedStocks.isEmpty()) {
            return;
        }
        for (StockWarningKey key : affectedStocks) {
            if (key.getMaterialId() != null && key.getDepotId() != null) {
                checkAndNotifyOne(key, tenantId);
            }
        }
    }

    private void checkAndNotifyOne(StockWarningKey key, Long tenantId) throws Exception {
        BigDecimal currentStock = getCurrentStock(key, tenantId);
        MaterialInitialStock safeStock = getSafeStock(key, tenantId);
        BigDecimal lowSafeStock = safeStock == null ? null : safeStock.getLowSafeStock();
        BigDecimal highSafeStock = safeStock == null ? null : safeStock.getHighSafeStock();

        boolean lowExceeded = isEnabledThreshold(lowSafeStock)
                && currentStock.compareTo(lowSafeStock) < 0;
        boolean highExceeded = isEnabledThreshold(highSafeStock)
                && currentStock.compareTo(highSafeStock) > 0;

        boolean notifyLow = updateWarningState(
                key, tenantId, WARNING_LOW, lowExceeded, currentStock);
        boolean notifyHigh = updateWarningState(
                key, tenantId, WARNING_HIGH, highExceeded, currentStock);
        if (!notifyLow && !notifyHigh) {
            return;
        }

        List<Long> userIds = getNotifyUserIds(key.getDepotId(), tenantId);
        if (userIds.isEmpty()) {
            return;
        }
        Material material = materialMapper.selectByPrimaryKey(key.getMaterialId());
        Depot depot = depotMapper.selectByPrimaryKey(key.getDepotId());
        String materialName = escapeHtml(
                material == null ? String.valueOf(key.getMaterialId()) : material.getName());
        String depotName = escapeHtml(
                depot == null ? String.valueOf(key.getDepotId()) : depot.getName());
        if (notifyLow) {
            sendMessages(userIds, tenantId, "\u5e93\u5b58\u9884\u8b66-\u4f4e\u5e93\u5b58",
                    buildContent(materialName, depotName, currentStock, lowSafeStock,
                            "\u4f4e\u4e8e\u6700\u4f4e\u5b89\u5168\u5e93\u5b58"));
        }
        if (notifyHigh) {
            sendMessages(userIds, tenantId, "\u5e93\u5b58\u9884\u8b66-\u9ad8\u5e93\u5b58",
                    buildContent(materialName, depotName, currentStock, highSafeStock,
                            "\u9ad8\u4e8e\u6700\u9ad8\u5b89\u5168\u5e93\u5b58"));
        }
    }

    private BigDecimal getCurrentStock(StockWarningKey key, Long tenantId) {
        MaterialCurrentStockExample example = new MaterialCurrentStockExample();
        example.createCriteria()
                .andTenantIdEqualTo(tenantId)
                .andMaterialIdEqualTo(key.getMaterialId())
                .andDepotIdEqualTo(key.getDepotId())
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<MaterialCurrentStock> list = materialCurrentStockMapper.selectByExample(example);
        if (list == null || list.isEmpty() || list.get(0).getCurrentNumber() == null) {
            return BigDecimal.ZERO;
        }
        return list.get(0).getCurrentNumber();
    }

    private MaterialInitialStock getSafeStock(StockWarningKey key, Long tenantId) {
        MaterialInitialStockExample example = new MaterialInitialStockExample();
        example.createCriteria()
                .andTenantIdEqualTo(tenantId)
                .andMaterialIdEqualTo(key.getMaterialId())
                .andDepotIdEqualTo(key.getDepotId())
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<MaterialInitialStock> list = materialInitialStockMapper.selectByExample(example);
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    private boolean updateWarningState(StockWarningKey key, Long tenantId, String warningType,
                                       boolean exceeded, BigDecimal currentStock) {
        int inserted = stockWarningStatusMapper.insertIfAbsent(
                tenantId, key.getMaterialId(), key.getDepotId(), warningType, exceeded, currentStock);
        if (inserted > 0) {
            return exceeded;
        }
        StockWarningStatus warningStatus = stockWarningStatusMapper.selectForUpdate(
                tenantId, key.getMaterialId(), key.getDepotId(), warningType);
        if (warningStatus == null) {
            return false;
        }
        boolean notified = !Boolean.TRUE.equals(warningStatus.getActive()) && exceeded;
        stockWarningStatusMapper.updateState(
                warningStatus.getId(), exceeded, currentStock, notified);
        return notified;
    }

    public List<Long> getNotifyUserIds(Long depotId, Long tenantId) throws Exception {
        UserExample example = new UserExample();
        example.createCriteria()
                .andTenantIdEqualTo(tenantId)
                .andStatusEqualTo(BusinessConstants.USER_STATUS_NORMAL)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<User> users = userMapper.selectByExample(example);
        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }
        boolean depotPermissionEnabled = systemConfigService.getDepotFlag();
        List<Long> userIds = new ArrayList<>();
        for (User user : users) {
            if ("admin".equals(user.getLoginName())
                    || !userService.hasFunctionPermission(user.getId(), STOCK_WARNING_URL)) {
                continue;
            }
            if (!depotPermissionEnabled || hasDepotPermission(user.getId(), depotId)) {
                userIds.add(user.getId());
            }
        }
        return userIds;
    }

    private boolean hasDepotPermission(Long userId, Long depotId) throws Exception {
        List<UserBusiness> relations =
                userBusinessService.getBasicData(userId.toString(), "UserDepot");
        if (relations == null || relations.isEmpty()
                || StringUtil.isEmpty(relations.get(0).getValue())) {
            return false;
        }
        String relationValue = relations.get(0).getValue();
        if (relationValue.length() < 2) {
            return false;
        }
        String depotIds = relationValue.substring(1, relationValue.length() - 1)
                .replace("][", ",");
        return StringUtil.isNotEmpty(depotIds)
                && StringUtil.strToLongList(depotIds).contains(depotId);
    }

    private void sendMessages(List<Long> userIds, Long tenantId, String title, String content) {
        for (Long userId : userIds) {
            msgService.insertSystemMsg(title, content, MESSAGE_TYPE, userId, tenantId);
        }
    }

    private boolean isEnabledThreshold(BigDecimal threshold) {
        return threshold != null && threshold.compareTo(BigDecimal.ZERO) != 0;
    }

    private String buildContent(String materialName, String depotName, BigDecimal currentStock,
                                BigDecimal threshold, String warningText) {
        return "\u5546\u54c1\u3010" + materialName + "\u3011\u5728\u4ed3\u5e93\u3010"
                + depotName + "\u3011\u7684\u5f53\u524d\u5e93\u5b58\u4e3a "
                + formatNumber(currentStock) + "\uff0c" + warningText + " "
                + formatNumber(threshold) + "\u3002";
    }

    private String formatNumber(BigDecimal number) {
        return number == null ? "0" : number.stripTrailingZeros().toPlainString();
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
