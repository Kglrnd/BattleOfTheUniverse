import { Component, effect, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';

import { AuthService } from '../auth.service';
import { PlanetView } from '../models';
import { UniverseApiService } from '../../features/universe/universe-api.service';

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css'
})
export class SidebarComponent {
  private readonly auth = inject(AuthService);
  private readonly api = inject(UniverseApiService);
  private readonly router = inject(Router);

  protected readonly isAdmin = this.auth.isAdmin;
  protected readonly planets = signal<PlanetView[]>([]);
  protected readonly universeExpanded = signal(true);
  protected readonly adminExpanded = signal(true);

  protected readonly catalogTypes = [
    { type: 'buildings', label: 'Buildings' },
    { type: 'ships', label: 'Ships' },
    { type: 'technologies', label: 'Technologies' }
  ];

  constructor() {
    effect(() => {
      if (this.auth.isAuthenticated()) {
        this.api.listPlanets().subscribe((planets) => this.planets.set(planets));
      } else {
        this.planets.set([]);
      }
    });
  }

  toggleUniverse(): void {
    this.universeExpanded.update((v) => !v);
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
