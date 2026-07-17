import { TestBed } from '@angular/core/testing';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { of, throwError } from 'rxjs';

import { TowerView } from '../../core/models';
import { DefenseApiService } from './defense-api.service';
import { DefenseComponent } from './defense.component';

function tower(overrides: Partial<TowerView> = {}): TowerView {
  return {
    key: 'light_defense_tower',
    name: 'Light Defense Tower',
    description: 'desc',
    owned: 2,
    buildingQuantity: 1,
    unitCost: { metal: 100, crystal: 50, deuterium: 0 },
    unitBuildTimeSeconds: 60,
    buildActive: false,
    buildEndsAt: null,
    ...overrides
  } as TowerView;
}

describe('DefenseComponent', () => {
  let build: ReturnType<typeof vi.fn>;
  let list: ReturnType<typeof vi.fn>;

  async function setup(towers: TowerView[]) {
    list = vi.fn(() => of(towers));
    build = vi.fn(() => of({}));

    await TestBed.configureTestingModule({
      imports: [
        DefenseComponent,
        TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
      ],
      providers: [{ provide: DefenseApiService, useValue: { list, build } }]
    }).compileComponents();

    const fixture = TestBed.createComponent(DefenseComponent);
    fixture.componentRef.setInput('planetId', 1);
    fixture.detectChanges();
    return fixture;
  }

  it('builds a tower and reloads the list on success', async () => {
    const fixture = await setup([tower()]);
    fixture.componentInstance.build(tower(), 3);

    expect(build).toHaveBeenCalledWith(1, 'light_defense_tower', 3);
    const component = fixture.componentInstance as unknown as { queuingTower: () => string | null };
    expect(component.queuingTower()).toBeNull();
  });

  it('ignores a build request with an invalid quantity', async () => {
    const fixture = await setup([tower()]);
    fixture.componentInstance.build(tower(), 0);
    expect(build).not.toHaveBeenCalled();
  });

  it('accepts a build request for exactly quantity 1 (boundary)', async () => {
    const fixture = await setup([tower()]);
    fixture.componentInstance.build(tower(), 1);
    expect(build).toHaveBeenCalledWith(1, 'light_defense_tower', 1);
  });

  it('ignores a build request while already queuing', async () => {
    const fixture = await setup([tower()]);
    const component = fixture.componentInstance as unknown as { queuingTower: { set: (v: string | null) => void } };
    component.queuingTower.set('some_key');

    fixture.componentInstance.build(tower(), 3);

    expect(build).not.toHaveBeenCalled();
  });

  it('surfaces an error message when building fails', async () => {
    build = vi.fn(() => throwError(() => ({ error: { message: 'not enough resources' } })));
    await TestBed.configureTestingModule({
      imports: [
        DefenseComponent,
        TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
      ],
      providers: [{ provide: DefenseApiService, useValue: { list: vi.fn(() => of([tower()])), build } }]
    }).compileComponents();
    const fixture = TestBed.createComponent(DefenseComponent);
    fixture.componentRef.setInput('planetId', 1);
    fixture.detectChanges();

    fixture.componentInstance.build(tower(), 3);

    const component = fixture.componentInstance as unknown as { errorMessage: () => string | null };
    expect(component.errorMessage()).toBe('not enough resources');
  });

  it('hasActiveDefenseJob reflects whether any tower is building', async () => {
    const fixture = await setup([tower({ buildActive: true })]);
    expect(fixture.componentInstance.hasActiveDefenseJob()).toBe(true);
  });

  it('hasActiveDefenseJob is false when nothing is building', async () => {
    const fixture = await setup([tower({ buildActive: false })]);
    expect(fixture.componentInstance.hasActiveDefenseJob()).toBe(false);
  });

  it('hasActiveDefenseJob is true when only some towers are building (distinguishes .some from .every)', async () => {
    const fixture = await setup([tower({ buildActive: false }), tower({ key: 'heavy_defense_tower', buildActive: true })]);
    expect(fixture.componentInstance.hasActiveDefenseJob()).toBe(true);
  });

  it('remainingLabel formats the countdown for a building tower', async () => {
    const endsAt = new Date(Date.now() + 65_000).toISOString();
    const fixture = await setup([tower({ buildEndsAt: endsAt })]);
    expect(fixture.componentInstance.remainingLabel(tower({ buildEndsAt: endsAt }))).toBe('1:05');
  });

  it('progress computes the exact percentage complete from unitBuildTimeSeconds * buildingQuantity', async () => {
    const fixture = await setup([tower()]);
    // total duration = 60s * 2 quantity = 120s; endsAt is 30s away -> 90s elapsed -> 75%
    const endsAt = new Date(Date.now() + 30_000).toISOString();
    const result = fixture.componentInstance.progress(tower({ buildEndsAt: endsAt, unitBuildTimeSeconds: 60, buildingQuantity: 2 }));
    expect(result).toBeCloseTo(75, 0);
  });

  it('progress defaults buildingQuantity to 1 when null', async () => {
    const fixture = await setup([tower()]);
    // total duration = 60s * 1 (default) = 60s; endsAt is 30s away -> 30s elapsed -> 50%
    const endsAt = new Date(Date.now() + 30_000).toISOString();
    const result = fixture.componentInstance.progress(tower({ buildEndsAt: endsAt, unitBuildTimeSeconds: 60, buildingQuantity: null }));
    expect(result).toBeCloseTo(50, 0);
  });

  it('towerName and towerDescription look up translations specifically under the "defenses" catalog category', async () => {
    await TestBed.configureTestingModule({
      imports: [
        DefenseComponent,
        TranslocoTestingModule.forRoot({
          langs: {
            en: { catalogData: { defenses: { light_defense_tower: { name: 'Translated Name', description: 'Translated Desc' } } } }
          },
          translocoConfig: { availableLangs: ['en'], defaultLang: 'en' }
        })
      ],
      providers: [{ provide: DefenseApiService, useValue: { list: vi.fn(() => of([tower()])), build: vi.fn() } }]
    }).compileComponents();
    const fixture = TestBed.createComponent(DefenseComponent);
    fixture.componentRef.setInput('planetId', 1);
    fixture.detectChanges();

    const component = fixture.componentInstance as unknown as {
      towerName: (t: TowerView) => string;
      towerDescription: (t: TowerView) => string;
    };
    expect(component.towerName(tower())).toBe('Translated Name');
    expect(component.towerDescription(tower())).toBe('Translated Desc');
  });
});
