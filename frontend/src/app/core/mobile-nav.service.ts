import { Injectable, signal } from '@angular/core';

/** Whether the off-canvas sidebar drawer is open on narrow (mobile) viewports; irrelevant above the sidebar's collapse breakpoint. */
@Injectable({ providedIn: 'root' })
export class MobileNavService {
  private readonly openSignal = signal(false);
  readonly isOpen = this.openSignal.asReadonly();

  toggle(): void {
    this.openSignal.update((open) => !open);
  }

  close(): void {
    this.openSignal.set(false);
  }
}
