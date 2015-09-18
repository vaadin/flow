import {Inject, bootstrap, Component, View, NgIf} from 'angular2/angular2';
import {Http, httpInjectables} from 'angular2/http';

@Component({
  selector: 'angular-grid',
  appInjector: [httpInjectables]
})
@View({
  templateUrl: 'angular-grid.html',
  directives: [NgIf]
})
export class AngularGrid {
  selected: Object;
  grid = document.querySelector("angular-grid vaadin-grid");
  gender = document.querySelector("angular-grid select");

  constructor(@Inject(Http) http: Http) {
    // Set a datasource for the vaadin-grid
    this.grid.data.source = req =>
      http.get(this.getUrl(this.gender.value, Math.max(req.count, 1)))
        .map(res => res.json().results)
        .subscribe(results => req.success(results, this.gender.value ? 50 : 100));

    this.grid.then(() => {
      // Set a renderer for the picture column
      this.grid.columns[0].renderer = cell =>
      cell.element.innerHTML = "<img style='width: 30px' src='" + cell.data + "' />"

      // Add a new header row with the gender select in it
      this.grid.header.addRow(1, ["", this.gender]);
    });


  }

  getUrl(gender: string, results: number) : string {
    return randomUserUrl + '?nat=us&gender=' + gender + '&results=' + results;
  }

  onSelect() {
    this.selected = undefined;
    const selectedIndex = this.grid.selection.selected()[0];
    this.grid.data.getItem(selectedIndex, (err, data) => this.selected = data);
  }
}

bootstrap(AngularGrid);
