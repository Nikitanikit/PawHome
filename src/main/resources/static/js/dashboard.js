renderNavbar();

if (!Auth.isLoggedIn()){
  window.location.href = "login.html?redirect=dashboard.html";
}

const user = Auth.getUser();
document.getElementById("welcomeMsg").textContent =
  `Welcome back, ${user ? user.full_name.split(" ")[0] : "friend"}. Track your favorites and adoption applications here.`;

document.querySelectorAll(".tab").forEach(tab => {
  tab.addEventListener("click", () => {
    document.querySelectorAll(".tab").forEach(t => t.classList.remove("active"));
    tab.classList.add("active");
    ["applications", "favorites", "profile"].forEach(key => {
      document.getElementById(key + "Tab").style.display = key === tab.dataset.tab ? "block" : "none";
    });
  });
});

async function loadApplications(){
  const el = document.getElementById("applicationsTab");
  el.innerHTML = `<div class="loading"><span class="spinner"></span> Loading applications...</div>`;
  try{
    const { applications } = await api.get("/applications/mine");
    if (applications.length === 0){
      el.innerHTML = `<div class="empty-state"><div class="icon">📋</div><p>You haven't applied to adopt a pet yet.</p><a href="browse.html" class="btn btn-outline" style="margin-top:12px;">Browse pets</a></div>`;
      return;
    }
    el.innerHTML = applications.map(a => `
      <div class="app-card">
        <img src="${a.pet_image}" alt="${escapeHtml(a.pet_name)}">
        <div class="info">
          <strong>${escapeHtml(a.pet_name)}</strong>
          <div class="pet-meta">Applied ${escapeHtml((a.applied_at || "").split(" ")[0])}</div>
          ${a.admin_note ? `<div class="pet-meta">Note: ${escapeHtml(a.admin_note)}</div>` : ""}
        </div>
        <span class="pill ${a.status}">${a.status}</span>
      </div>`).join("");
  }catch(err){
    el.innerHTML = `<div class="empty-state"><p>${escapeHtml(err.message)}</p></div>`;
  }
}

async function loadFavorites(){
  const el = document.getElementById("favoritesTab");
  el.innerHTML = `<div class="loading"><span class="spinner"></span> Loading favorites...</div>`;
  try{
    const { favorites } = await api.get("/favorites");
    if (favorites.length === 0){
      el.innerHTML = `<div class="empty-state"><div class="icon">♡</div><p>No favorites saved yet.</p><a href="browse.html" class="btn btn-outline" style="margin-top:12px;">Browse pets</a></div>`;
      return;
    }
    el.innerHTML = `<div class="pet-grid">` + favorites.map(p => `
      <div class="pet-card">
        <a href="pet-details.html?id=${p.id}" class="thumb">
          <img src="${p.image_url}" alt="${escapeHtml(p.name)}">
          <span class="pet-status ${p.status}">${p.status}</span>
        </a>
        <div class="body">
          <h3>${escapeHtml(p.name)}</h3>
          <div class="pet-meta">${escapeHtml(p.breed)} · ${p.age_years} yrs</div>
          <div class="row-actions">
            <a href="pet-details.html?id=${p.id}" class="btn btn-outline btn-sm btn-block">View</a>
            <button class="btn btn-danger btn-sm unfav-btn" data-pet="${p.id}">Remove</button>
          </div>
        </div>
      </div>`).join("") + `</div>`;

    document.querySelectorAll(".unfav-btn").forEach(btn => {
      btn.addEventListener("click", async () => {
        try{
          await api.del(`/favorites/${btn.dataset.pet}`);
          toast("Removed from favorites.", "success");
          loadFavorites();
        }catch(err){ toast(err.message, "error"); }
      });
    });
  }catch(err){
    el.innerHTML = `<div class="empty-state"><p>${escapeHtml(err.message)}</p></div>`;
  }
}

async function loadProfile(){
  const el = document.getElementById("profileTab");
  el.innerHTML = `<div class="loading"><span class="spinner"></span> Loading profile...</div>`;
  try{
    const { user } = await api.get("/auth/me");
    el.innerHTML = `
      <div class="profile-grid">
        <div><span>Full name</span><strong>${escapeHtml(user.full_name)}</strong></div>
        <div><span>Email</span><strong>${escapeHtml(user.email)}</strong></div>
        <div><span>Phone</span><strong>${escapeHtml(user.phone)}</strong></div>
        <div><span>Address</span><strong>${escapeHtml(user.address)}</strong></div>
        <div><span>Member since</span><strong>${escapeHtml((user.created_at || "").split(" ")[0])}</strong></div>
      </div>
      <p class="hint" style="margin-top:14px;">To change your password, use <a href="forgot-password.html">Forgot password</a>.</p>`;
  }catch(err){
    el.innerHTML = `<div class="empty-state"><p>${escapeHtml(err.message)}</p></div>`;
  }
}

loadApplications();
loadFavorites();
loadProfile();
