import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { AppFooterComponent } from './app-footer.component';
import { VersionService } from '../version.service';

describe('AppFooterComponent', () => {
  it('renders the version once the version service resolves', async () => {
    await TestBed.configureTestingModule({
      imports: [AppFooterComponent],
      providers: [{ provide: VersionService, useValue: { getVersion: () => of('1.2.3') } }]
    }).compileComponents();

    const fixture = TestBed.createComponent(AppFooterComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('v1.2.3');
  });

  it('renders nothing when the version service resolves to null (e.g. the request failed)', async () => {
    await TestBed.configureTestingModule({
      imports: [AppFooterComponent],
      providers: [{ provide: VersionService, useValue: { getVersion: () => of(null) } }]
    }).compileComponents();

    const fixture = TestBed.createComponent(AppFooterComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.app-version')).toBeNull();
  });
});
