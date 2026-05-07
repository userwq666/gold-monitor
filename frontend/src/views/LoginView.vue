<template>
  <div class="login-page">
    <div class="login-card card">
      <h2>金价监控系统</h2>
      <p style="color:#999;margin-bottom:20px;">请登录以继续</p>
      <form @submit.prevent="handleLogin">
        <div style="margin-bottom:12px;">
          <input v-model="username" placeholder="用户名" required style="width:100%;" />
        </div>
        <div style="margin-bottom:20px;">
          <input v-model="password" type="password" placeholder="密码" required style="width:100%;" />
        </div>
        <p v-if="error" style="color:#e74c3c;font-size:13px;margin-bottom:12px;">{{ error }}</p>
        <button type="submit" class="btn btn-primary" style="width:100%;justify-content:center;" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
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
.login-page { display: flex; justify-content: center; align-items: center; min-height: 80vh; }
.login-card { width: 360px; text-align: center; }
</style>
