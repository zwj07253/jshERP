package com.jsh.erp.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import jxl.Sheet;
import jxl.Workbook;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;

/** 调用 OpenAI 兼容的 Chat Completions 接口，将库存文件转换为严格 JSON。 */
@Service
public class AiInventoryParserService {

    private static final int MAX_EXTRACTED_TEXT = 120_000;

    public JSONObject parse(MultipartFile file, AiModelConfigService.Config config) throws Exception {
        validateFile(file, config);
        String extension = extension(file.getOriginalFilename());
        JSONObject request = baseRequest(config);
        JSONArray messages = new JSONArray();
        messages.add(message("system", systemPrompt(config.customPrompt)));

        if (isImage(extension)) {
            if (!config.visionEnabled) {
                throw new IllegalArgumentException("当前 AI 配置未启用图片识别，请联系系统管理员开启视觉模型");
            }
            JSONArray content = new JSONArray();
            JSONObject text = new JSONObject();
            text.put("type", "text");
            text.put("text", "请解析这张外贸库存单据图片，并按系统要求返回库存数据。");
            content.add(text);
            JSONObject imageUrl = new JSONObject();
            imageUrl.put("url", "data:" + imageMime(extension) + ";base64," + Base64.getEncoder().encodeToString(file.getBytes()));
            JSONObject image = new JSONObject();
            image.put("type", "image_url");
            image.put("image_url", imageUrl);
            content.add(image);
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", content);
            messages.add(userMessage);
        } else {
            String extracted = extractText(file, extension);
            if (isBlank(extracted)) {
                throw new IllegalArgumentException("文件中没有提取到可识别文字；扫描版 PDF 请先转为图片后上传");
            }
            messages.add(message("user", "下面是文件内容，请解析：\n\n" + limit(extracted)));
        }
        request.put("messages", messages);
        request.put("temperature", 0);
        String response = execute(config, request);
        return parseModelJson(response);
    }

    public String testConnection(AiModelConfigService.Config config) throws Exception {
        JSONObject request = baseRequest(config);
        JSONArray messages = new JSONArray();
        messages.add(message("user", "只回复 OK"));
        request.put("messages", messages);
        request.put("temperature", 0);
        String content = modelContent(execute(config, request));
        return content.length() > 120 ? content.substring(0, 120) : content;
    }

    private JSONObject baseRequest(AiModelConfigService.Config config) {
        JSONObject request = new JSONObject();
        request.put("model", config.modelName);
        return request;
    }

    private String execute(AiModelConfigService.Config config, JSONObject body) throws Exception {
        int timeout = Math.max(15, config.timeoutSeconds) * 1000;
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Math.min(timeout, 30_000))
                .setConnectionRequestTimeout(Math.min(timeout, 30_000))
                .setSocketTimeout(timeout)
                .build();
        try (CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build()) {
            HttpPost post = new HttpPost(config.apiUrl);
            post.setHeader("Authorization", "Bearer " + config.apiToken);
            post.setHeader("Content-Type", "application/json; charset=UTF-8");
            post.setEntity(new StringEntity(body.toJSONString(), StandardCharsets.UTF_8));
            try (CloseableHttpResponse response = client.execute(post)) {
                int status = response.getStatusLine().getStatusCode();
                String payload = response.getEntity() == null ? "" : EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                if (status < 200 || status >= 300) {
                    throw new IllegalStateException("AI 服务调用失败（HTTP " + status + "）：" + safeMessage(payload));
                }
                return payload;
            }
        }
    }

    private JSONObject parseModelJson(String responsePayload) {
        String content = modelContent(responsePayload).trim();
        if (content.startsWith("```")) {
            content = content.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        }
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalStateException("AI 未返回有效 JSON，请检查模型是否支持指令解析");
        }
        try {
            JSONObject result = JSON.parseObject(content.substring(start, end + 1));
            if (result.getJSONArray("rows") == null) {
                throw new IllegalStateException("AI 返回结果缺少 rows 数组");
            }
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("AI 返回的 JSON 格式无法解析：" + e.getMessage(), e);
        }
    }

    private String modelContent(String responsePayload) {
        try {
            JSONObject response = JSON.parseObject(responsePayload);
            JSONArray choices = response.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw new IllegalStateException("AI 响应中没有 choices 数据");
            }
            JSONObject message = choices.getJSONObject(0).getJSONObject("message");
            String content = message == null ? null : message.getString("content");
            if (isBlank(content)) throw new IllegalStateException("AI 响应内容为空");
            return content;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("AI 服务响应格式不兼容 OpenAI Chat Completions：" + e.getMessage(), e);
        }
    }

    private String extractText(MultipartFile file, String extension) throws Exception {
        switch (extension) {
            case "xls": return extractXls(file);
            case "xlsx": return extractXlsx(file);
            case "csv":
            case "txt": return new String(file.getBytes(), StandardCharsets.UTF_8);
            case "pdf": return extractPdf(file);
            default: throw new IllegalArgumentException("不支持该文件格式，请上传 xls、xlsx、csv、txt、pdf、png、jpg 或 jpeg 文件");
        }
    }

    private String extractXls(MultipartFile file) throws Exception {
        StringBuilder result = new StringBuilder();
        Workbook workbook = Workbook.getWorkbook(new ByteArrayInputStream(file.getBytes()));
        try {
            for (Sheet sheet : workbook.getSheets()) {
                result.append("工作表：").append(sheet.getName()).append('\n');
                for (int row = 0; row < sheet.getRows(); row++) {
                    for (int col = 0; col < sheet.getColumns(); col++) {
                        if (col > 0) result.append('\t');
                        result.append(sheet.getCell(col, row).getContents());
                    }
                    result.append('\n');
                    if (result.length() > MAX_EXTRACTED_TEXT) return limit(result.toString());
                }
            }
        } finally {
            workbook.close();
        }
        return result.toString();
    }

    private String extractXlsx(MultipartFile file) throws Exception {
        StringBuilder result = new StringBuilder();
        DataFormatter formatter = new DataFormatter(Locale.CHINA);
        try (org.apache.poi.ss.usermodel.Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            for (org.apache.poi.ss.usermodel.Sheet sheet : workbook) {
                result.append("工作表：").append(sheet.getSheetName()).append('\n');
                for (Row row : sheet) {
                    short lastCell = row.getLastCellNum();
                    for (int col = 0; col < Math.max(0, lastCell); col++) {
                        if (col > 0) result.append('\t');
                        if (row.getCell(col) != null) result.append(formatter.formatCellValue(row.getCell(col)));
                    }
                    result.append('\n');
                    if (result.length() > MAX_EXTRACTED_TEXT) return limit(result.toString());
                }
            }
        }
        return result.toString();
    }

    private String extractPdf(MultipartFile file) throws Exception {
        PdfReader reader = new PdfReader(file.getBytes());
        StringBuilder result = new StringBuilder();
        try {
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                result.append("第 ").append(page).append(" 页\n");
                result.append(PdfTextExtractor.getTextFromPage(reader, page)).append('\n');
                if (result.length() > MAX_EXTRACTED_TEXT) return limit(result.toString());
            }
        } finally {
            reader.close();
        }
        return result.toString();
    }

    private String systemPrompt(String customPrompt) {
        return "你是 ERP 外贸库存单据解析器。只返回一个 JSON 对象，不要 Markdown，不要解释。" +
                "返回格式必须是 {\"rows\":[...],\"warnings\":[]}。每一行字段固定为：" +
                "shipmentNo（发运批次号）、purchaseNumber（采购单号）、barCode（商品条码/SKU条码）、" +
                "quantity（发运数量）、inTransitQuantity（在途数量）、clearedQuantity（清关数量）、" +
                "stockedQuantity（已入库数量）、lockedQuantity（客户锁定数量）、salesAmount（销售金额）、" +
                "confidence（0到1）。不要编造缺失值，缺失字段返回 null；数量和金额必须为数字，不能带单位或千分位。" +
                (isBlank(customPrompt) ? "" : "\n管理员补充规则：" + customPrompt);
    }

    private JSONObject message(String role, String content) {
        JSONObject message = new JSONObject();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private void validateFile(MultipartFile file, AiModelConfigService.Config config) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("请选择要解析的文件");
        long maxBytes = Math.max(1, config.maxFileMb) * 1024L * 1024L;
        if (file.getSize() > maxBytes) throw new IllegalArgumentException("文件不能超过 " + config.maxFileMb + "MB");
        String extension = extension(file.getOriginalFilename());
        if (!("xls".equals(extension) || "xlsx".equals(extension) || "csv".equals(extension) || "txt".equals(extension)
                || "pdf".equals(extension) || isImage(extension))) {
            throw new IllegalArgumentException("不支持该文件格式，请上传 xls、xlsx、csv、txt、pdf、png、jpg 或 jpeg 文件");
        }
    }

    private String safeMessage(String payload) {
        String value = payload == null ? "" : payload.replaceAll("[\\r\\n]+", " ").trim();
        return value.length() > 500 ? value.substring(0, 500) : value;
    }

    private String limit(String value) {
        return value.length() > MAX_EXTRACTED_TEXT ? value.substring(0, MAX_EXTRACTED_TEXT) : value;
    }

    private String extension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private boolean isImage(String extension) {
        return "png".equals(extension) || "jpg".equals(extension) || "jpeg".equals(extension);
    }

    private String imageMime(String extension) {
        return "png".equals(extension) ? "image/png" : "image/jpeg";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
