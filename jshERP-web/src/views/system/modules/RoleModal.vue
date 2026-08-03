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
      style="top:15%;height: 60%;">
      <template slot="footer">
        <a-button key="back" v-if="isReadOnly" @click="handleCancel">
          {{ $t('common.cancel') }}
        </a-button>
      </template>
      <a-spin :spinning="confirmLoading">
        <a-form :form="form" id="roleModal">
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.roleName')">
            <a-input :placeholder="$t('common.roleName')" v-decorator.trim="[ 'name', validatorRules.name]" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.dataTypeLabel')">
            <a-select :placeholder="$t('system.selectDataType')" v-decorator="[ 'type', validatorRules.type]" style="width:94%">
              <a-select-option value="全部数据">{{ $t('system.allData') }}</a-select-option>
              <a-select-option value="本部门数据">{{ $t('system.deptData') }}</a-select-option>
              <a-select-option value="个人数据">{{ $t('system.personalData') }}</a-select-option>
            </a-select>
            <a-tooltip :title="$t('common.dataTypeTip')">
              <a-icon type="question-circle" style="width:6%; padding-left: 5px; font-size: 18px;" />
            </a-tooltip>
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.priceLimitLabel')">
            <j-select-multiple style="width:94%" :placeholder="$t('system.selectPriceLimit')" v-model="priceLimitList.value" :options="priceLimitList.options"/>
            <a-tooltip :title="$t('common.priceLimitTip')">
              <a-icon type="question-circle" style="width:6%; padding-left: 5px; font-size: 18px;" />
            </a-tooltip>
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.remark')">
            <a-textarea :rows="1" :placeholder="$t('common.enterRemark')" v-decorator="[ 'description', validatorRules.description ]" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.sort')">
            <a-input :placeholder="$t('common.sort')" v-decorator.trim="[ 'sort' ]" />
          </a-form-item>
        </a-form>
      </a-spin>
    </a-modal>
  </div>
</template>
<script>
  import pick from 'lodash.pick'
  import JSelectMultiple from '@/components/jeecg/JSelectMultiple'
  import {addRole,editRole,checkRole } from '@/api/api'
  import {autoJumpNextInput} from "@/utils/util"
  import {mixinDevice} from '@/utils/mixin'
  export default {
    name: "RoleModal",
    mixins: [mixinDevice],
    components: {
      JSelectMultiple
    },
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
        priceLimitList: {
          options: [
            { 'value': '1', 'text': this.$t('system.priceLimitHomePagePurchase')},
            { 'value': '2', 'text': this.$t('system.priceLimitHomePageRetail')},
            { 'value': '3', 'text': this.$t('system.priceLimitHomePageSales')},
            { 'value': '4', 'text': this.$t('system.priceLimitBillPurchase')},
            { 'value': '5', 'text': this.$t('system.priceLimitBillRetail')},
            { 'value': '6', 'text': this.$t('system.priceLimitBillSales')},
            { 'value': '7', 'text': this.$t('system.priceLimitStockCost')}
          ],
          value: ''
        },
        confirmLoading: false,
        form: this.$form.createForm(this),
        validatorRules:{
          name:{
            rules: [
              { required: true, message: this.$t('system.roleNameRequired') },
              { min: 2, max: 30, message: this.$t('system.roleNameLength'), trigger: 'blur' },
              { validator: this.validateRoleName}
            ]
          },
          type:{
            rules: [
              { required: true, message: this.$t('system.dataTypeRequired') }
            ]
          },
          description:{
            rules: [
              { min: 0, max: 100, message: this.$t('system.descLength'), trigger: 'blur' }
            ]
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
        this.priceLimitList.value = this.model.priceLimit
        this.visible = true;
        this.$nextTick(() => {
          this.form.setFieldsValue(pick(this.model,'name', 'type', 'sort', 'description'))
          autoJumpNextInput('roleModal')
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
            formData.priceLimit = this.priceLimitList.value
            let obj;
            if(!this.model.id){
              obj=addRole(formData);
            }else{
              obj=editRole(formData);
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
      validateRoleName(rule, value, callback){
        let params = {
          name: value,
          id: this.model.id?this.model.id:0
        };
        checkRole(params).then((res)=>{
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
