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
      style="top:15%;height: 55%;">
      <template slot="footer">
        <a-button key="back" v-if="isReadOnly" @click="handleCancel">
          {{ $t('common.cancel') }}
        </a-button>
      </template>
      <a-spin :spinning="confirmLoading">
        <a-form :form="form" id="accountModal">
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.name')">
            <a-input :placeholder="$t('common.enterName')" v-decorator.trim="[ 'name', validatorRules.name]" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.serialNo')">
            <a-input :placeholder="$t('common.enterNumber')" v-decorator.trim="[ 'serialNo', validatorRules.serialNo ]" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.initialAmountLabel')">
            <a-input :placeholder="$t('system.enterInitialAmount')" v-decorator.trim="[ 'initialAmount', validatorRules.initialAmount ]" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.sort')">
            <a-input :placeholder="$t('common.sort')" v-decorator.trim="[ 'sort', validatorRules.sort ]" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.remark')">
            <a-textarea :rows="2" :placeholder="$t('common.enterRemark')" v-decorator="[ 'remark', validatorRules.remark ]" />
          </a-form-item>
        </a-form>
      </a-spin>
    </a-modal>
  </div>
</template>
<script>
  import pick from 'lodash.pick'
  import {addAccount,editAccount,checkAccount } from '@/api/api'
  import {autoJumpNextInput} from "@/utils/util"
  import {mixinDevice} from '@/utils/mixin'
  export default {
    name: "AccountModal",
    mixins: [mixinDevice],
    data () {
      return {
        title:this.$t('common.action'),
        visible: false,
        model: {},
        isReadOnly: false,
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
        validatorRules:{
          name:{
            rules: [
              { required: true, message: this.$t('common.enterName') },
              { max: 50, message: this.$t('system.nameLength50'), trigger: 'blur' },
              { validator: this.validateAccountName}
            ]
          },
          serialNo: { rules: [{ max: 50, message: this.$t('system.serialNoLength50'), trigger: 'blur' }] },
          initialAmount: { rules: [{ validator: this.validateInitialAmount }] },
          sort: { rules: [
              { max: 10, message: this.$t('system.sortLength10'), trigger: 'blur' },
              { pattern: /^\d*$/, message: this.$t('system.sortNonNegativeInt'), trigger: 'blur' }
            ] },
          remark: { rules: [{ max: 100, message: this.$t('system.remarkLength100'), trigger: 'blur' }] }
        },
      }
    },
    created () {
    },
    methods: {
      add () {
        this.isReadOnly = false;
        this.edit({});
      },
      edit (record) {
        this.form.resetFields();
        this.model = Object.assign({}, record);
        this.visible = true;
        this.$nextTick(() => {
          this.form.setFieldsValue(pick(this.model,'name', 'serialNo', 'initialAmount', 'sort', 'remark'))
          autoJumpNextInput('accountModal')
        });
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
            let formData = Object.assign({}, values);
            let obj;
            if(!this.model.id){
              obj=addAccount(formData);
            }else{
              formData.id = this.model.id;
              obj=editAccount(formData);
            }
            obj.then((res)=>{
              if(res.code === 200){
                that.$emit('ok');
                that.close();
              }else{
                that.$message.warning((res.data && res.data.message) || res.data || this.$t('system.saveFailed'));
              }
            }).finally(() => {
              that.confirmLoading = false;
            })
          }
        })
      },
      handleCancel () {
        this.close()
      },
      validateAccountName(rule, value, callback){
        let params = {
          name: value,
          id: this.model.id?this.model.id:0
        };
        checkAccount(params).then((res)=>{
          if(res && res.code===200) {
            if(!res.data.status){
              callback();
            } else {
              callback(this.$t('common.nameExists'));
            }
          } else {
            callback(res.data);
          }
        }).catch(() => callback(this.$t('system.nameCheckFailed')));
      },
      validateInitialAmount(rule, value, callback) {
        if(value === undefined || value === null || value === '') {
          callback();
          return;
        }
        if(!/^-?\d{1,18}(\.\d{1,6})?$/.test(String(value))) {
          callback(this.$t('system.initialAmountFormat'));
          return;
        }
        callback();
      }
    }
  }
</script>
<style scoped>

</style>
