"use strict";

// ---------- State ----------

const state = {
  token: null,
  user: null,      // { id, username }
  gyms: [],
  routes: [],
  climbs: [],
  registerMode: false,
};

const RESULTS = {
  FLASH: "Flash",
  ONSIGHT: "Onsight",
  TOP_ROPE: "Toprope",
  REPEAT: "Wiederholung",
  FIRST_ASCEND: "Erstbegehung",
  NORMAL_ASCEN: "Durchstieg",
};

const $ = (id) => document.getElementById(id);

// ---------- Session (token in localStorage, so a reload keeps you logged in) ----------

function saveSession() {
  try {
    localStorage.setItem("climbtracker.session", JSON.stringify({ token: state.token, user: state.user }));
  } catch { /* storage blocked: session only lives until reload */ }
}

function loadSession() {
  try {
    const saved = JSON.parse(localStorage.getItem("climbtracker.session"));
    if (saved && saved.token && saved.user) {
      state.token = saved.token;
      state.user = saved.user;
    }
  } catch { /* ignore */ }
}

function clearSession() {
  state.token = null;
  state.user = null;
  try { localStorage.removeItem("climbtracker.session"); } catch { /* ignore */ }
}

// ---------- API ----------

class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

async function api(method, path, body) {
  const headers = { "Content-Type": "application/json" };
  if (state.token) headers.Authorization = "Bearer " + state.token;

  const res = await fetch(path, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  // Token expired or invalid: back to the login screen.
  if (res.status === 401 && state.token) {
    logout();
    throw new ApiError(401, "Deine Sitzung ist abgelaufen. Bitte melde dich neu an.");
  }
  if (!res.ok) throw new ApiError(res.status, await errorMessage(res));
  return res.status === 204 ? null : res.json();
}

async function errorMessage(res) {
  // Validation errors come back as { violations: [{ field: "create.dto.grade", message: "..." }] }
  try {
    const data = await res.json();
    if (Array.isArray(data.violations) && data.violations.length) {
      return data.violations
        .map((v) => `${v.field.split(".").pop()}: ${v.message}`)
        .join(", ");
    }
  } catch { /* no JSON body */ }

  switch (res.status) {
    case 400: return "Ungültige Eingabe.";
    case 401: return "Benutzername oder Passwort ist falsch.";
    case 403: return "Das darf nur, wer den Eintrag erstellt hat.";
    case 404: return "Eintrag nicht gefunden.";
    case 409: return "Dieser Benutzername ist schon vergeben.";
    default: return `Fehler ${res.status}. Bitte später nochmals versuchen.`;
  }
}

// ---------- Messages ----------

let messageTimer;

function showMessage(text, isError = false) {
  const box = $("message");
  box.textContent = text;
  box.classList.toggle("error", isError);
  box.hidden = false;
  clearTimeout(messageTimer);
  if (!isError) messageTimer = setTimeout(() => (box.hidden = true), 3000);
}

function hideMessage() {
  $("message").hidden = true;
}

/** Runs an async action and shows its error, if any. */
async function attempt(action) {
  hideMessage();
  try {
    await action();
  } catch (e) {
    showMessage(e.message || "Unbekannter Fehler.", true);
  }
}

// ---------- DOM helper (textContent only, so user input can never inject HTML) ----------

function el(tag, props = {}, ...children) {
  const node = document.createElement(tag);
  for (const [key, value] of Object.entries(props)) {
    if (key === "class") node.className = value;
    else if (key === "style") Object.assign(node.style, value);
    else if (key.startsWith("on")) node.addEventListener(key.slice(2), value);
    else node.setAttribute(key, value);
  }
  for (const child of children) {
    if (child === null || child === undefined || child === false) continue;
    node.append(child instanceof Node ? child : String(child));
  }
  return node;
}

// ---------- Views ----------

const VIEWS = ["login", "routes", "gyms", "climbs"];

function showView(name) {
  for (const v of VIEWS) $("view-" + v).hidden = v !== name;
  for (const tab of document.querySelectorAll(".tab")) {
    if (tab.dataset.view === name) tab.setAttribute("aria-current", "page");
    else tab.removeAttribute("aria-current");
  }
  const loggedIn = name !== "login";
  $("nav").hidden = !loggedIn;
  $("user-box").hidden = !loggedIn;
  if (loggedIn) $("user-name").textContent = state.user.username;
}

// ---------- Auth ----------

function setRegisterMode(on) {
  state.registerMode = on;
  $("auth-title").textContent = on ? "Registrieren" : "Anmelden";
  $("auth-submit").textContent = on ? "Konto erstellen" : "Anmelden";
  $("auth-switch-text").textContent = on ? "Schon ein Konto?" : "Noch kein Konto?";
  $("auth-switch").textContent = on ? "Anmelden" : "Registrieren";
  $("auth-form").elements.password.autocomplete = on ? "new-password" : "current-password";
}

async function submitAuth(event) {
  event.preventDefault();
  const form = event.target;
  const credentials = { username: form.elements.username.value.trim(), password: form.elements.password.value };

  await attempt(async () => {
    if (state.registerMode) await api("POST", "/auth/register", credentials);
    const login = await api("POST", "/auth/login", credentials);
    state.token = login.token;
    state.user = login.user;
    saveSession();
    form.reset();
    setRegisterMode(false);
    await enterApp();
  });
}

function logout() {
  clearSession();
  state.gyms = state.routes = state.climbs = [];
  showView("login");
}

async function enterApp() {
  showView("routes");
  await loadAll();
}

// ---------- Data ----------

async function loadAll() {
  const [gyms, routes, climbs] = await Promise.all([
    api("GET", "/gyms"),
    api("GET", "/routes"),
    api("GET", "/climbs"),
  ]);
  state.gyms = gyms.sort((a, b) => a.name.localeCompare(b.name));
  state.routes = routes;
  state.climbs = climbs;
  renderAll();
}

const gymName = (id) => state.gyms.find((g) => g.id === id)?.name ?? "–";
const routeById = (id) => state.routes.find((r) => r.id === id);

function renderAll() {
  renderGymSelects();
  renderRoutes();
  renderGyms();
  renderClimbs();
}

function renderGymSelects() {
  const filter = $("filter-gym");
  const selected = filter.value;
  filter.replaceChildren(
    el("option", { value: "" }, "Alle Hallen"),
    ...state.gyms.map((g) => el("option", { value: g.id }, g.name)),
  );
  filter.value = state.gyms.some((g) => String(g.id) === selected) ? selected : "";

  $("route-form").elements.gymId.replaceChildren(...state.gyms.map((g) => el("option", { value: g.id }, g.name)));
}

// ---------- Routes ----------

function filteredRoutes() {
  const gymId = $("filter-gym").value;
  const grade = $("filter-grade").value.trim().toLowerCase();
  const onlyActive = $("filter-active").checked;
  const onlyMine = $("filter-mine").checked;

  return state.routes
    .filter((r) => !gymId || String(r.gymId) === gymId)
    .filter((r) => !grade || r.grade.toLowerCase().startsWith(grade))
    .filter((r) => !onlyActive || r.active)
    .filter((r) => !onlyMine || r.ownerId === state.user.id)
    .sort((a, b) => gymName(a.gymId).localeCompare(gymName(b.gymId)) || a.grade.localeCompare(b.grade));
}

function renderRoutes() {
  const routes = filteredRoutes();
  $("route-list").replaceChildren(...routes.map((r) =>
    el("tr", {},
      el("td", {}, el("span", { class: "swatch", style: { backgroundColor: r.color }, title: r.color })),
      el("td", {}, el("strong", {}, r.grade)),
      el("td", {}, gymName(r.gymId)),
      el("td", {}, el("span", { class: r.active ? "badge on" : "badge" }, r.active ? "Aktiv" : "Abgeschraubt")),
      el("td", { class: "right" },
        el("div", { class: "row-actions" },
          el("button", { class: "small secondary", type: "button", onclick: () => openClimbForm(r.id) }, "Climb eintragen"),
          r.ownerId === state.user.id &&
            el("button", { class: "small secondary", type: "button", onclick: () => openRouteForm(r) }, "Bearbeiten"),
        ),
      ),
    ),
  ));
  $("route-empty").hidden = routes.length > 0;
}

function openRouteForm(route) {
  if (!state.gyms.length) {
    showMessage("Lege zuerst eine Halle an.", true);
    return;
  }
  const form = $("route-form");
  form.reset();
  form.elements.editId.value = route ? route.id : "";
  $("route-form-title").textContent = route ? "Route bearbeiten" : "Neue Route";
  if (route) {
    form.elements.gymId.value = route.gymId;
    form.elements.grade.value = route.grade;
    form.elements.color.value = route.color;
    form.elements.active.checked = route.active;
  } else if ($("filter-gym").value) {
    form.elements.gymId.value = $("filter-gym").value; // preselect the gym you are looking at
  }
  form.hidden = false;
  form.elements.grade.focus();
}

async function submitRoute(event) {
  event.preventDefault();
  const form = event.target;
  const id = form.elements.editId.value;
  const route = {
    gymId: Number(form.elements.gymId.value),
    grade: form.elements.grade.value.trim(),
    color: form.elements.color.value,
    active: form.elements.active.checked,
  };
  await attempt(async () => {
    if (id) await api("PUT", "/routes/" + id, route);
    else await api("POST", "/routes", route);
    form.hidden = true;
    await loadAll();
    showMessage(id ? "Route gespeichert." : "Route erstellt.");
  });
}

// ---------- Gyms ----------

function renderGyms() {
  $("gym-list").replaceChildren(...state.gyms.map((g) => {
    const routeCount = state.routes.filter((r) => r.gymId === g.id).length;
    const safeUrl = /^https?:\/\//i.test(g.website) ? g.website : null;
    return el("tr", {},
      el("td", {}, el("strong", {}, g.name)),
      el("td", {}, g.address),
      el("td", {}, safeUrl ? el("a", { href: safeUrl, target: "_blank", rel: "noopener" }, g.website) : g.website),
      el("td", {}, routeCount),
      el("td", { class: "right" },
        el("button", { class: "small secondary", type: "button", onclick: () => showGymRoutes(g.id) }, "Routen anzeigen"),
      ),
    );
  }));
  $("gym-empty").hidden = state.gyms.length > 0;
}

function showGymRoutes(gymId) {
  $("filter-gym").value = gymId;
  renderRoutes();
  showView("routes");
}

async function submitGym(event) {
  event.preventDefault();
  const form = event.target;
  const gym = {
    name: form.elements.name.value.trim(),
    address: form.elements.address.value.trim(),
    website: form.elements.website.value.trim(),
  };
  await attempt(async () => {
    await api("POST", "/gyms", gym);
    form.reset();
    form.hidden = true;
    await loadAll();
    showMessage("Halle erstellt.");
  });
}

// ---------- Climbs ----------

function routeLabel(route) {
  return route ? `${route.grade} · ${gymName(route.gymId)}` : "Unbekannte Route";
}

function renderClimbs() {
  const mine = state.climbs.filter((c) => c.ownerId === state.user.id).sort((a, b) => b.id - a.id);
  $("climb-list").replaceChildren(...mine.map((c) => {
    const route = routeById(c.routeId);
    return el("tr", {},
      el("td", {},
        route && el("span", { class: "swatch", style: { backgroundColor: route.color } }),
        el("strong", {}, route ? route.grade : "?"),
      ),
      el("td", {}, route ? gymName(route.gymId) : "–"),
      el("td", {}, RESULTS[c.result] ?? c.result),
      el("td", {}, c.attempts),
      el("td", {}, `${c.rating} / 5`),
      el("td", {}, c.note),
      el("td", { class: "right" },
        el("button", { class: "small danger", type: "button", onclick: () => deleteClimb(c) }, "Löschen"),
      ),
    );
  }));
  $("climb-empty").hidden = mine.length > 0;
}

function openClimbForm(routeId) {
  const activeRoutes = state.routes
    .filter((r) => r.active || r.id === routeId)
    .sort((a, b) => routeLabel(a).localeCompare(routeLabel(b)));
  if (!activeRoutes.length) {
    showMessage("Es gibt noch keine aktiven Routen.", true);
    return;
  }
  const form = $("climb-form");
  form.reset();
  form.elements.routeId.replaceChildren(...activeRoutes.map((r) => el("option", { value: r.id }, routeLabel(r))));
  form.elements.result.replaceChildren(...Object.entries(RESULTS).map(([value, label]) => el("option", { value }, label)));
  if (routeId) form.elements.routeId.value = routeId;
  form.hidden = false;
  showView("climbs");
  form.elements.attempts.focus();
}

async function submitClimb(event) {
  event.preventDefault();
  const form = event.target;
  const climb = {
    routeId: Number(form.elements.routeId.value),
    result: form.elements.result.value,
    attempts: Number(form.elements.attempts.value),
    rating: Number(form.elements.rating.value),
    note: form.elements.note.value.trim(),
  };
  await attempt(async () => {
    await api("POST", "/climbs", climb);
    form.hidden = true;
    await loadAll();
    showMessage("Climb eingetragen.");
  });
}

async function deleteClimb(climb) {
  if (!confirm(`Climb auf ${routeLabel(routeById(climb.routeId))} löschen?`)) return;
  await attempt(async () => {
    await api("DELETE", "/climbs/" + climb.id);
    await loadAll();
    showMessage("Climb gelöscht.");
  });
}

// ---------- Wiring ----------

function init() {
  $("auth-form").addEventListener("submit", submitAuth);
  $("auth-switch").addEventListener("click", () => setRegisterMode(!state.registerMode));
  $("logout").addEventListener("click", logout);

  for (const tab of document.querySelectorAll(".tab")) {
    tab.addEventListener("click", () => {
      hideMessage();
      showView(tab.dataset.view);
    });
  }

  // "Abbrechen" closes the form it belongs to.
  for (const button of document.querySelectorAll("[data-cancel]")) {
    button.addEventListener("click", () => (button.closest("form").hidden = true));
  }

  $("route-new").addEventListener("click", () => openRouteForm(null));
  $("route-form").addEventListener("submit", submitRoute);
  $("gym-new").addEventListener("click", () => {
    $("gym-form").hidden = false;
    $("gym-form").elements.name.focus();
  });
  $("gym-form").addEventListener("submit", submitGym);
  $("climb-new").addEventListener("click", () => openClimbForm(null));
  $("climb-form").addEventListener("submit", submitClimb);

  for (const id of ["filter-gym", "filter-active", "filter-mine"]) $(id).addEventListener("change", renderRoutes);
  $("filter-grade").addEventListener("input", renderRoutes);

  loadSession();
  if (state.token) attempt(enterApp);
  else showView("login");
}

init();
