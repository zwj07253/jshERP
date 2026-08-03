package com.jsh.erp;

import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.Material;
import com.jsh.erp.datasource.entities.Unit;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.MaterialMapperEx;
import com.jsh.erp.datasource.mappers.UnitMapper;
import com.jsh.erp.datasource.mappers.UnitMapperEx;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.service.LogService;
import com.jsh.erp.service.UnitService;
import com.jsh.erp.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnitServiceTest {

    @Mock
    private UnitMapper unitMapper;
    @Mock
    private UnitMapperEx unitMapperEx;
    @Mock
    private UserService userService;
    @Mock
    private LogService logService;
    @Mock
    private MaterialMapperEx materialMapperEx;

    @InjectMocks
    private UnitService unitService;

    @Test
    void rejectsWriteWithoutEditPermission() throws Exception {
        stubUser(false);

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> unitService.insertUnit(validUnit(), null));

        assertEquals(ExceptionConstants.UNIT_PERMISSION_CODE, exception.getCode());
        verify(unitMapper, never()).insertSelective(any());
    }

    @Test
    void insertUsesOnlyAllowedFields() throws Exception {
        stubUser(true);
        JSONObject request = validUnit();
        request.put("id", 999L);
        request.put("tenantId", 888L);
        request.put("deleteFlag", "1");
        request.put("enabled", false);
        request.put("basicUnit", " piece ");
        request.put("ratio", new BigDecimal("12.000"));
        when(unitMapper.selectByExample(any())).thenReturn(Collections.emptyList());
        when(unitMapper.insertSelective(any())).thenReturn(1);

        unitService.insertUnit(request, null);

        ArgumentCaptor<Unit> captor = ArgumentCaptor.forClass(Unit.class);
        verify(unitMapper).insertSelective(captor.capture());
        Unit inserted = captor.getValue();
        assertNull(inserted.getId());
        assertNull(inserted.getTenantId());
        assertNull(inserted.getDeleteFlag());
        assertEquals(true, inserted.getEnabled());
        assertEquals("piece", inserted.getBasicUnit());
        assertEquals("piece/(box=12piece)", inserted.getName());
    }

    @Test
    void rejectsInvalidRatios() throws Exception {
        stubUser(true);
        String[] invalidRatios = {"-1", "0", "1", "1.000", "1.0001"};
        for (String ratio : invalidRatios) {
            JSONObject request = validUnit();
            request.put("ratio", new BigDecimal(ratio));
            BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                    () -> unitService.insertUnit(request, null));
            assertEquals(ExceptionConstants.UNIT_INVALID_CODE, exception.getCode());
        }
        verify(unitMapper, never()).insertSelective(any());
    }

    @Test
    void rejectsDuplicateAndUnpairedOptionalUnits() throws Exception {
        stubUser(true);
        JSONObject duplicate = validUnit();
        duplicate.put("otherUnitTwo", "piece");
        duplicate.put("ratioTwo", 24);
        assertThrows(BusinessRunTimeException.class, () -> unitService.insertUnit(duplicate, null));

        JSONObject unpaired = validUnit();
        unpaired.put("otherUnitTwo", "carton");
        assertThrows(BusinessRunTimeException.class, () -> unitService.insertUnit(unpaired, null));

        JSONObject skipped = validUnit();
        skipped.put("otherUnitThree", "pallet");
        skipped.put("ratioThree", 120);
        assertThrows(BusinessRunTimeException.class, () -> unitService.insertUnit(skipped, null));
    }

    @Test
    void rejectsChangingUnitUsedByMaterial() throws Exception {
        stubUser(true);
        Unit existing = unit(1L);
        when(unitMapper.selectByExample(any())).thenReturn(Collections.singletonList(existing));
        when(materialMapperEx.getMaterialListByUnitIds(Collections.singletonList(1L)))
                .thenReturn(Collections.singletonList(new Material()));
        JSONObject request = validUnit();
        request.put("id", 1L);
        request.put("ratio", 24);

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> unitService.updateUnit(request, null));

        assertEquals(ExceptionConstants.UNIT_IN_USE_CODE, exception.getCode());
        verify(unitMapperEx, never()).updateUnit(any());
    }

    @Test
    void convertsStockQuantityAndPricesUsingMatchedUnitOnly() {
        Unit unit = unit(1L);
        unit.setOtherUnitTwo("carton");
        unit.setRatioTwo(new BigDecimal("24"));

        assertEquals(new BigDecimal("0.500000"),
                unitService.parseStockByUnit(new BigDecimal("12"), unit, "carton"));
        assertEquals(new BigDecimal("288"),
                unitService.parseBasicNumberByUnit(new BigDecimal("12"), unit, "carton"));
        assertEquals(new BigDecimal("240"),
                unitService.parseUnitPriceByUnit(new BigDecimal("10"), unit, "carton"));
        assertEquals(new BigDecimal("10.00"),
                unitService.parseAllPriceByUnit(new BigDecimal("240"), unit, "carton"));
        assertEquals(new BigDecimal("12"),
                unitService.parseStockByUnit(new BigDecimal("12"), unit, "piece"));
    }

    private JSONObject validUnit() {
        JSONObject request = new JSONObject();
        request.put("basicUnit", "piece");
        request.put("otherUnit", "box");
        request.put("ratio", 12);
        return request;
    }

    private void stubUser(boolean allowed) throws Exception {
        User user = new User();
        user.setId(101L);
        user.setTenantId(7L);
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.hasButtonPermission(101L, "/system/unit", "1")).thenReturn(allowed);
    }

    private Unit unit(Long id) {
        Unit unit = new Unit();
        unit.setId(id);
        unit.setName("piece/(box=12piece)");
        unit.setBasicUnit("piece");
        unit.setOtherUnit("box");
        unit.setRatio(new BigDecimal("12"));
        unit.setEnabled(true);
        unit.setDeleteFlag("0");
        return unit;
    }
}
