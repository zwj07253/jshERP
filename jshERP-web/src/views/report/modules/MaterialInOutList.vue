<template>
  <div ref="container">
    <a-modal
      :title="title"
      :width="1400"
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
              <a-button type="primary" @click="searchQuery">{{ $t('common.search') }}</a-button>
              <a-button style="margin-left: 8px" @click="searchReset">{{ $t('common.reset') }}</a-button>
              <a-button style="margin-left: 8px" @click="exportExcel" icon="download">{{ $t('common.export') }}</a-button>
            </a-col>
          </a-row>
        </a-form>
      </div>
      <!-- table区域-begin -->
      <a-table
        bordered
        ref="table"
        size="middle"
        rowKey="id"
        :columns="columns"
        :dataSource="dataSource"
        :components="handleDrag(columns)"
        :pagination="ipagination"
        :loading="loading"
        @change="handleTableChange">
        <span slot="numberCustomRender" slot-scope="text, record">
          <a @click="myHandleDetail(record)">{{record.number}}</a>
        </span>
      </a-table>
      <!-- table区域-end -->
      <!-- 表单区域 -->
      <bill-detail ref="billDetail"></bill-detail>
    </a-modal>
  </div>
</template>
<script>
  import BillDetail from '../../bill/dialog/BillDetail'
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import JEllipsis from '@/components/jeecg/JEllipsis'
  import { findBillDetailByNumber } from '@/api/api'
  import { mixinDevice } from '@/utils/mixin'

  export default {
    name: "MaterialInOutList",
    mixins:[JeecgListMixin, mixinDevice],
    components: {
      BillDetail,
      JEllipsis
    },
    data () {
      return {
        title:this.$t('common.action'),
        visible: false,
        disableMixinCreated: true,
        toFromType: '',
        currentMaterialId: '',
        // 查询条件
        queryParam: {
          depotIds: '',
          materialId:'',
          number: '',
          beginTime: '',
          endTime: '',
        },
        ipagination:{
          pageSizeOptions: ['10', '20', '30', '100', '200']
        },
        tabKey: "1",
        // 表头
        columns: [
          {
            title: '#',
            dataIndex: '',
            key:'rowIndex',
            width:40,
            align:"center",
            customRender:function (t,r,index) {
              return parseInt(index)+1;
            }
          },
          {
            title: this.$t('common.billNo'), dataIndex: 'number', width: 120,
            scopedSlots: { customRender: 'numberCustomRender' },
          },
          { title: this.$t('common.type'), dataIndex: 'type', width: 80},
          { title: this.$t('common.barcode'), dataIndex: 'barCode', width: 100},
          { title: this.$t('common.name'), dataIndex: 'materialName', width: 200},
          { title: this.$t('common.depotName'), dataIndex: 'depotName', width: 80},
          { title: this.$t('common.quantity'), dataIndex: 'basicNumber', width: 70},
          { title: this.$t('purchase.form.columns.unitPrice'), dataIndex: 'unitPrice', width: 70},
          { title: this.$t('common.amount'), dataIndex: 'allPrice', width: 70},
          { title: this.$t('common.date'), dataIndex: 'operTime', width: 110}
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
          list: "/depotItem/findDetailByDepotIdsAndMaterialId"
        }
      }
    },
    created() {
    },
    methods: {
      getQueryParams() {
        let param = Object.assign({}, this.queryParam, this.isorter)
        param.field = this.getQueryField()
        param.materialId = this.currentMaterialId
        param.currentPage = this.ipagination.current
        param.pageSize = this.ipagination.pageSize
        return param
      },
      show(record, depotIds) {
        if(!record || !record.id) {
          this.$message.warning(this.$t('common.error'))
          return
        }
        this.model = Object.assign({}, record);
        this.currentMaterialId = record.id
        this.visible = true;
        this.queryParam.depotIds = depotIds
        this.queryParam.materialId = record.id
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
        findBillDetailByNumber({ number: record.number }).then((res) => {
          if (res && res.code === 200) {
            this.$refs.billDetail.isCanBackCheck = false
            that.$refs.billDetail.show(res.data, record.type);
            that.$refs.billDetail.title=this.$t('common.detail');
          }
        })
      },
      exportExcel() {
        let list = []
        let head = this.$t('common.billNo') + ',' + this.$t('common.type') + ',' + this.$t('common.barCode') + ',' + this.$t('common.name') + ',' + this.$t('common.depotName') + ',' + this.$t('common.number') + ',' + this.$t('common.unitPrice') + ',' + this.$t('common.amount') + ',' + this.$t('common.date')
        for (let i = 0; i < this.dataSource.length; i++) {
          let item = []
          let ds = this.dataSource[i]
          item.push(ds.number, ds.type, ds.barCode, ds.materialName, ds.depotName, ds.basicNumber, ds.unitPrice, ds.allPrice, ds.operTime)
          list.push(item)
        }
        let tip = this.$t('report.productStockFlowQuery')
        this.handleExportXlsPost(this.$t('report.productStockFlow'), this.$t('report.productStockFlow'), head, tip, list)
      }
    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less'
</style>
