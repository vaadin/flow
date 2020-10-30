/* tslint:disable:max-classes-per-file */
import { DBSchema, IDBPDatabase, openDB } from 'idb';

const REQUEST_QUEUE_DB_NAME = 'request-queue';
const REQUEST_QUEUE_STORE_NAME = 'requests';

/**
 * The callback for deferred calls
 */
export type OnDeferredCallCallback = (deferrableCall: DeferredCall) => Promise<void>;

type EndpointCallSubmitFn = (
  isDeferred: boolean,
  endpoint: string,
  method: string,
  params?: any
) =>  Promise<any>;

export interface DeferredCallHandler {
  handleDeferredCall: OnDeferredCallCallback;
}

export class DeferredCall implements EndpointRequest{
  id?: number;
  endpoint: string;
  method: string;
  params?: any;
  submitting?: boolean;
  private _keepInTheQueue = false;
  private _submitFunction: (call: EndpointRequest)=>Promise<any>;
  constructor(endpointCall: EndpointRequest, submitFunction: (call: EndpointRequest)=>Promise<any>){
    this.id = endpointCall.id;
    this.endpoint = endpointCall.endpoint;
    this.method = endpointCall.method;
    this.params = endpointCall.params;
    this._submitFunction = submitFunction;
  }
  
  async submit(): Promise<any>{
    return this._submitFunction(this);
  }

  keepInTheQueue(){
    this._keepInTheQueue = true;
  }
  _shouldKeepInTheQueue(){
    return this._keepInTheQueue;
  }
}
export interface EndpointRequest {
  id?: number;
  endpoint: string;
  method: string;
  params?: any;
  submitting?: boolean
}

export interface DeferrableResult<T> {
  isDeferred: boolean;
  endpointRequest?: EndpointRequest;
  result?: T;
}

export interface RequestQueueDB extends DBSchema {
  requests: {
    value: EndpointRequest;
    key: number;
  };
}

class Offline {
  checkOnline(): boolean {
    return navigator.onLine;
  }
  
  async cacheEndpointRequest(endpointRequest: EndpointRequest): Promise<EndpointRequest>{
    const db = await this.openOrCreateDB();
    const id = await db.add(REQUEST_QUEUE_STORE_NAME, endpointRequest);
    db.close();
    endpointRequest.id = id;
    return endpointRequest;
  }
  
  async processDeferredCalls(submitFunction: EndpointCallSubmitFn, deferredCallHandler?: DeferredCallHandler) {
    const db = await this.openOrCreateDB();
  
    /**
     * Cannot wait for submitting the cached requests in the indexed db transaction,
     * as the transaction only wait for db operations.
     * See https://github.com/jakearchibald/idb#transaction-lifetime
     */
    const shouldSubmit = await this.shouldSubmitCachedRequests(db);
  
    if (shouldSubmit) {
      await this.submitCachedRequests(db, submitFunction, deferredCallHandler);
    }
  
    db.close();
  }
  
  async openOrCreateDB(): Promise<IDBPDatabase<RequestQueueDB>> {
    return openDB<RequestQueueDB>(REQUEST_QUEUE_DB_NAME, 1, {
      upgrade(db) {
        db.createObjectStore(REQUEST_QUEUE_STORE_NAME, {
          keyPath: 'id',
          autoIncrement: true
        });
      },
    });
  }
  
  async shouldSubmitCachedRequests(db: IDBPDatabase<RequestQueueDB>) {
    let shouldSubmit = false;
    if (db.objectStoreNames.contains(REQUEST_QUEUE_STORE_NAME) && await db.count(REQUEST_QUEUE_STORE_NAME) > 0) {
      const tx = db.transaction(REQUEST_QUEUE_STORE_NAME, 'readwrite');
  
      let cursor = await tx.store.openCursor();
      while (cursor) {
        const request = cursor.value;
        if (!request.submitting) {
          shouldSubmit = true;
          request.submitting = true;
          cursor.update(request);
        }
        cursor = await cursor.continue();
      }
      await tx.done;
    }
    return shouldSubmit;
  }
  
  
  async submitCachedRequests(db: IDBPDatabase<RequestQueueDB>, submitFunction: EndpointCallSubmitFn, deferredCallHandler?: DeferredCallHandler) {
    const cachedRequests = await db.getAll(REQUEST_QUEUE_STORE_NAME);
    for (const request of cachedRequests) {
      if (request.submitting) {
        try {
          let shouldDelete = true;
          if (deferredCallHandler) {
            const deferredCall = new DeferredCall(request, ({endpoint, method, params}) => submitFunction(true, endpoint, method, params));
            await deferredCallHandler.handleDeferredCall(deferredCall);
            shouldDelete = !deferredCall._shouldKeepInTheQueue();
          } else {
            await submitFunction(true, request.endpoint, request.method, request.params);
          }
          if(shouldDelete){
            await db.delete(REQUEST_QUEUE_STORE_NAME, request.id!);
          }else{
            request.submitting = false;
            await db.put(REQUEST_QUEUE_STORE_NAME, request);
          }
        } catch (error) {
          request.submitting = false;
          await db.put(REQUEST_QUEUE_STORE_NAME, request);
          throw error;
        }
      }
    }
  }
}

export const offline = new Offline();