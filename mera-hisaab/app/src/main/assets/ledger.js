'use strict';
const Ledger=(()=>{
const sources=['side','business','opening','borrowed'];
const sum=o=>Object.values(o).reduce((a,b)=>a+b,0);
const clone=o=>JSON.parse(JSON.stringify(o));
const zero=()=>Object.fromEntries(sources.map(x=>[x,0]));
function fail(s){throw new Error(s);}
function integer(n){if(!Number.isSafeInteger(n)||Math.abs(n)>9e12)fail('Invalid amount / Raqam durust nahi');}
function take(pool,n,first,second){integer(n);let a=zero(),left=n;first=sources.includes(first)?first:'business';for(const s of [...new Set([first,second,'side','opening','borrowed','business'])].filter(s=>sources.includes(s))){a[s]=Math.min(Math.max(0,pool[s]),left);left-=a[s];}a[first]+=left;return a;}
function slice(pool,n){let a=zero(),left=n; for(const s of sources){a[s]=Math.min(pool[s]||0,left);left-=a[s];}if(left)fail('Principal allocation error');return a;}
function replay(p){
 let b={sources:zero(),wallets:Object.create(null),investments:Object.create(null),sales:Object.create(null),debts:Object.create(null),events:[],bookedProfit:0};
 for(const w of p.wallets)b.wallets[w.id]=0;
 function cash(w,n){if(!(w in b.wallets))fail('Account missing / Account nahi mila');integer(n);b.wallets[w]+=n;integer(b.wallets[w]);}
 function expense(t){let a=zero(),left=t.amount;let priority=t.autoFunding?[]:[t.source,t.second];for(const s of [...new Set([...priority,'side','opening','borrowed','business'])].filter(s=>sources.includes(s))){a[s]=Math.min(Math.max(0,b.sources[s]),left);left-=a[s];}a.side+=left;for(const s of sources)b.sources[s]-=a[s];cash(t.wallet,-t.amount);}
 function funding(t,n){let preferred=sources.includes(t.source)?t.source:t.type==='invest'?'business':t.type==='debtPayment'?'borrowed':'side';let a=take(b.sources,n,preferred,t.second);for(const s of sources)b.sources[s]-=a[s];cash(t.wallet,-n);return a;}
 function incoming(w,a){for(const s of sources)b.sources[s]+=a[s]||0;cash(w,sum(a));}
 function receiveSale(s,n,w,e){if(n>s.total-s.paid)fail('Payment exceeds amount due / Baqi raqam se zyada payment');let principal=Math.min(n,Math.max(0,s.cost-s.paid));let used=slice(s.remaining,principal);for(const k of sources)s.remaining[k]-=used[k];used.business+=n-principal;incoming(w,used);s.paid+=n;e.income+=n-principal;e.businessIncome+=n-principal;}
 const ts=p.transactions.slice().sort((a,c)=>a.date.localeCompare(c.date)||a.order-c.order);
 const ids=new Set();
 for(const t of ts){
  if(!/^[a-zA-Z0-9_-]{1,80}$/.test(t.id)||ids.has(t.id)||!/^\d{4}-\d{2}-\d{2}$/.test(t.date)||!Number.isFinite(t.order))fail('Invalid transaction');ids.add(t.id);
  integer(t.amount);if(t.amount<0)fail('Negative amount');
  let e={...t,income:0,expense:0,businessIncome:0,booked:0};
  switch(t.type){
   case 'opening': if(!sources.includes(t.source))fail('Invalid opening source');b.sources[t.source]+=t.amount;cash(t.wallet,t.amount);break;
   case 'openingInvestment': b.investments[t.id]={id:t.id,note:t.note,date:t.date,cost:t.amount,remaining:t.amount,alloc:{...zero(),opening:t.amount}};break;
   case 'income': if(!['side','business'].includes(t.source))fail('Invalid income source');b.sources[t.source]+=t.amount;cash(t.wallet,t.amount);e.income=t.amount;e.businessIncome=t.source==='business'?t.amount:0;break;
   case 'expense':expense(t);e.expense=t.amount;break;
   case 'invest': b.investments[t.id]={id:t.id,note:t.note,date:t.date,cost:t.amount,remaining:t.amount,alloc:funding(t,t.amount)};break;
   case 'sale':{
    let lot=b.investments[t.link];if(!lot)fail('Original investment must come first / Pehle investment ki entry honi chahiye');integer(t.cost);integer(t.paid);
    if(t.cost<=0||t.cost>lot.remaining||t.paid<0||t.paid>t.amount)fail('Invalid sale cost/payment / Sale ki lagat ya payment durust nahi');
    let a=slice(lot.alloc,t.cost);for(const s of sources)lot.alloc[s]-=a[s];lot.remaining-=t.cost;
    let sale={id:t.id,person:t.person,note:t.note,total:t.amount,cost:t.cost,paid:0,remaining:a,investment:t.link};b.sales[t.id]=sale;receiveSale(sale,t.paid,t.wallet,e);e.booked=t.amount-t.cost;b.bookedProfit+=e.booked;break;
   }
   case 'receiveSale':{let s=b.sales[t.link];if(!s)fail('Sale missing / Sale nahi mili');receiveSale(s,t.amount,t.wallet,e);break;}
   case 'transfer': if(t.wallet===t.to)fail('Choose two different accounts / Do alag accounts chunein');cash(t.wallet,-t.amount);cash(t.to,t.amount);break;
   case 'lend': b.debts[t.id]={id:t.id,kind:'lend',person:t.person,total:t.amount,paid:0,alloc:funding(t,t.amount),note:t.note};break;
   case 'borrow': b.sources.borrowed+=t.amount;cash(t.wallet,t.amount);b.debts[t.id]={id:t.id,kind:'borrow',person:t.person,total:t.amount,paid:0,note:t.note};break;
   case 'debtPayment':{
    let d=b.debts[t.link];if(!d||t.amount>d.total-d.paid)fail('Invalid debt payment / Udhaar ki payment durust nahi');
    if(d.kind==='lend'){let a=slice(d.alloc,t.amount);for(const s of sources)d.alloc[s]-=a[s];incoming(t.wallet,a);}else funding(t,t.amount);d.paid+=t.amount;break;
   }
   default:fail('Unknown entry type');
  }
  if(sum(b.sources)!==sum(b.wallets))fail('Ledger is out of balance');
  b.events.push(e);
 }
 b.available=sum(b.wallets);b.invested=Object.values(b.investments).reduce((n,x)=>n+x.remaining,0);
 b.receivable=Object.values(b.sales).reduce((n,x)=>n+x.total-x.paid,0)+Object.values(b.debts).filter(x=>x.kind==='lend').reduce((n,x)=>n+x.total-x.paid,0);
 b.payable=Object.values(b.debts).filter(x=>x.kind==='borrow').reduce((n,x)=>n+x.total-x.paid,0);
 b.capital=b.available+b.invested+b.receivable-b.payable;return b;
}
function validate(db){
 if(!db||db.version!==1||!Array.isArray(db.profiles)||db.profiles.length>100||!['en','ru'].includes(db.language))fail('Unsupported backup / Backup durust nahi');
 let ids=new Set();for(const p of db.profiles){if(!/^[a-zA-Z0-9_-]{1,80}$/.test(p.id)||ids.has(p.id)||typeof p.name!=='string'||!Array.isArray(p.wallets)||!Array.isArray(p.transactions)||p.transactions.length>100000)fail('Invalid profile');ids.add(p.id);if(!p.wallets.some(w=>w.id==='general'))fail('General account missing');let ws=new Set();for(const w of p.wallets){if(!/^[a-zA-Z0-9_-]{1,80}$/.test(w.id)||['__proto__','constructor','prototype'].includes(w.id)||ws.has(w.id)||typeof w.name!=='string')fail('Invalid wallet');ws.add(w.id);}replay(p);}if(db.profiles.length&&!ids.has(db.active))fail('Active profile missing');return db;
}
return {sources,sum,clone,zero,replay,validate};
})();
