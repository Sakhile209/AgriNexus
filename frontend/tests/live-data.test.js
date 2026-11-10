import assert from 'node:assert/strict'
import test from 'node:test'
import {coordinatePair,refreshDelay,escapeHtml,display,preferredMapLayer} from '../src/live-data.js'
test('coordinates require an actual valid saved pair, including valid zero values',()=>{
 assert.equal(coordinatePair({latitude:-27.75,longitude:29.93}),true)
 assert.equal(coordinatePair({latitude:0,longitude:0}),true)
 for(const farm of [{},{latitude:null,longitude:29},{latitude:91,longitude:29},{latitude:-27,longitude:181},{latitude:'invalid',longitude:29}])assert.equal(coordinatePair(farm),false)
})
test('automatic refresh respects provider scheduling and enforces a minimum delay',()=>{
 assert.equal(refreshDelay(new Date(Date.now()-10000).toISOString()),60000)
 const next=refreshDelay(new Date(Date.now()+1800000).toISOString());assert.ok(next>1799000&&next<=1800000)
 assert.equal(refreshDelay(new Date(Date.now()+86400000).toISOString()),3600000)
})
test('missing weather values remain missing and seller content is escaped',()=>{
 assert.equal(display(null,'%'),'Not available')
 assert.equal(display(0,' mm'),'0 mm')
 assert.equal(escapeHtml('<img onerror="alert(1)">'),'&lt;img onerror=&quot;alert(1)&quot;&gt;')
})

test('satellite is preferred when configured and the chosen standard view is respected',()=>{
 assert.equal(preferredMapLayer(true,null),'satellite')
 assert.equal(preferredMapLayer(true,'satellite'),'satellite')
 assert.equal(preferredMapLayer(true,'standard'),'standard')
 assert.equal(preferredMapLayer(false,'satellite'),'standard')
})
