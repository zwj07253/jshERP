package com.jsh.erp;

import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.datasource.entities.Organization;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.OrgaUserRelMapper;
import com.jsh.erp.datasource.mappers.OrganizationMapper;
import com.jsh.erp.datasource.mappers.OrganizationMapperEx;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.service.LogService;
import com.jsh.erp.service.OrganizationService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock private OrganizationMapper organizationMapper;
    @Mock private OrganizationMapperEx organizationMapperEx;
    @Mock private OrgaUserRelMapper orgaUserRelMapper;
    @Mock private UserService userService;
    @Mock private LogService logService;

    @InjectMocks private OrganizationService organizationService;

    @Test
    void rejectsWriteWithoutDepartmentPermission() throws Exception {
        stubCurrentUser(false);

        assertThrows(BusinessRunTimeException.class,
                () -> organizationService.insertOrganization(validBody(), null));

        verify(organizationMapperEx, never()).lockOrganizationWrite(anyLong());
        verify(organizationMapper, never()).insertSelective(any());
    }

    @Test
    void insertUsesOnlyWritableFieldsAndCurrentTenant() throws Exception {
        stubCurrentUser(true);
        when(organizationMapper.countByExample(any())).thenReturn(0L);
        when(organizationMapper.insertSelective(any())).thenReturn(1);
        JSONObject body = validBody();
        body.put("id", 99L);
        body.put("tenantId", 999L);
        body.put("deleteFlag", "1");

        assertEquals(1, organizationService.insertOrganization(body, null));

        ArgumentCaptor<Organization> captor = ArgumentCaptor.forClass(Organization.class);
        verify(organizationMapper).insertSelective(captor.capture());
        Organization saved = captor.getValue();
        assertNull(saved.getId());
        assertEquals(10L, saved.getTenantId());
        assertEquals(BusinessConstants.DELETE_FLAG_EXISTS, saved.getDeleteFlag());
        assertEquals("研发部", saved.getOrgAbr());
    }

    @Test
    void rejectsMovingDepartmentUnderItsDescendant() throws Exception {
        stubCurrentUser(true);
        Organization current = organization(10L, null);
        Organization child = organization(11L, 10L);
        when(organizationMapper.selectByPrimaryKey(10L)).thenReturn(current);
        when(organizationMapper.selectByPrimaryKey(11L)).thenReturn(child);
        JSONObject body = validBody();
        body.put("id", 10L);
        body.put("parentId", 11L);

        assertThrows(BusinessRunTimeException.class,
                () -> organizationService.updateOrganization(body, null));

        verify(organizationMapperEx, never()).editOrganization(any());
    }

    @Test
    void refusesDeletingDepartmentThatStillHasUsers() throws Exception {
        stubCurrentUser(true);
        Organization organization = organization(10L, null);
        when(organizationMapper.selectByExample(any())).thenReturn(Collections.singletonList(organization));
        when(organizationMapperEx.getOrganizationByParentIds(any())).thenReturn(Collections.emptyList());
        when(orgaUserRelMapper.countByExample(any())).thenReturn(1);

        assertThrows(BusinessRunTimeException.class,
                () -> organizationService.deleteOrganization(10L, null));

        verify(organizationMapperEx, never()).batchDeleteOrganizationByIds(any(), any(), any());
    }

    private void stubCurrentUser(boolean canEdit) throws Exception {
        User user = new User();
        user.setId(101L);
        user.setTenantId(10L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasButtonPermission(eq(101L), eq("/system/organization"), eq("1")))
                .thenReturn(canEdit);
    }

    private JSONObject validBody() {
        JSONObject body = new JSONObject();
        body.put("orgAbr", "研发部");
        body.put("orgNo", "RD");
        body.put("sort", "1");
        return body;
    }

    private Organization organization(Long id, Long parentId) {
        Organization organization = new Organization();
        organization.setId(id);
        organization.setParentId(parentId);
        organization.setOrgAbr("部门-" + id);
        organization.setOrgNo("ORG-" + id);
        organization.setTenantId(10L);
        organization.setDeleteFlag(BusinessConstants.DELETE_FLAG_EXISTS);
        return organization;
    }
}
