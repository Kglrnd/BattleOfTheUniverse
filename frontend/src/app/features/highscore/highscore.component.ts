import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth.service';
import { HighscoreEntry, HighscoreResponse, PlanetView } from '../../core/models';
import { UniverseApiService } from '../universe/universe-api.service';
import { HighscoreApiService } from './highscore-api.service';

@Component({
  selector: 'app-highscore',
  imports: [RouterLink],
  templateUrl: './highscore.component.html',
  styleUrl: './highscore.component.css'
})
export class HighscoreComponent {
  private readonly api = inject(HighscoreApiService);
  private readonly auth = inject(AuthService);
  private readonly universeApi = inject(UniverseApiService);

  protected readonly data = signal<HighscoreResponse | null>(null);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly currentUsername = this.auth.currentUser()?.username ?? null;

  protected readonly selectedUserId = signal<number | null>(null);
  protected readonly selectedPlanets = signal<PlanetView[] | null>(null);
  protected readonly planetsLoading = signal(false);

  constructor() {
    this.api.get().subscribe({
      next: (response) => {
        this.data.set(response);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message ?? 'Failed to load highscore.');
      }
    });
  }

  selectPlayer(entry: HighscoreEntry): void {
    if (this.selectedUserId() === entry.userId) {
      this.selectedUserId.set(null);
      this.selectedPlanets.set(null);
      return;
    }
    this.selectedUserId.set(entry.userId);
    this.selectedPlanets.set(null);
    this.planetsLoading.set(true);
    this.universeApi.byOwner(entry.userId).subscribe({
      next: (planets) => {
        this.planetsLoading.set(false);
        this.selectedPlanets.set(planets);
      },
      error: () => {
        this.planetsLoading.set(false);
        this.selectedPlanets.set([]);
      }
    });
  }
}
