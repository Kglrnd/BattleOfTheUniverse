import { TestBed } from '@angular/core/testing';
import { TranslocoService } from '@jsverse/transloco';
import { of } from 'rxjs';

import { AuthService } from '../auth.service';
import { LanguageSwitcherComponent } from './language-switcher.component';

describe('LanguageSwitcherComponent', () => {
  let setActiveLang: ReturnType<typeof vi.fn>;
  let updateLanguage: ReturnType<typeof vi.fn>;
  let isAuthenticated: boolean;

  async function setup() {
    setActiveLang = vi.fn();
    updateLanguage = vi.fn(() => of({}));
    isAuthenticated = false;

    await TestBed.configureTestingModule({
      imports: [LanguageSwitcherComponent],
      providers: [
        { provide: TranslocoService, useValue: { activeLang: () => 'en', setActiveLang } },
        { provide: AuthService, useValue: { isAuthenticated: () => isAuthenticated, updateLanguage } }
      ]
    }).compileComponents();
  }

  it('renders a button per available language', async () => {
    await setup();
    const fixture = TestBed.createComponent(LanguageSwitcherComponent);
    fixture.detectChanges();

    const buttons = fixture.nativeElement.querySelectorAll('button');
    expect(buttons.length).toBe(2);
  });

  it('select() sets the active language and persists it, without updating the account when unauthenticated', async () => {
    localStorage.clear();
    await setup();
    isAuthenticated = false;
    const fixture = TestBed.createComponent(LanguageSwitcherComponent);
    fixture.detectChanges();

    fixture.componentInstance.select('de');

    expect(setActiveLang).toHaveBeenCalledWith('de');
    expect(localStorage.getItem('lang')).toBe('de');
    expect(updateLanguage).not.toHaveBeenCalled();
  });

  it('select() also updates the account language when authenticated', async () => {
    await setup();
    isAuthenticated = true;
    const fixture = TestBed.createComponent(LanguageSwitcherComponent);
    fixture.detectChanges();

    fixture.componentInstance.select('de');

    expect(updateLanguage).toHaveBeenCalledWith('de');
  });
});
