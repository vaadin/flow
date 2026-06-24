// Call data for test assertion, real bundles don’t provide this object
const TestBundleData = {
  init: {
    shareScopes: {}
  },
  moduleFactoryResults: {},
};
window.TestBundleData = TestBundleData;

function wrapModuleFactory(module, moduleScopeFn) {
  return async () => {
    if (TestBundleData.moduleFactoryResults[module]) {
      return TestBundleData.moduleFactoryResults[module];
    } else {
      const exports = new Promise(resolve => resolve(moduleScopeFn()));
      TestBundleData.moduleFactoryResults[module] = exports;
      return exports;
    }
  };
}

const moduleMap = {};

function defineModule(module, moduleScopeFn) {
  moduleMap[module] = wrapModuleFactory(module, moduleScopeFn);
}

function internalImport(module) {
  return moduleMap[module]();
}

defineModule("./node_modules/@vaadin/testscope-all", async () => {
  // re-export from package alias
  return internalImport("./node_modules/@vaadin/testscope-all/all.js");
});

defineModule("./node_modules/@vaadin/testscope-all/all.js", async () => {
  // imports package for side effects, empty export
  await internalImport("./node_modules/@vaadin/testscope-button");
  return {};
});

defineModule("./node_modules/@vaadin/testscope-button", async () => {
  // re-export from package alias
  return internalImport("./node_modules/@vaadin/testscope-button/testscope-button.js");
});

defineModule("./node_modules/@vaadin/testscope-button/testscope-button.js", async () => {
  // re-export from another module
  return internalImport("./node_modules/@vaadin/testscope-button/src/testscope-button.js");
});

defineModule("./node_modules/@vaadin/testscope-button/src/testscope-button.js", async () => {
  class Button extends HTMLElement {
    static get is() {
      return 'testscope-button';
    }

    connectedCallback() {
      if (!this.textContent) {
        this.textContent = 'testscope-button from bundle';
      }
    }

    get isFromBundle() {
      return true;
    }
  }

  customElements.define('testscope-button', Button);

  // export the web component class
  return { Button };
});

defineModule("./node_modules/@vaadin/testscope-map/src/lib.js", async () => {
  return {
    default: {
      MAP: 'map',
    },
  };
});

defineModule("./node_modules/@vaadin/testscope-map", async () => {
  // re-export from package alias
  return internalImport("./node_modules/@vaadin/testscope-map/testscope-map.js");
});

defineModule("./node_modules/@vaadin/testscope-map/testscope-map.js", async () => {
  // re-export from another module
  return internalImport("./node_modules/@vaadin/testscope-map/src/testscope-map.js");
});

defineModule("./node_modules/@vaadin/testscope-map/src/testscope-map.js", async () => {
  // Async import instead of `internalImport` is necessary to verify imports
  // of default library export through the bundle resolver and response.
  // See https://github.com/vaadin/flow/issues/14355
  const {default: lib} = await import('@vaadin/testscope-map/src/lib.js');

  class Map extends HTMLElement {
    static get is() {
      return 'testscope-map';
    }

    connectedCallback() {
      if (!this.textContent) {
        this.textContent = `testscope-${lib.MAP} from bundle`;
      }
    }

    get isFromBundle() {
      return true;
    }
  }

  customElements.define('testscope-map', Map);

  // export the web component class
  return { Map };
});

export function init(shareScope) {
  if (!TestBundleData.init.shareScopes[shareScope]) {
    TestBundleData.init.shareScopes[shareScope] = true;
  }
}

export async function get(module) {
  if (moduleMap[module]) {
    const exports = await moduleMap[module]();
    return () => exports;
  } else {
    throw new Error(`Module ${module} does not exist in container.`);
  }
}