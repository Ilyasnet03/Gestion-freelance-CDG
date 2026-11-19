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
      title: 'Timesheet',
      image: '/assets/feature-timesheet.jpg',
      description: 'Saisie des heures travaillées, soumission des feuilles de temps et validation.',
    },
    {
      title: 'Projets et tâches',
      image: '/assets/feature-projects.jpg',
      description: 'Organisation des projets, tâches et affectations des freelances.',
    },
    {
      title: 'Facturation',
      image: '/assets/feature-billing.jpg',
      description: 'Préparation des PV de réception et suivi des éléments de facturation.',
    },
    {
      title: 'Analyse et aide à la décision',
      image: '/assets/feature-analysis.jpg',
      description: "Consultation des indicateurs clés et suivi de l'activité.",
    },
  ];
}
