<template>
  <div ref="container">
    <a-modal
      :title="title"
      :width="500"
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
      style="top:30%;height: 30%;">
      <template slot="footer">
        <a-button key="back" v-if="isReadOnly" @click="handleCancel">
          {{ $t('common.cancel') }}
        </a-button>
      </template>
      <a-spin :spinning="confirmLoading">
        <a-form :form="form">
          <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('material.enterPrice')">
            <a-input :placeholder="$t('material.enterPrice')" v-decorator.trim="[ 'price', validatorRules.price]" />
          </a-form-item>
        </a-form>
      </a-spin>
    </a-modal>
  </div>
</template>

<script>
  import {mixinDevice} from '@/utils/mixin'
  export default {
    name: 'BatchSetPriceModal',
    mixins: [mixinDevice],
    data () {
      return {
        title:this.$t('common.batchSetInfo'),
        visible: false,
        isReadOnly: false,
        batchType: '',
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
          price:{
            rules: [
              { required: true, message: this.$t('material.enterPriceRequired') }
            ]}
        }
      }
    },
    created () {
    },
    methods: {
      add (type) {
        this.batchType = type
        if(type === 'purchase') {
          this.title = this.$t('material.purchasePriceBatchSet')
        } else if(type === 'commodity') {
          this.title = this.$t('material.retailPriceBatchSet')
        } else if(type === 'wholesale') {
          this.title = this.$t('material.salesPriceBatchSet')
        } else if(type === 'low') {
          this.title = this.$t('material.lowestPriceBatchSet')
        }
        this.edit({});
      },
      edit (record) {
        this.form.resetFields();
        this.model = Object.assign({}, record);
        this.visible = true;
      },
      close () {
        this.$emit('close');
        this.visible = false;
      },
      handleOk () {
        let price = this.form.getFieldValue('price')
        this.$emit('ok', price, this.batchType);
        this.visible = false
      },
      handleCancel () {
        this.close()
      }
    }
  }
</script>

<style scoped>

</style>