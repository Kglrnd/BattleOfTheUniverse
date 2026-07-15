import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';

import { VersionService } from '../version.service';

@Component({
  selector: 'app-footer',
  imports: [],
  templateUrl: './app-footer.component.html',
  styleUrl: './app-footer.component.css'
})
export class AppFooterComponent {
  private readonly versionService = inject(VersionService);

  protected readonly version = toSignal(this.versionService.getVersion(), { initialValue: null });
}
