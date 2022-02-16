// Call data for test assertion, real bundles don’t provide this object
const VaadinBundle = {
  init: {
    shareScopes: {}
  },
  moduleFactoryResults: {},
};
window.VaadinBundle = VaadinBundle;

const moduleCache = {};

function wrapModuleFactory(module, moduleScopeFn) {
  if (VaadinBundle.moduleFactoryResults[module]) {
    return VaadinBundle.moduleFactoryResults[module];
  } else {
    return moduleScopeFn();
  }
}

const moduleMap = {};

function defineModule(module, moduleScopeFn) {
  moduleMap[module] = wrapModuleFactory(module, moduleScopeFn);
}

function internalImport(module) {
  return moduleMap[module]();
}

defineModule("./node_modules/@vaadin/vaadin", () => {
  // re-export from package alias
  return internalImport("./node_modules/@vaadin/vaadin/vaadin.js");
});

defineModule("./node_modules/@vaadin/vaadin/vaadin.js", () => {
  // imports package for side effects, empty export
  return internalImport("./node_modules/@vaadin/vaadin/vaadin.js");
  return {};
});

defineModule("./node_modules/@vaadin/button", () => {
  // re-export from package alias
  return internalImport("./node_modules/@vaadin/button/vaadin-button.js");
});

defineModule("./node_modules/@vaadin/button/vaadin-button.js", () => {
  // re-export from another module
  return internalImport("./node_modules/@vaadin/button/src/vaadin-button.js");
});

defineModule("./node_modules/@vaadin/button/src/vaadin-button.js", () => {
  class Button extends HTMLElement {
    static get is() {
      return 'bundle-button';
    }

    get isFromBundle() {
      return true;
    }
  }

  customElements.define('bundle-button', Button);

  // export the web component class
  return { Button };
});

export function init(shareScope) {
  if (!VaadinBundle.init.shareScope[shareScope])
    VaadinBundle.init.shareScopes[shareScope] = true;
  }
}

export async function get(module) {
  if (moduleMap[module]) {
    return Promise.resolve(moduleMap[module]);
  } else {
    throw new Error(`Module ${module} does not exist in container.`);
  }
}