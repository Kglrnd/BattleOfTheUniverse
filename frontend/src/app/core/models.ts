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
