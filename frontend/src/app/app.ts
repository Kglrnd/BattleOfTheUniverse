import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';

import { AuthService } from './core/auth.service';
import { SidebarComponent } from './core/sidebar/sidebar.component';
import { ResourceBarComponent } from './features/universe/resource-bar.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, SidebarComponent, ResourceBarComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  private readonly currentUrl = toSignal(
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd),
      map(() => this.router.url)
    ),
    { initialValue: this.router.url }
  );
  protected readonly isAdminArea = computed(() => this.currentUrl().startsWith('/admin'));

  logout(): void {
    this.auth.logout().subscribe(() => this.router.navigate(['/login']));
  }
}
