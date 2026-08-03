<template>
  <j-modal
    :title="title"
    :width="width"
    :visible="visible"
    :confirmLoading="confirmLoading"
    :keyboard="false"
    :forceRender="true"
    fullscreen
    switchFullscreen
    @cancel="handleCancel"
    :id="prefixNo"
    style="top:20px;height: 95%;">
    <template slot="footer">
      <a-button @click="handleCancel">{{ $t('common.cancel') }}</a-button>
      <a-button v-if="checkFlag && isCanCheck" :loading="confirmLoading" @click="handleOkAndCheck">{{ $t('common.saveAndApprove') }}</a-button>
      <a-button type="primary" :loading="confirmLoading" @click="handleOkOnly">{{ $t('common.save') }}</a-button>
      <!--发起多级审核-->
      <a-button v-if="!checkFlag" @click="handleWorkflow()" type="primary">{{ $t('common.submitWorkflow') }}</a-button>
    </template>
    <a-spin :spinning="confirmLoading">
      <a-form :form="form">
        <a-row class="form-row" :gutter="24">
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.customer')">
              <a-select :placeholder="$t('common.selectCustomer')" v-decorator="[ 'organId', validatorRules.organId ]"
                :dropdownMatchSelectWidth="false" showSearch optionFilterProp="children" @change="onChangeOrgan" @search="handleSearchCustomer">
                <div slot="dropdownRender" slot-scope="menu">
                  <v-nodes :vnodes="menu" />
                  <a-divider style="margin: 4px 0;" />
                  <div v-if="quickBtn.customer" class="dropdown-btn" @mousedown="e => e.preventDefault()" @click="addCustomer"><a-icon type="plus" /> {{ $t('sales.addCustomer') }}</div>
                  <div class="dropdown-btn" @mousedown="e => e.preventDefault()" @click="initCustomer"><a-icon type="reload" /> {{ $t('common.refresh') }}</div>
                </div>
                <a-select-option v-for="(item,index) in cusList" :key="index" :value="item.id">
                  {{ item.supplier }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billDate')">
              <j-date v-decorator="['billTime', validatorRules.billTime]" :show-time="true"/>
            </a-form-item>
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billNo')">
              <a-input :placeholder="$t('common.enterBillNo')" v-decorator.trim="[ 'billNo', validatorRules.billNo ]" />
            </a-form-item>
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.operator')">
              <a-select :placeholder="$t('common.selectOperator')" v-decorator="[ 'handsPersonId' ]"
                        :dropdownMatchSelectWidth="false" showSearch optionFilterProp="children">
                <div slot="dropdownRender" slot-scope="menu">
                  <v-nodes :vnodes="menu" />
                  <a-divider style="margin: 4px 0;" />
                  <div v-if="quickBtn.person" class="dropdown-btn" @mousedown="e => e.preventDefault()" @click="addPerson"><a-icon type="plus" /> {{ $t('common.add') }}{{ $t('common.operator') }}</div>
                  <div class="dropdown-btn" @mousedown="e => e.preventDefault()" @click="initPerson"><a-icon type="reload" /> {{ $t('common.refresh') }}</div>
                </div>
                <a-select-option v-for="(item,index) in personList" :key="index" :value="item.id">
                  {{ item.name }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <j-editable-table
          :ref="refKeys[0]"
          :loading="accountTable.loading"
          :columns="accountTable.columns"
          :dataSource="accountTable.dataSource"
          :minWidth="minWidth"
          :maxHeight="300"
          :rowNumber="true"
          :rowSelection="true"
          :actionButton="false"
          :actionDeleteButton="true"
          @deleted="onDeleted"
          @valueChange="onValueChange">
          <template #buttonBefore>
            <a-row :gutter="24" style="float:left;padding-bottom:8px;">
              <a-col :md="12" :sm="24">
                <a-button type="primary" icon="plus" @click="handleClickAdd">{{ $t('financial.selectBill') }}</a-button>
              </a-col>
              <a-col :md="12" :sm="24" style="padding-left:0">
                <a-button type="primary" icon="plus" @click="selectBeginNeed('客户')">{{ $t('financial.selectInitial') }}</a-button>
              </a-col>
            </a-row>
          </template>
          <template #buttonAfter>
            <a-row :gutter="24" style="float:left;padding-bottom:8px;">
              <a-col :md="12" :sm="24">
                <a-button icon="link" @click="handleWaitNeed('客户')">{{ $t('financial.pendingReceipt') }}</a-button>
              </a-col>
            </a-row>
          </template>
        </j-editable-table>
        <a-row class="form-row" :gutter="24">
          <a-col :lg="24" :md="24" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="{xs: { span: 24 },sm: { span: 24 }}" label="">
              <a-textarea :rows="2" :placeholder="$t('common.enterRemark')" v-decorator="[ 'remark' ]" style="margin-top:8px;"/>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row class="form-row" :gutter="24">
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('financial.form.account')">
              <a-select :placeholder="$t('retail.selectReceivingAccount')" v-decorator="[ 'accountId', validatorRules.accountId ]"
                :dropdownMatchSelectWidth="false" showSearch optionFilterProp="children">
                <div slot="dropdownRender" slot-scope="menu">
                  <v-nodes :vnodes="menu" />
                  <a-divider style="margin: 4px 0;" />
                  <div v-if="quickBtn.account" class="dropdown-btn" @mousedown="e => e.preventDefault()" @click="addAccount"><a-icon type="plus" /> {{ $t('common.add') }}</div>
                  <div class="dropdown-btn" @mousedown="e => e.preventDefault()" @click="initAccount"><a-icon type="reload" /> {{ $t('common.refresh') }}</div>
                </div>
                <a-select-option v-for="(item,index) in accountList" :key="index" :value="item.id">
                  {{ item.name }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('financial.totalReceipt')">
              <a-input :placeholder="$t('financial.form.enterTotalReceipt')" v-decorator.trim="[ 'totalPrice' ]" :readOnly="true"/>
            </a-form-item>
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('financial.discountAmount')">
              <a-input :placeholder="$t('financial.form.enterDiscountAmount')" v-decorator.trim="[ 'discountMoney', validatorRules.discountMoney ]" @change="onChangeDiscountMoney" />
            </a-form-item>
          </a-col>
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('financial.actualReceipt')">
              <a-input :placeholder="$t('financial.form.enterActualReceipt')" v-decorator.trim="[ 'changeAmount' ]" :readOnly="true"/>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row class="form-row" :gutter="24">
          <a-col :lg="6" :md="12" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.attachment')">
              <j-upload v-model="fileList" bizPath="financial"></j-upload>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-spin>
    <debt-bill-list ref="debtBillList" @ok="debtBillListOk"></debt-bill-list>
    <customer-modal ref="customerModalForm" @ok="customerModalFormOk"></customer-modal>
    <account-modal ref="accountModalForm" @ok="accountModalFormOk"></account-modal>
    <person-modal ref="personModalForm" @ok="personModalFormOk"></person-modal>
    <wait-need-list ref="waitNeedList" @ok="waitNeedListOk"></wait-need-list>
    <workflow-iframe ref="modalWorkflow" @ok="workflowModalFormOk"></workflow-iframe>
  </j-modal>
</template>
<script>
  import pick from 'lodash.pick'
  import DebtBillList from '../dialog/DebtBillList'
  import CustomerModal from '../../system/modules/CustomerModal'
  import AccountModal from '../../system/modules/AccountModal'
  import PersonModal from '../../system/modules/PersonModal'
  import WaitNeedList from '../dialog/WaitNeedList'
  import WorkflowIframe from '@/components/tools/WorkflowIframe'
  import { FormTypes } from '@/utils/JEditableTableUtil'
  import { JEditableTableMixin } from '@/mixins/JEditableTableMixin'
  import { FinancialModalMixin } from '../mixins/FinancialModalMixin'
  import JUpload from '@/components/jeecg/JUpload'
  import JDate from '@/components/jeecg/JDate'
  export default {
    name: "MoneyInModal",
    mixins: [JEditableTableMixin, FinancialModalMixin],
    components: {
      DebtBillList,
      CustomerModal,
      AccountModal,
      PersonModal,
      WaitNeedList,
      WorkflowIframe,
      JUpload,
      JDate,
      VNodes: {
        functional: true,
        render: (h, ctx) => ctx.props.vnodes,
      }
    },
    data () {
      return {
        title:this.$t('common.action'),
        width: '1600px',
        moreStatus: false,
        // 新增时子表默认添加几行空数据
        addDefaultRowNum: 0,
        visible: false,
        prefixNo: 'SK',
        model: {},
        fileList:[],
        labelCol: {
          xs: { span: 24 },
          sm: { span: 8 },
        },
        wrapperCol: {
          xs: { span: 24 },
          sm: { span: 16 },
        },
        refKeys: ['accountDataTable', ],
        activeKey: 'accountDataTable',
        accountTable: {
          loading: false,
          dataSource: [],
          columns: [
            { title: this.$t('financial.salesBillNo'),key: 'billNumber',width: '20%', type: FormTypes.input, readonly: true },
            { title: this.$t('financial.receivableDebt'),key: 'needDebt', width: '10%', type: FormTypes.inputNumber, statistics: true, readonly: true },
            { title: this.$t('financial.receivedDebt'), key: 'finishDebt', width: '10%', type: FormTypes.inputNumber, statistics: true, readonly: true },
            { title: this.$t('financial.thisReceipt'),key: 'eachAmount', width: '10%', type: FormTypes.inputNumber, statistics: true, placeholder: this.$t('common.pleaseEnter') + '${title}',
              validateRules: [
                { required: true, message: this.$t('financial.validation.fieldCannotBeEmpty') },
                { pattern: /^(?=.*[1-9])\d+(?:\.\d+)?$/, message: this.$t('financial.validation.fieldMustBePositive') }
              ]
            },
            { title: this.$t('common.remark'),key: 'remark', width: '20%', type: FormTypes.input, placeholder: this.$t('common.pleaseEnter') + '${title}'}
          ]
        },
        confirmLoading: false,
        validatorRules:{
          organId:{
            rules: [{ required: true, message: this.$t('sales.validation.customerRequired') }]
          },
          billTime:{
            rules: [{ required: true, message: this.$t('financial.form.selectBillDate') }]
          },
          billNo:{
            rules: [{ required: true, message: this.$t('purchase.validation.documentNumberRequired') }]
          },
          accountId:{
            rules: [{ required: true, message: this.$t('financial.form.selectReceiptAccount') }]
          },
          discountMoney:{
            rules: [
              { required: true, message: this.$t('financial.form.enterDiscount') },
              { pattern: /^(?:0|[1-9]\d*)(?:\.\d+)?$/, message: this.$t('financial.form.discountNotNegative') }
            ]
          }
        },
        url: {
          add: '/accountHead/addAccountHeadAndDetail',
          edit: '/accountHead/updateAccountHeadAndDetail',
          detailList: '/accountItem/getDetailList'
        }
      }
    },
    created () {
    },
    methods: {
      //调用完edit()方法之后会自动调用此方法
      editAfter() {
        this.billStatus = '0'
        if (this.action === 'add') {
          this.addInit(this.prefixNo)
          this.fileList = []
          if(this.actionWithOrgan) {
            //自动弹出待收款客户列表
            let that = this
            setTimeout(function() {
              that.$refs.waitNeedList.show('客户')
            },1000)
          }
        } else {
          this.model.billTime = this.model.billTimeStr
          this.$nextTick(() => {
            this.form.setFieldsValue(pick(this.model,'organId', 'handsPersonId', 'billTime', 'billNo', 'remark',
                  'accountId', 'totalPrice', 'discountMoney', 'changeAmount'))
          });
          this.fileList = this.model.fileName
          // 加载子表数据
          let params = {
            headerId: this.model.id
          }
          let url = this.readOnly ? this.url.detailList : this.url.detailList;
          this.requestSubTableData(url, params, this.accountTable);
        }
        this.initSystemConfig()
        this.initCustomer()
        this.initPerson()
        this.initAccount()
        this.initQuickBtn()
      },
      //提交单据时整理成formData
      classifyIntoFormData(allValues) {
        let totalPrice = 0
        let billMain = Object.assign(this.model, allValues.formValue)
        let detailArr = allValues.tablesValue[0].values
        billMain.type = '收款'
        for(let item of detailArr){
          totalPrice += item.eachAmount-0
        }
        billMain.totalPrice = totalPrice
        if(this.fileList && this.fileList.length > 0) {
          billMain.fileName = this.fileList
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
      handleClickAdd() {
        let organId = this.form.getFieldValue('organId')
        if(organId){
          this.$refs.debtBillList.show(organId, '出库', '销售', '客户', "")
          this.$refs.debtBillList.title = this.$t('financial.selectSalesDebtBill')
        } else {
          this.$message.warning(this.$t('sales.validation.customerRequired'));
        }
      }
    }
  }
</script>
<style scoped>
  .action-button {
    margin-bottom: 8px;
  }
  .gap {
    padding-left: 8px;
  }
</style>
