import { Component, computed, input } from '@angular/core';

export type GameIconKind = 'building' | 'ship' | 'defense' | 'planet';

/** Glow tint per catalog key, echoed as a CSS custom property by the icon's radial highlight. */
const GLOW_BY_KEY: Record<string, string> = {
  // buildings
  main_building: '#7dd3fc',
  metal_mine: '#f59e0b',
  crystal_mine: '#67e8f9',
  deuterium_synthesizer: '#7dd3fc',
  solar_plant: '#f59e0b',
  research_lab: '#7dd3fc',
  shipyard: '#7dd3fc',
  defense_facility: '#f87171',
  refinery: '#a78bfa',
  // ships
  light_fighter: '#7dd3fc',
  heavy_fighter: '#7dd3fc',
  light_cruiser: '#fb923c',
  cruiser: '#7dd3fc',
  heavy_cruiser: '#94a3b8',
  battlecruiser: '#f87171',
  galaxy_class: '#fbbf24',
  small_cargo: '#f59e0b',
  colony_ship: '#34d399',
  espionage_probe: '#7dd3fc',
  orbital_bomb: '#f87171',
  invasion_unit: '#f97316',
  // defense
  light_defense_tower: '#7dd3fc',
  heavy_defense_tower: '#f87171'
};

const HALO_BY_VARIANT: Record<number, string> = {
  0: 'rgba(94, 234, 167, 0.28)',
  1: 'rgba(245, 158, 11, 0.28)',
  2: 'rgba(125, 211, 252, 0.3)',
  3: 'rgba(148, 163, 184, 0.24)',
  4: 'rgba(45, 212, 191, 0.3)',
  5: 'rgba(167, 139, 250, 0.3)'
};

/**
 * Renders a catalog item (building/ship/defense tower) or a planet as a CSS-drawn icon
 * instead of a raster asset - inherently transparent, crisp at any size, and always in
 * sync with the app's color tokens. See core/game-icon/game-icon.component.css for the
 * per-key glyph shapes.
 */
@Component({
  selector: 'app-game-icon',
  templateUrl: './game-icon.component.html',
  styleUrl: './game-icon.component.css'
})
export class GameIconComponent {
  readonly kind = input.required<GameIconKind>();
  /** Catalog key; ignored for kind="planet". */
  readonly itemKey = input<string | null>(null, { alias: 'key' });
  /** PlanetView.imageVariant; ignored for every other kind. */
  readonly variant = input<number | null>(null);
  readonly size = input<number>(48);

  protected readonly glow = computed(() => GLOW_BY_KEY[this.itemKey() ?? ''] ?? '#7dd3fc');
  protected readonly halo = computed(() => HALO_BY_VARIANT[this.variant() ?? 0] ?? HALO_BY_VARIANT[0]);
  protected readonly planetClass = computed(() => `planet-${this.variant() ?? 0}`);
  protected readonly shipClass = computed(() => `ship-${this.itemKey() ?? ''}`);
  protected readonly towerClass = computed(() => (this.itemKey() === 'heavy_defense_tower' ? 'tower-heavy' : 'tower-light'));
}
