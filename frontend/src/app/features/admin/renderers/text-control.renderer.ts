import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { JsonFormsControl } from '@jsonforms/angular';
import { isStringControl, RankedTester, rankWith } from '@jsonforms/core';

@Component({
  selector: 'TextControlRenderer',
  template: `
    <div class="field" [style.display]="hidden ? 'none' : ''">
      <label [for]="id">{{ label }}</label>
      <input
        class="input"
        [type]="getType()"
        [id]="id"
        [formControl]="form"
        (input)="onChange($event)"
        (focus)="focused = true"
        (focusout)="focused = false"
      />
      @if (shouldShowUnfocusedDescription() || focused) {
        <span class="field-hint">{{ description }}</span>
      }
      @if (error) {
        <span class="field-error">{{ error }}</span>
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, ReactiveFormsModule],
})
export class TextControlRenderer extends JsonFormsControl {
  focused = false;
  override getEventValue = (event: any) => event.target.value || undefined;
  getType = (): string => {
    if (this.uischema.options && this.uischema.options['format']) {
      return this.uischema.options['format'];
    }
    if (this.scopedSchema && this.scopedSchema.format) {
      switch (this.scopedSchema.format) {
        case 'email':
          return 'email';
        case 'tel':
          return 'tel';
        default:
          return 'text';
      }
    }
    return 'text';
  };
}

export const TextControlRendererTester: RankedTester = rankWith(1, isStringControl);
