/**
 * CAMPUS RETRIEVE - College Lost & Found System
 * Modular Single-Page Client Application Logic
 */

// Application State
const state = {
  token: localStorage.getItem('token') || null,
  user: null,
  activeTab: 'browse',
  items: [],
  debounceTimer: null,
  lostImageUrl: null,
  foundImageUrl: null
};

// API Base Helper
async function api(endpoint, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
  if (state.token) {
    headers['Authorization'] = `Bearer ${state.token}`;
  }
  const res = await fetch(endpoint, { ...options, headers });
  if (res.status === 401) {
    // If token expired or unauthorized on private endpoint
    if (endpoint !== '/api/auth/me') {
      showToast('Session expired or login required.', 'error');
    }
    logout(false);
    throw new Error('Unauthorized');
  }
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    throw new Error(data.error || 'Request failed');
  }
  return data;
}

// Initialize Application
document.addEventListener('DOMContentLoaded', async () => {
  // Set default dates for report forms to today
  const today = new Date().toISOString().split('T')[0];
  const lostDate = document.getElementById('lost-date');
  const foundDate = document.getElementById('found-date');
  if (lostDate) lostDate.value = today;
  if (foundDate) foundDate.value = today;

  await checkAuth();
  await loadPublicStats();
  await loadItems();
  pollNotifications();
});

// Authentication Management
async function checkAuth() {
  if (!state.token) {
    renderAuthUI(null);
    return;
  }
  try {
    const user = await api('/api/auth/me');
    state.user = user;
    renderAuthUI(user);
    loadNotifications();
  } catch (err) {
    state.token = null;
    state.user = null;
    localStorage.removeItem('token');
    renderAuthUI(null);
  }
}

function renderAuthUI(user) {
  const container = document.getElementById('auth-section');
  const adminNav = document.getElementById('nav-btn-admin');

  if (user) {
    const initials = user.name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();
    const roleLabel = user.role === 'ROLE_ADMIN' ? 'Admin' : (user.role === 'ROLE_STAFF' ? 'Staff' : 'Student');
    
    container.innerHTML = `
      <div class="user-profile-badge">
        <div class="avatar">${initials}</div>
        <div class="user-info">
          <div class="user-name">${escapeHtml(user.name)}</div>
          <div class="user-role">${roleLabel} &bull; ${escapeHtml(user.collegeId)}</div>
        </div>
        <button class="btn-sm btn-outline" style="margin-left: 8px; padding: 4px 8px;" onclick="logout()">Logout</button>
      </div>
    `;

    // Only show admin nav if Admin or Staff
    if (adminNav) {
      adminNav.style.display = (user.role === 'ROLE_ADMIN' || user.role === 'ROLE_STAFF') ? 'flex' : 'none';
    }
  } else {
    container.innerHTML = `
      <button class="btn-sm btn-primary" onclick="showAuthModal()">Sign In / Register</button>
    `;
    if (adminNav) adminNav.style.display = 'none';
  }
}

async function quickLogin(email, password) {
  try {
    const data = await api('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password })
    });
    loginSuccess(data);
  } catch (err) {
    showToast(err.message || 'Login failed', 'error');
  }
}

async function handleLoginSubmit(e) {
  e.preventDefault();
  const email = document.getElementById('login-email').value;
  const password = document.getElementById('login-password').value;
  await quickLogin(email, password);
}

async function handleRegisterSubmit(e) {
  e.preventDefault();
  const name = document.getElementById('reg-name').value;
  const email = document.getElementById('reg-email').value;
  const collegeId = document.getElementById('reg-college-id').value;
  const role = document.getElementById('reg-role').value;
  const phone = document.getElementById('reg-phone').value;
  const password = document.getElementById('reg-password').value;

  try {
    const data = await api('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify({ name, email, collegeId, role, phone, password })
    });
    loginSuccess(data);
    showToast('Account created successfully!', 'success');
  } catch (err) {
    showToast(err.message || 'Registration failed', 'error');
  }
}

function loginSuccess(data) {
  state.token = data.token;
  localStorage.setItem('token', data.token);
  state.user = {
    id: data.id,
    name: data.name,
    email: data.email,
    collegeId: data.collegeId,
    role: data.role,
    phone: data.phone
  };
  renderAuthUI(state.user);
  closeModal('modal-auth');
  showToast(`Welcome back, ${data.name}!`, 'success');
  loadPublicStats();
  loadItems();
  loadNotifications();
}

function logout(notify = true) {
  state.token = null;
  state.user = null;
  localStorage.removeItem('token');
  renderAuthUI(null);
  document.getElementById('unread-count-badge').style.display = 'none';
  if (notify) showToast('Logged out successfully.', 'info');
  if (state.activeTab === 'admin' || state.activeTab === 'my-activity') {
    switchTab('browse');
  }
}

// Navigation & Tab Switching
function switchTab(tab) {
  state.activeTab = tab;
  
  // Update nav buttons
  document.querySelectorAll('.nav-tab').forEach(b => b.classList.remove('active'));
  const activeBtn = document.getElementById(`nav-btn-${tab}`);
  if (activeBtn) activeBtn.classList.add('active');

  // Show/hide tab contents
  document.querySelectorAll('.tab-content').forEach(c => c.style.display = 'none');
  const targetContent = document.getElementById(`tab-${tab}`);
  if (targetContent) targetContent.style.display = 'block';

  // Toggle hero banner visibility (only visible on Browse tab)
  const heroBanner = document.getElementById('hero-banner');
  if (heroBanner) {
    heroBanner.style.display = (tab === 'browse') ? 'block' : 'none';
  }

  // Load relevant data for tab
  if (tab === 'browse') {
    loadItems();
  } else if (tab === 'my-activity') {
    if (!state.user) {
      showAuthModal();
      showToast('Please sign in to view your activity and matches', 'info');
      switchTab('browse');
      return;
    }
    loadMyActivity();
  } else if (tab === 'admin') {
    if (!state.user || (state.user.role !== 'ROLE_ADMIN' && state.user.role !== 'ROLE_STAFF')) {
      showToast('Admin or Staff access required', 'error');
      switchTab('browse');
      return;
    }
    loadAdminData();
  } else if (tab === 'report-lost' || tab === 'report-found') {
    if (!state.user) {
      showAuthModal();
      showToast('Please sign in to report an item', 'info');
      switchTab('browse');
    }
  }
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

// Load Hero Public Statistics
async function loadPublicStats() {
  try {
    const stats = await api('/api/admin/stats/public');
    document.getElementById('hero-stat-lost').innerText = stats.totalLostItems;
    document.getElementById('hero-stat-found').innerText = stats.totalFoundItems;
    document.getElementById('hero-stat-matches').innerText = stats.potentialMatches;
    document.getElementById('hero-stat-returned').innerText = stats.returnedItems;
  } catch (err) {
    console.error('Could not load stats', err);
  }
}

// Browse & Search Items
function debounceSearch() {
  clearTimeout(state.debounceTimer);
  state.debounceTimer = setTimeout(() => {
    loadItems();
  }, 300);
}

function resetFilters() {
  document.getElementById('search-keyword').value = '';
  document.getElementById('filter-type').value = '';
  document.getElementById('filter-category').value = '';
  document.getElementById('filter-status').value = '';
  document.getElementById('filter-location').value = '';
  document.getElementById('filter-date').value = '';
  loadItems();
}

async function loadItems() {
  const container = document.getElementById('items-grid-container');
  container.innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 40px; color: var(--text-muted);">Loading campus items...</div>';

  const keyword = document.getElementById('search-keyword')?.value.trim() || '';
  const type = document.getElementById('filter-type')?.value || '';
  const category = document.getElementById('filter-category')?.value || '';
  const status = document.getElementById('filter-status')?.value || '';
  const location = document.getElementById('filter-location')?.value.trim() || '';
  const date = document.getElementById('filter-date')?.value || '';

  const params = new URLSearchParams();
  if (keyword) params.append('keyword', keyword);
  if (type) params.append('type', type);
  if (category) params.append('category', category);
  if (status) params.append('status', status);
  if (location) params.append('location', location);
  if (date) params.append('date', date);
  params.append('size', '50');

  try {
    const data = await api(`/api/items?${params.toString()}`);
    state.items = data.content || [];
    renderItemsGrid(state.items, container);
  } catch (err) {
    container.innerHTML = `<div class="empty-state"><p>Error loading items: ${err.message}</p></div>`;
  }
}

function renderItemsGrid(items, container) {
  if (!items || items.length === 0) {
    container.innerHTML = `
      <div class="empty-state">
        <div class="empty-icon">
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
        </div>
        <h3>No matching reports found</h3>
        <p>Try clearing or adjusting your search filters, or report the item if it hasn't been listed yet.</p>
        <button class="btn-sm btn-primary" onclick="resetFilters()">Reset All Filters</button>
      </div>
    `;
    return;
  }

  container.innerHTML = items.map(item => createItemCardHtml(item)).join('');
}

function createItemCardHtml(item) {
  const isLost = item.type === 'LOST';
  const typeBadgeClass = isLost ? 'badge-lost' : 'badge-found';
  const typeText = isLost ? 'LOST' : 'FOUND';
  const statusClass = `status-${item.status.toLowerCase()}`;

  // Image fallback with nice SVG category icon
  const mediaContent = item.imageUrl
    ? `<img src="${item.imageUrl}" alt="${escapeHtml(item.name)}" onerror="this.src='/images/placeholder.svg';">`
    : `<div class="media-fallback">
         <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
           <rect x="2" y="7" width="20" height="14" rx="2" ry="2"></rect>
           <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"></path>
         </svg>
         <span style="font-size: 0.75rem; text-transform: uppercase;">${escapeHtml(item.category.replace('_', ' '))}</span>
       </div>`;

  const colorBadge = item.color
    ? `<span style="font-size: 0.75rem; color: var(--text-muted);">&bull; Color: ${escapeHtml(item.color)}</span>`
    : '';

  const matchBanner = (item.matchCount && item.matchCount > 0)
    ? `<div class="match-banner-card">
         <span>🎯 AI Match: ${item.matchCount} Candidate${item.matchCount > 1 ? 's' : ''} Found</span>
         <span style="font-size: 0.7rem;">Click to view</span>
       </div>`
    : '';

  const holdingLocRow = (!isLost && item.currentHoldingLocation)
    ? `<div class="meta-item">
         <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 12v10H4V12"></path><path d="M2 7h20v5H2z"></path></svg>
         <span>Holding at: <strong>${escapeHtml(item.currentHoldingLocation)}</strong></span>
       </div>`
    : '';

  return `
    <div class="item-card" onclick="showItemDetail(${item.id})">
      <div class="item-card-media">
        <span class="item-badge-type ${typeBadgeClass}">${typeText}</span>
        <span class="item-badge-status ${statusClass}">${item.status}</span>
        ${mediaContent}
      </div>
      <div class="item-card-body">
        <div class="card-category-row">
          <span class="category-tag">${escapeHtml(item.category.replace('_', ' '))}</span>
          ${colorBadge}
        </div>
        <h3 class="item-title">${escapeHtml(item.name)}</h3>
        <p class="item-snippet">${escapeHtml(item.description)}</p>
        
        <div class="meta-list">
          <div class="meta-item">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z"></path></svg>
            <span>${escapeHtml(item.location)}</span>
          </div>
          <div class="meta-item">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>
            <span>${item.itemDate} ${item.itemTime ? '&bull; ' + escapeHtml(item.itemTime) : ''}</span>
          </div>
          ${holdingLocRow}
        </div>

        ${matchBanner}
      </div>

      <div class="item-card-footer">
        <button class="btn-card-action btn-outline" onclick="event.stopPropagation(); showItemDetail(${item.id})">
          View Details &amp; Claim
        </button>
      </div>
    </div>
  `;
}

// Show Item Detail & Claim Modal
async function showItemDetail(itemId) {
  try {
    const item = await api(`/api/items/${itemId}`);
    const matches = await api(`/api/items/${itemId}/matches`).catch(() => []);

    const isLost = item.type === 'LOST';
    const isOwner = item.owner;
    const canClaim = !isLost && (!state.user || state.user.id !== item.reporterId) && (item.status === 'FOUND' || item.status === 'MATCHED');

    document.getElementById('modal-item-title').innerText = `${item.type}: ${item.name}`;

    let matchesHtml = '';
    if (matches && matches.length > 0) {
      matchesHtml = `
        <div style="margin-top: 20px; padding: 16px; background: rgba(139, 92, 246, 0.1); border: 1px solid rgba(139, 92, 246, 0.3); border-radius: var(--radius-sm);">
          <h4 style="color: #c4b5fd; margin-bottom: 10px; display: flex; align-items: center; gap: 8px;">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><circle cx="12" cy="12" r="6"></circle></svg>
            AI Suggested Matches (${matches.length})
          </h4>
          <div style="display: flex; flex-direction: column; gap: 8px;">
            ${matches.map(m => {
              const other = (m.lostItem.id === item.id) ? m.foundItem : m.lostItem;
              return `
                <div style="padding: 10px; background: rgba(15, 23, 42, 0.7); border-radius: 6px; display: flex; justify-content: space-between; align-items: center;">
                  <div>
                    <strong style="color: #fff; font-size: 0.9rem;">${escapeHtml(other.name)}</strong>
                    <div style="font-size: 0.75rem; color: var(--text-muted);">${escapeHtml(other.location)} &bull; ${other.itemDate}</div>
                    <div style="font-size: 0.72rem; color: #a78bfa; margin-top: 2px;">${escapeHtml(m.matchReasons || '')}</div>
                  </div>
                  <div style="text-align: right;">
                    <span style="background: rgba(139, 92, 246, 0.2); color: #c4b5fd; padding: 4px 8px; border-radius: 12px; font-size: 0.8rem; font-weight: 700;">
                      ${m.matchScore}% Match
                    </span>
                    <br>
                    <button class="btn-sm btn-outline" style="margin-top: 6px; font-size: 0.75rem; padding: 2px 8px;" onclick="closeModal('modal-item-detail'); showItemDetail(${other.id})">
                      Inspect
                    </button>
                  </div>
                </div>
              `;
            }).join('')}
          </div>
        </div>
      `;
    }

    let secretDetailsHtml = '';
    if (item.secretDetails) {
      secretDetailsHtml = `
        <div style="margin-top: 14px; padding: 12px; background: rgba(0, 180, 216, 0.1); border: 1px solid rgba(0, 180, 216, 0.3); border-radius: 8px;">
          <strong style="color: var(--primary); font-size: 0.85rem;">🔒 Hidden Distinguishing Proof (Staff/Owner View):</strong>
          <p style="font-size: 0.85rem; color: #e2e8f0; margin-top: 4px;">${escapeHtml(item.secretDetails)}</p>
        </div>
      `;
    }

    let claimSectionHtml = '';
    if (canClaim) {
      claimSectionHtml = `
        <div style="margin-top: 24px; padding: 20px; background: rgba(16, 185, 129, 0.08); border: 1px solid rgba(16, 185, 129, 0.3); border-radius: var(--radius-md);">
          <h4 style="color: #34d399; margin-bottom: 8px; display: flex; align-items: center; gap: 8px;">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"></polyline></svg>
            Is this your item? Submit an Ownership Claim
          </h4>
          <p style="font-size: 0.85rem; color: var(--text-muted); margin-bottom: 14px;">
            To prevent fraud, provide distinct proof of ownership (e.g., lock screen wallpaper, secret marks, wallet contents, receipt/serial #). Campus staff will verify your claim before handing over.
          </p>
          <form onsubmit="handleClaimSubmit(event, ${item.id})">
            <div class="form-group" style="margin-bottom: 12px;">
              <label>Proof of Ownership Description <span class="req">*</span></label>
              <textarea id="claim-proof-desc" rows="3" required placeholder="Describe specific details only the true owner would know..."></textarea>
            </div>
            <button type="submit" class="btn-lg btn-found" style="width: 100%; justify-content: center;">
              Submit Ownership Claim Request
            </button>
          </form>
        </div>
      `;
    } else if (item.status === 'CLAIMED' || item.status === 'RETURNED' || item.status === 'CLOSED') {
      claimSectionHtml = `
        <div style="margin-top: 20px; padding: 12px; background: rgba(255, 255, 255, 0.05); border-radius: 8px; text-align: center; color: var(--text-muted); font-size: 0.9rem;">
          Item status is <strong>${item.status}</strong>. New claims are closed for this report.
        </div>
      `;
    }

    const bodyContainer = document.getElementById('modal-item-body');
    bodyContainer.innerHTML = `
      <div style="display: flex; flex-direction: column; gap: 16px;">
        ${item.imageUrl ? `<img src="${item.imageUrl}" alt="${escapeHtml(item.name)}" style="width: 100%; max-height: 280px; object-fit: cover; border-radius: var(--radius-sm); border: 1px solid var(--border-glass);">` : ''}
        
        <div>
          <span class="item-badge-type ${isLost ? 'badge-lost' : 'badge-found'}" style="position: static; display: inline-block; margin-bottom: 8px;">
            ${item.type} &bull; ${item.category.replace('_', ' ')}
          </span>
          <span class="item-badge-status status-${item.status.toLowerCase()}" style="position: static; display: inline-block; margin-left: 8px;">
            ${item.status}
          </span>
          <h2 style="font-size: 1.5rem; margin: 8px 0;">${escapeHtml(item.name)}</h2>
          <p style="color: var(--text-muted); line-height: 1.5;">${escapeHtml(item.description)}</p>
        </div>

        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px; padding: 14px; background: rgba(15, 23, 42, 0.6); border-radius: var(--radius-sm); font-size: 0.85rem;">
          <div><strong style="color: var(--text-dim);">Campus Location:</strong> <br>${escapeHtml(item.location)}</div>
          <div><strong style="color: var(--text-dim);">Date &amp; Time:</strong> <br>${item.itemDate} ${item.itemTime ? '&bull; ' + escapeHtml(item.itemTime) : ''}</div>
          <div><strong style="color: var(--text-dim);">Color:</strong> <br>${escapeHtml(item.color || 'Not specified')}</div>
          <div><strong style="color: var(--text-dim);">${isLost ? 'Contact Info' : 'Holding Location'}:</strong> <br>${escapeHtml(isLost ? item.contactInfo : (item.currentHoldingLocation || 'Campus Office'))}</div>
        </div>

        ${secretDetailsHtml}
        ${matchesHtml}
        ${claimSectionHtml}
      </div>
    `;

    openModal('modal-item-detail');
  } catch (err) {
    showToast('Failed to load item details: ' + err.message, 'error');
  }
}

// Ownership Claim Submission
async function handleClaimSubmit(e, itemId) {
  e.preventDefault();
  if (!state.user) {
    showAuthModal();
    showToast('Please sign in to submit a claim', 'info');
    return;
  }
  const proofDescription = document.getElementById('claim-proof-desc').value;
  try {
    await api(`/api/items/${itemId}/claim`, {
      method: 'POST',
      body: JSON.stringify({ proofDescription, proofImageUrl: '' })
    });
    showToast('Claim submitted! Campus administration will review your proof.', 'success');
    closeModal('modal-item-detail');
    loadItems();
    loadPublicStats();
  } catch (err) {
    showToast(err.message || 'Could not submit claim', 'error');
  }
}

// Image File Upload Helper
async function handleImageUpload(e, type) {
  const file = e.target.files[0];
  if (!file) return;

  const formData = new FormData();
  formData.append('file', file);

  const previewImg = document.getElementById(`${type}-preview-img`);
  const previewBox = document.getElementById(`${type}-image-preview`);
  const urlDisplay = document.getElementById(`${type}-image-url-display`);

  try {
    showToast('Uploading image...', 'info');
    const headers = {};
    if (state.token) headers['Authorization'] = `Bearer ${state.token}`;

    const res = await fetch('/api/upload', {
      method: 'POST',
      headers,
      body: formData
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || 'Upload failed');

    if (type === 'lost') state.lostImageUrl = data.url;
    if (type === 'found') state.foundImageUrl = data.url;

    previewImg.src = data.url;
    urlDisplay.innerText = 'Image uploaded successfully!';
    previewBox.style.display = 'flex';
    showToast('Photo uploaded successfully!', 'success');
  } catch (err) {
    showToast('Image upload failed: ' + err.message, 'error');
  }
}

// Submit Lost Item Report Form
async function submitLostReport(e) {
  e.preventDefault();
  if (!state.user) {
    showAuthModal();
    return;
  }

  const payload = {
    name: document.getElementById('lost-name').value,
    category: document.getElementById('lost-category').value,
    color: document.getElementById('lost-color').value,
    location: document.getElementById('lost-location').value,
    itemDate: document.getElementById('lost-date').value,
    itemTime: document.getElementById('lost-time').value,
    contactInfo: document.getElementById('lost-contact').value,
    description: document.getElementById('lost-description').value,
    secretDetails: document.getElementById('lost-secret').value,
    imageUrl: state.lostImageUrl || ''
  };

  try {
    const res = await api('/api/items/lost', {
      method: 'POST',
      body: JSON.stringify(payload)
    });
    showToast('Lost item reported successfully! AI matching scan started.', 'success');
    document.getElementById('form-lost').reset();
    state.lostImageUrl = null;
    document.getElementById('lost-image-preview').style.display = 'none';
    switchTab('browse');
    loadPublicStats();
  } catch (err) {
    showToast(err.message || 'Failed to submit lost report', 'error');
  }
}

// Submit Found Item Report Form
async function submitFoundReport(e) {
  e.preventDefault();
  if (!state.user) {
    showAuthModal();
    return;
  }

  const payload = {
    name: document.getElementById('found-name').value,
    category: document.getElementById('found-category').value,
    color: document.getElementById('found-color').value,
    location: document.getElementById('found-location').value,
    currentHoldingLocation: document.getElementById('found-holding').value,
    itemDate: document.getElementById('found-date').value,
    itemTime: document.getElementById('found-time').value,
    contactInfo: document.getElementById('found-contact').value,
    description: document.getElementById('found-description').value,
    secretDetails: document.getElementById('found-secret').value,
    imageUrl: state.foundImageUrl || ''
  };

  try {
    const res = await api('/api/items/found', {
      method: 'POST',
      body: JSON.stringify(payload)
    });
    showToast('Found item reported! Thank you for helping return campus belongings.', 'success');
    document.getElementById('form-found').reset();
    state.foundImageUrl = null;
    document.getElementById('found-image-preview').style.display = 'none';
    switchTab('browse');
    loadPublicStats();
  } catch (err) {
    showToast(err.message || 'Failed to submit found report', 'error');
  }
}

// My Activity, Claims & Matches Tab
async function loadMyActivity() {
  const itemsContainer = document.getElementById('my-items-container');
  const claimsTable = document.getElementById('my-claims-table-body');
  const matchesDeck = document.getElementById('ai-matches-deck');

  itemsContainer.innerHTML = '<p style="color: var(--text-muted);">Loading your reports...</p>';
  claimsTable.innerHTML = '<tr><td colspan="5" style="text-align: center;">Loading claims...</td></tr>';
  matchesDeck.innerHTML = '<p style="color: var(--text-muted);">Scanning matches...</p>';

  try {
    // 1. Load User's Reported Items
    const myItems = await api('/api/items/my');
    if (myItems.length === 0) {
      itemsContainer.innerHTML = '<div class="empty-state"><p>You haven\'t reported any lost or found items yet.</p></div>';
    } else {
      itemsContainer.innerHTML = myItems.map(item => createItemCardHtml(item)).join('');
    }

    // 2. Fetch AI Matches for User's Items
    let allMatches = [];
    for (const item of myItems) {
      const itemMatches = await api(`/api/items/${item.id}/matches`).catch(() => []);
      for (const m of itemMatches) {
        if (!allMatches.some(x => x.id === m.id)) {
          allMatches.push({ ...m, contextItemId: item.id });
        }
      }
    }

    if (allMatches.length === 0) {
      matchesDeck.innerHTML = `
        <div style="text-align: center; padding: 20px; color: var(--text-dim);">
          No AI matches detected yet. The system constantly monitors new incoming reports!
        </div>
      `;
    } else {
      matchesDeck.innerHTML = allMatches.map(m => `
        <div style="padding: 14px; background: rgba(15, 23, 42, 0.85); border: 1px solid rgba(139, 92, 246, 0.3); border-radius: var(--radius-sm); margin-bottom: 10px; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px;">
          <div>
            <span style="font-size: 0.75rem; text-transform: uppercase; background: var(--match-gradient); color: #fff; padding: 3px 8px; border-radius: 10px; font-weight: 700;">
              ${m.matchScore}% Match Score
            </span>
            <h4 style="color: #fff; margin: 6px 0 2px;">
              Lost: "${escapeHtml(m.lostItem.name)}" &harr; Found: "${escapeHtml(m.foundItem.name)}"
            </h4>
            <div style="font-size: 0.78rem; color: #a78bfa;">${escapeHtml(m.matchReasons || '')}</div>
          </div>
          <button class="btn-sm btn-primary" onclick="showItemDetail(${m.foundItem.id})">
            Inspect Found Item
          </button>
        </div>
      `).join('');
    }

    // 3. Load User's Submitted Claims
    const claims = await api('/api/claims/my');
    if (claims.length === 0) {
      claimsTable.innerHTML = '<tr><td colspan="5" style="text-align: center; color: var(--text-dim);">No claims submitted yet.</td></tr>';
    } else {
      claimsTable.innerHTML = claims.map(c => `
        <tr>
          <td><strong>${escapeHtml(c.itemName || 'Item #' + c.itemId)}</strong></td>
          <td>${new Date(c.createdAt).toLocaleDateString()}</td>
          <td style="max-width: 250px; font-size: 0.82rem; color: var(--text-muted);">${escapeHtml(c.proofDescription)}</td>
          <td><span class="item-badge-status status-${c.status.toLowerCase()}" style="position: static;">${c.status}</span></td>
          <td style="font-size: 0.82rem;">${c.adminNotes ? escapeHtml(c.adminNotes) : '<span style="color: var(--text-dim);">-</span>'}</td>
        </tr>
      `).join('');
    }
  } catch (err) {
    showToast('Failed to load activity: ' + err.message, 'error');
  }
}

// Admin / Staff Management Portal
async function loadAdminData() {
  try {
    // 1. Load KPI Stats
    const stats = await api('/api/admin/stats');
    document.getElementById('kpi-users').innerText = stats.totalUsers;
    document.getElementById('kpi-lost').innerText = stats.totalLostItems;
    document.getElementById('kpi-found').innerText = stats.totalFoundItems;
    document.getElementById('kpi-matches').innerText = stats.potentialMatches;
    document.getElementById('kpi-claims').innerText = stats.pendingClaims;

    // 2. Load Pending Claims Verification Queue
    const claims = await api('/api/claims');
    const pendingClaims = claims.filter(c => c.status === 'PENDING');
    const claimsTbody = document.getElementById('admin-claims-table-body');

    if (pendingClaims.length === 0) {
      claimsTbody.innerHTML = '<tr><td colspan="6" style="text-align: center; color: #34d399; padding: 24px;">🎉 All ownership claims have been verified! No pending reviews.</td></tr>';
    } else {
      claimsTbody.innerHTML = pendingClaims.map(c => `
        <tr>
          <td>
            <strong>${escapeHtml(c.itemName || 'Item #' + c.itemId)}</strong>
            <div style="font-size: 0.75rem; color: var(--text-dim);">Item ID: ${c.itemId}</div>
          </td>
          <td>
            <strong>${escapeHtml(c.claimantName || 'User')}</strong>
            <div style="font-size: 0.75rem; color: var(--text-dim);">${escapeHtml(c.claimantEmail || '')} &bull; ${escapeHtml(c.claimantCollegeId || '')}</div>
          </td>
          <td style="max-width: 240px; font-size: 0.82rem;">${escapeHtml(c.proofDescription)}</td>
          <td>${new Date(c.createdAt).toLocaleDateString()}</td>
          <td><span class="item-badge-status status-matched" style="position: static;">PENDING</span></td>
          <td>
            <div style="display: flex; gap: 6px;">
              <button class="btn-sm btn-found" style="padding: 4px 10px;" onclick="reviewClaim(${c.id}, 'APPROVED')">Approve</button>
              <button class="btn-sm btn-lost" style="padding: 4px 10px;" onclick="reviewClaim(${c.id}, 'REJECTED')">Reject</button>
            </div>
          </td>
        </tr>
      `).join('');
    }

    // 3. Load All Items Moderation Table
    const itemsData = await api('/api/items?size=100');
    const itemsTbody = document.getElementById('admin-items-table-body');
    const items = itemsData.content || [];

    itemsTbody.innerHTML = items.map(item => `
      <tr>
        <td>
          <span class="item-badge-type ${item.type === 'LOST' ? 'badge-lost' : 'badge-found'}" style="position: static; font-size: 0.7rem; padding: 2px 8px;">
            ${item.type}
          </span>
        </td>
        <td><strong>${escapeHtml(item.name)}</strong></td>
        <td>${escapeHtml(item.category.replace('_', ' '))}</td>
        <td>${escapeHtml(item.location)}</td>
        <td>${item.itemDate}</td>
        <td>
          <select style="width: auto; padding: 4px 8px; font-size: 0.8rem;" onchange="updateItemStatus(${item.id}, this.value)">
            <option value="LOST" ${item.status === 'LOST' ? 'selected' : ''}>LOST</option>
            <option value="FOUND" ${item.status === 'FOUND' ? 'selected' : ''}>FOUND</option>
            <option value="MATCHED" ${item.status === 'MATCHED' ? 'selected' : ''}>MATCHED</option>
            <option value="CLAIMED" ${item.status === 'CLAIMED' ? 'selected' : ''}>CLAIMED</option>
            <option value="RETURNED" ${item.status === 'RETURNED' ? 'selected' : ''}>RETURNED</option>
            <option value="CLOSED" ${item.status === 'CLOSED' ? 'selected' : ''}>CLOSED</option>
          </select>
        </td>
        <td style="font-size: 0.8rem;">${escapeHtml(item.reporterName || 'Student')}</td>
        <td>
          <button class="btn-sm btn-outline" style="color: var(--lost-accent); border-color: rgba(244, 63, 94, 0.4);" onclick="deleteItemReport(${item.id})">
            Delete
          </button>
        </td>
      </tr>
    `).join('');

  } catch (err) {
    showToast('Failed to load admin data: ' + err.message, 'error');
  }
}

// Admin Action: Review Claim
async function reviewClaim(claimId, status) {
  const reason = prompt(`Enter notes for claimant (Optional for ${status}):`, status === 'APPROVED' ? 'Proof verified against custody log.' : 'Distinguishing details do not match item.');
  if (reason === null) return; // User cancelled

  try {
    await api(`/api/claims/${claimId}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status, adminNotes: reason })
    });
    showToast(`Claim marked as ${status}! Claimant has been notified.`, 'success');
    loadAdminData();
    loadPublicStats();
  } catch (err) {
    showToast('Error reviewing claim: ' + err.message, 'error');
  }
}

// Admin Action: Update Item Status
async function updateItemStatus(itemId, newStatus) {
  try {
    await api(`/api/items/${itemId}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status: newStatus })
    });
    showToast(`Item status updated to ${newStatus}`, 'success');
    loadPublicStats();
  } catch (err) {
    showToast('Failed to update status: ' + err.message, 'error');
  }
}

// Admin Action: Delete Item Report
async function deleteItemReport(itemId) {
  if (!confirm('Are you sure you want to permanently delete this item report?')) return;
  try {
    await api(`/api/items/${itemId}`, { method: 'DELETE' });
    showToast('Item deleted successfully', 'info');
    loadAdminData();
    loadPublicStats();
  } catch (err) {
    showToast('Failed to delete item: ' + err.message, 'error');
  }
}

// Notifications System
async function loadNotifications() {
  if (!state.token) return;
  try {
    const unread = await api('/api/notifications/unread-count');
    const badge = document.getElementById('unread-count-badge');
    if (unread.unreadCount > 0) {
      badge.innerText = unread.unreadCount;
      badge.style.display = 'flex';
    } else {
      badge.style.display = 'none';
    }

    const notifs = await api('/api/notifications');
    const list = document.getElementById('notification-list');
    if (notifs.length === 0) {
      list.innerHTML = '<div style="padding: 24px; text-align: center; color: var(--text-dim);">No alerts yet.</div>';
    } else {
      list.innerHTML = notifs.map(n => `
        <div class="notif-item ${!n.isRead ? 'unread' : ''}" onclick="clickNotification(${n.id}, ${n.itemId})">
          <div class="notif-icon">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
          </div>
          <div class="notif-content">
            <h4>${escapeHtml(n.title)}</h4>
            <p>${escapeHtml(n.message)}</p>
            <div class="notif-time">${new Date(n.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</div>
          </div>
        </div>
      `).join('');
    }
  } catch (err) {
    console.error('Notification load error', err);
  }
}

function toggleNotifications() {
  const dropdown = document.getElementById('notification-dropdown');
  const isShown = dropdown.classList.contains('active');
  if (isShown) {
    dropdown.classList.remove('active');
  } else {
    dropdown.classList.add('active');
    loadNotifications();
  }
}

async function markAllNotificationsAsRead() {
  try {
    await api('/api/notifications/read-all', { method: 'PUT' });
    loadNotifications();
  } catch (err) {
    console.error(err);
  }
}

async function clickNotification(notifId, itemId) {
  try {
    await api(`/api/notifications/${notifId}/read`, { method: 'PUT' });
    document.getElementById('notification-dropdown').classList.remove('active');
    loadNotifications();
    if (itemId) {
      showItemDetail(itemId);
    }
  } catch (err) {
    console.error(err);
  }
}

function pollNotifications() {
  setInterval(() => {
    if (state.token) loadNotifications();
  }, 30000); // Poll every 30s
}

// Modal Helpers
function showAuthModal() {
  openModal('modal-auth');
}

function openModal(id) {
  document.getElementById(id).classList.add('active');
}

function closeModal(id) {
  document.getElementById(id).classList.remove('active');
}

function toggleAuthTab(tab) {
  const isLogin = tab === 'login';
  document.getElementById('btn-tab-login').classList.toggle('active', isLogin);
  document.getElementById('btn-tab-register').classList.toggle('active', !isLogin);
  document.getElementById('form-login').style.display = isLogin ? 'block' : 'none';
  document.getElementById('form-register').style.display = !isLogin ? 'block' : 'none';
}

// Toast Alert System
function showToast(message, type = 'info') {
  const container = document.getElementById('toast-container');
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.innerHTML = `
    <div style="flex: 1; font-size: 0.88rem;">${escapeHtml(message)}</div>
    <button style="background: transparent; color: var(--text-dim); font-size: 1.1rem; line-height: 1;" onclick="this.parentElement.remove()">&times;</button>
  `;
  container.appendChild(toast);
  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateX(50px)';
    setTimeout(() => toast.remove(), 300);
  }, 4000);
}

// Utilities
function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}
