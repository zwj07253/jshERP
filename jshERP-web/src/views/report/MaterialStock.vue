<!-- from 7 5 2 7 1 8 9 2 0 -->
<template>
  <a-row :gutter="24">
    <a-col :md="24">
      <a-card :style="cardStyle" :bordered="false">
        <!-- 查询区域 -->
        <div class="table-page-search-wrapper">
          <a-form layout="inline" @keyup.enter.native="searchQuery">
            <a-row :gutter="24">
              <a-col :md="5" :sm="24">
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
                <a-form-item :label="$t('common.materialInfo')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-input :placeholder="$t('common.materialSearchPlaceholder')" v-model="queryParam.materialParam"></a-input>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24" >
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
              <a-col :md="7" :sm="24">
                <a-form-item>
                  <span v-if="showStockPrice">{{ $t('report.totalStock') }}：{{currentStock}}，{{ $t('report.totalStockAmount') }}：{{currentStockPrice}}，{{ $t('report.totalWeight') }}：{{currentWeight}}</span>
                  <span v-if="!showStockPrice">{{ $t('report.totalStock') }}：{{currentStock}}，{{ $t('report.totalWeight') }}：{{currentWeight}}</span>
                </a-form-item>
              </a-col>
            </a-row>
            <template v-if="toggleSearchStatus">
              <a-row :gutter="24">
                <a-col :md="5" :sm="24">
                  <a-form-item :label="$t('report.category')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-tree-select style="width:100%" :dropdownStyle="{maxHeight:'200px',overflow:'auto'}" allow-clear
                                   :treeData="categoryTree" v-model="queryParam.categoryId" :placeholder="$t('common.selectCategory')">
                    </a-tree-select>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="24">
                  <a-form-item :label="$t('report.position')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-input style="width: 100%" :placeholder="$t('report.positionPlaceholder')" v-model="queryParam.position"></a-input>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="24">
                  <a-form-item :label="$t('report.zeroStock')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-select v-model="queryParam.zeroStock">
                      <a-select-option value="0">{{ $t('report.hide') }}</a-select-option>
                      <a-select-option value="1">{{ $t('report.show') }}</a-select-option>
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
              <a @click="showMaterialInOutList(record)" v-if="showStockPrice">{{record.id?$t('report.flow'):''}}</a>
              <a-divider type="vertical" v-if="showStockPrice" />
              <a @click="showMaterialDepotStockList(record)">{{record.id?$t('report.distribution'):''}}</a>
            </span>
            <template slot="customPic" slot-scope="text, record">
              <a-popover placement="right" trigger="click">
                <template slot="content">
                  <img :src="getImgUrl(record.imgName, record.imgLarge)" width="500px" />
                </template>
                <div class="item-info" v-if="record.imgName">
                  <img v-if="record.imgName" :src="getImgUrl(record.imgName, record.imgSmall)" class="item-img" :title="$t('report.viewLargeImage')" />
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
                :show-total="total => $t('common.totalItems', { total: total })">
                <template slot="buildOptionText" slot-scope="props">
                  <span>{{ props.value }}{{ $t('report.itemsPerPage') }}</span>
                </template>
              </a-pagination>
            </a-col>
          </a-row>
        </section>
        <!-- table区域-end -->
        <material-in-out-list ref="materialInOutList" @ok="modalFormOk"></material-in-out-list>
        <material-depot-stock-list ref="materialDepotStockList" @ok="modalFormOk"></material-depot-stock-list>
      </a-card>
    </a-col>
  </a-row>
</template>
<script>
  import MaterialInOutList from './modules/MaterialInOutList'
  import MaterialDepotStockList from './modules/MaterialDepotStockList'
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import { getAction, getFileAccessHttpUrl } from '@/api/manage'
  import {queryMaterialCategoryTreeList} from '@/api/api'
  import { getMpListShort } from "@/utils/util"
  import JEllipsis from '@/components/jeecg/JEllipsis'
  import moment from 'moment'
  import Vue from 'vue'
  export default {
    name: "MaterialStock",
    mixins:[JeecgListMixin],
    components: {
      MaterialInOutList,
      MaterialDepotStockList,
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
          categoryId: undefined,
          materialParam:'',
          position:'',
          zeroStock: '0',
          mpList: getMpListShort(Vue.ls.get('materialPropertyList'))  //扩展属性
        },
        ipagination:{
          pageSize: 10,
          pageSizeOptions: ['10', '20', '30', '100', '200', '300', '1000', '2000', '3000']
        },
        depotSelected:[],
        depotList: [],
        categoryTree:[],
        currentStock: '',
        currentStockPrice: '',
        currentWeight: '',
        showStockPrice: false,
        pageName: 'materialStock',
        // 默认索引
        defDataIndex:['rowIndex','action','mBarCode','name','standard','model','color','categoryName', 'position','unitName',
          'purchaseDecimal','initialStock','currentStock','currentStockPrice','currentWeight'],
        // 默认列
        defColumns: [
          {
            dataIndex: 'rowIndex', width:40, align:"center", slots: { title: 'customTitle' },
            customRender:(t,r,index) => {
              return (t !== this.$t('common.total')) ? (parseInt(index) + 1) : t
            }
          },
          {title: this.$t('report.stockDetail'), dataIndex: 'action', align:"center", width: 80,
            scopedSlots: { customRender: 'action' }
          },
          {title: this.$t('report.picture'), dataIndex: 'pic', width: 45, scopedSlots: { customRender: 'customPic' }},
          {title: this.$t('common.barcode'), dataIndex: 'mBarCode', width: 100, sorter: (a, b) => a.mBarCode - b.mBarCode},
          {title: this.$t('common.name'), dataIndex: 'name', width: 140, ellipsis:true},
          {title: this.$t('common.specification'), dataIndex: 'standard', width: 100, ellipsis:true},
          {title: this.$t('common.model'), dataIndex: 'model', width: 100, ellipsis:true},
          {title: this.$t('material.color'), dataIndex: 'color', width: 60, ellipsis:true},
          {title: this.$t('common.brand'), dataIndex: 'brand', width: 100, ellipsis:true},
          {title: this.$t('material.manufacturer'), dataIndex: 'mfrs', width: 100, ellipsis:true},
          {title: this.$t('report.category'), dataIndex: 'categoryName', width: 60, ellipsis:true},
          {title: this.$t('common.position'), dataIndex: 'position', width: 60, ellipsis:true},
          {title: this.$t('common.unit'), dataIndex: 'unitName', width: 60, ellipsis:true},
          {title: this.$t('report.costPrice'), dataIndex: 'purchaseDecimal', sorter: (a, b) => a.purchaseDecimal - b.purchaseDecimal, width: 60},
          {title: this.$t('report.initialStock'), dataIndex: 'initialStock', width: 60},
          {title: this.$t('purchase.form.columns.stock'), dataIndex: 'currentStock', sorter: (a, b) => a.currentStock - b.currentStock, width: 60,
            scopedSlots: { customRender: 'customRenderStock' }
          },
          {title: this.$t('report.stockAmount'), dataIndex: 'currentStockPrice', sorter: (a, b) => a.currentStockPrice - b.currentStockPrice, width: 80},
          {title: this.$t('common.weight'), dataIndex: 'currentWeight', sorter: (a, b) => a.currentWeight - b.currentWeight, width: 60}
        ],
        url: {
          list: "/material/getListWithStock"
        }
      }
    },
    created() {
      this.getDepotData()
      this.loadCategoryTreeData()
      this.initColumnsSetting()
    },
    methods: {
      moment,
      getQueryParams() {
        let param = Object.assign({}, this.queryParam, this.isorter);
        if(this.depotSelected && this.depotSelected.length>0) {
          param.depotIds = this.depotSelected.join()
        }
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
      getImgUrl(imgName, type) {
        if(imgName && imgName.split(',')) {
          type = type? type + '/':''
          return getFileAccessHttpUrl('systemConfig/static/' + type + imgName.split(',')[0])
        } else {
          return ''
        }
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
        this.loadData(1);
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
            this.tableAddTotalRow(this.columns, this.dataSource)
            this.currentStock = this.formatNumber(res.data.currentStock)
            this.currentStockPrice = this.formatNumber(res.data.currentStockPrice)
            this.currentWeight = this.formatNumber(res.data.currentWeight)
            this.showStockPrice = res.data.showStockPrice
          } else if(res.code===510){
            this.$message.warning(res.data)
          } else {
            this.$message.warning((res.data && res.data.message) || res.data || this.$t('report.queryFailed'))
          }
          this.loading = false;
        })
      },
      showMaterialInOutList(record) {
        let depotIds = ''
        if(this.depotSelected && this.depotSelected.length>0) {
          depotIds = this.depotSelected.join()
        }
        this.$refs.materialInOutList.show(record, depotIds);
        this.$refs.materialInOutList.title = this.$t('report.viewStockFlow');
        this.$refs.materialInOutList.disableSubmit = false;
      },
      showMaterialDepotStockList(record) {
        let depotIds = ''
        if(this.depotSelected && this.depotSelected.length>0) {
          depotIds = this.depotSelected.join()
        }
        this.$refs.materialDepotStockList.show(record, depotIds);
        this.$refs.materialDepotStockList.title = this.$t('report.viewStockDistribution', { barCode: record.mBarCode, name: record.name });
        this.$refs.materialDepotStockList.disableSubmit = false;
      },
      exportExcel() {
        let headArray = this.defColumns
          .filter(col => col.dataIndex !== 'rowIndex' && col.dataIndex !== 'action' && col.dataIndex !== 'pic')
          .map(col => col.title)
        let head = headArray.join(',')
        let params = this.getQueryParams()
        params.currentPage = 1
        params.pageSize = Math.max(this.ipagination.total || 1, 1)
        this.loading = true
        getAction(this.url.list, params).then(res => {
          if (res.code !== 200) {
            this.$message.warning((res.data && res.data.message) || res.data || this.$t('report.exportFailed'))
            return
          }
          let list = (res.data.rows || []).map(ds => {
            let item = []
            this.defColumns.forEach(col => {
              if (col.dataIndex !== 'rowIndex' && col.dataIndex !== 'action' && col.dataIndex !== 'pic') {
                item.push(ds[col.dataIndex])
              }
            })
            return item
          })
          this.handleExportXlsPost(this.$t('report.productStock'), this.$t('report.productStock'), head, this.$t('report.productStockQuery'), list)
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
</style>
