import { Component, ElementRef, HostListener, OnDestroy, inject } from '@angular/core';

@Component({
  selector: 'app-landing-hero',
  standalone: true,
  templateUrl: './landing-hero.component.html',
  styleUrl: './landing-hero.component.scss',
})
export class LandingHeroComponent implements OnDestroy {
  private readonly host = inject(ElementRef<HTMLElement>).nativeElement;
  private animationFrame = 0;

  @HostListener('window:scroll')
  onWindowScroll(): void {
    if (
      this.animationFrame ||
      typeof window === 'undefined' ||
      window.matchMedia('(prefers-reduced-motion: reduce)').matches
    ) {
      return;
    }

    this.animationFrame = window.requestAnimationFrame(() => {
      const offset = Math.min(window.scrollY * 0.3, 120);
      this.host.style.setProperty('--hero-parallax-y', `${offset}px`);
      this.animationFrame = 0;
    });
  }

  ngOnDestroy(): void {
    if (this.animationFrame && typeof window !== 'undefined') {
      window.cancelAnimationFrame(this.animationFrame);
    }
  }
}
