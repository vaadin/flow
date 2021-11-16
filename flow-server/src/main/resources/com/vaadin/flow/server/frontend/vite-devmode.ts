// @ts-ignore
if (import.meta.hot) {
  // @ts-ignore
  const hot = import.meta.hot;
  let pendingNavigationTo: string | undefined = undefined;

  window.addEventListener('vaadin-router-go', (routerEvent: any) => {
    pendingNavigationTo = routerEvent.detail.pathname + routerEvent.detail.search;
  });
  hot.on('vite:beforeFullReload', (payload: any) => {
    if (pendingNavigationTo) {
      // Force reload with the new URL
      location.href = pendingNavigationTo;
      // Prevent Vite from reloading
      payload.path = '/_fake/path.html';
    }
  });
}
