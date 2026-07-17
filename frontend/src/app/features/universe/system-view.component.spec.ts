import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { BehaviorSubject, of, throwError } from 'rxjs';

import { CurrentPlanetService } from '../../core/current-planet.service';
import { PlanetView, SystemSlotView, SystemView } from '../../core/models';
import { SystemViewComponent } from './system-view.component';
import { UniverseApiService } from './universe-api.service';

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

function slot(position: number, status: SystemSlotView['status'], p: PlanetView | null = null): SystemSlotView {
  return { position, status, planet: p };
}

function systemView(galaxy: number, system: number, slots: SystemSlotView[]): SystemView {
  return { galaxy, system, slots };
}

describe('SystemViewComponent', () => {
  let paramMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let navigate: ReturnType<typeof vi.fn>;
  let getSystem: ReturnType<typeof vi.fn>;
  let getHomePlanet: ReturnType<typeof vi.fn>;

  async function setup(
    params: Record<string, string>,
    ownedPlanets: PlanetView[] = [],
    getSystemOverride: ReturnType<typeof vi.fn> = vi.fn(() => of(systemView(1, 1, [])))
  ) {
    paramMap$ = new BehaviorSubject(convertToParamMap(params));
    navigate = vi.fn();
    getSystem = getSystemOverride;
    getHomePlanet = vi.fn(() => of(planet(1)));

    await TestBed.configureTestingModule({
      imports: [
        SystemViewComponent,
        TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
      ],
      providers: [
        { provide: UniverseApiService, useValue: { getSystem, getHomePlanet } },
        { provide: CurrentPlanetService, useValue: { planets: () => ownedPlanets } },
        { provide: Router, useValue: { navigate } },
        { provide: ActivatedRoute, useValue: { paramMap: paramMap$ } }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(SystemViewComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('loads the requested system from route params', async () => {
    await setup({ galaxy: '2', system: '5' }, [], vi.fn(() => of(systemView(2, 5, []))));
    expect(getSystem).toHaveBeenCalledWith(2, 5);
  });

  it('falls back to the home system when no route params are given', async () => {
    const fixture = await setup({});
    void fixture;
    expect(getHomePlanet).toHaveBeenCalled();
    expect(navigate).toHaveBeenCalledWith(['/universe/system', 1, 1]);
  });

  it('surfaces an error when determining the home system fails', async () => {
    getHomePlanet = vi.fn(() => throwError(() => ({ error: { message: 'no planets' } })));
    await TestBed.configureTestingModule({
      imports: [
        SystemViewComponent,
        TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
      ],
      providers: [
        { provide: UniverseApiService, useValue: { getSystem: vi.fn(() => of(systemView(1, 1, []))), getHomePlanet } },
        { provide: CurrentPlanetService, useValue: { planets: () => [] } },
        { provide: Router, useValue: { navigate: vi.fn() } },
        { provide: ActivatedRoute, useValue: { paramMap: new BehaviorSubject(convertToParamMap({})) } }
      ]
    }).compileComponents();
    const fixture = TestBed.createComponent(SystemViewComponent);
    fixture.detectChanges();

    const component = fixture.componentInstance as unknown as { errorMessage: () => string | null; loading: () => boolean };
    expect(component.errorMessage()).toBeTruthy();
    expect(component.loading()).toBe(false);
  });

  it('surfaces an error when the system is not found', async () => {
    const fixture = await setup(
      { galaxy: '2', system: '5' },
      [],
      vi.fn(() => throwError(() => ({ error: { message: 'not found' } })))
    );

    const component = fixture.componentInstance as unknown as { errorMessage: () => string | null };
    expect(component.errorMessage()).toBe('not found');
  });

  it('reloads when route params change', async () => {
    const fixture = await setup({ galaxy: '1', system: '1' });
    void fixture;
    getSystem.mockClear();

    paramMap$.next(convertToParamMap({ galaxy: '3', system: '10' }));

    expect(getSystem).toHaveBeenCalledWith(3, 10);
  });

  it('goTo navigates to the requested system', async () => {
    const fixture = await setup({ galaxy: '1', system: '1' });
    fixture.componentInstance.goTo(4, 7);
    expect(navigate).toHaveBeenCalledWith(['/universe/system', 4, 7]);
  });

  it('jump reads galaxy/system from input elements and navigates', async () => {
    const fixture = await setup({ galaxy: '1', system: '1' });
    navigate.mockClear();
    const galaxyInput = document.createElement('input');
    galaxyInput.type = 'number';
    galaxyInput.valueAsNumber = 3;
    const systemInput = document.createElement('input');
    systemInput.type = 'number';
    systemInput.valueAsNumber = 9;

    fixture.componentInstance.jump(galaxyInput, systemInput);

    expect(navigate).toHaveBeenCalledWith(['/universe/system', 3, 9]);
  });

  it('jump does nothing with invalid input values', async () => {
    const fixture = await setup({ galaxy: '1', system: '1' });
    navigate.mockClear();
    const galaxyInput = document.createElement('input');
    const systemInput = document.createElement('input');

    fixture.componentInstance.jump(galaxyInput, systemInput);

    expect(navigate).not.toHaveBeenCalled();
  });

  it('previousSystem/nextSystem wrap around system and galaxy boundaries', async () => {
    const fixture = await setup({ galaxy: '1', system: '1' }, [], vi.fn(() => of(systemView(1, 1, []))));
    navigate.mockClear();

    fixture.componentInstance.previousSystem();
    expect(navigate).toHaveBeenCalledWith(['/universe/system', 5, 100]);

    navigate.mockClear();
    getSystem = vi.fn(() => of(systemView(1, 100, [])));
    (TestBed.inject(UniverseApiService) as unknown as { getSystem: typeof getSystem }).getSystem = getSystem;
    paramMap$.next(convertToParamMap({ galaxy: '1', system: '100' }));

    fixture.componentInstance.nextSystem();
    expect(navigate).toHaveBeenCalledWith(['/universe/system', 2, 1]);
  });

  it('previousSystem/nextSystem do nothing when no system is loaded', async () => {
    getSystem = vi.fn(() => of(null as unknown as SystemView));
    await TestBed.configureTestingModule({
      imports: [
        SystemViewComponent,
        TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
      ],
      providers: [
        { provide: UniverseApiService, useValue: { getSystem, getHomePlanet: vi.fn(() => of(planet(1))) } },
        { provide: CurrentPlanetService, useValue: { planets: () => [] } },
        { provide: Router, useValue: { navigate: vi.fn() } },
        { provide: ActivatedRoute, useValue: { paramMap: new BehaviorSubject(convertToParamMap({ galaxy: '1', system: '1' })) } }
      ]
    }).compileComponents();
    const fixture = TestBed.createComponent(SystemViewComponent);
    fixture.detectChanges();

    const navigateSpy = TestBed.inject(Router).navigate as unknown as ReturnType<typeof vi.fn>;
    fixture.componentInstance.previousSystem();
    fixture.componentInstance.nextSystem();
    fixture.componentInstance.previousGalaxy();
    fixture.componentInstance.nextGalaxy();

    expect(navigateSpy).not.toHaveBeenCalled();
  });

  it('previousGalaxy/nextGalaxy wrap around galaxy boundaries', async () => {
    const fixture = await setup({ galaxy: '1', system: '10' }, [], vi.fn(() => of(systemView(1, 10, []))));
    navigate.mockClear();

    fixture.componentInstance.previousGalaxy();
    expect(navigate).toHaveBeenCalledWith(['/universe/system', 5, 10]);

    getSystem = vi.fn(() => of(systemView(5, 10, [])));
    (TestBed.inject(UniverseApiService) as unknown as { getSystem: typeof getSystem }).getSystem = getSystem;
    paramMap$.next(convertToParamMap({ galaxy: '5', system: '10' }));
    navigate.mockClear();

    fixture.componentInstance.nextGalaxy();
    expect(navigate).toHaveBeenCalledWith(['/universe/system', 1, 10]);
  });

  it('slotClass derives a CSS class from the slot status', async () => {
    const fixture = await setup({ galaxy: '1', system: '1' });
    expect(fixture.componentInstance.slotClass('FREE')).toBe('slot-free');
    expect(fixture.componentInstance.slotClass('OCCUPIED')).toBe('slot-occupied');
  });

  it('isMine checks ownership against CurrentPlanetService', async () => {
    const fixture = await setup({ galaxy: '1', system: '1' }, [planet(1)]);
    expect(fixture.componentInstance.isMine(planet(1))).toBe(true);
    expect(fixture.componentInstance.isMine(planet(2))).toBe(false);
  });

  it('actionable is true for free slots and enemy-occupied slots, false for own planets', async () => {
    const fixture = await setup({ galaxy: '1', system: '1' }, [planet(1)]);
    expect(fixture.componentInstance.actionable(slot(1, 'FREE'))).toBe(true);
    expect(fixture.componentInstance.actionable(slot(2, 'OCCUPIED', planet(2)))).toBe(true);
    expect(fixture.componentInstance.actionable(slot(3, 'OCCUPIED', planet(1)))).toBe(false);
    expect(fixture.componentInstance.actionable(slot(4, 'VOID'))).toBe(false);
    expect(fixture.componentInstance.actionable(slot(5, 'DESTROYED'))).toBe(false);
  });

  it('attack and colonize navigate to /fleet with the right mission and target', async () => {
    const fixture = await setup({ galaxy: '1', system: '1' });
    navigate.mockClear();

    fixture.componentInstance.attack(2, 3, 4);
    expect(navigate).toHaveBeenCalledWith(['/fleet'], {
      queryParams: { mission: 'ATTACK', targetGalaxy: 2, targetSystem: 3, targetPosition: 4 }
    });

    navigate.mockClear();
    fixture.componentInstance.colonize(2, 3, 4);
    expect(navigate).toHaveBeenCalledWith(['/fleet'], {
      queryParams: { mission: 'COLONIZE', targetGalaxy: 2, targetSystem: 3, targetPosition: 4 }
    });
  });

  it('onSlotMenuAction dispatches to the right handler for each action', async () => {
    const fixture = await setup({ galaxy: '1', system: '1' });
    navigate.mockClear();

    fixture.componentInstance.onSlotMenuAction('colonize', 1, 2, 3);
    expect(navigate).toHaveBeenCalledWith(['/fleet'], { queryParams: { mission: 'COLONIZE', targetGalaxy: 1, targetSystem: 2, targetPosition: 3 } });

    navigate.mockClear();
    fixture.componentInstance.onSlotMenuAction('attack', 1, 2, 3);
    expect(navigate).toHaveBeenCalledWith(['/fleet'], { queryParams: { mission: 'ATTACK', targetGalaxy: 1, targetSystem: 2, targetPosition: 3 } });

    navigate.mockClear();
    fixture.componentInstance.onSlotMenuAction('bombard', 1, 2, 3);
    expect(navigate).toHaveBeenCalledWith(['/fleet'], { queryParams: { mission: 'BOMBARD', targetGalaxy: 1, targetSystem: 2, targetPosition: 3 } });

    navigate.mockClear();
    fixture.componentInstance.onSlotMenuAction('invade', 1, 2, 3);
    expect(navigate).toHaveBeenCalledWith(['/fleet'], { queryParams: { mission: 'INVADE', targetGalaxy: 1, targetSystem: 2, targetPosition: 3 } });

    navigate.mockClear();
    fixture.componentInstance.onSlotMenuAction('unknown', 1, 2, 3);
    expect(navigate).not.toHaveBeenCalled();
  });
});
