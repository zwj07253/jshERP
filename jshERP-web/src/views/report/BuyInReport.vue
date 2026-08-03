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
                <a-form-item :label="$t('common.billDate')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-range-picker
                    style="width: 100%"
                    v-model="queryParam.createTimeRange"
                    format="YYYY-MM-DD"
                    :placeholder="[$t('common.startDate'), $t('common.endDate')]"
                    @change="onDateChange"
                  />
                </a-form-item>
              </a-col>
              <a-col :md="8" :sm="24">
                <span style="float: left;overflow: hidden;white-space: nowrap;" class="table-page-search-submitButtons">
                  <a-button type="primary" @click="searchQuery">{{ $t('common.search') }}</a-button>
                  <a-button style="margin-left: 8px" v-print="'#reportPrint'" icon="printer">{{ $t('common.print') }}</a-button>
                  <a-button style="margin-left: 8px" @click="exportExcel" icon="download">{{ $t('common.export') }}</a-button>
                  <a @click="handleToggleSearch" style="margin-left: 8px">
                    {{ toggleSearchStatus ? $t('common.collapse') : $t('common.expand') }}
                    <a-icon :type="toggleSearchStatus ? 'up' : 'down'"/>
                  </a>
                </span>
              </a-col>
              <a-col :md="4" :sm="24">
                <a-form-item>
                  <span style="white-space: nowrap;">{{ $t('report.actualPurchaseAmount') }}：{{realityPriceTotal}}</span>
                </a-form-item>
              </a-col>
            </a-row>
            <template v-if="toggleSearchStatus">
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
                  <a-form-item :label="$t('common.warehouse')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-select
                      optionFilterProp="children"
                      :dropdownMatchSelectWidth="false"
                      showSearch allow-clear style="width: 100%"
                      :placeholder="$t('common.selectWarehouse')"
                      v-model="queryParam.depotId">
                      <a-select-option v-for="(depot,index) in depotList" :value="depot.id" :key="index">
                        {{ depot.depotName }}
                      </a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="24" v-if="orgaTree.length">
                  <a-form-item :label="$t('report.dept')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-tree-select style="width:100%" allow-clear :treeData="orgaTree"
                                   v-model="queryParam.organizationId" :placeholder="$t('common.selectDept')">
                    </a-tree-select>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="24">
                  <a-form-item :label="$t('report.productCategory')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-tree-select style="width:100%" :dropdownStyle="{maxHeight:'200px',overflow:'auto'}" allow-clear
                                   :treeData="categoryTree" v-model="queryParam.categoryId" :placeholder="$t('common.selectProductCategory')">
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
      </a-card>
    </a-col>
  </a-row>
</template>
<script>
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import { getMpListShort, getPrevMonthFormatDate, getFormatDate } from '@/utils/util'
  import {getAction} from '@/api/manage'
  import {findBySelectSup, queryMaterialCategoryTreeList, getAllOrganizationTreeByUser} from '@/api/api'
  import JEllipsis from '@/components/jeecg/JEllipsis'
  import moment from 'moment'
  import Vue from 'vue'
  export default {
    name: "BuyInReport",
    mixins:[JeecgListMixin],
    components: {
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
        queryParam: {
          materialParam:'',
          beginTime: getPrevMonthFormatDate(3),
          endTime: getFormatDate(),
          createTimeRange: [moment(getPrevMonthFormatDate(3)), moment(getFormatDate())],
          organId: undefined,
          depotId: undefined,
          organizationId: undefined,
          mpList: getMpListShort(Vue.ls.get('materialPropertyList'))
        },
        ipagination:{
          pageSize: 10,
          pageSizeOptions: ['10', '20', '30', '100', '200']
        },
        supList: [],
        depotList: [],
        orgaTree: [],
        categoryTree:[],
        realityPriceTotal: '',
        setTimeFlag: null,
        tabKey: "1",
        pageName: 'buyInReport',
        // 默认索引
        defDataIndex:['rowIndex','barCode','materialName','materialStandard','materialModel','materialUnit',
          'inSum','inSumPrice','outSum','outSumPrice','inOutSumPrice'],
        // 默认列
        defColumns: [
          {
            dataIndex: 'rowIndex', width:40, align:"center", slots: { title: 'customTitle' },
            customRender:(t,r,index) => {
              return (t !== this.$t('common.total')) ? (parseInt(index) + 1) : t
            }
          },
          {title: this.$t('common.barcode'), dataIndex: 'barCode', sorter: (a, b) => a.barCode - b.barCode, width: 160},
          {title: this.$t('common.name'), dataIndex: 'materialName', width: 160, ellipsis:true},
          {title: this.$t('common.specification'), dataIndex: 'materialStandard', width: 80, ellipsis:true},
          {title: this.$t('common.model'), dataIndex: 'materialModel', width: 80, ellipsis:true},
          {title: this.$t('material.color'), dataIndex: 'materialColor', width: 60, ellipsis:true},
          {title: this.$t('common.brand'), dataIndex: 'materialBrand', width: 80, ellipsis:true},
          {title: this.$t('material.manufacturer'), dataIndex: 'materialMfrs', width: 80, ellipsis:true},
          {title: this.$t('purchase.form.columns.ext1'), dataIndex: 'otherField1', width: 80, ellipsis:true},
          {title: this.$t('purchase.form.columns.ext2'), dataIndex: 'otherField2', width: 80, ellipsis:true},
          {title: this.$t('purchase.form.columns.ext3'), dataIndex: 'otherField3', width: 80, ellipsis:true},
          {title: this.$t('common.unit'), dataIndex: 'materialUnit', width: 80, ellipsis:true},
          {title: this.$t('report.purchaseQty'), dataIndex: 'inSum', sorter: (a, b) => a.inSum - b.inSum, width: 80},
          {title: this.$t('report.purchaseAmount'), dataIndex: 'inSumPrice', sorter: (a, b) => a.inSumPrice - b.inSumPrice, width: 80},
          {title: this.$t('report.returnQty'), dataIndex: 'outSum', sorter: (a, b) => a.outSum - b.outSum, width: 80},
          {title: this.$t('report.returnAmount'), dataIndex: 'outSumPrice', sorter: (a, b) => a.outSumPrice - b.outSumPrice, width: 80},
          {title: this.$t('report.actualPurchaseAmount'), dataIndex: 'inOutSumPrice', sorter: (a, b) => a.inOutSumPrice - b.inOutSumPrice, width: 100}
        ],
        url: {
          list: "/depotItem/buyIn"
        }
      }
    },
    created () {
      this.initSupplier()
      this.getDepotData()
      this.loadAllOrgaData()
      this.loadCategoryTreeData()
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
          id: `buy-total-${this.ipagination.current}`,
          rowIndex: this.$t('common.total')
        }
        const numericFields = ['inSum', 'inSumPrice', 'outSum', 'outSumPrice', 'inOutSumPrice']
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
            this.realityPriceTotal = res.data.realityPriceTotal
          } else if(res.code===510){
            this.$message.warning(res.data)
          } else {
            this.$message.warning(typeof res.data === 'string' ? res.data : res.data.message)
          }
          this.loading = false;
        })
      },
      initSupplier() {
        let that = this;
        findBySelectSup({limit:1}).then((res)=>{
          if(res) {
            that.supList = res
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
      getDepotData() {
        getAction('/depot/findDepotByCurrentUser').then((res)=>{
          if(res.code === 200){
            this.depotList = res.data;
          }else{
            this.$message.info(res.data);
          }
        })
      },
      loadAllOrgaData(){
        let that = this
        let params = {}
        getAllOrganizationTreeByUser(params).then((res)=>{
          if(res){
            that.orgaTree = res
          }
        })
      },
      loadCategoryTreeData(){
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
        if(this.queryParam.beginTime == '' || this.queryParam.endTime == ''){
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
        let mpStr = getMpListShort(Vue.ls.get('materialPropertyList'))
        let head = this.$t('common.barCode') + ',' + this.$t('common.name') + ',' + this.$t('common.spec') + ',' + this.$t('common.model') + ',' +
          this.$t('common.color') + ',' + this.$t('common.brand') + ',' + this.$t('common.manufacturer') + ',' + mpStr + ',' +
          this.$t('common.unit') + ',' + this.$t('report.purchaseQty') + ',' + this.$t('report.purchaseAmount') + ',' +
          this.$t('report.returnQty') + ',' + this.$t('report.returnAmount') + ',' + this.$t('report.actualPurchaseAmount')
        for (let i = 0; i < dataSource.length; i++) {
          let item = []
          let ds = dataSource[i]
          item.push(ds.barCode, ds.materialName, ds.materialStandard, ds.materialModel, ds.materialColor, ds.materialBrand,
            ds.materialMfrs, ds.otherField1, ds.otherField2, ds.otherField3, ds.materialUnit, ds.inSum, ds.inSumPrice, ds.outSum, ds.outSumPrice, ds.inOutSumPrice)
          list.push(item)
        }
        let tip = this.$t('common.billDate') + '：' + this.queryParam.beginTime + '~' + this.queryParam.endTime
        this.handleExportXlsPost(this.$t('report.purchaseStats'), this.$t('report.purchaseStats'), head, tip, list)
      }
    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less'
</style>
