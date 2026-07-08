export type Role = 'PLAYER' | 'ADMIN';

export interface UserView {
  id: number;
  username: string;
  email: string | null;
  role: Role;
}

export interface PlanetView {
  id: number;
  name: string;
  galaxy: number;
  system: number;
  position: number;
  coordinates: string;
  planetClass: string;
  homeworld: boolean;
}

export interface ResourceView {
  resourceKey: 'METAL' | 'CRYSTAL' | 'DEUTERIUM' | 'ENERGY';
  displayName: string;
  amount: number;
}

export interface ResourceCost {
  metal: number;
  crystal: number;
  deuterium: number;
}

export interface BuildingView {
  key: string;
  name: string;
  description: string;
  level: number;
  nextLevelCost: ResourceCost;
  nextLevelBuildTimeSeconds: number;
  constructionActive: boolean;
  constructionEndsAt: string | null;
}

export interface UpgradeResponse {
  buildingKey: string;
  targetLevel: number;
  endsAt: string;
}

export interface ShipyardView {
  key: string;
  name: string;
  description: string;
  owned: number;
  unitCost: ResourceCost;
  unitBuildTimeSeconds: number;
  buildActive: boolean;
  buildingQuantity: number | null;
  buildEndsAt: string | null;
}

export interface ShipyardBuildResponse {
  shipKey: string;
  quantity: number;
  endsAt: string;
}

export type DriveScope = 'NONE' | 'SYSTEM' | 'INTER_SYSTEM' | 'GALAXY';

export interface TechnologyView {
  key: string;
  name: string;
  description: string;
  driveScope: DriveScope;
  level: number;
  nextLevelCost: ResourceCost;
  nextLevelResearchTimeSeconds: number;
  researchActive: boolean;
  researchTargetLevel: number | null;
  researchEndsAt: string | null;
}

export interface ResearchStartResponse {
  technologyKey: string;
  targetLevel: number;
  endsAt: string;
}

export type FleetMissionType = 'COLONIZE' | 'STATION';

export interface DispatchRequest {
  originPlanetId: number;
  shipKey: string;
  quantity: number;
  missionType: FleetMissionType;
  targetGalaxy: number;
  targetSystem: number;
  targetPosition: number;
}

export interface FleetMovementView {
  id: number;
  originPlanetId: number;
  shipKey: string;
  quantity: number;
  missionType: FleetMissionType;
  targetGalaxy: number;
  targetSystem: number;
  targetPosition: number;
  departedAt: string;
  arrivesAt: string;
}

export interface TravelTimeView {
  etaSeconds: number;
}

export type SlotStatus = 'OCCUPIED' | 'FREE' | 'VOID';

export interface SystemSlotView {
  position: number;
  status: SlotStatus;
  planet: PlanetView | null;
}

export interface SystemView {
  galaxy: number;
  system: number;
  slots: SystemSlotView[];
}
