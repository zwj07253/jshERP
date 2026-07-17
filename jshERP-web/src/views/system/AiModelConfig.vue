<template>
  <a-card title="AI 模型配置" :bordered="false">
    <a-alert
      message="用于外贸库存文件的 AI 解析，Token 仅在后端加密保存，页面不会读取明文。"
      description="接口需兼容 OpenAI Chat Completions；API 地址请填写完整请求地址，例如 https://api.example.com/v1/chat/completions。"
      type="info"
      show-icon
      style="margin-bottom:24px"
    />

    <a-spin :spinning="loading">
      <a-form-model ref="form" :model="form" :rules="rules" :label-col="labelCol" :wrapper-col="wrapperCol">
        <a-form-model-item label="启用 AI 导入" prop="enabled">
          <a-switch v-model="form.enabled" checked-children="启用" un-checked-children="停用" />
        </a-form-model-item>
        <a-form-model-item label="接口类型">
          <a-input v-model="form.provider" disabled />
        </a-form-model-item>
        <a-form-model-item label="API 地址" prop="apiUrl">
          <a-input v-model.trim="form.apiUrl" placeholder="https://api.example.com/v1/chat/completions" />
        </a-form-model-item>
        <a-form-model-item label="模型名称" prop="modelName">
          <a-input v-model.trim="form.modelName" placeholder="例如 gpt-4.1-mini、qwen-vl-plus" />
        </a-form-model-item>
        <a-form-model-item label="API Token" prop="apiToken">
          <a-input-password
            v-model.trim="form.apiToken"
            :placeholder="form.apiTokenConfigured ? '已配置；留空表示不修改' : '请输入 API Token'"
            autocomplete="new-password"
          />
          <div v-if="form.apiTokenConfigured" class="field-tip">{{ form.apiTokenMasked }}</div>
        </a-form-model-item>
        <a-form-model-item label="支持图片识别">
          <a-switch v-model="form.visionEnabled" />
          <span class="inline-tip">开启前请确认所选模型支持图片输入</span>
        </a-form-model-item>
        <a-form-model-item label="请求超时">
          <a-input-number v-model="form.timeoutSeconds" :min="15" :max="180" /> 秒
        </a-form-model-item>
        <a-form-model-item label="文件大小上限">
          <a-input-number v-model="form.maxFileMb" :min="1" :max="20" /> MB
        </a-form-model-item>
        <a-form-model-item label="补充解析规则">
          <a-textarea
            v-model="form.customPrompt"
            :rows="5"
            :max-length="4000"
            placeholder="可选，例如：没有填写库存状态时，已入库数量默认等于发运数量。"
          />
        </a-form-model-item>
        <a-form-model-item :wrapper-col="{ span: 16, offset: 5 }">
          <a-button type="primary" :loading="saving" @click="save">保存配置</a-button>
          <a-button style="margin-left:12px" :loading="testing" @click="testConnection">测试连接</a-button>
        </a-form-model-item>
      </a-form-model>
    </a-spin>
  </a-card>
</template>

<script>
import { getAction, putAction } from '@/api/manage'

export default {
  name: 'AiModelConfig',
  data () {
    return {
      loading: false,
      saving: false,
      testing: false,
      labelCol: { span: 5 },
      wrapperCol: { span: 16 },
      form: this.defaultForm(),
      rules: {
        apiUrl: [{ required: true, message: '请输入完整 API 地址', trigger: 'blur' }],
        modelName: [{ required: true, message: '请输入模型名称', trigger: 'blur' }]
      }
    }
  },
  created () {
    this.load()
  },
  methods: {
    defaultForm () {
      return {
        enabled: false,
        provider: 'OpenAI Compatible',
        apiUrl: '',
        modelName: '',
        apiToken: '',
        apiTokenConfigured: false,
        apiTokenMasked: '',
        timeoutSeconds: 60,
        maxFileMb: 10,
        visionEnabled: false,
        customPrompt: ''
      }
    },
    async load () {
      this.loading = true
      try {
        const res = await getAction('/ai/config/inventory')
        const body = res || {}
        if (body.code !== 200) throw new Error(this.errorMessage(body, '读取 AI 配置失败'))
        this.form = Object.assign(this.defaultForm(), body.data || {}, { apiToken: '' })
      } catch (e) {
        this.$message.error(e.message || '读取 AI 配置失败')
      } finally {
        this.loading = false
      }
    },
    save () {
      this.$refs.form.validate(async valid => {
        if (!valid) return
        if (!this.form.apiTokenConfigured && !this.form.apiToken) {
          this.$message.warning('首次配置时请输入 API Token')
          return
        }
        this.saving = true
        try {
          const res = await putAction('/ai/config/inventory', this.form)
          const body = res || {}
          if (body.code !== 200) throw new Error(this.errorMessage(body, '保存失败'))
          this.form = Object.assign(this.defaultForm(), body.data || {}, { apiToken: '' })
          this.$message.success('AI 模型配置已保存')
        } catch (e) {
          this.$message.error(e.message || '保存失败')
        } finally {
          this.saving = false
        }
      })
    },
    async testConnection () {
      this.testing = true
      try {
        const res = await getAction('/ai/config/inventory/test')
        const body = res || {}
        if (body.code !== 200) throw new Error(this.errorMessage(body, '连接失败'))
        this.$message.success(`连接成功：${(body.data && body.data.reply) || 'OK'}`)
      } catch (e) {
        this.$message.error(e.message || '连接失败')
      } finally {
        this.testing = false
      }
    },
    errorMessage (body, fallback) {
      return body && body.data && body.data.message ? body.data.message : fallback
    }
  }
}
</script>

<style scoped>
.field-tip,
.inline-tip {
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
}
.inline-tip {
  margin-left: 10px;
}
</style>
