import { Component } from '@angular/core';
import { TranslocoDirective } from '@jsverse/transloco';

/** Static roadmap page: what's already in the game, and what's planned next. No backend data - just i18n content. */
@Component({
  selector: 'app-features-page',
  imports: [TranslocoDirective],
  templateUrl: './features-page.component.html',
  styleUrl: './features-page.component.css'
})
export class FeaturesPageComponent {
  protected readonly implementedKeys = [
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
  ];

  protected readonly plannedKeys = ['multiPlanetBuild', 'tradingPost', 'clans', 'recycling'];
}
