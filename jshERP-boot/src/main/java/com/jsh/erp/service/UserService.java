package com.jsh.erp.service;

import com.jsh.erp.datasource.entities.*;
import com.jsh.erp.datasource.mappers.TenantMapper;
import com.jsh.erp.datasource.mappers.OrgaUserRelMapper;
import com.jsh.erp.datasource.mappers.UserBusinessMapper;
import com.jsh.erp.exception.BusinessParamCheckingException;
import com.jsh.erp.utils.*;
import org.springframework.util.StringUtils;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.mappers.UserMapper;
import com.jsh.erp.datasource.mappers.UserMapperEx;
import com.jsh.erp.datasource.vo.TreeNodeEx;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.exception.JshException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Service
public class UserService {
    private Logger logger = LoggerFactory.getLogger(UserService.class);

    @Resource
    private UserMapper userMapper;
    @Resource
    private TenantMapper tenantMapper;
    @Resource
    private UserMapperEx userMapperEx;
    @Resource
    private OrgaUserRelService orgaUserRelService;
    @Resource
    private OrgaUserRelMapper orgaUserRelMapper;
    @Resource
    private UserBusinessMapper userBusinessMapper;
    @Resource
    private LogService logService;
    @Resource
    private UserService userService;
    @Resource
    private TenantService tenantService;
    @Resource
    private UserBusinessService userBusinessService;
    @Resource
    private RoleService roleService;
    @Resource
    private FunctionService functionService;
    @Resource
    private OrganizationService organizationService;
    @Resource
    private PlatformConfigService platformConfigService;
    @Resource
    private RedisService redisService;
    @Resource
    private UserPasswordService userPasswordService;

    private static final String USER_URL = "/system/user";
    private static final String EDIT_BUTTON_CODE = "1";

    @Value("${tenant.userNumLimit}")
    private Integer userNumLimit;

    @Value("${tenant.tryDayLimit}")
    private Integer tryDayLimit;

    public User getUser(long id)throws Exception {
        User result=null;
        try{
            //先校验是否登录，然后才能查询用户数据
            HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
            Long userId = this.getUserId(request);
            if(userId!=null) {
                result = userMapper.selectByPrimaryKey(id);
            }
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    public List<User> getUserListByIds(String ids)throws Exception {
        List<Long> idList = StringUtil.strToLongList(ids);
        List<User> list = new ArrayList<>();
        try{
            UserExample example = new UserExample();
            example.createCriteria().andIdIn(idList);
            list = userMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<User> getUser(HttpServletRequest request) throws Exception {
        List<User> list=null;
        try{
            //先校验是否登录，然后才能查询用户数据
            Long userId = this.getUserId(request);
            if(userId!=null) {
                UserExample example = new UserExample();
                example.createCriteria().andStatusEqualTo(BusinessConstants.USER_STATUS_NORMAL).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
                list = userMapper.selectByExample(example);
            }
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<UserEx> select(String userName, String loginName)throws Exception {
        List<UserEx> list=null;
        try {
            //先校验是否登录，然后才能查询用户数据
            HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
            Long userId = this.getUserId(request);
            if(userId!=null) {
                PageUtils.startPage();
                list = userMapperEx.selectByConditionUser(userName, loginName);
                for (UserEx ue : list) {
                    String userType = "";
                    if (ue.getId().equals(ue.getTenantId())) {
                        userType = "租户";
                    } else if (ue.getTenantId() == null) {
                        userType = "超管";
                    } else {
                        userType = "普通";
                    }
                    ue.setUserType(userType);
                    //是否经理
                    String leaderFlagStr = "";
                    if ("1".equals(ue.getLeaderFlag())) {
                        leaderFlagStr = "是";
                    } else {
                        leaderFlagStr = "否";
                    }
                    ue.setLeaderFlagStr(leaderFlagStr);
                }
            }
        } catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public Long countUser(String userName, String loginName)throws Exception {
        Long result=null;
        try{
            result=userMapperEx.countsByUser(userName, loginName);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int insertUser(JSONObject obj, HttpServletRequest request)throws Exception {
        checkEditPermission();
        User input = JSONObject.parseObject(obj.toJSONString(), User.class);
        User currentUser = getCurrentUser();
        User user = editableUserFields(input);
        user.setTenantId(currentUser.getTenantId());
        user.setIsystem(BusinessConstants.USER_NOT_SYSTEM);
        user.setIsmanager(BusinessConstants.USER_NOT_MANAGER);
        user.setStatus(BusinessConstants.USER_STATUS_NORMAL);
        user.setDeleteFlag(BusinessConstants.DELETE_FLAG_EXISTS);
        user.setPassword(userPasswordService.encode(Tools.md5Encryp(BusinessConstants.USER_DEFAULT_PASSWORD)));
        checkLoginName(toUserEx(user));
        int result = userMapper.insertSelective(user);
        logService.insertLog("用户",
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_ADD).append(user.getLoginName()).toString(), request);
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int updateUser(JSONObject obj, HttpServletRequest request) throws Exception{
        checkEditPermission();
        User input = JSONObject.parseObject(obj.toJSONString(), User.class);
        User existing = requireManageableUser(input.getId(), false);
        User user = editableUserFields(input);
        user.setId(existing.getId());
        user.setLoginName(existing.getLoginName());
        int result = userMapper.updateByPrimaryKeySelective(user);
        logService.insertLog("用户",
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append(existing.getLoginName()).toString(), request);
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int resetPwd(String md5Pwd, Long id, HttpServletRequest request) throws Exception{
        checkEditPermission();
        User target = requireManageableUser(id, true);
        User user = new User();
        user.setId(id);
        user.setPassword(userPasswordService.encode(md5Pwd));
        int result = userMapper.updateByPrimaryKeySelective(user);
        if (result > 0) {
            redisService.deleteObjectByUser(id);
            logService.insertLog("用户",
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append(target.getLoginName()).toString(), request);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int updateCurrentUserPassword(String oldPassword, String newPassword, HttpServletRequest request) throws Exception {
        Long currentUserId = getUserId(request);
        User currentUser = currentUserId == null ? null : userMapper.selectByPrimaryKey(currentUserId);
        if (currentUser == null || !userPasswordService.matches(oldPassword, currentUser.getPassword())) {
            return 2;
        }
        User update = new User();
        update.setId(currentUserId);
        update.setPassword(userPasswordService.encode(newPassword));
        int result = userMapper.updateByPrimaryKeySelective(update);
        if (result > 0) {
            redisService.deleteObjectByUser(currentUserId);
            logService.insertLog("用户", BusinessConstants.LOG_OPERATION_TYPE_EDIT + currentUserId, request);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int deleteUser(Long id, HttpServletRequest request)throws Exception {
        return batDeleteUser(id.toString(), request);
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteUser(String ids, HttpServletRequest request)throws Exception {
        return batDeleteUser(ids, request);
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batDeleteUser(String ids, HttpServletRequest request) throws Exception{
        checkEditPermission();
        int result=0;
        StringBuffer sb = new StringBuffer();
        sb.append(BusinessConstants.LOG_OPERATION_TYPE_DELETE);
        List<User> list = getUserListByIds(ids);
        if (list.isEmpty()) {
            throw invalidUser("未找到可删除的用户");
        }
        Long currentUserId = getUserId(request);
        for(User user: list){
            if(user.getId().equals(currentUserId)) {
                throw invalidUser("不能删除当前登录用户");
            }
            if(BusinessConstants.DEFAULT_MANAGER.equals(user.getLoginName()) || user.getId().equals(user.getTenantId())) {
                logger.error("异常码[{}],异常提示[{}],参数,ids:[{}]",
                        ExceptionConstants.USER_LIMIT_TENANT_DELETE_CODE,ExceptionConstants.USER_LIMIT_TENANT_DELETE_MSG,ids);
                throw new BusinessRunTimeException(ExceptionConstants.USER_LIMIT_TENANT_DELETE_CODE,
                        ExceptionConstants.USER_LIMIT_TENANT_DELETE_MSG);
            }
            sb.append("[").append(user.getLoginName()).append("]");
        }
        String[] idsArray =ids.split(",");
        try{
            result = userMapperEx.batDeleteOrUpdateUser(idsArray);
            if(result>0) {
                List<Long> userIds = list.stream().map(User::getId).collect(java.util.stream.Collectors.toList());
                OrgaUserRel orgaUpdate = new OrgaUserRel();
                orgaUpdate.setDeleteFlag(BusinessConstants.DELETE_FLAG_DELETED);
                OrgaUserRelExample orgaExample = new OrgaUserRelExample();
                orgaExample.createCriteria().andUserIdIn(userIds);
                orgaUserRelMapper.updateByExampleSelective(orgaUpdate, orgaExample);

                UserBusiness businessUpdate = new UserBusiness();
                businessUpdate.setDeleteFlag(BusinessConstants.DELETE_FLAG_DELETED);
                UserBusinessExample businessExample = new UserBusinessExample();
                businessExample.createCriteria().andKeyIdIn(
                        userIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.toList()))
                        .andTypeIn(Arrays.asList("UserRole", "UserCustomer", "UserDepot"));
                userBusinessMapper.updateByExampleSelective(businessUpdate, businessExample);
                for (Long id : userIds) {
                    redisService.deleteObjectByUser(id);
                }
            }
            logService.insertLog("用户", sb.toString(), request);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        if(result<1){
            logger.error("异常码[{}],异常提示[{}],参数,ids:[{}]",
                    ExceptionConstants.USER_DELETE_FAILED_CODE,ExceptionConstants.USER_DELETE_FAILED_MSG,ids);
            throw new BusinessRunTimeException(ExceptionConstants.USER_DELETE_FAILED_CODE,
                    ExceptionConstants.USER_DELETE_FAILED_MSG);
        }
        return result;
    }

    /**
     * 校验验证码
     * @param code 验证码
     * @param uuid 唯一标识
     * @return 结果
     */
    public void validateCaptcha(String code, String uuid) throws Exception {
        PlatformConfig platformConfig = platformConfigService.getInfoByKey("checkcode_flag");
        if(platformConfig!=null && "1".equals(platformConfig.getPlatformValue())) {
            if(StringUtil.isNotEmpty(code) && StringUtil.isNotEmpty(uuid)) {
                code = code.trim();
                uuid = uuid.trim();
                String verifyKey = BusinessConstants.CAPTCHA_CODE_KEY + uuid;
                String captcha = redisService.getCacheObject(verifyKey);
                redisService.deleteObject(verifyKey);
                if (captcha == null) {
                    logger.error("异常码[{}],异常提示[{}]", ExceptionConstants.USER_JCAPTCHA_EXPIRE_CODE, ExceptionConstants.USER_JCAPTCHA_EXPIRE_MSG);
                    throw new BusinessRunTimeException(ExceptionConstants.USER_JCAPTCHA_EXPIRE_CODE, ExceptionConstants.USER_JCAPTCHA_EXPIRE_MSG);
                }
                if (!code.equalsIgnoreCase(captcha)) {
                    logger.error("异常码[{}],异常提示[{}]", ExceptionConstants.USER_JCAPTCHA_ERROR_CODE, ExceptionConstants.USER_JCAPTCHA_ERROR_MSG);
                    throw new BusinessRunTimeException(ExceptionConstants.USER_JCAPTCHA_ERROR_CODE, ExceptionConstants.USER_JCAPTCHA_ERROR_MSG);
                }
            } else {
                logger.error("异常码[{}],异常提示[{}]", ExceptionConstants.USER_JCAPTCHA_EMPTY_CODE, ExceptionConstants.USER_JCAPTCHA_EMPTY_MSG);
                throw new BusinessRunTimeException(ExceptionConstants.USER_JCAPTCHA_EMPTY_CODE, ExceptionConstants.USER_JCAPTCHA_EMPTY_MSG);
            }
        }
    }

    /**
     * 用户登录
     * @param loginName
     * @param password
     * @param request
     * @return
     * @throws Exception
     */
    public Map<String, Object> login(String loginName, String password, HttpServletRequest request) throws Exception {
        Map<String, Object> data = new HashMap<>();
        String msgTip = "";
        User user = null;
        //判断用户是否已经登录过，登录过不再处理
        Object userId = redisService.getObjectFromSessionByKey(request,"userId");
        if (userId != null) {
            logger.info("====用户已经登录过, login 方法调用结束====");
            msgTip = "user already login";
        }
        //获取用户状态
        int userStatus = -1;
        try {
            redisService.deleteObjectBySession(request,"userId");
            userStatus = validateUser(loginName, password);
        } catch (Exception e) {
            logger.error(">>>>>>>>>>>>>用户  " + loginName + " 登录 login 方法 访问服务层异常====", e);
            msgTip = "access service exception";
        }
        String token = UUID.randomUUID().toString().replaceAll("-", "") + "";
        switch (userStatus) {
            case ExceptionCodeConstants.UserExceptionCode.USER_NOT_EXIST:
                msgTip = "user is not exist";
                break;
            case ExceptionCodeConstants.UserExceptionCode.USER_PASSWORD_ERROR:
                msgTip = "user password error";
                break;
            case ExceptionCodeConstants.UserExceptionCode.BLACK_USER:
                msgTip = "user is black";
                break;
            case ExceptionCodeConstants.UserExceptionCode.USER_ACCESS_EXCEPTION:
                msgTip = "access service error";
                break;
            case ExceptionCodeConstants.UserExceptionCode.BLACK_TENANT:
                msgTip = "tenant is black";
                break;
            case ExceptionCodeConstants.UserExceptionCode.EXPIRE_TENANT:
                msgTip = "tenant is expire";
                break;
            case ExceptionCodeConstants.UserExceptionCode.USER_CONDITION_FIT:
                msgTip = "user can login";
                //验证通过 ，可以登录，放入session，记录登录日志
                user = getUserByLoginName(loginName);
                if(user.getTenantId()!=null) {
                    token = token + "_" + user.getTenantId();
                }
                redisService.storageObjectBySession(token,"userId",user.getId());
                break;
            default:
                break;
        }
        data.put("msgTip", msgTip);
        if(user!=null){
            //校验下密码是不是过于简单
            boolean pwdSimple = false;
            if(userPasswordService.matches(Tools.md5Encryp(BusinessConstants.USER_DEFAULT_PASSWORD), user.getPassword())) {
                pwdSimple = true;
            }
            user.setPassword(null);
            user.setWeixinOpenId(null);
            if(BusinessConstants.DEFAULT_MANAGER.equals(user.getLoginName())) {
                //如果是管理员，则发送登录邮件
                sendEmailToCurrentUser(request, user);
            }
            redisService.storageObjectBySession(token,"clientIp", Tools.getLocalIp(request));
            logService.insertLogWithUserId(user.getId(), user.getTenantId(), "用户",
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_LOGIN).append(user.getLoginName()).toString(),
                    ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
            data.put("token", token);
            data.put("user", user);
            data.put("pwdSimple", pwdSimple);
        }
        return data;
    }

    public int validateUser(String loginName, String password) throws Exception {
        /**默认是可以登录的*/
        List<User> list = null;
        try {
            UserExample example = new UserExample();
            example.createCriteria().andLoginNameEqualTo(loginName).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
            list = userMapper.selectByExample(example);
            if (null != list && list.size() == 0) {
                return ExceptionCodeConstants.UserExceptionCode.USER_NOT_EXIST;
            } else if(list.size() ==1) {
                if(list.get(0).getStatus()!=0) {
                    return ExceptionCodeConstants.UserExceptionCode.BLACK_USER;
                }
                Long tenantId = list.get(0).getTenantId();
                Tenant tenant = tenantService.getTenantByTenantId(tenantId);
                if(tenant!=null) {
                    if(tenant.getEnabled()!=null && !tenant.getEnabled()) {
                        return ExceptionCodeConstants.UserExceptionCode.BLACK_TENANT;
                    }
                    if(tenant.getExpireTime()!=null && tenant.getExpireTime().getTime()<System.currentTimeMillis()){
                        return ExceptionCodeConstants.UserExceptionCode.EXPIRE_TENANT;
                    }
                }
            } else {
                return ExceptionCodeConstants.UserExceptionCode.USER_ACCESS_EXCEPTION;
            }
        } catch (Exception e) {
            logger.error(">>>>>>>>访问验证用户姓名是否存在后台信息异常", e);
            return ExceptionCodeConstants.UserExceptionCode.USER_ACCESS_EXCEPTION;
        }
        try {
            User user = list.get(0);
            if (!userPasswordService.matches(password, user.getPassword())) {
                return ExceptionCodeConstants.UserExceptionCode.USER_PASSWORD_ERROR;
            }
            if (userPasswordService.needsUpgrade(user.getPassword())) {
                User update = new User();
                update.setId(user.getId());
                update.setPassword(userPasswordService.encode(password));
                userMapper.updateByPrimaryKeySelective(update);
            }
        } catch (RuntimeException e) {
            logger.error(">>>>>>>>>>访问验证用户密码后台信息异常", e);
            return ExceptionCodeConstants.UserExceptionCode.USER_ACCESS_EXCEPTION;
        }
        return ExceptionCodeConstants.UserExceptionCode.USER_CONDITION_FIT;
    }

    public Map<String, Object> loginByWeixin(User user, HttpServletRequest request) throws Exception {
        Map<String, Object> data = new HashMap<>();
        if (user == null || user.getStatus() == null || user.getStatus() != BusinessConstants.USER_STATUS_NORMAL
                || BusinessConstants.DELETE_FLAG_DELETED.equals(user.getDeleteFlag())) {
            data.put("msgTip", "user is black");
            return data;
        }
        Tenant tenant = tenantService.getTenantByTenantId(user.getTenantId());
        if (tenant != null && (Boolean.FALSE.equals(tenant.getEnabled())
                || (tenant.getExpireTime() != null && tenant.getExpireTime().before(new Date())))) {
            data.put("msgTip", "tenant is black or expire");
            return data;
        }
        redisService.deleteObjectBySession(request, "userId");
        String token = UUID.randomUUID().toString().replace("-", "");
        if (user.getTenantId() != null) {
            token += "_" + user.getTenantId();
        }
        redisService.storageObjectBySession(token, "userId", user.getId());
        redisService.storageObjectBySession(token, "clientIp", Tools.getLocalIp(request));
        logService.insertLogWithUserId(user.getId(), user.getTenantId(), "用户",
                BusinessConstants.LOG_OPERATION_TYPE_LOGIN + user.getLoginName(), request);
        user.setPassword(null);
        user.setWeixinOpenId(null);
        data.put("msgTip", "user can login");
        data.put("token", token);
        data.put("user", user);
        data.put("pwdSimple", false);
        return data;
    }

    public User getUserByLoginName(String loginName)throws Exception {
        UserExample example = new UserExample();
        example.createCriteria().andLoginNameEqualTo(loginName).andStatusEqualTo(BusinessConstants.USER_STATUS_NORMAL)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<User> list=null;
        try{
            list= userMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        User user =null;
        if(list!=null&&list.size()>0){
            user = list.get(0);
        }
        return user;
    }

    /**
     * 发送邮件给当前用户
     * @param request
     * @param user
     * @throws Exception
     */
    private void sendEmailToCurrentUser(HttpServletRequest request, User user) throws Exception {
        String platformName = platformConfigService.getPlatformConfigByKey("platform_name").getPlatformValue();
        String emailFrom = platformConfigService.getPlatformConfigByKey("email_from").getPlatformValue();
        String emailAuthCode = platformConfigService.getPlatformConfigByKey("email_auth_code").getPlatformValue();
        String emailSmtpHost = platformConfigService.getPlatformConfigByKey("email_smtp_host").getPlatformValue();
        if(StringUtil.isNotEmpty(emailFrom) && StringUtil.isNotEmpty(emailAuthCode) && StringUtil.isNotEmpty(emailSmtpHost)
                && StringUtil.isNotEmpty(user.getEmail())) {
            String emailSubject = "用户" + user.getLoginName() + "成功登录" + platformName;
            String emailBody = "用户" + user.getLoginName() + "成功登录" + platformName + "，登录时间：" + Tools.getCenternTime(new Date())
                    + "，登录IP：" + Tools.getLocalIp(request);
            platformConfigService.sendEmail(emailFrom, emailAuthCode, emailSmtpHost, user.getEmail(), emailSubject, emailBody);
        }
    }

    public int checkIsNameExist(Long id, String name)throws Exception {
        UserExample example = new UserExample();
        example.createCriteria().andIdNotEqualTo(id).andLoginNameEqualTo(name)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<User> list=null;
        try{
            list= userMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list==null?0:list.size();
    }
    /**
     * create by: cjl
     * description:
     *  获取当前用户信息
     * create time: 2019/1/24 10:01
     * @Param:
     * @return com.jsh.erp.datasource.entities.User
     */
    public User getCurrentUser()throws Exception{
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        Long userId = Long.parseLong(redisService.getObjectFromSessionByKey(request,"userId").toString());
        return getUser(userId);
    }

    /**
     * 根据用户名查询id
     * @param loginName
     * @return
     */
    public Long getIdByLoginName(String loginName) {
        Long userId = 0L;
        UserExample example = new UserExample();
        example.createCriteria().andLoginNameEqualTo(loginName).andStatusEqualTo(BusinessConstants.USER_STATUS_NORMAL)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<User> list = userMapper.selectByExample(example);
        if(list!=null) {
            userId = list.get(0).getId();
        }
        return userId;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void addUserAndOrgUserRel(UserEx ue, HttpServletRequest request) throws Exception{
        checkEditPermission();
        ue = editableUserEx(ue, false);
        if(BusinessConstants.DEFAULT_MANAGER.equals(ue.getLoginName())) {
            throw new BusinessRunTimeException(ExceptionConstants.USER_NAME_LIMIT_USE_CODE,
                    ExceptionConstants.USER_NAME_LIMIT_USE_MSG);
        } else {
            logService.insertLog("用户",
                    BusinessConstants.LOG_OPERATION_TYPE_ADD,
                    ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
            //校验角色和部门的选择逻辑
            checkRoleAndOrg(ue);
            //检查用户名和登录名
            checkLoginName(ue);
            //新增用户信息
            ue= this.addUser(ue);
            if(ue==null){
                logger.error("异常码[{}],异常提示[{}],参数,[{}]",
                        ExceptionConstants.USER_ADD_FAILED_CODE,ExceptionConstants.USER_ADD_FAILED_MSG);
                throw new BusinessRunTimeException(ExceptionConstants.USER_ADD_FAILED_CODE,
                        ExceptionConstants.USER_ADD_FAILED_MSG);
            }
            //用户id，根据用户名查询id
            Long userId = getIdByLoginName(ue.getLoginName());
            if(ue.getRoleId()!=null){
                JSONObject ubObj = new JSONObject();
                ubObj.put("type", "UserRole");
                ubObj.put("keyid", userId);
                ubObj.put("value", "[" + ue.getRoleId() + "]");
                userBusinessService.insertUserBusiness(ubObj, request);
            }
            if(ue.getOrgaId()!=null && "1".equals(ue.getLeaderFlag())){
                //检查当前部门是否存在经理
                List<User> checkList = userMapperEx.getListByOrgaId(ue.getId(), ue.getOrgaId());
                if(checkList.size()>0) {
                    throw new BusinessRunTimeException(ExceptionConstants.USER_LEADER_IS_EXIST_CODE,
                            ExceptionConstants.USER_LEADER_IS_EXIST_MSG);
                }
            }
            //新增用户和部门关联关系
            OrgaUserRel oul=new OrgaUserRel();
            //部门id
            oul.setOrgaId(ue.getOrgaId());
            oul.setUserId(userId);
            //用户在部门中的排序
            oul.setUserBlngOrgaDsplSeq(ue.getUserBlngOrgaDsplSeq());
            oul=orgaUserRelService.addOrgaUserRel(oul);
            if(oul==null){
                logger.error("异常码[{}],异常提示[{}],参数,[{}]",
                        ExceptionConstants.ORGA_USER_REL_ADD_FAILED_CODE,ExceptionConstants.ORGA_USER_REL_ADD_FAILED_MSG);
                throw new BusinessRunTimeException(ExceptionConstants.ORGA_USER_REL_ADD_FAILED_CODE,
                        ExceptionConstants.ORGA_USER_REL_ADD_FAILED_MSG);
            }
        }
    }
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    private UserEx addUser(UserEx ue) throws Exception{
        /**
         * 新增用户默认设置
         * 1是否系统自带默认为非系统自带
         * 2是否管理者默认为员工
         * 3默认用户状态为正常
         * */
        ue.setIsystem(BusinessConstants.USER_NOT_SYSTEM);
        if(ue.getIsmanager()==null){
            ue.setIsmanager(BusinessConstants.USER_NOT_MANAGER);
        }
        ue.setStatus(BusinessConstants.USER_STATUS_NORMAL);
        ue.setTenantId(getCurrentUser().getTenantId());
        ue.setDeleteFlag(BusinessConstants.DELETE_FLAG_EXISTS);
        ue.setPassword(userPasswordService.encode(ue.getPassword()));
        int result=0;
        try{
            result= userMapper.insertSelective(ue);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        if(result>0){
            return ue;
        }
        return null;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void registerUser(UserEx ue, Integer manageRoleId, HttpServletRequest request) throws Exception{
        /**
         * 多次创建事务，事物之间无法协同，应该在入口处创建一个事务以做协调
         */
        if(BusinessConstants.DEFAULT_MANAGER.equals(ue.getLoginName())) {
            throw new BusinessRunTimeException(ExceptionConstants.USER_NAME_LIMIT_USE_CODE,
                    ExceptionConstants.USER_NAME_LIMIT_USE_MSG);
        } else {
            UserEx registration = new UserEx();
            registration.setLoginName(ue.getLoginName());
            registration.setUsername(ue.getLoginName());
            registration.setPassword(userPasswordService.encode(ue.getPassword()));
            ue = registration;
            ue.setIsystem(BusinessConstants.USER_NOT_SYSTEM);
            ue.setIsmanager(BusinessConstants.USER_NOT_MANAGER);
            ue.setStatus(BusinessConstants.USER_STATUS_NORMAL);
            ue.setDeleteFlag(BusinessConstants.DELETE_FLAG_EXISTS);
            try{
                userMapper.insertSelective(ue);
                Long userId = getIdByLoginName(ue.getLoginName());
                ue.setId(userId);
            }catch(Exception e){
                JshException.writeFail(logger, e);
            }
            //更新租户id
            User user = new User();
            user.setId(ue.getId());
            user.setTenantId(ue.getId());
            updateUserTenant(user);
            //新增用户与角色的关系
            JSONObject ubObj = new JSONObject();
            ubObj.put("type", "UserRole");
            ubObj.put("keyid", ue.getId());
            JSONArray ubArr = new JSONArray();
            ubArr.add(manageRoleId);
            ubObj.put("value", ubArr.toString());
            ubObj.put("tenantId", ue.getId());
            userBusinessService.insertUserBusiness(ubObj, null);
            //创建租户信息
            JSONObject tenantObj = new JSONObject();
            tenantObj.put("tenantId", ue.getId());
            tenantObj.put("loginName",ue.getLoginName());
            Tenant tenant = JSONObject.parseObject(tenantObj.toJSONString(), Tenant.class);
            tenant.setCreateTime(new Date());
            tenant.setUserNumLimit(userNumLimit);
            tenant.setExpireTime(Tools.addDays(new Date(), tryDayLimit));
            tenantMapper.insertSelective(tenant);
            logger.info("===============创建租户信息完成===============");
        }
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    private void updateUserTenant(User user) throws Exception{
        UserExample example = new UserExample();
        example.createCriteria().andIdEqualTo(user.getId());
        try{
            userMapper.updateByPrimaryKeySelective(user);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void updateUserAndOrgUserRel(UserEx ue, HttpServletRequest request) throws Exception{
        checkEditPermission();
        User target = requireManageableUser(ue.getId(), false);
        UserEx input = ue;
        ue = editableUserEx(input, true);
        ue.setId(target.getId());
        ue.setLoginName(target.getLoginName());
        if(BusinessConstants.DEFAULT_MANAGER.equals(ue.getLoginName())) {
            throw new BusinessRunTimeException(ExceptionConstants.USER_NAME_LIMIT_USE_CODE,
                    ExceptionConstants.USER_NAME_LIMIT_USE_MSG);
        } else {
            logService.insertLog("用户",
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append(ue.getId()).toString(),
                    ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
            //校验角色和部门的选择逻辑
            checkRoleAndOrg(ue);
            //更新用户信息
            ue = this.updateUser(ue);
            if (ue == null) {
                logger.error("异常码[{}],异常提示[{}],参数,[{}]",
                        ExceptionConstants.USER_EDIT_FAILED_CODE, ExceptionConstants.USER_EDIT_FAILED_MSG);
                throw new BusinessRunTimeException(ExceptionConstants.USER_EDIT_FAILED_CODE,
                        ExceptionConstants.USER_EDIT_FAILED_MSG);
            }
            if(ue.getRoleId()!=null){
                JSONObject ubObj = new JSONObject();
                ubObj.put("type", "UserRole");
                ubObj.put("keyid", ue.getId());
                ubObj.put("value", "[" + ue.getRoleId() + "]");
                Long ubId = userBusinessService.checkIsValueExist("UserRole", ue.getId().toString());
                if(ubId!=null) {
                    ubObj.put("id", ubId);
                    userBusinessService.updateUserBusiness(ubObj, request);
                } else {
                    userBusinessService.insertUserBusiness(ubObj, request);
                }
            }
            if(ue.getOrgaId()!=null && "1".equals(ue.getLeaderFlag())){
                //检查当前部门是否存在经理
                List<User> checkList = userMapperEx.getListByOrgaId(ue.getId(), ue.getOrgaId());
                if(checkList.size()>0) {
                    throw new BusinessRunTimeException(ExceptionConstants.USER_LEADER_IS_EXIST_CODE,
                            ExceptionConstants.USER_LEADER_IS_EXIST_MSG);
                }
            }
            //更新用户和部门关联关系
            OrgaUserRel oul = new OrgaUserRel();
            //部门和用户关联关系id
            oul.setId(ue.getOrgaUserRelId());
            //部门id
            oul.setOrgaId(ue.getOrgaId());
            //用户id
            oul.setUserId(ue.getId());
            //用户在部门中的排序
            oul.setUserBlngOrgaDsplSeq(ue.getUserBlngOrgaDsplSeq());
            if (oul.getId() != null) {
                OrgaUserRel existingRelation = orgaUserRelMapper.selectByPrimaryKey(oul.getId());
                if (existingRelation == null || !ue.getId().equals(existingRelation.getUserId())) {
                    throw invalidUser("部门用户关系与目标用户不匹配");
                }
                //已存在部门和用户的关联关系，更新
                oul = orgaUserRelService.updateOrgaUserRel(oul);
            } else {
                //不存在部门和用户的关联关系，新建
                oul = orgaUserRelService.addOrgaUserRel(oul);
            }
            if (oul == null) {
                logger.error("异常码[{}],异常提示[{}],参数,[{}]",
                        ExceptionConstants.ORGA_USER_REL_EDIT_FAILED_CODE, ExceptionConstants.ORGA_USER_REL_EDIT_FAILED_MSG);
                throw new BusinessRunTimeException(ExceptionConstants.ORGA_USER_REL_EDIT_FAILED_CODE,
                        ExceptionConstants.ORGA_USER_REL_EDIT_FAILED_MSG);
            }
        }
    }
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    private UserEx updateUser(UserEx ue)throws Exception{
        int result =0;
        try{
            result=userMapper.updateByPrimaryKeySelective(ue);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        if(result>0){
            return ue;
        }
        return null;
    }

    /**
     * 校验角色和部门的选择逻辑：如果角色的数据类型是本部门数据，此时如果未选择部门，则异常
     * @param ue
     */
    private void checkRoleAndOrg(UserEx ue) throws Exception {
        Role selectedRole = roleService.requireActiveRole(ue.getRoleId());
        User currentUser = getCurrentUser();
        Long currentTenantId = currentUser == null ? null : currentUser.getTenantId();
        User targetUser = ue.getId() == null ? null : getUser(ue.getId());
        boolean targetIsTenantManager = targetUser != null
                && Objects.equals(targetUser.getId(), targetUser.getTenantId());
        if(currentTenantId != null && currentTenantId != 0L) {
            boolean localRole = Objects.equals(selectedRole.getTenantId(), currentTenantId);
            boolean tenantTemplateRole = targetIsTenantManager && selectedRole.getTenantId() == null;
            if(!localRole && !tenantTemplateRole) {
                throw new BusinessRunTimeException(ExceptionConstants.SUPPLIER_PERMISSION_CODE,
                        ExceptionConstants.SUPPLIER_PERMISSION_MSG);
            }
        }
        validateAssignableRole(selectedRole, currentUser);
        if (ue.getOrgaId() != null) {
            Organization organization = organizationService.getOrganization(ue.getOrgaId());
            if (organization == null || BusinessConstants.DELETE_FLAG_DELETED.equals(organization.getDeleteFlag())
                    || (currentTenantId != null && currentTenantId != 0L
                    && !Objects.equals(currentTenantId, organization.getTenantId()))) {
                throw invalidUser("部门不存在或不属于当前租户");
            }
        }
        if(!targetIsTenantManager) {
            //只对非租户的用户进行校验
            Long orgaId = ue.getOrgaId();
            String type = selectedRole.getType();
            if("本部门数据".equals(type) && orgaId==null) {
                throw new BusinessRunTimeException(ExceptionConstants.USER_ROLE_ORGA_EMPTY_CODE,
                        ExceptionConstants.USER_ROLE_ORGA_EMPTY_MSG);
            }
        }
    }

    private void validateAssignableRole(Role selectedRole, User currentUser) throws Exception {
        if (currentUser == null || BusinessConstants.DEFAULT_MANAGER.equals(currentUser.getLoginName())) {
            return;
        }
        Role currentRole = getRoleTypeByUserId(currentUser.getId());
        if (BusinessConstants.ROLE_TYPE_THIS_ORG.equals(currentRole.getType())
                && BusinessConstants.ROLE_TYPE_PUBLIC.equals(selectedRole.getType())) {
            throw invalidUser("不能分配超出当前用户数据范围的角色");
        }
        UserBusiness currentPermissions = getRolePermissions(currentRole.getId());
        UserBusiness selectedPermissions = getRolePermissions(selectedRole.getId());
        Set<Long> currentFunctions = parseRelationIds(currentPermissions == null ? null : currentPermissions.getValue());
        Set<Long> selectedFunctions = parseRelationIds(selectedPermissions == null ? null : selectedPermissions.getValue());
        if (!currentFunctions.containsAll(selectedFunctions)) {
            throw invalidUser("不能分配超出当前用户菜单权限的角色");
        }
        Map<Long, Set<String>> currentButtons = parseRoleButtons(currentPermissions == null ? null : currentPermissions.getBtnStr());
        Map<Long, Set<String>> selectedButtons = parseRoleButtons(selectedPermissions == null ? null : selectedPermissions.getBtnStr());
        for (Map.Entry<Long, Set<String>> entry : selectedButtons.entrySet()) {
            if (!currentButtons.getOrDefault(entry.getKey(), Collections.emptySet()).containsAll(entry.getValue())) {
                throw invalidUser("不能分配超出当前用户按钮权限的角色");
            }
        }
    }

    private UserBusiness getRolePermissions(Long roleId) throws Exception {
        List<UserBusiness> relations = userBusinessService.getBasicData(String.valueOf(roleId), "RoleFunctions");
        return relations == null || relations.isEmpty() ? null : relations.get(0);
    }

    private Set<Long> parseRelationIds(String value) {
        Set<Long> result = new HashSet<>();
        if (StringUtil.isEmpty(value)) {
            return result;
        }
        String normalized = value.replace("][", ",").replace("[", "").replace("]", "");
        if (StringUtil.isEmpty(normalized)) {
            return result;
        }
        for (String item : normalized.split(",")) {
            result.add(Long.parseLong(item.trim()));
        }
        return result;
    }

    private Map<Long, Set<String>> parseRoleButtons(String value) {
        Map<Long, Set<String>> result = new HashMap<>();
        if (StringUtil.isEmpty(value)) {
            return result;
        }
        for (Object item : JSONArray.parseArray(value)) {
            JSONObject button = JSONObject.parseObject(item.toString());
            Long functionId = button.getLong("funId");
            if (functionId != null) {
                String buttonValue = button.getString("btnStr");
                result.put(functionId, StringUtil.isEmpty(buttonValue)
                        ? new HashSet<>() : new HashSet<>(Arrays.asList(buttonValue.split(","))));
            }
        }
        return result;
    }
    /**
     *  检查登录名不能重复
     * create time: 2019/3/12 11:36
     * @Param: userEx
     * @return void
     */
    public void checkLoginName(UserEx userEx)throws Exception{
        List<User> list=null;
        if(userEx==null){
            return;
        }
        Long userId=userEx.getId();
        //检查登录名
        if(!StringUtils.isEmpty(userEx.getLoginName())){
            String loginName=userEx.getLoginName();
            list=this.getUserListByloginName(loginName);
            if(list!=null&&list.size()>0){
                if(list.size()>1){
                    //超过一条数据存在，该登录名已存在
                    logger.error("异常码[{}],异常提示[{}],参数,loginName:[{}]",
                            ExceptionConstants.USER_LOGIN_NAME_ALREADY_EXISTS_CODE,ExceptionConstants.USER_LOGIN_NAME_ALREADY_EXISTS_MSG,loginName);
                    throw new BusinessRunTimeException(ExceptionConstants.USER_LOGIN_NAME_ALREADY_EXISTS_CODE,
                            ExceptionConstants.USER_LOGIN_NAME_ALREADY_EXISTS_MSG);
                }
                //一条数据，新增时抛出异常，修改时和当前的id不同时抛出异常
                if(list.size()==1){
                    if(userId==null||(userId!=null&&!userId.equals(list.get(0).getId()))){
                        logger.error("异常码[{}],异常提示[{}],参数,loginName:[{}]",
                                ExceptionConstants.USER_LOGIN_NAME_ALREADY_EXISTS_CODE,ExceptionConstants.USER_LOGIN_NAME_ALREADY_EXISTS_MSG,loginName);
                        throw new BusinessRunTimeException(ExceptionConstants.USER_LOGIN_NAME_ALREADY_EXISTS_CODE,
                                ExceptionConstants.USER_LOGIN_NAME_ALREADY_EXISTS_MSG);
                    }
                }
            }
        }
    }
    /**
     * 通过登录名获取用户列表
     * */
    public List<User> getUserListByloginName(String loginName){
        List<User> list =null;
        try{
            list=userMapperEx.getUserListByUserNameOrLoginName(null,loginName);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<TreeNodeEx> getOrganizationUserTree()throws Exception {
        List<TreeNodeEx> list =null;
        try{
            list=userMapperEx.getNodeTree();
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    /**
     * 根据用户id查询角色信息
     * @param userId
     * @return
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public Role getRoleTypeByUserId(long userId) throws Exception {
        List<UserBusiness> list = userBusinessService.getBasicData(String.valueOf(userId), "UserRole");
        if(list == null || list.isEmpty() || StringUtil.isEmpty(list.get(0).getValue())) {
            throw new BusinessRunTimeException(ExceptionConstants.SUPPLIER_PERMISSION_CODE,
                    ExceptionConstants.SUPPLIER_PERMISSION_MSG);
        }
        String roleId = list.get(0).getValue().replace("[", "").replace("]", "");
        try {
            return roleService.requireActiveRole(Long.parseLong(roleId));
        } catch(NumberFormatException e) {
            throw new BusinessRunTimeException(ExceptionConstants.SUPPLIER_PERMISSION_CODE,
                    ExceptionConstants.SUPPLIER_PERMISSION_MSG);
        }
    }

    /**
     * 获取用户id
     * @param request
     * @return
     */
    public Long getUserId(HttpServletRequest request) throws Exception{
        Object userIdObj = redisService.getObjectFromSessionByKey(request,"userId");
        Long userId = null;
        if(userIdObj != null) {
            userId = Long.parseLong(userIdObj.toString());
        }
        return userId;
    }

    /**
     * 用户的按钮权限
     * @param userId
     * @return
     * @throws Exception
     */
    public JSONArray getBtnStrArrById(Long userId) throws Exception {
        JSONArray btnStrArr = new JSONArray();
        List<UserBusiness> userRoleList = userBusinessService.getBasicData(userId.toString(), "UserRole");
        if(userRoleList!=null && userRoleList.size()>0) {
            String roleValue = userRoleList.get(0).getValue();
            if(StringUtil.isNotEmpty(roleValue) && roleValue.indexOf("[")>-1 && roleValue.indexOf("]")>-1){
                roleValue = roleValue.replace("[", "").replace("]", ""); //角色id-单个
                try {
                    roleService.requireActiveRole(Long.parseLong(roleValue));
                } catch(NumberFormatException | BusinessRunTimeException e) {
                    return btnStrArr;
                }
                List<UserBusiness> roleFunctionsList = userBusinessService.getBasicData(roleValue, "RoleFunctions");
                if(roleFunctionsList!=null && roleFunctionsList.size()>0) {
                    String btnStr = roleFunctionsList.get(0).getBtnStr();
                    if(StringUtil.isNotEmpty(btnStr)){
                        btnStrArr = JSONArray.parseArray(btnStr);
                    }
                }
            }
        }
        //将数组中的funId转为url
        JSONArray btnStrWithUrlArr = new JSONArray();
        if(btnStrArr.size()>0) {
            List<Function> functionList = functionService.getFunction();
            Map<Long, String> functionMap = new HashMap<>();
            for (Function function: functionList) {
                functionMap.put(function.getId(), function.getUrl());
            }
            for (Object obj : btnStrArr) {
                JSONObject btnStrObj = JSONObject.parseObject(obj.toString());
                Long funId = btnStrObj.getLong("funId");
                JSONObject btnStrWithUrlObj = new JSONObject();
                btnStrWithUrlObj.put("url", functionMap.get(funId));
                btnStrWithUrlObj.put("btnStr", btnStrObj.getString("btnStr"));
                btnStrWithUrlArr.add(btnStrWithUrlObj);
            }
        }
        return btnStrWithUrlArr;
    }

    /**
     * 校验当前用户是否拥有指定页面的按钮权限。
     * admin 保持系统原有的超级管理员行为，默认拥有全部按钮权限。
     */
    public boolean hasButtonPermission(Long userId, String url, String buttonCode) throws Exception {
        if (userId == null) {
            return false;
        }
        User user = getUser(userId);
        if (user != null && "admin".equals(user.getLoginName())) {
            return true;
        }
        JSONArray buttonList = getBtnStrArrById(userId);
        for (Object item : buttonList) {
            JSONObject button = JSONObject.parseObject(item.toString());
            if (url.equals(button.getString("url"))) {
                String buttonString = button.getString("btnStr");
                if (StringUtil.isNotEmpty(buttonString)) {
                    List<String> buttonCodes = Arrays.asList(buttonString.split(","));
                    return buttonCodes.contains(buttonCode);
                }
            }
        }
        return false;
    }

    /**
     * 校验当前用户是否拥有指定页面的菜单功能权限。
     * 按钮权限只覆盖增删改审，详情查询仍需用菜单权限兜底。
     */
    public boolean hasFunctionPermission(Long userId, String url) throws Exception {
        if (userId == null) {
            return false;
        }
        User user = getUser(userId);
        if (user != null && "admin".equals(user.getLoginName())) {
            return true;
        }
        List<UserBusiness> userRoleList = userBusinessService.getBasicData(userId.toString(), "UserRole");
        if (userRoleList == null || userRoleList.isEmpty()) {
            return false;
        }
        String roleValue = userRoleList.get(0).getValue();
        if (StringUtil.isEmpty(roleValue)) {
            return false;
        }
        String roleId = roleValue.replace("[", "").replace("]", "");
        try {
            roleService.requireActiveRole(Long.parseLong(roleId));
        } catch(NumberFormatException | BusinessRunTimeException e) {
            return false;
        }
        List<UserBusiness> roleFunctionsList = userBusinessService.getBasicData(roleId, "RoleFunctions");
        if (roleFunctionsList == null || roleFunctionsList.isEmpty()) {
            return false;
        }
        String functionValue = roleFunctionsList.get(0).getValue();
        if (StringUtil.isEmpty(functionValue)) {
            return false;
        }
        List<Long> functionIds = StringUtil.strToLongList(
                functionValue.substring(1, functionValue.length() - 1).replace("][", ","));
        for (Function function : functionService.getFunction()) {
            if (functionIds.contains(function.getId()) && url.equals(function.getUrl())) {
                return true;
            }
        }
        return false;
    }

    public void checkReadPermission() throws Exception {
        User currentUser = getCurrentUser();
        if (currentUser == null || !hasFunctionPermission(currentUser.getId(), USER_URL)) {
            throw permissionDenied();
        }
    }

    public void checkEditPermission() throws Exception {
        User currentUser = getCurrentUser();
        if (currentUser == null || !hasButtonPermission(currentUser.getId(), USER_URL, EDIT_BUTTON_CODE)) {
            throw permissionDenied();
        }
    }

    public User getSafeUser(Long id) throws Exception {
        checkReadPermission();
        User user = requireManageableUser(id, false);
        User safe = editableUserFields(user);
        safe.setId(user.getId());
        safe.setStatus(user.getStatus());
        safe.setIsmanager(user.getIsmanager());
        safe.setIsystem(user.getIsystem());
        safe.setTenantId(user.getTenantId());
        safe.setDeleteFlag(user.getDeleteFlag());
        return safe;
    }

    public void checkRateLimit(String action, HttpServletRequest request, int maximum, long seconds) {
        String source = Tools.getLocalIp(request);
        String key = "security:rate:" + action + ":" + (source == null ? "unknown" : source);
        if (redisService.incrementWithExpire(key, seconds) > maximum) {
            throw new BusinessRunTimeException(ExceptionConstants.SUPPLIER_INVALID_CODE, "操作过于频繁，请稍后重试");
        }
    }

    private User requireManageableUser(Long id, boolean sensitiveOperation) throws Exception {
        User currentUser = getCurrentUser();
        User target = id == null ? null : userMapper.selectByPrimaryKey(id);
        if (currentUser == null || target == null || BusinessConstants.DELETE_FLAG_DELETED.equals(target.getDeleteFlag())) {
            throw invalidUser("用户不存在或已删除");
        }
        boolean administrator = BusinessConstants.DEFAULT_MANAGER.equals(currentUser.getLoginName());
        if (!administrator && !Objects.equals(currentUser.getTenantId(), target.getTenantId())) {
            throw permissionDenied();
        }
        boolean tenantManager = Objects.equals(target.getId(), target.getTenantId());
        if (!administrator && tenantManager && (!Objects.equals(currentUser.getId(), target.getId()) || sensitiveOperation)) {
            throw permissionDenied();
        }
        if (!administrator && BusinessConstants.DEFAULT_MANAGER.equals(target.getLoginName())) {
            throw permissionDenied();
        }
        return target;
    }

    private User editableUserFields(User source) {
        User target = new User();
        if (source == null) {
            return target;
        }
        target.setLoginName(source.getLoginName());
        target.setUsername(source.getUsername());
        target.setLeaderFlag(source.getLeaderFlag());
        target.setPosition(source.getPosition());
        target.setDepartment(source.getDepartment());
        target.setEmail(source.getEmail());
        target.setPhonenum(source.getPhonenum());
        target.setDescription(source.getDescription());
        target.setRemark(source.getRemark());
        return target;
    }

    private UserEx editableUserEx(UserEx source, boolean update) {
        UserEx target = new UserEx();
        User fields = editableUserFields(source);
        target.setLoginName(fields.getLoginName());
        target.setUsername(fields.getUsername());
        target.setLeaderFlag(fields.getLeaderFlag());
        target.setPosition(fields.getPosition());
        target.setDepartment(fields.getDepartment());
        target.setEmail(fields.getEmail());
        target.setPhonenum(fields.getPhonenum());
        target.setDescription(fields.getDescription());
        target.setRemark(fields.getRemark());
        target.setRoleId(source.getRoleId());
        target.setOrgaId(source.getOrgaId());
        target.setUserBlngOrgaDsplSeq(source.getUserBlngOrgaDsplSeq());
        target.setOrgaUserRelId(update ? source.getOrgaUserRelId() : null);
        if (!update) {
            target.setPassword(source.getPassword());
        }
        return target;
    }

    private UserEx toUserEx(User user) {
        UserEx result = new UserEx();
        result.setId(user.getId());
        result.setLoginName(user.getLoginName());
        return result;
    }

    private BusinessRunTimeException permissionDenied() {
        return new BusinessRunTimeException(ExceptionConstants.SUPPLIER_PERMISSION_CODE,
                ExceptionConstants.SUPPLIER_PERMISSION_MSG);
    }

    private BusinessRunTimeException invalidUser(String message) {
        return new BusinessRunTimeException(ExceptionConstants.SUPPLIER_INVALID_CODE, message);
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchSetStatus(Byte status, String ids, HttpServletRequest request)throws Exception {
        checkEditPermission();
        if (status == null || (status != BusinessConstants.USER_STATUS_NORMAL && status != 2)) {
            throw invalidUser("用户状态不合法");
        }
        int result=0;
        List<User> list = getUserListByIds(ids);
        if (list.isEmpty()) {
            throw invalidUser("未找到可操作的用户");
        }
        int newlyEnabled = (int) list.stream()
                .filter(item -> item.getStatus() == null || item.getStatus() != BusinessConstants.USER_STATUS_NORMAL).count();
        int enableUserSize = getUser(request).size();
        User userInfo = userService.getCurrentUser();
        Tenant tenant = tenantService.getTenantByTenantId(userInfo.getTenantId());
        if(tenant!=null) {
            if (newlyEnabled + enableUserSize > tenant.getUserNumLimit()
                    && status == BusinessConstants.USER_STATUS_NORMAL) {
                throw new BusinessParamCheckingException(ExceptionConstants.USER_ENABLE_OVER_LIMIT_FAILED_CODE,
                        ExceptionConstants.USER_ENABLE_OVER_LIMIT_FAILED_MSG);
            }
        }
        StringBuilder userStr = new StringBuilder();
        List<Long> idList = new ArrayList<>();
        Long currentUserId = getUserId(request);
        for(User user: list) {
            if (status == 2 && user.getId().equals(currentUserId)) {
                throw invalidUser("不能禁用当前登录用户");
            }
            requireManageableUser(user.getId(), status == 2);
            idList.add(user.getId());
            userStr.append(user.getLoginName()).append(" ");
        }
        String statusStr ="";
        if(status == 0) {
            statusStr ="批量启用";
        } else if(status == 2) {
            statusStr ="批量禁用";
        }
        if(idList.size()>0) {
            User user = new User();
            user.setStatus(status);
            UserExample example = new UserExample();
            example.createCriteria().andIdIn(idList);
            result = userMapper.updateByExampleSelective(user, example);
            if (status == 2 && result > 0) {
                for (Long id : idList) {
                    redisService.deleteObjectByUser(id);
                }
            }
            logService.insertLog("用户",
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append(userStr).append("-").append(statusStr).toString(),
                    ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        } else {
            result = 1;
        }
        return result;
    }

    public User getUserByWeixinCode(String weixinCode) throws Exception {
        String weixinLogin = platformConfigService.getPlatformConfigByKey("weixinUrl").getPlatformValue() + BusinessConstants.WEIXIN_LOGIN;
        String weixinAppid = platformConfigService.getPlatformConfigByKey("weixinAppid").getPlatformValue();
        String weixinSecret = platformConfigService.getPlatformConfigByKey("weixinSecret").getPlatformValue();
        String url = weixinLogin + "?appid=" + weixinAppid + "&secret=" + weixinSecret + "&js_code=" + weixinCode
                + "&grant_type=authorization_code";
        JSONObject jsonObject = HttpClient.httpGet(url);
        if(jsonObject!=null) {
            String weixinOpenId = jsonObject.getString("openid");
            if(StringUtil.isNotEmpty(weixinOpenId)) {
                return userMapperEx.getUserByWeixinOpenId(weixinOpenId);
            }
        }
        return null;
    }

    public int weixinBind(String loginName, String password, String weixinCode) throws Exception {
        String weixinUrl = platformConfigService.getPlatformConfigByKey("weixinUrl").getPlatformValue() + BusinessConstants.WEIXIN_LOGIN;
        String weixinAppid = platformConfigService.getPlatformConfigByKey("weixinAppid").getPlatformValue();
        String weixinSecret = platformConfigService.getPlatformConfigByKey("weixinSecret").getPlatformValue();
        String url = weixinUrl + "?appid=" + weixinAppid + "&secret=" + weixinSecret + "&js_code=" + weixinCode
                + "&grant_type=authorization_code";
        JSONObject jsonObject = HttpClient.httpGet(url);
        if(jsonObject!=null) {
            String weixinOpenId = jsonObject.getString("openid");
            if(StringUtil.isNotEmpty(weixinOpenId)) {
                User user = getUserByLoginName(loginName);
                if (user == null || !userPasswordService.matches(password, user.getPassword())) {
                    return 0;
                }
                User update = new User();
                update.setId(user.getId());
                update.setWeixinOpenId(weixinOpenId);
                return userMapper.updateByPrimaryKeySelective(update);
            }
        }
        return 0;
    }
}
