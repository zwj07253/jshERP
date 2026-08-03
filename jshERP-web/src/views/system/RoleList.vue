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
                <a-form-item :label="$t('common.roleName')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-input :placeholder="$t('common.roleName')" v-model="queryParam.name"></a-input>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item :label="$t('common.remark')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-input :placeholder="$t('common.enterRemark')" v-model="queryParam.description"></a-input>
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
              <template v-if="btnEnableList.indexOf(1)>-1">
                <a @click="handleSetFunction(record)">{{ $t('common.assignMenu') }}</a>
                <a-divider type="vertical" />
                <a @click="handleSetPushBtn(record.id, record.name)">{{ $t('common.assignButton') }}</a>
                <a-divider type="vertical" />
              </template>
              <a @click="handleEdit(record)">{{ btnEnableList.indexOf(1)>-1 ? $t('common.edit') : $t('common.view') }}</a>
              <a-divider v-if="btnEnableList.indexOf(1)>-1" type="vertical" />
              <a-popconfirm v-if="btnEnableList.indexOf(1)>-1" :title="$t('common.confirmDelete')" @confirm="() => handleDelete(record.id)">
                <a>{{ $t('common.delete') }}</a>
              </a-popconfirm>
              <a-modal v-model="roleModalVisible" :title="$t('common.operationTip')" @ok="handleRoleTip">
                <p>{{ $t('common.roleSaveSuccess') }}<b>{{ $t('common.assignMenu') }}</b>{{ $t('common.questionMark') }}</p>
              </a-modal>
            </span>
            <span slot="typeTitle">
              {{ $t('common.dataType') }}
              <a-tooltip :title="$t('common.dataTypeTip')">
                <a-icon type="question-circle" />
              </a-tooltip>
            </span>
            <span slot="priceLimitTitle">
              {{ $t('common.priceLimit') }}
              <a-tooltip :title="$t('common.priceLimitTip')">
                <a-icon type="question-circle" />
              </a-tooltip>
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
        <role-modal ref="modalForm" @ok="roleModalFormOk"></role-modal>
        <role-function-modal ref="roleFunctionModal" @ok="roleFunctionModalFormOk"></role-function-modal>
        <role-push-btn-modal ref="rolePushBtnModal" @ok="modalFormOk"></role-push-btn-modal>
        <a-modal v-model="roleFunctionModalVisible" :title="$t('common.operationTip')" @ok="handleRoleFunctionTip">
          <p>{{ $t('common.menuAssignSuccess') }}<b>{{ $t('common.assignButton') }}</b>{{ $t('common.questionMark') }}</p>
        </a-modal>
      </a-card>
    </a-col>
  </a-row>
</template>
<!-- f r o m 7 5  2 7 1  8 9 2 0 -->
<script>
  import RoleModal from './modules/RoleModal'
  import RoleFunctionModal from './modules/RoleFunctionModal'
  import RolePushBtnModal from './modules/RolePushBtnModal'
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import JDate from '@/components/jeecg/JDate'
  export default {
    name: "RoleList",
    mixins:[JeecgListMixin],
    components: {
      RoleModal,
      RoleFunctionModal,
      RolePushBtnModal,
      JDate
    },
    data () {
      return {
        description: '',
        roleModalVisible: false,
        roleFunctionModalVisible: false,
        currentRoleId: '',
        labelCol: {
          span: 5
        },
        wrapperCol: {
          span: 18,
          offset: 1
        },
        // 查询条件
        queryParam: {
          name: '',
          description: '',
        },
        urlPath: '/system/role',
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
            width: 200,
            scopedSlots: { customRender: 'action' },
          },
          {
            title: this.$t('common.roleName'), align:"left", dataIndex: 'name', width: 120
          },
          {
            align:"left", dataIndex: 'type', width: 100,
            slots: { title: 'typeTitle' }
          },
          {
            align:"left", dataIndex: 'priceLimitStr', width: 300,
            slots: { title: 'priceLimitTitle' }
          },
          {
            title: this.$t('common.remark'), align:"left", dataIndex: 'description', width: 150
          },
          { title: this.$t('common.sort'), align:"left", dataIndex: 'sort', width: 50},
          { title: this.$t('common.status'),dataIndex: 'enabled',width:60,align:"center",
            scopedSlots: { customRender: 'customRenderFlag' }
          }
        ],
        url: {
          list: "/role/list",
          delete: "/role/delete",
          deleteBatch: "/role/deleteBatch",
          batchSetStatusUrl: "/role/batchSetStatus"
        },
      }
    },
    computed: {
      importExcelUrl: function(){
        return `${window._CONFIG['domianURL']}/${this.url.importExcelUrl}`;
      }
    },
    methods: {
      handleSetFunction(record) {
        this.$refs.roleFunctionModal.edit(record);
        this.$refs.roleFunctionModal.title = this.$t('common.assignMenuTo') + record.name + this.$t('common.assignMenuTip')
        this.$refs.roleFunctionModal.disableSubmit = false;
      },
      handleSetPushBtn(roleId, roleName) {
        this.$refs.rolePushBtnModal.edit(roleId);
        this.$refs.rolePushBtnModal.title = this.$t('common.assignButtonTo') + roleName
        this.$refs.rolePushBtnModal.disableSubmit = false;
      },
      roleModalFormOk() {
        //重载列表
        this.loadData()
        this.roleModalVisible = true
      },
      roleFunctionModalFormOk(id) {
        //重载列表
        this.loadData()
        this.roleFunctionModalVisible = true
        this.currentRoleId = id
      },
      handleRoleTip() {
        let roleInfo
        if(this.currentRoleId) {
          //编辑的情况下
          for (let i = 0; i < this.dataSource.length; i++) {
            if(this.currentRoleId === this.dataSource[i].id) {
              roleInfo = this.dataSource[i]
            }
          }
        } else {
          //在新增的情况下
          if(this.dataSource.length) {
            roleInfo = this.dataSource[0]
          }
        }
        if(roleInfo) {
          this.roleModalVisible = false
          this.handleSetFunction(roleInfo)
        }
      },
      handleRoleFunctionTip() {
        if(this.currentRoleId) {
          this.roleFunctionModalVisible = false
          let roleName = ''
          for (let i = 0; i < this.dataSource.length; i++) {
            if(this.currentRoleId === this.dataSource[i].id) {
              roleName = this.dataSource[i].name
            }
          }
          this.handleSetPushBtn(this.currentRoleId, roleName)
        }
      },
      handleAdd: function () {
        this.currentRoleId = ''
        this.$refs.modalForm.add();
        this.$refs.modalForm.title = this.$t('common.add') + this.$t('common.saveThenAssignMenu');
        this.$refs.modalForm.disableSubmit = false;
      },
      handleEdit: function (record) {
        this.currentRoleId = record.id
        this.$refs.modalForm.edit(record);
        this.$refs.modalForm.title = this.$t('common.edit') + this.$t('common.saveThenAssignMenu');
        this.$refs.modalForm.disableSubmit = false;
        if(this.btnEnableList.indexOf(1)===-1) {
          this.$refs.modalForm.isReadOnly = true
        }
      }
    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less'
</style>
