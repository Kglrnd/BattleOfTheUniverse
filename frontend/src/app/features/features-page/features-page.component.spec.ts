import { TestBed } from '@angular/core/testing';
import { TranslocoTestingModule } from '@jsverse/transloco';

import { FeaturesPageComponent } from './features-page.component';

describe('FeaturesPageComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        FeaturesPageComponent,
        TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
      ]
    }).compileComponents();
  });

  it('lists the exact implemented feature keys, in order', () => {
    const fixture = TestBed.createComponent(FeaturesPageComponent);
    fixture.detectChanges();

    const component = fixture.componentInstance as unknown as { implementedKeys: string[] };
    expect(component.implementedKeys).toEqual([
      'planets',
      'resources',
      'buildings',
      'research',
      'fleet',
      'defense',
      'combat',
      'galaxy',
      'messages',
      'highscore',
      'adminCatalog',
      'accounts',
      'mobile'
    ]);
  });

  it('lists the exact planned feature keys, in order', () => {
    const fixture = TestBed.createComponent(FeaturesPageComponent);
    fixture.detectChanges();

    const component = fixture.componentInstance as unknown as { plannedKeys: string[] };
    expect(component.plannedKeys).toEqual(['multiPlanetBuild', 'tradingPost', 'clans', 'recycling']);
  });
});
