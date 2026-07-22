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
      style="top:20%;height: 50%;">
      <template slot="footer">
        <a-button key="back" v-if="isReadOnly" @click="handleCancel">
          取消
        </a-button>
      </template>
      <a-spin :spinning="confirmLoading">
        <a-form :form="form" id="inOutItemModal">
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" label="名称">
            <a-input placeholder="请输入名称" :disabled="isReadOnly" v-decorator.trim="[ 'name', validatorRules.name]" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" label="类型">
            <a-select placeholder="请选择类型" v-decorator="[ 'type', validatorRules.type]" :disabled="typeDisabled || isReadOnly">
              <a-select-option value="收入">收入</a-select-option>
              <a-select-option value="支出">支出</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" label="排序">
            <a-input placeholder="请输入排序" :disabled="isReadOnly" v-decorator.trim="[ 'sort', validatorRules.sort ]" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" label="备注">
            <a-textarea :rows="2" placeholder="请输入备注" :disabled="isReadOnly" v-decorator="[ 'remark', validatorRules.remark ]" />
          </a-form-item>
        </a-form>
      </a-spin>
    </a-modal>
  </div>
</template>
<script>
  import pick from 'lodash.pick'
  import {addInOutItem,editInOutItem,checkInOutItem } from '@/api/api'
  import {autoJumpNextInput} from "@/utils/util"
  import {mixinDevice} from '@/utils/mixin'
  export default {
    name: "InOutItemModal",
    mixins: [mixinDevice],
    data () {
      return {
        title:"操作",
        visible: false,
        model: {},
        typeParam: '',
        isReadOnly: false,
        typeDisabled: false,
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
              { max: 50, message: '长度不能超过 50 个字符', trigger: 'blur' }
            ]},
          type:{
            rules: [
              { required: true, message: '请选择类型!' }
            ]
          },
          sort:{
            rules: [
              { pattern: /^\d{1,10}$/, message: '排序只能填写不超过10位的非负整数', trigger: 'blur' }
            ]
          },
          remark:{
            rules: [
              { max: 100, message: '备注长度不能超过100个字符', trigger: 'blur' }
            ]
          }
        },
      }
    },
    created () {
    },
    methods: {
      add (type) {
        this.isReadOnly = false
        this.typeParam = type
        this.edit({});
      },
      edit (record, isReadOnly = false) {
        this.isReadOnly = isReadOnly
        this.form.resetFields();
        this.model = Object.assign({}, record);
        if(this.typeParam) {
          this.typeDisabled = true
          if(this.typeParam === 'in') {
            this.model.type = '收入'
          } else if(this.typeParam === 'out') {
            this.model.type = '支出'
          }
        } else {
          this.typeDisabled = false
        }
        this.visible = true;
        this.$nextTick(() => {
          this.form.setFieldsValue(pick(this.model,'name', 'type', 'sort', 'remark'))
          autoJumpNextInput('inOutItemModal')
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
            let formData = Object.assign(this.model, values);
            let obj;
            if(!this.model.id){
              obj=addInOutItem(formData);
            }else{
              obj=editInOutItem(formData);
            }
            obj.then((res)=>{
              if(res.code === 200){
                that.$emit('ok')
                that.confirmLoading = false
                that.close()
              } else {
                that.$message.warning(res.data.message);
                that.confirmLoading = false
              }
            }).finally(() => {
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
