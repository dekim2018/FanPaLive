<script setup>
import { tr } from '../i18n'
</script>

<template>
  <div class="empty">
    <h1 style="font-size:20px">{{ tr('페이지를 찾을 수 없습니다') }}</h1>
    <p class="muted">{{ tr('주소가 바뀌었거나 삭제된 페이지입니다. 홈에서 다시 찾아보세요.') }}</p>
    <RouterLink to="/" class="btn sm primary">{{ tr('홈으로') }}</RouterLink>
  </div>
</template>
