import { Component, DestroyRef, effect, inject, input, signal } from '@angular/core';

import { ResourceView } from '../../core/models';
import { UniverseApiService } from './universe-api.service';

@Component({
  selector: 'app-resource-bar',
  imports: [],
  templateUrl: './resource-bar.component.html',
  styleUrl: './resource-bar.component.css'
})
export class ResourceBarComponent {
  private readonly api = inject(UniverseApiService);
  private readonly destroyRef = inject(DestroyRef);

  readonly planetId = input.required<number>();

  protected readonly resources = signal<ResourceView[]>([]);

  constructor() {
    effect(() => {
      this.planetId();
      this.refresh();
    });

    const pollHandle = setInterval(() => this.refresh(), 5000);
    this.destroyRef.onDestroy(() => clearInterval(pollHandle));
  }

  private refresh(): void {
    this.api.getResources(this.planetId()).subscribe((resources) => this.resources.set(resources));
  }
}
