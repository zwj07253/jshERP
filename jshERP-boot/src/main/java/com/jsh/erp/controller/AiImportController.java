package com.jsh.erp.controller;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.base.BaseController;
import com.jsh.erp.service.AiDocumentParserService;
import com.jsh.erp.service.AiModelConfigService;
import com.jsh.erp.service.AiImportTaskService;
import com.jsh.erp.service.DepotItemService;
import com.jsh.erp.service.MaterialService;
import com.jsh.erp.service.SupplierService;
import com.jsh.erp.service.UserService;
import com.jsh.erp.utils.BaseResponseInfo;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Shared AI recognition endpoint. It only returns validated preview rows; it never writes business data. */
@RestController
@RequestMapping("/ai/import")
public class AiImportController extends BaseController {
    @Resource private AiModelConfigService configService;
    @Resource private AiDocumentParserService parserService;
    @Resource private DepotItemService depotItemService;
    @Resource private UserService userService;
    @Resource private SupplierService supplierService;
    @Resource private MaterialService materialService;
    @Resource private AiImportTaskService taskService;

    @PostMapping("/parse")
    public BaseResponseInfo parse(@RequestParam("file") MultipartFile file, @RequestParam("type") String type,
                                  @RequestParam(value="prefixNo",required=false) String prefixNo) {
        BaseResponseInfo res=new BaseResponseInfo();
        try {
            userService.getCurrentUser();
            if (!supported(type)) throw new IllegalArgumentException("不支持的 AI 导入类型");
            JSONObject result=parserService.parse(file,type,configService.getRuntimeConfig());
            Map<String,Object> data=new LinkedHashMap<>();
            data.put("warnings",result.getJSONArray("warnings"));
            JSONArray rows = "BILL_ITEM".equals(type) ? previewBillItems(result.getJSONArray("rows"),prefixNo).getJSONArray("rows") : JSONArray.parseArray(JSON.toJSONString(previewMasterData(result.getJSONArray("rows"),type)));
            data.put("rows", rows);
            data.put("taskId", taskService.create(type, prefixNo, rows, result.getJSONArray("warnings")));
            res.code=200; res.data=data;
        } catch(Exception e) { logger.error("AI import parse failed",e); res.code=500; Map<String,Object> data=new HashMap<>(); data.put("message",e.getMessage()==null?"AI 识别失败":e.getMessage()); res.data=data; }
        return res;
    }

    @PostMapping("/confirm")
    public BaseResponseInfo confirm(@org.springframework.web.bind.annotation.RequestBody JSONObject input, HttpServletRequest request) {
        BaseResponseInfo res = new BaseResponseInfo();
        try {
            String type=input.getString("type"); JSONArray rows=input.getJSONArray("rows"); String prefixNo=input.getString("prefixNo"); if(prefixNo==null) prefixNo="";
            taskService.require(input.getString("taskId"), type, prefixNo);
            if (!supported(type) || rows==null || rows.isEmpty()) throw new IllegalArgumentException("没有可确认的导入数据");
            if(rows.size()>1000) throw new IllegalArgumentException("单次导入不能超过1000条");
            String taskId=input.getString("taskId");
            if(!taskService.lock(taskId)) throw new IllegalStateException("该 AI 导入任务正在处理或已完成，请勿重复提交");
            try {
            if ("BILL_ITEM".equals(type)) {
                JSONObject preview=previewBillItems(rows,prefixNo);
                Map<String,Object> data=new LinkedHashMap<>(); data.put("count",preview.getJSONArray("rows").size()); data.put("rows",preview.getJSONArray("rows")); res.code=200;res.data=data;return res;
            }
            List<Map<String,Object>> checked=previewMasterData(rows,type);
            for(Map<String,Object> row:checked) if(!Boolean.TRUE.equals(row.get("valid"))) throw new IllegalArgumentException("存在校验失败的数据，请修正后再确认导入");
            MultipartFile file=new BytesFile("ai-import.xls", buildXls(type,rows));
            if("MATERIAL".equals(type)) materialService.importExcel(file,request);
            else if("VENDOR".equals(type)) supplierService.importVendor(file,request);
            else if("CUSTOMER".equals(type)) supplierService.importCustomer(file,request);
            else supplierService.importMember(file,request);
            Map<String,Object> data=new LinkedHashMap<>(); data.put("count",rows.size()); data.put("message","导入成功"); res.code=200;res.data=data;
            } catch(Exception e) { taskService.unlock(taskId); throw e; }
        } catch(Exception e){logger.error("AI import confirm failed",e);res.code=500;Map<String,Object>d=new HashMap<>();d.put("message",e.getMessage());res.data=d;} return res;
    }

    private JSONObject previewBillItems(JSONArray source,String prefixNo) throws Exception {
        if (source==null||source.isEmpty()) throw new IllegalArgumentException("AI 未识别到任何明细行");
        if (!("QGD".equals(prefixNo)||"CGDD".equals(prefixNo)||"XSDD".equals(prefixNo)||"CGRK".equals(prefixNo)||"XSCK".equals(prefixNo)||"QTRK".equals(prefixNo)||"QTCK".equals(prefixNo))) throw new IllegalArgumentException("当前单据不支持 AI 导入");
        if(source.size()>1000) throw new IllegalArgumentException("明细不能超过1000条");
        List<String> barCodes=new ArrayList<>(); List<Map<String,String>> details=new ArrayList<>();
        for(Object item:source){ JSONObject row=item instanceof JSONObject?(JSONObject)item:JSONObject.parseObject(String.valueOf(item)); String code=trim(row.getString("barCode")); if(code.isEmpty()) throw new IllegalArgumentException("存在未识别到条码的明细行，请补全后重试"); Map<String,String> detail=new HashMap<>(); detail.put("barCode",code); detail.put("depotName",trim(row.getString("depotName"))); detail.put("num",number(row,"quantity")); detail.put("unitPrice",number(row,"unitPrice")); detail.put("taxRate",number(row,"taxRate")); detail.put("remark",trim(row.getString("remark"))); details.add(detail);barCodes.add(code); }
        JSONObject preview=depotItemService.parseMapByExcelData(barCodes,details,prefixNo);
        if(preview==null) throw new IllegalArgumentException("无法匹配导入的商品条码"); return preview;
    }

    private List<Map<String,Object>> previewMasterData(JSONArray source,String type){
        List<Map<String,Object>> rows=new ArrayList<>(); if(source==null)return rows;
        for(Object item:source){ JSONObject row=item instanceof JSONObject?(JSONObject)item:JSONObject.parseObject(String.valueOf(item)); Map<String,Object> copy=new LinkedHashMap<>(); for(String key:row.keySet()) copy.put(key,row.get(key)); List<String> errors=new ArrayList<>(); if("MATERIAL".equals(type)){if(trim(row.getString("name")).isEmpty())errors.add("商品名称不能为空");if(trim(row.getString("unit")).isEmpty())errors.add("基本单位不能为空");if(trim(row.getString("barCode")).isEmpty())errors.add("条码不能为空");}else if(trim(row.getString("supplier")).isEmpty())errors.add("名称不能为空");copy.put("errors",errors);copy.put("valid",errors.isEmpty());rows.add(copy);}
        return rows;
    }
    private byte[] buildXls(String type, JSONArray rows) throws Exception {
        ByteArrayOutputStream out=new ByteArrayOutputStream(); WritableWorkbook book=Workbook.createWorkbook(out); try { WritableSheet sheet=book.createSheet("导入数据",0); String[] keys=keys(type); for(int c=0;c<keys.length;c++)sheet.addCell(new Label(c,0,keys[c])); for(int r=0;r<rows.size();r++){JSONObject row=rows.getJSONObject(r);for(int c=0;c<keys.length;c++){String v=value(row,keys[c],type);sheet.addCell(new Label(c,r+2,v));}} book.write(); } finally {book.close();} return out.toByteArray();
    }
    private String[] keys(String type){if("MATERIAL".equals(type))return new String[]{"name","standard","model","color","brand","categoryName","weight","expiryNum","unit","manyUnit","barCode","manyBarCode","ratio","sku","purchaseDecimal","commodityDecimal","wholesaleDecimal","lowDecimal","enabled","enableSerialNumber","enableBatchNumber","position","mfrs","otherField1","otherField2","otherField3","remark"};if("MEMBER".equals(type))return new String[]{"supplier","contacts","telephone","phoneNum","email","description","sort","enabled"};return new String[]{"supplier","contacts","telephone","phoneNum","email","fax","beginNeed","taxNum","taxRate","bankName","accountNumber","address","description","sort","enabled"};}
    private String value(JSONObject row,String key,String type){Object v=row.get(key);if("beginNeed".equals(key)){v="VENDOR".equals(type)?row.get("beginNeedPay"):row.get("beginNeedGet");if(v==null)v=row.get("beginNeed");}return v==null?"":String.valueOf(v);}
    private static class BytesFile implements MultipartFile { private final String name;private final byte[] bytes;BytesFile(String name,byte[] bytes){this.name=name;this.bytes=bytes;}public String getName(){return "file";}public String getOriginalFilename(){return name;}public String getContentType(){return "application/vnd.ms-excel";}public boolean isEmpty(){return bytes.length==0;}public long getSize(){return bytes.length;}public byte[] getBytes(){return bytes.clone();}public InputStream getInputStream(){return new ByteArrayInputStream(bytes);}public void transferTo(java.io.File dest)throws IOException{java.nio.file.Files.write(dest.toPath(),bytes);} }
    private boolean supported(String t){return "BILL_ITEM".equals(t)||"MATERIAL".equals(t)||"VENDOR".equals(t)||"CUSTOMER".equals(t)||"MEMBER".equals(t);} private String trim(String v){return v==null?"":v.trim();} private String number(JSONObject row,String key){Object v=row.get(key);if(v==null&&"quantity".equals(key))v=row.get("operNumber");return v==null?"":String.valueOf(v).replace(",","").trim();}
}
