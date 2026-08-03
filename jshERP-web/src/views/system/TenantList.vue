<template>
  <a-row :gutter="24">
    <a-col :md="24">
      <a-card :style="cardStyle" :bordered="false">
        <!-- 查询区域 -->
        <div class="table-page-search-wrapper">
          <a-form layout="inline" @keyup.enter.native="searchQuery">
            <a-row :gutter="24">
              <a-col :md="6" :sm="24">
                <a-form-item :label="$t('system.loginNameQuery')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-input :placeholder="$t('system.loginNamePlaceholder')" v-model="queryParam.loginName"></a-input>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item :label="$t('system.tenantTypeQuery')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-select v-model="queryParam.type" :placeholder="$t('system.selectTenantTypeQuery')">
                    <a-select-option value="0">{{ $t('system.trialTenantOpt') }}</a-select-option>
                    <a-select-option value="1">{{ $t('system.paidTenantOpt') }}</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item :label="$t('system.tenantStatus')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-select v-model="queryParam.enabled" :placeholder="$t('system.selectOperateStatus')">
                    <a-select-option value="1">{{ $t('common.enable') }}</a-select-option>
                    <a-select-option value="0">{{ $t('common.disable') }}</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <span style="float: left;overflow: hidden;" class="table-page-search-submitButtons">
                  <a-button type="primary" @click="searchQuery">{{ $t('common.search') }}</a-button>
                  <a-button style="margin-left: 8px" @click="searchReset">{{ $t('common.reset') }}</a-button>
                  <a @click="handleToggleSearch" style="margin-left: 8px">
                    {{ toggleSearchStatus ? $t('common.collapse') : $t('common.expand') }}
                    <a-icon :type="toggleSearchStatus ? 'up' : 'down'"/>
                  </a>
                </span>
              </a-col>
            </a-row>
            <template v-if="toggleSearchStatus">
              <a-row :gutter="24">
                <a-col :md="6" :sm="24">
                  <a-form-item :label="$t('common.remark')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-input v-model="queryParam.remark" :placeholder="$t('common.enterRemark')"></a-input>
                  </a-form-item>
                </a-col>
              </a-row>
            </template>
          </a-form>
        </div>
        <!-- 操作按钮区域 -->
        <div class="table-operator" style="border-top: 5px">
          <a-button @click="handleAdd" type="primary" icon="plus">{{ $t('common.add') }}</a-button>
          <a-button @click="batchSetStatus(1)" icon="check-square">{{ $t('common.enable') }}</a-button>
          <a-button @click="batchSetStatus(0)" icon="close-square">{{ $t('common.disable') }}</a-button>
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
              <a @click="handleEdit(record)">{{ $t('common.edit') }}</a>
            </span>
            <!-- 状态渲染模板 -->
            <template slot="customRenderType" slot-scope="type">
              <a-tag v-if="type==0">{{ $t('system.trialTenantOpt') }}</a-tag>
              <a-tag v-if="type==1" color="green">{{ $t('system.paidTenantOpt') }}</a-tag>
            </template>
            <template slot="customRenderEnabled" slot-scope="enabled">
              <a-tag v-if="enabled" color="green">{{ $t('common.enable') }}</a-tag>
              <a-tag v-if="!enabled" color="orange">{{ $t('common.disable') }}</a-tag>
            </template>
          </a-table>
        </div>
        <!-- table区域-end -->
        <tenant-modal ref="modalForm" @ok="modalFormOk"></tenant-modal>
      </a-card>
    </a-col>
  </a-row>
</template>
<!-- b y 7 5 2 7  1 8 9 2 0 -->
<script>
  import TenantModal from './modules/TenantModal'
  import {JeecgListMixin} from '@/mixins/JeecgListMixin'
  import JInput from '@/components/jeecg/JInput'
  import { getTenantRoleList } from '@/api/api'
  export default {
    name: "TenantList",
    mixins: [JeecgListMixin],
    components: {
      TenantModal,
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
        queryParam: {
          loginName: '',
          roleId: '',
          type: '',
          enabled: '',
          remark: ''
        },
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
            width: 100
          },
          { title: this.$t('system.loginNameQuery'), dataIndex: 'loginName', width: 100, align: "center"},
          { title: this.$t('system.userCount'), dataIndex: 'userCount', width: 60, align: "center"},
          { title: this.$t('system.userNumLimitCol'), dataIndex: 'userNumLimit', width: 80, align: "center"},
          { title: this.$t('system.tenantRoleCol'), dataIndex: 'roleName', width: 80, align: "center"},
          { title: this.$t('system.tenantTypeCol'),dataIndex: 'type',width:60,align:"center",
            scopedSlots: { customRender: 'customRenderType' }
          },
          { title: this.$t('system.tenantStatusCol'),dataIndex: 'enabled',width:60,align:"center",
            scopedSlots: { customRender: 'customRenderEnabled' }
          },
          { title: this.$t('common.createTime'), dataIndex: 'createTimeStr', width: 100, align: "center"},
          { title: this.$t('system.expireTimeCol'), dataIndex: 'expireTimeStr', width: 100, align: "center"},
          { title: this.$t('common.remark'), dataIndex: 'remark', width: 200, align: "center", ellipsis:true}
        ],
        url: {
          list: "/tenant/list",
          batchSetStatusUrl: "/tenant/batchSetStatus"
        },
      }
    },
    created () {
    },
    methods: {
    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less'
</style>