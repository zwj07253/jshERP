<template>
  <a-card :style="cardStyle" :bordered="false">
    <a-spin :spinning="confirmLoading">
      <a-form :form="form">
        <div class="tabs-header">
          <a-tabs :active-key="activeKey" @change="handleTabChange">
            <a-tab-pane key="sec1" :tab="$t('system.basicInfo')" />
            <a-tab-pane key="sec2" :tab="$t('system.configInfo')" />
          </a-tabs>
        </div>
        <div class="content-container" :style="contentStyle" ref="container">
          <div id="sec1" class="section" ref="sec1">
            <h2>{{ $t('system.basicInfoTitle') }}</h2>
            <a-row class="form-row" :gutter="24">
              <a-col :lg="24" :md="24" :sm="24">
                <a-form-item :labelCol="labelCol" :wrapperCol="baseWrapperCol" :label="$t('system.companyName')">
                  <a-input :placeholder="$t('system.companyName')" v-decorator.trim="[ 'companyName' ]" @change="handleCompanyName" />
                </a-form-item>
              </a-col>
              <a-col :lg="24" :md="24" :sm="24">
                <a-form-item :labelCol="labelCol" :wrapperCol="baseWrapperCol" :label="$t('system.contact')">
                  <a-input :placeholder="$t('system.contact')" v-decorator.trim="[ 'companyContacts' ]" @change="handleCompanyContacts" />
                </a-form-item>
              </a-col>
              <a-col :lg="24" :md="24" :sm="24">
                <a-form-item :labelCol="labelCol" :wrapperCol="baseWrapperCol" :label="$t('system.companyAddress')">
                  <a-input :placeholder="$t('system.companyAddress')" v-decorator.trim="[ 'companyAddress' ]" @change="handleCompanyAddress" />
                </a-form-item>
              </a-col>
              <a-col :lg="24" :md="24" :sm="24">
                <a-form-item :labelCol="labelCol" :wrapperCol="baseWrapperCol" :label="$t('system.companyTel')">
                  <a-input :placeholder="$t('system.companyTel')" v-decorator.trim="[ 'companyTel' ]" @change="handleCompanyTel" />
                </a-form-item>
              </a-col>
              <a-col :lg="24" :md="24" :sm="24">
                <a-form-item :labelCol="labelCol" :wrapperCol="baseWrapperCol" :label="$t('system.companyFax')">
                  <a-input :placeholder="$t('system.companyFax')" v-decorator.trim="[ 'companyFax' ]" @change="handleCompanyFax" />
                </a-form-item>
              </a-col>
              <a-col :lg="24" :md="24" :sm="24">
                <a-form-item :labelCol="labelCol" :wrapperCol="baseWrapperCol" :label="$t('system.companyPostCode')">
                  <a-input :placeholder="$t('system.companyPostCode')" v-decorator.trim="[ 'companyPostCode' ]" @change="handleCompanyPostCode" />
                </a-form-item>
              </a-col>
              <a-col :lg="24" :md="24" :sm="24">
                <a-form-item :labelCol="labelCol" :wrapperCol="baseWrapperCol" :label="$t('system.saleAgreement')">
                  <a-input :placeholder="$t('system.saleAgreement')" v-decorator.trim="[ 'saleAgreement' ]" @change="handleSaleAgreement" />
                </a-form-item>
              </a-col>
            </a-row>
          </div>
          <div id="sec2" class="section" ref="sec2">
            <h2>{{ $t('system.configInfoTitle') }}</h2>
            <a-row class="form-row" :gutter="24">
              <a-col :lg="24" :md="24" :sm="24">
                <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.depotPerm')">
                  <a-switch :checked-children="$t('common.enable')" :un-checked-children="$t('common.disable')" v-model="depotFlagSwitch" @change="onDepotChange"></a-switch>
                  （{{ $t('system.depotPermTip') }}）
                </a-form-item>
              </a-col>
              <a-col :lg="24" :md="24" :sm="24">
                <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.customerPerm')">
                  <a-switch :checked-children="$t('common.enable')" :un-checked-children="$t('common.disable')" v-model="customerFlagSwitch" @change="onCustomerChange"></a-switch>
                  （{{ $t('system.customerPermTip') }}）
                </a-form-item>
              </a-col>
              <a-col :lg="24" :md="24" :sm="24">
                <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.minusStock')">
                  <a-switch :checked-children="$t('common.enable')" :un-checked-children="$t('common.disable')" v-model="minusStockFlagSwitch" @change="onMinusStockChange"></a-switch>
                  （{{ $t('system.minusStockTip') }}）
                </a-form-item>
              </a-col>
              <a-col :lg="24" :md="24" :sm="24">
                <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.purchaseBySale')">
                  <a-switch :checked-children="$t('common.enable')" :un-checked-children="$t('common.disable')" v-model="purchaseBySaleFlagSwitch" @change="onPurchaseBySaleChange"></a-switch>
                  （{{ $t('system.purchaseBySaleTip') }}）
                </a-form-item>
              </a-col>
              <a-col :lg="24" :md="24" :sm="24">
                <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.overLinkBill')">
                  <a-switch :checked-children="$t('common.enable')" :un-checked-children="$t('common.disable')" v-model="overLinkBillFlagSwitch" @change="onOverLinkBillChange"></a-switch>
                  （{{ $t('system.overLinkBillTip') }}）
                </a-form-item>
              </a-col>
              <a-col :lg="24" :md="24" :sm="24">
                <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.updateUnitPrice')">
                  <a-switch :checked-children="$t('common.enable')" :un-checked-children="$t('common.disable')" v-model="updateUnitPriceFlagSwitch" @change="onUpdateUnitPriceChange"></a-switch>
                  （{{ $t('system.updateUnitPriceTip') }}）
                </a-form-item>
              </a-col>
              <a-col :lg="24" :md="24" :sm="24">
                <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.forceApproval')">
                  <a-switch :checked-children="$t('common.enable')" :un-checked-children="$t('common.disable')" v-model="forceApprovalFlagSwitch" @change="onForceApprovalChange"></a-switch>
                  （{{ $t('system.forceApprovalTip') }}）
                </a-form-item>
              </a-col>
              <a-col :lg="24" :md="24" :sm="24">
                <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.inOutManage')">
                  <a-switch :checked-children="$t('common.enable')" :un-checked-children="$t('common.disable')" v-model="inOutManageFlagSwitch" @change="onInOutManageChange"></a-switch>
                  （{{ $t('system.inOutManageTip') }}）
                </a-form-item>
              </a-col>
              <a-col :lg="24" :md="24" :sm="24">
                <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.multiAccount')">
                  <a-switch :checked-children="$t('common.enable')" :un-checked-children="$t('common.disable')" v-model="multiAccountFlagSwitch" @change="onMultiAccountChange"></a-switch>
                  （{{ $t('system.multiAccountTip') }}）
                </a-form-item>
              </a-col>
              <a-col :lg="24" :md="24" :sm="24">
                <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.moveAvgPrice')">
                  <a-switch :checked-children="$t('common.enable')" :un-checked-children="$t('common.disable')" v-model="moveAvgPriceFlagSwitch" @change="onMoveAvgPriceChange"></a-switch>
                  （{{ $t('system.moveAvgPriceTip') }}）
                </a-form-item>
              </a-col>
              <a-col :lg="24" :md="24" :sm="24">
                <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.auditPrint')">
                  <a-switch :checked-children="$t('common.enable')" :un-checked-children="$t('common.disable')" v-model="auditPrintFlagSwitch" @change="onAuditPrintChange"></a-switch>
                  （{{ $t('system.auditPrintTip') }}）
                </a-form-item>
              </a-col>
              <a-col :lg="24" :md="24" :sm="24">
                <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.zeroChangeAmount')">
                  <a-switch :checked-children="$t('common.enable')" :un-checked-children="$t('common.disable')" v-model="zeroChangeAmountFlagSwitch" @change="onZeroChangeAmountChange"></a-switch>
                  （{{ $t('system.zeroChangeAmountTip') }}）
                </a-form-item>
              </a-col>
              <a-col :lg="24" :md="24" :sm="24">
                <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.customerStaticPrice')">
                  <a-switch :checked-children="$t('common.enable')" :un-checked-children="$t('common.disable')" v-model="customerStaticPriceFlagSwitch" @change="onCustomerStaticPriceChange"></a-switch>
                  （{{ $t('system.customerStaticPriceTip') }}）
                </a-form-item>
              </a-col>
              <a-col :lg="24" :md="24" :sm="24">
                <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.materialPriceTax')">
                  <a-switch :checked-children="$t('common.enable')" :un-checked-children="$t('common.disable')" v-model="materialPriceTaxFlagSwitch" @change="onMaterialPriceTaxChange"></a-switch>
                  （{{ $t('system.materialPriceTaxTip') }}）
                </a-form-item>
              </a-col>
              <a-col :lg="24" :md="24" :sm="24" v-if="isShowApproval">
                <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.multiLevelApproval')">
                  <a-switch :checked-children="$t('common.enable')" :un-checked-children="$t('common.disable')" v-model="multiLevelApprovalFlagSwitch" @change="onMultiLevelApprovalChange"></a-switch>
                  <a-select :placeholder="$t('system.selectFlowType')" v-model="multiBillTypeSelect" style="width:400px;padding-left:10px"
                            mode="multiple" :maxTagCount="6" :dropdownMatchSelectWidth="false"
                            showSearch allow-clear optionFilterProp="children" @change="onMultiBillTypeChange">
                    <a-select-option v-for="(item,index) in billTypeList" :key="index" :value="item.key">
                      {{ item.value }}
                    </a-select-option>
                  </a-select>
                  （{{ $t('system.multiLevelApprovalTip') }}）<a-button type="link" @click="handleReload">{{ $t('system.clickToRefresh') }}</a-button>
                </a-form-item>
              </a-col>
            </a-row>
          </div>
        </div>
        <div class="action-footer">
          <a-button @click="handleCancel" :disabled="confirmLoading || !hasChanges">{{ $t('common.cancel') }}</a-button>
          <a-button type="primary" @click="doSave" :loading="confirmLoading" :disabled="!hasChanges">{{ $t('common.save') }}</a-button>
        </div>
      </a-form>
    </a-spin>
  </a-card>
</template>
<!-- b y 7 5 2 7  1 8 9 2 0 -->
<script>
  import pick from 'lodash.pick'
  import JSelectMultiple from '@/components/jeecg/JSelectMultiple'
  import { addSystemConfig, editSystemConfig } from '@/api/api'
  import { autoJumpNextInput } from '@/utils/util'
  import { getAction } from '@/api/manage'
  import { mixinDevice } from '@/utils/mixin.js'

  export default {
    name: "SystemConfigList",
    mixins: [mixinDevice],
    components: {
      JSelectMultiple
    },
    data () {
      return {
        title: this.$t('common.action'),
        cardStyle: '',
        contentStyle: '',
        activeKey: 'sec1',
        visible: true,
        model: {},
        hasChanges: false,
        depotFlagSwitch: false, //仓库权限状态
        customerFlagSwitch: false, //客户权限状态
        minusStockFlagSwitch: false, //负库存状态
        purchaseBySaleFlagSwitch: false, //以销定购状态
        overLinkBillFlagSwitch: false, //超出关联单据状态
        updateUnitPriceFlagSwitch: true, //更新单价状态
        forceApprovalFlagSwitch: false, //强审核
        inOutManageFlagSwitch: false, //出入库管理
        multiLevelApprovalFlagSwitch: false, //多级审核
        originalMultiLevelApprovalFlag: '0', //原始多级审核状态
        multiBillTypeSelect: [], //单据类型
        originalMultiBillTypeSelect: [], //原始单据类型
        isShowApproval: false, //是否展示多级审核
        multiAccountFlagSwitch: false, //多账户
        moveAvgPriceFlagSwitch: false, //移动平均价
        auditPrintFlagSwitch: false, //先审核后打印
        zeroChangeAmountFlagSwitch: false, //零收付款
        customerStaticPriceFlagSwitch: false, //客户静态单价
        materialPriceTaxFlagSwitch: false, //商品价格含税
        labelCol: {
          xs: { span: 24 },
          sm: { span: 2 },
        },
        baseWrapperCol: {
          xs: { span: 24 },
          sm: { span: 12 },
        },
        wrapperCol: {
          xs: { span: 24 },
          sm: { span: 18 },
        },
        confirmLoading: false,
        form: this.$form.createForm(this),
        billTypeList: [
          { 'key': 'LSCK', 'value': this.$t('system.billTypeLSCK') },
          { 'key': 'LSTH', 'value': this.$t('system.billTypeLSTH') },
          { 'key': 'QGD', 'value': this.$t('system.billTypeQGD') },
          { 'key': 'CGDD', 'value': this.$t('system.billTypeCGDD') },
          { 'key': 'CGRK', 'value': this.$t('system.billTypeCGRK') },
          { 'key': 'CGTH', 'value': this.$t('system.billTypeCGTH') },
          { 'key': 'XSDD', 'value': this.$t('system.billTypeXSDD') },
          { 'key': 'XSCK', 'value': this.$t('system.billTypeXSCK') },
          { 'key': 'XSTH', 'value': this.$t('system.billTypeXSTH') },
          { 'key': 'QTRK', 'value': this.$t('system.billTypeQTRK') },
          { 'key': 'QTCK', 'value': this.$t('system.billTypeQTCK') },
          { 'key': 'DBCK', 'value': this.$t('system.billTypeDBCK') },
          { 'key': 'ZZD', 'value': this.$t('system.billTypeZZD') },
          { 'key': 'CXD', 'value': this.$t('system.billTypeCXD') },
          { 'key': 'SR', 'value': this.$t('system.billTypeSR') },
          { 'key': 'ZC', 'value': this.$t('system.billTypeZC') },
          { 'key': 'SK', 'value': this.$t('system.billTypeSK') },
          { 'key': 'FK', 'value': this.$t('system.billTypeFK') },
          { 'key': 'ZZ', 'value': this.$t('system.billTypeZZ') },
          { 'key': 'SYF', 'value': this.$t('system.billTypeSYF') },
        ]
      }
    },
    created () {
      this.init()
      this.loadPlugins()
      if(this.isDesktop()) {
        this.cardStyle = 'height:' + (document.documentElement.clientHeight-100) + 'px'
        this.contentStyle = 'height:' + (document.documentElement.clientHeight-280) + 'px'
      }
    },
    methods: {
      handleTabChange(key) {
        this.activeKey = key
        // 使用 setTimeout 确保 DOM 更新完成
        this.$nextTick(() => {
          const element = document.getElementById(key)
          if (element) {
            element.scrollIntoView({
              behavior: 'smooth',  // 平滑滚动
              block: 'start'       // 滚动到顶部
            })
          }
        })
      },
      //初始化加载内容
      init () {
        let param = {
          search: {"companyName":""},
          currentPage: 1,
          pageSize: 10
        }
        getAction('/systemConfig/list', param).then((res)=>{
          if(res.code === 200){
            let record = res.data.rows[0]
            this.form.resetFields();
            this.model = Object.assign({}, record);
            this.hasChanges = false;
            this.visible = true;
            this.$nextTick(() => {
              this.form.setFieldsValue(pick(this.model,'companyName', 'companyContacts', 'companyAddress',
                'companyTel', 'companyFax', 'companyPostCode', 'saleAgreement'))
            });
            if(record.id) {
              if (record.depotFlag != null) {
                this.depotFlagSwitch = record.depotFlag == '1' ? true : false;
              }
              if (record.customerFlag != null) {
                this.customerFlagSwitch = record.customerFlag == '1' ? true : false;
              }
              if (record.minusStockFlag != null) {
                this.minusStockFlagSwitch = record.minusStockFlag == '1' ? true : false;
              }
              if (record.purchaseBySaleFlag != null) {
                this.purchaseBySaleFlagSwitch = record.purchaseBySaleFlag == '1' ? true : false;
              }
              if (record.overLinkBillFlag != null) {
                this.overLinkBillFlagSwitch = record.overLinkBillFlag == '1' ? true : false;
              }
              if (record.updateUnitPriceFlag != null) {
                this.updateUnitPriceFlagSwitch = record.updateUnitPriceFlag == '1' ? true : false;
              }
              if (record.forceApprovalFlag != null) {
                this.forceApprovalFlagSwitch = record.forceApprovalFlag == '1' ? true : false;
              }
              if (record.inOutManageFlag != null) {
                this.inOutManageFlagSwitch = record.inOutManageFlag == '1' ? true : false;
              }
              if (record.multiLevelApprovalFlag != null) {
                this.multiLevelApprovalFlagSwitch = record.multiLevelApprovalFlag == '1' ? true : false;
                this.originalMultiLevelApprovalFlag = record.multiLevelApprovalFlag
              }
              if (record.multiBillType != null && record.multiBillType != '') {
                this.multiBillTypeSelect = record.multiBillType.split(',')
                this.originalMultiBillTypeSelect = record.multiBillType
              }
              if (record.multiAccountFlag != null) {
                this.multiAccountFlagSwitch = record.multiAccountFlag == '1' ? true : false;
              }
              if (record.moveAvgPriceFlag != null) {
                this.moveAvgPriceFlagSwitch = record.moveAvgPriceFlag == '1' ? true : false;
              }
              if (record.auditPrintFlag != null) {
                this.auditPrintFlagSwitch = record.auditPrintFlag == '1' ? true : false;
              }
              if (record.zeroChangeAmountFlag != null) {
                this.zeroChangeAmountFlagSwitch = record.zeroChangeAmountFlag == '1' ? true : false;
              }
              if (record.customerStaticPriceFlag != null) {
                this.customerStaticPriceFlagSwitch = record.customerStaticPriceFlag == '1' ? true : false;
              }
              if (record.materialPriceTaxFlag != null) {
                this.materialPriceTaxFlagSwitch = record.materialPriceTaxFlag == '1' ? true : false;
              }
            }
          } else {
            this.$message.info(res.data);
          }
        })
      },
      loadPlugins() {
        //校验是否存在多级审批插件
        getAction('/plugin/checkByPluginId', { pluginIds: 'workflow' }).then((res)=> {
          if (res.code === 200 && res.data) {
            let info = res.data['workflow']
            if(info && info.installed && info.started) {
              this.isShowApproval = true
            }
          }
        })
        //校验是否存在盘点插件
        getAction('/plugin/checkByPluginId', { pluginIds: 'stock-check' }).then((res)=> {
          if (res.code === 200 && res.data) {
            let info = res.data['stock-check']
            if(info && info.installed && info.started) {
              this.billTypeList.push({ 'key': 'PDLR', 'value': this.$t('system.billTypePDLR') }, { 'key': 'PDFP', 'value': this.$t('system.billTypePDFP') })
              //校验是否存在生产插件
              getAction('/plugin/checkByPluginId', { pluginIds: 'produce' }).then((res)=> {
                if (res.code === 200 && res.data) {
                  let pInfo = res.data['produce']
                  if(pInfo && pInfo.installed && pInfo.started) {
                    this.billTypeList.push({ 'key': 'SC', 'value': this.$t('system.billTypeSC') }, { 'key': 'WW', 'value': this.$t('system.billTypeWW') })
                  }
                }
              })
            }
          }
        })
      },
      handleCompanyName(event) {
        this.model.companyName = event.target.value
        if(this.model.companyName && this.model.companyName.length>30) {
          this.$message.warning(this.$t('system.companyNameLength'))
        } else {
          this.handleChange()
        }
      },
      handleCompanyContacts(event) {
        this.model.companyContacts = event.target.value
        this.handleChange()
      },
      handleCompanyAddress(event) {
        this.model.companyAddress = event.target.value
        this.handleChange()
      },
      handleCompanyTel(event) {
        this.model.companyTel = event.target.value
        this.handleChange()
      },
      handleCompanyFax(event) {
        this.model.companyFax = event.target.value
        this.handleChange()
      },
      handleCompanyPostCode(event) {
        this.model.companyPostCode = event.target.value
        this.handleChange()
      },
      handleSaleAgreement(event) {
        this.model.saleAgreement = event.target.value
        if(this.model.saleAgreement && this.model.saleAgreement.length>400) {
          this.$message.warning(this.$t('system.saleAgreementLength'))
        } else {
          this.handleChange()
        }
      },
      onDepotChange(checked) {
        this.model.depotFlag = checked?'1':'0'
        this.handleChange()
      },
      onCustomerChange(checked) {
        this.model.customerFlag = checked?'1':'0'
        this.handleChange()
      },
      onMinusStockChange(checked) {
        this.model.minusStockFlag = checked?'1':'0'
        this.handleChange()
      },
      onPurchaseBySaleChange(checked) {
        this.model.purchaseBySaleFlag = checked?'1':'0'
        this.handleChange()
      },
      onOverLinkBillChange(checked) {
        this.model.overLinkBillFlag = checked?'1':'0'
        this.handleChange()
      },
      onUpdateUnitPriceChange(checked) {
        this.model.updateUnitPriceFlag = checked?'1':'0'
        this.handleChange()
      },
      onForceApprovalChange(checked) {
        this.model.forceApprovalFlag = checked?'1':'0'
        this.handleChange()
      },
      onInOutManageChange(checked) {
        this.model.inOutManageFlag = checked?'1':'0'
        this.handleChange()
      },
      onMultiLevelApprovalChange(checked) {
        this.model.multiLevelApprovalFlag = checked?'1':'0'
        if(!checked) {
          this.multiBillTypeSelect = []
          this.model.multiBillType = ''
        }
        this.handleChange()
      },
      onMultiBillTypeChange() {
        this.model.multiBillType = this.multiBillTypeSelect.join(",")
        this.handleChange()
      },
      onMultiAccountChange(checked) {
        this.model.multiAccountFlag = checked?'1':'0'
        this.handleChange()
      },
      onMoveAvgPriceChange(checked) {
        this.model.moveAvgPriceFlag = checked?'1':'0'
        this.handleChange()
      },
      onAuditPrintChange(checked) {
        this.model.auditPrintFlag = checked?'1':'0'
        this.handleChange()
      },
      onZeroChangeAmountChange(checked) {
        this.model.zeroChangeAmountFlag = checked?'1':'0'
        this.handleChange()
      },
      onCustomerStaticPriceChange(checked) {
        this.model.customerStaticPriceFlag = checked?'1':'0'
        this.handleChange()
      },
      onMaterialPriceTaxChange(checked) {
        this.model.materialPriceTaxFlag = checked?'1':'0'
        this.handleChange()
      },
      //改变内容（防抖，500ms内多次操作只提交最后一次）
      handleChange() {
        this.hasChanges = true
      },
      doSave() {
        if(this.model.companyName && this.model.companyName.length>30) {
          this.$message.warning(this.$t('system.companyNameLength'))
          return
        }
        if(this.model.saleAgreement && this.model.saleAgreement.length>400) {
          this.$message.warning(this.$t('system.saleAgreementLength'))
          return
        }
        this.confirmLoading = true
        const companyName = this.model.companyName
        let obj
        if(!this.model.id){
          obj = addSystemConfig(this.model)
        }else{
          obj = editSystemConfig(this.model)
        }
        obj.then((res)=>{
          if(res.code === 200){
            this.hasChanges = false
            this.$bus.$emit('company-name-updated', companyName)
            this.$message.success(this.$t('common.saveSuccess'))
            this.init()
          }else{
            this.$message.warning(res.data.message)
            // 保存失败时回滚UI状态：重新从服务端加载
            this.init()
          }
        }).finally(() => {
          this.confirmLoading = false
        })
      },
      handleCancel() {
        this.init()
      },
      //刷新浏览器
      handleReload() {
        location.reload()
      }
    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less'
</style>
<style>
.ant-form-item {
  margin-bottom: 12px;
}
.tabs-header {
  flex-shrink: 0;  /* 防止被压缩 */
  background: white;
  border-bottom: 1px solid #f0f0f0;
  padding: 0 8px;
}
/* 固定高度的滚动容器 */
.content-container {
  overflow-y: auto;   /* 出现滚动条 */
  padding: 16px;
  border-top: 1px solid #f0f0f0;
}

.action-footer {
  padding: 12px 24px;
  text-align: right;
  background: #fff;
  border-top: 1px solid #f0f0f0;
}

.action-footer .ant-btn + .ant-btn {
  margin-left: 8px;
}

/* 每个区域样式 */
.section {
  margin-bottom: 24px;
  padding: 10px 10px 10px 20px;
  background-color: #fafafa;
  border-radius: 4px;
  min-height: 200px;
  font-size: 12px;
}

.section h2 {
  margin-top: 0;
  margin-bottom: 16px;
  color: #1890ff;
  border-bottom: 1px solid #e8e8e8;
  padding-bottom: 8px;
}

/* 可选的滚动条美化 */
.content-container::-webkit-scrollbar {
  width: 6px;
}

.content-container::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.content-container::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}
</style>
