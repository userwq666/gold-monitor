<template>
  <div class="dashboard">
    <div class="price-header card">
      <div class="price-info">
        <span class="price-label">{{ priceLabel }}</span>
        <span class="price-value">{{ currentPrice ? currentPrice + ' 元/克' : '--' }}</span>
        <span class="price-time">更新于: {{ lastUpdate }}</span>
      </div>
      <div class="price-actions">
        <button class="btn btn-primary btn-sm" @click="manualFetch" :disabled="fetching">
          {{ fetching ? '抓取中...' : '手动抓取' }}
        </button>
        <button class="btn btn-sm" @click="toggleCrawler" :class="autoRefresh ? 'btn-danger' : 'btn-success'">
          {{ autoRefresh ? '关闭自动抓取' : '开启自动抓取' }}
        </button>
      </div>
    </div>

    <div class="card" style="margin-bottom:16px;">
      <h3 style="margin-bottom:12px;">近7日走势</h3>
      <div style="height:300px;">
        <Line v-if="chartData" :data="chartData" :options="chartOptions" />
        <p v-else style="color:#999;text-align:center;padding:60px 0;">暂无数据</p>
      </div>
    </div>

    <div class="card" style="margin-bottom:16px;">
      <div style="display:flex;align-items:center;gap:12px;">
        <h3>多源报价</h3>
        <select v-model="quoteCategory" @change="onQuoteCategoryChange" style="width:120px;">
          <option value="bank">银行金条</option>
          <option value="recycle">回收金价</option>
          <option value="jewelry">品牌金价</option>
        </select>
        <select v-model="selectedSource" @change="onSourceChange" style="flex:1;">
          <option v-for="s in currentSources" :key="s.name" :value="s.name">{{ s.name }} — {{ s.price }}</option>
        </select>
      </div>
    </div>

    <div class="card">

      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;">
        <h3>通知规则</h3>
        <button class="btn btn-primary btn-sm" @click="showRuleForm = true">添加规则</button>
      </div>

      <div v-if="showRuleForm" class="rule-form" style="padding:16px;background:#f8f9fa;border-radius:6px;margin-bottom:16px;">
        <div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:12px;">
          <input v-model="ruleForm.name" placeholder="规则名称" />
          <select v-model="ruleForm.type">
            <option value="THRESHOLD">阈值通知</option>
            <option value="CHANGE">涨跌幅通知</option>
            <option value="EXTREME">极值通知</option>
            <option value="SCHEDULED">定时报告</option>
          </select>
          <label v-if="ruleForm.type === 'THRESHOLD'" style="display:flex;align-items:center;gap:4px;">
            <input v-model.number="ruleForm.threshold" placeholder="阈值" type="number" step="0.01" style="flex:1;" />
            <span style="font-size:13px;color:#666;white-space:nowrap;">元/克</span>
          </label>
          <select v-if="ruleForm.type === 'THRESHOLD'" v-model="ruleForm.direction">
            <option value="ABOVE">高于</option>
            <option value="BELOW">低于</option>
          </select>
          <label v-if="ruleForm.type === 'CHANGE'" style="display:flex;align-items:center;gap:4px;">
            <input v-model.number="ruleForm.threshold" placeholder="变动金额" type="number" step="0.01" style="flex:1;" />
            <span style="font-size:13px;color:#666;white-space:nowrap;">元/克</span>
          </label>
          <input v-if="ruleForm.type === 'SCHEDULED'" v-model="ruleForm.time" type="time" />
        </div>
        <div style="margin-top:12px;display:flex;gap:8px;">
          <button class="btn btn-success btn-sm" @click="saveRule">保存</button>
          <button class="btn btn-sm" @click="showRuleForm = false">取消</button>
        </div>
      </div>

      <table>
        <thead>
          <tr><th>名称</th><th>类型</th><th>条件</th><th>状态</th><th>操作</th></tr>
        </thead>
        <tbody>
          <tr v-for="rule in rules" :key="rule.id">
            <td>{{ rule.name }}</td>
            <td>
              <span class="badge" :class="typeBadge(rule.type)">{{ typeLabel(rule.type) }}</span>
            </td>
            <td>{{ ruleDescription(rule) }}</td>
            <td>
              <span class="badge" :class="rule.enabled ? 'badge-green' : 'badge-red'">
                {{ rule.enabled ? '启用' : '禁用' }}
              </span>
            </td>
            <td>
              <button class="btn btn-sm" @click="toggleRule(rule)">{{ rule.enabled ? '禁用' : '启用' }}</button>
              <button class="btn btn-danger btn-sm" @click="deleteRule(rule.id)">删除</button>
            </td>
          </tr>
          <tr v-if="rules.length === 0"><td colspan="5" style="text-align:center;color:#999;">暂未添加规则</td></tr>
        </tbody>
      </table>
    </div>

    <div class="card" style="margin-top:16px;">
      <h3 style="margin-bottom:12px;">最近通知</h3>
      <table>
        <thead><tr><th>时间</th><th>类型</th><th>金价</th><th>消息</th><th>发送至</th></tr></thead>
        <tbody>
          <tr v-for="n in notifications" :key="n.id">
            <td style="font-size:13px;">{{ n.sentAt }}</td>
            <td><span class="badge" :class="typeBadge(n.type)">{{ n.type }}</span></td>
            <td>{{ n.price }} 元/克</td>
            <td style="max-width:300px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">{{ n.message }}</td>
            <td style="font-size:13px;">{{ n.sentTo }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script>
import { Line } from 'vue-chartjs'
import { Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend } from 'chart.js'
import api from '../api'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend)

export default {
  name: 'DashboardView',
  components: { Line },
  data() {
    return {
      currentPrice: null,
      priceLabel: 'Au99.99 当前金价',
      lastUpdate: '--',
      fetching: false,
      autoRefresh: false,
      refreshTimer: null,
      chartData: null,
      rules: [],
      showRuleForm: false,
      ruleForm: { name: '', type: 'THRESHOLD', threshold: null, direction: 'ABOVE', time: '' },
      notifications: [],
      quotes: null,
      quoteCategory: 'bank',
      selectedSource: '上海黄金交易所 Au99.99',
      chartOptions: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          x: {
            grid: { display: false },
            ticks: { autoSkip: true, maxTicksLimit: 20 }
          },
          y: { grid: { color: '#f0f0f0' } }
        }
      }
    }
  },
  computed: {
    currentSources() {
      if (!this.quotes) return []
      const map = {
        bank: () => this.quotes.bank_bars.map(b => ({ name: b.bank, price: b.price + ' 元/克' })),
        recycle: () => this.quotes.recycle.map(r => ({ name: r.gold_type, price: r.recycle_price + ' 元/克' })),
        jewelry: () => this.quotes.jewelry.map(j => ({ name: j.brand, price: j.gold_price + ' 元/克' }))
      }
      return (map[this.quoteCategory] || (() => []))()
    }
  },
  mounted() {
    this.loadData()
    if (this.autoRefresh) this.startAutoRefresh()
  },
  beforeUnmount() {
    this.stopAutoRefresh()
  },
  watch: {
    autoRefresh(val) {
      if (val) this.startAutoRefresh()
      else this.stopAutoRefresh()
    }
  },
  methods: {
    async toggleCrawler() {
      const running = !this.autoRefresh
      try {
        await api.toggleCrawler(running)
        this.autoRefresh = running
      } catch (e) {}
    },
    async checkCrawlerStatus() {
      try {
        const res = await api.getCrawlerStatus()
        this.autoRefresh = res.data.running
      } catch (e) {}
    },
    async loadData() {
      await this.loadQuotes()
      await this.loadNotifications()
      await this.checkCrawlerStatus()
      await Promise.all([
        this.loadPrice(this.selectedSource),
        this.loadHistory(this.selectedSource),
        this.loadRules()
      ])
    },
    async loadPrice(source) {
      try {
        let res
        if (source) {
          res = await api.getLatestPrice({ source })
          if (!res.data || !res.data.price) {
            res = await api.getLatestPrice()
          }
        } else {
          res = await api.getLatestPrice()
        }
        if (res.data && res.data.price) {
          this.currentPrice = res.data.price
          this.lastUpdate = res.data.fetchTime || '--'
          if (source) {
            this.priceLabel = res.data.source || source
          }
        }
      } catch (e) {}
    },
    async loadHistory(source) {
      try {
        let prices = []
        if (source) {
          const res = await api.getHistory({ since: new Date(Date.now() - 2 * 3600000).toISOString(), source })
          prices = res.data
        }
        if (!prices || prices.length === 0) {
          const res = await api.getHistory({ since: new Date(Date.now() - 2 * 3600000).toISOString() })
          prices = res.data || []
        }
        if (prices.length > 0) {
          const alertTimes = (this.notifications || []).map(n => n.sentAt)
          this.chartData = {
            labels: prices.map(p => {
              const t = p.fetchTime || ''
              return t.length > 16 ? t.slice(11, 16) : ''
            }),
            datasets: [{
              data: prices.map(p => p.price),
              borderColor: '#1a73e8',
              backgroundColor: 'rgba(26,115,232,0.1)',
              fill: true,
              tension: 0.3,
              pointRadius: prices.map(p =>
                alertTimes.some(t => Math.abs(new Date(t).getTime() - new Date(p.fetchTime).getTime()) < 30000) ? 5 : 2
              ),
              pointBackgroundColor: prices.map(p =>
                alertTimes.some(t => Math.abs(new Date(t).getTime() - new Date(p.fetchTime).getTime()) < 30000) ? '#e74c3c' : '#1a73e8'
              )
            }]
          }
        }
      } catch (e) {}
    },
    async loadRules() {
      try {
        const res = await api.getAlertRules()
        this.rules = res.data
      } catch (e) {}
    },
    async loadNotifications() {
      try {
        const res = await api.getNotifications()
        this.notifications = res.data || []
      } catch (e) {}
    },
    async loadQuotes() {
      try {
        const res = await api.getQuotes()
        this.quotes = res.data
      } catch (e) {}
    },
    async selectSource(name, price) {
      this.selectedSource = name
      this.priceLabel = name
      this.currentPrice = price
      await this.loadHistory(name)
      api.setPreferredSource(name).catch(() => {})
    },
    onQuoteCategoryChange() {
      if (this.currentSources.length > 0) {
        const first = this.currentSources[0]
        this.selectSource(first.name, parseFloat(first.price))
      }
    },
    onSourceChange() {
      if (this.selectedSource) {
        const src = this.currentSources.find(s => s.name === this.selectedSource)
        if (src) {
          this.selectSource(src.name, parseFloat(src.price))
        }
      }
    },
    async manualFetch() {
      this.fetching = true
      try {
        await api.manualFetch(this.selectedSource)
        await this.loadData()
      } catch (e) {}
      this.fetching = false
    },
    async saveRule() {
      try {
        await api.createAlertRule(this.ruleForm)
        this.showRuleForm = false
        this.ruleForm = { name: '', type: 'THRESHOLD', threshold: null, direction: 'ABOVE', time: '' }
        await this.loadRules()
      } catch (e) {
        alert('保存失败')
      }
    },
    async toggleRule(rule) {
      try {
        await api.updateAlertRule(rule.id, { ...rule, enabled: !rule.enabled })
        await this.loadRules()
      } catch (e) {}
    },
    async deleteRule(id) {
      if (!confirm('确定删除此规则？')) return
      try {
        await api.deleteAlertRule(id)
        await this.loadRules()
      } catch (e) {}
    },
    startAutoRefresh() {
      this.refreshTimer = setInterval(() => this.loadData(), 30000)
    },
    stopAutoRefresh() {
      if (this.refreshTimer) { clearInterval(this.refreshTimer); this.refreshTimer = null }
    },
    typeLabel(type) {
      const labels = { THRESHOLD: '阈值', CHANGE: '涨跌幅', EXTREME: '极值', SCHEDULED: '定时' }
      return labels[type] || type
    },
    typeBadge(type) {
      const map = { THRESHOLD: 'badge-green', CHANGE: 'badge-blue', EXTREME: 'badge-red', SCHEDULED: 'badge-green' }
      return map[type] || ''
    },
    ruleDescription(rule) {
      if (rule.type === 'THRESHOLD') return `${rule.direction === 'ABOVE' ? '高于' : '低于'} ${rule.threshold} 元/克`
      if (rule.type === 'CHANGE') return `变动超过 ${rule.threshold} 元/克`
      if (rule.type === 'EXTREME') return '7日内新高/新低'
      if (rule.type === 'SCHEDULED') return `每日 ${rule.time}`
      return ''
    }
  }
}
</script>

<style scoped>
.price-header { display: flex; justify-content: space-between; align-items: center; }
.price-info { display: flex; flex-direction: column; gap: 4px; }
.price-label { font-size: 14px; color: #666; }
.price-value { font-size: 36px; font-weight: 700; }
.price-value.up { color: #e74c3c; }
.price-value.down { color: #27ae60; }
.price-change { font-size: 14px; }
.price-time { font-size: 12px; color: #999; }
.price-actions { display: flex; gap: 8px; }
tr.selected { background: #e3f2fd !important; }
</style>
