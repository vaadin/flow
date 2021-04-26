import { MiddlewareClass, MiddlewareContext, MiddlewareNext } from './Connect';

const $wnd = window as any;

export interface LoginResult {
  error: boolean;
  token?: string;
  errorTitle?: string;
  errorMessage?: string;
  redirectUrl?: string;
}

export interface LoginOptions {
  loginProcessingUrl?: string;
  failureUrl?: string;
  /**
   * @deprecated The `defaultSuccessUrl` is not used anymore.
   */
  defaultSuccessUrl?: string;
}

export interface LogoutOptions {
  logoutUrl?: string;
}

/**
 * A helper method for Spring Security based form login.
 * @param username
 * @param password
 * @param options defines additional options, e.g, the loginProcessingUrl, failureUrl, defaultSuccessUrl etc.
 */
export async function login(username: string, password: string, options?: LoginOptions): Promise<LoginResult> {
  let result;
  try {
    const data = new FormData();
    data.append('username', username);
    data.append('password', password);

    const loginProcessingUrl = options && options.loginProcessingUrl ? options.loginProcessingUrl : '/login';
    const headers = getSpringCsrfTokenHeadersFromDocument(document);
    const response = await fetch(loginProcessingUrl, { method: 'POST', body: data, headers });

    const failureUrl = options && options.failureUrl ? options.failureUrl : '/login?error';
    // this assumes the default Spring Security form login configuration (handler URL and responses)
    if (response.ok && response.redirected) {
      if (response.url.endsWith(failureUrl)) {
        result = {
          error: true,
          errorTitle: 'Incorrect username or password.',
          errorMessage: 'Check that you have entered the correct username and password and try again.'
        };
      } else {
        const vaadinCsrfToken = await updateCsrfTokensBasedOnResponse(response);
        if (vaadinCsrfToken) {
          result = {
            error: false,
            token: vaadinCsrfToken,
            redirectUrl: response.url
          };
        }
      }
    }
  } catch (e) {
    result = {
      error: true,
      errorTitle: e.name,
      errorMessage: e.message
    };
  }

  return (
    result || {
      error: true,
      errorTitle: 'Error',
      errorMessage: 'Something went wrong when trying to login.'
    }
  );
}

/**
 * A helper method for Spring Security based form logout
 * @param options defines additional options, e.g, the logoutUrl.
 */
export async function logout(options?: LogoutOptions) {
  // this assumes the default Spring Security logout configuration (handler URL)
  const logoutUrl = options && options.logoutUrl ? options.logoutUrl : '/logout';
  try {
    const headers = getSpringCsrfTokenHeadersFromDocument(document);
    await doLogout(logoutUrl, headers);
  } catch {
    try {
      const response = await fetch('?nocache');
      const responseText = await response.text();
      const doc = new DOMParser().parseFromString(responseText, 'text/html');
      const headers = getSpringCsrfTokenHeadersFromDocument(doc);
      await doLogout(logoutUrl, headers);
    } catch (error) {
      // clear the token if the call fails
      delete $wnd.Vaadin?.TypeScript?.csrfToken;
      clearSpringCsrfMetaTags();
      throw error;
    }
  }
}

async function doLogout(logoutUrl: string, headers: Record<string, string>) {
  const response = await fetch(logoutUrl, { method: 'POST', headers });
  if (!response.ok) {
    throw new Error(`failed to logout with response ${response.status}`);
  }

  await updateCsrfTokensBasedOnResponse(response);
}

function updateSpringCsrfMetaTag(body: string) {
  const doc = new DOMParser().parseFromString(body, 'text/html');
  clearSpringCsrfMetaTags();
  Array.from(doc.head.querySelectorAll('meta[name="_csrf"], meta[name="_csrf_header"]')).forEach((el) =>
    document.head.appendChild(document.importNode(el, true))
  );
}

function clearSpringCsrfMetaTags() {
  Array.from(document.head.querySelectorAll('meta[name="_csrf"], meta[name="_csrf_header"]')).forEach((el) =>
    el.remove()
  );
}

const getCsrfTokenFromResponseBody = (body: string): string | undefined => {
  const match = body.match(/window\.Vaadin = \{TypeScript: \{"csrfToken":"([0-9a-zA-Z\\-]{36})"}};/i);
  return match ? match[1] : undefined;
};

const getSpringCsrfTokenHeadersFromDocument = (doc: Document): Record<string, string> => {
  const csrf = doc.head.querySelector('meta[name="_csrf"]');
  const csrfHeader = doc.head.querySelector('meta[name="_csrf_header"]');
  const headers: Record<string, string> = {};
  if (csrf !== null && csrfHeader !== null) {
    headers[(csrfHeader as HTMLMetaElement).content] = (csrf as HTMLMetaElement).content;
  }
  return headers;
};

async function updateCsrfTokensBasedOnResponse(response: Response): Promise<string | undefined> {
  const responseText = await response.text();
  const token = getCsrfTokenFromResponseBody(responseText);
  $wnd.Vaadin.TypeScript = $wnd.Vaadin.TypeScript || {};
  $wnd.Vaadin.TypeScript.csrfToken = token;
  updateSpringCsrfMetaTag(responseText);
  return token;
}

/**
 * It defines what to do when it detects a session is invalid. E.g.,
 * show a login view.
 * It takes an <code>EndpointCallContinue</code> parameter, which can be
 * used to continue the endpoint call.
 */
export type OnInvalidSessionCallback = () => Promise<LoginResult>;

/**
 * A helper class for handling invalid sessions during an endpoint call.
 * E.g., you can use this to show user a login page when the session has expired.
 */
export class InvalidSessionMiddleware implements MiddlewareClass {
  constructor(private onInvalidSessionCallback: OnInvalidSessionCallback) {}

  async invoke(context: MiddlewareContext, next: MiddlewareNext): Promise<Response> {
    const clonedContext = { ...context };
    clonedContext.request = context.request.clone();
    const response = await next(context);
    if (response.status === 401) {
      const loginResult = await this.onInvalidSessionCallback();
      if (loginResult.token) {
        clonedContext.request.headers.set('X-CSRF-Token', loginResult.token);
        return next(clonedContext);
      }
    }
    return response;
  }
}
