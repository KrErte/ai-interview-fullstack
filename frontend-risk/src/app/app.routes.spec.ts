import { appRoutes } from './app.routes';
import { authGuard } from './guards/auth.guard';
import { RefundPageComponent } from './pages/legal/refund.page';
import { TermsPageComponent } from './pages/legal/terms.page';
import { RoadmapPageComponent } from './pages/roadmap/roadmap.page';

describe('appRoutes (FP-63)', () => {
  const route = (path: string) => appRoutes.find(r => r.path === path);

  it('lazy-loads /terms, /refund and /roadmap', async () => {
    expect(await route('terms')!.loadComponent!()).toBe(TermsPageComponent);
    expect(await route('refund')!.loadComponent!()).toBe(RefundPageComponent);
    expect(await route('roadmap')!.loadComponent!()).toBe(RoadmapPageComponent);
  });

  it('protects /roadmap with the auth guard and gives every new route a title', () => {
    expect(route('roadmap')!.canActivate).toEqual([authGuard]);
    expect(route('refund')!.title).toBeTruthy();
    expect(route('roadmap')!.title).toBeTruthy();
  });

  it('keeps the wildcard last', () => {
    expect(appRoutes[appRoutes.length - 1].path).toBe('**');
  });
});
