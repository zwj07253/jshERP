<!-- b y 7 5 2 7  1 8 9 2 0 -->
<template>
  <div class="main user-layout-register" :style="mainStyle">
    <a-form ref="formRegister" :autoFormCreate="(form)=>{this.form = form}" id="formRegister">
      <a-form-item
        fieldDecoratorId="username"
        :fieldDecoratorOptions="{rules: [{ required: true, message: this.$t('common.usernameRequired')}, { validator: this.handleUserName}], validateTrigger: ['change', 'blur'], validateFirst: true}">
        <a-input size="large" type="text" autocomplete="false"
                 :placeholder="$t('common.usernamePlaceholder')"></a-input>
      </a-form-item>

      <a-popover placement="rightTop" trigger="click" :visible="state.passwordLevelChecked">
        <template slot="content">
          <div :style="{ width: '240px' }">
            <div :class="['user-register', passwordLevelClass]">{{$t('common.strength')}}：<span>{{ passwordLevelName }}</span></div>
            <a-progress :percent="state.percent" :showInfo="false" :strokeColor=" passwordLevelColor "/>
            <div style="margin-top: 10px;">
              <span>{{$t('common.passwordHint')}}</span>
            </div>
          </div>
        </template>
        <a-form-item
          fieldDecoratorId="password"
          :fieldDecoratorOptions="{rules: [{ required: false}, { validator: this.handlePasswordLevel }], validateTrigger: ['change', 'blur'], validateFirst: true}">
          <a-input-password size="large" type="password" @click="handlePasswordInputClick" autocomplete="false" :placeholder="$t('common.atLeast6')"></a-input-password>
        </a-form-item>
      </a-popover>

      <a-form-item
        fieldDecoratorId="password2"
        :fieldDecoratorOptions="{rules: [{ required: true, message: this.$t('common.passwordRequired') }, { validator: this.handlePasswordCheck }], validateTrigger: ['change', 'blur'], validateFirst: true}">

        <a-input-password size="large" type="password" autocomplete="false" :placeholder="$t('common.confirmPassword')"></a-input-password>
      </a-form-item>

      <a-row :gutter="0" v-if="checkcodeFlag==='1'">
        <a-col :span="14">
          <a-form-item
            fieldDecoratorId="inputCode"
            :fieldDecoratorOptions="{rules: [{ required: true, message: this.$t('common.codeRequired')}, { validator: this.handleInputCode}], validateTrigger: ['change', 'blur'], validateFirst: true}">
            <a-input
              size="large"
              type="text"
              default-value=""
              :placeholder="$t('common.codePlaceholder')">
              <a-icon slot="prefix" type="smile" :style="{ color: 'rgba(0,0,0,.25)' }"/>
            </a-input>
          </a-form-item>
        </a-col>
        <a-col :span="10" style="text-align: right">
          <img v-if="requestCodeSuccess" style="margin-top: 2px;" :src="randCodeImage" @click="handleChangeCheckCode"/>
          <img v-else style="margin-top: 2px;" src="../../assets/checkcode.png" @click="handleChangeCheckCode"/>
        </a-col>
      </a-row>

      <a-form-item :style="btnStyle">
        <a-button
          size="large"
          type="primary"
          htmlType="submit"
          class="register-button"
          :loading="registerBtn"
          @click.stop.prevent="handleSubmit"
          :disabled="registerBtn">{{$t('common.registerTenant')}}
        </a-button>
        <router-link class="login" :to="{ name: 'login' }">{{$t('common.loginExisting')}}</router-link>
      </a-form-item>

      <div class="login-copyright" v-if="device === 'mobile'">
        <a-row>
          <a-col>
            © 2015-2030 Powered By
            <a style="color:#00458a;" :href="systemUrl" target="_blank">{{$t('common.officialSite')}}</a>
          </a-col>
        </a-row>
      </div>
    </a-form>
  </div>
</template>
<!-- BY cao_yu_li -->
<script>
  import {mixinDevice} from '@/utils/mixin.js'
  import {getAction, postAction} from '@/api/manage'
  import md5 from 'md5'

  const levelClass = {
    0: 'error',
    1: 'error',
    2: 'warning',
    3: 'success'
  }
  const levelColor = {
    0: '#ff0000',
    1: '#ff0000',
    2: '#ff7e05',
    3: '#52c41a',
  }
  export default {
    name: "Register",
    components: {},
    mixins: [mixinDevice],
    data() {
      return {
        systemTitle: window.SYS_TITLE,
        systemUrl: window.SYS_URL,
        form: null,
        uuid:'',
        randCodeImage:'',
        checkcodeFlag:'',
        mainStyle: '',
        btnStyle: '',
        requestCodeSuccess:false,
        state: {
          time: 60,
          smsSendBtn: false,
          passwordLevel: 0,
          passwordLevelChecked: false,
          percent: 10,
          progressColor: '#FF0000'
        },
        registerBtn: false,
      }
    },
    computed: {
      passwordLevelClass() {
        return levelClass[this.state.passwordLevel]
      },
      passwordLevelName() {
        const levelNames = {
          0: this.$t('common.strengthLow'),
          1: this.$t('common.strengthLow'),
          2: this.$t('common.strengthMedium'),
          3: this.$t('common.strengthStrong')
        }
        return levelNames[this.state.passwordLevel]
      },
      passwordLevelColor() {
        return levelColor[this.state.passwordLevel]
      }
    },
    created () {
      this.getCheckcodeFlag()
      this.handleChangeCheckCode();
    },
    methods: {
      getCheckcodeFlag(){
        getAction('/platformConfig/getPlatform/checkcodeFlag').then((res) => {
          this.checkcodeFlag = res + ''
          if(this.checkcodeFlag === '1') {
            this.mainStyle = ''
            this.btnStyle = ''
          } else {
            this.mainStyle = 'padding-top:20px'
            this.btnStyle = 'margin-top:20px'
          }
        })
      },
      handleChangeCheckCode(){
        this.currdatetime = new Date().getTime();
        getAction('/user/randomImage').then(res=>{
          if(res.code == 200){
            this.uuid = res.data.uuid;
            this.randCodeImage = res.data.base64;
            this.requestCodeSuccess=true
          }else{
            this.$message.error(res.data)
            this.requestCodeSuccess=false
          }
        }).catch(()=>{
          this.requestCodeSuccess=false
        })
      },
      handleUserName(rule, value, callback) {
        let reg = /^(?=.*[a-z]).{4,}$/;
        if (!reg.test(value)) {
          callback(new Error(this.$t('common.usernameFormat')))
        }
        callback()
      },
      handlePasswordLevel(rule, value, callback) {

        let level = 0
        let reg = /^(?=.*[a-z])(?=.*\d).{6,}$/;
        if (!reg.test(value)) {
          callback(new Error(this.$t('common.passwordFormat')))
        }
        // 判断这个字符串中有没有数字
        if (/[0-9]/.test(value)) {
          level++
        }
        // 判断字符串中有没有字母
        if (/[a-zA-Z]/.test(value)) {
          level++
        }
        // 判断字符串中有没有特殊符号
        if (/[^0-9a-zA-Z_]/.test(value)) {
          level++
        }
        this.state.passwordLevel = level
        this.state.percent = level * 30
        if (level >= 2) {
          if (level >= 3) {
            this.state.percent = 100
          }
          callback()
        } else {
          if (level === 0) {
            this.state.percent = 10
          }
          callback(new Error(this.$t('common.passwordStrengthLow')))
        }
      },

      handlePasswordCheck(rule, value, callback) {
        let password = this.form.getFieldValue('password')
        //console.log('value', value)
        if (value === undefined) {
          callback(new Error(this.$t('common.enterPassword')))
        }
        if (value && password && value.trim() !== password.trim()) {
          callback(new Error(this.$t('common.passwordsNotMatch')))
        }
        callback()
      },

      handlePasswordInputClick() {
        if (!this.isMobile()) {
          this.state.passwordLevelChecked = true
          return;
        }
        this.state.passwordLevelChecked = false
      },

      handleInputCode(rule, value, callback) {
        callback()
      },

      handleSubmit() {
        let that = this
        that.registerBtn = true;
        this.form.validateFields((err, values) => {
          if (!err) {
            let register = {
              loginName: values.username,
              password: md5(values.password),
              code: values.inputCode,
              uuid: that.uuid
            };
            postAction("/user/registerUser", register).then((res) => {
              if(res.code === 200){
                this.$notification.success({
                  message: this.$t('common.confirm'),
                  description: this.$t('common.registeredSuccess'),
                  duration: 5
                });
                let that = this;
                setTimeout(function () {
                  that.$router.push({ name: "login", params:{
                      loginName: register.loginName
                    }
                  })
                },2000);
              } else {
                this.$notification['error']({
                  message: this.$t('common.confirm'),
                  description: res.data.message || this.$t('common.registeredFailed'),
                  duration: 2
                });
                //验证码刷新
                this.form.setFieldsValue({'inputCode':''})
                this.handleChangeCheckCode()
                that.registerBtn = false
              }
            }).catch((err) => {
              that.requestFailed(err);
            })
          } else {
            that.registerBtn = false
          }
        })
      },
      requestFailed(err) {
        this.$notification['error']({
          message: this.$t('common.error'),
          description: ((err.response || {}).data || {}).message || this.$t('common.requestError'),
          duration: 4,
        });
        this.registerBtn = false;
      }
    },
    watch: {
      'state.passwordLevel'(val) {
        console.log(val)

      }
    }
  }
</script>
<style lang="less">
  .user-register {

    &.error {
      color: #ff0000;
    }

    &.warning {
      color: #ff7e05;
    }

    &.success {
      color: #52c41a;
    }

  }

  .user-layout-register {
    .ant-input-group-addon:first-child {
      background-color: #fff;
    }
  }
</style>
<style lang="less" scoped>
  .user-layout-register {

    .ant-form-item {
      margin-bottom: 16px;
    }

    & > h3 {
      font-size: 16px;
      margin-bottom: 20px;
    }

    .getCaptcha {
      display: block;
      width: 100%;
      height: 40px;
    }

    .register-button {
      width: 50%;
    }

    .login {
      float: right;
      line-height: 40px;
      font-weight: bolder;
    }
  }
  .login-copyright {
    text-align: center;
    margin-top: 20px
  }
  .login-copyright, .login-copyright a {
    color: #666
  }

  .login-copyright .weixin {
    padding-left:10px;
    color: red;
    cursor:pointer
  }
</style>