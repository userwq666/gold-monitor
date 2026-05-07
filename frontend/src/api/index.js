import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 15000
})

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default {
  // Auth
  login(username, password) {
    return api.post('/auth/login', { username, password })
  },
  changePassword(oldPassword, newPassword) {
    return api.post('/auth/change-password', { oldPassword, newPassword })
  },

  // Gold Prices
  getLatestPrice(params) {
    return api.get('/gold-prices/latest', { params })
  },
  getQuotes() {
    return api.get('/gold-prices/quotes')
  },
  getHistory(params) {
    return api.get('/gold-prices/history', { params })
  },
  manualFetch(source) {
    return api.post('/gold-prices/fetch', { source })
  },

  // Alert Rules
  getAlertRules() {
    return api.get('/alert-rules')
  },
  createAlertRule(rule) {
    return api.post('/alert-rules', rule)
  },
  updateAlertRule(id, rule) {
    return api.put(`/alert-rules/${id}`, rule)
  },
  deleteAlertRule(id) {
    return api.delete(`/alert-rules/${id}`)
  },

  // Notifications
  getNotifications() {
    return api.get('/notifications')
  },
  sendTestEmail() {
    return api.post('/notifications/test')
  },

  // Admin - Recipients
  getRecipients() {
    return api.get('/admin/recipients')
  },
  addRecipient(data) {
    return api.post('/admin/recipients', data)
  },
  updateRecipient(id, data) {
    return api.put(`/admin/recipients/${id}`, data)
  },
  deleteRecipient(id) {
    return api.delete(`/admin/recipients/${id}`)
  },

  // Admin - Users
  getUsers() {
    return api.get('/admin/users')
  },
  addUser(user) {
    return api.post('/admin/users', user)
  },
  deleteUser(id) {
    return api.delete(`/admin/users/${id}`)
  },

  // Mail Config
  getMailConfig() {
    return api.get('/mail-config')
  },
  updateMailConfig(data) {
    return api.put('/mail-config', data)
  },
  setPreferredSource(source) {
    return api.put('/mail-config/preferred-source', { source })
  },

  // Admin - Sender Mails
  getSenderMails() {
    return api.get('/admin/senders')
  },
  addSenderMail(data) {
    return api.post('/admin/senders', data)
  },
  deleteSenderMail(id) {
    return api.delete(`/admin/senders/${id}`)
  },

  health() {
    return api.get('/health')
  },
  toggleCrawler(running) {
    return api.post('/crawler/toggle', { running })
  },
  getCrawlerStatus() {
    return api.get('/crawler/status')
  }
}
