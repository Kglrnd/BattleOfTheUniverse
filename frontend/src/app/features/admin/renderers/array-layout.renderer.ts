import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { JsonFormsAbstractControl, JsonFormsModule } from '@jsonforms/angular';
import {
  ArrayLayoutProps,
  createDefaultValue,
  findUISchema,
  isObjectArrayWithNesting,
  JsonFormsState,
  mapDispatchToArrayControlProps,
  mapStateToArrayLayoutProps,
  OwnPropsOfRenderer,
  Paths,
  RankedTester,
  rankWith,
  setReadonly,
  StatePropsOfArrayLayout,
  UISchemaElement,
  UISchemaTester,
  unsetReadonly,
} from '@jsonforms/core';
import { TranslocoDirective } from '@jsverse/transloco';

@Component({
  selector: 'ArrayLayoutRenderer',
  template: `
    <div class="array-layout" [style.display]="hidden ? 'none' : ''" *transloco="let t; prefix: 'admin.catalog'">
      <div class="array-layout-toolbar">
        <h3 class="array-layout-title">{{ label }}</h3>
        @if (error) {
          <span class="field-error">{{ error }}</span>
        }
        <button type="button" class="btn" [disabled]="!isEnabled()" (click)="add()">
          + {{ t('addItem') }}
        </button>
      </div>
      @if (noData) {
        <p class="field-hint">{{ t('noItems') }}</p>
      }
      @for (idx of items(); track idx; let first = $first; let last = $last) {
        <div class="card array-item">
          <jsonforms-outlet [renderProps]="getProps(idx)" />
          @if (isEnabled()) {
            <div class="card-actions">
              @if (uischema?.options?.['showSortButtons']) {
                <button type="button" class="btn" [disabled]="first" (click)="up(idx)">{{ t('moveUp') }}</button>
                <button type="button" class="btn" [disabled]="last" (click)="down(idx)">{{ t('moveDown') }}</button>
              }
              <button type="button" class="btn" (click)="remove(idx)">
                {{ t('removeItem') }}
              </button>
            </div>
          }
        </div>
      }
    </div>
  `,
  styles: [
    `
      .array-layout {
        display: flex;
        flex-direction: column;
        gap: 1rem;
      }
      .array-layout-toolbar {
        display: flex;
        align-items: center;
        gap: 1rem;
      }
      .array-layout-title {
        margin: 0;
        flex: 1 1 auto;
      }
      .array-item {
        display: flex;
        flex-direction: column;
        gap: 0.75rem;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, JsonFormsModule, TranslocoDirective],
})
export class ArrayLayoutRenderer
  extends JsonFormsAbstractControl<StatePropsOfArrayLayout>
  implements OnInit
{
  noData = false;
  uischemas: { tester: UISchemaTester; uischema: UISchemaElement }[] = [];

  private addItemFn!: (path: string, value: any) => () => void;
  private moveItemUpFn: ((path: string, index: number) => () => void) | undefined;
  private moveItemDownFn: ((path: string, index: number) => () => void) | undefined;
  private removeItemsFn: ((path: string, toDelete: number[]) => () => void) | undefined;

  mapToProps(state: JsonFormsState): StatePropsOfArrayLayout {
    return mapStateToArrayLayoutProps(state, this.getOwnProps());
  }

  items(): number[] {
    const count = typeof this.data === 'number' ? this.data : 0;
    return Array.from({ length: count }, (_, i) => i);
  }

  remove(index: number): void {
    this.removeItemsFn?.(this.propsPath, [index])();
  }

  add(): void {
    this.addItemFn(this.propsPath, createDefaultValue(this.scopedSchema, this.rootSchema))();
  }

  up(index: number): void {
    this.moveItemUpFn?.(this.propsPath, index)();
  }

  down(index: number): void {
    this.moveItemDownFn?.(this.propsPath, index)();
  }

  override ngOnInit(): void {
    super.ngOnInit();
    const { addItem, removeItems, moveUp, moveDown } = mapDispatchToArrayControlProps(
      this.jsonFormsService.updateCore.bind(this.jsonFormsService)
    );
    this.addItemFn = addItem;
    this.moveItemUpFn = moveUp;
    this.moveItemDownFn = moveDown;
    this.removeItemsFn = removeItems;
  }

  override mapAdditionalProps(props: ArrayLayoutProps): void {
    this.noData = !props.data || props.data === 0;
    this.uischemas = props.uischemas ?? [];
  }

  getProps(index: number): OwnPropsOfRenderer {
    const uischema = findUISchema(
      this.uischemas,
      this.scopedSchema,
      this.uischema.scope,
      this.propsPath,
      undefined,
      this.uischema,
      this.rootSchema
    );
    if (this.isEnabled()) {
      unsetReadonly(uischema);
    } else {
      setReadonly(uischema);
    }
    return {
      schema: this.scopedSchema,
      path: Paths.compose(this.propsPath, `${index}`),
      uischema,
    };
  }
}

export const ArrayLayoutRendererTester: RankedTester = rankWith(4, isObjectArrayWithNesting);
