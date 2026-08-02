import request from './request'

export interface LoginParams {
  username: string
  password: string
}

export interface RegisterParams extends LoginParams {
  email: string
}

export interface LoginResult {
  token: string
  username: string
  nickname?: string
}

export interface UserInfo {
  username: string
  nickname?: string
}

export function login(data: LoginParams) {
  return request.post<LoginResult>('/auth/login', data)
}

export function register(data: RegisterParams) {
  return request.post<null>('/auth/register', data)
}

export function logout() {
  return request.post<null>('/auth/logout')
}
