package com.jsh.erp.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.*;
import com.jsh.erp.datasource.mappers.*;
import com.jsh.erp.datasource.vo.DepotItemStockWarningCount;
import com.jsh.erp.datasource.vo.DepotItemVo4Stock;
import com.jsh.erp.datasource.vo.DepotItemVoBatchNumberList;
import com.jsh.erp.datasource.vo.InOutPriceVo;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.exception.JshException;
import com.jsh.erp.utils.StringUtil;
import com.jsh.erp.utils.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.*;

@Service
public class DepotItemService {
    private Logger logger = LoggerFactory.getLogger(DepotItemService.class);

    private final static String TYPE = "入库";
    private final static String SUM_TYPE = "number";
    private final static String IN = "in";
    private final static String OUT = "out";

    @Resource
    private DepotItemMapper depotItemMapper;
    @Resource
    private DepotItemMapperEx depotItemMapperEx;
    @Resource
    private MaterialService materialService;
    @Resource
    private MaterialExtendService materialExtendService;
    @Resource
    private SerialNumberMapperEx serialNumberMapperEx;
    @Resource
    private DepotHeadService depotHeadService;
    @Resource
    private DepotHeadMapper depotHeadMapper;
    @Resource
    private SerialNumberService serialNumberService;
    @Resource
    private UserService userService;
    @Resource
    private SystemConfigService systemConfigService;
    @Resource
    private DepotService depotService;
    @Resource
    private UnitService unitService;
    @Resource
    private MaterialCurrentStockMapper materialCurrentStockMapper;
    @Resource
    private MaterialCurrentStockMapperEx materialCurrentStockMapperEx;
    @Resource
    private MaterialMapperEx materialMapperEx;
    @Resource
    private LogService logService;

    public void checkRetailReportPermission() throws Exception {
        User currentUser = userService.getCurrentUser();
        Long userId = currentUser == null ? null : currentUser.getId();
        if(!userService.hasFunctionPermission(userId, "/report/retail_out_report")) {
            throw new BusinessRunTimeException(ExceptionConstants.RETAIL_REPORT_PERMISSION_CODE,
                    ExceptionConstants.RETAIL_REPORT_PERMISSION_MSG);
        }
    }

    public DepotItem getDepotItem(long id)throws Exception {
        DepotItem result=null;
        try{
            result=depotItemMapper.selectByPrimaryKey(id);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    public List<DepotItem> getDepotItem()throws Exception {
        DepotItemExample example = new DepotItemExample();
        example.createCriteria().andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<DepotItem> list=null;
        try{
            list=depotItemMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<DepotItem> select(String name, Integer type, String remark, int offset, int rows)throws Exception {
        List<DepotItem> list=null;
        try{
            list=depotItemMapperEx.selectByConditionDepotItem(name, type, remark, offset, rows);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public Long countDepotItem(String name, Integer type, String remark) throws Exception{
        Long result =null;
        try{
            result=depotItemMapperEx.countsByDepotItem(name, type, remark);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int insertDepotItem(JSONObject obj, HttpServletRequest request)throws Exception {
        DepotItem depotItem = JSONObject.parseObject(obj.toJSONString(), DepotItem.class);
        int result =0;
        try{
            result=depotItemMapper.insertSelective(depotItem);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int updateDepotItem(JSONObject obj, HttpServletRequest request)throws Exception {
        DepotItem depotItem = JSONObject.parseObject(obj.toJSONString(), DepotItem.class);
        int result =0;
        try{
            result=depotItemMapper.updateByPrimaryKeySelective(depotItem);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int deleteDepotItem(Long id, HttpServletRequest request)throws Exception {
        int result =0;
        try{
            result=depotItemMapper.deleteByPrimaryKey(id);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteDepotItem(String ids, HttpServletRequest request)throws Exception {
        List<Long> idList = StringUtil.strToLongList(ids);
        DepotItemExample example = new DepotItemExample();
        example.createCriteria().andIdIn(idList);
        int result =0;
        try{
            result=depotItemMapper.deleteByExample(example);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    public int checkIsNameExist(Long id, String name)throws Exception {
        DepotItemExample example = new DepotItemExample();
        example.createCriteria().andIdNotEqualTo(id).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<DepotItem> list =null;
        try{
            list=depotItemMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list==null?0:list.size();
    }

    public List<DepotItemVo4DetailByTypeAndMId> findDetailByDepotIdsAndMaterialIdList(String depotIds, Boolean forceFlag, Boolean inOutManageFlag, String sku, String batchNumber,
                                                                                      String number, String beginTime, String endTime, Long mId, Integer offset, Integer rows)throws Exception {
        String[] depotIdArrOld = null;
        if(StringUtil.isNotEmpty(depotIds)) {
            depotIdArrOld = depotIds.split(",");
        }
        List<Long> depotList = depotService.parseDepotListByArr(depotIdArrOld);
        Long[] depotIdArray = StringUtil.listToLongArray(depotList);
        List<DepotItemVo4DetailByTypeAndMId> list =null;
        try{
            list = depotItemMapperEx.findDetailByDepotIdsAndMaterialIdList(depotIdArray, forceFlag, inOutManageFlag, sku, batchNumber, number, beginTime, endTime, mId, offset, rows);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public Long findDetailByDepotIdsAndMaterialIdCount(String depotIds, Boolean forceFlag, Boolean inOutManageFlag, String sku, String batchNumber,
                                                       String number, String beginTime, String endTime, Long mId)throws Exception {
        String[] depotIdArrOld = null;
        if(StringUtil.isNotEmpty(depotIds)) {
            depotIdArrOld = depotIds.split(",");
        }
        List<Long> depotList = depotService.parseDepotListByArr(depotIdArrOld);
        Long[] depotIdArray = StringUtil.listToLongArray(depotList);
        Long result =null;
        try{
            result = depotItemMapperEx.findDetailByDepotIdsAndMaterialIdCount(depotIdArray, forceFlag, inOutManageFlag, sku, batchNumber, number, beginTime, endTime, mId);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int insertDepotItemWithObj(DepotItem depotItem)throws Exception {
        int result =0;
        try{
            result = depotItemMapper.insertSelective(depotItem);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int updateDepotItemWithObj(DepotItem depotItem)throws Exception {
        int result =0;
        try{
            result = depotItemMapper.updateByPrimaryKeySelective(depotItem);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    public List<DepotItem> getListByHeaderId(Long headerId)throws Exception {
        List<DepotItem> list =null;
        try{
            DepotItemExample example = new DepotItemExample();
            example.createCriteria().andHeaderIdEqualTo(headerId).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
            example.setOrderByClause("id asc");
            list = depotItemMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    /**
     * 查询当前单据中指定商品的明细信息
     * @param headerId
     * @param meId
     * @return
     * @throws Exception
     */
    public DepotItem getItemByHeaderIdAndMaterial(Long headerId, Long meId)throws Exception {
        DepotItem depotItem = new DepotItem();
        try{
            DepotItemExample example = new DepotItemExample();
            example.createCriteria().andHeaderIdEqualTo(headerId).andMaterialExtendIdEqualTo(meId).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
            List<DepotItem> list = depotItemMapper.selectByExample(example);
            if(list!=null && list.size()>0) {
                depotItem = list.get(0);
            }
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return depotItem;
    }

    /**
     * 查询被关联订单中指定商品的明细信息
     * @param linkStr
     * @param meId
     * @return
     * @throws Exception
     */
    public DepotItem getPreItemByHeaderIdAndMaterial(String linkStr, Long meId, Long linkId)throws Exception {
        DepotItem depotItem = new DepotItem();
        try{
            DepotHead depotHead = depotHeadService.getDepotHead(linkStr);
            if(null!=depotHead && null!=depotHead.getId()) {
                DepotItemExample example = new DepotItemExample();
                example.createCriteria().andHeaderIdEqualTo(depotHead.getId()).andMaterialExtendIdEqualTo(meId).andIdEqualTo(linkId).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
                List<DepotItem> list = depotItemMapper.selectByExample(example);
                if(list!=null && list.size()>0) {
                    depotItem = list.get(0);
                }
            }
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return depotItem;
    }

    public List<DepotItemVo4WithInfoEx> getDetailList(Long headerId)throws Exception {
        List<DepotItemVo4WithInfoEx> list =null;
        try{
            list = depotItemMapperEx.getDetailList(headerId);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<DepotItemVo4WithInfoEx> getInOutStock(String materialParam, List<Long> categoryIdList, List<Long> depotList, String endTime, Integer offset, Integer rows)throws Exception {
        List<DepotItemVo4WithInfoEx> list =null;
        try{
            list = depotItemMapperEx.getInOutStock(materialParam, categoryIdList, depotList, endTime, offset, rows);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public int getInOutStockCount(String materialParam, List<Long> categoryIdList, List<Long> depotList, String endTime)throws Exception {
        int result=0;
        try{
            result = depotItemMapperEx.getInOutStockCount(materialParam, categoryIdList, depotList, endTime);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    public List<DepotItemVo4WithInfoEx> getListWithBuyOrSale(String materialParam, String billType,
                                                             String beginTime, String endTime, String[] creatorArray, Long organId, String[] organArray, List<Long> categoryList, List<Long> depotList, Boolean forceFlag, Integer offset, Integer rows)throws Exception {
        List<DepotItemVo4WithInfoEx> list =null;
        try{
            list = depotItemMapperEx.getListWithBuyOrSale(materialParam, billType, beginTime, endTime, creatorArray, organId, organArray, categoryList, depotList, forceFlag, offset, rows);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<DepotItemVo4WithInfoEx> getRetailOutSummary(String materialParam, String beginTime, String endTime,
                                                            String[] creatorArray, Long organId, String[] organArray,
                                                            List<Long> categoryList, List<Long> depotList, Boolean forceFlag,
                                                            String column, String order, Integer offset, Integer rows) throws Exception {
        return depotItemMapperEx.getRetailOutSummary(materialParam, beginTime, endTime, creatorArray, organId,
                organArray, categoryList, depotList, forceFlag, column, order, offset, rows);
    }

    public int getListWithBuyOrSaleCount(String materialParam, String billType,
                                         String beginTime, String endTime, String[] creatorArray, Long organId, String[] organArray, List<Long> categoryList, List<Long> depotList, Boolean forceFlag)throws Exception {
        return depotItemMapperEx.getListWithBuyOrSaleCount(materialParam, billType, beginTime, endTime,
                creatorArray, organId, organArray, categoryList, depotList, forceFlag);
    }

    public BigDecimal buyOrSale(String type, String subType, Long meId, String beginTime, String endTime,
                                String[] creatorArray, Long organId, String [] organArray, List<Long> depotList, Boolean forceFlag, String sumType) throws Exception{
        BigDecimal result= BigDecimal.ZERO;
        try{
            if (SUM_TYPE.equals(sumType)) {
                result= depotItemMapperEx.buyOrSaleNumber(type, subType, meId, beginTime, endTime, creatorArray, organId, organArray, depotList, forceFlag, sumType);
            } else {
                result= depotItemMapperEx.buyOrSalePrice(type, subType, meId, beginTime, endTime, creatorArray, organId, organArray, depotList, forceFlag, sumType);
            }
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    public BigDecimal buyOrSalePriceTotal(String type, String subType, String materialParam, String beginTime, String endTime,
                                String[] creatorArray, Long organId, String [] organArray, List<Long> categoryList, List<Long> depotList, Boolean forceFlag) throws Exception{
        return depotItemMapperEx.buyOrSalePriceTotal(type, subType, materialParam, beginTime, endTime,
                creatorArray, organId, organArray, categoryList, depotList, forceFlag);

    }

    /**
     * 统计采购、销售、零售的总金额列表
     * @param beginTime
     * @param endTime
     * @return
     * @throws Exception
     */
    public List<InOutPriceVo> inOrOutPriceList(String beginTime, String endTime) throws Exception{
        List<InOutPriceVo> result = new ArrayList<>();
        try{
            String [] creatorArray = depotHeadService.getCreatorArray();
            Boolean forceFlag = systemConfigService.getForceApprovalFlag();
            result = depotItemMapperEx.inOrOutPriceList(beginTime, endTime, creatorArray, forceFlag);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void saveDetials(String rows, Long headerId, String actionType, HttpServletRequest request) throws Exception{
        //查询单据主表信息
        DepotHead depotHead =depotHeadMapper.selectByPrimaryKey(headerId);
        //删除序列号和回收序列号
        deleteOrCancelSerialNumber(actionType, depotHead, headerId);
        //删除单据的明细
        deleteDepotItemHeadId(headerId);
        JSONArray rowArr = JSONArray.parseArray(rows);
        if (null != rowArr && rowArr.size()>0) {
            boolean purchaseInbound = BusinessConstants.DEPOTHEAD_TYPE_IN.equals(depotHead.getType())
                    && BusinessConstants.SUB_TYPE_PURCHASE.equals(depotHead.getSubType());
            boolean purchaseReturn = BusinessConstants.DEPOTHEAD_TYPE_OUT.equals(depotHead.getType())
                    && BusinessConstants.SUB_TYPE_PURCHASE_RETURN.equals(depotHead.getSubType());
            boolean salesOutbound = BusinessConstants.DEPOTHEAD_TYPE_OUT.equals(depotHead.getType())
                    && BusinessConstants.SUB_TYPE_SALES.equals(depotHead.getSubType());
            boolean salesReturn = BusinessConstants.DEPOTHEAD_TYPE_IN.equals(depotHead.getType())
                    && BusinessConstants.SUB_TYPE_SALES_RETURN.equals(depotHead.getSubType());
            boolean otherStockBill = (BusinessConstants.DEPOTHEAD_TYPE_IN.equals(depotHead.getType())
                    || BusinessConstants.DEPOTHEAD_TYPE_OUT.equals(depotHead.getType()))
                    && BusinessConstants.SUB_TYPE_OTHER.equals(depotHead.getSubType());
            boolean assemble = BusinessConstants.DEPOTHEAD_TYPE_OTHER.equals(depotHead.getType())
                    && BusinessConstants.SUB_TYPE_ASSEMBLE.equals(depotHead.getSubType());
            boolean disassemble = BusinessConstants.DEPOTHEAD_TYPE_OTHER.equals(depotHead.getType())
                    && BusinessConstants.SUB_TYPE_DISASSEMBLE.equals(depotHead.getSubType());
            boolean purchaseDepotPermission = purchaseInbound || purchaseReturn || salesOutbound || salesReturn
                    || otherStockBill || assemble || disassemble;
            Set<Long> allowedPurchaseDepotIds = new HashSet<>();
            User currentUser = userService.getCurrentUser();
            boolean adminUser = currentUser != null && "admin".equals(currentUser.getLoginName());
            if (purchaseDepotPermission && !adminUser) {
                JSONArray allowedDepotArray = depotService.findDepotByCurrentUser();
                for (Object depotObject : allowedDepotArray) {
                    allowedPurchaseDepotIds.add(JSONObject.parseObject(depotObject.toString()).getLong("id"));
                }
            }
            //针对组装单、拆卸单校验是否存在组合件和普通子件
            checkAssembleWithMaterialType(rowArr, depotHead.getSubType());
            //校验多行明细当中是否存在重复的序列号
            checkSerialNumberRepeatWithCurrent(rowArr);
            List<DepotItem> depotItemList = new ArrayList<>();
            Map<String, BigDecimal> outboundQuantityMap = new HashMap<>();
            Set<String> lockedStockKeys = new HashSet<>();
            lockMaterialsByRows(rowArr, lockedStockKeys);
            for (int i = 0; i < rowArr.size(); i++) {
                DepotItem depotItem = new DepotItem();
                JSONObject rowObj = JSONObject.parseObject(rowArr.getString(i));
                depotItem.setHeaderId(headerId);
                String barCode = rowObj.getString("barCode");
                MaterialExtend materialExtend = materialExtendService.getInfoByBarCode(barCode);
                if(materialExtend == null) {
                    throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_BARCODE_IS_NOT_EXIST_CODE,
                            String.format(ExceptionConstants.MATERIAL_BARCODE_IS_NOT_EXIST_MSG, barCode));
                }
                depotItem.setMaterialId(materialExtend.getMaterialId());
                depotItem.setMaterialExtendId(materialExtend.getId());
                depotItem.setMaterialUnit(rowObj.getString("unit"));
                Material material= materialService.getMaterial(depotItem.getMaterialId());
                if (BusinessConstants.ENABLE_SERIAL_NUMBER_ENABLED.equals(material.getEnableSerialNumber()) ||
                        BusinessConstants.ENABLE_BATCH_NUMBER_ENABLED.equals(material.getEnableBatchNumber())) {
                    //组装拆卸单不能选择批号或序列号商品
                    if(BusinessConstants.SUB_TYPE_ASSEMBLE.equals(depotHead.getSubType()) ||
                            BusinessConstants.SUB_TYPE_DISASSEMBLE.equals(depotHead.getSubType())) {
                        throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_ASSEMBLE_SELECT_ERROR_CODE,
                                String.format(ExceptionConstants.MATERIAL_ASSEMBLE_SELECT_ERROR_MSG, barCode));
                    }
                    //调拨单不能选择批号或序列号商品（该场景走出库和入库单）
                    if(BusinessConstants.SUB_TYPE_TRANSFER.equals(depotHead.getSubType())) {
                        throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_TRANSFER_SELECT_ERROR_CODE,
                                String.format(ExceptionConstants.MATERIAL_TRANSFER_SELECT_ERROR_MSG, barCode));
                    }
                    //盘点业务不能选择批号或序列号商品（该场景走出库和入库单）
                    if(BusinessConstants.SUB_TYPE_CHECK_ENTER.equals(depotHead.getSubType())
                       ||BusinessConstants.SUB_TYPE_REPLAY.equals(depotHead.getSubType())) {
                        throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_STOCK_CHECK_ERROR_CODE,
                                String.format(ExceptionConstants.MATERIAL_STOCK_CHECK_ERROR_MSG, barCode));
                    }
                }
                if (StringUtil.isExist(rowObj.get("snList"))) {
                    depotItem.setSnList(rowObj.getString("snList"));
                    if(StringUtil.isExist(rowObj.get("depotId"))) {
                        String [] snArray = depotItem.getSnList().split(",");
                        int operNum = rowObj.getInteger("operNumber");
                        if(snArray.length == operNum) {
                            Long depotId = rowObj.getLong("depotId");
                            BigDecimal inPrice = BigDecimal.ZERO;
                            if (StringUtil.isExist(rowObj.get("unitPrice"))) {
                                inPrice = rowObj.getBigDecimal("unitPrice");
                            }
                            serialNumberService.addSerialNumberByBill(depotHead.getType(), depotHead.getSubType(),
                                    depotHead.getNumber(), materialExtend.getMaterialId(), depotId, inPrice, depotItem.getSnList());
                        } else {
                            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_SN_NUMBERE_FAILED_CODE,
                                    String.format(ExceptionConstants.DEPOT_HEAD_SN_NUMBERE_FAILED_MSG, barCode));
                        }
                    }
                } else {
                    //入库或出库
                    if (BusinessConstants.DEPOTHEAD_TYPE_IN.equals(depotHead.getType()) ||
                            BusinessConstants.DEPOTHEAD_TYPE_OUT.equals(depotHead.getType())) {
                        //序列号不能为空
                        if (BusinessConstants.ENABLE_SERIAL_NUMBER_ENABLED.equals(material.getEnableSerialNumber())) {
                            //如果开启出入库管理，并且类型等于采购、采购退货、销售、销售退货，则跳过
                            if(systemConfigService.getInOutManageFlag() &&
                                    (BusinessConstants.SUB_TYPE_PURCHASE.equals(depotHead.getSubType())
                                            ||BusinessConstants.SUB_TYPE_PURCHASE_RETURN.equals(depotHead.getSubType())
                                            ||BusinessConstants.SUB_TYPE_SALES.equals(depotHead.getSubType())
                                            ||BusinessConstants.SUB_TYPE_SALES_RETURN.equals(depotHead.getSubType()))) {
                                //跳过
                            } else {
                                throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_SERIAL_NUMBERE_EMPTY_CODE,
                                        String.format(ExceptionConstants.MATERIAL_SERIAL_NUMBERE_EMPTY_MSG, barCode));
                            }
                        }
                    }
                }
                if (StringUtil.isExist(rowObj.get("batchNumber"))) {
                    depotItem.setBatchNumber(rowObj.getString("batchNumber"));
                } else {
                    //入库或出库
                    if(BusinessConstants.DEPOTHEAD_TYPE_IN.equals(depotHead.getType()) ||
                            BusinessConstants.DEPOTHEAD_TYPE_OUT.equals(depotHead.getType())) {
                        //批号不能为空
                        if (BusinessConstants.ENABLE_BATCH_NUMBER_ENABLED.equals(material.getEnableBatchNumber())) {
                            //如果开启出入库管理，并且类型等于采购、采购退货、销售、销售退货，则跳过
                            if(systemConfigService.getInOutManageFlag() &&
                                    (BusinessConstants.SUB_TYPE_PURCHASE.equals(depotHead.getSubType())
                                            ||BusinessConstants.SUB_TYPE_PURCHASE_RETURN.equals(depotHead.getSubType())
                                            ||BusinessConstants.SUB_TYPE_SALES.equals(depotHead.getSubType())
                                            ||BusinessConstants.SUB_TYPE_SALES_RETURN.equals(depotHead.getSubType()))) {
                                //跳过
                            } else {
                                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_BATCH_NUMBERE_EMPTY_CODE,
                                        String.format(ExceptionConstants.DEPOT_HEAD_BATCH_NUMBERE_EMPTY_MSG, barCode));
                            }
                        }
                    }
                }
                if (StringUtil.isExist(rowObj.get("expirationDate"))) {
                    depotItem.setExpirationDate(rowObj.getDate("expirationDate"));
                }
                if (StringUtil.isExist(rowObj.get("sku"))) {
                    depotItem.setSku(rowObj.getString("sku"));
                }
                if (StringUtil.isExist(rowObj.get("linkId"))) {
                    depotItem.setLinkId(rowObj.getLong("linkId"));
                }
                //以下进行单位换算
                Unit unitInfo = materialService.findUnit(materialExtend.getMaterialId()); //查询多单位信息
                String submittedUnit = rowObj.getString("unit");
                if (!isMaterialUnitValid(submittedUnit, materialExtend, unitInfo)) {
                    throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_MATERIAL_UNIT_CODE,
                            String.format(ExceptionConstants.DEPOT_HEAD_MATERIAL_UNIT_MSG, barCode));
                }
                if (StringUtil.isExist(rowObj.get("operNumber"))) {
                    depotItem.setOperNumber(rowObj.getBigDecimal("operNumber"));
                    String unit = submittedUnit;
                    BigDecimal oNumber = rowObj.getBigDecimal("operNumber");
                    if (StringUtil.isNotEmpty(unitInfo.getName())) {
                        String basicUnit = unitInfo.getBasicUnit(); //基本单位
                        if (unit.equals(basicUnit)) { //如果等于基本单位
                            depotItem.setBasicNumber(oNumber); //数量一致
                        } else if (unit.equals(unitInfo.getOtherUnit())) { //如果等于副单位
                            depotItem.setBasicNumber(oNumber.multiply(unitInfo.getRatio())); //数量乘以比例
                        } else if (unit.equals(unitInfo.getOtherUnitTwo())) { //如果等于副单位2
                            depotItem.setBasicNumber(oNumber.multiply(unitInfo.getRatioTwo())); //数量乘以比例
                        } else if (unit.equals(unitInfo.getOtherUnitThree())) { //如果等于副单位3
                            depotItem.setBasicNumber(oNumber.multiply(unitInfo.getRatioThree())); //数量乘以比例
                        } else {
                            depotItem.setBasicNumber(oNumber); //数量一致
                        }
                    } else {
                        depotItem.setBasicNumber(oNumber); //其他情况
                    }
                }
                if (depotItem.getOperNumber() == null || depotItem.getOperNumber().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_CODE,
                            String.format(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_MSG, barCode));
                }
                //如果数量+已完成数量>原订单数量，给出预警(判断前提是存在关联订单|关联请购单)
                String linkStr = StringUtil.isNotEmpty(depotHead.getLinkNumber())? depotHead.getLinkNumber(): depotHead.getLinkApply();
                if (StringUtil.isNotEmpty(linkStr) && StringUtil.isExist(rowObj.get("preNumber")) && StringUtil.isExist(rowObj.get("finishNumber"))) {
                    if("add".equals(actionType)) {
                        //在新增模式进行状态赋值
                        BigDecimal preNumber = rowObj.getBigDecimal("preNumber");
                        BigDecimal finishNumber = rowObj.getBigDecimal("finishNumber");
                        if(depotItem.getOperNumber().add(finishNumber).compareTo(preNumber)>0) {
                            if(!systemConfigService.getOverLinkBillFlag()) {
                                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_NUMBER_NEED_EDIT_FAILED_CODE,
                                        String.format(ExceptionConstants.DEPOT_HEAD_NUMBER_NEED_EDIT_FAILED_MSG, barCode));
                            }
                        }
                    } else if("update".equals(actionType)) {
                        //当前单据的类型
                        String currentSubType = depotHead.getSubType();
                        //在更新模式进行状态赋值
                        String unit = rowObj.get("unit").toString();
                        Long preHeaderId = depotHeadService.getDepotHead(linkStr).getId();
                        if(null!=preHeaderId) {
                            //前一个单据的数量
                            BigDecimal preNumber = getPreItemByHeaderIdAndMaterial(linkStr, depotItem.getMaterialExtendId(), depotItem.getLinkId()).getOperNumber();
                            //除去此单据之外的已入库|已出库
                            BigDecimal realFinishNumber = getRealFinishNumber(currentSubType, depotItem.getMaterialExtendId(), depotItem.getLinkId(), preHeaderId, headerId, unitInfo, unit);
                            if(preNumber!=null) {
                                if (depotItem.getOperNumber().add(realFinishNumber).compareTo(preNumber) > 0) {
                                    if (!systemConfigService.getOverLinkBillFlag()) {
                                        throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_NUMBER_NEED_EDIT_FAILED_CODE,
                                                String.format(ExceptionConstants.DEPOT_HEAD_NUMBER_NEED_EDIT_FAILED_MSG, barCode));
                                    }
                                }
                            } else {
                                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_ITEM_PRE_BILL_IS_CHANGE_CODE,
                                        ExceptionConstants.DEPOT_ITEM_PRE_BILL_IS_CHANGE_MSG);
                            }
                        }
                    }
                }
                if (StringUtil.isExist(rowObj.get("unitPrice"))) {
                    BigDecimal unitPrice = rowObj.getBigDecimal("unitPrice");
                    depotItem.setUnitPrice(unitPrice);
                    if(materialExtend.getLowDecimal()!=null) {
                        //零售或销售单价低于最低售价，进行提示
                        if("零售".equals(depotHead.getSubType()) || "销售".equals(depotHead.getSubType())) {
                            if (unitPrice.compareTo(materialExtend.getLowDecimal()) < 0) {
                                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_UNIT_PRICE_LOW_CODE,
                                        String.format(ExceptionConstants.DEPOT_HEAD_UNIT_PRICE_LOW_MSG, barCode));
                            }
                        }
                    }
                }
                //如果是销售出库、销售退货、零售出库、零售退货则给采购单价字段赋值（如果是批次商品，则要根据批号去找之前的入库价）
                if(BusinessConstants.SUB_TYPE_SALES.equals(depotHead.getSubType()) ||
                    BusinessConstants.SUB_TYPE_SALES_RETURN.equals(depotHead.getSubType()) ||
                    BusinessConstants.SUB_TYPE_RETAIL.equals(depotHead.getSubType()) ||
                    BusinessConstants.SUB_TYPE_RETAIL_RETURN.equals(depotHead.getSubType())) {
                    boolean moveAvgPriceFlag = systemConfigService.getMoveAvgPriceFlag();
                    BigDecimal currentUnitPrice = materialCurrentStockMapperEx.getCurrentUnitPriceByMId(materialExtend.getMaterialId());
                    currentUnitPrice = unitService.parseUnitPriceByUnit(currentUnitPrice, unitInfo, depotItem.getMaterialUnit());
                    BigDecimal unitPrice = moveAvgPriceFlag? currentUnitPrice: materialExtend.getPurchaseDecimal();
                    depotItem.setPurchaseUnitPrice(unitPrice);
                    if(StringUtil.isNotEmpty(depotItem.getBatchNumber())) {
                        depotItem.setPurchaseUnitPrice(getDepotItemByBatchNumber(depotItem.getMaterialExtendId(),depotItem.getBatchNumber()).getUnitPrice());
                    }
                }
                if (StringUtil.isExist(rowObj.get("taxUnitPrice"))) {
                    depotItem.setTaxUnitPrice(rowObj.getBigDecimal("taxUnitPrice"));
                }
                if (StringUtil.isExist(rowObj.get("allPrice"))) {
                    depotItem.setAllPrice(rowObj.getBigDecimal("allPrice"));
                }
                if (StringUtil.isExist(rowObj.get("depotId"))) {
                    depotItem.setDepotId(rowObj.getLong("depotId"));
                    if (purchaseDepotPermission && !adminUser && !allowedPurchaseDepotIds.contains(depotItem.getDepotId())) {
                        if (assemble || disassemble) {
                            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_DATA_PERMISSION_CODE,
                                    ExceptionConstants.DEPOT_DATA_PERMISSION_MSG);
                        }
                        if (otherStockBill) {
                            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_OTHER_DATA_PERMISSION_CODE,
                                    ExceptionConstants.DEPOT_HEAD_OTHER_DATA_PERMISSION_MSG);
                        }
                        if (salesOutbound || salesReturn) {
                            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_SALES_DATA_PERMISSION_CODE,
                                    ExceptionConstants.DEPOT_HEAD_SALES_DATA_PERMISSION_MSG);
                        }
                        if (purchaseReturn) {
                            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_RETURN_DATA_PERMISSION_CODE,
                                    ExceptionConstants.DEPOT_HEAD_PURCHASE_RETURN_DATA_PERMISSION_MSG);
                        }
                        throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_PURCHASE_IN_DATA_PERMISSION_CODE,
                                ExceptionConstants.DEPOT_HEAD_PURCHASE_IN_DATA_PERMISSION_MSG);
                    }
                } else {
                    if(!BusinessConstants.SUB_TYPE_PURCHASE_APPLY.equals(depotHead.getSubType())
                            && !BusinessConstants.SUB_TYPE_PURCHASE_ORDER.equals(depotHead.getSubType())
                            && !BusinessConstants.SUB_TYPE_SALES_ORDER.equals(depotHead.getSubType())) {
                        throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_DEPOT_FAILED_CODE,
                                String.format(ExceptionConstants.DEPOT_HEAD_DEPOT_FAILED_MSG));
                    }
                }
                if(BusinessConstants.SUB_TYPE_TRANSFER.equals(depotHead.getSubType())) {
                    if (StringUtil.isExist(rowObj.get("anotherDepotId"))) {
                        if(rowObj.getLong("anotherDepotId").equals(rowObj.getLong("depotId"))) {
                            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ANOTHER_DEPOT_EQUAL_FAILED_CODE,
                                    String.format(ExceptionConstants.DEPOT_HEAD_ANOTHER_DEPOT_EQUAL_FAILED_MSG));
                        } else {
                            depotItem.setAnotherDepotId(rowObj.getLong("anotherDepotId"));
                        }
                    } else {
                        throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ANOTHER_DEPOT_FAILED_CODE,
                                String.format(ExceptionConstants.DEPOT_HEAD_ANOTHER_DEPOT_FAILED_MSG));
                    }
                }
                if (StringUtil.isExist(rowObj.get("taxRate"))) {
                    depotItem.setTaxRate(rowObj.getBigDecimal("taxRate"));
                }
                if (StringUtil.isExist(rowObj.get("taxMoney"))) {
                    depotItem.setTaxMoney(rowObj.getBigDecimal("taxMoney"));
                }
                if (StringUtil.isExist(rowObj.get("taxLastMoney"))) {
                    depotItem.setTaxLastMoney(rowObj.getBigDecimal("taxLastMoney"));
                }
                if (StringUtil.isExist(rowObj.get("mType"))) {
                    depotItem.setMaterialType(rowObj.getString("mType"));
                }
                if (StringUtil.isExist(rowObj.get("remark"))) {
                    depotItem.setRemark(rowObj.getString("remark"));
                }
                //出库时判断库存是否充足
                if(BusinessConstants.DEPOTHEAD_TYPE_OUT.equals(depotHead.getType())){
                    String stockMsg = material.getName() + "-" + barCode;
                    BigDecimal stock = getCurrentStockByParam(depotItem.getDepotId(),depotItem.getMaterialId());
                    if(StringUtil.isNotEmpty(depotItem.getSku())) {
                        //对于sku商品要换个方式计算库存
                        stock = getSkuStockByParam(depotItem.getDepotId(),depotItem.getMaterialExtendId(),null,null);
                    }
                    if(StringUtil.isNotEmpty(depotItem.getBatchNumber())) {
                        //对于批次商品要换个方式计算库存
                        stock = getOneBatchNumberStock(depotItem.getDepotId(), barCode, depotItem.getBatchNumber());
                        stockMsg += "-批号" + depotItem.getBatchNumber();
                    }
                    BigDecimal thisRealNumber = depotItem.getBasicNumber()==null?BigDecimal.ZERO:depotItem.getBasicNumber();
                    if(StringUtil.isNotEmpty(depotItem.getBatchNumber())) {
                        //对于批次商品，直接使用当前填写的数量
                        thisRealNumber = depotItem.getOperNumber()==null?BigDecimal.ZERO:depotItem.getOperNumber();
                    }
                    if(systemConfigService.getForceApprovalFlag() && "0".equals(depotHead.getStatus())) {
                        //如果开启强审核，并且没有保存的同时审核，则跳过库存判断
                    } else {
                        if(!systemConfigService.getMinusStockFlag()) {
                            lockMaterialForStockCheck(depotItem, lockedStockKeys);
                            String stockScopeKey = getStockScopeKey(depotItem, barCode);
                            BigDecimal accumulatedNumber = outboundQuantityMap.getOrDefault(stockScopeKey, BigDecimal.ZERO).add(thisRealNumber);
                            outboundQuantityMap.put(stockScopeKey, accumulatedNumber);
                            //加锁后重新读取，避免并发零售出库同时通过库存校验
                            stock = getStockForDepotItem(depotItem, barCode);
                            if(stock.compareTo(accumulatedNumber)<0){
                                //如果开启出入库管理，并且类型等于采购退货、销售，则跳过
                                if(systemConfigService.getInOutManageFlag() &&
                                        (BusinessConstants.SUB_TYPE_PURCHASE_RETURN.equals(depotHead.getSubType())
                                                ||BusinessConstants.SUB_TYPE_SALES.equals(depotHead.getSubType()))) {
                                    //跳过
                                } else {
                                    throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_STOCK_NOT_ENOUGH_CODE,
                                            String.format(ExceptionConstants.MATERIAL_STOCK_NOT_ENOUGH_MSG, stockMsg));
                                }
                            }
                        }
                    }
                    //出库时处理序列号
                    if(!BusinessConstants.SUB_TYPE_TRANSFER.equals(depotHead.getSubType())) {
                        //判断商品是否开启序列号，开启的售出序列号，未开启的跳过
                        if(BusinessConstants.ENABLE_SERIAL_NUMBER_ENABLED.equals(material.getEnableSerialNumber())) {
                            //如果开启出入库管理，并且类型等于采购、采购退货、销售、销售退货，则跳过
                            if(systemConfigService.getInOutManageFlag() &&
                                    (BusinessConstants.SUB_TYPE_PURCHASE.equals(depotHead.getSubType())
                                            ||BusinessConstants.SUB_TYPE_PURCHASE_RETURN.equals(depotHead.getSubType())
                                            ||BusinessConstants.SUB_TYPE_SALES.equals(depotHead.getSubType())
                                            ||BusinessConstants.SUB_TYPE_SALES_RETURN.equals(depotHead.getSubType()))) {
                                //跳过
                            } else {
                                //售出序列号，获得当前操作人
                                User userInfo = userService.getCurrentUser();
                                serialNumberService.checkAndUpdateSerialNumber(depotItem, depotHead.getNumber(), userInfo, StringUtil.toNull(depotItem.getSnList()));
                            }
                        }
                    }
                }
                depotItemList.add(depotItem);
            }
            if (assemble) {
                normalizeAssembleCost(depotHead, depotItemList);
                if (!systemConfigService.getMinusStockFlag()
                        && (!systemConfigService.getForceApprovalFlag()
                        || BusinessConstants.BILLS_STATUS_AUDIT.equals(depotHead.getStatus()))) {
                    checkAssembleMaterialStock(depotHead.getNumber(), depotItemList);
                }
            }
            if (disassemble) {
                normalizeDisassembleCost(depotHead, depotItemList);
                if (!systemConfigService.getMinusStockFlag()
                        && (!systemConfigService.getForceApprovalFlag()
                        || BusinessConstants.BILLS_STATUS_AUDIT.equals(depotHead.getStatus()))) {
                    checkDisassembleMaterialStock(depotHead.getNumber(), depotItemList);
                }
            }
            //批量写入单据明细数据
            depotItemMapperEx.batchInsert(depotItemList);
            for (DepotItem depotItem : depotItemList) {
                //更新当前库存
                updateCurrentStock(depotItem);
                //更新当前成本价
                updateCurrentUnitPrice(depotItem);
                //更新商品的价格
                updateMaterialExtendPrice(depotItem.getMaterialExtendId(), depotHead.getSubType(), depotHead.getBillType(), depotItem.getUnitPrice());
            }
            //如果关联单据号非空则更新订单的状态,单据类型：采购入库单、销售出库单、盘点复盘单、其它入库单、其它出库单
            if(BusinessConstants.SUB_TYPE_PURCHASE.equals(depotHead.getSubType())
                    || BusinessConstants.SUB_TYPE_SALES.equals(depotHead.getSubType())
                    || BusinessConstants.SUB_TYPE_REPLAY.equals(depotHead.getSubType())
                    || BusinessConstants.SUB_TYPE_OTHER.equals(depotHead.getSubType())) {
                if(StringUtil.isNotEmpty(depotHead.getLinkNumber())) {
                    if (BusinessConstants.SUB_TYPE_SALES.equals(depotHead.getSubType())) {
                        depotHeadService.recalculateSalesOrderStatus(depotHead.getLinkNumber());
                    } else {
                        //单据状态:是否全部完成 2-全部完成 3-部分完成（针对订单的分批出入库）
                        String billStatus = getBillStatusByParam(depotHead, depotHead.getLinkNumber(), "normal");
                        changeBillStatus(depotHead.getLinkNumber(), billStatus);
                    }
                }
            }
            //当前单据类型为采购订单的逻辑
            if(BusinessConstants.SUB_TYPE_PURCHASE_ORDER.equals(depotHead.getSubType())) {
                //如果关联单据号非空则更新订单的状态,此处针对销售订单转采购订单的场景
                if(StringUtil.isNotEmpty(depotHead.getLinkNumber())) {
                    String billStatus = getBillStatusByParam(depotHead, depotHead.getLinkNumber(), "normal");
                    changeBillPurchaseStatus(depotHead.getLinkNumber(), billStatus);
                }
                //如果关联单据号非空则更新订单的状态,此处针对请购单转采购订单的场景
                if(StringUtil.isNotEmpty(depotHead.getLinkApply())) {
                    String billStatus = getBillStatusByParam(depotHead, depotHead.getLinkApply(), "apply");
                    changeBillStatus(depotHead.getLinkApply(), billStatus);
                }
            }
        } else {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ROW_FAILED_CODE,
                    String.format(ExceptionConstants.DEPOT_HEAD_ROW_FAILED_MSG));
        }
    }
    /**
     * 判断单据的状态
     * 通过数组对比：原单据的商品和商品数量（汇总） 与 分批操作后单据的商品和商品数量（汇总）
     * @param depotHead
     * @param linkStr
     * @return
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public String getBillStatusByParam(DepotHead depotHead, String linkStr, String linkType) {
        String res = BusinessConstants.BILLS_STATUS_SKIPED;
        List<DepotItemVo4MaterialAndSum> linkList;
        List<DepotItemVo4MaterialAndSum> batchList;
        if (BusinessConstants.SUB_TYPE_PURCHASE_ORDER.equals(depotHead.getSubType())) {
            //采购订单按来源明细和基本数量统计，避免同一商品使用不同单位时误判状态
            linkList = depotItemMapperEx.getSourceBillDetailBasicSum(linkStr);
            batchList = depotItemMapperEx.getLinkedBillDetailBasicSum(linkStr, linkType,
                    BusinessConstants.DEPOTHEAD_TYPE_OTHER, BusinessConstants.SUB_TYPE_PURCHASE_ORDER);
        } else if (BusinessConstants.SUB_TYPE_PURCHASE.equals(depotHead.getSubType())) {
            //采购入库只统计关联采购订单的采购入库明细
            linkList = depotItemMapperEx.getSourceBillDetailBasicSum(linkStr);
            batchList = depotItemMapperEx.getLinkedBillDetailBasicSum(linkStr, "normal",
                    BusinessConstants.DEPOTHEAD_TYPE_IN, BusinessConstants.SUB_TYPE_PURCHASE);
        } else if (BusinessConstants.SUB_TYPE_SALES.equals(depotHead.getSubType())) {
            linkList = depotItemMapperEx.getSourceBillDetailBasicSum(linkStr);
            batchList = depotItemMapperEx.getAuditedSalesOutboundBasicSum(linkStr);
        } else if (BusinessConstants.SUB_TYPE_OTHER.equals(depotHead.getSubType())) {
            //其它出入库按来源明细ID和基本数量统计，避免同商品多行、多单位时误判完成状态。
            linkList = depotItemMapperEx.getSourceBillDetailBasicSum(linkStr);
            batchList = depotItemMapperEx.getLinkedBillDetailBasicSum(linkStr, "normal",
                    depotHead.getType(), BusinessConstants.SUB_TYPE_OTHER);
        } else {
            //兼容其它原有单据链路
            linkList = depotItemMapperEx.getLinkBillDetailMaterialSum(linkStr);
            batchList = depotItemMapperEx.getBatchBillDetailMaterialSum(linkStr, linkType, depotHead.getType());
        }
        if (batchList == null || batchList.isEmpty()) {
            return BusinessConstants.BILLS_STATUS_AUDIT;
        }
        //将分批操作后的单据的商品和商品数据构造成Map
        Map<Long, BigDecimal> materialSumMap = new HashMap<>();
        for(DepotItemVo4MaterialAndSum materialAndSum : batchList) {
            materialSumMap.merge(materialAndSum.getMaterialExtendId(), materialAndSum.getOperNumber(), BigDecimal::add);
        }
        for(DepotItemVo4MaterialAndSum materialAndSum : linkList) {
            //过滤掉原单里面有数量为0的商品
            if(materialAndSum.getOperNumber().compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal materialSum = materialSumMap.get(materialAndSum.getMaterialExtendId());
                if (materialSum != null) {
                    if (materialSum.compareTo(materialAndSum.getOperNumber()) < 0) {
                        res = BusinessConstants.BILLS_STATUS_SKIPING;
                    }
                } else {
                    res = BusinessConstants.BILLS_STATUS_SKIPING;
                }
            }
        }
        return res;
    }

    /**
     * 更新单据状态
     * @param linkStr
     * @param billStatus
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void changeBillStatus(String linkStr, String billStatus) {
        DepotHead depotHeadOrders = new DepotHead();
        depotHeadOrders.setStatus(billStatus);
        DepotHeadExample example = new DepotHeadExample();
        List<String> linkNoList = StringUtil.strToStringList(linkStr);
        example.createCriteria().andNumberIn(linkNoList);
        try{
            depotHeadMapper.updateByExampleSelective(depotHeadOrders, example);
        }catch(Exception e){
            logger.error("异常码[{}],异常提示[{}],异常[{}]",
                    ExceptionConstants.DATA_WRITE_FAIL_CODE,ExceptionConstants.DATA_WRITE_FAIL_MSG,e);
            throw new BusinessRunTimeException(ExceptionConstants.DATA_WRITE_FAIL_CODE,
                    ExceptionConstants.DATA_WRITE_FAIL_MSG);
        }
    }

    /**
     * 更新单据状态,此处针对销售订单转采购订单的场景
     * @param linkStr
     * @param billStatus
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void changeBillPurchaseStatus(String linkStr, String billStatus) {
        DepotHead depotHeadOrders = new DepotHead();
        depotHeadOrders.setPurchaseStatus(billStatus);
        DepotHeadExample example = new DepotHeadExample();
        List<String> linkNoList = StringUtil.strToStringList(linkStr);
        example.createCriteria().andNumberIn(linkNoList);
        try{
            depotHeadMapper.updateByExampleSelective(depotHeadOrders, example);
        }catch(Exception e){
            logger.error("异常码[{}],异常提示[{}],异常[{}]",
                    ExceptionConstants.DATA_WRITE_FAIL_CODE,ExceptionConstants.DATA_WRITE_FAIL_MSG,e);
            throw new BusinessRunTimeException(ExceptionConstants.DATA_WRITE_FAIL_CODE,
                    ExceptionConstants.DATA_WRITE_FAIL_MSG);
        }
    }

    /**
     * 根据批号查询单据明细信息
     * @param materialExtendId
     * @param batchNumber
     * @return
     */
    public DepotItem getDepotItemByBatchNumber(Long materialExtendId, String batchNumber) {
        List<DepotItem> depotItemList = depotItemMapperEx.getDepotItemByBatchNumber(materialExtendId, batchNumber);
        if(null != depotItemList && depotItemList.size() > 0){
            return depotItemList.get(0);
        } else {
            return new DepotItem();
        }
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void deleteDepotItemHeadId(Long headerId)throws Exception {
        try{
            //1、查询删除前的单据明细
            List<DepotItem> depotItemList = getListByHeaderId(headerId);
            //2、删除单据明细
            DepotItemExample example = new DepotItemExample();
            example.createCriteria().andHeaderIdEqualTo(headerId);
            depotItemMapper.deleteByExample(example);
            //3、计算删除之后单据明细中商品的库存和移动平均成本，避免编辑时沿用旧明细成本。
            for(DepotItem depotItem : depotItemList){
                updateCurrentStock(depotItem);
                updateCurrentUnitPrice(depotItem);
            }
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
    }

    /**
     * 删除序列号和回收序列号
     * @param actionType
     * @throws Exception
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void deleteOrCancelSerialNumber(String actionType, DepotHead depotHead, Long headerId) throws Exception {
        if(actionType.equals("update")) {
            User userInfo = userService.getCurrentUser();
            if(BusinessConstants.DEPOTHEAD_TYPE_IN.equals(depotHead.getType())){
                //入库逻辑
                //判断如果有序列号被出库了就不允许修改该单据
                List<SerialNumber> snList = serialNumberMapperEx.getIsSellListByInBillNo(depotHead.getNumber());
                if(!snList.isEmpty()){
                    String sn = snList.get(0).getSerialNumber();
                    throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_SN_NOT_ALLOW_UPDATE_CODE,
                            String.format(ExceptionConstants.DEPOT_HEAD_SN_NOT_ALLOW_UPDATE_MSG, sn));
                } else {
                    String number = depotHead.getNumber();
                    SerialNumberExample example = new SerialNumberExample();
                    example.createCriteria().andInBillNoEqualTo(number);
                    serialNumberService.deleteByExample(example);
                }
            } else if(BusinessConstants.DEPOTHEAD_TYPE_OUT.equals(depotHead.getType())){
                //出库逻辑
                DepotItemExample example = new DepotItemExample();
                example.createCriteria().andHeaderIdEqualTo(headerId).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
                List<DepotItem> depotItemList = depotItemMapper.selectByExample(example);
                if(null != depotItemList && depotItemList.size() > 0){
                    for (DepotItem depotItem : depotItemList){
                        if(StringUtil.isNotEmpty(depotItem.getSnList())){
                            serialNumberService.cancelSerialNumber(depotItem.getMaterialId(), depotHead.getNumber(), (depotItem.getBasicNumber() == null ? 0 : depotItem.getBasicNumber()).intValue(), userInfo);
                        }
                    }
                }
            }
        }
    }

    /**
     * 针对组装单、拆卸单校验是否存在组合件和普通子件
     * @param rowArr
     * @param subType
     */
    public void checkAssembleWithMaterialType(JSONArray rowArr, String subType) {
        if(BusinessConstants.SUB_TYPE_ASSEMBLE.equals(subType) ||
                BusinessConstants.SUB_TYPE_DISASSEMBLE.equals(subType)) {
            if(rowArr.size() < 2) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_CHECK_ASSEMBLE_EMPTY_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_CHECK_ASSEMBLE_EMPTY_MSG));
            }
            for (int index = 0; index < rowArr.size(); index++) {
                JSONObject rowObject = JSONObject.parseObject(rowArr.getString(index));
                String expectedMaterialType = index == 0 ? "组合件" : "普通子件";
                if (!expectedMaterialType.equals(rowObject.getString("mType"))) {
                    throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ASSEMBLE_STRUCTURE_CODE,
                            ExceptionConstants.DEPOT_HEAD_ASSEMBLE_STRUCTURE_MSG);
                }
            }
        }
    }

    /**
     * 校验多行明细当中是否存在重复的序列号
     * @param rowArr
     */
    public void checkSerialNumberRepeatWithCurrent(JSONArray rowArr) {
        List<String> allSnArr = new ArrayList<>();
        for (int i = 0; i < rowArr.size(); i++) {
            JSONObject rowObj = JSONObject.parseObject(rowArr.getString(i));
            if(StringUtil.isNotEmpty(rowObj.getString("snList"))) {
                String snList = rowObj.getString("snList");
                snList = snList.replaceAll("，", ",");
                List<String> snArr = StringUtil.strToStringList(snList);
                if(snArr!=null && !snArr.isEmpty()) {
                    allSnArr.addAll(snArr);
                }
            }
        }
        Set<String> seen = new HashSet<>();
        Set<String> duplicates = new HashSet<>();
        for (String str : allSnArr) {
            if (!seen.add(str)) {
                duplicates.add(str);
            }
        }
        if(!duplicates.isEmpty()) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_CHECK_SERIAL_NUMBER_REPEAT_CODE,
                    String.format(ExceptionConstants.DEPOT_HEAD_CHECK_SERIAL_NUMBER_REPEAT_MSG, String.join(", ", duplicates)));
        }
    }

    /**
     * 更新商品的价格
     * @param meId
     * @param subType
     * @param unitPrice
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void updateMaterialExtendPrice(Long meId, String subType, String billType, BigDecimal unitPrice) throws Exception {
        if(systemConfigService.getUpdateUnitPriceFlag()) {
            if (unitPrice!=null) {
                MaterialExtend materialExtend = new MaterialExtend();
                materialExtend.setId(meId);
                if(BusinessConstants.SUB_TYPE_PURCHASE.equals(subType)) {
                    materialExtend.setPurchaseDecimal(unitPrice);
                }
                if(BusinessConstants.SUB_TYPE_SALES.equals(subType)) {
                    materialExtend.setWholesaleDecimal(unitPrice);
                }
                if(BusinessConstants.SUB_TYPE_RETAIL.equals(subType)) {
                    materialExtend.setCommodityDecimal(unitPrice);
                }
                //其它入库-生产入库的情况更新采购单价
                if(BusinessConstants.SUB_TYPE_OTHER.equals(subType)) {
                    if(BusinessConstants.BILL_TYPE_PRODUCE_IN.equals(billType)) {
                        materialExtend.setPurchaseDecimal(unitPrice);
                    }
                }
                materialExtendService.updateMaterialExtend(materialExtend);
            }
        }
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public List<DepotItemStockWarningCount> findStockWarningCount(Integer offset, Integer rows, String materialParam, List<Long> depotList, List<Long> categoryList) {
        List<DepotItemStockWarningCount> list = null;
        try{
            list =depotItemMapperEx.findStockWarningCount(offset, rows, materialParam, depotList, categoryList);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int findStockWarningCountTotal(String materialParam, List<Long> depotList, List<Long> categoryList) {
        int result = 0;
        try{
            result =depotItemMapperEx.findStockWarningCountTotal(materialParam, depotList, categoryList);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    /**
     * 库存统计-sku
     * @param depotId
     * @param meId
     * @param beginTime
     * @param endTime
     * @return
     */
    public BigDecimal getSkuStockByParam(Long depotId, Long meId, String beginTime, String endTime) throws Exception {
        Boolean forceFlag = systemConfigService.getForceApprovalFlag();
        Boolean inOutManageFlag = systemConfigService.getInOutManageFlag();
        List<Long> depotList = depotService.parseDepotList(depotId);
        //盘点复盘后数量的变动
        BigDecimal stockCheckSum = depotItemMapperEx.getSkuStockCheckSumByDepotList(depotList, meId, forceFlag, beginTime, endTime);
        DepotItemVo4Stock stockObj = depotItemMapperEx.getSkuStockByParamWithDepotList(depotList, meId, forceFlag, inOutManageFlag, beginTime, endTime);
        BigDecimal stockSum = BigDecimal.ZERO;
        if(stockObj!=null) {
            BigDecimal inTotal = stockObj.getInTotal();
            BigDecimal transfInTotal = stockObj.getTransfInTotal();
            BigDecimal assemInTotal = stockObj.getAssemInTotal();
            BigDecimal disAssemInTotal = stockObj.getDisAssemInTotal();
            BigDecimal outTotal = stockObj.getOutTotal();
            BigDecimal transfOutTotal = stockObj.getTransfOutTotal();
            BigDecimal assemOutTotal = stockObj.getAssemOutTotal();
            BigDecimal disAssemOutTotal = stockObj.getDisAssemOutTotal();
            stockSum = inTotal.add(transfInTotal).add(assemInTotal).add(disAssemInTotal)
                    .subtract(outTotal).subtract(transfOutTotal).subtract(assemOutTotal).subtract(disAssemOutTotal);
        }
        return stockCheckSum.add(stockSum);
    }

    /**
     * 库存统计-单仓库
     * @param depotId
     * @param mId
     * @param beginTime
     * @param endTime
     * @return
     */
    public BigDecimal getStockByParam(Long depotId, Long mId, String beginTime, String endTime) throws Exception {
        List<Long> depotList = depotService.parseDepotList(depotId);
        return getStockByParamWithDepotList(depotList, mId, beginTime, endTime);
    }

    /**
     * 库存统计-多仓库
     * @param depotList
     * @param mId
     * @param beginTime
     * @param endTime
     * @return
     */
    public BigDecimal getStockByParamWithDepotList(List<Long> depotList, Long mId, String beginTime, String endTime) throws Exception {
        Boolean forceFlag = systemConfigService.getForceApprovalFlag();
        Boolean inOutManageFlag = systemConfigService.getInOutManageFlag();
        //初始库存
        BigDecimal initStock = zeroIfNull(materialService.getInitStockByMidAndDepotList(depotList, mId));
        //盘点复盘后数量的变动
        BigDecimal stockCheckSum = zeroIfNull(depotItemMapperEx.getStockCheckSumByDepotList(depotList, mId, forceFlag, beginTime, endTime));
        DepotItemVo4Stock stockObj = depotItemMapperEx.getStockByParamWithDepotList(depotList, mId, forceFlag, inOutManageFlag, beginTime, endTime);
        BigDecimal stockSum = BigDecimal.ZERO;
        if(stockObj!=null) {
            BigDecimal inTotal = zeroIfNull(stockObj.getInTotal());
            BigDecimal transfInTotal = zeroIfNull(stockObj.getTransfInTotal());
            BigDecimal assemInTotal = zeroIfNull(stockObj.getAssemInTotal());
            BigDecimal disAssemInTotal = zeroIfNull(stockObj.getDisAssemInTotal());
            BigDecimal outTotal = zeroIfNull(stockObj.getOutTotal());
            BigDecimal transfOutTotal = zeroIfNull(stockObj.getTransfOutTotal());
            BigDecimal assemOutTotal = zeroIfNull(stockObj.getAssemOutTotal());
            BigDecimal disAssemOutTotal = zeroIfNull(stockObj.getDisAssemOutTotal());
            stockSum = inTotal.add(transfInTotal).add(assemInTotal).add(disAssemInTotal)
                    .subtract(outTotal).subtract(transfOutTotal).subtract(assemOutTotal).subtract(disAssemOutTotal);
        }
        return initStock.add(stockCheckSum).add(stockSum);
    }

    /**
     * 统计时间段内的入库和出库数量-多仓库
     * @param depotList
     * @param mId
     * @param beginTime
     * @param endTime
     * @return
     */
    public Map<String, BigDecimal> getIntervalMapByParamWithDepotList(List<Long> depotList, Long mId, String beginTime, String endTime) throws Exception {
        Boolean forceFlag = systemConfigService.getForceApprovalFlag();
        Boolean inOutManageFlag = systemConfigService.getInOutManageFlag();
        Map<String,BigDecimal> intervalMap = new HashMap<>();
        BigDecimal inSum = BigDecimal.ZERO;
        BigDecimal outSum = BigDecimal.ZERO;
        //盘点复盘后数量的变动
        BigDecimal stockCheckSum = zeroIfNull(depotItemMapperEx.getStockCheckSumByDepotList(depotList, mId, forceFlag, beginTime, endTime));
        DepotItemVo4Stock stockObj = depotItemMapperEx.getStockByParamWithDepotList(depotList, mId, forceFlag, inOutManageFlag, beginTime, endTime);
        if(stockObj!=null) {
            BigDecimal inTotal = zeroIfNull(stockObj.getInTotal());
            BigDecimal transfInTotal = zeroIfNull(stockObj.getTransfInTotal());
            BigDecimal assemInTotal = zeroIfNull(stockObj.getAssemInTotal());
            BigDecimal disAssemInTotal = zeroIfNull(stockObj.getDisAssemInTotal());
            inSum = inTotal.add(transfInTotal).add(assemInTotal).add(disAssemInTotal);
            BigDecimal outTotal = zeroIfNull(stockObj.getOutTotal());
            BigDecimal transfOutTotal = zeroIfNull(stockObj.getTransfOutTotal());
            BigDecimal assemOutTotal = zeroIfNull(stockObj.getAssemOutTotal());
            BigDecimal disAssemOutTotal = zeroIfNull(stockObj.getDisAssemOutTotal());
            outSum = outTotal.add(transfOutTotal).add(assemOutTotal).add(disAssemOutTotal);
        }
        if(stockCheckSum.compareTo(BigDecimal.ZERO)>0) {
            inSum = inSum.add(stockCheckSum);
        } else {
            //盘点复盘数量为负数代表出库
            outSum = outSum.subtract(stockCheckSum);
        }
        intervalMap.put("inSum", inSum);
        intervalMap.put("outSum", outSum);
        return intervalMap;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 根据单据明细来批量更新当前库存
     * @param depotItem
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void updateCurrentStock(DepotItem depotItem) throws Exception {
        BigDecimal currentUnitPrice = materialCurrentStockMapperEx.getCurrentUnitPriceByMId(depotItem.getMaterialId());
        updateCurrentStockFun(depotItem.getMaterialId(), depotItem.getDepotId(), currentUnitPrice);
        if(depotItem.getAnotherDepotId()!=null){
            updateCurrentStockFun(depotItem.getMaterialId(), depotItem.getAnotherDepotId(), currentUnitPrice);
        }
    }

    /**
     * 根据单据明细来批量更新当前成本价
     * @param depotItem
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void updateCurrentUnitPrice(DepotItem depotItem) throws Exception {
        materialMapperEx.lockById(depotItem.getMaterialId());
        Boolean forceFlag = systemConfigService.getForceApprovalFlag();
        //此处给出入库管理的传值默认为false，不然会导致查询不到销售相关的单据
        Boolean inOutManageFlag = false;
        //查询多单位信息
        Unit unitInfo = materialService.findUnit(depotItem.getMaterialId());
        List<DepotItemVo4DetailByTypeAndMId> itemList = findDetailByDepotIdsAndMaterialIdList(null, forceFlag, inOutManageFlag, null,
                null, null, null, null, depotItem.getMaterialId(), null, null);
        Collections.reverse(itemList); //倒序之后变成按时间从前往后排序
        BigDecimal currentNumber = BigDecimal.ZERO;
        BigDecimal currentUnitPrice = BigDecimal.ZERO;
        BigDecimal currentAllPrice = BigDecimal.ZERO;
        for(DepotItemVo4DetailByTypeAndMId item: itemList) {
            BigDecimal basicNumber = item.getBnum()!=null?item.getBnum():BigDecimal.ZERO;
            //数量*单价  另外计算新的成本价
            BigDecimal allPrice = unitService.parseAllPriceByUnit(item.getAllPrice()!=null?item.getAllPrice():BigDecimal.ZERO, unitInfo, item.getMaterialUnit());
            boolean zeroCostStockBill = BusinessConstants.SUB_TYPE_ASSEMBLE.equals(item.getSubType())
                    || BusinessConstants.SUB_TYPE_DISASSEMBLE.equals(item.getSubType());
            if (basicNumber.compareTo(BigDecimal.ZERO) != 0
                    && (allPrice.compareTo(BigDecimal.ZERO) != 0 || zeroCostStockBill)) {
                //入库
                if (BusinessConstants.DEPOTHEAD_TYPE_IN.equals(item.getType())) {
                    //零售退货、销售退货
                    if (BusinessConstants.SUB_TYPE_RETAIL_RETURN.equals(item.getSubType()) || BusinessConstants.SUB_TYPE_SALES_RETURN.equals(item.getSubType())) {
                        //数量*当前的成本单价
                        currentNumber = currentNumber.add(basicNumber);
                        currentAllPrice = currentAllPrice.add(basicNumber.multiply(currentUnitPrice));
                    } else {
                        currentAllPrice = currentAllPrice.add(allPrice);
                        currentNumber = currentNumber.add(basicNumber);
                        //只有当前库存总金额和当前库存数量都大于0才计算移动平均价
                        if (currentAllPrice.compareTo(BigDecimal.ZERO) > 0 && currentNumber.compareTo(BigDecimal.ZERO) > 0) {
                            currentUnitPrice = currentAllPrice.divide(currentNumber, 4, BigDecimal.ROUND_HALF_UP);
                        } else {
                            currentUnitPrice = item.getUnitPrice();
                        }
                    }
                }
                //出库
                if (BusinessConstants.DEPOTHEAD_TYPE_OUT.equals(item.getType())) {
                    //采购退货
                    if (BusinessConstants.SUB_TYPE_PURCHASE_RETURN.equals(item.getSubType())) {
                        currentAllPrice = currentAllPrice.add(allPrice);
                        currentNumber = currentNumber.add(basicNumber);
                        //只有当前库存总金额和当前库存数量都大于0才计算移动平均价
                        if (currentAllPrice.compareTo(BigDecimal.ZERO) > 0 && currentNumber.compareTo(BigDecimal.ZERO) > 0) {
                            currentUnitPrice = currentAllPrice.divide(currentNumber, 4, BigDecimal.ROUND_HALF_UP);
                        } else {
                            currentUnitPrice = item.getUnitPrice();
                        }
                    } else {
                        //数量*当前的成本单价
                        currentNumber = currentNumber.add(basicNumber);
                        currentAllPrice = currentAllPrice.add(basicNumber.multiply(currentUnitPrice));
                    }
                }
                //组装产出的组合件按已固化的子件成本入库，普通子件按当前移动平均价出库。
                if (BusinessConstants.SUB_TYPE_ASSEMBLE.equals(item.getSubType())) {
                    currentNumber = currentNumber.add(basicNumber);
                    if ("组合件".equals(item.getMaterialType())) {
                        currentAllPrice = currentAllPrice.add(zeroIfNull(item.getAllPrice()));
                        if (currentAllPrice.compareTo(BigDecimal.ZERO) > 0
                                && currentNumber.compareTo(BigDecimal.ZERO) > 0) {
                            currentUnitPrice = currentAllPrice.divide(currentNumber, 4, BigDecimal.ROUND_HALF_UP);
                        }
                    } else {
                        currentAllPrice = currentAllPrice.add(basicNumber.multiply(currentUnitPrice));
                    }
                }
                //拆卸使用已固化且守恒的成本：组合件出库为负，普通子件入库为正。
                if (BusinessConstants.SUB_TYPE_DISASSEMBLE.equals(item.getSubType())) {
                    currentNumber = currentNumber.add(basicNumber);
                    currentAllPrice = currentAllPrice.add(zeroIfNull(item.getAllPrice()));
                    if (currentAllPrice.compareTo(BigDecimal.ZERO) > 0
                            && currentNumber.compareTo(BigDecimal.ZERO) > 0) {
                        currentUnitPrice = currentAllPrice.divide(currentNumber, 4, BigDecimal.ROUND_HALF_UP);
                    } else if (basicNumber.compareTo(BigDecimal.ZERO) > 0 && item.getUnitPrice() != null) {
                        currentUnitPrice = item.getUnitPrice();
                    }
                }
                if (BusinessConstants.SUB_TYPE_REPLAY.equals(item.getSubType())) {
                    //数量*当前的成本单价
                    currentNumber = currentNumber.add(basicNumber);
                    currentAllPrice = currentAllPrice.add(basicNumber.multiply(currentUnitPrice));
                }
                //防止单价金额溢出
                if(currentUnitPrice.compareTo(BigDecimal.valueOf(100000000))>0 || currentUnitPrice.compareTo(BigDecimal.valueOf(-100000000))<0) {
                    currentUnitPrice = BigDecimal.ZERO;
                }
            }
        }
        //更新实时库存中的当前单价
        materialCurrentStockMapperEx.updateUnitPriceByMId(currentUnitPrice, depotItem.getMaterialId());
    }

    /**
     * 根据商品和仓库来更新当前库存
     * @param mId
     * @param dId
     */
    public void updateCurrentStockFun(Long mId, Long dId, BigDecimal currentUnitPrice) throws Exception {
        if(mId!=null && dId!=null) {
            // Serialize the stock snapshot rebuild and the select/insert pair.
            materialMapperEx.lockById(mId);
            MaterialCurrentStockExample example = new MaterialCurrentStockExample();
            example.createCriteria().andMaterialIdEqualTo(mId).andDepotIdEqualTo(dId)
                    .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
            List<MaterialCurrentStock> list = materialCurrentStockMapper.selectByExample(example);
            MaterialCurrentStock materialCurrentStock = new MaterialCurrentStock();
            materialCurrentStock.setMaterialId(mId);
            materialCurrentStock.setDepotId(dId);
            materialCurrentStock.setCurrentNumber(getStockByParam(dId,mId,null,null));
            materialCurrentStock.setCurrentUnitPrice(currentUnitPrice);
            if(list!=null && list.size()>0) {
                Long mcsId = list.get(0).getId();
                materialCurrentStock.setId(mcsId);
                materialCurrentStockMapper.updateByPrimaryKeySelective(materialCurrentStock);
            } else {
                materialCurrentStockMapper.insertSelective(materialCurrentStock);
            }
        }
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public BigDecimal getFinishPurchaseNumber(Long meId, Long id, Long headerId, Unit unitInfo, String materialUnit, String linkType) {
        BigDecimal count = BigDecimal.ZERO;
        Long linkId = id;
        DepotHead depotHead =depotHeadMapper.selectByPrimaryKey(headerId);
        String linkStr = depotHead.getNumber(); //订单号
        // 针对以销定购的情况
        if(BusinessConstants.SUB_TYPE_SALES_ORDER.equals(depotHead.getSubType())) {
            String goToType = BusinessConstants.SUB_TYPE_PURCHASE_ORDER;
            String noType = "normal";
            count = depotItemMapperEx.getFinishNumber(meId, linkId, linkStr, noType, goToType);
            //根据多单位情况进行数量的转换
            if(materialUnit.equals(unitInfo.getOtherUnit()) && unitInfo.getRatio()!=null && unitInfo.getRatio().compareTo(BigDecimal.ZERO)!=0) {
                count = count.divide(unitInfo.getRatio(),2,BigDecimal.ROUND_HALF_UP);
            }
            if(materialUnit.equals(unitInfo.getOtherUnitTwo()) && unitInfo.getRatioTwo()!=null && unitInfo.getRatioTwo().compareTo(BigDecimal.ZERO)!=0) {
                count = count.divide(unitInfo.getRatioTwo(),2,BigDecimal.ROUND_HALF_UP);
            }
            if(materialUnit.equals(unitInfo.getOtherUnitThree()) && unitInfo.getRatioThree()!=null && unitInfo.getRatioThree().compareTo(BigDecimal.ZERO)!=0) {
                count = count.divide(unitInfo.getRatioThree(),2,BigDecimal.ROUND_HALF_UP);
            }
        }
        return count;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public BigDecimal getFinishNumber(Long meId, Long id, Long headerId, Unit unitInfo, String materialUnit, String linkType) {
        Long linkId = id;
        String goToType = "";
        DepotHead depotHead =depotHeadMapper.selectByPrimaryKey(headerId);
        String linkStr = depotHead.getNumber(); //订单号
        if("other".equals(linkType)) {
            //采购入库、采购退货、销售出库、销售退货都转其它入库
            if(BusinessConstants.SUB_TYPE_PURCHASE.equals(depotHead.getSubType())
                    || BusinessConstants.SUB_TYPE_PURCHASE_RETURN.equals(depotHead.getSubType())
                    || BusinessConstants.SUB_TYPE_SALES.equals(depotHead.getSubType())
                    || BusinessConstants.SUB_TYPE_SALES_RETURN.equals(depotHead.getSubType())) {
                goToType = BusinessConstants.SUB_TYPE_OTHER;
            }
        } else if("basic".equals(linkType)||"purchase".equals(linkType)) {
            //采购订单转采购入库
            if(BusinessConstants.SUB_TYPE_PURCHASE_ORDER.equals(depotHead.getSubType())) {
                goToType = BusinessConstants.SUB_TYPE_PURCHASE;
            }
            //销售订单转销售出库
            if(BusinessConstants.SUB_TYPE_SALES_ORDER.equals(depotHead.getSubType())) {
                goToType = BusinessConstants.SUB_TYPE_SALES;
            }
            //采购入库转采购退货
            if(BusinessConstants.SUB_TYPE_PURCHASE.equals(depotHead.getSubType())) {
                goToType = BusinessConstants.SUB_TYPE_PURCHASE_RETURN;
            }
            //销售出库转销售退货
            if(BusinessConstants.SUB_TYPE_SALES.equals(depotHead.getSubType())) {
                goToType = BusinessConstants.SUB_TYPE_SALES_RETURN;
            }
        }
        String noType = "normal";
        if(BusinessConstants.SUB_TYPE_PURCHASE_APPLY.equals(depotHead.getSubType())) {
            noType = "apply";
        }
        BigDecimal count = depotItemMapperEx.getFinishNumber(meId, linkId, linkStr, noType, goToType);
        //根据多单位情况进行数量的转换
        if(materialUnit.equals(unitInfo.getOtherUnit()) && unitInfo.getRatio()!=null && unitInfo.getRatio().compareTo(BigDecimal.ZERO)!=0) {
            count = count.divide(unitInfo.getRatio(),2,BigDecimal.ROUND_HALF_UP);
        }
        if(materialUnit.equals(unitInfo.getOtherUnitTwo()) && unitInfo.getRatioTwo()!=null && unitInfo.getRatioTwo().compareTo(BigDecimal.ZERO)!=0) {
            count = count.divide(unitInfo.getRatioTwo(),2,BigDecimal.ROUND_HALF_UP);
        }
        if(materialUnit.equals(unitInfo.getOtherUnitThree()) && unitInfo.getRatioThree()!=null && unitInfo.getRatioThree().compareTo(BigDecimal.ZERO)!=0) {
            count = count.divide(unitInfo.getRatioThree(),2,BigDecimal.ROUND_HALF_UP);
        }
        return count;
    }

    /**
     * 除去此单据之外的已入库|已出库|已转采购
     * @param currentSubType
     * @param meId
     * @param linkId
     * @param preHeaderId
     * @param currentHeaderId
     * @param unitInfo
     * @param materialUnit
     * @return
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public BigDecimal getRealFinishNumber(String currentSubType, Long meId, Long linkId, Long preHeaderId, Long currentHeaderId, Unit unitInfo, String materialUnit) {
        String goToType = currentSubType;
        DepotHead depotHead =depotHeadMapper.selectByPrimaryKey(preHeaderId);
        String linkStr = depotHead.getNumber(); //订单号
        String linkType = "normal";
        if(BusinessConstants.SUB_TYPE_PURCHASE_APPLY.equals(depotHead.getSubType())) {
            linkType = "apply";
        }
        BigDecimal count = depotItemMapperEx.getRealFinishNumber(meId, linkId, linkStr, linkType, currentHeaderId, goToType);
        //根据多单位情况进行数量的转换
        if(materialUnit.equals(unitInfo.getOtherUnit()) && unitInfo.getRatio()!=null && unitInfo.getRatio().compareTo(BigDecimal.ZERO)!=0) {
            count = count.divide(unitInfo.getRatio(),2,BigDecimal.ROUND_HALF_UP);
        }
        if(materialUnit.equals(unitInfo.getOtherUnitTwo()) && unitInfo.getRatioTwo()!=null && unitInfo.getRatioTwo().compareTo(BigDecimal.ZERO)!=0) {
            count = count.divide(unitInfo.getRatioTwo(),2,BigDecimal.ROUND_HALF_UP);
        }
        if(materialUnit.equals(unitInfo.getOtherUnitThree()) && unitInfo.getRatioThree()!=null && unitInfo.getRatioThree().compareTo(BigDecimal.ZERO)!=0) {
            count = count.divide(unitInfo.getRatioThree(),2,BigDecimal.ROUND_HALF_UP);
        }
        return count;
    }

    public List<DepotItemVoBatchNumberList> getBatchNumberList(String number, String name, Long depotId, String barCode,
                                                               String batchNumber, Boolean forceFlag, Boolean inOutManageFlag) throws Exception {
        List<DepotItemVoBatchNumberList> reslist = new ArrayList<>();
        List<DepotItemVoBatchNumberList> list =  depotItemMapperEx.getBatchNumberList(StringUtil.toNull(number), name,
                depotId, barCode, batchNumber, forceFlag, inOutManageFlag);
        for(DepotItemVoBatchNumberList bn: list) {
            if(bn.getTotalNum()!=null && bn.getTotalNum().compareTo(BigDecimal.ZERO)>0) {
                bn.setExpirationDateStr(Tools.parseDateToStr(bn.getExpirationDate()));
                if(bn.getUnitId()!=null) {
                    Unit unit = unitService.getUnit(bn.getUnitId());
                    String commodityUnit = bn.getCommodityUnit();
                    bn.setTotalNum(unitService.parseStockByUnit(bn.getTotalNum(), unit, commodityUnit));
                }
                reslist.add(bn);
            }
        }
        return reslist;
    }

    /**
     * 查询某个批号的商品库存
     * @param depotId
     * @param barCode
     * @param batchNumber
     * @return
     * @throws Exception
     */
    public BigDecimal getOneBatchNumberStock(Long depotId, String barCode, String batchNumber) throws Exception {
        BigDecimal totalNum = BigDecimal.ZERO;
        Boolean forceFlag = systemConfigService.getForceApprovalFlag();
        Boolean inOutManageFlag = systemConfigService.getInOutManageFlag();
        List<DepotItemVoBatchNumberList> list =  depotItemMapperEx.getBatchNumberList(null, null,
                depotId, barCode, batchNumber, forceFlag, inOutManageFlag);
        if(list!=null && list.size()>0) {
            for (DepotItemVoBatchNumberList bn : list) {
                BigDecimal rowTotal = bn.getTotalNum() == null ? BigDecimal.ZERO : bn.getTotalNum();
                if(rowTotal.compareTo(BigDecimal.ZERO)>0) {
                if(bn.getUnitId()!=null) {
                    Unit unit = unitService.getUnit(bn.getUnitId());
                    String commodityUnit = bn.getCommodityUnit();
                        rowTotal = unitService.parseStockByUnit(rowTotal, unit, commodityUnit);
                    }
                }
                totalNum = totalNum.add(rowTotal);
            }
        }
        return totalNum;
    }

    public Long getCountByMaterialAndDepot(Long mId, Long depotId) {
        return depotItemMapperEx.getCountByMaterialAndDepot(mId, depotId);
    }

    public JSONObject parseMapByExcelData(List<String> barCodeList, List<Map<String, String>> detailList, String prefixNo) throws Exception {
        JSONObject map = new JSONObject();
        JSONArray arr = new JSONArray();
        List<MaterialVo4Unit> list = depotItemMapperEx.getBillItemByParam(barCodeList);
        Map<String, MaterialVo4Unit> materialMap = new HashMap<>();
        Map<String, Long> depotMap = new HashMap<>();
        for (MaterialVo4Unit material: list) {
            materialMap.put(material.getmBarCode(), material);
        }
        JSONArray depotArr = depotService.findDepotByCurrentUser();
        for (Object depotObj: depotArr) {
            if(depotObj!=null) {
                JSONObject depotObject = JSONObject.parseObject(depotObj.toString());
                depotMap.put(depotObject.getString("depotName"), depotObject.getLong("id"));
            }
        }
        for (Map<String, String> detailMap: detailList) {
            JSONObject item = new JSONObject();
            String barCode = detailMap.get("barCode");
            if(StringUtil.isNotEmpty(barCode)) {
                MaterialVo4Unit m = materialMap.get(barCode);
                if(m!=null) {
                    //判断仓库是否存在
                    String depotName = detailMap.get("depotName");
                    if(StringUtil.isNotEmpty(depotName)) {
                        if(depotMap.get(depotName)!=null) {
                            item.put("depotName", depotName);
                            item.put("depotId", depotMap.get(depotName));
                        } else {
                            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_ITEM_DEPOTNAME_IS_NOT_EXIST_CODE,
                                    String.format(ExceptionConstants.DEPOT_ITEM_DEPOTNAME_IS_NOT_EXIST_MSG, depotName));
                        }
                    }
                    item.put("barCode", barCode);
                    item.put("name", m.getName());
                    item.put("standard", m.getStandard());
                    if(StringUtil.isNotEmpty(m.getModel())) {
                        item.put("model", m.getModel());
                    }
                    if(StringUtil.isNotEmpty(m.getColor())) {
                        item.put("color", m.getColor());
                    }
                    if(StringUtil.isNotEmpty(m.getSku())) {
                        item.put("sku", m.getSku());
                    }
                    BigDecimal stock = BigDecimal.ZERO;
                    if(StringUtil.isNotEmpty(m.getSku())){
                        stock = getSkuStockByParam(null, m.getMeId(),null,null);
                    } else {
                        stock = getCurrentStockByParam(null, m.getId());
                    }
                    item.put("stock", stock);
                    item.put("unit", m.getCommodityUnit());
                    BigDecimal operNumber = BigDecimal.ZERO;
                    BigDecimal unitPrice = BigDecimal.ZERO;
                    BigDecimal taxRate = BigDecimal.ZERO;
                    if(StringUtil.isNotEmpty(detailMap.get("num"))) {
                        operNumber = new BigDecimal(detailMap.get("num"));
                    }
                    if(StringUtil.isNotEmpty(detailMap.get("unitPrice"))) {
                        unitPrice = new BigDecimal(detailMap.get("unitPrice"));
                    } else {
                        if("CGDD".equals(prefixNo)) {
                            unitPrice = m.getPurchaseDecimal();
                        } else if("XSDD".equals(prefixNo)) {
                            unitPrice = m.getWholesaleDecimal();
                        }
                    }
                    if(StringUtil.isNotEmpty(detailMap.get("taxRate"))) {
                        taxRate = new BigDecimal(detailMap.get("taxRate"));
                    }
                    String remark = detailMap.get("remark");
                    item.put("operNumber", operNumber);
                    item.put("unitPrice", unitPrice);
                    BigDecimal allPrice = BigDecimal.ZERO;
                    if(unitPrice!=null && unitPrice.compareTo(BigDecimal.ZERO)!=0) {
                        allPrice = unitPrice.multiply(operNumber).setScale(2, BigDecimal.ROUND_HALF_UP);
                    }
                    BigDecimal taxMoney = BigDecimal.ZERO;
                    BigDecimal taxLastMoney = BigDecimal.ZERO;
                    if(systemConfigService.getMaterialPriceTaxFlag()) {
                        //商品价格含税-开启
                        if(taxRate.compareTo(BigDecimal.ZERO) != 0) {
                            BigDecimal taxRatePercent = taxRate.divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
                            taxMoney = allPrice.divide(BigDecimal.ONE.add(taxRatePercent), 2, BigDecimal.ROUND_HALF_UP)
                                    .multiply(taxRatePercent).setScale(2, BigDecimal.ROUND_HALF_UP);
                        }
                        taxLastMoney = allPrice;
                    } else {
                        if(taxRate.compareTo(BigDecimal.ZERO) != 0) {
                            taxMoney = taxRate.multiply(allPrice).divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
                        }
                        taxLastMoney = allPrice.add(taxMoney);
                    }
                    item.put("allPrice", allPrice);
                    item.put("taxRate", taxRate);
                    item.put("taxMoney", taxMoney);
                    item.put("taxLastMoney", taxLastMoney);
                    item.put("remark", remark);
                    arr.add(item);
                } else {
                    throw new BusinessRunTimeException(ExceptionConstants.DEPOT_ITEM_BARCODE_IS_NOT_EXIST_CODE,
                            String.format(ExceptionConstants.DEPOT_ITEM_BARCODE_IS_NOT_EXIST_MSG, barCode));
                }
            }
        }
        map.put("rows", arr);
        return map;
    }

    public BigDecimal getLastUnitPriceByParam(Long organId, Long meId, String prefixNo) {
        String type = "";
        String subType = "";
        if("XSDD".equals(prefixNo)) {
            type = "其它";
            subType = "销售订单";
        } else if("XSCK".equals(prefixNo)) {
            type = "出库";
            subType = "销售";
        } else if("XSTH".equals(prefixNo)) {
            type = "入库";
            subType = "销售退货";
        } else if("QTCK".equals(prefixNo)) {
            type = "出库";
            subType = "其它";
        }
        return depotItemMapperEx.getLastUnitPriceByParam(organId, meId, type, subType);
    }

    public BigDecimal getCurrentStockByParam(Long depotId, Long mId) {
        BigDecimal stock = depotItemMapperEx.getCurrentStockByParam(depotId, mId);
        return stock!=null? stock: BigDecimal.ZERO;
    }

    /**
     * 校验单据中的商品库存是否不足
     * @param number
     * @param headerId
     * @return
     * @throws Exception
     */
    public void checkMaterialStock(String number, Long headerId) throws Exception {
        DepotHead depotHead = new DepotHead();
        depotHead.setId(headerId);
        depotHead.setNumber(number);
        checkMaterialStock(Collections.singletonList(depotHead));
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void refreshAssembleCost(DepotHead depotHead) throws Exception {
        if (depotHead == null || depotHead.getId() == null) {
            return;
        }
        List<DepotItem> detailList = getListByHeaderId(depotHead.getId());
        normalizeAssembleCost(depotHead, detailList);
        for (DepotItem detail : detailList) {
            depotItemMapper.updateByPrimaryKeySelective(detail);
        }
    }

    private void normalizeAssembleCost(DepotHead depotHead, List<DepotItem> detailList) throws Exception {
        if (detailList == null || detailList.size() < 2) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ASSEMBLE_STRUCTURE_CODE,
                    ExceptionConstants.DEPOT_HEAD_ASSEMBLE_STRUCTURE_MSG);
        }
        DepotItem combinationItem = null;
        List<DepotItem> componentItems = new ArrayList<>();
        for (DepotItem detail : detailList) {
            if ("组合件".equals(detail.getMaterialType())) {
                if (combinationItem != null) {
                    throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ASSEMBLE_STRUCTURE_CODE,
                            ExceptionConstants.DEPOT_HEAD_ASSEMBLE_STRUCTURE_MSG);
                }
                combinationItem = detail;
            } else if ("普通子件".equals(detail.getMaterialType())) {
                componentItems.add(detail);
            } else {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ASSEMBLE_STRUCTURE_CODE,
                        ExceptionConstants.DEPOT_HEAD_ASSEMBLE_STRUCTURE_MSG);
            }
        }
        if (combinationItem == null || componentItems.isEmpty()
                || combinationItem.getBasicNumber() == null
                || combinationItem.getBasicNumber().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ASSEMBLE_STRUCTURE_CODE,
                    ExceptionConstants.DEPOT_HEAD_ASSEMBLE_STRUCTURE_MSG);
        }
        for (DepotItem component : componentItems) {
            if (Objects.equals(component.getMaterialExtendId(), combinationItem.getMaterialExtendId())) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ASSEMBLE_SAME_MATERIAL_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_ASSEMBLE_SAME_MATERIAL_MSG,
                                combinationItem.getMaterialExtendId()));
            }
        }
        BigDecimal totalComponentCost = BigDecimal.ZERO;
        Set<String> lockedCostKeys = new HashSet<>();
        for (DepotItem component : componentItems) {
            lockMaterialForStockCheck(component, lockedCostKeys);
            BigDecimal basicUnitCost = materialCurrentStockMapperEx.getCurrentUnitPriceByMId(component.getMaterialId());
            if (basicUnitCost == null) {
                MaterialExtend materialExtend = materialExtendService.getMaterialExtend(component.getMaterialExtendId());
                basicUnitCost = materialExtend == null || materialExtend.getPurchaseDecimal() == null
                        ? BigDecimal.ZERO : materialExtend.getPurchaseDecimal();
            }
            BigDecimal componentCost = component.getBasicNumber().multiply(basicUnitCost)
                    .setScale(2, BigDecimal.ROUND_HALF_UP);
            Unit componentUnit = materialService.findUnit(component.getMaterialId());
            BigDecimal submittedUnitCost = unitService.parseUnitPriceByUnit(basicUnitCost, componentUnit,
                    component.getMaterialUnit()).setScale(4, BigDecimal.ROUND_HALF_UP);
            component.setUnitPrice(submittedUnitCost);
            component.setPurchaseUnitPrice(submittedUnitCost);
            component.setAllPrice(componentCost);
            component.setTaxUnitPrice(submittedUnitCost);
            component.setTaxRate(BigDecimal.ZERO);
            component.setTaxMoney(BigDecimal.ZERO);
            component.setTaxLastMoney(componentCost);
            totalComponentCost = totalComponentCost.add(componentCost);
        }
        BigDecimal combinationBasicCost = totalComponentCost.divide(combinationItem.getBasicNumber(),
                4, BigDecimal.ROUND_HALF_UP);
        Unit combinationUnit = materialService.findUnit(combinationItem.getMaterialId());
        BigDecimal combinationSubmittedCost = unitService.parseUnitPriceByUnit(combinationBasicCost,
                combinationUnit, combinationItem.getMaterialUnit()).setScale(4, BigDecimal.ROUND_HALF_UP);
        combinationItem.setUnitPrice(combinationSubmittedCost);
        combinationItem.setPurchaseUnitPrice(combinationSubmittedCost);
        combinationItem.setAllPrice(totalComponentCost.setScale(2, BigDecimal.ROUND_HALF_UP));
        combinationItem.setTaxUnitPrice(combinationSubmittedCost);
        combinationItem.setTaxRate(BigDecimal.ZERO);
        combinationItem.setTaxMoney(BigDecimal.ZERO);
        combinationItem.setTaxLastMoney(combinationItem.getAllPrice());

        DepotHead costHead = new DepotHead();
        costHead.setId(depotHead.getId());
        costHead.setTotalPrice(combinationItem.getAllPrice());
        depotHeadMapper.updateByPrimaryKeySelective(costHead);
        depotHead.setTotalPrice(combinationItem.getAllPrice());
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void refreshDisassembleCost(DepotHead depotHead) throws Exception {
        if (depotHead == null || depotHead.getId() == null) {
            return;
        }
        List<DepotItem> detailList = getListByHeaderId(depotHead.getId());
        normalizeDisassembleCost(depotHead, detailList);
        for (DepotItem detail : detailList) {
            depotItemMapper.updateByPrimaryKeySelective(detail);
        }
    }

    /**
     * 拆卸以组合件当前移动平均成本作为总投入成本，并按各子件当前成本权重分摊。
     * 当所有子件成本权重均为零时按基本数量分摊，最后一行承接分币差，确保成本严格守恒。
     */
    private void normalizeDisassembleCost(DepotHead depotHead, List<DepotItem> detailList) throws Exception {
        if (detailList == null || detailList.size() < 2) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_STRUCTURE_CODE,
                    ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_STRUCTURE_MSG);
        }
        DepotItem combinationItem = null;
        List<DepotItem> componentItems = new ArrayList<>();
        for (int index = 0; index < detailList.size(); index++) {
            DepotItem detail = detailList.get(index);
            String expectedMaterialType = index == 0 ? "组合件" : "普通子件";
            if (!expectedMaterialType.equals(detail.getMaterialType())) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_STRUCTURE_CODE,
                        ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_STRUCTURE_MSG);
            }
            if ("组合件".equals(detail.getMaterialType())) {
                if (combinationItem != null) {
                    throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_STRUCTURE_CODE,
                            ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_STRUCTURE_MSG);
                }
                combinationItem = detail;
            } else {
                componentItems.add(detail);
            }
        }
        if (combinationItem == null || componentItems.isEmpty()
                || combinationItem.getBasicNumber() == null
                || combinationItem.getBasicNumber().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_STRUCTURE_CODE,
                    ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_STRUCTURE_MSG);
        }
        for (DepotItem component : componentItems) {
            if (component.getBasicNumber() == null || component.getBasicNumber().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_STRUCTURE_CODE,
                        ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_STRUCTURE_MSG);
            }
            if (Objects.equals(component.getMaterialExtendId(), combinationItem.getMaterialExtendId())) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_SAME_MATERIAL_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_SAME_MATERIAL_MSG,
                                combinationItem.getMaterialExtendId()));
            }
        }

        Set<String> lockedCostKeys = new HashSet<>();
        lockMaterialForStockCheck(combinationItem, lockedCostKeys);
        BigDecimal combinationBasicUnitCost = getCurrentBasicUnitCost(combinationItem);
        BigDecimal totalInputCost = combinationItem.getBasicNumber().multiply(combinationBasicUnitCost)
                .setScale(2, BigDecimal.ROUND_HALF_UP);
        setDisassembleItemCost(combinationItem, combinationBasicUnitCost, totalInputCost);

        List<BigDecimal> allocationWeights = new ArrayList<>();
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal totalQuantity = BigDecimal.ZERO;
        for (DepotItem component : componentItems) {
            lockMaterialForStockCheck(component, lockedCostKeys);
            BigDecimal weight = component.getBasicNumber().multiply(getCurrentBasicUnitCost(component));
            allocationWeights.add(weight);
            totalWeight = totalWeight.add(weight);
            totalQuantity = totalQuantity.add(component.getBasicNumber());
        }
        boolean allocateByCost = totalWeight.compareTo(BigDecimal.ZERO) > 0;
        BigDecimal denominator = allocateByCost ? totalWeight : totalQuantity;
        BigDecimal allocatedCost = BigDecimal.ZERO;
        for (int index = 0; index < componentItems.size(); index++) {
            DepotItem component = componentItems.get(index);
            BigDecimal componentCost;
            if (index == componentItems.size() - 1) {
                componentCost = totalInputCost.subtract(allocatedCost).setScale(2, BigDecimal.ROUND_HALF_UP);
            } else {
                BigDecimal weight = allocateByCost ? allocationWeights.get(index) : component.getBasicNumber();
                componentCost = denominator.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                        : totalInputCost.multiply(weight).divide(denominator, 2, BigDecimal.ROUND_HALF_UP);
                allocatedCost = allocatedCost.add(componentCost);
            }
            BigDecimal componentBasicUnitCost = componentCost.divide(component.getBasicNumber(),
                    4, BigDecimal.ROUND_HALF_UP);
            setDisassembleItemCost(component, componentBasicUnitCost, componentCost);
        }

        DepotHead costHead = new DepotHead();
        costHead.setId(depotHead.getId());
        costHead.setTotalPrice(totalInputCost);
        depotHeadMapper.updateByPrimaryKeySelective(costHead);
        depotHead.setTotalPrice(totalInputCost);
    }

    private BigDecimal getCurrentBasicUnitCost(DepotItem item) throws Exception {
        BigDecimal basicUnitCost = materialCurrentStockMapperEx.getCurrentUnitPriceByMId(item.getMaterialId());
        if (basicUnitCost == null) {
            MaterialExtend materialExtend = materialExtendService.getMaterialExtend(item.getMaterialExtendId());
            basicUnitCost = materialExtend == null || materialExtend.getPurchaseDecimal() == null
                    ? BigDecimal.ZERO : materialExtend.getPurchaseDecimal();
        }
        if (basicUnitCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_AMOUNT_CODE,
                    ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_AMOUNT_MSG);
        }
        return basicUnitCost;
    }

    private void setDisassembleItemCost(DepotItem item, BigDecimal basicUnitCost,
                                        BigDecimal allPrice) throws Exception {
        Unit unitInfo = materialService.findUnit(item.getMaterialId());
        BigDecimal submittedUnitCost = unitService.parseUnitPriceByUnit(basicUnitCost, unitInfo,
                item.getMaterialUnit()).setScale(4, BigDecimal.ROUND_HALF_UP);
        item.setUnitPrice(submittedUnitCost);
        item.setPurchaseUnitPrice(submittedUnitCost);
        item.setAllPrice(allPrice.setScale(2, BigDecimal.ROUND_HALF_UP));
        item.setTaxUnitPrice(submittedUnitCost);
        item.setTaxRate(BigDecimal.ZERO);
        item.setTaxMoney(BigDecimal.ZERO);
        item.setTaxLastMoney(item.getAllPrice());
    }

    public void checkAssembleMaterialStock(List<DepotHead> depotHeadList) throws Exception {
        Map<String, BigDecimal> outboundQuantityMap = new HashMap<>();
        Set<String> lockedStockKeys = new HashSet<>();
        for (DepotHead depotHead : depotHeadList) {
            checkAssembleMaterialStock(depotHead.getNumber(), getListByHeaderId(depotHead.getId()),
                    outboundQuantityMap, lockedStockKeys);
        }
    }

    public void checkDisassembleMaterialStock(List<DepotHead> depotHeadList) throws Exception {
        Map<String, BigDecimal> outboundQuantityMap = new HashMap<>();
        Set<String> lockedStockKeys = new HashSet<>();
        for (DepotHead depotHead : depotHeadList) {
            checkDisassembleMaterialStock(depotHead.getNumber(), getListByHeaderId(depotHead.getId()),
                    outboundQuantityMap, lockedStockKeys);
        }
    }

    private void checkDisassembleMaterialStock(String number, List<DepotItem> detailList) throws Exception {
        checkDisassembleMaterialStock(number, detailList, new HashMap<>(), new HashSet<>());
    }

    private void checkDisassembleMaterialStock(String number, List<DepotItem> detailList,
                                               Map<String, BigDecimal> outboundQuantityMap,
                                               Set<String> lockedStockKeys) throws Exception {
        List<DepotItem> combinationItems = new ArrayList<>();
        for (DepotItem detail : detailList) {
            if ("组合件".equals(detail.getMaterialType())) {
                combinationItems.add(detail);
            }
        }
        if (combinationItems.size() != 1) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_STRUCTURE_CODE,
                    ExceptionConstants.DEPOT_HEAD_DISASSEMBLE_STRUCTURE_MSG);
        }
        checkMaterialStock(number, combinationItems, outboundQuantityMap, lockedStockKeys);
    }

    private void checkAssembleMaterialStock(String number, List<DepotItem> detailList) throws Exception {
        checkAssembleMaterialStock(number, detailList, new HashMap<>(), new HashSet<>());
    }

    private void checkAssembleMaterialStock(String number, List<DepotItem> detailList,
                                            Map<String, BigDecimal> outboundQuantityMap,
                                            Set<String> lockedStockKeys) throws Exception {
        List<DepotItem> componentItems = new ArrayList<>();
        for (DepotItem detail : detailList) {
            if ("普通子件".equals(detail.getMaterialType())) {
                componentItems.add(detail);
            }
        }
        if (componentItems.isEmpty()) {
            throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_ASSEMBLE_STRUCTURE_CODE,
                    ExceptionConstants.DEPOT_HEAD_ASSEMBLE_STRUCTURE_MSG);
        }
        checkMaterialStock(number, componentItems, outboundQuantityMap, lockedStockKeys);
    }

    /**
     * 批量审核时在同一个库存口径内累计所有待审核出库数量，避免多张单据分别校验后造成负库存。
     */
    public void checkMaterialStock(List<DepotHead> depotHeadList) throws Exception {
        Map<String, BigDecimal> outboundQuantityMap = new HashMap<>();
        Set<String> lockedStockKeys = new HashSet<>();
        for (DepotHead depotHead : depotHeadList) {
            List<DepotItem> depotItemList = getListByHeaderId(depotHead.getId());
            checkMaterialStock(depotHead.getNumber(), depotItemList, outboundQuantityMap, lockedStockKeys);
        }
    }

    /** Lock every material in deterministic order before a multi-bill stock change. */
    public void lockMaterialsForStockChange(Collection<DepotHead> depotHeads) throws Exception {
        SortedSet<Long> materialIds = new TreeSet<>();
        for (DepotHead depotHead : depotHeads) {
            for (DepotItem depotItem : getListByHeaderId(depotHead.getId())) {
                if (depotItem.getMaterialId() != null) {
                    materialIds.add(depotItem.getMaterialId());
                }
            }
        }
        for (Long materialId : materialIds) {
            materialMapperEx.lockById(materialId);
        }
    }

    private void lockMaterialsByRows(JSONArray rowArr, Set<String> lockedStockKeys) throws Exception {
        SortedSet<Long> materialIds = new TreeSet<>();
        for (int i = 0; i < rowArr.size(); i++) {
            JSONObject row = JSONObject.parseObject(rowArr.getString(i));
            MaterialExtend extend = materialExtendService.getInfoByBarCode(row.getString("barCode"));
            if (extend != null && extend.getMaterialId() != null) {
                materialIds.add(extend.getMaterialId());
            }
        }
        for (Long materialId : materialIds) {
            materialMapperEx.lockById(materialId);
            lockedStockKeys.add(String.valueOf(materialId));
        }
    }

    public List<DepotItemVo4WithInfoEx> getSaleOutSummary(String materialParam, String beginTime, String endTime,
                                                          String[] creatorArray, Long organId, String[] organArray,
                                                          List<Long> categoryList, List<Long> depotList, Boolean forceFlag,
                                                          Integer offset, Integer rows) throws Exception {
        try {
            return depotItemMapperEx.getSaleOutSummary(materialParam, beginTime, endTime, creatorArray, organId,
                    organArray, categoryList, depotList, forceFlag, offset, rows);
        } catch (Exception e) {
            JshException.readFail(logger, e);
            return new ArrayList<>();
        }
    }

    private void checkMaterialStock(String number, List<DepotItem> depotItemList,
                                    Map<String, BigDecimal> outboundQuantityMap,
                                    Set<String> lockedStockKeys) throws Exception {
        for (DepotItem depotItem : depotItemList) {
            Material material = materialService.getMaterial(depotItem.getMaterialId());
            MaterialExtend materialExtend = materialExtendService.getMaterialExtend(depotItem.getMaterialExtendId());
            String stockMsg = material.getName() + "-" + materialExtend.getBarCode();
            BigDecimal stock = getCurrentStockByParam(depotItem.getDepotId(),depotItem.getMaterialId());
            if(StringUtil.isNotEmpty(depotItem.getSku())) {
                //对于sku商品要换个方式计算库存
                stock = getSkuStockByParam(depotItem.getDepotId(),depotItem.getMaterialExtendId(),null,null);
            }
            if(StringUtil.isNotEmpty(depotItem.getBatchNumber())) {
                //对于批次商品要换个方式计算库存
                stock = getOneBatchNumberStock(depotItem.getDepotId(), materialExtend.getBarCode(), depotItem.getBatchNumber());
                stockMsg += "-批号" + depotItem.getBatchNumber();
            }
            BigDecimal thisRealNumber = depotItem.getBasicNumber()==null?BigDecimal.ZERO:depotItem.getBasicNumber();
            if(StringUtil.isNotEmpty(depotItem.getBatchNumber())) {
                //对于批次商品，直接使用当前填写的数量
                thisRealNumber = depotItem.getOperNumber()==null?BigDecimal.ZERO:depotItem.getOperNumber();
            }
            if(thisRealNumber.compareTo(BigDecimal.ZERO)<=0) {
                throw new BusinessRunTimeException(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_CODE,
                        String.format(ExceptionConstants.DEPOT_HEAD_NUMBER_MUST_POSITIVE_MSG, materialExtend.getBarCode()));
            }
            lockMaterialForStockCheck(depotItem, lockedStockKeys);
            String stockScopeKey = getStockScopeKey(depotItem, materialExtend.getBarCode());
            BigDecimal accumulatedNumber = outboundQuantityMap.getOrDefault(stockScopeKey, BigDecimal.ZERO).add(thisRealNumber);
            outboundQuantityMap.put(stockScopeKey, accumulatedNumber);
            stock = getStockForDepotItem(depotItem, materialExtend.getBarCode());
            if(stock.compareTo(accumulatedNumber)<0){
                throw new BusinessRunTimeException(ExceptionConstants.BILL_MATERIAL_STOCK_NOT_ENOUGH_CODE,
                        String.format(ExceptionConstants.BILL_MATERIAL_STOCK_NOT_ENOUGH_MSG, number, stockMsg));
            }
        }
    }

    private boolean isMaterialUnitValid(String submittedUnit, MaterialExtend materialExtend, Unit unitInfo) {
        if (StringUtil.isEmpty(submittedUnit)) {
            return false;
        }
        if (unitInfo != null && StringUtil.isNotEmpty(unitInfo.getName())) {
            return submittedUnit.equals(unitInfo.getBasicUnit())
                    || submittedUnit.equals(unitInfo.getOtherUnit())
                    || submittedUnit.equals(unitInfo.getOtherUnitTwo())
                    || submittedUnit.equals(unitInfo.getOtherUnitThree());
        }
        return submittedUnit.equals(materialExtend.getCommodityUnit());
    }

    private void lockMaterialForStockCheck(DepotItem depotItem, Set<String> lockedStockKeys) {
        String lockKey = String.valueOf(depotItem.getMaterialId());
        if (lockedStockKeys.add(lockKey)) {
            // 始终锁定商品主记录，库存汇总行尚未创建时也有稳定的并发锁。
            materialMapperEx.lockById(depotItem.getMaterialId());
        }
    }

    private String getStockScopeKey(DepotItem depotItem, String barCode) {
        if (StringUtil.isNotEmpty(depotItem.getBatchNumber())) {
            return "batch:" + depotItem.getDepotId() + ":" + barCode + ":" + depotItem.getBatchNumber();
        }
        if (StringUtil.isNotEmpty(depotItem.getSku())) {
            return "sku:" + depotItem.getDepotId() + ":" + depotItem.getMaterialExtendId();
        }
        return "material:" + depotItem.getDepotId() + ":" + depotItem.getMaterialId();
    }

    private BigDecimal getStockForDepotItem(DepotItem depotItem, String barCode) throws Exception {
        BigDecimal stock = getCurrentStockByParam(depotItem.getDepotId(), depotItem.getMaterialId());
        if (StringUtil.isNotEmpty(depotItem.getSku())) {
            stock = getSkuStockByParam(depotItem.getDepotId(), depotItem.getMaterialExtendId(), null, null);
        }
        if (StringUtil.isNotEmpty(depotItem.getBatchNumber())) {
            stock = getOneBatchNumberStock(depotItem.getDepotId(), barCode, depotItem.getBatchNumber());
        }
        return stock;
    }
}
