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
      style="top:10%;height: 70%;">
      <template slot="footer">
        <a-button key="back" @click="handleCancel">
          取消
        </a-button>
        <a-button v-if="!isReadOnly" key="submit" type="primary" :loading="confirmLoading" @click="handleOk">
          保存
        </a-button>
      </template>
      <a-spin :spinning="confirmLoading">
        <a-form :form="form" id="depotModal">
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" label="仓库名称">
            <a-input :disabled="isReadOnly" placeholder="请输入仓库名称" v-decorator.trim="[ 'name', validatorRules.name]" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" label="仓库地址">
            <a-input :disabled="isReadOnly" placeholder="请输入仓库地址" v-decorator.trim="[ 'address', validatorRules.address ]" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" label="仓储费">
            <a-input-number :disabled="isReadOnly" :min="0" :precision="6" :step="0.01"
                            style="width: 100%" placeholder="请输入仓储费"
                            v-decorator="[ 'warehousing', validatorRules.warehousing ]" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" label="搬运费">
            <a-input-number :disabled="isReadOnly" :min="0" :precision="6" :step="0.01"
                            style="width: 100%" placeholder="请输入搬运费"
                            v-decorator="[ 'truckage', validatorRules.truckage ]" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" label="负责人">
            <a-select :disabled="isReadOnly" placeholder="选择负责人" v-decorator="[ 'principal' ]" :dropdownMatchSelectWidth="false">
              <a-select-option v-for="(item,index) in userList" :key="index" :value="item.id">
                {{ item.userName }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" label="排序">
            <a-input-number :disabled="isReadOnly" :min="0" :precision="0" style="width: 100%"
                            placeholder="请输入排序" v-decorator="[ 'sort', validatorRules.sort ]" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" label="备注">
            <a-textarea :disabled="isReadOnly" :rows="2" placeholder="请输入备注"
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
        title:"操作",
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
              { required: true, message: '请输入仓库名称!' },
              { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' },
              { validator: this.validateDepotName}
            ]},
          address: { rules: [{ max: 50, message: '仓库地址不能超过 50 个字符', trigger: 'blur' }] },
          warehousing: { rules: [{ type: 'number', min: 0, message: '仓储费必须为非负数', trigger: 'change' }] },
          truckage: { rules: [{ type: 'number', min: 0, message: '搬运费必须为非负数', trigger: 'change' }] },
          sort: { rules: [{ type: 'integer', min: 0, message: '排序必须为非负整数', trigger: 'change' }] },
          remark: { rules: [{ max: 100, message: '备注不能超过 100 个字符', trigger: 'blur' }] }
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
              callback("名称已经存在");
            }
          } else {
            callback((res.data && res.data.message) || res.data || '仓库名称校验失败');
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
