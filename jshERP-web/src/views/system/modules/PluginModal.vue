<template>
  <div ref="container">
    <a-modal
      :title="title"
      :width="800"
      :visible="visible"
      :confirmLoading="confirmLoading"
      :getContainer="() => $refs.container"
      :maskStyle="{'top':'93px','left':'154px'}"
      :wrapClassName="wrapClassNameInfo()"
      :mask="isDesktop()"
      :maskClosable="false"
      @ok="handleOk"
      @cancel="handleCancel"
      :cancelText="$t('common.cancel')"
      :okText="$t('common.save')"
      style="top:20%;height: 50%;">
      <a-spin :spinning="confirmLoading">
        <a-form :form="form">
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.machineCode')">
            <a-input v-decorator.trim="[ 'platformKey' ]" :readOnly="true"/>
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.pluginActivationCode')">
            <a-textarea :rows="2" :placeholder="$t('system.enterPluginActivationCode')" v-decorator="[ 'platformValue' ]"/>
          </a-form-item>
        </a-form>
      </a-spin>
    </a-modal>
  </div>
</template>
<script>
  import pick from 'lodash.pick'
  import {getPlatformConfigByKey } from '@/api/api'
  import {mixinDevice} from '@/utils/mixin'
  import { getAction, postAction } from '../../../api/manage'
  export default {
    name: "PluginModal",
    mixins: [mixinDevice],
    data () {
      return {
        title:this.$t('common.action'),
        visible: false,
        model: {},
        machineCode: '',
        activationCode: '',
        labelCol: {
          xs: { span: 24 },
          sm: { span: 5 },
        },
        wrapperCol: {
          xs: { span: 24 },
          sm: { span: 16 },
        },
        confirmLoading: false,
        form: this.$form.createForm(this),
      }
    },
    created () {
    },
    methods: {
      edit () {
        this.form.resetFields();
        this.model = Object.assign({}, {});
        getAction("/plugin/getMacWithSecret").then((res)=>{
          if(res && res.code == 200) {
            this.model.platformKey = res.data
            getPlatformConfigByKey( {"platformKey": "activation_code"}).then((res)=>{
              if(res && res.code == 200) {
                let val = res.data.platformValue || ''
                this.model.platformValue = val.length > 4 ? '****' + val.substring(val.length - 4) : ''
                this.visible = true;
                this.$nextTick(() => {
                  this.form.setFieldsValue(pick(this.model, 'platformKey','platformValue'))
                });
              }
            })
          }
        })
      },
      close () {
        this.$emit('close');
        this.visible = false;
      },
      handleOk () {
        const that = this;
        // 触发表单验证
        this.form.validateFields((err, values) => {
          if (!err) {
            that.confirmLoading = true;
            let formData = Object.assign(this.model, values);
            formData.platformKey = 'activation_code'
            postAction('/platformConfig/updatePlatformConfigByKey', formData).then((res)=>{
              if(res.code === 200){
                that.$message.info(this.$t('system.fillSuccess'));
              }else{
                that.$message.warning(res.data.message);
              }
            }).finally(() => {
              that.confirmLoading = false;
              that.close();
            })
          }
        })
      },
      handleCancel () {
        this.close()
      }
    }
  }
</script>
<style scoped>

</style>