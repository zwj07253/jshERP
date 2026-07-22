package com.jsh.erp.service;

import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.Material;
import com.jsh.erp.datasource.entities.Unit;
import com.jsh.erp.datasource.entities.UnitExample;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.MaterialMapperEx;
import com.jsh.erp.datasource.mappers.UnitMapper;
import com.jsh.erp.datasource.mappers.UnitMapperEx;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.exception.JshException;
import com.jsh.erp.utils.PageUtils;
import com.jsh.erp.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class UnitService {
    private static final String UNIT_URL = "/system/unit";
    private static final String EDIT_BUTTON_CODE = "1";
    private Logger logger = LoggerFactory.getLogger(UnitService.class);

    @Resource
    private UnitMapper unitMapper;

    @Resource
    private UnitMapperEx unitMapperEx;
    @Resource
    private UserService userService;
    @Resource
    private LogService logService;
    @Resource
    private MaterialMapperEx materialMapperEx;

    public Unit getUnit(long id)throws Exception {
        Unit result=null;
        try{
            UnitExample example = new UnitExample();
            example.createCriteria().andIdEqualTo(id)
                    .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
            List<Unit> list = unitMapper.selectByExample(example);
            result = list.isEmpty() ? null : list.get(0);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    public Map<Long, Unit> getUnitMap(Collection<Long> unitIds) throws Exception {
        Map<Long, Unit> unitMap = new HashMap<>();
        if (unitIds == null || unitIds.isEmpty()) {
            return unitMap;
        }
        UnitExample example = new UnitExample();
        example.createCriteria().andIdIn(new ArrayList<>(new LinkedHashSet<>(unitIds)))
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        for (Unit unit : unitMapper.selectByExample(example)) {
            unitMap.put(unit.getId(), unit);
        }
        return unitMap;
    }

    public List<Unit> getUnitListByIds(String ids)throws Exception {
        List<Long> idList = StringUtil.strToLongList(ids);
        List<Unit> list = new ArrayList<>();
        try{
            UnitExample example = new UnitExample();
            example.createCriteria().andIdIn(idList)
                    .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
            list = unitMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<Unit> getUnit()throws Exception {
        UnitExample example = new UnitExample();
        example.createCriteria().andEnabledEqualTo(true).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<Unit> list=null;
        try{
            list=unitMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<Unit> select(String name)throws Exception {
        List<Unit> list=null;
        try{
            PageUtils.startPage();
            list=unitMapperEx.selectByConditionUnit(name);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int insertUnit(JSONObject obj, HttpServletRequest request)throws Exception {
        checkUnitEditPermission();
        lockUnitWrite();
        Unit unit = buildUnit(obj, null);
        int result=0;
        try{
            validateUnit(unit);
            parseNameByUnit(unit);
            validateCanonicalName(unit);
            ensureUnitUnique(unit);
            unit.setEnabled(true);
            result=unitMapper.insertSelective(unit);
            logService.insertLog("多单位",
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_ADD).append(unit.getName()).toString(), request);
        }catch(BusinessRunTimeException e) {
            throw e;
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int updateUnit(JSONObject obj, HttpServletRequest request)throws Exception {
        checkUnitEditPermission();
        lockUnitWrite();
        Long id = obj.getLong("id");
        Unit existing = id == null ? null : getUnit(id);
        if (existing == null) {
            throw invalidUnit("多单位不存在");
        }
        Unit unit = buildUnit(obj, existing);
        int result=0;
        try{
            validateUnit(unit);
            parseNameByUnit(unit);
            validateCanonicalName(unit);
            if (definitionChanged(existing, unit) && isUnitInUse(unit.getId())) {
                throw new BusinessRunTimeException(ExceptionConstants.UNIT_IN_USE_CODE,
                        ExceptionConstants.UNIT_IN_USE_MSG);
            }
            ensureUnitUnique(unit);
            result=unitMapperEx.updateUnit(unit);
            logService.insertLog("多单位",
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append(unit.getName()).toString(), request);
        }catch(BusinessRunTimeException e) {
            throw e;
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    /**
     * 根据单位信息生成名称的格式
     * @param unit
     */
    private void parseNameByUnit(Unit unit) {
        String unitName = unit.getBasicUnit() + "/" + "(" +  unit.getOtherUnit() + "=" + ratioText(unit.getRatio()) + unit.getBasicUnit() + ")";
        if(StringUtil.isNotEmpty(unit.getOtherUnitTwo()) && unit.getRatioTwo()!=null) {
            unitName += "/" + "(" + unit.getOtherUnitTwo() + "=" + ratioText(unit.getRatioTwo()) + unit.getBasicUnit() + ")";
            if(StringUtil.isNotEmpty(unit.getOtherUnitThree()) && unit.getRatioThree()!=null) {
                unitName += "/" + "(" + unit.getOtherUnitThree() + "=" + ratioText(unit.getRatioThree()) + unit.getBasicUnit() + ")";
            }
        }
        unit.setName(unitName);
    }

    private String ratioText(BigDecimal ratio) {
        return ratio.stripTrailingZeros().toPlainString();
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int deleteUnit(Long id, HttpServletRequest request)throws Exception {
        return batchDeleteUnitByIds(id.toString());
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteUnit(String ids, HttpServletRequest request) throws Exception{
        return batchDeleteUnitByIds(ids);
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteUnitByIds(String ids)throws Exception {
        checkUnitEditPermission();
        lockUnitWrite();
        int result=0;
        List<Long> idList = new ArrayList<>(new LinkedHashSet<>(StringUtil.strToLongList(ids)));
        if (idList.isEmpty()) {
            return 0;
        }
        //校验产品表	jsh_material
        List<Material> materialList=null;
        try{
            materialList=materialMapperEx.getMaterialListByUnitIds(idList);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        if(materialList!=null&&materialList.size()>0){
            logger.error("异常码[{}],异常提示[{}],参数,UnitIds[{}]",
                    ExceptionConstants.DELETE_FORCE_CONFIRM_CODE,ExceptionConstants.DELETE_FORCE_CONFIRM_MSG,ids);
            throw new BusinessRunTimeException(ExceptionConstants.DELETE_FORCE_CONFIRM_CODE,
                    ExceptionConstants.DELETE_FORCE_CONFIRM_MSG);
        }
        //记录日志
        StringBuffer sb = new StringBuffer();
        sb.append(BusinessConstants.LOG_OPERATION_TYPE_DELETE);
        List<Unit> list = getUnitListByIds(ids);
        for(Unit unit: list){
            sb.append("[").append(unit.getName()).append("]");
        }
        logService.insertLog("多单位", sb.toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        User userInfo=userService.getCurrentUser();
        //校验通过执行删除操作
        try{
            result=unitMapperEx.batchDeleteUnitByIds(new Date(),userInfo==null?null:userInfo.getId(),idList);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    public int checkIsNameExist(Long id, String name)throws Exception {
        UnitExample example = new UnitExample();
        UnitExample.Criteria criteria = example.createCriteria().andNameEqualTo(name)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        if (id != null && id > 0) {
            criteria.andIdNotEqualTo(id);
        }
        List<Unit> list=null;
        try{
            list=unitMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list==null?0:list.size();
    }

    /**
     * 根据条件查询单位id
     * @param basicUnit
     * @param otherUnit
     * @param ratio
     * @return
     */
    public Long getUnitIdByParam(String basicUnit, String otherUnit, BigDecimal ratio){
        Long unitId = null;
        UnitExample example = new UnitExample();
        example.createCriteria().andBasicUnitEqualTo(basicUnit).andOtherUnitEqualTo(otherUnit).andRatioEqualTo(ratio)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<Unit> list = unitMapper.selectByExample(example);
        if(list!=null && list.size()>0) {
            unitId = list.get(0).getId();
        }
        return unitId;
    }

    /**
     * 根据多单位的比例进行库存换算（保留两位小数）
     * @param stock
     * @param unitInfo
     * @param materialUnit
     * @return
     */
    public BigDecimal parseStockByUnit(BigDecimal stock, Unit unitInfo, String materialUnit) {
        BigDecimal ratio = getRatioByUnit(unitInfo, materialUnit);
        if (stock != null && ratio != null) {
            stock = stock.divide(ratio, 6, BigDecimal.ROUND_HALF_UP);
        }
        return stock;
    }

    public BigDecimal parseBasicNumberByUnit(BigDecimal number, Unit unitInfo, String materialUnit) {
        BigDecimal ratio = getRatioByUnit(unitInfo, materialUnit);
        return number != null && ratio != null ? number.multiply(ratio) : number;
    }

    /**
     * 根据多单位的比例进行单价换算（保留两位小数）,变大
     * @param unitPrice
     * @param unitInfo
     * @param materialUnit
     * @return
     */
    public BigDecimal parseUnitPriceByUnit(BigDecimal unitPrice, Unit unitInfo, String materialUnit) {
        BigDecimal ratio = getRatioByUnit(unitInfo, materialUnit);
        if (unitPrice != null && ratio != null) {
            unitPrice = unitPrice.multiply(ratio);
        }
        return unitPrice;
    }

    /**
     * 根据多单位的比例进行总金额换算（保留两位小数），变小
     * @param allPrice
     * @param unitInfo
     * @param materialUnit
     * @return
     */
    public BigDecimal parseAllPriceByUnit(BigDecimal allPrice, Unit unitInfo, String materialUnit) {
        BigDecimal ratio = getRatioByUnit(unitInfo, materialUnit);
        if (allPrice != null && ratio != null) {
            allPrice = allPrice.divide(ratio, 2, BigDecimal.ROUND_HALF_UP);
        }
        return allPrice;
    }

    private BigDecimal getRatioByUnit(Unit unitInfo, String materialUnit) {
        if (unitInfo == null || StringUtil.isEmpty(materialUnit)
                || materialUnit.equals(unitInfo.getBasicUnit())) {
            return null;
        }
        BigDecimal ratio = null;
        if (materialUnit.equals(unitInfo.getOtherUnit())) {
            ratio = unitInfo.getRatio();
        } else if (materialUnit.equals(unitInfo.getOtherUnitTwo())) {
            ratio = unitInfo.getRatioTwo();
        } else if (materialUnit.equals(unitInfo.getOtherUnitThree())) {
            ratio = unitInfo.getRatioThree();
        }
        if (ratio != null && ratio.compareTo(BigDecimal.ONE) <= 0) {
            throw invalidUnit("单位比例必须大于1");
        }
        return ratio;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchSetStatus(Boolean status, String ids)throws Exception {
        checkUnitEditPermission();
        lockUnitWrite();
        logService.insertLog("多单位",
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_ENABLED).toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        List<Long> unitIds = StringUtil.strToLongList(ids);
        if (unitIds.isEmpty()) {
            return 0;
        }
        if (Boolean.FALSE.equals(status) && !materialMapperEx.getMaterialListByUnitIds(unitIds).isEmpty()) {
            throw new BusinessRunTimeException(ExceptionConstants.UNIT_IN_USE_CODE,
                    ExceptionConstants.UNIT_IN_USE_MSG);
        }
        Unit unit = new Unit();
        unit.setEnabled(status);
        UnitExample example = new UnitExample();
        example.createCriteria().andIdIn(unitIds)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        int result=0;
        try{
            result = unitMapper.updateByExampleSelective(unit, example);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    public void checkUnitEditPermission() throws Exception {
        User currentUser = userService.getCurrentUser();
        Long userId = currentUser == null ? null : currentUser.getId();
        if (!userService.hasButtonPermission(userId, UNIT_URL, EDIT_BUTTON_CODE)) {
            throw new BusinessRunTimeException(ExceptionConstants.UNIT_PERMISSION_CODE,
                    ExceptionConstants.UNIT_PERMISSION_MSG);
        }
    }

    private Unit buildUnit(JSONObject obj, Unit existing) {
        Unit unit = new Unit();
        if (existing != null) {
            unit.setId(existing.getId());
        }
        unit.setBasicUnit(StringUtil.toNull(obj.getString("basicUnit")));
        unit.setOtherUnit(StringUtil.toNull(obj.getString("otherUnit")));
        unit.setRatio(obj.getBigDecimal("ratio"));
        unit.setOtherUnitTwo(StringUtil.toNull(obj.getString("otherUnitTwo")));
        unit.setRatioTwo(obj.getBigDecimal("ratioTwo"));
        unit.setOtherUnitThree(StringUtil.toNull(obj.getString("otherUnitThree")));
        unit.setRatioThree(obj.getBigDecimal("ratioThree"));
        return unit;
    }

    private void validateUnit(Unit unit) {
        validateUnitName(unit.getBasicUnit(), "基本单位");
        validateUnitName(unit.getOtherUnit(), "副单位");
        validateRatio(unit.getRatio(), "副单位比例");
        validateOptionalUnit(unit.getOtherUnitTwo(), unit.getRatioTwo(), "副单位2");
        validateOptionalUnit(unit.getOtherUnitThree(), unit.getRatioThree(), "副单位3");
        if (unit.getOtherUnitThree() != null && unit.getOtherUnitTwo() == null) {
            throw invalidUnit("需要先设置副单位2");
        }
        Set<String> names = new LinkedHashSet<>();
        names.add(unit.getBasicUnit());
        if (!names.add(unit.getOtherUnit())
                || (unit.getOtherUnitTwo() != null && !names.add(unit.getOtherUnitTwo()))
                || (unit.getOtherUnitThree() != null && !names.add(unit.getOtherUnitThree()))) {
            throw invalidUnit("单位名称不能重复");
        }
    }

    private void validateOptionalUnit(String name, BigDecimal ratio, String label) {
        if (name == null && ratio == null) {
            return;
        }
        if (name == null || ratio == null) {
            throw invalidUnit(label + "和比例必须同时填写");
        }
        validateUnitName(name, label);
        validateRatio(ratio, label + "比例");
    }

    private void validateUnitName(String name, String label) {
        if (StringUtil.isEmpty(name) || name.length() > 10) {
            throw invalidUnit(label + "不能为空且长度不能超过10个字符");
        }
    }

    private void validateRatio(BigDecimal ratio, String label) {
        int integerDigits = ratio == null ? 0 : Math.max(0, ratio.precision() - ratio.scale());
        if (ratio == null || ratio.compareTo(BigDecimal.ONE) <= 0
                || ratio.scale() > 3 || integerDigits > 21) {
            throw invalidUnit(label + "必须大于1且最多保留三位小数");
        }
    }

    private void validateCanonicalName(Unit unit) {
        if (unit.getName().length() > 50) {
            throw invalidUnit("单位方案名称过长，请缩短单位名称或比例");
        }
    }

    private void ensureUnitUnique(Unit unit) throws Exception {
        if (checkIsNameExist(unit.getId(), unit.getName()) > 0) {
            throw new BusinessRunTimeException(ExceptionConstants.UNIT_ALREADY_EXISTS_CODE,
                    ExceptionConstants.UNIT_ALREADY_EXISTS_MSG);
        }
    }

    private boolean isUnitInUse(Long unitId) {
        return !materialMapperEx.getMaterialListByUnitIds(java.util.Collections.singletonList(unitId)).isEmpty();
    }

    private boolean definitionChanged(Unit before, Unit after) {
        return !Objects.equals(before.getBasicUnit(), after.getBasicUnit())
                || !Objects.equals(before.getOtherUnit(), after.getOtherUnit())
                || !sameDecimal(before.getRatio(), after.getRatio())
                || !Objects.equals(StringUtil.toNull(before.getOtherUnitTwo()), after.getOtherUnitTwo())
                || !sameDecimal(before.getRatioTwo(), after.getRatioTwo())
                || !Objects.equals(StringUtil.toNull(before.getOtherUnitThree()), after.getOtherUnitThree())
                || !sameDecimal(before.getRatioThree(), after.getRatioThree());
    }

    private boolean sameDecimal(BigDecimal left, BigDecimal right) {
        return left == null ? right == null : right != null && left.compareTo(right) == 0;
    }

    private BusinessRunTimeException invalidUnit(String reason) {
        return new BusinessRunTimeException(ExceptionConstants.UNIT_INVALID_CODE,
                String.format(ExceptionConstants.UNIT_INVALID_MSG, reason));
    }

    private void lockUnitWrite() throws Exception {
        User currentUser = userService.getCurrentUser();
        Long tenantId = currentUser == null || currentUser.getTenantId() == null ? 0L : currentUser.getTenantId();
        unitMapperEx.lockUnitWrite(tenantId);
    }
}
