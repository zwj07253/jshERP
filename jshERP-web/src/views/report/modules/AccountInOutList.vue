<template>
  <div ref="container">
    <a-modal
      :title="title"
      :width="1200"
      :visible="visible"
      :getContainer="() => $refs.container"
      :maskStyle="{'top':'93px','left':'154px'}"
      :wrapClassName="wrapClassNameInfo()"
      :mask="isDesktop()"
      :maskClosable="false"
      @cancel="handleCancel"
      :cancelText="$t('common.close')"
      style="top:20px;height: 95%;">
      <template slot="footer">
        <a-button key="back" @click="handleCancel">{{$t('common.cancel')}}</a-button>
      </template>
      <!-- 查询区域 -->
      <div class="table-page-search-wrapper">
        <!-- 搜索区域 -->
        <a-form layout="inline" @keyup.enter.native="searchQuery">
          <a-row :gutter="24">
            <a-col :md="8" :sm="24">
              <a-form-item :label="$t('common.billNo')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                <a-input :placeholder="$t('common.enterBillNo')" v-model="queryParam.number"></a-input>
              </a-form-item>
            </a-col>
            <a-col :md="8" :sm="24">
              <a-form-item :label="$t('common.billDate')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                <a-range-picker
                  style="width:100%"
                  v-model="queryParam.createTimeRange"
                  format="YYYY-MM-DD"
                  :placeholder="[$t('common.startDate'), $t('common.endDate')]"
                  @change="onDateChange"
                  @ok="onDateOk"
                />
              </a-form-item>
            </a-col>
            <a-col :md="8" :sm="24">
              <a-button type="primary" @click="searchQuery">{{$t('common.search')}}</a-button>
              <a-button style="margin-left: 8px" @click="searchReset">{{$t('common.reset')}}</a-button>
              <a-button style="margin-left: 8px" @click="exportExcel" icon="download">{{$t('common.export')}}</a-button>
            </a-col>
          </a-row>
        </a-form>
      </div>
      <!-- table区域-begin -->
      <a-table
        bordered
        ref="table"
        size="middle"
        rowKey="rowKey"
        :columns="columns"
        :dataSource="dataSource"
        :components="handleDrag(columns)"
        :pagination="ipagination"
        :loading="loading"
        @change="handleTableChange">
        <span slot="numberCustomRender" slot-scope="text, record">
          <a @click="myHandleDetail(record)">{{record.number}}</a>
        </span>
        <span slot="customTitle">
          <a-popover trigger="click" placement="right">
            <template slot="content">
              <a-checkbox-group @change="onColChange" v-model="settingDataIndex" :defaultValue="settingDataIndex">
                <a-row style="width: 600px">
                  <template v-for="(item,index) in defColumns">
                    <template>
                      <a-col :span="6">
                        <a-checkbox :value="item.dataIndex" v-if="item.dataIndex==='rowIndex'" disabled></a-checkbox>
                        <a-checkbox :value="item.dataIndex" v-if="item.dataIndex!=='rowIndex'">
                          <j-ellipsis :value="item.title" :length="10"></j-ellipsis>
                        </a-checkbox>
                      </a-col>
                    </template>
                  </template>
                </a-row>
                <a-row style="padding-top: 10px;">
                  <a-col>
                    {{$t('common.restoreColumns')}}<a-button @click="handleRestDefault" type="link" size="small">{{$t('common.restoreDefault')}}</a-button>
                  </a-col>
                </a-row>
              </a-checkbox-group>
            </template>
            <a-icon type="setting" />
          </a-popover>
        </span>
      </a-table>
      <!-- table区域-end -->
      <!-- 表单区域 -->
      <bill-detail ref="billDetail"></bill-detail>
      <financial-detail ref="financialDetail"></financial-detail>
    </a-modal>
  </div>
</template>
<script>
  import BillDetail from '../../bill/dialog/BillDetail'
  import FinancialDetail from '../../financial/dialog/FinancialDetail'
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import {mixinDevice} from '@/utils/mixin'
  import JEllipsis from '@/components/jeecg/JEllipsis'
  import {findBillDetailByNumber, findFinancialDetailByNumber} from '@/api/api'
  import {getAction} from '@/api/manage'
  export default {
    name: "AccountInOutList",
    mixins:[JeecgListMixin, mixinDevice],
    components: {
      BillDetail,
      FinancialDetail,
      JEllipsis
    },
    data () {
      return {
        title:this.$t('common.operation'),
        visible: false,
        disableMixinCreated: true,
        toFromType: '',
        currentAccountId: '',
        // 查询条件
        queryParam: {
          accountId:'',
          number: '',
          beginTime: '',
          endTime: '',
        },
        tabKey: "1",
        pageName: 'accountInOutList',
        // 默认索引
        defDataIndex:['rowIndex','number','type','supplierName','changeAmount','balance','operTime'],
        // 默认列
        defColumns: [
          {
            dataIndex: 'rowIndex',
            width:40,
            align:"center",
            slots: { title: 'customTitle' },
            customRender:function (t,r,index) {
              return parseInt(index)+1;
            }
          },
          {
            title: this.$t('common.billNo'), dataIndex: 'number', width: 120,
            scopedSlots: { customRender: 'numberCustomRender' },
          },
          { title: this.$t('common.type'), dataIndex: 'type', width: 100},
          { title: this.$t('common.unitInfo'), dataIndex: 'supplierName', width: 180, ellipsis:true},
          { title: this.$t('common.amount'), dataIndex: 'changeAmount', width: 100, ellipsis:true,
            customRender:(t,r,index) => {
              let amount = t
              if(Number(amount) > 0) {
                amount = '+' + amount
              }
              return amount + (r.aList ? '[' + this.$t('common.multiAccount') + ']' : '')
            }
          },
          { title: this.$t('common.balance'), dataIndex: 'balance', width: 80},
          { title: this.$t('common.billDate'), dataIndex: 'operTime', width: 120},
          { title: this.$t('common.remark'), dataIndex: 'remark', width: 150}
        ],
        labelCol: {
          xs: { span: 1 },
          sm: { span: 2 },
        },
        wrapperCol: {
          xs: { span: 10 },
          sm: { span: 16 },
        },
        url: {
          list: "/account/findAccountInOutList"
        }
      }
    },
    created() {
      this.initColumnsSetting()
    },
    methods: {
      getQueryParams() {
        let param = Object.assign({}, this.queryParam, this.isorter);
        param.field = this.getQueryField();
        param.accountId = this.currentAccountId
        param.currentPage = this.ipagination.current;
        param.pageSize = this.ipagination.pageSize;
        return param;
      },
      show(record) {
        this.model = Object.assign({}, record);
        this.currentAccountId = record.id
        this.visible = true;
        this.queryParam.accountId = record.id
        this.loadData(1)
      },
      close () {
        this.$emit('close');
        this.visible = false;
      },
      handleCancel () {
        this.close()
      },
      onDateChange: function (value, dateString) {
        this.queryParam.beginTime=dateString[0];
        this.queryParam.endTime=dateString[1];
      },
      onDateOk(value) {
        console.log(value);
      },
      myHandleDetail(record) {
        let that = this
        this.toFromType = record.fromType
        if(record.fromType === 'bill') {
          findBillDetailByNumber({ number: record.number }).then((res) => {
            if (res && res.code === 200) {
              this.$refs.billDetail.isCanBackCheck = false
              that.$refs.billDetail.show(res.data, record.type);
              that.$refs.billDetail.title=that.$t('common.detail');
            }
          })
        } else if(record.fromType === 'financial') {
          findFinancialDetailByNumber({ billNo: record.number }).then((res) => {
            if (res && res.code === 200) {
              this.$refs.financialDetail.isCanBackCheck = false
              that.$refs.financialDetail.show(res.data, record.type);
              that.$refs.financialDetail.title=that.$t('common.detail');
            }
          })
        }
      },
      exportExcel() {
        const params = Object.assign({}, this.getQueryParams(), {currentPage: 1, pageSize: 10000})
        this.loading = true
        getAction(this.url.list, params).then((res) => {
          if(res && res.code === 200) {
            if(res.data.total > 10000) {
              this.$message.warning(this.$t('report.exportLimit'))
              return
            }
            const list = (res.data.rows || []).map(ds => [
              ds.number, ds.type, ds.supplierName, this.getRealChangeAmount(ds),
              ds.balance, ds.operTime, ds.remark
            ])
            this.handleExportXlsPost(this.$t('report.accountFlowExport'), this.$t('report.accountFlowExport'),
              this.$t('common.billNo')+','+this.$t('common.type')+','+this.$t('common.unitInfo')+','+this.$t('common.amount')+','+this.$t('common.balance')+','+this.$t('common.billDate')+','+this.$t('common.remark'), this.$t('report.accountFlowExport'), list)
          } else {
            this.$message.warning((res && res.data) || this.$t('report.exportFailed'))
          }
        }).finally(() => {
          this.loading = false
        })
      },
      getRealChangeAmount(r) {
        let amount = r.changeAmount
        if(Number(amount) > 0) {
          amount = '+' + amount
        }
        return amount + (r.aList ? '[' + this.$t('common.multiAccount') + ']' : '')
      }
    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less'
</style>
