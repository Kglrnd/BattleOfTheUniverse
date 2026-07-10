import { Component, inject, signal } from '@angular/core';

import { AuthService } from '../../core/auth.service';
import { HighscoreResponse } from '../../core/models';
import { HighscoreApiService } from './highscore-api.service';

@Component({
  selector: 'app-highscore',
  imports: [],
  templateUrl: './highscore.component.html',
  styleUrl: './highscore.component.css'
})
export class HighscoreComponent {
  private readonly api = inject(HighscoreApiService);
  private readonly auth = inject(AuthService);

  protected readonly data = signal<HighscoreResponse | null>(null);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly currentUsername = this.auth.currentUser()?.username ?? null;

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
}
