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
