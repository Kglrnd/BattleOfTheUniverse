import { getBrowserLang } from '@jsverse/transloco';

export type AppLang = 'en' | 'de';

export const AVAILABLE_LANGS: AppLang[] = ['en', 'de'];

const LANG_STORAGE_KEY = 'lang';

export function isAppLang(value: string | null | undefined): value is AppLang {
  return value === 'en' || value === 'de';
}

/** Saved preference, else browser language, else English. */
export function detectInitialLang(): AppLang {
  const saved = localStorage.getItem(LANG_STORAGE_KEY);
  if (isAppLang(saved)) {
    return saved;
  }
  const browserLang = getBrowserLang();
  return browserLang === 'de' ? 'de' : 'en';
}

export function persistLang(lang: AppLang): void {
  localStorage.setItem(LANG_STORAGE_KEY, lang);
}
