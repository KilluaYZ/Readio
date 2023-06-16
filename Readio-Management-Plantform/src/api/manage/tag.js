import request from '@/utils/request'
import { useMock } from '@/settings'
import {getMockTagData,UpdateData,DelData,AddData,frontTagTreeData} from '@/mock/data'


//查询tag
export function getTag(params) {
  return request({
    url: '/works/tag/get',
    method: 'get',
    params: params
  })
}

//修改tag
export function updateTag(data) {
  return request({
    url: '/works/tag/update',
    method: 'post',
    data: data
  })
}

//删除tag
export function delTag(params) {
  return request({
    url: '/works/tag/del',
    method: 'get',
    params: params
  })
}

//添加tag
export function addTag(data) {
  return request({
    url: '/works/tag/add',
    method: 'post',
    data: data
  })
}


//获取该tag对应的所有series
export function getAllTagSeries(params) {
  return request({
    url: '/works/tag/getAllTagSeries',
    method: 'get',
    params: params
  })
}

//删除该tag对应的的series的联系
export function delTagSeriesRelation(params) {
  return request({
    url: '/works/tag/delSeriesRelation',
    method: 'get',
    params: params
  })
}

//添加该tag对应的的series的联系
export function addTagSeriesRelation(params) {
  return request({
    url: '/works/tag/addSeriesRelation',
    method: 'get',
    params: params
  })
}