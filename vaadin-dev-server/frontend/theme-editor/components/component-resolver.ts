/**
 * Resolves HTMLElement that should be considered instead of directly picked element.
 *
 * Used with overlays that have different HTMLElements visible than present in node tree.
 *
 * Resolvers cannot be added to component metadata as component metadata is dynamically imported after being picked.
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
    return matched ? <HTMLElement>document.querySelector('vaadin-login-overlay') : undefined;
  }
};

const _dialogOverlayResolver: Resolver = {
  resolve: (element: HTMLElement) => {
    // @ts-ignore explicit usage of Polymer property
    return element.localName === 'vaadin-dialog-overlay' ? <HTMLElement>_element['__dataHost'] : undefined;
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

const _resolvers = <Resolver[]>[
  _cookieConsentResolver,
  _loginFormOverlayResolver,
  _dialogOverlayResolver,
  _confirmDialogOverlayResolver,
  _notificationOverlayResolver
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

export const componentResolver = new ComponentResolver();
