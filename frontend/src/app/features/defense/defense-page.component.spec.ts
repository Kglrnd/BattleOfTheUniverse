import { TestBed } from '@angular/core/testing';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { of } from 'rxjs';

import { CurrentPlanetService } from '../../core/current-planet.service';
import { PlanetView, TowerView } from '../../core/models';
import { DefenseApiService } from './defense-api.service';
import { DefensePageComponent } from './defense-page.component';

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

describe('DefensePageComponent', () => {
  let select: ReturnType<typeof vi.fn>;

  async function setup(planets: PlanetView[], selectedPlanetId: number | null) {
    select = vi.fn();
    await TestBed.configureTestingModule({
      imports: [
        DefensePageComponent,
        TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
      ],
      providers: [
        {
          provide: CurrentPlanetService,
          useValue: { planets: () => planets, selectedPlanetId: () => selectedPlanetId, select }
        },
        { provide: DefenseApiService, useValue: { list: vi.fn(() => of([] as TowerView[])), build: vi.fn() } }
      ]
    }).compileComponents();
  }

  it('shows a hint when there are no planets', async () => {
    await setup([], null);
    const fixture = TestBed.createComponent(DefensePageComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('select')).toBeNull();
  });

  it('renders the planet select and the defense list when planets exist', async () => {
    await setup([planet(1), planet(2)], 1);
    const fixture = TestBed.createComponent(DefensePageComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('select')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('app-defense')).not.toBeNull();
  });

  it('onPlanetChange delegates to CurrentPlanetService.select', async () => {
    await setup([planet(1), planet(2)], 1);
    const fixture = TestBed.createComponent(DefensePageComponent);
    fixture.detectChanges();

    fixture.componentInstance.onPlanetChange(2);
    expect(select).toHaveBeenCalledWith(2);
  });
});
