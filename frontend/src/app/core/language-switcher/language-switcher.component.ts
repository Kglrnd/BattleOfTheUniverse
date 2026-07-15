import { Component, inject } from '@angular/core';
import { TranslocoService } from '@jsverse/transloco';

import { AuthService } from '../auth.service';
import { AppLang, AVAILABLE_LANGS, persistLang } from '../language';

@Component({
  selector: 'app-language-switcher',
  imports: [],
  templateUrl: './language-switcher.component.html',
  styleUrl: './language-switcher.component.css'
})
export class LanguageSwitcherComponent {
  private readonly transloco = inject(TranslocoService);
  private readonly auth = inject(AuthService);

  protected readonly langs = AVAILABLE_LANGS;
  protected readonly activeLang = this.transloco.activeLang;

  select(lang: AppLang): void {
    this.transloco.setActiveLang(lang);
    persistLang(lang);
    if (this.auth.isAuthenticated()) {
      this.auth.updateLanguage(lang).subscribe();
    }
  }
}
