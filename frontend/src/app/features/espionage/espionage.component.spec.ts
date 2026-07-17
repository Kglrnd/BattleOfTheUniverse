import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { of, throwError } from 'rxjs';

import { DriveOptionView, ShipyardView } from '../../core/models';
import { FleetApiService } from '../fleet/fleet-api.service';
import { UniverseApiService } from '../universe/universe-api.service';
import { EspionageComponent } from './espionage.component';

function probeShip(owned: number): ShipyardView {
  return {
    key: 'espionage_probe',
    name: 'Espionage Probe',
    description: 'desc',
    owned,
    buildingQuantity: 0,
    unitCost: { metal: 0, crystal: 0, deuterium: 0 },
    unitBuildTimeSeconds: 0,
    buildActive: false,
    buildEndsAt: null,
    unlocked: true,
    missingRequirements: []
  } as unknown as ShipyardView;
}

function driveOption(key: string, etaSeconds: number): DriveOptionView {
  return { key, name: key, driveScope: 'GALAXY', level: 1, speedMultiplier: 1, etaSeconds, fuelCost: 10 };
}

describe('EspionageComponent', () => {
  let driveOptions: ReturnType<typeof vi.fn>;
  let dispatch: ReturnType<typeof vi.fn>;

  async function setup(
    owned: number,
    driveOptionsOverride: ReturnType<typeof vi.fn> = vi.fn(() => of([driveOption('impulse_drive', 100)])),
    dispatchOverride: ReturnType<typeof vi.fn> = vi.fn(() => of({}))
  ) {
    driveOptions = driveOptionsOverride;
    dispatch = dispatchOverride;

    await TestBed.configureTestingModule({
      imports: [
        EspionageComponent,
        TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
      ],
      providers: [
        provideRouter([]),
        { provide: UniverseApiService, useValue: { getShips: vi.fn(() => of([probeShip(owned)])) } },
        { provide: FleetApiService, useValue: { driveOptions, dispatch } }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(EspionageComponent);
    fixture.componentRef.setInput('planetId', 1);
    fixture.detectChanges();
    return fixture;
  }

  function inputEvent(value: number): Event {
    const input = document.createElement('input');
    input.type = 'number';
    input.value = String(value);
    return { target: input } as unknown as Event;
  }

  it('clamps the requested quantity to the number of probes owned', async () => {
    const fixture = await setup(3);
    fixture.componentInstance.updateQuantity(inputEvent(10), 1, 1, 5);

    const component = fixture.componentInstance as unknown as { quantity: () => number };
    expect(component.quantity()).toBe(3);
  });

  it('resets drive options when quantity or target coordinates are invalid', async () => {
    const fixture = await setup(3);
    fixture.componentInstance.updateQuantity(inputEvent(0), 1, 1, 5);

    const component = fixture.componentInstance as unknown as { driveOptions: () => DriveOptionView[] };
    expect(component.driveOptions()).toEqual([]);
  });

  it('fetches drive options and selects the fastest by default', async () => {
    driveOptions = vi.fn(() => of([driveOption('slow', 200), driveOption('fast', 50)]));
    await TestBed.configureTestingModule({
      imports: [
        EspionageComponent,
        TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
      ],
      providers: [
        provideRouter([]),
        { provide: UniverseApiService, useValue: { getShips: vi.fn(() => of([probeShip(5)])) } },
        { provide: FleetApiService, useValue: { driveOptions, dispatch } }
      ]
    }).compileComponents();
    const fixture = TestBed.createComponent(EspionageComponent);
    fixture.componentRef.setInput('planetId', 1);
    fixture.detectChanges();

    fixture.componentInstance.updateQuantity(inputEvent(2), 1, 1, 5);

    const component = fixture.componentInstance as unknown as { selectedDriveKey: () => string | null };
    expect(component.selectedDriveKey()).toBe('fast');
  });

  it('surfaces an error when there are no drive-capable ships', async () => {
    const fixture = await setup(3, vi.fn(() => of([])));
    fixture.componentInstance.updateQuantity(inputEvent(2), 1, 1, 5);

    const component = fixture.componentInstance as unknown as { driveOptionsError: () => string | null };
    expect(component.driveOptionsError()).toBeTruthy();
  });

  it('surfaces an error when fetching drive options fails', async () => {
    const fixture = await setup(3, vi.fn(() => throwError(() => ({ error: { message: 'no route' } }))));
    fixture.componentInstance.updateQuantity(inputEvent(2), 1, 1, 5);

    const component = fixture.componentInstance as unknown as { driveOptionsError: () => string | null };
    expect(component.driveOptionsError()).toBe('no route');
  });

  it('selectDrive updates the selected drive key', async () => {
    const fixture = await setup(3);
    fixture.componentInstance.selectDrive('impulse_drive');
    const component = fixture.componentInstance as unknown as { selectedDriveKey: () => string | null };
    expect(component.selectedDriveKey()).toBe('impulse_drive');
  });

  it('formatEta formats seconds as m:ss', async () => {
    const fixture = await setup(3);
    expect(fixture.componentInstance.formatEta(65)).toBe('1:05');
  });

  it('send dispatches the espionage mission and shows confirmation', async () => {
    const fixture = await setup(3);
    fixture.componentInstance.updateQuantity(inputEvent(2), 1, 1, 5);
    fixture.componentInstance.selectDrive('impulse_drive');

    fixture.componentInstance.send(1, 1, 5);

    expect(dispatch).toHaveBeenCalledWith(
      expect.objectContaining({ originPlanetId: 1, missionType: 'ESPIONAGE', driveKey: 'impulse_drive' })
    );
    const component = fixture.componentInstance as unknown as { sentConfirmation: () => boolean; sending: () => boolean };
    expect(component.sentConfirmation()).toBe(true);
    expect(component.sending()).toBe(false);
  });

  it('does not send without a selected drive', async () => {
    const fixture = await setup(3, vi.fn(() => of([])));
    fixture.componentInstance.updateQuantity(inputEvent(2), 1, 1, 5);

    fixture.componentInstance.send(1, 1, 5);

    expect(dispatch).not.toHaveBeenCalled();
  });

  it('surfaces an error message when dispatch fails', async () => {
    dispatch = vi.fn(() => throwError(() => ({ error: { message: 'fleet busy' } })));
    await TestBed.configureTestingModule({
      imports: [
        EspionageComponent,
        TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
      ],
      providers: [
        provideRouter([]),
        { provide: UniverseApiService, useValue: { getShips: vi.fn(() => of([probeShip(3)])) } },
        { provide: FleetApiService, useValue: { driveOptions: vi.fn(() => of([driveOption('impulse_drive', 100)])), dispatch } }
      ]
    }).compileComponents();
    const fixture = TestBed.createComponent(EspionageComponent);
    fixture.componentRef.setInput('planetId', 1);
    fixture.detectChanges();

    fixture.componentInstance.updateQuantity(inputEvent(2), 1, 1, 5);
    fixture.componentInstance.selectDrive('impulse_drive');
    fixture.componentInstance.send(1, 1, 5);

    const component = fixture.componentInstance as unknown as { errorMessage: () => string | null };
    expect(component.errorMessage()).toBe('fleet busy');
  });
});
