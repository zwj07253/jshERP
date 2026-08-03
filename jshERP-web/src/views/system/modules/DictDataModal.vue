<template>
  <div ref="container">
    <a-modal
      :title="title"
      :width="600"
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
      <a-spin :spinning="confirmLoading">
        <a-form :form="form">
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.dictTypeLabel')">
            <a-input :placeholder="$t('system.enterDictType')" v-decorator.trim="[ 'dictType' ]" :readOnly="true" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.dictLabel')">
            <a-input :placeholder="$t('system.enterDictLabel')" v-decorator.trim="[ 'dictLabel', validatorRules.dictLabel]" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.dictValue')">
            <a-input :placeholder="$t('system.enterDictValue')" v-decorator.trim="[ 'dictValue', validatorRules.dictValue]" :disabled="!!model.dictCode" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.dictSort')">
            <a-input-number style="width: 100%" :placeholder="$t('system.enterDictSort')" v-decorator.trim="[ 'dictSort', validatorRules.dictSort ]" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.cssClass')">
            <a-input :placeholder="$t('system.enterCssClass')" v-decorator.trim="[ 'cssClass' ]" />
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('system.listClass')">
            <a-select :placeholder="$t('system.selectListClass')" showSearch allow-clear optionFilterProp="children" v-decorator.trim="[ 'listClass' ]">
              <a-select-option v-for="(item,index) in listClassOptions" :key="index" :value="item.value">
                {{ item.label + '(' + item.value + ')' }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.status')">
            <a-select style="width:100%" :placeholder="$t('common.selectStatus')" v-decorator.trim="[ 'status' ]">
              <a-select-option v-for="dict in dict.type.sys_normal_disable" :key="dict.value" :value="dict.value">
                {{ dict.label }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.remark')">
            <a-textarea :rows="2" :placeholder="$t('common.enterRemark')" v-decorator.trim="[ 'remark' ]" />
          </a-form-item>
        </a-form>
      </a-spin>
    </a-modal>
  </div>
</template>
<script>
  import pick from 'lodash.pick'
  import { addDictData, editDictData } from '@/api/api'
  import { mixinDevice } from '@/utils/mixin'
  export default {
    name: "DictDataModal",
    dicts: ['sys_normal_disable'],
    mixins: [mixinDevice],
    data () {
      return {
        title:this.$t('common.action'),
        visible: false,
        model: {},
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
          dictLabel:{
            rules: [
              { required: true, message: this.$t('system.dictLabelRequired') }
            ]
          },
          dictValue:{
            rules: [
              { required: true, message: this.$t('system.dictValueRequired') }
            ]
          },
          dictSort:{
            rules: [
              { required: true, message: this.$t('system.dictSortRequired') }
            ]
          }
        },
        // 数据标签回显样式
        listClassOptions: [
          {
            value: "default",
            label: this.$t('system.styleDefault')
          },
          {
            value: "blue",
            label: this.$t('system.stylePrimary')
          },
          {
            value: "green",
            label: this.$t('system.styleSuccess')
          },
          {
            value: "grey",
            label: this.$t('system.styleInfo')
          },
          {
            value: "orange",
            label: this.$t('system.styleWarning')
          },
          {
            value: "red",
            label: this.$t('system.styleDanger')
          }
        ],
      }
    },
    created () {
    },
    methods: {
      add (dictType) {
        this.edit({});
        this.model.dictType = dictType
        this.model.dictSort = 0
        this.model.listClass = 'default'
        this.model.status = '0'
        this.$nextTick(() => {
          this.form.setFieldsValue(pick(this.model, 'dictType', 'dictSort', 'listClass', 'status'))
        })
      },
      edit (record) {
        this.form.resetFields();
        this.model = Object.assign({}, record);
        this.visible = true;
        this.$nextTick(() => {
          this.form.setFieldsValue(pick(this.model, 'dictType', 'dictLabel', 'dictValue', 'cssClass', 'dictSort', 'listClass', 'status', 'remark'))
        })
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
            if(!this.model.dictCode){
              obj=addDictData(formData)
            }else{
              obj=editDictData(formData)
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
      }
    }
  }
</script>
<style scoped>
</style>