// Call data for test assertion, real bundles don’t provide this object
const TestBundleData = {
  init: {
    shareScopes: {}
  },
  moduleFactoryResults: {},
};
window.TestBundleData = TestBundleData;

function wrapModuleFactory(module, moduleScopeFn) {
  return () => {
    if (TestBundleData.moduleFactoryResults[module]) {
      return TestBundleData.moduleFactoryResults[module];
    } else {
      const exports = moduleScopeFn() || {};
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

defineModule("./node_modules/@bundle/bundle", () => {
  // re-export from package alias
  return internalImport("./node_modules/@bundle/bundle/bundle.js");
});

defineModule("./node_modules/@bundle/bundle/bundle.js", () => {
  // imports package for side effects, empty export
  return internalImport("./node_modules/@bundle/button");
  return {};
});

defineModule("./node_modules/@bundle/button", () => {
  // re-export from package alias
  return internalImport("./node_modules/@bundle/button/bundle-button.js");
});

defineModule("./node_modules/@bundle/button/bundle-button.js", () => {
  // re-export from another module
  return internalImport("./node_modules/@bundle/button/src/bundle-button.js");
});

defineModule("./node_modules/@bundle/button/src/bundle-button.js", () => {
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
  if (!TestBundleData.init.shareScopes[shareScope]) {
    TestBundleData.init.shareScopes[shareScope] = true;
  }
}

export async function get(module) {
  if (moduleMap[module]) {
    return Promise.resolve(moduleMap[module]);
  } else {
    throw new Error(`Module ${module} does not exist in container.`);
  }
}