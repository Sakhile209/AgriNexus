import assert from 'node:assert/strict'
import test from 'node:test'
import {extractVerificationToken} from '../src/email-verification.js'
test('verification links accept only the exact URL-safe token format',()=>{
 const raw='a'.repeat(43);assert.equal(extractVerificationToken('#verify-email='+raw),raw)
 for(const hash of ['#verify-email=short','#verify-email='+raw+'&email=other','#verify-email=<script>','?token='+raw])assert.equal(extractVerificationToken(hash),null)
})
