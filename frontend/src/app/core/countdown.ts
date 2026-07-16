/** Formats the time remaining until `endsAt` as `m:ss`, or `Finishing…` once it has passed. */
export function formatCountdown(endsAt: string | null): string {
  if (!endsAt) {
    return '';
  }
  const remainingMs = new Date(endsAt).getTime() - Date.now();
  if (remainingMs <= 0) {
    return 'Finishing…';
  }
  const totalSeconds = Math.ceil(remainingMs / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}:${seconds.toString().padStart(2, '0')}`;
}

/** Percentage (0-100) elapsed between `startedAt` and `endsAt`, clamped for clock skew. */
export function progressPercent(startedAt: string, endsAt: string | null): number {
  if (!endsAt) {
    return 0;
  }
  const start = new Date(startedAt).getTime();
  const end = new Date(endsAt).getTime();
  const total = end - start;
  if (total <= 0) {
    return 100;
  }
  const elapsed = Date.now() - start;
  return Math.min(100, Math.max(0, (elapsed / total) * 100));
}

/**
 * Percentage (0-100) elapsed for a job whose start time isn't tracked server-side, derived
 * from its known total duration (`endsAt - totalSeconds` stands in for the start time).
 */
export function progressPercentFromDuration(endsAt: string | null, totalSeconds: number): number {
  if (!endsAt || totalSeconds <= 0) {
    return 0;
  }
  const startedAt = new Date(new Date(endsAt).getTime() - totalSeconds * 1000).toISOString();
  return progressPercent(startedAt, endsAt);
}
