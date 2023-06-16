import request from '@/utils/request'

//获取系列信息
export function getSeriesBrief(params) {
    return request({
      url: '/works/getSeriesBrief',
      method: 'get',
      params: params
    })
}

//添加系列
export function addSeries(data) {
  return request({
    url: '/works/addSeries',
    method: 'post',
    data: data
  })
}

//修改系列
export function updateSeries(data) {
  return request({
    url: '/works/updateSeries',
    method: 'post',
    data: data
  })
}

//删除系列
export function delSeries(data) {
  return request({
    url: '/works/delSeries',
    method: 'post',
    data: data
  })
}



//获取帖子简略信息
export function getPiecesBrief(params) {
  return request({
    url: '/works/getPiecesBrief',
    method: 'get',
    params: params
  })
}

//获取帖子详细信息
export function getPiecesDetail(params) {
  return request({
    url: '/works/getPiecesDetail',
    method: 'get',
    params: params
  })
}

