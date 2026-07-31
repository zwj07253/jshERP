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
                <a-form-item :label="$t('retail.billNumber')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-input :placeholder="$t('retail.enterBillNumber')" v-model="queryParam.number"></a-input>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item :label="$t('retail.materialInfo')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-input :placeholder="$t('retail.enterMaterial')" v-model="queryParam.materialParam"></a-input>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item :label="$t('retail.billDate')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-range-picker
                    style="width:100%"
                    v-model="queryParam.createTimeRange"
                    format="YYYY-MM-DD"
                    :placeholder="[$t('retail.startDate'), $t('retail.endDate')]"
                    @change="onDateChange"
                    @ok="onDateOk"
                  />
                </a-form-item>
              </a-col>
              <span style="float: left;overflow: hidden;" class="table-page-search-submitButtons">
                <a-col :md="6" :sm="24">
                  <a-button type="primary" @click="searchQuery">{{ $t('retail.search') }}</a-button>
                  <a-button style="margin-left: 8px" @click="searchReset">{{ $t('retail.reset') }}</a-button>
                  <a @click="handleToggleSearch" style="margin-left: 8px">
                    {{ toggleSearchStatus ? $t('retail.collapse') : $t('retail.expand') }}
                    <a-icon :type="toggleSearchStatus ? 'up' : 'down'"/>
                  </a>
                </a-col>
              </span>
            </a-row>
            <template v-if="toggleSearchStatus">
              <a-row :gutter="24">
                <a-col :md="6" :sm="24">
                <a-form-item :label="$t('purchase.supplier')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-select :placeholder="$t('purchase.selectSupplier')" showSearch allow-clear optionFilterProp="children" v-model="queryParam.organId" @search="handleSearchSupplier">
                      <div slot="dropdownRender" slot-scope="menu">
                        <v-nodes :vnodes="menu" />
                        <a-divider style="margin: 4px 0;" />
                        <div class="dropdown-btn" @mousedown="e => e.preventDefault()" @click="initSupplier(0)"><a-icon type="reload" /> {{ $t('purchase.refreshList') }}</div>
                      </div>
                      <a-select-option v-for="(item,index) in supList" :key="index" :value="item.id">
                        {{ item.supplier }}
                      </a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="24">
                <a-form-item :label="$t('retail.warehouse')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-select :placeholder="$t('retail.selectWarehouse')" showSearch allow-clear optionFilterProp="children" v-model="queryParam.depotId">
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
                <a-form-item :label="$t('purchase.linkedOrder')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-input :placeholder="$t('purchase.enterLinkedOrder')" v-model="queryParam.linkNumber"></a-input>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="24">
                  <a-form-item :label="$t('retail.settlementAccount')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-select :placeholder="$t('retail.selectAccount')" showSearch allow-clear optionFilterProp="children" v-model="queryParam.accountId">
                      <a-select-option v-for="(item,index) in accountList" :key="index" :value="item.id">
                        {{ item.name }}
                      </a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="24">
                <a-form-item :label="$t('purchase.currentDebt')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-select :placeholder="$t('purchase.selectCurrentDebt')" allow-clear v-model="queryParam.hasDebt">
                    <a-select-option value="1">{{ $t('purchase.hasDebt') }}</a-select-option>
                    <a-select-option value="0">{{ $t('purchase.noDebt') }}</a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="24">
                <a-form-item :label="$t('purchase.finalDebt')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                  <a-select :placeholder="$t('purchase.selectFinalDebt')" allow-clear v-model="queryParam.hasLastDebt">
                    <a-select-option value="1">{{ $t('purchase.hasDebt') }}</a-select-option>
                    <a-select-option value="0">{{ $t('purchase.noDebt') }}</a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
                <a-col :md="6" :sm="24">
                  <a-form-item :label="$t('common.billStatus')" :labelCol="labelCol" :wrapperCol="wrapperCol">
                    <a-select :placeholder="$t('common.selectStatus')" allow-clear v-model="queryParam.status">
                    <a-select-option value="0">{{ $t('retail.pending') }}</a-select-option>
                    <a-select-option value="9" v-if="!checkFlag">{{ $t('retail.auditing') }}</a-select-option>
                    <a-select-option value="1">{{ $t('retail.approved') }}</a-select-option>
                    <a-select-option value="3">{{ $t('purchase.partialInbound') }}</a-select-option>
                    <a-select-option value="2">{{ $t('purchase.completedInbound') }}</a-select-option>
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
          <a-button v-if="btnEnableList.indexOf(1)>-1" @click="myHandleAdd" type="primary" icon="plus">{{ $t('retail.add') }}</a-button>
          <a-button v-if="btnEnableList.indexOf(1)>-1" icon="delete" @click="batchDel">{{ $t('retail.delete') }}</a-button>
          <a-button v-if="quickBtn.purchaseBack.indexOf(1)>-1 && btnEnableList.indexOf(1)>-1" icon="share-alt" @click="transferBill($t('purchase.transferToReturn'), quickBtn.purchaseBack)">{{ $t('purchase.transferToReturn') }}</a-button>
          <a-tooltip :title="$t('purchase.forceCloseInboundTip')">
            <a-button v-if="inOutManageFlag && btnEnableList.indexOf(1)>-1" icon="issues-close" @click="batchForceClose">{{ $t('common.forceClose') }}</a-button>
          </a-tooltip>
          <a-button v-if="checkFlag && btnEnableList.indexOf(2)>-1" icon="check" @click="batchSetStatus(1)">{{ $t('common.audit') }}</a-button>
          <a-button v-if="checkFlag && btnEnableList.indexOf(7)>-1" icon="stop" @click="batchSetStatus(0)">{{ $t('common.unaudit') }}</a-button>
          <a-tooltip :title="$t('purchase.correctDebtTip')">
            <a-button v-if="btnEnableList.indexOf(1)>-1" icon="fund" @click="batchSetLastDebt">{{ $t('purchase.correctDebt') }}</a-button>
          </a-tooltip>
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
                    {{ $t('common.restoreColumns') }}：<a-button @click="handleRestDefault" type="link" size="small">{{ $t('common.restoreDefault') }}</a-button>
                  </a-col>
                </a-row>
              </a-checkbox-group>
            </template>
            <a-button icon="setting">{{ $t('common.columnSettings') }}</a-button>
          </a-popover>
          <a-tooltip placement="left" :title="$t('purchase.inboundTip')" slot="action">
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
              <a @click="myHandleDetail(record, '采购入库', prefixNo)">{{ $t('retail.view') }}</a>
              <a-divider v-if="btnEnableList.indexOf(1)>-1" type="vertical" />
              <a v-if="btnEnableList.indexOf(1)>-1" @click="myHandleEdit(record)">{{ $t('common.edit') }}</a>
              <a-divider v-if="btnEnableList.indexOf(1)>-1" type="vertical" />
              <a v-if="btnEnableList.indexOf(1)>-1" @click="myHandleCopyAdd(record)">{{ $t('common.copy') }}</a>
              <a-divider v-if="btnEnableList.indexOf(1)>-1" type="vertical" />
              <a-popconfirm v-if="btnEnableList.indexOf(1)>-1" :title="$t('common.confirmDelete')" @confirm="() => myHandleDelete(record)">
                <a>{{ $t('common.delete') }}</a>
              </a-popconfirm>
            </span>
            <template slot="customRenderDebt" slot-scope="value, record">
              <a-tooltip :title="$t('purchase.hasPaymentBill')">
                <span style="color:green" v-if="value>0 && value>record.lastDebt">{{value}}</span>
              </a-tooltip>
              <a-tooltip :title="$t('purchase.noPayment')">
                <span style="color:red" v-if="value>0 && value===record.lastDebt">{{value}}</span>
              </a-tooltip>
              <span v-if="value===0">{{value}}</span>
            </template>
            <template slot="customRenderStatus" slot-scope="status">
              <a-tag v-if="status == '0'" color="red">{{ $t('retail.pending') }}</a-tag>
              <a-tag v-if="status == '1'" color="green">{{ $t('retail.approved') }}</a-tag>
              <a-tag v-if="status == '2'" color="cyan">{{ $t('purchase.completedInbound') }}</a-tag>
              <a-tag v-if="status == '3'" color="blue">{{ $t('purchase.partialInbound') }}</a-tag>
              <a-tag v-if="status == '9'" color="orange">{{ $t('retail.auditing') }}</a-tag>
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
        <purchase-in-modal ref="modalForm" @ok="modalFormOk" @close="modalFormClose"></purchase-in-modal>
        <purchase-back-modal ref="transferModalForm" @ok="modalFormOk" @close="modalFormClose"></purchase-back-modal>
        <bill-detail ref="modalDetail" @ok="modalFormOk" @close="modalFormClose"></bill-detail>
        <bill-excel-iframe ref="billExcelIframe" @ok="modalFormOk" @close="modalFormClose"></bill-excel-iframe>
      </a-card>
    </a-col>
  </a-row>
</template>
<!-- by ji sheng hua-->
<script>
  import PurchaseInModal from './modules/PurchaseInModal'
  import PurchaseBackModal from './modules/PurchaseBackModal'
  import BillDetail from './dialog/BillDetail'
  import BillExcelIframe from '@/components/tools/BillExcelIframe'
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import { BillListMixin } from './mixins/BillListMixin'
  import JEllipsis from '@/components/jeecg/JEllipsis'
  import JDate from '@/components/jeecg/JDate'
  import Vue from 'vue'
  export default {
    name: "PurchaseInList",
    mixins:[JeecgListMixin,BillListMixin],
    components: {
      PurchaseInModal,
      PurchaseBackModal,
      BillDetail,
      BillExcelIframe,
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
          type: "入库",
          subType: "采购",
          organId: undefined,
          depotId: undefined,
          creator: undefined,
          linkNumber: "",
          accountId: undefined,
          hasDebt: undefined,
          hasLastDebt: undefined,
          status: undefined,
          remark: ""
        },
        prefixNo: 'CGRK',
        urlPath: '/bill/purchase_in',
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
        defDataIndex:['action','organName','number','materialsList','operTimeStr','userName','materialCount','totalPrice','totalTaxLastMoney',
          'changeAmount','debt','lastDebt','status'],
        // 默认列
        defColumns: [
          {
            title: this.$t('common.action'),
            dataIndex: 'action',
            align:"center", width: 180,
            scopedSlots: { customRender: 'action' },
          },
          { title: this.$t('purchase.supplier'), dataIndex: 'organName',width:120, ellipsis:true},
          { title: this.$t('retail.billNumber'), dataIndex: 'number',width:160,
            customRender:function (text,record,index) {
              text = record.linkNumber?text+"[Ord]":text
              text = record.hasBackFlag?text+"[Ret]":text
              return text
            }
          },
          { title: this.$t('purchase.linkedOrder'), dataIndex: 'linkNumber',width:140},
          { title: this.$t('retail.materialInfo'), dataIndex: 'materialsList',width:220, ellipsis:true},
          { title: this.$t('retail.billDate'), dataIndex: 'operTimeStr',width:145},
          { title: this.$t('common.operator'), dataIndex: 'userName',width:80, ellipsis:true},
          { title: this.$t('common.quantity'), dataIndex: 'materialCount',width:60},
          { title: this.$t('common.totalAmount'), dataIndex: 'totalPrice',width:80},
          { title: this.$t('purchase.taxInclusiveTotal'), dataIndex: 'totalTaxLastMoney',width:80,
            customRender:function (text,record,index) {
              return (record.discountMoney + record.discountLastMoney).toFixed(2);
            }
          },
          { title: this.$t('purchase.form.discount'), dataIndex: 'discount',width:60,
            customRender:function (text,record,index) {
              return text? text + '%':''
            }
          },
          { title: this.$t('purchase.form.discountMoney'), dataIndex: 'discountMoney',width:80},
          { title: this.$t('purchase.form.otherMoney'), dataIndex: 'otherMoney',width:80},
          { title: this.$t('purchase.pendingPayment'), dataIndex: 'needInMoney',width:80,
            customRender:function (text,record,index) {
              let needInMoney = record.discountLastMoney + record.otherMoney - record.deposit
              return needInMoney? needInMoney.toFixed(2):0
            }
          },
          { title: this.$t('retail.settlementAccount'), dataIndex: 'accountName',width:80},
          { title: this.$t('purchase.form.deposit'), dataIndex: 'deposit',width:80},
          { title: this.$t('purchase.form.changeAmount'), dataIndex: 'changeAmount',width:80},
          { title: this.$t('purchase.currentDebt'), dataIndex: 'debt',width:80,
            scopedSlots: { customRender: 'customRenderDebt' }
          },
          { title: this.$t('purchase.finalDebt'), dataIndex: 'lastDebt',width:80},
          { title: this.$t('common.remark'), dataIndex: 'remark',width:200},
          { title: this.$t('common.status'), dataIndex: 'status', width: 80, align: "center",
            scopedSlots: { customRender: 'customRenderStatus' }
          }
        ],
        url: {
          list: "/depotHead/list",
          delete: "/depotHead/delete",
          deleteBatch: "/depotHead/deleteBatch",
          forceCloseBatch: "/depotHead/forceCloseBatch",
          batchSetLastDebtUrl: "/depotHead/batchSetLastDebt",
          batchSetStatusUrl: "/depotHead/batchSetStatus"
        }
      }
    },
    computed: {
    },
    created () {
      this.initSystemConfig()
      this.initSupplier()
      this.getDepotData()
      this.initUser()
      this.initAccount()
      this.initQuickBtn()
      this.getDepotByCurrentUser()
    },
    methods: {
    }
  }
</script>
<style scoped>
  @import '~@assets/less/common.less'
</style>
