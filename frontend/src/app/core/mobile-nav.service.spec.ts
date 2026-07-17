import { MobileNavService } from './mobile-nav.service';

describe('MobileNavService', () => {
  it('starts closed', () => {
    const service = new MobileNavService();
    expect(service.isOpen()).toBe(false);
  });

  it('toggle flips the open state', () => {
    const service = new MobileNavService();
    service.toggle();
    expect(service.isOpen()).toBe(true);
    service.toggle();
    expect(service.isOpen()).toBe(false);
  });

  it('close forces the state closed even when already open', () => {
    const service = new MobileNavService();
    service.toggle();
    service.close();
    expect(service.isOpen()).toBe(false);
  });
});
