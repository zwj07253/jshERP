<template>
  <a-modal
    :title="$t('menu.userList')"
    :width="1000"
    :visible="visible"
    :confirmLoading="confirmLoading"
    @ok="handleSubmit"
    @cancel="handleCancel">

    <a-table
      ref="table"
      bordered
      size="middle"
      rowKey="id"
      :columns="columns"
      :dataSource="dataSource"
      :pagination="ipagination"
      :loading="loading"
      :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"></a-table>
  </a-modal>
</template>

<script>
  import {getUserList} from '@/api/api'
  import {JeecgListMixin} from '@/mixins/JeecgListMixin'

  export default {
    name: "SelectUserListModal",
    mixins: [JeecgListMixin],
    data() {
      return {
        title: this.$t('common.action'),
        visible: false,
        model: {},
        confirmLoading: false,
        url: {
          add: "/act/model/create",
          list: "/sys/user/list"
        },
        columns: [
          {
            title: this.$t('common.username'),
            align: "center",
            dataIndex: 'username',
            fixed: 'left',
            width: 200
          },
          {
            title: this.$t('common.realname'),
            align: "center",
            dataIndex: 'realname',
          },
          {
            title: this.$t('common.sex'),
            align: "center",
            dataIndex: 'sex_dictText'
          },
          {
            title: this.$t('common.phoneNo'),
            align: "center",
            dataIndex: 'phone'
          },
          {
            title: this.$t('system.email'),
            align: "center",
            dataIndex: 'email'
          },
          {
            title: this.$t('common.status'),
            align: "center",
            dataIndex: 'status_dictText'
          }
        ]
      }
    },
    created() {
      //Step.2 加载用户数据
      getUserList().then((res) => {
        if (res.success) {
          this.dataSource = res.result.records;
          this.ipagination.total = res.result.total;
        }
      })
    },
    methods: {
      open() {
        this.visible = true;

        //Step.1 清空选中用户
        this.selectedRowKeys = []
        this.selectedRows = []
      },
      close() {
        this.$emit('close');
        this.visible = false;
      },
      handleChange(info) {
        let file = info.file;
        if (file.response.success) {
          this.$message.success(file.response.message);
          this.$emit('ok');
          this.close()
        } else {
          this.$message.warn(file.response.message);
          this.close()
        }

      },
      handleCancel() {
        this.close()
      },
      handleSubmit() {
        this.$emit('ok', this.selectionRows);
        this.close()
      },
    }
  }
</script>

<style>

</style>
