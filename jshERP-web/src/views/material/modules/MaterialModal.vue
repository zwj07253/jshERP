<template>
  <j-modal
    :title="title"
    :width="width"
    :visible="visible"
    :confirmLoading="confirmLoading"
    v-bind:prefixNo="prefixNo"
    fullscreen
    switchHelp
    switchFullscreen
    @cancel="handleCancel"
    :id="prefixNo"
    :style="modalStyle">
    <template slot="footer">
      <a-button key="back" @click="handleCancel">{{ $t('common.cancel') }}</a-button>
      <a-button type="primary" v-if="showOkFlag" :loading="confirmLoading" @click="handleOk">{{ $t('common.save') }}（Ctrl+S）</a-button>
    </template>
    <a-spin :spinning="confirmLoading">
      <a-form :form="form">
        <a-tabs v-model:activeKey="activeKey" size="small">
          <a-tab-pane key="1" :tab="$t('common.basicInfo')" id="materialHeadModal" forceRender>
            <a-row class="form-row" :gutter="24">
              <a-col :md="6" :sm="24">
                <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.name')" data-step="1" :data-title="$t('common.name')" :data-intro="$t('material.nameIntro')">
                  <a-input :placeholder="$t('common.enterName')" v-decorator.trim="[ 'name', validatorRules.name ]" @change="handleNameChange" />
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.specification')" data-step="2" :data-title="$t('common.specification')" :data-intro="$t('material.specIntro')">
                  <a-input :placeholder="$t('common.specification')" v-decorator.trim="[ 'standard', validatorRules.standard ]"/>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.model')" data-step="3" :data-title="$t('common.model')" :data-intro="$t('material.modelIntro')">
                  <a-input :placeholder="$t('common.model')" v-decorator.trim="[ 'model', validatorRules.model ]" />
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.unit')"
                  data-step="4" :data-title="$t('common.unit')" :data-intro="$t('material.unitIntro')">
                  <a-row class="form-row" :gutter="24">
                    <a-col :lg="15" :md="15" :sm="24" style="padding:0px 0px 0px 12px;">
                      <a-input :placeholder="$t('common.unit')" v-if="!unitChecked" v-decorator.trim="[ 'unit', validatorRules.unit ]" @change="onlyUnitOnChange" />
                      <a-select :value="unitList" :placeholder="$t('material.selectMultiUnit')" v-decorator="[ 'unitId', validatorRules.unitId ]" @change="manyUnitOnChange"
                        showSearch optionFilterProp="children" v-if="unitChecked" :dropdownMatchSelectWidth="false">
                        <div slot="dropdownRender" slot-scope="menu">
                          <v-nodes :vnodes="menu" />
                          <a-divider style="margin: 4px 0;" />
                          <div style="padding: 4px 8px; cursor: pointer;"
                               @mousedown="e => e.preventDefault()" @click="addUnit"><a-icon type="plus" /> {{ $t('common.add') }}</div>
                        </div>
                        <a-select-option v-for="(item,index) in unitList"
                          :key="index" :value="item.id">
                          {{ item.name }}
                        </a-select-option>
                      </a-select>
                    </a-col>
                    <a-col :lg="9" :md="9" :sm="24" style="padding:0px; text-align:center">
                      <a-checkbox :checked="unitChecked" @change="unitOnChange">{{ $t('common.unit') }}</a-checkbox>
                    </a-col>
                  </a-row>
                </a-form-item>
              </a-col>
            </a-row>
            <a-row class="form-row" :gutter="24">
              <a-col :md="6" :sm="24">
                <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('material.color')" data-step="5" :data-title="$t('material.color')"
                             :data-intro="$t('material.colorIntro')">
                  <a-input :placeholder="$t('material.color')" v-decorator.trim="[ 'color' ]" />
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.brand')" data-step="6" :data-title="$t('common.brand')"
                             :data-intro="$t('material.brandIntro')">
                  <a-input :placeholder="$t('common.brand')" v-decorator.trim="[ 'brand' ]" />
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('material.mnemonic')" data-step="7" :data-title="$t('material.mnemonic')"
                             :data-intro="$t('material.mnemonicIntro')">
                  <a-input placeholder="" v-decorator.trim="[ 'mnemonic' ]" :readOnly="true" />
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.category')"
                             data-step="8" :data-title="$t('common.category')" :data-intro="$t('material.categoryIntro')">
                  <a-tree-select style="width:100%" :dropdownStyle="{maxHeight:'200px',overflow:'auto'}" allow-clear
                                 :treeData="categoryTree" v-decorator="[ 'categoryId' ]" :placeholder="$t('material.selectCategory')">
                  </a-tree-select>
                </a-form-item>
              </a-col>
            </a-row>
            <a-row class="form-row" :gutter="24">
              <a-col :md="6" :sm="24">
                <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('material.weight')" data-step="9" :data-title="$t('material.weight')"
                  :data-intro="$t('material.weightIntro')">
                  <a-input-number style="width: 100%" :placeholder="$t('material.enterWeight')" v-decorator.trim="[ 'weight' ]" />
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('material.expiryNum')" data-step="10" :data-title="$t('material.expiryNum')"
                  :data-intro="$t('material.expiryNumIntro')">
                  <a-input-number style="width: 100%" :placeholder="$t('material.enterExpiry')" v-decorator.trim="[ 'expiryNum' ]" />
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.position')" data-step="11" :data-title="$t('common.position')"
                             :data-intro="$t('material.positionIntro')">
                  <a-input style="width: 100%" :placeholder="$t('common.position')" v-decorator.trim="[ 'position' ]" />
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('material.manufacturer')" data-step="12" :data-title="$t('material.manufacturer')"
                             :data-intro="$t('material.manufacturerIntro')">
                  <a-input :placeholder="$t('material.manufacturer')" v-decorator.trim="[ 'mfrs' ]" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row class="form-row" :gutter="24">
              <a-col :lg="6" :md="6" :sm="6">
                <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="mpShort.otherField1.name">
                  <a-input :placeholder="$t('material.enterFieldValue', {field: mpShort.otherField1.name})" v-decorator.trim="[ 'otherField1' ]" />
                </a-form-item>
              </a-col>
              <a-col :lg="6" :md="6" :sm="6">
                <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="mpShort.otherField2.name">
                  <a-input :placeholder="$t('material.enterFieldValue', {field: mpShort.otherField2.name})" v-decorator.trim="[ 'otherField2' ]" />
                </a-form-item>
              </a-col>
              <a-col :lg="6" :md="6" :sm="6">
                <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="mpShort.otherField3.name">
                  <a-input :placeholder="$t('material.enterFieldValue', {field: mpShort.otherField3.name})" v-decorator.trim="[ 'otherField3' ]" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row class="form-row" :gutter="24">
              <a-col :md="6" :sm="24">
                <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.columns.serialNumber')" data-step="13" :data-title="$t('purchase.form.columns.serialNumber')"
                  :data-intro="$t('material.snIntro')">
                  <a-tooltip :title="$t('material.snTooltip')">
                    <a-select :placeholder="$t('material.hasOrNotSN')" v-decorator="[ 'enableSerialNumber' ]">
                      <a-select-option value="1">{{ $t('common.yes') }}</a-select-option>
                      <a-select-option value="0">{{ $t('common.no') }}</a-select-option>
                    </a-select>
                  </a-tooltip>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.columns.batchNumber')" data-step="14" :data-title="$t('purchase.form.columns.batchNumber')"
                  :data-intro="$t('material.batchNumIntro')">
                  <a-tooltip :title="$t('material.batchNumTooltip')">
                    <a-select :placeholder="$t('material.hasOrNotBatch')" v-decorator="[ 'enableBatchNumber' ]">
                      <a-select-option value="1">{{ $t('common.yes') }}</a-select-option>
                      <a-select-option value="0">{{ $t('common.no') }}</a-select-option>
                    </a-select>
                  </a-tooltip>
                </a-form-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.property')" data-step="15" :data-title="$t('purchase.form.columns.sku')"
                  :data-intro="$t('material.skuIntro')">
                  <a-tooltip :title="$t('material.skuTooltip')">
                    <a-select mode="multiple" v-decorator="[ 'manySku' ]" showSearch optionFilterProp="children"
                      :placeholder="$t('common.property')" @change="onManySkuChange" :disabled="attributeStatus">
                      <div slot="dropdownRender" slot-scope="menu">
                        <v-nodes :vnodes="menu" />
                        <a-divider style="margin: 4px 0;" />
                        <div style="padding: 4px 8px; cursor: pointer;"
                             @mousedown="e => e.preventDefault()" @click="initMaterialAttribute">{{ $t('common.refreshList') }} <a-icon type="reload" /></div>
                      </div>
                      <a-select-option v-for="(item,index) in materialAttributeList" :key="index" :value="item.value" :disabled="item.disabled">
                        {{ item.name }}
                      </a-select-option>
                    </a-select>
                  </a-tooltip>
                </a-form-item>
              </a-col>
            </a-row>
            <a-row class="form-row" :gutter="24">
              <a-col :md="12" :sm="24" v-if="manySkuSelected>=1">
                <a-form-item :labelCol="{xs: { span: 24 },sm: { span: 4 }}" :wrapperCol="{xs: { span: 24 },sm: { span: 20 }}" :label="skuOneTitle">
                  <a-select mode="multiple" v-decorator="[ 'skuOne' ]" showSearch optionFilterProp="children"
                            :placeholder="$t('common.property')" @select="onSkuChange" @deselect="onSkuOneDeSelect">
                    <a-select-option v-for="(item,index) in skuOneList" :key="index" :value="item.value">
                      {{ item.name }}
                    </a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :md="12" :sm="24" v-if="manySkuSelected>=2">
                <a-form-item :labelCol="{xs: { span: 24 },sm: { span: 4 }}" :wrapperCol="{xs: { span: 24 },sm: { span: 20 }}" :label="skuTwoTitle">
                  <a-select mode="multiple" v-decorator="[ 'skuTwo' ]" showSearch optionFilterProp="children"
                            :placeholder="$t('common.property')" @select="onSkuChange" @deselect="onSkuTwoDeSelect">
                    <a-select-option v-for="(item,index) in skuTwoList" :key="index" :value="item.value">
                      {{ item.name }}
                    </a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :md="12" :sm="24" v-if="manySkuSelected>=3">
                <a-form-item :labelCol="{xs: { span: 24 },sm: { span: 4 }}" :wrapperCol="{xs: { span: 24 },sm: { span: 20 }}" :label="skuThreeTitle">
                  <a-select mode="multiple" v-decorator="[ 'skuThree' ]" showSearch optionFilterProp="children"
                            :placeholder="$t('common.property')" @select="onSkuChange" @deselect="onSkuThreeDeSelect">
                    <a-select-option v-for="(item,index) in skuThreeList" :key="index" :value="item.value">
                      {{ item.name }}
                    </a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
            </a-row>
            <div style="margin-top:8px;" id="materialDetailModal">
              <j-editable-table
                ref="editableMeTable"
                :loading="meTable.loading"
                :columns="meTable.columns"
                :dataSource="meTable.dataSource"
                :height="300"
                :minWidth="1000"
                :maxHeight="300"
                :rowNumber="true"
                :rowSelection="true"
                :actionButton="true"
                @valueChange="onValueChange"
                @added="onAdded"
                @deleted="onDeleted">
                <template #buttonAfter>
                  <a-button @click="batchSetPrice('purchase')">{{ $t('material.batchSetPrice') }}</a-button>
                  <a-button style="margin-left: 8px" @click="batchSetPrice('commodity')">{{ $t('material.batchSetPrice') }}</a-button>
                  <a-button style="margin-left: 8px" @click="batchSetPrice('wholesale')">{{ $t('material.batchSetPrice') }}</a-button>
                  <a-button style="margin-left: 8px" @click="batchSetPrice('low')">{{ $t('material.batchSetPrice') }}</a-button>
                </template>
              </j-editable-table>
              <!-- 表单区域 -->
              <batch-set-price-modal ref="priceModalForm" @ok="batchSetPriceModalFormOk"></batch-set-price-modal>
            </div>
            <a-row class="form-row" :gutter="24">
              <a-col :lg="24" :md="24" :sm="24">
                <a-form-item :labelCol="labelCol" :wrapperCol="{xs: { span: 24 },sm: { span: 24 }}" label="">
                  <a-textarea :rows="1" :placeholder="$t('common.enterRemark')" v-decorator="[ 'remark' ]" style="margin-top:8px;"/>
                </a-form-item>
              </a-col>
            </a-row>
          </a-tab-pane>
          <a-tab-pane key="2" :tab="$t('common.quantity')" forceRender>
            <j-editable-table
              ref="editableDepotTable"
              :loading="depotTable.loading"
              :columns="depotTable.columns"
              :dataSource="depotTable.dataSource"
              :minWidth="1000"
              :maxHeight="300"
              :rowNumber="true"
              :rowSelection="false"
              :actionButton="false">
              <template #buttonAfter>
                <a-button style="margin: 0px 0px 8px 0px" @click="batchSetStock('initStock')">{{ $t('material.batchSetStock') }}</a-button>
                <a-button style="margin-left: 8px" @click="batchSetStock('lowSafeStock')">{{ $t('material.batchSetStock') }}</a-button>
                <a-button style="margin-left: 8px" @click="batchSetStock('highSafeStock')">{{ $t('material.batchSetStock') }}</a-button>
              </template>
            </j-editable-table>
            <!-- 表单区域 -->
            <batch-set-stock-modal ref="stockModalForm" @ok="batchSetStockModalFormOk"></batch-set-stock-modal>
          </a-tab-pane>
          <a-tab-pane key="3" :tab="$t('common.uploadImage')" forceRender>
            <a-row class="form-row" :gutter="24" style="padding-top:20px">
              <a-col :lg="18" :md="18" :sm="24">
                <a-form-item :labelCol="{xs: { span: 24 },sm: { span: 3 }}" :wrapperCol="{xs: { span: 24 },sm: { span: 20 }}" :label="$t('common.uploadImage')">
                  <j-image-upload v-model="fileList" bizPath="material" :text="$t('common.clickToUpload')" isMultiple></j-image-upload>
                </a-form-item>
              </a-col>
              <a-col :lg="6" :md="6" :sm="24"></a-col>
            </a-row>
            <a-row class="form-row" :gutter="24">
              <a-col :lg="18" :md="18" :sm="24">
                <a-form-item :labelCol="{xs: { span: 24 },sm: { span: 3 }}" :wrapperCol="{xs: { span: 24 },sm: { span: 20 }}" :label="$t('common.uploadImage')">
                  {{ $t('common.fileSizeExceeds', {size: 1}) }}
                </a-form-item>
              </a-col>
              <a-col :lg="6" :md="6" :sm="24"></a-col>
            </a-row>
          </a-tab-pane>
        </a-tabs>
      </a-form>
    </a-spin>
    <unit-modal ref="unitModalForm" @ok="unitModalFormOk"></unit-modal>
  </j-modal>
</template>
<script>
  import pick from 'lodash.pick'
  import BatchSetPriceModal from './BatchSetPriceModal'
  import BatchSetStockModal from './BatchSetStockModal'
  import UnitModal from '../../system/modules/UnitModal'
  import JEditableTable from '@/components/jeecg/JEditableTable'
  import { FormTypes, getRefPromise, VALIDATE_NO_PASSED, validateFormAndTables } from '@/utils/JEditableTableUtil'
  import { changeNameToPinYin, checkMaterial, checkMaterialBarCode, getMaterialAttributeNameList, getMaterialAttributeValueListById, getMaxBarCode, queryMaterialCategoryTreeList } from '@/api/api'
  import { autoJumpNextInput, handleIntroJs, removeByVal, addBigNumbers, getMaterialPropertyList } from '@/utils/util'
  import { getAction, httpAction } from '@/api/manage'
  import JImageUpload from '@/components/jeecg/JImageUpload'
  import JDate from '@/components/jeecg/JDate'
  import Vue from 'vue'

  export default {
    name: "MaterialModal",
    components: {
      BatchSetPriceModal,
      BatchSetStockModal,
      UnitModal,
      JImageUpload,
      JDate,
      JEditableTable,
      VNodes: {
        functional: true,
        render: (h, ctx) => ctx.props.vnodes,
      }
    },
    data () {
      return {
        title: this.$t('common.action'),
        width: '1300px',
        visible: false,
        modalStyle: '',
        action: '',
        activeKey: '1',
        categoryTree: [],
        unitList: [],
        depotList: [],
        fileList:[],
        unitStatus: false,
        manyUnitStatus: true,
        unitChecked: false,
        switchDisabled: false, //开关的启用状态
        barCodeSwitch: false, //生成条码开关
        maxBarCodeInfo: '', //最大条码
        meDeleteIdList: [], //删除条码信息的id数组
        prefixNo: 'material',
        attributeStatus: false,
        materialAttributeList: [],
        skuOneTitle: this.$t('common.property') + '1',
        skuTwoTitle: this.$t('common.property') + '2',
        skuThreeTitle: this.$t('common.property') + '3',
        skuOneList: [],
        skuTwoList: [],
        skuThreeList: [],
        meOldDataSource: [],
        manySkuSelected: 0,
        model: {},
        showOkFlag: true,
        setTimeFlag: null,
        labelCol: {
          xs: { span: 24 },
          sm: { span: 8 },
        },
        wrapperCol: {
          xs: { span: 24 },
          sm: { span: 16 },
        },
        mpShort: {
          mfrs: {},
          otherField1: { name: this.$t('common.property') },
          otherField2: { name: this.$t('common.property') },
          otherField3: { name: this.$t('common.property') }
        },
        meTable: {
          loading: false,
          dataSource: [],
          columns: [
            {
              title: this.$t('common.barcode'), key: 'barCode', width: '15%', type: FormTypes.input, defaultValue: '', placeholder: this.$t('common.pleaseEnter') + '${title}',
              validateRules: [{ required: true, message: '${title}' + this.$t('material.cannotBeEmpty') },
                { pattern: /^.{4,40}$/, message: this.$t('material.lengthBetween4And40') },
                { handler: this.validateBarCode}]
            },
            {
              title: this.$t('common.unit'), key: 'commodityUnit', width: '8%', type: FormTypes.input, defaultValue: '', placeholder: this.$t('common.pleaseEnter') + '${title}',
              validateRules: [{ required: true, message: '${title}' + this.$t('material.cannotBeEmpty') }]
            },
            {
              title: this.$t('common.property'), key: 'sku', width: '25%', type: FormTypes.input, defaultValue: '', readonly:true, placeholder: this.$t('common.pleaseEnter') + '${title}'
            },
            {
              title: this.$t('common.amount'), key: 'purchaseDecimal', width: '9%', type: FormTypes.inputNumber, defaultValue: '', placeholder: this.$t('common.pleaseEnter') + '${title}'
            },
            {
              title: this.$t('common.amount'), key: 'commodityDecimal', width: '9%', type: FormTypes.inputNumber, defaultValue: '', placeholder: this.$t('common.pleaseEnter') + '${title}'
            },
            {
              title: this.$t('common.amount'), key: 'wholesaleDecimal', width: '9%', type: FormTypes.inputNumber, defaultValue: '', placeholder: this.$t('common.pleaseEnter') + '${title}'
            },
            {
              title: this.$t('common.amount'), key: 'lowDecimal', width: '9%', type: FormTypes.inputNumber, defaultValue: '', placeholder: this.$t('common.pleaseEnter') + '${title}'
            }
          ]
        },
        depotTable: {
          loading: false,
          dataSource: [],
          columns: [
            {
              title: this.$t('common.warehouse'), key: 'name', width: '15%', type: FormTypes.normal
            },
            {
              title: this.$t('common.quantity'), key: 'initStock', width: '15%', type: FormTypes.inputNumber, defaultValue: '', placeholder: this.$t('common.pleaseEnter') + '${title}'
            },
            {
              title: this.$t('common.safetyStock'), key: 'lowSafeStock', width: '15%', type: FormTypes.inputNumber, defaultValue: '', placeholder: this.$t('common.pleaseEnter') + '${title}'
            },
            {
              title: this.$t('common.safetyStock'), key: 'highSafeStock', width: '15%', type: FormTypes.inputNumber, defaultValue: '', placeholder: this.$t('common.pleaseEnter') + '${title}'
            }
          ]
        },
        confirmLoading: false,
        form: this.$form.createForm(this),
        validatorRules:{
          name:{
            rules: [
              { required: true, message: this.$t('common.enterName') },
              { max: 100, message: this.$t('common.aliasLength'), trigger: 'blur' }
            ]
          },
          standard:{
            rules: [
              { max: 100, message: this.$t('common.aliasLength'), trigger: 'blur' }
            ]
          },
          model:{
            rules: [
              { max: 100, message: this.$t('common.aliasLength'), trigger: 'blur' }
            ]
          },
          unit:{
            rules: [
              { required: true, message: this.$t('common.unit') }
            ]
          },
          unitId:{
            rules: [
              { required: true, message: this.$t('common.unit') }
            ]
          }
        },
        url: {
          add: '/material/add',
          edit: '/material/update',
          materialsExtendList: '/materialsExtend/getDetailList',
          depotWithStock: '/depot/getAllListWithStock'
        }
      }
    },
    created () {
      this.loadParseMaterialProperty()
      let realScreenWidth = window.screen.width
      this.width = realScreenWidth<1500?'1200px':'1400px'
    },
    mounted() {
      document.getElementById(this.prefixNo).addEventListener('keydown', this.handleOkKey)
    },
    beforeDestroy() {
      document.getElementById(this.prefixNo).removeEventListener('keydown', this.handleOkKey)
    },
    methods: {
      // 快捷键
      handleOkKey(e) {
        const key = window.event.keyCode ? window.event.keyCode : window.event.which
        if (key === 83 && e.ctrlKey) {
          //保存 CTRL+S
          this.handleOk()
          e.preventDefault()
        }
      },
      // 获取所有的editableTable实例
      getAllTable() {
        return Promise.all([
          getRefPromise(this, 'editableMeTable'),
          getRefPromise(this, 'editableDepotTable')
        ])
      },
      add () {
        //隐藏多属性
        this.meTable.columns[2].type = FormTypes.hidden
        // 默认新增一条数据
        this.getAllTable().then(editableTables => {
          editableTables[0].add()
        })
        this.edit({})
        this.$nextTick(() => {
          handleIntroJs('material', 11)
        })
      },
      edit (record) {
        let that = this
        this.form.resetFields();
        this.model = Object.assign({}, record);
        let attribute = record.attribute
        if(attribute) {
          //构造多属性
          let attrObj = JSON.parse(attribute)
          this.model.manySku = attrObj.manySku
          this.model.skuOne = attrObj.skuOne
          this.model.skuTwo = attrObj.skuTwo
          this.model.skuThree = attrObj.skuThree
        }
        this.activeKey = '1'
        this.manySkuSelected = 0
        this.barCodeSwitch = false
        this.maxBarCodeInfo = ''
        this.visible = true
        this.meDeleteIdList = []
        this.modalStyle = 'top:20px;height: 95%;'
        if(JSON.stringify(record) === '{}') {
          this.fileList = []
        } else {
          if(this.action === 'edit') {
            setTimeout(() => {
              this.fileList = record.imgName
            }, 5)
          }
        }
        this.$nextTick(() => {
          this.form.setFieldsValue(pick(this.model, 'name', 'standard', 'unit', 'unitId', 'model', 'color', 'brand', 'mnemonic',
            'categoryId','enableSerialNumber','enableBatchNumber','position','expiryNum','weight','remark','mfrs',
            'otherField1','otherField2','otherField3','manySku','skuOne','skuTwo','skuThree'))
          autoJumpNextInput('materialHeadModal')
          autoJumpNextInput('materialDetailModal')
        });
        this.initMaterialAttribute()
        this.loadTreeData()
        this.loadUnitListData()
        // 加载子表数据
        if (this.model.id) {
          //禁用多属性开关
          this.switchDisabled = true
          // 判断是否是多单位
          if(this.model.unit){
            this.unitChecked = false
            this.unitStatus = false
            this.manyUnitStatus = true
          } else {
            this.unitChecked = true
            this.unitStatus = true
            this.manyUnitStatus = false
          }
          //编辑状态下有多属性，则不允许修改
          if(this.model.manySku) {
            this.attributeStatus = true
            //加载每个多属性的下拉框
            setTimeout(function() {
              that.loadSkuList(that.model.manySku)
            },1000)
          } else {
            this.attributeStatus = false
          }
          let params = { materialId: this.model.id }
          this.requestMeTableData(this.url.materialsExtendList, params, this.meTable)
          this.requestDepotTableData(this.url.depotWithStock, { mId: this.model.id }, this.depotTable)
        } else {
          this.attributeStatus = false
          this.switchDisabled = false
          this.meTable.columns[2].readonly = true
          this.requestDepotTableData(this.url.depotWithStock, { mId: 0 }, this.depotTable)
        }
      },
      /** 查询条码tab的数据 */
      requestMeTableData(url, params, tab) {
        tab.loading = true
        getAction(url, params).then(res => {
          for (let i = 0; i < res.data.rows.length; i++) {
            if(res.data.rows[i].sku) {
              this.meTable.columns[2].type = FormTypes.input
            } else {
              this.meTable.columns[2].type = FormTypes.hidden
            }
          }
          tab.dataSource = res.data.rows || []
          this.meOldDataSource = res.data.rows || []
          //复制新增商品-初始化条码信息
          if(this.action === 'copyAdd') {
            getMaxBarCode({}).then((res)=> {
              if (res && res.code === 200) {
                let maxBarCode = res.data.barCode
                let meTableData = []
                for (let i = 0; i < tab.dataSource.length; i++) {
                  let meInfo = tab.dataSource[i]
                  console.log(maxBarCode)
                  console.log(addBigNumbers(maxBarCode, i+1))
                  meInfo.barCode = addBigNumbers(maxBarCode, i+1)
                  console.log(meInfo.barCode)
                  meTableData.push(meInfo)
                }
                tab.dataSource = meTableData
              }
            })
          }
        }).finally(() => {
          tab.loading = false
        })
      },
      /** 查询仓库tab的数据 */
      requestDepotTableData(url, params, tab) {
        tab.loading = true
        getAction(url, params).then(res => {
          tab.dataSource = res.data || []
        }).finally(() => {
          tab.loading = false
        })
      },
      close () {
        this.$emit('close')
        this.visible = false
        this.modalStyle = ''
        this.unitStatus = false
        this.manyUnitStatus = true
        this.unitChecked = false
        this.getAllTable().then(editableTables => {
          editableTables[0].initialize()
          editableTables[1].initialize()
        })
      },
      handleOk () {
        this.validateFields()
      },
      handleCancel () {
        this.close()
      },
      /** 触发表单验证 */
      validateFields() {
        this.getAllTable().then(tables => {
          /** 一次性验证主表和所有的次表 */
          return validateFormAndTables(this.form, tables)
        }).then(allValues => {
          let formData = this.classifyIntoFormData(allValues)
          formData.sortList = [];
          if(formData.unit === undefined) {formData.unit = ''}
          if(formData.unitId === undefined) {formData.unitId = ''}
          if(this.unitChecked) {formData.unit = ''} else {formData.unitId = ''}
          // 发起请求
          return this.requestAddOrEdit(formData)
        }).catch(e => {
          if (e.error === VALIDATE_NO_PASSED) {
            // 如果有未通过表单验证的子表，就自动跳转到它所在的tab
            this.activeKey = e.index == null ? this.activeKey : (e.index + 1).toString()
          } else {
            console.error(e)
          }
        })
      },
      /** 整理成formData */
      classifyIntoFormData(allValues) {
        let materialMain = Object.assign(this.model, allValues.formValue)
        return {
          ...materialMain, // 展开
          meList: allValues.tablesValue[0].values,
          stock: allValues.tablesValue[1].values,
        }
      },
      /** 发起新增或修改的请求 */
      requestAddOrEdit(formData) {
        //复制新增商品-初始化id和租户id
        if(this.action === 'copyAdd') {
          this.model.id = ''
          this.model.tenantId = ''
          formData.id = ''
          formData.tenantId = ''
        }
        if(formData.meList.length === 0) {
          this.$message.warning(this.$t('common.barcode'));
          return;
        }
        if(formData.enableSerialNumber === '1' && formData.enableBatchNumber === '1') {
          this.$message.warning(this.$t('material.snAndBatchOnlyOne'));
          return;
        }
        if(formData.manySku && formData.unitId) {
          this.$message.warning(this.$t('material.skuNoMultiUnit'));
          return;
        }
        //校验商品是否存在，通过校验商品的名称、型号、规格、颜色、单位、制造商等
        let param = {
          id: this.model.id?this.model.id:0,
          name: this.model.name,
          model: this.parseParam(this.model.model),
          color: this.parseParam(this.model.color),
          standard: this.parseParam(this.model.standard),
          mfrs: this.parseParam(this.model.mfrs),
          otherField1: this.parseParam(this.model.otherField1),
          otherField2: this.parseParam(this.model.otherField2),
          otherField3: this.parseParam(this.model.otherField3),
          unit: this.parseParam(this.model.unit),
          unitId: this.parseParam(this.model.unitId)
        }
        checkMaterial(param).then((res)=>{
          if(res && res.code===200) {
            if(res.data.status){
              this.$message.warning(this.$t('material.materialAlreadyExists'));
              return;
            } else {
              //进一步校验单位
              let basicUnit = '', otherUnit = '', otherUnitTwo = '', otherUnitThree = ''
              if(formData.unitId) {
                let unitArr = this.unitList
                for(let i=0; i < unitArr.length; i++) {
                  if(unitArr[i].id == formData.unitId) {
                    basicUnit = unitArr[i].basicUnit
                    otherUnit = unitArr[i].otherUnit
                    if(unitArr[i].otherUnitTwo) {
                      otherUnitTwo = unitArr[i].otherUnitTwo
                    }
                    if(unitArr[i].otherUnitThree) {
                      otherUnitThree = unitArr[i].otherUnitThree
                    }
                  }
                }
              }
              if(!formData.unit) {
                //此时为多单位
                if (formData.meList.length<2){
                  this.$message.warning(this.$t('material.multiUnitMinTwoRows'));
                  return;
                }
                if(formData.meList[0].commodityUnit != basicUnit) {
                  this.$message.warning(this.$t('material.unitMismatch', {unit: formData.meList[0].commodityUnit, correct: basicUnit}));
                  return;
                }
                if(formData.meList[1].commodityUnit != otherUnit) {
                  this.$message.warning(this.$t('material.unitMismatch', {unit: formData.meList[1].commodityUnit, correct: otherUnit}));
                  return;
                }
              }
              let skuCount = 0
              for(let i=0; i<formData.meList.length; i++) {
                let commodityUnit = formData.meList[i].commodityUnit;
                if(formData.unit) {
                  if(commodityUnit != formData.unit) {
                    this.$message.warning(this.$t('material.unitMismatch', {unit: commodityUnit, correct: formData.unit}));
                    return;
                  }
                } else if(formData.unitId) {
                  if(commodityUnit != basicUnit && commodityUnit != otherUnit && commodityUnit != otherUnitTwo && commodityUnit != otherUnitThree) {
                    let correctUnits = basicUnit + '】或【' + otherUnit
                    if(otherUnitTwo) {
                      correctUnits += '】或【' + otherUnitTwo
                    }
                    if(otherUnitThree) {
                      correctUnits += '】或【' + otherUnitThree
                    }
                    this.$message.warning(this.$t('material.unitMismatch', {unit: commodityUnit, correct: correctUnits}));
                    return;
                  }
                }
                if(formData.sku) {
                  skuCount++
                }
              }
              //对最低和最高安全库存进行校验
              for (let i = 0; i < formData.stock.length; i++) {
                let depotStockObj = formData.stock[i]
                if(skuCount && depotStockObj.initStock && depotStockObj.initStock-0) {
                  this.$message.warning(this.$t('material.skuNoInitStock'))
                  return
                }
                if(formData.enableSerialNumber === '1' && depotStockObj.initStock && depotStockObj.initStock-0) {
                  this.$message.warning(this.$t('material.snNoInitStock'))
                  return
                }
                if(formData.enableBatchNumber === '1' && depotStockObj.initStock && depotStockObj.initStock-0) {
                  this.$message.warning(this.$t('material.batchNoInitStock'))
                  return
                }
                if(depotStockObj.lowSafeStock && depotStockObj.highSafeStock) {
                  if(depotStockObj.lowSafeStock-0 > depotStockObj.highSafeStock-0) {
                    this.$message.warning(this.$t('material.safetyStockError', {name: depotStockObj.name}))
                    return
                  }
                }
              }
              //图片校验
              if(this.fileList && this.fileList.length > 0) {
                formData.imgName = this.fileList
                let fileArr = this.fileList.split(',')
                if(fileArr.length > 4) {
                  this.$message.warning(this.$t('material.imageLimitExceeded'));
                  return
                }
              } else {
                formData.imgName = ''
              }
              formData.meDeleteIdList = this.meDeleteIdList
              //接口调用
              let url = this.url.add, method = 'post'
              if (this.model.id) {
                url = this.url.edit
                method = 'put'
              }
              const that = this;
              this.confirmLoading = true
              httpAction(url, formData, method).then((res) => {
                if(res.code === 200){
                  that.$emit('ok');
                  that.confirmLoading = false
                  that.close();
                }else{
                  that.$message.warning(res.data.message);
                  that.confirmLoading = false
                }
              }).finally(() => {
              })
            }
          }
        }).catch(() => {
          this.$message.error(this.$t('material.checkFailed'))
        })
      },
      parseParam(param) {
        return param ? param: ""
      },
      validateBarCode(type, value, row, column, callback, target) {
        let params = {
          barCode: value,
          id: row.id.length >= 20?0: row.id
        };
        checkMaterialBarCode(params).then((res)=>{
          if(res && res.code===200) {
            if(!res.data.status){
              callback(true);
            } else {
              callback(false, this.$t('common.error'));
            }
          } else {
            callback(false, res.data);
          }
        });
      },
      loadTreeData(){
        let that = this;
        let params = {};
        params.id='';
        queryMaterialCategoryTreeList(params).then((res)=>{
          if(res){
            that.categoryTree = [];
            for (let i = 0; i < res.length; i++) {
              let temp = res[i];
              that.categoryTree.push(temp);
            }
          }
        })
      },
      loadUnitListData(){
        let that = this;
        let params = {};
        params.currentPage = 1;
        params.pageSize = 100;
        getAction('/unit/getAllList', params).then((res) => {
          if(res){
            that.unitList = res.data;
          }
        })
      },
      onManySkuChange(value) {
        this.manySkuSelected = value.length
        //控制多属性下拉框中选择项的状态
        if(value.length < 3){
          this.materialAttributeList.forEach((item,index,array)=>{
            (array.indexOf(item.value) === -1)?Vue.set(array[index], 'disabled', false):''
          })
        }else{
          this.materialAttributeList.forEach((item,index,array)=>{
            (value.indexOf(item.value) === -1)?Vue.set(array[index], 'disabled', true):''
          })
        }
        //更新属性1和属性2和属性3的下拉框
        if(value.length <= 3) {
          let skuOneId = value[0]
          let skuTwoId = value[1]
          let skuThreeId = value[2]
          this.materialAttributeList.forEach(item => {
            if(item.value === skuOneId) {
              this.skuOneTitle = item.name
            }
            if(item.value === skuTwoId) {
              this.skuTwoTitle = item.name
            }
            if(item.value === skuThreeId) {
              this.skuThreeTitle = item.name
            }
          })
          if(skuOneId) {
            getMaterialAttributeValueListById({'id': skuOneId}).then((res)=>{
              this.skuOneList = res? res:[]
            })
          }
          if(skuTwoId) {
            getMaterialAttributeValueListById({'id': skuTwoId}).then((res)=>{
              this.skuTwoList = res? res:[]
            })
          }
          if(skuThreeId) {
            getMaterialAttributeValueListById({'id': skuThreeId}).then((res)=>{
              this.skuThreeList = res? res:[]
            })
          }
        }
        //控制条码列表中的多属性列
        if(value.length>0) {
          this.meTable.columns[2].type = FormTypes.input
        } else {
          this.meTable.columns[2].type = FormTypes.hidden
        }
        this.barCodeSwitch = false;
        this.meTable.dataSource = []
      },
      //编辑页面加载的时候加载存在的sku
      loadSkuList(value) {
        this.manySkuSelected = value.length
        //更新属性1和属性2和属性3的下拉框
        if(value.length <= 3) {
          let skuOneId = value[0]
          let skuTwoId = value[1]
          let skuThreeId = value[2]
          this.materialAttributeList.forEach(item => {
            if(item.value === skuOneId) {
              this.skuOneTitle = item.name
            }
            if(item.value === skuTwoId) {
              this.skuTwoTitle = item.name
            }
            if(item.value === skuThreeId) {
              this.skuThreeTitle = item.name
            }
          })
          if(skuOneId) {
            getMaterialAttributeValueListById({'id': skuOneId}).then((res)=>{
              this.skuOneList = res? res:[]
              this.form.setFieldsValue(pick(this.model, 'skuOne'))
            })
          }
          if(skuTwoId) {
            getMaterialAttributeValueListById({'id': skuTwoId}).then((res)=>{
              this.skuTwoList = res? res:[]
              this.form.setFieldsValue(pick(this.model, 'skuTwo'))
            })
          }
          if(skuThreeId) {
            getMaterialAttributeValueListById({'id': skuThreeId}).then((res)=>{
              this.skuThreeList = res? res:[]
              this.form.setFieldsValue(pick(this.model, 'skuThree'))
            })
          }
        }
        this.barCodeSwitch = false
      },
      onSkuChange() {
        let skuOneData = this.form.getFieldValue('skuOne')
        let skuTwoData = this.form.getFieldValue('skuTwo')
        let skuThreeData = this.form.getFieldValue('skuThree')
        this.autoSkuList(skuOneData, skuTwoData, skuThreeData)
      },
      onSkuOneDeSelect(value) {
        let skuOneData = this.form.getFieldValue('skuOne')
        let skuTwoData = this.form.getFieldValue('skuTwo')
        let skuThreeData = this.form.getFieldValue('skuThree')
        removeByVal(skuOneData, value)
        this.autoSkuList(skuOneData, skuTwoData, skuThreeData)
      },
      onSkuTwoDeSelect(value) {
        let skuOneData = this.form.getFieldValue('skuOne')
        let skuTwoData = this.form.getFieldValue('skuTwo')
        let skuThreeData = this.form.getFieldValue('skuThree')
        removeByVal(skuTwoData, value)
        this.autoSkuList(skuOneData, skuTwoData, skuThreeData)
      },
      onSkuThreeDeSelect(value) {
        let skuOneData = this.form.getFieldValue('skuOne')
        let skuTwoData = this.form.getFieldValue('skuTwo')
        let skuThreeData = this.form.getFieldValue('skuThree')
        removeByVal(skuThreeData, value)
        this.autoSkuList(skuOneData, skuTwoData, skuThreeData)
      },
      autoSkuList(skuOneData, skuTwoData, skuThreeData) {
        let unit = this.form.getFieldValue('unit')
        //计算多属性已经选择了几个
        let skuArr = []
        if(this.getNumByField('skuOne')) {
          skuArr.push(skuOneData)
        }
        if(this.getNumByField('skuTwo')) {
          skuArr.push(skuTwoData)
        }
        if(this.getNumByField('skuThree')) {
          skuArr.push(skuThreeData)
        }
        let skuArrOne = skuArr[0]
        let skuArrTwo = skuArr[1]
        let skuArrThree = skuArr[2]
        let count = this.getNumByField('skuOne') + this.getNumByField('skuTwo') + this.getNumByField('skuThree')
        let barCodeSku = []
        if(count === 1) {
          let skuArrOnly = []
          if(this.getNumByField('skuOne')) {
            skuArrOnly = skuOneData
          } else if(this.getNumByField('skuTwo')) {
            skuArrOnly = skuTwoData
          } else if(this.getNumByField('skuThree')) {
            skuArrOnly = skuThreeData
          }
          for (let i = 0; i < skuArrOnly.length; i++) {
            barCodeSku.push(skuArrOnly[i])
          }
        } else if(count === 2) {
          for (let i = 0; i < skuArrOne.length; i++) {
            for (let j = 0; j < skuArrTwo.length; j++) {
              barCodeSku.push(skuArrOne[i] + '/' + skuArrTwo[j])
            }
          }
        } else if(count === 3) {
          for (let i = 0; i < skuArrOne.length; i++) {
            for (let j = 0; j < skuArrTwo.length; j++) {
              for (let k = 0; k < skuArrThree.length; k++) {
                barCodeSku.push(skuArrOne[i] + '/' + skuArrTwo[j] + '/' + skuArrThree[k])
              }
            }
          }
        }
        let meTableData = []
        getMaxBarCode({}).then((res)=>{
          if(res && res.code===200) {
            let k = 0
            let maxBarCode = res.data.barCode
            for (let i = 0; i < barCodeSku.length; i++) {
              let currentBarCode = ''
              let currentId = ''
              let purchaseDecimal = ''
              let commodityDecimal = ''
              let wholesaleDecimal = ''
              let lowDecimal = ''
              let defaultFlag = ''
              for (let j = 0; j < this.meOldDataSource.length; j++) {
                if(barCodeSku[i] === this.meOldDataSource[j].sku) {
                  currentBarCode = this.meOldDataSource[j].barCode
                  currentId = this.meOldDataSource[j].id
                  purchaseDecimal = this.meOldDataSource[j].purchaseDecimal
                  commodityDecimal = this.meOldDataSource[j].commodityDecimal
                  wholesaleDecimal = this.meOldDataSource[j].wholesaleDecimal
                  lowDecimal = this.meOldDataSource[j].lowDecimal
                  defaultFlag = this.meOldDataSource[j].defaultFlag
                }
              }
              if(currentBarCode) {
                //此时说明该sku之前就存在
                meTableData.push({id: currentId, barCode: currentBarCode, commodityUnit: unit, sku: barCodeSku[i],
                  purchaseDecimal: purchaseDecimal, commodityDecimal: commodityDecimal,
                  wholesaleDecimal: wholesaleDecimal, lowDecimal: lowDecimal, defaultFlag: defaultFlag})
              } else {
                k = k+1
                currentBarCode = addBigNumbers(maxBarCode, k)
                meTableData.push({barCode: currentBarCode, commodityUnit: unit, sku: barCodeSku[i]})
              }
            }
            this.meTable.dataSource = meTableData
          }
        })
      },
      getNumByField(field) {
        let num = 0
        if(this.form.getFieldValue(field)) {
          if(this.form.getFieldValue(field).length>0) {
            num = 1
          }
        }
        return num
      },
      onAdded(event) {
        const { row, target } = event
        let unit = ''
        if(this.unitStatus == false) {
          unit = this.form.getFieldValue('unit')
        }
        if(this.maxBarCodeInfo === '') {
          getMaxBarCode({}).then((res)=> {
            if (res && res.code === 200) {
              this.maxBarCodeInfo = res.data.barCode
              this.maxBarCodeInfo = addBigNumbers(this.maxBarCodeInfo, 1)
              target.setValues([{rowKey: row.id, values: {barCode: this.maxBarCodeInfo, commodityUnit: unit?unit:''}}])
            }
          })
        } else {
          this.maxBarCodeInfo = addBigNumbers(this.maxBarCodeInfo, 1)
          target.setValues([{rowKey: row.id, values: {barCode: this.maxBarCodeInfo, commodityUnit: unit?unit:''}}])
        }
      },
      onDeleted(value) {
        this.meDeleteIdList = (value)
      },
      //单元值改变一个字符就触发一次
      onValueChange(event) {
        const { type, row, column, value, target } = event
        switch(column.key) {
          case "purchaseDecimal":
          case "commodityDecimal":
          case "wholesaleDecimal":
          case "lowDecimal":
            this.changeDecimalByValue(row)
            break;
        }
      },
      //修改商品明细中的价格触发计算
      changeDecimalByValue(row) {
        let unitArr = this.unitList
        let basicUnit = '', otherUnit = '', ratio = 1, otherUnitTwo = '', ratioTwo = 1, otherUnitThree = '', ratioThree = 1
        for (let i = 0; i < unitArr.length; i++) {
          if(unitArr[i].id === this.form.getFieldValue('unitId')) {
            basicUnit = unitArr[i].basicUnit
            otherUnit = unitArr[i].otherUnit
            ratio = unitArr[i].ratio
            if(unitArr[i].otherUnitTwo) {
              otherUnitTwo = unitArr[i].otherUnitTwo
              ratioTwo = unitArr[i].ratioTwo
            }
            if(unitArr[i].otherUnitThree) {
              otherUnitThree = unitArr[i].otherUnitThree
              ratioThree = unitArr[i].ratioThree
            }
          }
        }
        if(row.commodityUnit === basicUnit) {
          this.$refs.editableMeTable.getValues((error, values) => {
            let mArr = values, basicPurchaseDecimal='', basicCommodityDecimal='', basicWholesaleDecimal='', basicLowDecimal=''
            const basicInfo = mArr.find(item => item.commodityUnit === basicUnit)
            const ratioByUnit = {
              [otherUnit]: ratio,
              [otherUnitTwo]: ratioTwo,
              [otherUnitThree]: ratioThree
            }
            if(basicInfo) {
              basicPurchaseDecimal = basicInfo.purchaseDecimal
              basicCommodityDecimal = basicInfo.commodityDecimal
              basicWholesaleDecimal = basicInfo.wholesaleDecimal
              basicLowDecimal = basicInfo.lowDecimal
            }
            for (let i = 0; i < mArr.length; i++) {
              let mInfo = mArr[i]
              const currentRatio = ratioByUnit[mInfo.commodityUnit]
              if(mInfo.commodityUnit !== basicUnit && currentRatio) {
                if(basicPurchaseDecimal) { mInfo.purchaseDecimal = (basicPurchaseDecimal*currentRatio).toFixed(2)}
                if(basicCommodityDecimal) { mInfo.commodityDecimal = (basicCommodityDecimal*currentRatio).toFixed(2)}
                if(basicWholesaleDecimal) { mInfo.wholesaleDecimal = (basicWholesaleDecimal*currentRatio).toFixed(2)}
                if(basicLowDecimal) { mInfo.lowDecimal = (basicLowDecimal*currentRatio).toFixed(2)}
              }
            }
            this.meTable.dataSource = mArr
          })
        }
      },
      batchSetPrice(type) {
        if(this.manySkuSelected>0 || this.model.id){
          this.$refs.priceModalForm.add(type);
          this.$refs.priceModalForm.disableSubmit = false;
        } else {
          this.$message.warning(this.$t('material.skuRequiredForBatch'));
        }
      },
      batchSetStock(type) {
        this.$refs.stockModalForm.add(type);
        this.$refs.stockModalForm.disableSubmit = false;
      },
      batchSetPriceModalFormOk(price, batchType) {
        let arr = this.meTable.dataSource
        if(arr.length === 0) {
          this.$message.warning(this.$t('material.enterBarcodeFirst'));
        } else {
          let meTableData = []
          for (let i = 0; i < arr.length; i++) {
            let meInfo = {barCode: arr[i].barCode, commodityUnit: arr[i].commodityUnit, sku: arr[i].sku,
              purchaseDecimal: arr[i].purchaseDecimal, commodityDecimal: arr[i].commodityDecimal,
              wholesaleDecimal: arr[i].wholesaleDecimal, lowDecimal: arr[i].lowDecimal}
            if(batchType === 'purchase') {
              meInfo.purchaseDecimal = price-0
            } else if(batchType === 'commodity') {
              meInfo.commodityDecimal = price-0
            } else if(batchType === 'wholesale') {
              meInfo.wholesaleDecimal = price-0
            } else if(batchType === 'low') {
              meInfo.lowDecimal = price-0
            }
            if(arr[i].id) {
              meInfo.id = arr[i].id
            }
            meTableData.push(meInfo)
          }
          this.meTable.dataSource = meTableData
        }
      },
      batchSetStockModalFormOk(stock, batchType) {
        let arr = this.depotTable.dataSource
        let depotTableData = []
        for (let i = 0; i < arr.length; i++) {
          let depotInfo = {name: arr[i].name, initStock: arr[i].initStock,
            lowSafeStock: arr[i].lowSafeStock, highSafeStock: arr[i].highSafeStock}
          if (batchType === 'initStock') {
            depotInfo.initStock = stock - 0
          } else if (batchType === 'lowSafeStock') {
            depotInfo.lowSafeStock = stock - 0
          } else if (batchType === 'highSafeStock') {
            depotInfo.highSafeStock = stock - 0
          }
          if (arr[i].id) {
            depotInfo.id = arr[i].id
          }
          depotTableData.push(depotInfo)
        }
        this.depotTable.dataSource = depotTableData
      },
      initMaterialAttribute() {
        getMaterialAttributeNameList().then((res)=>{
          if(res) {
            this.materialAttributeList = res
          }
        })
      },
      loadParseMaterialProperty() {
        let mpList = getMaterialPropertyList()
        for (let i = 0; i < mpList.length; i++) {
          if (mpList[i].nativeName === "扩展1") {
            this.mpShort.otherField1.name = mpList[i].anotherName
          }
          if (mpList[i].nativeName === "扩展2") {
            this.mpShort.otherField2.name = mpList[i].anotherName
          }
          if (mpList[i].nativeName === "扩展3") {
            this.mpShort.otherField3.name = mpList[i].anotherName
          }
        }
      },
      handleNameChange(e) {
        let that = this
        if(e.target.value) {
          if(this.setTimeFlag != null){
            clearTimeout(this.setTimeFlag)
          }
          this.setTimeFlag = setTimeout(()=>{
            changeNameToPinYin({name: e.target.value}).then((res) => {
              if (res && res.code === 200) {
                that.form.setFieldsValue({'mnemonic':res.data})
              } else {
                that.$message.warning(res.data)
              }
            })
          },500)
        } else {
          that.form.setFieldsValue({'mnemonic':''})
        }
      },
      onlyUnitOnChange(e) {
        this.$refs.editableMeTable.getValues((error, values) => {
          let mArr = values
          for (let i = 0; i < mArr.length; i++) {
            let mInfo = mArr[i]
            mInfo.commodityUnit = e.target.value
          }
          this.meTable.dataSource = mArr
        })
      },
      manyUnitOnChange(value) {
        let unitArr = this.unitList
        let basicUnit = '', otherUnit = '', ratio = 1, otherUnitTwo = '', ratioTwo = 1, otherUnitThree = '', ratioThree = 1
        for (let i = 0; i < unitArr.length; i++) {
          if(unitArr[i].id === value) {
            basicUnit = unitArr[i].basicUnit
            otherUnit = unitArr[i].otherUnit
            ratio = unitArr[i].ratio
            if(unitArr[i].otherUnitTwo) {
              otherUnitTwo = unitArr[i].otherUnitTwo
              ratioTwo = unitArr[i].ratioTwo
            }
            if(unitArr[i].otherUnitThree) {
              otherUnitThree = unitArr[i].otherUnitThree
              ratioThree = unitArr[i].ratioThree
            }
          }
        }
        this.$refs.editableMeTable.getValues((error, values) => {
          let mArr = values, basicPurchaseDecimal='', basicCommodityDecimal='', basicWholesaleDecimal='', basicLowDecimal=''
          for (let i = 0; i < mArr.length; i++) {
            let mInfo = mArr[i]
            if(i===0) {
              mInfo.commodityUnit = basicUnit
              basicPurchaseDecimal = mInfo.purchaseDecimal
              basicCommodityDecimal = mInfo.commodityDecimal
              basicWholesaleDecimal = mInfo.wholesaleDecimal
              basicLowDecimal = mInfo.lowDecimal
            } else {
              //副单位进行换算
              mInfo.commodityUnit = otherUnit
              if(basicPurchaseDecimal) { mInfo.purchaseDecimal = (basicPurchaseDecimal*ratio).toFixed(2)}
              if(basicCommodityDecimal) { mInfo.commodityDecimal = (basicCommodityDecimal*ratio).toFixed(2)}
              if(basicWholesaleDecimal) { mInfo.wholesaleDecimal = (basicWholesaleDecimal*ratio).toFixed(2)}
              if(basicLowDecimal) { mInfo.lowDecimal = (basicLowDecimal*ratio).toFixed(2)}
              if(otherUnitTwo && i===2) {
                mInfo.commodityUnit = otherUnitTwo
                if(basicPurchaseDecimal) { mInfo.purchaseDecimal = (basicPurchaseDecimal*ratioTwo).toFixed(2)}
                if(basicCommodityDecimal) { mInfo.commodityDecimal = (basicCommodityDecimal*ratioTwo).toFixed(2)}
                if(basicWholesaleDecimal) { mInfo.wholesaleDecimal = (basicWholesaleDecimal*ratioTwo).toFixed(2)}
                if(basicLowDecimal) { mInfo.lowDecimal = (basicLowDecimal*ratioTwo).toFixed(2)}
              }
              if(otherUnitThree && i===3) {
                mInfo.commodityUnit = otherUnitThree
                if(basicPurchaseDecimal) { mInfo.purchaseDecimal = (basicPurchaseDecimal*ratioThree).toFixed(2)}
                if(basicCommodityDecimal) { mInfo.commodityDecimal = (basicCommodityDecimal*ratioThree).toFixed(2)}
                if(basicWholesaleDecimal) { mInfo.wholesaleDecimal = (basicWholesaleDecimal*ratioThree).toFixed(2)}
                if(basicLowDecimal) { mInfo.lowDecimal = (basicLowDecimal*ratioThree).toFixed(2)}
              }
            }
          }
          this.meTable.dataSource = mArr
        })
      },
      unitOnChange (e) {
        let isChecked = e.target.checked;
        if(isChecked) {
          this.unitStatus = true;
          this.manyUnitStatus = false;
          this.unitChecked = true;
        } else {
          this.unitStatus = false;
          this.manyUnitStatus = true;
          this.unitChecked = false;
        }
      },
      addUnit() {
        this.$refs.unitModalForm.add();
        this.$refs.unitModalForm.title = this.$t('common.add');
        this.$refs.unitModalForm.disableSubmit = false;
      },
      unitModalFormOk() {
        this.loadUnitListData()
      }
    }
  }
</script>
<style scoped>
  .input-table {
    max-width: 100%;
    min-width: 1200px;
  }
  .tag-info {
    font-size:14px;
    height:32px;
    line-height:32px;
    width:100%;
    padding: 0px 11px;
    color: #bbb;
    background-color: #ffffff;
  }
</style>
