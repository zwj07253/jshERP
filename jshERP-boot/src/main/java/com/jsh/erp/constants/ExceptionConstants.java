package com.jsh.erp.constants;

import com.alibaba.fastjson2.JSONObject;

public class ExceptionConstants {
    /**
     * code 格式 type+五位数字，例如3500000
     * ResourceInfo(value = "inOutItem", type = 35)
     *
     * */

    public static final String GLOBAL_RETURNS_CODE = "code";
    public static final String GLOBAL_RETURNS_MESSAGE = "msg";
    public static final String GLOBAL_RETURNS_DATA = "data";

    /**
     * 正常返回/操作成功
     **/
    public static final int SERVICE_SUCCESS_CODE = 200;
    public static final String SERVICE_SUCCESS_MSG = "操作成功";
    /**
     * 数据查询异常
     */
    public static final int DATA_READ_FAIL_CODE = 300;
    public static final String DATA_READ_FAIL_MSG = "数据查询异常";
    /**
     * 数据写入异常
     */
    public static final int DATA_WRITE_FAIL_CODE = 301;
    public static final String DATA_WRITE_FAIL_MSG = "数据写入异常";

    /**
     * 系统运行时未知错误
     **/
    public static final int SERVICE_SYSTEM_ERROR_CODE = 500;
    public static final String SERVICE_SYSTEM_ERROR_MSG = "未知异常";
    /**
     * 检测到存在依赖数据，是否强制删除？
     **/
    public static final int DELETE_FORCE_CONFIRM_CODE = 601;
    public static final String DELETE_FORCE_CONFIRM_MSG = "检测到存在依赖数据，不能删除！";

    /**
     * 文件扩展名必须为xls
     **/
    public static final int FILE_EXTENSION_ERROR_CODE = 701;
    public static final String FILE_EXTENSION_ERROR_MSG = "文件扩展名必须为xls";

    /**
     * 用户信息
     * type = 5
     * */
    //添加用户信息失败
    public static final int USER_ADD_FAILED_CODE = 500000;
    public static final String USER_ADD_FAILED_MSG = "添加用户信息失败";
    //删除用户信息失败
    public static final int USER_DELETE_FAILED_CODE = 500001;
    public static final String USER_DELETE_FAILED_MSG = "删除用户信息失败";
    //修改用户信息失败
    public static final int USER_EDIT_FAILED_CODE = 500002;
    public static final String USER_EDIT_FAILED_MSG = "修改用户信息失败";
    //登录名已存在
    public static final int USER_LOGIN_NAME_ALREADY_EXISTS_CODE = 500003;
    public static final String USER_LOGIN_NAME_ALREADY_EXISTS_MSG = "登录名在本系统已存在";
    //用户录入数量超出限制
    public static final int USER_OVER_LIMIT_FAILED_CODE = 500004;
    public static final String USER_OVER_LIMIT_FAILED_MSG = "用户录入数量超出限制，请联系平台管理员";
    //此用户名限制使用
    public static final int USER_NAME_LIMIT_USE_CODE = 500005;
    public static final String USER_NAME_LIMIT_USE_MSG = "此用户名限制使用";
    //启用的用户数量超出限制
    public static final int USER_ENABLE_OVER_LIMIT_FAILED_CODE = 500006;
    public static final String USER_ENABLE_OVER_LIMIT_FAILED_MSG = "启用的用户数量超出限制，请联系平台管理员";
    //租户不能被删除
    public static final int USER_LIMIT_TENANT_DELETE_CODE = 500008;
    public static final String USER_LIMIT_TENANT_DELETE_MSG = "抱歉，租户不能被删除";
    //当前部门已经存在经理
    public static final int USER_LEADER_IS_EXIST_CODE = 500009;
    public static final String USER_LEADER_IS_EXIST_MSG = "抱歉，当前部门已经存在经理";
    //验证码错误
    public static final int USER_JCAPTCHA_ERROR_CODE = 500010;
    public static final String USER_JCAPTCHA_ERROR_MSG = "验证码错误";
    //验证码已失效
    public static final int USER_JCAPTCHA_EXPIRE_CODE = 500011;
    public static final String USER_JCAPTCHA_EXPIRE_MSG = "验证码已失效";
    //验证码不能为空
    public static final int USER_JCAPTCHA_EMPTY_CODE = 500012;
    public static final String USER_JCAPTCHA_EMPTY_MSG = "验证码不能为空";
    //当前角色的数据类型是本部门数据，所以必须选择部门
    public static final int USER_ROLE_ORGA_EMPTY_CODE = 500013;
    public static final String USER_ROLE_ORGA_EMPTY_MSG = "当前角色的数据类型是本部门数据，所以必须选择部门";

    /**
     * 角色信息
     * type = 10
     * */
    //添加角色信息失败
    public static final int ROLE_ADD_FAILED_CODE = 1000000;
    public static final String ROLE_ADD_FAILED_MSG = "添加角色信息失败";
    //删除角色信息失败
    public static final int ROLE_DELETE_FAILED_CODE = 1000001;
    public static final String ROLE_DELETE_FAILED_MSG = "删除角色信息失败";
    //修改角色信息失败
    public static final int ROLE_EDIT_FAILED_CODE = 1000002;
    public static final String ROLE_EDIT_FAILED_MSG = "修改角色信息失败";
    /**
     * 应用信息
     * type = 15
     * */
    //添加角色信息失败
    public static final int APP_ADD_FAILED_CODE = 1500000;
    public static final String APP_ADD_FAILED_MSG = "添加应用信息失败";
    //删除角色信息失败
    public static final int APP_DELETE_FAILED_CODE = 1500001;
    public static final String APP_DELETE_FAILED_MSG = "删除应用信息失败";
    //修改角色信息失败
    public static final int APP_EDIT_FAILED_CODE = 1500002;
    public static final String APP_EDIT_FAILED_MSG = "修改应用信息失败";
    /**
     *  仓库信息
     * type = 20
     * */
    //添加仓库信息失败
    public static final int DEPOT_ADD_FAILED_CODE = 2000000;
    public static final String DEPOT_ADD_FAILED_MSG = "添加仓库信息失败";
    //删除仓库信息失败
    public static final int DEPOT_DELETE_FAILED_CODE = 2000001;
    public static final String DEPOT_DELETE_FAILED_MSG = "删除仓库信息失败";
    //修改仓库信息失败
    public static final int DEPOT_EDIT_FAILED_CODE = 2000002;
    public static final String DEPOT_EDIT_FAILED_MSG = "修改仓库信息失败";
    public static final int DEPOT_PERMISSION_CODE = 2000003;
    public static final String DEPOT_PERMISSION_MSG = "抱歉，当前用户没有仓库信息的操作权限";
    public static final int DEPOT_INVALID_CODE = 2000004;
    public static final String DEPOT_INVALID_MSG = "仓库信息不合法：%s";
    public static final int DEPOT_ALREADY_EXISTS_CODE = 2000005;
    public static final String DEPOT_ALREADY_EXISTS_MSG = "仓库名称已经存在";
    public static final int DEPOT_IN_USE_CODE = 2000006;
    public static final String DEPOT_IN_USE_MSG = "仓库已被单据、库存或序列号使用，不能删除";
    public static final int DEPOT_DEFAULT_OPERATION_CODE = 2000007;
    public static final String DEPOT_DEFAULT_OPERATION_MSG = "默认仓库不能停用或删除，请先设置其它默认仓库";

    /**
     * 功能模块信息
     * type = 30
     * */
    //添加角色信息失败
    public static final int FUNCTIONS_ADD_FAILED_CODE = 3000000;
    public static final String FUNCTIONS_ADD_FAILED_MSG = "添加功能模块信息失败";
    //删除角色信息失败
    public static final int FUNCTIONS_DELETE_FAILED_CODE = 3000001;
    public static final String FUNCTIONS_DELETE_FAILED_MSG = "删除功能模块信息失败";
    //修改角色信息失败
    public static final int FUNCTIONS_EDIT_FAILED_CODE = 3000002;
    public static final String FUNCTIONS_EDIT_FAILED_MSG = "修改功能模块信息失败";
    /**
     * 收支项目信息
     * type = 35
     * */
    //添加收支项目信息失败
    public static final int IN_OUT_ITEM_ADD_FAILED_CODE = 3500000;
    public static final String IN_OUT_ITEM_ADD_FAILED_MSG = "添加收支项目信息失败";
    //删除收支项目信息失败
    public static final int IN_OUT_ITEM_DELETE_FAILED_CODE = 3500001;
    public static final String IN_OUT_ITEM_DELETE_FAILED_MSG = "删除收支项目信息失败";
    //修改收支项目信息失败
    public static final int IN_OUT_ITEM_EDIT_FAILED_CODE = 3500002;
    public static final String IN_OUT_ITEM_EDIT_FAILED_MSG = "修改收支项目信息失败";
    //该收支项目的名称已经存在
    public static final int IN_OUT_ITEM_NAME_EXIST_FAILED_CODE = 3500003;
    public static final String IN_OUT_ITEM_NAME_EXIST_FAILED_MSG = "该收支项目的名称已经存在，请修改！";
    /**
     *  多单位信息
     * type = 40
     * */
    //添加多单位信息失败
    public static final int UNIT_ADD_FAILED_CODE = 4000000;
    public static final String UNIT_ADD_FAILED_MSG = "添加多单位信息失败";
    //删除多单位信息失败
    public static final int UNIT_DELETE_FAILED_CODE = 4000001;
    public static final String UNIT_DELETE_FAILED_MSG = "删除多单位信息失败";
    //修改多单位信息失败
    public static final int UNIT_EDIT_FAILED_CODE = 4000002;
    public static final String UNIT_EDIT_FAILED_MSG = "修改多单位信息失败";
    public static final int UNIT_PERMISSION_CODE = 4000003;
    public static final String UNIT_PERMISSION_MSG = "抱歉，当前用户没有多单位的编辑权限";
    public static final int UNIT_INVALID_CODE = 4000004;
    public static final String UNIT_INVALID_MSG = "多单位设置不合法：%s";
    public static final int UNIT_ALREADY_EXISTS_CODE = 4000005;
    public static final String UNIT_ALREADY_EXISTS_MSG = "相同的多单位方案已存在";
    public static final int UNIT_IN_USE_CODE = 4000006;
    public static final String UNIT_IN_USE_MSG = "多单位已被商品使用，不允许修改、禁用或删除";
    /**
     *  经手人信息
     * type = 45
     * */
    //添加经手人信息失败
    public static final int PERSON_ADD_FAILED_CODE = 4500000;
    public static final String PERSON_ADD_FAILED_MSG = "添加经手人信息失败";
    //删除经手人信息失败
    public static final int PERSON_DELETE_FAILED_CODE = 4500001;
    public static final String PERSON_DELETE_FAILED_MSG = "删除经手人信息失败";
    //修改经手人信息失败
    public static final int PERSON_EDIT_FAILED_CODE = 4500002;
    public static final String PERSON_EDIT_FAILED_MSG = "修改经手人信息失败";
    /**
     * 用户角色模块关系信息
     * type = 50
     * */
    //添加用户角色模块关系信息失败
    public static final int USER_BUSINESS_ADD_FAILED_CODE = 5000000;
    public static final String USER_BUSINESS_ADD_FAILED_MSG = "添加用户角色模块关系信息失败";
    //删除用户角色模块关系信息失败
    public static final int USER_BUSINESS_DELETE_FAILED_CODE = 5000001;
    public static final String USER_BUSINESS_DELETE_FAILED_MSG = "删除用户角色模块关系信息失败";
    //修改用户角色模块关系信息失败
    public static final int USER_BUSINESS_EDIT_FAILED_CODE = 5000002;
    public static final String USER_BUSINESS_EDIT_FAILED_MSG = "修改用户角色模块关系信息失败";
    /**
     *  系统参数信息
     * type = 55
     * */
    //添加系统参数信息失败
    public static final int SYSTEM_CONFIG_ADD_FAILED_CODE = 5500000;
    public static final String SYSTEM_CONFIG_ADD_FAILED_MSG = "添加系统参数信息失败";
    //删除系统参数信息失败
    public static final int SYSTEM_CONFIG_DELETE_FAILED_CODE = 5500001;
    public static final String SYSTEM_CONFIG_DELETE_FAILED_MSG = "删除系统参数信息失败";
    //修改系统参数信息失败
    public static final int SYSTEM_CONFIG_EDIT_FAILED_CODE = 5500002;
    public static final String SYSTEM_CONFIG_EDIT_FAILED_MSG = "修改系统参数信息失败";
    /**
     * 商品扩展信息
     * type = 60
     * */
    //添加商品扩展信息失败
    public static final int MATERIAL_PROPERTY_ADD_FAILED_CODE = 6000000;
    public static final String MATERIAL_PROPERTY_ADD_FAILED_MSG = "添加商品扩展信息失败";
    //删除商品扩展信息失败
    public static final int MATERIAL_PROPERTY_DELETE_FAILED_CODE = 6000001;
    public static final String MATERIAL_PROPERTY_DELETE_FAILED_MSG = "删除商品扩展信息失败";
    //修改商品扩展信息失败
    public static final int MATERIAL_PROPERTY_EDIT_FAILED_CODE = 6000002;
    public static final String MATERIAL_PROPERTY_EDIT_FAILED_MSG = "修改商品扩展信息失败";
    /**
     *  账户信息
     * type = 65
     * */
    //添加账户信息失败
    public static final int ACCOUNT_ADD_FAILED_CODE = 6500000;
    public static final String ACCOUNT_ADD_FAILED_MSG = "添加账户信息失败";
    //删除账户信息失败
    public static final int ACCOUNT_DELETE_FAILED_CODE = 6500001;
    public static final String ACCOUNT_DELETE_FAILED_MSG = "删除账户信息失败";
    //修改账户信息失败
    public static final int ACCOUNT_EDIT_FAILED_CODE = 6500002;
    public static final String ACCOUNT_EDIT_FAILED_MSG = "修改账户信息失败";
    //账户统计查看权限
    public static final int ACCOUNT_REPORT_PERMISSION_CODE = 6500003;
    public static final String ACCOUNT_REPORT_PERMISSION_MSG = "抱歉，当前用户没有账户统计的查看权限";
    //账户统计请求的账户不存在或已删除
    public static final int ACCOUNT_REPORT_ACCOUNT_FAILED_CODE = 6500004;
    public static final String ACCOUNT_REPORT_ACCOUNT_FAILED_MSG = "抱歉，结算账户不存在或已删除";
    /**
     *  供应商信息
     * type = 70
     * */
    //添加供应商信息失败
    public static final int SUPPLIER_ADD_FAILED_CODE = 7000000;
    public static final String SUPPLIER_ADD_FAILED_MSG = "添加供应商信息失败";
    //删除供应商信息失败
    public static final int SUPPLIER_DELETE_FAILED_CODE = 7000001;
    public static final String SUPPLIER_DELETE_FAILED_MSG = "删除供应商信息失败";
    //修改供应商信息失败
    public static final int SUPPLIER_EDIT_FAILED_CODE = 7000002;
    public static final int SUPPLIER_PERMISSION_CODE = 7000003;
    public static final String SUPPLIER_PERMISSION_MSG = "当前用户没有往来单位编辑权限";
    public static final int SUPPLIER_INVALID_CODE = 7000004;
    public static final String SUPPLIER_INVALID_MSG = "往来单位信息不合法：%s";
    public static final int SUPPLIER_NOT_EXISTS_CODE = 7000005;
    public static final String SUPPLIER_NOT_EXISTS_MSG = "往来单位不存在或已删除";
    public static final int SUPPLIER_IN_USE_CODE = 7000006;
    public static final String SUPPLIER_IN_USE_MSG = "往来单位已被业务使用，不允许修改或删除";
    public static final String SUPPLIER_EDIT_FAILED_MSG = "修改供应商信息失败";
    /**
     * 商品类别信息
     * type = 75
     * */
    //添加商品类别信息失败
    public static final int MATERIAL_CATEGORY_ADD_FAILED_CODE = 7500000;
    public static final String MATERIAL_CATEGORY_ADD_FAILED_MSG = "添加商品类别信息失败";
    //删除商品类别信息失败
    public static final int MATERIAL_CATEGORY_DELETE_FAILED_CODE = 7500001;
    public static final String MATERIAL_CATEGORY_DELETE_FAILED_MSG = "删除商品类别信息失败";
    //修改商品类别信息失败
    public static final int MATERIAL_CATEGORY_EDIT_FAILED_CODE = 7500002;
    public static final String MATERIAL_CATEGORY_EDIT_FAILED_MSG = "修改商品类别信息失败";
    //商品类别编号已存在
    public static final int MATERIAL_CATEGORY_SERIAL_ALREADY_EXISTS_CODE = 7500003;
    public static final String MATERIAL_CATEGORY_SERIAL_ALREADY_EXISTS_MSG = "商品类别编号已存在";
    //根类别不支持修改
    public static final int MATERIAL_CATEGORY_ROOT_NOT_SUPPORT_EDIT_CODE = 7500004;
    public static final String MATERIAL_CATEGORY_ROOT_NOT_SUPPORT_EDIT_MSG = "根类别不支持修改";
    //根类别不支持删除
    public static final int MATERIAL_CATEGORY_ROOT_NOT_SUPPORT_DELETE_CODE = 7500005;
    public static final String MATERIAL_CATEGORY_ROOT_NOT_SUPPORT_DELETE_MSG = "根类别不支持删除";
    //该类别存在下级不允许删除
    public static final int MATERIAL_CATEGORY_CHILD_NOT_SUPPORT_DELETE_CODE = 7500006;
    public static final String MATERIAL_CATEGORY_CHILD_NOT_SUPPORT_DELETE_MSG = "该类别存在下级不允许删除";
    public static final int MATERIAL_CATEGORY_PERMISSION_CODE = 7500007;
    public static final String MATERIAL_CATEGORY_PERMISSION_MSG = "抱歉，当前用户没有商品类别的编辑权限";
    public static final int MATERIAL_CATEGORY_NAME_ALREADY_EXISTS_CODE = 7500008;
    public static final String MATERIAL_CATEGORY_NAME_ALREADY_EXISTS_MSG = "同一上级目录下商品类别名称已存在";
    public static final int MATERIAL_CATEGORY_PARENT_INVALID_CODE = 7500009;
    public static final String MATERIAL_CATEGORY_PARENT_INVALID_MSG = "商品类别上级目录无效";
    public static final int MATERIAL_CATEGORY_CYCLE_CODE = 7500010;
    public static final String MATERIAL_CATEGORY_CYCLE_MSG = "商品类别上级目录不能是自身或自身的下级";
    public static final int MATERIAL_CATEGORY_NOT_EXISTS_CODE = 7500011;
    public static final String MATERIAL_CATEGORY_NOT_EXISTS_MSG = "商品类别不存在";
    public static final int MATERIAL_CATEGORY_NAME_AMBIGUOUS_CODE = 7500012;
    public static final String MATERIAL_CATEGORY_NAME_AMBIGUOUS_MSG = "商品类别名称存在重复，请使用唯一的类别名称后再导入";
    public static final int MATERIAL_CATEGORY_REQUIRED_CODE = 7500013;
    public static final String MATERIAL_CATEGORY_REQUIRED_MSG = "商品类别名称和编号不能为空";
    /**
     * 商品信息
     * type = 80
     * */
    //商品信息不存在
    public static final int MATERIAL_NOT_EXISTS_CODE = 8000000;
    public static final String MATERIAL_NOT_EXISTS_MSG = "商品信息不存在";
    //商品信息不唯一
    public static final int MATERIAL_NOT_ONLY_CODE = 8000001;
    public static final String MATERIAL_NOT_ONLY_MSG = "商品信息不唯一";
    //该商品未开启序列号
    public static final int MATERIAL_NOT_ENABLE_SERIAL_NUMBER_CODE = 8000002;
    public static final String MATERIAL_NOT_ENABLE_SERIAL_NUMBER_MSG = "该商品未开启序列号功能";
    //商品的序列号不能为空
    public static final int MATERIAL_SERIAL_NUMBERE_EMPTY_CODE = 8000003;
    public static final String MATERIAL_SERIAL_NUMBERE_EMPTY_MSG = "抱歉，商品条码:%s的序列号不能为空";
    //商品库存不足
    public static final int MATERIAL_STOCK_NOT_ENOUGH_CODE = 8000004;
    public static final String MATERIAL_STOCK_NOT_ENOUGH_MSG = "商品:%s库存不足";
    //商品条码重复
    public static final int MATERIAL_BARCODE_EXISTS_CODE = 8000005;
    public static final String MATERIAL_BARCODE_EXISTS_MSG = "商品条码:%s重复";
    //商品-单位匹配不上
    public static final int MATERIAL_UNIT_MATE_CODE = 8000006;
    public static final String MATERIAL_UNIT_MATE_MSG = "抱歉，商品条码:%s的单位匹配不上，请完善多单位信息！";
    //商品条码长度应该为4到40位
    public static final int MATERIAL_BARCODE_LENGTH_ERROR_CODE = 8000007;
    public static final String MATERIAL_BARCODE_LENGTH_ERROR_MSG = "商品条码:%s的长度应该为4到40位";
    //序列号和批号只能有一项
    public static final int MATERIAL_ENABLE_MUST_ONE_CODE = 8000008;
    public static final String MATERIAL_ENABLE_MUST_ONE_MSG = "抱歉，商品条码:%s的序列号和批号不能同时填1";
    //名称为空
    public static final int MATERIAL_NAME_EMPTY_CODE = 8000010;
    public static final String MATERIAL_NAME_EMPTY_MSG = "第%s行名称为空";
    //基本单位为空
    public static final int MATERIAL_UNIT_EMPTY_CODE = 8000011;
    public static final String MATERIAL_UNIT_EMPTY_MSG = "第%s行基本单位为空";
    //状态格式错误
    public static final int MATERIAL_ENABLED_ERROR_CODE = 8000012;
    public static final String MATERIAL_ENABLED_ERROR_MSG = "第%s行状态格式错误";
    //单次导入超出1000条
    public static final int MATERIAL_IMPORT_OVER_LIMIT_CODE = 8000013;
    public static final String MATERIAL_IMPORT_OVER_LIMIT_MSG = "抱歉，单次导入不能超出1000条";
    //基础重量格式错误
    public static final int MATERIAL_WEIGHT_NOT_DECIMAL_CODE = 8000014;
    public static final String MATERIAL_WEIGHT_NOT_DECIMAL_MSG = "第%s行基础重量格式错误";
    //保质期格式错误
    public static final int MATERIAL_EXPIRY_NUM_NOT_INTEGER_CODE = 8000015;
    public static final String MATERIAL_EXPIRY_NUM_NOT_INTEGER_MSG = "第%s行保质期格式错误";
    //比例格式错误
    public static final int MATERIAL_RATIO_NOT_INTEGER_CODE = 8000016;
    public static final String MATERIAL_RATIO_NOT_INTEGER_MSG = "第%s行比例格式错误";
    //组装拆卸单不能选择批号或序列号商品
    public static final int MATERIAL_ASSEMBLE_SELECT_ERROR_CODE = 80000017;
    public static final String MATERIAL_ASSEMBLE_SELECT_ERROR_MSG = "抱歉，组装拆卸单不能选择批号或序列号商品:%s";
    //调拨单不能选择批号或序列号商品
    public static final int MATERIAL_TRANSFER_SELECT_ERROR_CODE = 80000018;
    public static final String MATERIAL_TRANSFER_SELECT_ERROR_MSG = "抱歉，调拨单不能选择批号或序列号商品:%s，建议走其它入库和出库单";
    //盘点业务不能选择批号或序列号商品
    public static final int MATERIAL_STOCK_CHECK_ERROR_CODE = 80000019;
    public static final String MATERIAL_STOCK_CHECK_ERROR_MSG = "抱歉，盘点业务不能选择批号或序列号商品:%s，建议走其它入库和出库单";
    //EXCEL中存在重复的商品
    public static final int MATERIAL_EXCEL_IMPORT_EXIST_CODE = 80000020;
    public static final String MATERIAL_EXCEL_IMPORT_EXIST_MSG = "抱歉，EXCEL中存在重复的商品，具体信息为：%s";
    //EXCEL中存在重复的条码
    public static final int MATERIAL_EXCEL_IMPORT_BARCODE_EXIST_CODE = 80000021;
    public static final String MATERIAL_EXCEL_IMPORT_BARCODE_EXIST_MSG = "抱歉，EXCEL中存在重复的条码，具体条码为：%s";
    //名称长度超出
    public static final int MATERIAL_NAME_OVER_CODE = 8000022;
    public static final String MATERIAL_NAME_OVER_MSG = "第%s行名称长度超出100个字符";
    //规格长度超出
    public static final int MATERIAL_STANDARD_OVER_CODE = 8000023;
    public static final String MATERIAL_STANDARD_OVER_MSG = "第%s行规格长度超出100个字符";
    //型号长度超出
    public static final int MATERIAL_MODEL_OVER_CODE = 8000024;
    public static final String MATERIAL_MODEL_OVER_MSG = "第%s行型号长度超出100个字符";
    //多属性商品不能输入库存，建议进行盘点录入
    public static final int MATERIAL_SKU_BEGIN_STOCK_FAILED_CODE = 8000025;
    public static final int MATERIAL_INITIAL_STOCK_INVALID_CODE = 8000030;
    public static final String MATERIAL_INITIAL_STOCK_INVALID_MSG = "商品期初库存设置不合法：%s";
    public static final int MATERIAL_PERMISSION_CODE = 8000031;
    public static final String MATERIAL_PERMISSION_MSG = "抱歉，当前用户没有商品信息的编辑权限";
    public static final int MATERIAL_UNIT_CONFIG_INVALID_CODE = 8000032;
    public static final String MATERIAL_UNIT_CONFIG_INVALID_MSG = "商品单位设置不合法：%s";
    public static final int MATERIAL_UNIT_HISTORY_LOCK_CODE = 8000033;
    public static final String MATERIAL_UNIT_HISTORY_LOCK_MSG = "商品已有历史单据，不允许修改单位或多单位方案";
    public static final int MATERIAL_SKU_CONFIG_INVALID_CODE = 8000034;
    public static final String MATERIAL_SKU_CONFIG_INVALID_MSG = "多属性明细设置不合法：%s";
    public static final int MATERIAL_SKU_HISTORY_LOCK_CODE = 8000035;
    public static final String MATERIAL_SKU_HISTORY_LOCK_MSG = "多属性明细已被历史单据使用，不允许修改或删除";
    public static final String MATERIAL_SKU_BEGIN_STOCK_FAILED_MSG = "多属性商品%s不能输入库存，建议进行盘点录入";
    //商品条码不存在，请重新选择
    public static final int MATERIAL_BARCODE_IS_NOT_EXIST_CODE = 8000026;
    public static final String MATERIAL_BARCODE_IS_NOT_EXIST_MSG = "商品条码%s不存在，请重新选择";
    //基本条码为空
    public static final int MATERIAL_BARCODE_EMPTY_CODE = 8000027;
    public static final String MATERIAL_BARCODE_EMPTY_MSG = "第%s行基本条码为空";
    //EXCEL中有副条码在系统中已存在（除自身商品之外）
    public static final int MATERIAL_EXCEL_IMPORT_MANY_BARCODE_EXIST_CODE = 80000028;
    public static final String MATERIAL_EXCEL_IMPORT_MANY_BARCODE_EXIST_MSG = "抱歉，EXCEL中有副条码在系统中已存在，具体副条码为：%s";
    //单据中商品库存不足
    public static final int BILL_MATERIAL_STOCK_NOT_ENOUGH_CODE = 80000029;
    public static final String BILL_MATERIAL_STOCK_NOT_ENOUGH_MSG = "单据:%s中商品:%s库存不足";

    /**
     *  单据信息
     * type = 85
     * */
    //添加单据信息失败
    public static final int DEPOT_HEAD_ADD_FAILED_CODE = 8500000;
    public static final String DEPOT_HEAD_ADD_FAILED_MSG = "添加单据信息失败";
    //删除单据信息失败
    public static final int DEPOT_HEAD_DELETE_FAILED_CODE = 8500001;
    public static final String DEPOT_HEAD_DELETE_FAILED_MSG = "删除单据信息失败";
    //修改单据信息失败
    public static final int DEPOT_HEAD_EDIT_FAILED_CODE = 8500002;
    public static final String DEPOT_HEAD_EDIT_FAILED_MSG = "修改单据信息失败";
    //单据录入-仓库不能为空
    public static final int DEPOT_HEAD_DEPOT_FAILED_CODE = 8500004;
    public static final String DEPOT_HEAD_DEPOT_FAILED_MSG = "仓库不能为空";
    //单据录入-调入仓库不能为空
    public static final int DEPOT_HEAD_ANOTHER_DEPOT_FAILED_CODE = 8500005;
    public static final String DEPOT_HEAD_ANOTHER_DEPOT_FAILED_MSG = "调入仓库不能为空";
    //单据录入-明细不能为空
    public static final int DEPOT_HEAD_ROW_FAILED_CODE = 8500006;
    public static final String DEPOT_HEAD_ROW_FAILED_MSG = "单据明细不能为空";
    //单据录入-账户不能为空
    public static final int DEPOT_HEAD_ACCOUNT_FAILED_CODE = 8500007;
    public static final String DEPOT_HEAD_ACCOUNT_FAILED_MSG = "结算账户不能为空";
    //单据录入-请修改多账户的结算金额
    public static final int DEPOT_HEAD_MANY_ACCOUNT_FAILED_CODE = 8500008;
    public static final String DEPOT_HEAD_MANY_ACCOUNT_FAILED_MSG = "请修改多账户的结算金额";
    //单据录入-调入仓库与原仓库不能重复
    public static final int DEPOT_HEAD_ANOTHER_DEPOT_EQUAL_FAILED_CODE = 8500010;
    public static final String DEPOT_HEAD_ANOTHER_DEPOT_EQUAL_FAILED_MSG = "调入仓库与原仓库不能重复";
    //单据删除-只有未审核的单据才能删除，请先进行反审核
    public static final int DEPOT_HEAD_UN_AUDIT_DELETE_FAILED_CODE = 8500011;
    public static final String DEPOT_HEAD_UN_AUDIT_DELETE_FAILED_MSG = "抱歉，只有未审核的单据才能删除，请先进行反审核";
    //单据审核-只有未审核的单据才能审核
    public static final int DEPOT_HEAD_UN_AUDIT_TO_AUDIT_FAILED_CODE = 8500012;
    public static final String DEPOT_HEAD_UN_AUDIT_TO_AUDIT_FAILED_MSG = "抱歉，只有未审核的单据才能审核";
    //单据反审核-只有已审核的单据才能反审核
    public static final int DEPOT_HEAD_AUDIT_TO_UN_AUDIT_FAILED_CODE = 8500013;
    public static final String DEPOT_HEAD_AUDIT_TO_UN_AUDIT_FAILED_MSG = "抱歉，只有已审核的单据才能反审核";
    //单据录入-商品条码XXX的数量需要修改下
    public static final int DEPOT_HEAD_NUMBER_NEED_EDIT_FAILED_CODE = 85000014;
    public static final String DEPOT_HEAD_NUMBER_NEED_EDIT_FAILED_MSG = "商品条码%s的数量需要修改下";
    //单据录入-商品的批号不能为空
    public static final int DEPOT_HEAD_BATCH_NUMBERE_EMPTY_CODE = 8500015;
    public static final String DEPOT_HEAD_BATCH_NUMBERE_EMPTY_MSG = "抱歉，商品条码:%s的批号不能为空";
    //单据录入-会员预付款余额不足
    public static final int DEPOT_HEAD_MEMBER_PAY_LACK_CODE = 8500016;
    public static final String DEPOT_HEAD_MEMBER_PAY_LACK_MSG = "抱歉，会员预付款余额不足";
    //单据录入-累计订金超出原订单中的订金
    public static final int DEPOT_HEAD_DEPOSIT_OVER_PRE_CODE = 8500017;
    public static final String DEPOT_HEAD_DEPOSIT_OVER_PRE_MSG = "抱歉，累计订金超出原订单中的订金";
    //单据录入-商品条码XXX的单价低于最低售价
    public static final int DEPOT_HEAD_UNIT_PRICE_LOW_CODE = 8500018;
    public static final String DEPOT_HEAD_UNIT_PRICE_LOW_MSG = "商品条码%s的单价低于最低售价";
    //单据录入-单据明细中必须要有组合件和普通子件
    public static final int DEPOT_HEAD_CHECK_ASSEMBLE_EMPTY_CODE = 8500020;
    public static final String DEPOT_HEAD_CHECK_ASSEMBLE_EMPTY_MSG = "抱歉，单据明细中必须要有组合件和普通子件";
    //单据录入-商品条码XXX的数量与序列号不一致
    public static final int DEPOT_HEAD_SN_NUMBERE_FAILED_CODE = 8500021;
    public static final String DEPOT_HEAD_SN_NUMBERE_FAILED_MSG = "抱歉，商品条码:%s的数量与序列号不一致";
    //单据录入-单据编号已经存在
    public static final int DEPOT_HEAD_BILL_NUMBER_EXIST_CODE = 8500022;
    public static final String DEPOT_HEAD_BILL_NUMBER_EXIST_MSG = "抱歉，单据编号已经存在";
    //单据录入-单据当前状态下不能修改
    public static final int DEPOT_HEAD_BILL_CANNOT_EDIT_CODE = 8500023;
    public static final String DEPOT_HEAD_BILL_CANNOT_EDIT_MSG = "抱歉，单据当前状态下不能修改";
    //单据删除-单据中的序列号已经出库，不能删除
    public static final int DEPOT_HEAD_SERIAL_IS_SELL_CODE = 8500024;
    public static final String DEPOT_HEAD_SERIAL_IS_SELL_MSG = "抱歉，单据%s的序列号已经出库，不能删除";
    //单据录入-单据附件不能超过规定数量
    public static final int DEPOT_HEAD_FILE_NUM_LIMIT_CODE = 8500025;
    public static final String DEPOT_HEAD_FILE_NUM_LIMIT_MSG = "抱歉，单据附件不能超过%s份";
    //单据录入-完成采购的单据不能进行反审核
    public static final int DEPOT_HEAD_PURCHASE_STATUS_TWO_CODE = 8500026;
    public static final String DEPOT_HEAD_PURCHASE_STATUS_TWO_MSG = "抱歉，完成采购的单据不能进行反审核";
    //单据录入-部分采购的单据不能进行反审核
    public static final int DEPOT_HEAD_PURCHASE_STATUS_THREE_CODE = 8500027;
    public static final String DEPOT_HEAD_PURCHASE_STATUS_THREE_MSG = "抱歉，部分采购的单据不能进行反审核";
    //单据录入-单据中存在部分状态，需要到新增界面关联单据
    public static final int DEPOT_ITEM_EXIST_PARTIALLY_STATUS_FAILED_CODE = 8500028;
    public static final String DEPOT_ITEM_EXIST_PARTIALLY_STATUS_FAILED_MSG = "抱歉，单据:%s是部分%s状态，需要到新增界面关联单据";
    //单据录入-关联请购单号和关联订单号不能同时录入
    public static final int DEPOT_ITEM_EXIST_REPEAT_NO_FAILED_CODE = 8500029;
    public static final String DEPOT_ITEM_EXIST_REPEAT_NO_FAILED_MSG = "抱歉，关联请购单号和关联订单号不能同时录入";
    //单据录入-单据最新状态不能进行批量操作
    public static final int DEPOT_ITEM_EXIST_NEW_STATUS_FAILED_CODE = 8500030;
    public static final String DEPOT_ITEM_EXIST_NEW_STATUS_FAILED_MSG = "抱歉，单据:%s最新状态不能进行批量操作";
    //单据录入-单据在该状态不能强制结单
    public static final int DEPOT_HEAD_FORCE_CLOSE_FAILED_CODE = 8500031;
    public static final String DEPOT_HEAD_FORCE_CLOSE_FAILED_MSG = "抱歉，单据:%s在该状态不能强制结单";
    //单据录入-请勿频繁提交单据
    public static final int DEPOT_HEAD_SUBMIT_REPEAT_FAILED_CODE = 8500032;
    public static final String DEPOT_HEAD_SUBMIT_REPEAT_FAILED_MSG = "抱歉，请不要频繁提交单据";
    //单据录入-单据明细中存在重复的序列号
    public static final int DEPOT_HEAD_CHECK_SERIAL_NUMBER_REPEAT_CODE = 8500033;
    public static final String DEPOT_HEAD_CHECK_SERIAL_NUMBER_REPEAT_MSG = "抱歉，单据明细中存在重复的序列号:%s";
    //单据录入-序列号已被出库，不允许修改该单据
    public static final int DEPOT_HEAD_SN_NOT_ALLOW_UPDATE_CODE = 8500035;
    public static final String DEPOT_HEAD_SN_NOT_ALLOW_UPDATE_MSG = "抱歉，序列号:%s已被出库，不允许修改该单据";
    //单据录入-商品数量必须大于0
    public static final int DEPOT_HEAD_NUMBER_MUST_POSITIVE_CODE = 8500036;
    public static final String DEPOT_HEAD_NUMBER_MUST_POSITIVE_MSG = "抱歉，商品条码:%s的数量必须大于0";
    //零售出库-主表金额和明细金额不一致
    public static final int DEPOT_HEAD_RETAIL_AMOUNT_MISMATCH_CODE = 8500037;
    public static final String DEPOT_HEAD_RETAIL_AMOUNT_MISMATCH_MSG = "抱歉，零售出库的单据金额与商品明细金额不一致";
    //零售出库-收款金额不足
    public static final int DEPOT_HEAD_RETAIL_RECEIPT_LACK_CODE = 8500038;
    public static final String DEPOT_HEAD_RETAIL_RECEIPT_LACK_MSG = "抱歉，零售出库不能欠款，收款金额不能小于单据金额";
    //零售出库-付款类型不正确
    public static final int DEPOT_HEAD_RETAIL_PAY_TYPE_CODE = 8500039;
    public static final String DEPOT_HEAD_RETAIL_PAY_TYPE_MSG = "抱歉，零售出库的付款类型只能是现付或预付款";
    //零售出库-预付款必须选择会员
    public static final int DEPOT_HEAD_RETAIL_MEMBER_REQUIRED_CODE = 8500040;
    public static final String DEPOT_HEAD_RETAIL_MEMBER_REQUIRED_MSG = "抱歉，使用预付款时必须选择会员";
    //业务单据-无按钮权限
    public static final int DEPOT_HEAD_RETAIL_PERMISSION_CODE = 8500041;
    public static final String DEPOT_HEAD_RETAIL_PERMISSION_MSG = "抱歉，当前用户没有%s的%s权限";
    //单据录入-不能修改单据业务类型
    public static final int DEPOT_HEAD_BILL_TYPE_CHANGE_CODE = 8500042;
    public static final String DEPOT_HEAD_BILL_TYPE_CHANGE_MSG = "抱歉，不能修改单据的业务类型";
    //单据录入-单据业务类型不合法
    public static final int DEPOT_HEAD_BILL_TYPE_INVALID_CODE = 8500043;
    public static final String DEPOT_HEAD_BILL_TYPE_INVALID_MSG = "抱歉，单据类型与业务类型不匹配";
    //零售退货-关联原单不合法
    public static final int DEPOT_HEAD_RETAIL_RETURN_SOURCE_CODE = 8500044;
    public static final String DEPOT_HEAD_RETAIL_RETURN_SOURCE_MSG = "抱歉，零售退货关联的原单不存在、未审核或不是零售出库单";
    //零售退货-会员与原单不一致
    public static final int DEPOT_HEAD_RETAIL_RETURN_MEMBER_CODE = 8500045;
    public static final String DEPOT_HEAD_RETAIL_RETURN_MEMBER_MSG = "抱歉，零售退货会员必须与原零售出库单一致";
    //零售退货-明细不属于原单
    public static final int DEPOT_HEAD_RETAIL_RETURN_DETAIL_CODE = 8500046;
    public static final String DEPOT_HEAD_RETAIL_RETURN_DETAIL_MSG = "抱歉，退货商品明细不属于关联的零售出库单";
    //零售退货-超过原单可退数量
    public static final int DEPOT_HEAD_RETAIL_RETURN_OVER_CODE = 8500047;
    public static final String DEPOT_HEAD_RETAIL_RETURN_OVER_MSG = "抱歉，商品条码:%s的累计退货数量超过原零售出库数量";
    //零售退货-退款金额不足
    public static final int DEPOT_HEAD_RETAIL_RETURN_REFUND_CODE = 8500048;
    public static final String DEPOT_HEAD_RETAIL_RETURN_REFUND_MSG = "抱歉，零售退货的付款金额不能小于退货金额";
    //零售退货-序列号不属于原单
    public static final int DEPOT_HEAD_RETAIL_RETURN_SN_CODE = 8500049;
    public static final String DEPOT_HEAD_RETAIL_RETURN_SN_MSG = "抱歉，序列号:%s不属于关联的零售出库单";
    //采购订单-关联请购单不合法
    public static final int DEPOT_HEAD_PURCHASE_APPLY_SOURCE_CODE = 8500050;
    public static final String DEPOT_HEAD_PURCHASE_APPLY_SOURCE_MSG = "抱歉，关联的请购单不存在、状态不可用或不是请购单";
    //采购订单-关联请购明细不合法
    public static final int DEPOT_HEAD_PURCHASE_APPLY_DETAIL_CODE = 8500051;
    public static final String DEPOT_HEAD_PURCHASE_APPLY_DETAIL_MSG = "抱歉，采购订单明细不属于关联的请购单";
    //采购订单-超过请购数量
    public static final int DEPOT_HEAD_PURCHASE_APPLY_OVER_CODE = 8500052;
    public static final String DEPOT_HEAD_PURCHASE_APPLY_OVER_MSG = "抱歉，商品条码:%s的累计采购数量超过请购数量";
    //采购订单-不能更换关联请购单
    public static final int DEPOT_HEAD_PURCHASE_APPLY_CHANGE_CODE = 8500053;
    public static final String DEPOT_HEAD_PURCHASE_APPLY_CHANGE_MSG = "抱歉，已关联请购单的采购订单不能更换或取消关联";
    //采购业务-必须选择有效供应商
    public static final int DEPOT_HEAD_PURCHASE_SUPPLIER_CODE = 8500054;
    public static final String DEPOT_HEAD_PURCHASE_SUPPLIER_MSG = "抱歉，采购业务必须选择有效且已启用的供应商";
    //采购业务-金额字段不合法
    public static final int DEPOT_HEAD_PURCHASE_AMOUNT_CODE = 8500055;
    public static final String DEPOT_HEAD_PURCHASE_AMOUNT_MSG = "抱歉，采购单据的价格、税率、折扣或付款金额不合法";
    //采购订单-关联销售订单不合法
    public static final int DEPOT_HEAD_PURCHASE_SALES_SOURCE_CODE = 8500056;
    public static final String DEPOT_HEAD_PURCHASE_SALES_SOURCE_MSG = "抱歉，关联的销售订单不存在、状态不可用或不是销售订单";
    //采购订单-关联销售订单明细不合法
    public static final int DEPOT_HEAD_PURCHASE_SALES_DETAIL_CODE = 8500057;
    public static final String DEPOT_HEAD_PURCHASE_SALES_DETAIL_MSG = "抱歉，采购订单明细不属于关联的销售订单";
    //采购订单-超过销售订单数量
    public static final int DEPOT_HEAD_PURCHASE_SALES_OVER_CODE = 8500058;
    public static final String DEPOT_HEAD_PURCHASE_SALES_OVER_MSG = "抱歉，商品条码:%s的累计采购数量超过销售订单数量";
    //采购订单-不能修改来源
    public static final int DEPOT_HEAD_PURCHASE_LINK_CHANGE_CODE = 8500059;
    public static final String DEPOT_HEAD_PURCHASE_LINK_CHANGE_MSG = "抱歉，采购订单保存后不能更换或取消关联来源";
    //采购入库-关联采购订单不合法
    public static final int DEPOT_HEAD_PURCHASE_IN_SOURCE_CODE = 8500060;
    public static final String DEPOT_HEAD_PURCHASE_IN_SOURCE_MSG = "抱歉，关联的采购订单不存在、状态不可用或不是采购订单";
    //采购入库-关联采购订单明细不合法
    public static final int DEPOT_HEAD_PURCHASE_IN_DETAIL_CODE = 8500061;
    public static final String DEPOT_HEAD_PURCHASE_IN_DETAIL_MSG = "抱歉，采购入库明细不属于关联的采购订单";
    //采购入库-超过采购订单数量
    public static final int DEPOT_HEAD_PURCHASE_IN_OVER_CODE = 8500062;
    public static final String DEPOT_HEAD_PURCHASE_IN_OVER_MSG = "抱歉，商品条码:%s的累计入库数量超过采购订单数量";
    //单据明细-商品单位不合法
    public static final int DEPOT_HEAD_MATERIAL_UNIT_CODE = 8500063;
    public static final String DEPOT_HEAD_MATERIAL_UNIT_MSG = "抱歉，商品条码:%s的单位不属于该商品";
    //采购入库-单据状态只能由服务端按保存或审核流程设置
    public static final int DEPOT_HEAD_PURCHASE_IN_STATUS_CODE = 8500064;
    public static final String DEPOT_HEAD_PURCHASE_IN_STATUS_MSG = "抱歉，采购入库单状态不合法，请通过保存或审核操作变更状态";
    //采购入库-存在采购退货时不能修改、反审核或删除
    public static final int DEPOT_HEAD_PURCHASE_IN_HAS_RETURN_CODE = 8500065;
    public static final String DEPOT_HEAD_PURCHASE_IN_HAS_RETURN_MSG = "抱歉，采购入库单已存在采购退货，不能%s，请先删除关联的采购退货单";
    //采购入库-无仓库或单据数据权限
    public static final int DEPOT_HEAD_PURCHASE_IN_DATA_PERMISSION_CODE = 8500066;
    public static final String DEPOT_HEAD_PURCHASE_IN_DATA_PERMISSION_MSG = "抱歉，当前用户没有采购入库单或所选仓库的数据权限";
    //采购入库-直接入库不能扣除采购订单订金
    public static final int DEPOT_HEAD_PURCHASE_IN_DEPOSIT_SOURCE_CODE = 8500067;
    public static final String DEPOT_HEAD_PURCHASE_IN_DEPOSIT_SOURCE_MSG = "抱歉，只有关联采购订单的采购入库单才能扣除订金";
    //采购入库-保存后不能修改单据编号
    public static final int DEPOT_HEAD_PURCHASE_IN_NUMBER_CHANGE_CODE = 8500068;
    public static final String DEPOT_HEAD_PURCHASE_IN_NUMBER_CHANGE_MSG = "抱歉，采购入库单保存后不能修改单据编号";
    //采购退货-单据状态只能由保存或审核流程设置
    public static final int DEPOT_HEAD_PURCHASE_RETURN_STATUS_CODE = 8500069;
    public static final String DEPOT_HEAD_PURCHASE_RETURN_STATUS_MSG = "抱歉，采购退货单状态不合法，请通过保存或审核操作变更状态";
    //采购退货-关联采购入库不合法
    public static final int DEPOT_HEAD_PURCHASE_RETURN_SOURCE_CODE = 8500070;
    public static final String DEPOT_HEAD_PURCHASE_RETURN_SOURCE_MSG = "抱歉，关联的采购入库单不存在、未审核或不允许退货";
    //采购退货-关联明细不合法
    public static final int DEPOT_HEAD_PURCHASE_RETURN_DETAIL_CODE = 8500071;
    public static final String DEPOT_HEAD_PURCHASE_RETURN_DETAIL_MSG = "抱歉，采购退货明细与关联采购入库不一致";
    //采购退货-退货数量超出采购入库数量
    public static final int DEPOT_HEAD_PURCHASE_RETURN_OVER_CODE = 8500072;
    public static final String DEPOT_HEAD_PURCHASE_RETURN_OVER_MSG = "抱歉，商品条码:%s的累计采购退货数量不能超过采购入库数量";
    //采购退货-数据权限
    public static final int DEPOT_HEAD_PURCHASE_RETURN_DATA_PERMISSION_CODE = 8500073;
    public static final String DEPOT_HEAD_PURCHASE_RETURN_DATA_PERMISSION_MSG = "抱歉，当前用户没有采购退货单或所选仓库的数据权限";
    //采购退货-单号或关联关系不能修改
    public static final int DEPOT_HEAD_PURCHASE_RETURN_LINK_CHANGE_CODE = 8500074;
    public static final String DEPOT_HEAD_PURCHASE_RETURN_LINK_CHANGE_MSG = "抱歉，采购退货单保存后不能修改单据编号或关联采购入库单";
    //采购退货-金额不合法
    public static final int DEPOT_HEAD_PURCHASE_RETURN_AMOUNT_CODE = 8500075;
    public static final String DEPOT_HEAD_PURCHASE_RETURN_AMOUNT_MSG = "抱歉，采购退货的价格、税率、优惠、其它费用或退款金额不合法";
    //采购退货-序列号与来源不一致
    public static final int DEPOT_HEAD_PURCHASE_RETURN_SERIAL_CODE = 8500076;
    public static final String DEPOT_HEAD_PURCHASE_RETURN_SERIAL_MSG = "抱歉，采购退货序列号不属于关联入库明细或所选仓库";
    //销售订单/销售出库-必须选择有效客户
    public static final int DEPOT_HEAD_SALES_CUSTOMER_CODE = 8500077;
    public static final String DEPOT_HEAD_SALES_CUSTOMER_MSG = "抱歉，销售业务必须选择有效且已启用的客户";
    //销售订单/销售出库-金额不合法
    public static final int DEPOT_HEAD_SALES_AMOUNT_CODE = 8500078;
    public static final String DEPOT_HEAD_SALES_AMOUNT_MSG = "抱歉，销售单据的价格、税率、折扣、订金或收款金额不合法";
    //销售订单/销售出库-状态不允许由客户端伪造
    public static final int DEPOT_HEAD_SALES_STATUS_CODE = 8500079;
    public static final String DEPOT_HEAD_SALES_STATUS_MSG = "抱歉，销售单据状态不合法，请通过保存、审核或业务流转操作变更状态";
    //销售订单/销售出库-保存后不能修改单号或关联来源
    public static final int DEPOT_HEAD_SALES_LINK_CHANGE_CODE = 8500080;
    public static final String DEPOT_HEAD_SALES_LINK_CHANGE_MSG = "抱歉，销售单据保存后不能修改单据编号、类型或关联销售订单";
    //销售出库-来源订单不合法
    public static final int DEPOT_HEAD_SALES_OUT_SOURCE_CODE = 8500081;
    public static final String DEPOT_HEAD_SALES_OUT_SOURCE_MSG = "抱歉，关联的销售订单不存在、状态不可用或客户不一致";
    //销售出库-来源明细不合法
    public static final int DEPOT_HEAD_SALES_OUT_DETAIL_CODE = 8500082;
    public static final String DEPOT_HEAD_SALES_OUT_DETAIL_MSG = "抱歉，销售出库明细不属于关联的销售订单";
    //销售出库-累计数量超过订单
    public static final int DEPOT_HEAD_SALES_OUT_OVER_CODE = 8500083;
    public static final String DEPOT_HEAD_SALES_OUT_OVER_MSG = "抱歉，商品条码:%s的累计出库数量超过销售订单数量";
    //销售业务-数据权限
    public static final int DEPOT_HEAD_SALES_DATA_PERMISSION_CODE = 8500084;
    public static final String DEPOT_HEAD_SALES_DATA_PERMISSION_MSG = "抱歉，当前用户没有该销售单据、客户或仓库的数据权限";

    public static final int DEPOT_HEAD_SALES_ORDER_HAS_OUTBOUND_CODE = 8500085;
    public static final String DEPOT_HEAD_SALES_ORDER_HAS_OUTBOUND_MSG = "抱歉，该销售订单已关联销售出库单，不能%s";

    public static final int DEPOT_HEAD_SALES_OUT_HAS_RETURN_CODE = 8500086;
    public static final String DEPOT_HEAD_SALES_OUT_HAS_RETURN_MSG = "抱歉，该销售出库单已关联销售退货单，不能%s";

    public static final int DEPOT_HEAD_SALES_OUT_HAS_FINANCIAL_CODE = 8500087;
    public static final String DEPOT_HEAD_SALES_OUT_HAS_FINANCIAL_MSG = "抱歉，该销售出库单已关联收款单，不能%s";

    public static final int DEPOT_HEAD_SALES_ACCOUNT_INVALID_CODE = 8500088;
    public static final String DEPOT_HEAD_SALES_ACCOUNT_INVALID_MSG = "抱歉，销售单据的结算账户不存在、已停用或多账户数据不合法";

    public static final int DEPOT_DATA_PERMISSION_CODE = 8500089;
    public static final String DEPOT_DATA_PERMISSION_MSG = "抱歉，当前用户没有所选仓库的数据权限";

    //销售退货-关联销售出库不合法
    public static final int DEPOT_HEAD_SALES_RETURN_SOURCE_CODE = 8500090;
    public static final String DEPOT_HEAD_SALES_RETURN_SOURCE_MSG = "抱歉，关联的销售出库单不存在、状态不可用或客户不一致";
    //销售退货-关联明细不合法
    public static final int DEPOT_HEAD_SALES_RETURN_DETAIL_CODE = 8500091;
    public static final String DEPOT_HEAD_SALES_RETURN_DETAIL_MSG = "抱歉，销售退货明细与关联销售出库不一致";
    //销售退货-累计数量超过销售出库数量
    public static final int DEPOT_HEAD_SALES_RETURN_OVER_CODE = 8500092;
    public static final String DEPOT_HEAD_SALES_RETURN_OVER_MSG = "抱歉，商品条码:%s的累计销售退货数量不能超过销售出库数量";
    //销售退货-序列号与来源不一致
    public static final int DEPOT_HEAD_SALES_RETURN_SERIAL_CODE = 8500093;
    public static final String DEPOT_HEAD_SALES_RETURN_SERIAL_MSG = "抱歉，销售退货序列号:%s不属于关联销售出库明细";
    //销售退货-已关联财务单据
    public static final int DEPOT_HEAD_SALES_RETURN_HAS_FINANCIAL_CODE = 8500094;
    public static final String DEPOT_HEAD_SALES_RETURN_HAS_FINANCIAL_MSG = "抱歉，该销售退货单已关联退款或收付款单，不能%s";

    //其它出入库
    public static final int DEPOT_HEAD_OTHER_STATUS_CODE = 8500095;
    public static final String DEPOT_HEAD_OTHER_STATUS_MSG = "抱歉，其它出入库单状态不合法，请通过保存或审核操作变更状态";

    public static final int DEPOT_HEAD_OTHER_LINK_CHANGE_CODE = 8500096;
    public static final String DEPOT_HEAD_OTHER_LINK_CHANGE_MSG = "抱歉，其它出入库单的编号或关联单据不能修改";

    public static final int DEPOT_HEAD_OTHER_IN_SOURCE_CODE = 8500097;
    public static final String DEPOT_HEAD_OTHER_IN_SOURCE_MSG = "抱歉，关联单据不存在、状态不可用或不是采购入库/销售退货单";

    public static final int DEPOT_HEAD_OTHER_IN_DETAIL_CODE = 8500098;
    public static final String DEPOT_HEAD_OTHER_IN_DETAIL_MSG = "抱歉，其它入库明细与关联单据不一致";

    public static final int DEPOT_HEAD_OTHER_IN_OVER_CODE = 8500099;
    public static final String DEPOT_HEAD_OTHER_IN_OVER_MSG = "抱歉，商品条码:%s的累计其它入库数量不能超过来源单数量";

    public static final int DEPOT_HEAD_OTHER_IN_AMOUNT_CODE = 8500100;
    public static final String DEPOT_HEAD_OTHER_IN_AMOUNT_MSG = "抱歉，其它入库的单价或金额不合法";

    public static final int DEPOT_HEAD_OTHER_DATA_PERMISSION_CODE = 8500101;
    public static final String DEPOT_HEAD_OTHER_DATA_PERMISSION_MSG = "抱歉，当前用户没有该其它出入库单或所选仓库的数据权限";

    public static final int DEPOT_HEAD_OTHER_BATCH_SOURCE_CODE = 8500102;
    public static final String DEPOT_HEAD_OTHER_BATCH_SOURCE_MSG = "抱歉，批量转其它入库/出库的来源单类型不合法";

    public static final int DEPOT_HEAD_OTHER_OUT_SOURCE_CODE = 8500103;
    public static final String DEPOT_HEAD_OTHER_OUT_SOURCE_MSG = "抱歉，关联单据不存在、状态不可用或不是销售出库/采购退货单";

    public static final int DEPOT_HEAD_OTHER_OUT_DETAIL_CODE = 8500104;
    public static final String DEPOT_HEAD_OTHER_OUT_DETAIL_MSG = "抱歉，其它出库明细与关联单据不一致";

    public static final int DEPOT_HEAD_OTHER_OUT_OVER_CODE = 8500105;
    public static final String DEPOT_HEAD_OTHER_OUT_OVER_MSG = "抱歉，商品条码:%s的累计其它出库数量不能超过来源单数量";

    public static final int DEPOT_HEAD_OTHER_OUT_AMOUNT_CODE = 8500106;
    public static final String DEPOT_HEAD_OTHER_OUT_AMOUNT_MSG = "抱歉，其它出库的单价或金额不合法";

    //调拨出库
    public static final int DEPOT_HEAD_TRANSFER_STATUS_CODE = 8500107;
    public static final String DEPOT_HEAD_TRANSFER_STATUS_MSG = "抱歉，调拨出库单状态不合法，请通过保存或审核操作变更状态";

    public static final int DEPOT_HEAD_TRANSFER_NUMBER_CHANGE_CODE = 8500108;
    public static final String DEPOT_HEAD_TRANSFER_NUMBER_CHANGE_MSG = "抱歉，调拨出库单编号不能修改";

    public static final int DEPOT_HEAD_TRANSFER_AMOUNT_CODE = 8500109;
    public static final String DEPOT_HEAD_TRANSFER_AMOUNT_MSG = "抱歉，调拨出库的单价或金额不合法";

    //组装单
    public static final int DEPOT_HEAD_ASSEMBLE_STATUS_CODE = 8500110;
    public static final String DEPOT_HEAD_ASSEMBLE_STATUS_MSG = "抱歉，组装单状态不合法，请通过保存或审核操作变更状态";

    public static final int DEPOT_HEAD_ASSEMBLE_NUMBER_CHANGE_CODE = 8500111;
    public static final String DEPOT_HEAD_ASSEMBLE_NUMBER_CHANGE_MSG = "抱歉，组装单编号不能修改";

    public static final int DEPOT_HEAD_ASSEMBLE_AMOUNT_CODE = 8500112;
    public static final String DEPOT_HEAD_ASSEMBLE_AMOUNT_MSG = "抱歉，组装单的成本或金额不合法";

    public static final int DEPOT_HEAD_ASSEMBLE_STRUCTURE_CODE = 8500113;
    public static final String DEPOT_HEAD_ASSEMBLE_STRUCTURE_MSG = "抱歉，组装单必须且只能包含一个组合件，后续明细必须全部为普通子件";

    public static final int DEPOT_HEAD_ASSEMBLE_SAME_MATERIAL_CODE = 8500114;
    public static final String DEPOT_HEAD_ASSEMBLE_SAME_MATERIAL_MSG = "抱歉，组合件不能同时作为普通子件投入:%s";

    //拆卸单
    public static final int DEPOT_HEAD_DISASSEMBLE_STATUS_CODE = 8500115;
    public static final String DEPOT_HEAD_DISASSEMBLE_STATUS_MSG = "抱歉，拆卸单状态不合法，请通过保存或审核操作变更状态";

    public static final int DEPOT_HEAD_DISASSEMBLE_NUMBER_CHANGE_CODE = 8500116;
    public static final String DEPOT_HEAD_DISASSEMBLE_NUMBER_CHANGE_MSG = "抱歉，拆卸单编号不能修改";

    public static final int DEPOT_HEAD_DISASSEMBLE_AMOUNT_CODE = 8500117;
    public static final String DEPOT_HEAD_DISASSEMBLE_AMOUNT_MSG = "抱歉，拆卸单的成本或金额不合法";

    public static final int DEPOT_HEAD_DISASSEMBLE_STRUCTURE_CODE = 8500118;
    public static final String DEPOT_HEAD_DISASSEMBLE_STRUCTURE_MSG = "抱歉，拆卸单必须且只能包含一个组合件，后续明细必须全部为普通子件";

    public static final int DEPOT_HEAD_DISASSEMBLE_SAME_MATERIAL_CODE = 8500119;
    public static final String DEPOT_HEAD_DISASSEMBLE_SAME_MATERIAL_MSG = "抱歉，组合件不能同时作为拆卸产出的普通子件:%s";

    public static final int DEPOT_HEAD_IN_DETAIL_REPORT_PERMISSION_CODE = 8500120;
    public static final String DEPOT_HEAD_IN_DETAIL_REPORT_PERMISSION_MSG = "抱歉，当前用户没有入库明细的查看权限";
    public static final int DEPOT_HEAD_OUT_DETAIL_REPORT_PERMISSION_CODE = 8500121;
    public static final String DEPOT_HEAD_OUT_DETAIL_REPORT_PERMISSION_MSG = "抱歉，当前用户没有出库明细的查看权限";
    public static final int DEPOT_HEAD_ALLOCATION_DETAIL_REPORT_PERMISSION_CODE = 8500122;
    public static final String DEPOT_HEAD_ALLOCATION_DETAIL_REPORT_PERMISSION_MSG = "抱歉，当前用户没有调拨明细的查看权限";
    public static final int DEPOT_HEAD_IN_MATERIAL_COUNT_REPORT_PERMISSION_CODE = 8500123;
    public static final String DEPOT_HEAD_IN_MATERIAL_COUNT_REPORT_PERMISSION_MSG = "抱歉，当前用户没有入库汇总的查看权限";
    public static final int DEPOT_HEAD_OUT_MATERIAL_COUNT_REPORT_PERMISSION_CODE = 8500124;
    public static final String DEPOT_HEAD_OUT_MATERIAL_COUNT_REPORT_PERMISSION_MSG = "抱歉，当前用户没有出库汇总的查看权限";
    public static final int DEPOT_HEAD_MATERIAL_COUNT_TYPE_INVALID_CODE = 8500125;
    public static final String DEPOT_HEAD_MATERIAL_COUNT_TYPE_INVALID_MSG = "抱歉，出入库汇总类型不合法";

    public static final int DEPOT_HEAD_STATEMENT_ACCOUNT_TYPE_INVALID_CODE = 8500126;
    public static final String DEPOT_HEAD_STATEMENT_ACCOUNT_TYPE_INVALID_MSG = "抱歉，对账类型不合法";
    public static final int DEPOT_HEAD_CUSTOMER_ACCOUNT_PERMISSION_CODE = 8500127;
    public static final String DEPOT_HEAD_CUSTOMER_ACCOUNT_PERMISSION_MSG = "抱歉，当前用户没有客户对账或收款管理的查看权限";
    public static final int DEPOT_HEAD_VENDOR_ACCOUNT_PERMISSION_CODE = 8500128;
    public static final String DEPOT_HEAD_VENDOR_ACCOUNT_PERMISSION_MSG = "抱歉，当前用户没有供应商对账或付款管理的查看权限";
    public static final int DEPOT_HEAD_DEBT_LIST_TYPE_INVALID_CODE = 8500129;
    public static final String DEPOT_HEAD_DEBT_LIST_TYPE_INVALID_MSG = "抱歉，欠款单据类型不合法";
    public static final int DEPOT_HEAD_CUSTOMER_DATA_PERMISSION_CODE = 8500130;
    public static final String DEPOT_HEAD_CUSTOMER_DATA_PERMISSION_MSG = "抱歉，当前用户没有该客户的数据权限";
    public static final int DEPOT_HEAD_DEBT_ORGAN_REQUIRED_CODE = 8500131;
    public static final String DEPOT_HEAD_DEBT_ORGAN_REQUIRED_MSG = "抱歉，请选择有效的往来单位";


    /**
     *  单据明细信息
     * type = 90
     * */
    //添加单据明细信息失败
    public static final int DEPOT_ITEM_ADD_FAILED_CODE = 9000000;
    public static final String DEPOT_ITEM_ADD_FAILED_MSG = "添加单据明细信息失败";
    //删除单据明细信息失败
    public static final int DEPOT_ITEM_DELETE_FAILED_CODE = 9000001;
    public static final String DEPOT_ITEM_DELETE_FAILED_MSG = "删除单据明细信息失败";
    //修改单据明细信息失败
    public static final int DEPOT_ITEM_EDIT_FAILED_CODE = 9000002;
    public static final String DEPOT_ITEM_EDIT_FAILED_MSG = "修改单据明细信息失败";
    //单据明细-明细中商品不存在
    public static final int DEPOT_ITEM_BARCODE_IS_NOT_EXIST_CODE = 9000003;
    public static final String DEPOT_ITEM_BARCODE_IS_NOT_EXIST_MSG = "抱歉，商品条码:%s在商品管理中不存在";
    //单据明细-明细中仓库不存在
    public static final int DEPOT_ITEM_DEPOTNAME_IS_NOT_EXIST_CODE = 9000004;
    public static final String DEPOT_ITEM_DEPOTNAME_IS_NOT_EXIST_MSG = "抱歉，仓库:%s在基础资料-仓库信息中不存在";
    //单据明细-单据中存在序列号，需要到新增界面关联单据
    public static final int DEPOT_ITEM_EXIST_SERIAL_NUMBER_FAILED_CODE = 9000005;
    public static final String DEPOT_ITEM_EXIST_SERIAL_NUMBER_FAILED_MSG = "抱歉，单据:%s里面存在序列号，需要到新增界面关联单据";
    //单据明细-单据中存在批号，需要到新增界面关联单据
    public static final int DEPOT_ITEM_EXIST_BATCH_NUMBER_FAILED_CODE = 9000006;
    public static final String DEPOT_ITEM_EXIST_BATCH_NUMBER_FAILED_MSG = "抱歉，单据:%s里面存在批号，需要到新增界面关联单据";
    //原关联单据已被修改，请重新关联
    public static final int DEPOT_ITEM_PRE_BILL_IS_CHANGE_CODE = 9000007;
    public static final String DEPOT_ITEM_PRE_BILL_IS_CHANGE_MSG = "抱歉，原关联单据已被修改，请重新关联";
    //零售统计-无查看权限
    public static final int RETAIL_REPORT_PERMISSION_CODE = 9000008;
    public static final String RETAIL_REPORT_PERMISSION_MSG = "抱歉，当前用户没有零售统计的查看权限";
    //采购统计-无查看权限
    public static final int BUY_REPORT_PERMISSION_CODE = 9000009;
    public static final String BUY_REPORT_PERMISSION_MSG = "抱歉，当前用户没有采购统计的查看权限";
    //销售统计-无查看权限
    public static final int SALE_REPORT_PERMISSION_CODE = 9000010;
    public static final String SALE_REPORT_PERMISSION_MSG = "抱歉，当前用户没有销售统计的查看权限";
    //进销存统计-无查看权限
    public static final int IN_OUT_STOCK_REPORT_PERMISSION_CODE = 9000011;
    public static final String IN_OUT_STOCK_REPORT_PERMISSION_MSG = "抱歉，当前用户没有进销存统计的查看权限";
    //库存预警-无查看权限
    public static final int STOCK_WARNING_REPORT_PERMISSION_CODE = 9000012;
    public static final String STOCK_WARNING_REPORT_PERMISSION_MSG = "抱歉，当前用户没有库存预警的查看权限";

    /**
     *  财务信息
     * type = 95
     * */
    //添加财务信息失败
    public static final int ACCOUNT_HEAD_ADD_FAILED_CODE = 9500000;
    public static final String ACCOUNT_HEAD_ADD_FAILED_MSG = "添加财务信息失败";
    //删除财务信息失败
    public static final int ACCOUNT_HEAD_DELETE_FAILED_CODE = 9500001;
    public static final String ACCOUNT_HEAD_DELETE_FAILED_MSG = "删除财务信息失败";
    //修改财务信息失败
    public static final int ACCOUNT_HEAD_EDIT_FAILED_CODE = 9500002;
    public static final String ACCOUNT_HEAD_EDIT_FAILED_MSG = "修改财务信息失败";
    //单据录入-明细不能为空
    public static final int ACCOUNT_HEAD_ROW_FAILED_CODE = 9500003;
    public static final String ACCOUNT_HEAD_ROW_FAILED_MSG = "单据明细不能为空";
    //单据删除-只有未审核的单据才能删除，请先进行反审核
    public static final int ACCOUNT_HEAD_UN_AUDIT_DELETE_FAILED_CODE = 9500004;
    public static final String ACCOUNT_HEAD_UN_AUDIT_DELETE_FAILED_MSG = "抱歉，只有未审核的单据才能删除，请先进行反审核";
    //财务信息录入-单据编号已经存在
    public static final int ACCOUNT_HEAD_BILL_NO_EXIST_CODE = 9500005;
    public static final String ACCOUNT_HEAD_BILL_NO_EXIST_MSG = "抱歉，单据编号已经存在";
    //财务信息录入-付款账户和明细中的账户重复
    public static final int ACCOUNT_HEAD_ACCOUNT_REPEAT_CODE = 9500006;
    public static final String ACCOUNT_HEAD_ACCOUNT_REPEAT_MSG = "抱歉，转出账户:%s不能同时作为转入账户";
    //财务信息审核-只有未审核的单据才能审核
    public static final int ACCOUNT_HEAD_UN_AUDIT_TO_AUDIT_FAILED_CODE = 9500007;
    public static final String ACCOUNT_HEAD_UN_AUDIT_TO_AUDIT_FAILED_MSG = "抱歉，只有未审核的单据才能审核";
    //财务信息反审核-只有已审核的单据才能反审核
    public static final int ACCOUNT_HEAD_AUDIT_TO_UN_AUDIT_FAILED_CODE = 9500008;
    public static final String ACCOUNT_HEAD_AUDIT_TO_UN_AUDIT_FAILED_MSG = "抱歉，只有已审核的单据才能反审核";

    public static final int ACCOUNT_HEAD_PERMISSION_CODE = 9500009;
    public static final String ACCOUNT_HEAD_PERMISSION_MSG = "抱歉，当前用户没有%s的%s权限";

    public static final int ACCOUNT_HEAD_DATA_PERMISSION_CODE = 9500010;
    public static final String ACCOUNT_HEAD_DATA_PERMISSION_MSG = "抱歉，当前用户没有该财务单据的数据权限";

    public static final int ACCOUNT_HEAD_STATUS_FAILED_CODE = 9500011;
    public static final String ACCOUNT_HEAD_STATUS_FAILED_MSG = "抱歉，财务单据状态不合法，请通过保存、审核或反审核操作变更状态";

    public static final int ACCOUNT_HEAD_TYPE_FAILED_CODE = 9500012;
    public static final String ACCOUNT_HEAD_TYPE_FAILED_MSG = "抱歉，财务单据类型不合法或不允许变更";

    public static final int ACCOUNT_HEAD_INCOME_AMOUNT_FAILED_CODE = 9500013;
    public static final String ACCOUNT_HEAD_INCOME_AMOUNT_FAILED_MSG = "抱歉，收入单金额必须大于零且主表金额必须等于明细合计";

    public static final int ACCOUNT_HEAD_INCOME_DETAIL_FAILED_CODE = 9500014;
    public static final String ACCOUNT_HEAD_INCOME_DETAIL_FAILED_MSG = "抱歉，收入单明细中的收入项目、账户或关联单据信息不合法";

    public static final int ACCOUNT_HEAD_ACCOUNT_FAILED_CODE = 9500015;
    public static final String ACCOUNT_HEAD_ACCOUNT_FAILED_MSG = "抱歉，收入账户不存在或已停用";

    public static final int ACCOUNT_HEAD_EXPENSE_AMOUNT_FAILED_CODE = 9500016;
    public static final String ACCOUNT_HEAD_EXPENSE_AMOUNT_FAILED_MSG = "抱歉，支出单金额必须大于零且主表金额必须等于明细合计";

    public static final int ACCOUNT_HEAD_EXPENSE_DETAIL_FAILED_CODE = 9500017;
    public static final String ACCOUNT_HEAD_EXPENSE_DETAIL_FAILED_MSG = "抱歉，支出单明细中的支出项目、账户或关联单据信息不合法";

    public static final int ACCOUNT_HEAD_EXPENSE_ACCOUNT_FAILED_CODE = 9500018;
    public static final String ACCOUNT_HEAD_EXPENSE_ACCOUNT_FAILED_MSG = "抱歉，支出账户不存在或已停用";

    public static final int ACCOUNT_HEAD_MONEY_IN_AMOUNT_FAILED_CODE = 9500019;
    public static final String ACCOUNT_HEAD_MONEY_IN_AMOUNT_FAILED_MSG = "抱歉，收款金额必须大于零、不能超过待收欠款，且主表金额必须与明细及优惠金额一致";

    public static final int ACCOUNT_HEAD_MONEY_IN_DETAIL_FAILED_CODE = 9500020;
    public static final String ACCOUNT_HEAD_MONEY_IN_DETAIL_FAILED_MSG = "抱歉，收款明细只能关联当前客户已审核且仍有欠款的销售出库单";

    public static final int ACCOUNT_HEAD_MONEY_IN_ACCOUNT_FAILED_CODE = 9500021;
    public static final String ACCOUNT_HEAD_MONEY_IN_ACCOUNT_FAILED_MSG = "抱歉，收款账户不存在或已停用";

    public static final int ACCOUNT_HEAD_MONEY_IN_ORGAN_FAILED_CODE = 9500022;
    public static final String ACCOUNT_HEAD_MONEY_IN_ORGAN_FAILED_MSG = "抱歉，收款客户不存在、已停用或类型不正确";

    public static final int ACCOUNT_HEAD_MONEY_OUT_AMOUNT_FAILED_CODE = 9500023;
    public static final String ACCOUNT_HEAD_MONEY_OUT_AMOUNT_FAILED_MSG = "抱歉，付款金额必须大于零、不能超过待付欠款，且主表金额必须与明细及优惠金额一致";

    public static final int ACCOUNT_HEAD_MONEY_OUT_DETAIL_FAILED_CODE = 9500024;
    public static final String ACCOUNT_HEAD_MONEY_OUT_DETAIL_FAILED_MSG = "抱歉，付款明细只能关联当前供应商已审核且仍有欠款的采购入库单";

    public static final int ACCOUNT_HEAD_MONEY_OUT_ACCOUNT_FAILED_CODE = 9500025;
    public static final String ACCOUNT_HEAD_MONEY_OUT_ACCOUNT_FAILED_MSG = "抱歉，付款账户不存在或已停用";

    public static final int ACCOUNT_HEAD_MONEY_OUT_ORGAN_FAILED_CODE = 9500026;
    public static final String ACCOUNT_HEAD_MONEY_OUT_ORGAN_FAILED_MSG = "抱歉，付款供应商不存在、已停用或类型不正确";

    public static final int ACCOUNT_HEAD_GIRO_AMOUNT_FAILED_CODE = 9500027;
    public static final String ACCOUNT_HEAD_GIRO_AMOUNT_FAILED_MSG = "抱歉，转账金额必须大于零，且转出金额必须等于转入明细合计";

    public static final int ACCOUNT_HEAD_GIRO_DETAIL_FAILED_CODE = 9500028;
    public static final String ACCOUNT_HEAD_GIRO_DETAIL_FAILED_MSG = "抱歉，转账明细中的转入账户或关联字段不合法";

    public static final int ACCOUNT_HEAD_GIRO_ACCOUNT_FAILED_CODE = 9500029;
    public static final String ACCOUNT_HEAD_GIRO_ACCOUNT_FAILED_MSG = "抱歉，转出账户不存在或已停用";

    public static final int ACCOUNT_HEAD_ADVANCE_IN_AMOUNT_FAILED_CODE = 9500030;
    public static final String ACCOUNT_HEAD_ADVANCE_IN_AMOUNT_FAILED_MSG = "抱歉，收预付款金额必须大于零，且主表金额必须等于明细合计";

    public static final int ACCOUNT_HEAD_ADVANCE_IN_DETAIL_FAILED_CODE = 9500031;
    public static final String ACCOUNT_HEAD_ADVANCE_IN_DETAIL_FAILED_MSG = "抱歉，收预付款明细中的账户或关联字段不合法";

    public static final int ACCOUNT_HEAD_ADVANCE_IN_ORGAN_FAILED_CODE = 9500032;
    public static final String ACCOUNT_HEAD_ADVANCE_IN_ORGAN_FAILED_MSG = "抱歉，付款会员不存在、已停用或类型不正确";

    public static final int ACCOUNT_HEAD_ADVANCE_IN_ACCOUNT_FAILED_CODE = 9500033;
    public static final String ACCOUNT_HEAD_ADVANCE_IN_ACCOUNT_FAILED_MSG = "抱歉，收预付款账户不存在或已停用";
    /**
     *  财务明细信息
     * type = 100
     * */
    //添加财务明细信息失败
    public static final int ACCOUNT_ITEM_ADD_FAILED_CODE = 10000000;
    public static final String ACCOUNT_ITEM_ADD_FAILED_MSG = "添加财务明细信息失败";
    //删除财务明细信息失败
    public static final int ACCOUNT_ITEM_DELETE_FAILED_CODE = 10000001;
    public static final String ACCOUNT_ITEM_DELETE_FAILED_MSG = "删除财务明细信息失败";
    //修改财务明细信息失败
    public static final int ACCOUNT_ITEM_EDIT_FAILED_CODE = 10000002;
    public static final String ACCOUNT_ITEM_EDIT_FAILED_MSG = "修改财务明细信息失败";
    /**
     * 序列号
     * type = 105
     * */
    /**序列号已存在*/
    public static final int SERIAL_NUMBERE_ALREADY_EXISTS_CODE = 10500000;
    public static final String SERIAL_NUMBERE_ALREADY_EXISTS_MSG = "序列号:%s已存在";
    /**序列号不存在或者已经出库*/
    public static final int SERIAL_NUMBERE_NOT_EXISTS_CODE = 10500001;
    public static final String SERIAL_NUMBERE_NOT_EXISTS_MSG = "序列号:%s不存在或者已经出库";
    /**
     * 部门信息
     * type = 110
     * */
    //添加部门信息失败
    public static final int ORGANIZATION_ADD_FAILED_CODE = 11000000;
    public static final String ORGANIZATION_ADD_FAILED_MSG = "添加部门信息失败";
    //删除部门信息失败
    public static final int ORGANIZATION_DELETE_FAILED_CODE = 11000001;
    public static final String ORGANIZATION_DELETE_FAILED_MSG = "删除部门信息失败";
    //修改部门信息失败
    public static final int ORGANIZATION_EDIT_FAILED_CODE = 11000002;
    public static final String ORGANIZATION_EDIT_FAILED_MSG = "修改部门信息失败";
    //部门编号已存在
    public static final int ORGANIZATION_NO_ALREADY_EXISTS_CODE = 11000003;
    public static final String ORGANIZATION_NO_ALREADY_EXISTS_MSG = "部门编号已存在";
    //根部门不允许删除
    public static final int ORGANIZATION_ROOT_NOT_ALLOWED_DELETE_CODE = 11000004;
    public static final String ORGANIZATION_ROOT_NOT_ALLOWED_DELETE_MSG = "根部门不允许删除";
    //根部门不允许修改
    public static final int ORGANIZATION_ROOT_NOT_ALLOWED_EDIT_CODE = 11000005;
    public static final String ORGANIZATION_ROOT_NOT_ALLOWED_EDIT_MSG = "根部门不允许修改";
    //该部门存在下级不允许删除
    public static final int ORGANIZATION_CHILD_NOT_ALLOWED_DELETE_CODE = 11000006;
    public static final String ORGANIZATION_CHILD_NOT_ALLOWED_DELETE_MSG = "该部门存在下级不允许删除";
    /**
     * 部门用户关联关系
     * type = 115
     * */
    //添加部门用户关联关系失败
    public static final int ORGA_USER_REL_ADD_FAILED_CODE = 11500000;
    public static final String ORGA_USER_REL_ADD_FAILED_MSG = "添加部门用户关联关系失败";
    //删除部门用户关联关系失败
    public static final int ORGA_USER_REL_DELETE_FAILED_CODE = 11500001;
    public static final String ORGA_USER_REL_DELETE_FAILED_MSG = "删除部门用户关联关系失败";
    //修改部门用户关联关系失败
    public static final int ORGA_USER_REL_EDIT_FAILED_CODE = 11500002;
    public static final String ORGA_USER_REL_EDIT_FAILED_MSG = "修改部门用户关联关系失败";

    //进销存统计，如果有权限的仓库数量太多则提示要选择仓库
    public static final int REPORT_TWO_MANY_DEPOT_FAILED_CODE = 510;
    public static final String REPORT_TWO_MANY_DEPOT_FAILED_MSG = "请选择仓库，再进行查询";

    /**
     * 生成单据编号
     * type = 120
     * */
    //获取唯一单据编号失败
    public static final int SEQUENCE_ONLY_FAILED_CODE = 12000001;
    public static final String SEQUENCE_ONLY_FAILED_MSG = "获取唯一单据编号失败，请稍后重试";
    //获取唯一单据编号操作被中断
    public static final int SEQUENCE_ONLY_BREAK_CODE = 12000002;
    public static final String SEQUENCE_ONLY_BREAK_MSG = "获取唯一单据编号操作被中断";

    /**
     * 字典
     * type = 125
     * */
    //字典类型已分配,不能删除
    public static final int DICT_TYPE_ALREADY_USED_CODE = 12500001;
    public static final String DICT_TYPE_ALREADY_USED_MSG = "%s已分配,不能删除";

    //演示用户禁止操作
    public static final int SYSTEM_CONFIG_TEST_USER_CODE = -1;
    public static final String SYSTEM_CONFIG_TEST_USER_MSG = "演示用户禁止操作";

    /**
     * 标准正常返回/操作成功返回
     * @return
     */
    public static JSONObject standardSuccess () {
        JSONObject success = new JSONObject();
        success.put(GLOBAL_RETURNS_CODE, SERVICE_SUCCESS_CODE);
        success.put(GLOBAL_RETURNS_MESSAGE, SERVICE_SUCCESS_MSG);
        return success;
    }
}
