<template>
  <div ref="container">
    <a-modal
      :title="title"
      :width="800"
      :ok=false
      :visible="visible"
      :confirmLoading="confirmLoading"
      :getContainer="() => $refs.container"
      :maskStyle="{'top':'93px','left':'154px'}"
      :wrapClassName="wrapClassNameInfo()"
      :mask="isDesktop()"
      :maskClosable="false"
      :okButtonProps="{ props: {disabled: disableSubmit} }"
      @ok="handleOk"
      @cancel="handleCancel"
      :cancelText="$t('common.cancel')"
      :okText="$t('common.save')"
      style="top:50px;height: 80%;">
      <a-spin :spinning="confirmLoading">
        <a-form :form="form" id="organizationModal">
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.name')">
            <a-input :placeholder="$t('common.enterName')" v-decorator="['orgAbr', validatorRules.orgAbr ]"/>
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.serialNo')">
            <a-input :placeholder="$t('common.enterNumber')" v-decorator="['orgNo', validatorRules.orgNo ]"/>
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.parentDept')">
            <a-tree-select style="width:100%" :dropdownStyle="{maxHeight:'200px',overflow:'auto'}"
                           allow-clear :treeDefaultExpandAll="true"
                 :treeData="departTree" v-decorator="[ 'parentId' ]" :placeholder="$t('system.selectParentDept')">
            </a-tree-select>
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.sort')">
            <a-input v-decorator="[ 'sort' ]"/>
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.remark')">
            <a-textarea :placeholder="$t('common.enterRemark')":rows="2" v-decorator.trim="[ 'remark' ]" />
          </a-form-item>
        </a-form>
      </a-spin>
    </a-modal>
  </div>
</template>

<script>
  import { httpAction } from '@/api/manage'
  import { queryOrganizationTreeList, checkOrganization } from '@/api/api'
  import {autoJumpNextInput} from "@/utils/util"
  import {mixinDevice} from '@/utils/mixin'
  import pick from 'lodash.pick'
  import ATextarea from 'ant-design-vue/es/input/TextArea'
  export default {
    name: "OrganizationModal",
    mixins: [mixinDevice],
    components: { ATextarea },
    data () {
      return {
        departTree:[],
        orgTypeData:[],
        phoneWarning:'',
        departName:"",
        title:this.$t('common.action'),
        visible: false,
        disableSubmit:false,
        model: {},
        menuhidden:false,
        menuusing:true,
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
          orgAbr: {
            rules: [
              { required: true, message: this.$t('common.enterName')},
              { validator: this.validateName}
            ]
          },
          orgNo: {rules: [{required: true, message: this.$t('system.codeRequired')}]}
        },
        url: {
          add: "/organization/add",
        }
      }
    },
    created () {
    },
    methods: {
      loadTreeData(){
        var that = this;
        let params = {};
        params.id='';
        queryOrganizationTreeList(params).then((res)=>{
          if(res){
            that.departTree = [];
            for (let i = 0; i < res.length; i++) {
              let temp = res[i];
              that.departTree.push(temp);
            }
          }
        })
      },
      add () {
        this.edit();
      },
      edit (record) {
        this.form.resetFields();
        this.model = Object.assign({}, {});
        this.visible = true;
        this.loadTreeData();
        this.$nextTick(() => {
          this.form.setFieldsValue(pick(record, 'orgAbr', 'orgNo', 'parentId', 'sort', 'remark'))
          autoJumpNextInput('organizationModal')
        });
      },
      close () {
        this.$emit('close');
        this.disableSubmit = false;
        this.visible = false;
      },
      handleOk () {
        const that = this;
        // 触发表单验证
        this.form.validateFields((err, values) => {
          if (!err) {
            that.confirmLoading = true;
            let formData = Object.assign(this.model, values);
            //时间格式化
            console.log(formData)
            httpAction(this.url.add,formData,"post").then((res)=>{
              if(res.code == 200){
                that.$message.success(res.data.message);
                that.loadTreeData();
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
      validateName(rule, value, callback){
        let params = {
          name: value,
          id: this.model.id?this.model.id:0
        };
        checkOrganization(params).then((res)=>{
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
  @import '~@assets/less/common.less'
</style>
