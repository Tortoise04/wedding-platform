// 图片资产契约：路径与命名由资产侧固定，这里只做映射，不校验落盘。
export const CATEGORY_SLUG = { 1: 'venue', 2: 'package', 3: 'host', 4: 'photo', 5: 'dress', 6: 'car' }
export const CATEGORY_NAME = { 1: '场地', 2: '套餐', 3: '司仪', 4: '摄影', 5: '婚纱', 6: '婚车' }

const FALLBACK_SLUG = 'package'
const FALLBACK_NAME = '套餐'
const VARIANTS = 5

export function categorySlug(categoryId) {
  return CATEGORY_SLUG[Number(categoryId)] || FALLBACK_SLUG
}

export function categoryName(categoryId) {
  return CATEGORY_NAME[Number(categoryId)] || FALLBACK_NAME
}

// 同一 id 必须稳定映射到同一张图
export function coverFor(service) {
  const id = Number(service?.id)
  const n = (Number.isFinite(id) ? Math.abs(Math.trunc(id)) % VARIANTS : 0) + 1
  return `/img/${categorySlug(service?.categoryId)}/${n}.jpg`
}

export const heroImages = ['/img/hero/1.jpg', '/img/hero/2.jpg', '/img/hero/3.jpg']
export const newsImages = ['/img/news/1.jpg', '/img/news/2.jpg', '/img/news/3.jpg']

export function newsImageFor(index) {
  return newsImages[Math.abs(Number(index) || 0) % newsImages.length]
}
