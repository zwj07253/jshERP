package com.jsh.erp;

import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.datasource.entities.Function;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.FunctionMapper;
import com.jsh.erp.datasource.mappers.FunctionMapperEx;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.service.FunctionService;
import com.jsh.erp.service.LogService;
import com.jsh.erp.service.SystemConfigService;
import com.jsh.erp.service.UserBusinessService;
import com.jsh.erp.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FunctionServiceTest {

    @Mock private FunctionMapper functionMapper;
    @Mock private FunctionMapperEx functionMapperEx;
    @Mock private UserService userService;
    @Mock private UserBusinessService userBusinessService;
    @Mock private SystemConfigService systemConfigService;
    @Mock private LogService logService;

    @InjectMocks private FunctionService functionService;

    // === 循环引用检测 ===

    @Test
    void rejectsDirectSelfReference() throws Exception {
        stubAdminUser();
        // number=0101, parentNumber=0101 -> 自己引用自己
        Function existing = function("0101", "0", 1L);
        when(functionMapper.selectByExample(any())).thenReturn(Collections.singletonList(existing));

        JSONObject body = functionBody("0101", "0101", "测试菜单");
        assertThrows(BusinessRunTimeException.class,
                () -> functionService.insertFunction(body, null));
    }

    @Test
    void rejectsIndirectCircularReference() throws Exception {
        stubAdminUser();
        // A -> B -> C -> A 的循环
        // 编辑 A(01), 设置 parentNumber = C(010201)
        when(functionMapper.selectByPrimaryKey(1L)).thenReturn(function("01", "0", 1L));
        // 校验 parentNumber=010201
        Function c = function("010201", "0102", 3L);
        Function b = function("0102", "01", 2L);
        Function a = function("01", "0", 1L);
        // getByNumber("010201") -> c
        // getByNumber("0102") -> b
        // getByNumber("01") -> a -> 检测到循环
        when(functionMapper.selectByExample(any()))
                .thenReturn(Collections.singletonList(c))
                .thenReturn(Collections.singletonList(b))
                .thenReturn(Collections.singletonList(a));

        JSONObject body = functionBody("01", "010201", "A菜单");
        body.put("id", 1L);
        assertThrows(BusinessRunTimeException.class,
                () -> functionService.updateFunction(body, null));
    }

    @Test
    void rejectsMissingParent() throws Exception {
        stubAdminUser();
        // parentNumber=9999 不存在
        when(functionMapper.selectByExample(any())).thenReturn(Collections.emptyList());

        JSONObject body = functionBody("0101", "9999", "测试菜单");
        assertThrows(BusinessRunTimeException.class,
                () -> functionService.insertFunction(body, null));
    }

    @Test
    void allowsValidParent() throws Exception {
        stubAdminUser();
        Function parent = function("0001", "0", 10L);
        // getByNumber("0001") -> parent, parent.parentNumber="0" -> 结束
        when(functionMapper.selectByExample(any())).thenReturn(Collections.singletonList(parent));
        when(functionMapper.insertSelective(any())).thenReturn(1);

        JSONObject body = functionBody("000101", "0001", "子菜单");
        assertEquals(1, functionService.insertFunction(body, null));
    }

    // === number 不可修改 ===

    @Test
    void rejectsNumberChangeOnUpdate() throws Exception {
        stubAdminUser();
        Function existing = function("0101", "0", 1L);
        when(functionMapper.selectByPrimaryKey(1L)).thenReturn(existing);

        JSONObject body = functionBody("0201", "0", "测试菜单");
        body.put("id", 1L);
        assertThrows(BusinessRunTimeException.class,
                () -> functionService.updateFunction(body, null));

        verify(functionMapper, never()).updateByPrimaryKeySelective(any());
    }

    @Test
    void allowsUpdateWithoutNumberChange() throws Exception {
        stubAdminUser();
        Function existing = function("0101", "0", 1L);
        when(functionMapper.selectByPrimaryKey(1L)).thenReturn(existing);
        // parentNumber="0" 不需要校验父菜单
        when(functionMapper.updateByPrimaryKeySelective(any())).thenReturn(1);

        JSONObject body = functionBody("0101", "0", "新名称");
        body.put("id", 1L);
        assertEquals(1, functionService.updateFunction(body, null));
    }

    // === 删除保护 ===

    @Test
    void rejectsDeleteWithChildren() throws Exception {
        stubAdminUser();
        Function parent = function("0001", "0", 1L);
        when(functionMapper.selectByExample(any()))
                .thenReturn(Collections.singletonList(parent))  // getFunctionListByIds
                .thenReturn(Collections.singletonList(function("000101", "0001", 2L))); // hasChildren

        assertThrows(BusinessRunTimeException.class,
                () -> functionService.deleteFunction(1L, null));

        verify(functionMapperEx, never()).batchDeleteFunctionByIds(any(), any(), any());
    }

    @Test
    void rejectsDeleteWithRoleReference() throws Exception {
        stubAdminUser();
        Function func = function("0001", "0", 1L);
        when(functionMapper.selectByExample(any()))
                .thenReturn(Collections.singletonList(func))  // getFunctionListByIds
                .thenReturn(Collections.emptyList()); // hasChildren -> empty
        // isReferencedByRoleFunctions -> has references
        when(userBusinessService.getUBKeyIdByTypeAndOneValue("RoleFunctions", "1"))
                .thenReturn(Collections.singletonList(5L));

        assertThrows(BusinessRunTimeException.class,
                () -> functionService.deleteFunction(1L, null));

        verify(functionMapperEx, never()).batchDeleteFunctionByIds(any(), any(), any());
    }

    @Test
    void allowsDeleteWithoutChildrenOrReferences() throws Exception {
        stubAdminUser();
        // batchDeleteFunctionByIds 访问 RequestContextHolder 做日志
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(new MockHttpServletRequest()));
        Function func = function("0001", "0", 1L);
        when(functionMapper.selectByExample(any()))
                .thenReturn(Collections.singletonList(func))  // getFunctionListByIds
                .thenReturn(Collections.emptyList()); // hasChildren -> empty
        // isReferencedByRoleFunctions -> empty
        when(userBusinessService.getUBKeyIdByTypeAndOneValue("RoleFunctions", "1"))
                .thenReturn(Collections.emptyList());
        when(functionMapperEx.batchDeleteFunctionByIds(any(), any(), any())).thenReturn(1);

        assertEquals(1, functionService.deleteFunction(1L, null));
    }

    // === helpers ===

    private void stubAdminUser() throws Exception {
        User admin = new User();
        admin.setId(1L);
        admin.setLoginName(BusinessConstants.DEFAULT_MANAGER);
        when(userService.getCurrentUser()).thenReturn(admin);
    }

    private Function function(String number, String parentNumber, Long id) {
        Function f = new Function();
        f.setId(id);
        f.setNumber(number);
        f.setParentNumber(parentNumber);
        f.setEnabled(true);
        f.setDeleteFlag(BusinessConstants.DELETE_FLAG_EXISTS);
        f.setName("菜单" + number);
        return f;
    }

    private JSONObject functionBody(String number, String parentNumber, String name) {
        JSONObject body = new JSONObject();
        body.put("number", number);
        body.put("parentNumber", parentNumber);
        body.put("name", name);
        body.put("url", "/test/" + number);
        body.put("component", "/test/TestList");
        body.put("sort", "1");
        body.put("icon", "setting");
        body.put("pushBtn", "1,2,3");
        return body;
    }
}
