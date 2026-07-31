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
      <a-button v-if="billPrintFlag && isShowPrintBtn" @click="handlePrintPro('零售出库')">{{ $t('common.printNew') }}</a-button>
      <a-button v-if="billPrintFlag && isShowPrintBtn" @click="handlePrint('零售出库')">{{ $t('common.print') }}</a-button>
      <a-button v-if="checkFlag && isCanCheck" :loading="confirmLoading" @click="handleOkAndCheck">{{ $t('common.saveAndApprove') }}</a-button>
      <a-button type="primary" :loading="confirmLoading" @click="handleOkOnly">{{ $t('common.save') }}（Ctrl+S）</a-button>
      <!--发起多级审核-->
      <a-button v-if="!checkFlag" @click="handleWorkflow()" type="primary">{{ $t('common.submitWorkflow') }}</a-button>
    </template>
    <a-spin :spinning="confirmLoading">
      <a-form :form="form">
        <a-row class="form-row" :gutter="24">
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('retail.memberCard')" data-step="1" :data-title="$t('retail.memberCard')"
                         :data-intro="$t('guide.memberCard')">
              <a-select :placeholder="$t('retail.selectMemberCard')" v-decorator="[ 'organId' ]"
                :dropdownMatchSelectWidth="false" showSearch optionFilterProp="children" @change="onChangeOrgan" @search="handleSearchRetail">
                <div slot="dropdownRender" slot-scope="menu">
                  <v-nodes :vnodes="menu" />
                  <a-divider style="margin: 4px 0;" />
                  <div v-if="quickBtn.member" class="dropdown-btn" @mousedown="e => e.preventDefault()" @click="addMember"><a-icon type="plus" /> {{ $t('retail.addMember') }}</div>
                  <div class="dropdown-btn" @mousedown="e => e.preventDefault()" @click="initRetail(0)"><a-icon type="reload" /> {{ $t('retail.refreshList') }}</div>
                </div>
                <a-select-option v-for="(item,index) in retailList" :key="index" :value="item.id">
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
                         :data-intro="$t('guide.autoNumber')">
              <a-input :placeholder="$t('purchase.form.documentNumber')" v-decorator.trim="[ 'number', validatorRules.number ]" />
            </a-form-item>
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('retail.paymentType')" data-step="3" :data-title="$t('retail.paymentType')"
                         :data-intro="$t('guide.paymentType')">
              <a-select :placeholder="$t('retail.selectAccount')" v-decorator="[ 'payType' ]" :dropdownMatchSelectWidth="false">
                <a-select-option v-for="(item,index) in payTypeList" :key="index" :value="item.value">
                  {{ item.text }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row class="form-row" :gutter="24">
          <a-col :lg="18" :md="12" :sm="24">
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
                    <a-input :placeholder="$t('common.scanSnPlaceholder')" v-model="scanBarCode" @pressEnter="scanPressEnter" ref="scanBarCode"/>
                  </a-col>
                  <a-col v-if="!scanStatus" :md="6" :sm="24" style="padding: 0px 18px 0 0">
                    <a-button @click="stopScan">{{ $t('common.hideScan') }}</a-button>
                  </a-col>
                </a-row>
              </template>
              <template #depotBatchSet>
                <a-icon type="down" @click="handleBatchSetDepot" />
              </template>
              <template #depotAdd>
                <a-divider v-if="quickBtn.depot" style="margin: 4px 0;" />
                <div v-if="quickBtn.depot" class="dropdown-btn" @click="addDepot"><a-icon type="plus" /> {{ $t('common.addNew') }}</div>
                <div class="dropdown-btn" @mousedown="e => e.preventDefault()" @click="initDepot"><a-icon type="reload" /> {{ $t('common.refresh') }}</div>
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
                <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.attachment')" data-step="9" :data-title="$t('common.attachment')"
                             :data-intro="$t('guide.attachment')">
                  <j-upload v-model="fileList" bizPath="bill"></j-upload>
                </a-form-item>
              </a-col>
            </a-row>
          </a-col>
          <div class="sign">
            <a-col :lg="6" :md="12" :sm="24">
              <a-row class="form-row" :gutter="24">
                <a-col :lg="24" :md="6" :sm="6"><br/><br/></a-col>
                <a-col :lg="24" :md="6" :sm="6">
                  <a-form-item :labelCol="signLabelCol" :wrapperCol="signWrapperCol" data-step="5" :data-title="$t('retail.billAmount')"
                               :data-intro="$t('guide.billAmount')">
                    <span slot="label" style="font-size: 20px;line-height:20px">{{ $t('retail.billAmount') }}</span>
                    <a-input v-decorator.trim="[ 'changeAmount' ]" :style="{color:'purple'}" :readOnly="true"/>
                  </a-form-item>
                </a-col>
                <a-col :lg="24" :md="6" :sm="6">
                  <a-form-item :labelCol="signLabelCol" :wrapperCol="signWrapperCol" data-step="6" :data-title="$t('retail.getAmount')"
                               :data-intro="$t('guide.receivedAmount')">
                    <span slot="label" style="font-size: 20px;line-height:20px">{{ $t('retail.getAmount') }}</span>
                    <a-input v-decorator.trim="[ 'getAmount', {rules: [
                      { required: true, message: $t('retail.getAmountPlaceholder') },
                      { validator: validateGetAmount }
                    ]} ]" :style="{color:'red'}" defaultValue="0" @change="onChangeGetAmount"/>
                  </a-form-item>
                </a-col>
                <a-col :lg="24" :md="6" :sm="6">
                  <a-form-item :labelCol="signLabelCol" :wrapperCol="signWrapperCol" data-step="7" :data-title="$t('retail.backAmount')"
                               :data-intro="$t('guide.changeAmount')">
                    <span slot="label" style="font-size: 20px;line-height:20px">{{ $t('retail.backAmount') }}</span>
                    <a-input v-decorator.trim="[ 'backAmount' ]" :style="{color:'green'}" :readOnly="true" defaultValue="0"/>
                  </a-form-item>
                </a-col>
                <a-col :lg="24" :md="6" :sm="6">
                  <a-form-item :labelCol="signLabelCol" :wrapperCol="signWrapperCol" data-step="8" :data-title="$t('retail.receivingAccountLabel')"
                               :data-intro="$t('guide.receivingAccount')">
                    <span slot="label" style="font-size: 20px;line-height:20px">{{ $t('retail.receivingAccountLabel') }}</span>
                    <a-select :placeholder="$t('retail.selectReceivingAccount')" style="font-size:20px;" v-decorator="[ 'accountId', validatorRules.accountId ]" :dropdownMatchSelectWidth="false">
                      <div slot="dropdownRender" slot-scope="menu">
                        <v-nodes :vnodes="menu" />
                        <a-divider style="margin: 4px 0;" />
                        <div v-if="quickBtn.account" class="dropdown-btn" @mousedown="e => e.preventDefault()" @click="addAccount"><a-icon type="plus" /> {{ $t('common.addNew') }}</div>
                        <div class="dropdown-btn" @mousedown="e => e.preventDefault()" @click="initAccount(0)"><a-icon type="reload" /> {{ $t('common.refresh') }}</div>
                      </div>
                      <a-select-option v-for="(item,index) in accountList" :key="index" :value="item.id">
                        {{ item.name }}
                      </a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
              </a-row>
            </a-col>
          </div>
        </a-row>
      </a-form>
    </a-spin>
    <member-modal ref="memberModalForm" @ok="memberModalFormOk"></member-modal>
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
  import MemberModal from '../../system/modules/MemberModal'
  import DepotModal from '../../system/modules/DepotModal'
  import AccountModal from '../../system/modules/AccountModal'
  import BatchSetDepot from '../dialog/BatchSetDepot'
  import WorkflowIframe from '@/components/tools/WorkflowIframe'
  import BillPrintIframe from '../dialog/BillPrintIframe'
  import BillPrintProIframe from '../dialog/BillPrintProIframe'
  import { FormTypes } from '@/utils/JEditableTableUtil'
  import { JEditableTableMixin } from '@/mixins/JEditableTableMixin'
  import { BillModalMixin } from '../mixins/BillModalMixin'
  import { getMpListShort,handleIntroJs } from "@/utils/util"
  import { getAccount } from '@/api/api'
  import { getAction } from '@/api/manage'
  import JUpload from '@/components/jeecg/JUpload'
  import JDate from '@/components/jeecg/JDate'
  import Vue from 'vue'
  export default {
    name: "RetailOutModal",
    mixins: [JEditableTableMixin, BillModalMixin],
    components: {
      MemberModal,
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
        title: this.$t('common.save'),
        width: '1600px',
        moreStatus: false,
        // 新增时子表默认添加几行空数据
        addDefaultRowNum: 1,
        visible: false,
        operTimeStr: '',
        prefixNo: 'LSCK',
        fileList:[],
        payTypeList: [],
        minWidth: 1100,
        model: {},
        labelCol: {
          xs: { span: 24 },
          sm: { span: 8 },
        },
        wrapperCol: {
          xs: { span: 24 },
          sm: { span: 16 },
        },
        signLabelCol: { xs: { span: 24 }, sm: { span: 12 } },
        signWrapperCol: { xs: { span: 24 }, sm: { span: 12 } },
        refKeys: ['materialDataTable', ],
        activeKey: 'materialDataTable',
        materialTable: {
          loading: false,
          dataSource: [],
          columns: [
            { title: this.$t('purchase.form.warehouse'), key: 'depotId', width: '10%', type: FormTypes.select, placeholder: '请选择${title}', options: [],
              allowSearch:true, validateRules: [{ required: true, message: '${title}不能为空' }]
            },
            { title: this.$t('purchase.form.columns.barcode'), key: 'barCode', width: '16%', type: FormTypes.popupJsh, kind: 'material', multi: true,
              validateRules: [{ required: true, message: '${title}不能为空' }]
            },
            { title: this.$t('purchase.form.columns.name'), key: 'name', width: '12%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.specification'), key: 'standard', width: '10%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.model'), key: 'model', width: '10%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.color'), key: 'color', width: '5%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.brand'), key: 'brand', width: '6%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.manufacturer'), key: 'mfrs', width: '6%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.ext1'), key: 'otherField1', width: '4%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.ext2'), key: 'otherField2', width: '4%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.ext3'), key: 'otherField3', width: '4%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.stock'), key: 'stock', width: '5%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.unit'), key: 'unit', width: '5%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.serialNumber'), key: 'snList', width: '12%', type: FormTypes.popupJsh, kind: 'sn', multi: true },
            { title: this.$t('purchase.form.columns.batchNumber'), key: 'batchNumber', width: '8%', type: FormTypes.popupJsh, kind: 'batch', multi: false },
            { title: this.$t('purchase.form.columns.expirationDate'), key: 'expirationDate',width: '9%', type: FormTypes.input, readonly: true },
            { title: this.$t('purchase.form.columns.sku'), key: 'sku', width: '9%', type: FormTypes.normal },
            { title: this.$t('purchase.form.columns.quantity'), key: 'operNumber', width: '6%', type: FormTypes.inputNumber, statistics: true,
              validateRules: [
                { required: true, message: '${title}不能为空' },
                { pattern: /^(?=.*[1-9])\d+(?:\.\d+)?$/, message: '${title}必须大于0' }
              ]
            },
            { title: this.$t('purchase.form.columns.unitPrice'), key: 'unitPrice', width: '6%', type: FormTypes.inputNumber},
            { title: this.$t('purchase.form.columns.amount'), key: 'allPrice', width: '6%', type: FormTypes.inputNumber, statistics: true },
            { title: this.$t('purchase.form.columns.remark'), key: 'remark', width: '7%', type: FormTypes.input }
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
      this.initPayTypeList()
      let realScreenWidth = window.screen.width
      this.minWidth = realScreenWidth<1500?800:1100
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
          this.$nextTick(() => {
            handleIntroJs(this.prefixNo, 1)
          })
          this.$nextTick(() => {
            this.form.setFieldsValue({'payType': '现付', 'getAmount':0, 'backAmount':0})
          })
        } else {
          this.model.operTime = this.model.operTimeStr
          if(this.model.backAmount) {
            this.model.getAmount = (this.model.changeAmount + this.model.backAmount).toFixed(2)
          } else {
            this.model.getAmount = this.model.changeAmount
          }
          this.fileList = this.model.fileName
          if(this.model.payType === '预付款'){
            this.payTypeList = []
            this.payTypeList.push({"value":"预付款", "text":"预付款"})
            this.payTypeList.push({"value":"现付", "text":"现付"})
          }
          this.$nextTick(() => {
            this.form.setFieldsValue(pick(this.model,'organId', 'operTime', 'number', 'payType', 'remark',
              'discount','discountMoney','discountLastMoney','otherMoney','accountId','changeAmount','getAmount','backAmount'))
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
        this.initRetail(0)
        this.initDepot()
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
        billMain.subType = '零售'
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
      //加载收款类型
      initPayTypeList() {
        this.payTypeList.push({"value":"现付", "text":"现付"})
      },
      initAccount(isChecked){
        getAccount({}).then((res)=>{
          if(res && res.code === 200) {
            this.accountList = res.data.accountList
            if(isChecked && this.accountList.length>0) {
              this.form.setFieldsValue({'accountId': this.accountList[0].id})
            }
          }
        })
      },
      //选择会员的触发事件
      onChangeOrgan(value) {
        getAction("/supplier/memberAdvance", {id: value}).then(res=>{
          if(res && res.code === 200){
            this.payTypeList = []
            let info = res.data
            if(info.advanceIn) {
              this.payTypeList.push({"value":"预付款", "text":"预付款（" + info.advanceIn + "）"})
              this.payTypeList.push({"value":"现付", "text":"现付"})
              this.$nextTick(() => {
                this.form.setFieldsValue({'payType': '预付款'})
              })
            } else {
              this.payTypeList.push({"value":"现付", "text":"现付"})
              this.$nextTick(() => {
                this.form.setFieldsValue({'payType': '现付'})
              })
            }
          }
        })
      },
      //改变实收金额、收款金额的值
      autoChangePrice(target) {
        let allLastMoney = target.statisticsColumns.allPrice
        this.$nextTick(() => {
          this.form.setFieldsValue({'changeAmount':allLastMoney,'getAmount':allLastMoney,'backAmount':0})
        });
      },
      //改变收款金额
      onChangeGetAmount(e) {
        const value = e.target.value
        let changeAmount = this.form.getFieldValue('changeAmount')-0
        let backAmount = (value - changeAmount).toFixed(2)-0
        this.$nextTick(() => {
          this.form.setFieldsValue({'backAmount':backAmount})
        });
      },
      validateGetAmount(rule, value, callback) {
        const getAmount = Number(value)
        const changeAmount = Number(this.form.getFieldValue('changeAmount'))
        if (!Number.isFinite(getAmount) || getAmount < 0) {
          callback(this.$t('purchase.validation.getAmountMustBePositive'))
        } else if (Number.isFinite(changeAmount) && getAmount < changeAmount) {
          callback(this.$t('purchase.validation.retailNoDebt'))
        } else {
          callback()
        }
      }
    }
  }
</script>
<style scoped>
  .sign .ant-input{
    font-size: 30px;
    font-weight:bolder;
    text-align:center;
    border-left-width:0px!important;
    border-top-width:0px!important;
    border-right-width:0px!important;
  }
</style>
