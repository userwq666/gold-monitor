<template>
  <div class="dashboard">
    <div class="price-header card">
      <div class="price-main">
        <div class="price-top-row">
          <span class="price-label">{{ priceLabel }}</span>
          <span class="price-badge" :class="priceUp ? 'badge-up' : (priceDown ? 'badge-down' : 'badge-flat')">
            {{ priceUp ? '▲ 上涨' : (priceDown ? '▼ 下跌' : '— 持平') }}
          </span>
        </div>
        <div class="price-row">
          <span class="price-value" :class="{ up: priceUp, down: priceDown }">
            {{ currentPrice ? currentPrice : '--' }}
          </span>
          <span class="price-unit">元/克</span>
        </div>
        <div class="price-meta">
          <span class="price-time">更新 {{ lastUpdate }}</span>
          <span class="price-divider">|</span>
          <span class="price-source">来源: {{ priceLabel }}</span>
        </div>
      </div>
      <div class="price-actions">
        <button class="btn btn-primary btn-sm" @click="manualFetch" :disabled="fetching">
          {{ fetching ? '抓取中...' : '⟳ 手动抓取' }}
        </button>
        <button class="btn btn-sm" @click="toggleCrawler" :class="autoRefresh ? 'btn-danger' : 'btn-success'">
          <span class="dot" :class="autoRefresh ? 'dot-red' : 'dot-green'"></span>
          {{ autoRefresh ? '自动运行中' : '开启爬虫' }}
        </button>
      </div>
    </div>

    <div class="card chart-card">
      <div class="card-header">
        <div class="card-header-left">
          <h3>价格走势</h3>
          <span class="card-badge">近 7 日</span>
        </div>
        <span class="card-hint">Au99.99 · 上海黄金交易所</span>
      </div>
      <div class="chart-wrap">
        <Line v-if="chartData" :data="chartData" :options="chartOptions" />
        <div v-else class="chart-empty">
          <span class="chart-empty-icon">📈</span>
          <span>等待数据采集...</span>
        </div>
      </div>
    </div>

    <div class="card source-card">
      <div class="source-inner">
        <div class="card-header-left">
          <h3>多源报价</h3>
          <span class="card-badge">{{ quoteCategoryLabel }}</span>
        </div>
        <div class="source-controls">
          <select v-model="quoteCategory" @change="onQuoteCategoryChange">
            <option value="bank">银行金条</option>
            <option value="recycle">回收金价</option>
            <option value="jewelry">品牌金价</option>
          </select>
          <select v-model="selectedSource" @change="onSourceChange">
            <option v-for="s in currentSources" :key="s.name" :value="s.name">{{ s.name }} — {{ s.price }}</option>
          </select>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <div class="card-header-left">
          <h3>通知规则</h3>
          <span class="card-badge">{{ rules.filter(r => r.enabled).length }} 条生效</span>
        </div>
        <button class="btn btn-primary btn-sm" @click="showRuleForm = true">+ 添加规则</button>
      </div>
      <div v-if="showRuleForm" class="rule-form">
        <div class="rule-form-grid">
          <input v-model="ruleForm.name" placeholder="规则名称" />
          <select v-model="ruleForm.type">
            <option value="THRESHOLD">阈值通知</option>
            <option value="CHANGE">涨跌幅通知</option>
            <option value="EXTREME">极值通知</option>
            <option value="SCHEDULED">定时报告</option>
          </select>
          <label v-if="ruleForm.type === 'THRESHOLD'" class="input-unit">
            <input v-model.number="ruleForm.threshold" placeholder="阈值" type="number" step="0.01" />
            <span>元/克</span>
          </label>
          <select v-if="ruleForm.type === 'THRESHOLD'" v-model="ruleForm.direction">
            <option value="ABOVE">高于</option>
            <option value="BELOW">低于</option>
          </select>
          <label v-if="ruleForm.type === 'CHANGE'" class="input-unit">
            <input v-model.number="ruleForm.threshold" placeholder="变动金额" type="number" step="0.01" />
            <span>元/克</span>
          </label>
          <input v-if="ruleForm.type === 'SCHEDULED'" v-model="ruleForm.time" type="time" />
        </div>
        <div class="rule-form-actions">
          <button class="btn btn-success btn-sm" @click="saveRule">✓ 保存</button>
          <button class="btn btn-sm cancel-btn" @click="showRuleForm = false">取消</button>
        </div>
      </div>
      <table>
        <thead>
          <tr><th>名称</th><th>类型</th><th>条件</th><th>状态</th><th>操作</th></tr>
        </thead>
        <tbody>
          <tr v-for="rule in rules" :key="rule.id">
            <td class="cell-name">{{ rule.name }}</td>
            <td><span class="badge" :class="typeBadge(rule.type)">{{ typeLabel(rule.type) }}</span></td>
            <td class="cell-desc">{{ ruleDescription(rule) }}</td>
            <td>
              <span class="badge" :class="rule.enabled ? 'badge-green' : 'badge-red'">
                {{ rule.enabled ? '启用' : '禁用' }}
              </span>
            </td>
            <td class="cell-actions">
              <button class="btn-toggle" @click="toggleRule(rule)">{{ rule.enabled ? '禁用' : '启用' }}</button>
              <button class="btn-del" @click="deleteRule(rule.id)">删除</button>
            </td>
          </tr>
          <tr v-if="rules.length === 0"><td colspan="5" class="empty-row">暂未添加规则</td></tr>
        </tbody>
      </table>
    </div>

    <div class="card">
      <div class="card-header">
        <div class="card-header-left">
          <h3>最近通知</h3>
          <span class="card-badge">{{ notifications.length }} 条</span>
        </div>
      </div>
      <table>
        <thead><tr><th>时间</th><th>类型</th><th>金价</th><th>消息</th><th>发送至</th></tr></thead>
        <tbody>
          <tr v-for="n in notifications" :key="n.id">
            <td class="cell-time">{{ n.sentAt }}</td>
            <td><span class="badge" :class="typeBadge(n.type)">{{ n.type }}</span></td>
            <td class="cell-price">{{ n.price }} 元/克</td>
            <td class="cell-msg">{{ n.message }}</td>
            <td class="cell-time">{{ n.sentTo }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script>
import { Line } from 'vue-chartjs'
import { Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend, Filler } from 'chart.js'
import api from '../api'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend, Filler)

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
      priceUp: false,
      priceDown: false,
      prevPrice: null,
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
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: 'rgba(8,12,24,0.9)',
            titleColor: '#5a7a9a',
            bodyColor: '#00d4ff',
            borderColor: 'rgba(0,212,255,0.2)',
            borderWidth: 1,
            padding: 10,
            bodyFont: { family: 'Orbitron' }
          }
        },
        scales: {
          x: {
            grid: { color: 'rgba(0,212,255,0.04)' },
            ticks: { autoSkip: true, maxTicksLimit: 12, color: '#4a5a7a', font: { size: 11 } }
          },
          y: {
            grid: { color: 'rgba(0,212,255,0.06)' },
            ticks: { color: '#4a5a7a', font: { size: 11 }, callback: v => v + ' 元' }
          }
        }
      }
    }
  },
  computed: {
    quoteCategoryLabel() {
      const map = { bank: '银行金条', recycle: '回收金价', jewelry: '品牌金价' }
      return map[this.quoteCategory] || ''
    },
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
      try { await api.toggleCrawler(running); this.autoRefresh = running } catch (e) {}
    },
    async checkCrawlerStatus() {
      try { const r = await api.getCrawlerStatus(); this.autoRefresh = r.data.running } catch (e) {}
    },
    async loadData() {
      await Promise.all([
        this.loadQuotes(),
        this.loadNotifications(),
        this.checkCrawlerStatus(),
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
          if (!res.data || !res.data.price) res = await api.getLatestPrice()
        } else {
          res = await api.getLatestPrice()
        }
        if (res.data && res.data.price) {
          this.prevPrice = this.currentPrice
          this.currentPrice = res.data.price
          this.lastUpdate = res.data.fetchTime || '--'
          if (source) this.priceLabel = res.data.source || source
          if (this.prevPrice) {
            this.priceUp = this.currentPrice > this.prevPrice
            this.priceDown = this.currentPrice < this.prevPrice
          }
        }
      } catch (e) {}
    },
    async loadHistory(source) {
      try {
        let prices = []
        if (source) {
          const r = await api.getHistory({ since: new Date(Date.now() - 2 * 3600000).toISOString(), source })
          prices = r.data
        }
        if (!prices || prices.length === 0) {
          const r = await api.getHistory({ since: new Date(Date.now() - 2 * 3600000).toISOString() })
          prices = r.data || []
        }
        if (prices.length > 0) {
          const alertTimes = (this.notifications || []).map(n => n.sentAt)
          this.chartData = {
            labels: prices.map(p => { const t = p.fetchTime || ''; return t.length > 16 ? t.slice(11, 16) : '' }),
            datasets: [{
              data: prices.map(p => p.price),
              borderColor: '#00d4ff',
              backgroundColor: (ctx) => {
                if (!ctx.chart?.chartArea) return 'rgba(0,212,255,0.05)'
                const g = ctx.chart.ctx.createLinearGradient(0, ctx.chart.chartArea.top, 0, ctx.chart.chartArea.bottom)
                g.addColorStop(0, 'rgba(0,212,255,0.15)')
                g.addColorStop(1, 'rgba(0,212,255,0.01)')
                return g
              },
              fill: true,
              tension: 0.4,
              pointRadius: prices.map(p => alertTimes.some(t => Math.abs(new Date(t).getTime() - new Date(p.fetchTime).getTime()) < 30000) ? 6 : 0),
              pointBackgroundColor: prices.map(p => alertTimes.some(t => Math.abs(new Date(t).getTime() - new Date(p.fetchTime).getTime()) < 30000) ? '#ff4757' : '#00d4ff'),
              pointBorderColor: '#fff',
              borderWidth: 2
            }]
          }
        }
      } catch (e) {}
    },
    async loadRules() { try { const r = await api.getAlertRules(); this.rules = r.data } catch (e) {} },
    async loadNotifications() { try { const r = await api.getNotifications(); this.notifications = r.data || [] } catch (e) {} },
    async loadQuotes() { try { const r = await api.getQuotes(); this.quotes = r.data } catch (e) {} },
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
        if (src) this.selectSource(src.name, parseFloat(src.price))
      }
    },
    async manualFetch() {
      this.fetching = true
      try { await api.manualFetch(this.selectedSource); await this.loadData() } catch (e) {}
      this.fetching = false
    },
    async saveRule() {
      try {
        await api.createAlertRule(this.ruleForm)
        this.showRuleForm = false
        this.ruleForm = { name: '', type: 'THRESHOLD', threshold: null, direction: 'ABOVE', time: '' }
        await this.loadRules()
      } catch (e) { alert('保存失败') }
    },
    async toggleRule(rule) {
      try { await api.updateAlertRule(rule.id, { ...rule, enabled: !rule.enabled }); await this.loadRules() } catch (e) {}
    },
    async deleteRule(id) {
      if (!confirm('确定删除此规则？')) return
      try { await api.deleteAlertRule(id); await this.loadRules() } catch (e) {}
    },
    startAutoRefresh() { this.refreshTimer = setInterval(() => this.loadData(), 30000) },
    stopAutoRefresh() { if (this.refreshTimer) { clearInterval(this.refreshTimer); this.refreshTimer = null } },
    typeLabel(t) { return { THRESHOLD: '阈值', CHANGE: '涨跌幅', EXTREME: '极值', SCHEDULED: '定时' }[t] || t },
    typeBadge(t) { return { THRESHOLD: 'badge-green', CHANGE: 'badge-blue', EXTREME: 'badge-red', SCHEDULED: 'badge-green' }[t] || '' },
    ruleDescription(r) {
      if (r.type === 'THRESHOLD') return `${r.direction === 'ABOVE' ? '高于' : '低于'} ${r.threshold} 元/克`
      if (r.type === 'CHANGE') return `变动超过 ${r.threshold} 元/克`
      if (r.type === 'EXTREME') return '7日内新高/新低'
      if (r.type === 'SCHEDULED') return `每日 ${r.time}`
      return ''
    }
  }
}
</script>

<style scoped>
.dashboard { display: flex; flex-direction: column; gap: 14px; }
.price-header { display: flex; justify-content: space-between; align-items: flex-start; padding: 24px 28px; }
.price-main { display: flex; flex-direction: column; gap: 6px; }
.price-top-row { display: flex; align-items: center; gap: 12px; }
.price-label { font-size: 13px; color: #5a7a9a; font-weight: 600; letter-spacing: 0.5px; }
.price-badge { font-size: 11px; font-weight: 700; padding: 2px 10px; border-radius: 20px; letter-spacing: 0.5px; }
.badge-up { background: rgba(255, 71, 87, 0.12); color: #ff6b7a; border: 1px solid rgba(255, 71, 87, 0.2); }
.badge-down { background: rgba(0, 255, 136, 0.1); color: #00ff88; border: 1px solid rgba(0, 255, 136, 0.2); }
.badge-flat { background: rgba(90, 122, 154, 0.1); color: #5a7a9a; border: 1px solid rgba(90, 122, 154, 0.2); }
.price-row { display: flex; align-items: baseline; gap: 6px; }
.price-value { font-size: 48px; font-weight: 700; font-family: 'Orbitron', monospace; letter-spacing: 2px; line-height: 1; }
.price-value.up { color: #ff4757; text-shadow: 0 0 40px rgba(255, 71, 87, 0.25); }
.price-value.down { color: #00ff88; text-shadow: 0 0 40px rgba(0, 255, 136, 0.25); }
.price-unit { font-size: 16px; color: #4a5a7a; font-weight: 600; }
.price-meta { display: flex; align-items: center; gap: 10px; margin-top: 2px; }
.price-time, .price-divider { font-size: 12px; color: #3a4a6a; }
.price-source { font-size: 12px; color: #00d4ff; font-weight: 600; }
.price-actions { display: flex; gap: 8px; align-items: center; flex-shrink: 0; }
.dot { display: inline-block; width: 7px; height: 7px; border-radius: 50%; }
.dot-green { background: #00ff88; box-shadow: 0 0 10px rgba(0, 255, 136, 0.7); animation: pulse 2s infinite; }
.dot-red { background: #ff4757; box-shadow: 0 0 10px rgba(255, 71, 87, 0.7); animation: pulse 2s infinite; }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.4; } }
.chart-card { margin-bottom: 0; }
.chart-wrap { height: 300px; position: relative; }
.chart-empty { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; color: #3a4a6a; gap: 8px; font-size: 13px; }
.chart-empty-icon { font-size: 32px; }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.card-header-left { display: flex; align-items: center; gap: 10px; }
.card-badge { font-size: 11px; color: #4a5a7a; font-weight: 600; padding: 2px 10px; border-radius: 20px; background: rgba(0, 212, 255, 0.06); border: 1px solid rgba(0, 212, 255, 0.1); }
.card-hint { font-size: 11px; color: #3a4a6a; font-family: 'Orbitron', monospace; letter-spacing: 1px; }
.source-card { margin-bottom: 0; }
.source-inner { display: flex; align-items: center; gap: 12px; }
.source-controls { display: flex; align-items: center; gap: 8px; margin-left: auto; }
.source-controls select { width: 130px; }
.source-controls select:last-child { width: 260px; }
.rule-form { margin-bottom: 14px; padding: 18px; background: rgba(8, 12, 24, 0.6); border-radius: 10px; border: 1px solid rgba(0, 212, 255, 0.06); }
.rule-form-grid { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 10px; }
.rule-form-actions { margin-top: 12px; display: flex; gap: 8px; }
.input-unit { display: flex; align-items: center; gap: 4px; }
.input-unit input { flex: 1; }
.input-unit span { font-size: 12px; color: #3a4a6a; white-space: nowrap; }
.cancel-btn { background: rgba(90, 122, 154, 0.1); color: #5a7a9a; border: 1px solid rgba(90, 122, 154, 0.2); }
.cell-name { font-weight: 600; color: #e0e8f0; }
.cell-desc { font-size: 13px; color: #8a9ab0; }
.cell-time { font-size: 12px; color: #4a5a7a; white-space: nowrap; }
.cell-price { font-family: 'Orbitron', monospace; font-size: 13px; letter-spacing: 0.5px; color: #00d4ff; font-weight: 600; }
.cell-msg { max-width: 300px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 13px; }
.cell-actions { display: flex; gap: 6px; }
.btn-toggle { padding: 3px 10px; border-radius: 6px; border: 1px solid rgba(0, 212, 255, 0.2); background: rgba(0, 212, 255, 0.06); color: #66e5ff; font-size: 12px; cursor: pointer; font-weight: 600; transition: all 0.2s; }
.btn-toggle:hover { background: rgba(0, 212, 255, 0.15); }
.btn-del { padding: 3px 10px; border-radius: 6px; border: 1px solid rgba(255, 71, 87, 0.2); background: rgba(255, 71, 87, 0.06); color: #ff6b7a; font-size: 12px; cursor: pointer; font-weight: 600; transition: all 0.2s; }
.btn-del:hover { background: rgba(255, 71, 87, 0.15); }
.empty-row { text-align: center; color: #3a4a6a; padding: 24px !important; font-size: 13px; }
tr.selected td { background: rgba(0, 212, 255, 0.06); }
h3 { font-size: 15px; color: #d0d8e8; font-weight: 700; letter-spacing: 0.5px; }
</style>
