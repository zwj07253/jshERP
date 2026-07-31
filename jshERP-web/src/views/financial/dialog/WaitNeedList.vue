<template>
  <div ref="container">
    <a-modal
      :title="title"
      :width="800"
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
        <a-button key="back" @click="handleCancel">{{ $t('financial.cancelEsc') }}</a-button>
      </template>
      <!-- 查询区域 -->
      <div class="table-page-search-wrapper">
        <!-- 搜索区域 -->
        <a-form layout="inline" @keyup.enter.native="searchQuery">
          <a-row :gutter="24">
            <a-col :md="12" :sm="24">
              <a-form-item :label="organType" :labelCol="labelCol" :wrapperCol="wrapperCol">
                <a-select :placeholder="$t('common.pleaseSelect') + (organType === '客户' ? $t('common.customer') : $t('common.supplier'))" v-model="queryParam.organId"
                          :dropdownMatchSelectWidth="false" showSearch allow-clear optionFilterProp="children" @search="handleSearchSupplier">
                  <div slot="dropdownRender" slot-scope="menu">
                    <v-nodes :vnodes="menu" />
                    <a-divider style="margin: 4px 0;" />
                    <div class="dropdown-btn" @mousedown="e => e.preventDefault()" @click="initSupplier"><a-icon type="reload" /> {{ $t('common.refreshList') }}</div>
                  </div>
                  <a-select-option v-for="(item,index) in supList" :key="index" :value="item.id">
                    {{ item.supplier }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <span style="float: left;overflow: hidden;" class="table-page-search-submitButtons">
              <a-col :md="12" :sm="24">
                <a-button type="primary" @click="searchQuery">{{ $t('common.search') }}</a-button>
                <a-button style="margin-left: 8px" @click="searchReset">{{ $t('common.reset') }}</a-button>
              </a-col>
            </span>
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
        :pagination="ipagination"
        :loading="loading"
        @change="handleTableChange">
        <span slot="action" slot-scope="text, record">
          <a @click="handleAction(record)">{{actionType}}</a>
        </span>
      </a-table>
      <!-- table区域-end -->
      <div>{{ $t('financial.debtDetailNote', [$t('menu.reportQuery'), organType === $t('common.customer') ? $t('report.customerReconciliation') : $t('report.supplierReconciliation')]) }}</div>
    </a-modal>
  </div>
</template>

<script>
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import { findBySelectCus, findBySelectSup } from '@/api/api'
  import { getFormatDate } from '@/utils/util'
  import { getAction } from '@/api/manage'
  export default {
    name: 'WaitNeedList',
    mixins:[JeecgListMixin],
    components: {
      VNodes: {
        functional: true,
        render: (h, ctx) => ctx.props.vnodes,
      }
    },
    data () {
      return {
        title: this.$t('common.action'),
        visible: false,
        disableMixinCreated: true,
        organType: '',
        actionType: '',
        setTimeFlag: null,
        supList: [],
        selectBillRows: [],
        queryParam: {
          organId: undefined,
          supplierType: '',
          hasDebt: '1',
          beginTime: '1990-01-01',
          endTime: getFormatDate(),
        },
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
            title: this.$t('common.action'),
            dataIndex: 'action',
            width:100,
            align:"center",
            scopedSlots: { customRender: 'action' },
          },
          { title: '', dataIndex: 'supplier',width:400, ellipsis:true},
          { title: this.$t('purchase.form.debtPlaceholder'), dataIndex: 'allNeed',width:150 }
        ],
        url: {
          list: "/depotHead/getStatementAccount"
        }
      }
    },
    created() {
    },
    methods: {
      getQueryParams() {
        let param = Object.assign({}, this.queryParam, this.isorter);
        param.field = this.getQueryField();
        param.currentPage = this.ipagination.current;
        param.pageSize = this.ipagination.pageSize;
        return param;
      },
      show(organType) {
        this.organType = organType
        this.columns[1].title = organType
        this.model = Object.assign({}, {});
        this.visible = true
        if(organType === '客户') {
          this.title = this.$t('financial.waitReceiptCustomer')
          this.queryParam.supplierType = '客户'
          this.actionType = this.$t('financial.receipt')
        } else if(organType === '供应商') {
          this.title = this.$t('financial.waitPaymentSupplier')
          this.queryParam.supplierType = '供应商'
          this.actionType = this.$t('financial.payment')
        }
        this.loadData(1)
        this.initSupplier()
      },
      initSupplier() {
        let that = this
        if(this.organType === '客户') {
          findBySelectCus({limit:1}).then((res) => {
            if (res) {
              that.supList = res
            }
          })
        } else if(this.organType === '供应商') {
          findBySelectSup({limit:1}).then((res) => {
            if (res) {
              that.supList = res;
            }
          })
        }
      },
      handleSearchSupplier(value) {
        let that = this
        if(this.setTimeFlag != null){
          clearTimeout(this.setTimeFlag);
        }
        if(this.organType === '客户') {
          this.setTimeFlag = setTimeout(() => {
            findBySelectCus({ key: value, limit:1 }).then((res) => {
              if (res) {
                that.supList = res
              }
            })
          }, 500)
        } else if(this.organType === '供应商') {
          this.setTimeFlag = setTimeout(() => {
            findBySelectSup({ key: value, limit:1 }).then((res) => {
              if (res) {
                that.supList = res;
              }
            })
          }, 500)
        }
      },
      //选择供应商进行付款，选择客户进行收款
      handleAction(record) {
        let type = ''
        let subType = ''
        if(this.organType === '客户') {
          type = '出库'
          subType = '销售'
        } else if(this.organType === '供应商') {
          type = '入库'
          subType = '采购'
        }
        let params = {
          search: {
            organId: record.id,
            materialParam: "",
            number: "",
            type: type,
            subType: subType,
            status: ""
          },
          currentPage: 1,
          pageSize: 1000
        }
        getAction('/depotHead/debtList', params).then((res) => {
          if (res.code === 200) {
            this.selectBillRows = res.data.rows
            this.$emit('ok', this.organType, record.id, this.selectBillRows)
            this.selectBillRows = []
            this.close()
          }
        })
      },
      close () {
        this.$emit('close')
        this.visible = false
      },
      handleCancel () {
        this.close()
      },
      searchReset() {
        this.queryParam = {
          organId: undefined,
          supplierType: this.organType,
          hasDebt: '1',
          beginTime: '1990-01-01',
          endTime: getFormatDate(),
        }
        this.loadData(1)
      }
    }
  }
</script>

<style scoped>

</style>