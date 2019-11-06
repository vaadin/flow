import {ConnectClient} from '@vaadin/flow-frontend/Connect';

export const client = new ConnectClient({endpoint: 'connect'});

export const nullIfUndefined = function(obj: any) {
  return obj === undefined ? null : obj;
}
