var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") return Reflect.decorate(decorators, target, key, desc);
    switch (arguments.length) {
        case 2: return decorators.reduceRight(function(o, d) { return (d && d(o)) || o; }, target);
        case 3: return decorators.reduceRight(function(o, d) { return (d && d(target, key)), void 0; }, void 0);
        case 4: return decorators.reduceRight(function(o, d) { return (d && d(target, key, o)) || o; }, desc);
    }
};
var angular2_1 = require('angular2/angular2');
var AngularGridDom = (function () {
    function AngularGridDom() {
        this.users = [
            { "firstname": "raul", "lastname": "diez", "thumbnail": randomUserUrl + "portraits/thumb/men/39.jpg" },
            { "firstname": "sonia", "lastname": "benitez", "thumbnail": randomUserUrl + "portraits/thumb/women/91.jpg" },
            { "firstname": "luis", "lastname": "torres", "thumbnail": randomUserUrl + "portraits/thumb/men/11.jpg" },
        ];
    }
    AngularGridDom = __decorate([
        angular2_1.Component({
            selector: 'angular-grid-dom'
        }),
        angular2_1.View({
            template: "\n  <vaadin-grid selection-mode='disabled'>\n    <table>\n      <colgroup>\n        <col width=\"80\">\n        <col header-text=\"First name\">\n        <col header-text=\"Last name\">\n      </colgroup>\n      <tbody>\n        <tr *ng-for=\"var user of users\">\n          <td><img src=\"{{user.thumbnail}}\" style=\"width: 30px\"></td>\n          <td>{{user.firstname}}</td>\n          <td>{{user.lastname}}</td>\n        </tr>\n      </tbody>\n    </table>\n  </vaadin-grid>\n  ",
            directives: [angular2_1.NgFor, angular2_1.NgIf]
        })
    ], AngularGridDom);
    return AngularGridDom;
})();
exports.AngularGridDom = AngularGridDom;
angular2_1.bootstrap(AngularGridDom);
