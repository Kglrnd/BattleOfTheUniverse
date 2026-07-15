import { Component, DestroyRef, computed, inject } from '@angular/core';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { TranslocoDirective } from '@jsverse/transloco';
import { filter, map } from 'rxjs';

import { AuthService } from '../auth.service';
import { CurrentPlanetService } from '../current-planet.service';
import { MessagesApiService } from '../../features/messages/messages-api.service';

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive, TranslocoDirective],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css'
})
export class SidebarComponent {
  private readonly auth = inject(AuthService);
  private readonly currentPlanet = inject(CurrentPlanetService);
  private readonly messagesApi = inject(MessagesApiService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly isAdmin = this.auth.isAdmin;
  protected readonly canAccessAdmin = this.auth.canAccessAdmin;
  protected readonly planets = this.currentPlanet.planets;

  private readonly unreadCountResource = rxResource({
    params: () => (this.auth.isAuthenticated() ? {} : undefined),
    stream: () => this.messagesApi.unreadCount()
  });
  protected readonly unreadCount = computed(() => this.unreadCountResource.value()?.count ?? 0);

  private readonly currentUrl = toSignal(
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd),
      map(() => this.router.url)
    ),
    { initialValue: this.router.url }
  );
  protected readonly isAdminArea = computed(() => this.currentUrl().startsWith('/admin'));

  protected readonly catalogTypes = [
    { type: 'buildings', labelKey: 'catalogBuildings' },
    { type: 'ships', labelKey: 'catalogShips' },
    { type: 'technologies', labelKey: 'catalogTechnologies' },
    { type: 'defenses', labelKey: 'catalogDefenses' }
  ];

  constructor() {
    const pollHandle = setInterval(() => this.unreadCountResource.reload(), 10000);
    this.destroyRef.onDestroy(() => clearInterval(pollHandle));
  }

  goToPlanet(event: Event): void {
    const select = event.target as HTMLSelectElement;
    const id = Number(select.value);
    select.value = '';
    if (id) {
      this.router.navigate(['/universe', id]);
    }
  }
}
