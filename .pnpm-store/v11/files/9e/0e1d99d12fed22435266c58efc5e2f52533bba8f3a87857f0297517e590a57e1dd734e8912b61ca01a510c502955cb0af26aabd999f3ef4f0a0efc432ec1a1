(function () {
    'use strict';

    if (typeof document === 'undefined' || 'adoptedStyleSheets' in document) { return; }

    var hasShadyCss = 'ShadyCSS' in window && !ShadyCSS.nativeShadow;
    var bootstrapper = document.implementation.createHTMLDocument('');
    var closedShadowRootRegistry = new WeakMap();
    var _DOMException = typeof DOMException === 'object' ? Error : DOMException;
    var defineProperty = Object.defineProperty;
    var forEach = Array.prototype.forEach;

    var importPattern = /@import.+?;?$/gm;
    function rejectImports(contents) {
        var _contents = contents.replace(importPattern, '');
        if (_contents !== contents) {
            console.warn('@import rules are not allowed here. See https://github.com/WICG/construct-stylesheets/issues/119#issuecomment-588352418');
        }
        return _contents.trim();
    }
    function isElementConnected(element) {
        return 'isConnected' in element
            ? element.isConnected
            : document.contains(element);
    }
    function unique(arr) {
        return arr.filter(function (value, index) { return arr.indexOf(value) === index; });
    }
    function diff(arr1, arr2) {
        return arr1.filter(function (value) { return arr2.indexOf(value) === -1; });
    }
    function removeNode(node) {
        node.parentNode.removeChild(node);
    }
    function getShadowRoot(element) {
        return element.shadowRoot || closedShadowRootRegistry.get(element);
    }

    var cssStyleSheetMethods = [
        'addRule',
        'deleteRule',
        'insertRule',
        'removeRule',
    ];
    var NonConstructedStyleSheet = CSSStyleSheet;
    var nonConstructedProto = NonConstructedStyleSheet.prototype;
    nonConstructedProto.replace = function () {
        return Promise.reject(new _DOMException("Can't call replace on non-constructed CSSStyleSheets."));
    };
    nonConstructedProto.replaceSync = function () {
        throw new _DOMException("Failed to execute 'replaceSync' on 'CSSStyleSheet': Can't call replaceSync on non-constructed CSSStyleSheets.");
    };
    function isCSSStyleSheetInstance(instance) {
        return typeof instance === 'object'
            ? proto$1.isPrototypeOf(instance) ||
                nonConstructedProto.isPrototypeOf(instance)
            : false;
    }
    function isNonConstructedStyleSheetInstance(instance) {
        return typeof instance === 'object'
            ? nonConstructedProto.isPrototypeOf(instance)
            : false;
    }
    var $basicStyleElement = new WeakMap();
    var $locations = new WeakMap();
    var $adoptersByLocation = new WeakMap();
    var $appliedMethods = new WeakMap();
    function addAdopterLocation(sheet, location) {
        var adopter = document.createElement('style');
        $adoptersByLocation.get(sheet).set(location, adopter);
        $locations.get(sheet).push(location);
        return adopter;
    }
    function getAdopterByLocation(sheet, location) {
        return $adoptersByLocation.get(sheet).get(location);
    }
    function removeAdopterLocation(sheet, location) {
        $adoptersByLocation.get(sheet).delete(location);
        $locations.set(sheet, $locations.get(sheet).filter(function (_location) { return _location !== location; }));
    }
    function restyleAdopter(sheet, adopter) {
        requestAnimationFrame(function () {
            adopter.textContent = $basicStyleElement.get(sheet).textContent;
            $appliedMethods
                .get(sheet)
                .forEach(function (command) {
                return adopter.sheet[command.method].apply(adopter.sheet, command.args);
            });
        });
    }
    function checkInvocationCorrectness(self) {
        if (!$basicStyleElement.has(self)) {
            throw new TypeError('Illegal invocation');
        }
    }
    function ConstructedStyleSheet() {
        var self = this;
        var style = document.createElement('style');
        bootstrapper.body.appendChild(style);
        $basicStyleElement.set(self, style);
        $locations.set(self, []);
        $adoptersByLocation.set(self, new WeakMap());
        $appliedMethods.set(self, []);
    }
    var proto$1 = ConstructedStyleSheet.prototype;
    proto$1.replace = function replace(contents) {
        try {
            this.replaceSync(contents);
            return Promise.resolve(this);
        }
        catch (e) {
            return Promise.reject(e);
        }
    };
    proto$1.replaceSync = function replaceSync(contents) {
        checkInvocationCorrectness(this);
        if (typeof contents === 'string') {
            var self_1 = this;
            $basicStyleElement.get(self_1).textContent = rejectImports(contents);
            $appliedMethods.set(self_1, []);
            $locations.get(self_1).forEach(function (location) {
                if (location.isConnected()) {
                    restyleAdopter(self_1, getAdopterByLocation(self_1, location));
                }
            });
        }
    };
    defineProperty(proto$1, 'cssRules', {
        configurable: true,
        enumerable: true,
        get: function cssRules() {
            checkInvocationCorrectness(this);
            return $basicStyleElement.get(this).sheet.cssRules;
        },
    });
    defineProperty(proto$1, 'media', {
        configurable: true,
        enumerable: true,
        get: function media() {
            checkInvocationCorrectness(this);
            return $basicStyleElement.get(this).sheet.media;
        },
    });
    cssStyleSheetMethods.forEach(function (method) {
        proto$1[method] = function () {
            var self = this;
            checkInvocationCorrectness(self);
            var args = arguments;
            $appliedMethods.get(self).push({ method: method, args: args });
            $locations.get(self).forEach(function (location) {
                if (location.isConnected()) {
                    var sheet = getAdopterByLocation(self, location).sheet;
                    sheet[method].apply(sheet, args);
                }
            });
            var basicSheet = $basicStyleElement.get(self).sheet;
            return basicSheet[method].apply(basicSheet, args);
        };
    });
    defineProperty(ConstructedStyleSheet, Symbol.hasInstance, {
        configurable: true,
        value: isCSSStyleSheetInstance,
    });

    var defaultObserverOptions = {
        childList: true,
        subtree: true,
    };
    var locations = new WeakMap();
    function getAssociatedLocation(element) {
        var location = locations.get(element);
        if (!location) {
            location = new Location(element);
            locations.set(element, location);
        }
        return location;
    }
    function attachAdoptedStyleSheetProperty(constructor) {
        defineProperty(constructor.prototype, 'adoptedStyleSheets', {
            configurable: true,
            enumerable: true,
            get: function () {
                return getAssociatedLocation(this).sheets;
            },
            set: function (sheets) {
                getAssociatedLocation(this).update(sheets);
            },
        });
    }
    function traverseWebComponents(node, callback) {
        var iter = document.createNodeIterator(node, NodeFilter.SHOW_ELEMENT, function (foundNode) {
            return getShadowRoot(foundNode)
                ? NodeFilter.FILTER_ACCEPT
                : NodeFilter.FILTER_REJECT;
        },
        null, false);
        for (var next = void 0; (next = iter.nextNode());) {
            callback(getShadowRoot(next));
        }
    }
    var $element = new WeakMap();
    var $uniqueSheets = new WeakMap();
    var $observer = new WeakMap();
    function isExistingAdopter(self, element) {
        return (element instanceof HTMLStyleElement &&
            $uniqueSheets.get(self).some(function (sheet) { return getAdopterByLocation(sheet, self); }));
    }
    function getAdopterContainer(self) {
        var element = $element.get(self);
        return element instanceof Document ? element.body : element;
    }
    function adopt(self) {
        var styleList = document.createDocumentFragment();
        var sheets = $uniqueSheets.get(self);
        var observer = $observer.get(self);
        var container = getAdopterContainer(self);
        observer.disconnect();
        sheets.forEach(function (sheet) {
            styleList.appendChild(getAdopterByLocation(sheet, self) || addAdopterLocation(sheet, self));
        });
        container.insertBefore(styleList, null);
        observer.observe(container, defaultObserverOptions);
        sheets.forEach(function (sheet) {
            restyleAdopter(sheet, getAdopterByLocation(sheet, self));
        });
    }
    function Location(element) {
        var self = this;
        self.sheets = [];
        $element.set(self, element);
        $uniqueSheets.set(self, []);
        $observer.set(self, new MutationObserver(function (mutations, observer) {
            if (!document) {
                observer.disconnect();
                return;
            }
            mutations.forEach(function (mutation) {
                if (!hasShadyCss) {
                    forEach.call(mutation.addedNodes, function (node) {
                        if (!(node instanceof Element)) {
                            return;
                        }
                        traverseWebComponents(node, function (root) {
                            getAssociatedLocation(root).connect();
                        });
                    });
                }
                forEach.call(mutation.removedNodes, function (node) {
                    if (!(node instanceof Element)) {
                        return;
                    }
                    if (isExistingAdopter(self, node)) {
                        adopt(self);
                    }
                    if (!hasShadyCss) {
                        traverseWebComponents(node, function (root) {
                            getAssociatedLocation(root).disconnect();
                        });
                    }
                });
            });
        }));
    }
    Location.prototype = {
        isConnected: function () {
            var element = $element.get(this);
            return element instanceof Document
                ? element.readyState !== 'loading'
                : isElementConnected(element.host);
        },
        connect: function () {
            var container = getAdopterContainer(this);
            $observer.get(this).observe(container, defaultObserverOptions);
            if ($uniqueSheets.get(this).length > 0) {
                adopt(this);
            }
            traverseWebComponents(container, function (root) {
                getAssociatedLocation(root).connect();
            });
        },
        disconnect: function () {
            $observer.get(this).disconnect();
        },
        update: function (sheets) {
            var self = this;
            var locationType = $element.get(self) === document ? 'Document' : 'ShadowRoot';
            if (!Array.isArray(sheets)) {
                throw new TypeError("Failed to set the 'adoptedStyleSheets' property on " + locationType + ": Iterator getter is not callable.");
            }
            if (!sheets.every(isCSSStyleSheetInstance)) {
                throw new TypeError("Failed to set the 'adoptedStyleSheets' property on " + locationType + ": Failed to convert value to 'CSSStyleSheet'");
            }
            if (sheets.some(isNonConstructedStyleSheetInstance)) {
                throw new TypeError("Failed to set the 'adoptedStyleSheets' property on " + locationType + ": Can't adopt non-constructed stylesheets");
            }
            self.sheets = sheets;
            var oldUniqueSheets = $uniqueSheets.get(self);
            var uniqueSheets = unique(sheets);
            var removedSheets = diff(oldUniqueSheets, uniqueSheets);
            removedSheets.forEach(function (sheet) {
                removeNode(getAdopterByLocation(sheet, self));
                removeAdopterLocation(sheet, self);
            });
            $uniqueSheets.set(self, uniqueSheets);
            if (self.isConnected() && uniqueSheets.length > 0) {
                adopt(self);
            }
        },
    };

    window.CSSStyleSheet = ConstructedStyleSheet;
    attachAdoptedStyleSheetProperty(Document);
    if ('ShadowRoot' in window) {
        attachAdoptedStyleSheetProperty(ShadowRoot);
        var proto = Element.prototype;
        var attach_1 = proto.attachShadow;
        proto.attachShadow = function attachShadow(init) {
            var root = attach_1.call(this, init);
            if (init.mode === 'closed') {
                closedShadowRootRegistry.set(this, root);
            }
            return root;
        };
    }
    var documentLocation = getAssociatedLocation(document);
    if (documentLocation.isConnected()) {
        documentLocation.connect();
    }
    else {
        document.addEventListener('DOMContentLoaded', documentLocation.connect.bind(documentLocation));
    }

}());
