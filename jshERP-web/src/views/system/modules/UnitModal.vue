<template>
  <div ref="container">
    <a-modal
      :title="title"
      :width="700"
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
      style="top:100px; height:55%;">
      <template slot="footer">
        <a-button key="back" v-if="isReadOnly" @click="handleCancel">
          {{ $t('common.cancel') }}
        </a-button>
      </template>
      <a-spin :spinning="confirmLoading">
        <a-form :form="form" id="unitModal">
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.basicUnit')">
            <a-input :placeholder="$t('system.enterBasicUnit')" v-decorator.trim="[ 'basicUnit', validatorRules.basicUnit]" />
          </a-form-item>
        </a-form>
        <a-form :form="form">
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.otherUnit')">
            <a-input :placeholder="$t('system.enterOtherUnit')" style="width:48%" v-decorator.trim="[ 'otherUnit' ]" />
            =
            <a-input :suffix="$t('system.basicUnit')" :placeholder="$t('system.enterRatio')" style="width:48%" v-decorator.trim="[ 'ratio' ]" />
          </a-form-item>
        </a-form>
        <a-form :form="form">
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.otherUnit2')">
            <a-input :placeholder="$t('system.enterOtherUnit2')" style="width:48%" v-decorator.trim="[ 'otherUnitTwo' ]" />
            =
            <a-input :suffix="$t('system.basicUnit')" :placeholder="$t('system.enterRatio2')" style="width:48%" v-decorator.trim="[ 'ratioTwo' ]" />
          </a-form-item>
        </a-form>
        <a-form :form="form">
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.otherUnit3')">
            <a-input :placeholder="$t('system.enterOtherUnit3')" style="width:48%" v-decorator.trim="[ 'otherUnitThree' ]" />
            =
            <a-input :suffix="$t('system.basicUnit')" :placeholder="$t('system.enterRatio3')" style="width:48%" v-decorator.trim="[ 'ratioThree' ]" />
          </a-form-item>
        </a-form>
      </a-spin>
    </a-modal>
  </div>
</template>
<script>
  import pick from 'lodash.pick'
  import {addUnit,editUnit,checkUnit } from '@/api/api'
  import {autoJumpNextInput} from "@/utils/util"
  import {isDecimalThree} from "@/utils/validate"
  import {mixinDevice} from '@/utils/mixin'
  export default {
    name: "UnitModal",
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
          basicUnit:{
            rules: [
              { required: true, message: this.$t('system.basicUnitRequired') },
              { min: 1, max: 10, message: this.$t('system.basicUnitLength'), trigger: 'blur' }
            ]},
          otherUnit:{
            rules: [
              { required: true, message: this.$t('system.otherUnitRequired') },
              { min: 1, max: 10, message: this.$t('system.otherUnitLength'), trigger: 'blur' }
            ]},
          ratio:{
            rules: [
              { required: true, message: this.$t('system.ratioRequired') },
              { validator: this.validateRatio}
            ]}
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
          this.form.setFieldsValue(pick(this.model,'basicUnit','otherUnit','ratio','otherUnitTwo','ratioTwo','otherUnitThree','ratioThree'))
          autoJumpNextInput('unitModal')
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
            if(!formData.otherUnit) {
              that.$message.warning(this.$t('system.otherUnitEmpty'));
              that.confirmLoading = false;
              return;
            }
            if(formData.otherUnit) {
              if(!formData.ratio) {
                that.$message.warning(this.$t('system.ratioEmpty'));
                that.confirmLoading = false;
                return;
              }
              if(!isDecimalThree(formData.ratio)) {
                that.$message.warning(this.$t('system.ratioFormat'))
                that.confirmLoading = false
                return
              }
              if(Number(formData.ratio) <= 1) {
                that.$message.warning(this.$t('system.ratioGreaterThan1'))
                that.confirmLoading = false
                return
              }
            }
            if(formData.otherUnitTwo) {
              if(!formData.ratioTwo) {
                that.$message.warning(this.$t('system.ratio2Empty'));
                that.confirmLoading = false;
                return;
              }
              if(!isDecimalThree(formData.ratioTwo)) {
                that.$message.warning(this.$t('system.ratio2Format'))
                that.confirmLoading = false
                return
              }
              if(Number(formData.ratioTwo) <= 1) {
                that.$message.warning(this.$t('system.ratio2GreaterThan1'))
                that.confirmLoading = false
                return
              }
            }
            if(formData.otherUnitThree) {
              if(!formData.ratioThree) {
                that.$message.warning(this.$t('system.ratio3Empty'));
                that.confirmLoading = false;
                return;
              }
              if(!isDecimalThree(formData.ratioThree)) {
                that.$message.warning(this.$t('system.ratio3Format'))
                that.confirmLoading = false
                return
              }
              if(Number(formData.ratioThree) <= 1) {
                that.$message.warning(this.$t('system.ratio3GreaterThan1'))
                that.confirmLoading = false
                return
              }
            }
            if(!formData.otherUnitTwo && formData.otherUnitThree) {
              that.$message.warning(this.$t('system.otherUnit2Before3'));
              that.confirmLoading = false;
              return;
            }
            if(formData.basicUnit === formData.otherUnit) {
              that.$message.warning(this.$t('system.basicOtherUnitSame'));
              that.confirmLoading = false;
              return;
            }
            if(formData.basicUnit === formData.otherUnitTwo) {
              that.$message.warning(this.$t('system.basicOtherUnit2Same'));
              that.confirmLoading = false;
              return;
            }
            if(formData.basicUnit === formData.otherUnitThree) {
              that.$message.warning(this.$t('system.basicOtherUnit3Same'));
              that.confirmLoading = false;
              return;
            }
            const unitNames = [formData.basicUnit, formData.otherUnit, formData.otherUnitTwo, formData.otherUnitThree].filter(Boolean)
            if(new Set(unitNames).size !== unitNames.length) {
              that.$message.warning(this.$t('system.unitNameDuplicate'))
              that.confirmLoading = false
              return
            }
            let obj;
            if(!this.model.id){
              obj=addUnit(formData);
            }else{
              obj=editUnit(formData);
            }
            obj.then((res)=>{
              if(res.code === 200){
                that.$emit('ok');
                that.close();
              }else{
                that.$message.warning(res.data.message);
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
      validateRatio(rule, value, callback) {
        if (value > 1) {
          callback();
        } else {
          callback(this.$t('system.ratioGreaterThan1Validator'));
        }
      }
    }
  }
</script>
<style scoped>

</style>
