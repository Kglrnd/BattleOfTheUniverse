import { TestBed } from '@angular/core/testing';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { of, throwError } from 'rxjs';

import { BuildingView } from '../../core/models';
import { BuildingListComponent } from './building-list.component';
import { UniverseApiService } from './universe-api.service';

function building(overrides: Partial<BuildingView> = {}): BuildingView {
  return {
    key: 'metal_mine',
    name: 'Metal Mine',
    description: 'desc',
    level: 3,
    nextLevelCost: { metal: 100, crystal: 50, deuterium: 0 },
    nextLevelBuildTimeSeconds: 120,
    constructionActive: false,
    constructionEndsAt: null,
    ...overrides
  } as BuildingView;
}

describe('BuildingListComponent', () => {
  let upgrade: ReturnType<typeof vi.fn>;
  let getBuildings: ReturnType<typeof vi.fn>;

  async function setup(buildings: BuildingView[]) {
    getBuildings = vi.fn(() => of(buildings));
    upgrade = vi.fn(() => of({}));

    await TestBed.configureTestingModule({
      imports: [
        BuildingListComponent,
        TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
      ],
      providers: [{ provide: UniverseApiService, useValue: { getBuildings, upgrade } }]
    }).compileComponents();

    const fixture = TestBed.createComponent(BuildingListComponent);
    fixture.componentRef.setInput('planetId', 1);
    fixture.detectChanges();
    return fixture;
  }

  it('upgrades a building and reloads on success', async () => {
    const fixture = await setup([building()]);
    fixture.componentInstance.upgrade(building());

    expect(upgrade).toHaveBeenCalledWith(1, 'metal_mine');
    const component = fixture.componentInstance as unknown as { upgrading: () => string | null };
    expect(component.upgrading()).toBeNull();
  });

  it('ignores an upgrade request while already upgrading', async () => {
    const fixture = await setup([building()]);
    const component = fixture.componentInstance as unknown as { upgrading: { set: (v: string | null) => void } };
    component.upgrading.set('metal_mine');

    fixture.componentInstance.upgrade(building());

    expect(upgrade).not.toHaveBeenCalled();
  });

  it('surfaces an error message when upgrading fails', async () => {
    const failingUpgrade = vi.fn(() => throwError(() => ({ error: { message: 'insufficient resources' } })));
    await TestBed.configureTestingModule({
      imports: [
        BuildingListComponent,
        TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
      ],
      providers: [{ provide: UniverseApiService, useValue: { getBuildings: vi.fn(() => of([building()])), upgrade: failingUpgrade } }]
    }).compileComponents();
    const fixture = TestBed.createComponent(BuildingListComponent);
    fixture.componentRef.setInput('planetId', 1);
    fixture.detectChanges();

    fixture.componentInstance.upgrade(building());

    const component = fixture.componentInstance as unknown as { errorMessage: () => string | null };
    expect(component.errorMessage()).toBe('insufficient resources');
  });

  it('hasActiveConstruction reflects whether any building is under construction', async () => {
    const fixture = await setup([building({ constructionActive: true })]);
    expect(fixture.componentInstance.hasActiveConstruction()).toBe(true);
  });

  it('hasActiveConstruction is false when nothing is under construction', async () => {
    const fixture = await setup([building({ constructionActive: false })]);
    expect(fixture.componentInstance.hasActiveConstruction()).toBe(false);
  });

  it('remainingLabel and progress reflect the construction countdown', async () => {
    const endsAt = new Date(Date.now() + 60_000).toISOString();
    const fixture = await setup([building()]);
    expect(fixture.componentInstance.remainingLabel(building({ constructionEndsAt: endsAt }))).toBe('1:00');

    const progress = fixture.componentInstance.progress(building({ constructionEndsAt: endsAt, nextLevelBuildTimeSeconds: 120 }));
    expect(progress).toBeGreaterThanOrEqual(0);
    expect(progress).toBeLessThanOrEqual(100);
  });

  it('hasActiveConstruction is true when only some buildings are under construction (distinguishes .some from .every)', async () => {
    const fixture = await setup([building({ constructionActive: false }), building({ key: 'crystal_mine', constructionActive: true })]);
    expect(fixture.componentInstance.hasActiveConstruction()).toBe(true);
  });

  it('buildingName and buildingDescription look up translations specifically under the "buildings" catalog category', async () => {
    await TestBed.configureTestingModule({
      imports: [
        BuildingListComponent,
        TranslocoTestingModule.forRoot({
          langs: { en: { catalogData: { buildings: { metal_mine: { name: 'Translated Name', description: 'Translated Desc' } } } } },
          translocoConfig: { availableLangs: ['en'], defaultLang: 'en' }
        })
      ],
      providers: [{ provide: UniverseApiService, useValue: { getBuildings: vi.fn(() => of([building()])), upgrade: vi.fn() } }]
    }).compileComponents();
    const fixture = TestBed.createComponent(BuildingListComponent);
    fixture.componentRef.setInput('planetId', 1);
    fixture.detectChanges();

    const component = fixture.componentInstance as unknown as {
      buildingName: (b: BuildingView) => string;
      buildingDescription: (b: BuildingView) => string;
    };
    expect(component.buildingName(building())).toBe('Translated Name');
    expect(component.buildingDescription(building())).toBe('Translated Desc');
  });
});
