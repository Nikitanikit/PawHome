/* ============================================================
   admin.js - sidebar layout + auth guard shared by every admin page.
   Call requireAdmin() at the top of each admin page (except login),
   then renderAdminShell(activeKey, pageTitle) to build the chrome.
   ============================================================ */
function requireAdmin(){
  if (!Auth.isAdmin()){
    window.location.href = "login.html";
    return false;
  }
  return true;
}

const ADMIN_NAV = [
  { key: "dashboard", href: "dashboard.html", icon: "📊", label: "Dashboard" },
  { key: "pets", href: "manage-pets.html", icon: "🐾", label: "Manage Pets" },
  { key: "add-pet", href: "add-pet.html", icon: "➕", label: "Add Pet" },
  { key: "users", href: "manage-users.html", icon: "👤", label: "Manage Users" },
  { key: "applications", href: "applications.html", icon: "📋", label: "Adoption Applications" },
  { key: "reports", href: "reports.html", icon: "📈", label: "Reports" },
  { key: "settings", href: "settings.html", icon: "⚙️", label: "Settings" },
];

function renderAdminShell(activeKey, pageTitle){
  const shell = document.getElementById("adminShell");
  const admin = Auth.getUser();

  shell.innerHTML = `
    <aside class="admin-sidebar" id="adminSidebar">
      <a href="dashboard.html" class="brand">🐾 PawHome Admin</a>
      <ul class="admin-nav">
        ${ADMIN_NAV.map(n => `<li><a href="${n.href}" class="${n.key === activeKey ? 'active' : ''}">${n.icon} ${n.label}</a></li>`).join("")}
      </ul>
      <button class="logout-btn" id="adminLogoutBtn">↩ Logout</button>
    </aside>
    <main class="admin-main">
      <button class="btn btn-outline btn-sm admin-mobile-toggle" id="adminMenuToggle">☰ Menu</button>
      <div class="admin-topbar">
        <h1>${pageTitle}</h1>
        <div style="font-size:14px; color:var(--ink-soft);">${admin ? "Signed in as <strong>" + escapeHtml(admin.full_name || admin.username) + "</strong>" : ""}</div>
      </div>
      <div id="adminContent"></div>
    </main>`;

  document.getElementById("adminLogoutBtn").addEventListener("click", async () => {
    const ok = await confirmAction({ title: "Log out?", message: "You'll need to log in again to access the admin panel.", confirmLabel: "Log out" });
    if (ok) Auth.logout("login.html");
  });

  const toggle = document.getElementById("adminMenuToggle");
  if (toggle){
    toggle.addEventListener("click", () => document.getElementById("adminSidebar").classList.toggle("open"));
  }
}
