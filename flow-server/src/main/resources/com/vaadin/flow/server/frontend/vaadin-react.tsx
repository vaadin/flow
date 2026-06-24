/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
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
