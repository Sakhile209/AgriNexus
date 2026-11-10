// Browser-only verification fixtures. No email is sent and no real account is created.
import assert from 'node:assert/strict'
const {chromium}=await import(process.env.PLAYWRIGHT_MODULE||'playwright')
const browser=await chromium.launch({headless:true,executablePath:process.env.CHROMIUM_PATH,args:['--no-sandbox']})
const page=await browser.newPage({viewport:{width:1280,height:900}}),errors=[];page.on('pageerror',error=>errors.push(error.message))
let mode='pending',confirm='success',calls=[]
await page.route('**/api/v1/**',async route=>{
 const path=new URL(route.request().url()).pathname;let status=200,body=[];calls.push({path,method:route.request().method()})
 if(path.endsWith('/auth/register')){status=mode==='failure'?503:202;body=mode==='failure'?{code:'EMAIL_UNAVAILABLE',message:'Email verification is temporarily unavailable. Please try again later.'}:{message:'Registration successful! Please check your email to verify your account.',verificationRequired:true,resendAfterSeconds:60}}
 if(path.endsWith('/auth/login')){status=403;body={code:'EMAIL_NOT_VERIFIED',message:'Please verify your email address before logging in.'}}
 if(path.endsWith('/verification/resend'))body={message:'If an unverified account exists for this email address, a verification email will be sent. Please check your inbox and spam folder.',verificationRequired:true,resendAfterSeconds:60}
 if(path.endsWith('/verification/confirm')){status=confirm==='success'?200:confirm==='expired'?410:400;body=confirm==='success'?{message:'Email verified successfully! You can now log in to AgriNexus.'}:confirm==='expired'?{code:'VERIFICATION_EXPIRED',message:'This verification link has expired. Request a new verification email.'}:{code:'VERIFICATION_INVALID',message:'This verification link is invalid or has already been used.'}}
 await route.fulfill({status,contentType:'application/json',body:JSON.stringify(body)})
})
const base=process.env.AGRINEXUS_WEB_URL||'http://localhost:5174/'
try{
 await page.clock.install({time:Date.now()});await page.goto(base)
 await page.locator('.landing-actions .primary').click()
 const form=page.locator('.auth-modal form')
 for(const [field,value]of Object.entries({firstName:'Test',lastName:'Farmer',phoneNumber:'+27000000000',email:'farmer@example.invalid',password:'test-Password-12345'}))await form.locator(`[name=${field}]`).fill(value)
 await form.getByRole('button',{name:'Create account',exact:true}).click()
 await page.getByRole('heading',{name:'Check your email',exact:true}).waitFor();assert.equal(await page.evaluate(()=>sessionStorage.getItem('agrinexus.session')),null);assert.equal(calls.filter(c=>c.path.endsWith('/farms')).length,0)
 assert.equal(await page.locator('.verification-resend').isDisabled(),true)
 await page.clock.fastForward(60001);await page.getByRole('button',{name:'Resend Verification Email',exact:true}).click();await page.getByText('If an unverified account exists for this email address, a verification email will be sent.',{exact:false}).waitFor();assert.equal(await page.locator('.verification-resend').isDisabled(),true)
 await page.getByRole('button',{name:'Back to Log In',exact:true}).click();await page.getByRole('heading',{name:'Welcome back'}).waitFor()
 await page.locator('.auth-modal [name=email]').fill('farmer@example.invalid');await page.locator('.auth-modal [name=password]').fill('test-Password-12345');await page.locator('.auth-modal form').getByRole('button',{name:'Log in',exact:true}).click();await page.getByText('Please verify your email address before logging in.',{exact:true}).waitFor();assert.equal(await page.locator('.verification-login-resend').count(),1)
 const raw='a'.repeat(43);await page.goto(base+'#verify-email='+raw);await page.getByRole('heading',{name:'Email verified',exact:true}).waitFor();assert.equal(await page.evaluate(()=>location.hash),'#email-verification');assert.equal(await page.evaluate(()=>sessionStorage.getItem('agrinexus.session')),null)
 await page.clock.fastForward(4001);await page.getByRole('heading',{name:'Welcome back'}).waitFor()
 confirm='expired';await page.goto(base+'#verify-email='+raw);await page.getByRole('heading',{name:'Verification link expired',exact:true}).waitFor();await page.locator('.verification-new').click();await page.locator('.verification-resend-form [name=email]').waitFor()
 await page.setViewportSize({width:390,height:844});assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true);await page.screenshot({path:'/tmp/agrinexus-check-email-mobile.png',fullPage:true})
 confirm='invalid';await page.goto(base+'#verify-email='+raw);await page.getByRole('heading',{name:'Invalid verification link',exact:true}).waitFor()
 mode='failure';await page.goto(base);await page.locator('.landing-actions .primary').click();const retryForm=page.locator('.auth-modal form');for(const [field,value]of Object.entries({firstName:'Test',lastName:'Farmer',phoneNumber:'+27000000000',email:'farmer@example.invalid',password:'test-Password-12345'}))await retryForm.locator(`[name=${field}]`).fill(value);await retryForm.getByRole('button',{name:'Create account'}).click();await page.getByText('Email verification is temporarily unavailable. Please try again later.',{exact:true}).waitFor();assert.equal(await page.locator('.verification-backdrop').count(),0)
 assert.deepEqual(errors,[]);console.log(JSON.stringify({passed:['pending registration without login','60-second cooldown','generic resend confirmation','unverified login with resend','valid verification and token removed from URL','success redirects to login','expired link and resend','invalid link','mobile verification layout','email-service configuration failure','no browser exceptions']}))
}catch(error){console.error({errors});await page.screenshot({path:'/tmp/verification-ui-failure.png',fullPage:true});throw error}finally{await browser.close()}
