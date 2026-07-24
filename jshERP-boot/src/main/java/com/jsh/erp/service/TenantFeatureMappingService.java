package com.jsh.erp.service;

import com.jsh.erp.datasource.entities.Feature;
import com.jsh.erp.datasource.entities.TenantFeatureMapping;
import com.jsh.erp.datasource.entities.TenantFeatureMappingExample;
import com.jsh.erp.datasource.mappers.TenantFeatureMappingMapper;
import com.jsh.erp.exception.JshException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TenantFeatureMappingService {
    private Logger logger = LoggerFactory.getLogger(TenantFeatureMappingService.class);

    @Resource
    private TenantFeatureMappingMapper tenantFeatureMappingMapper;

    @Resource
    private FeatureService featureService;

    /**
     * 获取指定租户已启用的功能模块编码集合
     */
    public Set<String> getTenantFeatureCodes(Long tenantId) {
        if (tenantId == null) {
            return Collections.emptySet();
        }
        try {
            TenantFeatureMappingExample example = new TenantFeatureMappingExample();
            example.createCriteria().andTenantIdEqualTo(tenantId).andEnabledEqualTo(true);
            List<TenantFeatureMapping> mappings = tenantFeatureMappingMapper.selectByExample(example);
            if (mappings == null || mappings.isEmpty()) {
                return Collections.emptySet();
            }
            Set<Long> featureIds = mappings.stream()
                    .map(TenantFeatureMapping::getFeatureId)
                    .collect(Collectors.toSet());
            // 根据 featureId 查找 featureCode
            Set<String> codes = new HashSet<>();
            for (Long featureId : featureIds) {
                Feature feature = featureService.getFeature(featureId);
                if (feature != null && feature.getFeatureCode() != null) {
                    codes.add(feature.getFeatureCode());
                }
            }
            return codes;
        } catch (Exception e) {
            logger.error("获取租户功能模块失败", e);
            return Collections.emptySet();
        }
    }

    /**
     * 获取指定租户的功能授权列表（含功能详情）
     */
    public List<Map<String, Object>> getTenantFeatures(Long tenantId) throws Exception {
        List<Map<String, Object>> result = new ArrayList<>();
        // 获取所有功能模块
        List<Feature> allFeatures = featureService.getAllFeatures();
        // 获取租户已授权的功能ID集合
        Set<Long> enabledFeatureIds = new HashSet<>();
        if (tenantId != null) {
            TenantFeatureMappingExample example = new TenantFeatureMappingExample();
            example.createCriteria().andTenantIdEqualTo(tenantId).andEnabledEqualTo(true);
            List<TenantFeatureMapping> mappings = tenantFeatureMappingMapper.selectByExample(example);
            if (mappings != null) {
                enabledFeatureIds = mappings.stream()
                        .map(TenantFeatureMapping::getFeatureId)
                        .collect(Collectors.toSet());
            }
        }
        for (Feature feature : allFeatures) {
            Map<String, Object> item = new HashMap<>();
            item.put("featureId", feature.getId());
            item.put("featureCode", feature.getFeatureCode());
            item.put("featureName", feature.getFeatureName());
            item.put("description", feature.getDescription());
            item.put("enabled", enabledFeatureIds.contains(feature.getId()));
            result.add(item);
        }
        return result;
    }

    /**
     * 批量更新租户功能授权
     * @param tenantId 租户ID
     * @param featureIds 需要启用的功能ID列表
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void batchUpdateTenantFeatures(Long tenantId, List<Long> featureIds) throws Exception {
        if (tenantId == null) {
            return;
        }
        // 先删除该租户的所有功能授权
        TenantFeatureMappingExample deleteExample = new TenantFeatureMappingExample();
        deleteExample.createCriteria().andTenantIdEqualTo(tenantId);
        tenantFeatureMappingMapper.deleteByExample(deleteExample);
        // 再插入新的授权
        if (featureIds != null && !featureIds.isEmpty()) {
            for (Long featureId : featureIds) {
                TenantFeatureMapping mapping = new TenantFeatureMapping();
                mapping.setTenantId(tenantId);
                mapping.setFeatureId(featureId);
                mapping.setEnabled(true);
                mapping.setCreateTime(new Date());
                tenantFeatureMappingMapper.insertSelective(mapping);
            }
        }
    }

    public List<TenantFeatureMapping> getByTenantId(Long tenantId) throws Exception {
        TenantFeatureMappingExample example = new TenantFeatureMappingExample();
        example.createCriteria().andTenantIdEqualTo(tenantId);
        List<TenantFeatureMapping> list = null;
        try {
            list = tenantFeatureMappingMapper.selectByExample(example);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return list == null ? Collections.emptyList() : list;
    }
}
