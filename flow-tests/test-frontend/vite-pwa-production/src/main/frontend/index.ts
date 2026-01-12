import './home-view';
import './about-view.ts';
import { Router } from '@vaadin/router';

const router = new Router(document.querySelector('#outlet'));

router.setRoutes([
  {
    path: '',
    component: 'home-view'
  },
  {
    path: 'about',
    component: 'about-view'
  }
]);
