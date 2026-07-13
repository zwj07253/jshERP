<template>
  <div class="trade-dashboard">
    <a-alert
      banner
      message="外贸手表 Demo：中国采购 → 国际发运 → 墨西哥清关 → 海外库存 → 分销销售 → 到岸成本与毛利"
      type="info"
      show-icon
      style="margin-bottom: 16px"
    />

    <a-row :gutter="16">
      <a-col v-for="item in cards" :key="item.key" :xs="24" :sm="12" :xl="6" style="margin-bottom: 16px">
        <a-card :loading="loading" :bordered="false" class="metric-card">
          <div class="metric-title">{{ item.title }}</div>
          <div class="metric-value">{{ item.prefix || '¥' }}{{ formatMoney(overview[item.key]) }}{{ item.suffix || '' }}</div>
          <div class="metric-note">{{ item.note }}</div>
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="16">
      <a-col :xl="14" :lg="24" style="margin-bottom: 16px">
        <a-card title="发运批次进度" :loading="loading" :bordered="false" :body-style="{ padding: '0' }">
          <a-table :columns="shipmentColumns" :dataSource="shipments" :pagination="false" rowKey="id" size="middle">
            <span slot="status" slot-scope="text"><a-tag :color="statusColor(text)">{{ text }}</a-tag></span>
            <span slot="amount" slot-scope="text">¥{{ formatMoney(text) }}</span>
          </a-table>
        </a-card>
      </a-col>
      <a-col :xl="10" :lg="24" style="margin-bottom: 16px">
        <a-card title="清关与单证风险" :loading="loading" :bordered="false" :body-style="{ padding: '0' }">
          <a-list :dataSource="exceptions" size="small">
            <a-list-item slot="renderItem" slot-scope="item">
              <a-list-item-meta :description="item.exceptionNote || item.ownerName">
                <span slot="title">{{ item.shipmentNo }} · {{ item.documentType }}</span>
              </a-list-item-meta>
              <a-tag :color="item.status === '缺失' ? 'red' : 'orange'">{{ item.status }}</a-tag>
            </a-list-item>
          </a-list>
          <div v-if="!exceptions.length" class="empty-note">当前没有待处理的单证异常</div>
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="16">
      <a-col :xl="12" :lg="24" style="margin-bottom: 16px">
        <a-card title="外贸库存状态" :loading="loading" :bordered="false">
          <a-table :columns="inventoryColumns" :dataSource="inventory" :pagination="false" rowKey="statusName" size="small">
            <span slot="quantity" slot-scope="text">{{ formatMoney(text) }}</span>
            <span slot="amount" slot-scope="text">¥{{ formatMoney(text) }}</span>
          </a-table>
        </a-card>
      </a-col>
      <a-col :xl="12" :lg="24" style="margin-bottom: 16px">
        <a-card title="墨西哥销售毛利看板" :loading="loading" :bordered="false">
          <a-table :columns="profitColumns" :dataSource="profit" :pagination="false" rowKey="materialName" size="small">
            <span slot="money" slot-scope="text">¥{{ formatMoney(text) }}</span>
            <span slot="margin" slot-scope="text">{{ formatMoney(text) }}%</span>
            <span slot="customer" slot-scope="text">{{ text || '-' }}</span>
          </a-table>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script>
import { getAction } from '@/api/manage'

export default {
  name: 'TradeDashboard',
  data () {
    return {
      loading: true,
      overview: {},
      shipments: [],
      exceptions: [],
      inventory: [],
      profit: [],
      cards: [
        { key: 'purchaseAmount', title: '外贸采购总额', note: '关联采购与发运批次' },
        { key: 'lockedSalesAmount', title: '墨西哥锁定销售额', note: '已关联海外客户订单' },
        { key: 'availableStockAmount', title: '墨西哥可销售货值', note: '已入库且未被客户锁定' },
        { key: 'transitRiskDocumentCount', title: '在途风险单证数', note: '在途/到港/清关阶段需关注', prefix: '', suffix: ' 单' },
        { key: 'inTransitAmount', title: '海运在途货值', note: '已开船、尚未到港' },
        { key: 'customsAmount', title: '清关中货值', note: '到港待放行货物' },
        { key: 'landedCost', title: '已核算到岸成本', note: '采购、物流、关税和本地费用' },
        { key: 'grossProfit', title: '演示毛利', note: '销售额扣减售出到岸成本' }
      ],
      shipmentColumns: [
        { title: '批次号', dataIndex: 'shipmentNo', width: 150 },
        { title: '柜号', dataIndex: 'containerNo', width: 135 },
        { title: '路线', customRender: (text, record) => `${record.originPort} → ${record.destinationPort}` },
        { title: '状态', dataIndex: 'status', scopedSlots: { customRender: 'status' }, width: 90 },
        { title: '货值', dataIndex: 'purchaseAmount', scopedSlots: { customRender: 'amount' }, width: 120 }
      ],
      inventoryColumns: [
        { title: '库存状态', dataIndex: 'statusName' },
        { title: '数量', dataIndex: 'quantity', scopedSlots: { customRender: 'quantity' }, align: 'right' },
        { title: '货值', dataIndex: 'amount', scopedSlots: { customRender: 'amount' }, align: 'right' }
      ],
      profitColumns: [
        { title: 'SKU', dataIndex: 'materialName' },
        { title: '客户', dataIndex: 'salesCustomerName', scopedSlots: { customRender: 'customer' } },
        { title: '销售额', dataIndex: 'salesAmount', scopedSlots: { customRender: 'money' }, align: 'right' },
        { title: '售出成本', dataIndex: 'soldLandedCost', scopedSlots: { customRender: 'money' }, align: 'right' },
        { title: '实际毛利', dataIndex: 'actualGrossProfit', scopedSlots: { customRender: 'money' }, align: 'right' },
        { title: '毛利率', dataIndex: 'actualGrossMargin', scopedSlots: { customRender: 'margin' }, align: 'right' }
      ]
    }
  },
  created () {
    this.loadData()
  },
  methods: {
    async loadData () {
      this.loading = true
      try {
        const result = await Promise.all([
          getAction('/trade/dashboard/overview'),
          getAction('/trade/shipment/list'),
          getAction('/trade/document/list', { status: '缺失' }),
          getAction('/trade/inventory/status'),
          getAction('/trade/cost/profit')
        ])
        this.overview = result[0].data.data || {}
        this.shipments = result[1].data.data || []
        this.exceptions = result[2].data.data || []
        this.inventory = result[3].data.data || []
        this.profit = (result[4].data.data || []).filter(item => Number(item.salesAmount || 0) > 0).slice(0, 5)
      } finally {
        this.loading = false
      }
    },
    formatMoney (value) {
      return Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })
    },
    statusColor (status) {
      return { '已入库': 'green', '清关中': 'orange', '在途': 'blue', '异常': 'red', '已到港': 'cyan' }[status] || 'default'
    }
  }
}
</script>

<style scoped>
.trade-dashboard { padding: 4px; }
.metric-card { min-height: 132px; }
.metric-title { color: rgba(0, 0, 0, .55); font-size: 14px; }
.metric-value { margin-top: 12px; font-size: 24px; color: #17233d; font-weight: 600; }
.metric-note { margin-top: 8px; color: rgba(0, 0, 0, .45); font-size: 12px; }
.empty-note { padding: 24px; text-align: center; color: rgba(0, 0, 0, .45); }
</style>
