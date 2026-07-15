import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { JsonFormsModule } from '@jsonforms/angular';
import { GroupLayout, RankedTester, rankWith, uiTypeIs } from '@jsonforms/core';

import { LayoutChildrenRenderPropsPipe, LayoutRenderer } from './layout-renderer.base';

@Component({
  selector: 'app-group-layout-renderer',
  template: `
    <div class="card group-layout" [style.display]="hidden ? 'none' : ''">
      @if (label) {
        <h3 class="card-title">{{ label }}</h3>
      }
      @for (props of uischema | layoutChildrenRenderProps: schema : path; track trackElement($index, props)) {
        <jsonforms-outlet [renderProps]="props" />
      }
    </div>
  `,
  styles: [
    `
      .group-layout {
        display: flex;
        flex-direction: column;
        gap: 1rem;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, JsonFormsModule, LayoutChildrenRenderPropsPipe],
})
export class GroupLayoutRenderer extends LayoutRenderer<GroupLayout> {}

export const GroupLayoutRendererTester: RankedTester = rankWith(1, uiTypeIs('Group'));
