import { BOMB_SHIP_KEY, formatCargo, formatShipManifest, GALAXY_CLASS_SHIP_KEY, INVASION_SHIP_KEY, isAttackMission, missionLabel, shipCategory } from './fleet-mission';
import { FleetMissionType } from './models';

describe('shipCategory', () => {
  it('categorizes utility ships', () => {
    expect(shipCategory('small_cargo')).toBe('UTILITY');
    expect(shipCategory('colony_ship')).toBe('UTILITY');
    expect(shipCategory('espionage_probe')).toBe('UTILITY');
  });

  it('categorizes special "weapon of last resort" ships', () => {
    expect(shipCategory(BOMB_SHIP_KEY)).toBe('SPECIAL');
    expect(shipCategory(INVASION_SHIP_KEY)).toBe('SPECIAL');
  });

  it('defaults to combat category for anything else', () => {
    expect(shipCategory(GALAXY_CLASS_SHIP_KEY)).toBe('COMBAT');
    expect(shipCategory('light_fighter')).toBe('COMBAT');
  });
});

describe('missionLabel', () => {
  const cases: [FleetMissionType, string][] = [
    ['COLONIZE', 'Colonizing'],
    ['STATION', 'Stationing'],
    ['ATTACK', 'Attacking'],
    ['ESPIONAGE', 'Spying'],
    ['TRANSPORT', 'Transporting'],
    ['BOMBARD', 'Bombarding'],
    ['INVADE', 'Invading']
  ];

  it.each(cases)('labels %s as %s', (type, label) => {
    expect(missionLabel(type)).toBe(label);
  });
});

describe('isAttackMission', () => {
  it('treats ATTACK, BOMBARD and INVADE as attack missions', () => {
    expect(isAttackMission('ATTACK')).toBe(true);
    expect(isAttackMission('BOMBARD')).toBe(true);
    expect(isAttackMission('INVADE')).toBe(true);
  });

  it('treats other mission types as non-attack', () => {
    expect(isAttackMission('COLONIZE')).toBe(false);
    expect(isAttackMission('STATION')).toBe(false);
    expect(isAttackMission('ESPIONAGE')).toBe(false);
    expect(isAttackMission('TRANSPORT')).toBe(false);
  });
});

describe('formatShipManifest', () => {
  it('formats an empty list as an empty string', () => {
    expect(formatShipManifest([])).toBe('');
  });

  it('joins ship quantities with a multiplication sign', () => {
    expect(formatShipManifest([{ shipKey: 'light_fighter', quantity: 3 }])).toBe('3× light_fighter');
  });

  it('joins multiple ship quantities with a comma', () => {
    expect(
      formatShipManifest([
        { shipKey: 'light_fighter', quantity: 3 },
        { shipKey: 'cruiser', quantity: 1 }
      ])
    ).toBe('3× light_fighter, 1× cruiser');
  });
});

describe('formatCargo', () => {
  it('formats an empty list as an empty string', () => {
    expect(formatCargo([])).toBe('');
  });

  it('joins resource quantities with the resource key', () => {
    expect(formatCargo([{ resourceKey: 'METAL', amount: 500 }])).toBe('500 METAL');
  });

  it('joins multiple resource quantities with a comma', () => {
    expect(
      formatCargo([
        { resourceKey: 'METAL', amount: 500 },
        { resourceKey: 'CRYSTAL', amount: 250 }
      ])
    ).toBe('500 METAL, 250 CRYSTAL');
  });
});
