import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { JsonFormsModule } from '@jsonforms/angular';
import { JsonSchema, UISchemaElement } from '@jsonforms/core';
import { TranslocoTestingModule } from '@jsverse/transloco';

import { catalogEditorRenderers } from './index';

@Component({
  imports: [JsonFormsModule],
  template: `<jsonforms
    [schema]="schema"
    [uischema]="uischema!"
    [data]="data"
    [renderers]="renderers"
    [readonly]="readonly"
    (dataChange)="onDataChange($event)"
  />`
})
class HarnessComponent {
  schema!: JsonSchema;
  uischema: UISchemaElement | undefined = undefined;
  data: unknown;
  readonly renderers = catalogEditorRenderers;
  readonly = false;
  lastData: unknown;
  onDataChange(d: unknown): void {
    this.lastData = d;
  }
}

async function render(schema: JsonSchema, data: unknown, uischema?: UISchemaElement, readonly = false) {
  await TestBed.configureTestingModule({
    imports: [
      HarnessComponent,
      TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
    ]
  }).compileComponents();

  const fixture = TestBed.createComponent(HarnessComponent);
  fixture.componentInstance.schema = schema;
  fixture.componentInstance.uischema = uischema;
  fixture.componentInstance.data = data;
  fixture.componentInstance.readonly = readonly;
  fixture.detectChanges();
  return fixture;
}

describe('catalog editor custom renderers', () => {
  describe('BooleanControlRenderer', () => {
    it('renders a checkbox reflecting the boolean value and emits changes', async () => {
      const fixture = await render({ type: 'object', properties: { active: { type: 'boolean' } } }, { active: false });

      const checkbox: HTMLInputElement = fixture.nativeElement.querySelector('input[type="checkbox"]');
      expect(checkbox.checked).toBe(false);

      checkbox.checked = true;
      checkbox.dispatchEvent(new Event('change'));
      fixture.detectChanges();

      expect(fixture.componentInstance.lastData).toEqual({ active: true });
    });
  });

  describe('TextControlRenderer', () => {
    it('renders a text input and emits changes', async () => {
      const fixture = await render({ type: 'object', properties: { name: { type: 'string' } } }, { name: 'old' });

      const input: HTMLInputElement = fixture.nativeElement.querySelector('input[type="text"]');
      expect(input.value).toBe('old');

      input.value = 'new value';
      input.dispatchEvent(new Event('input'));
      fixture.detectChanges();

      expect(fixture.componentInstance.lastData).toEqual({ name: 'new value' });
    });

    it('uses an email input type for string fields with format "email"', async () => {
      const fixture = await render({ type: 'object', properties: { email: { type: 'string', format: 'email' } } }, { email: 'a@b.com' });
      const input: HTMLInputElement = fixture.nativeElement.querySelector('input');
      expect(input.type).toBe('email');
    });

    it('uses a tel input type for string fields with format "tel"', async () => {
      const fixture = await render({ type: 'object', properties: { phone: { type: 'string', format: 'tel' } } }, { phone: '123' });
      const input: HTMLInputElement = fixture.nativeElement.querySelector('input');
      expect(input.type).toBe('tel');
    });

    it('uses a text input type for an unrecognized format via the options override', async () => {
      const fixture = await render(
        { type: 'object', properties: { code: { type: 'string' } } },
        { code: 'abc' },
        { type: 'Control', scope: '#/properties/code', options: { format: 'search' } } as UISchemaElement
      );
      const input: HTMLInputElement = fixture.nativeElement.querySelector('input');
      expect(input.type).toBe('search');
    });
  });

  describe('NumberControlRenderer', () => {
    it('renders a number input, applies min/max/step from the schema, and emits parsed values', async () => {
      const fixture = await render(
        { type: 'object', properties: { level: { type: 'integer', minimum: 1, maximum: 10 } } },
        { level: 3 }
      );

      const input: HTMLInputElement = fixture.nativeElement.querySelector('input[type="number"]');
      expect(input.min).toBe('1');
      expect(input.max).toBe('10');
      expect(input.step).toBe('1');

      input.value = '7';
      input.dispatchEvent(new Event('input'));
      fixture.detectChanges();
      expect(fixture.componentInstance.lastData).toEqual({ level: 7 });

      input.value = '';
      input.dispatchEvent(new Event('input'));
      fixture.detectChanges();
      expect(fixture.componentInstance.lastData).toEqual({ level: undefined });
    });

    it('defaults the step to 0.1 for non-integer numeric fields without multipleOf', async () => {
      const fixture = await render({ type: 'object', properties: { ratio: { type: 'number' } } }, { ratio: 0.5 });
      const input: HTMLInputElement = fixture.nativeElement.querySelector('input[type="number"]');
      expect(input.step).toBe('0.1');
    });
  });

  describe('EnumControlRenderer', () => {
    it('renders select options from the schema enum and emits the selected value', async () => {
      const fixture = await render(
        { type: 'object', properties: { tier: { type: 'string', enum: ['light', 'heavy'] } } },
        { tier: 'light' }
      );

      const select: HTMLSelectElement = fixture.nativeElement.querySelector('select');
      expect(Array.from(select.options).map((o) => o.value)).toEqual(['light', 'heavy']);

      select.value = 'heavy';
      select.dispatchEvent(new Event('change'));
      fixture.detectChanges();

      expect(fixture.componentInstance.lastData).toEqual({ tier: 'heavy' });
    });
  });

  describe('VerticalLayoutRenderer and GroupLayoutRenderer', () => {
    it('renders a group card with a title inside the root vertical layout', async () => {
      const schema: JsonSchema = { type: 'object', properties: { name: { type: 'string' } } };
      const uischema: UISchemaElement = {
        type: 'VerticalLayout',
        elements: [
          {
            type: 'Group',
            label: 'Details',
            elements: [{ type: 'Control', scope: '#/properties/name' }]
          }
        ]
      } as UISchemaElement;

      const fixture = await render(schema, { name: 'x' }, uischema);

      expect(fixture.nativeElement.querySelector('.vertical-layout')).not.toBeNull();
      const group = fixture.nativeElement.querySelector('.group-layout');
      expect(group).not.toBeNull();
      expect(group.querySelector('.card-title').textContent).toBe('Details');
    });
  });

  describe('ObjectControlRenderer', () => {
    it('renders a nested object field as its own card, titled with the camelCase-split, capitalized path', async () => {
      const schema: JsonSchema = {
        type: 'object',
        properties: {
          baseCost: { type: 'object', properties: { metal: { type: 'number' } } }
        }
      };
      const uischema: UISchemaElement = {
        type: 'VerticalLayout',
        elements: [{ type: 'Control', scope: '#/properties/baseCost' }]
      } as UISchemaElement;

      const fixture = await render(schema, { baseCost: { metal: 100 } }, uischema);

      expect(fixture.nativeElement.querySelector('.object-layout')).not.toBeNull();
      expect(fixture.nativeElement.querySelector('input[type="number"]')).not.toBeNull();
      expect(fixture.nativeElement.querySelector('.card-title').textContent).toBe('Base Cost');
    });

    it('start-cases a snake_case path the same way as camelCase', async () => {
      const schema: JsonSchema = {
        type: 'object',
        properties: {
          unit_cost: { type: 'object', properties: { metal: { type: 'number' } } }
        }
      };
      const uischema: UISchemaElement = {
        type: 'VerticalLayout',
        elements: [{ type: 'Control', scope: '#/properties/unit_cost' }]
      } as UISchemaElement;

      const fixture = await render(schema, { unit_cost: { metal: 100 } }, uischema);

      expect(fixture.nativeElement.querySelector('.card-title').textContent).toBe('Unit Cost');
    });

    it('start-cases a dotted (nested-path) scope by joining each segment', async () => {
      const schema: JsonSchema = {
        type: 'object',
        properties: {
          outer: {
            type: 'object',
            properties: {
              innerGroup: { type: 'object', properties: { x: { type: 'string' } } }
            }
          }
        }
      };

      const fixture = await render(schema, { outer: { innerGroup: { x: 'y' } } });

      const titles = Array.from(fixture.nativeElement.querySelectorAll('.card-title')).map((el) => (el as HTMLElement).textContent);
      expect(titles).toContain('Outer');
      expect(titles).toContain('Outer Inner Group');
    });

    it('renders the root object control as a plain vertical layout with no group title', async () => {
      const schema: JsonSchema = { type: 'object', properties: { name: { type: 'string' } } };
      const uischema: UISchemaElement = { type: 'Control', scope: '#' } as UISchemaElement;

      const fixture = await render(schema, { name: 'x' }, uischema);

      expect(fixture.nativeElement.querySelector('.vertical-layout')).not.toBeNull();
      expect(fixture.nativeElement.querySelector('.card-title')).toBeNull();
    });

    it('marks the nested control read-only when the form is disabled', async () => {
      const schema: JsonSchema = {
        type: 'object',
        properties: {
          baseCost: { type: 'object', properties: { metal: { type: 'number' } } }
        }
      };
      const uischema: UISchemaElement = {
        type: 'VerticalLayout',
        elements: [{ type: 'Control', scope: '#/properties/baseCost' }]
      } as UISchemaElement;

      const fixture = await render(schema, { baseCost: { metal: 100 } }, uischema, true);

      const input: HTMLInputElement = fixture.nativeElement.querySelector('input[type="number"]');
      expect(input.disabled).toBe(true);
    });
  });

  describe('ArrayLayoutRenderer', () => {
    // isObjectArrayWithNesting requires the array item schema to contain a nested object
    // (object-within-object), not just a flat object - a flat items schema falls through to
    // no matching custom renderer at all, so "detail" here is load-bearing, not incidental.
    const schema: JsonSchema = {
      type: 'object',
      properties: {
        requirements: {
          type: 'array',
          items: {
            type: 'object',
            properties: {
              kind: { type: 'string' },
              detail: { type: 'object', properties: { note: { type: 'string' } } }
            }
          }
        }
      }
    };

    it('renders one card per array item and shows a no-items hint when empty', async () => {
      const fixture = await render(schema, { requirements: [] });
      expect(fixture.nativeElement.querySelector('.field-hint')).not.toBeNull();
      expect(fixture.nativeElement.querySelectorAll('.array-item').length).toBe(0);
    });

    it('adds a new item when the add button is clicked', async () => {
      const fixture = await render(schema, { requirements: [] });
      const addButton: HTMLButtonElement = fixture.nativeElement.querySelector('.array-layout-toolbar button');

      addButton.click();
      fixture.detectChanges();

      expect((fixture.componentInstance.lastData as { requirements: unknown[] }).requirements.length).toBe(1);
    });

    it('removes an item when its remove button is clicked', async () => {
      const fixture = await render(schema, {
        requirements: [
          { kind: 'BUILDING', detail: { note: 'first' } },
          { kind: 'TECHNOLOGY', detail: { note: 'second' } }
        ]
      });

      const items = fixture.nativeElement.querySelectorAll('.array-item');
      expect(items.length).toBe(2);

      const removeButton: HTMLButtonElement = items[0].querySelector('.card-actions button:last-child');
      removeButton.click();
      fixture.detectChanges();

      const remaining = (fixture.componentInstance.lastData as { requirements: { kind: string }[] }).requirements;
      expect(remaining.length).toBe(1);
      expect(remaining[0].kind).toBe('TECHNOLOGY');
    });

    it('shows no move buttons when showSortButtons is not set', async () => {
      const fixture = await render(schema, { requirements: [{ kind: 'BUILDING', detail: { note: 'first' } }] });
      const items = fixture.nativeElement.querySelectorAll('.array-item');
      const buttons = Array.from(items[0].querySelectorAll('.card-actions button')) as HTMLButtonElement[];
      expect(buttons.length).toBe(1); // just "remove"
    });

    it('reorders items via the move up/down buttons when showSortButtons is enabled', async () => {
      const uischema: UISchemaElement = { type: 'Control', scope: '#/properties/requirements', options: { showSortButtons: true } } as UISchemaElement;
      const fixture = await render(
        schema,
        {
          requirements: [
            { kind: 'FIRST', detail: { note: 'a' } },
            { kind: 'SECOND', detail: { note: 'b' } }
          ]
        },
        uischema
      );

      const items = fixture.nativeElement.querySelectorAll('.array-item');
      const firstItemButtons = Array.from(items[0].querySelectorAll('.card-actions button')) as HTMLButtonElement[];
      // moveUp, moveDown, remove
      expect(firstItemButtons.length).toBe(3);
      expect(firstItemButtons[0].disabled).toBe(true); // first item can't move up
      expect(firstItemButtons[1].disabled).toBe(false);

      firstItemButtons[1].click(); // move first item down
      fixture.detectChanges();

      const reordered = (fixture.componentInstance.lastData as { requirements: { kind: string }[] }).requirements;
      expect(reordered.map((r) => r.kind)).toEqual(['SECOND', 'FIRST']);
    });
  });
});
