import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { JsonFormsControl } from '@jsonforms/angular';
import {
  EnumOption,
  isEnumControl,
  JsonFormsState,
  mapStateToEnumControlProps,
  OwnPropsOfControl,
  OwnPropsOfEnum,
  RankedTester,
  rankWith,
  StatePropsOfControl,
} from '@jsonforms/core';

@Component({
  selector: 'app-enum-control-renderer',
  template: `
    <div class="field" [style.display]="hidden ? 'none' : ''">
      <label [for]="id">{{ label }}</label>
      <select class="select" [id]="id" [formControl]="form" (change)="onChange($event)">
        @for (option of options; track option.value) {
          <option [value]="option.value">{{ option.label }}</option>
        }
      </select>
      @if (error) {
        <span class="field-error">{{ error }}</span>
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, ReactiveFormsModule],
})
export class EnumControlRenderer extends JsonFormsControl {
  options: EnumOption[] = [];
  override getEventValue = (event: Event) => (event.target as HTMLSelectElement).value;

  protected override mapToProps(state: JsonFormsState): StatePropsOfControl & OwnPropsOfEnum {
    return mapStateToEnumControlProps(state, this.getOwnProps());
  }

  override mapAdditionalProps(props: StatePropsOfControl & OwnPropsOfEnum): void {
    this.options = props.options ?? [];
  }

  protected override getOwnProps(): OwnPropsOfControl & OwnPropsOfEnum {
    return { ...super.getOwnProps() };
  }
}

export const EnumControlRendererTester: RankedTester = rankWith(3, isEnumControl);
