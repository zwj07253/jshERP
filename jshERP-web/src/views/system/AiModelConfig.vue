<template>
  <a-card title="AI 模型配置" :bordered="false">
    <a-alert message="AI 导入会把上传文件中的业务内容发送到此处配置的模型。Token 只会在后端加密保存。" type="info" show-icon style="margin-bottom:24px" />
    <a-spin :spinning="loading">
      <a-form-model ref="form" :model="form" :rules="rules" :label-col="{span:5}" :wrapper-col="{span:14}">
        <a-form-model-item label="启用 AI 导入"><a-switch v-model="form.enabled" /></a-form-model-item>
        <a-form-model-item label="API 格式" prop="apiFormat">
          <a-select v-model="form.apiFormat" @change="handleApiFormatChange">
            <a-select-option value="OPENAI">OpenAI 兼容</a-select-option>
            <a-select-option value="ANTHROPIC">Anthropic 兼容</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="API 地址" prop="apiUrl" :extra="form.apiFormat==='ANTHROPIC' ? '切换格式时会自动转换为 /anthropic/v1/messages。' : '切换格式时会自动转换为 /v1/chat/completions。'"><a-input v-model.trim="form.apiUrl" :placeholder="form.apiFormat==='ANTHROPIC' ? '例如 https://.../anthropic' : '例如 https://.../v1'" /></a-form-model-item>
        <a-form-model-item label="模型名称" prop="modelName"><a-input v-model.trim="form.modelName" placeholder="例如 gpt-4.1-mini" /></a-form-model-item>
        <a-form-model-item label="API Token" prop="apiToken"><a-input-password v-model.trim="form.apiToken" :placeholder="form.apiTokenConfigured ? '已配置，留空不修改' : '请输入 API Token'" /></a-form-model-item>
        <a-form-model-item label="支持图片识别"><a-switch v-model="form.visionEnabled" /></a-form-model-item>
        <a-form-model-item label="请求超时"><a-input-number v-model="form.timeoutSeconds" :min="15" :max="180" /> 秒</a-form-model-item>
        <a-form-model-item label="文件大小上限"><a-input-number v-model="form.maxFileMb" :min="1" :max="20" /> MB</a-form-model-item>
        <a-form-model-item label="补充规则"><a-textarea v-model="form.customPrompt" :rows="4" :max-length="4000" /></a-form-model-item>
        <a-form-model-item :wrapper-col="{span:14,offset:5}"><a-button type="primary" :loading="saving" @click="save">保存</a-button><a-button style="margin-left:10px" :loading="testing" @click="test">测试连接</a-button></a-form-model-item>
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
      form: this.empty(),
      rules: {
        apiUrl: [{ required: true, message: '请输入 API 地址', trigger: 'blur' }],
        modelName: [{ required: true, message: '请输入模型名称', trigger: 'blur' }]
      }
    }
  },
  created () { this.load() },
  methods: {
    empty () {
      return { enabled: false, apiFormat: 'OPENAI', apiUrl: '', modelName: '', apiToken: '', apiTokenConfigured: false, timeoutSeconds: 60, maxFileMb: 10, visionEnabled: false, customPrompt: '' }
    },
    endpointBase (url) {
      let value = (url || '').trim().replace(/\/+$/, '')
      const suffixes = ['/anthropic/v1/messages', '/v1/chat/completions', '/v1/messages', '/chat/completions', '/anthropic']
      let changed = true
      while (value && changed) {
        changed = false
        for (const suffix of suffixes) {
          if (value.endsWith(suffix)) {
            value = value.slice(0, -suffix.length).replace(/\/+$/, '')
            changed = true
            break
          }
        }
      }
      return value
    },
    normalizedApiUrl (format, url) {
      const base = this.endpointBase(url)
      if (!base) return ''
      const protocolBase = format === 'ANTHROPIC' && base.endsWith('/v1') ? base.slice(0, -3) : base
      return format === 'ANTHROPIC' ? `${protocolBase}/anthropic/v1/messages` : `${protocolBase}/v1/chat/completions`
    },
    handleApiFormatChange (format) {
      this.form.apiUrl = this.normalizedApiUrl(format, this.form.apiUrl)
    },
    async load () {
      this.loading = true
      try {
        const r = await getAction('/ai/config')
        if (r.code !== 200) throw new Error(r.data && r.data.message)
        this.form = Object.assign(this.empty(), r.data, { apiToken: '' })
        this.form.apiUrl = this.normalizedApiUrl(this.form.apiFormat, this.form.apiUrl)
      } catch (e) {
        this.$message.error(e.message || '读取配置失败')
      } finally {
        this.loading = false
      }
    },
    save () {
      this.$refs.form.validate(async valid => {
        if (!valid) return
        if (!this.form.apiTokenConfigured && !this.form.apiToken) {
          this.$message.warning('首次配置必须填写 API Token')
          return
        }
        this.form.apiUrl = this.normalizedApiUrl(this.form.apiFormat, this.form.apiUrl)
        this.saving = true
        try {
          const r = await putAction('/ai/config', this.form)
          if (r.code !== 200) throw new Error(r.data && r.data.message)
          this.form = Object.assign(this.empty(), r.data, { apiToken: '' })
          this.$message.success('已保存')
        } catch (e) {
          this.$message.error(e.message || '保存失败')
        } finally {
          this.saving = false
        }
      })
    },
    async test () {
      this.testing = true
      try {
        const r = await getAction('/ai/config/test')
        if (r.code !== 200) throw new Error(r.data && r.data.message)
        this.$message.success('连接成功：' + (r.data.reply || 'OK'))
      } catch (e) {
        this.$message.error(e.message || '连接失败')
      } finally {
        this.testing = false
      }
    }
  }
}
</script>
