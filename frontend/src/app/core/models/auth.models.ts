import { CrmRole } from './role.model';

export interface LoginRequest {
  username: string | null;
  email: string | null;
  password: string;
}

export interface LoginResponse {
  username: string;
  email: string;
  enabled: boolean;
  token: string;
  refreshToken: string;
  type: string;
  role: CrmRole;
  roles: CrmRole[];
  expiration: number;
}

export interface AuthSession {
  accessToken: string;
  refreshToken: string;
  user: Omit<LoginResponse, 'token' | 'refreshToken'>;
}
