import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { of } from 'rxjs';

import { CurrentPlanetService } from '../../core/current-planet.service';
import { PlanetView, ShipyardView } from '../../core/models';
import { FleetApiService } from '../fleet/fleet-api.service';
import { UniverseApiService } from '../universe/universe-api.service';
import { EspionagePageComponent } from './espionage-page.component';

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

describe('EspionagePageComponent', () => {
  let select: ReturnType<typeof vi.fn>;

  async function setup(planets: PlanetView[], selectedPlanetId: number | null) {
    select = vi.fn();
    await TestBed.configureTestingModule({
      imports: [
        EspionagePageComponent,
        TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
      ],
      providers: [
        provideRouter([]),
        {
          provide: CurrentPlanetService,
          useValue: { planets: () => planets, selectedPlanetId: () => selectedPlanetId, select }
        },
        { provide: UniverseApiService, useValue: { getShips: vi.fn(() => of([] as ShipyardView[])) } },
        { provide: FleetApiService, useValue: { driveOptions: vi.fn(() => of([])), dispatch: vi.fn() } }
      ]
    }).compileComponents();
  }

  it('shows a hint when there are no planets', async () => {
    await setup([], null);
    const fixture = TestBed.createComponent(EspionagePageComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('select')).toBeNull();
  });

  it('renders the planet select and espionage panel when planets exist', async () => {
    await setup([planet(1)], 1);
    const fixture = TestBed.createComponent(EspionagePageComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('select')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('app-espionage')).not.toBeNull();
  });

  it('onPlanetChange delegates to CurrentPlanetService.select', async () => {
    await setup([planet(1), planet(2)], 1);
    const fixture = TestBed.createComponent(EspionagePageComponent);
    fixture.detectChanges();

    fixture.componentInstance.onPlanetChange(2);
    expect(select).toHaveBeenCalledWith(2);
  });
});
