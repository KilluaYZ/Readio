import request from '@/utils/request'
import { useMock } from '../settings'
import { TookenData,UserData } from '../mock/data'
// 登录方法
export function login(phoneNumber, passWord, code, uuid) {
  const data = {
    phoneNumber,
    passWord,
    code,
    uuid
  }

  return false ? new Promise((resolve, reject) => {
    setTimeout(() => {
      resolve(TookenData)
    },100)
  }) : request({
    url: 'app/auth/login',
    headers: {
      isToken: false
    },
    method: 'post',
    data: data
  })

}

// 注册方法
export function register(data) {
  return request({
    url: 'app/auth/register',
    headers: {
      isToken: false
    },
    method: 'post',
    data: data
  })
}

// 获取用户详细信息
export function getInfo() {
  console.log('enter getInfo')
  return false ?
    new Promise((resolve, reject) => {
      setTimeout(() => {
        console.log('enter getInfo Promise')
        resolve(UserData)
      },100)
    })
    : request({
      url: 'app/auth/profile',
      method: 'get',
      headers:{
        isRepeatSubmit: false
      }
    })
}

// 退出方法
export function logout() {
  return request({
    url: 'app/auth/logout',
    method: 'get'
  })
}

// 获取验证码
export function getCodeImg() {
  return request({
    url: 'app/auth/captchaImage',
    headers: {
      isToken: false
    },
    method: 'get',
    timeout: 20000
  })
}