renderNavbar("browse");

let favoriteIds = new Set();

function petCardHTML(p){
  const disabled = p.status !== "available" ? "disabled" : "";
  return `
  <div class="pet-card">
    <a href="pet-details.html?id=${p.id}" class="thumb">
      <img src="${p.image_url}" alt="${escapeHtml(p.name)}">
      <span class="pet-status ${p.status}">${p.status}</span>
    </a>
    <button class="fav-btn ${favoriteIds.has(p.id) ? 'active' : ''}" data-pet="${p.id}" title="Save to favorites">${favoriteIds.has(p.id) ? '♥' : '♡'}</button>
    <div class="body">
      <h3><a href="pet-details.html?id=${p.id}" style="color:inherit;">${escapeHtml(p.name)}</a></h3>
      <div class="pet-meta">${escapeHtml(p.species)} · ${escapeHtml(p.breed)} · ${p.age_years} yrs</div>
      <div class="pet-tags">
        <span class="tag">${escapeHtml(p.gender)}</span>
        <span class="tag">${escapeHtml(p.location)}</span>
      </div>
      <div class="pet-fee">${formatMoney(p.adoption_fee)} adoption fee</div>
      <div class="row-actions">
        <a href="pet-details.html?id=${p.id}" class="btn btn-outline btn-sm" style="flex:1;">Details</a>
        <button class="btn btn-primary btn-sm add-cart-btn" style="flex:1;" data-pet="${p.id}" data-name="${escapeHtml(p.name)}" ${disabled}>
          ${p.status === "available" ? "Add to Cart" : "Unavailable"}
        </button>
      </div>
    </div>
  </div>`;
}

async function loadFavorites(){
  if (!Auth.isLoggedIn()) return;
  try{
    const data = await api.get("/favorites");
    favoriteIds = new Set(data.favorites.map(f => f.id));
  }catch(e){ /* non fatal */ }
}

async function loadSpecies(){
  try{
    const data = await api.get("/pets/meta/species");
    const sel = document.getElementById("species");
    data.species.forEach(s => {
      const opt = document.createElement("option");
      opt.value = s; opt.textContent = s;
      sel.appendChild(opt);
    });
  }catch(e){}
}

async function loadPets(){
  const grid = document.getElementById("petGrid");
  const countEl = document.getElementById("resultCount");
  grid.innerHTML = `<div class="loading" style="grid-column:1/-1;"><span class="spinner"></span> Loading pets...</div>`;

  const params = new URLSearchParams();
  const search = document.getElementById("search").value.trim();
  const species = document.getElementById("species").value;
  const status = document.getElementById("status").value;
  const location = document.getElementById("location").value.trim();
  if (search) params.set("search", search);
  if (species) params.set("species", species);
  if (status) params.set("status", status);
  if (location) params.set("location", location);

  try{
    const data = await api.get("/pets?" + params.toString());
    countEl.textContent = `${data.count} pet${data.count === 1 ? "" : "s"} found`;
    if (data.pets.length === 0){
      grid.innerHTML = `<div class="empty-state" style="grid-column:1/-1;"><div class="icon">🔍</div><p>No pets match your search. Try clearing a filter.</p></div>`;
      return;
    }
    grid.innerHTML = data.pets.map(petCardHTML).join("");
    attachCardEvents();
  }catch(err){
    grid.innerHTML = `<div class="empty-state" style="grid-column:1/-1;"><div class="icon">⚠️</div><p>${escapeHtml(err.message)}</p></div>`;
  }
}

function attachCardEvents(){
  document.querySelectorAll(".add-cart-btn").forEach(btn => {
    btn.addEventListener("click", async () => {
      if (!Auth.isLoggedIn()){
        toast("Please log in to add a pet to your cart.", "error");
        window.location.href = "login.html";
        return;
      }
      setLoading(btn, true, "Adding...");
      try{
        await api.post("/cart", { pet_id: Number(btn.dataset.pet) });
        toast(`${btn.dataset.name} added to your cart.`, "success");
        refreshNavCartCount();
      }catch(err){
        toast(err.message, "error");
      }finally{
        setLoading(btn, false);
      }
    });
  });

  document.querySelectorAll(".fav-btn").forEach(btn => {
    btn.addEventListener("click", async () => {
      if (!Auth.isLoggedIn()){
        toast("Please log in to save favorites.", "error");
        window.location.href = "login.html";
        return;
      }
      const petId = Number(btn.dataset.pet);
      try{
        if (favoriteIds.has(petId)){
          await api.del(`/favorites/${petId}`);
          favoriteIds.delete(petId);
          btn.classList.remove("active"); btn.textContent = "♡";
        } else {
          await api.post("/favorites", { pet_id: petId });
          favoriteIds.add(petId);
          btn.classList.add("active"); btn.textContent = "♥";
        }
      }catch(err){ toast(err.message, "error"); }
    });
  });
}

document.getElementById("filterForm").addEventListener("submit", (e) => {
  e.preventDefault();
  loadPets();
});

(async function init(){
  await loadFavorites();
  await loadSpecies();
  await loadPets();
})();
