import Vue from 'vue'
import { getAction, postAction } from '@/api/manage'
import { FormTypes } from '@/utils/JEditableTableUtil'
import { findBillDetailByNumber, findBySelectSup, findBySelectCus, findBySelectRetail, getUserList, getAccount,
  waitBillCount, getCurrentSystemConfig, getPlatformConfigByKey, getPersonByNumType } from '@/api/api'
import { getCheckFlag, getFormatDate, getMpListShort, getPrevMonthFormatDate } from '@/utils/util'
import { USER_ID } from '@/store/mutation-types'
import moment from 'moment'
import pick from 'lodash.pick'

export const BillListMixin = {
  data () {
    return {
      /* 原始审核是否开启 */
      checkFlag: true,
      /* 单据Excel是否开启 */
      isShowExcel: false,
      //以销定购的场景开关
      purchaseBySaleFlag: false,
      //商品价格含税开关
      materialPriceTaxFlag: false,
      setTimeFlag: null,
      waitTotal: 0,
      dateFormat: 'YYYY-MM-DD',
      billExcelUrl: '',
      defaultDepotId: '',
      supList: [],
      cusList: [],
      retailList: [],
      salesManList: [],
      userList: [],
      accountList: [],
      // 实际索引
      settingDataIndex: [],
      // 存储展开的行key
      expandedRowKeys: [],
      // 实际列
      columns:[],
      // 明细表头
      detailColumns:[],
      // 列定义
      defDetailColumns: [],
      retailOutColumns: [
        { title: this.$t('common.depotName'), dataIndex: 'depotName'},
        { title: this.$t('common.barcode'), dataIndex: 'barCode'},
        { title: this.$t('common.name'), dataIndex: 'name'},
        { title: this.$t('common.specification'), dataIndex: 'standard'},
        { title: this.$t('common.model'), dataIndex: 'model'},
        { title: this.$t('material.color'), dataIndex: 'color'},
        { title: this.$t('common.brand'), dataIndex: 'brand'},
        { title: this.$t('material.manufacturer'), dataIndex: 'mfrs'},
        { title: this.$t('purchase.form.columns.ext1'), dataIndex: 'otherField1'},
        { title: this.$t('purchase.form.columns.ext2'), dataIndex: 'otherField2'},
        { title: this.$t('purchase.form.columns.ext3'), dataIndex: 'otherField3'},
        { title: this.$t('purchase.form.columns.stock'), dataIndex: 'stock'},
        { title: this.$t('common.unit'), dataIndex: 'unit'},
        { title: this.$t('purchase.form.columns.serialNumber'), dataIndex: 'snList', width:300},
        { title: this.$t('purchase.form.columns.batchNumber'), dataIndex: 'batchNumber'},
        { title: this.$t('purchase.form.columns.expirationDate'), dataIndex: 'expirationDate'},
        { title: this.$t('purchase.form.columns.sku'), dataIndex: 'sku'},
        { title: this.$t('common.quantity'), dataIndex: 'operNumber'},
        { title: this.$t('purchase.form.columns.unitPrice'), dataIndex: 'unitPrice'},
        { title: this.$t('common.amount'), dataIndex: 'allPrice'},
        { title: this.$t('common.weight'), dataIndex: 'weight'},
        { title: this.$t('common.position'), dataIndex: 'position'},
        { title: this.$t('common.remark'), dataIndex: 'remark'}
      ],
      retailBackColumns: [
        { title: this.$t('common.depotName'), dataIndex: 'depotName'},
        { title: this.$t('common.barcode'), dataIndex: 'barCode'},
        { title: this.$t('common.name'), dataIndex: 'name'},
        { title: this.$t('common.specification'), dataIndex: 'standard'},
        { title: this.$t('common.model'), dataIndex: 'model'},
        { title: this.$t('material.color'), dataIndex: 'color'},
        { title: this.$t('common.brand'), dataIndex: 'brand'},
        { title: this.$t('material.manufacturer'), dataIndex: 'mfrs'},
        { title: this.$t('purchase.form.columns.ext1'), dataIndex: 'otherField1'},
        { title: this.$t('purchase.form.columns.ext2'), dataIndex: 'otherField2'},
        { title: this.$t('purchase.form.columns.ext3'), dataIndex: 'otherField3'},
        { title: this.$t('purchase.form.columns.stock'), dataIndex: 'stock'},
        { title: this.$t('common.unit'), dataIndex: 'unit'},
        { title: this.$t('purchase.form.columns.serialNumber'), dataIndex: 'snList', width:300},
        { title: this.$t('purchase.form.columns.batchNumber'), dataIndex: 'batchNumber'},
        { title: this.$t('purchase.form.columns.expirationDate'), dataIndex: 'expirationDate'},
        { title: this.$t('purchase.form.columns.sku'), dataIndex: 'sku'},
        { title: this.$t('common.quantity'), dataIndex: 'operNumber'},
        { title: this.$t('purchase.form.columns.unitPrice'), dataIndex: 'unitPrice'},
        { title: this.$t('common.amount'), dataIndex: 'allPrice'},
        { title: this.$t('common.weight'), dataIndex: 'weight'},
        { title: this.$t('common.position'), dataIndex: 'position'},
        { title: this.$t('common.remark'), dataIndex: 'remark'}
      ],
      purchaseApplyColumns: [
        { title: this.$t('common.barcode'), dataIndex: 'barCode'},
        { title: this.$t('common.name'), dataIndex: 'name'},
        { title: this.$t('common.specification'), dataIndex: 'standard'},
        { title: this.$t('common.model'), dataIndex: 'model'},
        { title: this.$t('material.color'), dataIndex: 'color'},
        { title: this.$t('common.brand'), dataIndex: 'brand'},
        { title: this.$t('material.manufacturer'), dataIndex: 'mfrs'},
        { title: this.$t('purchase.form.columns.ext1'), dataIndex: 'otherField1'},
        { title: this.$t('purchase.form.columns.ext2'), dataIndex: 'otherField2'},
        { title: this.$t('purchase.form.columns.ext3'), dataIndex: 'otherField3'},
        { title: this.$t('common.unit'), dataIndex: 'unit'},
        { title: this.$t('purchase.form.columns.sku'), dataIndex: 'sku'},
        { title: this.$t('common.quantity'), dataIndex: 'operNumber'},
        { title: this.$t('purchase.finishPurchased'), dataIndex: 'finishNumber'},
        { title: this.$t('common.remark'), dataIndex: 'remark'}
      ],
      purchaseOrderColumns: [
        { title: this.$t('common.barcode'), dataIndex: 'barCode'},
        { title: this.$t('common.name'), dataIndex: 'name'},
        { title: this.$t('common.specification'), dataIndex: 'standard'},
        { title: this.$t('common.model'), dataIndex: 'model'},
        { title: this.$t('material.color'), dataIndex: 'color'},
        { title: this.$t('common.brand'), dataIndex: 'brand'},
        { title: this.$t('material.manufacturer'), dataIndex: 'mfrs'},
        { title: this.$t('purchase.form.columns.ext1'), dataIndex: 'otherField1'},
        { title: this.$t('purchase.form.columns.ext2'), dataIndex: 'otherField2'},
        { title: this.$t('purchase.form.columns.ext3'), dataIndex: 'otherField3'},
        { title: this.$t('purchase.form.columns.stock'), dataIndex: 'stock'},
        { title: this.$t('common.unit'), dataIndex: 'unit'},
        { title: this.$t('purchase.form.columns.sku'), dataIndex: 'sku'},
        { title: this.$t('common.quantity'), dataIndex: 'operNumber'},
        { title: this.$t('purchase.finishPurchased'), dataIndex: 'finishNumber'},
        { title: this.$t('purchase.form.columns.unitPrice'), dataIndex: 'unitPrice'},
        { title: this.$t('common.amount'), dataIndex: 'allPrice'},
        { title: this.$t('purchase.form.columns.taxRate') + '(%)', dataIndex: 'taxRate'},
        { title: this.$t('purchase.form.columns.taxAmount'), dataIndex: 'taxMoney'},
        { title: this.$t('purchase.form.columns.taxTotal'), dataIndex: 'taxLastMoney'},
        { title: this.$t('common.remark'), dataIndex: 'remark'}
      ],
      purchaseInColumns: [
        { title: this.$t('common.depotName'), dataIndex: 'depotName'},
        { title: this.$t('common.barcode'), dataIndex: 'barCode'},
        { title: this.$t('common.name'), dataIndex: 'name'},
        { title: this.$t('common.specification'), dataIndex: 'standard'},
        { title: this.$t('common.model'), dataIndex: 'model'},
        { title: this.$t('material.color'), dataIndex: 'color'},
        { title: this.$t('common.brand'), dataIndex: 'brand'},
        { title: this.$t('material.manufacturer'), dataIndex: 'mfrs'},
        { title: this.$t('purchase.form.columns.ext1'), dataIndex: 'otherField1'},
        { title: this.$t('purchase.form.columns.ext2'), dataIndex: 'otherField2'},
        { title: this.$t('purchase.form.columns.ext3'), dataIndex: 'otherField3'},
        { title: this.$t('purchase.form.columns.stock'), dataIndex: 'stock'},
        { title: this.$t('common.unit'), dataIndex: 'unit'},
        { title: this.$t('purchase.form.columns.serialNumber'), dataIndex: 'snList', width:300},
        { title: this.$t('purchase.form.columns.batchNumber'), dataIndex: 'batchNumber'},
        { title: this.$t('purchase.form.columns.expirationDate'), dataIndex: 'expirationDate'},
        { title: this.$t('purchase.form.columns.sku'), dataIndex: 'sku'},
        { title: this.$t('common.quantity'), dataIndex: 'operNumber'},
        { title: this.$t('purchase.finishInbound'), dataIndex: 'finishNumber'},
        { title: this.$t('purchase.form.columns.unitPrice'), dataIndex: 'unitPrice'},
        { title: this.$t('common.amount'), dataIndex: 'allPrice'},
        { title: this.$t('purchase.form.columns.taxRate') + '(%)', dataIndex: 'taxRate'},
        { title: this.$t('purchase.form.columns.taxAmount'), dataIndex: 'taxMoney'},
        { title: this.$t('purchase.form.columns.taxTotal'), dataIndex: 'taxLastMoney'},
        { title: this.$t('common.weight'), dataIndex: 'weight'},
        { title: this.$t('common.position'), dataIndex: 'position'},
        { title: this.$t('common.remark'), dataIndex: 'remark'}
      ],
      purchaseBackColumns: [
        { title: this.$t('common.depotName'), dataIndex: 'depotName'},
        { title: this.$t('common.barcode'), dataIndex: 'barCode'},
        { title: this.$t('common.name'), dataIndex: 'name'},
        { title: this.$t('common.specification'), dataIndex: 'standard'},
        { title: this.$t('common.model'), dataIndex: 'model'},
        { title: this.$t('material.color'), dataIndex: 'color'},
        { title: this.$t('common.brand'), dataIndex: 'brand'},
        { title: this.$t('material.manufacturer'), dataIndex: 'mfrs'},
        { title: this.$t('purchase.form.columns.ext1'), dataIndex: 'otherField1'},
        { title: this.$t('purchase.form.columns.ext2'), dataIndex: 'otherField2'},
        { title: this.$t('purchase.form.columns.ext3'), dataIndex: 'otherField3'},
        { title: this.$t('purchase.form.columns.stock'), dataIndex: 'stock'},
        { title: this.$t('common.unit'), dataIndex: 'unit'},
        { title: this.$t('purchase.form.columns.serialNumber'), dataIndex: 'snList', width:300},
        { title: this.$t('purchase.form.columns.batchNumber'), dataIndex: 'batchNumber'},
        { title: this.$t('purchase.form.columns.expirationDate'), dataIndex: 'expirationDate'},
        { title: this.$t('purchase.form.columns.sku'), dataIndex: 'sku'},
        { title: this.$t('common.quantity'), dataIndex: 'operNumber'},
        { title: this.$t('sales.finishNumber'), dataIndex: 'finishNumber'},
        { title: this.$t('purchase.form.columns.unitPrice'), dataIndex: 'unitPrice'},
        { title: this.$t('common.amount'), dataIndex: 'allPrice'},
        { title: this.$t('purchase.form.columns.taxRate') + '(%)', dataIndex: 'taxRate'},
        { title: this.$t('purchase.form.columns.taxAmount'), dataIndex: 'taxMoney'},
        { title: this.$t('purchase.form.columns.taxTotal'), dataIndex: 'taxLastMoney'},
        { title: this.$t('common.weight'), dataIndex: 'weight'},
        { title: this.$t('common.position'), dataIndex: 'position'},
        { title: this.$t('common.remark'), dataIndex: 'remark'}
      ],
      saleOrderColumns: [
        { title: this.$t('common.barcode'), dataIndex: 'barCode'},
        { title: this.$t('common.name'), dataIndex: 'name'},
        { title: this.$t('common.specification'), dataIndex: 'standard'},
        { title: this.$t('common.model'), dataIndex: 'model'},
        { title: this.$t('material.color'), dataIndex: 'color'},
        { title: this.$t('common.brand'), dataIndex: 'brand'},
        { title: this.$t('material.manufacturer'), dataIndex: 'mfrs'},
        { title: this.$t('purchase.form.columns.ext1'), dataIndex: 'otherField1'},
        { title: this.$t('purchase.form.columns.ext2'), dataIndex: 'otherField2'},
        { title: this.$t('purchase.form.columns.ext3'), dataIndex: 'otherField3'},
        { title: this.$t('purchase.form.columns.stock'), dataIndex: 'stock'},
        { title: this.$t('common.unit'), dataIndex: 'unit'},
        { title: this.$t('purchase.form.columns.sku'), dataIndex: 'sku'},
        { title: this.$t('common.quantity'), dataIndex: 'operNumber'},
        { title: this.$t('purchase.finishPurchased'), dataIndex: 'finishPurchaseNumber'},
        { title: this.$t('sales.partialSales'), dataIndex: 'finishNumber'},
        { title: this.$t('purchase.form.columns.unitPrice'), dataIndex: 'unitPrice'},
        { title: this.$t('common.amount'), dataIndex: 'allPrice'},
        { title: this.$t('purchase.form.columns.taxRate') + '(%)', dataIndex: 'taxRate'},
        { title: this.$t('purchase.form.columns.taxAmount'), dataIndex: 'taxMoney'},
        { title: this.$t('purchase.form.columns.taxTotal'), dataIndex: 'taxLastMoney'},
        { title: this.$t('common.remark'), dataIndex: 'remark'}
      ],
      saleOutColumns: [
        { title: this.$t('common.depotName'), dataIndex: 'depotName'},
        { title: this.$t('common.barcode'), dataIndex: 'barCode'},
        { title: this.$t('common.name'), dataIndex: 'name'},
        { title: this.$t('common.specification'), dataIndex: 'standard'},
        { title: this.$t('common.model'), dataIndex: 'model'},
        { title: this.$t('material.color'), dataIndex: 'color'},
        { title: this.$t('common.brand'), dataIndex: 'brand'},
        { title: this.$t('material.manufacturer'), dataIndex: 'mfrs'},
        { title: this.$t('purchase.form.columns.ext1'), dataIndex: 'otherField1'},
        { title: this.$t('purchase.form.columns.ext2'), dataIndex: 'otherField2'},
        { title: this.$t('purchase.form.columns.ext3'), dataIndex: 'otherField3'},
        { title: this.$t('purchase.form.columns.stock'), dataIndex: 'stock'},
        { title: this.$t('common.unit'), dataIndex: 'unit'},
        { title: this.$t('purchase.form.columns.serialNumber'), dataIndex: 'snList', width:300},
        { title: this.$t('purchase.form.columns.batchNumber'), dataIndex: 'batchNumber'},
        { title: this.$t('purchase.form.columns.expirationDate'), dataIndex: 'expirationDate'},
        { title: this.$t('purchase.form.columns.sku'), dataIndex: 'sku'},
        { title: this.$t('common.quantity'), dataIndex: 'operNumber'},
        { title: this.$t('sales.finishNumber'), dataIndex: 'finishNumber'},
        { title: this.$t('purchase.form.columns.unitPrice'), dataIndex: 'unitPrice'},
        { title: this.$t('common.amount'), dataIndex: 'allPrice'},
        { title: this.$t('purchase.form.columns.taxRate') + '(%)', dataIndex: 'taxRate'},
        { title: this.$t('purchase.form.columns.taxAmount'), dataIndex: 'taxMoney'},
        { title: this.$t('purchase.form.columns.taxTotal'), dataIndex: 'taxLastMoney'},
        { title: this.$t('common.weight'), dataIndex: 'weight'},
        { title: this.$t('common.position'), dataIndex: 'position'},
        { title: this.$t('common.remark'), dataIndex: 'remark'}
      ],
      saleBackColumns: [
        { title: this.$t('common.depotName'), dataIndex: 'depotName'},
        { title: this.$t('common.barcode'), dataIndex: 'barCode'},
        { title: this.$t('common.name'), dataIndex: 'name'},
        { title: this.$t('common.specification'), dataIndex: 'standard'},
        { title: this.$t('common.model'), dataIndex: 'model'},
        { title: this.$t('material.color'), dataIndex: 'color'},
        { title: this.$t('common.brand'), dataIndex: 'brand'},
        { title: this.$t('material.manufacturer'), dataIndex: 'mfrs'},
        { title: this.$t('purchase.form.columns.ext1'), dataIndex: 'otherField1'},
        { title: this.$t('purchase.form.columns.ext2'), dataIndex: 'otherField2'},
        { title: this.$t('purchase.form.columns.ext3'), dataIndex: 'otherField3'},
        { title: this.$t('purchase.form.columns.stock'), dataIndex: 'stock'},
        { title: this.$t('common.unit'), dataIndex: 'unit'},
        { title: this.$t('purchase.form.columns.serialNumber'), dataIndex: 'snList', width:300},
        { title: this.$t('purchase.form.columns.batchNumber'), dataIndex: 'batchNumber'},
        { title: this.$t('purchase.form.columns.expirationDate'), dataIndex: 'expirationDate'},
        { title: this.$t('purchase.form.columns.sku'), dataIndex: 'sku'},
        { title: this.$t('common.quantity'), dataIndex: 'operNumber'},
        { title: this.$t('purchase.finishInbound'), dataIndex: 'finishNumber'},
        { title: this.$t('purchase.form.columns.unitPrice'), dataIndex: 'unitPrice'},
        { title: this.$t('common.amount'), dataIndex: 'allPrice'},
        { title: this.$t('purchase.form.columns.taxRate') + '(%)', dataIndex: 'taxRate'},
        { title: this.$t('purchase.form.columns.taxAmount'), dataIndex: 'taxMoney'},
        { title: this.$t('purchase.form.columns.taxTotal'), dataIndex: 'taxLastMoney'},
        { title: this.$t('common.weight'), dataIndex: 'weight'},
        { title: this.$t('common.position'), dataIndex: 'position'},
        { title: this.$t('common.remark'), dataIndex: 'remark'}
      ],
      otherInColumns: [
        { title: this.$t('common.depotName'), dataIndex: 'depotName'},
        { title: this.$t('common.barcode'), dataIndex: 'barCode'},
        { title: this.$t('common.name'), dataIndex: 'name'},
        { title: this.$t('common.specification'), dataIndex: 'standard'},
        { title: this.$t('common.model'), dataIndex: 'model'},
        { title: this.$t('material.color'), dataIndex: 'color'},
        { title: this.$t('common.brand'), dataIndex: 'brand'},
        { title: this.$t('material.manufacturer'), dataIndex: 'mfrs'},
        { title: this.$t('purchase.form.columns.ext1'), dataIndex: 'otherField1'},
        { title: this.$t('purchase.form.columns.ext2'), dataIndex: 'otherField2'},
        { title: this.$t('purchase.form.columns.ext3'), dataIndex: 'otherField3'},
        { title: this.$t('purchase.form.columns.stock'), dataIndex: 'stock'},
        { title: this.$t('common.unit'), dataIndex: 'unit'},
        { title: this.$t('purchase.form.columns.serialNumber'), dataIndex: 'snList', width:300},
        { title: this.$t('purchase.form.columns.batchNumber'), dataIndex: 'batchNumber'},
        { title: this.$t('purchase.form.columns.expirationDate'), dataIndex: 'expirationDate'},
        { title: this.$t('purchase.form.columns.sku'), dataIndex: 'sku'},
        { title: this.$t('common.quantity'), dataIndex: 'operNumber'},
        { title: this.$t('purchase.form.columns.unitPrice'), dataIndex: 'unitPrice'},
        { title: this.$t('common.amount'), dataIndex: 'allPrice'},
        { title: this.$t('common.weight'), dataIndex: 'weight'},
        { title: this.$t('common.position'), dataIndex: 'position'},
        { title: this.$t('common.remark'), dataIndex: 'remark'}
      ],
      otherOutColumns: [
        { title: this.$t('common.depotName'), dataIndex: 'depotName'},
        { title: this.$t('common.barcode'), dataIndex: 'barCode'},
        { title: this.$t('common.name'), dataIndex: 'name'},
        { title: this.$t('common.specification'), dataIndex: 'standard'},
        { title: this.$t('common.model'), dataIndex: 'model'},
        { title: this.$t('material.color'), dataIndex: 'color'},
        { title: this.$t('common.brand'), dataIndex: 'brand'},
        { title: this.$t('material.manufacturer'), dataIndex: 'mfrs'},
        { title: this.$t('purchase.form.columns.ext1'), dataIndex: 'otherField1'},
        { title: this.$t('purchase.form.columns.ext2'), dataIndex: 'otherField2'},
        { title: this.$t('purchase.form.columns.ext3'), dataIndex: 'otherField3'},
        { title: this.$t('purchase.form.columns.stock'), dataIndex: 'stock'},
        { title: this.$t('common.unit'), dataIndex: 'unit'},
        { title: this.$t('purchase.form.columns.serialNumber'), dataIndex: 'snList', width:300},
        { title: this.$t('purchase.form.columns.batchNumber'), dataIndex: 'batchNumber'},
        { title: this.$t('purchase.form.columns.expirationDate'), dataIndex: 'expirationDate'},
        { title: this.$t('purchase.form.columns.sku'), dataIndex: 'sku'},
        { title: this.$t('common.quantity'), dataIndex: 'operNumber'},
        { title: this.$t('purchase.form.columns.unitPrice'), dataIndex: 'unitPrice'},
        { title: this.$t('common.amount'), dataIndex: 'allPrice'},
        { title: this.$t('common.weight'), dataIndex: 'weight'},
        { title: this.$t('common.position'), dataIndex: 'position'},
        { title: this.$t('common.remark'), dataIndex: 'remark'}
      ],
      allocationOutColumns: [
        { title: this.$t('common.depotName'), dataIndex: 'depotName'},
        { title: this.$t('common.barcode'), dataIndex: 'barCode'},
        { title: this.$t('common.name'), dataIndex: 'name'},
        { title: this.$t('common.specification'), dataIndex: 'standard'},
        { title: this.$t('common.model'), dataIndex: 'model'},
        { title: this.$t('material.color'), dataIndex: 'color'},
        { title: this.$t('common.brand'), dataIndex: 'brand'},
        { title: this.$t('material.manufacturer'), dataIndex: 'mfrs'},
        { title: this.$t('purchase.form.columns.ext1'), dataIndex: 'otherField1'},
        { title: this.$t('purchase.form.columns.ext2'), dataIndex: 'otherField2'},
        { title: this.$t('purchase.form.columns.ext3'), dataIndex: 'otherField3'},
        { title: this.$t('purchase.form.columns.stock'), dataIndex: 'stock'},
        { title: this.$t('common.inboundDepot'), dataIndex: 'anotherDepotName'},
        { title: this.$t('common.unit'), dataIndex: 'unit'},
        { title: this.$t('purchase.form.columns.sku'), dataIndex: 'sku'},
        { title: this.$t('common.quantity'), dataIndex: 'operNumber'},
        { title: this.$t('purchase.form.columns.unitPrice'), dataIndex: 'unitPrice'},
        { title: this.$t('common.amount'), dataIndex: 'allPrice'},
        { title: this.$t('common.weight'), dataIndex: 'weight'},
        { title: this.$t('common.position'), dataIndex: 'position'},
        { title: this.$t('common.remark'), dataIndex: 'remark'}
      ],
      assembleColumns: [
        { title: this.$t('common.productType'), dataIndex: 'mType'},
        { title: this.$t('common.depotName'), dataIndex: 'depotName'},
        { title: this.$t('common.barcode'), dataIndex: 'barCode'},
        { title: this.$t('common.name'), dataIndex: 'name'},
        { title: this.$t('common.specification'), dataIndex: 'standard'},
        { title: this.$t('common.model'), dataIndex: 'model'},
        { title: this.$t('material.color'), dataIndex: 'color'},
        { title: this.$t('common.brand'), dataIndex: 'brand'},
        { title: this.$t('material.manufacturer'), dataIndex: 'mfrs'},
        { title: this.$t('purchase.form.columns.ext1'), dataIndex: 'otherField1'},
        { title: this.$t('purchase.form.columns.ext2'), dataIndex: 'otherField2'},
        { title: this.$t('purchase.form.columns.ext3'), dataIndex: 'otherField3'},
        { title: this.$t('purchase.form.columns.stock'), dataIndex: 'stock'},
        { title: this.$t('common.unit'), dataIndex: 'unit'},
        { title: this.$t('purchase.form.columns.sku'), dataIndex: 'sku'},
        { title: this.$t('common.quantity'), dataIndex: 'operNumber'},
        { title: this.$t('purchase.form.columns.unitPrice'), dataIndex: 'unitPrice'},
        { title: this.$t('common.amount'), dataIndex: 'allPrice'},
        { title: this.$t('common.remark'), dataIndex: 'remark'}
      ],
      disassembleColumns: [
        { title: this.$t('common.productType'), dataIndex: 'mType'},
        { title: this.$t('common.depotName'), dataIndex: 'depotName'},
        { title: this.$t('common.barcode'), dataIndex: 'barCode'},
        { title: this.$t('common.name'), dataIndex: 'name'},
        { title: this.$t('common.specification'), dataIndex: 'standard'},
        { title: this.$t('common.model'), dataIndex: 'model'},
        { title: this.$t('material.color'), dataIndex: 'color'},
        { title: this.$t('common.brand'), dataIndex: 'brand'},
        { title: this.$t('material.manufacturer'), dataIndex: 'mfrs'},
        { title: this.$t('purchase.form.columns.ext1'), dataIndex: 'otherField1'},
        { title: this.$t('purchase.form.columns.ext2'), dataIndex: 'otherField2'},
        { title: this.$t('purchase.form.columns.ext3'), dataIndex: 'otherField3'},
        { title: this.$t('purchase.form.columns.stock'), dataIndex: 'stock'},
        { title: this.$t('common.unit'), dataIndex: 'unit'},
        { title: this.$t('purchase.form.columns.sku'), dataIndex: 'sku'},
        { title: this.$t('common.quantity'), dataIndex: 'operNumber'},
        { title: this.$t('purchase.form.columns.unitPrice'), dataIndex: 'unitPrice'},
        { title: this.$t('common.amount'), dataIndex: 'allPrice'},
        { title: this.$t('common.remark'), dataIndex: 'remark'}
      ],
      stockCheckReplayColumns: [
        { title: this.$t('common.depotName'), dataIndex: 'depotName'},
        { title: this.$t('common.barcode'), dataIndex: 'barCode'},
        { title: this.$t('common.name'), dataIndex: 'name'},
        { title: this.$t('common.specification'), dataIndex: 'standard'},
        { title: this.$t('common.model'), dataIndex: 'model'},
        { title: this.$t('common.brand'), dataIndex: 'brand'},
        { title: this.$t('material.manufacturer'), dataIndex: 'mfrs'},
        { title: this.$t('purchase.form.columns.ext1'), dataIndex: 'otherField1'},
        { title: this.$t('purchase.form.columns.ext2'), dataIndex: 'otherField2'},
        { title: this.$t('purchase.form.columns.ext3'), dataIndex: 'otherField3'},
        { title: this.$t('purchase.form.columns.stock'), dataIndex: 'stock'},
        { title: this.$t('common.unit'), dataIndex: 'unit'},
        { title: this.$t('purchase.form.columns.sku'), dataIndex: 'sku'},
        { title: this.$t('common.quantity'), dataIndex: 'operNumber'},
        { title: this.$t('purchase.form.columns.unitPrice'), dataIndex: 'unitPrice'},
        { title: this.$t('common.amount'), dataIndex: 'allPrice'},
        { title: this.$t('common.remark'), dataIndex: 'remark'}
      ],
      quickBtn: {
        retailBack: '',
        purchaseOrder: '',
        purchaseIn: '',
        purchaseBack: '',
        saleOut: '',
        saleBack: ''
      },
      queryParam: {
        beginTime: getPrevMonthFormatDate(3),
        endTime: getFormatDate(),
        createTimeRange: [moment(getPrevMonthFormatDate(3)), moment(getFormatDate())]
      }
    }
  },
  computed: {
    importExcelUrl: function(){
      return `${window._CONFIG['domianURL']}/${this.url.importExcelUrl}`;
    },
    isBatchDelEnabled: function () {
      for (let i = 0; i < this.selectedRowKeys.length; i++) {
        if (!this.selectionRows[i].actionsEnabled.delete) {
          return false;
        }
      }
      return true;
    }
  },
  created() {
    this.initColumnsSetting()
    this.isShowExcel = Vue.ls.get('isShowExcel');
  },
  activated() {
    // 转单弹窗挂在来源列表中，保存成功时只能直接刷新来源列表。
    // 目标单据列表使用 keep-alive 缓存，重新切回标签时统一刷新，避免必须手动刷新页面。
    if(this._hasActivated) {
      this.loadData(1)
    }
    this._hasActivated = true
  },
  methods: {
    loadData(arg) {
      // 重置展开状态
      this.expandedRowKeys = []
      if (arg === 1) {
        this.ipagination.current = 1
      }
      let params = this.getQueryParams() //查询条件
      this.loading = true
      getAction(this.url.list, params).then((res) => {
        if (res.code===200) {
          this.dataSource = res.data.rows
          this.ipagination.total = res.data.total
          this.tableAddTotalRow(this.columns, this.dataSource)
        } else if(res.code===510){
          this.$message.warning(res.data)
        } else {
          this.$message.warning(res.data.message)
        }
        this.loading = false
        this.onClearSelected()
      })
    },
    myHandleAdd() {
      this.$refs.modalForm.action = "add";
      if(this.btnEnableList.indexOf(2)===-1) {
        this.$refs.modalForm.isCanCheck = false
      }
      this.handleAdd();
    },
    myHandleCopyAdd(record) {
      this.$refs.modalForm.action = "copyAdd";
      if(this.btnEnableList.indexOf(2)===-1) {
        this.$refs.modalForm.isCanCheck = false
      }
      //复制单据的时候需要移除关联单据的相关信息，并避免修改列表中的原记录
      const copyRecord = Object.assign({}, record, {
        linkNumber: '',
        linkApply: '',
        billType: '',
        deposit: ''
      })
      this.$refs.modalForm.edit(copyRecord);
      this.$refs.modalForm.title = this.$t('common.copy') + this.$t('common.add');
      this.$refs.modalForm.disableSubmit = false;
      //开启明细的编辑模式
      this.$refs.modalForm.rowCanEdit = true
      let columnIndex = copyRecord.subType === '组装单' || copyRecord.subType === '拆卸单'?2:1
      this.$refs.modalForm.materialTable.columns[columnIndex].type = FormTypes.popupJsh
    },
    myHandleEdit(record) {
      if(record.status === '0') {
        this.$refs.modalForm.action = "edit";
        if(this.btnEnableList.indexOf(2)===-1) {
          this.$refs.modalForm.isCanCheck = false
        }
        //查询单条单据信息
        findBillDetailByNumber({ number: record.number }).then((res) => {
          if (res && res.code === 200) {
            let item = res.data
            this.handleEdit(item)
          }
        })
      } else {
        this.$message.warning(this.$t('common.onlyPendingCanEdit'))
      }
    },
    myHandleDelete(record) {
      if(record.status === '0') {
        this.handleDelete(record.id)
      } else {
        this.$message.warning(this.$t('common.onlyPendingCanDelete'))
      }
    },
    myHandleDetail(record, type, prefixNo) {
      if(this.btnEnableList.indexOf(7)===-1) {
        this.$refs.modalDetail.isCanBackCheck = false
      }
      this.handleDetail(record, type, prefixNo);
    },
    batchForceClose: function () {
      if(!this.url.forceCloseBatch){
        this.$message.error(this.$t('common.setUrlError', { field: 'forceCloseBatch' }))
        return
      }
      if (this.selectedRowKeys.length <= 0) {
        this.$message.warning(this.$t('common.selectRecord'))
      } else {
        let ids = "";
        for (let a = 0; a < this.selectedRowKeys.length; a++) {
          ids += this.selectedRowKeys[a] + ","
        }
        let that = this
        this.$confirm({
          title: this.$t('common.confirm') + this.$t('common.forceClose'),
          content: this.$t('common.confirmOperateSelected'),
          onOk: function () {
            that.loading = true
            postAction(that.url.forceCloseBatch, {ids: ids}).then((res) => {
              if(res.code === 200){
                that.loadData()
              } else {
                that.$message.warning(res.data.message)
              }
            }).finally(() => {
              that.loading = false
            });
          }
        });
      }
    },
    batchForceClosePurchase: function () {
      if(!this.url.forceClosePurchaseBatch){
        this.$message.error(this.$t('common.setUrlError', { field: 'forceClosePurchaseBatch' }))
        return
      }
      if (this.selectedRowKeys.length <= 0) {
        this.$message.warning(this.$t('common.selectRecord'))
      } else {
        let ids = "";
        for (let a = 0; a < this.selectedRowKeys.length; a++) {
          ids += this.selectedRowKeys[a] + ","
        }
        let that = this
        this.$confirm({
          title: this.$t('common.confirm') + this.$t('common.forceClose') + '(' + this.$t('purchase.transferToOrder') + ')',
          content: this.$t('common.confirmOperateSelected'),
          onOk: function () {
            that.loading = true
            postAction(that.url.forceClosePurchaseBatch, {ids: ids}).then((res) => {
              if(res.code === 200){
                that.loadData()
              } else {
                that.$message.warning(res.data.message)
              }
            }).finally(() => {
              that.loading = false
            });
          }
        });
      }
    },
    //批量修正最终欠款
    batchSetLastDebt() {
      if (this.selectedRowKeys.length <= 0) {
        this.$message.warning(this.$t('common.selectRecord'))
      } else {
        let ids = "";
        for (let a = 0; a < this.selectedRowKeys.length; a++) {
          ids += this.selectedRowKeys[a] + ","
        }
        let that = this
        this.$confirm({
          title: this.$t('common.confirm') + this.$t('sales.correctDebt'),
          content: this.$t('common.confirmOperateSelected'),
          onOk: function () {
            that.loading = true
            postAction(that.url.batchSetLastDebtUrl, {ids: ids}).then((res) => {
              if(res.code === 200){
                that.loadData()
                that.$message.success(that.$t('common.operateSuccess'))
              } else {
                that.$message.warning(res.data.message)
              }
            }).finally(() => {
              that.loading = false
            });
          }
        });
      }
    },
    handleApprove(record) {
      this.$refs.modalForm.action = "approve";
      this.$refs.modalForm.edit(record);
      this.$refs.modalForm.title = this.$t('common.audit');
    },
    searchReset() {
      this.queryParam = {
        type: this.queryParam.type,
        subType: this.queryParam.subType,
        beginTime: getPrevMonthFormatDate(3),
        endTime: getFormatDate(),
        createTimeRange: [moment(getPrevMonthFormatDate(3)), moment(getFormatDate())]
      }
      this.loadData(1)
    },
    onDateChange: function (value, dateString) {
      this.queryParam.beginTime=dateString[0]
      this.queryParam.endTime=dateString[1]
      if(dateString[0] && dateString[1]) {
        this.queryParam.createTimeRange = [moment(dateString[0]), moment(dateString[1])]
      }
    },
    onDateOk(value) {
      console.log(value);
    },
    initSystemConfig() {
      getCurrentSystemConfig().then((res) => {
        if(res.code === 200 && res.data){
          let multiBillType = res.data.multiBillType
          let multiLevelApprovalFlag = res.data.multiLevelApprovalFlag
          this.checkFlag = getCheckFlag(multiBillType, multiLevelApprovalFlag, this.prefixNo)
          this.purchaseBySaleFlag = res.data.purchaseBySaleFlag==='1'?true:false
          this.inOutManageFlag = res.data.inOutManageFlag==='1'?true:false
          this.materialPriceTaxFlag = res.data.materialPriceTaxFlag==='1'?true:false
        }
      })
      getPlatformConfigByKey({ "platformKey": "bill_excel_url" }).then((res) => {
        if (res && res.code === 200) {
          if(res.data.platformValue) {
            this.billExcelUrl = res.data.platformValue
          }
        }
      })
    },
    initSupplier() {
      let that = this;
      findBySelectSup({limit:1}).then((res)=>{
        if(res) {
          that.supList = res;
        }
      });
    },
    initCustomer() {
      let that = this;
      findBySelectCus({limit:1}).then((res)=>{
        if(res) {
          that.cusList = res;
        }
      });
    },
    initRetail() {
      let that = this;
      findBySelectRetail({limit:1}).then((res)=>{
        if(res) {
          that.retailList = res;
        }
      });
    },
    initSalesman() {
      let that = this;
      getPersonByNumType({type:1}).then((res)=>{
        if(res) {
          that.salesManList = res;
        }
      });
    },
    getDepotData() {
      getAction('/depot/findDepotByCurrentUser').then((res)=>{
        if(res.code === 200){
          this.depotList = res.data;
        }else{
          this.$message.info(res.data);
        }
      })
    },
    initUser() {
      getUserList({}).then((res)=>{
        if(res) {
          this.userList = res;
        }
      });
    },
    initAccount() {
      getAccount({}).then((res)=>{
        if(res && res.code === 200) {
          this.accountList = res.data.accountList
        }
      })
    },
    initWaitBillCount(type, subType, status) {
      waitBillCount({search: {
          type: type, subType: subType, status: status
        }}).then((res)=>{
        if(res && res.code === 200) {
          this.waitTotal = res.data.total
        }
      })
    },
    //加载初始化列
    initColumnsSetting(){
      let columnsStr = Vue.ls.get(this.prefixNo)
      if(columnsStr && columnsStr.indexOf(',')>-1) {
        this.settingDataIndex = columnsStr.split(',')
      } else {
        this.settingDataIndex = this.defDataIndex
      }
      this.columns = this.defColumns.filter(item => {
        if(this.purchaseBySaleFlag) {
          //以销定购-开启
          return this.settingDataIndex.includes(item.dataIndex)
        } else {
          //以销定购-关闭
          if(this.prefixNo === 'CGDD') {
            //采购订单只显示除了关联订单之外的列
            if(item.dataIndex!=='linkNumber') {
              return this.settingDataIndex.includes(item.dataIndex)
            }
          } else {
            return this.settingDataIndex.includes(item.dataIndex)
          }
        }
      })
    },
    //加载快捷按钮：转入库、转出库等
    initQuickBtn() {
      // 登录后按钮权限按用户保存；保留旧键以兼容历史缓存。
      let btnStrList = Vue.ls.get('winBtnStrList_' + Vue.ls.get(USER_ID)) || Vue.ls.get('winBtnStrList')
      if (btnStrList) {
        for (let i = 0; i < btnStrList.length; i++) {
          if (btnStrList[i].btnStr) {
            this.quickBtn.retailBack = btnStrList[i].url === '/bill/retail_back'?btnStrList[i].btnStr:this.quickBtn.retailBack
            this.quickBtn.purchaseOrder = btnStrList[i].url === '/bill/purchase_order'?btnStrList[i].btnStr:this.quickBtn.purchaseOrder
            this.quickBtn.purchaseIn = btnStrList[i].url === '/bill/purchase_in'?btnStrList[i].btnStr:this.quickBtn.purchaseIn
            this.quickBtn.purchaseBack = btnStrList[i].url === '/bill/purchase_back'?btnStrList[i].btnStr:this.quickBtn.purchaseBack
            this.quickBtn.saleOut = btnStrList[i].url === '/bill/sale_out'?btnStrList[i].btnStr:this.quickBtn.saleOut
            this.quickBtn.saleBack = btnStrList[i].url === '/bill/sale_back'?btnStrList[i].btnStr:this.quickBtn.saleBack
          }
        }
      }
    },
    handleSearchSupplier(value) {
      let that = this
      if(this.setTimeFlag != null){
        clearTimeout(this.setTimeFlag);
      }
      this.setTimeFlag = setTimeout(()=>{
        findBySelectSup({key: value, limit:1}).then((res) => {
          if(res) {
            that.supList = res;
          }
        })
      },500)
    },
    handleSearchCustomer(value) {
      let that = this
      if(this.setTimeFlag != null){
        clearTimeout(this.setTimeFlag);
      }
      this.setTimeFlag = setTimeout(()=>{
        findBySelectCus({key: value, limit:1}).then((res) => {
          if(res) {
            that.cusList = res;
          }
        })
      },500)
    },
    handleSearchRetail(value) {
      let that = this
      if(this.setTimeFlag != null){
        clearTimeout(this.setTimeFlag);
      }
      this.setTimeFlag = setTimeout(()=>{
        findBySelectRetail({key: value, limit:1}).then((res) => {
          if(res) {
            that.retailList = res;
          }
        })
      },500)
    },
    handleQuickEdit() {
      if (this.selectedRowKeys.length === 0) {
        this.$message.warning(this.$t('common.selectRecord'))
        return
      }
      if (this.selectedRowKeys.length > 1) {
        this.$message.warning(this.$t('common.selectOnlyOne'))
        return
      }
      const record = this.dataSource.find(item => item.id === this.selectedRowKeys[0])
      if (record) {
        this.$refs.quickEditModal.show(record)
      }
    },
    getDepotByCurrentUser() {
      getAction('/depot/findDepotByCurrentUser').then((res) => {
        if (res.code === 200) {
          if (res.data.length === 1) {
            this.defaultDepotId = res.data[0].id+''
          } else {
            for (let i = 0; i < res.data.length; i++) {
              if(res.data[i].isDefault){
                this.defaultDepotId = res.data[i].id+''
              }
            }
          }
        }
      })
    },
    //跳转到下一个单据页面
    transferBill(type, quickBtnStr) {
      if (this.selectedRowKeys.length <= 0) {
        this.$message.warning(this.$t('common.selectRecord'))
      } else if (this.selectedRowKeys.length > 1) {
        this.$message.warning(this.$t('common.selectOnlyOne'))
      } else {
        let info = this.selectionRows[0]
        if(info.status === '1' || info.status === '3') {
          let linkType = 'basic'
          if(type === '转采购订单-以销定购') {
            linkType = 'purchase'
          } else {
            linkType = 'basic'
          }
          let param = {
            headerId: info.id,
            mpList : '',
            linkType: linkType
          }
          getAction('/depotItem/getDetailList', param).then((res) => {
            if (res.code === 200) {
              // 快捷转单不经过 LinkBillList，必须在打开目标单据前排除已全部转完的明细。
              // 否则目标弹窗会先创建，随后在 linkBillListOk 中因剩余数量为 0 而变为空白。
              const availableDetails = res.data.rows.filter(item =>
                Number(item.preNumber || 0) > Number(item.finishNumber || 0)
              )
              if (availableDetails.length === 0) {
                this.$message.warning(this.$t('purchase.validation.linkedBillCompleted'))
                return
              }
              let deposit = info.changeAmount - info.finishDeposit
              let transferParam = {
                list: availableDetails,
                number: info.number,
                organId: info.organId,
                discount: info.discount,
                deposit: deposit,
                remark: info.remark,
                accountId: info.accountId,
                salesMan: info.salesMan,
                payType: info.payType
              }
              if(type === '转采购订单-以销定购') {
                let list = transferParam.list
                list.forEach(item => {
                  item.finishNumber = item.finishPurchaseNumber
                })
                this.$refs.transferPurchaseModalForm.action = "add"
                this.$refs.transferPurchaseModalForm.transferParam = transferParam
                this.$refs.transferPurchaseModalForm.defaultDepotId = this.defaultDepotId
                this.$refs.transferPurchaseModalForm.add()
                this.$refs.transferPurchaseModalForm.title = type
                if(quickBtnStr.indexOf(2)===-1) {
                  this.$refs.transferPurchaseModalForm.isCanCheck = false
                }
              } else {
                this.$refs.transferModalForm.action = "add"
                this.$refs.transferModalForm.transferParam = transferParam
                this.$refs.transferModalForm.defaultDepotId = this.defaultDepotId
                this.$refs.transferModalForm.materialPriceTaxFlag = this.materialPriceTaxFlag
                this.$refs.transferModalForm.add()
                this.$refs.transferModalForm.title = type
                if(quickBtnStr.indexOf(2)===-1) {
                  this.$refs.transferModalForm.isCanCheck = false
                }
              }
            }
          })
        } else {
          this.$message.warning(this.$t('common.operateFailed'))
        }
      }
    },
    //列设置更改事件
    onColChange (checkedValues) {
      this.columns = this.defColumns.filter(item => {
        return checkedValues.includes(item.dataIndex)
      })
      let columnsStr = checkedValues.join()
      Vue.ls.set(this.prefixNo, columnsStr)
    },
    //恢复默认
    handleRestDefault() {
      Vue.ls.remove(this.prefixNo)
      this.initColumnsSetting()
    },
    //导出单据
    handleExport() {
      let search = this.getQueryParams().search
      this.$refs.billExcelIframe.show(this.model, this.billExcelUrl + '?search=' + search + '&type=1', 150)
      this.$refs.billExcelIframe.title = this.$t('common.confirm') + this.$t('common.export')
    },
    // 展开/折叠行
    onExpand(expanded, record) {
      if (expanded) {
        this.expandedRowKeys = [...new Set([...this.expandedRowKeys, record.id])]
        let showType = 'basic'
        if(record.subType === '采购' || record.subType === '采购退货' || record.subType === '销售' || record.subType === '销售退货') {
          if (record.status === '3') {
            showType = 'other'
          }
        } else {
          if (record.status === '3') {
            showType = 'basic'
          } else if (record.purchaseStatus === '3') {
            showType = 'purchase'
          }
        }
        let params = {
          headerId: record.id,
          mpList: getMpListShort(Vue.ls.get('materialPropertyList')),  //扩展属性
          linkType: showType,
          isReadOnly: '0'
        }
        let url = '/depotItem/getDetailList'
        this.requestSubTableData(record, url, params)
      } else {
        this.expandedRowKeys = this.expandedRowKeys.filter(key => key !== record.id)
      }
    },
    requestSubTableData(record, url, params, success) {
      record.loading = true
      getAction(url, params).then(res => {
        if(res && res.code === 200){
          record.childrens = res.data.rows
          this.initSetting(record, record.childrens)
          record.loading = false
          typeof success === 'function' ? success(res) : ''
        }
      }).finally(() => {
        record.loading = false
      })
    },
    initSetting(record, ds) {
      if (this.prefixNo === 'LSCK') {
        this.defDetailColumns = this.retailOutColumns
      } else if (this.prefixNo === 'LSTH') {
        this.defDetailColumns = this.retailBackColumns
      } else if (this.prefixNo === 'QGD') {
        this.defDetailColumns = this.purchaseApplyColumns
      } else if (this.prefixNo === 'CGDD') {
        this.defDetailColumns = this.purchaseOrderColumns
      } else if (this.prefixNo === 'CGRK') {
        this.defDetailColumns = this.purchaseInColumns
      } else if (this.prefixNo === 'CGTH') {
        this.defDetailColumns = this.purchaseBackColumns
      } else if (this.prefixNo === 'XSDD') {
        this.defDetailColumns = this.saleOrderColumns
      } else if (this.prefixNo === 'XSCK') {
        this.defDetailColumns = this.saleOutColumns
      } else if (this.prefixNo === 'XSTH') {
        this.defDetailColumns = this.saleBackColumns
      } else if (this.prefixNo === 'QTRK') {
        this.defDetailColumns = this.otherInColumns
      } else if (this.prefixNo === 'QTCK') {
        this.defDetailColumns = this.otherOutColumns
      } else if (this.prefixNo === 'DBCK') {
        this.defDetailColumns = this.allocationOutColumns
      } else if (this.prefixNo === 'ZZD') {
        this.defDetailColumns = this.assembleColumns
      } else if (this.prefixNo === 'CXD') {
        this.defDetailColumns = this.disassembleColumns
      } else if (this.prefixNo === 'PDFP') {
        this.defDetailColumns = this.stockCheckReplayColumns
      }
      //动态替换扩展字段
      this.handleChangeOtherField()
      //判断序列号、批号、有效期、多属性、重量、仓位货架、扩展、备注等是否有值
      let needAddkeywords = []
      for (let i = 0; i < ds.length; i++) {
        if(ds[i].snList) {
          needAddkeywords.push('snList')
        }
        if(ds[i].batchNumber) {
          needAddkeywords.push('batchNumber')
        }
        if(ds[i].expirationDate) {
          needAddkeywords.push('expirationDate')
        }
        if(ds[i].sku) {
          needAddkeywords.push('sku')
        }
        if(ds[i].weight) {
          needAddkeywords.push('weight')
        }
        if(ds[i].position) {
          needAddkeywords.push('position')
        }
        if(ds[i].brand) {
          needAddkeywords.push('brand')
        }
        if(ds[i].mfrs) {
          needAddkeywords.push('mfrs')
        }
        if(ds[i].otherField1) {
          needAddkeywords.push('otherField1')
        }
        if(ds[i].otherField2) {
          needAddkeywords.push('otherField2')
        }
        if(ds[i].otherField3) {
          needAddkeywords.push('otherField3')
        }
        if(ds[i].taxRate) {
          needAddkeywords.push('taxRate')
        }
        if(ds[i].remark) {
          needAddkeywords.push('remark')
        }
      }
      let currentCol = []
      if(record.status === '3' || record.purchaseStatus === '3') {
        //部分采购|部分销售的时候显示全部列
        for(let i=0; i<this.defDetailColumns.length; i++){
          currentCol.push(this.defDetailColumns[i])
        }
        this.detailColumns = currentCol
      } else {
        for(let i=0; i<this.defDetailColumns.length; i++){
          //移除列
          let needRemoveKeywords = ['finishNumber','finishPurchaseNumber','snList','batchNumber','expirationDate','sku','weight','position',
            'brand','mfrs','otherField1','otherField2','otherField3','taxRate','remark']
          if(needRemoveKeywords.indexOf(this.defDetailColumns[i].dataIndex)===-1) {
            let info = {}
            info.title = this.defDetailColumns[i].title
            info.dataIndex = this.defDetailColumns[i].dataIndex
            if(this.defDetailColumns[i].width) {
              info.width = this.defDetailColumns[i].width
            }
            if(this.defDetailColumns[i].dataIndex === 'barCode') {
              info.scopedSlots = { customRender: 'customBarCode' }
            }
            currentCol.push(info)
          }
          //添加有数据的列
          if(needAddkeywords.indexOf(this.defDetailColumns[i].dataIndex)>-1) {
            let info = {}
            info.title = this.defDetailColumns[i].title
            info.dataIndex = this.defDetailColumns[i].dataIndex
            if(this.defDetailColumns[i].width) {
              info.width = this.defDetailColumns[i].width
            }
            currentCol.push(info)
          }
        }
        this.detailColumns = currentCol
      }
    },
    //动态替换扩展字段
    handleChangeOtherField() {
      let mpStr = getMpListShort(Vue.ls.get('materialPropertyList'))
      if(mpStr) {
        let mpArr = mpStr.split(',')
        if(mpArr.length ===3) {
          for (let i = 0; i < this.defDetailColumns.length; i++) {
            if(this.defDetailColumns[i].dataIndex === 'otherField1') {
              this.defDetailColumns[i].title = mpArr[0]
            }
            if(this.defDetailColumns[i].dataIndex === 'otherField2') {
              this.defDetailColumns[i].title = mpArr[1]
            }
            if(this.defDetailColumns[i].dataIndex === 'otherField3') {
              this.defDetailColumns[i].title = mpArr[2]
            }
          }
        }
      }
    }
  }
}
