import { routes } from "%routesJsImportPath%";
import { registerGlobalClickHandler } from "Frontend/generated/flow/Flow.js";

(window as any).Vaadin ??= {};
(window as any).Vaadin.routesConfig = routes;
registerGlobalClickHandler();

export { routes as forHMROnly };

// @ts-ignore
if (import.meta.hot) {
   // @ts-ignore
   import.meta.hot.accept((module) => {
     if (module?.forHMROnly) {
       (window as any).Vaadin.routesConfig = module.forHMROnly;
     }
  });
}
