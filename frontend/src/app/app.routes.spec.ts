import { adminGuard, authGuard, staffGuard } from './core/auth.guard';
import { routes } from './app.routes';

describe('routes', () => {
  it('redirects the empty path to /universe', () => {
    const root = routes.find((r) => r.path === '');
    expect(root?.redirectTo).toBe('universe');
    expect(root?.pathMatch).toBe('full');
  });

  it('redirects unknown paths (wildcard) to /universe', () => {
    const wildcard = routes.find((r) => r.path === '**');
    expect(wildcard?.redirectTo).toBe('universe');
  });

  it('does not guard the public login/register routes', () => {
    expect(routes.find((r) => r.path === 'login')?.canActivate).toBeUndefined();
    expect(routes.find((r) => r.path === 'register')?.canActivate).toBeUndefined();
  });

  const authGuardedPaths = [
    'universe',
    'universe/system',
    'universe/system/:galaxy/:system',
    'universe/:id',
    'buildings',
    'resources',
    'shipyard',
    'fleet',
    'defense',
    'espionage',
    'research',
    'messages',
    'highscore',
    'features'
  ];

  it.each(authGuardedPaths)('guards "%s" with authGuard', (path) => {
    expect(routes.find((r) => r.path === path)?.canActivate).toEqual([authGuard]);
  });

  const staffGuardedPaths = ['admin/catalog/:type', 'admin/planets', 'admin/users'];

  it.each(staffGuardedPaths)('guards "%s" with staffGuard', (path) => {
    expect(routes.find((r) => r.path === path)?.canActivate).toEqual([staffGuard]);
  });

  it('guards admin/reset with adminGuard specifically (stricter than staffGuard)', () => {
    expect(routes.find((r) => r.path === 'admin/reset')?.canActivate).toEqual([adminGuard]);
  });

  it('every route other than the redirects has a loadComponent factory that resolves to a component class', async () => {
    const withoutRedirect = routes.filter((r) => r.path !== '' && r.path !== '**');
    for (const route of withoutRedirect) {
      expect(route.loadComponent).toBeInstanceOf(Function);
      const component = await route.loadComponent!();
      expect(component).toBeInstanceOf(Function);
    }
  }, 20000);
});
