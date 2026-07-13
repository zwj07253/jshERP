<template>
  <a-card title="进口成本与毛利" :bordered="false">
    <a-alert message="第一阶段按采购金额比例分摊费用；销售额和毛利为 Demo 演示口径。" type="warning" show-icon style="margin-bottom:16px" />
    <a-table :columns="columns" :dataSource="dataSource" :loading="loading" rowKey="materialName" :pagination="false">
      <span slot="money" slot-scope="text">¥{{ formatMoney(text) }}</span>
      <span slot="margin" slot-scope="text">{{ text }}%</span>
    </a-table>
  </a-card>
</template>

<script>
import { getAction } from '@/api/manage'
export default { name: 'TradeCostReport', data () { return { loading: false, dataSource: [], columns: [
  { title: '发运批次', dataIndex: 'shipmentNo' }, { title: 'SKU', dataIndex: 'materialName' }, { title: '采购成本', dataIndex: 'purchaseCost', scopedSlots: { customRender: 'money' } }, { title: '物流分摊', dataIndex: 'logisticsCost', scopedSlots: { customRender: 'money' } }, { title: '关税分摊', dataIndex: 'dutyCost', scopedSlots: { customRender: 'money' } }, { title: '到岸成本', dataIndex: 'landedCost', scopedSlots: { customRender: 'money' } }, { title: '预估毛利', dataIndex: 'estimatedGrossProfit', scopedSlots: { customRender: 'money' } }, { title: '毛利率', dataIndex: 'grossMargin', scopedSlots: { customRender: 'margin' } }
] } }, created () { this.loadData() }, methods: { async loadData () { this.loading = true; try { const res = await getAction('/trade/cost/profit'); this.dataSource = res.data.data || [] } finally { this.loading = false } }, formatMoney (value) { return Number(value || 0).toLocaleString('zh-CN', { maximumFractionDigits: 2 }) } } }
</script>
