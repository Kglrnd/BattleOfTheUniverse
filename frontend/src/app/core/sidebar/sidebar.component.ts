import { Component, DestroyRef, computed, effect, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { filter } from 'rxjs';

import { AuthService } from '../auth.service';
import { PlanetView } from '../models';
import { UniverseApiService } from '../../features/universe/universe-api.service';
import { MessagesApiService } from '../../features/messages/messages-api.service';

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css'
})
export class SidebarComponent {
  private readonly auth = inject(AuthService);
  private readonly api = inject(UniverseApiService);
  private readonly messagesApi = inject(MessagesApiService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly isAdmin = this.auth.isAdmin;
  protected readonly canAccessAdmin = this.auth.canAccessAdmin;
  protected readonly planets = signal<PlanetView[]>([]);
  protected readonly unreadCount = signal(0);

  private readonly currentUrl = signal(this.router.url);
  protected readonly isAdminArea = computed(() => this.currentUrl().startsWith('/admin'));

  protected readonly catalogTypes = [
    { type: 'buildings', label: 'Buildings' },
    { type: 'ships', label: 'Ships' },
    { type: 'technologies', label: 'Technologies' },
    { type: 'defenses', label: 'Defenses' }
  ];

  constructor() {
    effect(() => {
      if (this.auth.isAuthenticated()) {
        this.api.listPlanets().subscribe((planets) => this.planets.set(planets));
        this.refreshUnreadCount();
      } else {
        this.planets.set([]);
        this.unreadCount.set(0);
      }
    });

    const pollHandle = setInterval(() => {
      if (this.auth.isAuthenticated()) {
        this.refreshUnreadCount();
      }
    }, 10000);
    this.destroyRef.onDestroy(() => clearInterval(pollHandle));

    this.router.events.pipe(filter((event) => event instanceof NavigationEnd)).subscribe(() => {
      this.currentUrl.set(this.router.url);
    });
  }

  private refreshUnreadCount(): void {
    this.messagesApi.unreadCount().subscribe((result) => this.unreadCount.set(result.count));
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
