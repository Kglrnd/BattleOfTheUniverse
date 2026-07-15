import { Routes } from '@angular/router';

import { authGuard, adminGuard, staffGuard } from './core/auth.guard';

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
    path: 'buildings',
    canActivate: [authGuard],
    loadComponent: () => import('./features/universe/buildings-page.component').then((m) => m.BuildingsPageComponent)
  },
  {
    path: 'shipyard',
    canActivate: [authGuard],
    loadComponent: () => import('./features/fleet/shipyard-page.component').then((m) => m.ShipyardPageComponent)
  },
  {
    path: 'fleet',
    canActivate: [authGuard],
    loadComponent: () => import('./features/fleet/fleet.component').then((m) => m.FleetComponent)
  },
  {
    path: 'defense',
    canActivate: [authGuard],
    loadComponent: () => import('./features/defense/defense-page.component').then((m) => m.DefensePageComponent)
  },
  {
    path: 'espionage',
    canActivate: [authGuard],
    loadComponent: () => import('./features/espionage/espionage-page.component').then((m) => m.EspionagePageComponent)
  },
  {
    path: 'research',
    canActivate: [authGuard],
    loadComponent: () => import('./features/research/research.component').then((m) => m.ResearchComponent)
  },
  {
    path: 'messages',
    canActivate: [authGuard],
    loadComponent: () => import('./features/messages/messages.component').then((m) => m.MessagesComponent)
  },
  {
    path: 'highscore',
    canActivate: [authGuard],
    loadComponent: () => import('./features/highscore/highscore.component').then((m) => m.HighscoreComponent)
  },
  {
    path: 'admin/catalog/:type',
    canActivate: [staffGuard],
    loadComponent: () => import('./features/admin/catalog-editor.component').then((m) => m.CatalogEditorComponent)
  },
  {
    path: 'admin/planets',
    canActivate: [staffGuard],
    loadComponent: () => import('./features/admin/admin-planets.component').then((m) => m.AdminPlanetsComponent)
  },
  {
    path: 'admin/users',
    canActivate: [staffGuard],
    loadComponent: () => import('./features/admin/admin-users.component').then((m) => m.AdminUsersComponent)
  },
  {
    path: 'admin/reset',
    canActivate: [adminGuard],
    loadComponent: () => import('./features/admin/admin-reset.component').then((m) => m.AdminResetComponent)
  },
  { path: '**', redirectTo: 'universe' }
];
