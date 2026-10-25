import { HttpContextToken } from '@angular/common/http';

/** A PDF denial is reported beside its download action; other API navigation stays unchanged. */
export const PDF_DOWNLOAD_REQUEST = new HttpContextToken<boolean>(() => false);
