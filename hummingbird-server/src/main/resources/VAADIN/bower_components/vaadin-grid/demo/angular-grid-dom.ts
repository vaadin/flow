import {bootstrap, Component, View, NgFor} from 'angular2/angular2';

@Component({
  selector: 'angular-grid-dom'
})
@View({
  template: `
  <vaadin-grid selection-mode='disabled'>
    <table>
      <colgroup>
        <col width="80">
        <col>
        <col>
      </colgroup>
      <thead>
        <tr>
          <th>Name</th>
          <th>First name</th>
          <th>Last name</th>
        </tr>
      </thead>
      <tbody>
        <tr *ng-for="var user of users">
          <td><img src="{{user.thumbnail}}" style="width: 30px"></td>
          <td>{{user.firstname}}</td>
          <td>{{user.lastname}}</td>
        </tr>
      </tbody>
    </table>
  </vaadin-grid>
  `,
  directives: [NgFor]
})

class AngularGridDom {
  users = [
      {"firstname":"raul","lastname":"diez","thumbnail":randomUserUrl + "portraits/thumb/men/39.jpg"},
      {"firstname":"sonia","lastname":"benitez","thumbnail":randomUserUrl + "portraits/thumb/women/91.jpg"},
      {"firstname":"luis","lastname":"torres","thumbnail":randomUserUrl + "portraits/thumb/men/11.jpg"},
  ]
}

bootstrap(AngularGridDom);
