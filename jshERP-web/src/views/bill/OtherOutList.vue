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
                <a-form-item :label="$t('common.billNo')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-input :placeholder="$t('common.enterBillNo')" v-model="queryParam.number"></a-input>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item :label="$t('common.materialInfo')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-input :placeholder="$t('common.enterMaterial')" v-model="queryParam.materialParam"></a-input>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item :label="$t('common.billDate')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-range-picker
                    style="width:100%"
                    v-model="queryParam.createTimeRange"
                    format="YYYY-MM-DD"
                    :placeholder="[$t('common.startDate'), $t('common.endDate')]"
                    @change="onDateChange"
                    @ok="onDateOk"
                  />
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
                  <a-form-item :label="$t('common.customer')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-select :placeholder="$t('common.selectCustomer')" showSearch allow-clear optionFilterProp="children" v-model="queryParam.organId" @search="handleSearchCustomer">
                      <div slot="dropdownRender" slot-scope="menu">
                        <v-nodes :vnodes="menu" />
                        <a-divider style="margin: 4px 0;" />
                        <div class="dropdown-btn" @mousedown="e => e.preventDefault()" @click="initCustomer(0)"><a-icon type="reload" /> {{ $t('common.refreshList') }}</div>
                      </div>
                      <a-select-option v-for="(item,index) in cusList" :key="index" :value="item.id">
                        {{ item.supplier }}
                      </a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="24">
                  <a-form-item :label="$t('common.depotName')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-select :placeholder="$t('common.selectWarehouse')" showSearch allow-clear optionFilterProp="children" v-model="queryParam.depotId">
                      <a-select-option v-for="(depot,index) in depotList" :key="index" :value="depot.id">
                        {{ depot.depotName }}
                      </a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="24">
                  <a-form-item :label="$t('common.operator')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-select :placeholder="$t('common.selectOperator')" showSearch allow-clear optionFilterProp="children" v-model="queryParam.creator">
                      <a-select-option v-for="(item,index) in userList" :key="index" :value="item.id">
                        {{ item.userName }}
                      </a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="24">
                  <a-form-item :label="$t('common.linkedBill')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-input :placeholder="$t('common.enterLinkedBill')" v-model="queryParam.linkNumber"></a-input>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="24">
                  <a-form-item :label="$t('common.billStatus')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-select :placeholder="$t('common.selectStatus')" allow-clear v-model="queryParam.status">
                      <a-select-option value="0">{{ $t('common.pending') }}</a-select-option>
                      <a-select-option value="9" v-if="!checkFlag">{{ $t('common.auditing') }}</a-select-option>
                      <a-select-option value="1">{{ $t('common.approved') }}</a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="24">
                  <a-form-item :label="$t('common.billRemark')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-input :placeholder="$t('common.enterRemark')" v-model="queryParam.remark"></a-input>
                  </a-form-item>
                </a-col>
              </a-row>
            </template>
          </a-form>
        </div>
        <!-- 操作按钮区域 -->
        <div class="table-operator"  style="margin-top: 5px">
          <a-button v-if="btnEnableList.indexOf(1)>-1" @click="myHandleAdd" type="primary" icon="plus">{{ $t('common.add') }}</a-button>
          <a-button v-if="inOutManageFlag && btnEnableList.indexOf(1)>-1" @click="handleWaitBill" icon="link">待出库({{waitTotal}})</a-button>
          <a-button v-if="btnEnableList.indexOf(1)>-1" icon="delete" @click="batchDel">{{ $t('common.delete') }}</a-button>
          <a-button v-if="checkFlag && btnEnableList.indexOf(2)>-1" icon="check" @click="batchSetStatus(1)">{{ $t('common.audit') }}</a-button>
          <a-button v-if="checkFlag && btnEnableList.indexOf(7)>-1" icon="stop" @click="batchSetStatus(0)">{{ $t('common.unaudit') }}</a-button>
          <a-button v-if="isShowExcel && btnEnableList.indexOf(3)>-1" icon="download" @click="handleExport">{{ $t('common.export') }}</a-button>
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
          <a-tooltip placement="left" :title="$t('common.otherOutTip')" slot="action">
            <a-icon v-if="btnEnableList.indexOf(1)>-1" type="question-circle" style="font-size:20px;float:right;" />
          </a-tooltip>
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
            :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
            :expandedRowKeys="expandedRowKeys"
            @expand="onExpand"
            @change="handleTableChange">
            <span slot="action" slot-scope="text, record">
              <a @click="myHandleDetail(record, '其它出库', prefixNo)">{{ $t('common.view') }}</a>
              <a-divider v-if="btnEnableList.indexOf(1)>-1" type="vertical" />
              <a v-if="btnEnableList.indexOf(1)>-1" @click="myHandleEdit(record)">{{ $t('common.edit') }}</a>
              <a-divider v-if="btnEnableList.indexOf(1)>-1" type="vertical" />
              <a v-if="btnEnableList.indexOf(1)>-1" @click="myHandleCopyAdd(record)">{{ $t('common.copy') }}</a>
              <a-divider v-if="btnEnableList.indexOf(1)>-1" type="vertical" />
              <a-popconfirm v-if="btnEnableList.indexOf(1)>-1" :title="$t('common.confirmDelete')" @confirm="() => myHandleDelete(record)">
                <a>{{ $t('common.delete') }}</a>
              </a-popconfirm>
            </span>
            <template slot="customRenderStatus" slot-scope="status">
              <a-tag v-if="status == '0'" color="red">{{ $t('common.pending') }}</a-tag>
              <a-tag v-if="status == '1'" color="green">{{ $t('common.approved') }}</a-tag>
              <a-tag v-if="status == '9'" color="orange">{{ $t('common.auditing') }}</a-tag>
            </template>
            <a-table
              bordered
              size="small"
              slot="expandedRowRender"
              slot-scope="record"
              :loading="record.loading"
              :columns="detailColumns"
              :dataSource="record.childrens"
              :row-key="record => record.id"
              :pagination="false">
            </a-table>
          </a-table>
        </div>
        <!-- table区域-end -->
        <!-- 表单区域 -->
        <other-out-modal ref="modalForm" @ok="modalFormOk" @close="modalFormClose"></other-out-modal>
        <bill-detail ref="modalDetail" @ok="modalFormOk" @close="modalFormClose"></bill-detail>
        <bill-excel-iframe ref="billExcelIframe" @ok="modalFormOk" @close="modalFormClose"></bill-excel-iframe>
        <batch-wait-bill-list ref="batchWaitBill" @ok="modalFormOk" @close="waitModalFormClose"></batch-wait-bill-list>
      </a-card>
    </a-col>
  </a-row>
</template>
<!--power by j i s h e n g h u a-->
<script>
  import OtherOutModal from './modules/OtherOutModal'
  import BillDetail from './dialog/BillDetail'
  import BillExcelIframe from '@/components/tools/BillExcelIframe'
  import BatchWaitBillList from './dialog/BatchWaitBillList'
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import { BillListMixin } from './mixins/BillListMixin'
  import JEllipsis from '@/components/jeecg/JEllipsis'
  import JDate from '@/components/jeecg/JDate'
  import { deleteAction } from '@/api/manage'
  export default {
    name: "OtherOutList",
    mixins:[JeecgListMixin,BillListMixin],
    components: {
      OtherOutModal,
      BillDetail,
      BillExcelIframe,
      BatchWaitBillList,
      JEllipsis,
      JDate,
      VNodes: {
        functional: true,
        render: (h, ctx) => ctx.props.vnodes,
      }
    },
    data () {
      return {
        // 查询条件
        queryParam: {
          number: "",
          materialParam: "",
          type: "出库",
          subType: "其它",
          organId: undefined,
          depotId: undefined,
          creator: undefined,
          linkNumber: "",
          status: undefined,
          remark: ""
        },
        prefixNo: 'QTCK',
        urlPath: '/bill/other_out',
        //出入库管理开关，适合独立仓管场景
        inOutManageFlag: false,
        labelCol: {
          span: 5
        },
        wrapperCol: {
          span: 18,
          offset: 1
        },
        // 默认索引
        defDataIndex:['action','organName','number','materialsList','operTimeStr','userName','materialCount','totalPrice','status'],
        // 默认列
        defColumns: [
          {
            title: this.$t('common.action'),
            dataIndex: 'action',
            align:"center", width: 160,
            scopedSlots: { customRender: 'action' },
          },
          { title: this.$t('common.customer'), dataIndex: 'organName',width:120, ellipsis:true},
          { title: this.$t('common.billNo'), dataIndex: 'number',width:160,
            customRender:function (text,record,index) {
              text = record.linkNumber?text+"[转]":text
              return text
            }
          },
          { title: this.$t('common.linkedBill'), dataIndex: 'linkNumber',width:140},
          { title: this.$t('common.materialInfo'), dataIndex: 'materialsList',width:220, ellipsis:true},
          { title: this.$t('common.billDate'), dataIndex: 'operTimeStr',width:145},
          { title: this.$t('common.operator'), dataIndex: 'userName',width:80, ellipsis:true},
          { title: this.$t('common.quantity'), dataIndex: 'materialCount',width:60},
          { title: this.$t('common.totalAmount'), dataIndex: 'totalPrice',width:80},
          { title: this.$t('common.remark'), dataIndex: 'remark',width:200},
          { title: this.$t('common.status'), dataIndex: 'status', width: 80, align: "center",
            scopedSlots: { customRender: 'customRenderStatus' }
          }
        ],
        url: {
          list: "/depotHead/list",
          delete: "/depotHead/delete",
          deleteBatch: "/depotHead/deleteBatch",
          batchSetStatusUrl: "/depotHead/batchSetStatus"
        }
      }
    },
    computed: {
    },
    created() {
      this.initSystemConfig()
      this.initCustomer()
      this.getDepotData()
      this.initUser()
      this.initWaitBillCount('出库', '销售,采购退货', '1')
    },
    methods: {
      searchQuery() {
        this.loadData(1)
        if(this.inOutManageFlag) {
          this.initWaitBillCount('出库', '销售,采购退货', '1')
        }
      },
      searchReset() {
        this.queryParam = {
          type: this.queryParam.type,
          subType: this.queryParam.subType
        }
        this.loadData(1)
        if(this.inOutManageFlag) {
          this.initWaitBillCount('出库', '销售,采购退货', '1')
        }
      },
      myHandleDelete(record) {
        let that = this
        if(record.status === '0') {
          deleteAction(that.url.delete, {id: record.id}).then((res) => {
            if(res.code === 200){
              that.loadData(1)
              if(that.inOutManageFlag) {
                that.initWaitBillCount('出库', '销售,采购退货', '1')
              }
            } else {
              that.$message.warning(res.data.message);
            }
          })
        } else {
          this.$message.warning(this.$t('common.deleteUnapprovedOnly'))
        }
      },
      batchDel: function () {
        if (this.selectedRowKeys.length <= 0) {
          this.$message.warning(this.$t('common.selectRecord'))
        } else {
          let ids = ""
          for (let i = 0; i < this.selectedRowKeys.length; i++) {
            ids += this.selectedRowKeys[i] + ","
          }
          let that = this
          this.$confirm({
            title: this.$t('common.confirmAction'),
            content: this.$t('common.confirmDeleteSelected'),
            onOk: function () {
              that.loading = true
              deleteAction(that.url.deleteBatch, {ids: ids}).then((res) => {
                if(res.code === 200){
                  that.loadData()
                  that.onClearSelected()
                  if(that.inOutManageFlag) {
                    that.initWaitBillCount('出库', '销售,采购退货', '1')
                  }
                } else {
                  that.$message.warning(res.data.message)
                }
              }).finally(() => {
                that.loading = false
              })
            }
          })
        }
      },
      //待出库
      handleWaitBill() {
        this.$refs.batchWaitBill.show('出库', '销售,采购退货', "1")
        this.$refs.batchWaitBill.title = "批量选择销售出库或采购退货"
      },
      waitModalFormClose() {
        this.loadData()
        this.initWaitBillCount('出库', '销售,采购退货', '1')
      },
    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less'
</style>
