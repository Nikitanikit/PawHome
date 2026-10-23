/* ============================================================
   api.js - one place that talks to the Flask backend, plus small
   shared UI helpers (toast notifications, confirm modal) used by
   every page.
   ============================================================ */
const API_BASE = "/api";

const Auth = {
  getToken(){ return localStorage.getItem("ph_token"); },
  getRole(){ return localStorage.getItem("ph_role"); },
  getUser(){ const s = localStorage.getItem("ph_user"); return s ? JSON.parse(s) : null; },
  setSession(token, role, profile){
    localStorage.setItem("ph_token", token);
    localStorage.setItem("ph_role", role);
    localStorage.setItem("ph_user", JSON.stringify(profile));
  },
  clear(){
    localStorage.removeItem("ph_token");
    localStorage.removeItem("ph_role");
    localStorage.removeItem("ph_user");
  },
  isLoggedIn(){ return !!this.getToken() && this.getRole() === "user"; },
  isAdmin(){ return !!this.getToken() && this.getRole() === "admin"; },
  logout(redirect){
    this.clear();
    window.location.href = redirect || "login.html";
  }
};

async function apiFetch(path, options = {}){
  const headers = Object.assign({ "Content-Type": "application/json" }, options.headers || {});
  const token = Auth.getToken();
  if (token) headers["Authorization"] = "Bearer " + token;

  let res;
  try{
    res = await fetch(API_BASE + path, Object.assign({}, options, { headers }));
  }catch(err){
    throw new Error("Could not reach the server. Please check the backend is running.");
  }

  let data = {};
  try{ data = await res.json(); }catch(e){ /* no body */ }

  if (res.status === 401 && (Auth.getToken())){
    // session expired mid-use
    Auth.clear();
  }
  if (!res.ok){
    throw new Error(data.error || "Something went wrong. Please try again.");
  }
  return data;
}

const api = {
  get:  (path) => apiFetch(path, { method: "GET" }),
  post: (path, body) => apiFetch(path, { method: "POST", body: JSON.stringify(body || {}) }),
  put:  (path, body) => apiFetch(path, { method: "PUT", body: JSON.stringify(body || {}) }),
  del:  (path) => apiFetch(path, { method: "DELETE" }),
};

/* ---------------- Toast notifications ---------------- */
function toast(message, type = "success"){
  let wrap = document.querySelector(".toast-wrap");
  if (!wrap){
    wrap = document.createElement("div");
    wrap.className = "toast-wrap";
    document.body.appendChild(wrap);
  }
  const el = document.createElement("div");
  el.className = `toast ${type}`;
  el.textContent = message;
  wrap.appendChild(el);
  setTimeout(() => el.remove(), 3800);
}

/* ---------------- Confirm modal (used before delete / logout) ---------------- */
function confirmAction({ title, message, confirmLabel = "Confirm", danger = true }){
  return new Promise((resolve) => {
    let backdrop = document.getElementById("confirm-modal");
    if (backdrop) backdrop.remove();

    backdrop = document.createElement("div");
    backdrop.id = "confirm-modal";
    backdrop.className = "modal-backdrop show";
    backdrop.innerHTML = `
      <div class="modal">
        <h3>${title}</h3>
        <p style="margin:0;">${message}</p>
        <div class="modal-actions">
          <button class="btn btn-outline btn-sm" data-act="cancel">Cancel</button>
          <button class="btn ${danger ? 'btn-danger' : 'btn-primary'} btn-sm" data-act="ok">${confirmLabel}</button>
        </div>
      </div>`;
    document.body.appendChild(backdrop);

    backdrop.addEventListener("click", (e) => {
      if (e.target === backdrop || e.target.dataset.act === "cancel"){
        backdrop.remove();
        resolve(false);
      }
      if (e.target.dataset.act === "ok"){
        backdrop.remove();
        resolve(true);
      }
    });
  });
}

function setLoading(el, isLoading, loadingText = "Loading..."){
  if (!el) return;
  if (isLoading){
    el.dataset.originalHtml = el.innerHTML;
    el.innerHTML = `<span class="spinner" style="width:14px;height:14px;border-width:2px;"></span> ${loadingText}`;
    el.disabled = true;
  } else {
    if (el.dataset.originalHtml) el.innerHTML = el.dataset.originalHtml;
    el.disabled = false;
  }
}

function escapeHtml(str){
  const div = document.createElement("div");
  div.textContent = str ?? "";
  return div.innerHTML;
}

function formatMoney(n){
  return "₹" + Number(n || 0).toLocaleString("en-IN");
}
