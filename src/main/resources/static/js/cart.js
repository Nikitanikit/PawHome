renderNavbar();

if (!Auth.isLoggedIn()){
  window.location.href = "login.html?redirect=cart.html";
}

const itemsEl = document.getElementById("cartItems");
const rowsEl = document.getElementById("summaryRows");
const totalEl = document.getElementById("summaryTotal");
const checkoutBtn = document.getElementById("checkoutBtn");

async function load(){
  try{
    const data = await api.get("/cart");
    render(data.items, data.total);
  }catch(err){
    itemsEl.innerHTML = `<div class="empty-state"><div class="icon">⚠️</div><p>${escapeHtml(err.message)}</p></div>`;
  }
}

function render(items, total){
  if (items.length === 0){
    itemsEl.innerHTML = `<div class="empty-state"><div class="icon">🛒</div><p>Your cart is empty.</p><a href="browse.html" class="btn btn-outline" style="margin-top:14px;">Browse pets</a></div>`;
    rowsEl.innerHTML = "";
    totalEl.textContent = formatMoney(0);
    checkoutBtn.disabled = true;
    return;
  }

  itemsEl.innerHTML = items.map(p => `
    <div class="cart-item">
      <img src="${p.image_url}" alt="${escapeHtml(p.name)}">
      <div class="info">
        <h3>${escapeHtml(p.name)}</h3>
        <div class="pet-meta">${escapeHtml(p.breed)} · ${p.age_years} yrs · ${escapeHtml(p.location)}</div>
        <div class="pet-fee">${formatMoney(p.adoption_fee)}</div>
      </div>
      <button class="btn btn-outline btn-sm remove-btn" data-pet="${p.id}" data-name="${escapeHtml(p.name)}">Remove</button>
    </div>`).join("");

  rowsEl.innerHTML = items.map(p => `
    <div class="summary-row"><span>${escapeHtml(p.name)}</span><span>${formatMoney(p.adoption_fee)}</span></div>
  `).join("");
  totalEl.textContent = formatMoney(total);
  checkoutBtn.disabled = false;

  document.querySelectorAll(".remove-btn").forEach(btn => {
    btn.addEventListener("click", async () => {
      const ok = await confirmAction({
        title: "Remove from cart?",
        message: `Remove ${btn.dataset.name} from your cart?`,
        confirmLabel: "Remove"
      });
      if (!ok) return;
      try{
        await api.del(`/cart/${btn.dataset.pet}`);
        toast(`${btn.dataset.name} removed from cart.`, "success");
        refreshNavCartCount();
        load();
      }catch(err){ toast(err.message, "error"); }
    });
  });
}

checkoutBtn.addEventListener("click", () => {
  window.location.href = "checkout.html";
});

load();
