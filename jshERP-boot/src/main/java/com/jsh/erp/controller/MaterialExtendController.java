package com.jsh.erp.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.datasource.entities.MaterialExtend;
import com.jsh.erp.datasource.vo.MaterialExtendVo4List;
import com.jsh.erp.service.MaterialExtendService;
import com.jsh.erp.service.RoleService;
import com.jsh.erp.service.UserService;
import com.jsh.erp.utils.BaseResponseInfo;
import com.jsh.erp.utils.ErpInfo;
import com.jsh.erp.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jsh.erp.utils.ResponseJsonUtil.returnJson;
import static com.jsh.erp.utils.ResponseJsonUtil.returnStr;

/**
 * @author jijiaqing
 */
@RestController
@RequestMapping(value = "/materialsExtend")
@Tag(name = "商品价格扩展")
public class MaterialExtendController {
    private Logger logger = LoggerFactory.getLogger(MaterialExtendController.class);
    @Resource
    private MaterialExtendService materialExtendService;
    @Resource
    private UserService userService;
    @Resource
    private RoleService roleService;

    @GetMapping(value = "/info")
    @Operation(summary = "根据id获取信息")
    public String getList(@RequestParam("id") Long id,
                          HttpServletRequest request) throws Exception {
        MaterialExtend materialExtend = materialExtendService.getMaterialExtend(id);
        maskPrices(materialExtend, request);
        Map<String, Object> objectMap = new HashMap<>();
        if(materialExtend != null) {
            objectMap.put("info", materialExtend);
            return returnJson(objectMap, ErpInfo.OK.name, ErpInfo.OK.code);
        } else {
            return returnJson(objectMap, ErpInfo.ERROR.name, ErpInfo.ERROR.code);
        }
    }

    @PostMapping(value = "/add")
    @Operation(summary = "新增")
    public String addResource(@RequestBody JSONObject obj, HttpServletRequest request)throws Exception {
        Map<String, Object> objectMap = new HashMap<>();
        int insert = materialExtendService.insertMaterialExtend(obj, request);
        return returnStr(objectMap, insert);
    }

    @PutMapping(value = "/update")
    @Operation(summary = "修改")
    public String updateResource(@RequestBody JSONObject obj, HttpServletRequest request)throws Exception {
        Map<String, Object> objectMap = new HashMap<>();
        int update = materialExtendService.updateMaterialExtend(obj, request);
        return returnStr(objectMap, update);
    }

    @DeleteMapping(value = "/delete")
    @Operation(summary = "删除")
    public String deleteResource(@RequestParam("id") Long id, HttpServletRequest request)throws Exception {
        Map<String, Object> objectMap = new HashMap<>();
        int delete = materialExtendService.deleteMaterialExtend(id, request);
        return returnStr(objectMap, delete);
    }

    @DeleteMapping(value = "/deleteBatch")
    @Operation(summary = "批量删除")
    public String batchDeleteResource(@RequestParam("ids") String ids, HttpServletRequest request)throws Exception {
        Map<String, Object> objectMap = new HashMap<>();
        int delete = materialExtendService.batchDeleteMaterialExtendByIds(ids, request);
        return returnStr(objectMap, delete);
    }

    @GetMapping(value = "/getDetailList")
    @Operation(summary = "价格信息列表")
    public BaseResponseInfo getDetailList(@RequestParam("materialId") Long materialId,
                                          HttpServletRequest request)throws Exception {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            List<MaterialExtendVo4List> dataList = new ArrayList<MaterialExtendVo4List>();
            Long userId = userService.getUserId(request);
            String priceLimit = userService.getRoleTypeByUserId(userId).getPriceLimit();
            if(materialId!=0) {
                dataList = materialExtendService.getDetailList(materialId);
            }
            JSONObject outer = new JSONObject();
            outer.put("total", dataList.size());
            //存放数据json数组
            JSONArray dataArray = new JSONArray();
            if (null != dataList) {
                for (MaterialExtendVo4List md : dataList) {
                    JSONObject item = new JSONObject();
                    item.put("id", md.getId());
                    item.put("barCode", md.getBarCode());
                    item.put("commodityUnit", md.getCommodityUnit());
                    if(StringUtil.isNotEmpty(md.getSku())){
                        item.put("sku", md.getSku());
                    }
                    item.put("purchaseDecimal", roleService.parseBillPriceByLimit(md.getPurchaseDecimal(), "buy", priceLimit, request));
                    item.put("commodityDecimal", roleService.parseBillPriceByLimit(md.getCommodityDecimal(), "retail", priceLimit, request));
                    item.put("wholesaleDecimal", roleService.parseBillPriceByLimit(md.getWholesaleDecimal(), "sale", priceLimit, request));
                    item.put("lowDecimal", roleService.parseBillPriceByLimit(md.getLowDecimal(), "sale", priceLimit, request));
                    dataArray.add(item);
                }
            }
            outer.put("rows", dataArray);
            res.code = 200;
            res.data = outer;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = "获取数据失败";
        }
        return res;
    }

    /**
     * 根据条码查询商品信息
     * @param barCode
     * @param request
     * @return
     * @throws Exception
     */
    @GetMapping(value = "/getInfoByBarCode")
    @Operation(summary = "根据条码查询商品信息")
    public BaseResponseInfo getInfoByBarCode(@RequestParam("barCode") String barCode,
                                          HttpServletRequest request)throws Exception {
        BaseResponseInfo res = new BaseResponseInfo();
        Map<String, Object> map = new HashMap<String, Object>();
        try {
            MaterialExtend materialExtend = materialExtendService.getInfoByBarCode(barCode);
            maskPrices(materialExtend, request);
            res.code = 200;
            res.data = materialExtend;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = "获取数据失败";
        }
        return res;
    }

    private void maskPrices(MaterialExtend materialExtend, HttpServletRequest request) throws Exception {
        if (materialExtend == null) {
            return;
        }
        Long userId = userService.getUserId(request);
        String priceLimit = userService.getRoleTypeByUserId(userId).getPriceLimit();
        materialExtend.setPurchaseDecimal(roleService.parseBillPriceByLimit(materialExtend.getPurchaseDecimal(), "buy", priceLimit, request));
        materialExtend.setCommodityDecimal(roleService.parseBillPriceByLimit(materialExtend.getCommodityDecimal(), "retail", priceLimit, request));
        materialExtend.setWholesaleDecimal(roleService.parseBillPriceByLimit(materialExtend.getWholesaleDecimal(), "sale", priceLimit, request));
        materialExtend.setLowDecimal(roleService.parseBillPriceByLimit(materialExtend.getLowDecimal(), "sale", priceLimit, request));
    }

    /**
     * 校验条码是否存在
     * @param id
     * @param barCode
     * @param request
     * @return
     * @throws Exception
     */
    @GetMapping(value = "/checkIsBarCodeExist")
    @Operation(summary = "校验条码是否存在")
    public BaseResponseInfo checkIsBarCodeExist(@RequestParam("id") Long id,
                                                @RequestParam("barCode") String barCode,
                                             HttpServletRequest request)throws Exception {
        BaseResponseInfo res = new BaseResponseInfo();
        Map<String, Object> map = new HashMap<>();
        try {
            int exist = materialExtendService.checkIsBarCodeExist(id, barCode);
            if(exist > 0) {
                map.put("status", true);
            } else {
                map.put("status", false);
            }
            res.code = 200;
            res.data = map;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = "获取数据失败";
        }
        return res;
    }
}
