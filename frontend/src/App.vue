<template>
  <div class="app">
    <nav class="nav" v-if="$route.meta.requiresAuth">
      <strong>GOLD MONITOR</strong>
      <router-link to="/" :class="{ active: $route.path === '/' }">看板</router-link>
      <router-link to="/admin" :class="{ active: $route.path === '/admin' }">管理</router-link>
      <span class="spacer"></span>
      <span style="font-size:12px;color:#4a5a7a;letter-spacing:0.5px;">{{ localStorageUsername }}</span>
      <button class="btn btn-sm" @click="logout" style="background:rgba(255,71,87,0.1);color:#ff6b7a;border:1px solid rgba(255,71,87,0.2);">退出</button>
    </nav>
    <div class="content">
      <router-view />
    </div>
  </div>
</template>

<script>
export default {
  name: 'App',
  computed: {
    localStorageUsername() {
      return localStorage.getItem('username') || ''
    }
  },
  methods: {
    logout() {
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      this.$router.push('/login')
    }
  }
}
</script>

<style>
.content { max-width: 1200px; margin: 0 auto; padding: 20px; }
</style>
