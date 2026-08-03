<template>
  <div class="main">
    <keep-alive>
      <router-view v-if="keepAlive" :key="viewKey" />
    </keep-alive>
    <router-view v-if="!keepAlive" :key="viewKey" />
  </div>
</template>

<script>
  export default {
    name: "RouteView",
    computed: {
      keepAlive () {
        return this.$route.meta.keepAlive
      },
      viewKey () {
        return this.$route.fullPath + ':' + this.$i18n.locale
      }
    }
  }
</script>
