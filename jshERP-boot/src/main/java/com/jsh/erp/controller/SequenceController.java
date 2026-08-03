package com.jsh.erp.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.jsh.erp.service.SequenceService;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.utils.BaseResponseInfo;
import com.jsh.erp.utils.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @author YUEWEI 752*718*920
 */
@RestController
@RequestMapping(value = "/sequence")
@Tag(name = "单据编号")
public class SequenceController {
    private Logger logger = LoggerFactory.getLogger(SequenceController.class);

    @Resource
    private SequenceService sequenceService;

    /**
     * 单据编号生成接口
     * @param request
     * @return
     */
    @GetMapping(value = "/buildNumber")
    @Operation(summary = "单据编号生成接口")
    public BaseResponseInfo buildNumber(@RequestParam("prefixNo") String prefixNo,
                                        HttpServletRequest request)throws Exception {
        BaseResponseInfo res = new BaseResponseInfo();
        Map<String, Object> map = new HashMap<String, Object>();
        try {
            Long tenantId = Tools.getTenantIdByToken(request.getHeader("X-Access-Token"));
            String number = sequenceService.buildNumber(prefixNo, tenantId);
            map.put("defaultNumber", number);
            res.code = 200;
            res.data = map;
        } catch(BusinessRunTimeException e) {
            res.code = e.getCode();
            res.data = e.getData().get("message");
        } catch(Exception e){
            logger.error(e.getMessage(), e);
            res.code = 500;
            res.data = "获取数据失败";
        }
        return res;
    }

}
