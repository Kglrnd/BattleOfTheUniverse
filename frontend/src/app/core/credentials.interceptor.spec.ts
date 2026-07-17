import { HttpRequest } from '@angular/common/http';

import { credentialsInterceptor } from './credentials.interceptor';

describe('credentialsInterceptor', () => {
  it('adds withCredentials to /api requests', () => {
    const req = new HttpRequest('GET', '/api/version');
    const next = vi.fn((r) => r);

    credentialsInterceptor(req, next);

    const forwarded = next.mock.calls[0][0] as HttpRequest<unknown>;
    expect(forwarded.withCredentials).toBe(true);
  });

  it('passes non-/api requests through unchanged', () => {
    const req = new HttpRequest('GET', '/i18n/en.json');
    const next = vi.fn((r) => r);

    credentialsInterceptor(req, next);

    expect(next).toHaveBeenCalledWith(req);
  });
});
