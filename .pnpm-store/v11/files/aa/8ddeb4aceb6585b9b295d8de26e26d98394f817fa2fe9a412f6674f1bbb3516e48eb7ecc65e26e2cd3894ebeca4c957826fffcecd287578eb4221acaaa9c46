var e=class extends Error{constructor(e){super(e),this.name=`Eta Error`}},t=class extends e{constructor(e){super(e),this.name=`EtaParser Error`}},n=class extends e{constructor(e){super(e),this.name=`EtaRuntime Error`}},r=class extends e{constructor(e){super(e),this.name=`EtaFileResolution Error`}},i=class extends e{constructor(e){super(e),this.name=`EtaNameResolution Error`}};function a(e,n,r){let i=n.slice(0,r).split(/\n/),a=i.length,o=i[a-1].length+1;throw e+=` at line `+a+` col `+o+`:

  `+n.split(/\n/)[a-1]+`
  `+Array(o).join(` `)+`^`,new t(e)}function o(e,t,r,i){let a=t.split(`
`),o=Math.max(r-3,0),s=Math.min(a.length,r+3),c=i,l=a.slice(o,s).map((e,t)=>{let n=t+o+1;return(n===r?` >> `:`    `)+n+`| `+e}).join(`
`),u=new n((c?c+`:`+r+`
`:`line `+r+`
`)+l+`

`+e.message);throw u.name=e.name,u.cause=e,u}const s=(async()=>{}).constructor;function c(e,n){let r=this.config,i=n?.async?s:Function;try{return new i(r.varName,`options`,this.compileToString.call(this,e,n))}catch(r){throw r instanceof SyntaxError?new t(`Bad template syntax

`+r.message+`
`+Array(r.message.length+1).join(`=`)+`
`+this.compileToString.call(this,e,n)+`
`):r}}function l(e,t){let n=this.config,r=t?.async,i=this.compileBody,a=this.parse.call(this,e),o=`${n.functionHeader}
let include = (__eta_t, __eta_d) => this.render(__eta_t, {...${n.varName}, ...(__eta_d ?? {})}, options);
let includeAsync = (__eta_t, __eta_d) => this.renderAsync(__eta_t, {...${n.varName}, ...(__eta_d ?? {})}, options);

let __eta = {res: "", e: this.config.escapeFunction, f: this.config.filterFunction, blocks: {}${n.debug?`, line: 1, templateStr: "`+e.replace(/\\|"/g,`\\$&`).replace(/\r\n|\n|\r/g,`\\n`)+`"`:``}};

function layout(path, data) {
  __eta.layout = path;
  __eta.layoutData = data;
}${n.debug?`try {`:``}${n.useWith?`with(`+n.varName+`||{}){`:``}

function ${n.outputFunctionName}(s){__eta.res+=s;}
function capture(fn){const s=__eta.res;__eta.res='';try{fn();return __eta.res}finally{__eta.res=s;}}
async function captureAsync(fn){const s=__eta.res;__eta.res='';try{await fn();return __eta.res}finally{__eta.res=s;}}
function block(name,fn){if(__eta.layout){if(fn){__eta.blocks[name]=capture(fn);}return '';}const b=${n.varName}.__blocks||{};if(name in b){return b[name];}return fn?capture(fn):'';}
async function blockAsync(name,fn){if(__eta.layout){if(fn){__eta.blocks[name]=await captureAsync(fn);}return '';}const b=${n.varName}.__blocks||{};if(name in b){return b[name];}return fn?await captureAsync(fn):'';}

${i.call(this,a)}
if (__eta.layout) {
  __eta.res = ${r?`await includeAsync`:`include`} (__eta.layout, {...${n.varName}, body: __eta.res, ...__eta.layoutData, __blocks: __eta.blocks});
}
${n.useWith?`}`:``}${n.debug?`} catch (e) { this.RuntimeErr(e, __eta.templateStr, __eta.line, options.filepath) }`:``}
return __eta.res;
`;if(n.plugins)for(let e=0;e<n.plugins.length;e++){let t=n.plugins[e];t.processFnString&&(o=t.processFnString(o,n))}return o}function u(e){let t=this.config,n=0,r=e.length,i=``;for(;n<r;n++){let r=e[n];if(typeof r==`string`)i+=`__eta.res+='`+r+`';
`;else{let e=r.t,n=r.val||``;t.debug&&(i+=`__eta.line=`+r.lineNo+`
`),e===`r`?(t.autoFilter&&(n=`__eta.f(`+n+`)`),i+=`__eta.res+=`+n+`;
`):e===`i`?(t.autoFilter&&(n=`__eta.f(`+n+`)`),t.autoEscape&&(n=`__eta.e(`+n+`)`),i+=`__eta.res+=`+n+`;
`):e===`e`?i+=n+`
`:Object.hasOwn(t.customTags,e)&&(i+=`__eta.res+=this.config.customTags[${JSON.stringify(e)}](${JSON.stringify(n)},${t.varName});\n`)}}return i}function d(e,t,n,r){let i,a;return Array.isArray(t.autoTrim)?(i=t.autoTrim[1],a=t.autoTrim[0]):i=a=t.autoTrim,(n||n===!1)&&(i=n),(r||r===!1)&&(a=r),!a&&!i?e:i===`slurp`&&a===`slurp`?e.trim():(i===`_`||i===`slurp`?e=e.trimStart():(i===`-`||i===`nl`)&&(e=e.replace(/^(?:\r\n|\n|\r)/,``)),a===`_`||a===`slurp`?e=e.trimEnd():(a===`-`||a===`nl`)&&(e=e.replace(/(?:\r\n|\n|\r)$/,``)),e)}const f={"&":`&amp;`,"<":`&lt;`,">":`&gt;`,'"':`&quot;`,"'":`&#39;`};function p(e){return f[e]}function m(e){let t=String(e);return/[&<>"']/.test(t)?t.replace(/[&<>"']/g,p):t}const h={autoEscape:!0,autoFilter:!1,autoTrim:[!1,`nl`],cache:!1,cacheFilepaths:!0,customTags:{},debug:!1,escapeFunction:m,filterFunction:e=>String(e),outputFunctionName:`output`,functionHeader:``,parse:{exec:``,interpolate:`=`,raw:`~`},plugins:[],rmWhitespace:!1,tags:[`<%`,`%>`],useWith:!1,varName:`it`,defaultExtension:`.eta`},g=/`(?:\\[\s\S]|\${(?:[^{}]|{(?:[^{}]|{[^}]*})*})*}|(?!\${)[^\\`])*`/g,_=/'(?:\\[\s\w"'\\`]|[^\n\r'\\])*?'/g,v=/"(?:\\[\s\w"'\\`]|[^\n\r"\\])*?"/g;function y(e){return e.replace(/[.*+\-?^${}()|[\]\\]/g,`\\$&`)}function b(e,t){return e.slice(0,t).split(`
`).length}function x(e){let t=this.config,n=[],r=!1,i=0,o=t.parse,s=Object.keys(t.customTags);if(t.plugins)for(let n=0;n<t.plugins.length;n++){let r=t.plugins[n];r.processTemplate&&(e=r.processTemplate(e,t))}t.rmWhitespace&&(e=e.replace(/[\r\n]+/g,`
`).replace(/^\s+|\s+$/gm,``)),g.lastIndex=0,_.lastIndex=0,v.lastIndex=0;function c(e,i){e&&(e=d(e,t,r,i),e&&(e=e.replace(/\\|'/g,`\\$&`).replace(/\r\n|\n|\r/g,`\\n`),n.push(e)))}let l=[o.exec,o.interpolate,o.raw,...s].reduce((e,t)=>e&&t?e+`|`+y(t):t?y(t):e,``),u=RegExp(y(t.tags[0])+`(-|_)?\\s*(`+l+`)?\\s*`,`g`),f=RegExp(`'|"|\`|\\/\\*|(\\s*(-|_)?`+y(t.tags[1])+`)`,`g`),p;for(;p=u.exec(e);){let l=e.slice(i,p.index);i=p[0].length+p.index;let d=p[1],m=p[2]||``;c(l,d),f.lastIndex=i;let h,y=!1;for(;h=f.exec(e);)if(h[1]){let t=e.slice(i,h.index);u.lastIndex=i=f.lastIndex,r=h[2],y={t:m===o.exec?`e`:m===o.raw?`r`:m===o.interpolate?`i`:s.includes(m)?m:``,val:t};break}else{let t=h[0];if(t===`/*`){let t=e.indexOf(`*/`,f.lastIndex);t===-1&&a(`unclosed comment`,e,h.index),f.lastIndex=t}else t===`'`?(_.lastIndex=h.index,_.exec(e)?f.lastIndex=_.lastIndex:a(`unclosed string`,e,h.index)):t===`"`?(v.lastIndex=h.index,v.exec(e)?f.lastIndex=v.lastIndex:a(`unclosed string`,e,h.index)):t==="`"&&(g.lastIndex=h.index,g.exec(e)?f.lastIndex=g.lastIndex:a(`unclosed string`,e,h.index))}y?(t.debug&&(y.lineNo=b(e,p.index)),n.push(y)):a(`unclosed tag`,e,p.index)}if(c(e.slice(i,e.length),!1),t.plugins)for(let e=0;e<t.plugins.length;e++){let r=t.plugins[e];r.processAST&&(n=r.processAST(n,t))}return n}function S(e,t){let n=t?.async?this.templatesAsync:this.templatesSync;if(this.resolvePath&&this.readFile&&!e.startsWith(`@`)){let e=t.filepath,r=n.get(e);if(this.config.cache&&r)return r;{let r=this.readFile(e),i=this.compile(r,t);return this.config.cache&&n.define(e,i),i}}else{let t=n.get(e);if(t)return t;throw new i(`Failed to get template '${e}'`)}}function C(e,t,n){let r,i={...n,async:!1};return typeof e==`string`?(this.resolvePath&&this.readFile&&!e.startsWith(`@`)&&(i.filepath=this.resolvePath(e,i)),r=S.call(this,e,i)):r=e,r.call(this,t,i)}function w(e,t,n){let r,i={...n,async:!0};typeof e==`string`?(this.resolvePath&&this.readFile&&!e.startsWith(`@`)&&(i.filepath=this.resolvePath(e,i)),r=S.call(this,e,i)):r=e;let a=r.call(this,t,i);return Promise.resolve(a)}function T(e,t){let n=this.compile(e,{async:!1});return C.call(this,n,t)}function E(e,t){let n=this.compile(e,{async:!0});return w.call(this,n,t)}var D=class{constructor(e){this.cache=e}define(e,t){this.cache[e]=t}get(e){return this.cache[e]}remove(e){delete this.cache[e]}reset(){this.cache={}}load(e){this.cache={...this.cache,...e}}},O=class{constructor(t){t?this.config={...h,...t}:this.config={...h};let n=[this.config.parse.exec,this.config.parse.interpolate,this.config.parse.raw,`-`,`_`];for(let t of Object.keys(this.config.customTags))if(n.includes(t))throw new e(`Custom tag prefix "${t}" conflicts with a built-in prefix`)}config;RuntimeErr=o;compile=c;compileToString=l;compileBody=u;parse=x;render=C;renderAsync=w;renderString=T;renderStringAsync=E;filepathCache={};templatesSync=new D({});templatesAsync=new D({});resolvePath=null;readFile=null;configure(e){this.config={...this.config,...e}}withConfig(e){return{...this,config:{...this.config,...e}}}loadTemplate(e,t,n){if(typeof t==`string`)(n?.async?this.templatesAsync:this.templatesSync).define(e,this.compile(t,n));else{let r=this.templatesSync;(t.constructor.name===`AsyncFunction`||n?.async)&&(r=this.templatesAsync),r.define(e,t)}}},k=class extends O{};export{k as Eta,e as EtaError,r as EtaFileResolutionError,i as EtaNameResolutionError,t as EtaParseError,n as EtaRuntimeError};
//# sourceMappingURL=core.js.map