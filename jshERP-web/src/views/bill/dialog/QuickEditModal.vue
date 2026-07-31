<template>
  <a-modal
    :title="title"
    :width="600"
    :visible="visible"
    :confirmLoading="confirmLoading"
    @ok="handleOk"
    @cancel="handleCancel"
    :okText="$t('common.save')"
    :cancelText="$t('common.cancel')">
    <a-spin :spinning="confirmLoading">
      <a-form :form="form">
        <a-form-item :label="$t('common.billNo')" :labelCol="{span: 5}" :wrapperCol="{span: 18}">
          <a-input v-decorator="['number']" :readOnly="true" />
        </a-form-item>
        <a-form-item :label="$t('common.remark')" :labelCol="{span: 5}" :wrapperCol="{span: 18}">
          <a-textarea
            v-decorator="['remark']"
            :rows="4"
            :placeholder="$t('common.enterRemark')" />
        </a-form-item>
      </a-form>
    </a-spin>
  </a-modal>
</template>

<script>
import { quickEditDepotHead } from '@/api/api'
import pick from 'lodash.pick'

export default {
  name: 'QuickEditModal',
  data() {
    return {
      title: this.$t('bill.quickEditRemark'),
      visible: false,
      confirmLoading: false,
      form: this.$form.createForm(this),
      model: {}
    }
  },
  methods: {
    show(record) {
      this.visible = true
      this.model = Object.assign({}, record)
      this.$nextTick(() => {
        this.form.setFieldsValue(pick(this.model, ['number', 'remark']))
      })
    },
    handleOk() {
      this.form.validateFields((err, values) => {
        if (!err) {
          this.confirmLoading = true
          let params = Object.assign({}, this.model, values)
          quickEditDepotHead(params).then((res) => {
            if (res.code === 200) {
              this.$message.success(this.$t('common.saveSuccess'))
              this.close()
            } else {
              this.$message.warning(res.message)
            }
          }).catch(() => {
            this.$message.error(this.$t('common.saveFailed'))
          }).finally(() => {
            this.confirmLoading = false
          })
        }
      })
    },
    close() {
      this.form.resetFields()
      this.visible = false
      this.$emit('close')
    },
    handleCancel () {
      this.close()
    },
  }
}
</script>
