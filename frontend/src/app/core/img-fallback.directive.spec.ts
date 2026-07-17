import { Component, ElementRef, ViewChild } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { ImgFallbackDirective } from './img-fallback.directive';

@Component({
  imports: [ImgFallbackDirective],
  template: `<img appImgFallback #img />`
})
class HostComponent {
  @ViewChild('img') img!: ElementRef<HTMLImageElement>;
}

describe('ImgFallbackDirective', () => {
  it('hides the image element from layout when it errors', () => {
    const fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();

    const img = fixture.componentInstance.img.nativeElement;
    expect(img.style.display).toBe('');

    img.dispatchEvent(new Event('error'));
    expect(img.style.display).toBe('none');
  });
});
