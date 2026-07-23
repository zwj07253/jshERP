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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
        JSONArray meArr = obj.getJSONArray("meList");
        JSONArray deletedJson = obj.getJSONArray("meDeleteIdList");
        if (deletedJson == null) {
            deletedJson = new JSONArray();
        }
        JSONArray sortJson = StringUtil.isEmpty(sortList) ? new JSONArray() : JSONArray.parseArray(sortList);

        // Step 1: 校验商品归属
        requireActiveMaterial(materialId);

        // Step 2: 读取数据库现有明细
        Set<Long> existingDetailIds = new HashSet<>();
        List<MaterialExtendVo4List> existingDetails = Collections.emptyList();
        if ("update".equals(type)) {
            existingDetails = materialExtendMapperEx.getDetailList(materialId);
            for (MaterialExtendVo4List detail : existingDetails) {
                existingDetailIds.add(detail.getId());
            }
        }

        // Step 3: 分类为新增/修改/删除列表
        JSONArray insertedJson = new JSONArray();
        JSONArray updatedJson = new JSONArray();
        if (null != meArr) {
            if ("insert".equals(type)) {
                for (int i = 0; i < meArr.size(); i++) {
                    insertedJson.add(meArr.getJSONObject(i));
                }
            } else if ("update".equals(type)) {
                for (int i = 0; i < meArr.size(); i++) {
                    JSONObject tempJson = meArr.getJSONObject(i);
                    Long tempId = tempJson.getLong("id");
                    if (tempId == null) {
                        insertedJson.add(tempJson);
                    } else {
                        requireDetailOwnership(tempId, existingDetailIds);
                        updatedJson.add(tempJson);
                    }
                }
                // 针对多属性商品，对比新旧条码列表，将缺失的条码加入删除队列
                if (hasManySku(obj)) {
                    Map<String, String> barCodeMap = new HashMap<>();
                    for (int i = 0; i < meArr.size(); i++) {
                        String bc = meArr.getJSONObject(i).getString("barCode");
                        if (bc != null) barCodeMap.put(bc, bc);
                    }
                    for (MaterialExtendVo4List me : existingDetails) {
                        if (me.getBarCode() != null && barCodeMap.get(me.getBarCode()) == null) {
                            deletedJson.add(me.getId());
                        }
                    }
                }
            }
        }

        // Step 4: 校验SKU配置完整性
        validateSkuDetails(obj, meArr);

        // Step 5: 校验请求内部条码重复
        validateBarcodeUniquenessInRequest(meArr);

        // Step 6: 校验历史引用
        // 删除：历史使用的SKU完全不允许删除
        List<Long> deletedIds = new ArrayList<>();
        if (null != deletedJson) {
            for (int i = 0; i < deletedJson.size(); i++) {
                Long detailId = deletedJson.getLong(i);
                requireDetailOwnership(detailId, existingDetailIds);
                deletedIds.add(detailId);
            }
            if (!deletedIds.isEmpty()) {
                ensureDetailsNotInUse(deletedIds);
            }
        }
        // 修改：历史使用的SKU只锁定身份字段（sku、barCode、commodityUnit），价格允许修改
        if (null != updatedJson && updatedJson.size() > 0) {
            validateHistoryFieldChanges(updatedJson, existingDetails);
        }

        // Step 7: 执行删除
        if (!deletedIds.isEmpty()) {
            this.batchDeleteMaterialExtendByIds(deletedIds);
        }

        // Step 8: 执行新增
        if (null != insertedJson) {
            for (int i = 0; i < insertedJson.size(); i++) {
                JSONObject tempInsertedJson = JSONObject.parseObject(insertedJson.getString(i));
                MaterialExtend materialExtend = buildMaterialExtendFromJson(tempInsertedJson);
                materialExtend.setMaterialId(materialId);
                materialExtend.setDefaultFlag("0");
                validateBarcodeForInsert(tempInsertedJson.getString("barCode"));
                this.insertMaterialExtend(materialExtend);
            }
        }

        // Step 9: 执行修改
        if (null != updatedJson) {
            for (int i = 0; i < updatedJson.size(); i++) {
                JSONObject tempUpdatedJson = JSONObject.parseObject(updatedJson.getString(i));
                MaterialExtend materialExtend = buildMaterialExtendFromJson(tempUpdatedJson);
                materialExtend.setId(tempUpdatedJson.getLong("id"));
                validateBarcodeForUpdate(tempUpdatedJson.getLong("id"), tempUpdatedJson.getString("barCode"));
                this.updateMaterialExtend(materialExtend);
                materialExtendMapperEx.specialUpdatePrice(materialExtend);
            }
        }

        // Step 10: 处理排序和默认标识
        applySortAndDefaultFlag(sortJson, materialId, type, existingDetailIds);

        // Step 11: 规范化默认标识
        normalizeDefaultFlag(materialId);
        return null;
    }

    private void requireDetailOwnership(Long detailId, Set<Long> existingDetailIds) {
        if (detailId == null || !existingDetailIds.contains(detailId)) {
            throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_NOT_EXISTS_CODE,
                    ExceptionConstants.MATERIAL_NOT_EXISTS_MSG);
        }
    }

    /**
     * 从JSON构建MaterialExtend对象（通用字段）
     */
    private MaterialExtend buildMaterialExtendFromJson(JSONObject json) {
        MaterialExtend me = new MaterialExtend();
        // 统一空条码为null：""、" "、null 全部标准化为 null
        String barCode = StringUtil.toNull(json.getString("barCode"));
        if (barCode != null) {
            me.setBarCode(barCode);
        }
        if (StringUtils.isNotEmpty(json.getString("commodityUnit"))) {
            me.setCommodityUnit(json.getString("commodityUnit"));
        }
        if (json.get("sku") != null) {
            me.setSku(json.getString("sku"));
        }
        if (StringUtils.isNotEmpty(json.getString("purchaseDecimal"))) {
            me.setPurchaseDecimal(json.getBigDecimal("purchaseDecimal"));
        }
        if (StringUtils.isNotEmpty(json.getString("commodityDecimal"))) {
            me.setCommodityDecimal(json.getBigDecimal("commodityDecimal"));
        }
        if (StringUtils.isNotEmpty(json.getString("wholesaleDecimal"))) {
            me.setWholesaleDecimal(json.getBigDecimal("wholesaleDecimal"));
        }
        if (StringUtils.isNotEmpty(json.getString("lowDecimal"))) {
            me.setLowDecimal(json.getBigDecimal("lowDecimal"));
        }
        return me;
    }

    /**
     * 校验请求内部条码不重复
     */
    private void validateBarcodeUniquenessInRequest(JSONArray meArr) {
        if (meArr == null) return;
        Set<String> barcodeSet = new HashSet<>();
        for (int i = 0; i < meArr.size(); i++) {
            String barCode = StringUtil.toNull(meArr.getJSONObject(i).getString("barCode"));
            if (barCode != null && !barcodeSet.add(barCode)) {
                throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_BARCODE_EXISTS_CODE,
                        String.format(ExceptionConstants.MATERIAL_BARCODE_EXISTS_MSG, barCode));
            }
        }
    }

    /**
     * 校验新增时条码在数据库中不存在
     */
    private void validateBarcodeForInsert(String barCode) throws Exception {
        if (StringUtils.isNotEmpty(barCode)) {
            int exist = checkIsBarCodeExist(0L, barCode);
            if (exist > 0) {
                throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_BARCODE_EXISTS_CODE,
                        String.format(ExceptionConstants.MATERIAL_BARCODE_EXISTS_MSG, barCode));
            }
        }
    }

    /**
     * 校验修改时条码在数据库中不存在（排除自身）
     */
    private void validateBarcodeForUpdate(Long id, String barCode) throws Exception {
        if (StringUtils.isNotEmpty(barCode)) {
            int exist = checkIsBarCodeExist(id, barCode);
            if (exist > 0) {
                throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_BARCODE_EXISTS_CODE,
                        String.format(ExceptionConstants.MATERIAL_BARCODE_EXISTS_MSG, barCode));
            }
        }
    }

    /**
     * 处理排序和默认标识
     */
    private void applySortAndDefaultFlag(JSONArray sortJson, Long materialId, String type,
                                          Set<Long> existingDetailIds) throws Exception {
        if (null != sortJson && sortJson.size() > 0) {
            for (int i = 0; i < sortJson.size(); i++) {
                JSONObject tempSortJson = JSONObject.parseObject(sortJson.getString(i));
                MaterialExtend materialExtend = new MaterialExtend();
                if (StringUtil.isExist(tempSortJson.get("id"))) {
                    Long detailId = tempSortJson.getLong("id");
                    if ("update".equals(type)) {
                        requireDetailOwnership(detailId, existingDetailIds);
                    }
                    materialExtend.setId(detailId);
                }
                if (StringUtil.isExist(tempSortJson.get("defaultFlag"))) {
                    materialExtend.setDefaultFlag(tempSortJson.getString("defaultFlag"));
                }
                this.updateMaterialExtend(materialExtend);
            }
        } else {
            // 新增的时候将第一条记录设置为默认基本单位
            MaterialExtendExample example = new MaterialExtendExample();
            example.createCriteria().andMaterialIdEqualTo(materialId)
                    .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
            List<MaterialExtend> meList = materialExtendMapper.selectByExample(example);
            if (meList != null) {
                for (int i = 0; i < meList.size(); i++) {
                    MaterialExtend materialExtend = new MaterialExtend();
                    materialExtend.setId(meList.get(i).getId());
                    materialExtend.setDefaultFlag(i == 0 ? "1" : "0");
                    this.updateMaterialExtend(materialExtend);
                }
            }
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
        }catch(DuplicateKeyException e){
            throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_BARCODE_EXISTS_CODE,
                    String.format(ExceptionConstants.MATERIAL_BARCODE_EXISTS_MSG,
                            materialExtend.getBarCode()));
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
        }catch(DuplicateKeyException e){
            throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_BARCODE_EXISTS_CODE,
                    String.format(ExceptionConstants.MATERIAL_BARCODE_EXISTS_MSG,
                            materialExtend.getBarCode()));
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
        MaterialExtend existing = getMaterialExtend(id);
        if (existing == null) {
            throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_NOT_EXISTS_CODE,
                    ExceptionConstants.MATERIAL_NOT_EXISTS_MSG);
        }
        requireTenantOwnership(existing);
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
        List<Long> idList = StringUtil.strToLongList(ids);
        requireBatchTenantOwnership(idList);
        return batchDeleteMaterialExtendByIds(idList);
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
        requireTenantOwnership(existing);
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

    /**
     * 校验记录属于当前租户
     */
    private void requireTenantOwnership(MaterialExtend record) throws Exception {
        if (record == null) return;
        User currentUser = userService.getCurrentUser();
        Long currentTenantId = currentUser == null ? null : currentUser.getTenantId();
        if (currentTenantId != null && !currentTenantId.equals(record.getTenantId())) {
            throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_NOT_EXISTS_CODE,
                    ExceptionConstants.MATERIAL_NOT_EXISTS_MSG);
        }
    }

    /**
     * 校验批量操作的所有记录属于当前租户
     */
    private void requireBatchTenantOwnership(List<Long> idList) throws Exception {
        if (idList == null || idList.isEmpty()) return;
        User currentUser = userService.getCurrentUser();
        Long currentTenantId = currentUser == null ? null : currentUser.getTenantId();
        if (currentTenantId == null) return;
        Long[] idArray = StringUtil.listToLongArray(idList);
        if (idArray == null || idArray.length == 0) return;
        List<MaterialExtend> records = materialExtendMapperEx.getListByMId(idArray);
        if (records != null) {
            for (MaterialExtend record : records) {
                if (!currentTenantId.equals(record.getTenantId())) {
                    throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_NOT_EXISTS_CODE,
                            ExceptionConstants.MATERIAL_NOT_EXISTS_MSG);
                }
            }
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
        Set<String> skus = new LinkedHashSet<>();
        Set<String> barCodes = new HashSet<>();
        for (int i = 0; i < details.size(); i++) {
            JSONObject detail = details.getJSONObject(i);
            String sku = StringUtil.toNull(detail.getString("sku"));
            String barCode = StringUtil.toNull(detail.getString("barCode"));
            if (sku == null) {
                throw invalidSku("SKU 不能为空");
            }
            if (!skus.add(sku)) {
                throw invalidSku("SKU 重复: " + sku);
            }
            if (barCode == null) {
                throw invalidSku("SKU 条码不能为空");
            }
            if (!barCodes.add(barCode)) {
                throw invalidSku("SKU 条码重复: " + barCode);
            }
        }
        JSONArray attributes = obj.getJSONArray("manySku");
        if (attributes.size() > 3) {
            throw invalidSku("属性维度不能超过3个");
        }
        Set<String> expected = buildExpectedSkus(obj);
        if (!expected.isEmpty() && !expected.equals(skus)) {
            Set<String> missing = new LinkedHashSet<>(expected);
            missing.removeAll(skus);
            Set<String> extra = new LinkedHashSet<>(skus);
            extra.removeAll(expected);
            StringBuilder sb = new StringBuilder();
            if (!missing.isEmpty()) {
                sb.append("缺少SKU: ").append(String.join(", ", missing));
            }
            if (!extra.isEmpty()) {
                if (sb.length() > 0) sb.append("; ");
                sb.append("多余SKU: ").append(String.join(", ", extra));
            }
            throw invalidSku(sb.length() > 0 ? sb.toString() : "SKU 明细与属性组合不一致");
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

    /**
     * 校验历史使用的SKU的身份字段是否被修改。
     * 身份字段（sku、barCode、commodityUnit）不允许修改，价格字段允许修改。
     */
    private void validateHistoryFieldChanges(JSONArray updatedJson,
                                              List<MaterialExtendVo4List> existingDetails) throws Exception {
        List<Long> updatedIds = new ArrayList<>();
        for (int i = 0; i < updatedJson.size(); i++) {
            updatedIds.add(updatedJson.getJSONObject(i).getLong("id"));
        }
        if (updatedIds.isEmpty()) return;
        // 找出被历史单据使用的SKU行
        List<Long> validIds = new ArrayList<>();
        for (Long id : updatedIds) if (id != null) validIds.add(id);
        if (validIds.isEmpty()) return;
        boolean inUse = depotItemMapperEx.getCountByMaterialExtendIds(validIds) > 0;
        if (!inUse) return;
        // 构建现有明细的ID->记录映射
        Map<Long, MaterialExtendVo4List> existingMap = new HashMap<>();
        for (MaterialExtendVo4List detail : existingDetails) {
            existingMap.put(detail.getId(), detail);
        }
        // 对比身份字段
        for (int i = 0; i < updatedJson.size(); i++) {
            JSONObject tempJson = updatedJson.getJSONObject(i);
            Long id = tempJson.getLong("id");
            if (id == null) continue;
            MaterialExtendVo4List existing = existingMap.get(id);
            if (existing == null) continue;
            String newSku = tempJson.getString("sku");
            String newBarCode = tempJson.getString("barCode");
            String newUnit = tempJson.getString("commodityUnit");
            boolean identityChanged = false;
            if (newSku != null && !newSku.equals(existing.getSku())) {
                identityChanged = true;
            }
            if (newBarCode != null && !newBarCode.equals(existing.getBarCode())) {
                identityChanged = true;
            }
            if (newUnit != null && !newUnit.equals(existing.getCommodityUnit())) {
                identityChanged = true;
            }
            if (identityChanged) {
                throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_SKU_HISTORY_IDENTITY_LOCK_CODE,
                        ExceptionConstants.MATERIAL_SKU_HISTORY_IDENTITY_LOCK_MSG);
            }
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
