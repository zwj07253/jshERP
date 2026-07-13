<template>
  <a-card title="外贸库存状态" :bordered="false">
    <a-alert message="可销售库存与客户锁定库存分开统计；在途与清关中库存不计入墨西哥可销售库存。" type="info" show-icon style="margin-bottom:16px" />
    <a-table :columns="columns" :dataSource="dataSource" :loading="loading" rowKey="statusName" :pagination="false">
      <span slot="quantity" slot-scope="text">{{ formatNumber(text) }}</span>
      <span slot="amount" slot-scope="text">¥{{ formatNumber(text) }}</span>
    </a-table>
  </a-card>
</template>

<script>
import { getAction } from '@/api/manage'
export default { name: 'TradeInventory', data () { return { loading: false, dataSource: [], columns: [
  { title: '库存状态', dataIndex: 'statusName' }, { title: '数量', dataIndex: 'quantity', scopedSlots: { customRender: 'quantity' }, align: 'right' }, { title: '货值', dataIndex: 'amount', scopedSlots: { customRender: 'amount' }, align: 'right' }
] } }, created () { this.loadData() }, methods: { async loadData () { this.loading = true; try { const res = await getAction('/trade/inventory/status'); this.dataSource = res.data.data || [] } finally { this.loading = false } }, formatNumber (value) { return Number(value || 0).toLocaleString('zh-CN', { maximumFractionDigits: 2 }) } } }
</script>
