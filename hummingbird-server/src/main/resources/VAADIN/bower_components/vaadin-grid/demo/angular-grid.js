var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") return Reflect.decorate(decorators, target, key, desc);
    switch (arguments.length) {
        case 2: return decorators.reduceRight(function(o, d) { return (d && d(o)) || o; }, target);
        case 3: return decorators.reduceRight(function(o, d) { return (d && d(target, key)), void 0; }, void 0);
        case 4: return decorators.reduceRight(function(o, d) { return (d && d(target, key, o)) || o; }, desc);
    }
};
var __param = (this && this.__param) || function (paramIndex, decorator) {
    return function (target, key) { decorator(target, key, paramIndex); }
};
var angular2_1 = require('angular2/angular2');
var http_1 = require('angular2/http');
var AngularGrid = (function () {
    function AngularGrid(http) {
        var _this = this;
        this.grid = document.querySelector("angular-grid vaadin-grid");
        this.gender = document.querySelector("angular-grid select");
        this.grid.data.source = function (req) {
            return http.get(_this.getUrl(_this.gender.value, Math.max(req.count, 1)))
                .map(function (res) { return res.json().results; })
                .subscribe(function (results) { return req.success(results, _this.gender.value ? 50 : 100); });
        };
        this.grid.then(function () {
            _this.grid.columns[0].renderer = function (cell) {
                return cell.element.innerHTML = "<img style='width: 30px' src='" + cell.data + "' />";
            };
            _this.grid.header.addRow(1, ["", _this.gender]);
        });
    }
    AngularGrid.prototype.getUrl = function (gender, results) {
        return randomUserUrl + '?nat=us&gender=' + gender + '&results=' + results;
    };
    AngularGrid.prototype.onSelect = function () {
        var _this = this;
        this.selected = undefined;
        var selectedIndex = this.grid.selection.selected()[0];
        this.grid.data.getItem(selectedIndex, function (err, data) { return _this.selected = data; });
    };
    AngularGrid = __decorate([
        angular2_1.Component({
            selector: 'angular-grid',
            appInjector: [http_1.httpInjectables]
        }),
        angular2_1.View({
            templateUrl: 'angular-grid.html',
            directives: [angular2_1.NgIf]
        }),
        __param(0, angular2_1.Inject(http_1.Http))
    ], AngularGrid);
    return AngularGrid;
})();
exports.AngularGrid = AngularGrid;
angular2_1.bootstrap(AngularGrid);
