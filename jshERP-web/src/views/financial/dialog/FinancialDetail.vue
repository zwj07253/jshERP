<template>
  <j-modal
    :title="title"
    :width="width"
    :visible="visible"
    :maskClosable="false"
    :forceRender="true"
    :style="modalStyle"
    fullscreen
    switchFullscreen
    @cancel="handleCancel"
    wrapClassName="ant-modal-cust-warp">
    <template slot="footer">
      <a-button key="back" @click="handleCancel">{{ $t('financial.cancelEsc') }}</a-button>
      <!--此处为解决缓存问题-->
      <a-button v-if="financialType === '收预付款'" v-print="'#advanceInPrint'">{{ $t('common.print') }}</a-button>
      <a-button v-if="financialType === '转账'" v-print="'#giroPrint'">{{ $t('common.print') }}</a-button>
      <a-button v-if="financialType === '收入'" v-print="'#itemInPrint'">{{ $t('common.print') }}</a-button>
      <a-button v-if="financialType === '支出'" v-print="'#itemOutPrint'">{{ $t('common.print') }}</a-button>
      <a-button v-if="financialType === '收款'" v-print="'#moneyInPrint'">{{ $t('common.print') }}</a-button>
      <a-button v-if="financialType === '付款'" v-print="'#moneyOutPrint'">{{ $t('common.print') }}</a-button>
      <!--反审核-->
      <a-button v-if="checkFlag && isCanBackCheck && model.status==='1'" @click="handleBackCheck()">{{ $t('common.unaudit') }}</a-button>
    </template>
    <a-form :form="form">
      <!--收预付款-->
      <template v-if="financialType === '收预付款'">
        <section ref="print" id="advanceInPrint">
          <a-row class="form-row" :gutter="24">
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('financial.form.payingMember')">
                <a-input v-decorator="['id']" hidden/>
                {{model.organName}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.operator')">
                {{model.handsPersonName}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billDate')">
                {{model.billTimeStr}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billNo')">
                {{model.billNo}}
              </a-form-item>
            </a-col>
          </a-row>
          <a-table
            ref="table"
            size="middle"
            bordered
            rowKey="id"
            :pagination="false"
            :loading="loading"
            :columns="advanceInColumns"
            :dataSource="dataSource">
          </a-table>
          <a-row class="form-row" :gutter="24">
            <a-col :lg="24" :md="24" :sm="24">
              <a-form-item :labelCol="labelCol" :wrapperCol="{xs: { span: 24 },sm: { span: 24 }}" label="" style="padding:20px 10px;">
                {{model.remark}}
              </a-form-item>
            </a-col>
          </a-row>
          <a-row class="form-row" :gutter="24">
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('financial.form.totalAmount')">
                {{model.totalPrice}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('financial.form.receiptAmount')">
                {{model.changeAmount}}
              </a-form-item>
            </a-col>
            <a-col :span="6"></a-col>
            <a-col :span="6"></a-col>
            <a-col :span="6"></a-col>
          </a-row>
        </section>
      </template>
      <!--转账-->
      <template v-if="financialType === '转账'">
        <section ref="print" id="giroPrint">
          <a-row class="form-row" :gutter="24">
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.operator')">
                <a-input v-decorator="['id']" hidden/>
                {{model.handsPersonName}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billDate')">
                {{model.billTimeStr}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billNo')">
                {{model.billNo}}
              </a-form-item>
            </a-col>
            <a-col :span="6"></a-col>
          </a-row>
          <a-table
            ref="table"
            size="middle"
            bordered
            rowKey="id"
            :pagination="false"
            :loading="loading"
            :columns="giroColumns"
            :dataSource="dataSource">
          </a-table>
          <a-row class="form-row" :gutter="24">
            <a-col :lg="24" :md="24" :sm="24">
              <a-form-item :labelCol="labelCol" :wrapperCol="{xs: { span: 24 },sm: { span: 24 }}" label="" style="padding:20px 10px;">
                {{model.remark}}
              </a-form-item>
            </a-col>
          </a-row>
          <a-row class="form-row" :gutter="24">
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('financial.form.transferOutAccount')">
                {{model.accountName}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('financial.form.transferAmount')">
                {{model.changeAmount}}
              </a-form-item>
            </a-col>
            <a-col :span="6"></a-col>
            <a-col :span="6"></a-col>
          </a-row>
        </section>
      </template>
      <!--收入-->
      <template v-if="financialType === '收入'">
        <section ref="print" id="itemInPrint">
          <a-row class="form-row" :gutter="24">
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.supplier')">
                <a-input v-decorator="['id']" hidden/>
                {{model.organName}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.operator')">
                {{model.handsPersonName}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billDate')">
                {{model.billTimeStr}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billNo')">
                {{model.billNo}}
              </a-form-item>
            </a-col>
          </a-row>
          <a-table
            ref="table"
            size="middle"
            bordered
            rowKey="id"
            :pagination="false"
            :loading="loading"
            :columns="itemInColumns"
            :dataSource="dataSource">
          </a-table>
          <a-row class="form-row" :gutter="24">
            <a-col :lg="24" :md="24" :sm="24">
              <a-form-item :labelCol="labelCol" :wrapperCol="{xs: { span: 24 },sm: { span: 24 }}" label="" style="padding:20px 10px;">
                {{model.remark}}
              </a-form-item>
            </a-col>
          </a-row>
          <a-row class="form-row" :gutter="24">
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('financial.form.incomeAccount')">
                {{model.accountName}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('financial.form.incomeAmount')">
                {{model.changeAmount}}
              </a-form-item>
            </a-col>
            <a-col :span="6"></a-col>
            <a-col :span="6"></a-col>
          </a-row>
        </section>
      </template>
      <!--支出-->
      <template v-if="financialType === '支出'">
        <section ref="print" id="itemOutPrint">
          <a-row class="form-row" :gutter="24">
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.supplier')">
                <a-input v-decorator="['id']" hidden/>
                {{model.organName}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.operator')">
                {{model.handsPersonName}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billDate')">
                {{model.billTimeStr}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billNo')">
                {{model.billNo}}
              </a-form-item>
            </a-col>
          </a-row>
          <a-table
            ref="table"
            size="middle"
            bordered
            rowKey="id"
            :pagination="false"
            :loading="loading"
            :columns="itemOutColumns"
            :dataSource="dataSource">
          </a-table>
          <a-row class="form-row" :gutter="24">
            <a-col :lg="24" :md="24" :sm="24">
              <a-form-item :labelCol="labelCol" :wrapperCol="{xs: { span: 24 },sm: { span: 24 }}" label="" style="padding:20px 10px;">
                {{model.remark}}
              </a-form-item>
            </a-col>
          </a-row>
          <a-row class="form-row" :gutter="24">
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('financial.form.expenseAccount')">
                {{model.accountName}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('financial.form.expenseAmount')">
                {{model.changeAmount}}
              </a-form-item>
            </a-col>
            <a-col :span="6"></a-col>
            <a-col :span="6"></a-col>
          </a-row>
        </section>
      </template>
      <!--收款-->
      <template v-if="financialType === '收款'">
        <section ref="print" id="moneyInPrint">
          <a-row class="form-row" :gutter="24">
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.customer')">
                <a-input v-decorator="['id']" hidden/>
                {{model.organName}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.operator')">
                {{model.handsPersonName}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billDate')">
                {{model.billTimeStr}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billNo')">
                {{model.billNo}}
              </a-form-item>
            </a-col>
          </a-row>
          <a-table
            ref="table"
            size="middle"
            bordered
            rowKey="id"
            :pagination="false"
            :loading="loading"
            :columns="moneyInColumns"
            :dataSource="dataSource">
          </a-table>
          <a-row class="form-row" :gutter="24">
            <a-col :lg="24" :md="24" :sm="24">
              <a-form-item :labelCol="labelCol" :wrapperCol="{xs: { span: 24 },sm: { span: 24 }}" label="" style="padding:20px 10px;">
                {{model.remark}}
              </a-form-item>
            </a-col>
          </a-row>
          <a-row class="form-row" :gutter="24">
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('financial.form.account')">
                {{model.accountName}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('financial.totalReceipt')">
                {{model.totalPrice}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('financial.discountAmount')">
                {{model.discountMoney}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('financial.actualReceipt')">
                {{model.changeAmount}}
              </a-form-item>
            </a-col>
          </a-row>
        </section>
      </template>
      <!--付款-->
      <template v-if="financialType === '付款'">
        <section ref="print" id="moneyOutPrint">
          <a-row class="form-row" :gutter="24">
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.supplier')">
                <a-input v-decorator="['id']" hidden/>
                {{model.organName}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.operator')">
                {{model.handsPersonName}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billDate')">
                {{model.billTimeStr}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billNo')">
                {{model.billNo}}
              </a-form-item>
            </a-col>
          </a-row>
          <a-table
            ref="table"
            size="middle"
            bordered
            rowKey="id"
            :pagination="false"
            :loading="loading"
            :columns="moneyOutColumns"
            :dataSource="dataSource">
          </a-table>
          <a-row class="form-row" :gutter="24">
            <a-col :lg="24" :md="24" :sm="24">
              <a-form-item :labelCol="labelCol" :wrapperCol="{xs: { span: 24 },sm: { span: 24 }}" label="" style="padding:20px 10px;">
                {{model.remark}}
              </a-form-item>
            </a-col>
          </a-row>
          <a-row class="form-row" :gutter="24">
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('financial.form.account')">
                {{model.accountName}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('financial.totalPayment')">
                {{model.totalPrice}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('financial.discountAmount')">
                {{model.discountMoney}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('financial.actualPayment')">
                {{model.changeAmount}}
              </a-form-item>
            </a-col>
          </a-row>
        </section>
      </template>
      <template v-if="fileList && fileList.length>0">
        <a-row class="form-row" :gutter="24">
          <a-col :span="12">
            <a-form-item :labelCol="{xs: { span: 24 },sm: { span: 3 }}" :wrapperCol="{xs: { span: 24 },sm: { span: 21 }}" :label="$t('common.attachment')">
              <j-upload v-model="fileList" bizPath="bill" :disabled="true" :buttonVisible="false"></j-upload>
            </a-form-item>
          </a-col>
          <a-col :span="12"></a-col>
        </a-row>
      </template>
    </a-form>
  </j-modal>
</template>
<script>
  import pick from 'lodash.pick'
  import { getAction, postAction } from '@/api/manage'
  import { findFinancialDetailByNumber, getCurrentSystemConfig } from '@/api/api'
  import { getCheckFlag } from '@/utils/util'
  import JUpload from '@/components/jeecg/JUpload'

  export default {
    name: 'FinancialDetail',
    components: {
      JUpload
    },
    data () {
      return {
        title: this.$t('common.detail'),
        width: '1600px',
        visible: false,
        modalStyle: '',
        model: {},
        isCanBackCheck: true,
        financialType: '',
        fileList: [],
        /* 原始反审核是否开启 */
        checkFlag: true,
        labelCol: {
          xs: { span: 24 },
          sm: { span: 6 },
        },
        wrapperCol: {
          xs: { span: 24 },
          sm: { span: 16 },
        },
        form: this.$form.createForm(this),
        loading: false,
        dataSource: [],
        url: {
          detailList: '/accountItem/getDetailList',
          batchSetStatusUrl: '/accountHead/batchSetStatus'
        },
        advanceInColumns: [
          { title: '#',dataIndex:'',width:'5%',align:'center',customRender:function(t,r,index){return parseInt(index)+1;}},
          { title: this.$t('system.accountName'),dataIndex: 'accountName',width: '30%'},
          { title: this.$t('financial.form.amount'),dataIndex: 'eachAmount', width: '30%'},
          { title: this.$t('common.remark'),dataIndex: 'remark', width: '30%'}
        ],
        giroColumns: [
          { title: '#',dataIndex:'',width:'5%',align:'center',customRender:function(t,r,index){return parseInt(index)+1;}},
          { title: this.$t('system.accountName'),dataIndex: 'accountName',width: '30%'},
          { title: this.$t('financial.form.amount'),dataIndex: 'eachAmount', width: '30%'},
          { title: this.$t('common.remark'),dataIndex: 'remark', width: '30%'}
        ],
        itemInColumns: [
          { title: '#',dataIndex:'',width:'5%',align:'center',customRender:function(t,r,index){return parseInt(index)+1;}},
          { title: this.$t('financial.form.incomeItem'),dataIndex: 'inOutItemName',width: '30%'},
          { title: this.$t('financial.form.amount'),dataIndex: 'eachAmount', width: '30%'},
          { title: this.$t('common.remark'),dataIndex: 'remark', width: '30%'}
        ],
        itemOutColumns: [
          { title: '#',dataIndex:'',width:'5%',align:'center',customRender:function(t,r,index){return parseInt(index)+1;}},
          { title: this.$t('financial.form.expenseItem'),dataIndex: 'inOutItemName',width: '30%'},
          { title: this.$t('financial.form.amount'),dataIndex: 'eachAmount', width: '30%'},
          { title: this.$t('common.remark'),dataIndex: 'remark', width: '30%'}
        ],
        moneyInColumns: [
          { title: '#',dataIndex:'',width:'5%',align:'center',customRender:function(t,r,index){return parseInt(index)+1;}},
          { title: this.$t('financial.salesBillNo'), dataIndex: 'billNumber', width: '20%' },
          { title: this.$t('financial.receivableDebt'),dataIndex: 'needDebt', width: '10%'},
          { title: this.$t('financial.receivedDebt'),dataIndex: 'finishDebt', width: '10%'},
          { title: this.$t('financial.thisReceipt'),dataIndex: 'eachAmount', width: '10%'},
          { title: this.$t('common.remark'),dataIndex: 'remark', width: '20%'}
        ],
        moneyOutColumns: [
          { title: '#',dataIndex:'',width:'5%',align:'center',customRender:function(t,r,index){return parseInt(index)+1;}},
          { title: this.$t('financial.purchaseBillNo'), dataIndex: 'billNumber', width: '20%' },
          { title: this.$t('financial.payableDebt'),dataIndex: 'needDebt', width: '10%'},
          { title: this.$t('financial.paidPayableDebt'),dataIndex: 'finishDebt', width: '10%'},
          { title: this.$t('financial.thisPayment'),dataIndex: 'eachAmount', width: '10%'},
          { title: this.$t('common.remark'),dataIndex: 'remark', width: '20%'}
        ],
      }
    },
    created () {
      let realScreenWidth = window.screen.width
      this.width = realScreenWidth<1500?'1200px':'1550px'
    },
    methods: {
      show(record, type, prefixNo) {
        //查询单条财务信息
        findFinancialDetailByNumber({ billNo: record.billNo }).then((res) => {
          if (res && res.code === 200) {
            let item = res.data
            this.financialType = type
            this.prefixNo = prefixNo
            //附件下载
            this.fileList = item.fileName
            this.visible = true
            this.modalStyle = 'top:20px;height: 95%;'
            this.model = Object.assign({}, item)
            this.$nextTick(() => {
              this.form.setFieldsValue(pick(this.model, 'id'))
            });
            let params = {
              headerId: this.model.id,
            }
            let url = this.readOnly ? this.url.detailList : this.url.detailList;
            this.requestSubTableData(url, params);
            this.getSystemConfig()
          }
        })
      },
      requestSubTableData(url, params, success) {
        this.loading = true
        getAction(url, params).then(res => {
          if(res && res.code === 200){
            this.dataSource = res.data.rows
            typeof success === 'function' ? success(res) : ''
          }
        }).finally(() => {
          this.loading = false
        })
      },
      getSystemConfig() {
        getCurrentSystemConfig().then((res) => {
          if(res.code === 200 && res.data){
            let multiBillType = res.data.multiBillType
            let multiLevelApprovalFlag = res.data.multiLevelApprovalFlag
            this.checkFlag = getCheckFlag(multiBillType, multiLevelApprovalFlag, this.prefixNo)
          }
        })
      },
      handleBackCheck() {
        let that = this
        this.$confirm({
          title: this.$t('common.confirmAction'),
          content: this.$t('financial.reverseAuditConfirm'),
          onOk: function () {
            that.loading = true
            postAction(that.url.batchSetStatusUrl, {status: '0', ids: that.model.id}).then((res) => {
              if(res.code === 200){
                that.$emit('ok')
                that.loading = false
                that.close()
              } else {
                that.$message.warning(res.data.message)
                that.loading = false
              }
            }).finally(() => {
            })
          }
        })
      },
      handleCancel() {
        this.close()
      },
      close() {
        this.$emit('close')
        this.visible = false
        this.modalStyle = ''
      },
    }
  }
</script>

<style scoped>

</style>
