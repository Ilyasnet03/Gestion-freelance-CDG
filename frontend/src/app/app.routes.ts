import { Routes } from '@angular/router';

export const routes: Routes = [
  // App renders home or dashboard from its auth signal and URL; these routes keep the SPA paths valid.
  { path: '', children: [], pathMatch: 'full' },
  { path: 'dashboard', children: [], pathMatch: 'full' },
  { path: 'login', redirectTo: '', pathMatch: 'full' },
  { path: '**', redirectTo: '' },
];
