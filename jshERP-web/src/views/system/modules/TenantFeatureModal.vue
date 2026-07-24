<template>
  <a-modal
    :title="'功能授权 - ' + tenantName"
    :width="600"
    :visible="visible"
    :confirmLoading="confirmLoading"
    @ok="handleOk"
    @cancel="handleCancel"
    okText="保存"
    cancelText="取消">
    <a-spin :spinning="loading">
      <a-form-model>
        <a-form-model-item
          v-for="item in featureList"
          :key="item.featureId"
          :label="item.featureName"
          :labelCol="{span: 8}"
          :wrapperCol="{span: 16}"
          style="margin-bottom: 12px;">
          <a-switch
            :checked="item.enabled"
            @change="(checked) => onFeatureChange(item.featureId, checked)"
            checkedChildren="已开通"
            unCheckedChildren="未开通" />
          <span v-if="item.description" style="margin-left: 8px; color: #999; font-size: 12px;">{{ item.description }}</span>
        </a-form-model-item>
        <a-empty v-if="!loading && featureList.length === 0" description="暂无功能模块" />
      </a-form-model>
    </a-spin>
  </a-modal>
</template>

<script>
  import { getTenantFeatures, batchUpdateTenantFeatures } from '@/api/api'

  export default {
    name: 'TenantFeatureModal',
    data() {
      return {
        visible: false,
        confirmLoading: false,
        loading: false,
        tenantId: null,
        tenantName: '',
        featureList: []
      }
    },
    methods: {
      show(tenantId, tenantName) {
        this.tenantId = tenantId
        this.tenantName = tenantName || ''
        this.visible = true
        this.loadFeatures()
      },
      loadFeatures() {
        this.loading = true
        getTenantFeatures({ tenantId: this.tenantId }).then(res => {
          if (res.code === 200) {
            this.featureList = res.data || []
          }
          this.loading = false
        }).catch(() => {
          this.loading = false
        })
      },
      onFeatureChange(featureId, checked) {
        const item = this.featureList.find(f => f.featureId === featureId)
        if (item) {
          item.enabled = checked
        }
      },
      handleOk() {
        this.confirmLoading = true
        const enabledFeatureIds = this.featureList
          .filter(f => f.enabled)
          .map(f => f.featureId)
        batchUpdateTenantFeatures({
          tenantId: this.tenantId,
          featureIds: enabledFeatureIds
        }).then(res => {
          if (res.code === 200) {
            this.$message.success('功能授权更新成功')
            this.visible = false
            this.$emit('ok')
          } else {
            this.$message.error(res.data || '更新失败')
          }
          this.confirmLoading = false
        }).catch(() => {
          this.$message.error('更新失败')
          this.confirmLoading = false
        })
      },
      handleCancel() {
        this.visible = false
      }
    }
  }
</script>
