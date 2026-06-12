// ── Config ────────────────────────────────────────────────────────────────
const API_BASE = 'http://localhost:8080/api';

// ── Auth ──────────────────────────────────────────────────────────────────
const Auth = {
  getToken:   () => localStorage.getItem('et_token'),
  getUser:    () => JSON.parse(localStorage.getItem('et_user') || 'null'),
  isLoggedIn: () => !!localStorage.getItem('et_token'),
  save(token, user) {
    localStorage.setItem('et_token', token);
    localStorage.setItem('et_user', JSON.stringify(user));
  },
  logout() {
    localStorage.removeItem('et_token');
    localStorage.removeItem('et_user');
    window.location.href = 'index.html';
  },
  requireAuth() {
    if (!this.isLoggedIn()) { window.location.href = 'index.html'; return false; }
    return true;
  }
};

// ── API client ────────────────────────────────────────────────────────────
const api = {
  async request(method, path, body = null, params = null) {
    const url = new URL(API_BASE + path);
    if (params) {
      Object.entries(params).forEach(([k, v]) => {
        if (v !== null && v !== undefined && v !== '') url.searchParams.set(k, v);
      });
    }
    const headers = { 'Content-Type': 'application/json' };
    const token = Auth.getToken();
    if (token) headers['Authorization'] = `Bearer ${token}`;
    const res = await fetch(url, { method, headers, body: body ? JSON.stringify(body) : null });
    if (res.status === 401) { Auth.logout(); return; }
    const data = await res.json();
    if (!data.success && !res.ok) throw new Error(data.message || 'Request failed');
    return data;
  },
  get:    (path, params) => api.request('GET',    path, null, params),
  post:   (path, body)   => api.request('POST',   path, body),
  put:    (path, body)   => api.request('PUT',     path, body),
  delete: (path)         => api.request('DELETE',  path),
  async downloadCsv(params) {
    const url = new URL(API_BASE + '/transactions/export/csv');
    Object.entries(params).forEach(([k, v]) => {
      if (v !== null && v !== undefined && v !== '') url.searchParams.set(k, v);
    });
    const res = await fetch(url, { headers: { Authorization: `Bearer ${Auth.getToken()}` } });
    if (!res.ok) throw new Error('Export failed');
    const blob = await res.blob();
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `transactions-${new Date().toISOString().slice(0,10)}.csv`;
    link.click();
  }
};

// ── Toast ─────────────────────────────────────────────────────────────────
const Toast = {
  container: null,
  init() {
    this.container = document.getElementById('toast-container');
    if (!this.container) {
      this.container = document.createElement('div');
      this.container.id = 'toast-container';
      this.container.className = 'toast-container';
      document.body.appendChild(this.container);
    }
  },
  show(message, type = 'info', duration = 3500) {
    this.init();
    const icons = { success: '✓', error: '✕', info: 'ℹ' };
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = `<span style="font-weight:700;flex-shrink:0">${icons[type]}</span><span>${message}</span>`;
    this.container.appendChild(toast);
    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transition = 'opacity 0.3s';
      setTimeout(() => toast.remove(), 300);
    }, duration);
  },
  success: (msg) => Toast.show(msg, 'success'),
  error:   (msg) => Toast.show(msg, 'error'),
  info:    (msg) => Toast.show(msg, 'info')
};

// ── Format helpers ────────────────────────────────────────────────────────
const fmt = {
  currency(amount) {
    if (amount === null || amount === undefined) return '৳0';
    return '৳' + Number(amount).toLocaleString('en-IN', { minimumFractionDigits: 0, maximumFractionDigits: 2 });
  },
  date(dateStr) {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
  },
  month(monthStr) {
    if (!monthStr) return '';
    const [y, m] = monthStr.split('-');
    return new Date(y, m - 1).toLocaleDateString('en-US', { month: 'short', year: 'numeric' });
  },
  currentMonth() {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  },
  capitalize: (s) => s ? s.charAt(0).toUpperCase() + s.slice(1) : ''
};

// ── Sidebar ───────────────────────────────────────────────────────────────
function initSidebar() {
  const sidebar   = document.querySelector('.sidebar');
  const hamburger = document.querySelector('.hamburger');
  if (!sidebar || !hamburger) return;

  // Create overlay
  let overlay = document.getElementById('sidebar-overlay');
  if (!overlay) {
    overlay = document.createElement('div');
    overlay.id = 'sidebar-overlay';
    overlay.className = 'sidebar-overlay';
    document.body.appendChild(overlay);
  }

  function openSidebar() {
    sidebar.classList.add('open');
    overlay.classList.add('active');
    hamburger.classList.add('active');
    document.body.style.overflow = 'hidden';
  }

  function closeSidebar() {
    sidebar.classList.remove('open');
    overlay.classList.remove('active');
    hamburger.classList.remove('active');
    document.body.style.overflow = '';
  }

  hamburger.addEventListener('click', (e) => {
    e.stopPropagation();
    sidebar.classList.contains('open') ? closeSidebar() : openSidebar();
  });

  overlay.addEventListener('click', closeSidebar);

  // Close when a nav link is tapped on mobile
  sidebar.querySelectorAll('.nav-link').forEach(link => {
    link.addEventListener('click', () => {
      if (window.innerWidth <= 768) closeSidebar();
    });
  });

  // Close on resize to desktop
  window.addEventListener('resize', () => {
    if (window.innerWidth > 768) closeSidebar();
  });
}

// ── Active nav link ───────────────────────────────────────────────────────
function setActiveSidebarLink() {
  const page = window.location.pathname.split('/').pop() || 'dashboard.html';
  document.querySelectorAll('.nav-link').forEach(link => {
    link.classList.remove('active');
    if (link.getAttribute('href') === page) link.classList.add('active');
  });
}

// ── Populate sidebar user ─────────────────────────────────────────────────
function populateSidebarUser() {
  const user = Auth.getUser();
  if (!user) return;
  const nameEl   = document.getElementById('sb-user-name');
  const emailEl  = document.getElementById('sb-user-email');
  const avatarEl = document.getElementById('sb-user-avatar');
  if (nameEl)   nameEl.textContent   = user.name  || '';
  if (emailEl)  emailEl.textContent  = user.email || '';
  if (avatarEl) avatarEl.textContent = (user.name || 'U').charAt(0).toUpperCase();
}

// ── Init page ─────────────────────────────────────────────────────────────
function initPage(requiresAuth = true) {
  if (requiresAuth && !Auth.requireAuth()) return false;
  populateSidebarUser();
  setActiveSidebarLink();
  initSidebar();
  document.getElementById('btn-logout')?.addEventListener('click', Auth.logout);
  return true;
}

// ── Sidebar HTML ──────────────────────────────────────────────────────────
const SIDEBAR_HTML = `
<div class="sidebar">
  <div class="sidebar-logo">
    <div class="brand">Spend<span>Wise</span></div>
  </div>
  <div class="sidebar-user">
    <div class="user-avatar" id="sb-user-avatar">U</div>
    <div class="user-info">
      <div class="name"  id="sb-user-name">Loading…</div>
      <div class="email" id="sb-user-email"></div>
    </div>
  </div>
  <nav class="sidebar-nav">
    <div class="nav-section-label">Overview</div>
    <a href="dashboard.html" class="nav-link">
      <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
          d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"/>
      </svg>
      Dashboard
    </a>
    <div class="nav-section-label">Money</div>
    <a href="transactions.html" class="nav-link">
      <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
          d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/>
      </svg>
      Transactions
    </a>
    <a href="budget.html" class="nav-link">
      <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
          d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"/>
      </svg>
      Budget
    </a>
  </nav>
  <div class="sidebar-footer">
    <button class="btn btn-ghost w-full" id="btn-logout">
      <svg width="15" height="15" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
          d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"/>
      </svg>
      Sign out
    </button>
  </div>
</div>`;

// ── Categories ────────────────────────────────────────────────────────────
const CATEGORIES = [
  'Food & Dining', 'Transport', 'Shopping', 'Housing', 'Utilities',
  'Healthcare', 'Education', 'Entertainment', 'Travel', 'Personal Care',
  'Savings', 'Investment', 'Salary', 'Freelance', 'Business', 'Gift', 'Other'
];

function categoryOptions(selected = '') {
  return CATEGORIES.map(c =>
    `<option value="${c}" ${c === selected ? 'selected' : ''}>${c}</option>`
  ).join('');
}
