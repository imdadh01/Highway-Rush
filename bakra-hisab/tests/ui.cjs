const {chromium}=require('playwright');
(async()=>{const browser=await chromium.launch({headless:true,args:['--no-sandbox']});const page=await browser.newPage({viewport:{width:393,height:852},deviceScaleFactor:1});const errors=[];page.on('pageerror',e=>errors.push(e.message));await page.goto('http://127.0.0.1:8765');
const click=async(a,id)=>page.locator(`[data-act="${a}"]${id?`[data-id="${id}"]`:''}`).first().click();const fill=async(n,v)=>page.locator(`[name="${n}"]`).fill(v);const save=async()=>{await click('preview');await click('confirm')};
await click('new-season');await fill('name','Eid 2027');await fill('opening','100000');await save();
await click('form','goat');await fill('cost','20000');await fill('paidByMe','20000');await save();
await click('form','expense');await fill('total','8000');await fill('share','4000');await fill('paidByMe','8000');await fill('title','Mulazim salary');await save();
await click('nav','partner');await page.getByText('Partner se lena',{exact:true}).waitFor();
await click('form','partner');await page.locator('[name="direction"]').selectOption('received');await fill('amount','4000');await save();
await click('nav','home');await click('form','sale');await fill('price','30000');await fill('received','10000');await fill('customer','Ahmed');await fill('phone','03001234567');await save();
await click('form','receipt');await fill('amount','20000');await save();
await page.reload();await page.getByRole('heading',{name:'Bakra Hisab',exact:true}).waitFor();
await page.screenshot({path:'tmp/app/home.png',fullPage:true});await click('nav','cash');await page.screenshot({path:'tmp/app/cash.png',fullPage:true});
for(const r of ['expenses','goats','reports','customers','employees','backup','settings']){await page.evaluate(r=>nav(r),r);if(await page.locator('body').evaluate(e=>e.scrollWidth>innerWidth))throw Error('Overflow '+r)}
const result=await page.evaluate(()=>({summary:E.summary(db,db.current),entries:E.ledger(db,db.current).length}));if(result.summary.profit!==600000||result.summary.partner!==0||result.summary.receivable!==0)throw Error(JSON.stringify(result));if(errors.length)throw Error(errors.join('\n'));console.log('UI PASS',JSON.stringify(result));await browser.close();})();
