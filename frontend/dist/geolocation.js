export function locationError(error) {
 if(error?.code===1)return 'Location permission was denied. Allow location for this site in your browser settings, then try again.'
 if(error?.code===2)return 'Your device could not determine its location. Check location services and try again.'
 if(error?.code===3)return 'Location request timed out. Move somewhere with a better signal and try again.'
 return error?.message || 'Live location is unavailable. Please try again.'
}
export function watchLiveLocation(onPosition,onError,geo=globalThis.navigator?.geolocation) {
 if(!globalThis.isSecureContext && typeof window!=='undefined'){onError(new Error('Live location requires HTTPS or localhost.'));return ()=>{}}
 if(!geo){onError(new Error('This browser does not support live location.'));return ()=>{}}
 let active=true
 const id=geo.watchPosition(position=>{
  if(!active)return
  const {latitude,longitude,accuracy}=position.coords
  if(!Number.isFinite(latitude)||!Number.isFinite(longitude)||Math.abs(latitude)>90||Math.abs(longitude)>180||!Number.isFinite(accuracy)||accuracy<0){onError(new Error('The device returned an invalid location. Please retry.'));return}
  onPosition({latitude,longitude,accuracy,timestamp:position.timestamp})
 },error=>{if(active)onError(error)},{enableHighAccuracy:true,maximumAge:0,timeout:20000})
 return ()=>{active=false;geo.clearWatch(id)}
}
