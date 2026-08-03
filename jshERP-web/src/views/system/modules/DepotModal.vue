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
      style="top:10%;height: 70%;">
      <template slot="footer">
        <a-button key="back" @click="handleCancel">
          {{ $t('common.cancel') }}
        </a-button>
        <a-button v-if="!isReadOnly" key="submit" type="primary" :loading="confirmLoading" @click="handleOk">
          {{ $t('common.save') }}
        </a-button>
      </template>
      <a-spin :spinning="confirmLoading">
        <a-form :form="form" id="depotModal">
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.depotName')">
            <a-input :disabled="isReadOnly" :placeholder="$t('system.enterDepotName')" v-decorator.trim="[ 'name', validatorRules.name]" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.depotAddressLabel')">
            <a-input :disabled="isReadOnly" :placeholder="$t('system.enterDepotAddress')" v-decorator.trim="[ 'address', validatorRules.address ]" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.warehousingFeeLabel')">
            <a-input-number :disabled="isReadOnly" :min="0" :precision="6" :step="0.01"
                            style="width: 100%" :placeholder="$t('system.enterWarehousingFee')"
                            v-decorator="[ 'warehousing', validatorRules.warehousing ]" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.truckageFeeLabel')">
            <a-input-number :disabled="isReadOnly" :min="0" :precision="6" :step="0.01"
                            style="width: 100%" :placeholder="$t('system.enterTruckageFee')"
                            v-decorator="[ 'truckage', validatorRules.truckage ]" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.principalLabel')">
            <a-select :disabled="isReadOnly" :placeholder="$t('system.selectPrincipal')" v-decorator="[ 'principal' ]" :dropdownMatchSelectWidth="false">
              <a-select-option v-for="(item,index) in userList" :key="index" :value="item.id">
                {{ item.userName }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.sort')">
            <a-input-number :disabled="isReadOnly" :min="0" :precision="0" style="width: 100%"
                            :placeholder="$t('common.sort')" v-decorator="[ 'sort', validatorRules.sort ]" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.remark')">
            <a-textarea :disabled="isReadOnly" :rows="2" :placeholder="$t('common.enterRemark')"
                        v-decorator.trim="[ 'remark', validatorRules.remark ]" />
          </a-form-item>
        </a-form>
      </a-spin>
    </a-modal>
  </div>
</template>
<script>
  import pick from 'lodash.pick'
  import {addDepot,editDepot,checkDepot,getUserList } from '@/api/api'
  import {autoJumpNextInput} from "@/utils/util"
  import {mixinDevice} from '@/utils/mixin'
  export default {
    name: "DepotModal",
    mixins: [mixinDevice],
    data () {
      return {
        title:this.$t('common.action'),
        visible: false,
        model: {},
        maskStyle: '',
        userList: [],
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
              { required: true, message: this.$t('system.depotNameRequired') },
              { min: 2, max: 20, message: this.$t('system.depotNameLength'), trigger: 'blur' },
              { validator: this.validateDepotName}
            ]},
          address: { rules: [{ max: 50, message: this.$t('system.addressLength'), trigger: 'blur' }] },
          warehousing: { rules: [{ type: 'number', min: 0, message: this.$t('system.warehousingNonNegative'), trigger: 'change' }] },
          truckage: { rules: [{ type: 'number', min: 0, message: this.$t('system.truckageNonNegative'), trigger: 'change' }] },
          sort: { rules: [{ type: 'integer', min: 0, message: this.$t('system.sortNonNegativeInt'), trigger: 'change' }] },
          remark: { rules: [{ max: 100, message: this.$t('system.remarkLength100'), trigger: 'blur' }] }
        },
      }
    },
    created () {
      this.initUser()
    },
    methods: {
      add () {
        this.isReadOnly = false
        this.edit({});
      },
      edit (record) {
        this.form.resetFields();
        this.model = Object.assign({}, record);
        this.visible = true;
        this.$nextTick(() => {
          this.form.setFieldsValue(pick(this.model,
            'name', 'address', 'warehousing', 'truckage', 'principal', 'sort', 'remark'))
          autoJumpNextInput('depotModal')
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
              obj=addDepot(formData);
            }else{
              obj=editDepot(formData);
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
      validateDepotName(rule, value, callback){
        let params = {
          name: value,
          id: this.model.id?this.model.id:0
        };
        checkDepot(params).then((res)=>{
          if(res && res.code===200) {
            if(!res.data.status){
              callback();
            } else {
              callback(this.$t('common.nameExists'));
            }
          } else {
            callback((res.data && res.data.message) || res.data || this.$t('system.depotNameCheckFailed'));
          }
        });
      },
      initUser() {
        getUserList({}).then((res)=>{
          if(res) {
            this.userList = res;
          }
        });
      }
    }
  }
</script>
<style scoped>

</style>
