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

defineModule("./node_modules/@testscope/all", () => {
  // re-export from package alias
  return internalImport("./node_modules/@testscope/all/all.js");
});

defineModule("./node_modules/@testscope/all/all.js", () => {
  // imports package for side effects, empty export
  return internalImport("./node_modules/@testscope/button");
  return {};
});

defineModule("./node_modules/@testscope/button", () => {
  // re-export from package alias
  return internalImport("./node_modules/@testscope/button/testscope-button.js");
});

defineModule("./node_modules/@testscope/button/testscope-button.js", () => {
  // re-export from another module
  return internalImport("./node_modules/@testscope/button/src/testscope-button.js");
});

defineModule("./node_modules/@testscope/button/src/testscope-button.js", () => {
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