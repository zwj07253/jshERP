package com.jsh.erp.filter;

import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.UserMapper;
import com.jsh.erp.service.RedisService;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter(filterName = "LogCostFilter", urlPatterns = {"/*"},
        initParams = {@WebInitParam(name = "filterPath",
                      value = "/jshERP-boot/platformConfig/getPlatform#/jshERP-boot/v2/api-docs#/jshERP-boot/v3/api-docs#/jshERP-boot/swagger-ui#/jshERP-boot/webjars#" +
                              "/jshERP-boot/systemConfig/static#/jshERP-boot/api/plugin/wechat/weChat/share#" +
                              "/jshERP-boot/api/plugin/general-ledger/pdf/voucher#/jshERP-boot/api/plugin/tenant-statistics/tenantClean")})
public class LogCostFilter implements Filter {

    private static final String FILTER_PATH = "filterPath";

    private String[] allowUrls;
    @Resource
    private RedisService redisService;
    @Resource
    private UserMapper userMapper;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        String filterPath = filterConfig.getInitParameter(FILTER_PATH);
        if (!StringUtils.isEmpty(filterPath)) {
            allowUrls = filterPath.contains("#") ? filterPath.split("#") : new String[]{filterPath};
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest servletRequest = (HttpServletRequest) request;
        HttpServletResponse servletResponse = (HttpServletResponse) response;
        String requestUrl = servletRequest.getRequestURI();
        if(requestUrl.contains("..") || requestUrl.contains("%2e") || requestUrl.contains("%2E")) {
            servletResponse.setStatus(500);
            servletResponse.getWriter().write("loginOut");
            return;
        }
        //具体，比如：处理若用户未登录，则跳转到登录页
        Object userId = redisService.getObjectFromSessionByKey(servletRequest,"userId");
        if(userId!=null) {
            try {
                User user = userMapper.selectByPrimaryKey(Long.parseLong(userId.toString()));
                if (user != null && user.getStatus() != null && user.getStatus() == BusinessConstants.USER_STATUS_NORMAL
                        && !BusinessConstants.DELETE_FLAG_DELETED.equals(user.getDeleteFlag())) {
                    chain.doFilter(request, response);
                    return;
                }
            } catch (RuntimeException e) {
                // Invalid or stale sessions fail closed.
            }
            redisService.deleteObjectBySession(servletRequest, "userId");
        }
        if (requestUrl.equals("/jshERP-boot/doc.html") || requestUrl.equals("/jshERP-boot/user/login")
                || requestUrl.equals("/jshERP-boot/user/register") || requestUrl.equals("/jshERP-boot/user/weixinLogin")
                || requestUrl.equals("/jshERP-boot/user/weixinBind") || requestUrl.equals("/jshERP-boot/user/registerUser")
                || requestUrl.equals("/jshERP-boot/user/randomImage")) {
            chain.doFilter(request, response);
            return;
        }
        if (null != allowUrls && allowUrls.length > 0) {
            for (String url : allowUrls) {
                if (requestUrl.startsWith(url)) {
                    chain.doFilter(request, response);
                    return;
                }
            }
        }
        servletResponse.setStatus(500);
        if(!requestUrl.equals("/jshERP-boot/user/logout") && !requestUrl.equals("/jshERP-boot/function/findMenuByPNumber")) {
            servletResponse.getWriter().write("loginOut");
        }
    }

    @Override
    public void destroy() {

    }
}
