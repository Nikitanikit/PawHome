renderNavbar();

if (!Auth.isLoggedIn()){
  window.location.href = "login.html?redirect=checkout.html";
}

const body = document.getElementById("checkoutBody");
let selectedMethod = "Card";

async function load(){
  try{
    const data = await api.get("/cart");
    if (data.items.length === 0){
      body.innerHTML = `<div class="empty-state"><div class="icon">🛒</div><p>Your cart is empty.</p><a href="browse.html" class="btn btn-outline" style="margin-top:14px;">Browse pets</a></div>`;
      return;
    }
    renderForm(data.items, data.total);
  }catch(err){
    body.innerHTML = `<div class="empty-state"><div class="icon">⚠️</div><p>${escapeHtml(err.message)}</p></div>`;
  }
}

function renderForm(items, total){
  body.innerHTML = `
    <div class="form-card" style="max-width:none; padding:28px;">
      <h3 style="font-size:16px;">Order (${items.length} pet${items.length === 1 ? "" : "s"})</h3>
      ${items.map(p => `<div class="summary-row"><span>${escapeHtml(p.name)}</span><span>${formatMoney(p.adoption_fee)}</span></div>`).join("")}
      <div class="summary-total"><span>Total</span><span>${formatMoney(total)}</span></div>

      <h3 style="font-size:16px; margin-top:24px;">Payment method</h3>
      <div class="pay-methods">
        <div class="pay-method active" data-method="Card">💳 Card</div>
        <div class="pay-method" data-method="UPI">📱 UPI</div>
        <div class="pay-method" data-method="Net Banking">🏦 Net Banking</div>
      </div>

      <div id="cardFields">
        <div class="field-grid-2">
          <div class="field"><label>Card number</label><input type="text" id="cardNumber" placeholder="4242 4242 4242 4242" maxlength="19"></div>
          <div class="field"><label>Name on card</label><input type="text" id="cardName" placeholder="As on card"></div>
        </div>
        <div class="field-grid-2">
          <div class="field"><label>Expiry</label><input type="text" id="cardExpiry" placeholder="MM/YY" maxlength="5"></div>
          <div class="field"><label>CVV</label><input type="text" id="cardCvv" placeholder="123" maxlength="3"></div>
        </div>
      </div>

      <div class="alert alert-error" id="payError"></div>
      <button class="btn btn-primary btn-block" id="payBtn">Pay ${formatMoney(total)}</button>
      <p class="hint" style="margin-top:10px; text-align:center;">This is a demo checkout — no real payment network is contacted.</p>
    </div>`;

  document.querySelectorAll(".pay-method").forEach(el => {
    el.addEventListener("click", () => {
      document.querySelectorAll(".pay-method").forEach(x => x.classList.remove("active"));
      el.classList.add("active");
      selectedMethod = el.dataset.method;
      document.getElementById("cardFields").style.display = selectedMethod === "Card" ? "block" : "none";
    });
  });

  document.getElementById("payBtn").addEventListener("click", () => pay(items));
}

async function pay(items){
  const payBtn = document.getElementById("payBtn");
  const errorEl = document.getElementById("payError");
  errorEl.classList.remove("show");

  if (selectedMethod === "Card"){
    const num = document.getElementById("cardNumber").value.replace(/\s/g, "");
    const expiry = document.getElementById("cardExpiry").value;
    const cvv = document.getElementById("cardCvv").value;
    if (num.length < 12 || !/^\d{2}\/\d{2}$/.test(expiry) || cvv.length < 3){
      errorEl.textContent = "Please enter valid card details.";
      errorEl.classList.add("show");
      return;
    }
  }

  setLoading(payBtn, true, "Processing payment...");
  try{
    const data = await api.post("/checkout", { payment_method: selectedMethod });
    refreshNavCartCount();
    renderReceipt(data.receipts);
  }catch(err){
    errorEl.textContent = err.message;
    errorEl.classList.add("show");
  }finally{
    setLoading(payBtn, false);
  }
}

function renderReceipt(receipts){
  document.getElementById("checkoutIntro").textContent = "Payment successful — finish an application for each pet below.";
  body.innerHTML = `
    <div class="alert alert-success show" style="margin-top:20px;">Payment successful! Please complete an adoption application to confirm each adoption.</div>
    ${receipts.map(r => `
      <div class="receipt-card">
        <strong>${escapeHtml(r.pet_name)}</strong>
        <p style="margin:6px 0 12px;">Paid ${formatMoney(r.amount)} · Transaction ${escapeHtml(r.transaction_id)}</p>
        <a href="apply.html?pet_id=${r.pet_id}" class="btn btn-primary btn-sm">Complete adoption application</a>
      </div>
    `).join("")}
    <a href="dashboard.html" class="btn btn-outline" style="margin-top:10px;">Go to dashboard</a>
  `;
}

load();
