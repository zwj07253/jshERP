<template>
  <!-- 定义在这里的参数都是不可在外部覆盖的，防止出现问题 -->
  <j-select-biz-component
    :value="value"
    :ellipsisLength="25"
    :listUrl="url.list"
    :columns="columns"
    v-on="$listeners"
    v-bind="attrs"
  />
</template>

<script>
  import JSelectBizComponent from './JSelectBizComponent'

  export default {
    name: 'JSelectMultiUser',
    components: { JSelectBizComponent },
    props: ['value'],
    data() {
      return {
        url: { list: '/sys/user/list' },
        columns: [
          { title: this.$t('common.realname'), align: 'center', width: '25%', widthRight: '70%', dataIndex: 'realname' },
          { title: this.$t('common.username'), align: 'center', width: '25%', dataIndex: 'username' },
          { title: this.$t('system.phone'), align: 'center', width: '20%', dataIndex: 'phone' },
          { title: this.$t('common.birthday'), align: 'center', width: '20%', dataIndex: 'birthday' }
        ],
        // 定义在这里的参数都是可以在外部传递覆盖的，可以更灵活的定制化使用的组件
        default: {
          name: this.$t('system.user'),
          width: 1200,
          displayKey: 'realname',
          returnKeys: ['id', 'username'],
          queryParamText: this.$t('common.username'),
        }
      }
    },
    computed: {
      attrs() {
        return Object.assign(this.default, this.$attrs)
      }
    }
  }
</script>

<style lang="less" scoped></style>