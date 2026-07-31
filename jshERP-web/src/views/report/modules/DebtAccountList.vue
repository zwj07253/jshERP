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
        <a-button key="back" @click="handleCancel">{{ $t('common.cancel') }}</a-button>
      </template>
      <!-- 查询区域 -->
      <div class="table-page-search-wrapper">
        <!-- 搜索区域 -->
        <a-form layout="inline" @keyup.enter.native="searchQuery">
          <a-row :gutter="24">
            <a-col :md="6" :sm="24">
              <a-form-item :label="$t('common.billNo')" :labelCol="{span: 5}" :wrapperCol="{span: 18, offset: 1}">
                <a-input :placeholder="$t('common.enterBillNo')" v-model="queryParam.number"></a-input>
              </a-form-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-item :label="$t('common.materialInfo')" :labelCol="{span: 5}" :wrapperCol="{span: 18, offset: 1}">
                <a-input :placeholder="$t('common.enterMaterial')" v-model="queryParam.materialParam"></a-input>
              </a-form-item>
            </a-col>
            <span style="float: left;overflow: hidden;" class="table-page-search-submitButtons">
              <a-col :md="12" :sm="24">
                <a-button type="primary" @click="searchQuery">{{ $t('common.search') }}</a-button>
                <a-button style="margin-left: 8px" v-print="'#debtAccountPrint'" icon="printer">{{ $t('common.print') }}</a-button>
                <a-button style="margin-left: 8px" @click="handleExportXls($t('report.debtDetailExport'))" icon="download">{{ $t('common.export') }}</a-button>
                <a-button v-if="canViewFinancialHistory" style="margin-left: 8px" @click="handleHistoryFinancial" icon="history">{{historyText}}</a-button>
              </a-col>
            </span>
          </a-row>
        </a-form>
      </div>
      <!-- table区域-begin -->
      <section ref="debtPrint" id="debtAccountPrint">
        <a-table
          bordered
          ref="table"
          size="middle"
          rowKey="id"
          :columns="columns"
          :dataSource="dataSource"
          :pagination="ipagination"
          :loading="loading"
          @change="handleTableChange">
          <span slot="numberCustomRender" slot-scope="text, record">
            <a @click="myHandleDetail(record)">{{record.number}}</a>
          </span>
        </a-table>
      </section>
      <!-- table区域-end -->
      <!-- 表单区域 -->
      <bill-detail ref="modalDetail"></bill-detail>
      <history-financial-list ref="historyFinancial"></history-financial-list>
    </a-modal>
  </div>
</template>

<script>
  import BillDetail from '../../bill/dialog/BillDetail'
  import HistoryFinancialList from './HistoryFinancialList'
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import { getMpListShort } from "@/utils/util"
  import {mixinDevice} from '@/utils/mixin'
  import { findBillDetailByNumber } from '@/api/api'
  import Vue from 'vue'
  export default {
    name: 'DebtAccountList',
    mixins:[JeecgListMixin, mixinDevice],
    components: {
      BillDetail,
      HistoryFinancialList
    },
    data () {
      return {
        title: this.$t('common.action'),
        visible: false,
        disableMixinCreated: true,
        queryParam: {
          organId: "",
          materialParam: "",
          number: "",
          type: "",
          subType: "",
          status: "",
          mpList: getMpListShort(Vue.ls.get('materialPropertyList'))  //扩展属性
        },
        historyText: '',
        financialType: '',
        canViewFinancialHistory: false,
        labelCol: {
          xs: { span: 24 },
          sm: { span: 8 },
        },
        wrapperCol: {
          xs: { span: 24 },
          sm: { span: 16 },
        },
        // 表头
        columns: [
          {
            title: '#', dataIndex: 'rowIndex', width:40, align:"center",
            customRender:(t,r,index) => {
              return (t !== this.$t('common.total')) ? (parseInt(index) + 1) : t
            }
          },
          {
            title: this.$t('common.billNo'), dataIndex: 'number', width: 120,
            scopedSlots: { customRender: 'numberCustomRender' },
          },
          { title: '', dataIndex: 'organName',width:120},
          { title: this.$t('common.materialInfo'), dataIndex: 'materialsList',width:200, ellipsis:true,
            customRender:function (text,record,index) {
              if(text) {
                return text.replace(",","，");
              }
            }
          },
          { title: this.$t('common.billDate'), dataIndex: 'operTimeStr',width:130},
          { title: this.$t('common.operator'), dataIndex: 'userName',width:60, ellipsis:true},
          { title: this.$t('report.debtorDebt'), dataIndex: 'needDebt',width:70},
          { title: this.$t('report.debtorPaid'), dataIndex: 'finishDebt',width:70},
          { title: this.$t('report.debtorPending'), dataIndex: 'debt',width:70}
        ],
        url: {
          list: "/depotHead/debtList",
          exportXlsUrl: "/depotHead/debtExport"
        }
      }
    },
    computed: {
      getType: function () {
        return 'checkbox';
      }
    },
    created() {
    },
    methods: {
      show(organId, type, subType, organType, status, beginTime, endTime, canViewFinancialHistory) {
        this.queryParam.organId = organId
        this.queryParam.type = type
        this.queryParam.subType = subType
        this.queryParam.status = status
        this.queryParam.beginTime = beginTime
        this.queryParam.endTime = endTime
        this.canViewFinancialHistory = canViewFinancialHistory === true
        this.columns[2].title = organType
        if(type === '入库') {
          this.columns[7].title = this.$t('report.debtorPaid')
          this.columns[8].title = this.$t('report.debtorPending')
          this.historyText = this.$t('report.historyPayment')
          this.financialType = '付款'
        } else if(type === '出库') {
          this.columns[7].title = this.$t('report.debtorPaid')
          this.columns[8].title = this.$t('report.debtorPending')
          this.historyText = this.$t('report.historyCollection')
          this.financialType = '收款'
        }
        this.model = Object.assign({}, {});
        this.visible = true;
        this.loadData(1)
      },
      myHandleDetail(record) {
        findBillDetailByNumber({ number: record.number }).then((res) => {
          if (res && res.code === 200) {
            let type = res.data.depotHeadType
            type = type.replace('其它','')
            this.$refs.modalDetail.isCanBackCheck = false
            this.handleDetail(res.data, type)
          }
        })
      },
      close () {
        this.$emit('close');
        this.visible = false;
      },
      handleCancel () {
        this.close()
      },
      onDateOk(value) {
        console.log(value);
      },
      handleHistoryFinancial() {
        this.$refs.historyFinancial.visible = true
        this.$refs.historyFinancial.title = this.historyText
        this.$refs.historyFinancial.queryParam.organId = this.queryParam.organId
        this.$refs.historyFinancial.queryParam.beginTime = this.queryParam.beginTime
        this.$refs.historyFinancial.queryParam.endTime = this.queryParam.endTime
        this.$refs.historyFinancial.queryParam.type = this.financialType
        this.$refs.historyFinancial.show()
      }
    }
  }
</script>

<style scoped>

</style>
