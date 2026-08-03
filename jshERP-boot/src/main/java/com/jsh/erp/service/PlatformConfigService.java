package com.jsh.erp.service;

import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.PlatformConfig;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.datasource.entities.PlatformConfigExample;
import com.jsh.erp.datasource.mappers.PlatformConfigMapper;
import com.jsh.erp.datasource.mappers.PlatformConfigMapperEx;
import com.jsh.erp.exception.JshException;
import com.jsh.erp.utils.HttpClient;
import com.jsh.erp.utils.PageUtils;
import com.jsh.erp.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.mail.*;
import javax.mail.internet.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

@Service
public class PlatformConfigService {
    private Logger logger = LoggerFactory.getLogger(PlatformConfigService.class);

    /** 公开可查询的平台配置 key（无需 admin 权限） */
    private static final Set<String> PUBLIC_PLATFORM_KEYS = new HashSet<>(Arrays.asList(
            "platform_name", "platform_url", "register_flag", "checkcode_flag",
            "bill_print_flag", "bill_print_url", "pay_fee_url", "send_workflow_url",
            "bill_excel_url"
    ));

    /** 敏感配置 key，API 响应中必须脱敏 */
    private static final Set<String> SENSITIVE_PLATFORM_KEYS = new HashSet<>(Arrays.asList(
            "activation_code", "app_activation_code", "email_auth_code",
            "aliOss_accessKeySecret", "weixinSecret"
    ));

    /** 可通过 updatePlatformConfigByKey 修改的 key */
    private static final Set<String> MUTABLE_PLATFORM_KEYS = new HashSet<>(Arrays.asList(
            "platform_name", "platform_url", "register_flag", "checkcode_flag",
            "bill_print_flag", "bill_print_url", "pay_fee_url", "send_workflow_url",
            "bill_excel_url", "email_from", "email_auth_code", "email_smtp_host",
            "aliOss_endpoint", "aliOss_accessKeyId", "aliOss_accessKeySecret",
            "aliOss_bucketName", "aliOss_linkUrl", "weixinUrl", "weixinAppid", "weixinSecret"
    ));

    @Resource
    private UserService userService;

    @Resource
    private RedisService redisService;

    @Resource
    private PlatformConfigMapper platformConfigMapper;

    @Resource
    private PlatformConfigMapperEx platformConfigMapperEx;

    public PlatformConfig getPlatformConfig(long id)throws Exception {
        PlatformConfig result=null;
        try{
            if(userService.isPlatformSuperAdmin(userService.getCurrentUser())) {
                result = platformConfigMapper.selectByPrimaryKey(id);
                if(result != null && SENSITIVE_PLATFORM_KEYS.contains(result.getPlatformKey())) {
                    result.setPlatformValue("******");
                }
            }
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    public List<PlatformConfig> getPlatformConfig()throws Exception {
        PlatformConfigExample example = new PlatformConfigExample();
        example.createCriteria();
        List<PlatformConfig> list=Collections.emptyList();
        try{
            if(userService.isPlatformSuperAdmin(userService.getCurrentUser())) {
                list = platformConfigMapper.selectByExample(example);
            }
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<PlatformConfig> select(String platformKey)throws Exception {
        List<PlatformConfig> list=Collections.emptyList();
        try{
            if(userService.isPlatformSuperAdmin(userService.getCurrentUser())) {
                PageUtils.startPage();
                list = platformConfigMapperEx.selectByConditionPlatformConfig(platformKey);
                // 对敏感key的值进行脱敏
                for(PlatformConfig config : list) {
                    if(SENSITIVE_PLATFORM_KEYS.contains(config.getPlatformKey())) {
                        config.setPlatformValue("******");
                    }
                }
            }
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int insertPlatformConfig(JSONObject obj, HttpServletRequest request) throws Exception{
        PlatformConfig platformConfig = JSONObject.parseObject(obj.toJSONString(), PlatformConfig.class);
        int result=0;
        try{
            if(userService.isPlatformSuperAdmin(userService.getCurrentUser())) {
                result = platformConfigMapper.insertSelective(platformConfig);
            }
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int updatePlatformConfig(JSONObject obj, HttpServletRequest request) throws Exception{
        PlatformConfig platformConfig = JSONObject.parseObject(obj.toJSONString(), PlatformConfig.class);
        int result=0;
        try{
            if(userService.isPlatformSuperAdmin(userService.getCurrentUser())) {
                // 只允许修改 platformValue，禁止修改 platformKey 等字段
                PlatformConfig update = new PlatformConfig();
                update.setId(platformConfig.getId());
                // 敏感字段如果提交的是脱敏值则忽略，不覆盖真实值
                if(platformConfig.getId() != null) {
                    PlatformConfig existing = platformConfigMapper.selectByPrimaryKey(platformConfig.getId());
                    if(existing != null && SENSITIVE_PLATFORM_KEYS.contains(existing.getPlatformKey())
                            && "******".equals(platformConfig.getPlatformValue())) {
                        return 0;
                    }
                    // 校验值合法性
                    if(platformConfig.getPlatformValue() != null) {
                        validatePlatformValue(platformConfig);
                    }
                }
                update.setPlatformValue(platformConfig.getPlatformValue());
                result = platformConfigMapper.updateByPrimaryKeySelective(update);
            }
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    /**
     * 校验平台配置值的合法性
     */
    private void validatePlatformValue(PlatformConfig config) throws Exception {
        String key = config.getPlatformKey();
        String value = config.getPlatformValue();
        if(key == null || value == null) {
            // 通过id更新时，需要查询原始key
            if(config.getId() != null) {
                PlatformConfig existing = platformConfigMapper.selectByPrimaryKey(config.getId());
                if(existing != null) {
                    key = existing.getPlatformKey();
                }
            }
        }
        if(key == null) {
            return;
        }
        // URL 类型的 key 必须校验协议
        if(key.endsWith("_url") || key.endsWith("Url")) {
            if(StringUtil.isNotEmpty(value) && !value.startsWith("http://") && !value.startsWith("https://")) {
                throw new BusinessRunTimeException(ExceptionConstants.PLATFORM_CONFIG_URL_INVALID_CODE,
                        ExceptionConstants.PLATFORM_CONFIG_URL_INVALID_MSG);
            }
        }
        // 开关类型只能是 0 或 1
        if(key.endsWith("_flag")) {
            if(StringUtil.isNotEmpty(value) && !"0".equals(value) && !"1".equals(value)) {
                throw new BusinessRunTimeException(ExceptionConstants.PLATFORM_CONFIG_FLAG_INVALID_CODE,
                        ExceptionConstants.PLATFORM_CONFIG_FLAG_INVALID_MSG);
            }
        }
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int deletePlatformConfig(Long id, HttpServletRequest request)throws Exception {
        int result=0;
        try{
            if(userService.isPlatformSuperAdmin(userService.getCurrentUser())) {
                result = platformConfigMapper.deleteByPrimaryKey(id);
            }
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeletePlatformConfig(String ids, HttpServletRequest request)throws Exception {
        List<Long> idList = StringUtil.strToLongList(ids);
        PlatformConfigExample example = new PlatformConfigExample();
        example.createCriteria().andIdIn(idList);
        int result=0;
        try{
            if(userService.isPlatformSuperAdmin(userService.getCurrentUser())) {
                result = platformConfigMapper.deleteByExample(example);
            }
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    public int updatePlatformConfigByKey(String platformKey, String platformValue)throws Exception {
        int result=0;
        try{
            if(userService.isPlatformSuperAdmin(userService.getCurrentUser())) {
                if(!MUTABLE_PLATFORM_KEYS.contains(platformKey)) {
                    logger.warn("拒绝更新非允许的平台配置key: {}", platformKey);
                    return 0;
                }
                // 校验 URL 和开关值
                PlatformConfig validateConfig = new PlatformConfig();
                validateConfig.setPlatformKey(platformKey);
                validateConfig.setPlatformValue(platformValue);
                validatePlatformValue(validateConfig);
                PlatformConfig platformConfig = new PlatformConfig();
                platformConfig.setPlatformValue(platformValue);
                PlatformConfigExample example = new PlatformConfigExample();
                example.createCriteria().andPlatformKeyEqualTo(platformKey);
                result = platformConfigMapper.updateByExampleSelective(platformConfig, example);
            }
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    public PlatformConfig getInfoByKey(String platformKey)throws Exception {
        PlatformConfig platformConfig = new PlatformConfig();
        try{
            if(!PUBLIC_PLATFORM_KEYS.contains(platformKey)) {
                platformConfig = null;
            } else {
                PlatformConfigExample example = new PlatformConfigExample();
                example.createCriteria().andPlatformKeyEqualTo(platformKey);
                List<PlatformConfig> list=platformConfigMapper.selectByExample(example);
                if(list!=null && list.size()>0){
                    platformConfig = list.get(0);
                }
            }
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return platformConfig;
    }

    /**
     * 根据key查询平台信息-内部专用方法
     * @param platformKey
     * @return
     * @throws Exception
     */
    public PlatformConfig getPlatformConfigByKey(String platformKey)throws Exception {
        PlatformConfig platformConfig = new PlatformConfig();
        try{
            PlatformConfigExample example = new PlatformConfigExample();
            example.createCriteria().andPlatformKeyEqualTo(platformKey);
            List<PlatformConfig> list=platformConfigMapper.selectByExample(example);
            if(list!=null && list.size()>0){
                platformConfig = list.get(0);
            }
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return platformConfig;
    }

    /**
     * 获取微信token信息（带分布式锁防止并发刷新）
     * @return
     * @throws Exception
     */
    public String getAccessToken() throws Exception {
        String accessToken = redisService.getCacheObject("weixinToken");
        if(StringUtil.isNotEmpty(accessToken)) {
            return accessToken;
        }
        // 缓存未命中，使用分布式锁防止并发刷新
        String lockKey = "weixinToken_lock";
        boolean locked = false;
        try {
            locked = redisService.setIfAbsent(lockKey, "1", 10, java.util.concurrent.TimeUnit.SECONDS);
            if(locked) {
                // 获取锁成功，再次检查缓存（double-check）
                accessToken = redisService.getCacheObject("weixinToken");
                if(StringUtil.isNotEmpty(accessToken)) {
                    return accessToken;
                }
                // 获取token
                String weixinUrl = getPlatformConfigByKey("weixinUrl").getPlatformValue();
                String weixinAppid = getPlatformConfigByKey("weixinAppid").getPlatformValue();
                String weixinSecret = getPlatformConfigByKey("weixinSecret").getPlatformValue();
                String url = weixinUrl + BusinessConstants.WEIXIN_TOKEN
                        + "?grant_type=client_credential&appid=" + weixinAppid + "&secret=" + weixinSecret;
                JSONObject jsonObject = HttpClient.httpGet(url);
                if (jsonObject != null) {
                    accessToken = jsonObject.getString("access_token");
                    Long expiresIn = jsonObject.getLong("expires_in");
                    if (StringUtil.isNotEmpty(accessToken) && expiresIn != null && expiresIn > 0) {
                        // 提前失效，避免边界时间使用已过期token
                        long cacheTtl = expiresIn - 200;
                        redisService.storageKeyWithTime("weixinToken", accessToken, cacheTtl);
                    } else {
                        logger.error("微信token获取失败: 返回数据中缺少access_token或expires_in");
                    }
                } else {
                    logger.error("微信token获取失败: 接口返回null");
                }
            } else {
                // 未获取到锁，等待后重试读取缓存
                Thread.sleep(200);
                accessToken = redisService.getCacheObject("weixinToken");
                if(StringUtil.isEmpty(accessToken)) {
                    logger.warn("微信token获取等待超时，缓存仍未就绪");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("微信token获取被中断");
        } finally {
            if(locked) {
                redisService.deleteObject(lockKey);
            }
        }
        return accessToken != null ? accessToken : "";
    }

    /**
     * 发送邮件(该方法将在一个单独的线程中执行)
     * @return
     */
    @Async
    public void sendEmail(String emailFrom, String emailAuthCode, String emailSmtpHost, String toEmail, String emailSubject, String emailBody) {
        // 配置邮件服务器属性
        Properties properties = new Properties();
        properties.put("mail.smtp.host", emailSmtpHost);
        properties.put("mail.smtp.port", "465");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        properties.put("mail.smtp.socketFactory.port", "465");
        try {
            Session session = Session.getInstance(properties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(emailFrom, emailAuthCode);
                }
            });
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(emailFrom));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(emailSubject);
            message.setText(emailBody);
            Transport.send(message);
            logger.info("邮件发送成功，收件人: {}", toEmail);
        } catch (Exception e) {
            // 不记录邮件密码等敏感信息
            logger.error("邮件发送失败，收件人: {}，错误: {}", toEmail, e.getMessage());
        }
    }
}
