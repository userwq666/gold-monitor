<template>
  <div class="admin">
    <div class="card" style="margin-bottom:16px;">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;">
        <h3>发件号池</h3>
        <button class="btn btn-primary btn-sm" @click="showSenderForm = true">添加账号</button>
      </div>
      <div v-if="showSenderForm" class="form-row" style="padding:12px;background:#f8f9fa;border-radius:6px;margin-bottom:12px;">
        <input v-model="senderForm.username" placeholder="邮箱" style="flex:2;" />
        <input v-model="senderForm.password" type="password" placeholder="授权码" style="flex:2;" />
        <input v-model="senderForm.host" placeholder="smtp.qq.com" style="flex:1;" />
        <input v-model.number="senderForm.port" placeholder="587" type="number" style="width:70px;" />
        <button class="btn btn-success btn-sm" @click="saveSender">保存</button>
        <button class="btn btn-sm" @click="showSenderForm = false">取消</button>
      </div>
      <table>
        <thead><tr><th>邮箱</th><th>服务器</th><th>端口</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="s in senders" :key="s.id">
            <td>{{ s.username }}</td>
            <td>{{ s.host }}</td>
            <td>{{ s.port }}</td>
            <td><button class="btn btn-danger btn-sm" @click="deleteSender(s.id)">删除</button></td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="card" style="margin-bottom:16px;">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;">
        <h3>收件人管理</h3>
        <button class="btn btn-primary btn-sm" @click="showRecipientForm = true">添加收件人</button>
      </div>

      <div v-if="showRecipientForm" class="form-row" style="padding:12px;background:#f8f9fa;border-radius:6px;margin-bottom:12px;">
        <input v-model="recipientForm.email" placeholder="邮箱地址" style="flex:2;" />
        <input v-model="recipientForm.name" placeholder="姓名" style="flex:1;" />
        <button class="btn btn-success btn-sm" @click="saveRecipient">保存</button>
        <button class="btn btn-sm" @click="showRecipientForm = false">取消</button>
      </div>

      <table>
        <thead><tr><th>姓名</th><th>邮箱</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="r in recipients" :key="r.id">
            <td>{{ r.name }}</td>
            <td>{{ r.email }}</td>
            <td><span class="badge" :class="r.enabled ? 'badge-green' : 'badge-red'">{{ r.enabled ? '启用' : '禁用' }}</span></td>
            <td>
              <button class="btn btn-sm" @click="toggleRecipient(r)">{{ r.enabled ? '禁用' : '启用' }}</button>
              <button class="btn btn-danger btn-sm" @click="deleteRecipient(r.id)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="card">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;">
        <h3>账号管理</h3>
        <button class="btn btn-primary btn-sm" @click="showUserForm = true">添加账号</button>
      </div>

      <div v-if="showUserForm" class="form-row" style="padding:12px;background:#f8f9fa;border-radius:6px;margin-bottom:12px;">
        <input v-model="userForm.username" placeholder="用户名" style="flex:1;" />
        <input v-model="userForm.password" type="password" placeholder="密码" style="flex:1;" />
        <button class="btn btn-success btn-sm" @click="saveUser">保存</button>
        <button class="btn btn-sm" @click="showUserForm = false">取消</button>
      </div>

      <table>
        <thead><tr><th>用户名</th><th>角色</th><th>状态</th><th>创建时间</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="u in users" :key="u.id">
            <td>{{ u.username }}</td>
            <td>{{ u.role }}</td>
            <td><span class="badge" :class="u.enabled ? 'badge-green' : 'badge-red'">{{ u.enabled ? '启用' : '禁用' }}</span></td>
            <td style="font-size:13px;">{{ u.createdAt }}</td>
            <td>
              <button v-if="u.username !== 'admin'" class="btn btn-danger btn-sm" @click="deleteUser(u.id)">删除</button>
              <span v-else style="font-size:12px;color:#999;">不可删除</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="card" style="margin-top:16px;">
      <h3 style="margin-bottom:12px;">系统操作</h3>
      <div style="display:flex;gap:12px;">
        <button class="btn btn-primary btn-sm" @click="sendTestEmail">发送测试邮件</button>
        <button class="btn btn-success btn-sm" @click="fetchAndSend">抓取并发送通知</button>
      </div>
      <p v-if="actionMsg" style="margin-top:12px;font-size:13px;color:#27ae60;">{{ actionMsg }}</p>
    </div>
  </div>
</template>

<script>
import api from '../api'

export default {
  name: 'AdminView',
  data() {
    return {
      recipients: [],
      users: [],
      showRecipientForm: false,
      recipientForm: { email: '', name: '' },
      showUserForm: false,
      userForm: { username: '', password: '' },
      actionMsg: '',
      senders: [],
      showSenderForm: false,
      senderForm: { username: '', password: '', host: 'smtp.qq.com', port: 587 }
    }
  },
  mounted() { this.loadData() },
  methods: {
    async loadData() {
      try {
        const [r, u, s] = await Promise.all([api.getRecipients(), api.getUsers(), api.getSenderMails()])
        this.recipients = r.data
        this.users = u.data
        this.senders = s.data || []
      } catch (e) {}
    },
    async saveRecipient() {
      try {
        await api.addRecipient(this.recipientForm)
        this.showRecipientForm = false
        this.recipientForm = { email: '', name: '' }
        await this.loadData()
      } catch (e) { alert('保存失败') }
    },
    async toggleRecipient(r) {
      try {
        await api.updateRecipient(r.id, { ...r, enabled: !r.enabled })
        await this.loadData()
      } catch (e) {}
    },
    async deleteRecipient(id) {
      if (!confirm('确定删除此收件人？')) return
      try { await api.deleteRecipient(id); await this.loadData() } catch (e) {}
    },
    async saveUser() {
      try {
        await api.addUser(this.userForm)
        this.showUserForm = false
        this.userForm = { username: '', password: '' }
        await this.loadData()
      } catch (e) { alert(e.response?.data?.error || '保存失败') }
    },
    async deleteUser(id) {
      if (!confirm('确定删除此账号？')) return
      try { await api.deleteUser(id); await this.loadData() } catch (e) {}
    },
    async sendTestEmail() {
      this.actionMsg = ''
      try { await api.sendTestEmail(); this.actionMsg = '测试邮件已发送！' } catch (e) { alert('发送失败') }
    },
    async saveSender() {
      try {
        await api.addSenderMail(this.senderForm)
        this.showSenderForm = false
        this.senderForm = { username: '', password: '', host: 'smtp.qq.com', port: 587 }
        await this.loadData()
      } catch (e) { alert('保存失败') }
    },
    async deleteSender(id) {
      if (!confirm('确定删除？')) return
      try { await api.deleteSenderMail(id); await this.loadData() } catch (e) {}
    },
    async saveMailConfig() {
      this.mailMsg = ''
      try {
        await api.updateMailConfig(this.mailConfig)
        this.mailMsg = '邮箱配置已保存！'
      } catch (e) { alert('保存失败') }
    },
    async fetchAndSend() {
      this.actionMsg = ''
      try {
        const res = await api.manualFetch()
        this.actionMsg = res.data.message || '抓取完成'
      } catch (e) { alert('抓取失败') }
    }
  }
}
</script>

<style scoped>
.form-row { display: flex; gap: 8px; align-items: center; }
.mail-form { display: flex; flex-direction: column; gap: 10px; }
.mail-row { display: flex; align-items: center; gap: 12px; }
.mail-row label { width: 100px; font-size: 14px; color: #666; flex-shrink: 0; }
.mail-row input { flex: 1; }
</style>
