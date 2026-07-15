import { Directive, ElementRef, HostListener, inject } from '@angular/core';

/**
 * Hides the image instead of showing the browser's broken-image icon when the
 * underlying asset (e.g. an icon not yet added under public/images/) 404s.
 */
@Directive({
  selector: 'img[appImgFallback]'
})
export class ImgFallbackDirective {
  private readonly el = inject(ElementRef<HTMLImageElement>);

  @HostListener('error')
  onError(): void {
    this.el.nativeElement.style.visibility = 'hidden';
  }
}
