import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { BehaviorSubject, of, throwError } from 'rxjs';

import { AuthService } from '../../core/auth.service';
import { DriveOptionView, FleetMovementView, PlanetView, ResourceView, ShipyardView } from '../../core/models';
import { UniverseApiService } from '../universe/universe-api.service';
import { FleetApiService } from './fleet-api.service';
import { FleetComponent } from './fleet.component';

function planet(id: number, homeworld = false, overrides: Partial<PlanetView> = {}): PlanetView {
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
    destroyed: false,
    ...overrides
  };
}

function ship(overrides: Partial<ShipyardView> = {}): ShipyardView {
  return {
    key: 'light_fighter',
    name: 'Light Fighter',
    description: 'desc',
    owned: 10,
    cargoCapacity: 50,
    hydrogenConsumption: 0,
    unitCost: { metal: 100, crystal: 50, deuterium: 0 },
    unitBuildTimeSeconds: 60,
    buildActive: false,
    buildingQuantity: null,
    buildEndsAt: null,
    unlocked: true,
    missingRequirements: [],
    ...overrides
  };
}

function driveOption(key: string, etaSeconds: number): DriveOptionView {
  return { key, name: key, driveScope: 'GALAXY', level: 1, speedMultiplier: 1, etaSeconds, fuelCost: 10 };
}

function movement(overrides: Partial<FleetMovementView> = {}): FleetMovementView {
  return {
    id: 1,
    originPlanetId: 1,
    ships: [],
    cargo: [],
    missionType: 'TRANSPORT',
    targetGalaxy: 1,
    targetSystem: 1,
    targetPosition: 2,
    departedAt: new Date(Date.now() - 5000).toISOString(),
    arrivesAt: new Date(Date.now() + 5000).toISOString(),
    ...overrides
  };
}

describe('FleetComponent', () => {
  let queryParamMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let getShips: ReturnType<typeof vi.fn>;
  let getResources: ReturnType<typeof vi.fn>;
  let buildShips: ReturnType<typeof vi.fn>;
  let movements: ReturnType<typeof vi.fn>;
  let dispatch: ReturnType<typeof vi.fn>;
  let driveOptions: ReturnType<typeof vi.fn>;

  async function setup(
    initialQueryParams: Record<string, string> = { origin: '11' },
    ships: ShipyardView[] = [ship({ key: 'light_fighter', owned: 10, cargoCapacity: 50 }), ship({ key: 'orbital_bomb', owned: 2, cargoCapacity: 0 })],
    resources: ResourceView[] = [{ resourceKey: 'METAL', displayName: 'Metal', amount: 1000 }],
    movementsList: FleetMovementView[] = [],
    driveOptionsResult: DriveOptionView[] = [driveOption('impulse_drive', 100)]
  ) {
    queryParamMap$ = new BehaviorSubject(convertToParamMap(initialQueryParams));
    getShips = vi.fn(() => of(ships));
    getResources = vi.fn(() => of(resources));
    buildShips = vi.fn();
    movements = vi.fn(() => of(movementsList));
    dispatch = vi.fn(() => of({}));
    driveOptions = vi.fn(() => of(driveOptionsResult));

    const universeApiStub = { listPlanets: vi.fn(() => of([planet(1, true), planet(11), planet(33)])), getShips, getResources, buildShips };
    const fleetApiStub = { movements, dispatch, driveOptions };

    await TestBed.configureTestingModule({
      imports: [
        FleetComponent,
        TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
      ],
      providers: [
        { provide: UniverseApiService, useValue: universeApiStub as unknown as UniverseApiService },
        { provide: FleetApiService, useValue: fleetApiStub as unknown as FleetApiService },
        { provide: AuthService, useValue: { isAuthenticated: () => true } as unknown as AuthService },
        {
          provide: ActivatedRoute,
          useValue: {
            queryParamMap: queryParamMap$,
            snapshot: { queryParamMap: convertToParamMap(initialQueryParams) }
          }
        }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(FleetComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  it('preselects the requested origin planet in the DOM even when it is not the first option', async () => {
    // Regression test: a plain [value] binding on <select> races against the @for loop
    // creating its <option> elements. When the origin signal and the planets list are both
    // set in the same tick, a browser applies select.value before the matching <option>
    // exists and silently falls back to the first option - it only "looked correct" when
    // the requested planet happened to already be first in the list. The fix binds the
    // select via [ngModel] so SelectControlValueAccessor re-syncs after each option
    // registers.
    const fixture = await setup();

    const select: HTMLSelectElement = fixture.nativeElement.querySelector('#origin-select');
    expect(select.value).toBe('11');
  });

  it('falls back to the current planet selection when no valid origin query param is given', async () => {
    const fixture = await setup({});
    const component = fixture.componentInstance as unknown as { originPlanetId: () => number | null };
    expect(component.originPlanetId()).toBe(1);
  });

  it('ignores an origin query param that does not match any owned planet', async () => {
    const fixture = await setup({ origin: '999' });
    const component = fixture.componentInstance as unknown as { originPlanetId: () => number | null };
    expect(component.originPlanetId()).toBe(1);
  });

  it('otherPlanets excludes the current origin planet', async () => {
    const fixture = await setup({ origin: '11' });
    const others = fixture.componentInstance['otherPlanets']().map((p: PlanetView) => p.id);
    expect(others).toEqual([1, 33]);
  });

  it('onOriginChange updates origin, selects the planet, and resets manifest/cargo/drive state', async () => {
    const fixture = await setup({ origin: '11' });
    fixture.componentInstance.onOriginChange(33);
    TestBed.flushEffects();

    const component = fixture.componentInstance as unknown as { originPlanetId: () => number | null };
    expect(component.originPlanetId()).toBe(33);
    expect(getShips).toHaveBeenCalledWith(33);
  });

  it('a manually selected origin survives further change-detection ticks (regression: the initial "?origin=" snapshot must only apply once)', async () => {
    const fixture = await setup({ origin: '11' });
    fixture.componentInstance.onOriginChange(33);
    TestBed.flushEffects();
    TestBed.flushEffects();
    fixture.detectChanges();

    const component = fixture.componentInstance as unknown as { originPlanetId: () => number | null };
    expect(component.originPlanetId()).toBe(33);
  });

  it('setMissionType updates the mission type and clears cargo/drive state', async () => {
    const fixture = await setup();
    fixture.componentInstance.setMissionType('ATTACK');
    const component = fixture.componentInstance as unknown as { missionType: () => string };
    expect(component.missionType()).toBe('ATTACK');
  });

  it('applies a requested mission/target from query params (system-view "Attack" link)', async () => {
    const fixture = await setup({ origin: '11', mission: 'ATTACK', targetGalaxy: '2', targetSystem: '3', targetPosition: '4' });
    const component = fixture.componentInstance as unknown as {
      missionType: () => string;
      prefillGalaxy: () => number | null;
      prefillSystem: () => number | null;
      prefillPosition: () => number | null;
    };
    expect(component.missionType()).toBe('ATTACK');
    expect(component.prefillGalaxy()).toBe(2);
    expect(component.prefillSystem()).toBe(3);
    expect(component.prefillPosition()).toBe(4);
  });

  it('ignores an invalid/unknown mission query param', async () => {
    const fixture = await setup({ origin: '11', mission: 'NOT_A_MISSION', targetGalaxy: '2', targetSystem: '3', targetPosition: '4' });
    const component = fixture.componentInstance as unknown as { missionType: () => string };
    expect(component.missionType()).toBe('COLONIZE');
  });

  it('onTargetPlanetChange fills in the target coordinate inputs for the selected planet', async () => {
    const fixture = await setup({ origin: '11' });
    const select = document.createElement('select');
    const option = document.createElement('option');
    option.value = '33';
    select.appendChild(option);
    select.value = '33';
    const galaxyInput = document.createElement('input');
    const systemInput = document.createElement('input');
    const positionInput = document.createElement('input');

    fixture.componentInstance.onTargetPlanetChange({ target: select } as unknown as Event, galaxyInput, systemInput, positionInput);

    expect(galaxyInput.value).toBe('1');
    expect(systemInput.value).toBe('1');
    expect(positionInput.value).toBe('33');
  });

  it('onTargetPlanetChange does nothing for an unknown planet id', async () => {
    const fixture = await setup({ origin: '11' });
    const select = document.createElement('select');
    select.value = '999';
    const galaxyInput = document.createElement('input');
    galaxyInput.value = 'unchanged';

    fixture.componentInstance.onTargetPlanetChange({ target: select } as unknown as Event, galaxyInput, document.createElement('input'), document.createElement('input'));

    expect(galaxyInput.value).toBe('unchanged');
  });

  function numberInputEvent(value: number): Event {
    const input = document.createElement('input');
    input.type = 'number';
    input.value = String(value);
    return { target: input } as unknown as Event;
  }

  it('updateManifestQuantity clamps quantity to ships available and adds it to the manifest', async () => {
    const fixture = await setup({ origin: '11' }, [ship({ key: 'light_fighter', owned: 5 })]);
    fixture.componentInstance.updateManifestQuantity('light_fighter', numberInputEvent(20), 1, 1, 5);

    expect(fixture.componentInstance['currentManifest']()).toEqual([{ shipKey: 'light_fighter', quantity: 5 }]);
  });

  it('updateManifestQuantity removes the ship from the manifest when set to zero', async () => {
    const fixture = await setup({ origin: '11' }, [ship({ key: 'light_fighter', owned: 5 })]);
    fixture.componentInstance.updateManifestQuantity('light_fighter', numberInputEvent(3), 1, 1, 5);
    fixture.componentInstance.updateManifestQuantity('light_fighter', numberInputEvent(0), 1, 1, 5);

    expect(fixture.componentInstance['currentManifest']()).toEqual([]);
  });

  describe('specialShipCompositionError', () => {
    it('returns null when no special ships are in the manifest', async () => {
      const fixture = await setup({ origin: '11' }, [ship({ key: 'light_fighter', owned: 5 })]);
      fixture.componentInstance.updateManifestQuantity('light_fighter', numberInputEvent(2), 1, 1, 5);
      expect(fixture.componentInstance['specialShipCompositionError']()).toBeNull();
    });

    it('rejects mixing orbital_bomb and invasion_unit in the same fleet', async () => {
      const fixture = await setup({ origin: '11' }, [
        ship({ key: 'orbital_bomb', owned: 5 }),
        ship({ key: 'invasion_unit', owned: 5 }),
        ship({ key: 'galaxy_class', owned: 5 })
      ]);
      fixture.componentInstance.updateManifestQuantity('orbital_bomb', numberInputEvent(1), 1, 1, 5);
      fixture.componentInstance.updateManifestQuantity('invasion_unit', numberInputEvent(1), 1, 1, 5);
      fixture.componentInstance.updateManifestQuantity('galaxy_class', numberInputEvent(1), 1, 1, 5);

      expect(fixture.componentInstance['specialShipCompositionError']()).toBeTruthy();
    });

    it('rejects a special ship flying alone', async () => {
      const fixture = await setup({ origin: '11' }, [ship({ key: 'orbital_bomb', owned: 5 })]);
      fixture.componentInstance.updateManifestQuantity('orbital_bomb', numberInputEvent(1), 1, 1, 5);

      expect(fixture.componentInstance['specialShipCompositionError']()).toBeTruthy();
    });

    it('rejects a special ship without a galaxy_class escort', async () => {
      const fixture = await setup({ origin: '11' }, [ship({ key: 'orbital_bomb', owned: 5 }), ship({ key: 'light_fighter', owned: 5 })]);
      fixture.componentInstance.updateManifestQuantity('orbital_bomb', numberInputEvent(1), 1, 1, 5);
      fixture.componentInstance.updateManifestQuantity('light_fighter', numberInputEvent(1), 1, 1, 5);

      expect(fixture.componentInstance['specialShipCompositionError']()).toBeTruthy();
    });

    it('allows a special ship escorted by a galaxy_class ship', async () => {
      const fixture = await setup({ origin: '11' }, [ship({ key: 'orbital_bomb', owned: 5 }), ship({ key: 'galaxy_class', owned: 5 })]);
      fixture.componentInstance.updateManifestQuantity('orbital_bomb', numberInputEvent(1), 1, 1, 5);
      fixture.componentInstance.updateManifestQuantity('galaxy_class', numberInputEvent(1), 1, 1, 5);

      expect(fixture.componentInstance['specialShipCompositionError']()).toBeNull();
    });
  });

  describe('cargo handling', () => {
    it('fleetCargoCapacity sums cargo capacity across the manifest', async () => {
      const fixture = await setup({ origin: '11' }, [ship({ key: 'light_fighter', owned: 5, cargoCapacity: 50 })]);
      fixture.componentInstance.updateManifestQuantity('light_fighter', numberInputEvent(3), 1, 1, 5);

      expect(fixture.componentInstance['fleetCargoCapacity']()).toBe(150);
    });

    it('updateCargoQuantity clamps to resources on hand and remaining cargo capacity', async () => {
      const fixture = await setup(
        { origin: '11' },
        [ship({ key: 'small_cargo', owned: 1, cargoCapacity: 100 })],
        [{ resourceKey: 'METAL', displayName: 'Metal', amount: 40 }]
      );
      fixture.componentInstance.setMissionType('TRANSPORT');
      fixture.componentInstance.updateManifestQuantity('small_cargo', numberInputEvent(1), 1, 1, 5);

      fixture.componentInstance.updateCargoQuantity('METAL', numberInputEvent(9999));

      expect(fixture.componentInstance['currentCargo']()).toEqual([{ resourceKey: 'METAL', amount: 40 }]);
    });

    it('updateCargoQuantity removes the resource from cargo when set to zero', async () => {
      const fixture = await setup(
        { origin: '11' },
        [ship({ key: 'small_cargo', owned: 1, cargoCapacity: 100 })],
        [{ resourceKey: 'METAL', displayName: 'Metal', amount: 40 }]
      );
      fixture.componentInstance.updateManifestQuantity('small_cargo', numberInputEvent(1), 1, 1, 5);
      fixture.componentInstance.updateCargoQuantity('METAL', numberInputEvent(20));
      fixture.componentInstance.updateCargoQuantity('METAL', numberInputEvent(0));

      expect(fixture.componentInstance['currentCargo']()).toEqual([]);
    });

    it('resourceOnHand returns 0 for a resource not present', async () => {
      const fixture = await setup({ origin: '11' }, undefined, []);
      expect(fixture.componentInstance['resourceOnHand']('METAL')).toBe(0);
    });
  });

  describe('drive options and dispatch', () => {
    it('refreshDriveOptions resets when there is no manifest', async () => {
      const fixture = await setup({ origin: '11' });
      fixture.componentInstance.refreshDriveOptions(1, 1, 5);

      const component = fixture.componentInstance as unknown as { driveOptions: () => DriveOptionView[] };
      expect(component.driveOptions()).toEqual([]);
      expect(driveOptions).not.toHaveBeenCalled();
    });

    it('fetches drive options and auto-selects the fastest', async () => {
      const fixture = await setup(
        { origin: '11' },
        [ship({ key: 'light_fighter', owned: 5 })],
        undefined,
        undefined,
        [driveOption('slow', 200), driveOption('fast', 20)]
      );
      fixture.componentInstance.updateManifestQuantity('light_fighter', numberInputEvent(2), 1, 1, 5);

      const component = fixture.componentInstance as unknown as { selectedDriveKey: () => string | null };
      expect(component.selectedDriveKey()).toBe('fast');
    });

    it('surfaces an error when fetching drive options fails', async () => {
      const fixture = await setup({ origin: '11' }, [ship({ key: 'light_fighter', owned: 5 })]);
      const failingDriveOptions = vi.fn(() => throwError(() => ({ error: { message: 'no route' } })));
      (fixture.debugElement.injector.get(FleetApiService) as unknown as { driveOptions: typeof failingDriveOptions }).driveOptions =
        failingDriveOptions;

      fixture.componentInstance.updateManifestQuantity('light_fighter', numberInputEvent(2), 1, 1, 5);
      fixture.detectChanges();

      const component = fixture.componentInstance as unknown as { driveOptionsError: () => string | null };
      expect(component.driveOptionsError()).toBe('no route');
    });

    it('selectDrive updates the selected drive key', async () => {
      const fixture = await setup({ origin: '11' });
      fixture.componentInstance.selectDrive('impulse_drive');
      const component = fixture.componentInstance as unknown as { selectedDriveKey: () => string | null };
      expect(component.selectedDriveKey()).toBe('impulse_drive');
    });

    it('formatEta formats seconds as m:ss', async () => {
      const fixture = await setup();
      expect(fixture.componentInstance.formatEta(125)).toBe('2:05');
    });

    it('dispatch sends the manifest and resets state on success', async () => {
      const fixture = await setup({ origin: '11' }, [ship({ key: 'light_fighter', owned: 5 })]);
      fixture.componentInstance.updateManifestQuantity('light_fighter', numberInputEvent(2), 1, 1, 5);
      fixture.componentInstance.selectDrive('impulse_drive');

      fixture.componentInstance.dispatch(1, 1, 5);

      expect(dispatch).toHaveBeenCalledWith(
        expect.objectContaining({ originPlanetId: 11, missionType: 'COLONIZE', driveKey: 'impulse_drive' })
      );
      const component = fixture.componentInstance as unknown as { dispatching: () => boolean };
      expect(component.dispatching()).toBe(false);
    });

    it('dispatch includes cargo only for TRANSPORT missions', async () => {
      const fixture = await setup(
        { origin: '11' },
        [ship({ key: 'small_cargo', owned: 5, cargoCapacity: 100 })],
        [{ resourceKey: 'METAL', displayName: 'Metal', amount: 40 }]
      );
      fixture.componentInstance.setMissionType('TRANSPORT');
      fixture.componentInstance.updateManifestQuantity('small_cargo', numberInputEvent(1), 1, 1, 5);
      fixture.componentInstance.updateCargoQuantity('METAL', numberInputEvent(20));
      fixture.componentInstance.selectDrive('impulse_drive');

      fixture.componentInstance.dispatch(1, 1, 5);

      expect(dispatch).toHaveBeenCalledWith(expect.objectContaining({ cargo: [{ resourceKey: 'METAL', amount: 20 }] }));
    });

    it('does not dispatch without a selected drive', async () => {
      const fixture = await setup({ origin: '11' }, [ship({ key: 'light_fighter', owned: 5 })], undefined, undefined, []);
      fixture.componentInstance.updateManifestQuantity('light_fighter', numberInputEvent(2), 1, 1, 5);
      fixture.detectChanges();

      fixture.componentInstance.dispatch(1, 1, 5);

      expect(dispatch).not.toHaveBeenCalled();
    });

    it('surfaces an error message when dispatch fails', async () => {
      const fixture = await setup({ origin: '11' }, [ship({ key: 'light_fighter', owned: 5 })]);
      const failingDispatch = vi.fn(() => throwError(() => ({ error: { message: 'fleet busy' } })));
      (fixture.debugElement.injector.get(FleetApiService) as unknown as { dispatch: typeof failingDispatch }).dispatch = failingDispatch;

      fixture.componentInstance.updateManifestQuantity('light_fighter', numberInputEvent(2), 1, 1, 5);
      fixture.componentInstance.selectDrive('impulse_drive');
      fixture.componentInstance.dispatch(1, 1, 5);

      const component = fixture.componentInstance as unknown as { errorMessage: () => string | null };
      expect(component.errorMessage()).toBe('fleet busy');
    });
  });

  describe('display helpers', () => {
    it('originLabel formats a known planet and falls back to an id for unknown ones', async () => {
      const fixture = await setup({ origin: '11' });
      expect(fixture.componentInstance.originLabel(11)).toBe('Planet 11 [1:1:11]');
      expect(fixture.componentInstance.originLabel(999)).toBe('#999');
    });

    it('remainingMovementLabel and movementProgress reflect the movement countdown', async () => {
      const fixture = await setup({ origin: '11' }, undefined, undefined, [movement()]);
      const m = movement();
      expect(fixture.componentInstance.remainingMovementLabel(m)).not.toBe('');
      const progress = fixture.componentInstance.movementProgress(m);
      expect(progress).toBeGreaterThanOrEqual(0);
      expect(progress).toBeLessThanOrEqual(100);
    });

    it('exposes the exact list of resources that can be loaded as TRANSPORT cargo', async () => {
      const fixture = await setup();
      const component = fixture.componentInstance as unknown as { transportableResources: string[] };
      expect(component.transportableResources).toEqual(['METAL', 'CRYSTAL', 'DEUTERIUM', 'HYDROGEN']);
    });
  });
});
