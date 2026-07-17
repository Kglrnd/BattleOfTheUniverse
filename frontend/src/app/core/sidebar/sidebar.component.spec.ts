import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { Subject, of } from 'rxjs';

import { MessagesApiService } from '../../features/messages/messages-api.service';
import { AuthService } from '../auth.service';
import { CurrentPlanetService } from '../current-planet.service';
import { MobileNavService } from '../mobile-nav.service';
import { PlanetView } from '../models';
import { SidebarComponent } from './sidebar.component';

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

describe('SidebarComponent', () => {
  let events$: Subject<unknown>;
  let navigate: ReturnType<typeof vi.fn>;
  let mobileNav: MobileNavService;
  let isAuthenticated: boolean;
  let unreadCount: ReturnType<typeof vi.fn>;
  let routerStub: { events: unknown; url: string; navigate: ReturnType<typeof vi.fn> };

  async function setup(authenticated = true) {
    events$ = new Subject();
    navigate = vi.fn();
    isAuthenticated = authenticated;
    unreadCount = vi.fn(() => of({ count: 3 }));
    routerStub = { events: events$.asObservable(), url: '/universe', navigate };

    await TestBed.configureTestingModule({
      imports: [
        SidebarComponent,
        TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
      ],
      providers: [
        { provide: AuthService, useValue: { isAdmin: () => false, canAccessAdmin: () => false, isAuthenticated: () => isAuthenticated } },
        { provide: CurrentPlanetService, useValue: { planets: () => [planet(1), planet(2)] } },
        { provide: MessagesApiService, useValue: { unreadCount } },
        { provide: Router, useValue: routerStub },
        { provide: ActivatedRoute, useValue: {} }
      ]
    }).compileComponents();

    mobileNav = TestBed.inject(MobileNavService);
    const fixture = TestBed.createComponent(SidebarComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('reflects the mobile nav open state via the host "open" class', async () => {
    const fixture = await setup();
    expect(fixture.nativeElement.classList.contains('open')).toBe(false);

    mobileNav.toggle();
    fixture.detectChanges();

    expect(fixture.nativeElement.classList.contains('open')).toBe(true);
  });

  it('computes unread count from the messages resource', async () => {
    const fixture = await setup();
    const component = fixture.componentInstance as unknown as { unreadCount: () => number };
    expect(component.unreadCount()).toBe(3);
  });

  it('does not fetch unread count when unauthenticated', async () => {
    await setup(false);
    expect(unreadCount).not.toHaveBeenCalled();
  });

  it('tracks the admin area based on router navigation events', async () => {
    const fixture = await setup();
    const component = fixture.componentInstance as unknown as { isAdminArea: () => boolean };
    expect(component.isAdminArea()).toBe(false);

    routerStub.url = '/admin/users';
    events$.next(new NavigationEnd(1, '/admin/users', '/admin/users'));
    fixture.detectChanges();

    expect(component.isAdminArea()).toBe(true);
  });

  it('goToPlanet navigates to the selected planet and resets the select', async () => {
    const fixture = await setup();
    const select = document.createElement('select');
    // A real empty <option value=""> lets us tell "reset to the empty option" (selectedIndex 0)
    // apart from "value assigned to something no option matches" (selectedIndex -1) - both read
    // back as select.value === '', so asserting on .value alone can't distinguish them.
    const emptyOption = document.createElement('option');
    emptyOption.value = '';
    select.appendChild(emptyOption);
    const option1 = document.createElement('option');
    option1.value = '5';
    select.appendChild(option1);
    select.value = '5';

    fixture.componentInstance.goToPlanet({ target: select } as unknown as Event);

    expect(navigate).toHaveBeenCalledWith(['/universe', 5]);
    expect(select.value).toBe('');
    expect(select.selectedIndex).toBe(0);
  });

  it('goToPlanet does nothing when no planet id is selected', async () => {
    const fixture = await setup();
    const select = document.createElement('select');
    navigate.mockClear();

    fixture.componentInstance.goToPlanet({ target: select } as unknown as Event);

    expect(navigate).not.toHaveBeenCalled();
  });

  it('exposes the exact list of admin catalog types and labels', async () => {
    const fixture = await setup();
    const component = fixture.componentInstance as unknown as { catalogTypes: { type: string; labelKey: string }[] };
    expect(component.catalogTypes).toEqual([
      { type: 'buildings', labelKey: 'catalogBuildings' },
      { type: 'ships', labelKey: 'catalogShips' },
      { type: 'technologies', labelKey: 'catalogTechnologies' },
      { type: 'defenses', labelKey: 'catalogDefenses' }
    ]);
  });

  it('polls for unread messages every 10 seconds until destroyed', async () => {
    vi.useFakeTimers();
    try {
      const fixture = await setup();
      unreadCount.mockClear();

      vi.advanceTimersByTime(10000);
      TestBed.flushEffects();
      expect(unreadCount).toHaveBeenCalledTimes(1);

      fixture.destroy();
      unreadCount.mockClear();
      vi.advanceTimersByTime(30000);
      TestBed.flushEffects();
      expect(unreadCount).not.toHaveBeenCalled();
    } finally {
      vi.useRealTimers();
    }
  });
});
