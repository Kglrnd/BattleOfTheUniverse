import { Component, DestroyRef, effect, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';

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
  protected readonly planets = signal<PlanetView[]>([]);
  protected readonly unreadCount = signal(0);
  protected readonly adminExpanded = signal(true);

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
  }

  private refreshUnreadCount(): void {
    this.messagesApi.unreadCount().subscribe((result) => this.unreadCount.set(result.count));
  }

  toggleAdmin(): void {
    this.adminExpanded.update((v) => !v);
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
