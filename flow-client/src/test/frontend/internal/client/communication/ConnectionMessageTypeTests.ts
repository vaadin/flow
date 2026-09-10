// Beyond the Java suite: ConnectionMessageType has no Java test class in src/test/java or src/test-gwt/java —
// the enum it ports is nested in DefaultConnectionStateHandler, whose Gwt test covers the handler rather than the priorities — so every case here is beyond the Java suite.
import { expect } from '@open-wc/testing';
import {
  ConnectionMessageType,
  isHigherPriorityThan,
  isMessage
} from '../../../../../main/frontend/internal/client/communication/ConnectionMessageType';

describe('ConnectionMessageType', () => {
  it('classifies push and XHR as message transports, heartbeat as not', () => {
    expect(isMessage(ConnectionMessageType.HEARTBEAT)).to.be.false;
    expect(isMessage(ConnectionMessageType.PUSH)).to.be.true;
    expect(isMessage(ConnectionMessageType.XHR)).to.be.true;
  });

  it('orders priority XHR > PUSH > HEARTBEAT', () => {
    expect(isHigherPriorityThan(ConnectionMessageType.XHR, ConnectionMessageType.PUSH)).to.be.true;
    expect(isHigherPriorityThan(ConnectionMessageType.PUSH, ConnectionMessageType.HEARTBEAT)).to.be.true;
    expect(isHigherPriorityThan(ConnectionMessageType.XHR, ConnectionMessageType.HEARTBEAT)).to.be.true;

    expect(isHigherPriorityThan(ConnectionMessageType.HEARTBEAT, ConnectionMessageType.XHR)).to.be.false;
    expect(isHigherPriorityThan(ConnectionMessageType.PUSH, ConnectionMessageType.XHR)).to.be.false;
  });

  it('is not higher priority than itself', () => {
    expect(isHigherPriorityThan(ConnectionMessageType.XHR, ConnectionMessageType.XHR)).to.be.false;
  });
});
