package com.jsh.erp.service;

import com.alibaba.fastjson2.JSONObject;
import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.Material;
import com.jsh.erp.datasource.entities.MaterialCategory;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.MaterialCategoryMapper;
import com.jsh.erp.datasource.mappers.MaterialCategoryMapperEx;
import com.jsh.erp.datasource.mappers.MaterialMapperEx;
import com.jsh.erp.datasource.vo.TreeNode;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.exception.JshException;
import com.jsh.erp.utils.PageUtils;
import com.jsh.erp.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class MaterialCategoryService {
    private static final Long ROOT_CATEGORY_ID = 1L;
    private static final String MATERIAL_CATEGORY_URL = "/material/material_category";
    private static final String EDIT_BUTTON_CODE = "1";

    private Logger logger = LoggerFactory.getLogger(MaterialCategoryService.class);

    @Resource
    private MaterialCategoryMapper materialCategoryMapper;
    @Resource
    private MaterialCategoryMapperEx materialCategoryMapperEx;
    @Resource
    private UserService userService;
    @Resource
    private LogService logService;
    @Resource
    private MaterialMapperEx materialMapperEx;

    public MaterialCategory getMaterialCategory(long id)throws Exception {
        return getActiveMaterialCategory(id);
    }

    public List<MaterialCategory> getMaterialCategory()throws Exception {
        return getActiveMaterialCategories();
    }

    public List<MaterialCategory> getAllList(Long parentId)throws Exception {
        return getMCList(normalizeParentId(parentId));
    }

    public List<MaterialCategory> getMCList(Long parentId)throws Exception {
        List<MaterialCategory> categories = getActiveMaterialCategories();
        Set<Long> descendantIds = collectDescendantIds(categories, parentId, false);
        List<MaterialCategory> result = new ArrayList<>();
        for (MaterialCategory category : categories) {
            if (descendantIds.contains(category.getId()) && !ROOT_CATEGORY_ID.equals(category.getId())) {
                result.add(category);
            }
        }
        return result;
    }

    public List<MaterialCategory> select(String name, Integer parentId) throws Exception{
        List<MaterialCategory> list=null;
        try{
            PageUtils.startPage();
            list=materialCategoryMapperEx.selectByConditionMaterialCategory(name, parentId);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public synchronized int insertMaterialCategory(JSONObject obj, HttpServletRequest request)throws Exception {
        checkMaterialCategoryEditPermission();
        lockMaterialCategoryWrite();
        MaterialCategory materialCategory = buildMaterialCategory(obj, null);
        materialCategory.setCreateTime(new Date());
        materialCategory.setUpdateTime(new Date());
        validateMaterialCategory(materialCategory);
        int result=0;
        try{
            result=materialCategoryMapper.insertSelective(materialCategory);
            logService.insertLog("商品类型",
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_ADD).append(materialCategory.getName()).toString(), request);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public synchronized int updateMaterialCategory(JSONObject obj, HttpServletRequest request) throws Exception{
        checkMaterialCategoryEditPermission();
        lockMaterialCategoryWrite();
        Long id = obj.getLong("id");
        if (id == null) {
            throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_CATEGORY_NOT_EXISTS_CODE,
                    ExceptionConstants.MATERIAL_CATEGORY_NOT_EXISTS_MSG);
        }
        if (ROOT_CATEGORY_ID.equals(id)) {
            throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_CATEGORY_ROOT_NOT_SUPPORT_EDIT_CODE,
                    ExceptionConstants.MATERIAL_CATEGORY_ROOT_NOT_SUPPORT_EDIT_MSG);
        }
        MaterialCategory existing = getActiveMaterialCategory(id);
        if (existing == null) {
            throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_CATEGORY_NOT_EXISTS_CODE,
                    ExceptionConstants.MATERIAL_CATEGORY_NOT_EXISTS_MSG);
        }
        MaterialCategory materialCategory = buildMaterialCategory(obj, existing);
        materialCategory.setUpdateTime(new Date());
        validateMaterialCategory(materialCategory);
        int result=0;
        try{
            result=materialCategoryMapperEx.editMaterialCategory(materialCategory);
            logService.insertLog("商品类型",
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append(materialCategory.getName()).toString(), request);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int deleteMaterialCategory(Long id, HttpServletRequest request)throws Exception {
        return batchDeleteMaterialCategoryByIds(id.toString());
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteMaterialCategory(String ids, HttpServletRequest request)throws Exception {
        return batchDeleteMaterialCategoryByIds(ids);
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public synchronized int batchDeleteMaterialCategoryByIds(String ids) throws Exception {
        checkMaterialCategoryEditPermission();
        lockMaterialCategoryWrite();
        int result=0;
        List<Long> categoryIds = new ArrayList<>(new LinkedHashSet<>(StringUtil.strToLongList(ids)));
        if (categoryIds.isEmpty()) {
            return 0;
        }
        if (categoryIds.contains(ROOT_CATEGORY_ID)) {
            throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_CATEGORY_ROOT_NOT_SUPPORT_DELETE_CODE,
                    ExceptionConstants.MATERIAL_CATEGORY_ROOT_NOT_SUPPORT_DELETE_MSG);
        }
        List<MaterialCategory> selectedCategories = new ArrayList<>();
        Map<Long, MaterialCategory> activeCategoryMap = new HashMap<>();
        for (MaterialCategory category : getActiveMaterialCategories()) {
            activeCategoryMap.put(category.getId(), category);
        }
        for (Long categoryId : categoryIds) {
            MaterialCategory category = activeCategoryMap.get(categoryId);
            if (category == null) {
                throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_CATEGORY_NOT_EXISTS_CODE,
                        ExceptionConstants.MATERIAL_CATEGORY_NOT_EXISTS_MSG);
            }
            selectedCategories.add(category);
        }
        //校验产品表	jsh_material
        List<Material> materialList=null;
        try{
            materialList= materialMapperEx.getMaterialListByCategoryIds(categoryIds);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        if(materialList!=null&&materialList.size()>0){
            logger.error("异常码[{}],异常提示[{}],参数,CategoryIds[{}]",
                    ExceptionConstants.DELETE_FORCE_CONFIRM_CODE,ExceptionConstants.DELETE_FORCE_CONFIRM_MSG,ids);
            throw new BusinessRunTimeException(ExceptionConstants.DELETE_FORCE_CONFIRM_CODE,
                    ExceptionConstants.DELETE_FORCE_CONFIRM_MSG);
        }
        StringBuffer sb = new StringBuffer();
        sb.append(BusinessConstants.LOG_OPERATION_TYPE_DELETE);
        for(MaterialCategory materialCategory: selectedCategories){
            sb.append("[").append(materialCategory.getName()).append("]");
        }
        logService.insertLog("商品类型", sb.toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        //更新时间
        Date updateDate =new Date();
        //更新人
        User userInfo=userService.getCurrentUser();
        Long updater=userInfo==null?null:userInfo.getId();
        List<MaterialCategory> mcList = materialCategoryMapperEx.getMaterialCategoryListByCategoryIds(categoryIds);
        if(mcList!=null && mcList.size()>0) {
            logger.error("异常码[{}],异常提示[{}]",
                    ExceptionConstants.MATERIAL_CATEGORY_CHILD_NOT_SUPPORT_DELETE_CODE,ExceptionConstants.MATERIAL_CATEGORY_CHILD_NOT_SUPPORT_DELETE_MSG);
            throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_CATEGORY_CHILD_NOT_SUPPORT_DELETE_CODE,
                    ExceptionConstants.MATERIAL_CATEGORY_CHILD_NOT_SUPPORT_DELETE_MSG);
        } else {
            result=materialCategoryMapperEx.batchDeleteMaterialCategoryByIds(updateDate,updater,categoryIds);
        }
        return result;
    }

    public int checkIsNameExist(Long id, String name, Long parentId)throws Exception {
        int count = 0;
        Long normalizedParentId = normalizeParentId(parentId);
        for (MaterialCategory category : getActiveMaterialCategories()) {
            if (!Objects.equals(category.getId(), id)
                    && Objects.equals(normalizeParentId(category.getParentId()), normalizedParentId)
                    && Objects.equals(category.getName(), name)) {
                count++;
            }
        }
        return count;
    }

    public List<MaterialCategory> findById(Long id)throws Exception {
        MaterialCategory category = id == null ? null : getActiveMaterialCategory(id);
        return category == null ? new ArrayList<>() : Collections.singletonList(category);
    }
    /**
     * description:
     * 获取商品类别树数据
     */
    public List<TreeNode> getMaterialCategoryTree(Long id) throws Exception{
        List<MaterialCategory> categories = getActiveMaterialCategories();
        Long excludedRootId = normalizeParentId(id);
        Set<Long> excludedIds = excludedRootId == null ? Collections.emptySet()
                : collectDescendantIds(categories, excludedRootId, true);
        Map<Long, TreeNode> nodes = new LinkedHashMap<>();
        for (MaterialCategory category : categories) {
            if (!excludedIds.contains(category.getId())) {
                nodes.put(category.getId(), toTreeNode(category));
            }
        }
        List<TreeNode> roots = new ArrayList<>();
        for (MaterialCategory category : categories) {
            TreeNode node = nodes.get(category.getId());
            if (node == null) {
                continue;
            }
            Long parentId = normalizeParentId(category.getParentId());
            TreeNode parent = parentId == null ? null : nodes.get(parentId);
            if (parent == null) {
                if (parentId == null) {
                    roots.add(node);
                }
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }
    /**
     * 根据商品类别编号判断商品类别是否已存在
     * */
    public void  checkMaterialCategorySerialNo(MaterialCategory mc)throws Exception {
        if(mc==null){
            return;
        }
        if(StringUtil.isEmpty(mc.getSerialNo())){
            return;
        }
        //根据商品类别编号查询商品类别
        List<MaterialCategory> mList=null;
        try{
            mList= materialCategoryMapperEx.getMaterialCategoryBySerialNo(mc.getSerialNo(), mc.getId());
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        if(mList!=null && !mList.isEmpty()){
            throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_CATEGORY_SERIAL_ALREADY_EXISTS_CODE,
                    ExceptionConstants.MATERIAL_CATEGORY_SERIAL_ALREADY_EXISTS_MSG);
        }
    }

    /**
     * 根据名称获取类型
     * @param name
     */
    public Long getCategoryIdByName(String name) throws Exception {
        if (StringUtil.isEmpty(name)) {
            return null;
        }
        List<MaterialCategory> list = new ArrayList<>();
        for (MaterialCategory category : getActiveMaterialCategories()) {
            if (name.equals(category.getName())) {
                list.add(category);
            }
        }
        if(list.size() > 1) {
            throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_CATEGORY_NAME_AMBIGUOUS_CODE,
                    ExceptionConstants.MATERIAL_CATEGORY_NAME_AMBIGUOUS_MSG);
        }
        return list.isEmpty() ? null : list.get(0).getId();
    }

    public void checkMaterialCategoryEditPermission() throws Exception {
        User currentUser = userService.getCurrentUser();
        Long userId = currentUser == null ? null : currentUser.getId();
        if (!userService.hasButtonPermission(userId, MATERIAL_CATEGORY_URL, EDIT_BUTTON_CODE)) {
            throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_CATEGORY_PERMISSION_CODE,
                    ExceptionConstants.MATERIAL_CATEGORY_PERMISSION_MSG);
        }
    }

    private void lockMaterialCategoryWrite() throws Exception {
        User currentUser = userService.getCurrentUser();
        Long tenantId = currentUser == null || currentUser.getTenantId() == null
                ? 0L : currentUser.getTenantId();
        materialCategoryMapperEx.lockMaterialCategoryWrite(tenantId);
    }

    public List<Long> getCategoryIdListByParentId(Long parentId) throws Exception {
        parentId = normalizeParentId(parentId);
        if (parentId == null) {
            return new ArrayList<>();
        }
        List<MaterialCategory> categories = getActiveMaterialCategories();
        Set<Long> ids = collectDescendantIds(categories, parentId, true);
        return new ArrayList<>(ids);
    }

    private MaterialCategory buildMaterialCategory(JSONObject obj, MaterialCategory existing) {
        MaterialCategory category = new MaterialCategory();
        if (existing != null) {
            category.setId(existing.getId());
            category.setName(existing.getName());
            category.setCategoryLevel(existing.getCategoryLevel());
            category.setParentId(existing.getParentId());
            category.setSort(existing.getSort());
            category.setSerialNo(existing.getSerialNo());
            category.setRemark(existing.getRemark());
        }
        if (obj.containsKey("name")) {
            category.setName(obj.getString("name"));
        }
        if (obj.containsKey("categoryLevel")) {
            category.setCategoryLevel(obj.getShort("categoryLevel"));
        }
        if (obj.containsKey("parentId")) {
            category.setParentId(normalizeParentId(obj.getLong("parentId")));
        }
        if (obj.containsKey("sort")) {
            category.setSort(obj.getString("sort"));
        }
        if (obj.containsKey("serialNo")) {
            category.setSerialNo(obj.getString("serialNo"));
        }
        if (obj.containsKey("remark")) {
            category.setRemark(obj.getString("remark"));
        }
        return category;
    }

    private void validateMaterialCategory(MaterialCategory category) throws Exception {
        if (StringUtil.isEmpty(category.getName()) || StringUtil.isEmpty(category.getSerialNo())) {
            throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_CATEGORY_REQUIRED_CODE,
                    ExceptionConstants.MATERIAL_CATEGORY_REQUIRED_MSG);
        }
        List<MaterialCategory> categories = getActiveMaterialCategories();
        Map<Long, MaterialCategory> categoryMap = new HashMap<>();
        for (MaterialCategory item : categories) {
            categoryMap.put(item.getId(), item);
        }
        validateParent(category, categoryMap);
        for (MaterialCategory item : categories) {
            if (!Objects.equals(item.getId(), category.getId())
                    && Objects.equals(normalizeParentId(item.getParentId()), normalizeParentId(category.getParentId()))
                    && category.getName().equals(item.getName())) {
                throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_CATEGORY_NAME_ALREADY_EXISTS_CODE,
                        ExceptionConstants.MATERIAL_CATEGORY_NAME_ALREADY_EXISTS_MSG);
            }
        }
        checkMaterialCategorySerialNo(category);
    }

    private void validateParent(MaterialCategory category, Map<Long, MaterialCategory> categoryMap) {
        Long parentId = normalizeParentId(category.getParentId());
        category.setParentId(parentId);
        if (parentId == null) {
            return;
        }
        if (!categoryMap.containsKey(parentId)) {
            throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_CATEGORY_PARENT_INVALID_CODE,
                    ExceptionConstants.MATERIAL_CATEGORY_PARENT_INVALID_MSG);
        }
        Set<Long> visited = new HashSet<>();
        Long currentId = parentId;
        while (currentId != null) {
            if (Objects.equals(currentId, category.getId()) || !visited.add(currentId)) {
                throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_CATEGORY_CYCLE_CODE,
                        ExceptionConstants.MATERIAL_CATEGORY_CYCLE_MSG);
            }
            MaterialCategory parent = categoryMap.get(currentId);
            if (parent == null) {
                throw new BusinessRunTimeException(ExceptionConstants.MATERIAL_CATEGORY_PARENT_INVALID_CODE,
                        ExceptionConstants.MATERIAL_CATEGORY_PARENT_INVALID_MSG);
            }
            currentId = normalizeParentId(parent.getParentId());
        }
    }

    private MaterialCategory getActiveMaterialCategory(Long id) throws Exception {
        for (MaterialCategory category : getActiveMaterialCategories()) {
            if (Objects.equals(category.getId(), id)) {
                return category;
            }
        }
        return null;
    }

    private List<MaterialCategory> getActiveMaterialCategories() throws Exception {
        try {
            return materialCategoryMapperEx.getActiveMaterialCategoryList();
        } catch (Exception e) {
            JshException.readFail(logger, e);
            return new ArrayList<>();
        }
    }

    private Set<Long> collectDescendantIds(List<MaterialCategory> categories, Long parentId, boolean includeParent) {
        Map<Long, List<Long>> childrenByParent = new HashMap<>();
        for (MaterialCategory category : categories) {
            Long normalizedParentId = normalizeParentId(category.getParentId());
            childrenByParent.computeIfAbsent(normalizedParentId, key -> new ArrayList<>()).add(category.getId());
        }
        Set<Long> result = new LinkedHashSet<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        if (includeParent && parentId != null) {
            result.add(parentId);
        }
        List<Long> directChildren = childrenByParent.get(normalizeParentId(parentId));
        if (directChildren != null) {
            queue.addAll(directChildren);
        }
        while (!queue.isEmpty()) {
            Long categoryId = queue.removeFirst();
            if (!result.add(categoryId)) {
                continue;
            }
            List<Long> children = childrenByParent.get(categoryId);
            if (children != null) {
                queue.addAll(children);
            }
        }
        return result;
    }

    private TreeNode toTreeNode(MaterialCategory category) {
        TreeNode node = new TreeNode();
        node.setId(category.getId());
        node.setKey(category.getId());
        node.setValue(category.getId());
        node.setTitle(category.getName());
        node.setChildren(new ArrayList<>());
        return node;
    }

    private Long normalizeParentId(Long parentId) {
        return parentId != null && parentId == 0L ? null : parentId;
    }
}
