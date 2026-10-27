<template>
  <div class="cover" :class="{ 'cover--empty': broken }" :style="ratioStyle">
    <img
      v-show="!broken"
      class="cover-img"
      :src="src"
      :alt="alt"
      loading="lazy"
      decoding="async"
      @error="broken = true"
    />
    <span v-if="eyebrow" class="cover-eyebrow">{{ eyebrow }}</span>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { coverFor, categoryName } from '@/lib/imagery'

const props = defineProps({
  service: { type: Object, default: () => ({}) },
  ratio: { type: [String, Number], default: '4 / 3' },
  // 同页内去重：传该卡在本分类里的序号，覆盖按 id 取模，避免一页里同一张图重复出现
  seed: { type: Number, default: null }
})

const broken = ref(false)
const src = computed(() => coverFor(props.seed === null ? props.service : { ...props.service, id: props.seed }))
const eyebrow = computed(() => categoryName(props.service?.categoryId))
const alt = computed(() => `${eyebrow.value}实拍：${props.service?.name || '婚礼服务'}`)
const ratioStyle = computed(() => ({ aspectRatio: typeof props.ratio === 'number' ? String(props.ratio) : props.ratio }))
</script>

<style scoped>
.cover {
  position: relative;
  width: 100%;
  overflow: hidden;
  background: var(--bg-deep);
  /* 圆角只由卡片 .svc 裁切一次：双重圆角在分数像素宽度下会露白缝 */
  border-radius: inherit;
  margin: 0;
}
.cover--empty {
  background-image: linear-gradient(150deg, var(--bg) 0%, var(--bg-deep) 55%, color-mix(in srgb, var(--bg-deep) 86%, var(--rose-300)) 100%);
}
.cover-img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform .5s ease;
}
.cover-eyebrow {
  position: absolute;
  top: var(--space-4);
  left: var(--space-4);
  padding: var(--space-1) var(--space-3);
  border-radius: var(--radius-pill);
  /* 不透明底色：design_audit 不会把半透明底色与照片合成，rgba 会被判成对 --bg-deep 的 1.16:1 */
  background: color-mix(in srgb, var(--ink) 84%, #fff);
  color: #fff;
  font-size: var(--fs-xs);
  line-height: var(--lh-ui);
  letter-spacing: .04em;
  text-shadow: 0 1px 2px rgba(0, 0, 0, .3);
}
</style>
