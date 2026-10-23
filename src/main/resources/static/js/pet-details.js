renderNavbar("browse");

const petId = new URLSearchParams(window.location.search).get("id");
const container = document.getElementById("detailsContainer");

async function load(){
  if (!petId){
    container.innerHTML = emptyState("This pet link looks incomplete.");
    return;
  }
  try{
    const { pet } = await api.get(`/pets/${petId}`);
    document.title = `${pet.name} — PawHome`;
    container.innerHTML = render(pet);
    attachEvents(pet);
  }catch(err){
    container.innerHTML = emptyState(err.message);
  }
}

function emptyState(msg){
  return `<div class="empty-state"><div class="icon">🐾</div><p>${escapeHtml(msg)}</p></div>`;
}

function render(p){
  return `
  <div class="details-wrap">
    <div class="details-img"><img src="${p.image_url}" alt="${escapeHtml(p.name)}"></div>
    <div>
      <span class="pet-status ${p.status}" style="position:static; display:inline-block; margin-bottom:10px;">${p.status}</span>
      <h1 style="font-size:38px;">${escapeHtml(p.name)}</h1>
      <p style="font-size:17px;">${escapeHtml(p.description)}</p>

      <div class="spec-grid">
        <div><span>Breed</span><strong>${escapeHtml(p.breed)}</strong></div>
        <div><span>Age</span><strong>${p.age_years} years</strong></div>
        <div><span>Gender</span><strong>${escapeHtml(p.gender)}</strong></div>
        <div><span>Color</span><strong>${escapeHtml(p.color)}</strong></div>
        <div><span>Location</span><strong>${escapeHtml(p.location)}</strong></div>
        <div><span>Vaccination</span><strong>${escapeHtml(p.vaccination_status)}</strong></div>
        <div><span>Health status</span><strong>${escapeHtml(p.health_status)}</strong></div>
        <div><span>Adoption fee</span><strong>${formatMoney(p.adoption_fee)}</strong></div>
      </div>

      <h3 style="font-size:18px;">Personality</h3>
      <p>${escapeHtml(p.personality)}</p>
      <h3 style="font-size:18px;">Adoption requirements</h3>
      <p>${escapeHtml(p.adoption_requirements)}</p>

      <div class="details-actions">
        <button class="btn btn-primary" id="addCartBtn" ${p.status !== "available" ? "disabled" : ""}>
          ${p.status === "available" ? "Add to Cart" : "Not available"}
        </button>
        <button class="btn btn-outline" id="favBtn">♡ Save to favorites</button>
      </div>
      <p class="hint" style="margin-top:14px; font-size:13px;">
        Adding to cart reserves this pet and starts the adoption-fee payment. You'll then complete a short application.
      </p>
    </div>
  </div>`;
}

function attachEvents(pet){
  const cartBtn = document.getElementById("addCartBtn");
  const favBtn = document.getElementById("favBtn");

  cartBtn.addEventListener("click", async () => {
    if (!Auth.isLoggedIn()){
      toast("Please log in to adopt this pet.", "error");
      window.location.href = "login.html";
      return;
    }
    setLoading(cartBtn, true, "Adding...");
    try{
      await api.post("/cart", { pet_id: pet.id });
      toast(`${pet.name} added to your cart.`, "success");
      refreshNavCartCount();
    }catch(err){
      toast(err.message, "error");
    }finally{
      setLoading(cartBtn, false);
    }
  });

  favBtn.addEventListener("click", async () => {
    if (!Auth.isLoggedIn()){
      toast("Please log in to save favorites.", "error");
      window.location.href = "login.html";
      return;
    }
    try{
      await api.post("/favorites", { pet_id: pet.id });
      favBtn.innerHTML = "♥ Saved to favorites";
      toast("Saved to favorites.", "success");
    }catch(err){ toast(err.message, "error"); }
  });
}

load();
