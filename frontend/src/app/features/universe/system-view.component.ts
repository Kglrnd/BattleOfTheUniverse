import { Component, DestroyRef, inject, signal } from '@angular/core';
import { Menu, MenuContent, MenuItem, MenuTrigger } from '@angular/aria/menu';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslocoDirective, TranslocoService } from '@jsverse/transloco';

import { CurrentPlanetService } from '../../core/current-planet.service';
import { GameAssetPipe } from '../../core/game-asset.pipe';
import { ImgFallbackDirective } from '../../core/img-fallback.directive';
import { PlanetView, SystemSlotView, SystemView } from '../../core/models';
import { UniverseApiService } from './universe-api.service';

@Component({
  selector: 'app-system-view',
  imports: [RouterLink, Menu, MenuContent, MenuItem, MenuTrigger, TranslocoDirective, GameAssetPipe, ImgFallbackDirective],
  templateUrl: './system-view.component.html',
  styleUrl: './system-view.component.css'
})
export class SystemViewComponent {
  private readonly api = inject(UniverseApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly currentPlanet = inject(CurrentPlanetService);
  private readonly transloco = inject(TranslocoService);

  private static readonly MIN_SYSTEM = 1;
  private static readonly MAX_SYSTEM = 100;
  private static readonly MIN_GALAXY = 1;
  private static readonly MAX_GALAXY = 5;

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

  /** Wraps into the previous galaxy's last system once system 1 is passed, so browsing is continuous. */
  previousSystem(): void {
    const current = this.system();
    if (!current) {
      return;
    }
    let { galaxy, system } = current;
    system -= 1;
    if (system < SystemViewComponent.MIN_SYSTEM) {
      system = SystemViewComponent.MAX_SYSTEM;
      galaxy = this.previousGalaxyNumber(galaxy);
    }
    this.goTo(galaxy, system);
  }

  /** Wraps into the next galaxy's first system once system 100 is passed, so browsing is continuous. */
  nextSystem(): void {
    const current = this.system();
    if (!current) {
      return;
    }
    let { galaxy, system } = current;
    system += 1;
    if (system > SystemViewComponent.MAX_SYSTEM) {
      system = SystemViewComponent.MIN_SYSTEM;
      galaxy = this.nextGalaxyNumber(galaxy);
    }
    this.goTo(galaxy, system);
  }

  previousGalaxy(): void {
    const current = this.system();
    if (!current) {
      return;
    }
    this.goTo(this.previousGalaxyNumber(current.galaxy), current.system);
  }

  nextGalaxy(): void {
    const current = this.system();
    if (!current) {
      return;
    }
    this.goTo(this.nextGalaxyNumber(current.galaxy), current.system);
  }

  slotClass(status: string): string {
    return 'slot-' + status.toLowerCase();
  }

  isMine(planet: PlanetView): boolean {
    return this.currentPlanet.planets().some((p) => p.id === planet.id);
  }

  actionable(slot: SystemSlotView): boolean {
    return slot.status === 'FREE' || (slot.status === 'OCCUPIED' && !!slot.planet && !this.isMine(slot.planet));
  }

  onSlotMenuAction(action: string, galaxy: number, system: number, position: number): void {
    if (action === 'colonize') {
      this.colonize(galaxy, system, position);
    } else if (action === 'attack') {
      this.attack(galaxy, system, position);
    } else if (action === 'bombard') {
      this.dispatchTo('BOMBARD', galaxy, system, position);
    } else if (action === 'invade') {
      this.dispatchTo('INVADE', galaxy, system, position);
    }
  }

  attack(galaxy: number, system: number, position: number): void {
    this.dispatchTo('ATTACK', galaxy, system, position);
  }

  colonize(galaxy: number, system: number, position: number): void {
    this.dispatchTo('COLONIZE', galaxy, system, position);
  }

  private dispatchTo(mission: string, galaxy: number, system: number, position: number): void {
    this.router.navigate(['/fleet'], {
      queryParams: { mission, targetGalaxy: galaxy, targetSystem: system, targetPosition: position }
    });
  }

  private previousGalaxyNumber(galaxy: number): number {
    return galaxy <= SystemViewComponent.MIN_GALAXY ? SystemViewComponent.MAX_GALAXY : galaxy - 1;
  }

  private nextGalaxyNumber(galaxy: number): number {
    return galaxy >= SystemViewComponent.MAX_GALAXY ? SystemViewComponent.MIN_GALAXY : galaxy + 1;
  }

  private loadHomeSystem(): void {
    this.api.getHomePlanet().subscribe({
      next: (planet) => this.goTo(planet.galaxy, planet.system),
      error: () => {
        this.loading.set(false);
        this.errorMessage.set(this.transloco.translate('universe.systemView.couldNotDetermineStartingSystem'));
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
        this.errorMessage.set(err.error?.message ?? this.transloco.translate('universe.systemView.systemNotFound'));
      }
    });
  }
}
