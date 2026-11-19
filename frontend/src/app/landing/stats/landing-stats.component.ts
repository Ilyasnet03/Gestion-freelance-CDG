import { AfterViewInit, Component, ElementRef, OnDestroy, inject, signal } from '@angular/core';
import { RevealDirective } from '../shared/reveal.directive';

@Component({
  selector: 'app-landing-stats',
  standalone: true,
  imports: [RevealDirective],
  templateUrl: './landing-stats.component.html',
  styleUrl: './landing-stats.component.scss',
})
export class LandingStatsComponent implements AfterViewInit, OnDestroy {
  private readonly host = inject(ElementRef<HTMLElement>).nativeElement;
  private readonly targets = [3, 4, 5];
  readonly counts = signal([0, 0, 0]);

  private observer?: IntersectionObserver;
  private animationFrame = 0;

  ngAfterViewInit(): void {
    if (
      typeof window === 'undefined' ||
      typeof IntersectionObserver === 'undefined' ||
      window.matchMedia('(prefers-reduced-motion: reduce)').matches
    ) {
      this.counts.set(this.targets);
      return;
    }

    this.observer = new IntersectionObserver(
      (entries, observer) => {
        if (entries.some((entry) => entry.isIntersecting)) {
          observer.disconnect();
          this.animateCounts();
        }
      },
      { threshold: 0.2 },
    );
    this.observer.observe(this.host);
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
    if (this.animationFrame && typeof window !== 'undefined') {
      window.cancelAnimationFrame(this.animationFrame);
    }
  }

  private animateCounts(): void {
    const startedAt = performance.now();
    const update = (now: number): void => {
      const progress = Math.min((now - startedAt) / 1200, 1);
      const eased = 1 - Math.pow(1 - progress, 3);
      this.counts.set(this.targets.map((target) => Math.round(target * eased)));

      if (progress < 1) {
        this.animationFrame = window.requestAnimationFrame(update);
      } else {
        this.animationFrame = 0;
      }
    };

    this.animationFrame = window.requestAnimationFrame(update);
  }
}
