<template>
  <a-card :title="$t('system.aiModelConfig')" :bordered="false">
    <a-alert :message="$t('system.aiImportTip')" type="info" show-icon style="margin-bottom:24px" />
    <a-spin :spinning="loading">
      <a-form-model ref="form" :model="form" :rules="rules" :label-col="{span:5}" :wrapper-col="{span:14}">
        <a-form-model-item :label="$t('system.enableAiImport')"><a-switch v-model="form.enabled" /></a-form-model-item>
        <a-form-model-item :label="$t('system.apiFormat')" prop="apiFormat">
          <a-select v-model="form.apiFormat" @change="handleApiFormatChange">
            <a-select-option value="OPENAI">{{ $t('system.openaiCompatible') }}</a-select-option>
            <a-select-option value="ANTHROPIC">{{ $t('system.anthropicCompatible') }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item :label="$t('system.apiUrl')" prop="apiUrl" :extra="form.apiFormat==='ANTHROPIC' ? $t('system.anthropicSwitchTip') : $t('system.openaiSwitchTip')"><a-input v-model.trim="form.apiUrl" :placeholder="form.apiFormat==='ANTHROPIC' ? $t('system.anthropicPlaceholder') : $t('system.openaiPlaceholder')" /></a-form-model-item>
        <a-form-model-item :label="$t('system.modelName')" prop="modelName"><a-input v-model.trim="form.modelName" :placeholder="$t('system.modelNamePlaceholder')" /></a-form-model-item>
        <a-form-model-item :label="$t('system.apiToken')" prop="apiToken"><a-input-password v-model.trim="form.apiToken" :placeholder="form.apiTokenConfigured ? $t('system.apiTokenConfigured') : $t('system.enterApiToken')" /></a-form-model-item>
        <a-form-model-item :label="$t('system.visionEnabled')"><a-switch v-model="form.visionEnabled" /></a-form-model-item>
        <a-form-model-item :label="$t('system.requestTimeout')"><a-input-number v-model="form.timeoutSeconds" :min="15" :max="180" /> {{ $t('system.seconds') }}</a-form-model-item>
        <a-form-model-item :label="$t('system.maxFileSize')"><a-input-number v-model="form.maxFileMb" :min="1" :max="20" /> MB</a-form-model-item>
        <a-form-model-item :label="$t('system.customPrompt')"><a-textarea v-model="form.customPrompt" :rows="4" :max-length="4000" /></a-form-model-item>
        <a-form-model-item :wrapper-col="{span:14,offset:5}"><a-button type="primary" :loading="saving" @click="save">{{ $t('common.save') }}</a-button><a-button style="margin-left:10px" :loading="testing" @click="test">{{ $t('system.testConnection') }}</a-button></a-form-model-item>
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
        apiUrl: [{ required: true, message: this.$t('system.apiUrlRequired'), trigger: 'blur' }],
        modelName: [{ required: true, message: this.$t('system.modelNameRequired'), trigger: 'blur' }]
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
        this.$message.error(e.message || this.$t('system.readConfigFailed'))
      } finally {
        this.loading = false
      }
    },
    save () {
      this.$refs.form.validate(async valid => {
        if (!valid) return
        if (!this.form.apiTokenConfigured && !this.form.apiToken) {
          this.$message.warning(this.$t('system.firstConfigTokenRequired'))
          return
        }
        this.form.apiUrl = this.normalizedApiUrl(this.form.apiFormat, this.form.apiUrl)
        this.saving = true
        try {
          const r = await putAction('/ai/config', this.form)
          if (r.code !== 200) throw new Error(r.data && r.data.message)
          this.form = Object.assign(this.empty(), r.data, { apiToken: '' })
          this.$message.success(this.$t('system.saved'))
        } catch (e) {
          this.$message.error(e.message || this.$t('system.saveFailed'))
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
        this.$message.success(this.$t('system.connectionSuccess') + (r.data.reply || 'OK'))
      } catch (e) {
        this.$message.error(e.message || this.$t('system.connectionFailed'))
      } finally {
        this.testing = false
      }
    }
  }
}
</script>
