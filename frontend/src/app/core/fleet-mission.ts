import { FleetMissionType, ShipQuantity } from './models';

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
  }
}

export function isAttackMission(type: FleetMissionType): boolean {
  return type === 'ATTACK';
}

export function formatShipManifest(ships: ShipQuantity[]): string {
  return ships.map((s) => `${s.quantity}× ${s.shipKey}`).join(', ');
}
