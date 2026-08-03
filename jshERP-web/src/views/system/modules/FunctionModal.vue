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
      style="top:5%;height: 90%;">
      <template slot="footer">
        <a-button key="back" v-if="isReadOnly" @click="handleCancel">
          {{ $t('common.cancel') }}
        </a-button>
      </template>
      <a-spin :spinning="confirmLoading">
        <a-form :form="form" id="functionModal">
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.serialNo')">
            <a-input :placeholder="$t('common.enterNumber')" v-decorator.trim="[ 'number', validatorRules.number]" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.name')">
            <a-input :placeholder="$t('common.enterName')" v-decorator.trim="[ 'name', validatorRules.name]" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.parentNumber')">
            <a-input-search :placeholder="$t('system.selectParentNumber')" v-decorator.trim="[ 'parentNumber', validatorRules.parentNumber ]"
                            @search="onSearchParentNumber" :readOnly="true" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.parentName')">
            <a-input v-decorator.trim="[ 'parentName' ]" :readOnly="true" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.link')">
            <a-input :placeholder="$t('system.enterLink')" v-decorator.trim="[ 'url', validatorRules.url ]" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.component')">
            <a-input :placeholder="$t('system.enterComponent')" v-decorator.trim="[ 'component', validatorRules.component ]" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.sort')">
            <a-input :placeholder="$t('common.sort')" v-decorator.trim="[ 'sort', validatorRules.sort ]" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.functionButton')">
            <j-select-multiple :placeholder="$t('system.selectFunctionButton')" v-model="jselectMultiple.value" :options="jselectMultiple.options"/>
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.iconLabel')">
            <a-input :placeholder="$t('system.enterIcon')" v-decorator.trim="[ 'icon', validatorRules.icon ]" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.isEnabled')">
            <a-switch :checked-children="$t('common.enable')" :un-checked-children="$t('common.disable')" v-model="enabledSwitch" @change="onChange"/>
          </a-form-item>
        </a-form>
      </a-spin>
      <function-tree-modal ref="functionTreeModal" @ok="functionTreeModalOk"></function-tree-modal>
    </a-modal>
  </div>
</template>
<script>
  import pick from 'lodash.pick'
  import FunctionTreeModal from './FunctionTreeModal'
  import {addFunction,editFunction,checkFunction, checkNumber } from '@/api/api'
  import {autoJumpNextInput} from "@/utils/util"
  import {mixinDevice} from '@/utils/mixin'
  import JSelectMultiple from '@/components/jeecg/JSelectMultiple'
  export default {
    name: "FunctionModal",
    mixins: [mixinDevice],
    components: {
      FunctionTreeModal,
      JSelectMultiple
    },
    data () {
      return {
        title:this.$t('common.action'),
        visible: false,
        model: {},
        enabledSwitch: true, //是否启用
        isReadOnly: false,
        jselectMultiple: {
          options: [
            { text: this.$t('system.btnEdit'), value: '1' },
            { text: this.$t('system.btnAudit'), value: '2' },
            { text: this.$t('system.btnUnaudit'), value: '7' },
            { text: this.$t('system.btnExport'), value: '3' },
            { text: this.$t('system.btnEnableDisable'), value: '4' },
            { text: this.$t('system.btnPrint'), value: '5' },
            { text: this.$t('system.btnVoid'), value: '6' }
          ],
          value: ''
        },
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
          number:{
            rules: [
              { required: true, message: this.$t('system.numberRequired') },
              { min: 2, max: 30, message: this.$t('system.numberLength'), trigger: 'blur' },
              { validator: this.validateNumber}
            ]
          },
          name:{
            rules: [
              { required: true, message: this.$t('common.enterName') },
              { min: 2, max: 30, message: this.$t('system.roleNameLength'), trigger: 'blur' },
              { validator: this.validateName}
            ]
          },
          parentNumber:{
            rules: [
              { required: true, message: this.$t('system.parentNumberRequired') }
            ]
          },
          url:{
            rules: [
              { required: true, message: this.$t('system.linkRequired') }
            ]
          },
          component:{
            rules: [
              { required: true, message: this.$t('system.componentRequired') }
            ]
          },
          sort:{
            rules: [
              { required: true, message: this.$t('system.sortRequired') }
            ]
          },
          icon:{
            rules: [
              { required: true, message: this.$t('system.iconRequired') }
            ]
          },
        },
      }
    },
    created () {
    },
    methods: {
      onChange(checked) {
        this.model.enabled = checked
      },
      add () {
        this.edit({});
        this.model.enabled = true
        this.enabledSwitch = true
      },
      edit (record) {
        this.form.resetFields();
        this.model = Object.assign({}, record);
        this.visible = true;
        if(record.enabled!=null){
          this.enabledSwitch = record.enabled?true:false;
        }
        if(this.model.id){
          this.jselectMultiple.value = record.pushBtn
        } else {
          this.jselectMultiple.value = ''
        }
        this.$nextTick(() => {
          this.form.setFieldsValue(pick(this.model,'number', 'name', 'parentNumber', 'parentName', 'url', 'component', 'sort', 'pushBtn', 'icon', 'enabled'))
          autoJumpNextInput('functionModal')
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
            formData.pushBtn = this.jselectMultiple.value
            let obj;
            if(!this.model.id){
              obj=addFunction(formData);
            }else{
              obj=editFunction(formData);
            }
            obj.then((res)=>{
              if(res.code === 200){
                that.$emit('ok');
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
      },
      validateNumber(rule, value, callback){
        let params = {
          number: value,
          id: this.model.id?this.model.id:0
        };
        checkNumber(params).then((res)=>{
          if(res && res.code===200) {
            if(!res.data.status){
              callback();
            } else {
              callback(this.$t('system.numberExists'));
            }
          } else {
            callback(res.data);
          }
        });
      },
      validateName(rule, value, callback){
        let params = {
          name: value,
          id: this.model.id?this.model.id:0
        };
        checkFunction(params).then((res)=>{
          if(res && res.code===200) {
            if(!res.data.status){
              callback();
            } else {
              callback(this.$t('system.nameExistsExcl'));
            }
          } else {
            callback(res.data);
          }
        });
      },
      onSearchParentNumber() {
        this.$refs.functionTreeModal.edit(this.model.id);
        this.$refs.functionTreeModal.title = this.$t('system.selectParentNumberTitle');
        this.$refs.functionTreeModal.disableSubmit = false;
      },
      functionTreeModalOk(number, name) {
        this.form.setFieldsValue({'parentNumber': number, 'parentName': name})
      }
    }
  }
</script>
<style scoped>

</style>