<template>
  <div class="login-page">
    <div class="login-card card">
      <h2 class="login-title">GOLD MONITOR</h2>
      <p class="login-subtitle">金价监控系统 · 请登录</p>
      <form @submit.prevent="handleLogin">
        <div style="margin-bottom:14px;">
          <input v-model="username" placeholder="用户名" required style="width:100%;" />
        </div>
        <div style="margin-bottom:22px;">
          <input v-model="password" type="password" placeholder="密码" required style="width:100%;" />
        </div>
        <p v-if="error" class="login-error">{{ error }}</p>
        <button type="submit" class="btn btn-primary" style="width:100%;justify-content:center;padding:12px;" :disabled="loading">
          {{ loading ? '验证中...' : '登 录' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script>
import api from '../api'

export default {
  name: 'LoginView',
  data() {
    return { username: '', password: '', error: '', loading: false }
  },
  methods: {
    async handleLogin() {
      this.error = ''
      this.loading = true
      try {
        const res = await api.login(this.username, this.password)
        localStorage.setItem('token', res.data.token)
        localStorage.setItem('username', res.data.username)
        this.$router.push('/')
      } catch (e) {
        this.error = e.response?.data?.error || '登录失败，请检查网络'
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.login-page {
  display: flex; justify-content: center; align-items: center;
  min-height: 80vh;
  position: relative;
}
.login-page::before {
  content: '';
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: radial-gradient(ellipse at center, rgba(0, 212, 255, 0.04) 0%, transparent 60%);
  pointer-events: none;
}
.login-card { width: 380px; text-align: center; padding: 40px 32px; position: relative; }
.login-title {
  font-family: 'Orbitron', monospace;
  font-size: 22px; color: #00d4ff; margin-bottom: 6px;
  text-shadow: 0 0 30px rgba(0, 212, 255, 0.3);
  letter-spacing: 2px;
}
.login-subtitle { font-size: 13px; color: #4a5a7a; margin-bottom: 28px; }
.login-error { color: #ff6b7a; font-size: 13px; margin-bottom: 14px; }
</style>
