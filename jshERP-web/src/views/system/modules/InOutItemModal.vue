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
      <template slot="footer">
        <a-button key="back" v-if="isReadOnly" @click="handleCancel">
          {{ $t('common.cancel') }}
        </a-button>
      </template>
      <a-spin :spinning="confirmLoading">
        <a-form :form="form" id="inOutItemModal">
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.name')">
            <a-input :placeholder="$t('common.enterName')" :disabled="isReadOnly" v-decorator.trim="[ 'name', validatorRules.name]" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.type')">
            <a-select :placeholder="$t('common.selectType')" v-decorator="[ 'type', validatorRules.type]" :disabled="typeDisabled || isReadOnly">
              <a-select-option value="收入">{{ $t('financial.itemIncome') }}</a-select-option>
              <a-select-option value="支出">{{ $t('financial.itemExpense') }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.sort')">
            <a-input :placeholder="$t('common.sort')" :disabled="isReadOnly" v-decorator.trim="[ 'sort', validatorRules.sort ]" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.remark')">
            <a-textarea :rows="2" :placeholder="$t('common.enterRemark')" :disabled="isReadOnly" v-decorator="[ 'remark', validatorRules.remark ]" />
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
        title:this.$t('common.action'),
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
              { required: true, message: this.$t('common.enterName') },
              { max: 50, message: this.$t('system.nameLength50'), trigger: 'blur' }
            ]},
          type:{
            rules: [
              { required: true, message: this.$t('system.selectTypeRequired') }
            ]
          },
          sort:{
            rules: [
              { pattern: /^\d{1,10}$/, message: this.$t('system.sortNonNegativeInt10'), trigger: 'blur' }
            ]
          },
          remark:{
            rules: [
              { max: 100, message: this.$t('system.remarkLength100'), trigger: 'blur' }
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
