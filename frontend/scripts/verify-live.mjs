// Isolated browser fixtures: no production farm records or external price feeds are modified.
import assert from 'node:assert/strict'
const {chromium}=await import(process.env.PLAYWRIGHT_MODULE || 'playwright')
const browser=await chromium.launch({headless:true,executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox','--use-angle=swiftshader','--enable-unsafe-swiftshader']})
const page=await browser.newPage({viewport:{width:1440,height:1000}})
const errors=[];page.on('pageerror',e=>errors.push(e.message))
const calls=[]
const farms=[{id:'11111111-1111-1111-1111-111111111111',name:'Test Newcastle',farmType:'Mixed',province:'KwaZulu-Natal',municipality:'Newcastle',latitude:-27.75,longitude:29.93},{id:'22222222-2222-2222-2222-222222222222',name:'Test Cape Town',farmType:'Crop',province:'Western Cape',municipality:'Cape Town',latitude:-33.93,longitude:18.42}]
const user={firstName:'Browser',lastName:'Test',email:'browser@example.invalid'}
const auth={accessToken:'isolated-browser-fixture',user}
let offers=[],weatherCalls=0
await page.addInitScript(auth=>sessionStorage.setItem('agrinexus.session',JSON.stringify(auth)),auth)
await page.route('https://tile.openstreetmap.org/**',route=>route.fulfill({contentType:'image/png',body:Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aU1cAAAAASUVORK5CYII=','base64')}))
await page.route('**/api/v1/**',async route=>{
 const request=route.request(),url=new URL(request.url()),path=url.pathname.replace('/api/v1','');calls.push({path,method:request.method()})
 const farm=farms.find(f=>path.includes(f.id))||farms[0]
 const weather={available:true,temperatureC:farm===farms[0]?23:16,humidityPercent:65,windSpeedKph:15,windDirectionDegrees:180,currentRainfallMm:0,rainfallMm:1,rainProbability:30,minimumC:12,maximumC:25,provider:'Open-Meteo',location:farm.name,status:'current',retrievedAt:new Date().toISOString(),observedAt:'2026-11-10T10:00',nextRefreshAt:new Date(Date.now()+1800000).toISOString(),forecast:[{date:'2026-11-10',minimumC:12,maximumC:25,rainfallMm:1,rainProbability:30}],advisories:[]}
 let body=[]
 if(path==='/farms')body=farms
 else if(path.endsWith('/map-config'))body={standardTiles:'https://tile.openstreetmap.org/{z}/{x}/{y}.png',satelliteAvailable:false}
 else if(path.endsWith('/weather')){body=weather;weatherCalls++}
 else if(path.endsWith('/dashboard'))body={farmName:farm.name,activeLivestock:0,activeCrops:0,upcomingVaccinations:0,unreadNotifications:0,livestockBySpecies:{},weather}
 else if(path===`/farms/${farm.id}`){if(request.method()==='PATCH')Object.assign(farm,request.postDataJSON());body=farm}
 else if(path==='/market-prices/status')body={prices:[],sources:[{source:'Test connector',state:'not_configured',message:'No licensed feed configured'}]}
 else if(path==='/marketplace/listings'&&request.method()==='POST'){body={...request.postDataJSON(),id:'33333333-3333-3333-3333-333333333333',active:true,createdAt:new Date().toISOString(),updatedAt:new Date().toISOString()};offers=[body]}
 else if(path.startsWith('/marketplace/listings/')&&request.method()==='PATCH'){const data=request.postDataJSON();offers=[{...offers[0],...data.listing,active:data.active}];body=offers[0]}
 else if(path==='/marketplace/listings')body=offers.filter(p=>p.active)
 else if(path==='/marketplace/mine')body=offers
 await route.fulfill({status:200,contentType:'application/json',body:JSON.stringify(body)})
})
try{
 await page.clock.install({time:Date.now()})
 await page.goto(process.env.AGRINEXUS_WEB_URL||'http://localhost:5174/')
 await page.locator('#live-weather-detail').getByText('Test Newcastle',{exact:false}).waitFor()
 await page.locator('.maplibregl-canvas').waitFor()
 await page.locator('.maplibregl-marker').click()
 await page.getByText('Test Newcastle · Newcastle, KwaZulu-Natal · -27.75, 29.93').waitFor()
 assert.equal(await page.locator('.map-layer option[value=satellite]').isDisabled(),true)
 await page.locator('.farm-switcher select').selectOption(farms[1].id)
 await page.locator('#live-weather-detail').getByText('Test Cape Town',{exact:false}).waitFor()
 await page.locator('.maplibregl-marker').click()
 await page.getByText('Test Cape Town · Cape Town, Western Cape · -33.93, 18.42').waitFor()
 const before=weatherCalls;await page.getByRole('button',{name:'Refresh',exact:true}).click();await page.waitForFunction(()=>!document.querySelector('#live-weather-detail').hasAttribute('aria-busy'));assert.ok(weatherCalls>before)
 const beforeAutomatic=weatherCalls;await page.clock.fastForward(1800001);await page.waitForFunction(()=>!document.querySelector('#live-weather-detail').hasAttribute('aria-busy'));assert.ok(weatherCalls>beforeAutomatic)
 await page.clock.resume()
 await page.context().grantPermissions(['geolocation']);await page.context().setGeolocation({latitude:-33.94,longitude:18.43,accuracy:7})
 await page.getByRole('button',{name:'Use live location',exact:true}).click();await page.getByText('Live device GPS: -33.940000, 18.430000',{exact:false}).waitFor();await page.locator('.live-device-marker').waitFor()
 assert.equal(calls.filter(c=>c.method==='PATCH').length,0)
 await page.context().setGeolocation({latitude:-33.95,longitude:18.44,accuracy:9});await page.getByText('Live device GPS: -33.950000, 18.440000',{exact:false}).waitFor()
 await page.getByRole('button',{name:'Save GPS as farm location',exact:true}).click();await page.getByText('Farm GPS saved: -33.95, 18.44.',{exact:false}).waitFor();assert.equal(farms[1].latitude,-33.95);assert.equal(farms[1].longitude,18.44)
 await page.getByRole('button',{name:'Stop live location',exact:true}).click();await page.getByText('Live location stopped.',{exact:false}).waitFor();assert.equal(await page.locator('.live-device-marker').count(),0)
 await page.locator('[data-home]').click()
 await page.getByText('No licensed feed configured').waitFor()
 await page.getByRole('button',{name:'Create listing',exact:true}).click()
 const form=page.locator('.live-listing-form')
 for(const [name,value]of Object.entries({title:'Browser test maize',category:'Grain',quantity:'10',unit:'kg',priceZar:'5',location:'Test pickup',contact:'test@example.invalid'}))await form.locator(`[name=${name}]`).fill(value)
 await form.getByRole('button',{name:'Save listing'}).click()
 await page.locator('.seller-listings').getByText('Browser test maize',{exact:true}).waitFor()
 await page.locator('.my-listings summary').click();await page.getByRole('button',{name:'Edit listing',exact:true}).click()
 await page.locator('.live-listing-form [name=active]').uncheck();await page.locator('.live-listing-form').getByRole('button',{name:'Save listing'}).click()
 await page.getByText('No active seller listings match your search.').waitFor()
 await page.getByRole('button',{name:'Close marketplace'}).click()
 await page.setViewportSize({width:390,height:844});await page.locator('.live-farm-map').scrollIntoViewIfNeeded();assert.ok(await page.locator('.live-farm-map').isVisible())
 assert.deepEqual(errors,[])
 console.log(JSON.stringify({passed:['selected farm weather','selected farm map coordinates','satellite configuration gate','manual weather refresh','automatic weather refresh','live GPS updates','GPS does not save automatically','explicit farm GPS save','stop GPS tracking','missing licensed feed state','seller create','seller availability update','mobile map visibility','no browser exceptions'],weatherCalls,apiRequests:calls.length}))
}catch(error){console.error(JSON.stringify({errors,gpsStatus:await page.locator('.live-location-status').textContent().catch(()=>null)}));throw error}finally{await browser.close()}
