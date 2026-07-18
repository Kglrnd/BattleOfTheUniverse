import { TestBed } from '@angular/core/testing';

import { GameIconComponent } from './game-icon.component';

describe('GameIconComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [GameIconComponent] }).compileComponents();
  });

  function create(inputs: Partial<{ kind: string; key: string | null; variant: number | null; size: number }>) {
    const fixture = TestBed.createComponent(GameIconComponent);
    if (inputs.kind !== undefined) fixture.componentRef.setInput('kind', inputs.kind);
    if (inputs.key !== undefined) fixture.componentRef.setInput('key', inputs.key);
    if (inputs.variant !== undefined) fixture.componentRef.setInput('variant', inputs.variant);
    if (inputs.size !== undefined) fixture.componentRef.setInput('size', inputs.size);
    fixture.detectChanges();
    return fixture;
  }

  const glowByKey: Record<string, string> = {
    main_building: '#7dd3fc',
    metal_mine: '#f59e0b',
    crystal_mine: '#67e8f9',
    deuterium_synthesizer: '#7dd3fc',
    solar_plant: '#f59e0b',
    research_lab: '#7dd3fc',
    shipyard: '#7dd3fc',
    defense_facility: '#f87171',
    refinery: '#a78bfa',
    construction_hub: '#fbbf24',
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
    light_defense_tower: '#7dd3fc',
    heavy_defense_tower: '#f87171'
  };

  it.each(Object.entries(glowByKey))('resolves the glow color for catalog key "%s"', (key, expected) => {
    const fixture = create({ kind: 'building', key });
    const component = fixture.componentInstance as unknown as { glow: () => string };
    expect(component.glow()).toBe(expected);
  });

  it('falls back to the default glow color for an unknown key', () => {
    const fixture = create({ kind: 'building', key: 'unknown_key' });
    const component = fixture.componentInstance as unknown as { glow: () => string };
    expect(component.glow()).toBe('#7dd3fc');
  });

  it('falls back to the default glow color when no key is set', () => {
    const fixture = create({ kind: 'building' });
    const component = fixture.componentInstance as unknown as { glow: () => string };
    expect(component.glow()).toBe('#7dd3fc');
  });

  const haloByVariant: Record<number, string> = {
    0: 'rgba(94, 234, 167, 0.28)',
    1: 'rgba(245, 158, 11, 0.28)',
    2: 'rgba(125, 211, 252, 0.3)',
    3: 'rgba(148, 163, 184, 0.24)',
    4: 'rgba(45, 212, 191, 0.3)',
    5: 'rgba(167, 139, 250, 0.3)'
  };

  it.each(Object.entries(haloByVariant))('resolves the halo color for planet variant %s', (variant, expected) => {
    const fixture = create({ kind: 'planet', variant: Number(variant) });
    const component = fixture.componentInstance as unknown as { halo: () => string; planetClass: () => string };
    expect(component.halo()).toBe(expected);
    expect(component.planetClass()).toBe(`planet-${variant}`);
  });

  it('falls back to variant 0 halo when variant is null/unknown', () => {
    const fixture = create({ kind: 'planet' });
    const component = fixture.componentInstance as unknown as { halo: () => string; planetClass: () => string };
    expect(component.halo()).toBe('rgba(94, 234, 167, 0.28)');
    expect(component.planetClass()).toBe('planet-0');
  });

  it('marks heavy_defense_tower with the heavy tower class', () => {
    const fixture = create({ kind: 'defense', key: 'heavy_defense_tower' });
    const component = fixture.componentInstance as unknown as { towerClass: () => string };
    expect(component.towerClass()).toBe('tower-heavy');
  });

  it('marks other defense keys with the light tower class', () => {
    const fixture = create({ kind: 'defense', key: 'light_defense_tower' });
    const component = fixture.componentInstance as unknown as { towerClass: () => string };
    expect(component.towerClass()).toBe('tower-light');
  });

  it('derives the ship CSS class from the item key', () => {
    const fixture = create({ kind: 'ship', key: 'cruiser' });
    const component = fixture.componentInstance as unknown as { shipClass: () => string };
    expect(component.shipClass()).toBe('ship-cruiser');
  });

  it('derives a bare "ship-" class when no key is set', () => {
    const fixture = create({ kind: 'ship' });
    const component = fixture.componentInstance as unknown as { shipClass: () => string };
    expect(component.shipClass()).toBe('ship-');
  });
});
