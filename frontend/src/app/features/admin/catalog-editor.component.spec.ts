import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { BehaviorSubject, of, throwError } from 'rxjs';

import { AdminCatalogApiService } from './admin-catalog-api.service';
import { CatalogEditorComponent } from './catalog-editor.component';

describe('CatalogEditorComponent', () => {
  let paramMap$: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let getData: ReturnType<typeof vi.fn>;
  let getSchema: ReturnType<typeof vi.fn>;
  let save: ReturnType<typeof vi.fn>;

  async function setup(params: Record<string, string> = { type: 'buildings' }) {
    paramMap$ = new BehaviorSubject(convertToParamMap(params));
    getData = vi.fn(() => of({ metal_mine: { name: 'Metal Mine' } }));
    getSchema = vi.fn(() => of({ type: 'object' }));
    save = vi.fn(() => of({ metal_mine: { name: 'Metal Mine' } }));

    await TestBed.configureTestingModule({
      imports: [
        CatalogEditorComponent,
        TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
      ],
      providers: [
        { provide: AdminCatalogApiService, useValue: { getData, getSchema, save } },
        { provide: ActivatedRoute, useValue: { paramMap: paramMap$, snapshot: { paramMap: convertToParamMap(params) } } }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(CatalogEditorComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('defaults to "buildings" when no type param is present', async () => {
    await setup({});
    expect(getData).toHaveBeenCalledWith('buildings');
    expect(getSchema).toHaveBeenCalledWith('buildings');
  });

  it('loads the schema and data for the requested catalog type', async () => {
    await setup({ type: 'ships' });
    expect(getData).toHaveBeenCalledWith('ships');
    expect(getSchema).toHaveBeenCalledWith('ships');
  });

  it('onDataChange updates the local data signal', async () => {
    const fixture = await setup();
    const component = fixture.componentInstance as unknown as { data: () => unknown };

    fixture.componentInstance.onDataChange({ metal_mine: { name: 'Renamed' } });

    expect(component.data()).toEqual({ metal_mine: { name: 'Renamed' } });
  });

  it('save persists the current data and shows a saved message', async () => {
    const fixture = await setup();
    fixture.componentInstance.save();

    expect(save).toHaveBeenCalledWith('buildings', { metal_mine: { name: 'Metal Mine' } });
    const component = fixture.componentInstance as unknown as { savedMessage: () => string | null; saving: () => boolean };
    expect(component.savedMessage()).toBeTruthy();
    expect(component.saving()).toBe(false);
  });

  it('save surfaces an error message when saving fails', async () => {
    save = vi.fn(() => throwError(() => ({ error: { message: 'validation failed' } })));
    await TestBed.configureTestingModule({
      imports: [
        CatalogEditorComponent,
        TranslocoTestingModule.forRoot({ langs: { en: {} }, translocoConfig: { availableLangs: ['en'], defaultLang: 'en' } })
      ],
      providers: [
        { provide: AdminCatalogApiService, useValue: { getData: vi.fn(() => of({})), getSchema: vi.fn(() => of({ type: 'object' })), save } },
        {
          provide: ActivatedRoute,
          useValue: { paramMap: new BehaviorSubject(convertToParamMap({ type: 'buildings' })), snapshot: { paramMap: convertToParamMap({}) } }
        }
      ]
    }).compileComponents();
    const fixture = TestBed.createComponent(CatalogEditorComponent);
    fixture.detectChanges();

    fixture.componentInstance.save();

    const component = fixture.componentInstance as unknown as { errorMessage: () => string | null };
    expect(component.errorMessage()).toBe('validation failed');
  });

  it('switches type and reloads schema/data when the route param changes', async () => {
    const fixture = await setup({ type: 'buildings' });
    getData.mockClear();
    getSchema.mockClear();

    paramMap$.next(convertToParamMap({ type: 'technologies' }));
    fixture.detectChanges();

    expect(getData).toHaveBeenCalledWith('technologies');
    expect(getSchema).toHaveBeenCalledWith('technologies');
  });
});
