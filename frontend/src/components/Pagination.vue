<template>
  <div class="pagination" v-if="total > 1">
    <button :disabled="page <= 1" @click="change(page - 1)">上一页</button>
    <span>{{ page }} / {{ total }}</span>
    <button :disabled="page >= total" @click="change(page + 1)">下一页</button>
  </div>
</template>

<script setup>
/**
 * 通用分页组件
 *
 * 用法：
 * <Pagination :page="currentPage" :total="totalPages" @change="loadPage" />
 *
 * @emit change(page) — 点击页码时触发，传出新页码
 */
const props = defineProps({
  page: { type: Number, default: 1 },
  total: { type: Number, default: 1 }
})

const emit = defineEmits(['change'])

function change(p) {
  if (p >= 1 && p <= props.total) {
    emit('change', p)
  }
}
</script>

<style scoped>
.pagination {
  display: flex; justify-content: center; align-items: center; gap: 16px; margin-top: 20px;
}
.pagination button {
  padding: 6px 16px; border: 1px solid #ddd; border-radius: 6px;
  background: #fff; font-size: 13px; cursor: pointer;
}
.pagination button:disabled { opacity: 0.4; cursor: not-allowed; }
.pagination span { font-size: 13px; color: #666; }
</style>
