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
        <a-button key="back" @click="handleCancel">{{ $t('common.cancel') }}</a-button>
      </template>
      <a-spin :spinning="confirmLoading" :tip="$t('common.aiRecognizing')">
        <a-row class="form-row" :gutter="24">
          <a-col :md="24" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.step1')">
              <a target="_blank" :href="templateUrl"><b>{{templateName}}</b></a>
              <p>{{ $t('common.templateTip') }}</p>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row class="form-row" :gutter="24">
          <a-col :md="24" :sm="24">
            <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.step2')">
              <a-upload name="file" :showUploadList="false" :multiple="false" :headers="tokenHeader" :action="importExcelUrl" @change="handleImportExcel">
                <a-button type="primary" icon="import">{{ $t('common.import') }}</a-button>
              </a-upload>
              <a-upload name="file" :showUploadList="false" :multiple="false" :headers="tokenHeader" :data="aiFileData" :action="aiImportUrl" @change="handleAiImport" style="margin-left:10px">
                <a-button type="dashed" icon="robot">{{ $t('common.aiRecognition') }}</a-button>
              </a-upload>
              <p style="margin:8px 0 0;color:#999">{{ $t('common.aiImportHint') }}</p>
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
            this.$message.error(response && response.data ? response.data.message : this.$t('common.aiRecognitionFailed'))
            return
          }
          const rows = response.data && response.data.rows ? response.data.rows : []
          const invalid = rows.filter(row => row.valid === false)
          if (invalid.length) {
            this.$message.error(this.$t('common.aiMissingFields', { count: invalid.length }))
            return
          }
          this.$confirm({
            title: this.$t('common.confirmAiImport'),
            content: this.$t('common.aiIdentifiedAndValidated', { count: rows.length }),
            onOk: () => postAction('/ai/import/confirm', { type: this.aiType, prefixNo: '', taskId: response.data.taskId, rows: rows }).then(res => {
              if (res && res.code === 200) { this.$message.success(this.$t('common.importSuccess', { count: res.data.count })); this.close(); this.$emit('ok') }
              else this.$message.error(res && res.data ? res.data.message : this.$t('common.importFailed'))
            })
          })
        } else if (info.file.status === 'error') {
          this.confirmLoading = false
          this.$message.error(this.$t('common.aiFileRecognitionFailed'))
        }
      }
    }
  }
</script>

<style scoped>

</style>
