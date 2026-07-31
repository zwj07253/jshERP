<template>
  <div ref="container">
    <a-modal
      :title="title"
      :width="1300"
      :visible="visible"
      :getContainer="() => $refs.container"
      :maskStyle="{'top':'93px','left':'154px'}"
      :wrapClassName="wrapClassNameInfo()"
      :mask="isDesktop()"
      :maskClosable="false"
      @cancel="handleCancel"
      :cancelText="$t('common.close')"
      style="top:40px;height: 90%;">
      <template slot="footer">
        <a-button @click="handleCancel">{{ $t('common.close') }}</a-button>
      </template>
      <!-- 查询区域 -->
      <div class="table-page-search-wrapper">
        <!-- 搜索区域 -->
        <a-form layout="inline" @keyup.enter.native="searchQuery">
          <a-row :gutter="24">
            <a-col :md="6" :sm="24">
              <a-form-item :label="$t('system.dictNameLabel')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                <a-select :placeholder="$t('system.selectDictName')" showSearch allow-clear optionFilterProp="children" v-model="queryParam.dictType">
                  <a-select-option v-for="(item,index) in typeOptions" :key="index" :value="item.dictType">
                    {{ item.dictName }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-item :label="$t('system.dictLabel')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                <a-input :placeholder="$t('system.enterDictLabel')" v-model="queryParam.dictLabel"></a-input>
              </a-form-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-item :label="$t('common.status')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                <a-select v-model="queryParam.status" :placeholder="$t('common.selectStatus')">
                  <a-select-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :value="dict.value">
                    {{ dict.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <span style="float: left;overflow: hidden;" class="table-page-search-submitButtons">
              <a-col :md="4" :sm="24">
                <a-button type="primary" @click="searchQuery">{{ $t('common.search') }}</a-button>
                <a-button style="margin-left: 8px" @click="searchReset">{{ $t('common.reset') }}</a-button>
              </a-col>
            </span>
          </a-row>
        </a-form>
      </div>
      <!-- 操作按钮区域 -->
      <div class="table-operator" style="border-top: 5px">
        <a-button v-if="btnEnableList.indexOf(1)>-1" @click="handleAddWithData" type="primary" icon="plus">{{ $t('common.add') }}</a-button>
        <a-button v-if="btnEnableList.indexOf(1)>-1" @click="batchDel" icon="delete">{{ $t('common.delete') }}</a-button>
      </div>
      <!-- table区域-begin -->
      <a-table
        bordered
        ref="table"
        size="middle"
        rowKey="dictCode"
        :columns="columns"
        :dataSource="dataSource"
        :components="handleDrag(columns)"
        :pagination="ipagination"
        :loading="loading"
        :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
        @change="handleTableChange">
        <span slot="action" slot-scope="text, record">
          <a v-if="btnEnableList.indexOf(1)>-1" @click="handleEdit(record)">{{ $t('common.edit') }}</a>
          <a-divider v-if="btnEnableList.indexOf(1)>-1" type="vertical" />
          <a-popconfirm v-if="btnEnableList.indexOf(1)>-1" :title="$t('common.confirmDelete')" @confirm="() => handleDelete(record.dictCode)">
            <a>{{ $t('common.delete') }}</a>
          </a-popconfirm>
        </span>
        <template slot="customRenderDictLabel" slot-scope="text, record">
          <span v-if="record.listClass == '' || record.listClass == 'default'">{{record.dictLabel}}</span>
          <a-tag v-else :color="record.listClass == 'grey' ? '' : record.listClass">{{record.dictLabel}}</a-tag>
        </template>
        <!-- 状态渲染模板 -->
        <template slot="customRenderStatus" slot-scope="status">
          <dict-tag :options="dict.type.sys_normal_disable" :value="status"/>
        </template>
      </a-table>
      <!-- table区域-end -->
      <!-- 表单区域 -->
      <dict-data-modal ref="modalForm" @ok="modalFormOk"></dict-data-modal>
    </a-modal>
  </div>
</template>
<script>
  import { getDictOptionselect } from '@/api/api'
  import DictDataModal from './DictDataModal'
  import { mixinDevice } from '@/utils/mixin'
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  export default {
    name: "DictDataListModal",
    dicts: ['sys_normal_disable'],
    components: { DictDataModal },
    mixins:[JeecgListMixin, mixinDevice],
    data () {
      return {
        title: this.$t('system.dictData'),
        visible: false,
        disableMixinCreated: true,
        // 类型数据字典
        typeOptions: [],
        queryParam: {
          dictType: undefined,
          dictLabel: "",
          status: undefined
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
            scopedSlots: {customRender: 'action'},
            align: "center",
            width: 80
          },
          { title: this.$t('system.dictCode'), dataIndex: 'dictCode',width:100, align:"center"},
          { title: this.$t('system.dictLabel'),dataIndex: 'dictLabel',width: 100,align:"center",
            scopedSlots: { customRender: 'customRenderDictLabel' }
          },
          { title: this.$t('system.dictValue'), dataIndex: 'dictValue',width:100, align:"center"},
          { title: this.$t('system.dictSort'), dataIndex: 'dictSort',width:100, align:"center"},
          { title: this.$t('common.status'), dataIndex: 'status',width:100, align:"center",
            scopedSlots: { customRender: 'customRenderStatus' }
          },
          { title: this.$t('common.remark'), dataIndex: 'remark',width:100},
          { title: this.$t('common.createTime'), dataIndex: 'createTime',width:100}
        ],
        dataSource:[],
        url: {
          list: "/dict/data/list",
          delete: "/dict/data/delete",
          deleteBatch: "/dict/data/deleteBatch"
        }
      }
    },
    created () {
    },
    methods: {
      show(record) {
        this.model = Object.assign({}, {})
        this.visible = true
        this.queryParam.dictType = record.dictType
        this.getTypeList()
        this.loadData(1)
        this.initActiveBtnStr()
      },
      /** 查询字典类型列表 */
      getTypeList() {
        getDictOptionselect().then(res => {
          this.typeOptions = res.data;
        });
      },
      handleAddWithData() {
        this.$refs.modalForm.add(this.queryParam.dictType)
        this.$refs.modalForm.title = this.$t('common.add');
        this.$refs.modalForm.disableSubmit = false;
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
      searchReset() {
        this.queryParam.dictLabel = ""
        this.queryParam.status = undefined
        this.loadData(1);
      }
    }
  }
</script>
<style scoped>
</style>
