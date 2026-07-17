import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { TranslocoService, TranslocoTestingModule } from '@jsverse/transloco';
import { of, throwError } from 'rxjs';

import { AuthService } from '../../core/auth.service';
import { PlanetView, UserView } from '../../core/models';
import { UniverseApiService } from '../universe/universe-api.service';
import { RegisterComponent } from './register.component';

function planet(id: number): PlanetView {
  return {
    id,
    name: `Planet ${id}`,
    galaxy: 1,
    system: 1,
    position: id,
    coordinates: `[1:1:${id}]`,
    planetClass: 'TEMPERATE',
    homeworld: true,
    researchEfficiency: 100,
    imageVariant: 0,
    destroyed: false
  };
}

describe('RegisterComponent', () => {
  let navigate: ReturnType<typeof vi.fn>;

  async function setup(
    register: ReturnType<typeof vi.fn> = vi.fn(() => of({} as UserView)),
    login: ReturnType<typeof vi.fn> = vi.fn(() => of({} as UserView)),
    getHomePlanet: ReturnType<typeof vi.fn> = vi.fn(() => of(planet(7)))
  ) {
    await TestBed.configureTestingModule({
      imports: [
        RegisterComponent,
        TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
      ],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { register, login } },
        { provide: UniverseApiService, useValue: { getHomePlanet } }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(RegisterComponent);
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
    return { fixture, register, login, getHomePlanet };
  }

  function fillValidForm(fixture: Awaited<ReturnType<typeof setup>>['fixture']) {
    fixture.componentInstance['form'].setValue({ username: 'player1', email: 'player@example.com', password: 'secret1' });
  }

  it('does not submit when the form is invalid', async () => {
    const { fixture, register } = await setup();
    fixture.componentInstance.submit();
    expect(register).not.toHaveBeenCalled();
  });

  it('registers, logs in, and navigates to the homeworld on success', async () => {
    const { fixture, register, login } = await setup();
    fillValidForm(fixture);
    const transloco = TestBed.inject(TranslocoService);
    vi.spyOn(transloco, 'getActiveLang').mockReturnValue('en');

    fixture.componentInstance.submit();

    expect(register).toHaveBeenCalledWith(
      expect.objectContaining({ username: 'player1', email: 'player@example.com', password: 'secret1', preferredLanguage: 'en' })
    );
    expect(login).toHaveBeenCalledWith('player1', 'secret1');
    expect(navigate).toHaveBeenCalledWith(['/universe', 7]);
  });

  it('navigates to /universe when fetching the homeworld fails after register+login', async () => {
    const failingGetHomePlanet: ReturnType<typeof vi.fn> = vi.fn(() => throwError(() => new Error('fail')));
    const { fixture } = await setup(undefined, undefined, failingGetHomePlanet);
    fillValidForm(fixture);

    fixture.componentInstance.submit();

    expect(navigate).toHaveBeenCalledWith(['/universe']);
  });

  it('navigates to /login when auto-login after registration fails', async () => {
    const failingLogin: ReturnType<typeof vi.fn> = vi.fn(() => throwError(() => new Error('fail')));
    const { fixture } = await setup(undefined, failingLogin);
    fillValidForm(fixture);

    fixture.componentInstance.submit();

    expect(navigate).toHaveBeenCalledWith(['/login']);
  });

  it('shows the server error message when registration fails', async () => {
    const failingRegister: ReturnType<typeof vi.fn> = vi.fn(() => throwError(() => ({ error: { message: 'Username taken' } })));
    const { fixture } = await setup(failingRegister);
    fillValidForm(fixture);

    fixture.componentInstance.submit();

    expect(fixture.componentInstance['errorMessage']()).toBe('Username taken');
    expect(fixture.componentInstance['submitting']()).toBe(false);
  });

  it('falls back to a translated error message when the server sends none', async () => {
    const failingRegister: ReturnType<typeof vi.fn> = vi.fn(() => throwError(() => ({ error: null })));
    const { fixture } = await setup(failingRegister);
    fillValidForm(fixture);

    fixture.componentInstance.submit();

    expect(fixture.componentInstance['errorMessage']()).toBeTruthy();
  });
});
