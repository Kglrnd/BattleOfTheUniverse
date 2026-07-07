import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink, RouterLinkActive } from '@angular/router';
import { JsonFormsModule } from '@jsonforms/angular';
import { JsonSchema } from '@jsonforms/core';

import { AdminCatalogApiService } from './admin-catalog-api.service';
import { catalogEditorRenderers } from './renderers';

@Component({
  selector: 'app-catalog-editor',
  imports: [RouterLink, RouterLinkActive, JsonFormsModule],
  templateUrl: './catalog-editor.component.html',
  styleUrl: './catalog-editor.component.css'
})
export class CatalogEditorComponent {
  private readonly api = inject(AdminCatalogApiService);
  private readonly route = inject(ActivatedRoute);

  protected readonly renderers = catalogEditorRenderers;
  protected readonly type = signal('buildings');
  protected readonly schema = signal<JsonSchema | undefined>(undefined);
  protected readonly data = signal<unknown>(undefined);
  protected readonly savedMessage = signal<string | null>(null);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly saving = signal(false);

  constructor() {
    this.route.paramMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      const type = params.get('type') ?? 'buildings';
      this.type.set(type);
      this.load(type);
    });
  }

  private load(type: string): void {
    this.schema.set(undefined);
    this.data.set(undefined);
    this.api.getSchema(type).subscribe((schema) => this.schema.set(schema as JsonSchema));
    this.api.getData(type).subscribe((data) => this.data.set(data));
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
        this.savedMessage.set('Saved.');
      },
      error: (err) => {
        this.saving.set(false);
        this.errorMessage.set(err.error?.message ?? 'Save failed.');
      }
    });
  }
}
