import { Component } from '@angular/core';
import { RevealDirective } from '../shared/reveal.directive';

@Component({
  selector: 'app-landing-features',
  standalone: true,
  imports: [RevealDirective],
  templateUrl: './landing-features.component.html',
  styleUrl: './landing-features.component.scss',
})
export class LandingFeaturesComponent {
  readonly features = [
    {
      icon: 'timesheet',
      title: 'Timesheet',
      description: 'Saisissez les heures travaillées par semaine et suivez leur validation.',
    },
    {
      icon: 'projects',
      title: 'Projets et tâches',
      description: 'Organisez les projets, les tâches et les affectations des freelances.',
    },
    {
      icon: 'billing',
      title: 'Facturation',
      description: 'Préparez les PV de réception et suivez les éléments de facturation.',
    },
    {
      icon: 'analysis',
      title: 'Analyse et aide à la décision',
      description: 'Consultez les indicateurs clés et repérez rapidement les points d’attention.',
    },
  ];
}
