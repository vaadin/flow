package com.vaadin.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.vaadin.client.communication.DomApiImpl;
import com.vaadin.client.communication.PlainDomApiImpl;

public class HummingbirdClientTest extends GWTTestCase {
    static {
        if (GWT.isClient()) {
            useDomImpl(new PlainDomApiImpl());
            installPromisePolyfill();
            installCollectionsPolyfill();
            installNodeListIndexOf();
            // setTreeDebugging(true);
        }
    }

    @Override
    public String getModuleName() {
        return "com.vaadin.ClientEngineXSI";
    }

    private static native void useDomImpl(DomApiImpl impl)
    /*-{
        @com.vaadin.client.communication.DomApi::impl = impl;
    }-*/;

    private static native void setTreeDebugging(boolean debug)
    /*-{
        @com.vaadin.client.communication.tree.TreeUpdater::debug = debug;
    }-*/;

    private static native void installPromisePolyfill()
    /*-{
        // promise-polyfill 2.1.0 (with 'this' replaced to '$wnd' at the end)

        !function(a){function b(a,b){return function(){a.apply(b,arguments)}}function c(a){if("object"!=typeof this)throw new TypeError("Promises must be constructed via new");if("function"!=typeof a)throw new TypeError("not a function");this._state=null,this._value=null,this._deferreds=[],i(a,b(e,this),b(f,this))}function d(a){var b=this;return null===this._state?void this._deferreds.push(a):void j(function(){var c=b._state?a.onFulfilled:a.onRejected;if(null===c)return void(b._state?a.resolve:a.reject)(b._value);var d;try{d=c(b._value)}catch(e){return void a.reject(e)}a.resolve(d)})}function e(a){try{if(a===this)throw new TypeError("A promise cannot be resolved with itself.");if(a&&("object"==typeof a||"function"==typeof a)){var c=a.then;if("function"==typeof c)return void i(b(c,a),b(e,this),b(f,this))}this._state=!0,this._value=a,g.call(this)}catch(d){f.call(this,d)}}function f(a){this._state=!1,this._value=a,g.call(this)}function g(){for(var a=0,b=this._deferreds.length;b>a;a++)d.call(this,this._deferreds[a]);this._deferreds=null}function h(a,b,c,d){this.onFulfilled="function"==typeof a?a:null,this.onRejected="function"==typeof b?b:null,this.resolve=c,this.reject=d}function i(a,b,c){var d=!1;try{a(function(a){d||(d=!0,b(a))},function(a){d||(d=!0,c(a))})}catch(e){if(d)return;d=!0,c(e)}}var j="function"==typeof setImmediate&&setImmediate||function(a){setTimeout(a,1)},k=Array.isArray||function(a){return"[object Array]"===Object.prototype.toString.call(a)};c.prototype["catch"]=function(a){return this.then(null,a)},c.prototype.then=function(a,b){var e=this;return new c(function(c,f){d.call(e,new h(a,b,c,f))})},c.all=function(){var a=Array.prototype.slice.call(1===arguments.length&&k(arguments[0])?arguments[0]:arguments);return new c(function(b,c){function d(f,g){try{if(g&&("object"==typeof g||"function"==typeof g)){var h=g.then;if("function"==typeof h)return void h.call(g,function(a){d(f,a)},c)}a[f]=g,0===--e&&b(a)}catch(i){c(i)}}if(0===a.length)return b([]);for(var e=a.length,f=0;f<a.length;f++)d(f,a[f])})},c.resolve=function(a){return a&&"object"==typeof a&&a.constructor===c?a:new c(function(b){b(a)})},c.reject=function(a){return new c(function(b,c){c(a)})},c.race=function(a){return new c(function(b,c){for(var d=0,e=a.length;e>d;d++)a[d].then(b,c)})},c._setImmediateFn=function(a){j=a},"undefined"!=typeof module&&module.exports?module.exports=c:a.Promise||(a.Promise=c)}($wnd);
    }-*/;

    private static native void installCollectionsPolyfill()
    /*-{
        // es6-collections 0.5.5 (with 'window' replaced to '$wnd' at the end)

        (function(e){function f(a,c){function b(a){if(!this||this.constructor!==b)return new b(a);this._keys=[];this._values=[];this._itp=[];this.objectOnly=c;a&&v.call(this,a)}c||w(a,"size",{get:x});a.constructor=b;b.prototype=a;return b}function v(a){this.add?a.forEach(this.add,this):a.forEach(function(a){this.set(a[0],a[1])},this)}function d(a){this.has(a)&&(this._keys.splice(b,1),this._values.splice(b,1),this._itp.forEach(function(a){b<a[0]&&a[0]--}));return-1<b}function m(a){return this.has(a)?this._values[b]:
        void 0}function n(a,c){if(this.objectOnly&&c!==Object(c))throw new TypeError("Invalid value used as weak collection key");if(c!=c||0===c)for(b=a.length;b--&&!y(a[b],c););else b=a.indexOf(c);return-1<b}function p(a){return n.call(this,this._values,a)}function q(a){return n.call(this,this._keys,a)}function r(a,c){this.has(a)?this._values[b]=c:this._values[this._keys.push(a)-1]=c;return this}function t(a){this.has(a)||this._values.push(a);return this}function h(){(this._keys||0).length=this._values.length=
        0}function z(){return k(this._itp,this._keys)}function l(){return k(this._itp,this._values)}function A(){return k(this._itp,this._keys,this._values)}function B(){return k(this._itp,this._values,this._values)}function k(a,c,b){var g=[0],e=!1;a.push(g);return{next:function(){var f,d=g[0];!e&&d<c.length?(f=b?[c[d],b[d]]:c[d],g[0]++):(e=!0,a.splice(a.indexOf(g),1));return{done:e,value:f}}}}function x(){return this._values.length}function u(a,c){for(var b=this.entries();;){var d=b.next();if(d.done)break;
        a.call(c,d.value[1],d.value[0],this)}}var b,w=Object.defineProperty,y=function(a,b){return isNaN(a)?isNaN(b):a===b};"undefined"==typeof WeakMap&&(e.WeakMap=f({"delete":d,clear:h,get:m,has:q,set:r},!0));"undefined"!=typeof Map&&"function"===typeof(new Map).values&&(new Map).values().next||(e.Map=f({"delete":d,has:q,get:m,set:r,keys:z,values:l,entries:A,forEach:u,clear:h}));"undefined"!=typeof Set&&"function"===typeof(new Set).values&&(new Set).values().next||(e.Set=f({has:p,add:t,"delete":d,clear:h,
        keys:l,values:l,entries:B,forEach:u}));"undefined"==typeof WeakSet&&(e.WeakSet=f({"delete":d,add:t,clear:h,has:p},!0))})("undefined"!=typeof exports&&"undefined"!=typeof global?global:$wnd);
    }-*/;

    private static native void installNodeListIndexOf()
    /*-{
        $wnd.NodeList.prototype.indexOf = function(item) { return Array.prototype.indexOf.call(this, item); }
    }-*/;

}
