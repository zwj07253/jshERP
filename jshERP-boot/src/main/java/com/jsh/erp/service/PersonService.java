package com.jsh.erp.service;

import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.AccountHead;
import com.jsh.erp.datasource.entities.DepotHead;
import com.jsh.erp.datasource.entities.Person;
import com.jsh.erp.datasource.entities.PersonExample;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.AccountHeadMapperEx;
import com.jsh.erp.datasource.mappers.DepotHeadMapperEx;
import com.jsh.erp.datasource.mappers.PersonMapper;
import com.jsh.erp.datasource.mappers.PersonMapperEx;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.exception.JshException;
import com.jsh.erp.utils.PageUtils;
import com.jsh.erp.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PersonService {
    private final Logger logger = LoggerFactory.getLogger(PersonService.class);
    private static final String PERSON_URL = "/system/person";
    private static final String EDIT_BUTTON_CODE = "1";
    private static final String TYPE_SALES = "销售员";
    private static final String TYPE_WAREHOUSE = "仓管员";
    private static final String TYPE_FINANCE = "财务员";
    private static final String[] SALES_URLS = {"/bill/sale_order", "/bill/sale_out", "/bill/sale_back",
            "/bill/retail_out", "/bill/retail_back"};
    private static final String[] FINANCE_URLS = {"/financial/item_in", "/financial/item_out",
            "/financial/money_in", "/financial/money_out", "/financial/giro", "/financial/advance_in"};
    private static final String[] WAREHOUSE_URLS = {"/bill/purchase_in", "/bill/purchase_back",
            "/bill/other_in", "/bill/other_out", "/bill/allocation_out", "/bill/assemble", "/bill/disassemble"};

    @Resource
    private PersonMapper personMapper;
    @Resource
    private PersonMapperEx personMapperEx;
    @Resource
    private UserService userService;
    @Resource
    private LogService logService;
    @Resource
    private AccountHeadMapperEx accountHeadMapperEx;
    @Resource
    private DepotHeadMapperEx depotHeadMapperEx;

    public Person getPerson(long id)throws Exception {
        try {
            PersonExample example = new PersonExample();
            example.createCriteria().andIdEqualTo(id)
                    .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
            List<Person> list = personMapper.selectByExample(example);
            return list.isEmpty() ? null : list.get(0);
        } catch(Exception e) {
            JshException.readFail(logger, e);
            return null;
        }
    }

    public List<Person> getPersonListByIds(String ids)throws Exception {
        List<Long> idList = parseIds(ids);
        if (idList.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            PersonExample example = new PersonExample();
            example.createCriteria().andIdIn(idList)
                    .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
            return personMapper.selectByExample(example);
        } catch(Exception e) {
            JshException.readFail(logger, e);
            return Collections.emptyList();
        }
    }

    public List<Person> getPerson()throws Exception {
        PersonExample example = new PersonExample();
        example.createCriteria().andEnabledEqualTo(true)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        try {
            return personMapper.selectByExample(example);
        } catch(Exception e) {
            JshException.readFail(logger, e);
            return Collections.emptyList();
        }
    }

    public List<Person> select(String name, String type)throws Exception {
        try {
            PageUtils.startPage();
            return personMapperEx.selectByConditionPerson(name, type);
        } catch(Exception e) {
            JshException.readFail(logger, e);
            return Collections.emptyList();
        }
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int insertPerson(JSONObject obj, HttpServletRequest request)throws Exception {
        checkEditPermission();
        lockPersonWrite();
        Person person = buildPerson(obj, null);
        validatePerson(person);
        ensureUnique(person);
        try {
            person.setEnabled(true);
            int result = personMapper.insertSelective(person);
            logService.insertLog("经手人", BusinessConstants.LOG_OPERATION_TYPE_ADD + person.getName(), request);
            return result;
        } catch(DuplicateKeyException e) {
            throw duplicateName();
        } catch(BusinessRunTimeException e) {
            throw e;
        } catch(Exception e) {
            JshException.writeFail(logger, e);
            return 0;
        }
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int updatePerson(JSONObject obj, HttpServletRequest request)throws Exception {
        checkEditPermission();
        lockPersonWrite();
        Long id = obj.getLong("id");
        Person existing = id == null ? null : getPerson(id);
        if (existing == null) {
            throw invalidPerson("经手人不存在");
        }
        Person person = buildPerson(obj, existing);
        validatePerson(person);
        if (!Objects.equals(existing.getType(), person.getType()) && isInUse(existing.getId())) {
            throw new BusinessRunTimeException(ExceptionConstants.PERSON_IN_USE_CODE,
                    ExceptionConstants.PERSON_IN_USE_MSG);
        }
        ensureUnique(person);
        try {
            int result = personMapper.updateByPrimaryKeySelective(person);
            logService.insertLog("经手人", BusinessConstants.LOG_OPERATION_TYPE_EDIT + person.getName(), request);
            return result;
        } catch(DuplicateKeyException e) {
            throw duplicateName();
        } catch(BusinessRunTimeException e) {
            throw e;
        } catch(Exception e) {
            JshException.writeFail(logger, e);
            return 0;
        }
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int deletePerson(Long id, HttpServletRequest request)throws Exception {
        return batchDeletePersonByIds(id == null ? null : id.toString());
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeletePerson(String ids, HttpServletRequest request) throws Exception {
        return batchDeletePersonByIds(ids);
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeletePersonByIds(String ids)throws Exception {
        checkEditPermission();
        lockPersonWrite();
        List<Long> idList = parseIds(ids);
        if (idList.isEmpty()) {
            return 0;
        }
        String[] idArray = idList.stream().map(String::valueOf).toArray(String[]::new);
        List<AccountHead> accountHeads = accountHeadMapperEx.getAccountHeadListByHandsPersonIds(idArray);
        List<DepotHead> depotHeads = depotHeadMapperEx.getDepotHeadListBySalesPersonIds(idArray);
        if ((accountHeads != null && !accountHeads.isEmpty()) || (depotHeads != null && !depotHeads.isEmpty())) {
            throw new BusinessRunTimeException(ExceptionConstants.DELETE_FORCE_CONFIRM_CODE,
                    ExceptionConstants.DELETE_FORCE_CONFIRM_MSG);
        }
        StringBuilder log = new StringBuilder(BusinessConstants.LOG_OPERATION_TYPE_DELETE);
        for(Person person : getPersonListByIds(ids)) {
            log.append("[").append(person.getName()).append("]");
        }
        logService.insertLog("经手人", log.toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        try {
            return personMapperEx.batchDeletePersonByIds(idArray);
        } catch(Exception e) {
            JshException.writeFail(logger, e);
            return 0;
        }
    }

    public int checkIsNameExist(Long id, String name) throws Exception {
        String normalizedName = StringUtil.toNull(name);
        if (normalizedName == null) {
            return 0;
        }
        PersonExample example = new PersonExample();
        example.createCriteria().andIdNotEqualTo(id == null ? 0L : id).andNameEqualTo(normalizedName)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        try {
            return personMapper.selectByExample(example).size();
        } catch(Exception e) {
            JshException.readFail(logger, e);
            return 0;
        }
    }

    /** 用于历史单据展示，禁用人员仍应显示姓名。 */
    public Map<Long,String> getPersonMap() throws Exception {
        PersonExample example = new PersonExample();
        example.createCriteria().andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        Map<Long,String> personMap = new HashMap<>();
        for(Person person : personMapper.selectByExample(example)) {
            personMap.put(person.getId(), person.getName());
        }
        return personMap;
    }

    public String getPersonByMapAndIds(Map<Long,String> personMap, String personIds) {
        if (StringUtil.isEmpty(personIds)) {
            return "";
        }
        List<String> names = new ArrayList<>();
        for(String value : personIds.split(",")) {
            String id = value.trim();
            if (StringUtil.isNumeric(id)) {
                String name = personMap.get(Long.parseLong(id));
                if (StringUtil.isNotEmpty(name)) {
                    names.add(name);
                }
            }
        }
        return String.join(" ", names);
    }

    public List<Person> getPersonByType(String type)throws Exception {
        validateType(type);
        PersonExample example = new PersonExample();
        example.createCriteria().andTypeEqualTo(type).andEnabledEqualTo(true)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        example.setOrderByClause("case when sort ~ '^[0-9]+$' then sort::bigint end asc nulls last, id desc");
        try {
            return personMapper.selectByExample(example);
        } catch(Exception e) {
            JshException.readFail(logger, e);
            return Collections.emptyList();
        }
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchSetStatus(Boolean status, String ids)throws Exception {
        checkEditPermission();
        lockPersonWrite();
        if (status == null) {
            throw invalidPerson("状态不能为空");
        }
        List<Long> personIds = parseIds(ids);
        if (personIds.isEmpty()) {
            return 0;
        }
        logService.insertLog("经手人", BusinessConstants.LOG_OPERATION_TYPE_ENABLED,
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        Person person = new Person();
        person.setEnabled(status);
        PersonExample example = new PersonExample();
        example.createCriteria().andIdIn(personIds)
                .andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        try {
            return personMapper.updateByExampleSelective(person, example);
        } catch(Exception e) {
            JshException.writeFail(logger, e);
            return 0;
        }
    }

    public void checkReadPermission() throws Exception {
        if (!hasFunction(PERSON_URL)) {
            throw permissionDenied();
        }
    }

    public void checkEditPermission() throws Exception {
        User user = userService.getCurrentUser();
        if (!userService.hasButtonPermission(user == null ? null : user.getId(), PERSON_URL, EDIT_BUTTON_CODE)) {
            throw permissionDenied();
        }
    }

    public void checkSelectPermission(String type) throws Exception {
        validateType(type);
        String[] urls = TYPE_SALES.equals(type) ? SALES_URLS
                : TYPE_FINANCE.equals(type) ? FINANCE_URLS : WAREHOUSE_URLS;
        if (!hasFunction(PERSON_URL) && !hasAnyFunction(urls)) {
            throw permissionDenied();
        }
    }

    public void checkBusinessReadPermission() throws Exception {
        if (!hasFunction(PERSON_URL) && !hasAnyFunction(SALES_URLS)
                && !hasAnyFunction(FINANCE_URLS) && !hasAnyFunction(WAREHOUSE_URLS)) {
            throw permissionDenied();
        }
    }

    public void lockPersonWrite() throws Exception {
        User user = userService.getCurrentUser();
        personMapperEx.lockPersonWrite(user == null || user.getTenantId() == null ? 0L : user.getTenantId());
    }

    public void validateFinancialPerson(Long personId, Long previousPersonId) throws Exception {
        if (personId == null) {
            return;
        }
        lockPersonWrite();
        validateBillPerson(personId, TYPE_FINANCE, Objects.equals(personId, previousPersonId));
    }

    public String validateSalesPersons(String personIds, String previousPersonIds) throws Exception {
        if (StringUtil.isEmpty(personIds)) {
            return null;
        }
        lockPersonWrite();
        List<Long> ids = parseIds(personIds);
        Set<Long> previousIds = StringUtil.isEmpty(previousPersonIds)
                ? Collections.emptySet() : new LinkedHashSet<>(parseIds(previousPersonIds));
        for (Long id : ids) {
            validateBillPerson(id, TYPE_SALES, previousIds.contains(id));
        }
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private void validateBillPerson(Long id, String expectedType, boolean existingReference) throws Exception {
        Person person = getPerson(id);
        if (person == null || !expectedType.equals(person.getType())
                || (!Boolean.TRUE.equals(person.getEnabled()) && !existingReference)) {
            throw new BusinessRunTimeException(ExceptionConstants.PERSON_BILL_INVALID_CODE,
                    ExceptionConstants.PERSON_BILL_INVALID_MSG);
        }
    }

    private Person buildPerson(JSONObject obj, Person existing) {
        Person person = new Person();
        if (existing != null) {
            person.setId(existing.getId());
        }
        person.setName(StringUtil.toNull(obj.getString("name")));
        person.setType(StringUtil.toNull(obj.getString("type")));
        person.setSort(StringUtil.toNull(obj.getString("sort")));
        return person;
    }

    private void validatePerson(Person person) {
        if (person.getName() == null || person.getName().length() > 50) {
            throw invalidPerson("姓名不能为空且长度不能超过50个字符");
        }
        validateType(person.getType());
        if (person.getSort() != null && !person.getSort().matches("\\d{1,10}")) {
            throw invalidPerson("排序只能填写不超过10位的非负整数");
        }
    }

    private void validateType(String type) {
        if (!TYPE_SALES.equals(type) && !TYPE_WAREHOUSE.equals(type) && !TYPE_FINANCE.equals(type)) {
            throw invalidPerson("类型只能为销售员、仓管员或财务员");
        }
    }

    private void ensureUnique(Person person) throws Exception {
        if (checkIsNameExist(person.getId(), person.getName()) > 0) {
            throw duplicateName();
        }
    }

    private boolean isInUse(Long id) {
        String[] ids = {String.valueOf(id)};
        List<AccountHead> accountHeads = accountHeadMapperEx.getAccountHeadListByHandsPersonIds(ids);
        List<DepotHead> depotHeads = depotHeadMapperEx.getDepotHeadListBySalesPersonIds(ids);
        return (accountHeads != null && !accountHeads.isEmpty()) || (depotHeads != null && !depotHeads.isEmpty());
    }

    private List<Long> parseIds(String ids) {
        if (StringUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        try {
            List<Long> result = new ArrayList<>(new LinkedHashSet<>(StringUtil.strToLongList(ids)));
            if (result.stream().anyMatch(id -> id == null || id <= 0)) {
                throw invalidPerson("经手人编号不合法");
            }
            return result;
        } catch (NumberFormatException e) {
            throw invalidPerson("经手人编号不合法");
        }
    }

    private boolean hasFunction(String url) throws Exception {
        User user = userService.getCurrentUser();
        return userService.hasFunctionPermission(user == null ? null : user.getId(), url);
    }

    private boolean hasAnyFunction(String[] urls) throws Exception {
        for (String url : urls) {
            if (hasFunction(url)) {
                return true;
            }
        }
        return false;
    }

    private BusinessRunTimeException duplicateName() {
        return new BusinessRunTimeException(ExceptionConstants.PERSON_NAME_EXIST_FAILED_CODE,
                ExceptionConstants.PERSON_NAME_EXIST_FAILED_MSG);
    }

    private BusinessRunTimeException permissionDenied() {
        return new BusinessRunTimeException(ExceptionConstants.PERSON_PERMISSION_CODE,
                ExceptionConstants.PERSON_PERMISSION_MSG);
    }

    private BusinessRunTimeException invalidPerson(String reason) {
        return new BusinessRunTimeException(ExceptionConstants.PERSON_INVALID_CODE,
                String.format(ExceptionConstants.PERSON_INVALID_MSG, reason));
    }
}
