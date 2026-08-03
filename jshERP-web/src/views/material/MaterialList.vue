<!-- by 7527189 2 0 -->
<template>
  <a-row :gutter="24">
    <a-col :md="24">
      <a-card :style="cardStyle" :bordered="false">
        <!-- 查询区域 -->
        <div class="table-page-search-wrapper">
          <!-- 搜索区域 -->
          <a-form layout="inline" @keyup.enter.native="searchQuery">
            <a-row :gutter="24">
              <a-col :md="6" :sm="24">
                <a-form-item :label="$t('common.category')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-tree-select style="width:100%" :dropdownStyle="{maxHeight:'200px',overflow:'auto'}" allow-clear
                   :treeData="categoryTree" v-model="queryParam.categoryId" :placeholder="$t('material.selectCategory')">
                  </a-tree-select>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item :label="$t('common.name')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-input :placeholder="$t('common.enterMaterial')" v-model="queryParam.materialParam"></a-input>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item :label="$t('common.specification')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-input :placeholder="$t('common.specification')" v-model="queryParam.standard"></a-input>
                </a-form-item>
              </a-col>
              <span style="float: left;overflow: hidden;" class="table-page-search-submitButtons">
                <a-col :md="6" :sm="24">
                  <a-button type="primary" @click="searchQuery">{{ $t('common.search') }}</a-button>
                  <a-button style="margin-left: 8px" @click="searchReset">{{ $t('common.reset') }}</a-button>
                  <a @click="handleToggleSearch" style="margin-left: 8px">
                    {{ toggleSearchStatus ? $t('common.collapse') : $t('common.expand') }}
                    <a-icon :type="toggleSearchStatus ? 'up' : 'down'"/>
                  </a>
                </a-col>
              </span>
            </a-row>
            <template v-if="toggleSearchStatus">
              <a-row :gutter="24">
                <a-col :md="6" :sm="24">
                  <a-form-item :label="$t('common.model')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-input :placeholder="$t('common.enterMaterial')" v-model="queryParam.model"></a-input>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="24">
                  <a-form-item :label="$t('material.color')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-input :placeholder="$t('material.color')" v-model="queryParam.color"></a-input>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="24">
                  <a-form-item :label="$t('common.brand')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-input :placeholder="$t('common.enterMaterial')" v-model="queryParam.brand"></a-input>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="24">
                  <a-form-item :label="$t('material.manufacturer')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-input :placeholder="$t('material.manufacturer')" v-model="queryParam.mfrs"></a-input>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="24">
                  <a-form-item :label="queryTitle.mp1" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-input :placeholder="$t('material.enterFieldQuery', {field: queryTitle.mp1})" v-model="queryParam.otherField1"></a-input>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="24">
                  <a-form-item :label="queryTitle.mp2" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-input :placeholder="$t('material.enterFieldQuery', {field: queryTitle.mp2})" v-model="queryParam.otherField2"></a-input>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="24">
                  <a-form-item :label="queryTitle.mp3" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-input :placeholder="$t('material.enterFieldQuery', {field: queryTitle.mp3})" v-model="queryParam.otherField3"></a-input>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="24">
                  <a-form-item :label="$t('common.status')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-select :placeholder="$t('common.selectStatus')" v-model="queryParam.enabled">
                      <a-select-option value="1">{{ $t('common.enable') }}</a-select-option>
                      <a-select-option value="0">{{ $t('common.disable') }}</a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="24">
                  <a-form-item :label="$t('purchase.form.columns.serialNumber')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-select :placeholder="$t('material.hasOrNotSN')" v-model="queryParam.enableSerialNumber">
                      <a-select-option value="1">{{ $t('common.yes') }}</a-select-option>
                      <a-select-option value="0">{{ $t('common.no') }}</a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="24">
                  <a-form-item :label="$t('purchase.form.columns.batchNumber')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-select :placeholder="$t('material.hasOrNotBatch')" v-model="queryParam.enableBatchNumber">
                      <a-select-option value="1">{{ $t('common.yes') }}</a-select-option>
                      <a-select-option value="0">{{ $t('common.no') }}</a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="24">
                  <a-form-item :label="$t('common.position')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-input style="width: 100%" :placeholder="$t('material.enterFieldQuery', {field: $t('common.position')})" v-model="queryParam.position"></a-input>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="24">
                  <a-form-item :label="$t('material.weight')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-input-number style="width: 100%" :placeholder="$t('material.enterWeightQuery')" v-model="queryParam.weight"></a-input-number>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="24">
                  <a-form-item :label="$t('material.expiryNum')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-input-number style="width: 100%" :placeholder="$t('material.enterExpiryQuery')" v-model="queryParam.expiryNum"></a-input-number>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="24">
                  <a-form-item :label="$t('common.remark')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-input :placeholder="$t('common.enterRemark')" v-model="queryParam.remark"></a-input>
                  </a-form-item>
                </a-col>
              </a-row>
            </template>
          </a-form>
        </div>
        <!-- 操作按钮区域 -->
        <div class="table-operator"  style="margin-top: 5px">
          <a-button v-if="btnEnableList.indexOf(1)>-1" @click="handleAdd" type="primary" icon="plus">{{ $t('common.add') }}</a-button>
          <a-button v-if="btnEnableList.indexOf(1)>-1" @click="batchDel" icon="delete">{{ $t('common.delete') }}</a-button>
          <a-button v-if="btnEnableList.indexOf(1)>-1" @click="batchSetStatus(true)" icon="check-square">{{ $t('common.enable') }}</a-button>
          <a-button v-if="btnEnableList.indexOf(1)>-1" @click="batchSetStatus(false)" icon="close-square">{{ $t('common.disable') }}</a-button>
          <a-button v-if="btnEnableList.indexOf(1)>-1" @click="handleImportXls()" icon="import">{{ $t('common.import') }}</a-button>
          <a-button v-if="btnEnableList.indexOf(3)>-1" @click="handleExportXls($t('material.productInfo'))" icon="download">{{ $t('common.export') }}</a-button>
          <a-button v-if="btnEnableList.indexOf(1)>-1" @click="batchEdit()" icon="edit">{{ $t('material.batchSetInfo') }}</a-button>
          <a-button v-if="btnEnableList.indexOf(1)>-1" @click="batchSetMaterialCurrentStock()" icon="stock">{{ $t('material.batchSetStock') }}</a-button>
          <a-button v-if="btnEnableList.indexOf(1)>-1" @click="batchSetMaterialCurrentUnitPrice()" icon="fund">{{ $t('material.batchSetPrice') }}</a-button>
          <a-popover trigger="click" placement="right">
            <template slot="content">
              <a-checkbox-group @change="onColChange" v-model="settingDataIndex" :defaultValue="settingDataIndex">
                <a-row style="width: 500px">
                  <template v-for="(item,index) in defColumns">
                    <template>
                      <a-col :span="8">
                        <a-checkbox :value="item.dataIndex">
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
            <a-button icon="setting">{{ $t('common.columnSettings') }}</a-button>
          </a-popover>
        </div>
        <!-- table区域-begin -->
        <div>
          <a-table
            ref="table"
            size="middle"
            bordered
            rowKey="id"
            :columns="columns"
            :dataSource="dataSource"
            :components="handleDrag(columns)"
            :pagination="ipagination"
            :scroll="scroll"
            :loading="loading"
            :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange, columnWidth:'40px'}"
            @change="handleTableChange">
            <span slot="action" slot-scope="text, record">
              <a @click="handleEdit(record)">{{ $t('common.edit') }}</a>
              <a-divider v-if="btnEnableList.indexOf(1)>-1" type="vertical" />
              <a v-if="btnEnableList.indexOf(1)>-1" @click="handleCopyAdd(record)">{{ $t('common.copy') }}</a>
            </span>
            <template slot="customPic" slot-scope="text, record">
              <a-popover placement="right" trigger="click">
                <template slot="content">
                  <img :src="getImgUrl(record.imgName, record.imgLarge)" width="500px" />
                </template>
                <div class="item-info" v-if="record.imgName">
                  <img v-if="record.imgName" :src="getImgUrl(record.imgName, record.imgSmall)" class="item-img" :title="$t('common.view')" />
                </div>
              </a-popover>
            </template>
            <template slot="customBarCode" slot-scope="text, record">
              {{record.mBarCode}}
            </template>
            <template slot="customName" slot-scope="text, record">
              {{record.name}}
              <a-tag v-if="record.enableSerialNumber==1" color="orange">{{ $t('material.serialAbbrev') }}</a-tag>
              <a-tag v-if="record.enableBatchNumber==1" color="orange">{{ $t('material.batchAbbrev') }}</a-tag>
            </template>
            <template slot="customRenderInitialStock" slot-scope="text, record">
              <a-tooltip :title="record.bigUnitInitialStock">
                {{text}}
              </a-tooltip>
            </template>
            <template slot="customRenderStock" slot-scope="text, record">
              <a-tooltip :title="record.bigUnitStock">
                {{text}}
              </a-tooltip>
            </template>
            <template slot="customRenderEnabled" slot-scope="enabled">
              <a-tag v-if="enabled" color="green">{{ $t('common.enabled') }}</a-tag>
              <a-tag v-if="!enabled" color="orange">{{ $t('common.disabled') }}</a-tag>
            </template>
          </a-table>
        </div>
        <!-- table区域-end -->
        <!-- 表单区域 -->
        <material-modal ref="modalForm" @ok="modalFormOk"></material-modal>
        <import-file-modal ref="modalImportForm" @ok="modalFormOk"></import-file-modal>
        <batch-set-info-modal ref="batchSetInfoModalForm" @ok="modalFormOk"></batch-set-info-modal>
      </a-card>
    </a-col>
  </a-row>
</template>
<script>
  import MaterialModal from './modules/MaterialModal'
  import ImportFileModal from '@/components/tools/ImportFileModal'
  import BatchSetInfoModal from './modules/BatchSetInfoModal'
  import { queryMaterialCategoryTreeList } from '@/api/api'
  import { postAction, getFileAccessHttpUrl } from '@/api/manage'
  import { getMpListShort } from '@/utils/util'
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import JEllipsis from '@/components/jeecg/JEllipsis'
  import JDate from '@/components/jeecg/JDate'
  import Vue from 'vue'

  export default {
    name: "MaterialList",
    mixins:[JeecgListMixin],
    components: {
      MaterialModal,
      ImportFileModal,
      BatchSetInfoModal,
      JEllipsis,
      JDate
    },
    data () {
      return {
        categoryTree:[],
        mPropertyListShort: '',
        model: {},
        labelCol: {
          span: 5
        },
        wrapperCol: {
          span: 18,
          offset: 1
        },
        queryTitle: {
          mp1: this.$t('common.property'),
          mp2: this.$t('common.property'),
          mp3: this.$t('common.property')
        },
        // 查询条件
        queryParam: {
          categoryId: undefined,
          materialParam:'',
          standard:'',
          model:'',
          color:'',
          brand:'',
          mfrs:'',
          otherField1:'',
          otherField2:'',
          otherField3:'',
          weight:'',
          expiryNum:'',
          enabled: undefined,
          enableSerialNumber: undefined,
          enableBatchNumber: undefined,
          position: '',
          remark:'',
          mpList: getMpListShort(Vue.ls.get('materialPropertyList'))  //扩展属性
        },
        urlPath: '/material/material',
        ipagination:{
          pageSizeOptions: ['10', '20', '30', '50', '100', '200']
        },
        // 实际索引
        settingDataIndex:[],
        // 实际列
        columns:[],
        // 默认索引
        defDataIndex:['action','mBarCode','name','standard','model','color','categoryName','unit', 'stock',
          'purchaseDecimal','commodityDecimal','wholesaleDecimal','lowDecimal','enabled'],
        // 默认列
        defColumns: [
          {
            title: this.$t('common.action'),
            dataIndex: 'action',
            align:"center",
            width: 100,
            scopedSlots: { customRender: 'action' },
          },
          {title: this.$t('common.uploadImage'), dataIndex: 'pic', width: 60, scopedSlots: { customRender: 'customPic' }},
          {title: this.$t('common.barcode'), dataIndex: 'mBarCode', width: 120},
          {title: this.$t('common.productName'), dataIndex: 'name', width: 160, scopedSlots: { customRender: 'customName' }},
          {title: this.$t('common.specification'), dataIndex: 'standard', width: 120},
          {title: this.$t('common.model'), dataIndex: 'model', width: 120},
          {title: this.$t('material.color'), dataIndex: 'color', width: 70, ellipsis:true},
          {title: this.$t('common.brand'), dataIndex: 'brand', width: 100, ellipsis:true},
          {title: 'Mnemonic', dataIndex: 'mnemonic', width: 80, ellipsis:true},
          {title: this.$t('common.category'), dataIndex: 'categoryName', width: 100, ellipsis:true},
          {title: this.$t('purchase.form.columns.ext1'), dataIndex: 'otherField1', width: 100, ellipsis:true},
          {title: this.$t('purchase.form.columns.ext2'), dataIndex: 'otherField2', width: 100, ellipsis:true},
          {title: this.$t('purchase.form.columns.ext3'), dataIndex: 'otherField3', width: 100, ellipsis:true},
          {title: this.$t('common.unit'), dataIndex: 'unit', width: 100, ellipsis:true,
            customRender:function (t,r,index) {
              if (r) {
                let name = t?t:r.unitName
                if(r.sku) {
                  return name + '[SKU]';
                } else {
                  return name;
                }
              }
            }
          },
          {title: this.$t('material.weight'), dataIndex: 'weight', width: 80},
          {title: this.$t('material.expiryNum'), dataIndex: 'expiryNum', width: 60},
          {title: this.$t('material.manufacturer'), dataIndex: 'mfrs', width: 120, ellipsis:true},
          {title: this.$t('material.initialStock'), dataIndex: 'initialStock', width: 80,
            scopedSlots: { customRender: 'customRenderInitialStock' }
          },
          {title: this.$t('common.quantity'), dataIndex: 'stock', width: 80,
            scopedSlots: { customRender: 'customRenderStock' }
          },
          {title: this.$t('common.amount'), dataIndex: 'purchaseDecimal', width: 80},
          {title: this.$t('common.amount'), dataIndex: 'commodityDecimal', width: 80},
          {title: this.$t('common.amount'), dataIndex: 'wholesaleDecimal', width: 80},
          {title: this.$t('common.amount'), dataIndex: 'lowDecimal', width: 80},
          {title: this.$t('common.position'), dataIndex: 'position', width: 80},
          {title: this.$t('common.remark'), dataIndex: 'remark', width: 80},
          {title: this.$t('common.status'), dataIndex: 'enabled', align: "center", width: 60,
            scopedSlots: { customRender: 'customRenderEnabled' }
          }
        ],
        url: {
          list: "/material/list",
          delete: "/material/delete",
          deleteBatch: "/material/deleteBatch",
          importExcelUrl: "/material/importExcel",
          exportXlsUrl: "/material/exportExcel",
          batchSetStatusUrl: "/material/batchSetStatus",
          batchSetMaterialCurrentStockUrl: "/material/batchSetMaterialCurrentStock",
          batchSetMaterialCurrentUnitPriceUrl: "/material/batchSetMaterialCurrentUnitPrice",
        }
      }
    },
    created() {
      this.model = Object.assign({}, {});
      this.initColumnsSetting()
      this.loadTreeData()
      this.handleChangeOtherField(1)
    },
    computed: {
      importExcelUrl: function () {
        return `${window._CONFIG['domianURL']}${this.url.importExcelUrl}`;
      }
    },
    methods: {
      //加载初始化列
      initColumnsSetting(){
        let columnsStr = Vue.ls.get('materialColumns')
        if(columnsStr && columnsStr.indexOf(',')>-1) {
          this.settingDataIndex = columnsStr.split(',')
        } else {
          this.settingDataIndex = this.defDataIndex
        }
        this.columns = this.defColumns.filter(item => {
          return this.settingDataIndex.includes(item.dataIndex)
        })
      },
      //列设置更改事件
      onColChange (checkedValues) {
        this.columns = this.defColumns.filter(item => {
          return checkedValues.includes(item.dataIndex)
        })
        let columnsStr = checkedValues.join()
        Vue.ls.set('materialColumns', columnsStr)
      },
      //恢复默认
      handleRestDefault() {
        Vue.ls.remove('materialColumns')
        this.initColumnsSetting()
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
      batchSetMaterialCurrentStock () {
        if (this.selectedRowKeys.length <= 0) {
          this.$message.warning(this.$t('common.selectRecord'));
        } else {
          let ids = "";
          for (let a = 0; a < this.selectedRowKeys.length; a++) {
            ids += this.selectedRowKeys[a] + ",";
          }
          let that = this;
          this.$confirm({
            title: this.$t('common.confirmAction'),
            content: this.$t('common.confirmOperateSelected'),
            onOk: function () {
              that.loading = true;
              postAction(that.url.batchSetMaterialCurrentStockUrl, {ids: ids}).then((res) => {
                if(res.code === 200){
                  that.$message.info(that.$t('common.operateSuccess'));
                  that.loadData();
                  that.onClearSelected();
                } else {
                  that.$message.warning(res.data.message);
                }
              }).finally(() => {
                that.loading = false;
              });
            }
          });
        }
      },
      batchSetMaterialCurrentUnitPrice () {
        if (this.selectedRowKeys.length <= 0) {
          this.$message.warning(this.$t('common.selectRecord'));
        } else {
          let ids = "";
          for (let a = 0; a < this.selectedRowKeys.length; a++) {
            ids += this.selectedRowKeys[a] + ",";
          }
          let that = this;
          this.$confirm({
            title: this.$t('common.confirmAction'),
            content: this.$t('common.confirmOperateSelected'),
            onOk: function () {
              that.loading = true;
              postAction(that.url.batchSetMaterialCurrentUnitPriceUrl, {ids: ids}).then((res) => {
                if(res.code === 200){
                  that.$message.info(that.$t('common.operateSuccess'));
                  that.loadData();
                  that.onClearSelected();
                } else {
                  that.$message.warning(res.data.message);
                }
              }).finally(() => {
                that.loading = false;
              });
            }
          });
        }
      },
      batchEdit() {
        if (this.selectedRowKeys.length <= 0) {
          this.$message.warning(this.$t('common.selectRecord'));
        } else {
          let ids = "";
          for (let a = 0; a < this.selectedRowKeys.length; a++) {
            if(a === this.selectedRowKeys.length-1) {
              ids += this.selectedRowKeys[a]
            } else {
              ids += this.selectedRowKeys[a] + ','
            }
          }
          this.$refs.batchSetInfoModalForm.edit(ids);
          this.$refs.batchSetInfoModalForm.title = this.$t('material.batchSetInfo');
        }
      },
      handleAdd: function () {
        this.$refs.modalForm.action = "add";
        this.$refs.modalForm.add();
        this.$refs.modalForm.title = this.$t('common.add');
        this.$refs.modalForm.disableSubmit = false;
      },
      handleEdit: function (record) {
        this.$refs.modalForm.action = "edit";
        this.$refs.modalForm.edit(record);
        this.$refs.modalForm.title = this.$t('common.edit');
        this.$refs.modalForm.disableSubmit = false;
        if(this.btnEnableList.indexOf(1)===-1) {
          this.$refs.modalForm.showOkFlag = false
        }
      },
      handleCopyAdd(record) {
        this.$refs.modalForm.action = "copyAdd";
        this.$refs.modalForm.edit(record);
        this.$refs.modalForm.title = this.$t('common.copy');
        this.$refs.modalForm.disableSubmit = false;
      },
      getImgUrl(imgName, type) {
        if(imgName && imgName.split(',')) {
          type = type? type + '/':''
          return getFileAccessHttpUrl('systemConfig/static/' + type + imgName.split(',')[0])
        } else {
          return ''
        }
      },
      handleImportXls() {
        let importExcelUrl = this.url.importExcelUrl
        let templateUrl = '/doc/goods_template.xls'
        let templateName = this.$t('common.detailExcelTemplate')
        this.$refs.modalImportForm.initModal(importExcelUrl, templateUrl, templateName);
        this.$refs.modalImportForm.title = this.$t('common.import');
      },
      searchReset() {
        this.queryParam = {
          mpList: getMpListShort(Vue.ls.get('materialPropertyList'))  //扩展属性
        }
        this.loadData(1);
      }
    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less'
</style>
<style>
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
