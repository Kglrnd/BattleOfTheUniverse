import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { TranslocoDirective, TranslocoService } from '@jsverse/transloco';

import { AuthService } from '../../core/auth.service';
import { HighscoreEntry } from '../../core/models';
import { UniverseApiService } from '../universe/universe-api.service';
import { HighscoreApiService } from './highscore-api.service';

@Component({
  selector: 'app-highscore',
  imports: [RouterLink, TranslocoDirective],
  templateUrl: './highscore.component.html',
  styleUrl: './highscore.component.css'
})
export class HighscoreComponent {
  private readonly api = inject(HighscoreApiService);
  private readonly auth = inject(AuthService);
  private readonly universeApi = inject(UniverseApiService);
  private readonly transloco = inject(TranslocoService);

  private readonly highscoreResource = rxResource({ stream: () => this.api.get() });
  protected readonly loading = this.highscoreResource.isLoading;
  protected readonly errorMessage = computed(() => {
    const error = this.highscoreResource.error() as HttpErrorResponse | undefined;
    return error ? (error.error?.message ?? this.transloco.translate('highscore.failedToLoad')) : null;
  });
  // Reading .value() on an errored resource with no prior successful value throws, so this
  // must check for an error first rather than let the template call .value() unconditionally.
  protected readonly data = computed(() => (this.errorMessage() ? null : (this.highscoreResource.value() ?? null)));

  protected readonly currentUsername = this.auth.currentUser()?.username ?? null;

  protected readonly selectedUserId = signal<number | null>(null);
  private readonly selectedPlanetsResource = rxResource({
    params: () => {
      const userId = this.selectedUserId();
      return userId === null ? undefined : { userId };
    },
    stream: ({ params }) => this.universeApi.byOwner(params.userId)
  });
  protected readonly selectedPlanets = computed(() =>
    this.selectedUserId() === null ? null : (this.selectedPlanetsResource.value() ?? [])
  );
  protected readonly planetsLoading = this.selectedPlanetsResource.isLoading;

  selectPlayer(entry: HighscoreEntry): void {
    this.selectedUserId.set(this.selectedUserId() === entry.userId ? null : entry.userId);
  }
}
