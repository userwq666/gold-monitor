<template>
  <div class="app">
    <nav class="nav" v-if="$route.meta.requiresAuth">
      <strong>金价监控</strong>
      <router-link to="/" :class="{ active: $route.path === '/' }">看板</router-link>
      <router-link to="/admin" :class="{ active: $route.path === '/admin' }">管理</router-link>
      <span class="spacer"></span>
      <span style="font-size:13px;color:#999;">{{ localStorageUsername }}</span>
      <button class="btn btn-sm" @click="logout">退出</button>
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
