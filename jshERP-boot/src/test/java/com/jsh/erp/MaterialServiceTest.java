package com.jsh.erp;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.DepotItem;
import com.jsh.erp.datasource.entities.Material;
import com.jsh.erp.datasource.entities.MaterialExample;
import com.jsh.erp.datasource.entities.MaterialWithInitStock;
import com.jsh.erp.datasource.entities.Unit;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.DepotItemMapperEx;
import com.jsh.erp.datasource.mappers.MaterialMapper;
import com.jsh.erp.datasource.mappers.MaterialMapperEx;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.service.MaterialService;
import com.jsh.erp.service.MaterialExtendService;
import com.jsh.erp.service.MaterialCategoryService;
import com.jsh.erp.service.LogService;
import com.jsh.erp.service.UnitService;
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
    @Mock
    private UnitService unitService;
    @Mock
    private DepotItemMapperEx depotItemMapperEx;
    @Mock
    private MaterialCategoryService materialCategoryService;

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
    void rejectsExportWithoutExportPermission() throws Exception {
        User user = new User();
        user.setId(101L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasButtonPermission(101L, "/material/material", "3")).thenReturn(false);

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> materialService.exportExcel(null, null, null, null, null, null, null,
                        null, null, null, null, new org.springframework.mock.web.MockHttpServletRequest(), null));

        assertEquals(ExceptionConstants.MATERIAL_EXPORT_PERMISSION_CODE, exception.getCode());
        verify(materialMapperEx, never()).exportExcel(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void rejectsMissingMaterialCategory() throws Exception {
        stubCurrentUser(true);
        JSONObject body = new JSONObject();
        body.put("name", "商品");
        body.put("categoryId", 999L);
        body.put("sortList", "[]");
        when(materialCategoryService.getMaterialCategory(999L)).thenReturn(null);

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> materialService.insertMaterial(body, null));

        assertEquals(ExceptionConstants.MATERIAL_CATEGORY_REFERENCE_INVALID_CODE, exception.getCode());
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
        body.put("categoryId", 0L);
        body.put("sortList", "[]");

        materialService.insertMaterial(body, null);

        ArgumentCaptor<Material> captor = ArgumentCaptor.forClass(Material.class);
        verify(materialMapperEx).insertSelectiveEx(captor.capture());
        Material inserted = captor.getValue();
        assertNull(inserted.getId());
        assertNull(inserted.getTenantId());
        assertNull(inserted.getDeleteFlag());
        assertNull(inserted.getCategoryId());
        assertEquals(true, inserted.getEnabled());
    }

    @Test
    void batchUpdateClearsLegacyZeroCategory() throws Exception {
        stubCurrentUser(true);
        when(materialMapper.updateByExampleSelective(any(), any())).thenReturn(1);
        when(materialMapperEx.batchSetCategoryIdToNull(Collections.singletonList(1L))).thenReturn(1);
        JSONObject requested = new JSONObject();
        requested.put("categoryId", 0L);
        JSONObject body = new JSONObject();
        body.put("ids", "1");
        body.put("material", requested.toJSONString());

        assertEquals(1, materialService.batchUpdate(body));

        verify(materialMapperEx).batchSetCategoryIdToNull(Collections.singletonList(1L));
        verify(materialCategoryService, never()).getMaterialCategory(anyLong());
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

    @Test
    void rejectsMultiAttributeMaterialUsingMultiUnit() throws Exception {
        stubCurrentUser(true);
        JSONObject body = multiUnitBody();
        body.put("manySku", new JSONArray(Collections.singletonList(new JSONObject())));

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> materialService.insertMaterial(body, null));

        assertEquals(ExceptionConstants.MATERIAL_UNIT_CONFIG_INVALID_CODE, exception.getCode());
        verify(materialMapperEx, never()).insertSelectiveEx(any());
    }

    @Test
    void rejectsDisabledMultiUnitAndUnknownBarcodeUnit() throws Exception {
        stubCurrentUser(true);
        Unit disabled = multiUnit();
        disabled.setEnabled(false);
        when(unitService.getUnit(2L)).thenReturn(disabled);
        BusinessRunTimeException disabledException = assertThrows(BusinessRunTimeException.class,
                () -> materialService.insertMaterial(multiUnitBody(), null));
        assertEquals(ExceptionConstants.MATERIAL_UNIT_CONFIG_INVALID_CODE, disabledException.getCode());

        Unit enabled = multiUnit();
        when(unitService.getUnit(2L)).thenReturn(enabled);
        JSONObject invalidBarcode = multiUnitBody();
        JSONArray details = invalidBarcode.getJSONArray("meList");
        details.getJSONObject(1).put("commodityUnit", "pallet");
        BusinessRunTimeException barcodeException = assertThrows(BusinessRunTimeException.class,
                () -> materialService.insertMaterial(invalidBarcode, null));
        assertEquals(ExceptionConstants.MATERIAL_UNIT_CONFIG_INVALID_CODE, barcodeException.getCode());
    }

    @Test
    void rejectsChangingUnitAfterDocumentHistoryExists() throws Exception {
        stubCurrentUser(true);
        Material existing = new Material();
        existing.setId(9L);
        existing.setUnitId(1L);
        existing.setUnit("");
        when(materialMapper.selectByExample(any())).thenReturn(Collections.singletonList(existing));
        when(depotItemMapperEx.getDepotItemListListByMaterialIds(Collections.singletonList(9L)))
                .thenReturn(Collections.singletonList(new DepotItem()));
        JSONObject body = multiUnitBody();
        body.put("id", 9L);

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> materialService.updateMaterial(body, null));

        assertEquals(ExceptionConstants.MATERIAL_UNIT_HISTORY_LOCK_CODE, exception.getCode());
        verify(materialMapper, never()).updateByPrimaryKeySelective(any());
    }

    private JSONObject multiUnitBody() {
        JSONObject body = new JSONObject();
        body.put("name", "multi-unit material");
        body.put("unitId", 2L);
        body.put("sortList", "[]");
        JSONArray details = new JSONArray();
        JSONObject basic = new JSONObject();
        basic.put("commodityUnit", "piece");
        details.add(basic);
        JSONObject other = new JSONObject();
        other.put("commodityUnit", "box");
        details.add(other);
        body.put("meList", details);
        return body;
    }

    private Unit multiUnit() {
        Unit unit = new Unit();
        unit.setId(2L);
        unit.setBasicUnit("piece");
        unit.setOtherUnit("box");
        unit.setRatio(new java.math.BigDecimal("12"));
        unit.setEnabled(true);
        return unit;
    }

    private void stubCurrentUser(boolean allowed) throws Exception {
        User user = new User();
        user.setId(101L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasButtonPermission(101L, "/material/material", "1")).thenReturn(allowed);
    }
}
