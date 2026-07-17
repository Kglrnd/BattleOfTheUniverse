import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { of } from 'rxjs';

import { FleetMovementView, IncomingMovementView, PlanetView } from '../../core/models';
import { FleetApiService } from '../fleet/fleet-api.service';
import { PlanetListComponent } from './planet-list.component';
import { UniverseApiService } from './universe-api.service';

function planet(id: number): PlanetView {
  return {
    id,
    name: `Planet ${id}`,
    galaxy: 1,
    system: 1,
    position: id,
    coordinates: `[1:1:${id}]`,
    planetClass: 'TEMPERATE',
    homeworld: false,
    researchEfficiency: 100,
    imageVariant: 0,
    destroyed: false
  };
}

function outgoing(originPlanetId: number): FleetMovementView {
  return {
    id: 1,
    originPlanetId,
    ships: [],
    cargo: [],
    missionType: 'TRANSPORT',
    targetGalaxy: 1,
    targetSystem: 1,
    targetPosition: 2,
    departedAt: new Date().toISOString(),
    arrivesAt: new Date().toISOString()
  };
}

function incoming(targetPlanetId: number, missionType: IncomingMovementView['missionType']): IncomingMovementView {
  return {
    id: 1,
    ships: [],
    cargo: [],
    missionType,
    originPlanetId: 99,
    originOwnerUsername: 'enemy',
    targetPlanetId,
    targetPlanetName: 'Home',
    departedAt: new Date().toISOString(),
    arrivesAt: new Date().toISOString()
  };
}

describe('PlanetListComponent', () => {
  async function setup(planets: PlanetView[], outgoingMovements: FleetMovementView[], incomingMovements: IncomingMovementView[]) {
    await TestBed.configureTestingModule({
      imports: [
        PlanetListComponent,
        TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
      ],
      providers: [
        provideRouter([]),
        { provide: UniverseApiService, useValue: { listPlanets: vi.fn(() => of(planets)) } },
        {
          provide: FleetApiService,
          useValue: { movements: vi.fn(() => of(outgoingMovements)), incomingMovements: vi.fn(() => of(incomingMovements)) }
        }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(PlanetListComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('counts outgoing movements per planet', async () => {
    const fixture = await setup([planet(1)], [outgoing(1), outgoing(1), outgoing(2)], []);
    expect(fixture.componentInstance.outgoingCount(1)).toBe(2);
    expect(fixture.componentInstance.outgoingCount(2)).toBe(1);
    expect(fixture.componentInstance.outgoingCount(3)).toBe(0);
  });

  it('counts incoming attack movements separately from friendly ones', async () => {
    const fixture = await setup(
      [planet(1)],
      [],
      [incoming(1, 'ATTACK'), incoming(1, 'BOMBARD'), incoming(1, 'TRANSPORT'), incoming(2, 'ATTACK')]
    );
    expect(fixture.componentInstance.incomingAttackCount(1)).toBe(2);
    expect(fixture.componentInstance.incomingFriendlyCount(1)).toBe(1);
    expect(fixture.componentInstance.hasIncomingAttack(1)).toBe(true);
    expect(fixture.componentInstance.hasIncomingAttack(2)).toBe(true);
    expect(fixture.componentInstance.hasIncomingAttack(3)).toBe(false);
  });
});
