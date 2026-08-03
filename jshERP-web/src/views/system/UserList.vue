<template>
  <a-row :gutter="24">
    <a-col :md="24">
      <a-card :style="cardStyle" :bordered="false">
        <!-- 查询区域 -->
        <div class="table-page-search-wrapper">
          <a-form layout="inline" @keyup.enter.native="searchQuery">
            <a-row :gutter="24">
              <a-col :md="6" :sm="24">
                <a-form-item :label="$t('login.username')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-input :placeholder="$t('login.username')" v-model="queryParam.loginName"></a-input>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item :label="$t('common.name')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-input :placeholder="$t('common.name')" v-model="queryParam.userName"></a-input>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <span style="float: left;overflow: hidden;" class="table-page-search-submitButtons">
                  <a-button type="primary" @click="searchQuery">{{ $t('common.search') }}</a-button>
                  <a-button style="margin-left: 8px" @click="searchReset">{{ $t('common.reset') }}</a-button>
                </span>
              </a-col>
            </a-row>
          </a-form>
        </div>
        <!-- 操作按钮区域 -->
        <div class="table-operator" style="border-top: 5px">
          <a-button v-if="btnEnableList.indexOf(1)>-1" @click="handleAdd" type="primary" icon="plus">{{ $t('common.add') }}</a-button>
          <a-button v-if="btnEnableList.indexOf(1)>-1" @click="batchDel" icon="delete">{{ $t('common.delete') }}</a-button>
          <a-button v-if="btnEnableList.indexOf(1)>-1" @click="batchSetStatus(0)" icon="check-square">{{ $t('common.enable') }}</a-button>
          <a-button v-if="btnEnableList.indexOf(1)>-1" @click="batchSetStatus(2)" icon="close-square">{{ $t('common.disable') }}</a-button>
        </div>
        <!-- table区域-begin -->
        <div>
          <a-table
            ref="table"
            bordered
            size="middle"
            rowKey="id"
            :columns="columns"
            :dataSource="dataSource"
            :pagination="ipagination"
            :scroll="scroll"
            :loading="loading"
            :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
            @change="handleTableChange">
            <span slot="action" slot-scope="text, record">
              <a v-if="btnEnableList.indexOf(1)>-1 && depotFlag === '1' " @click="btnSetDepot(record)">{{ $t('common.assignWarehouse') }}</a>
              <a-divider v-if="btnEnableList.indexOf(1)>-1 && depotFlag === '1'" type="vertical" />
              <a v-if="btnEnableList.indexOf(1)>-1 && customerFlag === '1'" @click="btnSetCustomer(record)">{{ $t('common.assignCustomer') }}</a>
              <a-divider v-if="btnEnableList.indexOf(1)>-1 && customerFlag === '1'" type="vertical" />
              <a @click="handleEdit(record)">{{ $t('common.edit') }}</a>
              <a-divider v-if="btnEnableList.indexOf(1)>-1" type="vertical"/>
              <a-popconfirm v-if="btnEnableList.indexOf(1)>-1" :title="$t('common.confirmDelete')" @confirm="() => handleDelete(record.id)">
                <a>{{ $t('common.delete') }}</a>
              </a-popconfirm>
              <a-divider v-if="btnEnableList.indexOf(1)>-1" type="vertical"/>
              <a v-if="btnEnableList.indexOf(1)>-1" @click="handleResetModal(record)">{{ $t('common.resetPassword') }}</a>
            </span>
            <!-- 状态渲染模板 -->
            <template slot="customRenderFlag" slot-scope="status">
              <a-tag v-if="status===0" color="green">{{ $t('common.enable') }}</a-tag>
              <a-tag v-if="status===2" color="orange">{{ $t('common.disable') }}</a-tag>
            </template>
          </a-table>
        </div>
        <!-- table区域-end -->
        <user-modal ref="modalForm" @ok="modalFormOk"></user-modal>
        <user-depot-modal ref="userDepotModal" @ok="modalFormOk"></user-depot-modal>
        <user-customer-modal ref="userCustomerModal" @ok="modalFormOk"></user-customer-modal>
        <user-reset-modal ref="userResetModal" @ok="modalFormOk"></user-reset-modal>
      </a-card>
    </a-col>
  </a-row>
</template>
<!-- b y 7 5 2 7  1 8 9 2 0 -->
<script>
  import UserModal from './modules/UserModal'
  import UserDepotModal from './modules/UserDepotModal'
  import UserCustomerModal from './modules/UserCustomerModal'
  import UserResetModal from './modules/UserResetModal'
  import {postAction} from '@/api/manage';
  import {getCurrentSystemConfig} from '@/api/api'
  import {JeecgListMixin} from '@/mixins/JeecgListMixin'
  import JInput from '@/components/jeecg/JInput'
  export default {
    name: "UserList",
    mixins: [JeecgListMixin],
    components: {
      UserModal,
      UserDepotModal,
      UserCustomerModal,
      UserResetModal,
      JInput
    },
    data() {
      return {
        labelCol: {
          span: 5
        },
        wrapperCol: {
          span: 18,
          offset: 1
        },
        queryParam: {},
        urlPath: '/system/user',
        depotFlag: '0',
        customerFlag: '0',
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
            scopedSlots: {customRender: 'action'},
            align: "center",
            width: 200
          },
          { title: this.$t('login.username'), dataIndex: 'loginName', width: 100, align: "left"},
          { title: this.$t('common.name'), dataIndex: 'username', width: 100, align: "left"},
          { title: this.$t('common.userType'), dataIndex: 'userType', width: 80, align: "left" },
          { title: this.$t('common.roleName'), dataIndex: 'roleName', width: 100, align: "left"},
          { title: this.$t('common.department'), dataIndex: 'orgAbr', width: 100, align: "left"},
          { title: this.$t('common.isManager'), dataIndex: 'leaderFlagStr', width: 60, align: "left"},
          { title: this.$t('common.phoneNo'), dataIndex: 'phonenum', width: 80, align: "left"},
          { title: this.$t('common.sort'), dataIndex: 'userBlngOrgaDsplSeq', width: 40, align: "left"},
          { title: this.$t('common.status'),dataIndex: 'status',width:60,align:"center",
            scopedSlots: { customRender: 'customRenderFlag' }
          }
        ],
        url: {
          list: "/user/list",
          delete: "/user/delete",
          deleteBatch: "/user/deleteBatch",
          resetPwd: "/user/resetPwd",
          batchSetStatusUrl: "/user/batchSetStatus"
        },
      }
    },
    created () {
      this.getSystemConfig()
    },
    methods: {
      getSystemConfig() {
        getCurrentSystemConfig().then((res) => {
          if(res.code === 200 && res.data){
            this.depotFlag = res.data.depotFlag
            this.customerFlag = res.data.customerFlag
          }
        })
      },
      searchQuery() {
        this.loadData(1);
        this.getSystemConfig();
      },
      searchReset() {
        this.queryParam = {}
        this.loadData(1);
        this.getSystemConfig();
      },
      handleEdit: function (record) {
        this.$refs.modalForm.edit(record);
        this.$refs.modalForm.title = this.$t('common.edit');
        this.$refs.modalForm.disableSubmit = false;
        this.$refs.modalForm.isReadOnly = this.btnEnableList.indexOf(1) === -1
      },
      handleResetModal(record) {
        this.$refs.userResetModal.edit(record);
        this.$refs.userResetModal.title = this.$t('common.enterNewPasswordFor') + record.loginName;
      },
      btnSetDepot(record) {
        this.$refs.userDepotModal.edit(record);
        this.$refs.userDepotModal.title = this.$t('common.assignWarehouseTo') + record.username
        this.$refs.userDepotModal.disableSubmit = false;
      },
      btnSetCustomer(record) {
        this.$refs.userCustomerModal.edit(record);
        this.$refs.userCustomerModal.title = this.$t('common.assignCustomerTo') + record.username
        this.$refs.userCustomerModal.disableSubmit = false;
      },
    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less'
</style>
