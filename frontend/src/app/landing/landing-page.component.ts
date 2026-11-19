import { Component, EventEmitter, Output } from '@angular/core';
import { LandingFeaturesComponent } from './features/landing-features.component';
import { LandingFooterComponent } from './footer/landing-footer.component';
import { LandingHeaderComponent } from './header/landing-header.component';
import { LandingHeroComponent } from './hero/landing-hero.component';
import { LandingLoginSectionComponent } from './login-section/landing-login-section.component';
import { LandingProcessComponent } from './process/landing-process.component';
import { LandingStatsComponent } from './stats/landing-stats.component';

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [
    LandingHeaderComponent,
    LandingHeroComponent,
    LandingFeaturesComponent,
    LandingProcessComponent,
    LandingStatsComponent,
    LandingLoginSectionComponent,
    LandingFooterComponent,
  ],
  templateUrl: './landing-page.component.html',
  styleUrl: './landing-page.component.scss',
})
export class LandingPageComponent {
  @Output() authenticated = new EventEmitter<void>();
}
