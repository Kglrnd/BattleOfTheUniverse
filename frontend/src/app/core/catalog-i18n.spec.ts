import { TranslocoService } from '@jsverse/transloco';

import { catalogDescription, catalogName } from './catalog-i18n';

describe('catalog-i18n', () => {
  function translocoStub(translations: Record<string, string>) {
    return {
      translate: vi.fn((path: string) => translations[path] ?? path)
    } as unknown as TranslocoService;
  }

  it('returns the translated name when a translation exists', () => {
    const transloco = translocoStub({ 'catalogData.buildings.metal_mine.name': 'Metallmine' });
    const result = catalogName(transloco, 'buildings', { key: 'metal_mine', name: 'Metal Mine', description: 'desc' });
    expect(result).toBe('Metallmine');
  });

  it('falls back to the raw name when no translation exists', () => {
    const transloco = translocoStub({});
    const result = catalogName(transloco, 'buildings', { key: 'custom_building', name: 'Custom Building', description: 'desc' });
    expect(result).toBe('Custom Building');
  });

  it('returns the translated description when a translation exists', () => {
    const transloco = translocoStub({ 'catalogData.ships.light_fighter.description': 'Ein kleines Schiff' });
    const result = catalogDescription(transloco, 'ships', { key: 'light_fighter', name: 'Light Fighter', description: 'A small ship' });
    expect(result).toBe('Ein kleines Schiff');
  });

  it('falls back to the raw description when no translation exists', () => {
    const transloco = translocoStub({});
    const result = catalogDescription(transloco, 'technologies', { key: 'custom_tech', name: 'Custom', description: 'raw desc' });
    expect(result).toBe('raw desc');
  });
});
