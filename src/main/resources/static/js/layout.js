/* ============================================================
   layout.js - renders the shared navbar + footer into
   #site-navbar / #site-footer placeholders on every page.
   ============================================================ */
function renderNavbar(activePage){
  const mount = document.getElementById("site-navbar");
  if (!mount) return;
  const loggedIn = Auth.isLoggedIn();
  const user = Auth.getUser();

  const link = (href, label, key) =>
    `<a href="${href}" class="${activePage === key ? 'active' : ''}">${label}</a>`;

  mount.innerHTML = `
    <nav class="navbar">
      <div class="container">
        <a href="index.html" class="brand"><span class="paw">🐾</span> PawHome</a>
        <button class="nav-toggle" id="navToggle" aria-label="Toggle menu">☰</button>
        <ul class="nav-links">
          ${link("index.html", "Home", "home")}
          ${link("browse.html", "Browse Pets", "browse")}
          ${link("index.html#how-it-works", "How It Works", "how")}
          ${link("index.html#about", "About Us", "about")}
          ${link("index.html#contact", "Contact", "contact")}
        </ul>
        <div class="nav-cta">
          ${loggedIn ? `
            <a href="cart.html" class="btn btn-ghost nav-badge">🛒 Cart <span class="cart-count" id="navCartCount" style="display:none;">0</span></a>
            <a href="dashboard.html" class="btn btn-outline btn-sm">Hi, ${escapeHtml((user && user.full_name) || "there").split(" ")[0]}</a>
            <button class="btn btn-primary btn-sm" id="navLogoutBtn">Logout</button>
          ` : `
            <a href="login.html" class="btn btn-outline btn-sm">Log In</a>
            <a href="register.html" class="btn btn-primary btn-sm">Register</a>
          `}
        </div>
      </div>
    </nav>`;

  document.getElementById("navToggle").addEventListener("click", () => {
    document.body.classList.toggle("nav-open");
  });

  const logoutBtn = document.getElementById("navLogoutBtn");
  if (logoutBtn){
    logoutBtn.addEventListener("click", async () => {
      const ok = await confirmAction({ title: "Log out?", message: "You'll need to log in again to see your dashboard.", confirmLabel: "Log out" });
      if (ok) Auth.logout("index.html");
    });
  }

  if (loggedIn) refreshNavCartCount();
}

async function refreshNavCartCount(){
  try{
    const data = await api.get("/cart");
    const badge = document.getElementById("navCartCount");
    if (!badge) return;
    if (data.items.length > 0){
      badge.textContent = data.items.length;
      badge.style.display = "inline-block";
    }
  }catch(e){ /* not fatal on a page load */ }
}

function renderFooter(){
  const mount = document.getElementById("site-footer");
  if (!mount) return;
  mount.innerHTML = `
    <footer id="contact">
      <div class="container">
        <div>
          <a href="index.html" class="brand" style="color:#fff;">🐾 PawHome</a>
          <p style="margin-top:14px; max-width:280px;">A community shelter platform helping dogs, cats, rabbits and birds
          find loving homes across Maharashtra. Every adoption is reviewed by our team.</p>
        </div>
        <div>
          <h4>Explore</h4>
          <ul>
            <li><a href="index.html">Home</a></li>
            <li><a href="browse.html">Browse Pets</a></li>
            <li><a href="index.html#how-it-works">How It Works</a></li>
            <li><a href="index.html#about">About Us</a></li>
          </ul>
        </div>
        <div>
          <h4>Account</h4>
          <ul>
            <li><a href="login.html">Log In</a></li>
            <li><a href="register.html">Register</a></li>
            <li><a href="dashboard.html">Dashboard</a></li>
            <li><a href="admin/login.html">Admin Login</a></li>
          </ul>
        </div>
        <div>
          <h4>Contact</h4>
          <ul>
            <li>hello@pawhome.example</li>
            <li>+91 98765 43210</li>
            <li>Shivaji Nagar, Pune, MH</li>
          </ul>
        </div>
      </div>
      <div class="footer-bottom">© 2026 PawHome Shelter Platform · Built as a student project</div>
    </footer>`;
}

document.addEventListener("DOMContentLoaded", () => {
  renderFooter();
});
