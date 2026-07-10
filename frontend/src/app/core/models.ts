export type Role = 'PLAYER' | 'MODERATOR' | 'ADMIN';

export interface UserView {
  id: number;
  username: string;
  email: string | null;
  role: Role;
}

export interface AdminUserView {
  id: number;
  username: string;
  email: string;
  role: Role;
  active: boolean;
  createdAt: string;
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
  researchEfficiency: number;
}

export interface AdminPlanetView {
  id: number;
  name: string;
  ownerId: number;
  ownerUsername: string;
  galaxy: number;
  system: number;
  position: number;
  coordinates: string;
  planetClass: string;
  homeworld: boolean;
  createdAt: string;
}

export type ResourceKey = 'METAL' | 'CRYSTAL' | 'DEUTERIUM' | 'HYDROGEN' | 'ENERGY';

export interface ResourceView {
  resourceKey: ResourceKey;
  displayName: string;
  amount: number;
}

export interface ResourceCost {
  metal: number;
  crystal: number;
  deuterium: number;
}

export interface LockedRequirement {
  label: string;
  requiredLevel: number;
  currentLevel: number;
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
  unlocked: boolean;
  missingRequirements: LockedRequirement[];
  researchEfficiency: number | null;
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
  cargoCapacity: number;
  hydrogenConsumption: number;
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

export interface TowerView {
  key: string;
  name: string;
  description: string;
  owned: number;
  defense: number;
  unitCost: ResourceCost;
  unitBuildTimeSeconds: number;
  buildActive: boolean;
  buildingQuantity: number | null;
  buildEndsAt: string | null;
  unlocked: boolean;
  missingRequirements: LockedRequirement[];
}

export interface TowerBuildResponse {
  towerKey: string;
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
  unlocked: boolean;
  missingRequirements: LockedRequirement[];
}

export interface ResearchStartResponse {
  technologyKey: string;
  targetLevel: number;
  endsAt: string;
}

export interface ResearchPlanetOption {
  planetId: number;
  name: string;
  coordinates: string;
  researchEfficiency: number;
  researchLabLevel: number;
  active: boolean;
}

export type FleetMissionType = 'COLONIZE' | 'STATION' | 'ATTACK' | 'ESPIONAGE' | 'TRANSPORT';

export interface ShipQuantity {
  shipKey: string;
  quantity: number;
}

export interface ResourceQuantity {
  resourceKey: ResourceKey;
  amount: number;
}

export interface DispatchRequest {
  originPlanetId: number;
  ships: ShipQuantity[];
  missionType: FleetMissionType;
  targetGalaxy: number;
  targetSystem: number;
  targetPosition: number;
  driveKey: string;
  cargo?: ResourceQuantity[];
}

export interface FleetMovementView {
  id: number;
  originPlanetId: number;
  ships: ShipQuantity[];
  cargo: ResourceQuantity[];
  missionType: FleetMissionType;
  targetGalaxy: number;
  targetSystem: number;
  targetPosition: number;
  departedAt: string;
  arrivesAt: string;
}

export interface DriveOptionsRequest {
  originPlanetId: number;
  ships: ShipQuantity[];
  targetGalaxy: number;
  targetSystem: number;
  targetPosition: number;
}

export interface DriveOptionView {
  key: string;
  name: string;
  driveScope: DriveScope;
  level: number;
  speedMultiplier: number;
  etaSeconds: number;
  fuelCost: number;
}

export interface IncomingMovementView {
  id: number;
  ships: ShipQuantity[];
  cargo: ResourceQuantity[];
  missionType: FleetMissionType;
  originPlanetId: number;
  originOwnerUsername: string;
  targetPlanetId: number;
  targetPlanetName: string;
  departedAt: string;
  arrivesAt: string;
}

export type MessageType = 'PLAYER' | 'SYSTEM';

export interface MessageView {
  id: number;
  senderUsername: string;
  recipientUsername: string;
  subject: string;
  body: string;
  type: MessageType;
  sentAt: string;
  readAt: string | null;
}

export interface SendMessageRequest {
  recipientUsername: string;
  subject: string;
  body: string;
}

export interface UnreadCountView {
  count: number;
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

export interface HighscoreEntry {
  rank: number;
  userId: number;
  username: string;
  score: number;
}

export interface HighscoreResponse {
  top: HighscoreEntry[];
  me: HighscoreEntry | null;
}
