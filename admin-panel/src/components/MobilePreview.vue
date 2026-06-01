<template>
  <div class="phone-preview">
    <div class="phone-frame">
      <div class="phone-notch" />
      <div class="phone-screen">
        <div class="phone-header">
          <span>AppEnglish</span>
        </div>
        <div class="phone-content" v-html="html" />
        <div class="phone-home" />
      </div>
    </div>
    <div class="text-caption text-center mt-2 text-grey">Vista previa en celular</div>
  </div>
</template>

<script setup>
defineProps({ html: { type: String, default: '' } })
</script>

<style scoped>
.phone-preview { display: inline-flex; flex-direction: column; align-items: flex-start; flex-shrink: 0; }
.phone-frame {
  max-width: 240px; width: 240px; height: 480px;
  border: 3px solid #333; border-radius: 24px;
  background: #fff; overflow: hidden;
  position: relative;
  box-shadow: 0 4px 20px rgba(0,0,0,0.15);
  display: flex; flex-direction: column;
}
.phone-notch {
  width: 80px; height: 16px;
  background: #333; border-radius: 0 0 12px 12px;
  margin: 0 auto; flex-shrink: 0;
}
.phone-screen { flex: 1; overflow-y: auto; display: flex; flex-direction: column; }
.phone-header {
  background: #4CAF50; color: white;
  padding: 12px 16px; font-weight: bold; font-size: 16px;
  text-align: center; flex-shrink: 0;
}
.phone-content { flex: 1; overflow-y: auto; background: #f5f5f5; }

/* Subject/Topic/Unit/Block cards */
.phone-content :deep(.subject-card),
.phone-content :deep(.topic-card),
.phone-content :deep(.unit-card),
.phone-content :deep(.block-card) {
  margin: 0 12px 12px 12px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  cursor: pointer;
}

/* Bottom nav */
.phone-content :deep(.bottom-nav) {
  display: flex;
  justify-content: space-around;
  background: white;
  border-top: 1px solid #e0e0e0;
  padding: 8px 0;
}
.phone-content :deep(.nav-item) {
  text-align: center;
  flex: 1;
}
.phone-content :deep(.nav-icon) {
  font-size: 20px;
}
.phone-content :deep(.nav-label) {
  font-size: 10px;
  color: #757575;
  margin-top: 2px;
}
.phone-content :deep(.nav-item.active .nav-label) {
  color: #4CAF50;
}

/* Content styling for theory previews */
.phone-content :deep(body) {
  font-family: 'Roboto', sans-serif;
  font-size: 14px;
  color: #333;
  padding: 0;
  margin: 0;
  line-height: 1.6;
  background: #f5f5f5;
}
.phone-content :deep(h1) { font-size: 1.2em; margin: 10px 0 8px; color: #388E3C; border-bottom: 2px solid #C8E6C9; padding-bottom: 4px; }
.phone-content :deep(h2) { font-size: 1.1em; margin: 10px 0 6px; color: #4CAF50; font-weight: bold; }
.phone-content :deep(h3) { font-size: 1em; margin: 8px 0 4px; color: #4CAF50; }
.phone-content :deep(p) { margin: 4px 0 8px; font-size: 0.9em; }
.phone-content :deep(strong) { color: #2E7D32; }
.phone-content :deep(table) { border-collapse: collapse; width: 100%; margin: 8px 0; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
.phone-content :deep(th) { background: #4CAF50; color: white; padding: 8px 10px; text-align: left; font-size: 12px; font-weight: bold; }
.phone-content :deep(td) { padding: 6px 10px; border-bottom: 1px solid #E8F5E9; font-size: 12px; }
.phone-content :deep(tr:nth-child(even)) { background: #F1F8E9; }
.phone-content :deep(blockquote) { border-left: 4px solid #4CAF50; margin: 8px 0; padding: 6px 12px; color: #555; background: #F1F8E9; border-radius: 0 6px 6px 0; font-style: italic; }
.phone-content :deep(mark) { background: #FFF9C4; padding: 1px 4px; border-radius: 3px; color: #333; }
.phone-content :deep(img) { max-width: 100%; border-radius: 6px; }
.phone-content :deep(a) { color: #4CAF50; }
.phone-content :deep(ul), .phone-content :deep(ol) { padding-left: 16px; font-size: 0.85em; }
.phone-content :deep(.youtube-video) { margin: 6px 0; }
.phone-content :deep(.youtube-video iframe) { width: 100% !important; height: 110px !important; }
.phone-content :deep(.tip-box) { background: #FFF8E1; border-radius: 8px; padding: 8px 12px; margin: 8px 0; }
.phone-content :deep(.tip-box .tip-item) { display: flex; align-items: center; gap: 6px; margin-bottom: 4px; }
.phone-content :deep(hr) { border: none; border-top: 1px dashed #ccc; margin: 12px 0; }
.phone-home {
  width: 30px; height: 4px; background: #999; border-radius: 2px;
  margin: 8px auto 0; flex-shrink: 0;
}
</style>