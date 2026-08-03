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
      <a-button v-if="billPrintFlag && isShowPrintBtn" @click="handlePrintPro($t('purchase.purchaseApply'))">{{ $t('common.printNew') }}</a-button>
      <a-button v-if="billPrintFlag && isShowPrintBtn" @click="handlePrint($t('purchase.purchaseApply'))">{{ $t('common.print') }}</a-button>
      <a-button v-if="checkFlag && isCanCheck" :loading="confirmLoading" @click="handleOkAndCheck">{{ $t('common.saveAndApprove') }}</a-button>
      <a-button type="primary" :loading="confirmLoading" @click="handleOkOnly">{{ $t('common.save') }}（Ctrl+S）</a-button>
      <!--发起多级审核-->
      <a-button v-if="!checkFlag" @click="handleWorkflow()" type="primary">{{ $t('common.submitWorkflow') }}</a-button>
    </template>
    <a-spin :spinning="confirmLoading">
      <a-form :form="form">
        <a-row class="form-row" :gutter="24">
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.documentDate')">
              <j-date v-decorator="['operTime', validatorRules.operTime]" :show-time="true"/>
            </a-form-item>
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.documentNumber')" data-step="2" :data-title="$t('common.billNo')"
              :data-intro="$t('guide.autoNumber')">
              <a-input :placeholder="$t('purchase.form.documentNumber')" v-decorator.trim="[ 'number', validatorRules.number ]" />
            </a-form-item>
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
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
          :rowSelection="rowCanEdit"
          :actionButton="rowCanEdit"
          :dragSortAndNumber="rowCanEdit"
          @valueChange="onValueChange"
          @added="onAdded"
          @deleted="onDeleted">
          <template #buttonAfter>
            <a-row v-if="rowCanEdit" :gutter="24" style="float:left;padding-bottom: 5px;" data-step="4" :data-title="$t('common.scanEntry')" :data-intro="$t('common.scanEntry')">
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
                <a-button style="margin-left: 8px" @click="handleHistoryBillList"><a-icon type="history" />{{ $t('common.historyBill') }}</a-button>
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
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.attachment')" data-step="10" :data-title="$t('common.attachment')" :data-intro="$t('guide.attachment')">
              <j-upload v-model="fileList" bizPath="bill"></j-upload>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-spin>
    <import-item-modal ref="importItemModalForm" @ok="importItemModalFormOk"></import-item-modal>
    <history-bill-list ref="historyBillListModalForm"></history-bill-list>
    <workflow-iframe ref="modalWorkflow" @ok="workflowModalFormOk"></workflow-iframe>
    <bill-print-iframe ref="modalPrint"></bill-print-iframe>
    <bill-print-pro-iframe ref="modalPrintPro"></bill-print-pro-iframe>
  </j-modal>
</template>
<script>
  import pick from 'lodash.pick'
  import ImportItemModal from '../dialog/ImportItemModal'
  import HistoryBillList from '../dialog/HistoryBillList'
  import WorkflowIframe from '@/components/tools/WorkflowIframe'
  import BillPrintIframe from '../dialog/BillPrintIframe'
  import BillPrintProIframe from '../dialog/BillPrintProIframe'
  import { FormTypes } from '@/utils/JEditableTableUtil'
  import { JEditableTableMixin } from '@/mixins/JEditableTableMixin'
  import { BillModalMixin } from '../mixins/BillModalMixin'
  import { getMpListShort, changeListFmtMinus,handleIntroJs } from "@/utils/util"
  import JUpload from '@/components/jeecg/JUpload'
  import JDate from '@/components/jeecg/JDate'
  import Vue from 'vue'
  export default {
    name: "PurchaseApplyModal",
    mixins: [JEditableTableMixin,BillModalMixin],
    components: {
      ImportItemModal,
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
        title: this.$t('common.action'),
        width: '1600px',
        moreStatus: false,
        // 新增时子表默认添加几行空数据
        addDefaultRowNum: 1,
        visible: false,
        supList: [],
        depotList: [],
        operTimeStr: '',
        prefixNo: 'QGD',
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
            { title: this.$t('purchase.form.columns.barcode'), key: 'barCode', width: '12%', type: FormTypes.popupJsh, kind: 'material', multi: true,
              validateRules: [{ required: true, message: this.$t('purchase.validation.barcodeRequired') }]
            },
            { title: this.$t('purchase.form.columns.name'), key: 'name', width: '10%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.specification'), key: 'standard', width: '9%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.model'), key: 'model', width: '9%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.color'), key: 'color', width: '6%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.brand'), key: 'brand', width: '6%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.manufacturer'), key: 'mfrs', width: '6%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.ext1'), key: 'otherField1', width: '4%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.ext2'), key: 'otherField2', width: '4%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.ext3'), key: 'otherField3', width: '4%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.unit'), key: 'unit', width: '6%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.sku'), key: 'sku', width: '10%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.quantity'), key: 'operNumber', width: '6%', type: FormTypes.inputNumber, statistics: true,
              validateRules: [
                { required: true, message: this.$t('purchase.validation.quantityRequired') },
                { pattern: /^(?=.*[1-9])\d+(?:\.\d+)?$/, message: this.$t('purchase.validation.quantityMustBePositive') }
              ]
            },
            { title: this.$t('purchase.form.columns.remark'), key: 'remark', width: '8%', type: FormTypes.input},
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
        this.changeColumnHide()
        if (this.action === 'add') {
          this.addInit(this.prefixNo)
          this.fileList = []
          this.$nextTick(() => {
            handleIntroJs(this.prefixNo, 1)
          })
        } else {
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
            this.form.setFieldsValue(pick(this.model, 'operTime', 'number', 'remark'))
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
        this.initPlatform()
        this.initQuickBtn()
        this.handleChangeOtherField()
      },
      /** 整理成formData */
      classifyIntoFormData(allValues) {
        let billMain = Object.assign(this.model, allValues.formValue)
        let detailArr = allValues.tablesValue[0].values
        billMain.type = '其它'
        billMain.subType = '请购单'
        for(let item of detailArr){
          item.unitPrice = 0
          item.taxUnitPrice = 0
          item.allPrice = 0
          item.taxRate = 0
          item.taxMoney = 0
          item.taxLastMoney = 0
        }
        billMain.totalPrice = 0
        billMain.changeAmount = 0
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
        this.$refs.historyBillListModalForm.show('其它', '请购单', '', organId);
        this.$refs.historyBillListModalForm.disableSubmit = false;
      },
    }
  }
</script>
<style scoped>

</style>
