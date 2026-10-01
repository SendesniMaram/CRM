// Only implemented private routes are accepted. Navigation stays internal.
export function safeReturnUrl(value: string | null | undefined): string {
  if (!value || /[\\\x00-\x20]/.test(value)) return '/dashboard';
  const path = value.split(/[?#]/, 1)[0];
  return /^\/(dashboard|employees(?:\/(?:new|[1-9]\d*(?:\/edit)?))?)$/.test(path)
    ? value : '/dashboard';
}
