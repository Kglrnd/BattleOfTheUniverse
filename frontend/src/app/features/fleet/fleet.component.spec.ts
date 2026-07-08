import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { BehaviorSubject, of } from 'rxjs';

import { FleetMovementView, PlanetView, ShipyardView } from '../../core/models';
import { UniverseApiService } from '../universe/universe-api.service';
import { FleetApiService } from './fleet-api.service';
import { FleetComponent } from './fleet.component';

function planet(id: number, homeworld = false): PlanetView {
  return {
    id,
    name: `Planet ${id}`,
    galaxy: 1,
    system: 1,
    position: id,
    coordinates: `[1:1:${id}]`,
    planetClass: 'TEMPERATE',
    homeworld
  };
}

describe('FleetComponent', () => {
  let queryParamMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;

  beforeEach(async () => {
    queryParamMap$ = new BehaviorSubject(convertToParamMap({ origin: '11' }));

    const universeApiStub = {
      listPlanets: vi.fn(() => of([planet(1, true), planet(11), planet(33)])),
      getShips: vi.fn(() => of([] as ShipyardView[])),
      buildShips: vi.fn()
    };
    const fleetApiStub = {
      movements: vi.fn(() => of([] as FleetMovementView[])),
      dispatch: vi.fn(),
      driveOptions: vi.fn(() => of([]))
    };

    await TestBed.configureTestingModule({
      imports: [FleetComponent],
      providers: [
        { provide: UniverseApiService, useValue: universeApiStub as unknown as UniverseApiService },
        { provide: FleetApiService, useValue: fleetApiStub as unknown as FleetApiService },
        {
          provide: ActivatedRoute,
          useValue: {
            queryParamMap: queryParamMap$,
            snapshot: { queryParamMap: convertToParamMap({ origin: '11' }) }
          }
        }
      ]
    }).compileComponents();
  });

  it('preselects the requested origin planet in the DOM even when it is not the first option', async () => {
    // Regression test: a plain [value] binding on <select> races against the @for loop
    // creating its <option> elements. When the origin signal and the planets list are both
    // set in the same tick, a browser applies select.value before the matching <option>
    // exists and silently falls back to the first option - it only "looked correct" when
    // the requested planet happened to already be first in the list. The fix binds the
    // select via [ngModel] so SelectControlValueAccessor re-syncs after each option
    // registers.
    const fixture = TestBed.createComponent(FleetComponent);
    fixture.detectChanges();
    await fixture.whenStable();

    const select: HTMLSelectElement = fixture.nativeElement.querySelector('#origin-select');
    expect(select.value).toBe('11');
  });
});
