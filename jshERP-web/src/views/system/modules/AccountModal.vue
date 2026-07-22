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
      cancelText="取消"
      okText="保存"
      style="top:15%;height: 55%;">
      <template slot="footer">
        <a-button key="back" v-if="isReadOnly" @click="handleCancel">
          取消
        </a-button>
      </template>
      <a-spin :spinning="confirmLoading">
        <a-form :form="form" id="accountModal">
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" label="名称">
            <a-input placeholder="请输入名称" v-decorator.trim="[ 'name', validatorRules.name]" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" label="编号">
            <a-input placeholder="请输入编号" v-decorator.trim="[ 'serialNo', validatorRules.serialNo ]" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" label="期初金额">
            <a-input placeholder="请输入期初金额" v-decorator.trim="[ 'initialAmount', validatorRules.initialAmount ]" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" label="排序">
            <a-input placeholder="请输入排序" v-decorator.trim="[ 'sort', validatorRules.sort ]" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" label="备注">
            <a-textarea :rows="2" placeholder="请输入备注" v-decorator="[ 'remark', validatorRules.remark ]" />
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
        title:"操作",
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
              { required: true, message: '请输入名称!' },
              { max: 50, message: '长度不能超过 50 个字符', trigger: 'blur' },
              { validator: this.validateAccountName}
            ]
          },
          serialNo: { rules: [{ max: 50, message: '编号不能超过 50 个字符', trigger: 'blur' }] },
          initialAmount: { rules: [{ validator: this.validateInitialAmount }] },
          sort: { rules: [
              { max: 10, message: '排序不能超过 10 个字符', trigger: 'blur' },
              { pattern: /^\d*$/, message: '排序必须为非负整数', trigger: 'blur' }
            ] },
          remark: { rules: [{ max: 100, message: '备注不能超过 100 个字符', trigger: 'blur' }] }
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
                that.$message.warning((res.data && res.data.message) || res.data || '保存失败');
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
              callback("名称已经存在");
            }
          } else {
            callback(res.data);
          }
        }).catch(() => callback('名称校验失败，请稍后重试'));
      },
      validateInitialAmount(rule, value, callback) {
        if(value === undefined || value === null || value === '') {
          callback();
          return;
        }
        if(!/^-?\d{1,18}(\.\d{1,6})?$/.test(String(value))) {
          callback('期初金额最多18位整数、6位小数');
          return;
        }
        callback();
      }
    }
  }
</script>
<style scoped>

</style>
