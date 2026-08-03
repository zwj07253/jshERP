package com.jsh.erp;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.datasource.entities.Supplier;
import com.jsh.erp.datasource.entities.Depot;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.entities.UserBusiness;
import com.jsh.erp.datasource.entities.Role;
import com.jsh.erp.datasource.entities.Function;
import com.jsh.erp.datasource.mappers.SupplierMapperEx;
import com.jsh.erp.datasource.mappers.DepotMapper;
import com.jsh.erp.datasource.mappers.UserBusinessMapper;
import com.jsh.erp.datasource.mappers.UserBusinessMapperEx;
import com.jsh.erp.datasource.mappers.RoleMapper;
import com.jsh.erp.datasource.mappers.RoleMapperEx;
import com.jsh.erp.datasource.mappers.FunctionMapper;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.service.LogService;
import com.jsh.erp.service.UserBusinessService;
import com.jsh.erp.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserBusinessServiceTest {

    @Mock private UserBusinessMapper userBusinessMapper;
    @Mock private UserBusinessMapperEx userBusinessMapperEx;
    @Mock private SupplierMapperEx supplierMapperEx;
    @Mock private DepotMapper depotMapper;
    @Mock private LogService logService;
    @Mock private UserService userService;
    @Mock private RoleMapper roleMapper;
    @Mock private RoleMapperEx roleMapperEx;
    @Mock private FunctionMapper functionMapper;

    @InjectMocks private UserBusinessService userBusinessService;

    @Test
    void rejectsMissingCustomerBeforeChangingAssignments() {
        JSONArray userIds = new JSONArray();
        userIds.add(10L);

        assertThrows(BusinessRunTimeException.class,
                () -> userBusinessService.updateOneValueByKeyIdAndType("UserCustomer", userIds, "20"));

        verify(userBusinessMapper, never()).insertSelective(any());
        verify(userBusinessMapper, never()).updateByPrimaryKeySelective(any());
    }

    @Test
    void rejectsUpdateThatTargetsAnotherRelation() throws Exception {
        UserBusiness existing = relation(1L, "UserDepot", "10", "[30]");
        when(userBusinessMapper.selectByPrimaryKey(1L)).thenReturn(existing);

        JSONObject body = new JSONObject();
        body.put("id", 1L);
        body.put("type", "UserCustomer");
        body.put("keyId", "10");
        body.put("value", "[20]");

        assertThrows(BusinessRunTimeException.class,
                () -> userBusinessService.updateUserBusiness(body, null));

        verify(userBusinessMapper, never()).updateByPrimaryKeySelective(any());
    }

    @Test
    void assignsValidCustomerToValidUser() throws Exception {
        User user = new User();
        user.setId(10L);
        Supplier customer = new Supplier();
        customer.setId(20L);
        customer.setType("客户");
        customer.setEnabled(true);
        when(userService.getUser(10L)).thenReturn(user);
        when(supplierMapperEx.getInfoById(20L)).thenReturn(customer);
        when(userBusinessMapperEx.getOldListByType("UserCustomer")).thenReturn(Collections.emptyList());
        when(userBusinessMapperEx.getBasicDataByKeyIdAndType("10", "UserCustomer"))
                .thenReturn(Collections.emptyList());

        JSONArray userIds = new JSONArray();
        userIds.add(10L);

        assertEquals(1, userBusinessService.updateOneValueByKeyIdAndType("UserCustomer", userIds, "20"));

        ArgumentCaptor<UserBusiness> captor = ArgumentCaptor.forClass(UserBusiness.class);
        verify(userBusinessMapper).insertSelective(captor.capture());
        assertEquals("UserCustomer", captor.getValue().getType());
        assertEquals("10", captor.getValue().getKeyId());
        assertEquals("[20]", captor.getValue().getValue());
    }

    @Test
    void assignsValidDepotToValidUser() throws Exception {
        User user = new User();
        user.setId(10L);
        Depot depot = new Depot();
        depot.setId(30L);
        depot.setEnabled(true);
        depot.setDeleteFlag("0");
        when(userService.getUser(10L)).thenReturn(user);
        when(depotMapper.selectByPrimaryKey(30L)).thenReturn(depot);
        when(userBusinessMapperEx.getOldListByType("UserDepot")).thenReturn(Collections.emptyList());
        when(userBusinessMapperEx.getBasicDataByKeyIdAndType("10", "UserDepot"))
                .thenReturn(Collections.emptyList());
        JSONArray userIds = new JSONArray();
        userIds.add(10L);

        assertEquals(1, userBusinessService.updateOneValueByKeyIdAndType("UserDepot", userIds, "30"));

        ArgumentCaptor<UserBusiness> captor = ArgumentCaptor.forClass(UserBusiness.class);
        verify(userBusinessMapper).insertSelective(captor.capture());
        assertEquals("UserDepot", captor.getValue().getType());
        assertEquals("[30]", captor.getValue().getValue());
    }

    @Test
    void rejectsRoleMenuBeyondCurrentUserPermission() throws Exception {
        User currentUser = new User();
        currentUser.setId(10L);
        currentUser.setLoginName("manager");
        when(userService.getCurrentUser()).thenReturn(currentUser);
        Role currentRole = activeRole(5L);
        when(userService.getRoleTypeByUserId(10L)).thenReturn(currentRole);
        when(roleMapper.selectByPrimaryKey(8L)).thenReturn(activeRole(8L));
        when(userBusinessMapperEx.getBasicDataByKeyIdAndType("5", "RoleFunctions"))
                .thenReturn(Collections.singletonList(relation(20L, "RoleFunctions", "5", "[10]")));
        Function function = new Function();
        function.setId(11L);
        function.setEnabled(true);
        function.setDeleteFlag("0");
        when(functionMapper.selectByPrimaryKey(11L)).thenReturn(function);
        JSONObject body = new JSONObject();
        body.put("type", "RoleFunctions");
        body.put("keyId", "8");
        body.put("value", "[11]");

        assertThrows(BusinessRunTimeException.class,
                () -> userBusinessService.insertUserBusiness(body, null));

        verify(userBusinessMapper, never()).insertSelective(any());
    }

    @Test
    void rejectsButtonNotSupportedByFunction() throws Exception {
        User admin = new User();
        admin.setLoginName("admin");
        when(userService.getCurrentUser()).thenReturn(admin);
        when(roleMapper.selectByPrimaryKey(8L)).thenReturn(activeRole(8L));
        when(userBusinessMapperEx.getBasicDataByKeyIdAndType("8", "RoleFunctions"))
                .thenReturn(Collections.singletonList(relation(21L, "RoleFunctions", "8", "[10]")));
        Function function = new Function();
        function.setId(10L);
        function.setEnabled(true);
        function.setDeleteFlag("0");
        function.setPushBtn("1");
        when(functionMapper.selectByPrimaryKey(10L)).thenReturn(function);

        assertThrows(BusinessRunTimeException.class,
                () -> userBusinessService.updateBtnStr("8", "RoleFunctions",
                        "[{\"funId\":10,\"btnStr\":\"2\"}]"));

        verify(userBusinessMapper, never()).updateByExampleSelective(any(), any());
    }

    private UserBusiness relation(Long id, String type, String keyId, String value) {
        UserBusiness relation = new UserBusiness();
        relation.setId(id);
        relation.setType(type);
        relation.setKeyId(keyId);
        relation.setValue(value);
        return relation;
    }

    private Role activeRole(Long id) {
        Role role = new Role();
        role.setId(id);
        role.setEnabled(true);
        role.setDeleteFlag("0");
        return role;
    }
}
