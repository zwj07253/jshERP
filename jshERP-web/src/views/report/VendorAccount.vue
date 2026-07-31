<!-- f r o m 7 5  2 7 1  8 9 2 0 -->
<template>
  <a-row :gutter="24">
    <a-col :md="24">
      <a-card :style="cardStyle" :bordered="false">
        <!-- 查询区域 -->
        <div class="table-page-search-wrapper">
          <a-form layout="inline" @keyup.enter.native="searchQuery">
            <a-row :gutter="24">
              <a-col :md="6" :sm="24">
                <a-form-item :label="$t('common.supplier')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-select :placeholder="$t('common.selectSupplier')" v-model="queryParam.organId"
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
              <a-col :md="6" :sm="24">
                <a-form-item :label="$t('report.billCycle')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-range-picker
                    style="width: 100%"
                    v-model="queryParam.createTimeRange"
                    format="YYYY-MM-DD"
                    :placeholder="[$t('common.startDate'), $t('common.endDate')]"
                    @change="onDateChange"
                  />
                </a-form-item>
              </a-col>
              <a-col :md="12" :sm="24">
                <span class="table-page-search-submitButtons">
                  <a-button type="primary" @click="searchQuery">{{ $t('common.search') }}</a-button>
                  <a-button style="margin-left: 8px" v-print="'#reportPrint'" icon="printer">{{ $t('common.print') }}</a-button>
                  <a-button style="margin-left: 8px" @click="exportExcel" icon="download">{{ $t('common.export') }}</a-button>
                  <a @click="handleToggleSearch" style="margin-left: 8px">
                    {{ toggleSearchStatus ? $t('common.collapse') : $t('common.expand') }}
                    <a-icon :type="toggleSearchStatus ? 'up' : 'down'"/>
                  </a>
                </span>
              </a-col>
            </a-row>
            <a-row :gutter="24">
              <a-col :span="24">
                <div class="vendor-account-summary">{{firstTotal}} {{lastTotal}}</div>
              </a-col>
            </a-row>
            <template v-if="toggleSearchStatus">
              <a-row :gutter="24">
                <a-col :md="6" :sm="24">
                  <a-form-item :label="$t('report.debtDetail')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-select v-model="queryParam.hasDebt">
                      <a-select-option value="1">{{ $t('common.hasDebtOpt') }}</a-select-option>
                      <a-select-option value="0">{{ $t('common.noDebtOpt') }}</a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
              </a-row>
            </template>
          </a-form>
        </div>
        <!-- table区域-begin -->
        <section ref="print" id="reportPrint">
          <a-table
            bordered
            ref="table"
            size="middle"
            rowKey="id"
            :columns="columns"
            :dataSource="displayDataSource"
            :components="handleDrag(columns)"
            :pagination="false"
            :scroll="scroll"
            :loading="loading"
            @change="handleTableChange">
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
                              <j-ellipsis :value="item.title" v-if="item.dataIndex!=='allNeed'" :length="10"></j-ellipsis>
                              <j-ellipsis :value="$t('report.finalPayable')" v-if="item.dataIndex==='allNeed'" :length="10"></j-ellipsis>
                            </a-checkbox>
                          </a-col>
                        </template>
                      </template>
                    </a-row>
                    <a-row style="padding-top: 10px;">
                      <a-col>
                        {{ $t('common.restoreColumns') }}<a-button @click="handleRestDefault" type="link" size="small">{{ $t('common.restoreDefault') }}</a-button>
                      </a-col>
                    </a-row>
                  </a-checkbox-group>
                </template>
                <a-icon type="setting" />
              </a-popover>
            </span>
            <span slot="action" slot-scope="text, record">
              <a v-if="record.rowIndex !== $t('common.total')" @click="showDebtAccountList(record)">{{ $t('common.detail') }}</a>
            </span>
            <span slot="allNeedTitle">
              {{ $t('report.finalPayable') }}
              <a-tooltip :title="`${$t('report.finalPayable')}=${$t('report.prePayable')}+${$t('report.periodDebt')}-${$t('report.periodPayment')}`">
                <a-icon type="question-circle" />
              </a-tooltip>
            </span>
          </a-table>
          <a-row :gutter="24" style="margin-top: 8px;text-align:right;">
            <a-col :md="24" :sm="24">
              <a-pagination @change="paginationChange" @showSizeChange="paginationShowSizeChange"
                size="small"
                show-size-changer
                :showQuickJumper="true"
                :current="ipagination.current"
                :page-size="ipagination.pageSize"
                :page-size-options="ipagination.pageSizeOptions"
                :total="ipagination.total"
                :show-total="total => $t('common.total') + ' ' + total + ' ' + $t('report.itemsPerPage')">
                <template slot="buildOptionText" slot-scope="props">
                  <span>{{ props.value }}{{ $t('report.itemsPerPage') }}</span>
                </template>
              </a-pagination>
            </a-col>
          </a-row>
        </section>
        <!-- table区域-end -->
        <!-- 表单区域 -->
        <debt-account-list ref="debtAccountList"></debt-account-list>
      </a-card>
    </a-col>
  </a-row>
</template>
<script>
  import DebtAccountList from './modules/DebtAccountList'
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import { getFormatDate, getNowFormatYear, getPrevMonthFormatDate } from '@/utils/util'
  import { getAction } from '@/api/manage'
  import {findBySelectSup} from '@/api/api'
  import JEllipsis from '@/components/jeecg/JEllipsis'
  import moment from 'moment'
  export default {
    name: "VendorAccount",
    mixins:[JeecgListMixin],
    components: {
      DebtAccountList,
      JEllipsis,
      VNodes: {
        functional: true,
        render: (h, ctx) => ctx.props.vnodes,
      }
    },
    data () {
      return {
        labelCol: {
          span: 5
        },
        wrapperCol: {
          span: 18,
          offset: 1
        },
        // 查询条件
        queryParam: {
          supplierType: "供应商",
          organId: undefined,
          hasDebt: '1',
          beginTime: getPrevMonthFormatDate(3),
          endTime: getFormatDate(),
          createTimeRange: [moment(getPrevMonthFormatDate(3)), moment(getFormatDate())],
        },
        ipagination:{
          pageSize: 10,
          pageSizeOptions: ['10', '20', '30', '100', '200']
        },
        supList: [],
        firstTotal: '',
        lastTotal: '',
        canViewFinancialHistory: false,
        setTimeFlag: null,
        tabKey: "1",
        pageName: 'vendorAccount',
        // 默认索引
        defDataIndex:['rowIndex','action','supplier','contacts','telephone','phoneNum','email','preNeed','debtMoney','backMoney','allNeed'],
        // 默认列
        defColumns: [
          {
            dataIndex: 'rowIndex', width:40, align:"center", slots: { title: 'customTitle' },
            customRender:(t,r,index) => {
              return (t !== this.$t('common.total')) ? (parseInt(index) + 1) : t
            }
          },
          {title: this.$t('report.debtDetail'), dataIndex: 'action', align:"center", width: 80,
            scopedSlots: { customRender: 'action' }
          },
          {title: this.$t('common.supplier'), dataIndex: 'supplier', width: 150, ellipsis:true},
          {title: this.$t('system.contact'), dataIndex: 'contacts', width: 100, ellipsis:true},
          {title: this.$t('common.phoneNo'), dataIndex: 'telephone', width: 100},
          {title: this.$t('system.phone'), dataIndex: 'phoneNum', width: 100},
          {title: this.$t('common.email'), dataIndex: 'email', width: 100},
          {title: this.$t('report.prePayable'), dataIndex: 'preNeed', sorter: true, width: 80},
          {title: this.$t('report.periodDebt'), dataIndex: 'debtMoney', sorter: true, width: 80},
          {title: this.$t('report.periodPayment'), dataIndex: 'backMoney', sorter: true, width: 80},
          {dataIndex: 'allNeed', sorter: true, width: 80,
            scopedSlots: { title: 'allNeedTitle' }
          }
        ],
        url: {
          list: "/depotHead/getStatementAccount",
        }
      }
    },
    created () {
      this.initSupplier()
      this.initColumnsSetting()
    },
    computed: {
      displayDataSource() {
        const rows = (this.dataSource || []).slice()
        if (!rows.length) {
          return rows
        }
        const totalRow = {
          id: `vendor-account-total-${this.ipagination.current}`,
          rowIndex: this.$t('common.total')
        }
        const numericFields = ['preNeed', 'debtMoney', 'backMoney', 'allNeed']
        numericFields.forEach(field => {
          totalRow[field] = rows.reduce((sum, row) => {
            const value = Number.parseFloat(row[field])
            return sum + (Number.isFinite(value) ? value : 0)
          }, 0).toFixed(2)
        })
        return rows.concat(totalRow)
      }
    },
    methods: {
      getQueryParams() {
        let param = Object.assign({}, this.queryParam, this.isorter);
        param.field = this.getQueryField();
        param.currentPage = this.ipagination.current;
        param.pageSize = this.ipagination.pageSize;
        return param;
      },
      initSupplier() {
        let that = this;
        findBySelectSup({limit:1}).then((res)=>{
          if(res) {
            that.supList = res;
          }
        });
      },
      handleSearchSupplier(value) {
        let that = this
        if(this.setTimeFlag != null){
          clearTimeout(this.setTimeFlag);
        }
        this.setTimeFlag = setTimeout(()=>{
          findBySelectSup({key: value, limit:1}).then((res) => {
            if(res) {
              that.supList = res;
            }
          })
        },500)
      },
      onDateChange: function (value, dateString) {
        this.queryParam.beginTime=dateString[0]
        this.queryParam.endTime=dateString[1]
        if(dateString[0] && dateString[1]) {
          this.queryParam.createTimeRange = [moment(dateString[0]), moment(dateString[1])]
        }
      },
      loadData(arg) {
        //加载数据 若传入参数1则加载第一页的内容
        if (arg === 1) {
          this.ipagination.current = 1;
        }
        let params = this.getQueryParams();//查询条件
        this.loading = true;
        getAction(this.url.list, params).then((res) => {
          if (res.code===200) {
            this.dataSource = res.data.rows;
            this.ipagination.total = res.data.total;
            this.firstTotal = this.$t('report.prePayable') + '：' + this.formatNumber(res.data.firstMoney) + "，"
            this.lastTotal = this.$t('report.finalPayable') + '：' + this.formatNumber(res.data.lastMoney)
            this.canViewFinancialHistory = res.data.canViewFinancialHistory === true
          } else if(res.code===510){
            this.$message.warning(res.data)
          } else {
            this.$message.warning(typeof res.data === 'string' ? res.data : res.data.message)
          }
          this.loading = false;
        })
      },
      searchQuery() {
        if(this.queryParam.beginTime === '' || this.queryParam.endTime === ''){
          this.$message.warning(this.$t('report.selectBillDate'))
        } else {
          this.loadData(1);
        }
      },
      exportExcel() {
        if ((this.ipagination.total || 0) > 10000) {
          this.$message.warning(this.$t('report.exportLimit'))
          return
        }
        let params = this.getQueryParams()
        params.currentPage = 1
        params.pageSize = Math.max(this.ipagination.total || 0, 1)
        this.loading = true
        getAction(this.url.list, params).then((res) => {
          if (res.code === 200) {
            this.exportExcelRows(res.data.rows || [])
          } else {
            const message = typeof res.data === 'string' ? res.data : res.data && res.data.message
            this.$message.warning(message || this.$t('report.exportFailed'))
          }
        }).finally(() => {
          this.loading = false
        })
      },
      exportExcelRows(dataSource) {
        let list = []
        let head = this.$t('common.supplier') + ',' + this.$t('report.contacts') + ',' + this.$t('report.phoneNum') + ',' + this.$t('report.telephone') + ',' + this.$t('report.email') + ',' + this.$t('report.prePayable') + ',' + this.$t('report.periodDebt') + ',' + this.$t('report.periodPayment') + ',' + this.$t('report.finalPayable')
        for (let i = 0; i < dataSource.length; i++) {
          let item = []
          let ds = dataSource[i]
          item.push(ds.supplier, ds.contacts, ds.telephone, ds.phoneNum, ds.email, ds.preNeed, ds.debtMoney, ds.backMoney, ds.allNeed)
          list.push(item)
        }
        let tip = this.$t('common.billDate') + '：' + this.queryParam.beginTime + '~' + this.queryParam.endTime
        this.handleExportXlsPost(this.$t('report.supplierReconciliation'), this.$t('report.supplierReconciliation'), head, tip, list)
      },
      showDebtAccountList(record) {
        this.$refs.debtAccountList.show(record.id, '入库', '采购', this.$t('common.supplier'), "", this.queryParam.beginTime,
          this.queryParam.endTime, this.canViewFinancialHistory)
        this.$refs.debtAccountList.title = this.$t('report.debtDetail')
        this.$refs.debtAccountList.disableSubmit = false
      }
    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less';

  .vendor-account-summary {
    display: flex;
    min-height: 32px;
    align-items: center;
    justify-content: flex-end;
    text-align: right;
    overflow-wrap: anywhere;
  }
</style>
