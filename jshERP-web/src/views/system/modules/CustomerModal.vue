<template>
  <div ref="container">
    <a-modal
      :title="title"
      :width="1200"
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
      style="top:10%;height: 80%;">
      <template slot="footer">
        <a-button key="back" v-if="isReadOnly" @click="handleCancel">
          {{ $t('common.cancel') }}
        </a-button>
      </template>
      <a-spin :spinning="confirmLoading">
        <a-form :form="form" id="customerModal">
          <a-row class="form-row" :gutter="24">
            <a-col :span="24/2">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.name')">
                <a-input :placeholder="$t('common.enterName')" v-decorator.trim="[ 'supplier', validatorRules.supplier]" />
              </a-form-item>
            </a-col>
            <a-col :span="24/2">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.contact')">
                <a-input :placeholder="$t('system.contact')" v-decorator.trim="[ 'contacts' ]" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row class="form-row" :gutter="24">
            <a-col :span="24/2">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.phoneNo')">
                <a-input :placeholder="$t('system.enterPhoneNo')" v-decorator.trim="[ 'telephone', validatorRules.telephone ]" />
              </a-form-item>
            </a-col>
            <a-col :span="24/2">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.phone')">
                <a-input :placeholder="$t('system.enterPhone')" v-decorator.trim="[ 'phoneNum', validatorRules.phoneNum ]" />
              </a-form-item>
            </a-col>
            <a-col :span="24/2">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.email')">
                <a-input :placeholder="$t('system.enterEmail')" v-decorator.trim="[ 'email', validatorRules.email ]" />
              </a-form-item>
            </a-col>
            <a-col :span="24/2">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.fax')">
                <a-input :placeholder="$t('system.enterFax')" v-decorator.trim="[ 'fax' ]" />
              </a-form-item>
            </a-col>
            <a-col :span="24/2">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.beginNeedGetLabel')">
                <a-input-number style="width:100%" :min="0" :precision="2" :placeholder="$t('system.enterBeginNeedGet')" v-decorator="[ 'beginNeedGet' ]" />
              </a-form-item>
            </a-col>
            <a-col :span="24/2">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.allNeedGetLabel')">
                <a-input v-decorator.trim="[ 'allNeedGet' ]" :readOnly="true" />
              </a-form-item>
            </a-col>
            <a-col :span="24/2">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.taxNum')">
                <a-input :placeholder="$t('system.enterTaxNum')" v-decorator.trim="[ 'taxNum' ]" />
              </a-form-item>
            </a-col>
            <a-col :span="24/2">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.taxRateLabel')">
                <a-input-number style="width:100%" :min="0" :max="100" :precision="2" :placeholder="$t('purchase.form.columns.taxRate')" v-decorator="[ 'taxRate' ]" />
              </a-form-item>
            </a-col>
            <a-col :span="24/2">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.bankName')">
                <a-input :placeholder="$t('system.enterBankName')" v-decorator.trim="[ 'bankName' ]" />
              </a-form-item>
            </a-col>
            <a-col :span="24/2">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.accountNumber')">
                <a-input :placeholder="$t('system.enterAccountNumber')" v-decorator.trim="[ 'accountNumber' ]" />
              </a-form-item>
            </a-col>
            <a-col :span="24/2">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.address')">
                <a-input :placeholder="$t('common.address')" v-decorator.trim="[ 'address' ]" />
              </a-form-item>
            </a-col>
            <a-col :span="24/2">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.sort')">
                <a-input-number style="width:100%" :precision="0" :placeholder="$t('common.sort')" v-decorator="[ 'sort' ]" />
              </a-form-item>
            </a-col>
            <a-col :span="24/2">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.remark')">
                <a-textarea :rows="2" :placeholder="$t('common.enterRemark')" v-decorator.trim="[ 'description' ]" />
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>
      </a-spin>
    </a-modal>
  </div>
</template>
<script>
  import pick from 'lodash.pick'
  import {addSupplier,editSupplier,checkSupplier } from '@/api/api'
  import {autoJumpNextInput} from "@/utils/util"
  import {mixinDevice} from '@/utils/mixin'
  export default {
    name: "CustomerModal",
    mixins: [mixinDevice],
    data () {
      return {
        title:this.$t('common.action'),
        visible: false,
        model: {},
        isReadOnly: false,
        labelCol: {
          xs: { span: 24 },
          sm: { span: 4 },
        },
        wrapperCol: {
          xs: { span: 24 },
          sm: { span: 20 },
        },
        confirmLoading: false,
        form: this.$form.createForm(this),
        validatorRules:{
          supplier:{
            rules: [
              { required: true, message: this.$t('common.enterName') },
              { min: 2, max: 60, message: this.$t('system.lengthRange2to60'), trigger: 'blur' },
              { validator: this.validateSupplierName}
            ]
          },
          telephone: {
            rules: [{ pattern: /^$|^[0-9+\-\s()]{5,30}$/, message: this.$t('system.phoneFormatIncorrect') }]
          },
          phoneNum: {
            rules: [{ pattern: /^$|^[0-9+\-\s()]{5,30}$/, message: this.$t('system.phoneNumFormatIncorrect') }]
          },
          email: {
            rules: [{ type: 'email', message: this.$t('system.emailFormatIncorrect') }]
          }
        },
      }
    },
    created () {
    },
    methods: {
      add () {
        this.edit({});
      },
      edit (record) {
        this.form.resetFields();
        this.model = Object.assign({}, record);
        this.visible = true;
        this.$nextTick(() => {
          this.form.setFieldsValue(pick(this.model,'supplier', 'contacts', 'telephone', 'email',
            'phoneNum', 'fax', 'beginNeedGet', 'allNeedGet', 'taxNum', 'taxRate',
            'bankName', 'accountNumber', 'address', 'sort', 'description'))
          autoJumpNextInput('customerModal')
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
            if(this.model.beginNeedGet && this.model.beginNeedPay) {
              that.$message.warn(this.$t('system.beginNeedPayAndNeedGetConflict'));
              that.confirmLoading = false;
              return;
            }
            formData.type = "客户";
            let obj;
            if(!this.model.id){
              obj=addSupplier(formData);
            }else{
              obj=editSupplier(formData);
            }
            obj.then((res)=>{
              if(res.code === 200){
                that.$emit('ok');
                that.close();
              }else{
                that.$message.warning(res.data && res.data.message ? res.data.message : res.data);
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
      validateSupplierName(rule, value, callback){
        let params = {
          name: value,
          type: '客户',
          id: this.model.id?this.model.id:0
        };
        checkSupplier(params).then((res)=>{
          if(res && res.code===200) {
            if(!res.data.status){
              callback();
            } else {
              callback(this.$t('common.nameExists'));
            }
          } else {
            callback(res.data);
          }
        });
      }
    }
  }
</script>
<style scoped>

</style>
