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
      <a-button v-if="billPrintFlag && isShowPrintBtn" @click="handlePrintPro('销售出库')">{{ $t('common.printNew') }}</a-button>
      <a-button v-if="billPrintFlag && isShowPrintBtn" @click="handlePrint('销售出库')">{{ $t('common.print') }}</a-button>
      <a-button v-if="checkFlag && isCanCheck" :loading="confirmLoading" @click="handleOkAndCheck">{{ $t('common.saveAndApprove') }}</a-button>
      <a-button type="primary" :loading="confirmLoading" @click="handleOkOnly">{{ $t('common.save') }}</a-button>
      <!--发起多级审核-->
      <a-button v-if="!checkFlag" @click="handleWorkflow()" type="primary">{{ $t('common.submitWorkflow') }}</a-button>
    </template>
    <a-spin :spinning="confirmLoading">
      <a-form :form="form">
        <a-row class="form-row" :gutter="24">
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.customer')" data-step="1" :data-title="$t('common.customer')"
              :data-intro="$t('guide.customer')">
              <a-select :placeholder="$t('common.selectCustomer')" v-decorator="[ 'organId', validatorRules.organId ]" :disabled="!rowCanEdit"
                :dropdownMatchSelectWidth="false" showSearch optionFilterProp="children" @change="handleOrganChange" @search="handleSearchCustomer">
                <div slot="dropdownRender" slot-scope="menu">
                  <v-nodes :vnodes="menu" />
                  <a-divider style="margin: 4px 0;" />
                  <div v-if="quickBtn.customer" class="dropdown-btn" @mousedown="e => e.preventDefault()" @click="addCustomer"><a-icon type="plus" /> 新增客户</div>
                  <div class="dropdown-btn" @mousedown="e => e.preventDefault()" @click="initCustomer(0)"><a-icon type="reload" /> 刷新列表</div>
                </div>
                <a-select-option v-for="(item,index) in cusList" :key="index" :value="item.id">
                  {{ item.supplier }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billDate')">
              <j-date v-decorator="['operTime', validatorRules.operTime]" :show-time="true"/>
            </a-form-item>
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billNo')" data-step="2" :data-title="$t('common.billNo')"
                         :data-intro="$t('guide.autoNumber')">
              <a-input :placeholder="$t('common.enterBillNo')" v-decorator.trim="[ 'number', validatorRules.number ]"
                :readOnly="action !== 'add' && action !== 'copyAdd'" />
            </a-form-item>
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.linkedBill')" data-step="3" :data-title="$t('common.linkedBill')"
              :data-intro="$t('guide.salesLinkedOrder')">
              <a-input-search :placeholder="$t('common.selectLinkedOrder')" v-decorator="[ 'linkNumber' ]" @search="onSearchLinkNumber"
                :readOnly="true" :disabled="action !== 'add' && action !== 'copyAdd'"/>
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
            <a-row v-if="rowCanEdit" :gutter="24" style="float:left;padding-bottom:5px;padding-right:8px" data-step="4" :data-title="$t('common.scanEntry')" :data-intro="$t('common.scanEntry')">
              <a-col v-if="scanStatus" :md="6" :sm="24">
                <a-button @click="scanEnter">{{ $t('common.scanEntry') }}</a-button>
              </a-col>
              <a-col v-if="!scanStatus" :md="16" :sm="24" style="padding: 0 8px 0 12px">
                <a-input :placeholder="$t('common.scanSnPlaceholder')" v-model="scanBarCode" @pressEnter="scanPressEnter" ref="scanBarCode"/>
              </a-col>
              <a-col v-if="!scanStatus" :md="6" :sm="24" style="padding: 0px 12px 0 0">
                <a-button @click="stopScan">{{ $t('common.hideScan') }}</a-button>
              </a-col>
            </a-row>
            <a-row :gutter="24" style="float:left;padding-bottom: 5px;">
              <a-col :md="24" :sm="24">
                <a-button @click="handleHistoryBillList"><a-icon type="history" />历史单据</a-button>
              </a-col>
            </a-row>
            <a-row v-if="rowCanEdit" :gutter="24" style="float:left;padding-bottom: 5px;padding-left:20px;">
              <a-button icon="import" @click="onImport(prefixNo)">导入明细</a-button>
            </a-row>
          </template>
          <template #depotBatchSet>
            <a-icon type="down" @click="handleBatchSetDepot" />
          </template>
          <template #depotAdd>
            <a-divider v-if="quickBtn.depot" style="margin: 4px 0;" />
            <div v-if="quickBtn.depot" class="dropdown-btn" @click="addDepot"><a-icon type="plus" /> 新增</div>
            <div class="dropdown-btn" @mousedown="e => e.preventDefault()" @click="initDepot"><a-icon type="reload" /> 刷新</div>
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
                         :data-intro="$t('guide.discountRate')">
              <a-input style="width:80%;" :placeholder="$t('purchase.form.discountPlaceholder')" v-decorator.trim="[ 'discount' ]" suffix="%" @change="onChangeDiscount"/>
            </a-form-item>
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.paymentDiscount')" data-step="6" :data-title="$t('common.paymentDiscount')"
                         :data-intro="$t('guide.discountAmount')">
              <a-input :placeholder="$t('purchase.form.discountMoneyPlaceholder')" v-decorator.trim="[ 'discountMoney' ]" @change="onChangeDiscountMoney"/>
            </a-form-item>
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.discountLastMoney')" data-step="7" :data-title="$t('purchase.form.discountLastMoney')"
                         :data-intro="$t('guide.discountedAmount')">
              <a-input :placeholder="$t('common.enterDiscountAfterAmount')" v-decorator.trim="[ 'discountLastMoney' ]" :readOnly="true"/>
            </a-form-item>
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.otherMoney')" data-step="8" :data-title="$t('purchase.form.otherMoney')"
                         :data-intro="$t('guide.otherExpense')">
              <a-input :placeholder="$t('purchase.form.otherMoneyPlaceholder')" v-decorator.trim="[ 'otherMoney' ]" @change="onChangeOtherMoney"/>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row class="form-row" :gutter="24">
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.settleAccount')" data-step="9" :data-title="$t('common.settleAccount')"
                         :data-intro="$t('guide.multiAccount')">
              <a-select style="width:80%;" :placeholder="$t('common.selectSettleAccount')" v-decorator="[ 'accountId', validatorRules.accountId ]"
                        :dropdownMatchSelectWidth="false" allowClear @select="selectAccount">
                <div slot="dropdownRender" slot-scope="menu">
                  <v-nodes :vnodes="menu" />
                  <a-divider style="margin: 4px 0;" />
                  <div v-if="quickBtn.account" class="dropdown-btn" @mousedown="e => e.preventDefault()" @click="addAccount"><a-icon type="plus" /> 新增</div>
                  <div class="dropdown-btn" @mousedown="e => e.preventDefault()" @click="initAccount(0)"><a-icon type="reload" /> 刷新</div>
                </div>
                <a-select-option v-for="(item,index) in accountList" :key="index" :value="item.id">
                  {{ item.name }}
                </a-select-option>
              </a-select>
              <a-tooltip title="多账户明细">
                <a-button type="default" icon="folder" style="margin-left: 8px;" size="small" v-show="manyAccountBtnStatus" @click="handleManyAccount"/>
              </a-tooltip>
            </a-form-item>
          </a-col>
          <a-col v-if="depositStatus" :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.deductDeposit')">
              <a-input v-decorator.trim="[ 'deposit' ]" @change="onChangeDeposit"/>
            </a-form-item>
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.currentReceipt')">
              <a-input :placeholder="$t('common.enterCurrentReceipt')" v-decorator.trim="[ 'changeAmount', validatorRules.changeAmount ]" @change="onChangeChangeAmount"/>
            </a-form-item>
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.currentDebt')" data-step="10" :data-title="$t('common.currentDebt')"
                         :data-intro="$t('guide.salesDebt')">
              <a-input :placeholder="$t('common.enterCurrentDebt')" v-decorator.trim="[ 'debt', validatorRules.price ]" :readOnly="true"/>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row class="form-row" :gutter="24">
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.salesMan')" data-step="11" :data-title="$t('common.salesMan')"
                         :data-intro="$t('guide.salesPerson')">
              <j-select-multiple style="width:80%;" :placeholder="$t('common.selectSalesMan')" v-model="personList.value" :options="personList.options"/>
            </a-form-item>
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
          </a-col>
        </a-row>
        <a-row class="form-row" :gutter="24">
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.attachment')" data-step="12" :data-title="$t('common.attachment')"
                         :data-intro="$t('guide.attachment')">
              <j-upload v-model="fileList" bizPath="bill"></j-upload>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-spin>
    <many-account-modal ref="manyAccountModalForm" @ok="manyAccountModalFormOk"></many-account-modal>
    <import-item-modal ref="importItemModalForm" @ok="importItemModalFormOk"></import-item-modal>
    <link-bill-list ref="linkBillList" @ok="linkBillListOk"></link-bill-list>
    <customer-modal ref="customerModalForm" @ok="customerModalFormOk"></customer-modal>
    <depot-modal ref="depotModalForm" @ok="depotModalFormOk"></depot-modal>
    <account-modal ref="accountModalForm" @ok="accountModalFormOk"></account-modal>
    <batch-set-depot ref="batchSetDepotModalForm" @ok="batchSetDepotModalFormOk"></batch-set-depot>
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
  import CustomerModal from '../../system/modules/CustomerModal'
  import DepotModal from '../../system/modules/DepotModal'
  import AccountModal from '../../system/modules/AccountModal'
  import BatchSetDepot from '../dialog/BatchSetDepot'
  import HistoryBillList from '../dialog/HistoryBillList'
  import WorkflowIframe from '@/components/tools/WorkflowIframe'
  import BillPrintIframe from '../dialog/BillPrintIframe'
  import BillPrintProIframe from '../dialog/BillPrintProIframe'
  import { FormTypes } from '@/utils/JEditableTableUtil'
  import { JEditableTableMixin } from '@/mixins/JEditableTableMixin'
  import { BillModalMixin } from '../mixins/BillModalMixin'
  import { getMpListShort,handleIntroJs } from "@/utils/util"
  import JSelectMultiple from '@/components/jeecg/JSelectMultiple'
  import JUpload from '@/components/jeecg/JUpload'
  import JDate from '@/components/jeecg/JDate'
  import Vue from 'vue'
  import { getCurrentSystemConfig, findBySelectCus } from '@/api/api'
  export default {
    name: "SaleOutModal",
    mixins: [JEditableTableMixin, BillModalMixin],
    components: {
      ManyAccountModal,
      ImportItemModal,
      LinkBillList,
      CustomerModal,
      DepotModal,
      AccountModal,
      BatchSetDepot,
      HistoryBillList,
      WorkflowIframe,
      BillPrintIframe,
      BillPrintProIframe,
      JUpload,
      JDate,
      JSelectMultiple,
      VNodes: {
        functional: true,
        render: (h, ctx) => ctx.props.vnodes,
      }
    },
    data () {
      return {
        title: this.$t('common.action'),
        width: '1600px',
        moreStatus: false,
        // 新增时子表默认添加几行空数据
        addDefaultRowNum: 1,
        visible: false,
        operTimeStr: '',
        prefixNo: 'XSCK',
        depositStatus: false,
        fileList:[],
        rowCanEdit: true,
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
        activeKey: 'materialDataTable',
        materialTable: {
          loading: false,
          dataSource: [],
          columns: [
            { title: this.$t('common.depotName'), key: 'depotId', width: '8%', type: FormTypes.select, placeholder: '请选择${title}', options: [],
              allowSearch:true, validateRules: [{ required: true, message: '${title}不能为空' }]
            },
            { title: this.$t('common.barcode'), key: 'barCode', width: '12%', type: FormTypes.popupJsh, kind: 'material', multi: true,
              validateRules: [{ required: true, message: '${title}不能为空' }]
            },
            { title: this.$t('common.name'), key: 'name', width: '10%', type: FormTypes.normal },
            { title: this.$t('common.specification'), key: 'standard', width: '9%', type: FormTypes.normal },
            { title: this.$t('common.model'), key: 'model', width: '9%', type: FormTypes.normal },
            { title: this.$t('material.color'), key: 'color', width: '5%', type: FormTypes.normal },
            { title: this.$t('common.brand'), key: 'brand', width: '6%', type: FormTypes.normal },
            { title: this.$t('material.manufacturer'), key: 'mfrs', width: '6%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.ext1'), key: 'otherField1', width: '4%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.ext2'), key: 'otherField2', width: '4%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.ext3'), key: 'otherField3', width: '4%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.stock'), key: 'stock', width: '5%', type: FormTypes.normal },
            { title: this.$t('common.unit'), key: 'unit', width: '4%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.serialNumber'), key: 'snList', width: '12%', type: FormTypes.popupJsh, kind: 'sn', multi: true },
            { title: this.$t('purchase.form.columns.batchNumber'), key: 'batchNumber', width: '7%', type: FormTypes.popupJsh, kind: 'batch', multi: false },
            { title: this.$t('purchase.form.columns.expirationDate'), key: 'expirationDate',width: '7%', type: FormTypes.input, readonly: true },
            { title: this.$t('purchase.form.columns.sku'), key: 'sku', width: '9%', type: FormTypes.normal },
            { title: this.$t('common.originalQuantity'), key: 'preNumber', width: '4%', type: FormTypes.normal },
            { title: this.$t('sales.finishNumber'), key: 'finishNumber', width: '4%', type: FormTypes.normal },
            { title: this.$t('common.quantity'), key: 'operNumber', width: '4%', type: FormTypes.inputNumber, statistics: true,
              validateRules: [{ required: true, message: '${title}不能为空' }]
            },
            { title: this.$t('purchase.form.columns.unitPrice'), key: 'unitPrice', width: '4%', type: FormTypes.inputNumber},
            { title: this.$t('common.amount'), key: 'allPrice', width: '5%', type: FormTypes.inputNumber, statistics: true },
            { title: this.$t('purchase.form.columns.taxRate'), key: 'taxRate', width: '4%', type: FormTypes.inputNumber,placeholder: '%'},
            { title: this.$t('purchase.form.columns.taxAmount'), key: 'taxMoney', width: '5%', type: FormTypes.inputNumber, readonly: true, statistics: true },
            { title: this.$t('purchase.form.columns.taxTotal'), key: 'taxLastMoney', width: '7%', type: FormTypes.inputNumber, statistics: true },
            { title: this.$t('common.remark'), key: 'remark', width: '6%', type: FormTypes.input },
            { title: '关联id', key: 'linkId', width: '5%', type: FormTypes.hidden },
          ]
        },
        confirmLoading: false,
        validatorRules:{
          operTime:{
            rules: [
              { required: true, message: this.$t('common.fieldRequired') }
            ]
          },
          number:{
            rules: [
              { required: true, message: this.$t('purchase.validation.documentNumberRequired') }
            ]
          },
          organId:{
            rules: [
              { required: true, message: '请选择客户！' }
            ]
          },
          accountId:{
            rules: [
              { required: true, message: '请选择结算账户！' }
            ]
          },
          changeAmount:{
            rules: [
              { required: true, message: '请输入金额，如果为空请填0！' },
              { pattern: /^(([0-9][0-9]*)|([0]\.\d{0,4}|[0-9][0-9]*\.\d{0,4}))$/, message: this.$t('purchase.validation.amountPattern') }
            ]
          }
        },
        url: {
          add: '/depotHead/addDepotHeadAndDetail',
          edit: '/depotHead/updateDepotHeadAndDetail',
          detailList: '/depotItem/getDetailList'
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
        this.changeFormTypes(this.materialTable.columns, 'snList', 0)
        this.changeFormTypes(this.materialTable.columns, 'batchNumber', 0)
        this.changeFormTypes(this.materialTable.columns, 'expirationDate', 0)
        this.changeFormTypes(this.materialTable.columns, 'preNumber', 0)
        this.changeFormTypes(this.materialTable.columns, 'finishNumber', 0)
        this.salesConfigReady = Promise.all([this.initSystemConfig(), this.initDepot()])
        if (this.action === 'add') {
          this.depositStatus = false
          this.addInit(this.prefixNo)
          this.personList.value = ''
          this.fileList = []
          this.$nextTick(() => {
            handleIntroJs(this.prefixNo, 1)
            if(this.transferParam && this.transferParam.number) {
              let tp = this.transferParam
              this.salesConfigReady.then(() => {
                this.linkBillListOk(tp.list, tp.number, tp.organId, tp.discount, tp.deposit, tp.remark,
                  this.defaultDepotId, tp.accountId, tp.salesMan)
              })
            }
          })
        } else {
          if(this.model.linkNumber) {
            this.rowCanEdit = false
            this.materialTable.columns[1].type = FormTypes.normal
          }
          this.model.operTime = this.model.operTimeStr
          if(this.model.deposit) {
            this.depositStatus = true
          } else {
            this.depositStatus = false
            this.model.deposit = 0
          }
          this.model.debt = (this.model.discountLastMoney + this.model.otherMoney - this.model.deposit - this.model.changeAmount).toFixed(2)
          if(this.model.accountId == null) {
            this.model.accountId = 0
            this.manyAccountBtnStatus = true
            this.accountIdList = this.model.accountIdList
            this.accountMoneyList = this.model.accountMoneyList
          } else {
            this.manyAccountBtnStatus = false
          }
          this.personList.value = this.model.salesMan
          this.fileList = this.model.fileName
          this.$nextTick(() => {
            this.form.setFieldsValue(pick(this.model,'organId', 'operTime', 'number', 'linkNumber', 'remark',
              'discount','discountMoney','discountLastMoney','otherMoney','accountId','deposit','changeAmount','debt','salesMan'))
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
        this.initCustomer(0)
        this.initSalesman()
        this.initAccount(0)
        this.initPlatform()
        this.initQuickBtn()
        this.handleChangeOtherField()
      },
      //提交单据时整理成formData
      classifyIntoFormData(allValues) {
        let totalPrice = 0
        let billMain = Object.assign(this.model, allValues.formValue)
        let detailArr = allValues.tablesValue[0].values
        billMain.type = '出库'
        billMain.subType = '销售'
        for(let item of detailArr){
          totalPrice += item.allPrice-0
        }
        billMain.totalPrice = totalPrice
        if(billMain.accountId === 0) {
          billMain.accountId = ''
        }
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
        billMain.salesMan = this.personList.value
        billMain.status = this.billStatus
        return {
          info: JSON.stringify(billMain),
          rows: JSON.stringify(detailArr),
        }
      },
      handleHistoryBillList() {
        let organId = this.form.getFieldValue('organId')
        this.$refs.historyBillListModalForm.show('出库', '销售', '客户', organId);
        this.$refs.historyBillListModalForm.disableSubmit = false;
      },
      onSearchLinkNumber() {
        this.$refs.linkBillList.show('其它', '销售订单', '客户', "1,3")
        this.$refs.linkBillList.title = this.$t('sales.form.selectLinkedOrder')
      },
      async linkBillListOk(selectBillDetailRows, linkNumber, organId, discount, deposit, remark, depotId, accountId, salesMan) {
        if (this.salesConfigReady) {
          await this.salesConfigReady
        }
        let that = this
        this.rowCanEdit = false
        this.materialTable.columns[1].type = FormTypes.normal
        this.changeFormTypes(this.materialTable.columns, 'preNumber', 1)
        this.changeFormTypes(this.materialTable.columns, 'finishNumber', 1)
        if(selectBillDetailRows && selectBillDetailRows.length>0) {
          let listEx = []
          let allTaxLastMoney = 0
          for(let j=0; j<selectBillDetailRows.length; j++) {
            let info = selectBillDetailRows[j];
            if(info.finishNumber>0) {
              info.operNumber = info.preNumber - info.finishNumber
              info.allPrice = info.operNumber * info.unitPrice-0
              let taxRate = info.taxRate-0
              if(this.materialPriceTaxFlag) {
                let realAllPrice = (info.allPrice/(1+taxRate*0.01)).toFixed(2)-0
                info.taxMoney = (realAllPrice*taxRate*0.01).toFixed(2)-0
                info.taxLastMoney = info.allPrice
              } else {
                info.taxMoney = (info.allPrice*taxRate/100).toFixed(2)-0
                info.taxLastMoney = (info.allPrice + info.taxMoney).toFixed(2)-0
              }
            }
            info.linkId = info.id
            allTaxLastMoney += info.taxLastMoney
            if(info.operNumber>0) {
              listEx.push(info)
              this.changeColumnShow(info)
            }
          }
          this.materialTable.dataSource = listEx
          ///给优惠后金额重新赋值
          allTaxLastMoney = allTaxLastMoney?allTaxLastMoney:0
          let discountMoney = 0
          if(allTaxLastMoney!==0) {
            discountMoney = (discount/100*allTaxLastMoney).toFixed(2)-0
          }
          let discountLastMoney = (allTaxLastMoney - discountMoney).toFixed(2)-0
          let changeAmount = discountLastMoney
          if(deposit) {
            this.depositStatus = true
            changeAmount = (discountLastMoney - deposit).toFixed(2)-0
          }
          this.$nextTick(() => {
            this.form.setFieldsValue({
              'organId': organId,
              'linkNumber': linkNumber,
              'discount': discount,
              'discountMoney': discountMoney,
              'discountLastMoney': discountLastMoney,
              'deposit': deposit,
              'changeAmount': changeAmount,
              'accountId': accountId,
              'remark': remark
            })
            findBySelectCus({organId: organId, limit:1}).then((res)=> {
              this.cusList = res && Array.isArray(res) ? res : [];
            })
            getCurrentSystemConfig().then((res) => {
              if (res.code === 200 && res.data) {
                let flag = res.data.zeroChangeAmountFlag==='1'?true:false
                if(flag) {
                  //切换收付款的金额为0
                  let oldChangeAmount = this.form.getFieldValue('changeAmount')-0
                  this.form.setFieldsValue({'changeAmount':0, 'debt':oldChangeAmount})
                }
              }
            })
            this.personList.value = salesMan
          })
          //判断后进行仓库的切换
          if(depotId) {
            this.$nextTick(function () {
              that.batchSetDepotModalFormOk(depotId, listEx)
            })
          }
        }
      },
    }
  }
</script>
<style scoped>

</style>
