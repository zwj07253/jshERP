package com.jsh.erp;

import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.Material;
import com.jsh.erp.datasource.entities.MaterialExample;
import com.jsh.erp.datasource.entities.MaterialWithInitStock;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.MaterialMapper;
import com.jsh.erp.datasource.mappers.MaterialMapperEx;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.service.MaterialService;
import com.jsh.erp.service.MaterialExtendService;
import com.jsh.erp.service.LogService;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialServiceTest {

    @Mock
    private MaterialMapper materialMapper;
    @Mock
    private MaterialMapperEx materialMapperEx;
    @Mock
    private UserService userService;
    @Mock
    private MaterialExtendService materialExtendService;
    @Mock
    private LogService logService;

    @InjectMocks
    private MaterialService materialService;

    @Test
    void rejectsWriteWithoutEditPermission() throws Exception {
        stubCurrentUser(false);

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> materialService.insertMaterial(new JSONObject(), null));

        assertEquals(ExceptionConstants.MATERIAL_PERMISSION_CODE, exception.getCode());
        verify(materialMapperEx, never()).insertSelectiveEx(any());
    }

    @Test
    void batchUpdateOnlyCopiesAllowedBusinessFields() throws Exception {
        stubCurrentUser(true);
        when(materialMapper.updateByExampleSelective(any(), any())).thenReturn(1);
        JSONObject requested = new JSONObject();
        requested.put("color", "red");
        requested.put("name", "must-not-change");
        requested.put("id", 999L);
        requested.put("tenantId", 888L);
        requested.put("deleteFlag", "1");
        JSONObject body = new JSONObject();
        body.put("ids", "1");
        body.put("material", requested.toJSONString());

        materialService.batchUpdate(body);

        ArgumentCaptor<Material> captor = ArgumentCaptor.forClass(Material.class);
        verify(materialMapper).updateByExampleSelective(captor.capture(), any(MaterialExample.class));
        Material update = captor.getValue();
        assertEquals("red", update.getColor());
        assertNull(update.getName());
        assertNull(update.getId());
        assertNull(update.getTenantId());
        assertNull(update.getDeleteFlag());
    }

    @Test
    void insertIgnoresProtectedEntityFields() throws Exception {
        stubCurrentUser(true);
        JSONObject body = new JSONObject();
        body.put("name", "商品");
        body.put("id", 999L);
        body.put("tenantId", 888L);
        body.put("deleteFlag", "1");
        body.put("enabled", false);
        body.put("sortList", "[]");

        materialService.insertMaterial(body, null);

        ArgumentCaptor<Material> captor = ArgumentCaptor.forClass(Material.class);
        verify(materialMapperEx).insertSelectiveEx(captor.capture());
        Material inserted = captor.getValue();
        assertNull(inserted.getId());
        assertNull(inserted.getTenantId());
        assertNull(inserted.getDeleteFlag());
        assertEquals(true, inserted.getEnabled());
    }

    @Test
    void detectsDuplicateSkuInsideImportFile() {
        MaterialWithInitStock existing = new MaterialWithInitStock();
        existing.setName("商品");
        existing.setStandard("规格");
        existing.setModel("型号");
        existing.setColor("红");
        existing.setUnit("个");
        JSONObject basic = new JSONObject();
        basic.put("sku", "SKU-1");
        JSONObject extend = new JSONObject();
        extend.put("basic", basic);
        existing.setMaterialExObj(extend);

        assertThrows(BusinessRunTimeException.class, () -> materialService.batchCheckExistMaterialListByParam(
                Collections.singletonList(existing), "商品", "规格", "型号", "红", "个", "SKU-1"));
    }

    private void stubCurrentUser(boolean allowed) throws Exception {
        User user = new User();
        user.setId(101L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasButtonPermission(101L, "/material/material", "1")).thenReturn(allowed);
    }
}
