'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');const {takeApiKey,authEnvironment}=require('../../app/src/main/assets/gemini-control.js');
const root=require('./fixture-paths.cjs').fixture('gkey'),id='11111111-1111-4111-8111-111111111111',socket=path.join(root,'run.sock'),dir=path.join(root,'keys');fs.mkdirSync(dir,{mode:0o700});const file=path.join(dir,id+'.key'),secret='synthetic-test-key-not-real';
fs.writeFileSync(file,secret,{mode:0o600});assert.equal(takeApiKey(file,socket),secret);assert(!fs.existsSync(file));assert.equal(takeApiKey('',socket),'');
fs.writeFileSync(file,'invalid\nkey',{mode:0o600});assert.throws(()=>takeApiKey(file,socket));assert(!fs.existsSync(file));
const external=path.join(root,'outside');fs.writeFileSync(external,secret);fs.symlinkSync(external,file);assert.throws(()=>takeApiKey(file,socket));assert.equal(fs.readFileSync(external,'utf8'),secret);fs.unlinkSync(file);
assert.throws(()=>takeApiKey(external,socket));const auth=path.join(root,'auth',id+'.json');const c=authEnvironment('api',auth,{GEMINI_API_KEY:secret});assert.equal(c.env.GEMINI_API_KEY,secret);assert(!fs.readFileSync(auth,'utf8').includes(secret));c.cleanup();
console.log('PASS Gemini key handoff: explicit private file, exact directory/UUID, one-time read+delete, control rejection/delete, symlink/foreign-path refusal, no key in CLI settings; synthetic key only');
