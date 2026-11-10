import assert from 'node:assert/strict'
import test from 'node:test'
import {watchLiveLocation,locationError} from '../src/geolocation.js'
test('live GPS requests fresh high accuracy fixes and stops callbacks on cleanup',()=>{
 let success,options,cleared,received=[]
 const geo={watchPosition(callback,error,settings){success=callback;options=settings;return 7},clearWatch(id){cleared=id}}
 const stop=watchLiveLocation(fix=>received.push(fix),error=>assert.fail(error.message),geo)
 assert.deepEqual(options,{enableHighAccuracy:true,maximumAge:0,timeout:20000})
 success({coords:{latitude:-27.75,longitude:29.93,accuracy:8},timestamp:123})
 assert.deepEqual(received,[{latitude:-27.75,longitude:29.93,accuracy:8,timestamp:123}])
 stop();assert.equal(cleared,7);success({coords:{latitude:-28,longitude:30,accuracy:10},timestamp:124});assert.equal(received.length,1)
})
test('invalid location and denied permission have clear errors',()=>{
 let fail,message
 const geo={watchPosition(success,error){fail=error;success({coords:{latitude:100,longitude:29,accuracy:5}});return 0},clearWatch(){}}
 watchLiveLocation(()=>assert.fail('Invalid fix must not be used'),error=>message=error.message,geo)
 assert.match(message,/invalid location/)
 fail({code:1});assert.match(locationError({code:1}),/permission was denied/)
 assert.match(locationError({code:2}),/could not determine/)
 assert.match(locationError({code:3}),/timed out/)
})
