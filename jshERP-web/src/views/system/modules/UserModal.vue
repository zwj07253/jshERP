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
      style="top:2%;height:95%;">
      <template slot="footer">
        <a-button key="back" v-if="isReadOnly" @click="handleCancel">
          {{ $t('common.cancel') }}
        </a-button>
      </template>
      <a-spin :spinning="confirmLoading">
        <a-form :form="form" id="userModal">
          <a-form-item :label="$t('system.loginName')" :labelCol="labelCol" :wrapperCol="wrapperCol">
            <a-input :placeholder="$t('system.enterLoginName')" v-decorator.trim="[ 'loginName', validatorRules.loginName]" :disabled="isReadOnly || !!model.id" />
          </a-form-item>
          <a-form-item :label="$t('system.userPassword')" :labelCol="labelCol" :wrapperCol="wrapperCol" v-if="!model.id">
            <a-input-password :placeholder="$t('system.enterUserPassword')" v-decorator.trim="[ 'password', validatorRules.password]" :disabled="isReadOnly" />
          </a-form-item>
          <a-form-item :label="$t('system.userName')" :labelCol="labelCol" :wrapperCol="wrapperCol" >
            <a-input :placeholder="$t('system.enterUserName')" v-decorator.trim="[ 'username', validatorRules.username]" :disabled="isReadOnly" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.role')">
            <a-select v-if="!model.id||model.id!==model.tenantId" :placeholder="$t('system.selectRole')" v-decorator="[ 'roleId', validatorRules.roleId]" :dropdownMatchSelectWidth="false" :disabled="isReadOnly">
              <a-select-option v-for="(item,index) in roleList" :key="index" :value="item.id">
                {{ item.name }}
              </a-select-option>
            </a-select>
            <a-col v-if="model.id===model.tenantId"><a-row>{{ tenantRoleName }}</a-row></a-col>
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.departmentLabel')">
            <a-tree-select style="width:100%" :dropdownStyle="{maxHeight:'200px',overflow:'auto'}" allow-clear
               :treeData="orgaTree" v-decorator="[ 'orgaId' ]" :placeholder="$t('system.selectDepartment')" :disabled="isReadOnly">
            </a-tree-select>
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.position')">
            <a-input :placeholder="$t('system.enterPosition')" v-decorator.trim="[ 'position' ]" :disabled="isReadOnly" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.isManagerLabel')">
            <a-select :placeholder="$t('system.selectIsManager')" v-decorator="[ 'leaderFlag' ]" :disabled="isReadOnly">
              <a-select-option value="1">{{ $t('common.yes') }}</a-select-option>
              <a-select-option value="0">{{ $t('common.no') }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.phoneNumLabel')">
            <a-input :placeholder="$t('system.enterPhoneNum')" v-decorator.trim="[ 'phonenum' ]" :disabled="isReadOnly" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.email')">
            <a-input :placeholder="$t('system.enterEmail')" v-decorator.trim="[ 'email' ]" :disabled="isReadOnly" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.sort')">
            <a-input :placeholder="$t('common.sort')" v-decorator.trim="[ 'userBlngOrgaDsplSeq' ]" :disabled="isReadOnly" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.remark')">
            <a-textarea :rows="2" :placeholder="$t('common.enterRemark')" v-decorator="[ 'description' ]" :disabled="isReadOnly" />
          </a-form-item>
        </a-form>
      </a-spin>
    </a-modal>
  </div>
</template>
<script>
  import pick from 'lodash.pick'
  import Vue from 'vue'
  import md5 from 'md5'
  import JSelectPosition from '@/components/jeecgbiz/JSelectPosition'
  import { ACCESS_TOKEN } from "@/store/mutation-types"
  import {addUser,editUser,queryOrganizationTreeList,roleAllList} from '@/api/api'
  import {autoJumpNextInput} from "@/utils/util"
  import {mixinDevice} from '@/utils/mixin'
  import JImageUpload from '../../../components/jeecg/JImageUpload'
  export default {
    name: "UserModal",
    mixins: [mixinDevice],
    components: {
      JImageUpload,
      JSelectPosition
    },
    data () {
      return {
        title:this.$t('common.action'),
        visible: false,
        modalWidth:800,
        drawerWidth:700,
        orgaTree: [],
        roleList: [],
        userId:"", //保存用户id
        tenantRoleName: '', //租户的角色名称
        isReadOnly: false,
        disableSubmit:false,
        dateFormat:"YYYY-MM-DD",
        validatorRules:{
          loginName:{
            rules: [{
              required: true, message: this.$t('system.loginNameRequired')
            }]
          },
          password: {
            rules: [
              { required: true, message: this.$t('system.passwordRequired') },
              { pattern: /^(?=.*[a-z])(?=.*\d).{6,}$/, message: this.$t('login.passwordFormatError') }
            ]
          },
          username:{
            rules: [{
              required: true, message: this.$t('system.userNameRequired')
            }]
          },
          roleId:{
            rules: [{
              required: true, message: this.$t('system.roleRequired')
            }]
          }
        },
        model: {},
        labelCol: {
          xs: { span: 24 },
          sm: { span: 5 },
        },
        wrapperCol: {
          xs: { span: 24 },
          sm: { span: 16 },
        },
        uploadLoading:false,
        confirmLoading: false,
        headers:{},
        form:this.$form.createForm(this)
      }
    },
    created () {
      const token = Vue.ls.get(ACCESS_TOKEN);
      this.headers = {"X-Access-Token":token}
    },
    methods: {
      add () {
        this.edit({});
      },
      edit (record) {
        this.loadOrgaData()
        this.loadRoleData()
        this.form.resetFields();
        this.userId = record.id;
        this.visible = true;
        this.model = Object.assign({}, record);
        this.$nextTick(() => {
          this.form.setFieldsValue(pick(this.model,'loginName','username','roleId','orgaId','position','leaderFlag',
            'phonenum','email','userBlngOrgaDsplSeq','description'))
          this.tenantRoleName = this.model.roleName
          autoJumpNextInput('userModal')
        });
      },
      close() {
        this.$emit('close');
        this.visible = false;
        this.disableSubmit = false;
      },
      handleOk() {
        const that = this;
        // 触发表单验证
        this.form.validateFields((err, values) => {
          if (!err) {
            that.confirmLoading = true;
            let formData = Object.assign(this.model, values);
            let obj;
            if(!this.model.id){
              formData.id = this.userId;
              formData.password = md5(values.password);
              obj=addUser(formData);
            }else{
              obj=editUser(formData);
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
      handleCancel() {
        this.close()
      },
      loadOrgaData(){
        let that = this;
        let params = {};
        params.id='';
        queryOrganizationTreeList(params).then((res)=>{
          if(res){
            that.orgaTree = res
          }
        })
      },
      loadRoleData(){
        roleAllList({}).then((res)=>{
          if(res){
            this.roleList = res
          }
        })
      }
    }
  }
</script>

<style scoped>

</style>
