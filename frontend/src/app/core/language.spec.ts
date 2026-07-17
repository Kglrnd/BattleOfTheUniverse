import { detectInitialLang, isAppLang, persistLang } from './language';

function stubBrowserLanguage(lang: string): void {
  vi.spyOn(navigator, 'language', 'get').mockReturnValue(lang);
  vi.spyOn(navigator, 'languages', 'get').mockReturnValue([lang]);
}

describe('isAppLang', () => {
  it('accepts "en" and "de"', () => {
    expect(isAppLang('en')).toBe(true);
    expect(isAppLang('de')).toBe(true);
  });

  it('rejects anything else, including null/undefined', () => {
    expect(isAppLang('fr')).toBe(false);
    expect(isAppLang(null)).toBe(false);
    expect(isAppLang(undefined)).toBe(false);
  });
});

describe('detectInitialLang', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it('returns the saved preference when present', () => {
    localStorage.setItem('lang', 'de');
    expect(detectInitialLang()).toBe('de');
  });

  it('falls back to the browser language when it is German', () => {
    stubBrowserLanguage('de-DE');
    expect(detectInitialLang()).toBe('de');
  });

  it('falls back to English when the browser language is neither saved nor German', () => {
    stubBrowserLanguage('fr-FR');
    expect(detectInitialLang()).toBe('en');
  });
});

describe('persistLang', () => {
  it('saves the language preference to localStorage', () => {
    localStorage.clear();
    persistLang('de');
    expect(localStorage.getItem('lang')).toBe('de');
  });
});
