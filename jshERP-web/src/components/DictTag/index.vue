<template>
  <div>
    <template v-for="(item, index) in options">
      <template v-if="values.includes(item.value)">
        <span
          v-if="item.raw.listClass == 'default' || item.raw.listClass == ''"
          :index="index"
          :class="item.raw.cssClass"
          >{{ item.label }}</span>
        <a-tag
          v-else
          :index="index"
          :color="item.raw.listClass == 'grey' ? '' : item.raw.listClass"
          :class="item.raw.cssClass"
        >{{ item.label }}</a-tag>
      </template>
    </template>
    <template v-if="hasUnknownValues">
      <span>{{ unknownValues.join(', ') }}</span>
    </template>
  </div>
</template>

<script>
export default {
  name: "DictTag",
  props: {
    options: {
      type: Array,
      default: null,
    },
    value: [Number, String, Array]
  },
  computed: {
    values() {
      if (this.value !== null && typeof this.value !== 'undefined') {
        return Array.isArray(this.value) ? this.value : [String(this.value)];
      } else {
        return [];
      }
    },
    knownValues() {
      if (!this.options) return [];
      return this.options.map(item => item.value);
    },
    unknownValues() {
      return this.values.filter(v => !this.knownValues.includes(v));
    },
    hasUnknownValues() {
      return this.unknownValues.length > 0;
    },
  },
};
</script>
<style scoped>
.el-tag + .el-tag {
  margin-left: 10px;
}
</style>