<template>
  <a-modal
    :title="title"
    :width="500"
    :visible="visible"
    :confirmLoading="confirmLoading"
    :maskStyle="{'top':'93px','left':'154px'}"
    @cancel="handleCancel"
    cancelText="关闭"
    wrapClassName="ant-modal-cust-warp"
    style="top:20%;height: 55%;overflow-y: hidden">
    <template slot="footer">
      <a-button key="back" @click="handleCancel">
        关闭
      </a-button>
    </template>
    <a-spin :spinning="confirmLoading" tip="AI 正在识别并校验文件，请勿关闭窗口…">
      <a-form :form="form">
        <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" label="模板">
          <span><a :href="tmpUrl" target="_blank"><b>明细Excel模板[下载]</b></a></span>
        </a-form-item>
        <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" label="文件">
          <a-upload name="file" :showUploadList="false" :multiple="false" :headers="tokenHeader"
                    :data="setFileData" :action="importExcelUrl" @change="handleImportExcel">
            <a-button type="primary" icon="import">导入</a-button>
          </a-upload>
        </a-form-item>
        <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" label="AI 智能识别">
          <a-upload name="file" :showUploadList="false" :multiple="false" :headers="tokenHeader"
                    :data="setAiFileData" :action="aiImportUrl" @change="handleAiImport">
            <a-button type="dashed" icon="robot">上传文件并识别</a-button>
          </a-upload>
          <div style="color:#999;margin-top:6px">支持 Excel、CSV、TXT、PDF 和图片，识别后会按条码校验并回填单据。</div>
        </a-form-item>
      </a-form>
    </a-spin>
    <a-modal v-model="aiPreviewVisible" title="AI 识别结果（可修改后回填）" :width="1050" :maskClosable="false" @ok="confirmAiRows">
      <a-alert :type="invalidAiRows ? 'warning' : 'success'" :message="invalidAiRows ? '存在错误行：请补充条码或修正数据后再确认。' : '全部行已通过初步校验。'" show-icon style="margin-bottom:12px" />
      <a-table :dataSource="aiRows" :pagination="false" rowKey="_rowId" size="small" :rowClassName="row => row.valid === false ? 'ai-invalid-row' : ''">
        <a-table-column title="状态" :width="100"><template slot-scope="text,row"><a-tag :color="row.valid === false ? 'red' : 'green'">{{ row.valid === false ? '需修正' : '可回填' }}</a-tag></template></a-table-column>
        <a-table-column title="条码" :width="150"><template slot-scope="text,row"><a-input v-model="row.barCode" /></template></a-table-column>
        <a-table-column title="名称" dataIndex="name" :width="150" />
        <a-table-column title="数量" :width="110"><template slot-scope="text,row"><a-input v-model="row.operNumber" /></template></a-table-column>
        <a-table-column title="单价" :width="110"><template slot-scope="text,row"><a-input v-model="row.unitPrice" /></template></a-table-column>
        <a-table-column title="错误说明"><template slot-scope="text,row"><span style="color:#f5222d">{{ (row.errors || []).join('；') }}</span></template></a-table-column>
      </a-table>
    </a-modal>
  </a-modal>
</template>

<script>
  import { ACCESS_TOKEN } from '@/store/mutation-types'
  import Vue from 'vue'
  import { postAction } from '@/api/manage'

  export default {
    name: "ImportItemModal",
    components: {
    },
    data () {
      return {
        title:"导入明细",
        visible: false,
        prefixNo: '',
        tmpUrl: '',
        model: {},
        tokenHeader: {'X-Access-Token': Vue.ls.get(ACCESS_TOKEN)},
        labelCol: {
          xs: { span: 24 },
          sm: { span: 5 },
        },
        wrapperCol: {
          xs: { span: 24 },
          sm: { span: 16 },
        },
        confirmLoading: false,
        aiPreviewVisible: false,
        aiRows: [],
        aiTaskId: '',
        form: this.$form.createForm(this),
        url: {
          importExcelUrl: "/depotItem/importItemExcel",
          aiImportUrl: "/ai/import/parse",
        }
      }
    },
    created () {
    },
    computed: {
      invalidAiRows: function () {
        return this.aiRows.filter(row => row.valid === false).length
      },
      importExcelUrl: function () {
        return `${window._CONFIG['domianURL']}${this.url.importExcelUrl}`;
      },
      aiImportUrl: function () {
        return `${window._CONFIG['domianURL']}${this.url.aiImportUrl}`;
      }
    },
    methods: {
      add (prefixNo) {
        this.prefixNo = prefixNo
        if(prefixNo === 'QGD') {
          this.tmpUrl = '/doc/apply_item_template.xls'
        } else if(prefixNo === 'CGDD' || prefixNo === 'XSDD') {
          this.tmpUrl = '/doc/order_item_template.xls'
        } else if(prefixNo === 'CGRK' || prefixNo === 'XSCK') {
          this.tmpUrl = '/doc/buy_sale_item_template.xls'
        } else if(prefixNo === 'QTRK' || prefixNo === 'QTCK') {
          this.tmpUrl = '/doc/in_out_item_template.xls'
        }
        this.form.resetFields()
        this.model = Object.assign({}, {})
        this.visible = true
      },
      close () {
        this.$emit('close');
        this.visible = false;
      },
      handleCancel () {
        this.close()
      },
      //导入
      handleImportExcel(info){
        if (info.file.status !== 'uploading') {
          console.log(info.file, info.fileList);
        }
        if (info.file.status === 'done') {
          if (info.file.response) {
            if (info.file.response.code === 200) {
              this.$message.success('导入成功' + info.file.response.data.rows.length + '条')
              this.$emit('ok', info.file.response.data.rows);
              this.close()
            } else if (info.file.response.code === 500) {
              this.$message.warn(info.file.response.data.message)
            }
          } else {
            this.$message.error(`${info.file.name} ${info.file.response.data}.`);
          }
        } else if (info.file.status === 'error') {
          this.$message.error(`文件导入失败: ${info.file.msg} `);
        }
      },
      setFileData() {
        return {
          'prefixNo': this.prefixNo
        }
      },
      setAiFileData() {
        return { 'type': 'BILL_ITEM', 'prefixNo': this.prefixNo }
      },
      handleAiImport(info) {
        if (info.file.status === 'uploading') {
          this.confirmLoading = true
        } else if (info.file.status === 'done') {
          this.confirmLoading = false
          const response = info.file.response
          if (response && response.code === 200) {
            const rows = response.data && response.data.rows ? response.data.rows : []
            this.aiRows = rows.map((row, index) => Object.assign({ _rowId: index + 1, errors: [], valid: true }, row))
            this.aiTaskId = response.data.taskId
            this.aiPreviewVisible = true
          } else {
            this.$message.error(response && response.data && response.data.message ? response.data.message : 'AI 识别失败，请检查文件内容后重试')
          }
        } else if (info.file.status === 'error') {
          this.confirmLoading = false
          this.$message.error('AI 文件识别失败')
        }
      },
      confirmAiRows () {
        this.confirmLoading = true
        postAction('/ai/import/confirm', { type: 'BILL_ITEM', prefixNo: this.prefixNo, taskId: this.aiTaskId, rows: this.aiRows }).then(res => {
          if (res && res.code === 200) {
            this.$message.success('已回填 ' + res.data.count + ' 条明细')
            this.aiPreviewVisible = false
            this.$emit('ok', res.data.rows)
            this.close()
          } else this.$message.error(res && res.data && res.data.message ? res.data.message : 'AI 回填失败')
        }).finally(() => { this.confirmLoading = false })
      }
    }
  }
</script>
<style scoped>

</style>
