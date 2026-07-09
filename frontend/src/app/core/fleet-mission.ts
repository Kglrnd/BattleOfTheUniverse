import { FleetMissionType, ResourceQuantity, ShipQuantity } from './models';

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
  }
}

export function isAttackMission(type: FleetMissionType): boolean {
  return type === 'ATTACK';
}

export function formatShipManifest(ships: ShipQuantity[]): string {
  return ships.map((s) => `${s.quantity}× ${s.shipKey}`).join(', ');
}

export function formatCargo(cargo: ResourceQuantity[]): string {
  return cargo.map((c) => `${c.amount} ${c.resourceKey}`).join(', ');
}
