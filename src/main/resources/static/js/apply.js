renderNavbar();

if (!Auth.isLoggedIn()){
  window.location.href = "login.html?redirect=" + encodeURIComponent(window.location.pathname + window.location.search);
}

const petId = new URLSearchParams(window.location.search).get("pet_id");
const petStrip = document.getElementById("petStrip");
const form = document.getElementById("applyForm");
const errorAlert = document.getElementById("formError");
const submitBtn = document.getElementById("submitBtn");
let pet = null;

async function init(){
  if (!petId){
    petStrip.innerHTML = `<div class="empty-state"><p>No pet selected. Please choose a pet from the browse page.</p><a href="browse.html" class="btn btn-outline">Browse pets</a></div>`;
    form.style.display = "none";
    return;
  }
  try{
    const data = await api.get(`/pets/${petId}`);
    pet = data.pet;
    petStrip.innerHTML = `
      <div class="pet-strip">
        <img src="${pet.image_url}" alt="${escapeHtml(pet.name)}">
        <div>
          <strong>${escapeHtml(pet.name)}</strong>
          <div class="pet-meta">${escapeHtml(pet.breed)} · ${pet.age_years} yrs</div>
        </div>
      </div>`;
    const user = Auth.getUser();
    if (user){
      document.getElementById("full_name").value = user.full_name || "";
      document.getElementById("email").value = user.email || "";
      document.getElementById("phone").value = user.phone || "";
      document.getElementById("address").value = user.address || "";
    }
  }catch(err){
    petStrip.innerHTML = `<div class="empty-state"><p>${escapeHtml(err.message)}</p></div>`;
    form.style.display = "none";
  }
}

function setErr(id, hasError){
  document.getElementById("f-" + id).classList.toggle("error", hasError);
}

form.addEventListener("submit", async (e) => {
  e.preventDefault();
  errorAlert.classList.remove("show");

  const vals = {
    pet_id: Number(petId),
    full_name: document.getElementById("full_name").value.trim(),
    email: document.getElementById("email").value.trim(),
    phone: document.getElementById("phone").value.trim(),
    address: document.getElementById("address").value.trim(),
    housing_type: document.getElementById("housing_type").value,
    previous_pet_experience: document.getElementById("previous_pet_experience").value.trim(),
    reason_for_adoption: document.getElementById("reason_for_adoption").value.trim(),
    family_members: document.getElementById("family_members").value,
    employment_info: document.getElementById("employment_info").value.trim(),
    agreement: document.getElementById("agreement").checked,
  };

  let ok = true;
  ["full_name","email","phone","address","housing_type","previous_pet_experience",
   "reason_for_adoption","family_members","employment_info"].forEach(f => {
    const bad = !String(vals[f]).trim();
    setErr(f, bad);
    if (bad) ok = false;
  });
  setErr("agreement", !vals.agreement);
  if (!vals.agreement) ok = false;

  if (!ok){
    errorAlert.textContent = "Please complete all fields and accept the agreement.";
    errorAlert.classList.add("show");
    return;
  }

  setLoading(submitBtn, true, "Submitting...");
  try{
    await api.post("/applications", vals);
    form.style.display = "none";
    document.getElementById("successView").style.display = "block";
  }catch(err){
    errorAlert.textContent = err.message;
    errorAlert.classList.add("show");
  }finally{
    setLoading(submitBtn, false);
  }
});

init();
