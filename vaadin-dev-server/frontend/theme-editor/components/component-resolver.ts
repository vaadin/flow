/**
 * Resolves HTMLElement that should be considered instead of directly picked element.
 *
 * Used with overlays that have different HTMLElements visible than present in node tree.
 *
 * Resolvers cannot be added to component metadata as component metadata is dynamically imported after being picked.
 *
 * Using Polymer __dataHost property to get base Vaadin component.
 *
 * TODO: Refactor required after moving to Lit components
 */

type Resolver = {
  // returns source Vaadin element for given picked element
  resolve: (element: HTMLElement) => HTMLElement | undefined;
};

const _cookieConsentResolver: Resolver = {
  resolve: (element: HTMLElement) => {
    const matcher = (element: HTMLElement) => element.classList.contains('cc-banner');
    const matched = _isMatchingRecursive(matcher, element);
    return matched ? <HTMLElement>document.querySelector('vaadin-cookie-consent') : undefined;
  }
};

const _loginFormOverlayResolver: Resolver = {
  resolve: (element: HTMLElement) => {
    const matcher = (element: HTMLElement) => element.localName === 'vaadin-login-overlay-wrapper';
    const matched = _isMatchingRecursive(matcher, element);
    // @ts-ignore explicit usage of Polymer property
    return matched ? <HTMLElement>matched['__dataHost'] : undefined;
  }
};

const _dialogOverlayResolver: Resolver = {
  resolve: (element: HTMLElement) => {
    // @ts-ignore explicit usage of Polymer property
    return element.localName === 'vaadin-dialog-overlay' ? <HTMLElement>element['__dataHost'] : undefined;
  }
};

const _confirmDialogOverlayResolver: Resolver = {
  resolve: (element: HTMLElement) => {
    const matcher = (element: HTMLElement) => element.localName === 'vaadin-confirm-dialog-overlay';
    const matched = _isMatchingRecursive(matcher, element);
    // @ts-ignore explicit usage of Polymer property
    return matched ? <HTMLElement>matched['__dataHost'] : undefined;
  }
};

const _notificationOverlayResolver: Resolver = {
  resolve: (element: HTMLElement) => {
    const matcher = (element: HTMLElement) => element.localName === 'vaadin-notification-card';
    const matched = _isMatchingRecursive(matcher, element);
    // @ts-ignore explicit usage of Polymer property
    return matched ? <HTMLElement>matched['__dataHost'] : undefined;
  }
};
const _menuBarItemResolver: Resolver = {
  resolve: (element: HTMLElement) => {
    if (element.localName !== 'vaadin-menu-bar-item') {
      return;
    }
    const matcher = (element: HTMLElement) => element.localName === 'vaadin-menu-bar';
    return _isMatchingRecursive(matcher, element);
  }
};

// order is important
const _resolvers = <Resolver[]>[
  _cookieConsentResolver,
  _loginFormOverlayResolver,
  _dialogOverlayResolver,
  _confirmDialogOverlayResolver,
  _notificationOverlayResolver,
  _menuBarItemResolver
];

const _cookieConsentHighlightResolver: Resolver = {
  resolve: (element: HTMLElement) => {
    const matcher = (element: HTMLElement) => element.classList.contains('cc-banner');
    return _isMatchingRecursive(matcher, element);
  }
};

const _overlayHighlightResolver: Resolver = {
  resolve: (element: HTMLElement) => {
    const matcher = (element: HTMLElement) => element.shadowRoot?.querySelector('[part=overlay]') != undefined;
    const matched = _isMatchingRecursive(matcher, element);
    return <HTMLElement>matched?.shadowRoot?.querySelector('[part=overlay]');
  }
};

const _loginFormOverlayHighlightResolver: Resolver = {
  resolve: (element: HTMLElement) => {
    const matcher = (element: HTMLElement) => element.localName === 'vaadin-login-overlay-wrapper';
    const matched = _isMatchingRecursive(matcher, element);
    // @ts-ignore explicit usage of Polymer property
    return <HTMLElement>matched?.shadowRoot?.querySelector('[part=card]');
  }
};

// order is important
const _highlightResolvers = <Resolver[]>[
  _loginFormOverlayHighlightResolver,
  _cookieConsentHighlightResolver,
  _overlayHighlightResolver
];

// finds matching element or its parent
const _isMatchingRecursive = function (
  matcher: (element: HTMLElement) => boolean,
  element: HTMLElement
): HTMLElement | undefined {
  if (matcher(element)) {
    return element;
  } else if (element.parentNode && element.parentNode instanceof HTMLElement) {
    return _isMatchingRecursive(matcher, element.parentNode);
  } else {
    return undefined;
  }
};

class ComponentResolver {
  resolveElement(element: HTMLElement) {
    for (const i in _resolvers) {
      let resolved: HTMLElement | undefined = element;
      if ((resolved = _resolvers[i].resolve(element)) !== undefined) {
        return resolved;
      }
    }
    return element;
  }
}

class ComponentHighlightResolver {
  resolveElement(element: HTMLElement) {
    for (const i in _highlightResolvers) {
      let resolved: HTMLElement | undefined = element;
      if ((resolved = _highlightResolvers[i].resolve(element)) !== undefined) {
        return resolved;
      }
    }
    return element;
  }
}

export const componentResolver = new ComponentResolver();

export const componentHighlightResolver = new ComponentHighlightResolver();
