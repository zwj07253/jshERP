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
                <a-form-item :label="$t('system.personNameQuery')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-input :placeholder="$t('system.enterPersonNameQuery')" v-model="queryParam.name"></a-input>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item :label="$t('common.type')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-select v-model="queryParam.type" :placeholder="$t('common.selectType')">
                    <a-select-option value="">{{ $t('common.pleaseSelect') }}</a-select-option>
                    <a-select-option value="销售员">{{ $t('system.salesPerson') }}</a-select-option>
                    <a-select-option value="仓管员">{{ $t('system.warehouseKeeper') }}</a-select-option>
                    <a-select-option value="财务员">{{ $t('system.financeStaff') }}</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <span style="float: left;overflow: hidden;" class="table-page-search-submitButtons">
                <a-col :md="6" :sm="24">
                  <a-button type="primary" @click="searchQuery">{{ $t('common.search') }}</a-button>
                  <a-button style="margin-left: 8px" @click="searchReset">{{ $t('common.reset') }}</a-button>
                </a-col>
              </span>
            </a-row>
          </a-form>
        </div>
        <!-- 操作按钮区域 -->
        <div class="table-operator"  style="margin-top: 5px">
          <a-button v-if="btnEnableList.indexOf(1)>-1" @click="handleAdd" type="primary" icon="plus">{{ $t('common.add') }}</a-button>
          <a-button v-if="btnEnableList.indexOf(1)>-1" @click="batchDel" icon="delete">{{ $t('common.delete') }}</a-button>
          <a-button v-if="btnEnableList.indexOf(1)>-1" @click="batchSetStatus(true)" icon="check-square">{{ $t('common.enable') }}</a-button>
          <a-button v-if="btnEnableList.indexOf(1)>-1" @click="batchSetStatus(false)" icon="close-square">{{ $t('common.disable') }}</a-button>
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
            :pagination="ipagination"
            :scroll="scroll"
            :loading="loading"
            :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
            @change="handleTableChange">
            <span slot="action" slot-scope="text, record">
              <a @click="handleEdit(record)">{{btnEnableList.indexOf(1)>-1 ? $t('common.edit') : $t('common.view')}}</a>
              <a-divider v-if="btnEnableList.indexOf(1)>-1" type="vertical" />
              <a-popconfirm v-if="btnEnableList.indexOf(1)>-1" :title="$t('common.confirmDelete')" @confirm="() => handleDelete(record.id)">
                <a>{{ $t('common.delete') }}</a>
              </a-popconfirm>
            </span>
            <!-- 状态渲染模板 -->
            <template slot="customRenderFlag" slot-scope="enabled">
              <a-tag v-if="enabled" color="green">{{ $t('common.enable') }}</a-tag>
              <a-tag v-if="!enabled" color="orange">{{ $t('common.disable') }}</a-tag>
            </template>
          </a-table>
        </div>
        <!-- table区域-end -->
        <!-- 表单区域 -->
        <person-modal ref="modalForm" @ok="modalFormOk"></person-modal>
      </a-card>
    </a-col>
  </a-row>
</template>
<!-- f r o m 7 5  2 7 1  8 9 2 0 -->
<script>
  import PersonModal from './modules/PersonModal'
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import JDate from '@/components/jeecg/JDate'
  export default {
    name: "PersonList",
    mixins:[JeecgListMixin],
    components: {
      PersonModal,
      JDate
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
        queryParam: {name:'',type:''},
        urlPath: '/system/person',
        // 表头
        columns: [
          {
            title: '#',
            dataIndex: '',
            key:'rowIndex',
            width:40,
            align:"center",
            customRender:function (t,r,index) {
              return parseInt(index)+1;
            }
          },
          {
            title: this.$t('common.action'),
            dataIndex: 'action',
            align:"center",
            width: 100,
            scopedSlots: { customRender: 'action' },
          },
          { title: this.$t('system.personNameQuery'), align:"left", dataIndex: 'name', width: 200, },
          { title: this.$t('common.type'), align:"left", dataIndex: 'type', width: 150, },
          { title: this.$t('common.sort'), dataIndex: 'sort', width: 60},
          { title: this.$t('common.status'),dataIndex: 'enabled',width:60,align:"center",
            scopedSlots: { customRender: 'customRenderFlag' }
          }
        ],
        url: {
          list: "/person/list",
          delete: "/person/delete",
          deleteBatch: "/person/deleteBatch",
          batchSetStatusUrl: "/person/batchSetStatus"
        }
      }
    },
    computed: {

    },
    methods: {
      handleEdit: function (record) {
        const isReadOnly = this.btnEnableList.indexOf(1) === -1
        this.$refs.modalForm.edit(record, isReadOnly);
        this.$refs.modalForm.title = isReadOnly ? this.$t('common.view') : this.$t('common.edit');
        this.$refs.modalForm.disableSubmit = false;
      }
    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less'
</style>
