import { JsonFormsRendererRegistryEntry } from '@jsonforms/core';

import { ArrayLayoutRenderer, ArrayLayoutRendererTester } from './array-layout.renderer';
import { BooleanControlRenderer, BooleanControlRendererTester } from './boolean-control.renderer';
import { EnumControlRenderer, EnumControlRendererTester } from './enum-control.renderer';
import { GroupLayoutRenderer, GroupLayoutRendererTester } from './group-layout.renderer';
import { NumberControlRenderer, NumberControlRendererTester } from './number-control.renderer';
import { ObjectControlRenderer, ObjectControlRendererTester } from './object-control.renderer';
import { TextControlRenderer, TextControlRendererTester } from './text-control.renderer';
import { VerticalLayoutRenderer, VerticalLayoutRendererTester } from './vertical-layout.renderer';

export const catalogEditorRenderers: JsonFormsRendererRegistryEntry[] = [
  { tester: TextControlRendererTester, renderer: TextControlRenderer },
  { tester: NumberControlRendererTester, renderer: NumberControlRenderer },
  { tester: EnumControlRendererTester, renderer: EnumControlRenderer },
  { tester: BooleanControlRendererTester, renderer: BooleanControlRenderer },
  { tester: VerticalLayoutRendererTester, renderer: VerticalLayoutRenderer },
  { tester: GroupLayoutRendererTester, renderer: GroupLayoutRenderer },
  { tester: ObjectControlRendererTester, renderer: ObjectControlRenderer },
  { tester: ArrayLayoutRendererTester, renderer: ArrayLayoutRenderer },
];
