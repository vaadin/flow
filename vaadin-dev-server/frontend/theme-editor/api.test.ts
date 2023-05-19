import { aTimeout, expect } from '@open-wc/testing';
import sinon from 'sinon';
import { Commands, ResponseCode, ServerCssRule, ThemeEditorApi } from './api';

describe('theme editor API', () => {
  let api: ThemeEditorApi;
  let onMessageSpy: sinon.SinonSpy;
  let connectionMock: {
    onMessage: sinon.SinonSpy;
    send: sinon.SinonSpy;
  };

  beforeEach(() => {
    onMessageSpy = sinon.spy(() => {});
    connectionMock = {
      onMessage: onMessageSpy,
      send: sinon.spy(() => {})
    };
    api = new ThemeEditorApi(connectionMock as any);
  });

  function message(command: string, data: any) {
    return {
      command,
      data
    };
  }

  it('should pass through unhandled messages', () => {
    const unknownMessage = message('unknown1', {});
    connectionMock.onMessage(unknownMessage);

    expect(onMessageSpy.called).to.be.true;
    expect(onMessageSpy.args[0][0]).to.equal(unknownMessage);
  });

  it('should not pass through handled messages', () => {
    connectionMock.onMessage(message(Commands.response, { requestId: '0' }));

    expect(onMessageSpy.called).to.be.false;
  });

  it('should send messages', () => {
    const rules: ServerCssRule[] = [
      { selector: 'vaadin-button', properties: { background: 'red' } },
      { selector: 'vaadin-text-field', properties: { color: '' } }
    ];

    api.setCssRules(rules);

    expect(connectionMock.send.calledOnce).to.be.true;
    expect(connectionMock.send.args[0][0]).to.equal(Commands.setCssRules);
    expect(connectionMock.send.args[0][1]).to.deep.equal({
      requestId: '0',
      rules: rules,
      uiId: -1
    });
  });

  it('should resolve request when receiving ok response', async () => {
    const resolveSpy = sinon.spy();
    const rejectSpy = sinon.spy();
    api.setCssRules([]).then(resolveSpy).catch(rejectSpy);

    const responseData = { requestId: '0', code: ResponseCode.ok };
    connectionMock.onMessage(message(Commands.response, responseData));
    await aTimeout(0);

    expect(resolveSpy.called).to.be.true;
    expect(resolveSpy.calledWith(responseData)).to.be.true;
    expect(rejectSpy.called).to.be.false;
  });

  it('should reject request when receiving error response', async () => {
    const resolveSpy = sinon.spy();
    const rejectSpy = sinon.spy();
    api.setCssRules([]).then(resolveSpy).catch(rejectSpy);

    const responseData = { requestId: '0', code: ResponseCode.error };
    connectionMock.onMessage(message(Commands.response, responseData));
    await aTimeout(0);

    expect(resolveSpy.called).to.be.false;
    expect(rejectSpy.called).to.be.true;
    expect(rejectSpy.calledWith(responseData)).to.be.true;
  });

  it('should not resolve or reject request when receiving unknown request ID', async () => {
    const resolveSpy = sinon.spy();
    const rejectSpy = sinon.spy();
    api.setCssRules([]).then(resolveSpy).catch(rejectSpy);

    const responseData = { requestId: 'unknown', code: ResponseCode.error };
    connectionMock.onMessage(message(Commands.response, responseData));
    await aTimeout(0);

    expect(resolveSpy.called).to.be.false;
    expect(rejectSpy.called).to.be.false;
  });

  it('should increase request ID', () => {
    api.setCssRules([]);
    api.setCssRules([]);
    api.setCssRules([]);

    expect(connectionMock.send.calledThrice).to.be.true;
    expect(connectionMock.send.args[0][1].requestId).to.equal('0');
    expect(connectionMock.send.args[1][1].requestId).to.equal('1');
    expect(connectionMock.send.args[2][1].requestId).to.equal('2');
  });
});
