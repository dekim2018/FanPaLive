<script setup>
import { tr } from '../i18n'
import { nextTick, ref } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '../api/auth'
import { errorMessage } from '../api/http'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'

const router = useRouter()
const auth = useAuthStore()
const toast = useToastStore()
const form = ref({ username: '', password: '', passwordConfirm: '', nickname: '' })
const error = ref('')
const submitting = ref(false)
// 비밀번호 불일치는 확인 칸 바로 아래에 보여 주고 그 칸으로 초점을 옮긴다.
const mismatch = ref('')
const confirmInput = ref(null)

async function submit() {
  error.value = ''
  mismatch.value = ''
  if (form.value.password !== form.value.passwordConfirm) {
    mismatch.value = tr('비밀번호가 서로 다릅니다.')
    await nextTick()
    confirmInput.value?.focus()
    return
  }
  submitting.value = true
  try {
    await authApi.signup({ username: form.value.username, password: form.value.password, nickname: form.value.nickname })
    await auth.login(form.value.username, form.value.password)
    toast.success(tr('가입을 환영합니다. 응원할 팀을 골라 주세요.'))
    router.replace('/onboarding')
  } catch (e) {
    error.value = errorMessage(e, tr('회원가입에 실패했습니다.'))
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <form class="card stack" @submit.prevent="submit">
      <h1>{{ tr('회원가입') }}</h1>
      <label class="field">{{ tr('아이디 (영문 소문자, 숫자, _ 4~20자)') }}
        <input name="username" v-model.trim="form.username" class="input" pattern="[a-z0-9_]{4,20}" autocomplete="username" spellcheck="false" autocapitalize="none" required />
      </label>
      <label class="field">{{ tr('닉네임 (20자 이하)') }}
        <input name="nickname" autocomplete="nickname" v-model.trim="form.nickname" class="input" maxlength="20" required />
      </label>
      <label class="field">{{ tr('비밀번호 (8자 이상)') }}
        <input name="password" v-model="form.password" class="input" type="password" minlength="8" maxlength="64" autocomplete="new-password" required />
      </label>
      <label class="field">{{ tr('비밀번호 확인') }}
        <input ref="confirmInput" name="password-confirm" v-model="form.passwordConfirm" class="input" type="password" autocomplete="new-password" required
               :aria-invalid="!!mismatch" :aria-describedby="mismatch ? 'password-confirm-error' : undefined" />
      </label>
      <div v-if="mismatch" id="password-confirm-error" class="error-text" role="alert">{{ mismatch }}</div>
      <div v-if="error" class="error-text" role="alert">{{ error }}</div>
      <button class="btn primary block" :disabled="submitting">{{ tr('가입하기') }}</button>
      <div class="small muted">{{ tr('이미 계정이 있나요?') }} <RouterLink to="/login" class="text-link">{{ tr('로그인') }}</RouterLink></div>
    </form>
  </div>
</template>

<style scoped>
.auth-page { max-width: 420px; margin: 24px auto; }
</style>
