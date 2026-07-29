import { TestBed } from '@angular/core/testing';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { of, throwError } from 'rxjs';

import { AppSettingsView } from '../../core/models';
import { AdminSettingsApiService } from './admin-settings-api.service';
import { AdminSettingsComponent } from './admin-settings.component';

describe('AdminSettingsComponent', () => {
  async function setup(
    get: ReturnType<typeof vi.fn> = vi.fn(() => of<AppSettingsView>({ registrationEnabled: true })),
    updateRegistrationEnabled: ReturnType<typeof vi.fn> = vi.fn(() => of<AppSettingsView>({ registrationEnabled: false }))
  ) {
    await TestBed.configureTestingModule({
      imports: [
        AdminSettingsComponent,
        TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
      ],
      providers: [{ provide: AdminSettingsApiService, useValue: { get, updateRegistrationEnabled } }]
    }).compileComponents();

    const fixture = TestBed.createComponent(AdminSettingsComponent);
    fixture.detectChanges();
    return { fixture, get, updateRegistrationEnabled };
  }

  it('loads and exposes the current registration setting', async () => {
    const { fixture } = await setup();
    expect(fixture.componentInstance['registrationEnabled']()).toBe(true);
  });

  it('surfaces a load error message', async () => {
    const failingGet = vi.fn(() => throwError(() => Object.assign(new Error('boom'), { error: { message: 'load failed' } })));
    const { fixture } = await setup(failingGet);
    expect(fixture.componentInstance['errorMessage']()).toBe('load failed');
  });

  it('toggleRegistration flips the setting and updates local state', async () => {
    const { fixture, updateRegistrationEnabled } = await setup();

    fixture.componentInstance.toggleRegistration();

    expect(updateRegistrationEnabled).toHaveBeenCalledWith(false);
    expect(fixture.componentInstance['registrationEnabled']()).toBe(false);
    expect(fixture.componentInstance['saving']()).toBe(false);
  });

  it('surfaces an error when updating fails', async () => {
    const failingUpdate = vi.fn(() => throwError(() => ({ error: { message: 'denied' } })));
    const { fixture } = await setup(undefined, failingUpdate);

    fixture.componentInstance.toggleRegistration();

    expect(fixture.componentInstance['errorMessage']()).toBe('denied');
    expect(fixture.componentInstance['saving']()).toBe(false);
  });

  it('ignores a toggle call while already saving', async () => {
    const { fixture, updateRegistrationEnabled } = await setup();
    fixture.componentInstance['saving'].set(true);

    fixture.componentInstance.toggleRegistration();

    expect(updateRegistrationEnabled).not.toHaveBeenCalled();
  });
});
