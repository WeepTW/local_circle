export interface Credentials { username: string; password: string }
export interface LoginSession { userId: string }
export interface LoginSuggestion extends Credentials { id: string; label: string }
export interface LoginProvider {
  readonly suggestions?: readonly LoginSuggestion[];
  authenticate(credentials: Credentials): Promise<LoginSession>;
}
