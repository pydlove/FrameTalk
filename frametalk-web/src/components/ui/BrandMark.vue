<script setup lang="ts">
withDefaults(
  defineProps<{
    size?: number
    decorative?: boolean
  }>(),
  {
    size: 24,
    decorative: true,
  },
)
</script>

<template>
  <img
    class="brand-mark"
    src="/brand/frame-talk-nav-mark.svg"
    :width="size"
    :height="size"
    :alt="decorative ? '' : '帧言'"
    :aria-hidden="decorative || undefined"
  />
</template>

<style scoped>
.brand-mark {
  display: block;
  flex: 0 0 auto;
  border-radius: 22.5%;
}
</style>
