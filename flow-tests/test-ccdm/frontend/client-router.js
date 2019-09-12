import {Router} from '@vaadin/router';

const createNavigationLink = (text, link) => {
  const aLink = document.createElement('a');
  aLink.href = link;
  aLink.textContent = text;
  aLink.style.margin = '10px';
  return aLink;
}

export function loadRouter(flow) {
  //------- Configure Router
  const routes = [
    {
      path: '/',
      component: 'client-layout',
      children: [
        {
          path: 'client-view',
          action: () => {
            const div = document.createElement('div');
            div.textContent = 'Client view';
            div.id = 'clientView';
            return div;
          }
        },
        flow.route
      ]
    }
  ];

  const routerContainer = document.createElement('div');

  const navigationContainer = document.createElement('div');
  navigationContainer.appendChild(createNavigationLink('Empty view', ''));
  navigationContainer.appendChild(createNavigationLink('Client view', 'client-view'));
  navigationContainer.appendChild(createNavigationLink('Server view', 'serverview'));
  navigationContainer.appendChild(createNavigationLink('View with all events', 'view-with-all-events'));
  routerContainer.appendChild(navigationContainer);

  const outlet = document.createElement('div');
  outlet.id = 'outlet';
  routerContainer.appendChild(outlet);

  document.body.appendChild(routerContainer);

  const router = new Router(outlet);

  router.setRoutes(routes, true);
}
