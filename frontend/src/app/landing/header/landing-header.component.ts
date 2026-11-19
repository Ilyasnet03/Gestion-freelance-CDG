import { AfterViewInit, Component, HostListener, OnDestroy, signal } from '@angular/core';

@Component({
  selector: 'app-landing-header',
  standalone: true,
  templateUrl: './landing-header.component.html',
  styleUrl: './landing-header.component.scss',
})
export class LandingHeaderComponent implements AfterViewInit, OnDestroy {
  readonly scrolled = signal(false);
  readonly mobileMenuOpen = signal(false);
  readonly activeSection = signal('accueil');
  language: 'FR' | 'EN' | 'AR' = 'FR';

  private sectionObserver?: IntersectionObserver;
  private scrollFrame = 0;

  @HostListener('window:scroll')
  onWindowScroll(): void {
    if (this.scrollFrame || typeof window === 'undefined') return;

    this.scrollFrame = window.requestAnimationFrame(() => {
      this.scrolled.set(window.scrollY > 40);
      this.scrollFrame = 0;
    });
  }

  ngAfterViewInit(): void {
    this.onWindowScroll();
    if (typeof IntersectionObserver === 'undefined') return;

    this.sectionObserver = new IntersectionObserver(
      (entries) => {
        const visibleSection = entries
          .filter((entry) => entry.isIntersecting)
          .sort((left, right) => right.intersectionRatio - left.intersectionRatio)[0];

        if (visibleSection) {
          this.activeSection.set((visibleSection.target as HTMLElement).id);
        }
      },
      { rootMargin: '-20% 0px -60% 0px', threshold: [0, 0.2, 0.5, 0.75] },
    );

    for (const id of ['accueil', 'fonctionnalites', 'processus', 'connexion']) {
      const section = document.getElementById(id);
      if (section) this.sectionObserver.observe(section);
    }
  }

  changeLanguage(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.language = value === 'AR' || value === 'EN' ? value : 'FR';
    document.documentElement.lang = this.language.toLowerCase();
    document.documentElement.dir = this.language === 'AR' ? 'rtl' : 'ltr';
  }

  closeMobileMenu(): void {
    this.mobileMenuOpen.set(false);
  }

  ngOnDestroy(): void {
    this.sectionObserver?.disconnect();
    if (this.scrollFrame && typeof window !== 'undefined') {
      window.cancelAnimationFrame(this.scrollFrame);
    }
  }
}
