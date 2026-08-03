<!-- gitee 7 5 2 7 1 8 9 2 0 -->
<template>
  <a-row :gutter="24">
    <a-col :md="24">
      <a-card :style="cardStyle" :bordered="false">
        <!-- 查询区域 -->
        <div class="table-page-search-wrapper">
          <a-form layout="inline" @keyup.enter.native="searchQuery">
            <a-row :gutter="24">
              <a-col :md="6" :sm="24">
                <a-form-item :label="$t('common.warehouse')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-select
                    optionFilterProp="children"
                    showSearch allow-clear style="width: 100%"
                    :placeholder="$t('common.selectWarehouse')"
                    v-model="queryParam.depotId">
                    <a-select-option v-for="(depot,index) in depotList" :value="depot.id" :key="index">
                      {{ depot.depotName }}
                    </a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item :label="$t('common.materialInfo')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-input :placeholder="$t('common.materialInputPlaceholder')" v-model="queryParam.materialParam"></a-input>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item :label="$t('report.productCategory')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-tree-select style="width:100%" :dropdownStyle="{maxHeight:'200px',overflow:'auto'}" allow-clear
                                 :treeData="categoryTree" v-model="queryParam.categoryId" :placeholder="$t('common.selectCategory')">
                  </a-tree-select>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <span style="float: left;overflow: hidden;" class="table-page-search-submitButtons">
                  <a-button type="primary" @click="searchQuery">{{ $t('common.search') }}</a-button>
                  <a-button style="margin-left: 8px" v-print="'#reportPrint'" icon="printer">{{ $t('common.print') }}</a-button>
                  <a-button style="margin-left: 8px" @click="exportExcel" icon="download">{{ $t('common.export') }}</a-button>
                </span>
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
            rowKey="warningKey"
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
                        {{ $t('common.restoreColumns') }}<a-button @click="handleRestDefault" type="link" size="small">{{ $t('common.restoreDefault') }}</a-button>
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
                :show-total="total => $t('common.total', { total })">
                <template slot="buildOptionText" slot-scope="props">
                  <span>{{ props.value }}{{ $t('common.itemsPerPage') }}</span>
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
  import JEllipsis from '@/components/jeecg/JEllipsis'
  import {getAction} from '@/api/manage'
  import { queryMaterialCategoryTreeList } from '@/api/api'
  import { getMpListShort } from "@/utils/util"
  import Vue from 'vue'
  export default {
    name: "StockWarningReport",
    mixins:[JeecgListMixin],
    components: {
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
          materialParam:'',
          depotId: undefined,
          mpList: getMpListShort(Vue.ls.get('materialPropertyList'))  //扩展属性
        },
        ipagination:{
          pageSize: 10,
          pageSizeOptions: ['10', '20', '30', '100', '200']
        },
        depotList: [],
        categoryTree:[],
        tabKey: "1",
        pageName: 'stockWarningReport',
        // 默认索引
        defDataIndex:['rowIndex','depotName','barCode','mname','mstandard','mmodel','materialUnit','currentNumber',
          'lowSafeStock','highSafeStock','lowCritical','highCritical'],
        // 默认列
        defColumns: [
          {
            dataIndex: 'rowIndex', width:40, align:"center", slots: { title: 'customTitle' },
            customRender:(t,r,index) => {
              return (t !== this.$t('common.total')) ? (parseInt(index) + 1) : t
            }
          },
          {title: this.$t('system.depot'), dataIndex: 'depotName', width: 100, ellipsis:true},
          {title: this.$t('common.barcode'), dataIndex: 'barCode', sorter: true, width: 100},
          {title: this.$t('common.name'), dataIndex: 'mname', width: 100, ellipsis:true},
          {title: this.$t('common.specification'), dataIndex: 'mstandard', width: 80, ellipsis:true},
          {title: this.$t('common.model'), dataIndex: 'mmodel', width: 80, ellipsis:true},
          {title: this.$t('material.color'), dataIndex: 'mcolor', width: 50, ellipsis:true},
          {title: this.$t('common.brand'), dataIndex: 'brand', width: 80, ellipsis:true},
          {title: this.$t('material.manufacturer'), dataIndex: 'mmfrs', width: 80, ellipsis:true},
          {title: this.$t('purchase.form.columns.ext1'), dataIndex: 'motherField1', width: 80, ellipsis:true},
          {title: this.$t('purchase.form.columns.ext2'), dataIndex: 'motherField2', width: 80, ellipsis:true},
          {title: this.$t('purchase.form.columns.ext3'), dataIndex: 'motherField3', width: 80, ellipsis:true},
          {title: this.$t('common.unit'), dataIndex: 'materialUnit', width: 60, ellipsis:true},
          {title: this.$t('purchase.form.columns.stock'), dataIndex: 'currentNumber', sorter: true, width: 80},
          {title: this.$t('report.minSafetyStock'), dataIndex: 'lowSafeStock', sorter: true, width: 100},
          {title: this.$t('report.maxSafetyStock'), dataIndex: 'highSafeStock', sorter: true, width: 100},
          {title: this.$t('report.suggestInbound'), dataIndex: 'lowCritical', sorter: true, width: 80},
          {title: this.$t('report.suggestOutbound'), dataIndex: 'highCritical', sorter: true, width: 80}
        ],
        url: {
          list: "/depotItem/findStockWarningCount"
        }
      }
    },
    created () {
      this.getDepotData()
      this.loadCategoryTreeData()
      this.initColumnsSetting()
      this.handleChangeOtherField(0)
    },
    methods: {
      getQueryParams() {
        let param = Object.assign({}, this.queryParam, this.isorter);
        param.field = this.getQueryField();
        param.currentPage = this.ipagination.current;
        param.pageSize = this.ipagination.pageSize;
        return param;
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
      //动态替换扩展字段
      handleChangeOtherField(showQuery) {
        let mpStr = getMpListShort(Vue.ls.get('materialPropertyList'))
        if(mpStr) {
          let mpArr = mpStr.split(',')
          if(mpArr.length ===3) {
            if(showQuery) {
              this.queryTitle.mp1 = mpArr[0]
              this.queryTitle.mp2 = mpArr[1]
              this.queryTitle.mp3 = mpArr[2]
            }
            for (let i = 0; i < this.defColumns.length; i++) {
              if(this.defColumns[i].dataIndex === 'motherField1') {
                this.defColumns[i].title = mpArr[0]
              }
              if(this.defColumns[i].dataIndex === 'motherField2') {
                this.defColumns[i].title = mpArr[1]
              }
              if(this.defColumns[i].dataIndex === 'motherField3') {
                this.defColumns[i].title = mpArr[2]
              }
            }
          }
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
        let head = [this.$t('common.warehouse'), this.$t('report.barCode'), this.$t('report.name'), this.$t('report.specification'), this.$t('report.model'),
          this.$t('report.color'), this.$t('report.brand'), this.$t('report.manufacturer')].join(',') + ',' + mpStr + ',' +
          [this.$t('report.unit'), this.$t('report.stock'), this.$t('report.lowSafeStock'), this.$t('report.highSafeStock'),
          this.$t('report.suggestedInbound'), this.$t('report.suggestedOutbound')].join(',')
        for (let i = 0; i < dataSource.length; i++) {
          let item = []
          let ds = dataSource[i]
          item.push(ds.depotName, ds.barCode, ds.mname, ds.mstandard, ds.mmodel, ds.mcolor, ds.brand, ds.mmfrs,
            ds.motherField1, ds.motherField2, ds.motherField3, ds.materialUnit, ds.currentNumber, ds.lowSafeStock, ds.highSafeStock, ds.lowCritical, ds.highCritical)
          list.push(item)
        }
        let tip = this.$t('report.stockWarningQuery')
        this.handleExportXlsPost(this.$t('report.stockWarning'), this.$t('report.stockWarning'), head, tip, list)
      }
    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less'
</style>
