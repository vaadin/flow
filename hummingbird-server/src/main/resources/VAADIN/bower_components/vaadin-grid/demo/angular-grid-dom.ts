import {bootstrap, Component, View, NgIf, NgFor} from 'angular2/angular2';

@Component({
  selector: 'angular-grid-dom'
})
@View({
  template: `
  <vaadin-grid selection-mode='disabled'>
    <table>
      <colgroup>
        <col width="80">
        <col header-text="First name">
        <col header-text="Last name">
      </colgroup>
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
  directives: [NgFor, NgIf]
})
export class AngularGridDom {
  users = [
      {"firstname":"raul","lastname":"diez","thumbnail":randomUserUrl + "portraits/thumb/men/39.jpg"},
      {"firstname":"sonia","lastname":"benitez","thumbnail":randomUserUrl + "portraits/thumb/women/91.jpg"},
      {"firstname":"luis","lastname":"torres","thumbnail":randomUserUrl + "portraits/thumb/men/11.jpg"},
  ]
}

bootstrap(AngularGridDom);
