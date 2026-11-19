import { AfterViewInit, Directive, ElementRef, OnDestroy, inject } from '@angular/core';

@Directive({
  selector: '[appReveal]',
  standalone: true,
})
export class RevealDirective implements AfterViewInit, OnDestroy {
  private readonly element = inject(ElementRef<HTMLElement>).nativeElement;
  private observer?: IntersectionObserver;

  ngAfterViewInit(): void {
    this.element.classList.add('app-reveal');

    if (
      typeof window === 'undefined' ||
      typeof IntersectionObserver === 'undefined' ||
      window.matchMedia('(prefers-reduced-motion: reduce)').matches
    ) {
      this.element.classList.add('is-visible');
      return;
    }

    this.element.classList.add('reveal-ready');
    this.observer = new IntersectionObserver(
      (entries, observer) => {
        if (entries.some((entry) => entry.isIntersecting)) {
          this.element.classList.add('is-visible');
          this.element.classList.remove('reveal-ready');
          observer.unobserve(this.element);
        }
      },
      { threshold: 0.2 },
    );
    this.observer.observe(this.element);
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }
}
