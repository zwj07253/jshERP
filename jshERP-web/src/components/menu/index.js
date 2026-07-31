import Menu from 'ant-design-vue/es/menu'
import Icon from 'ant-design-vue/es/icon'
import { i18n } from '@/locales'

export const menuNameMap = {
  '首页': 'menu.home',
  '零售管理': 'menu.retailManagement', '零售出库': 'menu.retailOut', '零售出库列表': 'menu.retailOutList', '零售退货': 'menu.retailBack', '零售退货列表': 'menu.retailBackList',
  '采购管理': 'menu.purchaseManagement', '请购单': 'menu.purchaseApply', '请购单列表': 'menu.purchaseApplyList', '采购订单': 'menu.purchaseOrder', '采购订单列表': 'menu.purchaseOrderList', '采购入库': 'menu.purchaseInbound', '采购入库列表': 'menu.purchaseInList', '采购退货': 'menu.purchaseReturn', '采购退货列表': 'menu.purchaseBackList',
  '销售管理': 'menu.salesManagement', '销售出库': 'menu.saleOut', '销售出库列表': 'menu.saleOutList', '销售订单': 'menu.saleOrder', '销售订单列表': 'menu.saleOrderList', '销售退货': 'menu.saleBack', '销售退货列表': 'menu.saleBackList',
  '仓库管理': 'menu.warehouseManagement', '仓库列表': 'menu.depotManagement', '调拨管理': 'menu.allocationManagement', '调拨单列表': 'menu.allocationList', '库存管理': 'menu.stockManagement',
  '其它入库': 'menu.otherInbound', '其它出库': 'menu.otherOutbound', '调拨出库': 'menu.transferOutbound', '组装单': 'menu.assembly', '拆卸单': 'menu.disassembly',
  '财务管理': 'menu.financialManagement', '收付款': 'menu.moneyManagement', '预付款': 'menu.advanceIn', '预付款列表': 'menu.advanceInList', '收款': 'menu.moneyIn', '收款单列表': 'menu.moneyInList', '付款': 'menu.moneyOut', '付款单列表': 'menu.moneyOutList',
  '收入单': 'menu.incomeBill', '支出单': 'menu.expenseBill', '收款单': 'menu.receiptBill', '付款单': 'menu.paymentBill', '转账单': 'menu.transferBill', '收预付款': 'menu.advanceReceipt',
  '报表查询': 'menu.reportQuery', '采购报表': 'menu.buyInReport', '销售报表': 'menu.saleOutReport', '零售报表': 'menu.retailOutReport', '商品库存': 'menu.materialStock', '出入库报表': 'menu.inOutStockReport', '库存预警': 'menu.stockWarning', '账户报表': 'menu.accountReport', '历史财务': 'menu.historyFinancial',
  '账户统计': 'menu.accountStats', '零售统计': 'menu.retailStats', '采购统计': 'menu.purchaseStats', '销售统计': 'menu.salesStats', '入库明细': 'menu.inboundDetail', '出库明细': 'menu.outboundDetail', '调拨明细': 'menu.transferDetail', '入库汇总': 'menu.inboundSummary', '出库汇总': 'menu.outboundSummary', '进销存统计': 'menu.inventoryStats', '客户对账': 'menu.customerReconciliation', '供应商对账': 'menu.vendorReconciliation',
  '商品管理': 'menu.productManagement', '商品列表': 'menu.materialManagement', '商品类别': 'menu.materialCategory', '序列号': 'menu.serialNumber', '组合商品': 'menu.combination',
  '商品信息': 'menu.productInfo', '多单位': 'menu.multiUnit', '多属性': 'menu.multiProperty',
  '基础资料': 'menu.basicData', '供应商列表': 'menu.vendorManagement', '客户列表': 'menu.customerManagement', '会员列表': 'menu.memberManagement', '单位列表': 'menu.unitManagement', '结算账户': 'menu.accountManagement', '收支项目': 'menu.inOutItemManagement', '系统配置': 'menu.systemConfig',
  '供应商信息': 'menu.vendorInfo', '客户信息': 'menu.customerInfo', '会员信息': 'menu.memberInfo', '仓库信息': 'menu.depotInfo', '收入/支出': 'menu.incomeExpense', '账户列表': 'menu.accountList', '经手人管理': 'menu.personManagement',
  '系统管理': 'menu.systemManagement', '角色管理': 'menu.roleManagement', '用户列表': 'menu.userList', '菜单功能': 'menu.menuFunction', '平台配置': 'menu.platformConfig', '插件管理': 'menu.plugin', '消息管理': 'menu.message',
  '用户管理': 'menu.userManagement', '部门管理': 'menu.departmentManagement', '日志管理': 'menu.logManagement', '商品属性': 'menu.productProperty', 'AI模型配置': 'menu.aiModelConfig',
  '期初建账': 'menu.initialAccount'
}

function translateMenuName(text) {
  const key = menuNameMap[text]
  return key ? i18n.t(key) : text
}

const { Item, SubMenu } = Menu

export default {
  name: 'SMenu',
  props: {
    menu: {
      type: Array,
      required: true
    },
    theme: {
      type: String,
      required: false,
      default: 'dark'
    },
    mode: {
      type: String,
      required: false,
      default: 'inline'
    },
    collapsed: {
      type: Boolean,
      required: false,
      default: false
    }
  },
  data () {
    return {
      openKeys: [],
      selectedKeys: [],
      cachedOpenKeys: []
    }
  },
  computed: {
    rootSubmenuKeys: vm => {
      const keys = []
      vm.menu.forEach(item => keys.push(item.url))
      return keys
    }
  },
  mounted () {
    this.updateMenu()
  },
  watch: {
    collapsed (val) {
      if (val) {
        this.cachedOpenKeys = this.openKeys.concat()
        this.openKeys = []
      } else {
        this.openKeys = this.cachedOpenKeys
      }
    },
    $route: function () {
      this.updateMenu()
    }
  },
  methods: {
    // select menu item
    onOpenChange (openKeys) {

      // 在水平模式下时执行，并且不再执行后续
      if (this.mode === 'horizontal') {
        this.openKeys = openKeys
        return
      }
      // 非水平模式时
      const latestOpenKey = openKeys.find(key => !this.openKeys.includes(key))
      if (!this.rootSubmenuKeys.includes(latestOpenKey)) {
        this.openKeys = openKeys
      } else {
        this.openKeys = latestOpenKey ? [latestOpenKey] : []
      }
    },
    updateMenu () {
      const routes = this.$route.matched.concat()
      const { hidden } = this.$route.meta
      if (routes.length >= 3 && hidden) {
        routes.pop()
        this.selectedKeys = [routes[routes.length - 1].path]
      } else {
        this.selectedKeys = [routes.pop().path]
      }
      const openKeys = []
      if (this.mode === 'inline') {
        routes.forEach(item => {
          openKeys.push(item.path)
        })
      }
      //update-begin-author:taoyan date:20190510 for:online表单菜单点击展开的一级目录不对
      if(!this.selectedKeys|| this.selectedKeys[0].indexOf(":")<0){
        this.collapsed ? (this.cachedOpenKeys = openKeys) : (this.openKeys = openKeys)
      }
      //update-end-author:taoyan date:20190510 for:online表单菜单点击展开的一级目录不对
    },

    // render
    renderItem (menu) {
      if (!menu.hidden) {
        return menu.children && !menu.alwaysShow ? this.renderSubMenu(menu) : this.renderMenuItem(menu)
      }
      return null
    },
    renderMenuItem (menu) {
      const target = null
      const tag = target && 'a' || 'router-link'
      let props = { to: { name: menu.name } }
      if(menu.route && menu.route === '0'){
        props = { to: { path: menu.path } }
      }

      const attrs = { href: menu.url, target: menu.text }
      const displayText = translateMenuName(menu.text)

      if (menu.children) {
        // 把有子菜单的 并且 父菜单是要隐藏子菜单的
        // 都给子菜单增加一个 hidden 属性
        // 用来给刷新页面时， selectedKeys 做控制用
        menu.children.forEach(item => {
          item.meta = Object.assign(item.meta, { hidden: true })
        })
      }
      return (
        <Item {...{ key: menu.url }}>
          <tag {...{ props, attrs }} title={displayText}>
            {this.renderIcon(menu.icon)}
            <span>{displayText}</span>
          </tag>
        </Item>
      )
    },
    renderSubMenu (menu) {
      const itemArr = []
      if (!menu.alwaysShow) {
        menu.children.forEach(item => itemArr.push(this.renderItem(item)))
      }
      const displayText = translateMenuName(menu.text)
      return (
        <SubMenu {...{ key: menu.url }}>
          <span slot="title">
            {this.renderIcon(menu.icon)}
            <span title={displayText}>{displayText}</span>
          </span>
          {itemArr}
        </SubMenu>
      )
    },
    renderIcon (icon) {
      if (icon === 'none' || icon === undefined) {
        return null
      }
      const props = {}
      typeof (icon) === 'object' ? props.component = icon : props.type = icon
      return (
        <Icon {... { props } }/>
      )
    }
  },

  render () {
    const { mode, theme, menu } = this
    const props = {
      mode: mode,
      theme: theme,
      openKeys: this.openKeys,
      inlineIndent: 12,
    }
    const on = {
      select: obj => {
        if(obj.key.indexOf('http://')>-1 || obj.key.indexOf('https://')>-1) {
          window.open(obj.key)
        } else {
          this.selectedKeys = obj.selectedKeys
          this.$emit('select', obj)
        }
      },
      openChange: this.onOpenChange
    }

    const menuTree = menu.map(item => {
      if (item.hidden) {
        return null
      }
      return this.renderItem(item)
    })
    // {...{ props, on: on }}
    return (
      <Menu vModel={this.selectedKeys} {...{ props, on: on }}>
        {menuTree}
      </Menu>
    )
  }
}
