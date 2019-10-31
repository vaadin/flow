interface ConnectExceptionData {
  message: string;
  type: string;
  detail?: any;
  validationErrorData?: ValidationErrorData[];
}

const throwConnectException = (errorJson: ConnectExceptionData) => {
  if (errorJson.validationErrorData) {
    throw new VaadinConnectValidationError(
      errorJson.message,
      errorJson.validationErrorData,
      errorJson.type
    );
  } else {
    throw new VaadinConnectError(
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
      throw new VaadinConnectError(errorText);
    } else {
      throw new VaadinConnectError(
        'expected "200 OK" response, but got ' +
        `${response.status} ${response.statusText}`
      );
    }
  }
};

/**
 * Authenticate a Vaadin Connect client
 * @param client the connect client instance
 * @param askForCredentials if no valid tokens are found, ask for credentials,
 * `true` by default
 * @ignore
 */
const authenticateClient = async(client: ConnectClient,
                                 askForCredentials: boolean = true):
  Promise<AccessToken | null> => {
  let message;
  const _private = privates.get(client);
  let tokens = _private.tokens;

  while (!(tokens.accessToken && tokens.accessToken.isValid())) {

    let stayLoggedIn = tokens.stayLoggedIn;

    // delete current credentials because we are going to take new ones
    _private.tokens = new AuthTokens().save();

    const body = new URLSearchParams();
    if (tokens.refreshToken && tokens.refreshToken.isValid()) {
      body.append('grant_type', 'refresh_token');
      body.append('refresh_token', tokens.refreshToken.token);
    } else if (askForCredentials && client.credentials) {
      const creds = message !== undefined
        ? await client.credentials({message})
        : await client.credentials();
      if (!creds) {
        // No credentials returned, skip the token request
        break;
      }
      if (creds.username && creds.password) {
        body.append('grant_type', 'password');
        body.append('username', creds.username);
        body.append('password', creds.password);
        stayLoggedIn = creds.stayLoggedIn;
      }
    } else {
      break;
    }

    if (body.has('grant_type')) {
      const tokenResponse = await fetch(client.tokenEndpoint, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: body.toString(),
        signal: _private.controller.signal
      });

      if (tokenResponse.status === 400 || tokenResponse.status === 401) {
        const invalidResponse = await tokenResponse.json();
        if (invalidResponse.error === 'invalid_token') {
          tokens = new AuthTokens();
        }
        // Wrong credentials response, loop to ask again with the message
        message = invalidResponse.error_description;
      } else {
        await assertResponseIsOk(tokenResponse);
        // Successful token response
        tokens = new AuthTokens(await tokenResponse.json());
        _private.tokens = tokens;
        if (stayLoggedIn) {
          tokens.save();
        }
        break;
      }
    }
  }
  return tokens.accessToken;
};

/** @ignore */
const privates = new WeakMap();

/** @ignore */
const refreshTokenKey = 'vaadin.connect.refreshToken';

/** @ignore */
class Token {
  token: string;
  json: any;

  constructor(token: string) {
    this.token = token;
    this.json = JSON.parse(atob(token.split('.')[1]));
  }

  isValid(): boolean {
    return this.json.exp > Date.now() / 1000;
  }
}

/** @ignore */
interface AuthJson {
  access_token: string;
  refresh_token: string;
}

/** @ignore */
class AuthTokens {
  accessToken?: Token;
  refreshToken?: Token;
  stayLoggedIn?: boolean;

  constructor(authJson?: AuthJson) {
    if (authJson) {
      this.accessToken = new Token(authJson.access_token);
      this.refreshToken = new Token(authJson.refresh_token);
    }
  }

  save() {
    if (this.refreshToken) {
      localStorage.setItem(refreshTokenKey, this.refreshToken.token);
      this.stayLoggedIn = true;
    } else {
      localStorage.removeItem(refreshTokenKey);
    }
    return this;
  }

  restore() {
    const token = localStorage.getItem(refreshTokenKey);
    if (token) {
      try {
        this.refreshToken = new Token(token);
        this.stayLoggedIn = true;
      } catch (e) {
        // stored token is corrupted, remove it
        this.save();
      }
    }
    return this;
  }
}

/**
 * An exception that gets thrown when the Vaadin Connect backend responds
 * with not ok status.
 */
export class VaadinConnectError extends Error {
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
    super(
      `Message: '${message}', additional details: '${JSON.stringify(detail)}'`);
    this.type = type;
    this.detail = detail;
  }
}

/**
 * An exception that gets thrown if Vaadin Connect backend responds
 * with non-ok status and provides additional info
 * on the validation errors occurred.
 */
export class VaadinConnectValidationError extends VaadinConnectError {
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
 * The Access Token structure returned by the authentication server.
 */
export interface AccessToken {
  /**
   * The user used in credentials.
   */
  user_name: string;

  /**
   * The expiration time in Unix time format.
   */
  exp: number;

  /**
   * The list of the roles that the token meets.
   */
  authorities: string[];
}

/**
 * An object to provide user credentials for authorization grants.
 */
export interface Credentials {
  username: string;
  password: string;
  stayLoggedIn?: boolean;
}

export interface CredentialsCallbackOptions {
  /**
   * When credentials are asked again, contains
   * the error description from last token response.
   */
  message?: string;
}

/**
 * An async callback function providing credentials for authorization.
 * @param options
 */
export type CredentialsCallback = (options?: CredentialsCallbackOptions) =>
  Promise<Credentials | undefined> | Credentials | undefined;

/**
 * The `ConnectClient` constructor options.
 */
export interface ConnectClientOptions {
  /**
   * The `endpoint` property value.
   */
  endpoint?: string;

  /**
   * The `tokenEndpoint` property value.
   */
  tokenEndpoint?: string;

  /**
   * The `credentials` property value.
   */
  credentials?: CredentialsCallback;

  /**
   * The `middlewares` property value.
   */
  middlewares?: Middleware[];
}

export interface CallOptions {
  /**
   * Require authentication.
   */
  requireCredentials?: boolean;
}

/**
 * An object with the call arguments and the related Request instance.
 * See also {@link ConnectClient.call | the call() method in ConnectClient}.
 */
export interface MiddlewareContext {
  /**
   * The service class name.
   */
  service: string;

  /**
   * The method name to call on in the service class.
   */
  method: string;

  /**
   * Optional object with method call arguments.
   */
  params?: any;

  /**
   * Client options related with the call.
   */
  options: CallOptions;

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
 * An async callback function that can intercept the request and response
 * of a call.
 * @param context The information about the call and request
 * @param next Invokes the next in the call chain
 */
export type Middleware = (context: MiddlewareContext, next: MiddlewareNext) =>
  Promise<Response> | Response;

/**
 * Vaadin Connect client class is a low-level network calling utility. It stores
 * an endpoint and facilitates remote calls to services and methods
 * on the Vaadin Connect backend.
 *
 * Example usage:
 *
 * ```js
 * const client = new ConnectClient();
 * const responseData = await client.call('MyVaadinService', 'myMethod');
 * ```
 *
 * ### Endpoint
 *
 * The client supports an `endpoint` constructor option:
 * ```js
 * const client = new ConnectClient({endpoint: '/my-connect-endpoint'});
 * ```
 *
 * The default endpoint is '/connect'.
 *
 * ### Authorization
 *
 * The Connect client does OAuth 2 access token requests using
 * the `tokenEndpoint` constructor option.
 *
 * Supports the password credentials grant, which uses a username/password
 * pair provided by the `credentials` async callback constructor option:
 *
 * ```js
 * new ConnectClient({
 *   credentials: async() => {
 *     return {username: 'user', password: 'abc123'};
 *   }
 * });
 * ```
 *
 * The default token endpoint is '/oauth/token'.
 *
 * By default, the client requires authorization for calls, therefore
 * the `credentials` callback is called before a non-authorized client
 * is about to make a call. You can omit the authorization requirement using
 * the `requireCredentials: false` call option:
 *
 * ```js
 * const params = {};
 * await client.call('MyVaadinService', 'myMethod', params, {
 *   requireCredentials: false
 * });
 * ```
 */
export class ConnectClient {
  /**
   * The Vaadin Connect backend endpoint
   */
  endpoint: string = '/connect';

  /**
   * The Vaadin Connect OAuth 2 token endpoint.
   */
  tokenEndpoint: string = '/oauth/token';

  /**
   * Called when the client needs a username/password pair to authorize through
   * the token endpoint. When undefined or returns a falsy value,
   * the authorization is skipped, the requests made by the `call` method
   * would not include the authorization header.
   */
  credentials?: CredentialsCallback;

  /**
   * The array of middlewares that are invoked during a call.
   */
  middlewares: Middleware[] = [];

  /**
   * @param options Constructor options.
   */
  constructor(options: ConnectClientOptions = {}) {
    if (options.endpoint) {
      this.endpoint = options.endpoint;
    }

    if (options.tokenEndpoint) {
      this.tokenEndpoint = options.tokenEndpoint;
    }

    if (options.credentials) {
      this.credentials = options.credentials;
    }

    if (options.middlewares) {
      this.middlewares = options.middlewares;
    }

    privates.set(this, {
      controller: new AbortController(),
      tokens: new AuthTokens().restore()
    });
  }

  /**
   * Remove current accessToken and refreshToken, and cancel any authentication
   * request that might be in progress.
   *
   * After calling `logout()`, any new service call will ask for
   * user credentials.
   */
  async logout() {
    const _private = privates.get(this);
    _private.controller.abort();
    // controller signed as aborted cannot be reused
    _private.controller = new AbortController();
    _private.tokens = new AuthTokens().save();
  }

  /**
   * The access token returned by the authorization server.
   */
  get token(): AccessToken {
    const token = privates.get(this).tokens.accessToken;
    return token && Object.assign({}, token.json);
  }

  /**
   * Makes a JSON HTTP request to the `${endpoint}/${service}/${method}` URL,
   * optionally supplying the provided params as a JSON request body,
   * and asynchronously returns the parsed JSON response data.
   *
   * @param service Service class name.
   * @param method Method name to call in the service class.
   * @param params Optional object to be send in JSON request body.
   * @param options Optional client options for this call.
   * @returns {} Decoded JSON response data.
   */
  async call(
    service: string,
    method: string,
    params?: any,
    options: CallOptions = {}
  ): Promise<any> {
    if (arguments.length < 2) {
      throw new TypeError(
        `2 arguments required, but got only ${arguments.length}`
      );
    }

    options = Object.assign({requireCredentials: true}, options);
    if (options.requireCredentials) {
      await this.login();
    }

    const accessToken = privates.get(this).tokens.accessToken;
    const headers: Record<string, string> = {
      'Accept': 'application/json',
      'Content-Type': 'application/json'
    };
    if (accessToken) {
      // tslint:disable-next-line:no-string-literal
      headers['Authorization'] = `Bearer ${accessToken.token}`;
    }

    // Construct a Request instance from arguments
    const request = new Request(
      `${this.endpoint}/${service}/${method}`,
      {
        method: 'POST',
        headers,
        body: params !== undefined ? JSON.stringify(params) : undefined
      }
    );

    // The middleware `context`, includes the call arguments and the request
    // constructed from them
    const initialContext: MiddlewareContext = {
      service,
      method,
      params,
      options,
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
        return await fetch(context.request);
      };

    // Assemble the final middlewares array from internal
    // and external middlewares
    const middlewares = [responseHandlerMiddleware].concat(this.middlewares);

    // Fold the final middlewares array into a single function
    const chain = middlewares.reduceRight(
      (next: MiddlewareNext, middleware: Middleware) => {
        // Compose and return the new chain step, that takes the context and
        // invokes the current middleware with the context and the further chain
        // as the next argument
        return (context => middleware(context, next)) as MiddlewareNext;
      },
      // Initialize reduceRight the accumulator with `fetchNext`
      fetchNext
    );

    // Invoke all the folded async middlewares and return
    return await chain(initialContext);
  }

  /**
   * Makes a HTTP request to the {@link ConnectClient#tokenEndpoint} URL
   * to login and get the accessToken if the tokens {@link ConnectClient#token}
   * is not available or invalid. The {@link ConnectClient#credentials}
   * will be called if the `refreshToken` is invalid.
   *
   * @return a promise the the token that is used to access a service
   */
  async login(): Promise<AccessToken> {
    const _private = privates.get(this);
    // memoize to re-use in case of multiple calls
    _private.login = _private.login || authenticateClient(this);
    const token = await _private.login;
    delete _private.login;
    return token;
  }

  /**
   * Checks if the user is logged in.
   * If there saved tokens, tries to log in the user,
   * not asking for the credentials.
   *
   * @return {@code true} if the log in successful, {@code false} otherwise
   */
  async checkLoggedIn(): Promise<boolean> {
    return authenticateClient(this, false)
      .then(token => token !== null);
  }
}
