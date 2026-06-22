import { expect } from '@open-wc/testing';
import { resendRequest } from '../../main/frontend/internal/XhrConnection';

describe('XhrConnection', () => {
  it('resendRequest re-sends a request still in the OPENED state', () => {
    let sent = false;
    const xhr = {
      readyState: 1,
      send: () => {
        sent = true;
      }
    } as unknown as XMLHttpRequest;
    expect(resendRequest(xhr)).to.be.true;
    expect(sent).to.be.true;
  });

  it('resendRequest returns false when the request has progressed', () => {
    const xhr = { readyState: 4, send: () => {} } as unknown as XMLHttpRequest;
    expect(resendRequest(xhr)).to.be.false;
  });

  it('resendRequest returns false when send throws (running for real)', () => {
    const xhr = {
      readyState: 1,
      send: () => {
        throw new Error('running');
      }
    } as unknown as XMLHttpRequest;
    expect(resendRequest(xhr)).to.be.false;
  });
});
