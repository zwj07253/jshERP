<template>
  <a-card title="外贸库存状态" :bordered="false">
    <a-alert
      message="可销售库存、客户锁定库存、海运在途和清关中库存分开统计；明细按发运批次和 SKU 展示，便于演示每批货当前在哪里。"
      type="info"
      show-icon
      style="margin-bottom:16px"
    />

    <a-row :gutter="16" style="margin-bottom:16px">
      <a-col v-for="item in statusData" :key="item.statusName" :span="6">
        <a-card size="small" :bordered="true">
          <div class="summary-title">{{ item.statusName }}</div>
          <div class="summary-value">{{ formatNumber(item.quantity) }}</div>
          <div class="summary-sub">货值 ¥{{ formatNumber(item.amount) }}</div>
        </a-card>
      </a-col>
    </a-row>

    <div class="table-page-search-wrapper">
      <a-form layout="inline">
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="库存状态">
              <a-select v-model="query.status" allow-clear placeholder="全部状态" style="width:220px" @change="loadDetail">
                <a-select-option v-for="item in statusData" :key="item.statusName" :value="item.statusName">{{ item.statusName }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-button type="primary" icon="reload" @click="reload">刷新</a-button>
          </a-col>
        </a-row>
      </a-form>
    </div>

    <a-table
      :columns="columns"
      :dataSource="detailData"
      :loading="loading"
      rowKey="rowKey"
      :pagination="{ pageSize: 10 }"
      :scroll="{ x: 1400 }"
      size="middle"
    >
      <span slot="status" slot-scope="text"><a-tag :color="statusColor(text)">{{ text }}</a-tag></span>
      <span slot="number" slot-scope="text">{{ formatNumber(text) }}</span>
      <span slot="money" slot-scope="text">¥{{ formatNumber(text) }}</span>
      <span slot="customer" slot-scope="text">{{ text || '-' }}</span>
    </a-table>
  </a-card>
</template>

<script>
import { getAction } from '@/api/manage'

export default {
  name: 'TradeInventory',
  data () {
    return {
      loading: false,
      statusData: [],
      detailData: [],
      query: { status: undefined },
      columns: [
        { title: '库存状态', dataIndex: 'statusName', width: 140, fixed: 'left', scopedSlots: { customRender: 'status' } },
        { title: '发运批次', dataIndex: 'shipmentNo', width: 150 },
        { title: '批次状态', dataIndex: 'shipmentStatus', width: 110 },
        { title: '采购单', dataIndex: 'purchaseNumber', width: 150 },
        { title: 'SKU', dataIndex: 'materialName', width: 180 },
        { title: '型号', dataIndex: 'model', width: 140 },
        { title: '发运数量', dataIndex: 'shipmentQuantity', width: 100, align: 'right', scopedSlots: { customRender: 'number' } },
        { title: '在途', dataIndex: 'inTransitQuantity', width: 90, align: 'right', scopedSlots: { customRender: 'number' } },
        { title: '清关中', dataIndex: 'clearedQuantity', width: 90, align: 'right', scopedSlots: { customRender: 'number' } },
        { title: '已入库', dataIndex: 'stockedQuantity', width: 90, align: 'right', scopedSlots: { customRender: 'number' } },
        { title: '可销售', dataIndex: 'availableQuantity', width: 90, align: 'right', scopedSlots: { customRender: 'number' } },
        { title: '客户锁定', dataIndex: 'lockedQuantity', width: 100, align: 'right', scopedSlots: { customRender: 'number' } },
        { title: '采购货值', dataIndex: 'purchaseAmount', width: 120, align: 'right', scopedSlots: { customRender: 'money' } },
        { title: '销售金额', dataIndex: 'salesAmount', width: 120, align: 'right', scopedSlots: { customRender: 'money' } },
        { title: '销售单', dataIndex: 'salesNumber', width: 160 },
        { title: '锁定客户', dataIndex: 'salesCustomerName', width: 180, scopedSlots: { customRender: 'customer' } }
      ]
    }
  },
  created () {
    this.reload()
  },
  methods: {
    async reload () {
      await Promise.all([this.loadStatus(), this.loadDetail()])
    },
    async loadStatus () {
      const res = await getAction('/trade/inventory/status')
      this.statusData = res.data.data || []
    },
    async loadDetail () {
      this.loading = true
      try {
        const res = await getAction('/trade/inventory/detail', { status: this.query.status })
        this.detailData = (res.data.data || []).map((item, index) => Object.assign({ rowKey: `${item.shipmentNo}-${item.materialId}-${index}` }, item))
      } finally {
        this.loading = false
      }
    },
    formatNumber (value) {
      return Number(value || 0).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
    },
    statusColor (status) {
      return { '海运在途': 'blue', '清关中': 'orange', '墨西哥可销售库存': 'green', '客户锁定库存': 'purple', '已消耗': 'default' }[status] || 'default'
    }
  }
}
</script>

<style scoped>
.summary-title {
  color: rgba(0, 0, 0, 0.65);
  font-size: 13px;
}
.summary-value {
  margin-top: 8px;
  color: #001529;
  font-size: 24px;
  font-weight: 600;
  line-height: 32px;
}
.summary-sub {
  margin-top: 4px;
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
}
</style>
