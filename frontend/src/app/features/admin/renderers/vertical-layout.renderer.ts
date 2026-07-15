import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { JsonFormsModule } from '@jsonforms/angular';
import { RankedTester, rankWith, uiTypeIs, VerticalLayout } from '@jsonforms/core';

import { LayoutChildrenRenderPropsPipe, LayoutRenderer } from './layout-renderer.base';

@Component({
  selector: 'app-vertical-layout-renderer',
  template: `
    <div class="vertical-layout" [style.display]="hidden ? 'none' : ''">
      @for (props of uischema | layoutChildrenRenderProps: schema : path; track trackElement($index, props)) {
        <jsonforms-outlet [renderProps]="props" />
      }
    </div>
  `,
  styles: [
    `
      .vertical-layout {
        display: flex;
        flex-direction: column;
        gap: 1rem;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, JsonFormsModule, LayoutChildrenRenderPropsPipe],
})
export class VerticalLayoutRenderer extends LayoutRenderer<VerticalLayout> {}

export const VerticalLayoutRendererTester: RankedTester = rankWith(1, uiTypeIs('VerticalLayout'));
