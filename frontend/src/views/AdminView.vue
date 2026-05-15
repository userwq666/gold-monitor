<template>
  <div class="admin">
    <div class="card">
      <div class="card-header">
        <div class="card-header-left">
          <h3>发件号池</h3>
          <span class="card-badge">{{ senders.length }} 个</span>
        </div>
        <button class="btn btn-primary btn-sm" @click="showSenderForm = true">+ 添加账号</button>
      </div>
      <div v-if="showSenderForm" class="inline-form">
        <input v-model="senderForm.username" placeholder="邮箱" />
        <input v-model="senderForm.password" type="password" placeholder="授权码" />
        <input v-model="senderForm.host" placeholder="smtp.qq.com" />
        <input v-model.number="senderForm.port" placeholder="587" type="number" style="width:80px;" />
        <button class="btn btn-success btn-sm" @click="saveSender">✓ 保存</button>
        <button class="btn btn-sm cancel-btn" @click="showSenderForm = false">取消</button>
      </div>
      <table>
        <thead><tr><th>邮箱</th><th>服务器</th><th>端口</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="s in senders" :key="s.id">
            <td class="cell-name">{{ s.username }}</td>
            <td class="cell-desc">{{ s.host }}</td>
            <td class="cell-desc">{{ s.port }}</td>
            <td><button class="btn-del" @click="deleteSender(s.id)">删除</button></td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="card">
      <div class="card-header">
        <div class="card-header-left">
          <h3>收件人管理</h3>
          <span class="card-badge">{{ recipients.length }} 人</span>
        </div>
        <button class="btn btn-primary btn-sm" @click="showRecipientForm = true">+ 添加收件人</button>
      </div>
      <div v-if="showRecipientForm" class="inline-form">
        <input v-model="recipientForm.email" placeholder="邮箱地址" />
        <input v-model="recipientForm.name" placeholder="姓名" />
        <button class="btn btn-success btn-sm" @click="saveRecipient">✓ 保存</button>
        <button class="btn btn-sm cancel-btn" @click="showRecipientForm = false">取消</button>
      </div>
      <table>
        <thead><tr><th>姓名</th><th>邮箱</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="r in recipients" :key="r.id">
            <td class="cell-name">{{ r.name }}</td>
            <td class="cell-desc">{{ r.email }}</td>
            <td><span class="badge" :class="r.enabled ? 'badge-green' : 'badge-red'">{{ r.enabled ? '启用' : '禁用' }}</span></td>
            <td>
              <button class="btn-toggle" @click="toggleRecipient(r)">{{ r.enabled ? '禁用' : '启用' }}</button>
              <button class="btn-del" @click="deleteRecipient(r.id)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="card">
      <div class="card-header">
        <div class="card-header-left">
          <h3>账号管理</h3>
          <span class="card-badge">{{ users.length }} 个</span>
        </div>
        <button class="btn btn-primary btn-sm" @click="showUserForm = true">+ 添加账号</button>
      </div>
      <div v-if="showUserForm" class="inline-form">
        <input v-model="userForm.username" placeholder="用户名" />
        <input v-model="userForm.password" type="password" placeholder="密码" />
        <button class="btn btn-success btn-sm" @click="saveUser">✓ 保存</button>
        <button class="btn btn-sm cancel-btn" @click="showUserForm = false">取消</button>
      </div>
      <table>
        <thead><tr><th>用户名</th><th>角色</th><th>状态</th><th>创建时间</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="u in users" :key="u.id">
            <td class="cell-name">{{ u.username }}</td>
            <td><span class="badge badge-blue">{{ u.role }}</span></td>
            <td><span class="badge" :class="u.enabled ? 'badge-green' : 'badge-red'">{{ u.enabled ? '启用' : '禁用' }}</span></td>
            <td class="cell-time">{{ u.createdAt }}</td>
            <td>
              <span v-if="u.username !== 'admin'" class="btn-del" @click="deleteUser(u.id)" style="cursor:pointer;padding:3px 10px;border-radius:6px;border:1px solid rgba(255,71,87,0.2);background:rgba(255,71,87,0.06);color:#ff6b7a;font-size:12px;font-weight:600;">删除</span>
              <span v-else style="font-size:11px;color:#4a5a7a;">不可删除</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="card">
      <h3 style="margin-bottom:14px;">系统操作</h3>
      <div class="action-row">
        <button class="btn btn-primary btn-sm" @click="sendTestEmail">✉ 发送测试邮件</button>
        <button class="btn btn-success btn-sm" @click="fetchAndSend">⟳ 抓取并发送通知</button>
      </div>
      <p v-if="actionMsg" class="action-msg">{{ actionMsg }}</p>
    </div>
  </div>
</template>

<script>
import api from '../api'

export default {
  name: 'AdminView',
  data() {
    return {
      recipients: [], users: [],
      showRecipientForm: false, recipientForm: { email: '', name: '' },
      showUserForm: false, userForm: { username: '', password: '' },
      actionMsg: '', senders: [],
      showSenderForm: false, senderForm: { username: '', password: '', host: 'smtp.qq.com', port: 587 }
    }
  },
  mounted() { this.loadData() },
  methods: {
    async loadData() {
      try {
        const [r, u, s] = await Promise.all([api.getRecipients(), api.getUsers(), api.getSenderMails()])
        this.recipients = r.data; this.users = u.data; this.senders = s.data || []
      } catch (e) {}
    },
    async saveRecipient() {
      try { await api.addRecipient(this.recipientForm); this.showRecipientForm = false; this.recipientForm = { email: '', name: '' }; await this.loadData() } catch (e) { alert('保存失败') }
    },
    async toggleRecipient(r) { try { await api.updateRecipient(r.id, { ...r, enabled: !r.enabled }); await this.loadData() } catch (e) {} },
    async deleteRecipient(id) { if (!confirm('确定删除？')) return; try { await api.deleteRecipient(id); await this.loadData() } catch (e) {} },
    async saveUser() {
      try { await api.addUser(this.userForm); this.showUserForm = false; this.userForm = { username: '', password: '' }; await this.loadData() } catch (e) { alert(e.response?.data?.error || '保存失败') }
    },
    async deleteUser(id) { if (!confirm('确定删除？')) return; try { await api.deleteUser(id); await this.loadData() } catch (e) {} },
    async sendTestEmail() { this.actionMsg = ''; try { await api.sendTestEmail(); this.actionMsg = '测试邮件已发送！' } catch (e) { alert('发送失败') } },
    async saveSender() {
      try { await api.addSenderMail(this.senderForm); this.showSenderForm = false; this.senderForm = { username: '', password: '', host: 'smtp.qq.com', port: 587 }; await this.loadData() } catch (e) { alert('保存失败') }
    },
    async deleteSender(id) { if (!confirm('确定删除？')) return; try { await api.deleteSenderMail(id); await this.loadData() } catch (e) {} },
    async fetchAndSend() { this.actionMsg = ''; try { const r = await api.manualFetch(); this.actionMsg = r.data.message || '抓取完成' } catch (e) { alert('抓取失败') } }
  }
}
</script>

<style scoped>
.admin { display: flex; flex-direction: column; gap: 14px; }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.card-header-left { display: flex; align-items: center; gap: 10px; }
.card-badge { font-size: 11px; color: #4a5a7a; font-weight: 600; padding: 2px 10px; border-radius: 20px; background: rgba(0, 212, 255, 0.06); border: 1px solid rgba(0, 212, 255, 0.1); }
h3 { font-size: 15px; color: #d0d8e8; font-weight: 700; letter-spacing: 0.5px; }
.inline-form { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; padding: 14px; background: rgba(8, 12, 24, 0.6); border-radius: 10px; margin-bottom: 14px; border: 1px solid rgba(0, 212, 255, 0.06); }
.inline-form input { flex: 1; min-width: 120px; }
.cancel-btn { background: rgba(90, 122, 154, 0.1); color: #5a7a9a; border: 1px solid rgba(90, 122, 154, 0.2); }
.action-row { display: flex; gap: 10px; }
.action-msg { margin-top: 12px; font-size: 13px; color: #00ff88; font-weight: 600; }
.cell-name { font-weight: 600; color: #e0e8f0; }
.cell-desc { font-size: 13px; color: #8a9ab0; }
.cell-time { font-size: 12px; color: #4a5a7a; }
.btn-toggle { padding: 3px 10px; border-radius: 6px; border: 1px solid rgba(0, 212, 255, 0.2); background: rgba(0, 212, 255, 0.06); color: #66e5ff; font-size: 12px; cursor: pointer; font-weight: 600; }
.btn-toggle:hover { background: rgba(0, 212, 255, 0.15); }
.btn-del { padding: 3px 10px; border-radius: 6px; border: 1px solid rgba(255, 71, 87, 0.2); background: rgba(255, 71, 87, 0.06); color: #ff6b7a; font-size: 12px; cursor: pointer; font-weight: 600; }
.btn-del:hover { background: rgba(255, 71, 87, 0.15); }
</style>
