package com.jsh.erp;

import com.jsh.erp.datasource.mappers.OrgaUserRelMapper;
import com.jsh.erp.datasource.mappers.OrgaUserRelMapperEx;
import com.jsh.erp.service.LogService;
import com.jsh.erp.service.OrgaUserRelService;
import com.jsh.erp.service.OrganizationService;
import com.jsh.erp.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrgaUserRelServiceTest {

    @Mock private OrgaUserRelMapper orgaUserRelMapper;
    @Mock private OrgaUserRelMapperEx orgaUserRelMapperEx;
    @Mock private UserService userService;
    @Mock private OrganizationService organizationService;
    @Mock private LogService logService;

    @InjectMocks private OrgaUserRelService orgaUserRelService;

    @Test
    void invalidOrDeletedOrganizationNeverFallsBackToAllUsers() {
        when(organizationService.getOrgIdByParentId(404L)).thenReturn(Collections.emptyList());

        assertTrue(orgaUserRelService.getUserIdListByOrgId(404L).isEmpty());

        verify(orgaUserRelMapper, never()).selectByExample(any());
    }
}
