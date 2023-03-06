import { Connection } from '../vaadin-dev-tools';
import { ThemeEditorRule } from './model';

export enum Commands {
  response = 'themeEditorResponse',
  updateCssRules = 'themeEditorRules'
}

export enum ResponseCode {
  ok = 'ok',
  error = 'error'
}

export interface BaseResponse {
  requestId: string;
  code: ResponseCode;
}

interface RequestHandle {
  resolve: (response: unknown) => void;
  reject: (response: unknown) => void;
}

export class ThemeEditorApi {
  private wrappedConnection: Connection;
  private pendingRequests: { [key: string]: RequestHandle } = {};
  private requestCounter: number = 0;

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

    return new Promise<any>((resolve, reject) => {
      this.wrappedConnection.send(command, {
        ...data,
        requestId
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

  public updateCssRules(add: ThemeEditorRule[], remove: ThemeEditorRule[]): Promise<BaseResponse> {
    return this.sendRequest(Commands.updateCssRules, {
      add,
      remove
    });
  }
}
