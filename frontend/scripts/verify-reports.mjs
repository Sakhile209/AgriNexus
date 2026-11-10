import assert from 'node:assert/strict'
const {chromium}=await import(process.env.PLAYWRIGHT_MODULE||'playwright')
const browser=await chromium.launch({headless:true,executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--use-angle=swiftshader','--enable-unsafe-swiftshader']})
const page=await browser.newPage({viewport:{width:1600,height:1000}}),errors=[];page.on('pageerror',e=>errors.push(e.message))
const farms=[{id:'11111111-1111-1111-1111-111111111111',name:'Preview Farm',province:'KwaZulu-Natal',municipality:'Newcastle',latitude:-27.75,longitude:29.93},{id:'22222222-2222-2222-2222-222222222222',name:'Second farm',province:'Western Cape',municipality:'Cape Town',latitude:-33.93,longitude:18.42}]
const today=new Date().toISOString().slice(0,10),old=new Date(new Date().setMonth(new Date().getMonth()-5)).toISOString().slice(0,10)
const animal={id:'33333333-3333-3333-3333-333333333333',internalId:'C001',species:'Cattle',status:'ACTIVE',createdAt:old+'T12:00:00Z'}
let failHealth=false
await page.addInitScript(()=>sessionStorage.setItem('agrinexus.session',JSON.stringify({accessToken:'isolated-report-fixture',user:{firstName:'Preview',lastName:'Farmer'}})))
await page.route('https://tile.openstreetmap.org/**',route=>route.abort())
await page.route('**/api/v1/**',async route=>{
 const path=new URL(route.request().url()).pathname.replace('/api/v1',''),farm=farms.find(f=>path.includes(f.id))||farms[0];let body=[]
 if(path==='/farms')body=farms
 else if(path===`/farms/${farm.id}`)body=farm
 else if(path.endsWith('/animals'))body=farm===farms[0]?[animal]:[]
 else if(path.endsWith('/health-records')){if(failHealth){await route.fulfill({status:503,contentType:'application/json',body:'{"message":"Test health outage"}'});return}body=[{id:'h1',observedSymptoms:'Follow-up inspection',symptomsStartedOn:today,veterinarianContacted:false,createdAt:today+'T12:00:00Z'},{id:'h2',observedSymptoms:'Older inspection',symptomsStartedOn:old,veterinarianContacted:true,createdAt:old+'T12:00:00Z'}]}
 else if(path.endsWith('/vaccinations'))body=[{id:'v1',vaccineName:'FMD',administeredOn:today,nextDueOn:'2027-12-01'},{id:'v2',vaccineName:'FMD',administeredOn:old,nextDueOn:old}]
 else if(path.endsWith('/reports/livestock'))body={total:farm===farms[0]?1:0,byStatus:{ACTIVE:1},bySpecies:{Cattle:1}}
 else if(path.endsWith('/reports/health'))body={healthRecords:1,vaccinations:1,overdueVaccinations:0}
 else if(path.endsWith('/reports/crops'))body={total:0,byStatus:{}}
 else if(path.endsWith('/weather'))body={available:true,temperatureC:24,humidityPercent:58,windSpeedKph:12,rainProbability:0,provider:'Open-Meteo',retrievedAt:new Date().toISOString(),weatherCode:2,status:'current',location:farm.municipality,forecast:[],advisories:[]}
 else if(path.endsWith('/dashboard'))body={farmName:farm.name,activeLivestock:farm===farms[0]?1:0,activeCrops:0,upcomingVaccinations:0,livestockBySpecies:{Cattle:1},weather:{available:false,message:'Fixture'}}
 else if(path.endsWith('/map-config'))body={standardTiles:'https://tile.openstreetmap.org/{z}/{x}/{y}.png',satelliteAvailable:false}
 await route.fulfill({status:200,contentType:'application/json',body:JSON.stringify(body)})
})
try{
 await page.goto(process.env.AGRINEXUS_WEB_URL||'http://localhost:5174/')
 await page.locator('.app-shell aside nav button').filter({hasText:'Reports'}).click()
 await page.locator('.live-reports').waitFor();await page.waitForFunction(()=>document.querySelector('.report-status')?.textContent.startsWith('Current farm totals'))
 assert.deepEqual(await page.locator('.report-stat strong').allTextContents(),['1','1','0','1'])
 assert.equal(await page.locator('.report-donut').count(),2);assert.equal(await page.locator('.report-trend > svg').count(),1)
 await page.locator('.report-health').getByText('C001',{exact:true}).waitFor();await page.locator('.report-weather').getByText('24°C',{exact:true}).waitFor()
 await page.screenshot({path:'/tmp/agrinexus-reports-desktop.png',fullPage:true})
 await page.locator('.report-range').selectOption('6');assert.deepEqual(await page.locator('.report-stat strong').allTextContents(),['1','2','0','2'])
 await page.locator('[data-tab="Livestock"]').click();await page.locator('.report-detail-table').getByText('C001',{exact:true}).waitFor()
 await page.locator('[data-tab="Animal Health"]').click();await page.locator('.report-detail-table').getByText('FMD',{exact:true}).first().waitFor()
 await page.locator('[data-tab="Financial"]').click();await page.getByText('No financial totals have been estimated.',{exact:false}).waitFor()
 await page.locator('[data-tab="Overview"]').click()
 await page.locator('.report-download-menu summary').click();const downloaded=page.waitForEvent('download');await page.locator('.report-download-csv').click();const download=await downloaded;assert.ok(download.suggestedFilename().endsWith('.csv'))
 failHealth=true;await page.getByRole('button',{name:'Refresh reports'}).click();await page.waitForFunction(()=>document.querySelector('.report-status')?.textContent.includes('Health records. Refresh to retry.'));assert.equal(await page.locator('.report-stat strong').nth(3).textContent(),'—');failHealth=false
 await page.locator('.farm-switcher select').selectOption(farms[1].id);await page.waitForFunction(()=>document.querySelector('.report-stat strong')?.textContent==='0'&&document.querySelector('.report-status')?.textContent.startsWith('Current farm totals'));assert.deepEqual(await page.locator('.report-stat strong').allTextContents(),['0','0','0','0'])
 await page.setViewportSize({width:390,height:844});await page.screenshot({path:'/tmp/agrinexus-reports-mobile.png',fullPage:true});assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth),true)
 await page.locator('.report-add-crop').click();await page.locator('.workspace > .page-heading h2').getByText('Fields and crop cycles',{exact:true}).waitFor();assert.equal(await page.locator('.live-reports').count(),0)
 assert.deepEqual(errors,[]);console.log(JSON.stringify({passed:['reference report layout','actual counts','species/vaccination charts','health records and weather','date filters','category tabs','financial unavailable state','CSV download','partial API failure','selected farm refresh','responsive layout','Add Crop navigation','no browser exceptions'],screenshots:['/tmp/agrinexus-reports-desktop.png','/tmp/agrinexus-reports-mobile.png']}))
}catch(error){console.error({errors,status:await page.locator('.report-status').textContent().catch(()=>null)});await page.screenshot({path:'/tmp/reports-test-failure.png',fullPage:true});throw error}finally{await browser.close()}
