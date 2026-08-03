<template>
  <div ref="container">
    <a-modal
      :title="title"
      :width="1000"
      :visible="visible"
      :confirm-loading="confirmLoading"
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
      <a-spin :spinning="confirmLoading">
        <a-form :form="form">
          <a-row class="form-row" :gutter="24">
            <a-col :md="8" :sm="24">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('material.color')">
                <a-input :placeholder="$t('material.enterColor')" v-decorator.trim="[ 'color' ]" />
              </a-form-item>
            </a-col>
            <a-col :md="8" :sm="24">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.brand')">
                <a-input :placeholder="$t('common.brand')" v-decorator.trim="[ 'brand' ]" />
              </a-form-item>
            </a-col>
            <a-col :md="8" :sm="24">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('material.weight')">
                <a-input-number style="width: 100%" :placeholder="$t('material.enterWeight')" v-decorator.trim="[ 'weight' ]" />
              </a-form-item>
            </a-col>
            <a-col :md="8" :sm="24">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('material.expiryNum')">
                <a-input-number style="width: 100%" :placeholder="$t('material.enterExpiry')" v-decorator.trim="[ 'expiryNum' ]" />
              </a-form-item>
            </a-col>
            <a-col :md="8" :sm="24">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.category')">
                <a-tree-select style="width:100%" :dropdownStyle="{maxHeight:'200px',overflow:'auto'}" allow-clear
                               :treeData="categoryTree" v-decorator="[ 'categoryId' ]" :placeholder="$t('financial.selectCategory')">
                </a-tree-select>
              </a-form-item>
            </a-col>
            <a-col :md="8" :sm="24">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.position')">
                <a-input :placeholder="$t('common.position')" v-decorator.trim="[ 'position' ]" />
              </a-form-item>
            </a-col>
            <a-col :md="8" :sm="24">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('material.manufacturer')">
                <a-input :placeholder="$t('material.manufacturer')" v-decorator.trim="[ 'mfrs' ]" />
              </a-form-item>
            </a-col>

            <a-col :md="8" :sm="24">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.columns.serialNumber')">
                <a-select :placeholder="$t('material.hasOrNotSN')" v-decorator="[ 'enableSerialNumber' ]">
                  <a-select-option value="1">{{ $t('common.yes') }}</a-select-option>
                  <a-select-option value="0">{{ $t('common.no') }}</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :md="8" :sm="24">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.columns.batchNumber')">
                <a-select :placeholder="$t('material.hasOrNotBatch')" v-decorator="[ 'enableBatchNumber' ]">
                  <a-select-option value="1">{{ $t('common.yes') }}</a-select-option>
                  <a-select-option value="0">{{ $t('common.no') }}</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :md="8" :sm="24">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.remark')">
                <a-textarea :rows="1" :placeholder="$t('common.enterRemark')" v-decorator="[ 'remark' ]" style="margin-top:8px;"/>
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>
      </a-spin>
    </a-modal>
  </div>
</template>

<script>
  import {queryMaterialCategoryTreeList, batchUpdateMaterial} from '@/api/api'
  import {mixinDevice} from '@/utils/mixin'
  export default {
    name: 'BatchSetInfoModal',
    mixins: [mixinDevice],
    data () {
      return {
        title: this.$t('material.batchEdit'),
        visible: false,
        categoryTree: [],
        materialIds: '',
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
      }
    },
    created () {
    },
    methods: {
      loadTreeData(){
        let that = this
        let params = {}
        params.id=''
        queryMaterialCategoryTreeList(params).then((res)=>{
          if(res){
            that.categoryTree = [];
            for (let i = 0; i < res.length; i++) {
              let temp = res[i];
              that.categoryTree.push(temp)
            }
          }
        })
      },
      edit (ids) {
        this.materialIds = ids
        this.form.resetFields()
        this.model = Object.assign({}, '')
        this.loadTreeData()
        this.visible = true
      },
      close () {
        this.$emit('close');
        this.visible = false;
      },
      handleOk () {
        const that = this
        // 触发表单验证
        this.form.validateFields((err, values) => {
          if (!err) {
            let formData = Object.assign(this.model, values)
            if(JSON.stringify(formData) === '{}') {
              that.$message.warning(this.$t('material.enterBatchContent'))
              return
            }
            if(formData.enableSerialNumber === '1' && formData.enableBatchNumber === '1' ) {
              that.$message.warning(this.$t('material.snAndBatchOnlyOne'))
              return
            }
            let idList = that.materialIds?that.materialIds.split(','):[]
            that.$confirm({
              title: this.$t('common.confirmAction'),
              content: this.$t('material.confirmOperateCount', {count: idList.length}),
              onOk: function () {
                that.confirmLoading = true
                let paramObj = {
                  ids: that.materialIds,
                  material: JSON.stringify(formData)
                }
                batchUpdateMaterial(paramObj).then((res)=>{
                  if(res.code === 200){
                    that.$emit('ok')
                  }else{
                    that.$message.warning(res.data.message)
                  }
                }).finally(() => {
                  that.confirmLoading = false
                  that.close()
                })
              }
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