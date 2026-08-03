export function getNotificationTitle (record, i18n) {
  const titleKey = getStockWarningTitleKey(record)
  if (titleKey && i18n.te(titleKey)) {
    return i18n.t(titleKey)
  }
  return record && record.msgTitle ? record.msgTitle : ''
}

export function getNotificationContent (record, i18n) {
  if (!record || record.type !== 'stock_warning') {
    return record && record.msgContent ? record.msgContent : ''
  }
  const message = getStructuredStockWarningMessage(record.msgContent) || getLegacyStockWarningMessage(record.msgContent)
  if (message && i18n.te(message.key)) {
    return i18n.t(message.key, message.params || {})
  }
  return record.msgContent || ''
}

export function isStructuredStockWarning (record) {
  return Boolean(record && record.type === 'stock_warning' && getStructuredStockWarningMessage(record.msgContent))
}

function getStockWarningTitleKey (record) {
  if (!record || record.type !== 'stock_warning') {
    return ''
  }
  if (record.msgTitle === 'stockWarning.low.title' || record.msgTitle === '库存预警-低库存') {
    return 'stockWarning.low.title'
  }
  if (record.msgTitle === 'stockWarning.high.title' || record.msgTitle === '库存预警-高库存') {
    return 'stockWarning.high.title'
  }
  return ''
}

function getLegacyStockWarningMessage (content) {
  const low = /^商品【(.*?)】在仓库【(.*?)】的当前库存为 ([^，]*)，低于最低安全库存 (.*)。$/.exec(content)
  if (low) {
    return toMessage('stockWarning.low.content', low)
  }
  const high = /^商品【(.*?)】在仓库【(.*?)】的当前库存为 ([^，]*)，高于最高安全库存 (.*)。$/.exec(content)
  return high ? toMessage('stockWarning.high.content', high) : null
}

function getStructuredStockWarningMessage (content) {
  try {
    const message = JSON.parse(content)
    return message && message.key && message.params ? message : null
  } catch (e) {
    return null
  }
}

function toMessage (key, values) {
  return {
    key,
    params: {
      materialName: values[1],
      depotName: values[2],
      currentStock: values[3],
      threshold: values[4]
    }
  }
}
