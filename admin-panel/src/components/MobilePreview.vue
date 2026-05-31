<template>
  <div class="phone-preview">
    <div class="phone-frame">
      <div class="phone-notch" />
      <div class="phone-screen">
        <div class="phone-header">
          <span class="text-caption">AppSchool</span>
        </div>
        <div class="phone-content" v-html="html" />
      </div>
      <div class="phone-home" />
    </div>
    <div class="text-caption text-center mt-2 text-grey">Vista previa en celular</div>
  </div>
</template>

<script setup>
defineProps({ html: { type: String, default: '' } })
</script>

<style scoped>
.phone-preview { display: flex; flex-direction: column; align-items: center; }
.phone-frame {
  width: 240px; height: 480px;
  border: 3px solid #333; border-radius: 24px;
  background: #fff; overflow: hidden;
  position: relative;
  box-shadow: 0 4px 20px rgba(0,0,0,0.15);
}
.phone-notch {
  width: 80px; height: 16px;
  background: #333; border-radius: 0 0 12px 12px;
  margin: 0 auto;
}
.phone-screen { padding: 12px; height: calc(100% - 50px); overflow-y: auto; }
.phone-header {
  text-align: center; padding: 4px 0 8px;
  border-bottom: 1px solid #eee; margin-bottom: 8px;
  color: #4CAF50; font-weight: bold;
}
.phone-content :deep(h1) { font-size: 1.2em; margin: 8px 0; }
.phone-content :deep(h2) { font-size: 1.05em; margin: 6px 0; }
.phone-content :deep(h3) { font-size: 0.95em; margin: 4px 0; }
.phone-content :deep(p) { font-size: 0.85em; margin: 4px 0; line-height: 1.4; }
.phone-content :deep(table) { border-collapse: collapse; width: 100%; font-size: 0.75em; margin: 6px 0; }
.phone-content :deep(th), .phone-content :deep(td) { border: 1px solid #ddd; padding: 4px; }
.phone-content :deep(th) { background: #f5f5f5; }
.phone-content :deep(blockquote) { border-left: 2px solid #4CAF50; margin: 6px 0; padding: 2px 8px; font-size: 0.8em; color: #666; }
.phone-content :deep(img) { max-width: 100%; border-radius: 6px; }
.phone-content :deep(a) { color: #4CAF50; }
.phone-content :deep(ul), .phone-content :deep(ol) { padding-left: 16px; font-size: 0.85em; }
.phone-content :deep(.youtube-video) { margin: 6px 0; }
.phone-content :deep(.youtube-video iframe) { width: 100% !important; height: 110px !important; }
.phone-content :deep(mark) { padding: 1px 3px; border-radius: 2px; }
.phone-home {
  width: 30px; height: 4px; background: #999; border-radius: 2px;
  margin: 8px auto 0;
}
</style>
