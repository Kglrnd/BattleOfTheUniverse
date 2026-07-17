import { TestBed } from '@angular/core/testing';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { of, throwError } from 'rxjs';

import { AdminResetComponent } from './admin-reset.component';
import { AdminGameApiService } from './admin-game-api.service';

describe('AdminResetComponent', () => {
  let confirmSpy: ReturnType<typeof vi.spyOn>;

  async function setup(reset: ReturnType<typeof vi.fn> = vi.fn(() => of(undefined))) {
    await TestBed.configureTestingModule({
      imports: [
        AdminResetComponent,
        TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
      ],
      providers: [{ provide: AdminGameApiService, useValue: { reset } }]
    }).compileComponents();

    const fixture = TestBed.createComponent(AdminResetComponent);
    fixture.detectChanges();
    return { fixture, reset };
  }

  afterEach(() => confirmSpy?.mockRestore());

  it('does nothing when the user cancels the confirmation dialog', async () => {
    confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(false);
    const { fixture, reset } = await setup();

    fixture.componentInstance.reset();

    expect(reset).not.toHaveBeenCalled();
  });

  it('resets the game and shows a done message when confirmed', async () => {
    confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true);
    const { fixture, reset } = await setup();

    fixture.componentInstance.reset();

    expect(reset).toHaveBeenCalled();
    expect(fixture.componentInstance['doneMessage']()).toBeTruthy();
    expect(fixture.componentInstance['sending']()).toBe(false);
  });

  it('shows an error message when the reset fails', async () => {
    confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true);
    const failingReset = vi.fn(() => throwError(() => ({ error: { message: 'failed hard' } })));
    const { fixture } = await setup(failingReset);

    fixture.componentInstance.reset();

    expect(fixture.componentInstance['errorMessage']()).toBe('failed hard');
  });

  it('ignores a reset call while already sending', async () => {
    confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true);
    const { fixture, reset } = await setup();
    fixture.componentInstance['sending'].set(true);

    fixture.componentInstance.reset();

    expect(reset).not.toHaveBeenCalled();
  });
});
