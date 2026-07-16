import { FleetMissionType, ResourceQuantity, ShipQuantity } from './models';

/** Catalog keys for the "weapon of last resort" ships - shared by dispatch validation and UI hints. */
export const GALAXY_CLASS_SHIP_KEY = 'galaxy_class';
export const BOMB_SHIP_KEY = 'orbital_bomb';
export const INVASION_SHIP_KEY = 'invasion_unit';

export function missionLabel(type: FleetMissionType): string {
  switch (type) {
    case 'COLONIZE':
      return 'Colonizing';
    case 'STATION':
      return 'Stationing';
    case 'ATTACK':
      return 'Attacking';
    case 'ESPIONAGE':
      return 'Spying';
    case 'TRANSPORT':
      return 'Transporting';
    case 'BOMBARD':
      return 'Bombarding';
    case 'INVADE':
      return 'Invading';
  }
}

export function isAttackMission(type: FleetMissionType): boolean {
  return type === 'ATTACK' || type === 'BOMBARD' || type === 'INVADE';
}

export function formatShipManifest(ships: ShipQuantity[]): string {
  return ships.map((s) => `${s.quantity}× ${s.shipKey}`).join(', ');
}

export function formatCargo(cargo: ResourceQuantity[]): string {
  return cargo.map((c) => `${c.amount} ${c.resourceKey}`).join(', ');
}
