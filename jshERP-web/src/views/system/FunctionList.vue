<!-- f r o m 7 5  2 7 1  8 9 2 0 -->
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
                <a-form-item :label="$t('common.name')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-input :placeholder="$t('common.enterName')" v-model="queryParam.name"></a-input>
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
          <a-button @click="handleAdd" type="primary" icon="plus">{{ $t('common.add') }}</a-button>
          <a-button @click="batchDel" icon="delete">{{ $t('common.delete') }}</a-button>
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
              <a @click="handleEdit(record)">{{ $t('common.edit') }}</a>
              <a-divider type="vertical" />
              <a-popconfirm :title="$t('common.confirmDelete')" @confirm="() => handleDelete(record.id)">
                <a>{{ $t('common.delete') }}</a>
              </a-popconfirm>
            </span>
            <!-- 状态渲染模板 -->
            <template slot="customRenderFlag" slot-scope="enabled">
              <a-tag v-if="enabled==1" color="green">{{ $t('common.enable') }}</a-tag>
              <a-tag v-if="enabled==0" color="orange">{{ $t('common.disable') }}</a-tag>
            </template>
          </a-table>
        </div>
        <!-- table区域-end -->
        <!-- 表单区域 -->
        <function-modal ref="modalForm" @ok="modalFormOk"></function-modal>
      </a-card>
    </a-col>
  </a-row>
</template>
<script>
  import FunctionModal from './modules/FunctionModal'
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import JDate from '@/components/jeecg/JDate'
  export default {
    name: "FunctionList",
    mixins:[JeecgListMixin],
    components: {
      FunctionModal,
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
            width: 150,
            align:"center",
            scopedSlots: { customRender: 'action' },
          },
          {title: this.$t('system.numberCol'), dataIndex: 'number', width: 80},
          {title: this.$t('common.name'), dataIndex: 'name', width: 120, ellipsis:true},
          {title: this.$t('system.parentNumberCol'), dataIndex: 'parentNumber', width: 80},
          {title: this.$t('system.parentNameCol'), dataIndex: 'parentName', width: 120, ellipsis:true},
          {title: this.$t('system.linkCol'), dataIndex: 'url', width: 250, ellipsis:true},
          {title: this.$t('system.componentCol'), dataIndex: 'component', width: 250, ellipsis:true},
          {title: this.$t('common.sort'), dataIndex: 'sort', width: 60},
          {
            title: this.$t('system.isEnabledCol'), dataIndex: 'enabled', width: 80, align: "center",
            scopedSlots: { customRender: 'customRenderFlag' }
          },
          {title: this.$t('system.iconCol'), dataIndex: 'icon', width: 120}
        ],
        url: {
          list: "/function/list",
          delete: "/function/delete",
          deleteBatch: "/function/deleteBatch"
        }
      }
    },
    computed: {

    },
    methods: {

    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less'
</style>