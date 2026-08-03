<template>
  <div class="back-layout">
    <a-select class="language-select" :value="$i18n.locale" @change="changeLanguage">
      <a-select-option value="zh-CN">{{ $t('language.chinese') }}</a-select-option>
      <a-select-option value="en">{{ $t('language.english') }}</a-select-option>
    </a-select>
    <div id="userLayout" :class="['user-layout-wrapper', device]">
      <div class="container">
        <div class="poster-img">
          <img src="/static/rightImg.png?v=320">
        </div>
        <div class="right-form">
          <div class="top">
            <div class="header">
              <a-row>
                <a-col>
                  <a href="/">
                    <span class="title ignore">{{systemTitle}}</span>
                    <small class="desc">V1.0</small>
                  </a>
                </a-col>
              </a-row>
            </div>
          </div>
          <route-view></route-view>
        </div>
      </div>
    </div>
    <div class="footer" v-if="device === 'desktop'">
      <div class="third-party-platform" v-if="isShowRight">
        <div class="platform-info" @click="openAndroid()">
          <img src="/static/Android.png" style="height:30px" >
          <span>{{ $t('layout.android') }}</span>
        </div>
        <div style="width:50px"></div>
        <div class="platform-info" @click="openIPhone()">
          <img src="/static/iPhone.png" style="height:30px" >
          <span>{{ $t('layout.iphone') }}</span>
        </div>
        <div style="width:50px"></div>
        <div class="platform-info" @click="openMiniProgram()">
          <img src="/static/mini-program.png" style="height:30px" >
          <span>{{ $t('layout.miniProgram') }}</span>
        </div>
      </div>
      <p>
        <span v-if="this.isShowRight">科技</span>
        © # {{systemTitle}} - #
        <a style="color:#00458a; padding-right: 10px" :href="systemUrl" target="_blank">#</a>
        <span v-if="this.isShowRight"><a href="http://beian.miit.gov.cn/" target="_blank">#</a></span>
      </p>
    </div>
    <a-modal v-model="isAndroidShow" :title="$t('layout.downloadAndroid')" width="200" centered>
      <template slot="footer">
        <a-button key="back" @click="handleAndroidCancel">{{ $t('layout.cancel') }}</a-button>
      </template>
      <div class="platform-modal"><img src="/static/android-code.png" style="width:200px" /></div>
    </a-modal>
    <a-modal v-model="isIphoneShow" :title="$t('layout.downloadIphone')" width="200" centered>
      <template slot="footer">
        <a-button key="back" @click="handleIphoneCancel">{{ $t('layout.cancel') }}</a-button>
      </template>
      <div class="platform-modal"><img src="/static/iphone-code.png" style="width:200px" /></div>
    </a-modal>
    <a-modal v-model="isMiniProgramShow" :title="$t('layout.useMiniProgram')" width="200" centered>
      <template slot="footer">
        <a-button key="back" @click="handleMiniProgramCancel">{{ $t('layout.cancel') }}</a-button>
      </template>
      <div class="platform-modal"><img src="/static/weixin-code.png" style="width:200px;" /></div>
    </a-modal>
  </div>
</template>

<script>
  import RouteView from "@/components/layouts/RouteView"
  import { mixinDevice } from '@/utils/mixin.js'
  import { setLocale } from '@/locales'

  export default {
    name: "UserLayout",
    components: { RouteView },
    mixins: [mixinDevice],
    data () {
      return {
        systemTitle: window.SYS_TITLE,
        systemUrl: window.SYS_URL,
        isShowRight: false,
        isAndroidShow: false,
        isIphoneShow: false,
        isMiniProgramShow: false,
      }
    },
    mounted () {
      document.body.classList.add('userLayout')
    },
    beforeDestroy () {
      document.body.classList.remove('userLayout')
    },
    created () {
      let host = window.location.host
      if(host === 'cloud.gyjerp.com') {
        this.isShowRight = true
      } else {
        this.isShowRight = false
      }
    },
    methods: {
      changeLanguage(value) {
        setLocale(value)
      },
      handleAndroidCancel() {
        this.isAndroidShow = false
      },
      handleIphoneCancel() {
        this.isIphoneShow = false
      },
      handleMiniProgramCancel() {
        this.isMiniProgramShow = false
      },
      openAndroid() {
        this.isAndroidShow = true
      },
      openIPhone() {
        this.isIphoneShow = true
      },
      openMiniProgram() {
        this.isMiniProgramShow = true
      },
    }
  }
</script>

<style scoped>
  .back-layout {
    width: 100%;
    height: 100%;
    background-image: url(/static/bgimg.png?v=1);
    background-size: cover;
    background-repeat: no-repeat;
    position: relative;
    overflow: hidden;
  }
  .language-select {
    position: absolute;
    top: 10px;
    right: 16px;
    width: 110px;
    z-index: 1;
  }
  #userLayout.user-layout-wrapper.mobile {
    position: fixed;
    left: 6%;
    top: 10%;
    margin-left: 0px;
  }
  .third-party-platform {
    display: flex;
    flex-direction: row;
    justify-content: center;
    margin-bottom:15px;
    opacity:0.7
  }
  .third-party-platform .platform-info {
    display: flex;
    flex-direction: column;
    align-items: center;
    cursor: pointer;
    color:#1890ff
  }
  .platform-modal {
    padding:20px;
    margin:20px 50px;
    border:1px solid #eee;
  }
</style>
<style lang="less" scoped>
  #userLayout.user-layout-wrapper {
    position: fixed;
    left: 50%;
    top: 12%;
    margin-left: -543px;
    height: 100%;

    &.mobile {
      .container {
        .main {
          max-width: 368px;
          width: 98%;
        }
      }
      .poster-img {
        display: none;
      }
    }

    .container {
      float: left;
      width: 100%;
      z-index: 99;
      height: 70%;

      .poster-img {
        float: left;
        height: 100%;
      }

      .right-form {
        background-size: 100%;
        position: relative;
        width: 340px;
        height: 460px;
        background: rgba(255, 255, 255, 1);
        border-radius: 8px;
        right: 0;
        top: 0;
        padding: 10px 30px 0 30px;
        margin-top: 50px;
        -webkit-box-shadow: 0 2px 6px 0 rgb(200 200 200);
        box-shadow: 0 2px 6px 0 rgb(200 200 200);
        overflow: hidden;

        a {
          text-decoration: none;
        }

        .top {
          text-align: center;

          .header {
            height: 44px;
            line-height: 44px;
            margin-top: 35px;
            margin-bottom: 35px;
            .title {
              font-size: 35px;
              color: #666;
              font-family: "Chinese Quote", -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "Helvetica Neue", Helvetica, Arial, sans-serif, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol";
              font-weight: 700;
              position: relative;
              top: 2px;
            }
            .desc {
              font-size: 16px;
              color: #666;
              margin-top: 12px;
              margin-left: 10px;
              margin-bottom: 40px;
            }
          }
        }

        .main {
          min-width: 260px;
          width: 280px;
          margin: 0 auto;
        }
      }
    }
  }
  .footer {
    position: absolute;
    bottom: 0;
    padding: 0 16px;
    margin: 48px 0 12px;
    text-align: center;
    left: 33%;
    right: 33%;

    .links {
      margin-bottom: 8px;
      font-size: 14px;
      a {
        color: rgba(0, 0, 0, 0.45);
        transition: all 0.3s;
        &:not(:last-child) {
          margin-right: 40px;
        }
      }
    }
    .copyright {
      color: rgba(0, 0, 0, 0.45);
      font-size: 14px;
    }
  }
</style>
