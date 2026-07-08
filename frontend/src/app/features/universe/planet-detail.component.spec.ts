import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { BehaviorSubject, of } from 'rxjs';

import { BuildingView, FleetMovementView, IncomingMovementView, PlanetView, ResourceView } from '../../core/models';
import { FleetApiService } from '../fleet/fleet-api.service';
import { PlanetDetailComponent } from './planet-detail.component';
import { UniverseApiService } from './universe-api.service';

function planetView(id: number): PlanetView {
  return {
    id,
    name: `Planet ${id}`,
    galaxy: 1,
    system: 1,
    position: id,
    coordinates: `[1:1:${id}]`,
    planetClass: 'TEMPERATE',
    homeworld: false
  };
}

describe('PlanetDetailComponent', () => {
  let paramMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let getPlanet: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    paramMap$ = new BehaviorSubject(convertToParamMap({ id: '1' }));
    getPlanet = vi.fn((id: number) => of(planetView(id)));

    const apiStub = {
      getPlanet,
      getResources: vi.fn(() => of([] as ResourceView[])),
      getBuildings: vi.fn(() => of([] as BuildingView[]))
    };
    const fleetApiStub = {
      movements: vi.fn(() => of([] as FleetMovementView[])),
      incomingMovements: vi.fn(() => of([] as IncomingMovementView[]))
    };

    await TestBed.configureTestingModule({
      imports: [PlanetDetailComponent],
      providers: [
        { provide: UniverseApiService, useValue: apiStub as unknown as UniverseApiService },
        { provide: FleetApiService, useValue: fleetApiStub as unknown as FleetApiService },
        {
          provide: ActivatedRoute,
          useValue: { paramMap: paramMap$, snapshot: { paramMap: convertToParamMap({ id: '1' }) } }
        }
      ]
    }).compileComponents();
  });

  it('reloads planet data when the route id changes without the component being recreated', () => {
    // Angular's default route reuse strategy keeps the same PlanetDetailComponent instance
    // alive when navigating between /universe/:id routes with different ids (e.g. picking a
    // different planet from the sidebar dropdown) - it only pushes a new value on paramMap,
    // it does not tear down and reconstruct the component. A one-off snapshot read of the id
    // would go stale after the first navigation.
    const fixture = TestBed.createComponent(PlanetDetailComponent);
    fixture.detectChanges();

    expect(getPlanet).toHaveBeenCalledWith(1);
    expect(fixture.nativeElement.querySelector('h1')?.textContent).toBe('Planet 1');

    paramMap$.next(convertToParamMap({ id: '2' }));
    fixture.detectChanges();

    expect(getPlanet).toHaveBeenCalledWith(2);
    expect(fixture.nativeElement.querySelector('h1')?.textContent).toBe('Planet 2');
  });
});
