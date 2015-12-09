// Don't load twice
if (!XMLHttpRequest.prototype.___open && window.randomUserUrl) {
  //Faking randomuser.me REST call since the site is sometimes unavailable.
  //We have a set of 200 users cached in a static .json file instead, we
  //take it via ajax and split it conveniently based on parameters.
  //We use triple underscore for patched methods since angular already
  //patches XMLHttpRequest and uses single underscore.
  XMLHttpRequest.prototype.___open = XMLHttpRequest.prototype.open;
  XMLHttpRequest.prototype.open = function(method, url, type) {
   if (url.indexOf(window.randomUserUrl) === 0) {
     var tokens, re = /[?&]([^=]+)=([^&]*)/g;
     this.params = {};
     while (tokens = re.exec(url)) {
         this.params[tokens[1]] = tokens[2];
     }
     url = window.localUserUrl + "?results=" + this.params.results + "&gender=" + (this.params.gender || '');
   }
   // Angular uses sync open, but we want it async.
   this.___open(method, url, true)
  };
  XMLHttpRequest.prototype.___send = XMLHttpRequest.prototype.send;
  XMLHttpRequest.prototype.send = function() {
   var params = this.params;
   if (params) {
     var ___onreadystatechange = this.onreadystatechange;
     this.onreadystatechange = function() {
       if (this.readyState == XMLHttpRequest.DONE  && this.status == 200) {
         this.text = this.responseText;
         var json = JSON.parse(this.text);
         // Since response & responseText are read only we redefine getters.
         var getter = {get: function(){return this.text;}};
         Object.defineProperty(this, "responseText" , getter);
         Object.defineProperty(this, "response" , getter);
         // Filter by gender
         if (params.gender) {
           json.results = json.results.filter(function (o) {
             return o.user && o.user.gender == params.gender;
           });
         }
         // limit results.
         if (params.results) {
           json.results = json.results.splice(0, params.results)
         }
         this.text = JSON.stringify(json)
       }
       // Introducing some delay to make spiner visible for a while.
       // Angular patching sets original onreadystatechange to undefined.
       (___onreadystatechange && setTimeout(___onreadystatechange, 300));
     };
   }
   this.___send();
  };
}
