import { HttpErrorResponse } from '@angular/common/http';

/** A transport/server failure cannot prove that the stored session was revoked. */
export function isSessionUnavailable(error: unknown): error is HttpErrorResponse {
  return error instanceof HttpErrorResponse && (error.status === 0 || error.status >= 500);
}
