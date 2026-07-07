import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { JsonFormsAbstractControl, JsonFormsModule } from '@jsonforms/angular';
import {
  arrayDefaultTranslations,
  ArrayLayoutProps,
  ArrayTranslations,
  createDefaultValue,
  defaultJsonFormsI18nState,
  findUISchema,
  getArrayTranslations,
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

@Component({
  selector: 'ArrayLayoutRenderer',
  template: `
    <div class="array-layout" [style.display]="hidden ? 'none' : ''">
      <div class="array-layout-toolbar">
        <h3 class="array-layout-title">{{ label }}</h3>
        @if (error) {
          <span class="field-error">{{ error }}</span>
        }
        <button type="button" class="btn" [disabled]="!isEnabled()" (click)="add()">
          + {{ translations.addTooltip }}
        </button>
      </div>
      @if (noData) {
        <p class="field-hint">{{ translations.noDataMessage }}</p>
      }
      @for (idx of items(); track idx; let first = $first; let last = $last) {
        <div class="card array-item">
          <jsonforms-outlet [renderProps]="getProps(idx)" />
          @if (isEnabled()) {
            <div class="card-actions">
              @if (uischema?.options?.['showSortButtons']) {
                <button type="button" class="btn" [disabled]="first" (click)="up(idx)">Up</button>
                <button type="button" class="btn" [disabled]="last" (click)="down(idx)">Down</button>
              }
              <button type="button" class="btn" (click)="remove(idx)">
                {{ translations.removeTooltip }}
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
  imports: [CommonModule, JsonFormsModule],
})
export class ArrayLayoutRenderer
  extends JsonFormsAbstractControl<StatePropsOfArrayLayout>
  implements OnInit
{
  noData = false;
  translations: ArrayTranslations = {};
  uischemas: { tester: UISchemaTester; uischema: UISchemaElement }[] = [];

  private addItemFn!: (path: string, value: any) => () => void;
  private moveItemUpFn: ((path: string, index: number) => () => void) | undefined;
  private moveItemDownFn: ((path: string, index: number) => () => void) | undefined;
  private removeItemsFn: ((path: string, toDelete: number[]) => () => void) | undefined;

  mapToProps(state: JsonFormsState): StatePropsOfArrayLayout & { translations: ArrayTranslations } {
    const props = mapStateToArrayLayoutProps(state, this.getOwnProps());
    const t = state.jsonforms.i18n?.translate ?? defaultJsonFormsI18nState.translate;
    const translations = getArrayTranslations(t, arrayDefaultTranslations, props.i18nKeyPrefix ?? '', props.label);
    return { ...props, translations };
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

  override mapAdditionalProps(props: ArrayLayoutProps & { translations: ArrayTranslations }): void {
    this.noData = !props.data || props.data === 0;
    this.uischemas = props.uischemas ?? [];
    this.translations = props.translations;
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
