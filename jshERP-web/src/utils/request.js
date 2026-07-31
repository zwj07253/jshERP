import Vue from 'vue'
import axios from 'axios'
import store from '@/store'
import { VueAxios } from './axios'
import {Modal, notification} from 'ant-design-vue'
import { ACCESS_TOKEN } from "@/store/mutation-types"
import { i18n } from '@/locales'

/**
 * 【指定 axios的 baseURL】
 * 如果手工指定 baseURL: '/jshERP-boot'
 * 则映射后端域名，通过 vue.config.js
 * @type {*|string}
 */
let apiBaseUrl = window._CONFIG['domianURL'] || "/jshERP-boot";
//console.log("apiBaseUrl= ",apiBaseUrl)
// 创建 axios 实例
const service = axios.create({
  baseURL: apiBaseUrl, // api base_url
  timeout: 300000 // 请求超时时间
})

const err = (error) => {
  if (error.response) {
    let data = error.response.data
    const token = Vue.ls.get(ACCESS_TOKEN)
    switch (error.response.status) {
      case 403:
        notification.error({ message: i18n.t('common.systemPrompt'), description: i18n.t('common.accessDenied'),duration: 4})
        break
      case 500:
        if(token && data==="loginOut"){
          Modal.error({
            title: i18n.t('common.sessionExpired'),
            content: i18n.t('common.sessionExpiredMsg'),
            okText: i18n.t('common.relogin'),
            mask: false,
            onOk: () => {
              Vue.ls.remove(ACCESS_TOKEN)
              window.location.reload()
            }
          })
        }
        break
      case 404:
          notification.error({ message: i18n.t('common.systemPrompt'), description: i18n.t('common.resourceNotFound'),duration: 4})
        break
      case 504:
        notification.error({ message: i18n.t('common.systemPrompt'), description: i18n.t('common.networkTimeout')})
        break
      case 401:
        notification.error({ message: i18n.t('common.systemPrompt'), description: i18n.t('common.unauthorized'),duration: 4})
        if (token) {
          store.dispatch('Logout').then(() => {
            setTimeout(() => {
              window.location.reload()
            }, 1500)
          })
        }
        break
      default:
        notification.error({
          message: i18n.t('common.systemPrompt'),
          description: data.message,
          duration: 4
        })
        break
    }
  }
  return Promise.reject(error)
};

// request interceptor
service.interceptors.request.use(config => {
  const token = Vue.ls.get(ACCESS_TOKEN)
  if (token) {
    config.headers[ 'X-Access-Token' ] = token // 让每个请求携带自定义 token 请根据实际情况自行修改
  }
  return config
},(error) => {
  return Promise.reject(error)
})

// response interceptor
service.interceptors.response.use((response) => {
    return response.data
  }, err)

const installer = {
  vm: {},
  install (Vue, router = {}) {
    Vue.use(VueAxios, router, service)
  }
}

export {
  installer as VueAxios,
  service as axios
}