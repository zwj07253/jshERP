<template>
  <j-modal
    :title="title"
    :width="width"
    :visible="visible"
    :confirmLoading="confirmLoading"
    :keyboard="false"
    :forceRender="true"
    v-bind:prefixNo="prefixNo"
    fullscreen
    switchHelp
    switchFullscreen
    @cancel="handleCancel"
    :id="prefixNo"
    style="top:20px;height: 95%;">
    <template slot="footer">
      <a-button @click="handleCancel">{{ $t('common.cancel') }}</a-button>
      <a-button v-if="billPrintFlag && isShowPrintBtn" @click="handlePrintPro('采购订单')">{{ $t('common.printNew') }}</a-button>
      <a-button v-if="billPrintFlag && isShowPrintBtn" @click="handlePrint('采购订单')">{{ $t('common.print') }}</a-button>
      <a-button v-if="checkFlag && isCanCheck" :loading="confirmLoading" @click="handleOkAndCheck">{{ $t('common.saveAndApprove') }}</a-button>
      <a-button type="primary" :loading="confirmLoading" @click="handleOkOnly">{{ $t('common.save') }}（Ctrl+S）</a-button>
      <!--发起多级审核-->
      <a-button v-if="!checkFlag" @click="handleWorkflow()" type="primary">{{ $t('common.submitWorkflow') }}</a-button>
    </template>
    <a-spin :spinning="confirmLoading">
      <a-form :form="form">
        <a-row class="form-row" :gutter="24">
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.supplier')" data-step="1" :data-title="$t('purchase.form.supplier')"
              :data-intro="$t('purchase.form.supplierIntro')">
              <a-select :placeholder="$t('purchase.selectSupplier')" v-decorator="[ 'organId', validatorRules.organId ]"
                :dropdownMatchSelectWidth="false" showSearch optionFilterProp="children" @search="handleSearchSupplier">
                <div slot="dropdownRender" slot-scope="menu">
                  <v-nodes :vnodes="menu" />
                  <a-divider style="margin: 4px 0;" />
                  <div v-if="quickBtn.vendor" class="dropdown-btn" @mousedown="e => e.preventDefault()" @click="addSupplier"><a-icon type="plus" /> {{ $t('purchase.addSupplier') }}</div>
                  <div class="dropdown-btn" @mousedown="e => e.preventDefault()" @click="initSupplier(0)"><a-icon type="reload" /> {{ $t('purchase.refreshList') }}</div>
                </div>
                <a-select-option v-for="(item,index) in supList" :key="index" :value="item.id">
                  {{ item.supplier }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.documentDate')">
              <j-date v-decorator="['operTime', validatorRules.operTime]" :show-time="true"/>
            </a-form-item>
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.documentNumber')" data-step="2" :data-title="$t('purchase.form.documentNumber')"
              :data-intro="$t('purchase.form.documentNumberIntro')">
              <a-input :placeholder="$t('purchase.form.documentNumber')" v-decorator.trim="[ 'number', validatorRules.number ]" />
            </a-form-item>
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.linkedRequisition')" data-step="3" :data-title="$t('purchase.form.linkedRequisition')"
                         :data-intro="$t('purchase.form.linkedRequisitionIntro')">
              <a-input-search :placeholder="$t('purchase.form.selectLinkedRequisition')" v-decorator="[ 'linkApply' ]" @search="onSearchLinkApply" :readOnly="true"/>
            </a-form-item>
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item v-if="purchaseBySaleFlag" :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.linkedOrder')" data-step="3" :data-title="$t('purchase.form.linkedOrder')"
                         :data-intro="$t('purchase.form.linkedOrderIntro')">
              <a-input-search :placeholder="$t('purchase.form.selectLinkedOrder')" v-decorator="[ 'linkNumber' ]" @search="onSearchLinkNumber" :readOnly="true"/>
            </a-form-item>
          </a-col>
        </a-row>
        <j-editable-table id="billModal"
          :ref="refKeys[0]"
          :loading="materialTable.loading"
          :columns="materialTable.columns"
          :dataSource="materialTable.dataSource"
          :minWidth="minWidth"
          :maxHeight="300"
          :rowNumber="false"
          :rowSelection="true"
          :actionButton="rowCanEdit"
          :actionDeleteButton="!rowCanEdit"
          :dragSortAndNumber="rowCanEdit"
          @valueChange="onValueChange"
          @added="onAdded"
          @deleted="onDeleted">
          <template #buttonAfter>
            <a-row v-if="rowCanEdit" :gutter="24" style="float:left;padding-bottom:5px;padding-right:8px" data-step="4" :data-title="$t('common.scanEntry')" :data-intro="$t('common.scanEntryIntro')">
              <a-col v-if="scanStatus" :md="6" :sm="24">
                <a-button @click="scanEnter">{{ $t('common.scanEntry') }}</a-button>
              </a-col>
              <a-col v-if="!scanStatus" :md="16" :sm="24" style="padding: 0 8px 0 12px">
                <a-input :placeholder="$t('common.scanBarcodePlaceholder')" v-model="scanBarCode" @pressEnter="scanPressEnter" ref="scanBarCode"/>
              </a-col>
              <a-col v-if="!scanStatus" :md="6" :sm="24" style="padding: 0px 12px 0 0">
                <a-button @click="stopScan">{{ $t('common.hideScan') }}</a-button>
              </a-col>
            </a-row>
            <a-row :gutter="24" style="float:left;padding-bottom: 5px;">
              <a-col :md="24" :sm="24">
                <a-button @click="handleHistoryBillList"><a-icon type="history" />{{ $t('common.historyBill') }}</a-button>
              </a-col>
            </a-row>
            <a-row v-if="rowCanEdit" :gutter="24" style="float:left;padding-bottom: 5px;padding-left:20px;">
              <a-button icon="import" @click="onImport(prefixNo)">{{ $t('common.importDetail') }}</a-button>
            </a-row>
          </template>
        </j-editable-table>
        <a-row class="form-row" :gutter="24">
          <a-col :lg="24" :md="24" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="{xs: { span: 24 },sm: { span: 24 }}" label="">
              <a-textarea :rows="1" :placeholder="$t('common.enterRemark')" v-decorator="[ 'remark' ]" style="margin-top:8px;"/>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row class="form-row" :gutter="24">
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.discount')" data-step="5" :data-title="$t('purchase.form.discount')"
                         :data-intro="$t('purchase.form.discountIntro')">
              <a-input style="width:80%;" :placeholder="$t('purchase.form.discountPlaceholder')" v-decorator.trim="[ 'discount' ]" suffix="%" @change="onChangeDiscount"/>
            </a-form-item>
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.discountMoney')" data-step="6" :data-title="$t('purchase.form.discountMoney')"
                         :data-intro="$t('purchase.form.discountMoneyIntro')">
              <a-input :placeholder="$t('purchase.form.discountMoneyPlaceholder')" v-decorator.trim="[ 'discountMoney' ]" @change="onChangeDiscountMoney"/>
            </a-form-item>
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.discountLastMoney')" data-step="7" :data-title="$t('purchase.form.discountLastMoney')"
                         :data-intro="$t('purchase.form.discountLastMoneyIntro')">
              <a-input :placeholder="$t('purchase.form.discountLastMoneyPlaceholder')" v-decorator.trim="[ 'discountLastMoney' ]" :readOnly="true"/>
            </a-form-item>
          </a-col>
          <a-col :lg="6" :md="12" :sm="24"></a-col>
        </a-row>
        <a-row class="form-row" :gutter="24">
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.settlementAccount')" data-step="8" :data-title="$t('purchase.form.settlementAccount')"
                         :data-intro="$t('purchase.form.settlementAccountIntro')">
              <a-select style="width:80%;" :placeholder="$t('purchase.validation.accountRequired')" v-decorator="[ 'accountId' ]"
                        :dropdownMatchSelectWidth="false" allowClear @select="selectAccount">
                <div slot="dropdownRender" slot-scope="menu">
                  <v-nodes :vnodes="menu" />
                  <a-divider style="margin: 4px 0;" />
                  <div v-if="quickBtn.account" class="dropdown-btn" @mousedown="e => e.preventDefault()" @click="addAccount"><a-icon type="plus" /> {{ $t('common.add') }}</div>
                  <div class="dropdown-btn" @mousedown="e => e.preventDefault()" @click="initAccount(0)"><a-icon type="reload" /> {{ $t('common.refresh') }}</div>
                </div>
                <a-select-option v-for="(item,index) in accountList" :key="index" :value="item.id">
                  {{ item.name }}
                </a-select-option>
              </a-select>
              <a-tooltip :title="$t('purchase.form.manyAccountDetails')">
                <a-button type="default" icon="folder" style="margin-left: 8px;" size="small" v-show="manyAccountBtnStatus" @click="handleManyAccount"/>
              </a-tooltip>
            </a-form-item>
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.payDeposit')" data-step="9" :data-title="$t('purchase.form.payDeposit')"
                         :data-intro="$t('purchase.form.payDepositIntro')">
              <a-input :placeholder="$t('purchase.form.payDepositPlaceholder')" v-decorator.trim="[ 'changeAmount', validatorRules.price ]" @change="onChangeChangeAmount"/>
            </a-form-item>
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
          </a-col>
        </a-row>
        <a-row class="form-row" :gutter="24">
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.attachment')" data-step="10" :data-title="$t('common.attachment')" :data-intro="$t('common.attachmentIntro')">
              <j-upload v-model="fileList" bizPath="bill"></j-upload>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-spin>
    <many-account-modal ref="manyAccountModalForm" @ok="manyAccountModalFormOk"></many-account-modal>
    <import-item-modal ref="importItemModalForm" @ok="importItemModalFormOk"></import-item-modal>
    <vendor-modal ref="vendorModalForm" @ok="vendorModalFormOk"></vendor-modal>
    <account-modal ref="accountModalForm" @ok="accountModalFormOk"></account-modal>
    <link-bill-list ref="linkBillList" @ok="linkBillListOk"></link-bill-list>
    <history-bill-list ref="historyBillListModalForm"></history-bill-list>
    <workflow-iframe ref="modalWorkflow" @ok="workflowModalFormOk"></workflow-iframe>
    <bill-print-iframe ref="modalPrint"></bill-print-iframe>
    <bill-print-pro-iframe ref="modalPrintPro"></bill-print-pro-iframe>
  </j-modal>
</template>
<script>
  import pick from 'lodash.pick'
  import ManyAccountModal from '../dialog/ManyAccountModal'
  import ImportItemModal from '../dialog/ImportItemModal'
  import LinkBillList from '../dialog/LinkBillList'
  import VendorModal from '../../system/modules/VendorModal'
  import AccountModal from '../../system/modules/AccountModal'
  import HistoryBillList from '../dialog/HistoryBillList'
  import WorkflowIframe from '@/components/tools/WorkflowIframe'
  import BillPrintIframe from '../dialog/BillPrintIframe'
  import BillPrintProIframe from '../dialog/BillPrintProIframe'
  import { FormTypes } from '@/utils/JEditableTableUtil'
  import { JEditableTableMixin } from '@/mixins/JEditableTableMixin'
  import { BillModalMixin } from '../mixins/BillModalMixin'
  import { findBillDetailByNumber, getCurrentSystemConfig } from '@/api/api'
  import { getMpListShort, changeListFmtMinus,handleIntroJs } from "@/utils/util"
  import JUpload from '@/components/jeecg/JUpload'
  import JDate from '@/components/jeecg/JDate'
  import Vue from 'vue'
  export default {
    name: "PurchaseOrderModal",
    mixins: [JEditableTableMixin,BillModalMixin],
    components: {
      ManyAccountModal,
      ImportItemModal,
      LinkBillList,
      VendorModal,
      AccountModal,
      HistoryBillList,
      WorkflowIframe,
      BillPrintIframe,
      BillPrintProIframe,
      JUpload,
      JDate,
      VNodes: {
        functional: true,
        render: (h, ctx) => ctx.props.vnodes,
      }
    },
    data () {
      return {
        title: this.$t('common.operation'),
        width: '1600px',
        moreStatus: false,
        // 新增时子表默认添加几行空数据
        addDefaultRowNum: 1,
        visible: false,
        supList: [],
        depotList: [],
        operTimeStr: '',
        prefixNo: 'CGDD',
        fileList:[],
        rowCanEdit: true,
        //以销定购的场景开关
        purchaseBySaleFlag: false,
        model: {},
        labelCol: {
          xs: { span: 24 },
          sm: { span: 8 },
        },
        wrapperCol: {
          xs: { span: 24 },
          sm: { span: 16 },
        },
        refKeys: ['materialDataTable', ],
        tableKeys:['materialDataTable', ],
        activeKey: 'materialDataTable',
        materialTable: {
          loading: false,
          dataSource: [],
          columns: [
            { title: '', key: 'hiddenKey', width: '1%', type: FormTypes.hidden },
            { title: () => this.$t('purchase.form.columns.barcode'), key: 'barCode', width: '12%', type: FormTypes.popupJsh, kind: 'material', multi: true,
              validateRules: [{ required: true, message: () => this.$t('purchase.validation.barcodeRequired') }]
            },
            { title: () => this.$t('purchase.form.columns.name'), key: 'name', width: '10%', type: FormTypes.normal },
            { title: () => this.$t('purchase.form.columns.specification'), key: 'standard', width: '9%', type: FormTypes.normal },
            { title: () => this.$t('purchase.form.columns.model'), key: 'model', width: '9%', type: FormTypes.normal },
            { title: () => this.$t('purchase.form.columns.color'), key: 'color', width: '5%', type: FormTypes.normal },
            { title: () => this.$t('purchase.form.columns.brand'), key: 'brand', width: '6%', type: FormTypes.normal },
            { title: () => this.$t('purchase.form.columns.manufacturer'), key: 'mfrs', width: '6%', type: FormTypes.normal },
            { title: () => this.$t('purchase.form.columns.ext1'), key: 'otherField1', width: '4%', type: FormTypes.normal },
            { title: () => this.$t('purchase.form.columns.ext2'), key: 'otherField2', width: '4%', type: FormTypes.normal },
            { title: () => this.$t('purchase.form.columns.ext3'), key: 'otherField3', width: '4%', type: FormTypes.normal },
            { title: () => this.$t('purchase.form.columns.stock'), key: 'stock', width: '5%', type: FormTypes.normal },
            { title: () => this.$t('purchase.form.columns.unit'), key: 'unit', width: '4%', type: FormTypes.normal },
            { title: () => this.$t('purchase.form.columns.sku'), key: 'sku', width: '9%', type: FormTypes.normal },
            { title: () => this.$t('purchase.form.preNumber'), key: 'preNumber', width: '4%', type: FormTypes.normal },
            { title: () => this.$t('purchase.form.finishPurchased'), key: 'finishNumber', width: '4%', type: FormTypes.normal },
            { title: () => this.$t('purchase.form.columns.quantity'), key: 'operNumber', width: '5%', type: FormTypes.inputNumber, statistics: true,
              validateRules: [
                { required: true, message: () => this.$t('purchase.validation.quantityRequired') },
                { pattern: /^(?=.*[1-9])\d+(?:\.\d+)?$/, message: () => this.$t('purchase.validation.quantityMustBePositive') }
              ]
            },
            { title: () => this.$t('purchase.form.columns.unitPrice'), key: 'unitPrice', width: '5%', type: FormTypes.inputNumber },
            { title: () => this.$t('purchase.form.columns.amount'), key: 'allPrice', width: '5%', type: FormTypes.inputNumber, statistics: true },
            { title: () => this.$t('purchase.form.columns.taxRate'), key: 'taxRate', width: '4%', type: FormTypes.inputNumber,placeholder: '%'},
            { title: () => this.$t('purchase.form.columns.taxAmount'), key: 'taxMoney', width: '5%', type: FormTypes.inputNumber, readonly: true, statistics: true },
            { title: () => this.$t('purchase.form.columns.taxTotal'), key: 'taxLastMoney', width: '7%', type: FormTypes.inputNumber, statistics: true },
            { title: () => this.$t('purchase.form.columns.remark'), key: 'remark', width: '6%', type: FormTypes.input},
            { title: '关联id', key: 'linkId', width: '5%', type: FormTypes.hidden },
          ]
        },
        confirmLoading: false,
        validatorRules:{
          operTime:{
            rules: [
              { required: true, message: this.$t('purchase.validation.documentDateRequired') }
            ]
          },
          number:{
            rules: [
              { required: true, message: this.$t('purchase.validation.documentNumberRequired') }
            ]
          },
          organId:{
            rules: [
              { required: true, message: this.$t('purchase.validation.supplierRequired') }
            ]
          }
        },
        url: {
          add: '/depotHead/addDepotHeadAndDetail',
          edit: '/depotHead/updateDepotHeadAndDetail',
          detailList: '/depotItem/getDetailList',
          importExcelUrl: "/depotItem/importItemExcel",
        }
      }
    },
    created () {
    },
    methods: {
      //调用完edit()方法之后会自动调用此方法
      editAfter() {
        this.billStatus = '0'
        this.currentSelectDepotId = ''
        this.rowCanEdit = true
        this.materialTable.columns[1].type = FormTypes.popupJsh
        this.changeColumnHide()
        this.changeFormTypes(this.materialTable.columns, 'preNumber', 0)
        this.changeFormTypes(this.materialTable.columns, 'finishNumber', 0)
        if (this.action === 'add') {
          this.addInit(this.prefixNo)
          this.fileList = []
          this.$nextTick(() => {
            handleIntroJs(this.prefixNo, 1)
            if(this.transferParam && this.transferParam.number) {
              let tp = this.transferParam
              this.linkBillListOk(tp.list, tp.number, tp.organId)
            }
          })
        } else {
          if(this.model.linkNumber || this.model.linkApply) {
            this.rowCanEdit = false
            this.materialTable.columns[1].type = FormTypes.normal
          }
          this.model.operTime = this.model.operTimeStr
          if(this.model.accountId == null && this.model.accountIdList) {
            this.model.accountId = 0
            this.manyAccountBtnStatus = true
            this.accountIdList = this.model.accountIdList
            this.accountMoneyList = this.model.accountMoneyList
          } else {
            this.manyAccountBtnStatus = false
          }
          this.fileList = this.model.fileName
          this.$nextTick(() => {
            this.form.setFieldsValue(pick(this.model,'organId', 'operTime', 'number', 'linkApply', 'linkNumber', 'remark',
            'discount','discountMoney','discountLastMoney','accountId','changeAmount'))
          });
          // 加载子表数据
          let params = {
            headerId: this.model.id,
            mpList: getMpListShort(Vue.ls.get('materialPropertyList')),  //扩展属性
            linkType: 'basic'
          }
          let url = this.readOnly ? this.url.detailList : this.url.detailList;
          this.requestSubTableData(url, params, this.materialTable);
        }
        //复制新增单据-初始化单号和日期
        if(this.action === 'copyAdd') {
          this.model.id = ''
          this.model.tenantId = ''
          this.copyAddInit(this.prefixNo)
        }
        this.initSystemConfig()
        this.initSupplier(0)
        this.initAccount(0)
        this.initPlatform()
        this.initQuickBtn()
        this.handleChangeOtherField()
      },
      /** 整理成formData */
      classifyIntoFormData(allValues) {
        let totalPrice = 0
        let billMain = Object.assign(this.model, allValues.formValue)
        let detailArr = allValues.tablesValue[0].values
        billMain.type = '其它'
        billMain.subType = '采购订单'
        for(let item of detailArr){
          item.depotId = '' //订单不需要仓库
          totalPrice += item.allPrice-0
        }
        billMain.totalPrice = 0-totalPrice
        billMain.changeAmount = 0-billMain.changeAmount
        if(billMain.accountId === 0) {
          billMain.accountId = ''
        }
        this.accountMoneyList = changeListFmtMinus(this.accountMoneyList)
        billMain.accountIdList = this.accountIdList.length>0 ? JSON.stringify(this.accountIdList) : ""
        billMain.accountMoneyList = this.accountMoneyList.length>0 ? JSON.stringify(this.accountMoneyList) : ""
        if(this.fileList && this.fileList.length > 0) {
          billMain.fileName = this.fileList
        } else {
          billMain.fileName = ''
        }
        if(this.model.id){
          billMain.id = this.model.id
        }
        billMain.status = this.billStatus
        return {
          info: JSON.stringify(billMain),
          rows: JSON.stringify(detailArr),
        }
      },
      handleHistoryBillList() {
        let organId = this.form.getFieldValue('organId')
        this.$refs.historyBillListModalForm.show('其它', '采购订单', '供应商', organId);
        this.$refs.historyBillListModalForm.disableSubmit = false;
      },
      onSearchLinkNumber() {
        this.$refs.linkBillList.purchaseShow('其它', '销售订单', '客户', "1,3","0,3")
        this.$refs.linkBillList.title = this.$t('purchase.form.selectLinkedOrder')
      },
      onSearchLinkApply() {
        this.$refs.linkBillList.purchaseShow('其它', '请购单', '客户', "1,3")
        this.$refs.linkBillList.title = this.$t('purchase.form.selectLinkedRequisition')
      },
      async linkBillListOk(selectBillDetailRows, linkNumber, organId) {
        if(!selectBillDetailRows || selectBillDetailRows.length === 0) {
          return
        }
        this.confirmLoading = true
        try {
          //先确认来源类型并写入关联字段，避免明细已载入但关联单号尚未设置时被提交
          const res = await findBillDetailByNumber({'number':linkNumber})
          if (!res || res.code !== 200 || !res.data) {
            this.$message.error(this.$t('purchase.validation.linkBillNotFound'))
            return
          }
          const isPurchaseApply = res.data.subType === '请购单'
          await new Promise(resolve => {
            this.$nextTick(() => {
              this.form.setFieldsValue({
                'linkApply': isPurchaseApply ? linkNumber : '',
                'linkNumber': isPurchaseApply ? '' : linkNumber
              })
              resolve()
            })
          })

          this.rowCanEdit = false
          this.materialTable.columns[1].type = FormTypes.normal
          this.changeFormTypes(this.materialTable.columns, 'preNumber', 1)
          this.changeFormTypes(this.materialTable.columns, 'finishNumber', 1)
          let listEx = []
          let discountLastMoney = 0
          for(let j=0; j<selectBillDetailRows.length; j++) {
            let info = selectBillDetailRows[j];
            if (info.preNumber) {
              info.operNumber = info.preNumber - info.finishNumber
              info.unitPrice = info.purchaseDecimal
              info.allPrice = (info.operNumber * info.unitPrice).toFixed(2) - 0;
              info.taxRate = 0
              info.taxMoney = 0
              info.taxLastMoney = info.allPrice
              discountLastMoney += info.allPrice
            }
            info.linkId = info.id
            if(info.operNumber>0) {
              listEx.push(info)
              this.changeColumnShow(info)
            }
          }
          this.materialTable.dataSource = listEx
          //给优惠后金额重新赋值
          discountLastMoney = discountLastMoney?discountLastMoney:0
          this.$nextTick(() => {
            this.form.setFieldsValue({
              'discountLastMoney': discountLastMoney.toFixed(2),
              'changeAmount': 0
            })
          })
        } finally {
          this.confirmLoading = false
        }
      }
    }
  }
</script>
<style scoped>

</style>
