import { expect } from '@open-wc/testing';
import sinon from 'sinon';
import { ThemeEditorHistory } from './history';

describe('history', () => {
  let history: ThemeEditorHistory;
  let apiMock: {
    undo: sinon.SinonSpy;
    redo: sinon.SinonSpy;
  };

  beforeEach(() => {
    apiMock = {
      undo: sinon.spy(() => Promise.resolve()),
      redo: sinon.spy(() => Promise.resolve())
    };

    ThemeEditorHistory.clear();
    history = new ThemeEditorHistory(apiMock as any);
  });

  it('should not allow undo or redo initially', () => {
    expect(history.allowUndo).to.be.false;
    expect(history.allowRedo).to.be.false;
    expect(history.allowedActions.allowUndo).to.be.false;
    expect(history.allowedActions.allowRedo).to.be.false;

    history.undo();
    history.redo();

    expect(apiMock.undo.called).to.be.false;
    expect(apiMock.redo.called).to.be.false;
  });

  it('should undo requests', () => {
    history.push('request1');
    history.push('request2');
    history.push('request3');

    history.undo();
    history.undo();
    history.undo();

    expect(apiMock.undo.calledThrice).to.be.true;
    expect(apiMock.undo.args).to.deep.equal([['request3'], ['request2'], ['request1']]);
  });

  it('should only undo until there are no more previous entries', () => {
    history.push('request1');

    history.undo();
    history.undo();
    history.undo();

    expect(apiMock.undo.calledOnce).to.be.true;
    expect(apiMock.undo.args).to.deep.equal([['request1']]);
  });

  it('should only allow undo if there are previous entries', () => {
    history.push('request1');

    expect(history.allowUndo).to.be.true;
    expect(history.allowedActions.allowUndo).to.be.true;

    history.undo();

    expect(history.allowUndo).to.be.false;
    expect(history.allowedActions.allowUndo).to.be.false;

    history.redo();

    expect(history.allowUndo).to.be.true;
    expect(history.allowedActions.allowUndo).to.be.true;
  });

  it('should redo requests', () => {
    history.push('request1');
    history.push('request2');
    history.push('request3');

    history.undo();
    history.undo();
    history.undo();

    history.redo();
    history.redo();
    history.redo();

    expect(apiMock.redo.calledThrice).to.be.true;
    expect(apiMock.redo.args).to.deep.equal([['request1'], ['request2'], ['request3']]);
  });

  it('should only redo until there are no more next entries', () => {
    history.push('request1');

    history.undo();

    history.redo();
    history.redo();
    history.redo();

    expect(apiMock.redo.calledOnce).to.be.true;
    expect(apiMock.redo.args).to.deep.equal([['request1']]);
  });

  it('should only allow redo if there are next entries', () => {
    history.push('request1');
    history.undo();

    expect(history.allowRedo).to.be.true;
    expect(history.allowedActions.allowRedo).to.be.true;

    history.redo();

    expect(history.allowRedo).to.be.false;
    expect(history.allowedActions.allowRedo).to.be.false;

    history.undo();

    expect(history.allowRedo).to.be.true;
    expect(history.allowedActions.allowRedo).to.be.true;
  });

  it('should remove forward entries when pushing new entries', () => {
    history.push('request1');
    history.undo();
    apiMock.undo.resetHistory();

    expect(history.allowRedo).to.be.true;
    expect(history.allowedActions.allowRedo).to.be.true;

    history.push('request2');

    expect(history.allowRedo).to.be.false;
    expect(history.allowedActions.allowRedo).to.be.false;

    history.undo();
    history.undo();
    history.undo();

    expect(apiMock.undo.calledOnce).to.be.true;
  });

  it('should store history state as singleton', () => {
    history.push('request1');

    history = new ThemeEditorHistory(apiMock as any);

    expect(history.allowUndo).to.be.true;
    expect(history.allowedActions.allowUndo).to.be.true;

    history.undo();

    expect(apiMock.undo.calledOnce).to.be.true;
    expect(apiMock.undo.args).to.deep.equal([['request1']]);
  });

  describe('customizer functions', () => {
    let execute: sinon.SinonSpy;
    let rollback: sinon.SinonSpy;

    beforeEach(() => {
      execute = sinon.spy();
      rollback = sinon.spy();
    });

    it('should run execute customizer when pushing history entry', () => {
      history.push('request1', execute, rollback);

      expect(execute.calledOnce).to.be.true;
      expect(rollback.called).to.be.false;
    });

    it('should run rollback customizer on undo', async () => {
      history.push('request1', execute, rollback);
      execute.resetHistory();
      rollback.resetHistory();

      await history.undo();
      expect(execute.called).to.be.false;
      expect(rollback.calledOnce).to.be.true;
    });

    it('should run execute customizer on redo', async () => {
      history.push('request1', execute, rollback);
      await history.undo();
      execute.resetHistory();
      rollback.resetHistory();

      await history.redo();
      expect(execute.calledOnce).to.be.true;
      expect(rollback.called).to.be.false;
    });
  });
});
