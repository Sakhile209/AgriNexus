import {escapeHtml as e} from './live-data.js'
export function extractVerificationToken(hash){const match=hash.match(/^#verify-email=([A-Za-z0-9_-]{43})$/);return match?.[1]||null}
export async function verificationRequest(path,body){
 const response=await fetch(`/api/v1/auth${path}`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body),cache:'no-store',credentials:'omit',referrerPolicy:'no-referrer'})
 const data=await response.json().catch(()=>({}))
 if(!response.ok){const fields=Object.entries(data.fieldErrors||{}).map(([name,message])=>`${name.replace(/([A-Z])/g,' $1')}: ${message}`).join('; ');const error=new Error(fields||data.message||'The request could not be completed. Please try again.');error.code=data.code;error.status=response.status;throw error}
 return data
}
let restoreRootInert=()=>{},overlay,cooldownTimer,redirectTimer,resendUntil=0,loginRequested=typeof location!=='undefined'&&location.hash==='#login'
function closeScreen(){clearInterval(cooldownTimer);clearTimeout(redirectTimer);overlay?.remove();overlay=null;restoreRootInert();restoreRootInert=()=>{}}
function openLogin(){closeScreen();history.replaceState(null,'',location.pathname+location.search+'#login');sessionStorage.removeItem('agrinexus.session');loginRequested=true;document.querySelector('.auth-modal .modal-close')?.click();document.querySelector('.side-logout')?.click();setTimeout(enhanceLogin,0)}
function shell(){closeScreen();overlay=document.createElement('div');overlay.className='modal-backdrop verification-backdrop';overlay.innerHTML='<section class="auth-card verification-card" role="dialog" aria-modal="true" aria-labelledby="verification-title"><button type="button" class="modal-close verification-close" aria-label="Close verification">×</button><span class="brand"><span class="brand-mark">🌱</span><strong>Agri<span>Nexus</span></strong></span><div class="verification-content"></div></section>';document.body.append(overlay);const root=document.querySelector('#root');if(root){const previous=root.inert;root.inert=true;restoreRootInert=()=>{root.inert=previous}}overlay.querySelector('.verification-close').onclick=closeScreen;overlay.querySelector('.verification-close').focus();return overlay.querySelector('.verification-content')}
function bindLogin(target){target.querySelectorAll('.verification-login').forEach(button=>button.onclick=openLogin)}
function resendScreen(email='',message='Verify your email address',confirmation=false){
 const target=shell()
 target.innerHTML=`<div class="verification-envelope" aria-hidden="true">✉</div><h2 id="verification-title">${confirmation?'Check your email':e(message)}</h2><p>${confirmation?'Registration successful! Please check your email to verify your account.':'Enter your email to request a new verification link. If an unverified account exists, a verification email will be sent.'}</p><p class="verification-expires">Verification links expire after 15 minutes. Use the most recent email and check your spam folder.</p><form class="verification-resend-form"><label>Email<input type="email" name="email" autocomplete="email" maxlength="254" required value="${e(email)}"></label><p class="verification-message" role="status" aria-live="polite"></p><button type="submit" class="primary verification-resend">Resend Verification Email</button></form><button type="button" class="link verification-login">Back to Log In</button>`
 bindLogin(target)
 let resendBusy=false
 const button=target.querySelector('.verification-resend'),status=target.querySelector('.verification-message')
 const update=()=>{const left=Math.max(0,Math.ceil((resendUntil-Date.now())/1000));button.disabled=resendBusy||left>0;button.textContent=resendBusy?'Requesting…':left?`Resend available in ${left}s`:'Resend Verification Email'}
 update();cooldownTimer=setInterval(update,1000)
 target.querySelector('form').onsubmit=async event=>{
  event.preventDefault();if(resendBusy||resendUntil>Date.now())return
  resendBusy=true;button.disabled=true;button.textContent='Requesting…';status.textContent=''
  try{const result=await verificationRequest('/verification/resend',{email:target.querySelector('[name=email]').value.trim()});status.textContent=result.message;resendUntil=Date.now()+60000}
  catch(error){status.textContent=error.message;if(error.status===429)resendUntil=Date.now()+60000}
  finally{resendBusy=false;if(target.isConnected)update()}
 }
 target.querySelector('[name=email]').focus()
}
async function confirmToken(token){
 const target=shell();target.innerHTML='<div class="verification-envelope" aria-hidden="true">◌</div><h2 id="verification-title">Verifying your email…</h2><p role="status">Please wait while we check your verification link.</p>'
 try{
  const result=await verificationRequest('/verification/confirm',{token});if(!target.isConnected)return
  target.innerHTML=`<div class="verification-envelope verified" aria-hidden="true">✓</div><h2 id="verification-title">Email verified</h2><p role="status">${e(result.message)}</p><button type="button" class="primary verification-login">Log In to AgriNexus</button><p class="verification-expires">Taking you to login in a moment…</p>`
  bindLogin(target);redirectTimer=setTimeout(openLogin,4000)
 }catch(error){
  if(!target.isConnected)return
  const expired=error.code==='VERIFICATION_EXPIRED',retryable=error.status>=500||!error.status
  target.innerHTML=`<div class="verification-envelope" aria-hidden="true">${expired?'◷':'!'}</div><h2 id="verification-title">${expired?'Verification link expired':retryable?'Could not verify email':'Invalid verification link'}</h2><p role="alert">${e(error.message)}</p>${retryable?'<button class="primary verification-retry">Retry verification</button>':''}<button type="button" class="primary verification-new">Resend Verification Email</button><button type="button" class="link verification-login">Back to Log In</button>`
  target.querySelector('.verification-new').onclick=()=>resendScreen()
  target.querySelector('.verification-retry')?.addEventListener('click',()=>confirmToken(token));bindLogin(target)
 }
}
function processLink(){
 if(!location.hash.startsWith('#verify-email='))return
 const token=extractVerificationToken(location.hash)
 // Remove the raw token immediately; it is never stored in session/local storage.
 history.replaceState(null,'',location.pathname+location.search+'#email-verification')
 if(token)confirmToken(token);else resendScreen('','Invalid verification link')
}
function enhanceLogin(){
 if(loginRequested&&document.querySelector('.landing-actions .outline')){loginRequested=false;document.querySelector('.landing-actions .outline').click()}
 document.querySelectorAll('.auth-modal .auth-card form').forEach(form=>{
  const registration=!!form.querySelector('[name=firstName]')
  form.querySelectorAll('input').forEach(input=>{input.required=true})
  const existing=form.parentElement.querySelector('.verification-login-resend')
  if(registration){existing?.remove();return}
  if(existing)return
  const button=document.createElement('button');button.type='button';button.className='link verification-login-resend';button.textContent='Resend Verification Email';button.onclick=()=>resendScreen(form.querySelector('[name=email]')?.value||'');form.after(button)
 })
}
if(typeof document!=='undefined'){
 document.addEventListener('submit',async event=>{
  const form=event.target
  if(!(form instanceof HTMLFormElement)||!form.matches('.auth-modal .auth-card form')||!form.querySelector('[name=firstName]'))return
  event.preventDefault();event.stopImmediatePropagation()
  if(!form.reportValidity())return
  const button=form.querySelector('button[type=submit],button.primary'),fields=Object.fromEntries(new FormData(form))
  if(button.disabled)return
  button.disabled=true;button.textContent='Please wait…'
  let notice=form.querySelector('.verification-registration-error');if(!notice){notice=document.createElement('p');notice.className='error verification-registration-error';notice.setAttribute('role','alert');button.before(notice)}notice.textContent=''
  try{
   const result=await verificationRequest('/register',fields)
   // A registration response is never passed to the existing React login callback.
   if(result.verificationRequired!==true)throw new Error('Registration could not be confirmed. Please try again.')
   form.querySelector('[name=password]').value='';resendUntil=Date.now()+60000
   document.querySelector('.auth-modal .modal-close')?.click();resendScreen(fields.email,'',true)
  }catch(error){if(form.isConnected)notice.textContent=error.message}
  finally{if(form.isConnected){button.disabled=false;button.textContent='Create account'}}
 },true)
 document.addEventListener('keydown',event=>{if(event.key==='Escape'&&overlay)closeScreen()})
 new MutationObserver(enhanceLogin).observe(document.body,{childList:true,subtree:true})
 window.addEventListener('hashchange',processLink);enhanceLogin();processLink()
}
