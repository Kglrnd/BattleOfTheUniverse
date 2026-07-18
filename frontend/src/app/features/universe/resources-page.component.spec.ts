import { TestBed } from '@angular/core/testing';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { of } from 'rxjs';

import { CurrentPlanetService } from '../../core/current-planet.service';
import { PlanetView, ResourceProductionView } from '../../core/models';
import { ResourcesPageComponent } from './resources-page.component';
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

describe('ResourcesPageComponent', () => {
  let select: ReturnType<typeof vi.fn>;

  async function setup(planets: PlanetView[], selectedPlanetId: number | null) {
    select = vi.fn();
    await TestBed.configureTestingModule({
      imports: [
        ResourcesPageComponent,
        TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
      ],
      providers: [
        {
          provide: CurrentPlanetService,
          useValue: { planets: () => planets, selectedPlanetId: () => selectedPlanetId, select }
        },
        { provide: UniverseApiService, useValue: { getProduction: vi.fn(() => of([] as ResourceProductionView[])) } }
      ]
    }).compileComponents();
  }

  it('shows a hint when there are no planets', async () => {
    await setup([], null);
    const fixture = TestBed.createComponent(ResourcesPageComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('select')).toBeNull();
  });

  it('renders the planet select and production list when planets exist', async () => {
    await setup([planet(1)], 1);
    const fixture = TestBed.createComponent(ResourcesPageComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('select')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('app-resource-production-list')).not.toBeNull();
  });

  it('onPlanetChange delegates to CurrentPlanetService.select', async () => {
    await setup([planet(1), planet(2)], 1);
    const fixture = TestBed.createComponent(ResourcesPageComponent);
    fixture.detectChanges();

    fixture.componentInstance.onPlanetChange(2);
    expect(select).toHaveBeenCalledWith(2);
  });
});
