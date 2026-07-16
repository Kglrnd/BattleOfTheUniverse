import { Directive, ElementRef, HostListener, inject } from '@angular/core';

/**
 * Removes the image from layout instead of showing the browser's broken-image icon
 * (or its alt text) when the underlying asset (e.g. an icon not yet added under
 * public/images/) 404s - `display: none` rather than `visibility: hidden` so the
 * space it would have occupied collapses instead of leaving a blank gap.
 */
@Directive({
  selector: 'img[appImgFallback]'
})
export class ImgFallbackDirective {
  private readonly el = inject(ElementRef<HTMLImageElement>);

  @HostListener('error')
  onError(): void {
    this.el.nativeElement.style.display = 'none';
  }
}
