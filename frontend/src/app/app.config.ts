import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { definePreset } from '@primeuix/themes';
import Aura from '@primeuix/themes/aura';
import { providePrimeNG } from 'primeng/config';
import { DialogService } from 'primeng/dynamicdialog';
import { routes } from './app.routes';

const CdgTheme = definePreset(Aura, {
  semantic: {
    primary: {
      50: '#f5f8ee',
      100: '#eaf2db',
      200: '#d3e3b8',
      300: '#b7d08b',
      400: '#95b85a',
      500: '#628a1a',
      600: '#557b17',
      700: '#4f6f15',
      800: '#3f5a10',
      900: '#344a0e',
      950: '#1b2806',
    },
  },
});

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideHttpClient(),
    provideRouter(routes),
    providePrimeNG({
      theme: { preset: CdgTheme, options: { darkModeSelector: false } },
      translation: {
        emptyMessage: 'Aucun résultat trouvé',
        emptyFilterMessage: 'Aucun résultat trouvé',
        aria: { close: 'Fermer' },
      },
    }),
    DialogService,
  ],
};
