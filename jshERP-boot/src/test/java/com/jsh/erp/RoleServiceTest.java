package com.jsh.erp;

import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.datasource.entities.Role;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.entities.UserBusiness;
import com.jsh.erp.datasource.entities.Function;
import com.jsh.erp.datasource.mappers.FunctionMapper;
import com.jsh.erp.datasource.mappers.RoleMapper;
import com.jsh.erp.datasource.mappers.RoleMapperEx;
import com.jsh.erp.datasource.mappers.UserBusinessMapper;
import com.jsh.erp.datasource.mappers.UserBusinessMapperEx;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.service.LogService;
import com.jsh.erp.service.RoleService;
import com.jsh.erp.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock private RoleMapper roleMapper;
    @Mock private RoleMapperEx roleMapperEx;
    @Mock private UserBusinessMapperEx userBusinessMapperEx;
    @Mock private UserBusinessMapper userBusinessMapper;
    @Mock private FunctionMapper functionMapper;
    @Mock private LogService logService;
    @Mock private UserService userService;

    @InjectMocks private RoleService roleService;

    @Test
    void rejectsRoleWriteWithoutButtonPermission() throws Exception {
        stubCurrentUser(false);

        assertThrows(BusinessRunTimeException.class,
                () -> roleService.insertRole(validRoleBody(), null));

        verify(roleMapperEx, never()).lockRoleWrite(anyLong());
        verify(roleMapper, never()).insertSelective(any());
    }

    @Test
    void insertUsesWritableFieldsAndCurrentTenant() throws Exception {
        stubCurrentUser(true);
        when(roleMapper.countByExample(any())).thenReturn(0L);
        when(roleMapper.insertSelective(any())).thenReturn(1);
        JSONObject body = validRoleBody();
        body.put("id", 4L);
        body.put("tenantId", 999L);
        body.put("deleteFlag", "1");
        body.put("enabled", false);
        body.put("value", "unexpected");
        body.put("priceLimit", "7,1,1");

        assertEquals(1, roleService.insertRole(body, null));

        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(roleMapper).insertSelective(captor.capture());
        Role saved = captor.getValue();
        assertNull(saved.getId());
        assertEquals(10L, saved.getTenantId());
        assertEquals("0", saved.getDeleteFlag());
        assertTrue(saved.getEnabled());
        assertNull(saved.getValue());
        assertEquals("1,7", saved.getPriceLimit());
    }

    @Test
    void rejectsUnknownDataScopeType() throws Exception {
        stubCurrentUser(true);
        JSONObject body = validRoleBody();
        body.put("type", "未知类型");

        assertThrows(BusinessRunTimeException.class,
                () -> roleService.insertRole(body, null));

        verify(roleMapper, never()).insertSelective(any());
    }

    @Test
    void refusesDeletingAssignedRole() throws Exception {
        stubCurrentUser(true);
        when(roleMapper.selectByExample(any())).thenReturn(Collections.singletonList(activeRole(8L)));
        when(userBusinessMapperEx.getUBKeyIdByTypeAndOneValue("UserRole", "8"))
                .thenReturn(Collections.singletonList(20L));

        assertThrows(BusinessRunTimeException.class,
                () -> roleService.deleteRole(8L, null));

        verify(roleMapperEx, never()).batchDeleteRoleByIds(any(), any(), any());
    }

    @Test
    void protectsSystemAdministratorRole() throws Exception {
        stubCurrentUser(true);

        assertThrows(BusinessRunTimeException.class,
                () -> roleService.deleteRole(4L, null));

        verify(roleMapper, never()).selectByExample(any());
    }

    @Test
    void copiesNumberBasedTemplatePermissionsUsingCurrentMenuIds() throws Exception {
        Role template = activeRole(10L);
        template.setTenantId(null);
        UserBusiness templateFunctions = new UserBusiness();
        templateFunctions.setValue("[n:0001][n:010101]");
        templateFunctions.setBtnStr("[{\"funNumber\":\"010101\",\"btnStr\":\"1,3\"}]");
        Function root = function(101L, "0001");
        Function page = function(305L, "010101");
        when(roleMapper.selectByPrimaryKey(10L)).thenReturn(template);
        when(userBusinessMapperEx.getBasicDataByKeyIdAndType("10", "RoleFunctions"))
                .thenReturn(Collections.singletonList(templateFunctions));
        when(functionMapper.selectByExample(any())).thenReturn(java.util.Arrays.asList(root, page));
        doAnswer(invocation -> {
            ((Role) invocation.getArgument(0)).setId(88L);
            return 1;
        }).when(roleMapper).insertSelective(any());

        roleService.copyTemplateRoleForTenant(10, 20L);

        ArgumentCaptor<UserBusiness> captor = ArgumentCaptor.forClass(UserBusiness.class);
        verify(userBusinessMapper).insertSelective(captor.capture());
        assertEquals("[101][305]", captor.getValue().getValue());
        assertEquals("[{\"funId\":305,\"btnStr\":\"1,3\"}]", captor.getValue().getBtnStr());
    }

    private void stubCurrentUser(boolean canEdit) throws Exception {
        User user = new User();
        user.setId(101L);
        user.setTenantId(10L);
        user.setLoginName("manager");
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasButtonPermission(eq(101L), eq("/system/role"), eq("1"))).thenReturn(canEdit);
    }

    private JSONObject validRoleBody() {
        JSONObject body = new JSONObject();
        body.put("name", "销售角色");
        body.put("type", BusinessConstants.ROLE_TYPE_PUBLIC);
        body.put("description", "test");
        body.put("sort", "1");
        return body;
    }

    private Role activeRole(Long id) {
        Role role = new Role();
        role.setId(id);
        role.setName("role-" + id);
        role.setEnabled(true);
        role.setDeleteFlag(BusinessConstants.DELETE_FLAG_EXISTS);
        return role;
    }

    private Function function(Long id, String number) {
        Function function = new Function();
        function.setId(id);
        function.setNumber(number);
        function.setEnabled(true);
        function.setDeleteFlag(BusinessConstants.DELETE_FLAG_EXISTS);
        return function;
    }
}
