// Payload decoding is for session UX only; the backend validates the signature.
export function jwtExpiration(token: unknown): number | null {
  if (typeof token !== 'string') return null;
  const parts = token.split('.');
  if (parts.length !== 3 || parts.some((part) => !/^[A-Za-z0-9_-]+$/.test(part))) return null;
  try {
    const decode = (part: string) => {
      const base64 = part.replace(/-/g, '+').replace(/_/g, '/');
      const bytes = Uint8Array.from(atob(base64.padEnd(Math.ceil(base64.length / 4) * 4, '=')), (c) => c.charCodeAt(0));
      return JSON.parse(new TextDecoder('utf-8', { fatal: true }).decode(bytes));
    };
    const header = decode(parts[0]);
    if (typeof header?.alg !== 'string' || !header.alg) return null;
    const payload = decode(parts[1]);
    const exp = payload?.exp;
    return typeof exp === 'number' && Number.isFinite(exp) && exp > 0 && Number.isFinite(exp * 1000)
      ? exp * 1000 : null;
  } catch {
    return null;
  }
}
