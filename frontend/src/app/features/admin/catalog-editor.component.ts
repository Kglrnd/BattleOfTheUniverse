import { Component, computed, effect, inject, signal } from '@angular/core';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink, RouterLinkActive } from '@angular/router';
import { JsonFormsModule } from '@jsonforms/angular';
import { JsonSchema } from '@jsonforms/core';
import { TranslocoDirective, TranslocoService } from '@jsverse/transloco';
import { map } from 'rxjs';

import { AdminCatalogApiService } from './admin-catalog-api.service';
import { catalogEditorRenderers } from './renderers';

@Component({
  selector: 'app-catalog-editor',
  imports: [RouterLink, RouterLinkActive, JsonFormsModule, TranslocoDirective],
  templateUrl: './catalog-editor.component.html',
  styleUrl: './catalog-editor.component.css'
})
export class CatalogEditorComponent {
  private readonly api = inject(AdminCatalogApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly transloco = inject(TranslocoService);

  protected readonly renderers = catalogEditorRenderers;
  protected readonly type = toSignal(this.route.paramMap.pipe(map((params) => params.get('type') ?? 'buildings')), {
    requireSync: true
  });

  private readonly schemaResource = rxResource({
    params: () => ({ type: this.type() }),
    stream: ({ params }) => this.api.getSchema(params.type)
  });
  // Gate on isLoading (rather than just .value()) so switching catalog type blanks the
  // form immediately instead of briefly rendering the previous type's stale schema/data.
  protected readonly schema = computed(() =>
    this.schemaResource.isLoading() ? undefined : (this.schemaResource.value() as JsonSchema | undefined)
  );

  private readonly dataResource = rxResource({
    params: () => ({ type: this.type() }),
    stream: ({ params }) => this.api.getData(params.type)
  });
  protected readonly data = signal<unknown>(undefined);

  protected readonly savedMessage = signal<string | null>(null);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly saving = signal(false);

  constructor() {
    effect(() => {
      if (!this.dataResource.isLoading()) {
        this.data.set(this.dataResource.value());
      }
    });
  }

  onDataChange(data: unknown): void {
    this.data.set(data);
  }

  save(): void {
    this.saving.set(true);
    this.savedMessage.set(null);
    this.errorMessage.set(null);
    this.api.save(this.type(), this.data()).subscribe({
      next: (saved) => {
        this.data.set(saved);
        this.saving.set(false);
        this.savedMessage.set(this.transloco.translate('admin.catalog.saved'));
      },
      error: (err) => {
        this.saving.set(false);
        this.errorMessage.set(err.error?.message ?? this.transloco.translate('admin.catalog.saveFailed'));
      }
    });
  }
}
