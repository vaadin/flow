/// <reference types="intern" />
const { describe, it } = intern.getPlugin('interface.bdd');
const { expect } = intern.getPlugin('chai');
describe('Flow', () => {
    it('should work', () => {
        expect(true).to.be.true;
    });
});