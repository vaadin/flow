import { routes } from "%routesJsImportPath%";

(window as any).Vaadin ??= {};
(window as any).Vaadin.routesConfig = routes;

export { routes as forHMROnly };

// @ts-ignore
if (import.meta.hot) {
   // @ts-ignore
   import.meta.hot.accept((module) => {
     (window as any).Vaadin.routesConfig = module.forHMROnly;
  });
}
