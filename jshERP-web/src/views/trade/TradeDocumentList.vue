<template>
  <a-card title="清关单证" :bordered="false">
    <div style="margin-bottom: 16px"><a-radio-group v-model="status" @change="loadData"><a-radio-button value="">全部</a-radio-button><a-radio-button value="缺失">缺失</a-radio-button><a-radio-button value="审核中">审核中</a-radio-button><a-radio-button value="已通过">已通过</a-radio-button></a-radio-group></div>
    <a-table :columns="columns" :dataSource="dataSource" :loading="loading" rowKey="id" :pagination="false">
      <span slot="status" slot-scope="text"><a-tag :color="text === '缺失' ? 'red' : text === '审核中' ? 'orange' : 'green'">{{ text }}</a-tag></span>
      <span slot="exception" slot-scope="text">{{ text || '-' }}</span>
    </a-table>
  </a-card>
</template>

<script>
import { getAction } from '@/api/manage'
export default {
  name: 'TradeDocumentList',
  data () { return { loading: false, status: '', dataSource: [], columns: [
    { title: '发运批次', dataIndex: 'shipmentNo' }, { title: '单证类型', dataIndex: 'documentType' },
    { title: '单证编号', dataIndex: 'documentNo' }, { title: '状态', dataIndex: 'status', scopedSlots: { customRender: 'status' } },
    { title: '责任人', dataIndex: 'ownerName' }, { title: '截止日期', dataIndex: 'dueDate' },
    { title: '异常说明', dataIndex: 'exceptionNote', scopedSlots: { customRender: 'exception' } }
  ] } },
  created () { this.loadData() },
  methods: { async loadData () { this.loading = true; try { const res = await getAction('/trade/document/list', { status: this.status }); this.dataSource = res.data.data || [] } finally { this.loading = false } } }
}
</script>
