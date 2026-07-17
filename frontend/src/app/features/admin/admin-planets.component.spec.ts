import { TestBed } from '@angular/core/testing';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { of } from 'rxjs';

import { AdminPlanetView } from '../../core/models';
import { AdminPlanetsApiService } from './admin-planets-api.service';
import { AdminPlanetsComponent } from './admin-planets.component';

function planet(overrides: Partial<AdminPlanetView>): AdminPlanetView {
  return {
    id: 1,
    name: 'Homeworld',
    ownerId: 1,
    ownerUsername: 'alice',
    galaxy: 1,
    system: 1,
    position: 1,
    coordinates: '[1:1:1]',
    planetClass: 'TEMPERATE',
    homeworld: true,
    destroyed: false,
    createdAt: '2026-01-01T00:00:00Z',
    ...overrides
  };
}

describe('AdminPlanetsComponent', () => {
  async function setup(planets: AdminPlanetView[]) {
    await TestBed.configureTestingModule({
      imports: [
        AdminPlanetsComponent,
        TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
      ],
      providers: [{ provide: AdminPlanetsApiService, useValue: { list: vi.fn(() => of(planets)) } }]
    }).compileComponents();

    const fixture = TestBed.createComponent(AdminPlanetsComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('filters by owner username (case-insensitive, partial match)', async () => {
    const fixture = await setup([planet({ id: 1, ownerUsername: 'Alice' }), planet({ id: 2, ownerUsername: 'bob' })]);
    const component = fixture.componentInstance as unknown as { ownerFilter: { set: (v: string) => void }; filteredPlanets: () => AdminPlanetView[] };

    component.ownerFilter.set('ali');
    expect(component.filteredPlanets().map((p) => p.id)).toEqual([1]);
  });

  it('filters by planet name', async () => {
    const fixture = await setup([planet({ id: 1, name: 'Homeworld' }), planet({ id: 2, name: 'Colony' })]);
    const component = fixture.componentInstance as unknown as { nameFilter: { set: (v: string) => void }; filteredPlanets: () => AdminPlanetView[] };

    component.nameFilter.set('colo');
    expect(component.filteredPlanets().map((p) => p.id)).toEqual([2]);
  });

  it('filters by galaxy and system via the change handlers', async () => {
    const fixture = await setup([
      planet({ id: 1, galaxy: 1, system: 1 }),
      planet({ id: 2, galaxy: 2, system: 1 }),
      planet({ id: 3, galaxy: 1, system: 5 })
    ]);
    const component = fixture.componentInstance;

    component.onGalaxyFilterChange('1');
    let filtered = (component as unknown as { filteredPlanets: () => AdminPlanetView[] }).filteredPlanets();
    expect(filtered.map((p) => p.id)).toEqual([1, 3]);

    component.onSystemFilterChange('5');
    filtered = (component as unknown as { filteredPlanets: () => AdminPlanetView[] }).filteredPlanets();
    expect(filtered.map((p) => p.id)).toEqual([3]);

    component.onGalaxyFilterChange('');
    component.onSystemFilterChange('');
    filtered = (component as unknown as { filteredPlanets: () => AdminPlanetView[] }).filteredPlanets();
    expect(filtered.length).toBe(3);
  });

  it('filters to homeworlds only when toggled', async () => {
    const fixture = await setup([planet({ id: 1, homeworld: true }), planet({ id: 2, homeworld: false })]);
    const component = fixture.componentInstance as unknown as { homeworldOnly: { set: (v: boolean) => void }; filteredPlanets: () => AdminPlanetView[] };

    component.homeworldOnly.set(true);
    expect(component.filteredPlanets().map((p) => p.id)).toEqual([1]);
  });

  it('clearFilters resets every filter', async () => {
    const fixture = await setup([planet({ id: 1 }), planet({ id: 2, ownerUsername: 'bob' })]);
    const component = fixture.componentInstance as unknown as {
      ownerFilter: { set: (v: string) => void };
      homeworldOnly: { set: (v: boolean) => void };
      filteredPlanets: () => AdminPlanetView[];
    };

    component.ownerFilter.set('nomatch');
    expect(component.filteredPlanets().length).toBe(0);

    fixture.componentInstance.clearFilters();
    expect(component.filteredPlanets().length).toBe(2);
  });

  it('formatCreatedAt formats the ISO timestamp for display', async () => {
    const fixture = await setup([]);
    expect(fixture.componentInstance.formatCreatedAt('2026-01-01T00:00:00Z')).toBe(new Date('2026-01-01T00:00:00Z').toLocaleString());
  });
});
