import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { of, throwError } from 'rxjs';

import { AuthService } from '../../core/auth.service';
import { PlanetView, UserView } from '../../core/models';
import { UniverseApiService } from '../universe/universe-api.service';
import { LoginComponent } from './login.component';

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

describe('LoginComponent', () => {
  let navigate: ReturnType<typeof vi.fn>;

  async function setup(
    login: ReturnType<typeof vi.fn> = vi.fn(() => of({} as UserView)),
    getHomePlanet: ReturnType<typeof vi.fn> = vi.fn(() => of(planet(7)))
  ) {
    await TestBed.configureTestingModule({
      imports: [
        LoginComponent,
        TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
      ],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { login } },
        { provide: UniverseApiService, useValue: { getHomePlanet } }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(LoginComponent);
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
    return { fixture, login, getHomePlanet };
  }

  it('does not submit when the form is invalid', async () => {
    const { fixture, login } = await setup();
    fixture.componentInstance.submit();
    expect(login).not.toHaveBeenCalled();
  });

  it('logs in and navigates to the homeworld on success', async () => {
    const { fixture, login, getHomePlanet } = await setup();
    fixture.componentInstance['form'].setValue({ username: 'player', password: 'secret' });

    fixture.componentInstance.submit();

    expect(login).toHaveBeenCalledWith('player', 'secret');
    expect(getHomePlanet).toHaveBeenCalled();
    expect(navigate).toHaveBeenCalledWith(['/universe', 7]);
    expect(fixture.componentInstance['submitting']()).toBe(false);
  });

  it('navigates to /universe when fetching the homeworld fails after login', async () => {
    const failingGetHomePlanet: ReturnType<typeof vi.fn> = vi.fn(() => throwError(() => new Error('fail')));
    const { fixture } = await setup(undefined, failingGetHomePlanet);
    fixture.componentInstance['form'].setValue({ username: 'player', password: 'secret' });

    fixture.componentInstance.submit();

    expect(navigate).toHaveBeenCalledWith(['/universe']);
  });

  it('shows an error message when login fails', async () => {
    const failingLogin: ReturnType<typeof vi.fn> = vi.fn(() => throwError(() => new Error('invalid')));
    const { fixture } = await setup(failingLogin);
    fixture.componentInstance['form'].setValue({ username: 'player', password: 'wrong' });

    fixture.componentInstance.submit();

    expect(fixture.componentInstance['errorMessage']()).toBeTruthy();
    expect(fixture.componentInstance['submitting']()).toBe(false);
  });

  it('ignores a second submit while already submitting', async () => {
    const { fixture, login } = await setup();
    fixture.componentInstance['form'].setValue({ username: 'player', password: 'secret' });
    fixture.componentInstance['submitting'].set(true);

    fixture.componentInstance.submit();

    expect(login).not.toHaveBeenCalled();
  });
});
