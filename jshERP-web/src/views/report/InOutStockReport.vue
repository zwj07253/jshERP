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
                <a-form-item :label="$t('common.materialInfo')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-input :placeholder="$t('common.materialInfoPlaceholder')" v-model="queryParam.materialParam"></a-input>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item :label="$t('report.stockCycle')" :labelCol="labelCol" :wrapperCol="wrapperCol">
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
                <span style="float: left;overflow: hidden;" class="table-page-search-submitButtons">
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
                <div class="in-out-stock-summary">
                  <span v-if="showStockPrice">{{ $t('report.totalPeriodStock') }}：{{totalStockStr}}，{{ $t('report.totalPeriodAmount') }}：{{totalCountMoneyStr}}</span>
                  <span v-else>{{ $t('report.totalPeriodStock') }}：{{totalStockStr}}</span>
                </div>
              </a-col>
            </a-row>
            <template v-if="toggleSearchStatus">
              <a-row :gutter="24">
                <a-col :md="6" :sm="24">
                  <a-form-item :label="$t('common.warehouse')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-select
                      mode="multiple" :maxTagCount="1"
                      optionFilterProp="children"
                      showSearch style="width: 100%"
                      :placeholder="$t('common.selectWarehouse')"
                      v-model="depotSelected">
                      <a-select-option v-for="(depot,index) in depotList" :key="index" :value="depot.id">
                        {{ depot.depotName }}
                      </a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="24">
                  <a-form-item :label="$t('report.category')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-tree-select style="width:100%" :dropdownStyle="{maxHeight:'200px',overflow:'auto'}" allow-clear
                                   :treeData="categoryTree" v-model="queryParam.categoryId" :placeholder="$t('common.selectCategory')">
                    </a-tree-select>
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
            <span slot="action" slot-scope="text, record">
              <a v-if="record.rowIndex !== $t('common.total')" @click="showMaterialDepotStockList(record)">{{ $t('report.distribution') }}</a>
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
                        {{ $t('common.restoreColumns') }}：<a-button @click="handleRestDefault" type="link" size="small">{{ $t('common.restoreDefault') }}</a-button>
                      </a-col>
                    </a-row>
                  </a-checkbox-group>
                </template>
                <a-icon type="setting" />
              </a-popover>
            </span>
            <template slot="customPic" slot-scope="text, record">
              <a-popover placement="right" trigger="click">
                <template slot="content">
                  <img :src="getImgUrl(record.imgName, record.imgLarge)" width="500px" />
                </template>
                <div class="item-info" v-if="record.imgName">
                  <img v-if="record.imgName" :src="getImgUrl(record.imgName, record.imgSmall)" class="item-img" :title="$t('common.viewLargerImage')" />
                </div>
              </a-popover>
            </template>
            <template slot="customRenderStock" slot-scope="text, record">
              <a-tooltip :title="record.bigUnitStock">
                {{text}}
              </a-tooltip>
            </template>
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
                :show-total="total => $t('common.totalItems', { total })">
                <template slot="buildOptionText" slot-scope="props">
                  <span>{{ props.value }}{{ $t('report.itemsPerPage') }}</span>
                </template>
              </a-pagination>
            </a-col>
          </a-row>
        </section>
        <!-- table区域-end -->
        <material-depot-stock-list-with-time ref="materialDepotStockListWithTime" @ok="modalFormOk"></material-depot-stock-list-with-time>
      </a-card>
    </a-col>
  </a-row>
</template>
<script>
  import MaterialDepotStockListWithTime from './modules/MaterialDepotStockListWithTime'
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import { getAction, getFileAccessHttpUrl } from '@/api/manage'
  import {queryMaterialCategoryTreeList} from '@/api/api'
  import { getFormatDate, getMpListShort, getPrevMonthFormatDate } from '@/utils/util'
  import JEllipsis from '@/components/jeecg/JEllipsis'
  import moment from 'moment'
  import Vue from 'vue'
  export default {
    name: "InOutStockReport",
    mixins:[JeecgListMixin],
    components: {
      MaterialDepotStockListWithTime,
      JEllipsis
    },
    data () {
      return {
        // 查询条件
        currentMonth: moment().format('YYYY-MM'),
        monthFormat: 'YYYY-MM',
        labelCol: {
          span: 5
        },
        wrapperCol: {
          span: 18,
          offset: 1
        },
        queryParam: {
          depotId: undefined,
          beginTime: getPrevMonthFormatDate(1),
          endTime: getFormatDate(),
          createTimeRange: [moment(getPrevMonthFormatDate(1)), moment(getFormatDate())],
          materialParam:'',
          categoryId: undefined,
          mpList: getMpListShort(Vue.ls.get('materialPropertyList'))  //扩展属性
        },
        ipagination:{
          pageSize: 10,
          pageSizeOptions: ['10', '20', '30', '100', '200']
        },
        depotSelected:[],
        depotList: [],
        categoryTree:[],
        totalStockStr: '0',
        totalCountMoneyStr: '0',
        showStockPrice: false,
        pageName: 'inOutStockReport',
        // 默认索引
        defDataIndex:['rowIndex','action','barCode','materialName','materialStandard','materialModel','unitName','unitPrice',
          'prevSum','inSum','outSum','thisSum','thisAllPrice'],
        // 默认列
        defColumns: [
          {
            dataIndex: 'rowIndex', width:40, align:"center", slots: { title: 'customTitle' },
            customRender:(t,r,index) => {
              return (t !== this.$t('common.total')) ? (parseInt(index) + 1) : t
            }
          },
          {title: this.$t('report.stockDetail'), dataIndex: 'action', align:"center", width: 60,
            scopedSlots: { customRender: 'action' }
          },
          {title: this.$t('report.picture'), dataIndex: 'pic', width: 45, scopedSlots: { customRender: 'customPic' }},
          {title: this.$t('common.barcode'), dataIndex: 'barCode', sorter: true, width: 100},
          {title: this.$t('common.name'), dataIndex: 'materialName', width: 120, ellipsis:true},
          {title: this.$t('common.specification'), dataIndex: 'materialStandard', width: 80, ellipsis:true},
          {title: this.$t('common.model'), dataIndex: 'materialModel', width: 80, ellipsis:true},
          {title: this.$t('material.color'), dataIndex: 'materialColor', width: 50, ellipsis:true},
          {title: this.$t('common.brand'), dataIndex: 'materialBrand', width: 80, ellipsis:true},
          {title: this.$t('material.manufacturer'), dataIndex: 'materialMfrs', width: 80, ellipsis:true},
          {title: this.$t('purchase.form.columns.ext1'), dataIndex: 'otherField1', width: 50, ellipsis:true},
          {title: this.$t('purchase.form.columns.ext2'), dataIndex: 'otherField2', width: 50, ellipsis:true},
          {title: this.$t('purchase.form.columns.ext3'), dataIndex: 'otherField3', width: 50, ellipsis:true},
          {title: this.$t('common.unit'), dataIndex: 'unitName', width: 60, ellipsis:true},
          {title: this.$t('report.costPrice'), dataIndex: 'unitPrice', sorter: true, width: 60},
          {title: this.$t('report.previousBalance'), dataIndex: 'prevSum', sorter: true, width: 80},
          {title: this.$t('report.inboundQty'), dataIndex: 'inSum', sorter: true, width: 60},
          {title: this.$t('report.outboundQty'), dataIndex: 'outSum', sorter: true, width: 60},
          {title: this.$t('report.currentBalanceQty'), dataIndex: 'thisSum', sorter: true, width: 80,
            scopedSlots: { customRender: 'customRenderStock' }
          },
          {title: this.$t('report.balanceAmount'), dataIndex: 'thisAllPrice', sorter: true, width: 60}
        ],
        url: {
          list: "/depotItem/getInOutStock",
          totalCountMoney: "/depotItem/getInOutStockCountMoney"
        }
      }
    },
    created() {
      this.getDepotData()
      this.loadTreeData()
      this.getTotalCountMoney()
      this.initColumnsSetting()
      this.handleChangeOtherField(0)
    },
    computed: {
      displayDataSource() {
        const rows = (this.dataSource || []).slice()
        if (!rows.length) {
          return rows
        }
        const totalRow = {
          id: `in-out-stock-total-${this.ipagination.current}`,
          rowIndex: this.$t('common.total')
        }
        const numericFields = ['prevSum', 'inSum', 'outSum', 'thisSum', 'thisAllPrice']
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
      moment,
      getQueryParams() {
        let param = Object.assign({}, this.queryParam, this.isorter);
        if(this.depotSelected && this.depotSelected.length>0) {
          param.depotIds = this.depotSelected.join()
        }
        param.monthTime = this.queryParam.monthTime;
        param.field = this.getQueryField();
        param.currentPage = this.ipagination.current;
        param.pageSize = this.ipagination.pageSize;
        return param;
      },
      onDateChange: function (value, dateString) {
        this.queryParam.beginTime=dateString[0]
        this.queryParam.endTime=dateString[1]
        if(dateString[0] && dateString[1]) {
          this.queryParam.createTimeRange = [moment(dateString[0]), moment(dateString[1])]
        }
      },
      getDepotData() {
        getAction('/depot/findDepotByCurrentUser').then((res)=>{
          if(res.code === 200){
            this.depotList = res.data;
          }else{
            this.$message.info(res.data);
          }
        })
      },
      getTotalCountMoney(){
        let param = Object.assign({}, this.queryParam, this.isorter);
        if(this.depotSelected && this.depotSelected.length>0) {
          param.depotIds = this.depotSelected.join()
        }
        param.monthTime = this.queryParam.monthTime;
        getAction(this.url.totalCountMoney, param).then((res)=>{
          if(res && res.code === 200) {
            this.totalStockStr = this.formatNumber(res.data.totalStock)
            this.totalCountMoneyStr = this.formatNumber(res.data.totalCount)
            this.showStockPrice = res.data.showStockPrice
          } else {
            this.$message.warning((res && res.data && res.data.message) || (res && res.data) || this.$t('report.queryFailed'))
          }
        })
      },
      onChange: function (value, dateString) {
        console.log(dateString);
        this.queryParam.monthTime=dateString;
      },
      getImgUrl(imgName, type) {
        if(imgName && imgName.split(',')) {
          type = type? type + '/':''
          return getFileAccessHttpUrl('systemConfig/static/' + type + imgName.split(',')[0])
        } else {
          return ''
        }
      },
      loadTreeData(){
        let that = this;
        let params = {};
        params.id='';
        queryMaterialCategoryTreeList(params).then((res)=>{
          if(res){
            that.categoryTree = [];
            for (let i = 0; i < res.length; i++) {
              let temp = res[i];
              that.categoryTree.push(temp);
            }
          }
        })
      },
      searchQuery() {
        if(this.queryParam.beginTime === '' || this.queryParam.endTime === ''){
          this.$message.warning(this.$t('report.selectStockCycle'))
        } else {
          this.loadData(1);
          this.getTotalCountMoney();
        }
      },
      showMaterialDepotStockList(record) {
        let depotIds = ''
        if(this.depotSelected && this.depotSelected.length>0) {
          depotIds = this.depotSelected.join()
        }
        this.$refs.materialDepotStockListWithTime.show(record, depotIds, this.queryParam.beginTime, this.queryParam.endTime);
        this.$refs.materialDepotStockListWithTime.title = `${this.$t('report.viewStockDistribution')}（${this.$t('common.barCode')}：${record.barCode}，${this.$t('common.materialName')}：${record.materialName}）`;
        this.$refs.materialDepotStockListWithTime.disableSubmit = false;
      },
      exportExcel() {
        if ((this.ipagination.total || 0) > 10000) {
          this.$message.warning(this.$t('report.exportLimit'))
          return
        }
        const params = this.getQueryParams()
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
        let mpStr = getMpListShort(Vue.ls.get('materialPropertyList'))
        let head = this.$t('common.barCode') + ',' + this.$t('common.materialName') + ',' + this.$t('common.materialStandard') + ',' + this.$t('common.materialModel') + ',' + this.$t('common.materialColor') + ',' + this.$t('common.materialBrand') + ',' + this.$t('common.materialMfrs') + ',' + mpStr + ',' + this.$t('common.unitName') + ',' + this.$t('common.costPrice') + ',' + this.$t('report.prevStockQty') + ',' + this.$t('report.inQty') + ',' + this.$t('report.outQty') + ',' + this.$t('report.currentStockQty') + ',' + this.$t('report.stockAmount')
        for (let i = 0; i < dataSource.length; i++) {
          let item = []
          let ds = dataSource[i]
          item.push(ds.barCode, ds.materialName, ds.materialStandard, ds.materialModel, ds.materialColor, ds.materialBrand,
            ds.materialMfrs, ds.otherField1, ds.otherField2, ds.otherField3, ds.unitName, ds.unitPrice,
            ds.prevSum, ds.inSum, ds.outSum, ds.thisSum, ds.thisAllPrice)
          list.push(item)
        }
        let tip = this.$t('report.stockCycle') + '：' + this.queryParam.beginTime + '~' + this.queryParam.endTime
        this.handleExportXlsPost(this.$t('report.inOutStockStats'), this.$t('report.inOutStockStats'), head, tip, list)
      }
    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less'
</style>
<style scoped>
  .item-info {
    float:left;
    width:38px;
    height:38px;
    margin-left:6px
  }
  .item-img {
    cursor:pointer;
    position: static;
    display: block;
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
  .in-out-stock-summary {
    display: flex;
    min-height: 32px;
    align-items: center;
    justify-content: flex-end;
    text-align: right;
    overflow-wrap: anywhere;
  }
</style>
