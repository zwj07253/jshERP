package com.jsh.erp;

import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.AccountHead;
import com.jsh.erp.datasource.entities.Person;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.AccountHeadMapperEx;
import com.jsh.erp.datasource.mappers.DepotHeadMapperEx;
import com.jsh.erp.datasource.mappers.PersonMapper;
import com.jsh.erp.datasource.mappers.PersonMapperEx;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.service.LogService;
import com.jsh.erp.service.PersonService;
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
class PersonServiceTest {
    @Mock
    private PersonMapper personMapper;
    @Mock
    private PersonMapperEx personMapperEx;
    @Mock
    private UserService userService;
    @Mock
    private LogService logService;
    @Mock
    private AccountHeadMapperEx accountHeadMapperEx;
    @Mock
    private DepotHeadMapperEx depotHeadMapperEx;
    @InjectMocks
    private PersonService personService;

    @Test
    void rejectsWriteWithoutEditPermission() throws Exception {
        stubUser();
        when(userService.hasButtonPermission(101L, "/system/person", "1")).thenReturn(false);

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> personService.insertPerson(validBody(), null));

        assertEquals(ExceptionConstants.PERSON_PERMISSION_CODE, exception.getCode());
        verify(personMapper, never()).insertSelective(any());
    }

    @Test
    void insertUsesFieldWhitelistAndNormalizesValues() throws Exception {
        allowEdit();
        when(personMapper.selectByExample(any())).thenReturn(Collections.emptyList());
        when(personMapper.insertSelective(any())).thenReturn(1);
        JSONObject body = validBody();
        body.put("id", 999L);
        body.put("tenantId", 888L);
        body.put("deleteFlag", "1");
        body.put("enabled", false);
        body.put("name", " 小李 ");
        body.put("sort", "10");

        personService.insertPerson(body, null);

        ArgumentCaptor<Person> captor = ArgumentCaptor.forClass(Person.class);
        verify(personMapper).insertSelective(captor.capture());
        Person inserted = captor.getValue();
        assertNull(inserted.getId());
        assertNull(inserted.getTenantId());
        assertNull(inserted.getDeleteFlag());
        assertEquals(true, inserted.getEnabled());
        assertEquals("小李", inserted.getName());
        assertEquals("10", inserted.getSort());
    }

    @Test
    void rejectsInvalidTypeAndSort() throws Exception {
        allowEdit();
        JSONObject invalidType = validBody();
        invalidType.put("type", "业务员");
        assertEquals(ExceptionConstants.PERSON_INVALID_CODE,
                assertThrows(BusinessRunTimeException.class,
                        () -> personService.insertPerson(invalidType, null)).getCode());

        JSONObject invalidSort = validBody();
        invalidSort.put("sort", "1.5");
        assertEquals(ExceptionConstants.PERSON_INVALID_CODE,
                assertThrows(BusinessRunTimeException.class,
                        () -> personService.insertPerson(invalidSort, null)).getCode());
        verify(personMapper, never()).insertSelective(any());
    }

    @Test
    void rejectsChangingTypeWhenReferencedByFinancialBill() throws Exception {
        allowEdit();
        Person existing = person(1L, "财务员", true);
        when(personMapper.selectByExample(any())).thenReturn(Collections.singletonList(existing));
        when(accountHeadMapperEx.getAccountHeadListByHandsPersonIds(any()))
                .thenReturn(Collections.singletonList(new AccountHead()));
        JSONObject body = validBody();
        body.put("id", 1L);
        body.put("type", "销售员");

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> personService.updatePerson(body, null));

        assertEquals(ExceptionConstants.PERSON_IN_USE_CODE, exception.getCode());
        verify(personMapper, never()).updateByPrimaryKeySelective(any());
    }

    @Test
    void disabledPersonIsAllowedOnlyForExistingBillReference() throws Exception {
        stubUser();
        when(personMapper.selectByExample(any()))
                .thenReturn(Collections.singletonList(person(5L, "财务员", false)));

        personService.validateFinancialPerson(5L, 5L);
        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> personService.validateFinancialPerson(5L, null));

        assertEquals(ExceptionConstants.PERSON_BILL_INVALID_CODE, exception.getCode());
    }

    @Test
    void salesDeletionUsesSalesPersonReferencesInsteadOfCreator() throws Exception {
        allowEdit();
        when(accountHeadMapperEx.getAccountHeadListByHandsPersonIds(any())).thenReturn(Collections.emptyList());
        when(depotHeadMapperEx.getDepotHeadListBySalesPersonIds(any()))
                .thenReturn(Collections.singletonList(new com.jsh.erp.datasource.entities.DepotHead()));

        BusinessRunTimeException exception = assertThrows(BusinessRunTimeException.class,
                () -> personService.batchDeletePersonByIds("7"));

        assertEquals(ExceptionConstants.DELETE_FORCE_CONFIRM_CODE, exception.getCode());
        verify(personMapperEx, never()).batchDeletePersonByIds(any());
    }

    private JSONObject validBody() {
        JSONObject body = new JSONObject();
        body.put("name", "小李");
        body.put("type", "财务员");
        return body;
    }

    private Person person(Long id, String type, boolean enabled) {
        Person person = new Person();
        person.setId(id);
        person.setName("小李");
        person.setType(type);
        person.setEnabled(enabled);
        person.setDeleteFlag("0");
        return person;
    }

    private void allowEdit() throws Exception {
        stubUser();
        when(userService.hasButtonPermission(101L, "/system/person", "1")).thenReturn(true);
    }

    private void stubUser() throws Exception {
        User user = new User();
        user.setId(101L);
        user.setTenantId(7L);
        when(userService.getCurrentUser()).thenReturn(user);
    }
}
