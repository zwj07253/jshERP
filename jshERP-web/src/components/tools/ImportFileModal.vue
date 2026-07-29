<template>
  <div ref="container">
    <a-modal
      :title="title"
      :width="500"
      :visible="visible"
      :confirm-loading="confirmLoading"
      :getContainer="() => $refs.container"
      :maskStyle="{'top':'93px','left':'154px'}"
      :wrapClassName="wrapClassNameInfo()"
      :mask="isDesktop()"
      :maskClosable="false"
      @cancel="handleCancel"
      style="top:20%;height: 55%;">
      <template slot="footer">
        <a-button key="back" @click="handleCancel">取消</a-button>
      </template>
      <a-spin :spinning="confirmLoading" tip="AI 正在识别并校验文件，请勿关闭窗口…">
        <a-row class="form-row" :gutter="24">
          <a-col :md="24" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" label="第一步：">
              <a target="_blank" :href="templateUrl"><b>{{templateName}}</b></a>
              <p>提示：模板中的第一行请勿删除</p>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row class="form-row" :gutter="24">
          <a-col :md="24" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" label="第二步：">
              <a-upload name="file" :showUploadList="false" :multiple="false" :headers="tokenHeader" :action="importExcelUrl" @change="handleImportExcel">
                <a-button type="primary" icon="import">导入</a-button>
              </a-upload>
              <a-upload name="file" :showUploadList="false" :multiple="false" :headers="tokenHeader" :data="aiFileData" :action="aiImportUrl" @change="handleAiImport" style="margin-left:10px">
                <a-button type="dashed" icon="robot">AI 智能识别</a-button>
              </a-upload>
              <p style="margin:8px 0 0;color:#999">支持 Excel、CSV、TXT、PDF 和图片；识别结果会先校验，再由你确认导入。</p>
            </a-form-item>
          </a-col>
        </a-row>
      </a-spin>
    </a-modal>
  </div>
</template>

<script>
  import { JeecgListMixin } from '@/mixins/JeecgListMixin'
  import { postAction } from '@/api/manage'
  import {mixinDevice} from '@/utils/mixin'
  export default {
    name: 'ImportFileModal',
    mixins:[JeecgListMixin, mixinDevice],
    data () {
      return {
        title:"",
        visible: false,
        model: {},
        labelCol: {
          xs: { span: 24 },
          sm: { span: 5 },
        },
        wrapperCol: {
          xs: { span: 24 },
          sm: { span: 18 },
        },
        confirmLoading: false,
        disableMixinCreated: true,
        templateUrl: '',
        templateName: '',
        url: {
          importExcelUrl: '',
        }
      }
    },
    created () {
    },
    computed: {
      importExcelUrl: function () {
        return `${window._CONFIG['domianURL']}${this.url.importExcelUrl}`;
      },
      aiImportUrl: function () {
        return `${window._CONFIG['domianURL']}/ai/import/parse`;
      },
      aiType: function () {
        if (this.url.importExcelUrl === '/material/importExcel') return 'MATERIAL'
        if (this.url.importExcelUrl === '/supplier/importVendor') return 'VENDOR'
        if (this.url.importExcelUrl === '/supplier/importCustomer') return 'CUSTOMER'
        if (this.url.importExcelUrl === '/supplier/importMember') return 'MEMBER'
        return ''
      }
    },
    methods: {
      initModal(apiUrl, templateUrl, templateName) {
        this.url.importExcelUrl = apiUrl
        this.templateUrl = templateUrl
        this.templateName = templateName
        this.visible = true
      },
      close () {
        this.$emit('close');
        this.visible = false;
      },
      handleCancel () {
        this.close()
      },
      aiFileData () {
        return { type: this.aiType }
      },
      handleAiImport (info) {
        if (info.file.status === 'uploading') {
          this.confirmLoading = true
        } else if (info.file.status === 'done') {
          this.confirmLoading = false
          const response = info.file.response
          if (!response || response.code !== 200) {
            this.$message.error(response && response.data ? response.data.message : 'AI 识别失败')
            return
          }
          const rows = response.data && response.data.rows ? response.data.rows : []
          const invalid = rows.filter(row => row.valid === false)
          if (invalid.length) {
            this.$message.error('有 ' + invalid.length + ' 条数据缺少必填字段，请补全文件后重试')
            return
          }
          this.$confirm({
            title: '确认 AI 导入',
            content: '已识别并初步校验 ' + rows.length + ' 条数据，确认后将写入系统。',
            onOk: () => postAction('/ai/import/confirm', { type: this.aiType, prefixNo: '', taskId: response.data.taskId, rows: rows }).then(res => {
              if (res && res.code === 200) { this.$message.success('导入成功 ' + res.data.count + ' 条'); this.close(); this.$emit('ok') }
              else this.$message.error(res && res.data ? res.data.message : '导入失败')
            })
          })
        } else if (info.file.status === 'error') {
          this.confirmLoading = false
          this.$message.error('AI 文件识别失败')
        }
      }
    }
  }
</script>

<style scoped>

</style>
