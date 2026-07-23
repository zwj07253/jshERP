package com.jsh.erp.service;

import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.SysDictData;
import com.jsh.erp.datasource.entities.SysDictType;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.SysDictDataMapper;
import com.jsh.erp.datasource.mappers.SysDictTypeMapper;
import com.jsh.erp.exception.BusinessRunTimeException;
import com.jsh.erp.exception.JshException;
import com.jsh.erp.utils.DictUtils;
import com.jsh.erp.utils.PageUtils;
import com.jsh.erp.utils.StringUtil;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 字典 业务层处理
 *
 * @author jishenghua
 */
@Service
public class SysDictDataService {

    private Logger logger = LoggerFactory.getLogger(SysDictDataService.class);

    private static final String DICT_URL = "/system/dict";
    private static final String EDIT_BUTTON_CODE = "1";

    @Resource
    private LogService logService;

    @Resource
    private SysDictDataMapper dictDataMapper;

    @Resource
    private SysDictTypeMapper dictTypeMapper;

    @Resource
    private UserService userService;

    /**
     * 根据条件分页查询字典数据
     *
     * @param dictData 字典数据信息
     * @return 字典数据集合信息
     */
    public List<SysDictData> selectDictDataList(SysDictData dictData)
    {
        PageUtils.startPage();
        return dictDataMapper.selectDictDataList(dictData);
    }

    /**
     * 根据字典类型和字典键值查询字典数据信息
     *
     * @param dictType 字典类型
     * @param dictValue 字典键值
     * @return 字典标签
     */
    public String selectDictLabel(String dictType, String dictValue)
    {
        return dictDataMapper.selectDictLabel(dictType, dictValue);
    }

    /**
     * 根据字典数据ID查询信息
     *
     * @param dictCode 字典数据ID
     * @return 字典数据
     */
    public SysDictData selectDictDataById(Long dictCode)
    {
        return dictDataMapper.selectDictDataById(dictCode);
    }

    public void checkEditPermission() throws Exception {
        User currentUser = userService.getCurrentUser();
        Long userId = currentUser == null ? null : currentUser.getId();
        if (!userService.hasButtonPermission(userId, DICT_URL, EDIT_BUTTON_CODE)) {
            throw new BusinessRunTimeException(ExceptionConstants.DICT_EDIT_PERMISSION_CODE,
                    ExceptionConstants.DICT_EDIT_PERMISSION_MSG);
        }
    }

    private void checkBuiltInProtected(String dictType) {
        SysDictType type = dictTypeMapper.selectDictTypeByType(dictType);
        if (type != null && "1".equals(type.getBuiltIn())) {
            throw new BusinessRunTimeException(ExceptionConstants.DICT_BUILT_IN_PROTECTED_CODE,
                    ExceptionConstants.DICT_BUILT_IN_PROTECTED_MSG);
        }
    }

    private void checkDictValueUnique(SysDictData data) {
        List<SysDictData> existing = dictDataMapper.selectDictDataByType(data.getDictType());
        if (existing != null) {
            for (SysDictData d : existing) {
                if (d.getDictValue().equals(data.getDictValue())
                        && !d.getDictCode().equals(data.getDictCode())) {
                    throw new BusinessRunTimeException(ExceptionConstants.DICT_VALUE_ALREADY_EXISTS_CODE,
                            ExceptionConstants.DICT_VALUE_ALREADY_EXISTS_MSG);
                }
            }
        }
    }

    private void checkIsDefaultUnique(SysDictData data) {
        if (!"Y".equals(data.getIsDefault())) {
            return;
        }
        List<SysDictData> existing = dictDataMapper.selectDictDataByType(data.getDictType());
        if (existing != null) {
            for (SysDictData d : existing) {
                if ("Y".equals(d.getIsDefault()) && !d.getDictCode().equals(data.getDictCode())) {
                    throw new BusinessRunTimeException(ExceptionConstants.DICT_DEFAULT_ALREADY_EXISTS_CODE,
                            ExceptionConstants.DICT_DEFAULT_ALREADY_EXISTS_MSG);
                }
            }
        }
    }

    private void scheduleCacheRefresh(String dictType) {
        Runnable task = () -> {
            try {
                List<SysDictData> dictDatas = dictDataMapper.selectDictDataByType(dictType);
                DictUtils.setDictCache(dictType, dictDatas);
            } catch (Exception e) {
                logger.error("字典缓存刷新失败: {}", dictType, e);
            }
        };
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    /**
     * 新增保存字典数据信息
     *
     * @param data 字典数据信息
     * @return 结果
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int insertDictData(SysDictData data)
    {
        checkDictValueUnique(data);
        checkIsDefaultUnique(data);
        int row = dictDataMapper.insertDictData(data);
        if (row > 0)
        {
            scheduleCacheRefresh(data.getDictType());
        }
        return row;
    }

    /**
     * 修改保存字典数据信息
     *
     * @param data 字典数据信息
     * @return 结果
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int updateDictData(SysDictData data)
    {
        SysDictData existing = selectDictDataById(data.getDictCode());
        if (existing == null) {
            throw new BusinessRunTimeException(ExceptionConstants.DICT_VALUE_IMMUTABLE_CODE,
                    "字典数据不存在");
        }
        if (!existing.getDictType().equals(data.getDictType())) {
            throw new BusinessRunTimeException(ExceptionConstants.DICT_TYPE_IMMUTABLE_CODE,
                    ExceptionConstants.DICT_TYPE_IMMUTABLE_MSG);
        }
        if (!existing.getDictValue().equals(data.getDictValue())) {
            throw new BusinessRunTimeException(ExceptionConstants.DICT_VALUE_IMMUTABLE_CODE,
                    ExceptionConstants.DICT_VALUE_IMMUTABLE_MSG);
        }
        checkIsDefaultUnique(data);
        int row = dictDataMapper.updateDictData(data);
        if (row > 0)
        {
            scheduleCacheRefresh(data.getDictType());
        }
        return row;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int deleteDictData(Long id, HttpServletRequest request) throws Exception {
        return batchDeleteDictDataByIds(id.toString());
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteDictData(String ids, HttpServletRequest request) throws Exception {
        return batchDeleteDictDataByIds(ids);
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteDictDataByIds(String ids)throws Exception {
        int result = 0;
        String[] idArray = ids.split(",");
        try {
            //记录日志
            String dictType = "";
            StringBuffer sb = new StringBuffer();
            sb.append(BusinessConstants.LOG_OPERATION_TYPE_DELETE);
            List<SysDictData> list = getDictDataListByIds(ids);
            if(!list.isEmpty()) {
                dictType = list.get(0).getDictType();
                checkBuiltInProtected(dictType);
                sb.append("字典：").append(dictType).append("下的数据：");
            }
            for(SysDictData sysDictData: list){
                sb.append("[").append(sysDictData.getDictLabel()).append("]");
            }
            result = dictDataMapper.batchDeleteDictDataByIds(idArray);
            final String cacheDictType = dictType;
            scheduleCacheRefresh(cacheDictType);
            //记录日志
            logService.insertLog("字典数据", sb.toString(),
                    ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        } catch (BusinessRunTimeException e) {
            throw e;
        } catch (Exception e) {
            JshException.writeFail(logger, e);
        }
        return result;
    }

    public List<SysDictData> getDictDataListByIds(String ids)throws Exception {
        List<Long> idList = StringUtil.strToLongList(ids);
        return dictDataMapper.getDictDataListByIds(idList);
    }
}
