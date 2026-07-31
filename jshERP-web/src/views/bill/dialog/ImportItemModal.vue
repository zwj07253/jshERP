<template>
  <a-modal
    :title="title"
    :width="500"
    :visible="visible"
    :confirmLoading="confirmLoading"
    :maskStyle="{'top':'93px','left':'154px'}"
    @cancel="handleCancel"
    :cancelText="$t('common.close')"
    wrapClassName="ant-modal-cust-warp"
    style="top:20%;height: 55%;overflow-y: hidden">
    <template slot="footer">
      <a-button key="back" @click="handleCancel">
        {{ $t('common.close') }}
      </a-button>
    </template>
    <a-spin :spinning="confirmLoading" :tip="$t('common.aiRecognizing')">
      <a-form :form="form">
        <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.template')">
          <span><a :href="tmpUrl" target="_blank"><b>{{ $t('common.detailExcelTemplate') }}</b></a></span>
        </a-form-item>
        <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.file')">
          <a-upload name="file" :showUploadList="false" :multiple="false" :headers="tokenHeader"
                    :data="setFileData" :action="importExcelUrl" @change="handleImportExcel">
            <a-button type="primary" icon="import">{{ $t('common.importBtn') }}</a-button>
          </a-upload>
        </a-form-item>
        <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.aiRecognition')">
          <a-upload name="file" :showUploadList="false" :multiple="false" :headers="tokenHeader"
                    :data="setAiFileData" :action="aiImportUrl" @change="handleAiImport">
            <a-button type="dashed" icon="robot">{{ $t('common.uploadAndRecognize') }}</a-button>
          </a-upload>
          <div style="color:#999;margin-top:6px">{{ $t('common.aiRecognitionHint') }}</div>
        </a-form-item>
      </a-form>
    </a-spin>
    <a-modal v-model="aiPreviewVisible" :title="$t('common.aiResult')" :width="1050" :maskClosable="false" @ok="confirmAiRows">
      <a-alert :type="invalidAiRows ? 'warning' : 'success'" :message="invalidAiRows ? $t('common.errorRowsExist') : $t('common.allRowsValid')" show-icon style="margin-bottom:12px" />
      <a-table :dataSource="aiRows" :pagination="false" rowKey="_rowId" size="small" :rowClassName="row => row.valid === false ? 'ai-invalid-row' : ''">
        <a-table-column :title="$t('common.status')" :width="100"><template slot-scope="text,row"><a-tag :color="row.valid === false ? 'red' : 'green'">{{ row.valid === false ? $t('common.needFix') : $t('common.canFillBack') }}</a-tag></template></a-table-column>
        <a-table-column :title="$t('common.barcode')" :width="150"><template slot-scope="text,row"><a-input v-model="row.barCode" /></template></a-table-column>
        <a-table-column :title="$t('common.name')" dataIndex="name" :width="150" />
        <a-table-column :title="$t('common.quantity')" :width="110"><template slot-scope="text,row"><a-input v-model="row.operNumber" /></template></a-table-column>
        <a-table-column :title="$t('common.amount')" :width="110"><template slot-scope="text,row"><a-input v-model="row.unitPrice" /></template></a-table-column>
        <a-table-column :title="$t('common.errorDesc')"><template slot-scope="text,row"><span style="color:#f5222d">{{ (row.errors || []).join('；') }}</span></template></a-table-column>
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
        title: this.$t('common.importDetail'),
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
              this.$message.success(this.$t('common.importSuccess', { count: info.file.response.data.rows.length }))
              this.$emit('ok', info.file.response.data.rows);
              this.close()
            } else if (info.file.response.code === 500) {
              this.$message.warn(info.file.response.data.message)
            }
          } else {
            this.$message.error(`${info.file.name} ${info.file.response.data}.`);
          }
        } else if (info.file.status === 'error') {
          this.$message.error(this.$t('common.fileImportFailed') + ': ' + info.file.msg);
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
            this.$message.error(response && response.data && response.data.message ? response.data.message : this.$t('common.aiRecognitionFailed'))
          }
        } else if (info.file.status === 'error') {
          this.confirmLoading = false
          this.$message.error(this.$t('common.aiFileRecognitionFailed'))
        }
      },
      confirmAiRows () {
        this.confirmLoading = true
        postAction('/ai/import/confirm', { type: 'BILL_ITEM', prefixNo: this.prefixNo, taskId: this.aiTaskId, rows: this.aiRows }).then(res => {
          if (res && res.code === 200) {
            this.$message.success(this.$t('common.filledBack', { count: res.data.count }))
            this.aiPreviewVisible = false
            this.$emit('ok', res.data.rows)
            this.close()
          } else this.$message.error(res && res.data && res.data.message ? res.data.message : this.$t('common.aiFillBackFailed'))
        }).finally(() => { this.confirmLoading = false })
      }
    }
  }
</script>
<style scoped>

</style>
