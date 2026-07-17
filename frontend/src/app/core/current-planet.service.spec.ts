import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { AuthService } from './auth.service';
import { CurrentPlanetService } from './current-planet.service';
import { PlanetView } from './models';
import { UniverseApiService } from '../features/universe/universe-api.service';

function planet(id: number, homeworld = false): PlanetView {
  return {
    id,
    name: `Planet ${id}`,
    galaxy: 1,
    system: 1,
    position: id,
    coordinates: `[1:1:${id}]`,
    planetClass: 'TEMPERATE',
    homeworld,
    researchEfficiency: 100,
    imageVariant: 0,
    destroyed: false
  };
}

describe('CurrentPlanetService', () => {
  let listPlanets: ReturnType<typeof vi.fn>;

  function setup(isAuthenticated: boolean) {
    listPlanets = vi.fn(() => of([planet(1), planet(2, true), planet(3)]));
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: { isAuthenticated: () => isAuthenticated } },
        { provide: UniverseApiService, useValue: { listPlanets } }
      ]
    });
    const service = TestBed.inject(CurrentPlanetService);
    TestBed.flushEffects();
    return service;
  }

  it('does not fetch planets when unauthenticated', () => {
    const service = setup(false);
    expect(listPlanets).not.toHaveBeenCalled();
    expect(service.planets()).toEqual([]);
    expect(service.selectedPlanet()).toBeNull();
  });

  it('loads planets and defaults selection to the homeworld once authenticated', () => {
    const service = setup(true);
    expect(service.planets().length).toBe(3);
    expect(service.selectedPlanetId()).toBe(2);
    expect(service.selectedPlanet()?.homeworld).toBe(true);
  });

  it('select() switches the current planet and normalizes string ids from native <select>', () => {
    const service = setup(true);
    service.select('3' as unknown as number);
    TestBed.flushEffects();
    expect(service.selectedPlanetId()).toBe(3);
    expect(service.selectedPlanet()?.id).toBe(3);
  });

  it('reload() re-triggers the planets resource fetch', () => {
    const service = setup(true);
    listPlanets.mockClear();
    service.reload();
    TestBed.flushEffects();
    expect(listPlanets).toHaveBeenCalled();
  });
});
