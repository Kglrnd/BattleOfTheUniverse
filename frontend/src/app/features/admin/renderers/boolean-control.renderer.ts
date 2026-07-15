import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { JsonFormsControl } from '@jsonforms/angular';
import { isBooleanControl, RankedTester, rankWith } from '@jsonforms/core';

@Component({
  selector: 'app-boolean-control-renderer',
  template: `
    <div class="checkbox-field" [style.display]="hidden ? 'none' : ''">
      <input
        type="checkbox"
        [id]="id"
        [checked]="isChecked()"
        [disabled]="!isEnabled()"
        (change)="onChange($event)"
      />
      <label [for]="id">{{ label }}</label>
      @if (error) {
        <span class="field-error">{{ error }}</span>
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
})
export class BooleanControlRenderer extends JsonFormsControl {
  isChecked = () => !!this.data;
  override getEventValue = (event: Event) => (event.target as HTMLInputElement).checked;
}

export const BooleanControlRendererTester: RankedTester = rankWith(1, isBooleanControl);
