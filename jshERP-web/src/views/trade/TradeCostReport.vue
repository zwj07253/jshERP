<template>
  <a-card title="进口成本与销售毛利" :bordered="false">
    <a-alert
      message="成本先按采购货值比例分摊到批次 SKU，再按已锁定销售数量折算售出到岸成本；关联墨西哥销售单后即可看到实际毛利和毛利率。"
      type="warning"
      show-icon
      style="margin-bottom:16px"
    />

    <div class="table-operator">
      <a-button type="primary" icon="plus" @click="openModal">登记费用</a-button>
      <a-select v-model="allocateShipmentId" placeholder="选择待分摊批次" style="width:220px;margin-left:12px">
        <a-select-option v-for="item in shipments" :key="item.id" :value="item.id">{{ item.shipmentNo }}</a-select-option>
      </a-select>
      <a-button icon="calculator" style="margin-left:8px" @click="allocate">重新分摊</a-button>
    </div>

    <a-table :columns="columns" :dataSource="dataSource" :loading="loading" rowKey="rowKey" :pagination="false" :scroll="{x: 1500}">
      <span slot="money" slot-scope="text">¥{{ formatMoney(text) }}</span>
      <span slot="number" slot-scope="text">{{ formatMoney(text) }}</span>
      <span slot="margin" slot-scope="text">{{ formatMoney(text) }}%</span>
      <span slot="customer" slot-scope="text">{{ text || '-' }}</span>
    </a-table>

    <a-divider>费用登记明细</a-divider>
    <a-table :columns="costColumns" :dataSource="costs" :loading="costLoading" rowKey="id" :pagination="false" size="small">
      <span slot="money" slot-scope="text">¥{{ formatMoney(text) }}</span>
      <span slot="allocated" slot-scope="text">
        <a-tag :color="text === '1' ? 'green' : 'orange'">{{ text === '1' ? '已分摊' : '待分摊' }}</a-tag>
      </span>
    </a-table>

    <a-modal title="登记外贸费用" :visible="modalVisible" :confirmLoading="saving" @ok="save" @cancel="modalVisible=false">
      <a-form-model :label-col="{span:6}" :wrapper-col="{span:16}">
        <a-form-model-item label="发运批次" required>
          <a-select v-model="form.shipmentId">
            <a-select-option v-for="item in shipments" :key="item.id" :value="item.id">{{ item.shipmentNo }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="费用类型" required>
          <a-select v-model="form.costType">
            <a-select-option v-for="item in costTypes" :key="item" :value="item">{{ item }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="原币金额"><a-input-number v-model="form.originalAmount" :min="0" :precision="2" style="width:100%" /></a-form-model-item>
        <a-form-model-item label="币种 / 汇率">
          <a-input-group compact>
            <a-select v-model="form.currency" style="width:35%">
              <a-select-option value="CNY">CNY</a-select-option>
              <a-select-option value="USD">USD</a-select-option>
              <a-select-option value="MXN">MXN</a-select-option>
            </a-select>
            <a-input-number v-model="form.exchangeRate" :min="0" :precision="4" style="width:65%" />
          </a-input-group>
        </a-form-model-item>
        <a-form-model-item label="费用日期"><a-input v-model="form.costDate" placeholder="2026-08-05 10:00:00" /></a-form-model-item>
        <a-form-model-item label="备注"><a-textarea v-model="form.remark" :rows="2" /></a-form-model-item>
      </a-form-model>
    </a-modal>
  </a-card>
</template>

<script>
import { getAction, postAction } from '@/api/manage'

const emptyForm = () => ({ shipmentId: undefined, costType: '海运费', originalAmount: 0, currency: 'CNY', exchangeRate: 1, costDate: '', remark: '' })

export default {
  name: 'TradeCostReport',
  data () {
    return {
      loading: false,
      costLoading: false,
      saving: false,
      modalVisible: false,
      dataSource: [],
      costs: [],
      shipments: [],
      allocateShipmentId: undefined,
      form: emptyForm(),
      costTypes: ['国内运输', '装柜费', '海运费', '保险费', '报关费', '港口费', '仓储费', '关税', 'IVA', '本地配送费', '其他费用'],
      columns: [
        { title: '发运批次', dataIndex: 'shipmentNo', width: 150, fixed: 'left' },
        { title: 'SKU', dataIndex: 'materialName', width: 180, fixed: 'left' },
        { title: '客户', dataIndex: 'salesCustomerName', width: 180, scopedSlots: { customRender: 'customer' } },
        { title: '销售单', dataIndex: 'salesNumber', width: 150 },
        { title: '发运数量', dataIndex: 'shipmentQuantity', width: 90, align: 'right', scopedSlots: { customRender: 'number' } },
        { title: '已售/锁定', dataIndex: 'soldQuantity', width: 90, align: 'right', scopedSlots: { customRender: 'number' } },
        { title: '采购成本', dataIndex: 'purchaseCost', width: 120, align: 'right', scopedSlots: { customRender: 'money' } },
        { title: '物流分摊', dataIndex: 'logisticsCost', width: 120, align: 'right', scopedSlots: { customRender: 'money' } },
        { title: '关税分摊', dataIndex: 'dutyCost', width: 120, align: 'right', scopedSlots: { customRender: 'money' } },
        { title: '完税/到岸成本', dataIndex: 'landedCost', width: 130, align: 'right', scopedSlots: { customRender: 'money' } },
        { title: '销售金额', dataIndex: 'salesAmount', width: 120, align: 'right', scopedSlots: { customRender: 'money' } },
        { title: '售出到岸成本', dataIndex: 'soldLandedCost', width: 130, align: 'right', scopedSlots: { customRender: 'money' } },
        { title: '实际毛利', dataIndex: 'actualGrossProfit', width: 120, align: 'right', scopedSlots: { customRender: 'money' } },
        { title: '实际毛利率', dataIndex: 'actualGrossMargin', width: 110, align: 'right', scopedSlots: { customRender: 'margin' } }
      ],
      costColumns: [
        { title: '批次', dataIndex: 'shipmentNo' },
        { title: '费用类型', dataIndex: 'costType' },
        { title: '原币金额', dataIndex: 'originalAmount', scopedSlots: { customRender: 'money' } },
        { title: '币种', dataIndex: 'currency' },
        { title: '人民币金额', dataIndex: 'cnyAmount', scopedSlots: { customRender: 'money' } },
        { title: '分摊状态', dataIndex: 'allocatedFlag', scopedSlots: { customRender: 'allocated' } },
        { title: '备注', dataIndex: 'remark' }
      ]
    }
  },
  created () {
    this.loadData()
    this.loadCosts()
    this.loadShipments()
  },
  methods: {
    async loadData () {
      this.loading = true
      try {
        const res = await getAction('/trade/cost/profit')
        this.dataSource = (res.data.data || []).map((item, index) => Object.assign({ rowKey: `${item.shipmentNo}-${item.materialName}-${index}` }, item))
      } finally {
        this.loading = false
      }
    },
    async loadCosts () {
      this.costLoading = true
      try {
        const res = await getAction('/trade/cost/list')
        this.costs = res.data.data || []
      } finally {
        this.costLoading = false
      }
    },
    async loadShipments () {
      const res = await getAction('/trade/shipment/list')
      this.shipments = res.data.data || []
    },
    openModal () {
      this.form = emptyForm()
      this.modalVisible = true
    },
    async save () {
      if (!this.form.shipmentId || !this.form.costType) return this.$message.warning('请选择发运批次和费用类型')
      this.saving = true
      try {
        const res = await postAction('/trade/cost/add', this.form)
        if (res.code === 200) {
          this.$message.success('费用已登记，请执行重新分摊')
          this.modalVisible = false
          this.loadCosts()
        } else this.$message.error(res.data || '保存失败')
      } finally {
        this.saving = false
      }
    },
    async allocate () {
      if (!this.allocateShipmentId) return this.$message.warning('请选择要分摊的发运批次')
      const res = await postAction('/trade/cost/allocate', { shipmentId: this.allocateShipmentId })
      if (res.code === 200) {
        this.$message.success('成本分摊已完成')
        this.loadData()
        this.loadCosts()
      } else this.$message.error(res.data || '分摊失败')
    },
    formatMoney (value) {
      return Number(value || 0).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
    }
  }
}
</script>
