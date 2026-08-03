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
                <a-form-item :label="$t('common.memberCard')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-input :placeholder="$t('system.enterMemberCardQuery')" v-model="queryParam.supplier"></a-input>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item :label="$t('system.contact')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-input :placeholder="$t('system.contact')" v-model="queryParam.contacts"></a-input>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item :label="$t('common.phoneNo')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-input :placeholder="$t('common.phoneNo')" v-model="queryParam.telephone"></a-input>
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
                  <a-form-item :label="$t('system.phone')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-input :placeholder="$t('system.phone')" v-model="queryParam.phonenum"></a-input>
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
          <a-button v-if="btnEnableList.indexOf(3)>-1" @click="handleExportXls('会员信息')" icon="download">{{ $t('common.export') }}</a-button>
          <a-button v-if="btnEnableList.indexOf(1)>-1" @click="batchSetAdvanceIn()" icon="stock">{{ $t('financial.correctPrepaid') }}</a-button>
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
              <a v-if="btnEnableList.indexOf(1)>-1" @click="handleEdit(record)">{{ $t('common.edit') }}</a>
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
        <member-modal ref="modalForm" @ok="modalFormOk"></member-modal>
        <import-file-modal ref="modalImportForm" @ok="modalFormOk"></import-file-modal>
      </a-card>
    </a-col>
  </a-row>
</template>
<!-- f r o m 7 5  2 7 1  8 9 2 0 -->
<script>
  import MemberModal from './modules/MemberModal'
  import ImportFileModal from '@/components/tools/ImportFileModal'
  import { postAction } from '@/api/manage'
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import JDate from '@/components/jeecg/JDate'
  import Vue from 'vue'
  export default {
    name: "MemberList",
    mixins:[JeecgListMixin],
    components: {
      MemberModal,
      ImportFileModal,
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
        queryParam: {
          supplier:'',
          type:'会员',
          telephone:'',
          phonenum:''
        },
        urlPath: '/system/member',
        ipagination:{
          pageSizeOptions: ['10', '20', '30', '100', '200']
        },
        // 表头
        columns: [
          {
            title: '#',
            dataIndex: '',
            key:'rowIndex',
            width:60,
            align:"center",
            customRender:function (t,r,index) {
              return parseInt(index)+1;
            }
          },
          {
            title: this.$t('common.action'),
            dataIndex: 'action',
            width: 130,
            align:"center",
            scopedSlots: { customRender: 'action' },
          },
          { title: this.$t('common.memberCard'),dataIndex: 'supplier',width:150,align:"left"},
          { title: this.$t('system.contact'), dataIndex: 'contacts',width:70,align:"left"},
          { title: this.$t('common.phoneNo'), dataIndex: 'telephone',width:100,align:"left"},
          { title: this.$t('system.phone'), dataIndex: 'phoneNum',width:100,align:"left"},
          { title: this.$t('common.email'), dataIndex: 'email',width:150,align:"left"},
          { title: this.$t('common.prepaidAmount'),dataIndex: 'advanceIn',width:70,align:"left"},
          { title: this.$t('common.sort'), dataIndex: 'sort', width: 60,align:"left"},
          { title: this.$t('common.status'),dataIndex: 'enabled',width:60,align:"center",
            scopedSlots: { customRender: 'customRenderFlag' }
          }
        ],
        url: {
          list: "/supplier/list",
          delete: "/supplier/delete",
          deleteBatch: "/supplier/deleteBatch",
          importExcelUrl: "/supplier/importMember",
          exportXlsUrl: "/supplier/exportExcel",
          batchSetStatusUrl: "/supplier/batchSetStatus",
          batchSetAdvanceInUrl: "/supplier/batchSetAdvanceIn"
        }
      }
    },
    computed: {
      importExcelUrl: function () {
        return `${window._CONFIG['domianURL']}${this.url.importExcelUrl}`;
      }
    },
    methods: {
      searchReset() {
        this.queryParam = {
          type:'会员',
        }
        this.loadData(1);
      },
      handleImportXls() {
        let importExcelUrl = this.url.importExcelUrl
        let templateUrl = '/doc/member_template.xls'
        let templateName = this.$t('common.detailExcelTemplate')
        this.$refs.modalImportForm.initModal(importExcelUrl, templateUrl, templateName);
        this.$refs.modalImportForm.title = this.$t('system.member');
      },
      handleEdit: function (record) {
        this.$refs.modalForm.edit(record);
        this.$refs.modalForm.title = this.$t('common.edit');
        this.$refs.modalForm.disableSubmit = false;
        if(this.btnEnableList.indexOf(1)===-1) {
          this.$refs.modalForm.isReadOnly = true
        }
      },
      batchSetAdvanceIn() {
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
              postAction(that.url.batchSetAdvanceInUrl, {ids: ids}).then((res) => {
                if(res.code === 200){
                  that.$message.info(this.$t('common.operateSuccess'));
                  that.loadData();
                  that.onClearSelected();
              } else {
                that.$message.warning(res.data && res.data.message ? res.data.message : res.data);
                }
              }).finally(() => {
                that.loading = false;
              });
            }
          });
        }
      }
    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less'
</style>
