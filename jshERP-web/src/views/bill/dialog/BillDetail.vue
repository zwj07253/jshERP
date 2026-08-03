<template>
  <j-modal
    :title="title"
    :width="width"
    :visible="visible"
    :maskClosable="false"
    :forceRender="true"
    :style="modalStyle"
    fullscreen
    switchFullscreen
    @cancel="handleCancel"
    wrapClassName="ant-modal-cust-warp">
    <template slot="footer">
      <!--打印-->
      <a-button key="back" @click="handleCancel">{{ $t('common.cancel') }}(ESC)</a-button>
      <template v-if="isShowPrintBtn">
        <a-button v-if="billPrintFlag" @click="handlePrintPro">{{ $t('common.printNew') }}</a-button>
        <a-button v-if="billPrintFlag" @click="handlePrint">{{ $t('common.print') }}</a-button>
        <!--此处为解决缓存问题-->
        <a-button v-if="billType === '零售出库'" v-print="'#retailOutPrint'">{{ $t('common.print') }}</a-button>
        <a-button v-if="billType === '零售退货入库'" v-print="'#retailBackPrint'">{{ $t('common.print') }}</a-button>
        <a-button v-if="billType === '请购单'" v-print="'#purchaseApplyPrint'">{{ $t('common.print') }}</a-button>
        <a-button v-if="billType === '采购订单'" v-print="'#purchaseOrderPrint'">{{ $t('common.print') }}</a-button>
        <a-button v-if="billType === '采购入库'" v-print="'#purchaseInPrint'">{{ $t('common.print') }}</a-button>
        <a-button v-if="billType === '采购退货出库'" v-print="'#purchaseBackPrint'">{{ $t('common.print') }}</a-button>
        <a-button v-if="billType === '销售订单'" v-print="'#saleOrderPrint'">{{ $t('common.print') }}</a-button>
        <a-button v-if="billType === '销售出库'" v-print="'#saleOutPrint'">{{ $t('common.print') }}</a-button>
        <a-button v-if="billType === '销售退货入库'" v-print="'#saleBackPrint'">{{ $t('common.print') }}</a-button>
        <a-button v-if="billType === '其它入库'" v-print="'#otherInPrint'">{{ $t('common.print') }}</a-button>
        <a-button v-if="billType === '其它出库'" v-print="'#otherOutPrint'">{{ $t('common.print') }}</a-button>
        <a-button v-if="billType === '调拨出库'" v-print="'#allocationOutPrint'">{{ $t('common.print') }}</a-button>
        <a-button v-if="billType === '组装单'" v-print="'#assemblePrint'">{{ $t('common.print') }}</a-button>
        <a-button v-if="billType === '拆卸单'" v-print="'#disassemblePrint'">{{ $t('common.print') }}</a-button>
        <a-button v-if="billType === '盘点复盘'" v-print="'#stockCheckReplayPrint'">{{ $t('common.print') }}</a-button>
      </template>
      <!--导出Excel-->
      <a-button v-if="billType === '零售出库'||billType === '零售退货入库'" @click="retailExportExcel()">{{ $t('common.export') }}</a-button>
      <a-button v-if="billType === '请购单'" @click="applyExportExcel()">{{ $t('common.export') }}</a-button>
      <a-button v-if="billType === '采购订单'||billType === '销售订单'" @click="orderExportExcel()">{{ $t('common.export') }}</a-button>
      <a-button v-if="billType === '采购入库'||billType === '采购退货出库'||billType === '销售出库'||billType === '销售退货入库'"
                @click="purchaseSaleExportExcel()">{{ $t('common.export') }}</a-button>
      <a-button v-if="billType === '其它入库'||billType === '其它出库'" @click="otherExportExcel()">{{ $t('common.export') }}</a-button>
      <a-button v-if="billType === '调拨出库'" @click="allocationOutExportExcel()">{{ $t('common.export') }}</a-button>
      <a-button v-if="billType === '组装单'||billType === '拆卸单'" @click="assembleExportExcel()">{{ $t('common.export') }}</a-button>
      <a-button v-if="billType === '盘点复盘'" @click="stockCheckReplayExportExcel()">{{ $t('common.export') }}</a-button>
      <!--反审核-->
      <a-button v-if="checkFlag && isCanBackCheck && model.status==='1'" @click="handleBackCheck()">{{ $t('common.unaudit') }}</a-button>
    </template>
    <a-form :form="form">
      <!--零售出库-->
      <template v-if="billType === '零售出库'">
        <section ref="print" id="retailOutPrint">
          <a-row class="form-row" :gutter="24">
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.memberCard')">
                <a-input v-decorator="['id']" hidden/>
                {{model.organName}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billDate')">
                {{model.operTimeStr}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billNo')">
                {{model.number}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('retail.paymentType')">
                {{getPaymentTypeLabel(model.payType)}}
              </a-form-item>
            </a-col>
          </a-row>
          <a-row class="form-row" :gutter="24">
            <a-col :lg="18" :md="12" :sm="24">
              <div :style="tableWidthRetail">
                <a-table
                  ref="table"
                  size="middle"
                  bordered
                  rowKey="id"
                  :pagination="false"
                  :loading="loading"
                  :columns="columns"
                  :dataSource="dataSource">
                  <template slot="customBarCode" slot-scope="text, record">
                    <div :style="record.imgName?'float:left;line-height:30px':'float:left;'">{{record.barCode}}</div>
                    <a-popover placement="right" trigger="click">
                      <template slot="content"><img :src="getImgUrl(record.imgName, record.imgLarge)" width="500px" /></template>
                      <div class="item-info" v-if="record.imgName"><img v-if="record.imgName" :src="getImgUrl(record.imgName, record.imgSmall)" class="item-img" :title="$t('common.view')" /></div>
                    </a-popover>
                  </template>
                </a-table>
              </div>
            </a-col>
            <a-col :lg="6" :md="24" :sm="24" class="bill-summary-panel">
              <a-row class="form-row" :gutter="24">
                <a-col :lg="24" :md="6" :sm="6">
                  <a-form-item :labelCol="summaryLabelCol" :wrapperCol="summaryWrapperCol" :label="$t('common.documentAmount')">
                    {{model.changeAmount}}
                  </a-form-item>
                </a-col>
                <a-col :lg="24" :md="6" :sm="6">
                  <a-form-item :labelCol="summaryLabelCol" :wrapperCol="summaryWrapperCol" :label="$t('retail.getAmount')">
                    {{model.getAmount}}
                  </a-form-item>
                </a-col>
                <a-col :lg="24" :md="6" :sm="6">
                  <a-form-item :labelCol="summaryLabelCol" :wrapperCol="summaryWrapperCol" :label="$t('retail.backAmount')">
                    {{model.backAmount}}
                  </a-form-item>
                </a-col>
                <a-col :lg="24" :md="6" :sm="6">
                  <a-form-item :labelCol="summaryLabelCol" :wrapperCol="summaryWrapperCol" :label="$t('financial.form.account')">
                    {{model.accountName}}
                  </a-form-item>
                </a-col>
                <a-col v-if="model.hasBackFlag" :lg="24" :md="6" :sm="6">
                  <a-form-item :labelCol="summaryLabelCol" :wrapperCol="summaryWrapperCol" :label="$t('common.returnBillNo')">
                    <template v-for="(item, index) in linkNumberList">
                      <a @click="myHandleDetail(item.number)">{{item.number}}</a><br/>
                    </template>
                  </a-form-item>
                </a-col>
              </a-row>
            </a-col>
          </a-row>
          <a-row class="form-row" :gutter="24">
            <a-col :lg="24" :md="24" :sm="24">
              <a-form-item :labelCol="labelCol" :wrapperCol="{xs: { span: 24 },sm: { span: 24 }}" label="" style="padding:20px 10px;">
                {{model.remark}}
              </a-form-item>
            </a-col>
          </a-row>
        </section>
      </template>
      <!--零售退货-->
      <template v-else-if="billType === '零售退货入库'">
        <section ref="print" id="retailBackPrint">
          <a-row class="form-row" :gutter="24">
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.memberCard')">
                <a-input v-decorator="['id']" hidden/>
                {{model.organName}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billDate')">
                {{model.operTimeStr}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billNo')">
                {{model.number}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.linkedBill')">
                <a @click="myHandleDetail(model.linkNumber)">{{model.linkNumber}}</a>
              </a-form-item>
            </a-col>
          </a-row>
          <a-row class="form-row" :gutter="24">
            <a-col :lg="18" :md="12" :sm="24">
              <div :style="tableWidthRetail">
                <a-table
                  ref="table"
                  size="middle"
                  bordered
                  rowKey="id"
                  :pagination="false"
                  :loading="loading"
                  :columns="columns"
                  :dataSource="dataSource">
                  <template slot="customBarCode" slot-scope="text, record">
                    <div :style="record.imgName?'float:left;line-height:30px':'float:left;'">{{record.barCode}}</div>
                    <a-popover placement="right" trigger="click">
                      <template slot="content"><img :src="getImgUrl(record.imgName, record.imgLarge)" width="500px" /></template>
                      <div class="item-info" v-if="record.imgName"><img v-if="record.imgName" :src="getImgUrl(record.imgName, record.imgSmall)" class="item-img" :title="$t('common.view')" /></div>
                    </a-popover>
                  </template>
                </a-table>
              </div>
            </a-col>
            <a-col :lg="6" :md="24" :sm="24" class="bill-summary-panel">
              <a-row class="form-row" :gutter="24">
                <a-col :lg="24" :md="6" :sm="6">
                  <a-form-item :labelCol="summaryLabelCol" :wrapperCol="summaryWrapperCol" :label="$t('common.documentAmount')">
                    {{model.changeAmount}}
                  </a-form-item>
                </a-col>
                <a-col :lg="24" :md="6" :sm="6">
                  <a-form-item :labelCol="summaryLabelCol" :wrapperCol="summaryWrapperCol" :label="$t('purchase.form.paymentAmount')">
                    {{model.getAmount}}
                  </a-form-item>
                </a-col>
                <a-col :lg="24" :md="6" :sm="6">
                  <a-form-item :labelCol="summaryLabelCol" :wrapperCol="summaryWrapperCol" :label="$t('retail.backAmount')">
                    {{model.backAmount}}
                  </a-form-item>
                </a-col>
                <a-col :lg="24" :md="6" :sm="6">
                  <a-form-item :labelCol="summaryLabelCol" :wrapperCol="summaryWrapperCol" :label="$t('financial.form.account')">
                    {{model.accountName}}
                  </a-form-item>
                </a-col>
              </a-row>
            </a-col>
          </a-row>
          <a-row class="form-row" :gutter="24">
            <a-col :lg="24" :md="24" :sm="24">
              <a-form-item :labelCol="labelCol" :wrapperCol="{xs: { span: 24 },sm: { span: 24 }}" label="" style="padding:20px 10px;">
                {{model.remark}}
              </a-form-item>
            </a-col>
          </a-row>
        </section>
      </template>
      <!--请购单-->
      <template v-else-if="billType === '请购单'">
        <section ref="print" id="purchaseApplyPrint">
          <a-row class="form-row" :gutter="24">
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billDate')">
                {{model.operTimeStr}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billNo')">
                {{model.number}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
            </a-col>
            <a-col :span="6">
            </a-col>
          </a-row>
          <div :style="tableWidth">
            <a-table
              ref="table"
              size="middle"
              bordered
              rowKey="id"
              :pagination="false"
              :loading="loading"
              :columns="columns"
              :dataSource="dataSource">
              <template slot="customBarCode" slot-scope="text, record">
                <div :style="record.imgName?'float:left;line-height:30px':'float:left;'">{{record.barCode}}</div>
                <a-popover placement="right" trigger="click">
                  <template slot="content"><img :src="getImgUrl(record.imgName, record.imgLarge)" width="500px" /></template>
                  <div class="item-info" v-if="record.imgName"><img v-if="record.imgName" :src="getImgUrl(record.imgName, record.imgSmall)" class="item-img" :title="$t('common.view')" /></div>
                </a-popover>
              </template>
            </a-table>
          </div>
          <a-row class="form-row" :gutter="24">
            <a-col :lg="24" :md="24" :sm="24">
              <a-form-item :labelCol="labelCol" :wrapperCol="{xs: { span: 24 },sm: { span: 24 }}" label="" style="padding:20px 10px;">
                {{model.remark}}
              </a-form-item>
            </a-col>
          </a-row>
        </section>
      </template>
      <!--采购订单-->
      <template v-else-if="billType === '采购订单'">
        <section ref="print" id="purchaseOrderPrint">
          <a-row class="form-row" :gutter="24">
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.supplier')">
                <a-input v-decorator="['id']" hidden/>
                {{model.organName}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billDate')">
                {{model.operTimeStr}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billNo')">
                {{model.number}}
              </a-form-item>
            </a-col>
            <a-col :span="6" v-if="model.linkApply">
              <a-form-item :labelCol="{xs: { span: 24 },sm: { span: 6 }}" :wrapperCol="wrapperCol" :label="$t('purchase.form.linkedRequisition')">
                <a @click="myHandleDetail(model.linkApply)">{{model.linkApply}}</a>
              </a-form-item>
            </a-col>
            <a-col :span="6" v-if="model.linkNumber">
              <a-form-item v-if="purchaseBySaleFlag" :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.linkedBill')">
                {{model.linkNumber}}
              </a-form-item>
            </a-col>
          </a-row>
          <div :style="tableWidth">
            <a-table
              ref="table"
              size="middle"
              bordered
              rowKey="id"
              :pagination="false"
              :loading="loading"
              :columns="columns"
              :dataSource="dataSource">
              <template slot="customBarCode" slot-scope="text, record">
                <div :style="record.imgName?'float:left;line-height:30px':'float:left;'">{{record.barCode}}</div>
                <a-popover placement="right" trigger="click">
                  <template slot="content"><img :src="getImgUrl(record.imgName, record.imgLarge)" width="500px" /></template>
                  <div class="item-info" v-if="record.imgName"><img v-if="record.imgName" :src="getImgUrl(record.imgName, record.imgSmall)" class="item-img" :title="$t('common.view')" /></div>
                </a-popover>
              </template>
            </a-table>
          </div>
          <a-row class="form-row" :gutter="24">
            <a-col :lg="24" :md="24" :sm="24">
              <a-form-item :labelCol="labelCol" :wrapperCol="{xs: { span: 24 },sm: { span: 24 }}" label="" style="padding:20px 10px;">
                {{model.remark}}
              </a-form-item>
            </a-col>
          </a-row>
          <a-row class="form-row" :gutter="24">
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.discount')">
                {{model.discount}}%
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.discountMoney')">
                {{model.discountMoney}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="{xs: { span: 24 },sm: { span: 6 }}" :wrapperCol="wrapperCol" :label="$t('purchase.form.discountLastMoney')">
                {{model.discountLastMoney}}
              </a-form-item>
            </a-col>
            <a-col :span="6"></a-col>
          </a-row>
          <a-row class="form-row" :gutter="24">
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.settleAccount')">
                {{model.accountName}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.payDeposit')">
                {{model.changeAmount}}
              </a-form-item>
            </a-col>
            <a-col :span="6"></a-col>
            <a-col :span="6"></a-col>
          </a-row>
        </section>
      </template>
      <!--采购入库-->
      <template v-else-if="billType === '采购入库'">
        <section ref="print" id="purchaseInPrint">
          <a-row class="form-row" :gutter="24">
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.supplier')">
                <a-input v-decorator="['id']" hidden/>
                {{model.organName}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billDate')">
                {{model.operTimeStr}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billNo')">
                {{model.number}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.linkedBill')">
                <a @click="myHandleDetail(model.linkNumber)">{{model.linkNumber}}</a>
              </a-form-item>
            </a-col>
          </a-row>
          <div :style="tableWidth">
            <a-table
              ref="table"
              size="middle"
              bordered
              rowKey="id"
              :pagination="false"
              :loading="loading"
              :columns="columns"
              :dataSource="dataSource">
              <template slot="customBarCode" slot-scope="text, record">
                <div :style="record.imgName?'float:left;line-height:30px':'float:left;'">{{record.barCode}}</div>
                <a-popover placement="right" trigger="click">
                  <template slot="content"><img :src="getImgUrl(record.imgName, record.imgLarge)" width="500px" /></template>
                  <div class="item-info" v-if="record.imgName"><img v-if="record.imgName" :src="getImgUrl(record.imgName, record.imgSmall)" class="item-img" :title="$t('common.view')" /></div>
                </a-popover>
              </template>
            </a-table>
          </div>
          <a-row class="form-row" :gutter="24">
            <a-col :lg="24" :md="24" :sm="24">
              <a-form-item :labelCol="labelCol" :wrapperCol="{xs: { span: 24 },sm: { span: 24 }}" label="" style="padding:20px 10px;">
                {{model.remark}}
              </a-form-item>
            </a-col>
          </a-row>
          <a-row class="form-row" :gutter="24">
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.discount')">
                {{model.discount}}%
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.discountMoney')">
                {{model.discountMoney}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="{xs: { span: 24 },sm: { span: 6 }}" :wrapperCol="wrapperCol" :label="$t('purchase.form.discountLastMoney')">
                {{model.discountLastMoney}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.otherMoney')">
                {{model.otherMoney}}
              </a-form-item>
            </a-col>
          </a-row>
          <a-row class="form-row" :gutter="24">
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.settleAccount')">
                {{model.accountName}}
              </a-form-item>
            </a-col>
            <a-col v-if="model.deposit" :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.deposit')">
                {{model.deposit}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.changeAmount')">
                {{model.changeAmount}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.currentDebt')">
                {{model.debt}}
              </a-form-item>
            </a-col>
            <a-col v-if="model.hasBackFlag" :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.returnBillNo')">
                <template v-for="(item, index) in linkNumberList">
                  <a @click="myHandleDetail(item.number)">{{item.number}}</a><br/>
                </template>
              </a-form-item>
            </a-col>
          </a-row>
          <a-row class="form-row" :gutter="24">
            <a-col v-if="financialBillNoList.length" :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.paymentBillNo')">
                <template v-for="(item, index) in financialBillNoList">
                  <a @click="myHandleFinancialDetail(item.billNo)">{{item.billNo}}</a><br/>
                </template>
              </a-form-item>
            </a-col>
          </a-row>
        </section>
      </template>
      <!--采购退货-->
      <template v-else-if="billType === '采购退货出库'">
        <section ref="print" id="purchaseBackPrint">
          <a-row class="form-row" :gutter="24">
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.supplier')">
                <a-input v-decorator="['id']" hidden/>
                {{model.organName}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billDate')">
                {{model.operTimeStr}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billNo')">
                {{model.number}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.linkedBill')">
                <a @click="myHandleDetail(model.linkNumber)">{{model.linkNumber}}</a>
              </a-form-item>
            </a-col>
          </a-row>
          <div :style="tableWidth">
            <a-table
              ref="table"
              size="middle"
              bordered
              rowKey="id"
              :pagination="false"
              :loading="loading"
              :columns="columns"
              :dataSource="dataSource">
              <template slot="customBarCode" slot-scope="text, record">
                <div :style="record.imgName?'float:left;line-height:30px':'float:left;'">{{record.barCode}}</div>
                <a-popover placement="right" trigger="click">
                  <template slot="content"><img :src="getImgUrl(record.imgName, record.imgLarge)" width="500px" /></template>
                  <div class="item-info" v-if="record.imgName"><img v-if="record.imgName" :src="getImgUrl(record.imgName, record.imgSmall)" class="item-img" :title="$t('common.view')" /></div>
                </a-popover>
              </template>
            </a-table>
          </div>
          <a-row class="form-row" :gutter="24">
            <a-col :lg="24" :md="24" :sm="24">
              <a-form-item :labelCol="labelCol" :wrapperCol="{xs: { span: 24 },sm: { span: 24 }}" label="" style="padding:20px 10px;">
                {{model.remark}}
              </a-form-item>
            </a-col>
          </a-row>
          <a-row class="form-row" :gutter="24">
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.discount')">
                {{model.discount}}%
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.refundDiscount')">
                {{model.discountMoney}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="{xs: { span: 24 },sm: { span: 6 }}" :wrapperCol="wrapperCol" :label="$t('purchase.form.discountLastMoney')">
                {{model.discountLastMoney}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.otherMoney')">
                {{model.otherMoney}}
              </a-form-item>
            </a-col>
          </a-row>
          <a-row class="form-row" :gutter="24">
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.settleAccount')">
                {{model.accountName}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.currentRefund')">
                {{model.changeAmount}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.currentDebt')">
                {{model.debt}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
            </a-col>
          </a-row>
        </section>
      </template>
      <!--销售订单-->
      <template v-else-if="billType === '销售订单'">
        <section ref="print" id="saleOrderPrint">
          <a-row class="form-row" :gutter="24">
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.customer')">
                <a-input v-decorator="['id']" hidden/>
                {{model.organName}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billDate')">
                {{model.operTimeStr}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billNo')">
                {{model.number}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.salesMan')">
                {{model.salesManStr}}
              </a-form-item>
            </a-col>
          </a-row>
          <div :style="tableWidth">
            <a-table
              ref="table"
              size="middle"
              bordered
              rowKey="id"
              :pagination="false"
              :loading="loading"
              :columns="columns"
              :dataSource="dataSource">
              <template slot="customBarCode" slot-scope="text, record">
                <div :style="record.imgName?'float:left;line-height:30px':'float:left;'">{{record.barCode}}</div>
                <a-popover placement="right" trigger="click">
                  <template slot="content"><img :src="getImgUrl(record.imgName, record.imgLarge)" width="500px" /></template>
                  <div class="item-info" v-if="record.imgName"><img v-if="record.imgName" :src="getImgUrl(record.imgName, record.imgSmall)" class="item-img" :title="$t('common.view')" /></div>
                </a-popover>
              </template>
            </a-table>
          </div>
          <a-row class="form-row" :gutter="24">
            <a-col :lg="24" :md="24" :sm="24">
              <a-form-item :labelCol="labelCol" :wrapperCol="{xs: { span: 24 },sm: { span: 24 }}" label="" style="padding:20px 10px;">
                {{model.remark}}
              </a-form-item>
            </a-col>
          </a-row>
          <a-row class="form-row" :gutter="24">
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.discount')">
                {{model.discount}}%
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.discountMoney')">
                {{model.discountMoney}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="{xs: { span: 24 },sm: { span: 6 }}" :wrapperCol="wrapperCol" :label="$t('purchase.form.discountLastMoney')">
                {{model.discountLastMoney}}
              </a-form-item>
            </a-col>
            <a-col :span="6"></a-col>
          </a-row>
          <a-row class="form-row" :gutter="24">
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.settleAccount')">
                {{model.accountName}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.collectDeposit')">
                {{model.changeAmount}}
              </a-form-item>
            </a-col>
            <a-col :span="6"></a-col>
            <a-col :span="6"></a-col>
          </a-row>
        </section>
      </template>
      <!--销售出库-->
      <template v-else-if="billType === '销售出库'">
        <section ref="print" id="saleOutPrint">
          <a-row class="form-row" :gutter="24">
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.customer')">
                <a-input v-decorator="['id']" hidden/>
                {{model.organName}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billDate')">
                {{model.operTimeStr}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billNo')">
                {{model.number}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.linkedBill')">
                <a @click="myHandleDetail(model.linkNumber)">{{model.linkNumber}}</a>
              </a-form-item>
            </a-col>
          </a-row>
          <div :style="tableWidth">
            <a-table
              ref="table"
              size="middle"
              bordered
              rowKey="id"
              :pagination="false"
              :loading="loading"
              :columns="columns"
              :dataSource="dataSource">
              <template slot="customBarCode" slot-scope="text, record">
                <div :style="record.imgName?'float:left;line-height:30px':'float:left;'">{{record.barCode}}</div>
                <a-popover placement="right" trigger="click">
                  <template slot="content"><img :src="getImgUrl(record.imgName, record.imgLarge)" width="500px" /></template>
                  <div class="item-info" v-if="record.imgName"><img v-if="record.imgName" :src="getImgUrl(record.imgName, record.imgSmall)" class="item-img" :title="$t('common.view')" /></div>
                </a-popover>
              </template>
            </a-table>
          </div>
          <a-row class="form-row" :gutter="24">
            <a-col :lg="24" :md="24" :sm="24">
              <a-form-item :labelCol="labelCol" :wrapperCol="{xs: { span: 24 },sm: { span: 24 }}" label="" style="padding:20px 10px;">
                {{model.remark}}
              </a-form-item>
            </a-col>
          </a-row>
          <a-row class="form-row" :gutter="24">
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.discount')">
                {{model.discount}}%
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.receiptDiscount')">
                {{model.discountMoney}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="{xs: { span: 24 },sm: { span: 6 }}" :wrapperCol="wrapperCol" :label="$t('purchase.form.discountLastMoney')">
                {{model.discountLastMoney}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.otherMoney')">
                {{model.otherMoney}}
              </a-form-item>
            </a-col>
          </a-row>
          <a-row class="form-row" :gutter="24">
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.settleAccount')">
                {{model.accountName}}
              </a-form-item>
            </a-col>
            <a-col v-if="model.deposit" :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.deposit')">
                {{model.deposit}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('sales.form.changeAmount')">
                {{model.changeAmount}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.currentDebt')">
                {{model.debt}}
              </a-form-item>
            </a-col>
            <a-col v-if="model.hasBackFlag" :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.returnBillNo')">
                <template v-for="(item, index) in linkNumberList">
                  <a @click="myHandleDetail(item.number)">{{item.number}}</a><br/>
                </template>
              </a-form-item>
            </a-col>
          </a-row>
          <a-row class="form-row" :gutter="24">
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.salesMan')">
                {{model.salesManStr}}
              </a-form-item>
            </a-col>
            <a-col v-if="financialBillNoList.length" :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.receiptBillNo')">
                <template v-for="(item, index) in financialBillNoList">
                  <a @click="myHandleFinancialDetail(item.billNo)">{{item.billNo}}</a><br/>
                </template>
              </a-form-item>
            </a-col>
          </a-row>
        </section>
      </template>
      <!--销售退货-->
      <template v-else-if="billType === '销售退货入库'">
        <section ref="print" id="saleBackPrint">
          <a-row class="form-row" :gutter="24">
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.customer')">
                <a-input v-decorator="['id']" hidden/>
                {{model.organName}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billDate')">
                {{model.operTimeStr}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billNo')">
                {{model.number}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.linkedBill')">
                <a @click="myHandleDetail(model.linkNumber)">{{model.linkNumber}}</a>
              </a-form-item>
            </a-col>
          </a-row>
          <div :style="tableWidth">
            <a-table
              ref="table"
              size="middle"
              bordered
              rowKey="id"
              :pagination="false"
              :loading="loading"
              :columns="columns"
              :dataSource="dataSource">
              <template slot="customBarCode" slot-scope="text, record">
                <div :style="record.imgName?'float:left;line-height:30px':'float:left;'">{{record.barCode}}</div>
                <a-popover placement="right" trigger="click">
                  <template slot="content"><img :src="getImgUrl(record.imgName, record.imgLarge)" width="500px" /></template>
                  <div class="item-info" v-if="record.imgName"><img v-if="record.imgName" :src="getImgUrl(record.imgName, record.imgSmall)" class="item-img" :title="$t('common.view')" /></div>
                </a-popover>
              </template>
            </a-table>
          </div>
          <a-row class="form-row" :gutter="24">
            <a-col :lg="24" :md="24" :sm="24">
              <a-form-item :labelCol="labelCol" :wrapperCol="{xs: { span: 24 },sm: { span: 24 }}" label="" style="padding:20px 10px;">
                {{model.remark}}
              </a-form-item>
            </a-col>
          </a-row>
          <a-row class="form-row" :gutter="24">
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.discount')">
                {{model.discount}}%
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.refundDiscount')">
                {{model.discountMoney}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="{xs: { span: 24 },sm: { span: 6 }}" :wrapperCol="wrapperCol" :label="$t('purchase.form.discountLastMoney')">
                {{model.discountLastMoney}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('purchase.form.otherMoney')">
                {{model.otherMoney}}
              </a-form-item>
            </a-col>
          </a-row>
          <a-row class="form-row" :gutter="24">
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.settleAccount')">
                {{model.accountName}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.currentRefund')">
                {{model.changeAmount}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.currentDebt')">
                {{model.debt}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.salesMan')">
                {{model.salesManStr}}
              </a-form-item>
            </a-col>
          </a-row>
        </section>
      </template>
      <!--其它入库-->
      <template v-else-if="billType === '其它入库'">
        <section ref="print" id="otherInPrint">
          <a-row class="form-row" :gutter="24">
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.supplier')">
                <a-input v-decorator="['id']" hidden/>
                {{model.organName}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billDate')">
                {{model.operTimeStr}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billNo')">
                {{model.number}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item v-if="model.billType" :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.linkedBill')">
                {{model.linkNumber}} {{model.billType}}
              </a-form-item>
              <a-form-item v-if="!model.billType" :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.linkedBill')">
                <a @click="myHandleDetail(model.linkNumber)">{{model.linkNumber}}</a>
              </a-form-item>
            </a-col>
          </a-row>
          <div :style="tableWidth">
            <a-table
              ref="table"
              size="middle"
              bordered
              rowKey="id"
              :pagination="false"
              :loading="loading"
              :columns="columns"
              :dataSource="dataSource">
              <template slot="customBarCode" slot-scope="text, record">
                <div :style="record.imgName?'float:left;line-height:30px':'float:left;'">{{record.barCode}}</div>
                <a-popover placement="right" trigger="click">
                  <template slot="content"><img :src="getImgUrl(record.imgName, record.imgLarge)" width="500px" /></template>
                  <div class="item-info" v-if="record.imgName"><img v-if="record.imgName" :src="getImgUrl(record.imgName, record.imgSmall)" class="item-img" :title="$t('common.view')" /></div>
                </a-popover>
              </template>
            </a-table>
          </div>
          <a-row class="form-row" :gutter="24">
            <a-col :lg="24" :md="24" :sm="24">
              <a-form-item :labelCol="labelCol" :wrapperCol="{xs: { span: 24 },sm: { span: 24 }}" label="" style="padding:20px 10px;">
                {{model.remark}}
              </a-form-item>
            </a-col>
          </a-row>
        </section>
      </template>
      <!--其它出库-->
      <template v-else-if="billType === '其它出库'">
        <section ref="print" id="otherOutPrint">
          <a-row class="form-row" :gutter="24">
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.customer')">
                <a-input v-decorator="['id']" hidden/>
                {{model.organName}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billDate')">
                {{model.operTimeStr}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billNo')">
                {{model.number}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item v-if="model.billType" :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.linkedBill')">
                {{model.linkNumber}} {{model.billType}}
              </a-form-item>
              <a-form-item v-if="!model.billType" :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.linkedBill')">
                <a @click="myHandleDetail(model.linkNumber)">{{model.linkNumber}}</a>
              </a-form-item>
            </a-col>
          </a-row>
          <div :style="tableWidth">
            <a-table
              ref="table"
              size="middle"
              bordered
              rowKey="id"
              :pagination="false"
              :loading="loading"
              :columns="columns"
              :dataSource="dataSource">
              <template slot="customBarCode" slot-scope="text, record">
                <div :style="record.imgName?'float:left;line-height:30px':'float:left;'">{{record.barCode}}</div>
                <a-popover placement="right" trigger="click">
                  <template slot="content"><img :src="getImgUrl(record.imgName, record.imgLarge)" width="500px" /></template>
                  <div class="item-info" v-if="record.imgName"><img v-if="record.imgName" :src="getImgUrl(record.imgName, record.imgSmall)" class="item-img" :title="$t('common.view')" /></div>
                </a-popover>
              </template>
            </a-table>
          </div>
          <a-row class="form-row" :gutter="24">
            <a-col :lg="24" :md="24" :sm="24">
              <a-form-item :labelCol="labelCol" :wrapperCol="{xs: { span: 24 },sm: { span: 24 }}" label="" style="padding:20px 10px;">
                {{model.remark}}
              </a-form-item>
            </a-col>
          </a-row>
        </section>
      </template>
      <!--调拨出库-->
      <template v-else-if="billType === '调拨出库'">
        <section ref="print" id="allocationOutPrint">
          <a-row class="form-row" :gutter="24">
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billDate')">
                {{model.operTimeStr}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billNo')">
                {{model.number}}
              </a-form-item>
            </a-col>
            <a-col :span="6"></a-col>
            <a-col :span="6"></a-col>
          </a-row>
          <div :style="tableWidth">
            <a-table
              ref="table"
              size="middle"
              bordered
              rowKey="id"
              :pagination="false"
              :loading="loading"
              :columns="columns"
              :dataSource="dataSource">
              <template slot="customBarCode" slot-scope="text, record">
                <div :style="record.imgName?'float:left;line-height:30px':'float:left;'">{{record.barCode}}</div>
                <a-popover placement="right" trigger="click">
                  <template slot="content"><img :src="getImgUrl(record.imgName, record.imgLarge)" width="500px" /></template>
                  <div class="item-info" v-if="record.imgName"><img v-if="record.imgName" :src="getImgUrl(record.imgName, record.imgSmall)" class="item-img" :title="$t('common.view')" /></div>
                </a-popover>
              </template>
            </a-table>
          </div>
          <a-row class="form-row" :gutter="24">
            <a-col :lg="24" :md="24" :sm="24">
              <a-form-item :labelCol="labelCol" :wrapperCol="{xs: { span: 24 },sm: { span: 24 }}" label="" style="padding:20px 10px;">
                {{model.remark}}
              </a-form-item>
            </a-col>
          </a-row>
        </section>
      </template>
      <!--组装单-->
      <template v-else-if="billType === '组装单'">
        <section ref="print" id="assemblePrint">
          <a-row class="form-row" :gutter="24">
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billDate')">
                {{model.operTimeStr}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billNo')">
                {{model.number}}
              </a-form-item>
            </a-col>
            <a-col :span="6"></a-col>
            <a-col :span="6"></a-col>
          </a-row>
          <div :style="tableWidth">
            <a-table
              ref="table"
              size="middle"
              bordered
              rowKey="id"
              :pagination="false"
              :loading="loading"
              :columns="columns"
              :dataSource="dataSource">
              <template slot="customBarCode" slot-scope="text, record">
                <div :style="record.imgName?'float:left;line-height:30px':'float:left;'">{{record.barCode}}</div>
                <a-popover placement="right" trigger="click">
                  <template slot="content"><img :src="getImgUrl(record.imgName, record.imgLarge)" width="500px" /></template>
                  <div class="item-info" v-if="record.imgName"><img v-if="record.imgName" :src="getImgUrl(record.imgName, record.imgSmall)" class="item-img" :title="$t('common.view')" /></div>
                </a-popover>
              </template>
            </a-table>
          </div>
          <a-row class="form-row" :gutter="24">
            <a-col :lg="24" :md="24" :sm="24">
              <a-form-item :labelCol="labelCol" :wrapperCol="{xs: { span: 24 },sm: { span: 24 }}" label="" style="padding:20px 10px;">
                {{model.remark}}
              </a-form-item>
            </a-col>
          </a-row>
        </section>
      </template>
      <!--拆卸单-->
      <template v-else-if="billType === '拆卸单'">
        <section ref="print" id="disassemblePrint">
          <a-row class="form-row" :gutter="24">
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billDate')">
                {{model.operTimeStr}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billNo')">
                {{model.number}}
              </a-form-item>
            </a-col>
            <a-col :span="6"></a-col>
            <a-col :span="6"></a-col>
          </a-row>
          <div :style="tableWidth">
            <a-table
              ref="table"
              size="middle"
              bordered
              rowKey="id"
              :pagination="false"
              :loading="loading"
              :columns="columns"
              :dataSource="dataSource">
              <template slot="customBarCode" slot-scope="text, record">
                <div :style="record.imgName?'float:left;line-height:30px':'float:left;'">{{record.barCode}}</div>
                <a-popover placement="right" trigger="click">
                  <template slot="content"><img :src="getImgUrl(record.imgName, record.imgLarge)" width="500px" /></template>
                  <div class="item-info" v-if="record.imgName"><img v-if="record.imgName" :src="getImgUrl(record.imgName, record.imgSmall)" class="item-img" :title="$t('common.view')" /></div>
                </a-popover>
              </template>
            </a-table>
          </div>
          <a-row class="form-row" :gutter="24">
            <a-col :lg="24" :md="24" :sm="24">
              <a-form-item :labelCol="labelCol" :wrapperCol="{xs: { span: 24 },sm: { span: 24 }}" label="" style="padding:20px 10px;">
                {{model.remark}}
              </a-form-item>
            </a-col>
          </a-row>
        </section>
      </template>
      <!--盘点复盘-->
      <template v-else-if="billType === '盘点复盘'">
        <section ref="print" id="stockCheckReplayPrint">
          <a-row class="form-row" :gutter="24">
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billDate')">
                {{model.operTimeStr}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.billNo')">
                {{model.number}}
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item :labelCol="labelCol" :wrapperCol="wrapperCol" :label="$t('common.linkedBill')">
                {{model.linkNumber}}
              </a-form-item>
            </a-col>
            <a-col :span="6"></a-col>
          </a-row>
          <div :style="tableWidth">
            <a-table
              ref="table"
              size="middle"
              bordered
              rowKey="id"
              :pagination="false"
              :loading="loading"
              :columns="columns"
              :dataSource="dataSource">
              <template slot="customBarCode" slot-scope="text, record">
                <div :style="record.imgName?'float:left;line-height:30px':'float:left;'">{{record.barCode}}</div>
                <a-popover placement="right" trigger="click">
                  <template slot="content"><img :src="getImgUrl(record.imgName, record.imgLarge)" width="500px" /></template>
                  <div class="item-info" v-if="record.imgName"><img v-if="record.imgName" :src="getImgUrl(record.imgName, record.imgSmall)" class="item-img" :title="$t('common.view')" /></div>
                </a-popover>
              </template>
            </a-table>
          </div>
          <a-row class="form-row" :gutter="24">
            <a-col :lg="24" :md="24" :sm="24">
              <a-form-item :labelCol="labelCol" :wrapperCol="{xs: { span: 24 },sm: { span: 24 }}" label="" style="padding:20px 10px;">
                {{model.remark}}
              </a-form-item>
            </a-col>
          </a-row>
        </section>
      </template>
      <template v-if="fileList && fileList.length>0">
        <a-row class="form-row" :gutter="24">
          <a-col :span="10">
            <a-form-item :labelCol="{xs: { span: 24 },sm: { span: 3 }}" :wrapperCol="{xs: { span: 24 },sm: { span: 21 }}" :label="$t('common.attachment')">
              <j-upload v-model="fileList" bizPath="bill" :disabled="true" :buttonVisible="false"></j-upload>
            </a-form-item>
          </a-col>
          <a-col :span="14"></a-col>
        </a-row>
      </template>
    </a-form>
    <bill-print-iframe ref="modalDetail"></bill-print-iframe>
    <bill-print-pro-iframe ref="modalProDetail"></bill-print-pro-iframe>
    <financial-detail ref="financialDetailModal"></financial-detail>
  </j-modal>
</template>

<script>
  import pick from 'lodash.pick'
  import { getAction, postAction, getFileAccessHttpUrl } from '@/api/manage'
  import { findBillDetailByNumber, findFinancialDetailByNumber, getPlatformConfigByKey, getCurrentSystemConfig} from '@/api/api'
  import { getMpListShort, getCheckFlag, exportXlsPost } from "@/utils/util"
  import { getBillTypeLabel, getPaymentTypeLabel } from '@/utils/billI18n'
  import BillPrintIframe from './BillPrintIframe'
  import BillPrintProIframe from './BillPrintProIframe'
  import FinancialDetail from '../../financial/dialog/FinancialDetail'
  import JUpload from '@/components/jeecg/JUpload'
  import Vue from 'vue'
  export default {
    name: 'BillDetail',
    components: {
      BillPrintIframe,
      BillPrintProIframe,
      FinancialDetail,
      JUpload
    },
    data () {
      return {
        title: this.$t('common.detail'),
        width: '1600px',
        visible: false,
        modalStyle: '',
        model: {},
        isCanBackCheck: true,
        billType: '',
        billPrintFlag: false,
        fileList: [],
        purchaseBySaleFlag: false,
        linkNumberList: [],
        financialBillNoList: [],
        /* 原始反审核是否开启 */
        checkFlag: true,
        /* 是否显示打印按钮 */
        isShowPrintBtn: true,
        tableWidth: {
          'width': '1700px'
        },
        tableWidthRetail: {
          'width': '1200px'
        },
        labelCol: {
          xs: { span: 24 },
          sm: { span: 5 },
        },
        wrapperCol: {
          xs: { span: 24 },
          sm: { span: 16 },
        },
        summaryLabelCol: {
          xs: { span: 24 },
          sm: { span: 12 },
        },
        summaryWrapperCol: {
          xs: { span: 24 },
          sm: { span: 12 },
        },
        form: this.$form.createForm(this),
        loading: false,
        dataSource: [],
        url: {
          detailList: '/depotItem/getDetailList',
          batchSetStatusUrl: "/depotHead/batchSetStatus"
        },
        //扩展信息标题
        otherFieldTitle: '',
        //表头
        columns:[],
        //列定义
        defColumns: [],
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
          { title: this.$t('purchase.form.columns.quantity'), dataIndex: 'operNumber'},
          { title: this.$t('purchase.form.columns.unitPrice'), dataIndex: 'unitPrice'},
          { title: this.$t('purchase.form.columns.amount'), dataIndex: 'allPrice'},
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
          { title: this.$t('purchase.form.columns.quantity'), dataIndex: 'operNumber'},
          { title: this.$t('purchase.form.columns.unitPrice'), dataIndex: 'unitPrice'},
          { title: this.$t('purchase.form.columns.amount'), dataIndex: 'allPrice'},
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
          { title: this.$t('purchase.form.columns.quantity'), dataIndex: 'operNumber'},
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
          { title: this.$t('purchase.form.columns.quantity'), dataIndex: 'operNumber'},
          { title: this.$t('purchase.finishPurchased'), dataIndex: 'finishNumber'},
          { title: this.$t('purchase.form.columns.unitPrice'), dataIndex: 'unitPrice'},
          { title: this.$t('purchase.form.columns.amount'), dataIndex: 'allPrice'},
          { title: this.$t('system.taxRate'), dataIndex: 'taxRate'},
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
          { title: this.$t('purchase.form.columns.quantity'), dataIndex: 'operNumber'},
          { title: this.$t('purchase.finishInbound'), dataIndex: 'finishNumber'},
          { title: this.$t('purchase.form.columns.unitPrice'), dataIndex: 'unitPrice'},
          { title: this.$t('purchase.form.columns.amount'), dataIndex: 'allPrice'},
          { title: this.$t('system.taxRate'), dataIndex: 'taxRate'},
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
          { title: this.$t('purchase.form.columns.quantity'), dataIndex: 'operNumber'},
          { title: this.$t('sales.finishNumber'), dataIndex: 'finishNumber'},
          { title: this.$t('purchase.form.columns.unitPrice'), dataIndex: 'unitPrice'},
          { title: this.$t('purchase.form.columns.amount'), dataIndex: 'allPrice'},
          { title: this.$t('system.taxRate'), dataIndex: 'taxRate'},
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
          { title: this.$t('purchase.form.columns.quantity'), dataIndex: 'operNumber'},
          { title: this.$t('purchase.finishPurchased'), dataIndex: 'finishPurchaseNumber'},
          { title: this.$t('sales.partialSales'), dataIndex: 'finishNumber'},
          { title: this.$t('purchase.form.columns.unitPrice'), dataIndex: 'unitPrice'},
          { title: this.$t('purchase.form.columns.amount'), dataIndex: 'allPrice'},
          { title: this.$t('system.taxRate'), dataIndex: 'taxRate'},
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
          { title: this.$t('purchase.form.columns.quantity'), dataIndex: 'operNumber'},
          { title: this.$t('sales.finishNumber'), dataIndex: 'finishNumber'},
          { title: this.$t('purchase.form.columns.unitPrice'), dataIndex: 'unitPrice'},
          { title: this.$t('purchase.form.columns.amount'), dataIndex: 'allPrice'},
          { title: this.$t('system.taxRate'), dataIndex: 'taxRate'},
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
          { title: this.$t('purchase.form.columns.quantity'), dataIndex: 'operNumber'},
          { title: this.$t('purchase.finishInbound'), dataIndex: 'finishNumber'},
          { title: this.$t('purchase.form.columns.unitPrice'), dataIndex: 'unitPrice'},
          { title: this.$t('purchase.form.columns.amount'), dataIndex: 'allPrice'},
          { title: this.$t('system.taxRate'), dataIndex: 'taxRate'},
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
          { title: this.$t('purchase.form.columns.quantity'), dataIndex: 'operNumber'},
          { title: this.$t('purchase.form.columns.unitPrice'), dataIndex: 'unitPrice'},
          { title: this.$t('purchase.form.columns.amount'), dataIndex: 'allPrice'},
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
          { title: this.$t('purchase.form.columns.quantity'), dataIndex: 'operNumber'},
          { title: this.$t('purchase.form.columns.unitPrice'), dataIndex: 'unitPrice'},
          { title: this.$t('purchase.form.columns.amount'), dataIndex: 'allPrice'},
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
          { title: this.$t('purchase.form.columns.quantity'), dataIndex: 'operNumber'},
          { title: this.$t('purchase.form.columns.unitPrice'), dataIndex: 'unitPrice'},
          { title: this.$t('purchase.form.columns.amount'), dataIndex: 'allPrice'},
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
          { title: this.$t('purchase.form.columns.quantity'), dataIndex: 'operNumber'},
          { title: this.$t('purchase.form.columns.unitPrice'), dataIndex: 'unitPrice'},
          { title: this.$t('purchase.form.columns.amount'), dataIndex: 'allPrice'},
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
          { title: this.$t('purchase.form.columns.quantity'), dataIndex: 'operNumber'},
          { title: this.$t('purchase.form.columns.unitPrice'), dataIndex: 'unitPrice'},
          { title: this.$t('purchase.form.columns.amount'), dataIndex: 'allPrice'},
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
          { title: this.$t('purchase.form.columns.quantity'), dataIndex: 'operNumber'},
          { title: this.$t('purchase.form.columns.unitPrice'), dataIndex: 'unitPrice'},
          { title: this.$t('purchase.form.columns.amount'), dataIndex: 'allPrice'},
          { title: this.$t('common.remark'), dataIndex: 'remark'}
        ]
      }
    },
    created () {
      let realScreenWidth = window.screen.width
      this.width = realScreenWidth<1500?'1200px':'1600px'
      this.tableWidth = {
        'width': '100%'
      }
      this.tableWidthRetail = {
        'width': '100%'
      }
    },
    watch: {
      '$i18n.locale' () {
        this.refreshColumnTitles()
        if (this.billType) {
          this.title = getBillTypeLabel(this, this.billType) + '-' + this.$t('common.view')
          this.initSetting(this.model, this.billType, this.dataSource)
        }
      }
    },
    methods: {
      refreshColumnTitles () {
        const titleKeys = {
          depotName: 'common.depotName', barCode: 'common.barcode', name: 'common.name', standard: 'common.specification',
          model: 'common.model', color: 'material.color', stock: 'purchase.form.columns.stock', unit: 'common.unit',
          sku: 'purchase.form.columns.sku', operNumber: 'purchase.form.columns.quantity', unitPrice: 'purchase.form.columns.unitPrice',
          allPrice: 'purchase.form.columns.amount', remark: 'common.remark', batchNumber: 'purchase.form.columns.batchNumber',
          expirationDate: 'purchase.form.columns.expirationDate', snList: 'purchase.form.columns.serialNumber',
          weight: 'common.weight', position: 'common.position', brand: 'common.brand',
          mfrs: 'material.manufacturer', anotherDepotName: 'common.inboundDepot', mType: 'common.productType'
        }
        const columnSets = [
          this.retailOutColumns, this.retailBackColumns, this.purchaseApplyColumns, this.purchaseOrderColumns,
          this.purchaseInColumns, this.purchaseBackColumns, this.saleOrderColumns, this.saleOutColumns,
          this.saleBackColumns, this.otherInColumns, this.otherOutColumns, this.allocationOutColumns,
          this.assembleColumns, this.disassembleColumns, this.stockCheckReplayColumns
        ]
        columnSets.forEach(columns => columns.forEach(column => {
          if (titleKeys[column.dataIndex]) column.title = this.$t(titleKeys[column.dataIndex])
        }))
      },
      initSetting(record, type, ds) {
        if (type === '零售出库') {
          this.defColumns = this.retailOutColumns
        } else if (type === '零售退货入库') {
          this.defColumns = this.retailBackColumns
        } else if (type === '请购单') {
          this.defColumns = this.purchaseApplyColumns
        } else if (type === '采购订单') {
          this.defColumns = this.purchaseOrderColumns
        } else if (type === '采购入库') {
          this.defColumns = this.purchaseInColumns
        } else if (type === '采购退货出库') {
          this.defColumns = this.purchaseBackColumns
        } else if (type === '销售订单') {
          this.defColumns = this.saleOrderColumns
        } else if (type === '销售出库') {
          this.defColumns = this.saleOutColumns
        } else if (type === '销售退货入库') {
          this.defColumns = this.saleBackColumns
        } else if (type === '其它入库') {
          this.defColumns = this.otherInColumns
        } else if (type === '其它出库') {
          this.defColumns = this.otherOutColumns
        } else if (type === '调拨出库') {
          this.defColumns = this.allocationOutColumns
        } else if (type === '组装单') {
          this.defColumns = this.assembleColumns
        } else if (type === '拆卸单') {
          this.defColumns = this.disassembleColumns
        } else if (type === '盘点复盘') {
          this.defColumns = this.stockCheckReplayColumns
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
          if(record.status === '3') {
            //部分采购|部分销售
            needAddkeywords.push('finishNumber')
          }
          if(record.purchaseStatus === '3') {
            //销售订单转采购订单的场景
            needAddkeywords.push('finishPurchaseNumber')
          }
        }
        let currentCol = [{title:'#',dataIndex:'',align:'center',
          customRender:function(t,r,index){
            if(r.mType) {
              //组装和拆卸所有行都展示序号
              return index === ds.length?'':parseInt(index)+1
            } else {
              return index === ds.length-1?'':parseInt(index)+1
            }
          }
        }]
        for(let i=0; i<this.defColumns.length; i++){
          //移除列
          let needRemoveKeywords = ['finishNumber','finishPurchaseNumber','snList','batchNumber','expirationDate','sku',
            'weight','position','brand','mfrs','otherField1','otherField2','otherField3','taxRate','remark']
          if(needRemoveKeywords.indexOf(this.defColumns[i].dataIndex)===-1) {
            let info = {}
            info.title = this.defColumns[i].title
            info.dataIndex = this.defColumns[i].dataIndex
            if(this.defColumns[i].width) {
              info.width = this.defColumns[i].width
            }
            if(this.defColumns[i].dataIndex === 'barCode') {
              info.scopedSlots = { customRender: 'customBarCode' }
            }
            currentCol.push(info)
          }
          //添加有数据的列
          if(needAddkeywords.indexOf(this.defColumns[i].dataIndex)>-1) {
            let info = {}
            info.title = this.defColumns[i].title
            info.dataIndex = this.defColumns[i].dataIndex
            if(this.defColumns[i].width) {
              info.width = this.defColumns[i].width
            }
            currentCol.push(info)
          }
        }
        this.columns = currentCol
      },
      //动态替换扩展字段
      handleChangeOtherField() {
        let mpStr = getMpListShort(Vue.ls.get('materialPropertyList'))
        if(mpStr) {
          let mpArr = mpStr.split(',')
          if(mpArr.length ===3) {
            this.otherFieldTitle = mpStr
            for (let i = 0; i < this.defColumns.length; i++) {
              if(this.defColumns[i].dataIndex === 'otherField1') {
                this.defColumns[i].title = mpArr[0]
              }
              if(this.defColumns[i].dataIndex === 'otherField2') {
                this.defColumns[i].title = mpArr[1]
              }
              if(this.defColumns[i].dataIndex === 'otherField3') {
                this.defColumns[i].title = mpArr[2]
              }
            }
          }
        }
      },
      initPlatform() {
        getPlatformConfigByKey({"platformKey": "bill_print_flag"}).then((res)=> {
          if (res && res.code === 200) {
            if(this.billType === '零售出库'||this.billType === '零售退货入库'||this.billType === '请购单'||
              this.billType === '采购订单'||this.billType === '采购入库'||this.billType === '采购退货出库'||
              this.billType === '销售订单'||this.billType === '销售出库'||this.billType === '销售退货入库'||
              this.billType === '其它入库'||this.billType === '其它出库'||this.billType === '调拨出库'||
              this.billType === '组装单'||this.billType === '拆卸单') {
              this.billPrintFlag = res.data.platformValue==='1'?true:false
            }
          }
        })
      },
      getSystemConfig() {
        getCurrentSystemConfig().then((res) => {
          if(res.code === 200 && res.data){
            this.purchaseBySaleFlag = res.data.purchaseBySaleFlag==='1'?true:false
            let multiBillType = res.data.multiBillType
            let multiLevelApprovalFlag = res.data.multiLevelApprovalFlag
            this.checkFlag = getCheckFlag(multiBillType, multiLevelApprovalFlag, this.prefixNo)
            if(res.data.auditPrintFlag==='1') {
              if(this.model.status === '0' || this.model.status === '9') {
                this.isShowPrintBtn = false
              } else {
                this.isShowPrintBtn = true
              }
            } else {
              this.isShowPrintBtn = true
            }
          }
        })
      },
      getBillListByLinkNumber(number) {
        getAction('/depotHead/getBillListByLinkNumber', {number: number}).then(res => {
          if(res && res.code === 200){
            this.linkNumberList = res.data
          }
        })
      },
      getFinancialBillNoByBillId(billId) {
        getAction('/accountHead/getFinancialBillNoByBillId', {billId: billId}).then(res => {
          if(res && res.code === 200){
            this.financialBillNoList = res.data
          }
        })
      },
      show(record, type, prefixNo) {
        //查询单条单据信息
        findBillDetailByNumber({ number: record.number }).then((res) => {
          if (res && res.code === 200) {
            let item = res.data
            this.billType = type
            this.title = getBillTypeLabel(this, type) + '-' + this.$t('common.view')
            this.prefixNo = prefixNo
            //附件下载
            this.fileList = item.fileName
            this.visible = true
            this.modalStyle = 'top:20px;height: 95%;'
            this.model = Object.assign({}, item)
            if (this.model.backAmount) {
              this.model.getAmount = (this.model.changeAmount + this.model.backAmount).toFixed(2)
            } else {
              this.model.getAmount = this.model.changeAmount
            }
            this.model.debt = (this.model.discountLastMoney + this.model.otherMoney - (this.model.deposit + this.model.changeAmount)).toFixed(2)
            this.$nextTick(() => {
              this.form.setFieldsValue(pick(this.model, 'id'))
            });
            let showType = 'basic'
            if(item.subType === '采购' || item.subType === '采购退货' || item.subType === '销售' || item.subType === '销售退货') {
              if (item.status === '3') {
                showType = 'other'
              }
            } else {
              if (item.status === '3') {
                showType = 'basic'
              } else if (item.purchaseStatus === '3') {
                showType = 'purchase'
              }
            }
            let isReadOnly = '1'
            if(item.subType === '组装单' || item.subType === '拆卸单') {
              isReadOnly = '0'
            }
            let params = {
              headerId: this.model.id,
              mpList: getMpListShort(Vue.ls.get('materialPropertyList')),  //扩展属性
              linkType: showType,
              isReadOnly: isReadOnly
            }
            let url = this.readOnly ? this.url.detailList : this.url.detailList;
            this.requestSubTableData(item, type, url, params);
            this.initPlatform()
            this.getSystemConfig()
            this.getBillListByLinkNumber(this.model.number)
            this.getFinancialBillNoByBillId(this.model.id)
          }
        })
      },
      requestSubTableData(record, type, url, params, success) {
        this.loading = true
        getAction(url, params).then(res => {
          if(res && res.code === 200){
            this.dataSource = res.data.rows
            this.initSetting(record, type, this.dataSource)
            typeof success === 'function' ? success(res) : ''
          }
        }).finally(() => {
          this.loading = false
        })
      },
      handleBackCheck() {
        let that = this
        this.$confirm({
          title: this.$t('common.confirmAction'),
          content: this.$t('common.unauditConfirm'),
          onOk: function () {
            that.loading = true
            postAction(that.url.batchSetStatusUrl, {status: '0', ids: that.model.id}).then((res) => {
              if(res.code === 200){
                that.$emit('ok')
                that.loading = false
                that.close()
              } else {
                that.$message.warning(res.data.message)
                that.loading = false
              }
            }).finally(() => {
            })
          }
        })
      },
      handleCancel() {
        this.close()
      },
      close() {
        this.$emit('close')
        this.visible = false
        this.modalStyle = ''
      },
      myHandleDetail(billNumber) {
        findBillDetailByNumber({ number: billNumber }).then((res) => {
          if (res && res.code === 200) {
            let type = res.data.type === "其它"? "":res.data.type
            this.show(res.data, res.data.subType + type);
            this.title = getBillTypeLabel(this, res.data.subType + type) + '- ' + this.$t('common.detail');
          }
        })
      },
      getPaymentTypeLabel(paymentType) {
        return getPaymentTypeLabel(this, paymentType)
      },
      myHandleFinancialDetail(billNo) {
        let that = this
        findFinancialDetailByNumber({ billNo: billNo }).then((res) => {
          if (res && res.code === 200) {
            if(that.$refs.financialDetailModal) {
              that.$refs.financialDetailModal.show(res.data, res.data.type);
              that.$refs.financialDetailModal.title= res.data.type + '- ' + this.$t('common.detail');
            }
          }
        })
      },
      getImgUrl(imgName, type) {
        if(imgName && imgName.split(',')) {
          type = type? type + '/':''
          return getFileAccessHttpUrl('systemConfig/static/' + type + imgName.split(',')[0])
        } else {
          return ''
        }
      },
      //三联打印新版
      handlePrintPro() {
        getPlatformConfigByKey({"platformKey": "bill_print_pro_url"}).then((res)=> {
          if (res && res.code === 200) {
            let billPrintUrl = res.data.platformValue + '&no=' + this.model.number
            let billPrintHeight = document.documentElement.clientHeight - 260
            this.$refs.modalProDetail.show(this.model, billPrintUrl, billPrintHeight)
            this.$refs.modalProDetail.title = this.billType + '-' + this.$t('common.printNew')
          }
        })
      },
      //三联打印
      handlePrint() {
        getPlatformConfigByKey({"platformKey": "bill_print_url"}).then((res)=> {
          if (res && res.code === 200) {
            let billPrintUrl = res.data.platformValue + '&no=' + this.model.number
            let billPrintHeight = this.dataSource.length*50 + 600
            this.$refs.modalDetail.show(this.model, billPrintUrl, billPrintHeight)
            this.$refs.modalDetail.title = this.billType + '-' + this.$t('common.print')
          }
        })
      },
      //零售出库|零售退货入库
      retailExportExcel() {
        let list = []
        let head = this.$t('common.depotName') + ',' + this.$t('common.barcode') + ',' + this.$t('common.name') + ',' + this.$t('common.specification') + ',' + this.$t('common.model') + ',' + this.$t('material.color') + ',' +
          this.otherFieldTitle + ',' + this.$t('purchase.form.columns.stock') + ',' + this.$t('common.unit') + ',' + this.$t('purchase.form.columns.serialNumber') + ',' + this.$t('purchase.form.columns.batchNumber') + ',' +
          this.$t('purchase.form.columns.expirationDate') + ',' + this.$t('purchase.form.columns.sku') + ',' + this.$t('purchase.form.columns.quantity') + ',' + this.$t('purchase.form.columns.unitPrice') + ',' +
          this.$t('purchase.form.columns.amount') + ',' + this.$t('common.remark')
        for (let i = 0; i < this.dataSource.length; i++) {
          let item = []
          let ds = this.dataSource[i]
          item.push(ds.depotName, ds.barCode, ds.name, ds.standard, ds.model, ds.color, ds.otherField1, ds.otherField2, ds.otherField3, ds.stock, ds.unit,
            ds.snList, ds.batchNumber, ds.expirationDate, ds.sku, ds.operNumber, ds.unitPrice, ds.allPrice, ds.remark)
          list.push(item)
        }
        let organName = this.model.organName? this.$t('common.memberCard') + this.model.organName: ''
        let tip = organName + ' ' + this.$t('common.billDate') + this.model.operTimeStr + ' ' + this.$t('common.billNo') + this.model.number
        exportXlsPost(this.billType + '_' + this.model.number, this.$t('common.billExport'), head, tip, list)
      },
      //请购单
      applyExportExcel() {
        let list = []
        let head = this.$t('common.barcode') + ',' + this.$t('common.name') + ',' + this.$t('common.specification') + ',' + this.$t('common.model') + ',' + this.$t('material.color') + ',' +
          this.otherFieldTitle + ',' + this.$t('common.unit') + ',' + this.$t('purchase.form.columns.sku') + ',' + this.$t('purchase.form.preNumber') + ',' + this.$t('purchase.finishPurchased') + ',' +
          this.$t('purchase.form.columns.quantity') + ',' + this.$t('common.remark')
        for (let i = 0; i < this.dataSource.length; i++) {
          let item = []
          let ds = this.dataSource[i]
          item.push(ds.barCode, ds.name, ds.standard, ds.model, ds.color, ds.otherField1, ds.otherField2, ds.otherField3, ds.unit, ds.sku,
            ds.preNumber, ds.finishNumber, ds.operNumber, ds.remark)
          list.push(item)
        }
        let tip = this.$t('common.billDate') + '：' + this.model.operTimeStr + ' ' + this.$t('common.billNo') + this.model.number
        exportXlsPost(this.billType + '_' + this.model.number, this.$t('common.billExport'), head, tip, list)
      },
      //采购订单|销售订单
      orderExportExcel() {
        let list = []
        let organType = ''
        let head = ''
        if(this.billType === '采购订单') {
          organType = this.$t('common.supplier') + '：'
          head = this.$t('common.barcode') + ',' + this.$t('common.name') + ',' + this.$t('common.specification') + ',' + this.$t('common.model') + ',' + this.$t('material.color') + ',' +
            this.otherFieldTitle + ',' + this.$t('purchase.form.columns.stock') + ',' + this.$t('common.unit') + ',' + this.$t('purchase.form.columns.sku') + ',' +
            this.$t('purchase.form.columns.quantity') + ',' + this.$t('purchase.finishPurchased') + ',' + this.$t('purchase.form.columns.unitPrice') + ',' + this.$t('purchase.form.columns.amount') + ',' +
            this.$t('system.taxRate') + ',' + this.$t('purchase.form.columns.taxAmount') + ',' + this.$t('purchase.form.columns.taxTotal') + ',' + this.$t('common.remark')
        } else if(this.billType === '销售订单') {
          organType = this.$t('common.customer') + '：'
          head = this.$t('common.barcode') + ',' + this.$t('common.name') + ',' + this.$t('common.specification') + ',' + this.$t('common.model') + ',' + this.$t('material.color') + ',' +
            this.otherFieldTitle + ',' + this.$t('purchase.form.columns.stock') + ',' + this.$t('common.unit') + ',' + this.$t('purchase.form.columns.sku') + ',' +
            this.$t('purchase.form.columns.quantity') + ',' + this.$t('purchase.finishPurchased') + ',' + this.$t('sales.partialSales') + ',' + this.$t('purchase.form.columns.unitPrice') + ',' +
            this.$t('purchase.form.columns.amount') + ',' + this.$t('system.taxRate') + ',' + this.$t('purchase.form.columns.taxAmount') + ',' + this.$t('purchase.form.columns.taxTotal') + ',' + this.$t('common.remark')
        }
        for (let i = 0; i < this.dataSource.length; i++) {
          let item = []
          let ds = this.dataSource[i]
          if(this.billType === '采购订单') {
            item.push(ds.barCode, ds.name, ds.standard, ds.model, ds.color, ds.otherField1, ds.otherField2, ds.otherField3, ds.stock, ds.unit, ds.sku,
              ds.operNumber, ds.finishNumber, ds.unitPrice, ds.allPrice, ds.taxRate, ds.taxMoney, ds.taxLastMoney, ds.remark)
          } else if(this.billType === '销售订单') {
            item.push(ds.barCode, ds.name, ds.standard, ds.model, ds.color, ds.otherField1, ds.otherField2, ds.otherField3, ds.stock, ds.unit, ds.sku,
              ds.operNumber, ds.finishPurchaseNumber, ds.finishNumber, ds.unitPrice, ds.allPrice, ds.taxRate, ds.taxMoney, ds.taxLastMoney, ds.remark)
          }
          list.push(item)
        }
        let organName = this.model.organName? this.model.organName: ''
        let tip = organType + organName + ' ' + this.$t('common.billDate') + this.model.operTimeStr + ' ' + this.$t('common.billNo') + this.model.number
        exportXlsPost(this.billType + '_' + this.model.number, this.$t('common.billExport'), head, tip, list)
      },
      //采购入库|采购退货出库|销售出库|销售退货入库
      purchaseSaleExportExcel() {
        let list = []
        let organType = ''
        if(this.billType === '采购入库' || this.billType === '采购退货出库') {
          organType = this.$t('common.supplier') + '：'
        } else if(this.billType === '销售出库' || this.billType === '销售退货入库') {
          organType = this.$t('common.customer') + '：'
        }
        let head = this.$t('common.depotName') + ',' + this.$t('common.barcode') + ',' + this.$t('common.name') + ',' + this.$t('common.specification') + ',' + this.$t('common.model') + ',' + this.$t('material.color') + ',' +
          this.otherFieldTitle + ',' + this.$t('purchase.form.columns.stock') + ',' + this.$t('common.unit') + ',' + this.$t('purchase.form.columns.serialNumber') + ',' + this.$t('purchase.form.columns.batchNumber') + ',' +
          this.$t('purchase.form.columns.expirationDate') + ',' + this.$t('purchase.form.columns.sku') + ',' + this.$t('purchase.form.columns.quantity') + ',' + this.$t('purchase.form.columns.unitPrice') + ',' +
          this.$t('purchase.form.columns.amount') + ',' + this.$t('system.taxRate') + ',' + this.$t('purchase.form.columns.taxAmount') + ',' + this.$t('purchase.form.columns.taxTotal') + ',' +
          this.$t('common.weight') + ',' + this.$t('common.remark')
        for (let i = 0; i < this.dataSource.length; i++) {
          let item = []
          let ds = this.dataSource[i]
          item.push(ds.depotName, ds.barCode, ds.name, ds.standard, ds.model, ds.color, ds.otherField1, ds.otherField2, ds.otherField3, ds.stock, ds.unit,
            ds.snList, ds.batchNumber, ds.expirationDate, ds.sku, ds.operNumber, ds.unitPrice, ds.allPrice, ds.taxRate, ds.taxMoney, ds.taxLastMoney, ds.weight, ds.remark)
          list.push(item)
        }
        let organName = this.model.organName? this.model.organName: ''
        let linkNumber = this.model.linkNumber? this.model.linkNumber: ''
        let tip = organType + organName + ' ' + this.$t('common.billDate') + this.model.operTimeStr + ' ' + this.$t('common.billNo') +
          this.model.number + '' + this.$t('common.linkedBill') + '：' + linkNumber
        exportXlsPost(this.billType + '_' + this.model.number, this.$t('common.billExport'), head, tip, list)
      },
      //其它入库|其它出库
      otherExportExcel() {
        let list = []
        let organType = ''
        if(this.billType === '其它入库') {
          organType = this.$t('common.supplier') + '：'
        } else if(this.billType === '其它出库') {
          organType = this.$t('common.customer') + '：'
        }
        let head = this.$t('common.depotName') + ',' + this.$t('common.barcode') + ',' + this.$t('common.name') + ',' + this.$t('common.specification') + ',' + this.$t('common.model') + ',' + this.$t('material.color') + ',' +
          this.otherFieldTitle + ',' + this.$t('purchase.form.columns.stock') + ',' + this.$t('common.unit') + ',' + this.$t('purchase.form.columns.serialNumber') + ',' + this.$t('purchase.form.columns.batchNumber') + ',' +
          this.$t('purchase.form.columns.expirationDate') + ',' + this.$t('purchase.form.columns.sku') + ',' + this.$t('purchase.form.columns.quantity') + ',' + this.$t('purchase.form.columns.unitPrice') + ',' +
          this.$t('purchase.form.columns.amount') + ',' + this.$t('common.remark')
        for (let i = 0; i < this.dataSource.length; i++) {
          let item = []
          let ds = this.dataSource[i]
          item.push(ds.depotName, ds.barCode, ds.name, ds.standard, ds.model, ds.color, ds.otherField1, ds.otherField2, ds.otherField3, ds.stock, ds.unit,
            ds.snList, ds.batchNumber, ds.expirationDate, ds.sku, ds.operNumber, ds.unitPrice, ds.allPrice, ds.remark)
          list.push(item)
        }
        let organName = this.model.organName? this.model.organName: ''
        let tip = organType + organName + ' ' + this.$t('common.billDate') + this.model.operTimeStr + ' ' + this.$t('common.billNo') + this.model.number
        exportXlsPost(this.billType + '_' + this.model.number, this.$t('common.billExport'), head, tip, list)
      },
      //调拨出库
      allocationOutExportExcel() {
        let list = []
        let head = this.$t('common.depotName') + ',' + this.$t('common.barcode') + ',' + this.$t('common.name') + ',' + this.$t('common.specification') + ',' + this.$t('common.model') + ',' + this.$t('material.color') + ',' +
          this.otherFieldTitle + ',' + this.$t('purchase.form.columns.stock') + ',' + this.$t('common.inboundDepot') + ',' + this.$t('common.unit') + ',' + this.$t('purchase.form.columns.sku') + ',' +
          this.$t('purchase.form.columns.quantity') + ',' + this.$t('purchase.form.columns.unitPrice') + ',' + this.$t('purchase.form.columns.amount') + ',' + this.$t('common.remark')
        for (let i = 0; i < this.dataSource.length; i++) {
          let item = []
          let ds = this.dataSource[i]
          item.push(ds.depotName, ds.barCode, ds.name, ds.standard, ds.model, ds.color, ds.otherField1, ds.otherField2, ds.otherField3, ds.stock, ds.anotherDepotName, ds.unit,
            ds.sku, ds.operNumber, ds.unitPrice, ds.allPrice, ds.remark)
          list.push(item)
        }
        let tip = this.$t('common.billDate') + '：' + this.model.operTimeStr + ' ' + this.$t('common.billNo') + this.model.number
        exportXlsPost(this.billType + '_' + this.model.number, this.$t('common.billExport'), head, tip, list)
      },
      //组装单|拆卸单
      assembleExportExcel() {
        let list = []
        let head = [this.$t('common.productType') + ',' + this.$t('common.depotName') + ',' + this.$t('common.barcode') + ',' + this.$t('common.name') + ',' + this.$t('common.specification') + ',' + this.$t('common.model') + ',' + this.$t('material.color') + ',' +
          this.otherFieldTitle + ',' + this.$t('purchase.form.columns.stock') + ',' + this.$t('common.unit') + ',' + this.$t('purchase.form.columns.sku') + ',' +
          this.$t('purchase.form.columns.quantity') + ',' + this.$t('purchase.form.columns.unitPrice') + ',' + this.$t('purchase.form.columns.amount') + ',' + this.$t('common.remark')]
        for (let i = 0; i < this.dataSource.length; i++) {
          let item = []
          let ds = this.dataSource[i]
          item.push(ds.mType, ds.depotName, ds.barCode, ds.name, ds.standard, ds.model, ds.color, ds.otherField1, ds.otherField2, ds.otherField3, ds.stock, ds.unit,
            ds.sku, ds.operNumber, ds.unitPrice, ds.allPrice, ds.remark)
          list.push(item)
        }
        let tip = this.$t('common.billDate') + '：' + this.model.operTimeStr + ' ' + this.$t('common.billNo') + this.model.number
        exportXlsPost(this.billType + '_' + this.model.number, this.$t('common.billExport'), head, tip, list)
      },
      //盘点复盘
      stockCheckReplayExportExcel() {
        let list = []
        let head = this.$t('common.depotName') + ',' + this.$t('common.barcode') + ',' + this.$t('common.name') + ',' + this.$t('common.specification') + ',' + this.$t('common.model') + ',' +
          this.otherFieldTitle + ',' + this.$t('purchase.form.columns.stock') + ',' + this.$t('common.unit') + ',' + this.$t('purchase.form.columns.sku') + ',' +
          this.$t('purchase.form.columns.quantity') + ',' + this.$t('purchase.form.columns.unitPrice') + ',' + this.$t('purchase.form.columns.amount') + ',' + this.$t('common.remark')
        for (let i = 0; i < this.dataSource.length; i++) {
          let item = []
          let ds = this.dataSource[i]
          item.push(ds.depotName, ds.barCode, ds.name, ds.standard, ds.model, ds.otherField1, ds.otherField2, ds.otherField3, ds.stock, ds.unit,
            ds.sku, ds.operNumber, ds.unitPrice, ds.allPrice, ds.remark)
          list.push(item)
        }
        let linkNumber = this.model.linkNumber? this.model.linkNumber: ''
        let tip = this.$t('common.billDate') + '：' + this.model.operTimeStr + ' ' + this.$t('common.billNo') + this.model.number + '' + this.$t('common.linkedBill') + '：' + linkNumber
        exportXlsPost(this.billType + '_' + this.model.number, this.$t('common.billExport'), head, tip, list)
      }
    }
  }
</script>

<style scoped>
  .item-info {
    float:left;
    width:30px;
    height:30px;
    margin-left:8px
  }
  .item-img {
    cursor:pointer;
    position: static;
    display: block;
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
</style>
