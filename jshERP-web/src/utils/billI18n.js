const billTypeKeys = {
  '零售出库': 'menu.retailOut',
  '零售退货入库': 'menu.retailBack',
  '请购单': 'menu.purchaseApply',
  '采购订单': 'menu.purchaseOrder',
  '采购入库': 'menu.purchaseInbound',
  '采购退货出库': 'menu.purchaseReturn',
  '销售订单': 'menu.saleOrder',
  '销售出库': 'menu.saleOut',
  '销售退货入库': 'menu.saleBack',
  '其它入库': 'menu.otherInbound',
  '其它出库': 'menu.otherOutbound',
  '调拨出库': 'menu.transferOutbound',
  '组装单': 'menu.assembly',
  '拆卸单': 'menu.disassembly'
}

// Backend bill types remain Chinese identifiers; only their display labels vary by locale.
export function getBillTypeLabel (vm, billType) {
  const key = billTypeKeys[billType]
  return key ? vm.$t(key) : billType
}

export function getPaymentTypeLabel (vm, paymentType) {
  const labels = { '现付': 'retail.cashPayment', '预付款': 'retail.prepaidPayment' }
  return labels[paymentType] ? vm.$t(labels[paymentType]) : paymentType
}
