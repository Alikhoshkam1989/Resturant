'use strict';

const byId = id => document.getElementById(id);
const escapeHtml = value => String(value ?? '').replace(/[&<>"']/g, character => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[character]));
const money = value => new Intl.NumberFormat('en', {style:'currency', currency:'EUR'}).format(value);
const dateLabel = value => new Date(value + 'Z').toLocaleString('en', {dateStyle:'medium', timeStyle:'short'});
const toLocalInput = value => { const date = new Date(value + 'Z'); return new Date(date - date.getTimezoneOffset() * 60000).toISOString().slice(0,16); };
let config;
let tokens;
let user;
let page = location.hash.slice(1) || 'menu';
let busy = false;
let editing;
let imageUrls = [];
let refreshPromise;
let data = {categories:[], products:[], tables:[], reservations:[], orders:[], invoices:[]};
const staff = () => user?.realm_access?.roles?.includes('STAFF');

function notify(message, error = false) {
  byId('notice').textContent = message;
  byId('notice').className = error ? 'error' : '';
  byId('notice').hidden = !message;
}

function decodeClaims(token) {
  const encoded = token.split('.')[1].replace(/-/g,'+').replace(/_/g,'/');
  return JSON.parse(new TextDecoder().decode(Uint8Array.from(atob(encoded), character => character.charCodeAt(0))));
}

function saveTokens(result) {
  tokens = {...result, expiresAt:Date.now() + result.expires_in * 1000};
  user = decodeClaims(result.access_token);
  sessionStorage.setItem('table-session', JSON.stringify(tokens));
}

function clearSession() {
  sessionStorage.removeItem('table-session');
  tokens = null;
  user = null;
  data = {categories:[], products:[], tables:[], reservations:[], orders:[], invoices:[]};
  byId('editor').close();
  render();
}

async function tokenRequest(parameters) {
  let response;
  try {
    response = await fetch(config.issuer + '/protocol/openid-connect/token', {
      method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded'},
      body:new URLSearchParams({client_id:config.clientId, ...parameters}), signal:AbortSignal.timeout(15000)
    });
  } catch {
    throw new Error('Cannot reach the sign-in service. Start Keycloak and check the setup instructions below.');
  }
  if (!response.ok) throw new Error('Your sign-in session has expired or could not be verified. Please sign in again.');
  return response.json();
}

async function accessToken() {
  if (!tokens) throw new Error('Please sign in to continue.');
  if (tokens.expiresAt < Date.now() + 30000) {
    if (!refreshPromise) {
      refreshPromise = tokenRequest({grant_type:'refresh_token', refresh_token:tokens.refresh_token})
        .then(saveTokens).catch(error => { clearSession(); throw error; }).finally(() => { refreshPromise = null; });
    }
    await refreshPromise;
  }
  return tokens.access_token;
}

async function api(path, options = {}) {
  const bearer = await accessToken();
  const response = await fetch('/api/' + path, {
    ...options, headers:{...(options.body instanceof FormData ? {} : {'Content-Type':'application/json'}), ...options.headers, Authorization:'Bearer ' + bearer},
    signal:AbortSignal.timeout(20000)
  });
  if (!response.ok) {
    if (response.status === 401) clearSession();
    const problem = await response.json().catch(() => ({}));
    throw new Error(problem.detail || ({401:'Your session has expired. Please sign in again.',403:'You do not have permission to do this.',404:'This item no longer exists.'}[response.status]) || 'The request failed. Please try again.');
  }
  return response.status === 204 ? null : response.json();
}

async function login(register = false, audience = 'customer') {
  if (!config) throw new Error('The application is still connecting. Refresh the page and try again.');
  try {
    const response = await fetch(config.issuer + '/.well-known/openid-configuration', {signal:AbortSignal.timeout(5000)});
    if (!response.ok) throw new Error();
  } catch {
    throw new Error('The sign-in service is not running. Double-click Start-Login.cmd in the fully extracted standalone package, wait for startup, then try again.');
  }
  const base64url = bytes => btoa(String.fromCharCode(...bytes)).replace(/\+/g,'-').replace(/\//g,'_').replace(/=+$/,'');
  const verifier = base64url(crypto.getRandomValues(new Uint8Array(32)));
  const state = base64url(crypto.getRandomValues(new Uint8Array(24)));
  const challenge = base64url(new Uint8Array(await crypto.subtle.digest('SHA-256', new TextEncoder().encode(verifier))));
  const redirect = location.origin + '/';
  sessionStorage.setItem('table-login', JSON.stringify({verifier,state,redirect,audience,created:Date.now()}));
  const url = new URL(config.issuer + '/protocol/openid-connect/' + (register ? 'registrations' : 'auth'));
  url.search = new URLSearchParams({client_id:config.clientId, redirect_uri:redirect, response_type:'code', scope:'openid profile email', state, code_challenge:challenge, code_challenge_method:'S256'});
  if (audience === 'staff') url.searchParams.set('prompt','login');
  location.assign(url);
}

async function logout() {
  const hint = tokens?.id_token;
  clearSession();
  if (hint) {
    const url = new URL(config.issuer + '/protocol/openid-connect/logout');
    url.search = new URLSearchParams({id_token_hint:hint, post_logout_redirect_uri:location.origin + '/'});
    location.assign(url);
  }
}

async function loadData() {
  const resources = ['categories','products','tables','reservations', ...(staff() ? ['orders','invoices'] : [])];
  const results = await Promise.all(resources.map(resource => api(resource)));
  resources.forEach((resource,index) => { data[resource] = results[index]; });
  render();
}

function button(action, label, resource = '', id = '', extraClass = 'secondary') {
  return `<button type="button" class="${extraClass}" data-action="${action}" data-resource="${resource}" data-id="${id}">${label}</button>`;
}

function badge(label, off = false) { return `<span class="badge ${off ? 'off' : ''}">${label}</span>`; }
function tableName(id) { return 'Table ' + (data.tables.find(item => item.id === id)?.tableNumber ?? id); }
function empty(label) { return `<div class="empty"><h3>${label}</h3><p>New items will appear here once they are added.</p></div>`; }
function rowTable(headers, rows) {
  return rows.length ? `<div class="panel"><table><thead><tr>${headers.map(label => `<th scope="col">${label}</th>`).join('')}</tr></thead><tbody>${rows.map(cells => `<tr>${cells.map(cell => `<td>${cell}</td>`).join('')}</tr>`).join('')}</tbody></table></div>` : empty('Nothing here yet');
}

function welcome() {
  return `<section class="hero"><div><p class="eyebrow">Welcome to The Table</p><h1>Good moments.<br>Great company.<br>Your table awaits.</h1><p class="subtitle">Browse our menu, find your favourite dish and make room for something special.</p><div class="hero-actions">${button('login','Customer login','','','')}${button('register','Create customer account')}</div><p class="help">Here to manage the restaurant? Choose your workspace below.</p></div><div class="hero-art" aria-hidden="true"><div class="plate"><span>Bon appétit.</span><small>MADE TO BE SHARED</small></div></div></section><section class="login-options" aria-label="Choose your access"><article><p class="eyebrow">For our guests</p><h2>Customers</h2><p>Explore the menu and manage your own reservations.</p>${button('login','Customer login')}${button('register','Create customer account')}</article><article><p class="eyebrow">For our team</p><h2>Staff</h2><p>Manage tables, dishes, reservations, orders and invoices.</p>${button('staff-login','Staff login')}<p class="help">Use the account approved by your restaurant owner. No staff self-registration.</p></article><article><p class="eyebrow">For restaurant owners</p><h2>Owner administration</h2><p>Manage user accounts and decide who can access the staff workspace.</p>${button('owner','Restaurant owner login')}<p class="help">Requires an administrator account, not a customer or staff account.</p></article></section>`;
}

function ownerWelcome() {
  const adminUrl = config ? new URL('/admin/',config.issuer).href : '';
  return `<section class="owner-access"><p class="eyebrow">Restaurant owner</p><h1>Your team.<br>Your restaurant.</h1><p>Create your owner account here, or sign in to manage your team.</p><div class="hero-actions">${adminUrl ? '<a class="primary-link" href="' + escapeHtml(adminUrl) + '">Sign in as restaurant owner</a>' : '<button disabled>Connecting…</button>'}${button('owner-register','Create owner account')}${button('home','Back to customer login')}</div><div id="owner-registration"></div><p class="help">Owner accounts manage staff access. Customer accounts are registered separately.</p></section>`;
}

async function showOwnerRegistration() {
  const response = await fetch('/owner-setup');
  if (!response.ok) throw new Error('Could not check owner setup. Please try again.');
  const state = await response.json();
  if (!state.available) throw new Error('Owner registration is closed or not prepared. If an owner already exists, use owner login. For a new installation, close the app and sign-in service, then restart Start-Restaurant.cmd from the updated package.');
  byId('owner-registration').innerHTML = `<form id="owner-form" class="owner-form"><h2>Create owner account</h2><p>Use the one-time setup code shown in the application start window on this computer.</p><label>Setup code<input name="code" required maxlength="100" autocomplete="off"></label><label>Username<input name="username" required pattern="[a-zA-Z0-9][a-zA-Z0-9._-]{2,63}" autocomplete="username"><small>3–64 letters, numbers, dots, underscores or hyphens.</small></label><label>Email<input name="email" type="email" required maxlength="254" autocomplete="email"></label><label>Password<input name="password" type="password" required minlength="12" maxlength="128" autocomplete="new-password"><small>At least 12 characters.</small></label><label>Confirm password<input name="confirmation" type="password" required autocomplete="new-password"></label><button type="submit">Create owner account</button><p class="help">This installation can be claimed once. Your password is not stored in the browser.</p></form>`;
  byId('owner-form').querySelector('input').focus();
}

document.addEventListener('submit', async event => {
  if (event.target.id !== 'owner-form') return;
  event.preventDefault();
  const form = event.target;
  const values = Object.fromEntries(new FormData(form));
  if (values.password !== values.confirmation) { notify('Passwords do not match.', true); return; }
  delete values.confirmation;
  const submit = form.querySelector('[type="submit"]');
  if (submit.disabled) return;
  submit.disabled = true;
  submit.textContent = 'Creating account…';
  try {
    const response = await fetch('/owner-setup', {method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(values),signal:AbortSignal.timeout(60000)});
    const result = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(result.detail || result.message || 'Could not create owner. Check the setup code and try again.');
    form.reset();
    form.remove();
    notify(result.message || 'Owner account created. You can now sign in as restaurant owner.');
  } catch (error) { notify(error.message, true); }
  finally { submit.disabled = false; submit.textContent = 'Create owner account'; }
});

function render() {
  imageUrls.forEach(url => URL.revokeObjectURL(url));
  imageUrls = [];
  byId('account').innerHTML = user ? `<span class="signed-name">${escapeHtml(user.given_name || user.preferred_username || 'Welcome')} · ${staff() ? 'Staff' : 'Guest'}</span>${button('logout','Sign out')}` : `<div class="entry-links">${button('login','Customer login')}${button('staff-login','Staff login')}${button('owner','Owner login')}</div>`;
  if (!user) {
    byId('navigation').innerHTML = '';
    byId('content').innerHTML = page === 'owner' ? ownerWelcome() : welcome();
    return;
  }
  const pages = {menu:'Menu', reservations:staff() ? 'Reservations' : 'My reservations', ...(staff() ? {tables:'Tables',categories:'Categories',orders:'Orders',invoices:'Invoices'} : {})};
  if (!pages[page]) page = 'menu';
  byId('navigation').innerHTML = Object.entries(pages).map(([key,label]) => `<button type="button" data-page="${key}" ${page === key ? 'aria-current="page"' : ''}>${label}</button>`).join('');
  const descriptions = {menu:'Fresh favourites, ready for your next visit.',reservations:'A little planning. A lovely evening.',tables:'Manage seating and table availability.',categories:'Keep your menu thoughtfully organised.',orders:'From the kitchen to the table.',invoices:'Keep every bill in order.'};
  const resource = page === 'menu' ? 'products' : page;
  const singular = {products:'dish',reservations:'reservation',tables:'table',categories:'category',orders:'order',invoices:'invoice'};
  let html = `<div class="page-heading"><div><p class="eyebrow">${staff() ? 'Restaurant workspace' : 'Make yourself at home'}</p><h1>${pages[page]}</h1><p class="subtitle">${descriptions[page]}</p></div><div class="actions">${button('refresh','Refresh')}${staff() || page === 'reservations' ? button('new','+ New ' + singular[resource],resource,'','') : button('reserve','Reserve a table','','','')}</div></div>`;
  if (page === 'menu') {
    html += '<div class="toolbar"><label>Search the menu<input id="menu-search" type="search" placeholder="Search dishes or categories"></label></div><div id="menu-cards" class="cards"></div>';
  } else if (page === 'reservations') {
    html += `<p class="help">Times are shown in your local timezone (${escapeHtml(Intl.DateTimeFormat().resolvedOptions().timeZone)}). Each reservation lasts two hours.</p>`;
    html += rowTable(['Guest','Table','Date & time','Guests','Status','Actions'],data.reservations.map(item => [escapeHtml(item.customerName),tableName(item.tableId),dateLabel(item.startsAt),item.guests,badge(item.cancelled ? 'Cancelled' : 'Reserved',item.cancelled),item.cancelled ? '—' : `<div class="actions">${button('edit','Edit','reservations',item.id)}${button('delete','Cancel','reservations',item.id,'danger')}</div>`]));
  } else if (page === 'tables') {
    html += `<div class="cards">${data.tables.map(item => `<article class="card"><div class="card-body"><div class="card-top"><h2>Table ${item.tableNumber}</h2>${badge(item.available ? 'Available' : 'Unavailable',!item.available)}</div><p>${item.seats} seats</p><div class="actions">${button('edit','Edit','tables',item.id)}${button('delete','Delete','tables',item.id,'danger')}</div></div></article>`).join('')}</div>`;
    if (!data.tables.length) html += empty('No tables yet');
  } else if (page === 'categories') {
    html += rowTable(['Category','Description','Actions'],data.categories.map(item => [escapeHtml(item.name),escapeHtml(item.description),`<div class="actions">${button('edit','Edit','categories',item.id)}${button('delete','Delete','categories',item.id,'danger')}</div>`]));
  } else if (page === 'orders') {
    html += rowTable(['Order','Table','Items','Total','Status','Actions'],data.orders.map(item => {
      const billed = data.invoices.some(invoice => invoice.orderId === item.id);
      return ['#' + item.id,tableName(item.tableId),item.lines.map(line => `${line.quantity} × ${escapeHtml(line.productName)}`).join('<br>'),money(item.total),badge(billed ? 'Invoiced' : 'Open'),`<div class="actions">${button('view','Details','orders',item.id)}${billed ? '' : button('edit','Edit','orders',item.id) + button('bill','Invoice','orders',item.id) + button('delete','Delete','orders',item.id,'danger')}</div>`];
    }));
  } else if (page === 'invoices') {
    const unpaid = data.invoices.filter(item => !item.paid);
    html += `<div class="stats"><div class="stat"><strong>${data.invoices.length}</strong><small>Invoices</small></div><div class="stat"><strong>${unpaid.length}</strong><small>Awaiting payment</small></div><div class="stat"><strong>${money(unpaid.reduce((total,item) => total + item.total,0))}</strong><small>Outstanding</small></div></div>`;
    html += rowTable(['Invoice','Order','Created','Total','Status','Actions'],data.invoices.map(item => ['#' + item.id,'#' + item.orderId,dateLabel(item.createdAt),money(item.total),badge(item.paid ? 'Paid' : 'Unpaid',!item.paid),`<div class="actions">${button('view','Details','invoices',item.id)}${item.paid ? '' : button('pay','Mark paid','invoices',item.id) + button('delete','Delete','invoices',item.id,'danger')}</div>`]));
  }
  byId('content').innerHTML = html;
  if (page === 'menu') renderMenu();
}

function renderMenu() {
  imageUrls.forEach(url => URL.revokeObjectURL(url));
  imageUrls = [];
  const query = (byId('menu-search')?.value || '').toLowerCase();
  const products = data.products.filter(item => `${item.name} ${data.categories.find(category => category.id === item.categoryId)?.name || ''}`.toLowerCase().includes(query));
  byId('menu-cards').innerHTML = products.length ? products.map(item => `<article class="card"><div class="product-image" data-image="${item.id}"><span>${escapeHtml(data.categories.find(category => category.id === item.categoryId)?.name || 'From our kitchen')}</span></div><div class="card-body"><div class="card-top"><h3>${escapeHtml(item.name)}</h3><span class="price">${money(item.price)}</span></div>${badge(item.available ? 'On the menu' : 'Unavailable',!item.available)}${staff() ? `<div class="actions">${button('edit','Edit','products',item.id)}${button('image','Photo','products',item.id)}${button('delete','Delete','products',item.id,'danger')}</div>` : ''}</div></article>`).join('') : empty(query ? 'No matching dishes' : 'The menu is being prepared');
  loadImages();
}

async function loadImages() {
  const elements = [...document.querySelectorAll('[data-image]')];
  try {
    const bearer = await accessToken();
    await Promise.all(elements.map(async element => {
      try {
        const response = await fetch('/api/products/' + element.dataset.image + '/image', {headers:{Authorization:'Bearer ' + bearer},signal:AbortSignal.timeout(10000)});
        if (!response.ok || !element.isConnected) return;
        const blob = await response.blob();
        if (!element.isConnected) return;
        const url = URL.createObjectURL(blob);
        imageUrls.push(url);
        const image = document.createElement('img');
        image.src = url;
        image.loading = 'lazy';
        image.alt = data.products.find(item => item.id === Number(element.dataset.image))?.name || 'Dish';
        element.replaceChildren(image);
      } catch { /* De menukaart blijft bruikbaar als een foto niet geladen kan worden. */ }
    }));
  } catch { /* De volgende API-aanvraag toont zo nodig de aanmeldmelding. */ }
}

function input(name, label, value = '', type = 'text', attributes = '') {
  return `<label>${label}<input name="${name}" type="${type}" value="${escapeHtml(value)}" ${attributes} required></label>`;
}
function select(name, label, options, value) {
  return `<label>${label}<select name="${name}" required><option value="">Choose ${label.toLowerCase()}</option>${options.map(([id,text]) => `<option value="${id}" ${Number(value) === Number(id) ? 'selected' : ''}>${escapeHtml(text)}</option>`).join('')}</select></label>`;
}
function availableInput(value = true) { return `<label><input type="checkbox" name="available" ${value ? 'checked' : ''}>Available</label>`; }
function textArea(name,label,value,max) { return `<label>${label}<textarea name="${name}" maxlength="${max}">${escapeHtml(value)}</textarea></label>`; }
function lineRow(line = {}) {
  return `<div class="line-row"><select aria-label="Order product" required><option value="">Choose a dish</option>${data.products.filter(item => item.available || item.id === line.productId).map(item => `<option value="${item.id}" ${item.id === line.productId ? 'selected' : ''}>${escapeHtml(item.name)} · ${money(item.price)}</option>`).join('')}</select><input aria-label="Quantity" type="number" min="1" max="100" required value="${line.quantity || 1}"><button type="button" class="icon-button" data-action="remove-line" aria-label="Remove order line">×</button></div>`;
}

function openEditor(resource, id, mode = 'edit', selectedOrder) {
  const item = data[resource].find(entry => entry.id === Number(id)) || {};
  editing = {resource,id:item.id,mode};
  const names = {products:'dish',categories:'category',tables:'table',reservations:'reservation',orders:'order',invoices:'invoice'};
  byId('dialog-title').textContent = (mode === 'view' ? 'Details: ' : item.id ? 'Edit ' : 'New ') + names[resource];
  byId('form-error').hidden = true;
  byId('save').hidden = mode === 'view';
  byId('save').textContent = 'Save changes';
  let fields = '';
  if (mode === 'view') {
    const order = resource === 'orders' ? item : data.orders.find(order => order.id === item.orderId);
    fields = `<p>${resource === 'invoices' ? 'Invoice' : 'Order'} #${item.id} · ${dateLabel(item.createdAt)}</p><p>${escapeHtml(order ? tableName(order.tableId) : '')}</p>${rowTable(['Item','Quantity','Unit price'],(order?.lines || []).map(line => [escapeHtml(line.productName),line.quantity,money(line.unitPrice)]))}<h3 style="margin-top:20px">Total: ${money(item.total)}</h3>${resource === 'invoices' ? badge(item.paid ? 'Paid' : 'Unpaid',!item.paid) : ''}`;
  } else if (mode === 'image') {
    byId('dialog-title').textContent = 'Upload dish photo';
    fields = `<p>${escapeHtml(item.name)} · PNG or JPEG, maximum 2 MiB.</p><label>Image file<input type="file" name="file" accept="image/png,image/jpeg" required></label>`;
  } else if (resource === 'categories') {
    fields = input('name','Name',item.name || '','text','maxlength="80"') + textArea('description','Description',item.description || '',500);
  } else if (resource === 'products') {
    fields = input('name','Dish name',item.name || '','text','maxlength="100"') + select('categoryId','Category',data.categories.map(category => [category.id,category.name]),item.categoryId) + input('price','Price (EUR)',item.price ?? '','number','min="0" max="99999999.99" step="0.01"') + availableInput(item.available);
  } else if (resource === 'tables') {
    fields = input('tableNumber','Table number',item.tableNumber || '','number','min="1"') + input('seats','Seats',item.seats || 2,'number','min="1" max="50"') + availableInput(item.available);
  } else if (resource === 'reservations') {
    fields = input('customerName','Guest name',item.customerName || user.name || user.preferred_username || '','text','maxlength="100"') + select('tableId','Table',data.tables.filter(table => table.available || table.id === item.tableId).map(table => [table.id,`Table ${table.tableNumber} · ${table.seats} seats`]),item.tableId) + input('startsAt','Date & time (your local time)',item.startsAt ? toLocalInput(item.startsAt) : '','datetime-local') + input('guests','Guests',item.guests || 2,'number','min="1" max="50"') + textArea('remarks','Special requests',item.remarks || '',1000) + '<p class="help">Your table is reserved for two hours. Overlapping bookings cannot be saved.</p>';
  } else if (resource === 'orders') {
    fields = select('tableId','Table',data.tables.filter(table => table.available || table.id === item.tableId).map(table => [table.id,tableName(table.id)]),item.tableId) + '<div class="line-heading"><strong>Order items</strong>' + button('add-line','+ Add item') + '</div><div id="order-lines">' + (item.lines?.length ? item.lines : [{}]).map(lineRow).join('') + '</div><p class="help">Use each dish once. Existing order lines keep their agreed price. The server calculates the total.</p>';
  } else if (resource === 'invoices') {
    fields = select('orderId','Order',data.orders.filter(order => !data.invoices.some(invoice => invoice.orderId === order.id)).map(order => [order.id,`Order #${order.id} · ${tableName(order.tableId)} · ${money(order.total)}`]),selectedOrder) + '<p class="help">Creating an invoice locks this order. You can mark it as paid afterwards.</p>';
  }
  byId('fields').innerHTML = fields;
  byId('editor').showModal();
}

async function saveEditor(event) {
  event.preventDefault();
  if (busy || !editing) return;
  busy = true;
  byId('save').disabled = true;
  byId('save').textContent = 'Saving…';
  byId('form-error').hidden = true;
  const {resource,id,mode} = editing;
  try {
    const form = new FormData(byId('editor-form'));
    if (mode === 'image') {
      const file = form.get('file');
      if (file.size > 2 * 1024 * 1024) throw new Error('Please choose an image smaller than 2 MiB.');
      await api(`products/${id}/image`,{method:'PUT',body:form});
    } else {
      const payload = Object.fromEntries(form);
      for (const key of ['categoryId','tableId','orderId','price','seats','tableNumber','guests']) if (key in payload) payload[key] = Number(payload[key]);
      if (resource === 'products' || resource === 'tables') payload.available = form.has('available');
      if (resource === 'reservations') payload.startsAt = new Date(payload.startsAt).toISOString().slice(0,19);
      if (resource === 'orders') {
        payload.lines = [...document.querySelectorAll('.line-row')].map(row => ({productId:Number(row.querySelector('select').value),quantity:Number(row.querySelector('input').value)}));
        if (!payload.lines.length) throw new Error('Add at least one order item.');
        if (new Set(payload.lines.map(line => line.productId)).size !== payload.lines.length) throw new Error('Use each dish only once; adjust its quantity instead.');
      }
      await api(resource + (id ? '/' + id : ''),{method:id ? 'PUT' : 'POST',body:JSON.stringify(payload)});
    }
    byId('editor').close();
    notify('Saved successfully.');
    try { await loadData(); } catch (error) { notify('Saved, but the list could not be refreshed: ' + error.message,true); }
  } catch (error) {
    if (byId('editor').open) {
      byId('form-error').textContent = error.message;
      byId('form-error').hidden = false;
      byId('form-error').focus();
    } else {
      notify(error.message, true);
    }
  } finally {
    busy = false;
    byId('save').disabled = false;
    byId('save').textContent = 'Save changes';
  }
}

document.addEventListener('input',event => { if (event.target.id === 'menu-search') renderMenu(); });
window.addEventListener('hashchange',() => { const next = location.hash.slice(1) || 'menu'; if (page !== next) { page = next; render(); } });
document.addEventListener('click',async event => {
  const target = event.target.closest('button');
  if (!target || busy) return;
  const {action,resource,id,page:nextPage} = target.dataset;
  try {
    if (nextPage) { page = nextPage; location.hash = nextPage; notify(''); render(); return; }
    if (action === 'login' || action === 'register') return await login(action === 'register');
    if (action === 'owner-register') return await showOwnerRegistration();
    if (action === 'staff-login') return await login(false,'staff');
    if (action === 'owner') { page = 'owner'; location.hash = 'owner'; render(); byId('content').focus(); return; }
    if (action === 'home') { page = 'menu'; location.hash = ''; render(); byId('content').focus(); return; }
    if (action === 'logout') return await logout();
    if (action === 'close') return byId('editor').close();
    if (action === 'reserve') return openEditor('reservations');
    if (action === 'new' || action === 'edit') return openEditor(resource,id);
    if (action === 'view' || action === 'image') return openEditor(resource,id,action);
    if (action === 'bill') return openEditor('invoices',undefined,'edit',id);
    if (action === 'add-line') {
      if (document.querySelectorAll('.line-row').length >= 100) return;
      byId('order-lines').insertAdjacentHTML('beforeend',lineRow()); return;
    }
    if (action === 'remove-line') { target.closest('.line-row').remove(); return; }
    if (action === 'refresh') { target.disabled = true; await loadData(); notify('Up to date.'); return; }
    if (action === 'delete' || action === 'pay') {
      const message = action === 'pay' ? 'Mark this invoice as paid? Payment cannot be reversed.' : resource === 'reservations' ? 'Cancel this reservation?' : 'Delete this item? This cannot be undone.';
      if (!window.confirm(message)) return;
      busy = true; target.disabled = true;
      await api(resource + '/' + id,action === 'pay' ? {method:'PUT',body:JSON.stringify({paid:true})} : {method:'DELETE'});
      notify(action === 'pay' ? 'Invoice marked as paid.' : resource === 'reservations' ? 'Reservation cancelled.' : 'Item deleted.');
      try { await loadData(); } catch (error) { notify('Change saved, but refresh failed: ' + error.message,true); }
    }
  } catch (error) { notify(error.message || 'Connection failed. Please try again.',true); }
  finally { busy = false; target.disabled = false; }
});
byId('editor-form').addEventListener('submit',saveEditor);
byId('editor').addEventListener('cancel',event => { if (busy) event.preventDefault(); });

async function initialize() {
  render();
  try {
    const response = await fetch('/app-config',{signal:AbortSignal.timeout(10000)});
    if (!response.ok) throw new Error('The restaurant service is unavailable. Please restart the application.');
    config = await response.json();
    const query = new URLSearchParams(location.search);
    if (query.has('code') || query.has('error')) {
      const pending = JSON.parse(sessionStorage.getItem('table-login') || 'null');
      sessionStorage.removeItem('table-login');
      history.replaceState({},'',location.pathname);
      if (!pending || pending.state !== query.get('state') || Date.now() - pending.created > 600000) throw new Error('Sign-in could not be verified. Please start again.');
      if (query.has('error')) throw new Error('Sign-in was cancelled. You can try again whenever you are ready.');
      saveTokens(await tokenRequest({grant_type:'authorization_code',code:query.get('code'),redirect_uri:pending.redirect,code_verifier:pending.verifier}));
      if (pending.audience === 'staff' && !staff()) {
        clearSession();
        throw new Error('This account has no staff access. Ask the restaurant owner to assign the STAFF role, or use Customer login.');
      }
    } else {
      try {
        tokens = JSON.parse(sessionStorage.getItem('table-session') || 'null');
        if (tokens) user = decodeClaims(tokens.access_token);
      } catch { clearSession(); }
    }
    render();
    if (user) await loadData();
  } catch (error) { notify(error.message || 'Unable to connect. Please reload the page.',true); }
}
initialize();
