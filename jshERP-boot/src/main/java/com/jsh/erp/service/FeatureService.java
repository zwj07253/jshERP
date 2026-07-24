package com.jsh.erp.service;

import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.datasource.entities.Feature;
import com.jsh.erp.datasource.entities.FeatureExample;
import com.jsh.erp.datasource.mappers.FeatureMapper;
import com.jsh.erp.exception.JshException;
import com.jsh.erp.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
public class FeatureService {
    private Logger logger = LoggerFactory.getLogger(FeatureService.class);

    @Resource
    private FeatureMapper featureMapper;

    public Feature getFeature(long id) throws Exception {
        Feature result = null;
        try {
            result = featureMapper.selectByPrimaryKey(id);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return result;
    }

    public Feature getFeatureByCode(String featureCode) throws Exception {
        FeatureExample example = new FeatureExample();
        example.createCriteria().andFeatureCodeEqualTo(featureCode)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<Feature> list = featureMapper.selectByExample(example);
        if (list != null && list.size() > 0) {
            return list.get(0);
        }
        return null;
    }

    public List<Feature> getAllFeatures() throws Exception {
        FeatureExample example = new FeatureExample();
        example.createCriteria().andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED)
                .andEnabledEqualTo(true);
        example.setOrderByClause("sort asc");
        List<Feature> list = null;
        try {
            list = featureMapper.selectByExample(example);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return list == null ? Collections.emptyList() : list;
    }

    public List<Feature> select(String featureCode, String featureName) throws Exception {
        FeatureExample example = new FeatureExample();
        FeatureExample.Criteria criteria = example.createCriteria();
        criteria.andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        if (StringUtil.isNotEmpty(featureCode)) {
            criteria.andFeatureCodeLike("%" + featureCode + "%");
        }
        if (StringUtil.isNotEmpty(featureName)) {
            criteria.andFeatureNameLike("%" + featureName + "%");
        }
        example.setOrderByClause("sort asc");
        List<Feature> list = null;
        try {
            list = featureMapper.selectByExample(example);
        } catch (Exception e) {
            JshException.readFail(logger, e);
        }
        return list == null ? Collections.emptyList() : list;
    }

    public int insertFeature(Feature feature) throws Exception {
        feature.setDeleteFlag(BusinessConstants.DELETE_FLAG_EXISTS);
        feature.setCreateTime(new Date());
        int result = 0;
        try {
            result = featureMapper.insertSelective(feature);
        } catch (Exception e) {
            JshException.writeFail(logger, e);
        }
        return result;
    }

    public int updateFeature(Feature feature) throws Exception {
        feature.setUpdateTime(new Date());
        int result = 0;
        try {
            result = featureMapper.updateByPrimaryKeySelective(feature);
        } catch (Exception e) {
            JshException.writeFail(logger, e);
        }
        return result;
    }

    public int deleteFeature(Long id) throws Exception {
        Feature feature = new Feature();
        feature.setId(id);
        feature.setDeleteFlag(BusinessConstants.DELETE_FLAG_DELETED);
        int result = 0;
        try {
            result = featureMapper.updateByPrimaryKeySelective(feature);
        } catch (Exception e) {
            JshException.writeFail(logger, e);
        }
        return result;
    }

    public int batchDeleteFeature(String ids) throws Exception {
        List<Long> idList = StringUtil.strToLongList(ids);
        Feature feature = new Feature();
        feature.setDeleteFlag(BusinessConstants.DELETE_FLAG_DELETED);
        FeatureExample example = new FeatureExample();
        example.createCriteria().andIdIn(idList);
        int result = 0;
        try {
            result = featureMapper.updateByExampleSelective(feature, example);
        } catch (Exception e) {
            JshException.writeFail(logger, e);
        }
        return result;
    }
}
