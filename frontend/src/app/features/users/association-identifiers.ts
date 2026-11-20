import { HttpErrorResponse } from '@angular/common/http';

export function canonicalId(value: unknown): value is string {
  return typeof value === 'string' && /^[1-9][0-9]{0,18}$/.test(value)
    && (value.length < 19 || value <= '9223372036854775807');
}

/** Preserve integer id tokens from legacy numeric directories before JSON.parse can round them. */
export function parseDirectory(text: string): any {
  try {
    let idStart = -1;
    const preserved = text.replace(/"(?:\\[\s\S]|[^"\\])*"|-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?/g,
      (token: string, offset: number) => {
        if (offset === idStart) { idStart = -1; return token.startsWith('"') ? token : JSON.stringify(token); }
        if (token.startsWith('"') && JSON.parse(token) === 'id') {
          const suffix = /^\s*:\s*/.exec(text.slice(offset + token.length));
          if (suffix) idStart = offset + token.length + suffix[0].length;
        }
        return token;
      });
    return JSON.parse(preserved);
  } catch { throw new HttpErrorResponse({ status: 502 }); }
}
