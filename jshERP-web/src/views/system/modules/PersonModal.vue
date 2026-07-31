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
      style="top:20%;height: 45%;">
      <template slot="footer">
        <a-button key="back" v-if="isReadOnly" @click="handleCancel">
          {{ $t('common.cancel') }}
        </a-button>
      </template>
      <a-spin :spinning="confirmLoading">
        <a-form :form="form" id="personModal">
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.personName')">
            <a-input :placeholder="$t('system.enterPersonName')" :disabled="isReadOnly" v-decorator.trim="[ 'name', validatorRules.name]" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.type')">
            <a-select :placeholder="$t('common.selectType')" :disabled="isReadOnly" v-decorator="[ 'type', validatorRules.type]">
              <a-select-option value="销售员">{{ $t('system.salesPerson') }}</a-select-option>
              <a-select-option value="仓管员">{{ $t('system.warehouseKeeper') }}</a-select-option>
              <a-select-option value="财务员">{{ $t('system.financeStaff') }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.sort')">
            <a-input :placeholder="$t('common.sort')" :disabled="isReadOnly" v-decorator.trim="[ 'sort', validatorRules.sort ]" />
          </a-form-item>
        </a-form>
      </a-spin>
    </a-modal>
  </div>
</template>
<script>
  import pick from 'lodash.pick'
  import {addPerson,editPerson,checkPerson } from '@/api/api'
  import {autoJumpNextInput} from "@/utils/util"
  import {mixinDevice} from '@/utils/mixin'
  export default {
    name: "PersonModal",
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
              { required: true, message: this.$t('system.personNameRequired') },
              { max: 50, message: this.$t('system.nameLength50'), trigger: 'blur' },
              { validator: this.validatePersonName}
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
          }
        },
      }
    },
    created () {
    },
    methods: {
      add () {
        this.isReadOnly = false
        this.edit({});
      },
      edit (record, isReadOnly = false) {
        this.isReadOnly = isReadOnly
        this.form.resetFields();
        this.model = Object.assign({}, record);
        this.visible = true;
        this.$nextTick(() => {
          this.form.setFieldsValue(pick(this.model,'name', 'type', 'sort'))
          autoJumpNextInput('personModal')
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
              obj=addPerson(formData);
            }else{
              obj=editPerson(formData);
            }
            obj.then((res)=>{
              if(res.code === 200){
                that.$emit('ok')
                that.confirmLoading = false
                that.close()
              }else{
                that.$message.warning(res.data.message);
                that.confirmLoading = false
              }
            }).catch(() => {
              that.confirmLoading = false
            })
          }
        })
      },
      handleCancel () {
        this.close()
      },
      validatePersonName(rule, value, callback){
        let params = {
          name: value,
          id: this.model.id?this.model.id:0
        };
        checkPerson(params).then((res)=>{
          if(res && res.code===200) {
            if(!res.data.status){
              callback();
            } else {
              callback(this.$t('common.nameExists'));
            }
          } else {
            callback(this.$t('system.nameCheckFailed'));
          }
        }).catch(() => callback(this.$t('system.nameCheckFailed')));
      }
    }
  }
</script>
<style scoped>

</style>
