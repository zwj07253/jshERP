import Vue from 'vue'
import VueI18n from 'vue-i18n'
import zhCN from './zh-CN'
import en from './en'

Vue.use(VueI18n)

const messages = { 'zh-CN': zhCN, en }

// Some screens were migrated in batches and still use earlier key names. Keep
// these aliases here so every existing call resolves in both languages while
// the screen code is gradually consolidated on the canonical names.
const aliases = {
  'purchase.finishPurchased': 'purchase.form.finishPurchased', 'purchase.finishInbound': 'purchase.form.finishInbound', 'sales.finishNumber': 'sales.form.finishNumber', 'common.material': 'common.materialInfo',
  'sale.completedSales': 'sales.completedSales', 'sale.partialSales': 'sales.partialSales', 'bill.historyBills': 'purchase.historyBills', 'bill.allDetailsConverted': 'purchase.allDetailsConverted', 'bill.multiAccountNeedTwo': 'purchase.multiAccountNeedTwo', 'bill.multiAccountNoDuplicate': 'purchase.multiAccountNoDuplicate', 'bill.accountAmountMismatch': 'purchase.accountAmountMismatch', 'bill.fillSettleAmount': 'purchase.fillSettleAmount', 'bill.quickEditRemark': 'purchase.quickEditRemark', 'bill.allDetailsLinked': 'purchase.allDetailsLinked',
  'purchase.cancel': 'common.cancel', 'purchase.saveAndApprove': 'common.saveAndApprove', 'purchase.save': 'common.save', 'purchase.form.selectPurchaseOrder': 'purchase.form.selectLinkedOrder', 'common.selectLinkedOrder': 'purchase.selectLinkedOrder', 'purchase.forceClose': 'common.forceClose', 'dashboard.amount': 'common.amount', 'common.pleaseInput': 'common.pleaseEnter', 'report.thisMonthNetAmount': 'report.monthNetAmount', 'report.accountStatsQuery': 'report.accountStats', 'common.materialInfoPlaceholder': 'common.enterMaterial', 'common.selectDept': 'report.selectDept', 'common.selectProductCategory': 'report.selectProductCategory',
  'common.barCode': 'common.barcode', 'common.spec': 'common.specification', 'common.color': 'material.color', 'common.manufacturer': 'material.manufacturer', 'common.materialSearchPlaceholder': 'common.enterMaterial', 'report.selectCategory': 'report.productCategory', 'common.materialName': 'common.name', 'common.sku': 'purchase.form.columns.sku', 'common.unitPrice': 'purchase.form.columns.unitPrice', 'common.taxAmount': 'purchase.form.columns.taxAmount', 'common.inboundDate': 'report.inboundDate', 'report.materialParamPlaceholder': 'common.enterMaterial', 'common.itemsPerPage': 'report.itemsPerPage',
  'report.barCode': 'common.barcode', 'report.name': 'common.name', 'report.spec': 'common.specification', 'report.model': 'common.model', 'report.color': 'material.color', 'report.brand': 'common.brand', 'report.manufacturer': 'material.manufacturer', 'report.unit': 'common.unit', 'common.selectCategory': 'report.productCategory', 'common.materialStandard': 'common.specification', 'common.materialModel': 'common.model', 'common.materialColor': 'material.color', 'common.materialBrand': 'common.brand', 'common.materialMfrs': 'material.manufacturer', 'common.unitName': 'common.unit', 'common.costPrice': 'report.costPrice',
  'report.prevStockQty': 'report.previousBalance', 'report.inQty': 'report.inboundQty', 'report.outQty': 'report.outboundQty', 'report.currentStockQty': 'report.currentBalanceQty', 'report.positionPlaceholder': 'common.position', 'report.productStockQuery': 'report.productStock', 'common.number': 'common.serialNo', 'report.productStockFlowQuery': 'report.productStockFlow', 'common.outboundDate': 'report.outboundDate', 'common.materialInputPlaceholder': 'common.enterMaterial', 'common.paginationTotal': 'common.pagedTotal',
  'report.materialName': 'common.name', 'report.materialStandard': 'common.specification', 'report.materialModel': 'common.model', 'report.materialColor': 'material.color', 'report.materialBrand': 'common.brand', 'report.materialMfrs': 'material.manufacturer', 'report.materialUnit': 'common.unit', 'report.colBarCode': 'common.barcode', 'report.colName': 'common.name', 'report.colSpec': 'common.specification', 'report.colModel': 'common.model', 'report.colColor': 'material.color', 'report.colBrand': 'common.brand', 'report.colManufacturer': 'material.manufacturer', 'report.colUnit': 'common.unit', 'report.colSalesQty': 'report.salesQty', 'report.colSalesAmount': 'report.salesAmount', 'report.colReturnQty': 'report.returnQty', 'report.colReturnAmount': 'report.returnAmount', 'report.colActualSalesAmount': 'report.actualSalesAmount', 'report.specification': 'common.specification', 'report.lowSafeStock': 'report.minSafetyStock', 'report.highSafeStock': 'report.maxSafetyStock', 'report.suggestedInbound': 'report.suggestInbound', 'report.suggestedOutbound': 'report.suggestOutbound', 'financial.correctPrepaid': 'common.correctPrepaid'
}

const aliasText = {
  'common.enterName': { 'zh-CN': '请输入名称', en: 'Please enter a name' }, 'common.items': { 'zh-CN': '条', en: 'items' }, 'report.inboundSummary': { 'zh-CN': '入库汇总', en: 'Inbound summary' },
  'purchase.form.supplierIntro': { 'zh-CN': '请选择供应商', en: 'Please select a supplier' }, 'purchase.form.documentNumberIntro': { 'zh-CN': '请输入单据编号', en: 'Please enter the document number' }, 'purchase.form.linkedRequisitionIntro': { 'zh-CN': '请选择关联请购单', en: 'Please select the linked requisition' }, 'purchase.form.linkedOrderIntro': { 'zh-CN': '请选择关联订单', en: 'Please select the linked order' }, 'common.scanEntryIntro': { 'zh-CN': '扫码录入商品', en: 'Scan to enter products' }, 'purchase.form.discountIntro': { 'zh-CN': '请输入优惠率', en: 'Please enter the discount rate' }, 'purchase.form.discountMoneyIntro': { 'zh-CN': '请输入优惠金额', en: 'Please enter the discount amount' }, 'purchase.form.discountLastMoneyIntro': { 'zh-CN': '请输入优惠后金额', en: 'Please enter the amount after discount' }, 'purchase.form.settlementAccountIntro': { 'zh-CN': '请选择结算账户', en: 'Please select the settlement account' }, 'purchase.form.payDepositIntro': { 'zh-CN': '请输入支付订金', en: 'Please enter the deposit payment' }, 'common.attachmentIntro': { 'zh-CN': '上传附件', en: 'Upload an attachment' }, 'purchase.validation.linkBillNotFound': { 'zh-CN': '未找到关联单据', en: 'Linked document was not found' }
}

function getValue (source, key) { return key.split('.').reduce((value, part) => value && value[part], source) }
function setValue (target, key, value) { const parts = key.split('.'); const last = parts.pop(); const parent = parts.reduce((value, part) => (value[part] || (value[part] = {})), target); parent[last] = value }

Object.keys(messages).forEach(locale => {
  Object.keys(aliases).forEach(key => setValue(messages[locale], key, getValue(messages[locale], aliases[key])))
  Object.keys(aliasText).forEach(key => setValue(messages[locale], key, aliasText[key][locale]))
})

const localeKey = 'jshERP_locale'
const locale = localStorage.getItem(localeKey) || 'zh-CN'

export const i18n = new VueI18n({
  locale,
  fallbackLocale: 'zh-CN',
  messages
})

export function setLocale (value) {
  i18n.locale = value
  localStorage.setItem(localeKey, value)
}
