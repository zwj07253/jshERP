<!-- from 7 5 2 7 1 8 9 2 0 -->
<template>
  <a-row :gutter="24">
    <a-col :md="24">
      <a-card :style="cardStyle" :bordered="false">
        <!-- 查询区域 -->
        <div class="table-page-search-wrapper">
          <a-form layout="inline" @keyup.enter.native="searchQuery">
            <a-row :gutter="24">
              <a-col :md="6" :sm="24">
                <a-form-item :label="$t('common.name')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-input :placeholder="$t('common.pleaseInput')+$t('common.name')" v-model="queryParam.name"></a-input>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item :label="$t('report.accountNo')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-input :placeholder="$t('common.pleaseInput')+$t('report.accountNo')" v-model="queryParam.serialNo"></a-input>
                </a-form-item>
              </a-col>
              <a-col :md="5" :sm="24">
                <span class="table-page-search-submitButtons">
                  <a-button type="primary" @click="searchQuery">{{ $t('common.search') }}</a-button>
                  <a-button style="margin-left: 8px" v-print="'#reportPrint'" icon="printer">{{ $t('common.print') }}</a-button>
                  <a-button style="margin-left: 8px" @click="exportExcel" icon="download">{{ $t('common.export') }}</a-button>
                </span>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item>
                  <span>{{ $t('report.thisMonthTotal') }}：{{allMonthAmount}}，{{ $t('report.totalBalance') }}：{{allCurrentAmount}}</span>
                </a-form-item>
              </a-col>
            </a-row>
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
            :dataSource="dataSource"
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
                              <j-ellipsis :value="item.title" :length="10"></j-ellipsis>
                            </a-checkbox>
                          </a-col>
                        </template>
                      </template>
                    </a-row>
                    <a-row style="padding-top: 10px;">
                      <a-col>
                        {{ $t('common.restoreColumns') }}：<a-button @click="handleRestDefault" type="link" size="small">{{ $t('common.restoreDefault') }}</a-button>
                      </a-col>
                    </a-row>
                  </a-checkbox-group>
                </template>
                <a-icon type="setting" />
              </a-popover>
            </span>
            <span slot="action" slot-scope="text, record">
              <a @click="showAccountInOutList(record)">{{record.id?$t('report.accountFlow'):''}}</a>
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
                :show-total="total => $t('common.total') + ' ' + total + ' ' + $t('common.items')">
                <template slot="buildOptionText" slot-scope="props">
                  <span>{{ props.value }}{{ $t('report.itemsPerPage') }}</span>
                </template>
              </a-pagination>
            </a-col>
          </a-row>
        </section>
        <!-- table区域-end -->
        <account-in-out-list ref="accountInOutList" @ok="modalFormOk"></account-in-out-list>
      </a-card>
    </a-col>
  </a-row>
</template>
<script>
  import AccountInOutList from './modules/AccountInOutList'
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import JEllipsis from '@/components/jeecg/JEllipsis'
  import {getAction} from '@/api/manage'
  export default {
    name: "AccountReport",
    mixins:[JeecgListMixin],
    components: {
      AccountInOutList,
      JEllipsis
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
          name:'',
          serialNo:''
        },
        ipagination:{
          pageSize: 10,
          pageSizeOptions: ['10', '20', '30', '100', '200']
        },
        allMonthAmount: '',
        allCurrentAmount: '',
        tabKey: "1",
        pageName: 'accountReport',
        // 默认索引
        defDataIndex:['rowIndex','action','name','serialNo','initialAmount','thisMonthAmount','currentAmount'],
        // 默认列
        defColumns: [
          {
            dataIndex: 'rowIndex', width:60, align:"center", slots: { title: 'customTitle' },
            customRender:(t,r,index) => {
              return (t !== this.$t('common.total')) ? (parseInt(index) + 1) : t
            }
          },
          { title: this.$t('report.accountFlow'), dataIndex: 'action', align:"center", width: 120,
            scopedSlots: { customRender: 'action' }
          },
          { title: this.$t('common.name'), dataIndex: 'name', width: 150},
          { title: this.$t('common.serialNo'), dataIndex: 'serialNo', width: 150},
          { title: this.$t('report.initialAmount'), dataIndex: 'initialAmount', sorter: (a, b) => a.initialAmount - b.initialAmount, width: 100},
          { title: this.$t('report.monthNetAmount'), dataIndex: 'thisMonthAmount', sorter: (a, b) => a.thisMonthAmount - b.thisMonthAmount, width: 110},
          { title: this.$t('report.currentBalance'), dataIndex: 'currentAmount', sorter: (a, b) => a.currentAmount - b.currentAmount, width: 100}
        ],
        url: {
          list: "/account/listWithBalance",
          getStatistics: "/account/getStatistics"
        }
      }
    },
    created () {
      this.getAccountStatistics()
      this.initColumnsSetting()
    },
    methods: {
      getQueryParams() {
        let param = Object.assign({}, this.queryParam, this.isorter);
        param.field = this.getQueryField();
        param.currentPage = this.ipagination.current;
        param.pageSize = this.ipagination.pageSize;
        return param;
      },
      getAccountStatistics() {
        getAction(this.url.getStatistics, this.queryParam).then((res)=>{
          if(res && res.code === 200) {
            if(res.data){
              this.allMonthAmount = res.data.allMonthAmount
              this.allCurrentAmount = res.data.allCurrentAmount
            }
          }
        })
      },
      searchQuery() {
        this.loadData(1);
        this.getAccountStatistics();
      },
      showAccountInOutList(record) {
        this.$refs.accountInOutList.show(record);
        this.$refs.accountInOutList.title = this.$t('report.accountFlow') + "-" + record.name;
        this.$refs.accountInOutList.disableSubmit = false;
      },
      exportExcel() {
        const params = Object.assign({}, this.queryParam, {currentPage: 1, pageSize: 10000})
        this.loading = true
        getAction(this.url.list, params).then((res) => {
          if(res && res.code === 200) {
            if(res.data.total > 10000) {
              this.$message.warning(this.$t('report.exportLimit'))
              return
            }
            const list = (res.data.rows || []).map(ds => [
              ds.name, ds.serialNo, ds.initialAmount, ds.thisMonthAmount, ds.currentAmount
            ])
            this.handleExportXlsPost(this.$t('report.accountStats'), this.$t('report.accountStats'),
              this.$t('common.name') + ',' + this.$t('report.accountNo') + ',' + this.$t('report.initialAmount') + ',' + this.$t('report.thisMonthNetAmount') + ',' + this.$t('report.currentBalance'), this.$t('report.accountStatsQuery'), list)
          } else {
            this.$message.warning((res && res.data) || this.$t('report.exportFailed'))
          }
        }).finally(() => {
          this.loading = false
        })
      }
    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less'
</style>
