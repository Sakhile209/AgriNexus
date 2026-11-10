import {watchLiveLocation,locationError} from './geolocation.js'
export const provinces = ['Eastern Cape','Free State','Gauteng','KwaZulu-Natal','Limpopo','Mpumalanga','North West','Northern Cape','Western Cape']
export const escapeHtml = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]))
export const money = value => value == null ? 'Not available' : new Intl.NumberFormat('en-ZA',{style:'currency',currency:'ZAR'}).format(value)
export const display = (v, unit='') => v == null ? 'Not available' : `${v}${unit}`
export const timestamp = v => v ? new Date(v).toLocaleString('en-ZA') : 'No successful update'
export function refreshDelay(next, minimum=60000) { return Math.max(minimum, Math.min(3600000, new Date(next || Date.now()+1800000).getTime()-Date.now())) }
export async function api(path, token, options={}) {
 const response = await fetch(`/api/v1${path}`, {...options,headers:{...(token?{Authorization:`Bearer ${token}`} : {}),...(options.body && !(options.body instanceof FormData)?{'Content-Type':'application/json'}:{}),...options.headers}})
 if (!response.ok) {const error=await response.json().catch(()=>({})); throw new Error(error.message || `Request failed (${response.status}). Please retry.`)}
 return response.status===204 ? null : response.json()
}
export function coordinatePair(farm) {return farm?.latitude!=null && farm?.longitude!=null && Number.isFinite(Number(farm.latitude)) && Number.isFinite(Number(farm.longitude)) && Math.abs(Number(farm.latitude))<=90 && Math.abs(Number(farm.longitude))<=180}

export function preferredMapLayer(satelliteAvailable, preference) {
 return satelliteAvailable && preference!=='standard' ? 'satellite' : 'standard'
}

export function mountFarmContext(workspace, farmId, token) {
 const controller=new AbortController(); let timer, map, stopped=false, generation=0, selectedFarm, mapLibrary, liveMarker, latestFix, stopGps=()=>{}, tracking=false, mapReady=Promise.resolve()
 const pane=workspace.querySelector('.farm-map-pane'), detail=workspace.querySelector('#live-weather-detail')
 const e=escapeHtml
 const message=(text)=>{let box=pane.querySelector('.live-map-message');if(!box){box=document.createElement('p');box.className='live-map-message';pane.append(box)}box.textContent=text}
 async function drawMap(farm,deviceOnly=false) {
  if(!coordinatePair(farm)){message('Save this farm’s GPS coordinates in Settings to display its location.');return}
  try {
   const config=await api(`/farms/${farmId}/map-config`,token,{signal:controller.signal})
   if(stopped)return
   const maplibregl=await import('/vendor/maplibre-gl.mjs'); if(stopped)return;mapLibrary=maplibregl
   const standard={version:8,sources:{farm:{type:'raster',tiles:[config.standardTiles],tileSize:256,attribution:'© <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener">OpenStreetMap contributors</a>',maxzoom:19}},layers:[{id:'farm',type:'raster',source:'farm'}]}
   const styleFor=layer=>{
    const style=structuredClone(standard)
    if(layer==='satellite'&&config.satelliteAvailable){style.sources.farm.tiles=[location.origin+config.satelliteTiles];style.sources.farm.attribution='© <a href="https://www.maptiler.com/copyright/" target="_blank" rel="noopener">MapTiler</a> © Airbus DS © OpenStreetMap contributors';style.sources.farm.maxzoom=20}
    return style
   }
   let preference
   try{preference=localStorage.getItem('agrinexus.map-layer')}catch{}
   const initialLayer=preferredMapLayer(config.satelliteAvailable,preference)
   const container=pane.querySelector('.live-farm-map')
   map=new maplibregl.Map({container,style:styleFor(initialLayer),center:[Number(farm.longitude),Number(farm.latitude)],zoom:13,transformRequest:url=>url.startsWith(location.origin+'/api/')?{url,headers:{Authorization:`Bearer ${token}`}}:{url}})
   map.addControl(new maplibregl.NavigationControl(),'bottom-right')
   map.addControl(new maplibregl.FullscreenControl(),'bottom-left')
   const info=document.createElement('div');info.textContent=`${farm.name} · ${farm.municipality || ''}, ${farm.province} · ${farm.latitude}, ${farm.longitude}`
   if(!deviceOnly)new maplibregl.Marker({color:'#16a45a'}).setLngLat([Number(farm.longitude),Number(farm.latitude)]).setPopup(new maplibregl.Popup().setDOMContent(info)).addTo(map)
   map.on('error',()=>message('Some map tiles could not load. Check your connection or try the standard layer.'))
   const select=pane.querySelector('.map-layer')
   select.querySelector('[value="satellite"]').disabled=!config.satelliteAvailable
   select.title=config.satelliteAvailable?'Choose map imagery':'Satellite requires a licensed MapTiler key configured on the server'
   select.value=initialLayer
   select.onchange=()=>{
    try{localStorage.setItem('agrinexus.map-layer',select.value)}catch{}
    pane.querySelector('.live-map-message')?.remove();map.setStyle(styleFor(select.value))
   }
   pane.querySelector('.map-recenter').disabled=deviceOnly
   pane.querySelector('.map-recenter').onclick=()=>{if(coordinatePair(selectedFarm))map.flyTo({center:[Number(selectedFarm.longitude),Number(selectedFarm.latitude)],zoom:13})}
  } catch(error) {if(!stopped)message(error.message)}
 }
 async function weather() {
  const version=++generation
  detail.setAttribute('aria-busy','true')
  try {
   const w=await api(`/farms/${farmId}/weather`,token,{signal:controller.signal})
   if(stopped||version!==generation)return
   workspace.querySelector('#map-temperature').textContent=w.available?display(w.temperatureC,'°C'):'Weather unavailable'
   workspace.querySelector('#map-wind').textContent=w.available?`${display(w.windSpeedKph,' km/h')} · ${w.status==='stale'?'Cached / outdated':'Updated'} ${timestamp(w.retrievedAt)}`:w.message
   workspace.querySelector('#weather-summary').textContent=w.available?`${w.location} · ${display(w.temperatureC,'°C')} · ${display(w.rainProbability,'% rain')}`:w.message
   detail.innerHTML=`<div class="live-heading"><strong>Farm weather</strong><button type="button" class="weather-refresh">Refresh</button></div><p>${e(w.location)} · <a href="https://open-meteo.com/" target="_blank" rel="noopener">Open-Meteo</a></p>${w.message?`<p class="notice">${e(w.message)}</p>`:''}${w.available?`<dl class="weather-grid"><div><dt>Temperature</dt><dd>${display(w.temperatureC,'°C')}</dd></div><div><dt>Humidity</dt><dd>${display(w.humidityPercent,'%')}</dd></div><div><dt>Wind</dt><dd>${display(w.windSpeedKph,' km/h')} · ${display(w.windDirectionDegrees,'°')}</dd></div><div><dt>Current precipitation</dt><dd>${display(w.currentRainfallMm,' mm')}</dd></div><div><dt>Today’s precipitation</dt><dd>${display(w.rainfallMm,' mm')}</dd></div><div><dt>Rain probability</dt><dd>${display(w.rainProbability,'%')}</dd></div></dl><small>${w.status==='stale'?'Cached / outdated':'Provider forecast'} · Observation ${e(w.observedAt)} SAST<br>Last successful retrieval: ${e(timestamp(w.retrievedAt))}</small><details><summary>7-day forecast</summary><div class="forecast-scroll"><table><thead><tr><th>Date</th><th>Min / max °C</th><th>Rain mm</th><th>Probability</th></tr></thead><tbody>${w.forecast.map(d=>`<tr><td>${e(d.date)}</td><td>${display(d.minimumC)} / ${display(d.maximumC)}</td><td>${display(d.rainfallMm)}</td><td>${display(d.rainProbability,'%')}</td></tr>`).join('')}</tbody></table></div></details><div class="advisories"><strong>AgriNexus agricultural advisories</strong>${w.advisories.length?w.advisories.map(a=>`<p>${e(a.message)}</p>`).join(''):'<p>No advisory thresholds met in the available forecast.</p>'}<small>Generated from forecast thresholds. Official severe-weather warnings are not supplied by this provider. <a href="https://www.weathersa.co.za/" target="_blank" rel="noopener">Check SAWS warnings</a>.</small></div>`:''}<small>Next check: ${e(w.nextRefreshAt?timestamp(w.nextRefreshAt):'after coordinates are saved')}</small>`
   detail.querySelector('.weather-refresh').onclick=()=>{clearTimeout(timer);weather()}
   if(w.nextRefreshAt) timer=setTimeout(weather,refreshDelay(w.nextRefreshAt))
  } catch(error) {if(!stopped) {detail.textContent=`Weather unavailable: ${error.message}`;const retry=document.createElement('button');retry.textContent='Retry';retry.onclick=weather;detail.append(retry);timer=setTimeout(weather,300000)}}
  finally {if(!stopped)detail.removeAttribute('aria-busy')}
 }
 const gpsButton=pane.querySelector('.map-live-location'),gpsPanel=pane.querySelector('.live-location-panel'),gpsStatus=pane.querySelector('.live-location-status'),saveGps=pane.querySelector('.save-gps-farm')
 function stopTracking(){tracking=false;stopGps();liveMarker?.remove();liveMarker=null;latestFix=null;gpsButton.textContent='Use live location';saveGps.disabled=true}
 gpsButton.addEventListener('click',()=>{
  gpsPanel.hidden=false
  if(tracking){stopTracking();gpsStatus.textContent='Live location stopped. Your saved farm location is unchanged.';return}
  tracking=true;gpsButton.textContent='Stop live location';gpsStatus.textContent='Waiting for your device location. Allow location access when your browser asks.'
  stopGps=watchLiveLocation(async fix=>{
   latestFix=fix
   gpsStatus.textContent=`Live device GPS: ${fix.latitude.toFixed(6)}, ${fix.longitude.toFixed(6)} · accuracy ±${Math.round(fix.accuracy)} m · updated ${timestamp(fix.timestamp)}. Save this as the farm location only when you are at the farm.`
   saveGps.disabled=false
   await mapReady
   if(stopped||!tracking)return
   if(!map){mapReady=drawMap({...selectedFarm,latitude:fix.latitude,longitude:fix.longitude},true);await mapReady}
   if(stopped||!tracking||!map||!latestFix)return
   if(!liveMarker){const element=document.createElement('div');element.className='live-device-marker';element.setAttribute('aria-label','Your live device location');liveMarker=new mapLibrary.Marker({element}).setPopup(new mapLibrary.Popup().setText('Your live device location')).setLngLat([latestFix.longitude,latestFix.latitude]).addTo(map)}
   liveMarker.setLngLat([latestFix.longitude,latestFix.latitude]);map.flyTo({center:[latestFix.longitude,latestFix.latitude],zoom:Math.max(map.getZoom(),15)})
  },error=>{if(stopped)return;if(error?.code===2||error?.code===3){latestFix=null;saveGps.disabled=true;gpsStatus.textContent=locationError(error)+' Waiting for another live fix.'}else{stopTracking();gpsStatus.textContent=locationError(error)}})
 },{signal:controller.signal})
 saveGps.addEventListener('click',async()=>{
  if(!latestFix||!selectedFarm||stopped)return
  const fix={...latestFix};saveGps.disabled=true
  try{
   // Fetch latest details immediately before PATCH so profile edits are preserved.
   const current=await api(`/farms/${farmId}`,token,{signal:controller.signal})
   const body=Object.fromEntries(['name','farmType','province','municipality','sizeValue','sizeUnit','mainActivities'].map(k=>[k,current[k]??null]))
   body.latitude=Number(fix.latitude.toFixed(6));body.longitude=Number(fix.longitude.toFixed(6))
   const updated=await api(`/farms/${farmId}`,token,{method:'PATCH',body:JSON.stringify(body),signal:controller.signal})
   if(stopped)return
   selectedFarm=updated;map?.remove();map=null;liveMarker=null;mapReady=drawMap(updated);await mapReady
   if(stopped)return
   gpsStatus.textContent=`Farm GPS saved: ${updated.latitude}, ${updated.longitude}. Farm weather now uses this location.`
   clearTimeout(timer);weather()
  }catch(error){if(!stopped)gpsStatus.textContent=`Could not save farm location: ${error.message}`}
  finally{if(!stopped)saveGps.disabled=!latestFix}
 },{signal:controller.signal})
 api(`/farms/${farmId}`,token,{signal:controller.signal}).then(f=>{if(!stopped){selectedFarm=f;gpsButton.disabled=false;mapReady=drawMap(f)}}).catch(error=>{if(!stopped)message(error.message)})
 weather()
 return ()=>{stopped=true;generation++;clearTimeout(timer);controller.abort();stopTracking();map?.remove()}
}

export function openMarketplace(token, defaultProvince='') {
 const e=escapeHtml, modal=document.createElement('dialog');modal.className='live-market-dialog'
 let closed=false, generation=0, timer, records=[], mine=[], priceRecords=[], objectUrls=[]
 modal.innerHTML=`<header class="live-heading"><h2>South African markets & farmer listings</h2><button class="close-market" aria-label="Close marketplace">×</button></header><p>Reference prices are published market information. Seller prices are offers from farmers.</p><div class="live-filters"><label>Search<input name="search" placeholder="Commodity or product"></label><label>Province<select name="province"><option value="">All provinces</option><option value="National">National reference prices</option>${provinces.map(p=>`<option ${p===defaultProvince?'selected':''}>${p}</option>`).join('')}</select></label><label>Reference market<select name="market"><option value="">All markets</option></select></label><button class="refresh-market">Refresh</button></div><section><h3>Market reference prices · ZAR</h3><div class="source-status" role="status">Loading sources…</div><div class="reference-prices"></div><div class="price-history"></div></section><section><div class="live-heading"><h3>Farmer-created product listings</h3><button class="new-listing">Create listing</button></div><p>Availability and prices are set by sellers. Contact the seller to arrange a purchase.</p><div class="seller-listings">Loading listings…</div><details class="my-listings"><summary>My listings · edit availability</summary><div></div></details><div class="listing-editor"></div></section>`
 document.body.append(modal);modal.showModal()
 const dispose=()=>{closed=true;generation++;clearTimeout(timer);objectUrls.forEach(URL.revokeObjectURL);modal.remove()}
 modal.querySelector('.close-market').onclick=()=>modal.close();modal.addEventListener('close',dispose,{once:true})
 const filters=()=>({q:modal.querySelector('[name=search]').value.toLowerCase(),province:modal.querySelector('[name=province]').value,market:modal.querySelector('[name=market]').value})
 function paint(){
  const {q,province,market}=filters()
  const prices=priceRecords.filter(p=>(!q||p.commodity.toLowerCase().includes(q))&&(!province||p.province===province)&&(!market||p.market===market))
  modal.querySelector('.reference-prices').innerHTML=prices.length?`<div class="forecast-scroll"><table><thead><tr><th>Commodity</th><th>Price / unit</th><th>Change</th><th>Market</th><th>Publication & source</th></tr></thead><tbody>${prices.map(p=>`<tr><td><button class="history-link" data-id="${p.id}">${e(p.commodity)}</button></td><td>${money(p.priceZar)} / ${e(p.unit)}</td><td>${p.changePercent==null?'Not available':display(p.changePercent,'%')}</td><td>${e(p.market)} · ${e(p.province)}</td><td>${e(p.publishedOn)}${p.stale?' · Cached / older publication':''}<br><a href="${e(p.sourceUrl)}" target="_blank" rel="noopener">${e(p.source)}</a><br><small>Retrieved ${e(timestamp(p.capturedAt))}</small></td></tr>`).join('')}</tbody></table></div>`:'<p class="notice">No verified reference prices match these filters. Unlicensed and unavailable prices are not estimated.</p><p><a href="https://joburgmarket.co.za/daily-price-list/" target="_blank" rel="noopener">Joburg Market daily report</a> · <a href="https://www.sagis.org.za/swb/" target="_blank" rel="noopener">SAGIS weekly bulletin</a> · <a href="https://marketdata.jse.co.za/" target="_blank" rel="noopener">JSE market data</a></p>'
  modal.querySelectorAll('.history-link').forEach(b=>b.onclick=()=>history(priceRecords.find(p=>p.id===b.dataset.id)))
  const filtered=records.filter(p=>(!q||(p.title+' '+p.category).toLowerCase().includes(q))&&(!province||p.province===province))
  modal.querySelector('.seller-listings').innerHTML=filtered.length?filtered.map(p=>listingCard(p,false)).join(''):'<p>No active seller listings match your search.</p>'
  modal.querySelector('.my-listings > div').innerHTML=mine.length?mine.map(p=>listingCard(p,true)).join(''):'<p>You have no listings yet.</p>'
  modal.querySelectorAll('.edit-listing').forEach(b=>b.onclick=()=>editor(mine.find(p=>p.id===b.dataset.id)))
  modal.querySelectorAll('[data-photo]').forEach(async img=>{try {const response=await fetch(`/api/v1/marketplace/listings/${img.dataset.photo}/photo`,{headers:{Authorization:`Bearer ${token}`}});if(!response.ok)throw Error();const url=URL.createObjectURL(await response.blob());if(closed){URL.revokeObjectURL(url);return}objectUrls.push(url);img.src=url}catch{img.alt='Photo unavailable'}})
 }
 function listingCard(p,own){return `<article class="seller-card">${p.hasPhoto?`<img data-photo="${p.id}" alt="Product photo" loading="lazy">`:''}<div><strong>${e(p.title)}</strong><p>${e(p.category)} · ${e(p.province||'Province not supplied')} · ${e(p.location)}</p><b>${money(p.priceZar)} / ${e(p.unit)}</b><p>${e(p.quantity)} ${e(p.unit)} · ${p.active?'Available':'Unavailable'}</p><p>Seller contact: ${e(p.contact)}</p><small>Updated ${e(timestamp(p.updatedAt))}</small>${own?`<button class="edit-listing" data-id="${p.id}">Edit listing</button>`:''}</div></article>`}
 async function history(p){
  const target=modal.querySelector('.price-history');target.textContent='Loading publication history…'
  try{const params=new URLSearchParams({commodity:p.commodity,market:p.market,source:p.source,unit:p.unit,province:p.province});const points=await api('/market-prices/history?'+params,token);if(closed)return
   if(points.length<2){target.textContent='Price trends will appear after at least two source publications have been collected.';return}
   const values=points.map(p=>Number(p.priceZar)),min=Math.min(...values),max=Math.max(...values),range=max-min||1
   target.innerHTML=`<h4>${e(p.commodity)} · ${e(p.market)} · ${e(p.unit)}</h4><svg viewBox="0 0 700 180" role="img" aria-label="Published price history"><polyline fill="none" stroke="#15994c" stroke-width="3" points="${values.map((v,i)=>`${20+i/(values.length-1)*660},${150-(v-min)/range*130}`).join('')}"/>${values.map((v,i)=>`<circle tabindex="0" cx="${20+i/(values.length-1)*660}" cy="${150-(v-min)/range*130}" r="5" fill="#15994c"><title>${e(points[i].publishedOn)} · ${money(v)}</title></circle>`).join('')}</svg><label>Explore publications<input type="range" min="0" max="${points.length-1}" value="${points.length-1}" class="history-slider"></label><output class="history-value"></output>`
   const show=()=>{const v=points[Number(target.querySelector('input').value)];target.querySelector('output').textContent=`${v.publishedOn} · ${money(v.priceZar)} / ${v.unit} · ${v.source}`};target.querySelector('input').oninput=show;show()
  }catch(error){target.textContent=error.message}
 }
 function editor(p){
  const target=modal.querySelector('.listing-editor')
  target.innerHTML=`<h4>${p?'Edit listing':'New product listing'}</h4><form class="live-listing-form"><div class="live-filters">${[['title','Product name','text'],['category','Category','text'],['quantity','Quantity','number'],['unit','Unit','text'],['priceZar','Price per unit (ZAR)','number'],['location','Town / public pickup location','text'],['contact','Public buyer contact','text']].map(([name,label,type])=>`<label>${label}<input required name="${name}" type="${type}" ${type==='number'?`step="0.01" min="${name==='quantity'?'0.01':'0'}"`:''} maxlength="${name==='title'||name==='location'?150:name==='contact'?120:name==='unit'?40:80}" value="${e(p?.[name]??'')}"></label>`).join('')}<label>Province<select required name="province">${provinces.map(v=>`<option ${v===(p?.province||defaultProvince)?'selected':''}>${v}</option>`).join('')}</select></label><label>Product photo (JPEG/PNG, ≤2 MB)<input type="file" name="photo" accept="image/jpeg,image/png"></label>${p?`<label><input type="checkbox" name="active" ${p.active?'checked':''}>Available for sale</label>`:''}</div><p>Your contact details and pickup location will be visible with the listing. Exact private farm GPS coordinates are never attached.</p><p class="editor-message" role="status"></p><button type="submit">Save listing</button><button type="button" class="cancel-edit">Cancel</button></form>`
  target.querySelector('.cancel-edit').onclick=()=>target.replaceChildren()
  target.querySelector('form').onsubmit=async event=>{
   event.preventDefault();const form=event.currentTarget,submit=form.querySelector('[type=submit]'),status=form.querySelector('.editor-message');submit.disabled=true
   try {const data=new FormData(form),photo=data.get('photo');if(photo.size>2000000)throw new Error('Choose a photo under 2 MB.');const listing=Object.fromEntries(['title','category','quantity','unit','priceZar','location','contact','province'].map(k=>[k,['quantity','priceZar'].includes(k)?Number(data.get(k)):data.get(k)]));
    const result=await api(p?`/marketplace/listings/${p.id}`:'/marketplace/listings',token,{method:p?'PATCH':'POST',body:JSON.stringify(p?{listing,active:data.has('active')}:listing)})
    if(photo.size){const upload=new FormData();upload.append('file',photo);try{await api(`/marketplace/listings/${result.id}/photo`,token,{method:'POST',body:upload})}catch(error){status.textContent=`Listing saved, but photo upload failed: ${error.message}`;await load();return}}
    target.replaceChildren();await load()
   }catch(error){status.textContent=error.message}finally{submit.disabled=false}
  }
  target.scrollIntoView({behavior:'smooth',block:'nearest'})
 }
 async function load(){
  clearTimeout(timer);const version=++generation
  const results=await Promise.allSettled([api('/market-prices/status',token),api('/marketplace/listings',token),api('/marketplace/mine',token)])
  if(closed||version!==generation)return
  const [price,sellers,own]=results
  if(price.status==='fulfilled'){
   priceRecords=price.value.prices
   modal.querySelector('.source-status').innerHTML=price.value.sources.map(s=>`<p><b>${e(s.source)} · ${e(s.state.replaceAll('_',' '))}</b> ${e(s.message||'Latest available publication; not a real-time quote.')}<br><small>${e(timestamp(s.retrievedAt))}${s.nextRefreshAt?' · Next check '+e(timestamp(s.nextRefreshAt)):''}</small></p>`).join('')
   const select=modal.querySelector('[name=market]'),value=select.value;select.innerHTML='<option value="">All markets</option>'+[...new Set(priceRecords.map(p=>p.market))].map(m=>`<option>${e(m)}</option>`).join('');select.value=value
  }else modal.querySelector('.source-status').textContent=`${price.reason.message} Previously loaded prices, if any, may be outdated.`
  if(sellers.status==='fulfilled')records=sellers.value
  if(own.status==='fulfilled')mine=own.value
  objectUrls.forEach(URL.revokeObjectURL);objectUrls=[];paint()
  if(sellers.status==='rejected')modal.querySelector('.seller-listings').textContent=sellers.reason.message
  timer=setTimeout(load,300000)
 }
 modal.querySelectorAll('.live-filters input,.live-filters select').forEach(input=>input.addEventListener('input',()=>{modal.querySelector('.price-history').replaceChildren();paint()}))
 modal.querySelector('.refresh-market').onclick=load;modal.querySelector('.new-listing').onclick=()=>editor();load()
 return dispose
}
