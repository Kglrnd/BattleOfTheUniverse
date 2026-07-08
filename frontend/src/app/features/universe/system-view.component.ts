import { Component, DestroyRef, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { SystemView } from '../../core/models';
import { UniverseApiService } from './universe-api.service';

@Component({
  selector: 'app-system-view',
  imports: [RouterLink],
  templateUrl: './system-view.component.html',
  styleUrl: './system-view.component.css'
})
export class SystemViewComponent {
  private readonly api = inject(UniverseApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  private static readonly MIN_SYSTEM = 1;
  private static readonly MAX_SYSTEM = 100;

  protected readonly system = signal<SystemView | null>(null);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);

  constructor() {
    const subscription = this.route.paramMap.subscribe((params) => {
      const galaxy = Number(params.get('galaxy'));
      const system = Number(params.get('system'));
      if (galaxy && system) {
        this.load(galaxy, system);
      } else {
        this.loadHomeSystem();
      }
    });
    this.destroyRef.onDestroy(() => subscription.unsubscribe());
  }

  goTo(galaxy: number, system: number): void {
    this.router.navigate(['/universe/system', galaxy, system]);
  }

  jump(galaxyInput: HTMLInputElement, systemInput: HTMLInputElement): void {
    const galaxy = galaxyInput.valueAsNumber;
    const system = systemInput.valueAsNumber;
    if (!galaxy || !system) {
      return;
    }
    this.goTo(galaxy, system);
  }

  previousSystem(): void {
    const current = this.system();
    if (!current || current.system <= SystemViewComponent.MIN_SYSTEM) {
      return;
    }
    this.goTo(current.galaxy, current.system - 1);
  }

  nextSystem(): void {
    const current = this.system();
    if (!current || current.system >= SystemViewComponent.MAX_SYSTEM) {
      return;
    }
    this.goTo(current.galaxy, current.system + 1);
  }

  slotClass(status: string): string {
    return 'slot-' + status.toLowerCase();
  }

  private loadHomeSystem(): void {
    this.api.getHomePlanet().subscribe({
      next: (planet) => this.goTo(planet.galaxy, planet.system),
      error: () => {
        this.loading.set(false);
        this.errorMessage.set('Could not determine a starting system.');
      }
    });
  }

  private load(galaxy: number, system: number): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.api.getSystem(galaxy, system).subscribe({
      next: (view) => {
        this.system.set(view);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message ?? 'System not found.');
      }
    });
  }
}
