package com.jsh.erp;

import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.AccountItem;
import com.jsh.erp.datasource.entities.InOutItem;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.AccountItemMapperEx;
import com.jsh.erp.datasource.mappers.InOutItemMapper;
import com.jsh.erp.datasource.mappers.InOutItemMapperEx;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.service.InOutItemService;
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
class InOutItemServiceTest {

    @Mock
    private InOutItemMapper inOutItemMapper;
    @Mock
    private InOutItemMapperEx inOutItemMapperEx;
    @Mock
    private UserService userService;
    @Mock
    private LogService logService;
    @Mock
    private AccountItemMapperEx accountItemMapperEx;

    @InjectMocks
    private InOutItemService inOutItemService;

    @Test
    void rejectsWriteWithoutEditPermission() throws Exception {
        stubCurrentUser();
        when(userService.hasButtonPermission(101L, "/system/in_out_item", "1")).thenReturn(false);

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> inOutItemService.insertInOutItem(validBody(), null));

        assertEquals(ExceptionConstants.IN_OUT_ITEM_PERMISSION_CODE, exception.getCode());
        verify(inOutItemMapper, never()).insertSelective(any());
    }

    @Test
    void insertUsesOnlyAllowedFieldsAndNormalizesValues() throws Exception {
        allowEdit();
        when(inOutItemMapper.selectByExample(any())).thenReturn(Collections.emptyList());
        when(inOutItemMapper.insertSelective(any())).thenReturn(1);
        JSONObject body = validBody();
        body.put("id", 999L);
        body.put("tenantId", 888L);
        body.put("deleteFlag", "1");
        body.put("enabled", false);
        body.put("name", " 维修收入 ");
        body.put("sort", "10");

        inOutItemService.insertInOutItem(body, null);

        ArgumentCaptor<InOutItem> captor = ArgumentCaptor.forClass(InOutItem.class);
        verify(inOutItemMapper).insertSelective(captor.capture());
        InOutItem inserted = captor.getValue();
        assertNull(inserted.getId());
        assertNull(inserted.getTenantId());
        assertNull(inserted.getDeleteFlag());
        assertEquals(true, inserted.getEnabled());
        assertEquals("维修收入", inserted.getName());
        assertEquals("10", inserted.getSort());
    }

    @Test
    void rejectsInvalidTypeAndSort() throws Exception {
        allowEdit();
        JSONObject invalidType = validBody();
        invalidType.put("type", "其它");
        BusinessRunTimeException typeException = assertThrows(BusinessRunTimeException.class,
                () -> inOutItemService.insertInOutItem(invalidType, null));
        assertEquals(ExceptionConstants.IN_OUT_ITEM_INVALID_CODE, typeException.getCode());

        JSONObject invalidSort = validBody();
        invalidSort.put("sort", "1.5");
        BusinessRunTimeException sortException = assertThrows(BusinessRunTimeException.class,
                () -> inOutItemService.insertInOutItem(invalidSort, null));
        assertEquals(ExceptionConstants.IN_OUT_ITEM_INVALID_CODE, sortException.getCode());
        verify(inOutItemMapper, never()).insertSelective(any());
    }

    @Test
    void rejectsChangingTypeWhenItemIsInUse() throws Exception {
        allowEdit();
        InOutItem existing = item(1L, "收入");
        when(inOutItemMapper.selectByExample(any())).thenReturn(Collections.singletonList(existing));
        when(accountItemMapperEx.getAccountItemListByInOutItemIds(any()))
                .thenReturn(Collections.singletonList(new AccountItem()));
        JSONObject body = validBody();
        body.put("id", 1L);
        body.put("type", "支出");

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> inOutItemService.updateInOutItem(body, null));

        assertEquals(ExceptionConstants.IN_OUT_ITEM_IN_USE_CODE, exception.getCode());
        verify(inOutItemMapper, never()).updateByPrimaryKeySelective(any());
    }

    @Test
    void selectRequiresSystemOrMatchingFinancialPermission() throws Exception {
        stubCurrentUser();
        when(userService.hasFunctionPermission(101L, "/system/in_out_item")).thenReturn(false);
        when(userService.hasFunctionPermission(101L, "/financial/item_in")).thenReturn(true);

        inOutItemService.checkSelectPermission("in");

        when(userService.hasFunctionPermission(101L, "/financial/item_out")).thenReturn(false);
        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> inOutItemService.checkSelectPermission("out"));
        assertEquals(ExceptionConstants.IN_OUT_ITEM_PERMISSION_CODE, exception.getCode());
    }

    private JSONObject validBody() {
        JSONObject body = new JSONObject();
        body.put("name", "维修收入");
        body.put("type", "收入");
        body.put("remark", "测试");
        return body;
    }

    private void allowEdit() throws Exception {
        stubCurrentUser();
        when(userService.hasButtonPermission(101L, "/system/in_out_item", "1")).thenReturn(true);
    }

    private void stubCurrentUser() throws Exception {
        User user = new User();
        user.setId(101L);
        user.setTenantId(7L);
        when(userService.getCurrentUser()).thenReturn(user);
    }

    private InOutItem item(Long id, String type) {
        InOutItem item = new InOutItem();
        item.setId(id);
        item.setName("维修收入");
        item.setType(type);
        item.setEnabled(true);
        item.setDeleteFlag("0");
        return item;
    }
}
