/* tslint:disable:max-classes-per-file */
import { set, Store } from 'idb-keyval';
import { v4 as uuidv4 } from 'uuid';

const $wnd = window as any;
$wnd.Vaadin = $wnd.Vaadin || {};
$wnd.Vaadin.registrations = $wnd.Vaadin.registrations || [];
$wnd.Vaadin.registrations.push({
  is: 'endpoint'
});

interface ConnectExceptionData {
  message: string;
  type: string;
  detail?: any;
  validationErrorData?: ValidationErrorData[];
}

const throwConnectException = (errorJson: ConnectExceptionData) => {
  if (errorJson.validationErrorData) {
    throw new EndpointValidationError(
      errorJson.message,
      errorJson.validationErrorData,
      errorJson.type
    );
  } else {
    throw new EndpointError(
      errorJson.message,
      errorJson.type,
      errorJson.detail
    );
  }
};

/**
 * Throws a TypeError if the response is not 200 OK.
 * @param response The response to assert.
 * @ignore
 */
const assertResponseIsOk = async(response: Response): Promise<void> => {
  if (!response.ok) {
    const errorText = await response.text();
    let errorJson: ConnectExceptionData | null;
    try {
      errorJson = JSON.parse(errorText);
    } catch (ignored) {
      // not a json
      errorJson = null;
    }

    if (errorJson !== null) {
      throwConnectException(errorJson);
    } else if (errorText !== null && errorText.length > 0) {
      throw new EndpointResponseError(errorText, response);
    } else {
      throw new EndpointError(
        'expected "200 OK" response, but got ' +
        `${response.status} ${response.statusText}`
      );
    }
  }
};

/**
 * An exception that gets thrown for unexpected HTTP response.
 */
export class EndpointResponseError extends Error {
  /**
   * The optional response object, containing the HTTP response error
   */
  response: Response;

  /**
   * @param message the `message` property value
   * @param response the `response` property value
   */
  constructor(message: string, response: Response) {
    super(message);
    this.response = response;
  }
}

/**
 * An exception that gets thrown when the Vaadin Connect backend responds
 * with not ok status.
 */
export class EndpointError extends Error {
  /**
   * The optional name of the exception that was thrown on a backend
   */
  type?: string;

  /**
   * The optional detail object, containing additional information sent
   * from a backend
   */
  detail?: any;

  /**
   * @param message the `message` property value
   * @param type the `type` property value
   * @param detail the `detail` property value
   */
  constructor(message: string, type?: string, detail?: any) {
    super(message);
    this.type = type;
    this.detail = detail;
  }
}

/**
 * An exception that gets thrown if Vaadin Connect backend responds
 * with non-ok status and provides additional info
 * on the validation errors occurred.
 */
export class EndpointValidationError extends EndpointError {
  /**
   * An original validation error message.
   */
  validationErrorMessage: string;
  /**
   * An array of the validation errors.
   */
  validationErrorData: ValidationErrorData[];

  /**
   * @param message the `message` property value
   * @param validationErrorData the `validationErrorData` property value
   * @param type the `type` property value
   */
  constructor(message: string, validationErrorData: ValidationErrorData[],
              type?: string) {
    super(message, type, validationErrorData);
    this.validationErrorMessage = message;
    this.detail = null;
    this.validationErrorData = validationErrorData;
  }
}

/**
 * An object, containing all data for the particular validation error.
 */
export class ValidationErrorData {
  /**
   * The validation error message.
   */
  message: string;
  /**
   * The parameter name that caused the validation error.
   */
  parameterName?: string;

  /**
   * @param message the `message` property value
   * @param parameterName the `parameterName` property value
   */
  constructor(message: string, parameterName?: string) {
    this.message = message;
    this.parameterName = parameterName;
  }
}

/**
 * The `ConnectClient` constructor options.
 */
export interface ConnectClientOptions {
  /**
   * The `prefix` property value.
   */
  prefix?: string;

  /**
   * The `middlewares` property value.
   */
  middlewares?: Middleware[];
}

/**
 * An object with the call arguments and the related Request instance.
 * See also {@link ConnectClient.call | the call() method in ConnectClient}.
 */
export interface MiddlewareContext {
  /**
   * The endpoint name.
   */
  endpoint: string;

  /**
   * The method name to call on in the endpoint class.
   */
  method: string;

  /**
   * Optional object with method call arguments.
   */
  params?: any;

  /**
   * The Fetch API Request object reflecting the other properties.
   */
  request: Request;
}

/**
 * An async middleware callback that invokes the next middleware in the chain
 * or makes the actual request.
 * @param context The information about the call and request
 */
export type MiddlewareNext = (context: MiddlewareContext) =>
  Promise<Response> | Response;


/**
 * An interface that allows defining a middleware as a class.
 */
export interface MiddlewareClass {
  /**
   * @param context The information about the call and request
   * @param next Invokes the next in the call chain
   */
  invoke(context: MiddlewareContext, next: MiddlewareNext): Promise<Response> | Response;
}

/**
 * An async callback function that can intercept the request and response
 * of a call.
 */
type MiddlewareFunction = (context: MiddlewareContext, next: MiddlewareNext) =>
  Promise<Response> | Response;

/**
 * An async callback that can intercept the request and response
 * of a call, could be either a function or a class.
 */
export type Middleware = MiddlewareClass | MiddlewareFunction;

/**
 * Vaadin Connect client class is a low-level network calling utility. It stores
 * a prefix and facilitates remote calls to endpoint class methods
 * on the Vaadin Connect backend.
 *
 * Example usage:
 *
 * ```js
 * const client = new ConnectClient();
 * const responseData = await client.call('MyEndpoint', 'myMethod');
 * ```
 *
 * ### Prefix
 *
 * The client supports an `prefix` constructor option:
 * ```js
 * const client = new ConnectClient({prefix: '/my-connect-prefix'});
 * ```
 *
 * The default prefix is '/connect'.
 *
 */
export class ConnectClient {
  /**
   * The Vaadin Connect backend prefix
   */
  prefix: string = '/connect';

  /**
   * The array of middlewares that are invoked during a call.
   */
  middlewares: Middleware[] = [];

  private endpointRequetsQueue: Store | undefined;

  /**
   * @param options Constructor options.
   */
  constructor(options: ConnectClientOptions = {}) {
    if (options.prefix) {
      this.prefix = options.prefix;
    }

    if (options.middlewares) {
      this.middlewares = options.middlewares;
    }
  }

  /**
   * Makes a JSON HTTP request to the `${prefix}/${endpoint}/${method}` URL,
   * optionally supplying the provided params as a JSON request body,
   * and asynchronously returns the parsed JSON response data.
   *
   * @param endpoint Endpoint name.
   * @param method Method name to call in the endpoint class.
   * @param params Optional object to be send in JSON request body.
   * @param options Optional client options for this call.
   * @returns {} Decoded JSON response data.
   */
  async call(
    endpoint: string,
    method: string,
    params?: any,
  ): Promise<any> {
    if (arguments.length < 2) {
      throw new TypeError(
        `2 arguments required, but got only ${arguments.length}`
      );
    }

    const headers: Record<string, string> = {
      'Accept': 'application/json',
      'Content-Type': 'application/json',
      'X-CSRF-Token': $wnd.Vaadin.TypeScript && $wnd.Vaadin.TypeScript.csrfToken || ''
    };

    // helper to keep the undefined value in object after JSON.stringify
    const nullForUndefined = (obj: any): any => {
      for (const property in obj) {
        if (obj[property] === undefined) {
          obj[property] = null;
        }
      }
      return obj;
    }

    const request = new Request(
       `${this.prefix}/${endpoint}/${method}`, {
         method: 'POST',
         headers,
         body: params !== undefined ? JSON.stringify(nullForUndefined(params)) : undefined
        });

    // The middleware `context`, includes the call arguments and the request
    // constructed from them
    const initialContext: MiddlewareContext = {
      endpoint,
      method,
      params,
      request
    };

    // The internal middleware to assert and parse the response. The internal
    // response handling should come last after the other middlewares are done
    // with processing the response. That is why this middleware is first
    // in the final middlewares array.
    const responseHandlerMiddleware: Middleware =
      async(
        context: MiddlewareContext,
        next: MiddlewareNext
      ): Promise<Response> => {
        const response = await next(context);
        await assertResponseIsOk(response);
        return response.json();
      };

    // The actual fetch call itself is expressed as a middleware
    // chain item for our convenience. Always having an ending of the chain
    // this way makes the folding down below more concise.
    const fetchNext: MiddlewareNext =
      async(context: MiddlewareContext): Promise<Response> => {
        this.loading(true);
        return fetch(context.request).then(response => {
          this.loading(false);
          return response;
        });
      };

    // Assemble the final middlewares array from internal
    // and external middlewares
    const middlewares = [responseHandlerMiddleware as Middleware].concat(this.middlewares);

    // Fold the final middlewares array into a single function
    const chain = middlewares.reduceRight(
      (next: MiddlewareNext, middleware: Middleware) => {
        // Compose and return the new chain step, that takes the context and
        // invokes the current middleware with the context and the further chain
        // as the next argument
        return (context => {
          if(typeof middleware === 'function'){
            return middleware(context, next);
          }else {
            return (middleware as MiddlewareClass).invoke(context, next);
          }
        }) as MiddlewareNext;
      },
      // Initialize reduceRight the accumulator with `fetchNext`
      fetchNext
    );

    // Invoke all the folded async middlewares and return
    return chain(initialContext);
  }

  async deferrableCall(
    endpoint: string,
    method: string,
    params?: any,
  ): Promise<DeferrableResult<any>> {
    if (this.checkOnline()) {
      const result = await this.call(endpoint, method, params);
      return { isDeferred: false, result };
    } else {
      const endpointRequest = { id: uuidv4(), endpoint, method, params };
      await this.cacheEndpointRequest(endpointRequest);
      return { isDeferred: true, endpointRequest };
    }
  }

  private checkOnline(): boolean {
    return navigator.onLine;
  }

  private async cacheEndpointRequest(endpointRequest: EndpointRequest) {
    await set(endpointRequest.id, endpointRequest, this.getOrCreateEndpointRequetsQueue());
  }

  private getOrCreateEndpointRequetsQueue(): Store {
    if (!this.endpointRequetsQueue) {
      this.endpointRequetsQueue = new Store('cached-vaadin-endpoint-requests');
    }
    return this.endpointRequetsQueue;
  }

  // Re-use flow loading indicator when fetching endpoints
  private loading(action: boolean) {
    if ($wnd.Vaadin.Flow?.loading) {
      $wnd.Vaadin.Flow.loading(action);
    }
  }
}

export interface LoginResult {
  token?: string;
  error: boolean;
  errorTitle: string;
  errorMessage: string;
}

export interface LoginOptions{
  loginProcessingUrl?: string;
  failureUrl?: string;
  defaultSuccessUrl?: string;
}

export interface LogoutOptions{
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
    const response = await fetch(loginProcessingUrl, {method: 'POST', body: data});

    const failureUrl = options && options.failureUrl ? options.failureUrl : '/login?error'; 
    const defaultSuccessUrl = options && options.defaultSuccessUrl ? options.defaultSuccessUrl : '/'
    // this assumes the default Spring Security form login configuration (handler URL and responses)
    if (response.ok && response.redirected && response.url.endsWith(failureUrl)) {
      result = {
        error: true,
        errorTitle: 'Incorrect username or password.',
        errorMessage: 'Check that you have entered the correct username and password and try again.'
      };
    } else if (response.ok && response.redirected && response.url.endsWith(defaultSuccessUrl)) {
      // TODO: find a more efficient way to get a new CSRF token
      // parsing the full response body just to get a token may be wasteful
      const token = getCsrfTokenFromResponseBody(await response.text());
      if (token) {
        (window as any).Vaadin.TypeScript = (window as any).Vaadin.TypeScript || {};
        (window as any).Vaadin.TypeScript.csrfToken = token;
        result = {
          error: false,
          errorTitle: '',
          errorMessage: '',
          token
        };
      }
    }
  } catch (e) {
    result = {
      error: true,
      errorTitle: e.name,
      errorMessage: e.message
    }
  }

  return result || {
    error: true,
    errorTitle: 'Error',
    errorMessage: 'Something went wrong when trying to login.',
  };
}

/**
 * A helper method for Spring Security based form logout
 * @param options defines additional options, e.g, the logoutUrl.
 */
export async function logout(options?: LogoutOptions) {
  // this assumes the default Spring Security logout configuration (handler URL)
  const logoutUrl = options && options.logoutUrl ? options.logoutUrl : '/logout';
  const response = await fetch(logoutUrl);

  // TODO: find a more efficient way to get a new CSRF token
  // parsing the full response body just to get a token may be wasteful
  const token = getCsrfTokenFromResponseBody(await response.text());
  (window as any).Vaadin.TypeScript.csrfToken = token;
}

const getCsrfTokenFromResponseBody = (body: string): string | undefined => {
  const match = body.match(/window\.Vaadin = \{TypeScript: \{"csrfToken":"([0-9a-zA-Z\-]{36})"}};/i);
  return match ? match[1] : undefined;
}

type EndpointCallContinue = (token: string) => void;

/**
 * It defines what to do when it detects a session is invalid. E.g., 
 * show a login view.
 * It takes an <code>EndpointCallContinue</code> parameter, which can be 
 * used to continue the endpoint call.  
 */
export type OnInvalidSessionCallback = (continueFunc: EndpointCallContinue) => void;

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
      return new Promise(async resolve => {
        const continueFunc = (token: string) => {
          clonedContext.request.headers.set('X-CSRF-Token', token);
          resolve(next(clonedContext));
        }
        this.onInvalidSessionCallback(continueFunc);
      });
    } else {
      return response;
    }
  }
}

interface EndpointRequest{
  id: string;
  endpoint: string;
  method: string;
  params?: any;
}

export interface DeferrableResult<T> {
  isDeferred: boolean;
  endpointRequest?: EndpointRequest;
  result?: T;
}