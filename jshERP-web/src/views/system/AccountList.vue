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
                  <a-input :placeholder="$t('common.name')" v-model="queryParam.name"></a-input>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item :label="$t('common.serialNo')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-input :placeholder="$t('common.serialNo')" v-model="queryParam.serialNo"></a-input>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item :label="$t('common.remark')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-input :placeholder="$t('common.enterRemark')" v-model="queryParam.remark"></a-input>
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
          <a-button v-if="btnEnableList.indexOf(1)>-1" @click="batchSetStatus(false)"icon="close-square" >{{ $t('common.disable') }}</a-button>
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
              <a-popconfirm v-if="btnEnableList.indexOf(1)>-1" :title="$t('common.confirmSetDefault')" @confirm="() => handleSetDefault(record.id)">
                <a>{{ $t('common.setDefault') }}</a>
              </a-popconfirm>
              <a-divider v-if="btnEnableList.indexOf(1)>-1" type="vertical" />
              <a @click="handleEdit(record)">{{ $t('common.edit') }}</a>
              <a-divider v-if="btnEnableList.indexOf(1)>-1" type="vertical" />
              <a-popconfirm v-if="btnEnableList.indexOf(1)>-1" :title="$t('common.confirmDelete')" @confirm="() => handleDelete(record.id)">
                <a>{{ $t('common.delete') }}</a>
              </a-popconfirm>
            </span>
            <!-- 状态渲染模板 -->
            <template slot="customRenderEnabledFlag" slot-scope="enabled">
              <a-tag v-if="enabled" color="green">{{ $t('common.enable') }}</a-tag>
              <a-tag v-if="!enabled" color="orange">{{ $t('common.disable') }}</a-tag>
            </template>
            <template slot="customRenderFlag" slot-scope="isDefault">
              <a-tag v-if="isDefault" color="green">{{ $t('common.yes') }}</a-tag>
              <a-tag v-if="!isDefault" color="orange">{{ $t('common.no') }}</a-tag>
            </template>
          </a-table>
        </div>
        <!-- table区域-end -->
        <!-- 表单区域 -->
        <account-modal ref="modalForm" @ok="modalFormOk"></account-modal>
      </a-card>
    </a-col>
  </a-row>
</template>
<!-- BY cao_yu_li -->
<script>
  import AccountModal from './modules/AccountModal'
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import JDate from '@/components/jeecg/JDate'
  import { postAction } from '@api/manage'
  export default {
    name: "AccountList",
    mixins:[JeecgListMixin],
    components: {
      AccountModal,
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
        queryParam: {name:'',serialNo:'',remark:''},
        urlPath: '/system/account',
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
          { title: this.$t('common.name'), dataIndex: 'name', width: 100, align: "left"},
          { title: this.$t('common.serialNo'), dataIndex: 'serialNo', width: 150, align: "left"},
          { title: this.$t('common.openingAmount'), dataIndex: 'initialAmount', width: 100, align: "left"},
          { title: this.$t('common.remark'), dataIndex: 'remark', width: 200, align: "left"},
          { title: this.$t('common.sort'), dataIndex: 'sort', width: 60, align: "left"},
          { title: this.$t('common.status'),dataIndex: 'enabled',width:60,align:"center",
            scopedSlots: { customRender: 'customRenderEnabledFlag' }
          },
          { title: this.$t('common.isDefault'),dataIndex: 'isDefault',width:80,align:"center",
            scopedSlots: { customRender: 'customRenderFlag' }
          }
        ],
        url: {
          list: "/account/list",
          delete: "/account/delete",
          deleteBatch: "/account/deleteBatch",
          setDefault: "/account/updateIsDefault",
          batchSetStatusUrl: "/account/batchSetStatus"
        }
      }
    },
    computed: {

    },
    methods: {
      handleSetDefault: function (id) {
        if(!this.url.setDefault){
          this.$message.error(this.$t('common.setUrlError', {field: 'delete'}))
          return
        }
        let that = this;
        postAction(that.url.setDefault, {id: id}).then((res) => {
          if(res.code === 200){
            that.loadData();
          } else {
            that.$message.warning((res.data && res.data.message) || res.data || that.$t('system.setDefaultAccountFailed'));
          }
        });
      },
      handleEdit: function (record) {
        this.$refs.modalForm.edit(record);
        this.$refs.modalForm.title = this.$t('common.edit');
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
