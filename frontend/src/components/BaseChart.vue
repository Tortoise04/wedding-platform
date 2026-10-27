<template>
  <div ref="el" class="chart" :style="{ height }"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import echarts from '@/lib/echarts'

const props = defineProps({
  option: { type: Object, required: true },
  height: { type: String, default: '300px' }
})

const el = ref(null)
let chart = null

function render() {
  if (chart && props.option) chart.setOption(props.option, true)
}
function resize() { chart && chart.resize() }

onMounted(() => {
  chart = echarts.init(el.value)
  render()
  window.addEventListener('resize', resize)
})

watch(() => props.option, render, { deep: true })

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  chart && chart.dispose()
})
</script>

<style scoped>
.chart { width: 100%; }
</style>
