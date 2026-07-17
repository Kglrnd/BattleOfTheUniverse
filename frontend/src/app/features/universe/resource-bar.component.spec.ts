import { TestBed } from '@angular/core/testing';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { of } from 'rxjs';

import { CurrentPlanetService } from '../../core/current-planet.service';
import { ResourceView } from '../../core/models';
import { ResourceBarComponent } from './resource-bar.component';
import { UniverseApiService } from './universe-api.service';

describe('ResourceBarComponent', () => {
  let getResources: ReturnType<typeof vi.fn>;

  async function setup(selectedPlanetId: number | null) {
    getResources = vi.fn(() => of([] as ResourceView[]));
    await TestBed.configureTestingModule({
      imports: [
        ResourceBarComponent,
        TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
      ],
      providers: [
        { provide: CurrentPlanetService, useValue: { selectedPlanetId: () => selectedPlanetId, selectedPlanet: () => null } },
        { provide: UniverseApiService, useValue: { getResources } }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(ResourceBarComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('does not fetch resources when no planet is selected', async () => {
    await setup(null);
    expect(getResources).not.toHaveBeenCalled();
  });

  it('fetches resources for the currently selected planet', async () => {
    await setup(5);
    expect(getResources).toHaveBeenCalledWith(5);
  });
});
