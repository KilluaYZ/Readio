import request from '@/utils/request'
// import { resolve } from 'core-js/fn/promise'

//获取文件信息
export function getFileInfo(params) {
    return request({
      url: '/file/getFileInfo',
      method: 'get',
      params: params
    })
  }

//获取文件二进制
export function getFileBinaryById(params) {
  return request({
    url: '/file/getFileBinaryById',
    method: 'get',
    params: params,
    responseType: 'blob'
  })
}

//获取图片url
export function getImgUrl(fileId) {
  return getFileBinaryById(fileId).then((res) => {
    console.log('getImgUrl :: res = ')
    console.log(res)
    const imgUrl = window.URL.createObjectURL(new window.Blob([res]))
    return Promise.resolve(imgUrl)
  })
}

//修改文件信息
export function updateFileInfo(data) {
  return request({
    url: '/file/updateFileInfo',
    method: 'post',
    data: data
  })
}

//上传文件
export function uploadFile(data) {
  return request({
    url: '/file/uploadFile',
    method: 'post',
    data: data
  })
}

//删除文件
export function delFile(params) {
  return request({
    url: '/file/delFile',
    method: 'get',
    params: params
  })
}