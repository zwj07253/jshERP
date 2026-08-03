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
    switchFullscreen
    @cancel="handleCancel"
    :id="prefixNo"
    style="top:20px;height: 95%;">
    <template slot="footer">
      <a-button @click="handleCancel">{{ $t('common.cancel') }}</a-button>
      <a-button v-if="billPrintFlag && isShowPrintBtn" @click="handlePrintPro($t('purchase.purchaseReturnOutbound'))">{{ $t('common.printNew') }}</a-button>
      <a-button v-if="billPrintFlag && isShowPrintBtn" @click="handlePrint($t('purchase.purchaseReturnOutbound'))">{{ $t('common.print') }}</a-button>
      <a-button v-if="checkFlag && isCanCheck" :loading="confirmLoading" @click="handleOkAndCheck">{{ $t('common.saveAndApprove') }}</a-button>
      <a-button type="primary" :loading="confirmLoading" @click="handleOkOnly">{{ $t('common.save') }}（Ctrl+S）</a-button>
      <!--发起多级审核-->
      <a-button v-if="!checkFlag" @click="handleWorkflow()" type="primary">{{ $t('common.submitWorkflow') }}</a-button>
    </template>
    <a-spin :spinning="confirmLoading">
      <a-form :form="form">
        <a-row class="form-row" :gutter="24">
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.supplier')">
              <a-select :placeholder="$t('purchase.selectSupplier')" v-decorator="[ 'organId', validatorRules.organId ]" :disabled="!rowCanEdit"
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
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.documentNumber')">
              <a-input :placeholder="$t('purchase.form.documentNumber')" :disabled="action === 'edit'"
                       v-decorator.trim="[ 'number', validatorRules.number ]" />
            </a-form-item>
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.linkedBill')">
              <a-input-search :placeholder="$t('purchase.form.selectLinkedBill')" v-decorator="[ 'linkNumber' ]" @search="onSearchLinkNumber"
                              :readOnly="true" :disabled="action === 'edit'"/>
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
            <a-row v-if="rowCanEdit" :gutter="24" style="float:left;padding-bottom:5px;padding-right:8px" data-step="4" :data-title="$t('purchase.form.scanEntry')" :data-intro="$t('common.scanEntry')">
              <a-col v-if="scanStatus" :md="6" :sm="24">
                <a-button @click="scanEnter" style="margin-right: 8px">{{ $t('purchase.form.scanEntry') }}</a-button>
              </a-col>
              <a-col v-if="!scanStatus" :md="16" :sm="24" style="padding: 0 6px 0 12px">
                <a-input :placeholder="$t('common.scanSnPlaceholder')" v-model="scanBarCode" @pressEnter="scanPressEnter" ref="scanBarCode"/>
              </a-col>
              <a-col v-if="!scanStatus" :md="6" :sm="24" style="padding: 0px 12px 0 0">
                <a-button @click="stopScan" style="margin-right: 8px">{{ $t('purchase.form.hideScan') }}</a-button>
              </a-col>
            </a-row>
          </template>
          <template #depotBatchSet>
            <a-icon type="down" @click="handleBatchSetDepot" />
          </template>
          <template #depotAdd>
            <a-divider v-if="quickBtn.depot" style="margin: 4px 0;" />
            <div v-if="quickBtn.depot" class="dropdown-btn" @click="addDepot"><a-icon type="plus" /> {{ $t('purchase.form.add') }}</div>
            <div class="dropdown-btn" @mousedown="e => e.preventDefault()" @click="initDepot"><a-icon type="reload" /> {{ $t('purchase.form.refresh') }}</div>
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
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.discount')">
              <a-input style="width:80%;" :placeholder="$t('purchase.form.discountPlaceholder')" v-decorator.trim="[ 'discount' ]" suffix="%" @change="onChangeDiscount"/>
            </a-form-item>
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.discountMoney')">
              <a-input :placeholder="$t('purchase.form.discountMoneyPlaceholder')" v-decorator.trim="[ 'discountMoney' ]" @change="onChangeDiscountMoney"/>
            </a-form-item>
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.discountLastMoney')">
              <a-input :placeholder="$t('purchase.form.discountLastMoneyPlaceholder')" v-decorator.trim="[ 'discountLastMoney' ]" :readOnly="true"/>
            </a-form-item>
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.otherMoney')">
              <a-input :placeholder="$t('purchase.form.otherMoneyPlaceholder')" v-decorator.trim="[ 'otherMoney' ]" @change="onChangeOtherMoney"/>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row class="form-row" :gutter="24">
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.settlementAccount')">
              <a-select style="width:80%;" :placeholder="$t('purchase.validation.accountRequired')" v-decorator="[ 'accountId', validatorRules.accountId ]"
                        :dropdownMatchSelectWidth="false" allowClear @select="selectAccount">
                <div slot="dropdownRender" slot-scope="menu">
                  <v-nodes :vnodes="menu" />
                  <a-divider style="margin: 4px 0;" />
                  <div v-if="quickBtn.account" class="dropdown-btn" @mousedown="e => e.preventDefault()" @click="addAccount"><a-icon type="plus" /> {{ $t('purchase.form.add') }}</div>
                  <div class="dropdown-btn" @mousedown="e => e.preventDefault()" @click="initAccount(0)"><a-icon type="reload" /> {{ $t('purchase.form.refresh') }}</div>
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
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.changeAmount')">
              <a-input :placeholder="$t('purchase.form.changeAmountPlaceholder')" v-decorator.trim="[ 'changeAmount' ]" @change="onChangeChangeAmount"/>
            </a-form-item>
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.debt')">
              <a-input :placeholder="$t('purchase.form.debtPlaceholder')" v-decorator.trim="[ 'debt' ]" :readOnly="true"/>
            </a-form-item>
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
          </a-col>
        </a-row>
        <a-row class="form-row" :gutter="24">
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.attachment')">
              <j-upload v-model="fileList" bizPath="bill"></j-upload>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-spin>
    <many-account-modal ref="manyAccountModalForm" @ok="manyAccountModalFormOk"></many-account-modal>
    <link-bill-list ref="linkBillList" @ok="linkBillListOk"></link-bill-list>
    <vendor-modal ref="vendorModalForm" @ok="vendorModalFormOk"></vendor-modal>
    <depot-modal ref="depotModalForm" @ok="depotModalFormOk"></depot-modal>
    <account-modal ref="accountModalForm" @ok="accountModalFormOk"></account-modal>
    <batch-set-depot ref="batchSetDepotModalForm" @ok="batchSetDepotModalFormOk"></batch-set-depot>
    <workflow-iframe ref="modalWorkflow" @ok="workflowModalFormOk"></workflow-iframe>
    <bill-print-iframe ref="modalPrint"></bill-print-iframe>
    <bill-print-pro-iframe ref="modalPrintPro"></bill-print-pro-iframe>
  </j-modal>
</template>
<script>
  import pick from 'lodash.pick'
  import ManyAccountModal from '../dialog/ManyAccountModal'
  import LinkBillList from '../dialog/LinkBillList'
  import VendorModal from '../../system/modules/VendorModal'
  import DepotModal from '../../system/modules/DepotModal'
  import AccountModal from '../../system/modules/AccountModal'
  import BatchSetDepot from '../dialog/BatchSetDepot'
  import WorkflowIframe from '@/components/tools/WorkflowIframe'
  import BillPrintIframe from '../dialog/BillPrintIframe'
  import BillPrintProIframe from '../dialog/BillPrintProIframe'
  import { FormTypes } from '@/utils/JEditableTableUtil'
  import { JEditableTableMixin } from '@/mixins/JEditableTableMixin'
  import { BillModalMixin } from '../mixins/BillModalMixin'
  import { getMpListShort} from "@/utils/util"
  import JUpload from '@/components/jeecg/JUpload'
  import JDate from '@/components/jeecg/JDate'
  import Vue from 'vue'
  import { findBySelectSup } from '@/api/api'
  export default {
    name: "PurchaseBackModal",
    mixins: [JEditableTableMixin, BillModalMixin],
    components: {
      ManyAccountModal,
      LinkBillList,
      VendorModal,
      DepotModal,
      AccountModal,
      BatchSetDepot,
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
        title: this.$t('common.action'),
        width: '1600px',
        moreStatus: false,
        // 新增时子表默认添加几行空数据
        addDefaultRowNum: 1,
        visible: false,
        operTimeStr: '',
        prefixNo: 'CGTH',
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
            { title: this.$t('purchase.form.warehouse'), key: 'depotId', width: '8%', type: FormTypes.select, placeholder: '请选择${title}', options: [],
              allowSearch:true, validateRules: [{ required: true, message: '${title}不能为空' }]
            },
            { title: this.$t('purchase.form.columns.barcode'), key: 'barCode', width: '12%', type: FormTypes.popupJsh, kind: 'material', multi: true,
              validateRules: [{ required: true, message: '${title}不能为空' }]
            },
            { title: this.$t('purchase.form.columns.name'), key: 'name', width: '10%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.specification'), key: 'standard', width: '9%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.model'), key: 'model', width: '9%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.color'), key: 'color', width: '5%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.brand'), key: 'brand', width: '6%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.manufacturer'), key: 'mfrs', width: '6%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.ext1'), key: 'otherField1', width: '4%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.ext2'), key: 'otherField2', width: '4%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.ext3'), key: 'otherField3', width: '4%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.stock'), key: 'stock', width: '5%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.unit'), key: 'unit', width: '4%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.serialNumber'), key: 'snList', width: '12%', type: FormTypes.popupJsh, kind: 'sn', multi: true },
            { title: this.$t('purchase.form.columns.batchNumber'), key: 'batchNumber', width: '7%', type: FormTypes.popupJsh, kind: 'batch', multi: false },
            { title: this.$t('purchase.form.columns.expirationDate'), key: 'expirationDate',width: '7%', type: FormTypes.input, readonly: true },
            { title: this.$t('purchase.form.columns.sku'), key: 'sku', width: '9%', type: FormTypes.normal },
            { title: this.$t('purchase.form.preNumber'), key: 'preNumber', width: '5%', type: FormTypes.normal },
            { title: this.$t('purchase.form.finishReturn'), key: 'finishNumber', width: '5%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.quantity'), key: 'operNumber', width: '5%', type: FormTypes.inputNumber, statistics: true,
              validateRules: [{ required: true, message: '${title}不能为空' }]
            },
            { title: this.$t('purchase.form.columns.unitPrice'), key: 'unitPrice', width: '5%', type: FormTypes.inputNumber},
            { title: this.$t('purchase.form.columns.amount'), key: 'allPrice', width: '5%', type: FormTypes.inputNumber, statistics: true },
            { title: this.$t('purchase.form.columns.taxRate'), key: 'taxRate', width: '4%', type: FormTypes.inputNumber,placeholder: '%'},
            { title: this.$t('purchase.form.columns.taxAmount'), key: 'taxMoney', width: '5%', type: FormTypes.inputNumber, readonly: true, statistics: true },
            { title: this.$t('purchase.form.columns.taxTotal'), key: 'taxLastMoney', width: '7%', type: FormTypes.inputNumber, statistics: true },
            { title: this.$t('purchase.form.columns.remark'), key: 'remark', width: '6%', type: FormTypes.input },
            { title: '关联id', key: 'linkId', width: '5%', type: FormTypes.hidden },
            { title: '是否再次编辑', key: 'isEdit', width: '5%', type: FormTypes.hidden },
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
          },
          accountId:{
            rules: [
              { required: true, message: this.$t('purchase.validation.accountRequired') }
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
        const systemConfigReady = this.initSystemConfig().catch(() => null)
        const depotReady = this.initDepot().catch(() => null)
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
        if (this.action === 'add') {
          this.addInit(this.prefixNo)
          this.fileList = []
          this.$nextTick(() => {
            if(this.transferParam && this.transferParam.number) {
              let tp = this.transferParam
              Promise.all([systemConfigReady, depotReady]).then(() => {
                this.linkBillListOk(tp.list, tp.number, tp.organId, tp.discount, tp.deposit, tp.remark,
                  this.defaultDepotId, tp.accountId)
              })
            }
          })
        } else {
          if(this.model.linkNumber) {
            this.rowCanEdit = false
            this.materialTable.columns[1].type = FormTypes.normal
          }
          this.model.operTime = this.model.operTimeStr
          this.model.debt = (this.model.discountLastMoney + this.model.otherMoney - this.model.changeAmount).toFixed(2)
          if(this.model.accountId == null) {
            this.model.accountId = 0
            this.manyAccountBtnStatus = true
            this.accountIdList = this.model.accountIdList
            this.accountMoneyList = this.model.accountMoneyList
          } else {
            this.manyAccountBtnStatus = false
          }
          this.fileList = this.model.fileName
          this.$nextTick(() => {
            this.form.setFieldsValue(pick(this.model,'organId', 'operTime', 'number', 'linkNumber', 'remark',
              'discount','discountMoney','discountLastMoney','otherMoney','accountId','changeAmount','debt'))
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
        this.initSupplier(0)
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
        billMain.subType = '采购退货'
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
        billMain.status = this.billStatus
        return {
          info: JSON.stringify(billMain),
          rows: JSON.stringify(detailArr),
        }
      },
      onSearchLinkNumber() {
        this.$refs.linkBillList.show('入库', '采购', '供应商', "1,2,3")
        this.$refs.linkBillList.title = this.$t('purchase.selectLinkedInbound')
      },
      linkBillListOk(selectBillDetailRows, linkNumber, organId, discount, deposit, remark, depotId, accountId) {
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
            if(info.enableSerialNumber === '1') {
              //关联退货必须重新选择本次实际退回供应商的序列号。
              info.snList = ''
            }
            info.linkId = info.id
            allTaxLastMoney += info.taxLastMoney
            if(info.operNumber>0) {
              listEx.push(info)
              this.changeColumnShow(info)
            }
          }
          this.materialTable.dataSource = listEx
          if(listEx.length === 0) {
            this.$nextTick(() => {
              this.$message.warning(this.$t('purchase.validation.linkedBillCompleted'));
            })
          }
          ///给优惠后金额重新赋值
          allTaxLastMoney = allTaxLastMoney?allTaxLastMoney:0
          let discountMoney = 0
          if(allTaxLastMoney!==0) {
            discountMoney = (discount/100*allTaxLastMoney).toFixed(2)-0
          }
          let discountLastMoney = (allTaxLastMoney - discountMoney).toFixed(2)-0
          this.$nextTick(() => {
            this.form.setFieldsValue({
              'organId': organId,
              'linkNumber': linkNumber,
              'discount': discount,
              'discountMoney': discountMoney,
              'discountLastMoney': discountLastMoney,
              'changeAmount': discountLastMoney,
              'accountId': accountId,
              'remark': remark
            })
            findBySelectSup({organId: organId, limit:1}).then((res)=> {
              this.supList = res && Array.isArray(res) ? res : [];
            })
            if(this.zeroChangeAmountFlag) {
              //切换收付款的金额为0
              let oldChangeAmount = this.form.getFieldValue('changeAmount')-0
              this.form.setFieldsValue({'changeAmount':0, 'debt':oldChangeAmount})
            }
            let targetDepotId = depotId || this.defaultDepotId
            if(targetDepotId) {
              this.batchSetDepotModalFormOk(targetDepotId, listEx)
            }
          })
        }
      },
    }
  }
</script>
<style scoped>

</style>
