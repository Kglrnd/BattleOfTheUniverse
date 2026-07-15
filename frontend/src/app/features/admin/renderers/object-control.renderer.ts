import { ChangeDetectionStrategy, Component } from '@angular/core';
import { JsonFormsControlWithDetail, JsonFormsModule } from '@jsonforms/angular';
import {
  ControlWithDetailProps,
  findUISchema,
  Generate,
  GroupLayout,
  isObjectControl,
  RankedTester,
  rankWith,
  setReadonly,
  UISchemaElement,
} from '@jsonforms/core';

/** "baseCost" -> "Base Cost", used as a fallback group label for nested objects. */
function startCase(path: string): string {
  const withSpaces = path
    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
    .replace(/[_.]+/g, ' ')
    .trim();
  return withSpaces
    .split(' ')
    .filter(Boolean)
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}

@Component({
  selector: 'app-object-control-renderer',
  template: `
    <div class="card object-layout">
      <jsonforms-outlet [uischema]="detailUiSchema" [schema]="scopedSchema" [path]="propsPath" />
    </div>
  `,
  styles: [
    `
      .object-layout {
        padding: 1rem;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [JsonFormsModule],
})
export class ObjectControlRenderer extends JsonFormsControlWithDetail {
  detailUiSchema!: UISchemaElement;

  override mapAdditionalProps(props: ControlWithDetailProps): void {
    this.detailUiSchema = findUISchema(
      props.uischemas ?? [],
      props.schema,
      props.uischema.scope,
      props.path,
      () => {
        const newSchema = structuredClone(props.schema);
        delete newSchema.oneOf;
        delete newSchema.anyOf;
        delete newSchema.allOf;
        return Generate.uiSchema(newSchema, 'Group', undefined, this.rootSchema);
      },
      props.uischema,
      props.rootSchema
    );
    if (!props.path) {
      this.detailUiSchema.type = 'VerticalLayout';
    } else {
      (this.detailUiSchema as GroupLayout).label = startCase(props.path);
    }
    if (!this.isEnabled()) {
      setReadonly(this.detailUiSchema);
    }
  }
}

export const ObjectControlRendererTester: RankedTester = rankWith(2, isObjectControl);
