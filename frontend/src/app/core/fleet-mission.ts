import { FleetMissionType } from './models';

export function missionLabel(type: FleetMissionType): string {
  switch (type) {
    case 'COLONIZE':
      return 'Colonizing';
    case 'STATION':
      return 'Stationing';
    case 'ATTACK':
      return 'Attacking';
  }
}

export function isAttackMission(type: FleetMissionType): boolean {
  return type === 'ATTACK';
}
