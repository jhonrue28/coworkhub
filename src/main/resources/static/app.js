'use strict';
// Panel web de CoWorkHub: consume la API REST del mismo servidor con el JWT de /auth/login.

const DAY_START = 7;   // la franja del día va de 07:00 a 21:00 (horario más amplio de las sedes)
const DAY_HOURS = 14;
const ROOM_TYPES = [['MEETING_ROOM', 'Sala de reuniones'], ['PRIVATE_OFFICE', 'Oficina privada'], ['DESK', 'Escritorio']];
const STATUS = { PENDING: 'Pendiente', CONFIRMED: 'Confirmada', COMPLETED: 'Completada', CANCELLED: 'Cancelada', ACTIVE: 'Activo', SUSPENDED: 'Suspendido' };
const ROLES = { ADMIN: 'Administrador', RECEPTIONIST: 'Recepción', MEMBER: 'Miembro' };

const state = { token: sessionStorage.getItem('token'), profile: null, cache: {}, view: null };
const $ = sel => document.querySelector(sel);
const isAdmin = () => state.profile?.role === 'ADMIN';
const isStaff = () => ['ADMIN', 'RECEPTIONIST'].includes(state.profile?.role);

// ------------------------------------------------------------------ utilidades

/** Crea un elemento; el texto siempre entra como texto (nunca como HTML). */
function h(tag, attrs, ...kids) {
  const el = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs || {})) {
    if (v == null || v === false) continue;
    if (k.startsWith('on')) el.addEventListener(k.slice(2), v);
    else if (k === 'class') el.className = v;
    // setProperty también acepta variables CSS (--from), que Object.assign ignora
    else if (k === 'style' && typeof v === 'object') for (const [p, x] of Object.entries(v)) el.style.setProperty(p.startsWith('--') ? p : p.replace(/[A-Z]/g, m => '-' + m.toLowerCase()), x);
    else if (['value', 'checked', 'selected'].includes(k)) el[k] = v;
    else el.setAttribute(k, v === true ? '' : v);
  }
  for (const kid of kids.flat(Infinity)) {
    if (kid != null && kid !== false) el.append(kid instanceof Node ? kid : String(kid));
  }
  return el;
}

const money = v => v == null ? '—' : new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(v);
const hhmm = s => s ? String(s).slice(0, 5) : '';
const dateOf = s => String(s).slice(0, 10);
const timeOf = s => String(s).slice(11, 16);
const longDate = s => new Date(dateOf(s) + 'T00:00').toLocaleDateString('es-CO', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' });
const thisMonth = () => new Date().toISOString().slice(0, 7);
const badge = s => h('span', { class: 'badge ' + s.toLowerCase() }, STATUS[s] || s);

class ApiError extends Error {
  constructor(message, code, status) { super(message); this.code = code; this.status = status; }
}

async function api(path, { method = 'GET', body } = {}) {
  const headers = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (state.token) headers.Authorization = 'Bearer ' + state.token;
  const res = await fetch(path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = null; }
  if (res.status === 401 && path !== '/auth/login') {
    logout('Tu sesión venció. Inicia sesión de nuevo.');
    throw new ApiError('Sesión vencida', 'NO_AUTENTICADO', 401);
  }
  if (!res.ok) throw new ApiError(data?.error || `Error ${res.status}`, data?.codigo, res.status);
  return data;
}

function toast(message, { error = false, detail } = {}) {
  const el = h('div', { class: 'toast' + (error ? ' error' : ''), role: error ? 'alert' : 'status' }, message, detail && h('small', {}, detail));
  $('#toasts').append(el);
  setTimeout(() => el.remove(), error ? 7000 : 4000);
}

/** Ejecuta una acción con la API y muestra el error de negocio si falla. */
async function run(fn, button) {
  if (button) button.disabled = true;
  try { return await fn(); }
  catch (e) {
    if (e.status !== 401) toast(e.message, { error: true, detail: e.code && `Código ${e.code}` });
    throw e;
  } finally { if (button) button.disabled = false; }
}

// Los errores de la API ya se mostraron en un aviso; un error de programación no debe pasar en silencio
const quiet = p => p.catch(e => {
  if (e instanceof ApiError) return;
  console.error(e);
  toast('Algo falló en el panel. Recarga la página e inténtalo de nuevo.', { error: true, detail: e.message });
});

// ------------------------------------------------------------------ diálogo

const dialog = $('#dialog');
dialog.addEventListener('click', e => { if (e.target === dialog || e.target.closest('[data-close]')) dialog.close(); });

function openDialog(title, ...content) {
  $('#dialog-title').textContent = title;
  $('#dialog-body').replaceChildren(...content);
  if (!dialog.open) dialog.showModal();
}

// ------------------------------------------------------------------ formularios

/**
 * Campos: { key, label, type: text|email|password|number|time|date|datetime|select|checkbox|checks,
 *           options: () => [[valor, etiqueta]], required, step, min, max, only: 'create'|'edit' }
 */
function field(f, value) {
  const common = { name: f.key, required: f.required ?? (f.type !== 'checkbox' && f.type !== 'checks') };
  if (f.type === 'checkbox') {
    return h('label', { class: 'check' }, h('input', { type: 'checkbox', name: f.key, checked: value ?? f.default ?? false }), f.label);
  }
  if (f.type === 'checks') {
    const selected = new Set((value || []).map(Number));
    return h('fieldset', {}, h('legend', {}, f.label),
      h('div', { class: 'checks' }, f.options().map(([v, l]) =>
        h('label', { class: 'check' }, h('input', { type: 'checkbox', name: f.key, value: v, checked: selected.has(Number(v)) }), l))));
  }
  if (f.type === 'select') {
    const select = h('select', common,
      !common.required || value == null ? h('option', { value: '' }, f.placeholder || 'Elige una opción') : null,
      f.options().map(([v, l]) => h('option', { value: v }, l)));
    if (value != null) select.value = value;
    return h('label', {}, f.label, select);
  }
  const type = f.type === 'datetime' ? 'datetime-local' : (f.type || 'text');
  const v = value == null ? '' : f.type === 'time' ? hhmm(value) : f.type === 'datetime' ? String(value).slice(0, 16) : value;
  return h('label', {}, f.label, h('input', { ...common, type, value: v, step: f.step, min: f.min, max: f.max, autocomplete: f.type === 'password' ? 'new-password' : 'off' }));
}

function readForm(form, fields) {
  const data = {};
  for (const f of fields) {
    if (f.type === 'checkbox') data[f.key] = form.elements[f.key].checked;
    else if (f.type === 'checks') data[f.key] = [...form.querySelectorAll(`input[name="${f.key}"]:checked`)].map(i => Number(i.value));
    else {
      const raw = form.elements[f.key].value.trim();
      data[f.key] = raw === '' ? null : (f.type === 'number' || f.numeric) ? Number(raw) : raw;
    }
  }
  return data;
}

/** Formulario en el diálogo; onSubmit recibe los datos y debe devolver una promesa. */
function formDialog(title, fields, values, onSubmit, submitLabel = 'Guardar') {
  const error = h('p', { class: 'form-error', role: 'alert', hidden: true });
  const submit = h('button', { class: 'btn primary', type: 'submit' }, submitLabel);
  const form = h('form', { class: 'stack' }, fields.map(f => field(f, values?.[f.key])), error,
    h('div', { class: 'dialog-actions' }, h('button', { class: 'btn', type: 'button', 'data-close': true }, 'Cancelar'), submit));
  form.addEventListener('submit', async e => {
    e.preventDefault();
    error.hidden = true;
    submit.disabled = true;
    try {
      await onSubmit(readForm(form, fields));
      dialog.close();
    } catch (err) {
      if (err.status === 401) return;
      error.textContent = err.code ? `${err.message} (${err.code})` : err.message;
      error.hidden = false;
    } finally { submit.disabled = false; }
  });
  openDialog(title, form);
}

// ------------------------------------------------------------------ tablas

function table(columns, rows, empty) {
  if (!rows.length) return h('div', { class: 'table-wrap' }, h('p', { class: 'empty' }, empty));
  return h('div', { class: 'table-wrap' }, h('table', {},
    h('thead', {}, h('tr', {}, columns.map(c => h('th', { class: c.class, scope: 'col' }, c.label)))),
    h('tbody', {}, rows.map(r => h('tr', {}, columns.map(c => h('td', { class: c.class }, c.render(r))))))));
}

/** La reserva como barra dentro del día de 07:00 a 21:00. */
function slot(b) {
  const pos = s => { const [hh, mm] = timeOf(s).split(':').map(Number); return Math.min(DAY_HOURS, Math.max(0, hh + mm / 60 - DAY_START)); };
  return h('div', { class: 'slot' + (b.estado === 'CANCELLED' ? ' cancelled' : ''), title: `${timeOf(b.inicio)} a ${timeOf(b.fin)}` },
    h('small', {}, `${longDate(b.inicio)}, ${timeOf(b.inicio)}–${timeOf(b.fin)}`),
    h('div', { class: 'day-strip', 'aria-hidden': 'true' }, h('span', { style: { '--from': pos(b.inicio), '--to': pos(b.fin) } })));
}

function viewHead(title, description, ...actions) {
  return h('header', { class: 'view-head' }, h('div', {}, h('h1', {}, title), description && h('p', {}, description)), h('div', { class: 'row' }, actions));
}

// ------------------------------------------------------------------ catálogos en memoria (para los selects)

async function loadCache() {
  const [sedes, salas, equipamientos, servicios, planes] = await Promise.all(
    ['/api/sedes', '/api/salas', '/api/equipamientos', '/api/servicios-adicionales', '/api/planes'].map(p => api(p)));
  Object.assign(state.cache, { sedes, salas, equipamientos, servicios, planes });
  if (isStaff()) state.cache.miembros = await api('/api/miembros');
}

const opts = {
  sedes: () => state.cache.sedes.map(s => [s.id, s.nombre]),
  salas: () => state.cache.salas.filter(s => s.activa).map(s => [s.id, `${s.nombre} · ${s.sedeNombre} (hasta ${s.capacidad})`]),
  equipamientos: () => state.cache.equipamientos.map(e => [e.id, e.nombre]),
  servicios: () => state.cache.servicios.map(s => [s.id, `${s.nombre} (${money(s.precioUnitario)})`]),
  planes: () => state.cache.planes.map(p => [p.id, p.nombre]),
  miembros: () => (state.cache.miembros || []).filter(m => m.estado === 'ACTIVE').map(m => [m.id, `${m.nombreCompleto} (${m.documento})`]),
};

// ------------------------------------------------------------------ vista genérica de catálogo (CRUD)

function crudView(cfg) {
  return async view => {
    const list = h('div');
    const filterForm = cfg.filters && h('form', { class: 'row filters' }, cfg.filters.map(f => field({ ...f, required: false })),
      h('button', { class: 'btn', type: 'submit' }, 'Filtrar'));

    async function reload() {
      const query = filterForm ? new URLSearchParams(Object.entries(readForm(filterForm, cfg.filters)).filter(([, v]) => v != null)) : '';
      const rows = await api(cfg.path + (query.toString() ? '?' + query : ''));
      if (cfg.cacheKey && !query.toString()) state.cache[cfg.cacheKey] = rows;
      const actions = r => [
        ...(cfg.rowActions ? cfg.rowActions(r, reload) : []),
        cfg.canEdit() && h('button', { class: 'btn small', type: 'button', onclick: () => edit(r) }, 'Editar'),
        cfg.canEdit() && h('button', { class: 'btn small danger', type: 'button', onclick: e => remove(r, e.currentTarget) }, 'Eliminar'),
      ];
      list.replaceChildren(table([...cfg.columns, { label: '', class: 'actions', render: actions }], rows, cfg.empty));
    }

    const fieldsFor = mode => cfg.fields.filter(f => !f.only || f.only === mode);
    const create = () => formDialog(`Nuevo: ${cfg.singular}`, fieldsFor('create'), cfg.defaults, async data => {
      await api(cfg.path, { method: 'POST', body: data });
      toast(`${cfg.singular} creado`);
      await reload();
    }, 'Crear');
    const edit = r => formDialog(`Editar: ${cfg.singular}`, fieldsFor('edit'), cfg.toForm ? cfg.toForm(r) : r, async data => {
      await api(`${cfg.path}/${r.id}`, { method: 'PUT', body: data });
      toast('Cambios guardados');
      await reload();
    });
    const remove = (r, button) => {
      if (!confirm(`¿Eliminar "${r.nombre ?? r.nombreCompleto}"? No se puede deshacer.`)) return;
      quiet(run(async () => { await api(`${cfg.path}/${r.id}`, { method: 'DELETE' }); toast('Eliminado'); await reload(); }, button));
    };

    filterForm?.addEventListener('submit', e => { e.preventDefault(); quiet(run(reload)); });
    view.replaceChildren(
      viewHead(cfg.title, cfg.description, cfg.canCreate() && h('button', { class: 'btn primary', type: 'button', onclick: create }, `Nuevo: ${cfg.singular.toLowerCase()}`)),
      filterForm, list);
    await reload();
  };
}

const always = () => true;

const catalogViews = {
  sedes: crudView({
    title: 'Sedes', singular: 'Sede', path: '/api/sedes', cacheKey: 'sedes', canCreate: isAdmin, canEdit: isAdmin,
    description: 'Cada sede tiene su horario: solo se reserva dentro de él.',
    empty: 'Todavía no hay sedes.',
    columns: [
      { label: 'Nombre', render: r => r.nombre },
      { label: 'Ciudad', render: r => r.ciudad },
      { label: 'Dirección', render: r => r.direccion },
      { label: 'Horario', render: r => `${hhmm(r.horaApertura)} a ${hhmm(r.horaCierre)}` },
    ],
    fields: [
      { key: 'nombre', label: 'Nombre' },
      { key: 'ciudad', label: 'Ciudad' },
      { key: 'direccion', label: 'Dirección' },
      { key: 'horaApertura', label: 'Abre a las', type: 'time' },
      { key: 'horaCierre', label: 'Cierra a las', type: 'time' },
    ],
  }),
  salas: crudView({
    title: 'Salas', singular: 'Sala', path: '/api/salas', cacheKey: 'salas', canCreate: isAdmin, canEdit: isAdmin,
    description: 'Las salas inactivas no aparecen en la disponibilidad ni se pueden reservar.',
    empty: 'Todavía no hay salas.',
    filters: [{ key: 'sedeId', label: 'Sede', type: 'select', options: () => opts.sedes(), placeholder: 'Todas las sedes', numeric: true }],
    columns: [
      { label: 'Nombre', render: r => r.nombre },
      { label: 'Sede', render: r => r.sedeNombre },
      { label: 'Tipo', render: r => Object.fromEntries(ROOM_TYPES)[r.tipo] },
      { label: 'Capacidad', class: 'num', render: r => r.capacidad },
      { label: 'Tarifa por hora', class: 'num', render: r => money(r.tarifaHora) },
      { label: 'Equipamiento', render: r => r.equipamiento.map(e => e.nombre).join(', ') || '—' },
      { label: 'Estado', render: r => r.activa ? 'Activa' : h('span', { class: 'muted' }, 'Inactiva') },
    ],
    fields: [
      { key: 'nombre', label: 'Nombre' },
      { key: 'sedeId', label: 'Sede', type: 'select', options: () => opts.sedes(), numeric: true },
      { key: 'tipo', label: 'Tipo', type: 'select', options: () => ROOM_TYPES },
      { key: 'capacidad', label: 'Capacidad (personas)', type: 'number', min: 1 },
      { key: 'tarifaHora', label: 'Tarifa por hora (COP)', type: 'number', min: 1, step: '0.01' },
      { key: 'equipamientoIds', label: 'Equipamiento', type: 'checks', options: () => opts.equipamientos() },
      { key: 'activa', label: 'Se puede reservar', type: 'checkbox', default: true },
    ],
    toForm: r => ({ ...r, equipamientoIds: r.equipamiento.map(e => e.id) }),
  }),
  equipamientos: crudView({
    title: 'Equipamiento', singular: 'Equipamiento', path: '/api/equipamientos', cacheKey: 'equipamientos', canCreate: isAdmin, canEdit: isAdmin,
    description: 'Lo que puede tener una sala. Sirve para filtrar la disponibilidad.',
    empty: 'Todavía no hay equipamiento.',
    columns: [{ label: 'Nombre', render: r => r.nombre }],
    fields: [{ key: 'nombre', label: 'Nombre' }],
  }),
  servicios: crudView({
    title: 'Servicios adicionales', singular: 'Servicio', path: '/api/servicios-adicionales', cacheKey: 'servicios', canCreate: isAdmin, canEdit: isAdmin,
    description: 'Se agregan a una reserva mientras está pendiente. El precio queda fijo al agregarlo.',
    empty: 'Todavía no hay servicios.',
    columns: [
      { label: 'Nombre', render: r => r.nombre },
      { label: 'Precio unitario', class: 'num', render: r => money(r.precioUnitario) },
    ],
    fields: [
      { key: 'nombre', label: 'Nombre' },
      { key: 'precioUnitario', label: 'Precio unitario (COP)', type: 'number', min: 0, step: '0.01' },
    ],
  }),
  planes: crudView({
    title: 'Planes', singular: 'Plan', path: '/api/planes', cacheKey: 'planes', canCreate: isAdmin, canEdit: isAdmin,
    description: 'Las horas incluidas no se cobran. Las que pasan del plan se cobran con el descuento del plan.',
    empty: 'Todavía no hay planes.',
    columns: [
      { label: 'Nombre', render: r => r.nombre },
      { label: 'Horas incluidas al mes', class: 'num', render: r => r.horasIncluidasMes },
      { label: 'Descuento en horas extra', class: 'num', render: r => `${r.descuentoExcedente} %` },
      { label: 'Reservas activas máximas', class: 'num', render: r => r.maxReservasActivas },
    ],
    fields: [
      { key: 'nombre', label: 'Nombre' },
      { key: 'horasIncluidasMes', label: 'Horas incluidas al mes', type: 'number', min: 0 },
      { key: 'descuentoExcedente', label: 'Descuento en horas extra (%)', type: 'number', min: 0, max: 100 },
      { key: 'maxReservasActivas', label: 'Reservas activas máximas', type: 'number', min: 1 },
    ],
  }),
};

// ------------------------------------------------------------------ miembros

const membersView = crudView({
  title: 'Miembros', singular: 'Miembro', path: '/api/miembros', cacheKey: 'miembros', canCreate: always, canEdit: always,
  description: 'Al registrar un miembro se crea también su usuario para iniciar sesión. Un miembro suspendido no puede reservar.',
  empty: 'No hay miembros con esos filtros.',
  filters: [
    { key: 'nombre', label: 'Nombre contiene' },
    { key: 'estado', label: 'Estado', type: 'select', options: () => [['ACTIVE', 'Activo'], ['SUSPENDED', 'Suspendido']], placeholder: 'Todos' },
  ],
  columns: [
    { label: 'Nombre', render: r => r.nombreCompleto },
    { label: 'Documento', render: r => r.documento },
    { label: 'Email', render: r => r.email },
    { label: 'Plan', render: r => r.planNombre },
    { label: 'Estado', render: r => badge(r.estado) },
  ],
  fields: [
    { key: 'documento', label: 'Documento', only: 'create' },
    { key: 'nombreCompleto', label: 'Nombre completo' },
    { key: 'email', label: 'Email', type: 'email' },
    { key: 'planId', label: 'Plan', type: 'select', options: () => opts.planes(), numeric: true },
    { key: 'usuario', label: 'Usuario para iniciar sesión', only: 'create' },
    { key: 'password', label: 'Contraseña (mínimo 6 caracteres)', type: 'password', only: 'create' },
  ],
  rowActions: (r, reload) => [
    h('button', { class: 'btn small', type: 'button', onclick: () => consumptionDialog(r) }, 'Consumo'),
    h('button', {
      class: 'btn small', type: 'button',
      onclick: e => quiet(run(async () => {
        const action = r.estado === 'ACTIVE' ? 'suspender' : 'activar';
        await api(`/api/miembros/${r.id}/${action}`, { method: 'PATCH' });
        toast(r.estado === 'ACTIVE' ? `${r.nombreCompleto} quedó suspendido` : `${r.nombreCompleto} quedó activo`);
        await reload();
      }, e.currentTarget)),
    }, r.estado === 'ACTIVE' ? 'Suspender' : 'Activar'),
  ],
});

// ------------------------------------------------------------------ consumo

async function consumptionPanel(memberId) {
  const out = h('div');
  const month = h('input', { type: 'month', value: thisMonth(), 'aria-label': 'Mes' });
  async function load() {
    const c = await api(`/api/miembros/${memberId}/consumo?mes=${month.value}`);
    const used = Number(c.horasUsadas), included = Number(c.horasIncluidas);
    const pct = included > 0 ? Math.min(100, used / included * 100) : (used > 0 ? 100 : 0);
    out.replaceChildren(
      h('div', { class: 'figures' },
        h('div', { class: 'figure' }, h('b', {}, used), h('span', {}, 'horas usadas')),
        h('div', { class: 'figure' }, h('b', {}, included), h('span', {}, 'horas incluidas')),
        h('div', { class: 'figure' }, h('b', {}, Number(c.horasRestantes)), h('span', {}, 'horas restantes')),
        h('div', { class: 'figure' }, h('b', {}, Number(c.horasExcedentes)), h('span', {}, 'horas extra (se cobran)'))),
      h('div', { class: 'meter' + (Number(c.horasExcedentes) > 0 ? ' over' : ''), role: 'img', 'aria-label': `${Math.round(pct)} % de las horas del plan usadas` }, h('i', { style: { width: pct + '%' } })));
  }
  month.addEventListener('change', () => quiet(run(load)));
  await load();
  return h('div', { class: 'stack' }, h('label', { style: { justifySelf: 'start' } }, 'Mes', month), out);
}

async function consumptionDialog(member) {
  const panel = await run(() => consumptionPanel(member.id));
  openDialog(`Consumo de ${member.nombreCompleto}`, panel);
}

// ------------------------------------------------------------------ reservas

function bookingDialog(prefill = {}, onDone) {
  const fields = [
    isStaff()
      ? { key: 'miembroId', label: 'Miembro', type: 'select', options: () => opts.miembros(), numeric: true }
      : null,
    { key: 'salaId', label: 'Sala', type: 'select', options: () => opts.salas(), numeric: true },
    { key: 'inicio', label: 'Inicio', type: 'datetime', step: 1800 },
    { key: 'fin', label: 'Fin', type: 'datetime', step: 1800 },
    { key: 'asistentes', label: 'Asistentes', type: 'number', min: 1 },
  ].filter(Boolean);

  const services = h('div', { class: 'stack' });
  const addServiceRow = () => services.append(h('div', { class: 'service-row' },
    h('label', {}, 'Servicio', h('select', { name: 'srv' }, opts.servicios().map(([v, l]) => h('option', { value: v }, l)))),
    h('label', {}, 'Cantidad', h('input', { name: 'qty', type: 'number', min: 1, value: 1, required: true })),
    h('button', { class: 'btn small', type: 'button', onclick: e => e.currentTarget.parentElement.remove() }, 'Quitar')));

  const error = h('p', { class: 'form-error', role: 'alert', hidden: true });
  const submit = h('button', { class: 'btn primary', type: 'submit' }, 'Reservar');
  const form = h('form', { class: 'stack' },
    h('p', { class: 'muted' }, 'Duración de 30 min a 8 h, en bloques de 30 min, el mismo día y con al menos 1 h de anticipación.'),
    h('div', { class: 'grid-2' }, fields.map(f => field(f, prefill[f.key]))),
    h('fieldset', {}, h('legend', {}, 'Servicios adicionales (opcional)'), services,
      h('button', { class: 'btn small', type: 'button', style: { marginTop: '8px' }, onclick: addServiceRow }, 'Agregar servicio')),
    error,
    h('div', { class: 'dialog-actions' }, h('button', { class: 'btn', type: 'button', 'data-close': true }, 'Cancelar'), submit));

  form.addEventListener('submit', async e => {
    e.preventDefault();
    error.hidden = true;
    submit.disabled = true;
    const data = readForm(form, fields);
    if (!isStaff()) data.miembroId = state.profile.miembroId;
    data.servicios = [...services.children].map(row => ({ servicioId: Number(row.querySelector('[name=srv]').value), cantidad: Number(row.querySelector('[name=qty]').value) }));
    try {
      const b = await api('/api/reservas', { method: 'POST', body: data });
      dialog.close();
      toast(`Reserva creada: ${money(b.costoTotal)}`, { detail: `Sala ${money(b.costoSala)} + servicios ${money(b.costoServicios)}` });
      onDone?.();
    } catch (err) {
      if (err.status === 401) return;
      error.textContent = err.code ? `${err.message} (${err.code})` : err.message;
      error.hidden = false;
    } finally { submit.disabled = false; }
  });
  openDialog('Nueva reserva', form);
}

async function bookingDetail(id, onChange) {
  const b = await run(() => api(`/api/reservas/${id}`));
  const editable = b.estado === 'PENDING';
  const refresh = async () => { await bookingDetail(id, onChange); onChange?.(); };

  // null (no false) cuando no es editable: addForm?.x solo se salta con null/undefined
  const addForm = !editable ? null : h('form', { class: 'service-row' },
    h('label', {}, 'Servicio', h('select', { name: 'srv' }, opts.servicios().map(([v, l]) => h('option', { value: v }, l)))),
    h('label', {}, 'Cantidad', h('input', { name: 'qty', type: 'number', min: 1, value: 1, required: true })),
    h('button', { class: 'btn', type: 'submit' }, 'Agregar'));
  addForm?.addEventListener('submit', e => {
    e.preventDefault();
    quiet(run(async () => {
      await api(`/api/reservas/${id}/servicios`, { method: 'POST', body: { servicioId: Number(addForm.elements.srv.value), cantidad: Number(addForm.elements.qty.value) } });
      toast('Servicio agregado');
      await refresh();
    }, e.submitter));
  });

  openDialog(`Reserva #${b.id}`,
    h('div', { class: 'row', style: { justifyContent: 'space-between' } }, badge(b.estado), slot(b)),
    h('dl', { class: 'facts' },
      h('dt', {}, 'Sala'), h('dd', {}, b.salaNombre),
      h('dt', {}, 'Miembro'), h('dd', {}, b.miembroNombre),
      h('dt', {}, 'Asistentes'), h('dd', {}, b.asistentes),
      h('dt', {}, 'Costo de la sala'), h('dd', {}, money(b.costoSala)),
      h('dt', {}, 'Servicios'), h('dd', {}, money(b.costoServicios)),
      h('dt', {}, 'Total'), h('dd', {}, h('b', {}, money(b.costoTotal))),
      b.estado === 'CANCELLED' && [h('dt', {}, 'Cargo de cancelación'), h('dd', {}, money(b.cargoCancelacion))]),
    h('section', { class: 'stack' }, h('h3', {}, 'Servicios adicionales'),
      table([
        { label: 'Servicio', render: i => i.servicioNombre },
        { label: 'Cantidad', class: 'num', render: i => i.cantidad },
        { label: 'Precio', class: 'num', render: i => money(i.precioUnitarioAplicado) },
        { label: 'Subtotal', class: 'num', render: i => money(i.subtotal) },
        { label: '', class: 'actions', render: i => editable && h('button', {
          class: 'btn small danger', type: 'button',
          onclick: e => quiet(run(async () => { await api(`/api/reservas/${id}/servicios/${i.servicioId}`, { method: 'DELETE' }); toast('Servicio quitado'); await refresh(); }, e.currentTarget)),
        }, 'Quitar') },
      ], b.servicios, 'Sin servicios adicionales.'),
      editable ? addForm : h('p', { class: 'muted' }, 'Los servicios solo se cambian mientras la reserva está pendiente.')),
    h('div', { class: 'dialog-actions' }, bookingActions(b, refresh)));
}

function bookingActions(b, after) {
  const act = (label, path, confirmText, cls = 'btn small') => h('button', {
    class: cls, type: 'button',
    onclick: e => {
      if (confirmText && !confirm(confirmText)) return;
      quiet(run(async () => {
        const r = await api(`/api/reservas/${b.id}/${path}`, { method: 'POST' });
        toast(path === 'cancelar' ? `Reserva cancelada. Cargo: ${money(r.cargoCancelacion)}` : `Reserva ${STATUS[r.estado].toLowerCase()}`);
        await after();
      }, e.currentTarget));
    },
  }, label);
  return [
    isStaff() && b.estado === 'PENDING' && act('Confirmar', 'confirmar'),
    isStaff() && b.estado === 'CONFIRMED' && act('Completar', 'completar'),
    ['PENDING', 'CONFIRMED'].includes(b.estado) && act('Cancelar', 'cancelar',
      'Con 24 h o más de anticipación no hay cargo; entre 24 h y 2 h se cobra el 50 % de la sala; con menos de 2 h no se puede cancelar. ¿Cancelar la reserva?', 'btn small danger'),
  ];
}

async function bookingsView(view) {
  const list = h('div');
  let query;

  async function reload() {
    const rows = await api('/api/reservas?' + query);
    list.replaceChildren(table([
      { label: '#', class: 'num', render: b => b.id },
      { label: 'Cuándo', render: slot },
      { label: 'Sala', render: b => b.salaNombre },
      isStaff() && { label: 'Miembro', render: b => b.miembroNombre },
      { label: 'Estado', render: b => badge(b.estado) },
      { label: 'Total', class: 'num', render: b => money(b.costoTotal) },
      { label: '', class: 'actions', render: b => [
        h('button', { class: 'btn small', type: 'button', onclick: () => quiet(bookingDetail(b.id, () => quiet(reload()))) }, 'Ver'),
        bookingActions(b, reload),
      ] },
    ].filter(Boolean), rows, 'No hay reservas con ese filtro.'));
  }

  let filters = null;
  if (isStaff()) {
    // La API acepta un solo filtro a la vez
    const kind = h('select', { 'aria-label': 'Filtrar por' },
      h('option', { value: 'estado' }, 'Por estado'), h('option', { value: 'miembro' }, 'Por miembro'), h('option', { value: 'sala' }, 'Por sala y día'));
    const inputs = {
      estado: field({ key: 'estado', label: 'Estado', type: 'select', options: () => Object.entries(STATUS).slice(0, 4) }, 'PENDING'),
      miembro: field({ key: 'miembroId', label: 'Miembro', type: 'select', options: () => (state.cache.miembros || []).map(m => [m.id, m.nombreCompleto]) }),
      sala: h('div', { class: 'row' }, field({ key: 'salaId', label: 'Sala', type: 'select', options: () => state.cache.salas.map(s => [s.id, `${s.nombre} · ${s.sedeNombre}`]) }),
        field({ key: 'fecha', label: 'Día', type: 'date' }, new Date().toISOString().slice(0, 10))),
    };
    const holder = h('div', { class: 'row' }, inputs.estado);
    kind.addEventListener('change', () => holder.replaceChildren(inputs[kind.value]));
    filters = h('form', { class: 'row filters' }, h('label', {}, 'Filtrar', kind), holder, h('button', { class: 'btn', type: 'submit' }, 'Buscar'));
    const buildQuery = () => {
      const p = new URLSearchParams();
      filters.querySelectorAll('select[name], input[name]').forEach(el => p.set(el.name, el.value));
      return p.toString();
    };
    filters.addEventListener('submit', e => { e.preventDefault(); query = buildQuery(); quiet(run(reload)); });
    query = 'estado=PENDING';
  } else {
    query = 'miembroId=' + state.profile.miembroId;
  }

  view.replaceChildren(
    viewHead(isStaff() ? 'Reservas' : 'Mis reservas',
      isStaff() ? 'Confirma las pendientes, completa las que ya ocurrieron y cancela cuando haga falta.' : 'Tus reservas, de la más próxima a la más lejana.',
      h('button', { class: 'btn primary', type: 'button', onclick: () => bookingDialog({}, () => quiet(reload())) }, 'Nueva reserva')),
    filters, list);
  await reload();
}

// ------------------------------------------------------------------ disponibilidad

async function availabilityView(view) {
  const today = new Date().toISOString().slice(0, 10);
  const fields = [
    { key: 'sedeId', label: 'Sede', type: 'select', options: () => opts.sedes(), numeric: true },
    { key: 'fecha', label: 'Día', type: 'date', min: today },
    { key: 'horaInicio', label: 'Desde', type: 'time', step: 1800 },
    { key: 'horaFin', label: 'Hasta', type: 'time', step: 1800 },
    { key: 'capacidadMinima', label: 'Personas', type: 'number', min: 1, required: false },
  ];
  const equipment = { key: 'equipamientoIds', label: 'Debe tener', type: 'checks', options: () => opts.equipamientos() };
  const result = h('div', {}, h('p', { class: 'empty table-wrap' }, 'Elige sede, día y horario para ver las salas libres.'));
  const form = h('form', { class: 'section stack' },
    h('div', { class: 'row' }, fields.map(f => field(f, { sedeId: state.cache.sedes[0]?.id, fecha: today, horaInicio: '10:00', horaFin: '11:00' }[f.key]))),
    field(equipment), h('div', {}, h('button', { class: 'btn primary', type: 'submit' }, 'Buscar salas libres')));

  form.addEventListener('submit', e => {
    e.preventDefault();
    const data = readForm(form, [...fields, equipment]);
    const p = new URLSearchParams();
    for (const [k, v] of Object.entries(data)) {
      if (Array.isArray(v)) v.forEach(x => p.append(k, x));
      else if (v != null) p.set(k, v);
    }
    quiet(run(async () => {
      const rooms = await api('/api/disponibilidad?' + p);
      const prefill = { inicio: `${data.fecha}T${data.horaInicio}`, fin: `${data.fecha}T${data.horaFin}`, asistentes: data.capacidadMinima };
      result.replaceChildren(table([
        { label: 'Sala', render: r => r.nombre },
        { label: 'Tipo', render: r => Object.fromEntries(ROOM_TYPES)[r.tipo] },
        { label: 'Capacidad', class: 'num', render: r => r.capacidad },
        { label: 'Tarifa por hora', class: 'num', render: r => money(r.tarifaHora) },
        { label: 'Equipamiento', render: r => r.equipamiento.map(x => x.nombre).join(', ') || '—' },
        { label: '', class: 'actions', render: r => h('button', { class: 'btn small primary', type: 'button', onclick: () => bookingDialog({ ...prefill, salaId: r.id }, () => toast('Mira la reserva en Reservas')) }, 'Reservar') },
      ], rooms, 'No hay salas libres en ese horario. Prueba otro horario o quita filtros.'));
    }, e.submitter));
  });

  view.replaceChildren(viewHead('Disponibilidad', 'Salas activas sin reservas en el horario que elijas.'), form, result);
}

// ------------------------------------------------------------------ inicio

async function homeView(view) {
  const p = state.profile;
  const sections = [
    h('section', { class: 'section' }, h('h2', {}, 'Tu cuenta'),
      h('dl', { class: 'facts' },
        h('dt', {}, 'Usuario'), h('dd', {}, p.username),
        h('dt', {}, 'Rol'), h('dd', {}, ROLES[p.role]),
        p.miembroId && [h('dt', {}, 'Miembro'), h('dd', {}, '#' + p.miembroId)])),
  ];
  if (p.miembroId) {
    sections.push(h('section', { class: 'section' }, h('h2', {}, 'Tus horas del mes'), await consumptionPanel(p.miembroId)));
  } else {
    const pending = await api('/api/reservas?estado=PENDING');
    sections.push(h('section', { class: 'section' }, h('h2', {}, 'Por confirmar'),
      h('p', { class: pending.length ? '' : 'muted' }, pending.length
        ? `${pending.length} reserva${pending.length === 1 ? '' : 's'} pendiente${pending.length === 1 ? '' : 's'} de confirmar.`
        : 'No hay reservas pendientes.'),
      pending.length > 0 && h('button', { class: 'btn', type: 'button', style: { marginTop: '12px' }, onclick: () => show('reservas') }, 'Ver reservas pendientes')));
  }
  view.replaceChildren(viewHead(`Hola, ${p.username}`, null), ...sections);
}

// ------------------------------------------------------------------ navegación

const VIEWS = [
  { id: 'inicio', label: 'Inicio', render: homeView },
  { id: 'disponibilidad', label: 'Disponibilidad', render: availabilityView },
  { id: 'reservas', label: () => isStaff() ? 'Reservas' : 'Mis reservas', render: bookingsView },
  { id: 'miembros', label: 'Miembros', render: membersView, when: isStaff },
  { group: 'Catálogos' },
  { id: 'sedes', label: 'Sedes', render: catalogViews.sedes },
  { id: 'salas', label: 'Salas', render: catalogViews.salas },
  // Al miembro no le sirven como pantallas: el equipamiento lo filtra en Disponibilidad y los servicios los elige al reservar
  { id: 'equipamientos', label: 'Equipamiento', render: catalogViews.equipamientos, when: isStaff },
  { id: 'servicios', label: 'Servicios adicionales', render: catalogViews.servicios, when: isStaff },
  { id: 'planes', label: 'Planes', render: catalogViews.planes },
];

function renderNav() {
  $('#nav').replaceChildren(...VIEWS.filter(v => !v.when || v.when()).map(v => v.group
    ? h('p', { class: 'group' }, v.group)
    : h('button', { type: 'button', 'data-view': v.id, onclick: () => show(v.id) }, typeof v.label === 'function' ? v.label() : v.label)));
}

async function show(id) {
  const v = VIEWS.find(x => x.id === id && (!x.when || x.when())) || VIEWS[0];
  state.view = v.id;
  location.hash = v.id;
  document.querySelectorAll('#nav button').forEach(b => b.removeAttribute('aria-current'));
  document.querySelector(`#nav button[data-view="${v.id}"]`)?.setAttribute('aria-current', 'page');
  const view = $('#view');
  view.replaceChildren(h('p', { class: 'muted' }, 'Cargando…'));
  try { await v.render(view); view.focus(); }
  catch (e) {
    if (e.status === 401) return;
    view.replaceChildren(viewHead('No se pudo cargar esta sección', e.message), h('button', { class: 'btn', type: 'button', onclick: () => show(id) }, 'Reintentar'));
  }
}

// ------------------------------------------------------------------ sesión

async function start() {
  state.profile = await api('/auth/perfil');
  await loadCache();
  $('#who-name').textContent = state.profile.username;
  $('#who-role').textContent = ROLES[state.profile.role];
  $('#login').hidden = true;
  $('#app').hidden = false;
  renderNav();
  await show(location.hash.slice(1) || 'inicio');
}

function logout(message) {
  state.token = null;
  state.profile = null;
  sessionStorage.removeItem('token');
  if (dialog.open) dialog.close();
  $('#app').hidden = true;
  $('#login').hidden = false;
  const err = $('#login-error');
  err.textContent = message || '';
  err.hidden = !message;
}

$('#login-form').addEventListener('submit', async e => {
  e.preventDefault();
  const form = e.currentTarget;
  const button = form.querySelector('button');
  const err = $('#login-error');
  err.hidden = true;
  button.disabled = true;
  try {
    const r = await api('/auth/login', { method: 'POST', body: { username: form.elements.username.value.trim(), password: form.elements.password.value } });
    state.token = r.token;
    sessionStorage.setItem('token', r.token);
    form.reset();
    await start();
  } catch (ex) {
    if (ex.status === 401 && !state.token) { err.textContent = 'Usuario o contraseña incorrectos.'; err.hidden = false; }
    else if (ex.status !== 401) { err.textContent = 'No se pudo iniciar sesión: ' + ex.message; err.hidden = false; }
  } finally { button.disabled = false; }
});

$('#logout').addEventListener('click', () => { location.hash = ''; logout(); });

if (state.token) start().catch(() => logout());
else logout();
