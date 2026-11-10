import {api,escapeHtml as e,display,timestamp} from './live-data.js'

const svg=body=>`<svg viewBox="0 0 32 32" aria-hidden="true" focusable="false">${body}</svg>`
const cow=svg('<path fill="currentColor" d="M4 9h17l4 3 4-1v9h-6l-2-3H9v9H6v-8H4v8H2V13L0 9l1-1 3 3zm20 2-2-4 3 2 3-3-1 6z"/><circle cx="26" cy="15" r="1" fill="white"/>')
const syringe=svg('<g fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="m7 22 3 3L24 11l-6-6L4 19l3 3-5 7m15-25 8 8M22 3l7 7M24 5l-3 3M9 14l4 4m0-8 4 4m0-8 4 4"/></g>')
const sprout=svg('<path fill="currentColor" d="M15 29h3V17C29 19 31 9 30 4 22 3 17 7 16 13 11 5 5 5 1 7c0 8 5 13 14 11z"/><path fill="none" stroke="white" stroke-width="1" d="m5 10 11 7L26 8"/>')
const heart=svg('<path fill="currentColor" d="M16 28S1 18 1 10C1 1 12 0 16 7c4-7 15-6 15 3 0 8-15 18-15 18"/><path d="M4 15h6l3-6 4 12 3-6h8" fill="none" stroke="white" stroke-width="1.7"/>')
const icons={reports:svg('<g fill="currentColor"><rect x="2" y="17" width="6" height="13" rx="1"/><rect x="12" y="8" width="6" height="22" rx="1"/><rect x="22" y="1" width="6" height="29" rx="1"/></g>'),livestock:cow,vaccines:syringe,crops:sprout,health:heart,trend:cow,download:'⇩',weather:'☀'}

export function dateInSA(value=new Date()){return new Intl.DateTimeFormat('en-CA',{timeZone:'Africa/Johannesburg',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date(value))}
export function periodStart(months,now=new Date()){
 if(!months)return '0000-01-01'
 const [year,month]=dateInSA(now).split('-').map(Number)
 return new Date(Date.UTC(year,month-Number(months),1)).toISOString().slice(0,10)
}
const recordDate=r=>String(r.symptomsStartedOn||r.administeredOn||r.plantingDate||r.createdAt||'').slice(0,10)
export function summarizeReports(data,months=3,now=new Date()){
 const today=dateInSA(now),from=periodStart(months,now),inPeriod=r=>recordDate(r)>=from&&recordDate(r)<=today
 const animals=data.animals||[],health=(data.health||[]).filter(inPeriod),vaccinations=(data.vaccinations||[]).filter(inPeriod),crops=data.crops||[]
 const species={};for(const a of animals){const name=a.species?.trim()||'Unspecified';const key=name.charAt(0).toUpperCase()+name.slice(1).toLowerCase();species[key]=(species[key]||0)+1}
 const latest=new Map();for(const dose of data.vaccinations||[]){const key=dose.animalId+':'+dose.vaccineName.toLowerCase();if(!latest.has(key)||dose.administeredOn>latest.get(key).administeredOn)latest.set(key,dose)}
 const upcomingDate=new Date(new Date(today+'T00:00:00Z').getTime()+30*86400000).toISOString().slice(0,10)
 const schedules={Completed:0,Upcoming:0,Overdue:0}
 for(const v of latest.values())schedules[v.nextDueOn&&v.nextDueOn<today?'Overdue':v.nextDueOn&&v.nextDueOn<=upcomingDate?'Upcoming':'Completed']++
 const count=Number(months)||12,[y,m]=today.split('-').map(Number),trend=[]
 for(let i=count-1;i>=0;i--){const start=new Date(Date.UTC(y,m-1-i,1)),end=new Date(Date.UTC(y,m-i,1));trend.push({month:start.toLocaleDateString('en-ZA',{month:'short',timeZone:'UTC'}),date:start.toISOString().slice(0,7),count:animals.filter(a=>a.createdAt&&new Date(a.createdAt)<end&&String(a.createdAt).slice(0,10)<=today).length})}
 return {today,from,total:animals.length,active:animals.filter(a=>a.status==='ACTIVE').length,inactive:animals.filter(a=>a.status!=='ACTIVE').length,species,health,vaccinations,crops,periodCrops:crops.filter(inPeriod),schedules,trend}
}
export function csvCell(value){const text=String(value??'');return '"'+(/^[=+\-@\t\r]/.test(text)?"'"+text:text).replaceAll('"','""')+'"'}
const colours=['#08952f','#81cc35','#f4c21b','#fc943a','#3b9dd9','#9868cb']
function donut(title,entries,unit,kind=''){
 const values=Object.entries(entries),total=values.reduce((n,[,v])=>n+v,0);let cumulative=0
 const stops=values.filter(([,v])=>v>0).map(([name,v],i)=>{const from=cumulative;cumulative+=v/total*100;return `${kind==='vaccines'?({Completed:'#159c34',Upcoming:'#f6c500',Overdue:'#eb3d35'}[name]):colours[i%colours.length]} ${from}% ${cumulative}%`})
 return `<article class="report-card report-donut-card"><h3><span>${kind==='vaccines'?icons.vaccines:icons.livestock}</span>${e(title)}</h3><div class="report-donut-layout"><div class="report-donut" role="img" aria-label="${e(title+': '+values.map(([k,v])=>`${k} ${v}`).join(', '))}" style="--report-ring:${total?'conic-gradient('+stops.join(',')+')':'#eaf0eb'}"><div><strong>${total}</strong><small>${e(unit)}</small></div></div><ul class="report-legend">${values.map(([name,v],i)=>`<li><i style="background:${kind==='vaccines'?({Completed:'#159c34',Upcoming:'#f6c500',Overdue:'#eb3d35'}[name]):colours[i%colours.length]}"></i><span>${e(name)}</span><b>${v}</b><small>(${total?Math.round(v/total*100):0}%)</small></li>`).join('')}</ul></div>${!total?'<p class="report-muted">No records available.</p>':''}</article>`
}
function trendChart(points){
 const max=Math.max(3,...points.map(p=>p.count)),x=i=>36+(points.length===1?0:i/(points.length-1)*308),y=v=>150-v/max*120,line=points.map((p,i)=>`${x(i)},${y(p.count)}`).join(' ')
 return `<article class="report-card report-trend"><h3><span>${icons.trend}</span>Livestock Trend</h3><svg viewBox="0 0 370 190" role="img" aria-label="Cumulative livestock registrations by month"><defs><linearGradient id="report-trend-fill" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#69ce77" stop-opacity=".4"/><stop offset="100%" stop-color="#69ce77" stop-opacity=".12"/></linearGradient></defs>${[0,1,2,3].map(i=>{const value=Math.round(max/3*i);return `<line x1="36" x2="345" y1="${y(value)}" y2="${y(value)}" stroke="#e3eae6"/><text x="20" y="${y(value)+4}" text-anchor="end">${value}</text>`}).join('')}<polygon points="36,150 ${line} 344,150" fill="url(#report-trend-fill)"/><polyline points="${line}" fill="none" stroke="#079830" stroke-width="2.5"/>${points.map((p,i)=>`<circle tabindex="0" cx="${x(i)}" cy="${y(p.count)}" r="3.5" fill="white" stroke="#079830" stroke-width="2"><title>${e(p.date)}: ${p.count} animal registrations</title></circle><text x="${x(i)}" y="174" text-anchor="middle">${e(p.month)}</text>`).join('')}</svg><p class="report-footnote">Cumulative registrations, based on dates recorded in AgriNexus.</p></article>`
}
function card(title,value,subtitle,icon,tone,extra=''){return `<article class="report-stat ${tone}"><div class="report-stat-icon">${icon}</div><div><h3>${title}</h3><strong>${value}</strong>${extra}<p>${subtitle}</p></div></article>`}
function healthTable(records,all=false){return `<article class="report-card report-health"><div class="report-card-heading"><h3><span>${icons.health}</span>Recent Animal Health Records</h3><button class="report-health-all">${all?'Show recent':'View All →'}</button></div><div class="report-table-scroll"><table><thead><tr><th>Animal ID</th><th>Type</th><th>Date</th><th>Status</th></tr></thead><tbody>${records.length?records.slice(0,all?records.length:5).map(r=>`<tr><td><strong>${e(r.internalId)}</strong></td><td title="${e(r.observedSymptoms)}">${e(r.species)}</td><td>${e(new Date((r.symptomsStartedOn||r.createdAt).slice(0,10)+'T12:00:00Z').toLocaleDateString('en-ZA',{day:'2-digit',month:'short',year:'numeric'}))}</td><td><span class="report-badge">${r.veterinarianContacted?'Vet contacted':'Recorded'}</span></td></tr>`).join(''):'<tr><td colspan="4" class="report-empty">No health records in this period.</td></tr>'}</tbody></table></div></article>`}
function cropsCard(crops){return `<article class="report-card report-crops"><h3><span>${icons.crops}</span>Crops Overview</h3>${crops.length?`<ul class="report-crop-list">${crops.slice(0,4).map(c=>`<li><span>♧</span><div><strong>${e(c.cropType)}</strong><small>${e(c.fieldName)} · ${e(c.status)}</small></div></li>`).join('')}</ul>`:'<div class="report-sprouts" aria-hidden="true"><span>🌱</span><span>🌱</span><span>🌱</span></div><strong class="report-empty-title">No crops registered yet</strong><p>Add your first crop to start tracking planting, growth, and harvest.</p>'}<button class="report-add-crop">＋ Add Crop</button></article>`}
function weatherCard(w,farm){return `<article class="report-weather"><h3>${icons.weather} Weather – ${e(farm.municipality||farm.province)}</h3>${w?.available?`<div class="report-weather-current"><div><span class="report-sun">${w.weatherCode===0?'☀':'⛅'}</span><strong>${display(w.temperatureC,'°C')}</strong><p>${w.status==='stale'?'Cached / outdated forecast':'Current forecast'}</p></div><ul><li>♧ Humidity: ${display(w.humidityPercent,'%')}</li><li>≋ Wind: ${display(w.windSpeedKph,' km/h')}</li><li>☂ Rain: ${display(w.rainProbability,'%')}</li></ul></div>`:`<p class="report-weather-missing">${e(w?.message||'Weather unavailable. Save farm coordinates in Settings and retry.')}</p>`}<div class="report-weather-landscape" role="img" aria-label="Illustrative agricultural landscape"></div><small>Open-Meteo · ${w?.retrievedAt?e(timestamp(w.retrievedAt)):'No successful weather update'}</small></article>`}
export function reportCsv(data,summary,farm,tab){
 const rows=[['AgriNexus Report',farm.name],['Generated',new Date().toISOString()],['Date range',summary.from+' to '+summary.today],['Section',tab]]
 if(tab==='Overview'||tab==='Livestock'){
  rows.push([],['Animal ID','Species','Current status','Registered at'])
  for(const a of data.animals)rows.push([a.internalId,a.species,a.status,a.createdAt])
 }
 if(tab==='Overview'||tab==='Animal Health'){
  rows.push([],['Animal ID','Symptoms','Date','Veterinarian contacted']);for(const h of summary.health)rows.push([h.internalId,h.observedSymptoms,h.symptomsStartedOn||h.createdAt,h.veterinarianContacted?'Yes':'No'])
  rows.push([],['Animal ID','Vaccine','Administered','Next due']);for(const v of summary.vaccinations)rows.push([v.internalId,v.vaccineName,v.administeredOn,v.nextDueOn])
 }
 if(tab==='Overview'||tab==='Crops'){
  rows.push([],['Field','Crop','Status','Planting date']);for(const c of data.crops)rows.push([c.fieldName,c.cropType,c.status,c.plantingDate])
 }
 if(tab==='Financial')rows.push([],['Financial records','Not available in the current application'])
 return '\ufeff'+rows.map(r=>r.map(csvCell).join(',')).join('\r\n')
}
function downloadCsv(data,summary,farm,tab){
 const blob=new Blob([reportCsv(data,summary,farm,tab)],{type:'text/csv;charset=utf-8'}),url=URL.createObjectURL(blob),link=document.createElement('a');link.href=url;link.download=`AgriNexus-report-${summary.today}.csv`;link.click();setTimeout(()=>URL.revokeObjectURL(url),1000)
}

async function mapLimit(items,worker,limit=5){let index=0;await Promise.all(Array.from({length:Math.min(limit,items.length)},async()=>{while(index<items.length){const item=items[index++];await worker(item)}}))}
function navigate(label){[...document.querySelectorAll('.app-shell aside nav button')].find(b=>b.textContent.includes(label))?.click()}
function mountReports(workspace,farmId,token){
 const abort=new AbortController();let stopped=false,data={animals:[],health:[],vaccinations:[],crops:[],farm:{}},failures=[],tab='Overview',months=3,allHealth=false,loading=false
 const root=document.createElement('section');root.className='live-reports';workspace.prepend(root)
 const get=path=>api(path,token,{signal:abort.signal})
 const fail=label=>{if(!failures.includes(label))failures.push(label)}
 function paint(){
  if(stopped)return
  const s=summarizeReports(data,months),health=[...s.health].sort((a,b)=>recordDate(b).localeCompare(recordDate(a)))
  root.innerHTML=`<header class="reports-header"><div class="reports-title"><span>${icons.reports}</span><div><h1>Reports</h1><p>Farm performance, records and insights</p></div></div><div class="reports-actions"><label><span class="sr-only">Report date range</span><select class="report-range"><option value="3" ${months===3?'selected':''}>▣ Last 3 Months</option><option value="6" ${months===6?'selected':''}>▣ Last 6 Months</option><option value="12" ${months===12?'selected':''}>▣ Last 12 Months</option><option value="0" ${months===0?'selected':''}>▣ All Records</option></select></label><details class="report-download-menu"><summary>${icons.download} Download Report ⌄</summary><div><button class="report-download-csv" ${loading?'disabled':''}>Download CSV</button><button class="report-print">Print / Save PDF</button></div></details><button class="report-refresh" aria-label="Refresh reports" ${loading?'disabled':''}>↻</button></div></header><nav class="report-tabs" aria-label="Report categories">${['Overview','Livestock','Crops','Animal Health','Financial'].map((name,i)=>`<button data-tab="${name}" ${tab===name?'class="active" aria-current="page"':''}><span>${['⌂','♧','♧','⚕','▦'][i]}</span>${name}</button>`).join('')}</nav><div class="report-status" aria-live="polite">${loading?'Loading farm records…':failures.length?'Some information could not load: '+e(failures.join(', '))+'. Refresh to retry.':`Current farm totals · Health and vaccination records: ${s.from==='0000-01-01'?'all dates':e(s.from)} to ${s.today}`}</div>${tab==='Financial'?'<section class="report-card report-financial"><h2>Financial reports</h2><p>Income and expense records are not available in the current application. No financial totals have been estimated.</p></section>':`<div class="report-stat-grid">${card('Total Livestock',failures.includes('Livestock')?'—':s.total,`Active: ${s.active} | Inactive: ${s.inactive}`,icons.livestock,'mint')}${card('Vaccinations',failures.includes('Vaccinations')?'—':s.vaccinations.length,`<span class="report-overdue">● ${s.schedules.Overdue} overdue schedules</span>`,icons.vaccines,'blue',`<small class="report-completed">● Recorded in period</small>`)}${card('Total Crops',failures.includes('Crops')?'—':s.crops.length,s.crops.length?'Registered crop cycles':'No crops registered',icons.crops,'yellow')}${card('Health Records',failures.includes('Health records')?'—':s.health.length,'Records in selected period',icons.health,'pink')}</div><div class="report-chart-grid" ${tab==='Crops'||tab==='Animal Health'?'hidden':''}>${donut('Livestock by Species',s.species,'Animals')}${donut('Vaccination Status',s.schedules,'Schedules','vaccines')}${trendChart(s.trend)}</div><div class="report-bottom-grid" ${tab==='Livestock'?'hidden':''}>${healthTable(health,allHealth)}${cropsCard(s.crops)}${weatherCard(data.weather,data.farm)}</div>${tab==='Livestock'?`<section class="report-card report-detail-table"><h3>Livestock register</h3><div class="report-table-scroll"><table><thead><tr><th>Animal ID</th><th>Species</th><th>Breed</th><th>Current status</th></tr></thead><tbody>${data.animals.map(a=>`<tr><td>${e(a.internalId)}</td><td>${e(a.species)}</td><td>${e(a.breed||'—')}</td><td>${e(a.status)}</td></tr>`).join('')||'<tr><td colspan="4">No livestock registered.</td></tr>'}</tbody></table></div></section>`:''}${tab==='Crops'&&s.crops.length?`<section class="report-card report-detail-table"><h3>Crop records</h3><div class="report-table-scroll"><table><thead><tr><th>Crop</th><th>Field</th><th>Planted</th><th>Status</th></tr></thead><tbody>${s.crops.map(c=>`<tr><td>${e(c.cropType)}</td><td>${e(c.fieldName)}</td><td>${e(c.plantingDate)}</td><td>${e(c.status)}</td></tr>`).join('')}</tbody></table></div></section>`:''}${tab==='Animal Health'?`<section class="report-card report-detail-table"><h3>Vaccination records</h3><div class="report-table-scroll"><table><thead><tr><th>Animal ID</th><th>Vaccine</th><th>Administered</th><th>Next due</th></tr></thead><tbody>${s.vaccinations.map(v=>`<tr><td>${e(v.internalId)}</td><td>${e(v.vaccineName)}</td><td>${e(v.administeredOn)}</td><td>${e(v.nextDueOn||'Not recorded')}</td></tr>`).join('')||'<tr><td colspan="4">No vaccinations in this period.</td></tr>'}</tbody></table></div></section>`:''}`}`
  root.querySelectorAll('[data-tab]').forEach(button=>button.onclick=()=>{tab=button.dataset.tab;allHealth=tab==='Animal Health';paint()})
  root.querySelector('.report-range').onchange=event=>{months=Number(event.target.value);paint()}
  root.querySelector('.report-refresh').onclick=load
  root.querySelector('.report-health-all')?.addEventListener('click',()=>{allHealth=!allHealth;paint()})
  root.querySelector('.report-add-crop')?.addEventListener('click',()=>navigate('Crops'))
  root.querySelector('.report-download-csv').onclick=()=>downloadCsv(data,s,data.farm,tab)
  root.querySelector('.report-print').onclick=()=>window.print()
 }
 async function load(){
  if(loading||stopped)return
  loading=true;failures=[];paint()
  const results=await Promise.allSettled([get(`/farms/${farmId}`),get(`/farms/${farmId}/animals`),get(`/farms/${farmId}/fields`),get(`/farms/${farmId}/weather`)])
  if(stopped)return
  data={farm:{},animals:[],crops:[],health:[],vaccinations:[]}
  if(results[0].status==='fulfilled')data.farm=results[0].value;else fail('Farm details')
  if(results[1].status==='fulfilled')data.animals=results[1].value;else{fail('Livestock');fail('Health records');fail('Vaccinations')}
  if(results[3].status==='fulfilled')data.weather=results[3].value;else fail('Weather')
  const fields=results[2].status==='fulfilled'?results[2].value:[];if(results[2].status==='rejected')fail('Crops')
  await mapLimit(data.animals,async animal=>{
   const [h,v]=await Promise.allSettled([get(`/animals/${animal.id}/health-records`),get(`/animals/${animal.id}/vaccinations`)])
   if(h.status==='fulfilled')data.health.push(...h.value.map(record=>({...record,animalId:animal.id,internalId:animal.internalId,species:animal.species})));else fail('Health records')
   if(v.status==='fulfilled')data.vaccinations.push(...v.value.map(record=>({...record,animalId:animal.id,internalId:animal.internalId})));else fail('Vaccinations')
  })
  await mapLimit(fields,async field=>{try{const crops=await get(`/fields/${field.id}/crops`);data.crops.push(...crops.map(c=>({...c,fieldName:field.name})))}catch{fail('Crops')}})
  loading=false;paint()
 }
 load()
 return ()=>{stopped=true;abort.abort();root.remove()}
}

let dispose=()=>{},mountedFarm='',mountedWorkspace
function sync(){
 const workspace=document.querySelector('.app-shell .workspace'),active=workspace?.querySelector(':scope > .page-heading h2')?.textContent.trim()==='Simple summaries',farmId=document.querySelector('.farm-switcher select')?.value
 if(!active){if(mountedWorkspace){dispose();mountedWorkspace.classList.remove('report-active');mountedWorkspace=null;mountedFarm=''}return}
 if(workspace===mountedWorkspace&&farmId===mountedFarm)return
 dispose();mountedWorkspace?.classList.remove('report-active');mountedWorkspace=workspace;mountedFarm=farmId
 let token;try{token=JSON.parse(sessionStorage.getItem('agrinexus.session'))?.accessToken}catch{}
 if(!farmId||!token)return
 workspace.classList.add('report-active');dispose=mountReports(workspace,farmId,token)
}
if(typeof document!=='undefined'){
 new MutationObserver(sync).observe(document.body,{childList:true,subtree:true})
 document.addEventListener('change',event=>{if(event.target.matches('.farm-switcher select'))setTimeout(sync,0)})
 sync()
}
