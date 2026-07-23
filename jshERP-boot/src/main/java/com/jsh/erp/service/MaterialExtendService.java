package com.jsh.erp.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.MaterialExtend;
import com.jsh.erp.datasource.entities.MaterialExtendExample;
import com.jsh.erp.datasource.entities.Material;
import com.jsh.erp.datasource.entities.MaterialExample;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.MaterialExtendMapper;
import com.jsh.erp.datasource.mappers.MaterialExtendMapperEx;
import com.jsh.erp.datasource.mappers.MaterialMapper;
import com.jsh.erp.datasource.mappers.DepotItemMapperEx;
import com.jsh.erp.datasource.vo.MaterialExtendVo4List;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.exception.JshException;
import com.jsh.erp.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;


@Service
public class MaterialExtendService {
    private Logger logger = LoggerFactory.getLogger(MaterialExtendService.class);

    @Resource
    private MaterialExtendMapper materialExtendMapper;
    @Resource
    private MaterialExtendMapperEx materialExtendMapperEx;
    @Resource
    private UserService userService;
    @Resource
    private RedisService redisService;
    @Resource
    private DepotItemMapperEx depotItemMapperEx;
    @Resource
    private MaterialMapper materialMapper;
    
    public MaterialExtend getMaterialExtend(long id)throws Exception {
        MaterialExtend result=null;
        try{
            MaterialExtendExample example = new MaterialExtendExample();
            example.createCriteria().andIdEqualTo(id)
                    .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
            List<MaterialExtend> list = materialExtendMapper.selectByExample(example);
            result = list.isEmpty() ? null : list.get(0);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }
    public List<MaterialExtendVo4List> getDetailList(Long materialId) {
        List<MaterialExtendVo4List> list=null;
        try{
            list = materialExtendMapperEx.getDetailList(materialId);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<MaterialExtend> getListByMIds(List<Long> idList) {
        List<MaterialExtend> meList = null;
        try{
            Long [] idArray= StringUtil.listToLongArray(idList);
            if(idArray!=null && idArray.length>0) {
                meList = materialExtendMapperEx.getListByMId(idArray);
            }
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return meList;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public String saveDetials(JSONObject obj, String sortList, Long materialId, String type) throws Exception {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        JSONArray meArr = obj.getJSONArray("meList");
        JSONArray insertedJson = new JSONArray();
        JSONArray updatedJson = new JSONArray();
        JSONArray deletedJson = obj.getJSONArray("meDeleteIdList");
        if (deletedJson == null) {
            deletedJson = new JSONArray();
        }
        JSONArray sortJson = StringUtil.isEmpty(sortList) ? new JSONArray() : JSONArray.parseArray(sortList);
        Set<Long> existingDetailIds = new HashSet<>();
        List<MaterialExtendVo4List> existingDetails = Collections.emptyList();
        if ("update".equals(type)) {
            existingDetails = materialExtendMapperEx.getDetailList(materialId);
            for (MaterialExtendVo4List detail : existingDetails) {
                existingDetailIds.add(detail.getId());
            }
        }
        if (null != meArr) {
            if("insert".equals(type)){
                for (int i = 0; i < meArr.size(); i++) {
                    JSONObject tempJson = meArr.getJSONObject(i);
                    insertedJson.add(tempJson);
                }
            } else if("update".equals(type)){
                for (int i = 0; i < meArr.size(); i++) {
                    JSONObject tempJson = meArr.getJSONObject(i);
                    Long tempId = tempJson.getLong("id");
                    if(tempId == null){
                        insertedJson.add(tempJson);
                    } else {
                        requireDetailOwnership(tempJson.getLong("id"), existingDetailIds);
                        updatedJson.add(tempJson);
                    }
                }
                //针对多属性商品要考虑到有条码被删的情况，需要和原来的条码明细进行对比
                if(hasManySku(obj)) {
                    //1.先查询原来的条码列表
                    List<MaterialExtendVo4List> meList = existingDetails;
                    //2.构造新的条码列表map
                    Map<String, String> barCodeMap = new HashMap<>();
                    for (int i = 0; i < meArr.size(); i++) {
                        JSONObject tempJson = meArr.getJSONObject(i);
                        barCodeMap.put(tempJson.getString("barCode"),tempJson.getString("barCode"));
                    }
                    //3.如果老的条码在新的里面不存在，则丢入删除队列
                    for(MaterialExtendVo4List me: meList) {
                        if(barCodeMap.get(me.getBarCode()) == null) {
                            deletedJson.add(me.getId());
                        }
                    }
                }
            }
        }
        validateSkuDetails(obj, meArr);
        if (null != deletedJson) {
            List<Long> deletedIds = new ArrayList<>();
            for (int i = 0; i < deletedJson.size(); i++) {
                Long detailId = deletedJson.getLong(i);
                requireDetailOwnership(detailId, existingDetailIds);
                deletedIds.add(detailId);
            }
            if(!deletedIds.isEmpty()) {
                ensureDetailsNotInUse(deletedIds);
                this.batchDeleteMaterialExtendByIds(deletedIds);
            }
        }
        if (null != insertedJson) {
            for (int i = 0; i < insertedJson.size(); i++) {
                MaterialExtend materialExtend = new MaterialExtend();
                JSONObject tempInsertedJson = JSONObject.parseObject(insertedJson.getString(i));
                materialExtend.setMaterialId(materialId);
                materialExtend.setDefaultFlag("0");
                if (StringUtils.isNotEmpty(tempInsertedJson.getString("barCode"))) {
                    int exist = checkIsBarCodeExist(0L, tempInsertedJson.getString("barCode"));
                    if(exist>0) {
                        throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_BARCODE_EXISTS_CODE,
                                String.format(ExceptionConstants.MATERIAL_BARCODE_EXISTS_MSG,tempInsertedJson.getString("barCode")));
                    } else {
                        materialExtend.setBarCode(tempInsertedJson.getString("barCode"));
                    }
                }
                if (StringUtils.isNotEmpty(tempInsertedJson.getString("commodityUnit"))) {
                    materialExtend.setCommodityUnit(tempInsertedJson.getString("commodityUnit"));
                }
                if (tempInsertedJson.get("sku")!=null) {
                    materialExtend.setSku(tempInsertedJson.getString("sku"));
                }
                if (StringUtils.isNotEmpty(tempInsertedJson.getString("purchaseDecimal"))) {
                    materialExtend.setPurchaseDecimal(tempInsertedJson.getBigDecimal("purchaseDecimal"));
                }
                if (StringUtils.isNotEmpty(tempInsertedJson.getString("commodityDecimal"))) {
                    materialExtend.setCommodityDecimal(tempInsertedJson.getBigDecimal("commodityDecimal"));
                }
                if (StringUtils.isNotEmpty(tempInsertedJson.getString("wholesaleDecimal"))) {
                    materialExtend.setWholesaleDecimal(tempInsertedJson.getBigDecimal("wholesaleDecimal"));
                }
                if (StringUtils.isNotEmpty(tempInsertedJson.getString("lowDecimal"))) {
                    materialExtend.setLowDecimal(tempInsertedJson.getBigDecimal("lowDecimal"));
                }
                this.insertMaterialExtend(materialExtend);
            }
        }
        if (null != updatedJson) {
            List<Long> changedIds = new ArrayList<>();
            for (int i = 0; i < updatedJson.size(); i++) {
                changedIds.add(updatedJson.getJSONObject(i).getLong("id"));
            }
            ensureDetailsNotInUse(changedIds);
            for (int i = 0; i < updatedJson.size(); i++) {
                JSONObject tempUpdatedJson = JSONObject.parseObject(updatedJson.getString(i));
                MaterialExtend materialExtend = new MaterialExtend();
                materialExtend.setId(tempUpdatedJson.getLong("id"));
                changedIds.add(materialExtend.getId());
                if (StringUtils.isNotEmpty(tempUpdatedJson.getString("barCode"))) {
                    int exist = checkIsBarCodeExist(tempUpdatedJson.getLong("id"), tempUpdatedJson.getString("barCode"));
                    if(exist>0) {
                        throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_BARCODE_EXISTS_CODE,
                                String.format(ExceptionConstants.MATERIAL_BARCODE_EXISTS_MSG,tempUpdatedJson.getString("barCode")));
                    } else {
                        materialExtend.setBarCode(tempUpdatedJson.getString("barCode"));
                    }
                }
                if (StringUtils.isNotEmpty(tempUpdatedJson.getString("commodityUnit"))) {
                    materialExtend.setCommodityUnit(tempUpdatedJson.getString("commodityUnit"));
                }
                if (tempUpdatedJson.get("sku")!=null) {
                    materialExtend.setSku(tempUpdatedJson.getString("sku"));
                }
                if (StringUtils.isNotEmpty(tempUpdatedJson.getString("purchaseDecimal"))) {
                    materialExtend.setPurchaseDecimal(tempUpdatedJson.getBigDecimal("purchaseDecimal"));
                }
                if (StringUtils.isNotEmpty(tempUpdatedJson.getString("commodityDecimal"))) {
                    materialExtend.setCommodityDecimal(tempUpdatedJson.getBigDecimal("commodityDecimal"));
                }
                if (StringUtils.isNotEmpty(tempUpdatedJson.getString("wholesaleDecimal"))) {
                    materialExtend.setWholesaleDecimal(tempUpdatedJson.getBigDecimal("wholesaleDecimal"));
                }
                if (StringUtils.isNotEmpty(tempUpdatedJson.getString("lowDecimal"))) {
                    materialExtend.setLowDecimal(tempUpdatedJson.getBigDecimal("lowDecimal"));
                }
                this.updateMaterialExtend(materialExtend);
                //如果金额为空，此处单独置空
                materialExtendMapperEx.specialUpdatePrice(materialExtend);
            }
        }
        //处理条码的排序，基本单位排第一个
        if (null != sortJson && sortJson.size()>0) {
            //此处为更新的逻辑
            for (int i = 0; i < sortJson.size(); i++) {
                JSONObject tempSortJson = JSONObject.parseObject(sortJson.getString(i));
                MaterialExtend materialExtend = new MaterialExtend();
                if(StringUtil.isExist(tempSortJson.get("id"))) {
                    Long detailId = tempSortJson.getLong("id");
                    if ("update".equals(type)) {
                        requireDetailOwnership(detailId, existingDetailIds);
                    }
                    materialExtend.setId(detailId);
                }
                if(StringUtil.isExist(tempSortJson.get("defaultFlag"))) {
                    materialExtend.setDefaultFlag(tempSortJson.getString("defaultFlag"));
                }
                this.updateMaterialExtend(materialExtend);
            }
        } else {
            //新增的时候将第一条记录设置为默认基本单位
            MaterialExtendExample example = new MaterialExtendExample();
            example.createCriteria().andMaterialIdEqualTo(materialId).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
            List<MaterialExtend> meList = materialExtendMapper.selectByExample(example);
            if(meList!=null) {
                for(int i=0; i<meList.size(); i++) {
                    MaterialExtend materialExtend = new MaterialExtend();
                    materialExtend.setId(meList.get(i).getId());
                    if(i==0) {
                        materialExtend.setDefaultFlag("1"); //默认
                    } else {
                        materialExtend.setDefaultFlag("0"); //非默认
                    }
                    this.updateMaterialExtend(materialExtend);
                }
            }
        }
        normalizeDefaultFlag(materialId);
        return null;
    }

    private void requireDetailOwnership(Long detailId, Set<Long> existingDetailIds) {
        if (detailId == null || !existingDetailIds.contains(detailId)) {
            throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_NOT_EXISTS_CODE,
                    ExceptionConstants.MATERIAL_NOT_EXISTS_MSG);
        }
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int insertMaterialExtend(MaterialExtend materialExtend)throws Exception {
        User user = userService.getCurrentUser();
        materialExtend.setDeleteFlag(BusinessConstants.DELETE_FLAG_EXISTS);
        materialExtend.setCreateTime(new Date());
        materialExtend.setUpdateTime(new Date().getTime());
        materialExtend.setCreateSerial(user.getLoginName());
        materialExtend.setUpdateSerial(user.getLoginName());
        int result =0;
        try{
            result= materialExtendMapper.insertSelective(materialExtend);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int updateMaterialExtend(MaterialExtend materialExtend) throws Exception{
        User user = userService.getCurrentUser();
        materialExtend.setUpdateTime(System.currentTimeMillis());
        materialExtend.setUpdateSerial(user.getLoginName());
        int res =0;
        try{
            res= materialExtendMapper.updateByPrimaryKeySelective(materialExtend);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return res;
    }

    public int checkIsBarCodeExist(Long id, String barCode)throws Exception {
        MaterialExtendExample example = new MaterialExtendExample();
        MaterialExtendExample.Criteria criteria = example.createCriteria();
        criteria.andBarCodeEqualTo(barCode);
        if (id > 0) {
            criteria.andIdNotEqualTo(id).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        } else {
            criteria.andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        }
        List<MaterialExtend> list =null;
        try{
            list = materialExtendMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list==null?0:list.size();
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int deleteMaterialExtend(Long id, HttpServletRequest request)throws Exception {
        checkMaterialEditPermission();
        requireActiveDetail(id);
        ensureDetailsNotInUse(Collections.singletonList(id));
        int result =0;
        MaterialExtend materialExtend = new MaterialExtend();
        materialExtend.setId(id);
        materialExtend.setDeleteFlag(BusinessConstants.DELETE_FLAG_DELETED);
        Long userId = Long.parseLong(redisService.getObjectFromSessionByKey(request,"userId").toString());
        User user = userService.getUser(userId);
        materialExtend.setUpdateTime(new Date().getTime());
        materialExtend.setUpdateSerial(user.getLoginName());
        try{
            result= materialExtendMapper.updateByPrimaryKeySelective(materialExtend);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteMaterialExtendByIds(String ids, HttpServletRequest request) throws Exception{
        checkMaterialEditPermission();
        return batchDeleteMaterialExtendByIds(StringUtil.strToLongList(ids));
    }

    private int batchDeleteMaterialExtendByIds(List<Long> idList) throws Exception {
        if (idList == null || idList.isEmpty()) {
            return 0;
        }
        int result = 0;
        try{
            result = materialExtendMapperEx.batchDeleteMaterialExtendByIds(idList);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int insertMaterialExtend(JSONObject obj, HttpServletRequest request) throws Exception{
        checkMaterialEditPermission();
        MaterialExtend materialExtend = JSONObject.parseObject(obj.toJSONString(), MaterialExtend.class);
        if (materialExtend.getMaterialId() == null) {
            throw invalidSku("商品不能为空");
        }
        requireActiveMaterial(materialExtend.getMaterialId());
        validateStandaloneDetail(materialExtend, 0L);
        materialExtend.setId(null);
        materialExtend.setTenantId(null);
        materialExtend.setDeleteFlag(null);
        materialExtend.setDefaultFlag("0");
        materialExtend.setCreateTime(null);
        materialExtend.setCreateSerial(null);
        materialExtend.setUpdateTime(null);
        materialExtend.setUpdateSerial(null);
        int result = insertMaterialExtend(materialExtend);
        normalizeDefaultFlag(materialExtend.getMaterialId());
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int updateMaterialExtend(JSONObject obj, HttpServletRequest request)throws Exception {
        checkMaterialEditPermission();
        MaterialExtend materialExtend = JSONObject.parseObject(obj.toJSONString(), MaterialExtend.class);
        MaterialExtend existing = materialExtend.getId() == null ? null : getMaterialExtend(materialExtend.getId());
        if (existing == null) {
            throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_NOT_EXISTS_CODE,
                    ExceptionConstants.MATERIAL_NOT_EXISTS_MSG);
        }
        if (materialExtend.getMaterialId() != null && !Objects.equals(existing.getMaterialId(), materialExtend.getMaterialId())) {
            throw invalidSku("明细不属于指定商品");
        }
        validateStandaloneDetail(materialExtend, existing.getId());
        ensureDetailsNotInUse(Collections.singletonList(existing.getId()));
        materialExtend.setMaterialId(null);
        materialExtend.setTenantId(null);
        materialExtend.setDeleteFlag(null);
        materialExtend.setDefaultFlag(null);
        materialExtend.setCreateTime(null);
        materialExtend.setCreateSerial(null);
        materialExtend.setUpdateTime(null);
        materialExtend.setUpdateSerial(null);
        int result = updateMaterialExtend(materialExtend);
        normalizeDefaultFlag(existing.getMaterialId());
        return result;
    }

    private void checkMaterialEditPermission() throws Exception {
        User currentUser = userService.getCurrentUser();
        Long userId = currentUser == null ? null : currentUser.getId();
        if (!userService.hasButtonPermission(userId, "/material/material", "1")) {
            throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_PERMISSION_CODE,
                    ExceptionConstants.MATERIAL_PERMISSION_MSG);
        }
    }

    private boolean hasManySku(JSONObject obj) {
        JSONArray manySku = obj == null ? null : obj.getJSONArray("manySku");
        return manySku != null && !manySku.isEmpty();
    }

    private void validateSkuDetails(JSONObject obj, JSONArray details) {
        if (!hasManySku(obj)) {
            return;
        }
        if (details == null || details.isEmpty()) {
            throw invalidSku("至少需要一条 SKU 明细");
        }
        Set<String> skus = new HashSet<>();
        Set<String> barCodes = new HashSet<>();
        for (int i = 0; i < details.size(); i++) {
            JSONObject detail = details.getJSONObject(i);
            String sku = StringUtil.toNull(detail.getString("sku"));
            String barCode = StringUtil.toNull(detail.getString("barCode"));
            if (sku == null || !skus.add(sku)) {
                throw invalidSku("SKU 不能为空且不能重复");
            }
            if (barCode == null || !barCodes.add(barCode)) {
                throw invalidSku("SKU 条码不能为空且不能重复");
            }
        }
        JSONArray attributes = obj.getJSONArray("manySku");
        if (attributes.size() > 3) {
            throw invalidSku("属性维度不能超过3个");
        }
        Set<String> expected = buildExpectedSkus(obj);
        if (!expected.isEmpty() && !expected.equals(skus)) {
            throw invalidSku("SKU 明细与属性组合不一致");
        }
    }

    private Set<String> buildExpectedSkus(JSONObject obj) {
        List<List<String>> dimensions = new ArrayList<>();
        for (String field : Arrays.asList("skuOne", "skuTwo", "skuThree")) {
            JSONArray values = obj.getJSONArray(field);
            if (values != null && !values.isEmpty()) {
                List<String> dimension = new ArrayList<>();
                for (Object value : values) {
                    String text = StringUtil.toNull(String.valueOf(value));
                    if (text != null) dimension.add(text);
                }
                if (!dimension.isEmpty()) dimensions.add(dimension);
            }
        }
        if (dimensions.isEmpty()) return Collections.emptySet();
        Set<String> result = new LinkedHashSet<>();
        result.add("");
        for (List<String> dimension : dimensions) {
            Set<String> next = new LinkedHashSet<>();
            for (String prefix : result) {
                for (String value : dimension) {
                    next.add(prefix.isEmpty() ? value : prefix + "/" + value);
                }
            }
            result = next;
        }
        return result;
    }

    private void normalizeDefaultFlag(Long materialId) throws Exception {
        List<MaterialExtendVo4List> details = materialExtendMapperEx.getDetailList(materialId);
        for (int i = 0; i < details.size(); i++) {
            MaterialExtend update = new MaterialExtend();
            update.setId(details.get(i).getId());
            update.setDefaultFlag(i == 0 ? "1" : "0");
            updateMaterialExtend(update);
        }
    }

    private void requireActiveDetail(Long id) throws Exception {
        if (id == null || getMaterialExtend(id) == null) {
            throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_NOT_EXISTS_CODE,
                    ExceptionConstants.MATERIAL_NOT_EXISTS_MSG);
        }
    }

    private void requireActiveMaterial(Long materialId) {
        MaterialExample example = new MaterialExample();
        example.createCriteria().andIdEqualTo(materialId)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<Material> materials = materialMapper.selectByExample(example);
        if (materials == null || materials.isEmpty()) {
            throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_NOT_EXISTS_CODE,
                    ExceptionConstants.MATERIAL_NOT_EXISTS_MSG);
        }
    }

    private void validateStandaloneDetail(MaterialExtend detail, Long excludedId) throws Exception {
        if (StringUtil.isEmpty(detail.getBarCode()) && excludedId != null) {
            MaterialExtend existing = getMaterialExtend(excludedId);
            detail.setBarCode(existing.getBarCode());
        }
        if (StringUtil.isEmpty(detail.getSku()) && excludedId != null) {
            MaterialExtend existing = getMaterialExtend(excludedId);
            detail.setSku(existing.getSku());
        }
        if (StringUtil.isEmpty(detail.getBarCode())) {
            throw invalidSku("条码不能为空");
        }
        if (checkIsBarCodeExist(excludedId, detail.getBarCode()) > 0) {
            throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_BARCODE_EXISTS_CODE,
                    String.format(ExceptionConstants.MATERIAL_BARCODE_EXISTS_MSG, detail.getBarCode()));
        }
        if (StringUtil.isEmpty(detail.getSku())) {
            throw invalidSku("SKU不能为空");
        }
    }

    private void ensureDetailsNotInUse(Collection<Long> ids) {
        List<Long> validIds = new ArrayList<>();
        for (Long id : ids) if (id != null) validIds.add(id);
        if (!validIds.isEmpty() && depotItemMapperEx.getCountByMaterialExtendIds(validIds) > 0) {
            throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_SKU_HISTORY_LOCK_CODE,
                    ExceptionConstants.MATERIAL_SKU_HISTORY_LOCK_MSG);
        }
    }

    private BusinessRunTimeException invalidSku(String reason) {
        return new BusinessRunTimeException(ExceptionConstants.MATERIAL_SKU_CONFIG_INVALID_CODE,
                String.format(ExceptionConstants.MATERIAL_SKU_CONFIG_INVALID_MSG, reason));
    }

    public List<MaterialExtend> getMaterialExtendByTenantAndTime(Long tenantId, Long lastTime, Long syncNum)throws Exception {
        List<MaterialExtend> list=new ArrayList<MaterialExtend>();
        try{
            //先获取最大的时间戳，再查两个时间戳之间的数据，这样同步能够防止丢失数据（应为时间戳有重复）
            Long maxTime = materialExtendMapperEx.getMaxTimeByTenantAndTime(tenantId, lastTime, syncNum);
            if(tenantId!=null && lastTime!=null && maxTime!=null) {
                MaterialExtendExample example = new MaterialExtendExample();
                example.createCriteria().andTenantIdEqualTo(tenantId)
                        .andUpdateTimeGreaterThan(lastTime)
                        .andUpdateTimeLessThanOrEqualTo(maxTime);
                list=materialExtendMapper.selectByExample(example);
            }
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public Long selectIdByMaterialIdAndDefaultFlag(Long materialId, String defaultFlag) {
        Long id = 0L;
        MaterialExtendExample example = new MaterialExtendExample();
        example.createCriteria().andMaterialIdEqualTo(materialId).andDefaultFlagEqualTo(defaultFlag)
                                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<MaterialExtend> list = materialExtendMapper.selectByExample(example);
        if(list!=null && list.size()>0) {
            id = list.get(0).getId();
        }
        return id;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public Long selectIdByMaterialIdAndBarCode(Long materialId, String barCode) {
        Long id = 0L;
        MaterialExtendExample example = new MaterialExtendExample();
        example.createCriteria().andMaterialIdEqualTo(materialId).andBarCodeEqualTo(barCode)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<MaterialExtend> list = materialExtendMapper.selectByExample(example);
        if(list!=null && list.size()>0) {
            id = list.get(0).getId();
        }
        return id;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public List<MaterialExtend> getListByMaterialIdAndDefaultFlagAndBarCode(Long materialId, String defaultFlag, String barCode) {
        MaterialExtendExample example = new MaterialExtendExample();
        example.createCriteria().andMaterialIdEqualTo(materialId).andDefaultFlagEqualTo(defaultFlag).andBarCodeNotEqualTo(barCode)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        return materialExtendMapper.selectByExample(example);
    }

    public MaterialExtend getInfoByBarCode(String barCode)throws Exception {
        MaterialExtendExample example = new MaterialExtendExample();
        example.createCriteria().andBarCodeEqualTo(barCode)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<MaterialExtend> list = materialExtendMapper.selectByExample(example);
        if(list!=null && list.size()>0) {
            return list.get(0);
        } else {
            return null;
        }
    }

    /**
     * 商品的副条码和数据库里面的商品条码存在重复（除自身商品之外）
     * @param manyBarCode
     * @param barCode
     * @return
     */
    public int getCountByManyBarCodeWithoutUs(String manyBarCode, String barCode) {
        MaterialExtendExample example = new MaterialExtendExample();
        example.createCriteria().andBarCodeEqualTo(manyBarCode).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<MaterialExtend> list = materialExtendMapper.selectByExample(example);
        if(list!=null && list.size()>0) {
            for(MaterialExtend me: list) {
                List<MaterialExtend> basicMeList = materialExtendMapperEx.getBasicInfoByMid(me.getMaterialId());
                for(MaterialExtend basicMe: basicMeList) {
                    if(basicMe!=null && !barCode.equals(basicMe.getBarCode())) {
                        return 1;
                    }
                }
            }
        }
        return 0;
    }
}
