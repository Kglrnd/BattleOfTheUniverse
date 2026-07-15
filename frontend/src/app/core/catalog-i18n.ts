import { TranslocoService } from '@jsverse/transloco';

/**
 * Catalog definitions (buildings/ships/technologies/defenses) are game-balance data
 * seeded from JSON and editable at runtime via the admin catalog editor - their
 * name/description text lives in the backend, not in the frontend's translation
 * files. We ship German text for the pre-seeded default catalog under the
 * `catalogData.<category>.<key>.*` keys; anything an admin adds or edits later
 * has no translation entry, so this falls back to the raw value from the API
 * (Transloco's default missing-key handler returns the key itself, which is how
 * a miss is detected here).
 */
export type CatalogCategory = 'buildings' | 'ships' | 'technologies' | 'defenses';

export interface CatalogTextItem {
  key: string;
  name: string;
  description: string;
}

function localize(transloco: TranslocoService, path: string, fallback: string): string {
  const value = transloco.translate(path);
  return value === path ? fallback : value;
}

export function catalogName(transloco: TranslocoService, category: CatalogCategory, item: CatalogTextItem): string {
  return localize(transloco, `catalogData.${category}.${item.key}.name`, item.name);
}

export function catalogDescription(transloco: TranslocoService, category: CatalogCategory, item: CatalogTextItem): string {
  return localize(transloco, `catalogData.${category}.${item.key}.description`, item.description);
}
