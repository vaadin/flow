/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import './home-view';
import './about-view.ts';
import { Router } from '@vaadin/router';

const router = new Router(document.querySelector('#outlet'));

router.setRoutes([
  {
    path: '',
    component: 'home-view',
  },
  {
    path: 'about',
    component: 'about-view',
  },
]);
