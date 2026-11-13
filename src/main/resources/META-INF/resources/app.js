"use strict";

// ---------- State ----------

const state = {
  token: null,
  user: null,      // { id, username }
  account: null,   // { id, money, interestRate, owner }
  history: [],     // bookings made in this browser session (the API has no history endpoint)
  registerMode: false,
};

const $ = (id) => document.getElementById(id);

const chf = new Intl.NumberFormat("de-CH", { style: "currency", currency: "CHF" });
const time = new Intl.DateTimeFormat("de-CH", { hour: "2-digit", minute: "2-digit", second: "2-digit" });

// ---------- Session (token in localStorage, so a reload keeps you logged in) ----------

const SESSION_KEY = "bank.session";

function saveSession() {
  try {
    localStorage.setItem(SESSION_KEY, JSON.stringify({ token: state.token, user: state.user }));
  } catch { /* storage blocked: session only lives until reload */ }
}

function loadSession() {
  try {
    const saved = JSON.parse(localStorage.getItem(SESSION_KEY));
    if (saved && saved.token && saved.user) {
      state.token = saved.token;
      state.user = saved.user;
    }
  } catch { /* ignore */ }
}

function clearSession() {
  state.token = null;
  state.user = null;
  state.account = null;
  state.history = [];
  try { localStorage.removeItem(SESSION_KEY); } catch { /* ignore */ }
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
  if (!res.ok) throw new ApiError(res.status, await errorMessage(res, path));
  return res.status === 204 ? null : res.json();
}

async function errorMessage(res, path) {
  const text = await res.text().catch(() => "");

  // Validation errors come back as { violations: [{ field: "withdraw.body.amount", message: "..." }] }
  try {
    const data = JSON.parse(text);
    if (Array.isArray(data.violations) && data.violations.length) {
      return data.violations.map((v) => `${v.field.split(".").pop()}: ${v.message}`).join(", ");
    }
  } catch { /* not JSON */ }

  // Known messages from BankService.
  if (text.includes("Not enough money")) return "Nicht genug Guthaben.";
  if (text.includes("same account")) return "Du kannst nicht auf dein eigenes Konto überweisen.";

  switch (res.status) {
    case 400: return "Ungültige Eingabe.";
    case 401: return "Benutzername oder Passwort ist falsch.";
    case 404: return path === "/bank/transfer" ? "Diesen Empfänger gibt es nicht." : "Kein Konto gefunden.";
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

/** Runs an async action, disables the form's button meanwhile, and shows its error, if any. */
async function attempt(action, form) {
  hideMessage();
  const button = form?.querySelector("button[type=submit]");
  if (button) button.disabled = true;
  try {
    await action();
  } catch (e) {
    showMessage(e.message || "Unbekannter Fehler.", true);
  } finally {
    if (button) button.disabled = false;
  }
}

// ---------- DOM helper (textContent only, so user input can never inject HTML) ----------

function el(tag, props = {}, ...children) {
  const node = document.createElement(tag);
  for (const [key, value] of Object.entries(props)) {
    if (key === "class") node.className = value;
    else node.setAttribute(key, value);
  }
  for (const child of children) {
    if (child === null || child === undefined || child === false) continue;
    node.append(child instanceof Node ? child : String(child));
  }
  return node;
}

// ---------- Views ----------

function showView(name) {
  $("view-login").hidden = name !== "login";
  $("view-account").hidden = name !== "account";
  const loggedIn = name !== "login";
  $("user-box").hidden = !loggedIn;
  if (loggedIn) $("user-name").textContent = state.user.username;
}

// ---------- Auth ----------

function setRegisterMode(on) {
  state.registerMode = on;
  $("auth-title").textContent = on ? "Registrieren" : "Anmelden";
  $("auth-submit").textContent = on ? "Konto eröffnen" : "Anmelden";
  $("auth-switch-text").textContent = on ? "Schon ein Konto?" : "Noch kein Konto?";
  $("auth-switch").textContent = on ? "Anmelden" : "Registrieren";
  $("auth-bonus").hidden = !on;
  $("auth-form").elements.password.autocomplete = on ? "new-password" : "current-password";
}

async function submitAuth(event) {
  event.preventDefault();
  const form = event.target;
  const credentials = { username: form.elements.username.value.trim(), password: form.elements.password.value };
  const registering = state.registerMode;

  await attempt(async () => {
    if (registering) await api("POST", "/auth/register", credentials);
    const login = await api("POST", "/auth/login", credentials);
    state.token = login.token;
    state.user = login.user;
    saveSession();
    form.reset();
    setRegisterMode(false);
    await enterApp();
    if (registering) showMessage(`Willkommen! Dein Konto wurde mit ${chf.format(state.account.money)} eröffnet.`);
  }, form);
}

function logout() {
  clearSession();
  showView("login");
}

async function enterApp() {
  await loadAccount();
  showView("account");
}

// ---------- Account ----------

async function loadAccount() {
  state.account = await api("GET", "/bank/account");
  renderAccount();
}

function renderAccount() {
  $("balance").textContent = chf.format(state.account.money);
  $("account-meta").textContent = `Konto Nr. ${state.account.id} · Zins ${state.account.interestRate} %`;
  renderHistory();
}

function renderHistory() {
  $("history").replaceChildren(...state.history.map((b) =>
    el("tr", {},
      el("td", {}, time.format(b.at)),
      el("td", {}, b.kind),
      el("td", {}, b.details),
      el("td", { class: "right negative" }, "− " + chf.format(b.amount)),
      el("td", { class: "right" }, chf.format(b.balance)),
    ),
  ));
  $("history-empty").hidden = state.history.length > 0;
}

function addBooking(kind, details, amount) {
  state.history.unshift({ at: new Date(), kind, details, amount, balance: state.account.money });
}

/** Reads a CHF amount from an input, rounded to Rappen. */
function readAmount(input) {
  return Math.round(Number(input.value) * 100) / 100;
}

/** Quarkus sends a 400 without its message, so check the balance here to show a clear error. */
function checkAmount(amount) {
  if (!(amount > 0)) {
    showMessage("Der Betrag muss grösser als 0 sein.", true);
    return false;
  }
  if (amount > state.account.money) {
    showMessage(`Nicht genug Guthaben. Verfügbar: ${chf.format(state.account.money)}.`, true);
    return false;
  }
  return true;
}

async function submitWithdraw(event) {
  event.preventDefault();
  const form = event.target;
  const amount = readAmount(form.elements.amount);
  if (!checkAmount(amount)) return;

  await attempt(async () => {
    state.account = await api("POST", "/bank/withdraw", { amount });
    addBooking("Abhebung", "–", amount);
    renderAccount();
    form.reset();
    showMessage(`${chf.format(amount)} abgehoben.`);
  }, form);
}

async function submitTransfer(event) {
  event.preventDefault();
  const form = event.target;
  const toUsername = form.elements.toUsername.value.trim();
  const amount = readAmount(form.elements.amount);

  if (toUsername === state.user.username) {
    showMessage("Du kannst nicht auf dein eigenes Konto überweisen.", true);
    return;
  }
  if (!checkAmount(amount)) return;
  if (!confirm(`${chf.format(amount)} an ${toUsername} überweisen?`)) return;

  await attempt(async () => {
    state.account = await api("POST", "/bank/transfer", { toUsername, amount });
    addBooking("Überweisung", "an " + toUsername, amount);
    renderAccount();
    form.reset();
    showMessage(`${chf.format(amount)} an ${toUsername} überwiesen.`);
  }, form);
}

// ---------- Wiring ----------

function init() {
  $("auth-form").addEventListener("submit", submitAuth);
  $("auth-switch").addEventListener("click", () => setRegisterMode(!state.registerMode));
  $("logout").addEventListener("click", logout);

  $("withdraw-form").addEventListener("submit", submitWithdraw);
  $("transfer-form").addEventListener("submit", submitTransfer);
  for (const button of document.querySelectorAll("[data-quick]")) {
    button.addEventListener("click", () => {
      $("withdraw-form").elements.amount.value = button.dataset.quick;
    });
  }

  loadSession();
  if (state.token) attempt(enterApp);
  else showView("login");
}

init();
