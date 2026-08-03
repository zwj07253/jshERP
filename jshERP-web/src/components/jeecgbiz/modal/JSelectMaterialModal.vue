<template>
  <a-modal
    :width="modalWidth"
    :visible="visible"
    :title="title"
    :wrapClassName="wrapClassNameInfo()"
    @ok="handleSubmit"
    @cancel="close"
    :cancelText="$t('common.closeEsc')"
    style="top:20px;height: 95%;"
  >
    <a-row :gutter="10" style="padding: 10px; margin: -10px">
      <a-col :md="24" :sm="24">
        <!-- 查询区域 -->
        <div class="table-page-search-wrapper">
          <!-- 搜索区域 -->
          <a-form layout="inline" @keyup.enter.native="onSearch">
            <a-row :gutter="24">
              <a-col :md="6" :sm="8">
                <a-form-item :label="$t('common.keyword')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-input ref="material" :placeholder="$t('common.enterBarcodeNameMnemonic')" v-model="queryParam.q"></a-input>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="8">
                <a-form-item :label="$t('common.specModel')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-input :placeholder="$t('common.enterSpecModel')" v-model="queryParam.standardOrModel"></a-input>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="8">
                <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.depot')">
                  <a-select :placeholder="$t('common.selectWarehouse')" v-model="queryParam.depotId" @change="onDepotChange"
                    :dropdownMatchSelectWidth="false" showSearch optionFilterProp="children" allow-clear>
                    <a-select-option v-for="(item,index) in depotList" :key="index" :value="item.id">
                      {{ item.depotName }}
                    </a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <span style="float: left;overflow: hidden;" class="table-page-search-submitButtons">
                <a-col :md="6" :sm="8">
                  <a-button type="primary" @click="loadMaterialData(1)">{{ $t('common.search') }}</a-button>
                  <a-button style="margin-left: 8px" @click="searchReset(1)">{{ $t('common.reset') }}</a-button>
                  <a-tooltip :title="$t('common.noResultsAddProduct')">
                    <a-button style="margin-left: 8px" @click="addMaterial">{{ $t('common.add') }}</a-button>
                  </a-tooltip>
                  <a @click="handleToggleSearch" style="margin-left: 8px">
                    {{ toggleSearchStatus ? $t('common.collapse') : $t('common.expand') }}
                    <a-icon :type="toggleSearchStatus ? 'up' : 'down'"/>
                  </a>
                </a-col>
              </span>
            </a-row>
            <template v-if="toggleSearchStatus">
              <a-row :gutter="24">
                <a-col :md="6" :sm="8">
                  <a-form-item :label="$t('material.color')" :labelCol="{span: 5}" :wrapperCol="{span: 18, offset: 1}">
                    <a-input :placeholder="$t('common.enterColor')" v-model="queryParam.color"></a-input>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="8">
                  <a-form-item :label="$t('common.brand')" :labelCol="{span: 5}" :wrapperCol="{span: 18, offset: 1}">
                    <a-input :placeholder="$t('common.brand')" v-model="queryParam.brand"></a-input>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="8">
                  <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.category')">
                    <a-tree-select style="width:100%" :dropdownStyle="{maxHeight:'200px',overflow:'auto'}" allow-clear
                                   :treeData="categoryTree" v-model="queryParam.categoryId" :placeholder="$t('financial.selectCategory')">
                    </a-tree-select>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="8">
                  <a-form-item :label="$t('material.manufacturer')" :labelCol="{span: 5}" :wrapperCol="{span: 18, offset: 1}">
                    <a-input :placeholder="$t('material.manufacturer')" v-model="queryParam.mfrs"></a-input>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="8">
                  <a-form-item :label="queryTitle.mp1" :labelCol="{span: 5}" :wrapperCol="{span: 18, offset: 1}">
                    <a-input :placeholder="$t('common.pleaseEnter') + queryTitle.mp1" v-model="queryParam.otherField1"></a-input>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="8">
                  <a-form-item :label="queryTitle.mp2" :labelCol="{span: 5}" :wrapperCol="{span: 18, offset: 1}">
                    <a-input :placeholder="$t('common.pleaseEnter') + queryTitle.mp2" v-model="queryParam.otherField2"></a-input>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="8">
                  <a-form-item :label="queryTitle.mp3" :labelCol="{span: 5}" :wrapperCol="{span: 18, offset: 1}">
                    <a-input :placeholder="$t('common.pleaseEnter') + queryTitle.mp3" v-model="queryParam.otherField3"></a-input>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="24">
                  <a-form-item :label="$t('purchase.form.columns.serialNumber')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-select :placeholder="$t('common.snEnabled')" v-model="queryParam.enableSerialNumber">
                      <a-select-option value="1">{{ $t('common.yesShort') }}</a-select-option>
                      <a-select-option value="0">{{ $t('common.noShort') }}</a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="24">
                  <a-form-item :label="$t('purchase.form.columns.batchNumber')" :labelCol="{span: 5}" :wrapperCol="{span: 18, offset: 1}">
                    <a-select :placeholder="$t('common.batchEnabled')" v-model="queryParam.enableBatchNumber">
                      <a-select-option value="1">{{ $t('common.yesShort') }}</a-select-option>
                      <a-select-option value="0">{{ $t('common.noShort') }}</a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
              </a-row>
            </template>
          </a-form>
          <a-table
            ref="table"
            :scroll="scrollTrigger"
            size="middle"
            rowKey="id"
            :columns="columns"
            :dataSource="dataSource"
            :pagination="ipagination"
            :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange,type: getType}"
            :loading="loading"
            :customRow="rowAction"
            @change="handleTableChange">
            <template slot="customBarCode" slot-scope="text, record">
              <div :style="record.imgName?'float:left;line-height:30px':'float:left;'">{{record.mBarCode}}</div>
              <a-popover placement="right" trigger="click">
                <template slot="content">
                  <img :src="getImgUrl(record.imgName, record.imgLarge)" width="500px" />
                </template>
                <div class="item-info" v-if="record.imgName">
                  <img v-if="record.imgName" :src="getImgUrl(record.imgName, record.imgSmall)" class="item-img" :title="$t('common.view')" />
                </div>
              </a-popover>
            </template>
            <template slot="customName" slot-scope="text, record">
              {{record.name}}
              <a-tag v-if="record.enableSerialNumber==1" color="orange">序</a-tag>
              <a-tag v-if="record.enableBatchNumber==1" color="orange">批</a-tag>
            </template>
          </a-table>
        </div>
      </a-col>
    </a-row>
    <material-modal ref="materialModalForm" @ok="modalFormOk"></material-modal>
  </a-modal>
</template>

<script>
  import { getAction, getFileAccessHttpUrl } from '@/api/manage'
  import {filterObj, getMpListShort} from '@/utils/util'
  import {getMaterialBySelect, queryMaterialCategoryTreeList} from '@/api/api'
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import {mixinDevice} from '@/utils/mixin'
  import Vue from 'vue'

  export default {
    name: 'JSelectMaterialModal',
    mixins:[JeecgListMixin, mixinDevice],
    components: {
      MaterialModal: () => import('@/views/material/modules/MaterialModal')
    },
    props: ['rows', 'multi', 'barCode'],
    data() {
      return {
        modalWidth: 1450,
        queryTitle: {
          mp1: this.$t('purchase.form.columns.ext1'),
          mp2: this.$t('purchase.form.columns.ext2'),
          mp3: this.$t('purchase.form.columns.ext3')
        },
        queryParam: {
          q: '',
          standardOrModel: '',
          depotId: undefined,
          color: '',
          brand: '',
          categoryId: undefined,
          mfrs: '',
          otherField1:'',
          otherField2:'',
          otherField3:'',
          enableSerialNumber: undefined,
          enableBatchNumber: undefined
        },
        labelCol: {
          xs: { span: 24 },
          sm: { span: 5 },
        },
        wrapperCol: {
          xs: { span: 24 },
          sm: { span: 16 },
        },
        categoryTree:[],
        columns: [
          {dataIndex: 'mBarCode', title: this.$t('common.barcode'), scopedSlots: { customRender: 'customBarCode' }},
          {dataIndex: 'name', title: this.$t('common.name'), scopedSlots: { customRender: 'customName' }},
          {dataIndex: 'categoryName', title: this.$t('common.category')},
          {dataIndex: 'standard', title: this.$t('common.specification')},
          {dataIndex: 'model', title: this.$t('common.model')},
          {dataIndex: 'color', title: this.$t('material.color')},
          {dataIndex: 'brand', title: this.$t('common.brand')},
          {dataIndex: 'mfrs', title: this.$t('material.manufacturer')},
          {dataIndex: 'unit', title: this.$t('common.unit')},
          {dataIndex: 'sku', title: this.$t('purchase.form.columns.sku')},
          {dataIndex: 'stock', title: this.$t('purchase.form.columns.stock')},
          {dataIndex: 'otherField1', title: this.$t('purchase.form.columns.ext1')},
          {dataIndex: 'otherField2', title: this.$t('purchase.form.columns.ext2')},
          {dataIndex: 'otherField3', title: this.$t('purchase.form.columns.ext3')}
        ],
        scrollTrigger: {},
        dataSource: [],
        selectedRowKeys: [],
        selectMaterialRows: [],
        selectMaterialIds: [],
        title: this.$t('common.productSelection'),
        ipagination: {
          current: 1,
          pageSize: 10,
          pageSizeOptions: ['10', '20', '30'],
          showTotal: (total, range) => {
            return this.$t('common.pagedTotal', { range0: range[0], range1: range[1], total: total })
          },
          showQuickJumper: true,
          showSizeChanger: true,
          total: 0
        },
        isorter: {
          column: 'createTime',
          order: 'desc'
        },
        departTree: [],
        depotList: [],
        visible: false,
        form: this.$form.createForm(this),
        loading: false,
        expandedKeys: [],
        disableMixinCreated: true,
      }
    },
    computed: {
      // 计算属性的 getter
      getType: function () {
        return this.multi == true ? 'checkbox' : 'radio';
      }
    },
    watch: {
      barCode: {
        immediate: true,
        handler() {
          this.initBarCode()
        }
      },
    },
    created() {
      // 该方法触发屏幕自适应
      this.resetScreenSize()
      this.handleChangeOtherField()
    },
    methods: {
      initBarCode() {
        if (this.barCode) {
          this.$emit('initComp', this.barCode)
        } else {
          // JSelectUserByDep组件bug issues/I16634
          this.$emit('initComp', '')
        }
      },
      loadMaterialData(arg) {
        if (arg === 1) {
          this.ipagination.current = 1;
        }
        this.loading = true
        let params = this.getQueryParams()//查询条件
        getMaterialBySelect(params).then((res) => {
          if (res) {
            this.dataSource = res.rows
            this.ipagination.total = res.total
            if(res.total ===1) {
              if(this.queryParam.q === this.dataSource[0].mBarCode||
                this.queryParam.q === this.dataSource[0].name||
                this.queryParam.q === this.dataSource[0].mnemonic) {
                this.title = this.$t('common.productSelectionReenter')
                this.$nextTick(() => this.$refs.material.focus());
              } else {
                this.title = this.$t('common.productSelection')
              }
            } else {
              this.title = this.$t('common.productSelection')
            }
          }
          this.loading = false
          this.onClearSelected()
        })
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
      // 触发屏幕自适应
      resetScreenSize() {
        let realScreenWidth = window.screen.width
        this.modalWidth = realScreenWidth<1600?'1300px':'1550px'
        let screenWidth = document.body.clientWidth;
        if (screenWidth < 500) {
          this.scrollTrigger = {x: 800};
        } else {
          this.scrollTrigger = {};
        }
      },
      //动态替换扩展字段
      handleChangeOtherField() {
        let mpStr = getMpListShort(Vue.ls.get('materialPropertyList'))
        if(mpStr) {
          let mpArr = mpStr.split(',')
          if(mpArr.length ===3) {
            this.queryTitle.mp1 = mpArr[0]
            this.queryTitle.mp2 = mpArr[1]
            this.queryTitle.mp3 = mpArr[2]
            for (let i = 0; i < this.columns.length; i++) {
              if(this.columns[i].dataIndex === 'otherField1') {
                this.columns[i].title = mpArr[0]
              }
              if(this.columns[i].dataIndex === 'otherField2') {
                this.columns[i].title = mpArr[1]
              }
              if(this.columns[i].dataIndex === 'otherField3') {
                this.columns[i].title = mpArr[2]
              }
            }
          }
        }
      },
      showModal(barCode) {
        this.visible = true;
        this.title = this.$t('common.productSelection')
        this.queryParam.q = barCode
        this.$nextTick(() => this.$refs.material.focus());
        this.loadTreeData()
        this.getDepotList()
        this.initDepotSelect()
        this.loadMaterialData();
        this.form.resetFields();
      },
      getQueryParams() {
        let param = Object.assign({}, this.queryParam, this.isorter);
        param.mpList = getMpListShort(Vue.ls.get('materialPropertyList'))  //扩展属性
        param.page = this.ipagination.current;
        param.rows = this.ipagination.pageSize;
        return filterObj(param);
      },
      getQueryField() {
        let str = 'id,';
        for (let a = 0; a < this.columns.length; a++) {
          str += ',' + this.columns[a].dataIndex;
        }
        return str;
      },
      searchReset(num) {
        let that = this;
        if (num !== 0) {
          that.queryParam = {};
          that.loadMaterialData(1);
        }
        that.selectedRowKeys = [];
        that.selectMaterialIds = [];
      },
      addMaterial() {
        this.$refs.materialModalForm.add()
        this.$refs.materialModalForm.title = this.$t('common.addNewProduct')
      },
      getImgUrl(imgName, type) {
        if(imgName && imgName.split(',')) {
          type = type? type + '/':''
          return getFileAccessHttpUrl('systemConfig/static/' + type + imgName.split(',')[0])
        } else {
          return ''
        }
      },
      close() {
        this.searchReset(0);
        this.visible = false;
      },
      handleTableChange(pagination, filters, sorter) {
        if (Object.keys(sorter).length > 0) {
          this.isorter.column = sorter.field;
          this.isorter.order = 'ascend' === sorter.order ? 'asc' : 'desc';
        }
        this.ipagination = pagination;
        this.loadMaterialData();
      },
      handleSubmit() {
        let that = this;
        this.getSelectMaterialRows();
        if(that.selectMaterialRows.length>0) {
          that.$emit('ok', that.selectMaterialRows, that.selectMaterialIds);
          that.searchReset(0)
          that.close();
        } else {
          that.$message.warning(this.$t('common.pleaseSelect') + this.$t('common.productSelection'))
        }
      },
      //获取选择信息
      getSelectMaterialRows(rowId) {
        let dataSource = this.dataSource;
        let materialIds = "";
        this.selectMaterialRows = [];
        for (let i = 0, len = dataSource.length; i < len; i++) {
          if (this.selectedRowKeys.includes(dataSource[i].id)) {
            this.selectMaterialRows.push(dataSource[i]);
            materialIds = materialIds + "," + dataSource[i].mBarCode
          }
        }
        this.selectMaterialIds = materialIds.substring(1);
      },
      getDepotList() {
        let that = this;
        getAction('/depot/findDepotByCurrentUser').then((res) => {
          if(res.code === 200){
            that.depotList = res.data
          }
        })
      },
      initDepotSelect() {
        if(this.rows) {
          if(JSON.parse(this.rows).depotId){
            this.queryParam.depotId = JSON.parse(this.rows).depotId-0
          }
        }
      },
      onDepotChange(value) {
        this.queryParam.depotId = value
      },
      onSelectChange(selectedRowKeys, selectionRows) {
        this.selectedRowKeys = selectedRowKeys;
        this.selectionRows = selectionRows;
      },
      onSearch() {
        if(this.dataSource && this.dataSource.length===1) {
          if(this.queryParam.q === this.dataSource[0].mBarCode||
            this.queryParam.q === this.dataSource[0].name||
            this.queryParam.q === this.dataSource[0].mnemonic) {
            let arr = []
            arr.push(this.dataSource[0].id)
            this.selectedRowKeys = arr
            this.handleSubmit()
          } else {
            this.loadMaterialData(1)
          }
        } else {
          this.loadMaterialData(1)
        }
      },
      modalFormOk() {
        this.loadMaterialData()
      },
      rowAction(record, index) {
        return {
          on: {
            dblclick: () => {
              let arr = []
              arr.push(record.id)
              this.selectedRowKeys = arr
              this.handleSubmit()
            }
          }
        }
      }
    }
  }
</script>

<style scoped>
  .ant-table-tbody .ant-table-row td {
    padding-top: 10px;
    padding-bottom: 10px;
  }

  #components-layout-demo-custom-trigger .trigger {
    font-size: 18px;
    line-height: 64px;
    padding: 0 24px;
    cursor: pointer;
    transition: color .3s;
  }

  .item-info {
    float:left;
    width:30px;
    height:30px;
    margin-left:8px
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
