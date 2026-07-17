import { TestBed } from '@angular/core/testing';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { of, throwError } from 'rxjs';

import { ResearchPlanetOption, TechnologyView } from '../../core/models';
import { ResearchApiService } from './research-api.service';
import { ResearchComponent } from './research.component';

function tech(overrides: Partial<TechnologyView> = {}): TechnologyView {
  return {
    key: 'laser_technology',
    name: 'Laser Technology',
    description: 'desc',
    driveScope: 'GALAXY',
    level: 1,
    nextLevelCost: { metal: 100, crystal: 50, deuterium: 0 },
    nextLevelResearchTimeSeconds: 120,
    researchActive: false,
    researchTargetLevel: null,
    researchEndsAt: null,
    unlocked: true,
    missingRequirements: [],
    ...overrides
  } as TechnologyView;
}

function planetOption(overrides: Partial<ResearchPlanetOption> = {}): ResearchPlanetOption {
  return {
    planetId: 1,
    name: 'Homeworld',
    coordinates: '[1:1:1]',
    researchEfficiency: 100,
    researchLabLevel: 5,
    active: false,
    ...overrides
  };
}

describe('ResearchComponent', () => {
  let start: ReturnType<typeof vi.fn>;
  let activate: ReturnType<typeof vi.fn>;

  async function setup(technologies: TechnologyView[], planetOptions: ResearchPlanetOption[]) {
    start = vi.fn(() => of({}));
    activate = vi.fn(() => of({}));

    await TestBed.configureTestingModule({
      imports: [
        ResearchComponent,
        TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
      ],
      providers: [
        {
          provide: ResearchApiService,
          useValue: { list: vi.fn(() => of(technologies)), listPlanetOptions: vi.fn(() => of(planetOptions)), start, activate }
        }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(ResearchComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('hasActiveResearch reflects whether any technology is researching', async () => {
    const fixture = await setup([tech({ researchActive: true })], []);
    expect(fixture.componentInstance.hasActiveResearch()).toBe(true);
  });

  it('hasActiveResearchPlanet reflects whether any research planet is active', async () => {
    const fixture = await setup([], [planetOption({ active: true })]);
    expect(fixture.componentInstance.hasActiveResearchPlanet()).toBe(true);
  });

  it('activate activates a planet and reloads on success', async () => {
    const fixture = await setup([], [planetOption()]);
    fixture.componentInstance.activate(planetOption());

    expect(activate).toHaveBeenCalledWith(1);
    const component = fixture.componentInstance as unknown as { activating: () => number | null };
    expect(component.activating()).toBeNull();
  });

  it('does not activate a planet with no research lab', async () => {
    const fixture = await setup([], [planetOption({ researchLabLevel: 0 })]);
    fixture.componentInstance.activate(planetOption({ researchLabLevel: 0 }));
    expect(activate).not.toHaveBeenCalled();
  });

  it('does not activate an already-active planet', async () => {
    const fixture = await setup([], [planetOption({ active: true })]);
    fixture.componentInstance.activate(planetOption({ active: true }));
    expect(activate).not.toHaveBeenCalled();
  });

  it('surfaces an error when activating fails', async () => {
    const failingActivate = vi.fn(() => throwError(() => ({ error: { message: 'no lab' } })));
    await TestBed.configureTestingModule({
      imports: [
        ResearchComponent,
        TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
      ],
      providers: [
        {
          provide: ResearchApiService,
          useValue: { list: vi.fn(() => of([])), listPlanetOptions: vi.fn(() => of([planetOption()])), start: vi.fn(), activate: failingActivate }
        }
      ]
    }).compileComponents();
    const fixture = TestBed.createComponent(ResearchComponent);
    fixture.detectChanges();

    fixture.componentInstance.activate(planetOption());

    const component = fixture.componentInstance as unknown as { errorMessage: () => string | null };
    expect(component.errorMessage()).toBe('no lab');
  });

  it('start begins researching a technology when a research planet is active', async () => {
    const fixture = await setup([tech()], [planetOption({ active: true })]);
    fixture.componentInstance.start(tech());

    expect(start).toHaveBeenCalledWith('laser_technology');
  });

  it('does not start research without an active research planet', async () => {
    const fixture = await setup([tech()], [planetOption({ active: false })]);
    fixture.componentInstance.start(tech());
    expect(start).not.toHaveBeenCalled();
  });

  it('surfaces an error when starting research fails', async () => {
    const failingStart = vi.fn(() => throwError(() => ({ error: { message: 'busy lab' } })));
    await TestBed.configureTestingModule({
      imports: [
        ResearchComponent,
        TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
      ],
      providers: [
        {
          provide: ResearchApiService,
          useValue: {
            list: vi.fn(() => of([tech()])),
            listPlanetOptions: vi.fn(() => of([planetOption({ active: true })])),
            start: failingStart,
            activate: vi.fn()
          }
        }
      ]
    }).compileComponents();
    const fixture = TestBed.createComponent(ResearchComponent);
    fixture.detectChanges();

    fixture.componentInstance.start(tech());

    const component = fixture.componentInstance as unknown as { errorMessage: () => string | null };
    expect(component.errorMessage()).toBe('busy lab');
  });

  it('remainingLabel formats an active research countdown', async () => {
    const endsAt = new Date(Date.now() + 65_000).toISOString();
    const fixture = await setup([tech({ researchEndsAt: endsAt })], []);
    expect(fixture.componentInstance.remainingLabel(tech({ researchEndsAt: endsAt }))).toBe('1:05');
  });

  it('remainingLabel returns an empty string when not researching', async () => {
    const fixture = await setup([tech()], []);
    expect(fixture.componentInstance.remainingLabel(tech({ researchEndsAt: null }))).toBe('');
  });

  it('remainingLabel returns Finishing… once the end time has passed', async () => {
    const fixture = await setup([tech()], []);
    expect(fixture.componentInstance.remainingLabel(tech({ researchEndsAt: new Date(Date.now() - 1000).toISOString() }))).toBe(
      'Finishing…'
    );
  });

  it('progress computes percentage complete for active research', async () => {
    const endsAt = new Date(Date.now() + 30_000).toISOString();
    const fixture = await setup([tech()], []);
    const result = fixture.componentInstance.progress(tech({ researchEndsAt: endsAt, nextLevelResearchTimeSeconds: 60 }));
    expect(result).toBeGreaterThanOrEqual(0);
    expect(result).toBeLessThanOrEqual(100);
  });
});
