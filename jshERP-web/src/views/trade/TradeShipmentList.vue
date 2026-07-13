<template>
  <a-card title="发运批次" :bordered="false">
    <a-table :columns="columns" :dataSource="dataSource" :loading="loading" rowKey="id" :pagination="false">
      <span slot="status" slot-scope="text"><a-tag :color="statusColor(text)">{{ text }}</a-tag></span>
      <span slot="amount" slot-scope="text">¥{{ formatMoney(text) }}</span>
      <span slot="action" slot-scope="text, record"><a @click="showDetail(record)">查看履约详情</a></span>
    </a-table>
    <a-drawer title="发运批次详情" :visible="drawerVisible" width="760" @close="drawerVisible=false">
      <a-descriptions v-if="detail.info" :column="2" bordered size="small">
        <a-descriptions-item label="发运批次">{{ detail.info.shipmentNo }}</a-descriptions-item>
        <a-descriptions-item label="当前状态">{{ detail.info.status }}</a-descriptions-item>
        <a-descriptions-item label="柜号">{{ detail.info.containerNo }}</a-descriptions-item>
        <a-descriptions-item label="提单号">{{ detail.info.billOfLadingNo }}</a-descriptions-item>
        <a-descriptions-item label="线路">{{ detail.info.originPort }} → {{ detail.info.destinationPort }}</a-descriptions-item>
        <a-descriptions-item label="预计到港">{{ detail.info.estimatedArrivalDate }}</a-descriptions-item>
      </a-descriptions>
      <a-divider>SKU 明细</a-divider>
      <a-table :columns="itemColumns" :dataSource="detail.items || []" rowKey="id" :pagination="false" size="small" />
      <a-divider>运输与清关单证</a-divider>
      <a-timeline>
        <a-timeline-item v-for="doc in detail.documents || []" :key="doc.id" :color="doc.status === '缺失' ? 'red' : 'blue'">
          {{ doc.documentType }}：{{ doc.status }}<span v-if="doc.exceptionNote">（{{ doc.exceptionNote }}）</span>
        </a-timeline-item>
      </a-timeline>
    </a-drawer>
  </a-card>
</template>

<script>
import { getAction } from '@/api/manage'
export default {
  name: 'TradeShipmentList',
  data () {
    return {
      loading: false, drawerVisible: false, dataSource: [], detail: {},
      columns: [
        { title: '批次号', dataIndex: 'shipmentNo' }, { title: '柜号', dataIndex: 'containerNo' },
        { title: '起运港', dataIndex: 'originPort' }, { title: '目的港', dataIndex: 'destinationPort' },
        { title: '状态', dataIndex: 'status', scopedSlots: { customRender: 'status' } },
        { title: '采购货值', dataIndex: 'purchaseAmount', scopedSlots: { customRender: 'amount' } },
        { title: '操作', scopedSlots: { customRender: 'action' } }
      ],
      itemColumns: [
        { title: 'SKU', dataIndex: 'materialName' }, { title: '型号', dataIndex: 'model' },
        { title: '数量', dataIndex: 'quantity' }, { title: '在途', dataIndex: 'inTransitQuantity' },
        { title: '已清关', dataIndex: 'clearedQuantity' }, { title: '已入库', dataIndex: 'stockedQuantity' }
      ]
    }
  },
  created () { this.loadData() },
  methods: {
    async loadData () { this.loading = true; try { const res = await getAction('/trade/shipment/list'); this.dataSource = res.data.data || [] } finally { this.loading = false } },
    async showDetail (record) { const res = await getAction('/trade/shipment/detail', { id: record.id }); this.detail = res.data.data || {}; this.drawerVisible = true },
    formatMoney (value) { return Number(value || 0).toLocaleString('zh-CN', { maximumFractionDigits: 2 }) },
    statusColor (status) { return { '已入库': 'green', '清关中': 'orange', '在途': 'blue' }[status] || 'default' }
  }
}
</script>
