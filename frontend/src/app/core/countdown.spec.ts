import { formatCountdown, progressPercent, progressPercentFromDuration } from './countdown';

describe('formatCountdown', () => {
  it('returns an empty string when there is no end time', () => {
    expect(formatCountdown(null)).toBe('');
  });

  it('returns "Finishing…" once the end time has passed', () => {
    expect(formatCountdown(new Date(Date.now() - 1000).toISOString())).toBe('Finishing…');
  });

  it('formats remaining time as m:ss with zero-padded seconds', () => {
    const endsAt = new Date(Date.now() + 65_000).toISOString();
    expect(formatCountdown(endsAt)).toBe('1:05');
  });
});

describe('progressPercent', () => {
  it('returns 0 when there is no end time', () => {
    expect(progressPercent(new Date().toISOString(), null)).toBe(0);
  });

  it('returns 100 when total duration is zero or negative (clock skew)', () => {
    const now = new Date().toISOString();
    expect(progressPercent(now, now)).toBe(100);
  });

  it('clamps to 0 when elapsed time is negative (start is in the future)', () => {
    const start = new Date(Date.now() + 10_000).toISOString();
    const end = new Date(Date.now() + 20_000).toISOString();
    expect(progressPercent(start, end)).toBe(0);
  });

  it('clamps to 100 when elapsed exceeds total duration', () => {
    const start = new Date(Date.now() - 20_000).toISOString();
    const end = new Date(Date.now() - 10_000).toISOString();
    expect(progressPercent(start, end)).toBe(100);
  });

  it('computes a proportional percentage between start and end', () => {
    const start = new Date(Date.now() - 5_000).toISOString();
    const end = new Date(Date.now() + 5_000).toISOString();
    const result = progressPercent(start, end);
    expect(result).toBeGreaterThan(40);
    expect(result).toBeLessThan(60);
  });
});

describe('progressPercentFromDuration', () => {
  it('returns 0 when there is no end time', () => {
    expect(progressPercentFromDuration(null, 100)).toBe(0);
  });

  it('returns 0 when totalSeconds is zero or negative', () => {
    expect(progressPercentFromDuration(new Date().toISOString(), 0)).toBe(0);
  });

  it('derives the start time from endsAt minus totalSeconds', () => {
    const endsAt = new Date(Date.now() + 5_000).toISOString();
    const result = progressPercentFromDuration(endsAt, 10);
    expect(result).toBeGreaterThan(40);
    expect(result).toBeLessThan(60);
  });
});
