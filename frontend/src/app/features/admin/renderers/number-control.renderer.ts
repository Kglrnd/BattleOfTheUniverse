import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { JsonFormsControl } from '@jsonforms/angular';
import {
  isIntegerControl,
  isNumberControl,
  or,
  RankedTester,
  rankWith,
  StatePropsOfControl,
} from '@jsonforms/core';

@Component({
  selector: 'app-number-control-renderer',
  template: `
    <div class="field" [style.display]="hidden ? 'none' : ''">
      <label [for]="id">{{ label }}</label>
      <input
        class="input"
        type="number"
        [id]="id"
        [formControl]="form"
        [min]="min"
        [max]="max"
        [step]="step"
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
export class NumberControlRenderer extends JsonFormsControl {
  min: number | null = null;
  max: number | null = null;
  step: number | null = null;
  focused = false;

  override getEventValue = (event: Event) => {
    const value = (event.target as HTMLInputElement).value;
    if (value === '' || value === null) {
      return undefined;
    }
    const parsed = Number(value);
    return Number.isNaN(parsed) ? undefined : parsed;
  };

  override mapAdditionalProps(props: StatePropsOfControl): void {
    if (!this.scopedSchema) {
      return;
    }
    this.min = this.scopedSchema.minimum ?? null;
    this.max = this.scopedSchema.maximum ?? null;
    const testerContext = { rootSchema: this.rootSchema, config: props.config };
    const isInteger = isIntegerControl(this.uischema, this.rootSchema, testerContext);
    this.step = this.scopedSchema.multipleOf ?? (isInteger ? 1 : 0.1);
  }
}

export const NumberControlRendererTester: RankedTester = rankWith(
  2,
  or(isNumberControl, isIntegerControl)
);
