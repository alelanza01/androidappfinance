// Uses the approved prototype without changing its source. Fixture and state are exported for native QA.
const fs=require('fs'),path=require('path'),http=require('http'),assert=require('assert/strict');
const {chromium}=require('/opt/codex/runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const repo=path.resolve(__dirname,'../..'),root=path.join(repo,'design/ux-rebuild'),out=path.join(repo,'release/v3.4-rc2/prototype');
const server=http.createServer((req,res)=>{const f=path.resolve(root,'.'+new URL(req.url,'http://localhost').pathname);if(!f.startsWith(root+path.sep)){res.writeHead(403);return res.end()}fs.readFile(f,(e,b)=>{res.setHeader('Content-Type',({'.html':'text/html','.js':'text/javascript','.css':'text/css'})[path.extname(f)]||'application/octet-stream');res.end(e?'':b)})});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));const b=await chromium.launch({executablePath:'/usr/bin/chromium',args:['--no-sandbox']});const p=await b.newPage({viewport:{width:1100,height:1000},deviceScaleFactor:1,locale:'it-IT',timezoneId:'Europe/Rome'});await p.addInitScript(()=>{window.__DEMO_TODAY='2026-10-31'});await p.goto('http://127.0.0.1:'+server.address().port+'/index.html');
const state=await p.evaluate(()=>Prototype.getState());fs.writeFileSync(path.join(repo,'app/src/test/resources/rc2/fixture.json'),JSON.stringify({today:'2026-10-31',state},null,2)+'\n');
const manifest=[];
for(const theme of ['light','dark']){
 const reset=async()=>{await p.evaluate(t=>{Prototype.reset();Prototype.setTheme(t)},theme);await p.addStyleTag({content:'.device{width:360px!important;height:800px!important;border-radius:0!important;box-shadow:none!important}.mobile-demo{display:none!important}*{animation:none!important;transition:none!important}'})};
 const capture=async(name,view={})=>{await p.evaluate(()=>{document.activeElement?.blur();document.querySelector('#main').scrollTop=0;document.querySelector('#toast')?.classList.remove('visible')});await p.locator('#device').screenshot({path:path.join(out,`${name}-${theme}.png`)});manifest.push({name,theme,today:'2026-10-31',cash:120000,reserve:30000,commitments:38000,view})};
 for(const [name,dest]of [['home','home'],['settings','settings'],['advances','advances'],['analysis','analysis'],['movements','movements'],['commitments','commitments']]){await reset();await p.evaluate(d=>Prototype.go(d),dest);if(dest==='commitments')await p.evaluate(()=>{calendarMonth='2026-11';viewCalendar=false;render()});await capture(name,{page:dest,filter:dest==='movements'?'all/all':dest==='advances'?'open':null})}
 await reset();await p.evaluate(()=>{createMenu()});await capture('register',{underlying:'home'});
 await reset();await p.evaluate(()=>{newDraft('expense');draft.amount='600';draft.category=27;render()});await capture('expense',{amount:60000,category:27,count:1});
 await p.evaluate(()=>{rateChoice=3;rateMode12='total';draft.amountMode='total';page='rates';render()});await capture('rates-total',{amount:60000,count:3,mode:'total'});
 await reset();await p.evaluate(()=>{newDraft('expense');draft.amount='600';draft.category=27;rateChoice=3;rateMode12='each';draft.amountMode='each';page='rates';render()});await capture('rates-each',{amount:60000,count:3,mode:'each'});
 await reset();await p.evaluate(()=>{simValue='600';Prototype.go('simulator')});await capture('simulator',{simulation:60000,period:30,scenario:false});
 for(let step=0;step<4;step++){await reset();await p.evaluate(step=>{wizard12={step,amount:'1200',reserve:'300',items:[],committed:false};page='onboarding';render()},step);await capture('onboarding-'+(step+1),{step,amount:120000,reserve:30000,items:[]})}
}
fs.writeFileSync(path.join(out,'states.json'),JSON.stringify(manifest,null,2)+'\n');assert.equal(manifest.length,30);console.log('Fixture condivisa e 30 screenshot HTML approvati, data fissa 2026-10-31.');await b.close();server.close();})().catch(e=>{console.error(e);server.close();process.exit(1)});
