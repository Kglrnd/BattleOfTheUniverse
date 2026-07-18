import { TestBed } from '@angular/core/testing';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { of, throwError } from 'rxjs';

import { ShipyardQueueView, ShipyardView } from '../../core/models';
import { ShipyardComponent } from './shipyard.component';
import { UniverseApiService } from '../universe/universe-api.service';

function ship(overrides: Partial<ShipyardView> = {}): ShipyardView {
  return {
    key: 'light_fighter',
    name: 'Light Fighter',
    description: 'desc',
    owned: 5,
    buildingQuantity: 1,
    unitCost: { metal: 100, crystal: 50, deuterium: 0 },
    unitBuildTimeSeconds: 60,
    buildActive: false,
    buildEndsAt: null,
    unlocked: true,
    missingRequirements: [],
    ...overrides
  } as ShipyardView;
}

function emptyQueue(): ShipyardQueueView {
  return { maxSize: 0, entries: [] };
}

describe('ShipyardComponent', () => {
  let buildShips: ReturnType<typeof vi.fn>;
  let getShips: ReturnType<typeof vi.fn>;
  let getShipyardQueue: ReturnType<typeof vi.fn>;

  async function setup(ships: ShipyardView[], queue: ShipyardQueueView = emptyQueue()) {
    getShips = vi.fn(() => of(ships));
    getShipyardQueue = vi.fn(() => of(queue));
    buildShips = vi.fn(() => of({}));

    await TestBed.configureTestingModule({
      imports: [
        ShipyardComponent,
        TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
      ],
      providers: [{ provide: UniverseApiService, useValue: { getShips, getShipyardQueue, buildShips } }]
    }).compileComponents();

    const fixture = TestBed.createComponent(ShipyardComponent);
    fixture.componentRef.setInput('planetId', 1);
    fixture.detectChanges();
    return fixture;
  }

  it('groups ships into combat/utility/special sections', async () => {
    const fixture = await setup([
      ship({ key: 'light_fighter' }),
      ship({ key: 'small_cargo' }),
      ship({ key: 'orbital_bomb' })
    ]);
    const component = fixture.componentInstance as unknown as {
      shipSections: () => { labelKey: string; ships: ShipyardView[] }[];
    };
    const sections = component.shipSections();
    expect(sections.find((s) => s.labelKey === 'combatShips')?.ships.length).toBe(1);
    expect(sections.find((s) => s.labelKey === 'utilityShips')?.ships.length).toBe(1);
    expect(sections.find((s) => s.labelKey === 'specialShips')?.ships.length).toBe(1);
  });

  it('builds a ship and reloads on success', async () => {
    const fixture = await setup([ship()]);
    fixture.componentInstance.build(ship(), 2);

    expect(buildShips).toHaveBeenCalledWith(1, 'light_fighter', 2);
    const component = fixture.componentInstance as unknown as { queuingShip: () => string | null };
    expect(component.queuingShip()).toBeNull();
  });

  it('ignores a build request with an invalid quantity', async () => {
    const fixture = await setup([ship()]);
    fixture.componentInstance.build(ship(), -1);
    expect(buildShips).not.toHaveBeenCalled();
  });

  it('surfaces an error message when building fails', async () => {
    const failingBuild = vi.fn(() => throwError(() => ({ error: { message: 'shipyard busy' } })));
    await TestBed.configureTestingModule({
      imports: [
        ShipyardComponent,
        TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
      ],
      providers: [
        {
          provide: UniverseApiService,
          useValue: { getShips: vi.fn(() => of([ship()])), getShipyardQueue: vi.fn(() => of(emptyQueue())), buildShips: failingBuild }
        }
      ]
    }).compileComponents();
    const fixture = TestBed.createComponent(ShipyardComponent);
    fixture.componentRef.setInput('planetId', 1);
    fixture.detectChanges();

    fixture.componentInstance.build(ship(), 2);

    const component = fixture.componentInstance as unknown as { errorMessage: () => string | null };
    expect(component.errorMessage()).toBe('shipyard busy');
  });

  it('hasActiveShipyardJob reflects whether any ship is building', async () => {
    const fixture = await setup([ship({ buildActive: true })]);
    expect(fixture.componentInstance.hasActiveShipyardJob()).toBe(true);
  });

  it('isQueueFull falls back to hasActiveShipyardJob below the pipeline unlock level', async () => {
    const fixture = await setup([ship({ buildActive: true })], emptyQueue());
    expect(fixture.componentInstance.isQueueFull()).toBe(true);
  });

  it('isQueueFull is false below the unlock level when nothing is building', async () => {
    const fixture = await setup([ship({ buildActive: false })], emptyQueue());
    expect(fixture.componentInstance.isQueueFull()).toBe(false);
  });

  it('isQueueFull reflects the pipeline capacity once unlocked, independent of buildActive', async () => {
    const fixture = await setup([ship({ buildActive: false })], {
      maxSize: 15,
      entries: Array.from({ length: 15 }, (_, i) => ({
        shipKey: 'light_fighter',
        shipName: 'Light Fighter',
        quantity: 1,
        position: i + 1,
        startedAt: new Date().toISOString(),
        endsAt: new Date().toISOString()
      }))
    });
    expect(fixture.componentInstance.isQueueFull()).toBe(true);
  });

  it('does not render the pipeline section when the queue is hidden (maxSize 0)', async () => {
    const fixture = await setup([ship()], emptyQueue());
    expect(fixture.nativeElement.querySelector('.pipeline-section')).toBeNull();
  });

  it('renders the pipeline section with every queued entry once unlocked', async () => {
    const fixture = await setup([ship()], {
      maxSize: 15,
      entries: [
        { shipKey: 'light_fighter', shipName: 'Light Fighter', quantity: 3, position: 1, startedAt: new Date().toISOString(), endsAt: new Date(Date.now() + 60_000).toISOString() },
        { shipKey: 'small_cargo', shipName: 'Transport Ship', quantity: 1, position: 2, startedAt: new Date().toISOString(), endsAt: new Date(Date.now() + 120_000).toISOString() }
      ]
    });

    const section = fixture.nativeElement.querySelector('.pipeline-section');
    expect(section).not.toBeNull();
    const items = section.querySelectorAll('.pipeline-list li');
    expect(items.length).toBe(2);
  });
});
