/* Date-only arithmetic uses UTC components, never UTC conversion of local midnight. */
const Dates={
 iso:(y,m,d)=>`${String(y).padStart(4,'0')}-${String(m).padStart(2,'0')}-${String(d).padStart(2,'0')}`,
 today(){if(window.__DEMO_TODAY)return window.__DEMO_TODAY;const d=new Date();return this.iso(d.getFullYear(),d.getMonth()+1,d.getDate());},
 valid(s){if(!/^\d{4}-\d{2}-\d{2}$/.test(s))return false;const [y,m,d]=s.split('-').map(Number),v=new Date(Date.UTC(y,m-1,d));return y>=1900&&y<=2200&&v.getUTCFullYear()===y&&v.getUTCMonth()===m-1&&v.getUTCDate()===d;},
 add(s,n){const d=new Date(s+'T12:00:00Z');d.setUTCDate(d.getUTCDate()+n);return this.iso(d.getUTCFullYear(),d.getUTCMonth()+1,d.getUTCDate());},
 diff:(a,b)=>Math.round((Date.parse(b+'T12:00:00Z')-Date.parse(a+'T12:00:00Z'))/86400000),
 month(s,n){const [y,m,day]=s.split('-').map(Number),d=new Date(Date.UTC(y,m-1+n,1)),last=new Date(Date.UTC(d.getUTCFullYear(),d.getUTCMonth()+1,0)).getUTCDate();return this.iso(d.getUTCFullYear(),d.getUTCMonth()+1,Math.min(day,last));},
 label(s,year=false){if(!this.valid(s))return 'Scegli data';return new Intl.DateTimeFormat('it-IT',{day:'numeric',month:'short',...(year?{year:'numeric'}:{}),timeZone:'UTC'}).format(new Date(s+'T12:00:00Z'));},
 grid(month){const [y,m]=month.split('-').map(Number),first=new Date(Date.UTC(y,m-1,1)),offset=(first.getUTCDay()+6)%7,days=new Date(Date.UTC(y,m,0)).getUTCDate();return {offset,days,dates:Array.from({length:days},(_,i)=>this.iso(y,m,i+1))};}
};
