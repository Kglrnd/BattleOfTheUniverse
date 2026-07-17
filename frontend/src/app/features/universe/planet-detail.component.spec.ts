import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { BehaviorSubject, of, throwError } from 'rxjs';

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
    homeworld: false,
    researchEfficiency: 100,
    imageVariant: 0,
    destroyed: false
  };
}

function outgoing(originPlanetId: number): FleetMovementView {
  return {
    id: 1,
    originPlanetId,
    ships: [],
    cargo: [],
    missionType: 'TRANSPORT',
    targetGalaxy: 1,
    targetSystem: 1,
    targetPosition: 2,
    departedAt: new Date(Date.now() - 5000).toISOString(),
    arrivesAt: new Date(Date.now() + 5000).toISOString()
  };
}

function incoming(targetPlanetId: number): IncomingMovementView {
  return {
    id: 2,
    ships: [],
    cargo: [],
    missionType: 'ATTACK',
    originPlanetId: 99,
    originOwnerUsername: 'enemy',
    targetPlanetId,
    targetPlanetName: 'Home',
    departedAt: new Date(Date.now() - 5000).toISOString(),
    arrivesAt: new Date(Date.now() + 5000).toISOString()
  };
}

describe('PlanetDetailComponent', () => {
  let paramMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let getPlanet: ReturnType<typeof vi.fn>;
  let renamePlanet: ReturnType<typeof vi.fn>;
  let reloadPlanets: ReturnType<typeof vi.fn>;

  async function setup(
    id = 1,
    outgoingMovements: FleetMovementView[] = [],
    incomingMovements: IncomingMovementView[] = [],
    renamePlanetOverride?: ReturnType<typeof vi.fn>
  ) {
    paramMap$ = new BehaviorSubject(convertToParamMap({ id: String(id) }));
    getPlanet = vi.fn((planetId: number) => of(planetView(planetId)));
    renamePlanet = renamePlanetOverride ?? vi.fn(() => of(planetView(id)));
    reloadPlanets = vi.fn();

    const apiStub = {
      getPlanet,
      getResources: vi.fn(() => of([] as ResourceView[])),
      getBuildings: vi.fn(() => of([] as BuildingView[])),
      renamePlanet
    };
    const fleetApiStub = {
      movements: vi.fn(() => of(outgoingMovements)),
      incomingMovements: vi.fn(() => of(incomingMovements))
    };

    await TestBed.configureTestingModule({
      imports: [
        PlanetDetailComponent,
        TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
      ],
      providers: [
        { provide: UniverseApiService, useValue: apiStub as unknown as UniverseApiService },
        { provide: FleetApiService, useValue: fleetApiStub as unknown as FleetApiService },
        {
          provide: ActivatedRoute,
          useValue: { paramMap: paramMap$, snapshot: { paramMap: convertToParamMap({ id: String(id) }) } }
        }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(PlanetDetailComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('reloads planet data when the route id changes without the component being recreated', async () => {
    // Angular's default route reuse strategy keeps the same PlanetDetailComponent instance
    // alive when navigating between /universe/:id routes with different ids (e.g. picking a
    // different planet from the sidebar dropdown) - it only pushes a new value on paramMap,
    // it does not tear down and reconstruct the component. A one-off snapshot read of the id
    // would go stale after the first navigation.
    const fixture = await setup(1);

    expect(getPlanet).toHaveBeenCalledWith(1);
    expect(fixture.nativeElement.querySelector('h1')?.textContent).toBe('Planet 1');

    paramMap$.next(convertToParamMap({ id: '2' }));
    fixture.detectChanges();

    expect(getPlanet).toHaveBeenCalledWith(2);
    expect(fixture.nativeElement.querySelector('h1')?.textContent).toBe('Planet 2');
  });

  it('filters outgoing movements to only those departing from this planet', async () => {
    const fixture = await setup(1, [outgoing(1), outgoing(1), outgoing(2)]);
    const component = fixture.componentInstance as unknown as { outgoingMovements: () => FleetMovementView[] };
    expect(component.outgoingMovements().length).toBe(2);
    expect(component.outgoingMovements().every((m) => m.originPlanetId === 1)).toBe(true);
  });

  it('filters incoming movements to only those targeting this planet', async () => {
    const fixture = await setup(1, [], [incoming(1), incoming(1), incoming(2)]);
    const component = fixture.componentInstance as unknown as { incomingMovements: () => IncomingMovementView[] };
    expect(component.incomingMovements().length).toBe(2);
    expect(component.incomingMovements().every((m) => m.targetPlanetId === 1)).toBe(true);
  });

  it('remainingMovementLabel and movementProgress reflect the movement countdown', async () => {
    const movement = outgoing(1);
    const fixture = await setup(1, [movement]);
    expect(fixture.componentInstance.remainingMovementLabel(movement)).not.toBe('');
    const progress = fixture.componentInstance.movementProgress(movement);
    expect(progress).toBeGreaterThan(0);
    expect(progress).toBeLessThan(100);
  });

  it('startRename opens the rename form and clears any previous error', async () => {
    const fixture = await setup(1);
    const component = fixture.componentInstance as unknown as { renaming: () => boolean; renameError: { set: (v: string | null) => void } };
    component.renameError.set('stale error');

    fixture.componentInstance.startRename();

    expect(component.renaming()).toBe(true);
    expect((fixture.componentInstance as unknown as { renameError: () => string | null }).renameError()).toBeNull();
  });

  it('cancelRename closes the rename form and clears the error', async () => {
    const fixture = await setup(1);
    fixture.componentInstance.startRename();

    fixture.componentInstance.cancelRename();

    const component = fixture.componentInstance as unknown as { renaming: () => boolean; renameError: () => string | null };
    expect(component.renaming()).toBe(false);
    expect(component.renameError()).toBeNull();
  });

  it('submitRename does nothing for a blank name', async () => {
    const fixture = await setup(1);
    const input = document.createElement('input');
    input.value = '   ';

    fixture.componentInstance.submitRename(input, 1);

    expect(renamePlanet).not.toHaveBeenCalled();
  });

  it('submitRename ignores a second submit while already pending', async () => {
    const fixture = await setup(1);
    const component = fixture.componentInstance as unknown as { renamePending: { set: (v: boolean) => void } };
    component.renamePending.set(true);
    const input = document.createElement('input');
    input.value = 'New Name';

    fixture.componentInstance.submitRename(input, 1);

    expect(renamePlanet).not.toHaveBeenCalled();
  });

  it('submitRename trims the input, renames, closes the form, and reloads on success', async () => {
    const fixture = await setup(1);
    const input = document.createElement('input');
    input.value = '  New Name  ';

    fixture.componentInstance.submitRename(input, 1);

    expect(renamePlanet).toHaveBeenCalledWith(1, 'New Name');
    const component = fixture.componentInstance as unknown as {
      renaming: () => boolean;
      renamePending: () => boolean;
    };
    expect(component.renaming()).toBe(false);
    expect(component.renamePending()).toBe(false);
  });

  it('submitRename surfaces the server error message on failure', async () => {
    const failingRename = vi.fn(() => throwError(() => ({ error: { message: 'name taken' } })));
    const fixture = await setup(1, [], [], failingRename);
    const input = document.createElement('input');
    input.value = 'Taken Name';

    fixture.componentInstance.submitRename(input, 1);

    const component = fixture.componentInstance as unknown as { renameError: () => string | null; renamePending: () => boolean };
    expect(component.renameError()).toBe('name taken');
    expect(component.renamePending()).toBe(false);
  });

  it('submitRename falls back to a translated error message when the server sends none', async () => {
    const failingRename = vi.fn(() => throwError(() => ({ error: null })));
    const fixture = await setup(1, [], [], failingRename);
    const input = document.createElement('input');
    input.value = 'Taken Name';

    fixture.componentInstance.submitRename(input, 1);

    const component = fixture.componentInstance as unknown as { renameError: () => string | null };
    expect(component.renameError()).toBeTruthy();
  });
});
