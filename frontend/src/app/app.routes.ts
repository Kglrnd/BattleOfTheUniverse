import { Routes } from '@angular/router';

import { authGuard, adminGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'universe' },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register.component').then((m) => m.RegisterComponent)
  },
  {
    path: 'universe',
    canActivate: [authGuard],
    loadComponent: () => import('./features/universe/planet-list.component').then((m) => m.PlanetListComponent)
  },
  {
    path: 'universe/system',
    canActivate: [authGuard],
    loadComponent: () => import('./features/universe/system-view.component').then((m) => m.SystemViewComponent)
  },
  {
    path: 'universe/system/:galaxy/:system',
    canActivate: [authGuard],
    loadComponent: () => import('./features/universe/system-view.component').then((m) => m.SystemViewComponent)
  },
  {
    path: 'universe/:id',
    canActivate: [authGuard],
    loadComponent: () => import('./features/universe/planet-detail.component').then((m) => m.PlanetDetailComponent)
  },
  {
    path: 'fleet',
    canActivate: [authGuard],
    loadComponent: () => import('./features/fleet/fleet.component').then((m) => m.FleetComponent)
  },
  {
    path: 'research',
    canActivate: [authGuard],
    loadComponent: () => import('./features/research/research.component').then((m) => m.ResearchComponent)
  },
  {
    path: 'admin/catalog/:type',
    canActivate: [adminGuard],
    loadComponent: () => import('./features/admin/catalog-editor.component').then((m) => m.CatalogEditorComponent)
  },
  { path: '**', redirectTo: 'universe' }
];
