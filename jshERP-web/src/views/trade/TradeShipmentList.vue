<template>
  <a-card title="发运批次" :bordered="false">
    <div class="table-operator">
      <a-button type="primary" icon="plus" @click="openModal()">新增发运批次</a-button>
    </div>

    <a-table :columns="columns" :dataSource="dataSource" :loading="loading" rowKey="id" :pagination="false">
      <span slot="status" slot-scope="text"><a-tag :color="statusColor(text)">{{ text }}</a-tag></span>
      <span slot="amount" slot-scope="text">¥{{ formatMoney(text) }}</span>
      <span slot="action" slot-scope="text,record">
        <a @click="showDetail(record)">履约详情</a>
        <a-divider type="vertical" />
        <a @click="openModal(record)">编辑</a>
        <a-divider v-if="nextStatus(record.status)" type="vertical" />
        <a v-if="nextStatus(record.status)" @click="advanceStatus(record)">推进至{{ nextStatus(record.status) }}</a>
      </span>
    </a-table>

    <a-modal :title="form.id ? '编辑发运批次' : '新增发运批次'" :visible="modalVisible" :confirmLoading="saving" @ok="save" @cancel="modalVisible=false">
      <a-form-model :label-col="{span:6}" :wrapper-col="{span:16}">
        <a-form-model-item label="发运批次号" required><a-input v-model="form.shipmentNo" :disabled="!!form.id" placeholder="例如 SH-MX-2026-004" /></a-form-model-item>
        <a-form-model-item label="柜号"><a-input v-model="form.containerNo" /></a-form-model-item>
        <a-form-model-item label="提单号"><a-input v-model="form.billOfLadingNo" /></a-form-model-item>
        <a-form-model-item label="起运港" required><a-input v-model="form.originPort" /></a-form-model-item>
        <a-form-model-item label="目的港" required><a-input v-model="form.destinationPort" /></a-form-model-item>
        <a-form-model-item label="承运商"><a-input v-model="form.carrierName" /></a-form-model-item>
        <a-form-model-item label="贸易术语">
          <a-select v-model="form.incoterms">
            <a-select-option value="CIF">CIF</a-select-option>
            <a-select-option value="FOB">FOB</a-select-option>
            <a-select-option value="EXW">EXW</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="币种 / 汇率">
          <a-input-group compact>
            <a-select v-model="form.currency" style="width:35%">
              <a-select-option value="USD">USD</a-select-option>
              <a-select-option value="CNY">CNY</a-select-option>
              <a-select-option value="MXN">MXN</a-select-option>
            </a-select>
            <a-input-number v-model="form.exchangeRate" :min="0" :precision="4" style="width:65%" />
          </a-input-group>
        </a-form-model-item>
        <a-form-model-item label="预计到港"><a-input v-model="form.estimatedArrivalDate" placeholder="2026-08-05 10:00:00" /></a-form-model-item>
        <a-form-model-item label="备注"><a-textarea v-model="form.remark" :rows="2" /></a-form-model-item>
      </a-form-model>
    </a-modal>

    <a-drawer title="发运批次详情" :visible="drawerVisible" width="1100" @close="drawerVisible=false">
      <a-descriptions v-if="detail.info" :column="2" bordered size="small">
        <a-descriptions-item label="发运批次">{{ detail.info.shipmentNo }}</a-descriptions-item>
        <a-descriptions-item label="当前状态">{{ detail.info.status }}</a-descriptions-item>
        <a-descriptions-item label="柜号">{{ detail.info.containerNo }}</a-descriptions-item>
        <a-descriptions-item label="提单号">{{ detail.info.billOfLadingNo }}</a-descriptions-item>
        <a-descriptions-item label="线路">{{ detail.info.originPort }} → {{ detail.info.destinationPort }}</a-descriptions-item>
        <a-descriptions-item label="预计到港">{{ detail.info.estimatedArrivalDate }}</a-descriptions-item>
      </a-descriptions>

      <a-divider>SKU 明细</a-divider>
      <div class="table-operator">
        <a-button type="primary" size="small" icon="plus" @click="openItemModal()">关联采购 SKU</a-button>
      </div>
      <a-table :columns="itemColumns" :dataSource="detail.items || []" rowKey="id" :pagination="false" size="small" :scroll="{x: 1300}">
        <span slot="money" slot-scope="text">¥{{ formatMoney(text) }}</span>
        <span slot="itemAction" slot-scope="text,record">
          <template v-if="record.depotItemId">
            <a @click="openItemModal(record)">修改数量</a>
            <a-divider type="vertical" />
          </template>
          <template v-else>
            <span>演示明细</span>
            <a-divider type="vertical" />
          </template>
          <a @click="openSalesModal(record)">关联销售</a>
          <template v-if="record.salesDepotItemId">
            <a-divider type="vertical" />
            <a-popconfirm title="确认取消关联销售单？" @confirm="unlinkSales(record)"><a>取消销售</a></a-popconfirm>
          </template>
          <template v-if="record.depotItemId">
            <a-divider type="vertical" />
            <a-popconfirm title="确认移除此 SKU 明细？" @confirm="removeItem(record)"><a>移除</a></a-popconfirm>
          </template>
        </span>
      </a-table>

      <a-divider>运输与清关单证</a-divider>
      <a-timeline>
        <a-timeline-item v-for="doc in detail.documents || []" :key="doc.id" :color="doc.status === '缺失' ? 'red' : 'blue'">
          {{ doc.documentType }}：{{ doc.status }}<span v-if="doc.exceptionNote">（{{ doc.exceptionNote }}）</span>
        </a-timeline-item>
      </a-timeline>
    </a-drawer>

    <a-modal :title="itemForm.id ? '修改发运数量' : '关联采购 SKU'" :visible="itemModalVisible" :confirmLoading="itemSaving" @ok="saveItem" @cancel="itemModalVisible=false">
      <a-form-model :label-col="{span:6}" :wrapper-col="{span:16}">
        <a-form-model-item label="采购单明细" required>
          <a-select v-model="itemForm.depotItemId" :disabled="!!itemForm.id" show-search option-filter-prop="children" @change="selectPurchaseItem">
            <a-select-option v-for="item in purchaseOptions" :key="item.depotItemId" :value="item.depotItemId">
              {{ item.purchaseNumber }}｜{{ item.materialName }} {{ item.model || '' }}｜采购 {{ item.purchaseQuantity }}
            </a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="本次发运数量" required><a-input-number v-model="itemForm.quantity" :min="0.000001" :precision="2" style="width:100%" /></a-form-model-item>
        <a-form-model-item label="采购单价"><a-input :value="selectedPurchase ? '¥' + formatMoney(selectedPurchase.purchaseUnitPrice) : '-'" disabled /></a-form-model-item>
      </a-form-model>
    </a-modal>

    <a-modal title="关联销售订单" :visible="salesModalVisible" :confirmLoading="salesSaving" @ok="saveSalesLink" @cancel="salesModalVisible=false">
      <a-form-model :label-col="{span:6}" :wrapper-col="{span:16}">
        <a-form-model-item label="发运 SKU"><a-input :value="salesForm.materialName" disabled /></a-form-model-item>
        <a-form-model-item label="销售单明细" required>
          <a-select v-model="salesForm.salesDepotItemId" show-search option-filter-prop="children" @change="selectSalesItem">
            <a-select-option v-for="item in salesOptions" :key="item.salesDepotItemId" :value="item.salesDepotItemId">
              {{ item.salesNumber }}｜{{ item.salesCustomerName || '客户' }}｜{{ item.materialName }} {{ item.model || '' }}｜数量 {{ item.salesQuantity }}
            </a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="锁定数量" required><a-input-number v-model="salesForm.soldQuantity" :min="0.000001" :precision="2" style="width:100%" /></a-form-model-item>
        <a-form-model-item label="销售单价"><a-input :value="selectedSales ? '¥' + formatMoney(selectedSales.salesUnitPrice) : '-'" disabled /></a-form-model-item>
      </a-form-model>
    </a-modal>
  </a-card>
</template>

<script>
import { getAction, postAction, putAction } from '@/api/manage'

const emptyForm = () => ({ shipmentNo: '', containerNo: '', billOfLadingNo: '', originPort: '深圳盐田港', destinationPort: 'Manzanillo', carrierName: '', incoterms: 'CIF', currency: 'USD', exchangeRate: 7.2, estimatedArrivalDate: '', remark: '' })
const emptyItemForm = () => ({ id: undefined, shipmentId: undefined, depotItemId: undefined, quantity: undefined })
const emptySalesForm = () => ({ id: undefined, salesDepotItemId: undefined, soldQuantity: undefined, materialName: '' })

export default {
  name: 'TradeShipmentList',
  data () {
    return {
      loading: false,
      saving: false,
      drawerVisible: false,
      modalVisible: false,
      itemModalVisible: false,
      itemSaving: false,
      salesModalVisible: false,
      salesSaving: false,
      dataSource: [],
      detail: {},
      form: emptyForm(),
      itemForm: emptyItemForm(),
      salesForm: emptySalesForm(),
      purchaseOptions: [],
      salesOptions: [],
      selectedPurchase: null,
      selectedSales: null,
      columns: [
        { title: '批次号', dataIndex: 'shipmentNo' },
        { title: '柜号', dataIndex: 'containerNo' },
        { title: '起运港', dataIndex: 'originPort' },
        { title: '目的港', dataIndex: 'destinationPort' },
        { title: '状态', dataIndex: 'status', scopedSlots: { customRender: 'status' } },
        { title: '采购货值', dataIndex: 'purchaseAmount', scopedSlots: { customRender: 'amount' } },
        { title: '操作', width: 270, scopedSlots: { customRender: 'action' } }
      ],
      itemColumns: [
        { title: '采购单', dataIndex: 'purchaseNumber', width: 140 },
        { title: 'SKU', dataIndex: 'materialName', width: 180 },
        { title: '型号', dataIndex: 'model', width: 120 },
        { title: '数量', dataIndex: 'quantity', width: 80 },
        { title: '已入库', dataIndex: 'stockedQuantity', width: 80 },
        { title: '已锁定', dataIndex: 'soldQuantity', width: 80 },
        { title: '采购金额', dataIndex: 'purchaseAmount', width: 120, scopedSlots: { customRender: 'money' } },
        { title: '销售单', dataIndex: 'salesNumber', width: 140 },
        { title: '客户', dataIndex: 'salesCustomerName', width: 160 },
        { title: '销售金额', dataIndex: 'salesAmount', width: 120, scopedSlots: { customRender: 'money' } },
        { title: '操作', width: 230, fixed: 'right', scopedSlots: { customRender: 'itemAction' } }
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
        const res = await getAction('/trade/shipment/list')
        this.dataSource = res.data.data || []
      } finally {
        this.loading = false
      }
    },
    async showDetail (record) {
      const res = await getAction('/trade/shipment/detail', { id: record.id })
      this.detail = res.data.data || {}
      this.drawerVisible = true
      this.loadPurchaseOptions()
    },
    async loadPurchaseOptions () {
      const res = await getAction('/trade/shipment/purchase-options')
      this.purchaseOptions = res.data.data || []
    },
    async loadSalesOptions (materialId) {
      const res = await getAction('/trade/shipment/sales-options', { materialId })
      this.salesOptions = res.data.data || []
    },
    openModal (record) {
      this.form = record ? Object.assign(emptyForm(), record) : emptyForm()
      this.modalVisible = true
    },
    async save () {
      if (!this.form.shipmentNo || !this.form.originPort || !this.form.destinationPort) return this.$message.warning('请填写批次号、起运港和目的港')
      this.saving = true
      try {
        const res = this.form.id ? await putAction('/trade/shipment/update', this.form) : await postAction('/trade/shipment/add', this.form)
        if (res.code === 200) {
          this.$message.success('保存成功')
          this.modalVisible = false
          this.loadData()
        } else this.$message.error(res.data || '保存失败')
      } finally {
        this.saving = false
      }
    },
    openItemModal (record) {
      this.itemForm = record ? { id: record.id, shipmentId: this.detail.info.id, depotItemId: record.depotItemId, quantity: Number(record.quantity) } : { id: undefined, shipmentId: this.detail.info.id, depotItemId: undefined, quantity: undefined }
      this.selectedPurchase = this.purchaseOptions.find(item => item.depotItemId === this.itemForm.depotItemId) || null
      this.itemModalVisible = true
    },
    selectPurchaseItem (id) {
      this.selectedPurchase = this.purchaseOptions.find(item => item.depotItemId === id) || null
    },
    async saveItem () {
      if (!this.itemForm.depotItemId || !this.itemForm.quantity) return this.$message.warning('请选择采购 SKU 并填写数量')
      this.itemSaving = true
      try {
        const res = this.itemForm.id ? await putAction('/trade/shipment/item/update', this.itemForm) : await postAction('/trade/shipment/item/add', this.itemForm)
        if (res.code === 200) {
          this.$message.success('发运 SKU 已保存')
          this.itemModalVisible = false
          await this.showDetail(this.detail.info)
          this.loadData()
        } else this.$message.error(res.data || '保存失败')
      } finally {
        this.itemSaving = false
      }
    },
    async removeItem (record) {
      const res = await postAction('/trade/shipment/item/delete', { id: record.id })
      if (res.code === 200) {
        this.$message.success('SKU 明细已移除')
        await this.showDetail(this.detail.info)
        this.loadData()
      } else this.$message.error(res.data || '移除失败')
    },
    async openSalesModal (record) {
      this.salesForm = {
        id: record.id,
        salesDepotItemId: record.salesDepotItemId,
        soldQuantity: Number(record.soldQuantity || 0) || undefined,
        materialName: `${record.materialName || ''} ${record.model || ''}`
      }
      this.selectedSales = null
      await this.loadSalesOptions(record.materialId)
      if (record.salesDepotItemId) this.selectedSales = this.salesOptions.find(item => item.salesDepotItemId === record.salesDepotItemId) || null
      this.salesModalVisible = true
    },
    selectSalesItem (id) {
      this.selectedSales = this.salesOptions.find(item => item.salesDepotItemId === id) || null
      if (this.selectedSales && !this.salesForm.soldQuantity) this.salesForm.soldQuantity = Number(this.selectedSales.salesQuantity)
    },
    async saveSalesLink () {
      if (!this.salesForm.salesDepotItemId || !this.salesForm.soldQuantity) return this.$message.warning('请选择销售单明细并填写锁定数量')
      this.salesSaving = true
      try {
        const res = await postAction('/trade/shipment/item/sales-link', this.salesForm)
        if (res.code === 200) {
          this.$message.success('销售订单已关联')
          this.salesModalVisible = false
          await this.showDetail(this.detail.info)
          this.loadData()
        } else this.$message.error(res.data || '关联失败')
      } finally {
        this.salesSaving = false
      }
    },
    async unlinkSales (record) {
      const res = await postAction('/trade/shipment/item/sales-unlink', { id: record.id })
      if (res.code === 200) {
        this.$message.success('已取消销售关联')
        await this.showDetail(this.detail.info)
        this.loadData()
      } else this.$message.error(res.data || '取消失败')
    },
    nextStatus (status) {
      const flow = ['待订舱', '已订舱', '已装柜', '已开船', '在途', '已到港', '清关中', '已入库', '已完成']
      const index = flow.indexOf(status)
      return index >= 0 && index < flow.length - 1 ? flow[index + 1] : ''
    },
    advanceStatus (record) {
      const target = this.nextStatus(record.status)
      this.$confirm({
        title: '确认推进状态？',
        content: `${record.shipmentNo} 将从“${record.status}”推进至“${target}”。`,
        onOk: async () => {
          const res = await postAction('/trade/shipment/status', { id: record.id, status: target })
          if (res.code === 200) {
            this.$message.success('状态已推进')
            this.loadData()
          } else this.$message.error(res.data || '推进失败')
        }
      })
    },
    formatMoney (value) {
      return Number(value || 0).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
    },
    statusColor (status) {
      return { '已入库': 'green', '已完成': 'green', '清关中': 'orange', '已到港': 'cyan', '在途': 'blue', '异常': 'red' }[status] || 'default'
    }
  }
}
</script>
