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
    private static final String LOW_WARNING_TITLE_KEY = "stockWarning.low.title";
    private static final String LOW_WARNING_CONTENT_KEY = "stockWarning.low.content";
    private static final String HIGH_WARNING_TITLE_KEY = "stockWarning.high.title";
    private static final String HIGH_WARNING_CONTENT_KEY = "stockWarning.high.content";

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
        String materialName = material == null ? String.valueOf(key.getMaterialId()) : material.getName();
        String depotName = depot == null ? String.valueOf(key.getDepotId()) : depot.getName();
        if (notifyLow) {
            sendMessages(userIds, tenantId, LOW_WARNING_TITLE_KEY,
                    buildContent(LOW_WARNING_CONTENT_KEY, materialName, depotName, currentStock, lowSafeStock));
        }
        if (notifyHigh) {
            sendMessages(userIds, tenantId, HIGH_WARNING_TITLE_KEY,
                    buildContent(HIGH_WARNING_CONTENT_KEY, materialName, depotName, currentStock, highSafeStock));
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

    private String buildContent(String key, String materialName, String depotName, BigDecimal currentStock,
                                BigDecimal threshold) {
        return "{\"key\":\"" + escapeJson(key) + "\",\"params\":{"
                + "\"materialName\":\"" + escapeJson(materialName) + "\","
                + "\"depotName\":\"" + escapeJson(depotName) + "\","
                + "\"currentStock\":\"" + escapeJson(formatNumber(currentStock)) + "\","
                + "\"threshold\":\"" + escapeJson(formatNumber(threshold)) + "\"}}";
    }

    private String formatNumber(BigDecimal number) {
        return number == null ? "0" : number.stripTrailingZeros().toPlainString();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '\\': escaped.append("\\\\"); break;
                case '"': escaped.append("\\\""); break;
                case '\b': escaped.append("\\b"); break;
                case '\f': escaped.append("\\f"); break;
                case '\n': escaped.append("\\n"); break;
                case '\r': escaped.append("\\r"); break;
                case '\t': escaped.append("\\t"); break;
                default:
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
            }
        }
        return escaped.toString();
    }

}
