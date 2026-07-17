<template>
  <a-card title="外贸库存状态" :bordered="false">
    <a-alert
      message="可销售库存、客户锁定库存、海运在途和清关中库存分开统计；明细按发运批次和 SKU 展示，便于演示每批货当前在哪里。"
      type="info"
      show-icon
      style="margin-bottom:16px"
    />

    <div class="table-operator" style="margin-bottom:16px">
      <a-button type="primary" icon="import" @click="importVisible = true">导入库存</a-button>
      <a-button icon="robot" @click="openAiImport">AI 导入</a-button>
      <span class="import-tip">按发运批次号、采购单号和商品条码匹配库存 SKU</span>
    </div>

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

    <a-modal
      title="导入外贸库存"
      :visible="importVisible"
      :confirmLoading="importLoading"
      :footer="null"
      width="560px"
      @cancel="importVisible = false"
    >
      <a-alert
        message="请使用 .xls 格式模板，前两行是说明和表头，数据从第三行开始。"
        description="必填：发运批次号、采购单号、商品条码、发运数量；库存状态和销售金额可按实际情况填写。"
        type="info"
        show-icon
        style="margin-bottom:20px"
      />
      <a-form layout="inline">
        <a-form-item label="模板">
          <a-button type="link" icon="download" @click="downloadTemplate">下载外贸库存模板</a-button>
        </a-form-item>
        <a-form-item label="文件">
          <a-upload
            name="file"
            accept=".xls"
            :showUploadList="false"
            :multiple="false"
            :headers="tokenHeader"
            :action="importExcelUrl"
            @change="handleImportExcel"
          >
            <a-button type="primary" icon="upload">选择 Excel 导入</a-button>
          </a-upload>
        </a-form-item>
      </a-form>
      <div class="import-columns">
        导入字段：发运批次号、采购单号、商品条码、发运数量、在途数量、清关中数量、已入库数量、客户锁定数量、销售金额。
      </div>
    </a-modal>

    <a-modal
      title="AI 解析导入外贸库存"
      :visible="aiVisible"
      :confirmLoading="aiConfirmLoading"
      :okButtonProps="{ props: { disabled: !aiRows.length || aiHasError } }"
      okText="确认导入"
      cancelText="取消"
      width="1240px"
      @ok="confirmAiImport"
      @cancel="closeAiImport"
    >
      <a-alert
        message="AI 只负责识别，系统会再次匹配发运批次、采购单和商品条码；确认前请核对黄色提示，红色数据必须修正。"
        type="warning"
        show-icon
        style="margin-bottom:16px"
      />
      <a-upload-dragger
        v-if="!aiRows.length"
        :file-list="aiFileList"
        :before-upload="beforeAiUpload"
        :remove="removeAiFile"
        accept=".xls,.xlsx,.csv,.txt,.pdf,.png,.jpg,.jpeg"
        :multiple="false"
      >
        <p class="ant-upload-drag-icon"><a-icon type="inbox" /></p>
        <p class="ant-upload-text">点击或拖拽库存文件到这里</p>
        <p class="ant-upload-hint">支持 Excel、CSV、文本、PDF 和图片，文件内容将发送给已配置的 AI 模型解析。</p>
      </a-upload-dragger>
      <div v-if="!aiRows.length" style="text-align:center;margin-top:16px">
        <a-button type="primary" icon="robot" :loading="aiParsing" :disabled="!aiFile" @click="parseAiFile">
          开始 AI 解析
        </a-button>
      </div>

      <template v-else>
        <div class="ai-result-toolbar">
          <span>模型：{{ aiModel || '-' }}，共识别 {{ aiRows.length }} 条</span>
          <a-button size="small" @click="resetAiFile">重新上传</a-button>
        </div>
        <a-table
          :columns="aiColumns"
          :data-source="aiRows"
          row-key="rowNo"
          size="small"
          :pagination="false"
          :scroll="{ x: 1680, y: 420 }"
        >
          <template slot="shipmentNo" slot-scope="text, record"><a-input v-model.trim="record.shipmentNo" /></template>
          <template slot="purchaseNumber" slot-scope="text, record"><a-input v-model.trim="record.purchaseNumber" /></template>
          <template slot="barCode" slot-scope="text, record"><a-input v-model.trim="record.barCode" /></template>
          <template slot="quantity" slot-scope="text, record"><a-input-number v-model="record.quantity" :min="0" /></template>
          <template slot="inTransitQuantity" slot-scope="text, record"><a-input-number v-model="record.inTransitQuantity" :min="0" /></template>
          <template slot="clearedQuantity" slot-scope="text, record"><a-input-number v-model="record.clearedQuantity" :min="0" /></template>
          <template slot="stockedQuantity" slot-scope="text, record"><a-input-number v-model="record.stockedQuantity" :min="0" /></template>
          <template slot="lockedQuantity" slot-scope="text, record"><a-input-number v-model="record.lockedQuantity" :min="0" /></template>
          <template slot="salesAmount" slot-scope="text, record"><a-input-number v-model="record.salesAmount" :min="0" /></template>
          <template slot="confidence" slot-scope="text">{{ confidencePercent(text) }}</template>
          <template slot="validation" slot-scope="text, record">
            <a-tag :color="validationColor(record.validationStatus)">{{ validationLabel(record.validationStatus) }}</a-tag>
            <div class="validation-message">{{ record.validationMessage }}</div>
          </template>
        </a-table>
      </template>
    </a-modal>
  </a-card>
</template>

<script>
import { getAction, postAction, downFile, uploadAction } from '@/api/manage'
import { ACCESS_TOKEN } from '@/store/mutation-types'
import Vue from 'vue'

export default {
  name: 'TradeInventory',
  data () {
    return {
      loading: false,
      importVisible: false,
      importLoading: false,
      aiVisible: false,
      aiParsing: false,
      aiConfirmLoading: false,
      aiFile: null,
      aiFileList: [],
      aiRows: [],
      aiModel: '',
      tokenHeader: { 'X-Access-Token': Vue.ls.get(ACCESS_TOKEN) },
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
      ],
      aiColumns: [
        { title: '行', dataIndex: 'rowNo', width: 55, fixed: 'left' },
        { title: '发运批次号', dataIndex: 'shipmentNo', width: 170, scopedSlots: { customRender: 'shipmentNo' } },
        { title: '采购单号', dataIndex: 'purchaseNumber', width: 170, scopedSlots: { customRender: 'purchaseNumber' } },
        { title: '商品条码', dataIndex: 'barCode', width: 150, scopedSlots: { customRender: 'barCode' } },
        { title: '发运数量', dataIndex: 'quantity', width: 115, scopedSlots: { customRender: 'quantity' } },
        { title: '在途', dataIndex: 'inTransitQuantity', width: 105, scopedSlots: { customRender: 'inTransitQuantity' } },
        { title: '清关', dataIndex: 'clearedQuantity', width: 105, scopedSlots: { customRender: 'clearedQuantity' } },
        { title: '已入库', dataIndex: 'stockedQuantity', width: 105, scopedSlots: { customRender: 'stockedQuantity' } },
        { title: '客户锁定', dataIndex: 'lockedQuantity', width: 115, scopedSlots: { customRender: 'lockedQuantity' } },
        { title: '销售金额', dataIndex: 'salesAmount', width: 130, scopedSlots: { customRender: 'salesAmount' } },
        { title: '置信度', dataIndex: 'confidence', width: 90, scopedSlots: { customRender: 'confidence' } },
        { title: '系统校验', dataIndex: 'validationStatus', width: 240, fixed: 'right', scopedSlots: { customRender: 'validation' } }
      ]
    }
  },
  computed: {
    importExcelUrl () {
      return `${window._CONFIG['domianURL']}/trade/inventory/importExcel`
    },
    aiHasError () {
      return this.aiRows.some(item => item.validationStatus === 'error')
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
    downloadTemplate () {
      downFile('/trade/inventory/template').then(res => {
        const blob = new Blob([res.data], { type: 'application/vnd.ms-excel' })
        const link = document.createElement('a')
        link.href = window.URL.createObjectURL(blob)
        link.download = '外贸库存导入模板.xls'
        link.click()
        window.URL.revokeObjectURL(link.href)
      })
    },
    handleImportExcel (info) {
      if (info.file.status === 'uploading') {
        this.importLoading = true
      }
      if (info.file.status === 'done') {
        this.importLoading = false
        const response = info.file.response
        if (response && response.code === 200) {
          const rows = response.data && response.data.rows ? response.data.rows.length : 0
          this.$message.success(`导入成功，共处理 ${rows} 条库存明细`)
          this.importVisible = false
          this.reload()
        } else {
          const message = response && response.data && response.data.message
            ? response.data.message
            : '文件导入失败，请检查模板和数据'
          this.$message.error(message)
        }
      } else if (info.file.status === 'error') {
        this.importLoading = false
        this.$message.error('文件导入失败，请检查网络或登录状态')
      }
    },
    openAiImport () {
      this.aiVisible = true
    },
    closeAiImport () {
      if (this.aiParsing || this.aiConfirmLoading) return
      this.aiVisible = false
    },
    beforeAiUpload (file) {
      this.aiFile = file
      this.aiFileList = [file]
      return false
    },
    removeAiFile () {
      this.aiFile = null
      this.aiFileList = []
      return true
    },
    resetAiFile () {
      this.aiRows = []
      this.aiModel = ''
      this.aiFile = null
      this.aiFileList = []
    },
    async parseAiFile () {
      if (!this.aiFile) return
      this.aiParsing = true
      try {
        const formData = new FormData()
        formData.append('file', this.aiFile)
        const res = await uploadAction('/trade/inventory/ai/parse', formData)
        const body = res || {}
        if (body.code !== 200) throw new Error(this.aiErrorMessage(body, 'AI 解析失败'))
        this.aiRows = (body.data && body.data.rows) || []
        this.aiModel = body.data && body.data.model
        if (!this.aiRows.length) throw new Error('AI 没有识别到库存明细')
        this.$message.success(`AI 已解析 ${this.aiRows.length} 条数据，请核对后确认导入`)
      } catch (e) {
        this.$message.error(e.message || 'AI 解析失败')
      } finally {
        this.aiParsing = false
      }
    },
    async confirmAiImport () {
      if (!this.aiRows.length || this.aiHasError) return
      this.aiConfirmLoading = true
      try {
        const res = await postAction('/trade/inventory/ai/confirm', { rows: this.aiRows })
        const body = res || {}
        if (body.code !== 200) throw new Error(this.aiErrorMessage(body, '确认导入失败'))
        const count = body.data && body.data.count ? body.data.count : 0
        this.$message.success(`AI 库存导入完成，共处理 ${count} 条`)
        this.aiVisible = false
        this.resetAiFile()
        this.reload()
      } catch (e) {
        this.$message.error(e.message || '确认导入失败')
      } finally {
        this.aiConfirmLoading = false
      }
    },
    aiErrorMessage (body, fallback) {
      return body && body.data && body.data.message ? body.data.message : fallback
    },
    confidencePercent (value) {
      return value === null || value === undefined ? '-' : `${Math.round(Number(value) * 100)}%`
    },
    validationColor (status) {
      return { valid: 'green', warning: 'orange', error: 'red' }[status] || 'default'
    },
    validationLabel (status) {
      return { valid: '可导入', warning: '需核对', error: '不可导入' }[status] || '未知'
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
.import-tip {
  margin-left: 12px;
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
}
.import-columns {
  margin-top: 20px;
  padding: 10px 12px;
  color: rgba(0, 0, 0, 0.55);
  background: #fafafa;
  font-size: 12px;
  line-height: 20px;
}
.ai-result-toolbar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  color: rgba(0, 0, 0, 0.65);
}
.validation-message {
  margin-top: 4px;
  color: rgba(0, 0, 0, 0.55);
  font-size: 12px;
  white-space: normal;
}
</style>
