import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { Observable, of, throwError } from 'rxjs';

import { AuthService } from '../../core/auth.service';
import { HighscoreEntry, HighscoreResponse, PlanetView, UserView } from '../../core/models';
import { UniverseApiService } from '../universe/universe-api.service';
import { HighscoreApiService } from './highscore-api.service';
import { HighscoreComponent } from './highscore.component';

function entry(userId: number, rank: number): HighscoreEntry {
  return { rank, userId, username: `user${userId}`, score: 1000 - rank };
}

function planet(id: number): PlanetView {
  return {
    id,
    name: `Planet ${id}`,
    galaxy: 1,
    system: 1,
    position: id,
    coordinates: `[1:1:${id}]`,
    planetClass: 'TEMPERATE',
    homeworld: false,
    researchEfficiency: 100,
    imageVariant: 0,
    destroyed: false
  };
}

describe('HighscoreComponent', () => {
  let byOwner: ReturnType<typeof vi.fn>;

  async function setup(getResponse: () => Observable<HighscoreResponse>) {
    byOwner = vi.fn(() => of([planet(1)]));
    await TestBed.configureTestingModule({
      imports: [
        HighscoreComponent,
        TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
      ],
      providers: [
        provideRouter([]),
        { provide: HighscoreApiService, useValue: { get: vi.fn(getResponse) } },
        { provide: AuthService, useValue: { currentUser: () => ({ username: 'me' }) as UserView } },
        { provide: UniverseApiService, useValue: { byOwner } }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(HighscoreComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('exposes the loaded highscore data', async () => {
    const response: HighscoreResponse = { top: [entry(1, 1), entry(2, 2)], me: entry(3, 10) };
    const fixture = await setup(() => of(response));

    const component = fixture.componentInstance as unknown as { data: () => HighscoreResponse | null };
    expect(component.data()).toEqual(response);
  });

  it('surfaces an error message when the request fails', async () => {
    const fixture = await setup(() => throwError(() => Object.assign(new Error('boom'), { error: { message: 'boom' } })));

    const component = fixture.componentInstance as unknown as { errorMessage: () => string | null };
    expect(component.errorMessage()).toBe('boom');
  });

  it('selectPlayer toggles the selected user and loads their planets', async () => {
    const response: HighscoreResponse = { top: [entry(1, 1)], me: null };
    const fixture = await setup(() => of(response));

    const component = fixture.componentInstance as unknown as {
      selectPlayer: (e: HighscoreEntry) => void;
      selectedPlanets: () => PlanetView[] | null;
    };

    expect(component.selectedPlanets()).toBeNull();

    component.selectPlayer(entry(1, 1));
    fixture.detectChanges();
    expect(byOwner).toHaveBeenCalledWith(1);
    expect(component.selectedPlanets()).toEqual([planet(1)]);

    component.selectPlayer(entry(1, 1));
    fixture.detectChanges();
    expect(component.selectedPlanets()).toBeNull();
  });
});
