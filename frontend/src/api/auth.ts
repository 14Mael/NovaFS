import request from './request'

export interface LoginParams {
  username: string
  password: string
}

export interface RegisterParams extends LoginParams {
  email: string
}

export interface UserInfo {
  id: number
  username: string
  nickname?: string
  email?: string
}

export interface LoginResult {
  token: string
  user: UserInfo
}

export function login(data: LoginParams) {
  return request.post<LoginResult>('/auth/login', data)
}

export function register(data: RegisterParams) {
  return request.post<null>('/auth/register', data)
}