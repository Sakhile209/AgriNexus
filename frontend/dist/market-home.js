const money = new Intl.NumberFormat('en-ZA', { style: 'currency', currency: 'ZAR' })

async function getJson(path) {
  const response = await fetch(path)
  if (!response.ok) throw new Error(`Request failed (${response.status})`)
  return response.json()
}

function card(content) {
  const article = document.createElement('article')
  article.className = 'live-market-card'
  article.innerHTML = content
  return article
}

async function mountMarketHome() {
  const landing = document.querySelector('.landing main')
  const platform = document.querySelector('.landing .platform')
  if (!landing || !platform || document.querySelector('#live-markets')) return false

  const section = document.createElement('section')
  section.id = 'live-markets'
  section.className = 'live-markets'
  section.innerHTML = `
    <p class="section-kicker">LIVE MARKET INTELLIGENCE</p>
    <h2>Prices and produce available now</h2>
    <p class="section-lead">API-backed updates from AgriNexus market contributors and sellers.</p>
    <div class="live-market-columns">
      <div><div class="live-market-heading"><h3>Market prices</h3><span id="price-status">Updating…</span></div><div id="market-price-list" class="live-market-grid"></div></div>
      <div><div class="live-market-heading"><h3>Marketplace</h3><span id="listing-status">Loading…</span></div><div id="marketplace-list" class="live-market-grid"></div></div>
    </div>`
  landing.insertBefore(section, platform)

  const story = document.createElement('section')
  story.className = 'home-story'
  story.innerHTML = `<div class="home-story-copy"><p class="section-kicker">BUILT FOR PRACTICAL FARMING</p><div><h2>One connected place for the work that keeps a farm moving.</h2><p>AgriNexus brings livestock, crop, weather, price and marketplace information together so farmers can act with clarity and confidence.</p></div></div><div class="home-story-stats"><article><strong>One</strong><span>clear record for farm operations and activity.</span></article><article><strong>Live</strong><span>weather and market information when decisions matter.</span></article><article><strong>100%</strong><span>designed to grow from simple records into smart agriculture.</span></article></div>`
  landing.insertBefore(story, section)

  const cta = document.createElement('section')
  cta.className = 'home-cta'
  cta.innerHTML = `<h2>Ready to grow with better information?</h2><p>Start with the tools you already have. Add more as your farm grows.</p><button class="primary" type="button">Create your account →</button>`
  cta.querySelector('button').addEventListener('click', () => document.querySelector('.landing-actions .primary')?.click())
  landing.append(cta)

  try {
    const prices = await getJson('/api/v1/market-prices')
    const target = section.querySelector('#market-price-list')
    prices.slice(0, 4).forEach(item => target.append(card(`<span>${item.market} · ${item.province}</span><h4>${item.commodity}</h4><strong>${money.format(item.priceZar)} <small>/ ${item.unit}</small></strong><em>${item.source}</em>`)))
    section.querySelector('#price-status').textContent = prices.length ? `Updated ${new Date(prices[0].capturedAt).toLocaleString('en-ZA')}` : 'No prices yet'
  } catch (error) {
    section.querySelector('#price-status').textContent = error.message
  }

  try {
    const listings = await getJson('/api/v1/marketplace/listings')
    const target = section.querySelector('#marketplace-list')
    listings.slice(0, 4).forEach(item => target.append(card(`<span>${item.category} · ${item.location}</span><h4>${item.title}</h4><strong>${money.format(item.priceZar)} <small>/ ${item.unit}</small></strong><em>${item.quantity} ${item.unit} available</em>`)))
    section.querySelector('#listing-status').textContent = `${listings.length} active listing${listings.length === 1 ? '' : 's'}`
  } catch (error) {
    section.querySelector('#listing-status').textContent = error.message
  }
  return true
}

if (!await mountMarketHome()) {
  const observer = new MutationObserver(async () => { if (await mountMarketHome()) observer.disconnect() })
  observer.observe(document.body, { childList: true, subtree: true })
}
