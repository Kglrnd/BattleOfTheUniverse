import { TestBed } from '@angular/core/testing';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { of } from 'rxjs';

import { ResourceProductionView } from '../../core/models';
import { ResourceProductionListComponent } from './resource-production-list.component';
import { UniverseApiService } from './universe-api.service';

function production(overrides: Partial<ResourceProductionView> = {}): ResourceProductionView {
  return {
    buildingKey: 'metal_mine',
    buildingName: 'Metal Mine',
    buildingDescription: 'desc',
    resourceKey: 'METAL',
    resourceDisplayName: 'Metal',
    currentLevel: 3,
    productionEfficiency: 100,
    currentProductionPerHour: 120,
    levels: [
      { level: 1, productionPerHour: 60, currentLevel: false },
      { level: 2, productionPerHour: 90, currentLevel: false },
      { level: 3, productionPerHour: 120, currentLevel: true },
      { level: 4, productionPerHour: 150, currentLevel: false }
    ],
    ...overrides
  };
}

describe('ResourceProductionListComponent', () => {
  async function setup(productions: ResourceProductionView[]) {
    await TestBed.configureTestingModule({
      imports: [
        ResourceProductionListComponent,
        TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
      ],
      providers: [{ provide: UniverseApiService, useValue: { getProduction: vi.fn(() => of(productions)) } }]
    }).compileComponents();

    const fixture = TestBed.createComponent(ResourceProductionListComponent);
    fixture.componentRef.setInput('planetId', 1);
    fixture.detectChanges();
    return fixture;
  }

  it('shows a hint when there are no resource-producing buildings', async () => {
    const fixture = await setup([]);
    expect(fixture.nativeElement.querySelector('.production-card')).toBeNull();
  });

  it('renders a card with the production level table for each producing building', async () => {
    const fixture = await setup([production()]);
    const rows = fixture.nativeElement.querySelectorAll('.production-table tbody tr');

    expect(fixture.nativeElement.querySelectorAll('.production-card').length).toBe(1);
    expect(rows.length).toBe(4);
    expect(fixture.nativeElement.querySelector('.current-row')?.textContent).toContain('3');
  });

  it('buildingName and buildingDescription look up translations specifically under the "buildings" catalog category', async () => {
    await TestBed.configureTestingModule({
      imports: [
        ResourceProductionListComponent,
        TranslocoTestingModule.forRoot({
          langs: { en: { catalogData: { buildings: { metal_mine: { name: 'Translated Name', description: 'Translated Desc' } } } } },
          translocoConfig: { availableLangs: ['en'], defaultLang: 'en' }
        })
      ],
      providers: [{ provide: UniverseApiService, useValue: { getProduction: vi.fn(() => of([production()])) } }]
    }).compileComponents();
    const fixture = TestBed.createComponent(ResourceProductionListComponent);
    fixture.componentRef.setInput('planetId', 1);
    fixture.detectChanges();

    const component = fixture.componentInstance as unknown as {
      buildingName: (p: ResourceProductionView) => string;
      buildingDescription: (p: ResourceProductionView) => string;
    };
    expect(component.buildingName(production())).toBe('Translated Name');
    expect(component.buildingDescription(production())).toBe('Translated Desc');
  });
});
