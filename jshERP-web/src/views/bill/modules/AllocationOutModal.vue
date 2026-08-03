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
      <a-button v-if="billPrintFlag && isShowPrintBtn" @click="handlePrintPro('调拨出库')">{{ $t('common.printNew') }}</a-button>
      <a-button v-if="billPrintFlag && isShowPrintBtn" @click="handlePrint('调拨出库')">{{ $t('common.print') }}</a-button>
      <a-button v-if="checkFlag && isCanCheck" :loading="confirmLoading" @click="handleOkAndCheck">{{ $t('common.saveAndApprove') }}</a-button>
      <a-button type="primary" :loading="confirmLoading" @click="handleOkOnly">{{ $t('common.save') }}</a-button>
      <!--发起多级审核-->
      <a-button v-if="!checkFlag" @click="handleWorkflow()" type="primary">{{ $t('common.submitWorkflow') }}</a-button>
    </template>
    <a-spin :spinning="confirmLoading">
      <a-form :form="form">
        <a-row class="form-row" :gutter="24">
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billDate')">
              <j-date v-decorator="['operTime', validatorRules.operTime]" :show-time="true"/>
            </a-form-item>
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billNo')">
              <a-input :placeholder="$t('common.enterBillNo')" v-decorator.trim="[ 'number', validatorRules.number ]" />
            </a-form-item>
          </a-col>
          <a-col :lg="6" :md="12" :sm="24"></a-col>
          <a-col :lg="6" :md="12" :sm="24"></a-col>
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
          :actionButton="true"
          :dragSortAndNumber="true"
          @valueChange="onValueChange"
          @added="onAdded"
          @deleted="onDeleted">
          <template #buttonAfter>
            <a-row :gutter="24" style="float:left;" data-step="4" :data-title="$t('common.scanEntry')" :data-intro="$t('common.scanEntry')">
              <a-col v-if="scanStatus" :md="6" :sm="24">
                <a-button @click="scanEnter">{{ $t('common.scanEntry') }}</a-button>
              </a-col>
              <a-col v-if="!scanStatus" :md="16" :sm="24" style="padding: 0 6px 0 12px">
                <a-input :placeholder="$t('common.scanBarcodePlaceholder')" v-model="scanBarCode" @pressEnter="scanPressEnter" ref="scanBarCode"/>
              </a-col>
              <a-col v-if="!scanStatus" :md="6" :sm="24" style="padding: 0px">
                <a-button @click="stopScan">{{ $t('common.hideScan') }}</a-button>
              </a-col>
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
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.attachment')">
              <j-upload v-model="fileList" bizPath="bill"></j-upload>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-spin>
    <depot-modal ref="depotModalForm" @ok="depotModalFormOk"></depot-modal>
    <batch-set-depot ref="batchSetDepotModalForm" @ok="batchSetDepotModalFormOk"></batch-set-depot>
    <workflow-iframe ref="modalWorkflow" @ok="workflowModalFormOk"></workflow-iframe>
    <bill-print-iframe ref="modalPrint"></bill-print-iframe>
    <bill-print-pro-iframe ref="modalPrintPro"></bill-print-pro-iframe>
  </j-modal>
</template>
<script>
  import pick from 'lodash.pick'
  import DepotModal from '../../system/modules/DepotModal'
  import BatchSetDepot from '../dialog/BatchSetDepot'
  import WorkflowIframe from '@/components/tools/WorkflowIframe'
  import BillPrintIframe from '../dialog/BillPrintIframe'
  import BillPrintProIframe from '../dialog/BillPrintProIframe'
  import { FormTypes } from '@/utils/JEditableTableUtil'
  import { JEditableTableMixin } from '@/mixins/JEditableTableMixin'
  import { BillModalMixin } from '../mixins/BillModalMixin'
  import { getMpListShort } from "@/utils/util"
  import JUpload from '@/components/jeecg/JUpload'
  import JDate from '@/components/jeecg/JDate'
  import Vue from 'vue'
  export default {
    name: "AllocationOutModal",
    mixins: [JEditableTableMixin, BillModalMixin],
    components: {
      DepotModal,
      BatchSetDepot,
      WorkflowIframe,
      BillPrintIframe,
      BillPrintProIframe,
      JUpload,
      JDate
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
        prefixNo: 'DBCK',
        fileList:[],
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
            { title: this.$t('common.inboundDepot'), key: 'anotherDepotId', width: '8%', type: FormTypes.select, placeholder: '请选择${title}', options: [], allowSearch:true,
              validateRules: [{ required: true, message: '${title}不能为空' }]
            },
            { title: this.$t('common.unit'), key: 'unit', width: '4%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.sku'), key: 'sku', width: '9%', type: FormTypes.normal },
            { title: this.$t('common.quantity'), key: 'operNumber', width: '5%', type: FormTypes.inputNumber, statistics: true,
              validateRules: [{ required: true, message: '${title}不能为空' }]
            },
            { title: this.$t('purchase.form.columns.unitPrice'), key: 'unitPrice', width: '5%', type: FormTypes.inputNumber},
            { title: this.$t('common.amount'), key: 'allPrice', width: '5%', type: FormTypes.inputNumber, statistics: true },
            { title: this.$t('common.remark'), key: 'remark', width: '5%', type: FormTypes.input }
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
          type:{
            rules: [
              { required: true, message: '请选择类型!' }
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
        this.changeColumnHide()
        this.changeFormTypes(this.materialTable.columns, 'snList', 0)
        this.changeFormTypes(this.materialTable.columns, 'batchNumber', 0)
        this.changeFormTypes(this.materialTable.columns, 'expirationDate', 0)
        if (this.action === 'add') {
          this.addInit(this.prefixNo)
          this.fileList = []
        } else {
          this.model.operTime = this.model.operTimeStr
          this.fileList = this.model.fileName
          this.$nextTick(() => {
            this.form.setFieldsValue(pick(this.model,'organId', 'operTime', 'number', 'remark',
              'discount','discountMoney','discountLastMoney','otherMoney','accountId','changeAmount'))
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
        this.initDepot()
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
        billMain.subType = '调拨'
        for(let item of detailArr){
          totalPrice += item.allPrice-0
        }
        billMain.totalPrice = totalPrice
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
    }
  }
</script>
<style scoped>

</style>
