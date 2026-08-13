import { DomModule } from '@polymer/polymer/lib/elements/dom-module.js';
const $_documentContainer = document.createElement('template');

$_documentContainer.innerHTML = `<dom-module id="vaadin-development-mode-probe">
</dom-module>`;

document.head.appendChild($_documentContainer.content);
const version = window['Polymer'] && window['Polymer'].version;

const useHtmlImports = version && version.indexOf('2') === 0;

function isForcedDevelopmentMode() {
  return localStorage.getItem("vaadin.developmentmode.force");
}
function isLocalhost() {
  return (["localhost","127.0.0.1"].indexOf(window.location.hostname) >= 0);
}
function endsWith(string, ending) {
  return string.lastIndexOf(ending) == string.length-ending.length;
}
function getHtmlImports(scope, htmlImports) {
  if (scope) {
    const imports = [...scope.querySelectorAll("link[rel=import]")];
    imports.forEach(function(link) {
      if (htmlImports.indexOf(link.href) == -1) {
        htmlImports.push(link.href);
        getHtmlImports(link.import, htmlImports)
      }
    });
  }
  return htmlImports;
}

function isUnbundled() {
  if (useHtmlImports) {
    // If the app is bundled, then polymer.html should be included in some bundle and not in `polymer/polymer.html`
    const htmlImports = getHtmlImports(document, []);
    return htmlImports.filter(function(href) { return endsWith(href, "polymer/polymer-element.html")}).length > 0;
  } else {
    const scripts = Array.from(document.querySelectorAll("script"));
    // TODO(web-padawan): this is not expected to work for applications using native ES modules,
    // since the index.html is unlikely to contain any direct imports of Vaadin components.
    // It works with the AMD transform of CLI, as the <script> tags are being added to <head>.
    return scripts.filter(script => script.src.indexOf("@vaadin") > -1).length > 0;
  }
}

function isFlowProductionMode() {
  if (window.Vaadin && window.Vaadin.Flow && window.Vaadin.Flow.clients) {
    const productionModeApps = Object.keys(window.Vaadin.Flow.clients)
    .map(key => window.Vaadin.Flow.clients[key])
    .filter(client => client.productionMode);
    if (productionModeApps.length > 0) {
      return true;
    }
  }
  return false;
}

if (!window.Vaadin) {
  window['Vaadin'] = {};
}
if (typeof window.Vaadin.developmentMode === "undefined") {
  try {
    window.Vaadin.developmentMode = isForcedDevelopmentMode() || (isLocalhost() && isUnbundled() && !isFlowProductionMode());
  } catch (e) {
    // Some error in this code, assume production so no further actions will be taken
    window.Vaadin.developmentMode = false;
  }

  const getAssetpath = function () {
    return DomModule.import("vaadin-development-mode-probe").assetpath;
  }

  const prepareHtmlPath = function (id) {
    let path = getAssetpath();
    return path + "../" + id + "/" + id + ".html";
  }

  const prepareJsPath = function (id) {
    const scope = '@vaadin';
    let path = getAssetpath();
    return path.slice(0, path.indexOf(scope) + scope.length) + "/" + id + "/" + id + ".js";
  }

  const runCallback = function(id, optionalArgument) {
    if (window.Vaadin && window.Vaadin.developmentModeCallback) {
      const callback = window.Vaadin.developmentModeCallback[id];
      if (callback) {
        callback(optionalArgument);
      }
    }
  }

  const importAndRun = function(id, optionalArgument) {
    let path = prepareHtmlPath(id);
  }

  const loadAndRun = function(id, optionalArgument) {
    let path = prepareJsPath(id);
    let script = document.body.querySelector("script[src='" + path + "'][async]");

    if (!script) {
      script = document.createElement("script");
      script.setAttribute("src", path);
      script.async = true;

      script.onreadystatechange = script.onload = function() {
        script.__dynamicImportLoaded = true;
        runCallback(id, optionalArgument);
      }

      script.onerror = function() {
        // In case of an error, remove the script from the document so that it
        // will be automatically created again the next time
        if (script.parentNode) {
          script.parentNode.removeChild(script);
        }
      }
    }

    if (script.parentNode == null) {
      document.body.appendChild(script);
      // if the script already loaded, dispatch a fake load event
      // so that listeners are called and get a proper event argument.
    } else if (script.__dynamicImportLoaded) {
      script.dispatchEvent(new Event('load'));
    }
  }

  window.Vaadin.runIfDevelopmentMode = function(id, optionalArgument) {
    // Imports a HTML file named "id"/id".html and
    // triggers a callback defined in window.Vaadin.developmentModeCallback (if present)
    // with the given (optional) argument
    if (!window.Vaadin.developmentMode) {
      return;
    }

    if (window.HTMLImports && useHtmlImports) {
      HTMLImports.whenReady(function() {
        importAndRun(id, optionalArgument);
      });
    } else if (useHtmlImports) {
      importAndRun(id, optionalArgument);
    } else {
      loadAndRun(id, optionalArgument);
    }
  }
}
