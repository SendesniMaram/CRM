// Only implemented private routes are accepted. Navigation stays internal.
export function safeReturnUrl(value: string | null | undefined): string {
  if (!value || /[\\\x00-\x20]/.test(value)) return '/dashboard';
  const path = value.split(/[?#]/, 1)[0];
  return /^\/(dashboard|hr|employees(?:\/(?:new|[1-9]\d*(?:\/edit)?))?|departments(?:\/(?:new|[1-9]\d*\/edit))?|leaves(?:\/(?:new|[1-9]\d*\/edit))?)$/.test(path)
    ? value : '/dashboard';
}
