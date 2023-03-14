import { Connection } from '../connection';

export enum Commands {
  response = 'themeEditorResponse',
  setCssRules = 'themeEditorRules',
  loadPreview = 'themeEditorLoadPreview',
  loadRules = 'themeEditorLoadRules',
  history = 'themeEditorHistory'
}

export enum ResponseCode {
  ok = 'ok',
  error = 'error'
}

export interface BaseResponse {
  requestId: string;
  code: ResponseCode;
}

export interface LoadPreviewResponse extends BaseResponse {
  css: string;
}

export interface ServerCssRule {
  selector: string;
  properties: { [key: string]: string };
}

export interface LoadRulesResponse {
  rules: ServerCssRule[];
}

interface RequestHandle {
  resolve: (response: unknown) => void;
  reject: (response: unknown) => void;
}

export class ThemeEditorApi {
  private wrappedConnection: Connection;
  private pendingRequests: { [key: string]: RequestHandle } = {};
  private requestCounter: number = 0;
  private globalUiId: number = this.getGlobalUiId();

  constructor(wrappedConnection: Connection) {
    this.wrappedConnection = wrappedConnection;
    const prevOnMessage = this.wrappedConnection.onMessage;
    this.wrappedConnection.onMessage = (message: any) => {
      if (message.command === Commands.response) {
        this.handleResponse(message.data);
      } else {
        prevOnMessage.call(this.wrappedConnection, message);
      }
    };
  }

  private sendRequest(command: string, data: any) {
    const requestId = (this.requestCounter++).toString();
    const uiId = data['uiId'] ?? this.globalUiId;

    return new Promise<any>((resolve, reject) => {
      this.wrappedConnection.send(command, {
        ...data,
        requestId,
        uiId
      });
      this.pendingRequests[requestId] = {
        resolve,
        reject
      };
    });
  }

  private handleResponse(data: BaseResponse) {
    const requestHandle = this.pendingRequests[data.requestId];
    if (!requestHandle) {
      console.warn('Received response for unknown request');
      return;
    }

    delete this.pendingRequests[data.requestId];

    if (data.code === ResponseCode.ok) {
      requestHandle.resolve(data);
    } else {
      requestHandle.reject(data);
    }
  }

  public setCssRules(rules: ServerCssRule[]): Promise<BaseResponse> {
    return this.sendRequest(Commands.setCssRules, {
      rules
    });
  }

  public loadPreview(): Promise<LoadPreviewResponse> {
    return this.sendRequest(Commands.loadPreview, {});
  }

  public loadRules(selectorFilter: string): Promise<LoadRulesResponse> {
    return this.sendRequest(Commands.loadRules, { selectorFilter });
  }

  public undo(requestId: string) {
    return this.sendRequest(Commands.history, { undo: requestId });
  }

  public redo(requestId: string) {
    return this.sendRequest(Commands.history, { redo: requestId });
  }

  private getGlobalUiId(): number {
    const vaadin = (window as any).Vaadin;
    if (vaadin && vaadin.Flow) {
      const { clients } = vaadin.Flow;
      const appIds = Object.keys(clients);
      for (const appId of appIds) {
        const client = clients[appId];
        if (client.getNodeId) {
          return client.getUIId();
        }
      }
    }
    return -1;
  }
}
