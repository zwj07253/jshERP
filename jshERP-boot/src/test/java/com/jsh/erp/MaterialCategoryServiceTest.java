package com.jsh.erp;

import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.MaterialCategory;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.MaterialCategoryMapper;
import com.jsh.erp.datasource.mappers.MaterialCategoryMapperEx;
import com.jsh.erp.datasource.mappers.MaterialMapperEx;
import com.jsh.erp.datasource.vo.TreeNode;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.service.LogService;
import com.jsh.erp.service.MaterialCategoryService;
import com.jsh.erp.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialCategoryServiceTest {

    @Mock
    private MaterialCategoryMapper materialCategoryMapper;
    @Mock
    private MaterialCategoryMapperEx materialCategoryMapperEx;
    @Mock
    private UserService userService;
    @Mock
    private LogService logService;
    @Mock
    private MaterialMapperEx materialMapperEx;

    @InjectMocks
    private MaterialCategoryService materialCategoryService;

    @Test
    void rejectsWriteWithoutEditPermission() throws Exception {
        stubCurrentUser();
        when(userService.hasButtonPermission(101L, "/material/material_category", "1")).thenReturn(false);

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> materialCategoryService.insertMaterialCategory(validBody(), null));

        assertEquals(ExceptionConstants.MATERIAL_CATEGORY_PERMISSION_CODE, exception.getCode());
        verify(materialCategoryMapper, never()).insertSelective(any());
    }

    @Test
    void insertIgnoresProtectedEntityFieldsAndNormalizesRootParent() throws Exception {
        allowEdit();
        when(materialCategoryMapperEx.getActiveMaterialCategoryList()).thenReturn(Collections.emptyList());
        when(materialCategoryMapperEx.getMaterialCategoryBySerialNo("CAT-1", null))
                .thenReturn(Collections.emptyList());
        when(materialCategoryMapper.insertSelective(any())).thenReturn(1);
        JSONObject body = validBody();
        body.put("id", 999L);
        body.put("tenantId", 888L);
        body.put("deleteFlag", "1");
        body.put("parentId", 0L);

        materialCategoryService.insertMaterialCategory(body, null);

        ArgumentCaptor<MaterialCategory> captor = ArgumentCaptor.forClass(MaterialCategory.class);
        verify(materialCategoryMapper).insertSelective(captor.capture());
        MaterialCategory inserted = captor.getValue();
        assertNull(inserted.getId());
        assertNull(inserted.getTenantId());
        assertNull(inserted.getDeleteFlag());
        assertNull(inserted.getParentId());
    }

    @Test
    void rejectsMovingCategoryBelowItsDescendant() throws Exception {
        allowEdit();
        MaterialCategory parent = category(2L, null, "父类别", "P");
        MaterialCategory child = category(3L, 2L, "子类别", "C");
        when(materialCategoryMapperEx.getActiveMaterialCategoryList()).thenReturn(Arrays.asList(parent, child));
        JSONObject body = validBody();
        body.put("id", 2L);
        body.put("name", "父类别");
        body.put("serialNo", "P");
        body.put("parentId", 3L);

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> materialCategoryService.updateMaterialCategory(body, null));

        assertEquals(ExceptionConstants.MATERIAL_CATEGORY_CYCLE_CODE, exception.getCode());
    }

    @Test
    void rejectsDuplicateNameUnderSameParent() throws Exception {
        allowEdit();
        when(materialCategoryMapperEx.getActiveMaterialCategoryList()).thenReturn(Collections.singletonList(
                category(2L, null, "测试类别", "OTHER")));

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> materialCategoryService.insertMaterialCategory(validBody(), null));

        assertEquals(ExceptionConstants.MATERIAL_CATEGORY_NAME_ALREADY_EXISTS_CODE, exception.getCode());
    }

    @Test
    void rejectsDuplicateSerialNumber() throws Exception {
        allowEdit();
        when(materialCategoryMapperEx.getActiveMaterialCategoryList()).thenReturn(Collections.emptyList());
        when(materialCategoryMapperEx.getMaterialCategoryBySerialNo("CAT-1", null))
                .thenReturn(Collections.singletonList(category(2L, null, "其他类别", "CAT-1")));

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> materialCategoryService.insertMaterialCategory(validBody(), null));

        assertEquals(ExceptionConstants.MATERIAL_CATEGORY_SERIAL_ALREADY_EXISTS_CODE, exception.getCode());
    }

    @Test
    void rejectsEditingRootCategory() throws Exception {
        allowEdit();
        JSONObject body = validBody();
        body.put("id", 1L);

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> materialCategoryService.updateMaterialCategory(body, null));

        assertEquals(ExceptionConstants.MATERIAL_CATEGORY_ROOT_NOT_SUPPORT_EDIT_CODE, exception.getCode());
    }

    @Test
    void rejectsDeletingProtectedRootCategory() throws Exception {
        allowEdit();

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> materialCategoryService.batchDeleteMaterialCategoryByIds("1"));

        assertEquals(ExceptionConstants.MATERIAL_CATEGORY_ROOT_NOT_SUPPORT_DELETE_CODE, exception.getCode());
    }

    @Test
    void buildsTreeWithOneFlatCategoryQueryAndExcludesEditedSubtree() throws Exception {
        List<MaterialCategory> categories = Arrays.asList(
                category(1L, null, "根", "ROOT"),
                category(2L, 1L, "一级", "A"),
                category(3L, 2L, "二级", "B"),
                category(4L, 1L, "同级", "C"));
        when(materialCategoryMapperEx.getActiveMaterialCategoryList()).thenReturn(categories);

        List<TreeNode> fullTree = materialCategoryService.getMaterialCategoryTree(0L);
        List<TreeNode> editTree = materialCategoryService.getMaterialCategoryTree(2L);

        assertEquals(1, fullTree.size());
        assertEquals(2, fullTree.get(0).getChildren().size());
        assertEquals(1, editTree.size());
        assertEquals(1, editTree.get(0).getChildren().size());
        assertEquals(4L, editTree.get(0).getChildren().get(0).getId());
        verify(materialCategoryMapperEx, org.mockito.Mockito.times(2)).getActiveMaterialCategoryList();
    }

    @Test
    void rejectsAmbiguousCategoryNameDuringImportLookup() throws Exception {
        when(materialCategoryMapperEx.getActiveMaterialCategoryList()).thenReturn(Arrays.asList(
                category(2L, 1L, "配件", "A"),
                category(3L, 4L, "配件", "B")));

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> materialCategoryService.getCategoryIdByName("配件"));

        assertEquals(ExceptionConstants.MATERIAL_CATEGORY_NAME_AMBIGUOUS_CODE, exception.getCode());
    }

    private void allowEdit() throws Exception {
        stubCurrentUser();
        when(userService.hasButtonPermission(101L, "/material/material_category", "1")).thenReturn(true);
    }

    private void stubCurrentUser() throws Exception {
        User user = new User();
        user.setId(101L);
        when(userService.getCurrentUser()).thenReturn(user);
    }

    private JSONObject validBody() {
        JSONObject body = new JSONObject();
        body.put("name", "测试类别");
        body.put("serialNo", "CAT-1");
        body.put("sort", "1");
        return body;
    }

    private MaterialCategory category(Long id, Long parentId, String name, String serialNo) {
        MaterialCategory category = new MaterialCategory();
        category.setId(id);
        category.setParentId(parentId);
        category.setName(name);
        category.setSerialNo(serialNo);
        category.setSort("1");
        return category;
    }
}
