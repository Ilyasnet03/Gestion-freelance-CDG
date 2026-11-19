import { Component } from '@angular/core';
import { RevealDirective } from '../shared/reveal.directive';

@Component({
  selector: 'app-landing-process',
  standalone: true,
  imports: [RevealDirective],
  templateUrl: './landing-process.component.html',
  styleUrl: './landing-process.component.scss',
})
export class LandingProcessComponent {
  readonly steps = [
    {
      title: 'Saisir',
      description: 'Le freelance renseigne les heures et les tâches réalisées.',
    },
    {
      title: 'Soumettre',
      description: 'La feuille de temps est transmise au responsable du projet.',
    },
    {
      title: 'Valider',
      description: 'Le responsable vérifie et valide les éléments déclarés.',
    },
    {
      title: 'Générer le PV',
      description: 'Le PV de réception est préparé à partir du temps validé.',
    },
  ];
}
