"""
二创管理
"""
import functools
import random
from typing import List

from flask import request
from flask import Blueprint
from flask import redirect
from flask import url_for
from werkzeug.security import check_password_hash, generate_password_hash
from readio.utils.buildResponse import *
from readio.utils.auth import *
import readio.database.connectPool
import readio.utils.check as check
from readio.utils.myExceptions import *
from readio.utils.executeSQL import *
from readio.manage.userManage import get_user_by_id_aux
from readio.mainpage.appBookDetailsPage import get_sub_comment_ids_stack, check_comment_liked, get_comment_details, check_comment_liked

# appAuth = Blueprint('/auth/app', __name__)
bp = Blueprint('worksManage', __name__, url_prefix='/works')

pooldb = readio.database.connectPool.pooldb


def __random_get_pieces_brief_sql(size: int) -> list:
    sql = 'select piecesId from pieces'
    ids = execute_sql_query(pooldb, sql)
    # 随机从列表中抽取size个元素
    if size > len(ids):
        size = len(ids)
    ids = list(map(lambda x: x["piecesId"], ids))
    rand_ids = random.sample(ids, size)
    id_string = ','.join(str(i) for i in rand_ids)
    sql = f'select piecesId, seriesId, title, userId, status, content, collect, likes, views, shares from pieces ' \
          f'where piecesId in ({id_string})'
    rows = execute_sql_query(pooldb, sql)

    return rows

def __recommend_pieces_sql(idx: int,size: int) -> list:
    sql = 'select piecesId, seriesId, title, userId, status, content, collect, likes, views, shares from pieces order by updateTime desc limit %s, %s '
    print(f'[DEBUG] idx = {idx}, size = {size}')
    offset = int(idx) * int(size)
    rows = execute_sql_query(pooldb, sql, (offset, size))
    return rows


def __get_tags_by_seriesId_sql(seriesId: int, mode='default') -> list:
    try:
        # print(f'[DEBUG] seriesId = {seriesId} type = {type(seriesId)}')
        conn, cursor = pooldb.get_conn()
        cursor.execute('select tags.tagId as tagId, tags.content as content, linkedTimes from tags, tag_series where '
                       'tag_series.seriesId = %s  and tag_series.tagId = tags.tagId', seriesId)

        rows = cursor.fetchall()
        if mode == 'default':
            pass
        elif mode == 'hot':
            max_linked_id = 0
            max_linked_tag = None
            for tag in rows:
                if tag['linkedTimes'] >= max_linked_id:
                    max_linked_id = tag['linkedTimes']
                    max_linked_tag = tag
            if max_linked_tag is not None:
                rows = [max_linked_tag]
            else:
                rows = []

        for i in range(len(rows)):
            rows[i]['type'] = 'primary'
        # print(f'[DEBUG] rows = {rows}')

        return rows

    except Exception as e:
        check.printException(e)
        raise e
    finally:
        if conn is not None:
            pooldb.close_conn(conn, cursor)


def __query_pieces_sql(query_param: dict) -> List[Dict]:
    sql_select = 'select DISTINCT pieces.piecesId as piecesId, pieces.seriesId as seriesId, ' \
                 'pieces.title as title, pieces.userId as userId, pieces.content as content, ' \
                 'pieces.createTime as createTime, pieces.updateTime as updateTime, ' \
                 'pieces.status as status, pieces.likes as likes, pieces.views as views, ' \
                 'pieces.shares as shares, pieces.collect as collect from pieces '

    args_str_list = []
    args_val_list = []

    if 'piecesId' in query_param:
        args_str_list.append(f' and piecesId = %s ')
        args_val_list.append(query_param['piecesId'])
    if 'title' in query_param:
        args_str_list.append(f' and title like %s ')
        args_val_list.append(f'%{query_param["title"]}%')
    if 'content' in query_param:
        args_str_list.append(f' and content like %s ')
        args_val_list.append(f'%{query_param["content"]}%')
    if 'userId' in query_param or 'userName' in query_param:
        sql_select += " , users "
        args_str_list.append(f' and users.id = pieces.userId ')
        if 'userName' in query_param:
            args_str_list.append(f' and users.userName like %s ')
            args_val_list.append(f'%{query_param["userName"]}%')
        if 'userId' in query_param:
            args_str_list.append(f' and users.id = %s ')
            args_val_list.append(f'{query_param["userId"]}')

    sql = sql_select
    if len(args_str_list):
        sql += ' where 1=1 '
        
    for item in args_str_list:
        sql += item
    
    if 'sortMode' in query_param:
        if query_param['sortMode'] == 'hot':
            sql += ' order by pieces.likes desc '
        elif query_param['sortMode'] == 'new':
            sql += ' order by pieces.updateTime desc '    

    # print(f'[DEBUG] sql = {sql}')
    rows = execute_sql_query(pooldb, sql, tuple(args_val_list))
    # print(f'[DEBUG] rows = {rows}')
    return rows


def __search_pieces_sql(keyword: str):
    sql = 'select distinct * from pieces, users where pieces.status = 1 and pieces.userId = users.id and (pieces.title like %s or users.userName like %s) order by pieces.updateTime desc '
    print(f'[DEBUG] sql = {sql}')
    rows = execute_sql_query(pooldb, sql, (f"%{keyword}%", f"%{keyword}%"))
    return rows

def __get_all_pieces_count() -> int:
    sql = 'select COUNT(*) from pieces'
    row = execute_sql_query_one(pooldb, sql)
    count = int(row['COUNT(*)'])
    return count


@bp.route('/getPiecesBrief', methods=['GET'])
def get_bref():
    """
    获取一章简略信息
    """
    try:
        mode = request.args.get('mode')
        if mode is None:
            mode = 'recommend'
            
        if mode == 'random': 
            size = request.args.get('size')
            if size is None:
                size = 15
            rows = __random_get_pieces_brief_sql(size)

        elif mode == 'query':
            rows = __query_pieces_sql(request.args)
            print(f'[DEBUG] 拿到了pieces数据，共{len(rows)}条')
            pageSize = request.args.get('pageSize')
            pageNum = request.args.get('pageNum')
            if pageNum is not None and pageSize is not None:
                pageSize = int(pageSize)
                pageNum = int(pageNum)
                rows = rows[pageSize * (pageNum - 1): pageSize * pageNum]
            print('[DEBUG] 分页完成')
        elif mode == 'recommend':
            """
            启用推荐方式，每次获取前端的当前访问次数，然后根据访问次数确定推荐内容
            如果没有给访问次数，则按照随机推荐
            """
            queryTimes = request.args.get('queryTimes')
            if queryTimes is None:
                # 按照随机方式推荐
                rows = __random_get_pieces_brief_sql(15)
            else:    
                # 每次推送15个，其中三个随机
                new_rows = __recommend_pieces_sql(queryTimes, 12)
                if len(new_rows) <= 0:
                    rand_rows = __random_get_pieces_brief_sql(15)
                else:
                    rand_rows = __random_get_pieces_brief_sql(3)
                rows = []
                for row in new_rows:
                    rows.append(row)
                for row in rand_rows:
                    rows.append(row)
            
        elif mode == 'search':
            """
            启用搜索方式，用或运算查询所有符合的项
            """
            print(f'[DEBUG] 进入search分支')
            keyword = request.args.get('keyword')
            rows = __search_pieces_sql(keyword)

        # 查找最热门的标签
        for i in range(len(rows)):
            max_linked_tag = __get_tags_by_seriesId_sql(int(rows[i]['seriesId']), 'hot')
            if len(max_linked_tag):
                rows[i]['tag'] = max_linked_tag[0]
            else:
                rows[i]['tag'] = None

        # 查找对应的series的详细信息
        for i in range(len(rows)):
            seriesId = rows[i].get('seriesId')
            if seriesId is not None:
                series = __query_series_brief_sql({"seriesId": seriesId})
                if series is not None and len(series):
                    rows[i]['series'] = series[0]
                tag_list = __get_tags_by_seriesId_sql(seriesId)
                rows[i]['tag'] = tag_list

        # 查找对应的用户
        for i in range(len(rows)):
            rows[i]['user'] = get_user_by_id(rows[i]['userId'])

        # 缩短content
        for i in range(len(rows)):
            rows[i]['content'] = rows[i]['content'][:40]

        # 获取用户点赞收藏情况
        # 先设置未点赞，未收藏
        for i in range(len(rows)):
            rows[i]["isLiked"] = 0
            rows[i]["isCollected"] = 0
        try:
            # 尝试获取user点赞信息
            user = check_user_before_request(request)
            userId = user['id']
            user_all_liked_pieces_id_list = __get_all_like_pieces_id_by_userid(userId)
            user_all_collected_pieces_id_list = __get_all_collect_pieces_id_by_userid(userId)
            # print(f'[DEBUG] user_all_liked_pieces_id_list = {user_all_liked_pieces_id_list}')
            # print(f'[DEBUG] user_all_collected_pieces_id_list = {user_all_collected_pieces_id_list}')
            for i in range(len(rows)):
                if int(rows[i]['piecesId']) in user_all_liked_pieces_id_list:
                    # print(f'[DEBUG] enter like')
                    # 在用户喜欢的列表里
                    rows[i]["isLiked"] = 1
                if int(rows[i]['piecesId']) in user_all_collected_pieces_id_list:
                    # print(f'[DEBUG] enter collect')
                    # 在用户喜欢的列表里
                    rows[i]["isCollected"] = 1

        except NetworkException:
            print("用户未登录或不存在，不返回点赞收藏信息")
        except Exception as e:
            raise e

        return build_success_response(rows)

    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)

    except Exception as e:
        check.printException(e)
        return build_error_response(code=500, msg='服务器内部错误')


def __query_series_brief_sql(query_param: dict) -> List[dict]:
    sql_from_table = 'select distinct series.seriesId as seriesId, seriesName, userId, isFinished, abstract, likes, views, shares, collect, series.createTime as createTime from series '
    arg_list = []
    sql = sql_from_table
    if 'seriesName' in query_param or 'seriesId' in query_param or 'seriesTag' in query_param:
        sql_where = ' where 1=1 '
        if 'seriesName' in query_param:
            sql_where += ' and seriesName like %s '
            arg_list.append(f'%{query_param["seriesName"]}%')
        if 'seriesId' in query_param:
            sql_where += ' and seriesId = %s '
            arg_list.append(query_param['seriesId'])
        if 'seriesTag' in query_param:
            sql_from_table += ' , tag_series, tags '
            sql_where += ' and tag_series.seriesId = series.seriesId and tag_series.tagId = tags.tagId and tags.content like %s '
            arg_list.append(f"%{query_param['seriesTag']}%")

        sql = sql_from_table + sql_where

    if 'sortMode' in query_param:
        if query_param['sortMode'] == 'Hot':
            sql += ' order by series.likes desc '
        elif query_param['sortMode'] == 'New':
            sql += ' order by series.createTime desc '
        else:
            sql += ' order by series.seriesName asc '
    else:
        sql += ' order by series.seriesName asc '

    # print(f'[DEBUG] sql = {sql}')
    rows = execute_sql_query(pooldb, sql, tuple(arg_list))

    return rows


@bp.route('/getSeriesBrief', methods=['GET'])
def get_series_brief():
    """
    获取系列简略信息
    """
    try:
        query_param = {}
        seriesName = request.args.get('seriesName')
        seriesId = request.args.get('seriesId')
        seriesTag = request.args.get('seriesTag')
        sortMode = request.args.get('sortMode')
        if seriesName is not None:
            query_param['seriesName'] = seriesName
        if seriesId is not None:
            query_param['seriesId'] = seriesId
        if seriesTag is not None:
            query_param['seriesTag'] = seriesTag
        if sortMode is not None:
            query_param['sortMode'] = sortMode
        rows = __query_series_brief_sql(query_param)

        length = len(rows)
        # 如果前端传来了pageSize和pageNum则说明需要分页
        pageSize = request.args.get('pageSize')
        pageNum = request.args.get('pageNum')
        if pageNum is not None and pageSize is not None:
            pageSize = int(pageSize)
            pageNum = int(pageNum)
            rows = rows[(pageNum - 1) * pageSize:pageNum * pageSize]

        for i in range(len(rows)):
            rows[i]['user'] = get_user_by_id(rows[i]['userId'])
            rows[i]['tag'] = __get_tags_by_seriesId_sql(rows[i]['seriesId'])

        return build_success_response(data=rows, length=length)

    except Exception as e:
        check.printException(e)
        return build_error_response(code=500, msg='服务器内部错误')


def __get_pieces_by_id_sql(piecesId: int) -> dict:
    # return execute_sql_query_one(pooldb,
    #     'select pieces.piecesId as piecesId, pieces.seriesId as seriesId, pieces.title as title, pieces.userId as '
    #     'userId,  pieces.content as content, pieces.createTime as createTime, pieces.updateTime as updateTime, '
    #     'pieces.status as status, pieces.likes as likes, pieces.views as views, pieces.shares as shares, '
    #     'series.seriesName as seriesName from pieces, series where piecesId = %s and pieces.seriesId = '
    #     'series.seriesId ',
    #     piecesId)
    return execute_sql_query_one(pooldb, 'select * from pieces where piecesId = %s', piecesId)


def __get_pieces_by_id_aux(piecesId: int, request=None) -> dict:
    piece = __get_pieces_by_id_sql(piecesId)
    if piece is None:
        return None

    # 获取用户点赞收藏情况
    # 先设置未点赞，未收藏
    piece["isLiked"] = 0
    piece["isCollected"] = 0
    if request is not None:
        try:
            # 尝试获取user点赞信息
            user = check_user_before_request(request)
            userId = user['id']
            user_all_liked_pieces_id_list = __get_all_like_pieces_id_by_userid(userId)
            user_all_collected_pieces_id_list = __get_all_collect_pieces_id_by_userid(userId)
            # print(f'[DEBUG] user_all_liked_pieces_id_list = {user_all_liked_pieces_id_list}')
            # print(f'[DEBUG] user_all_collected_pieces_id_list = {user_all_collected_pieces_id_list}')

            if int(piece['piecesId']) in user_all_liked_pieces_id_list:
                # print(f'[DEBUG] enter like')
                # 在用户喜欢的列表里
                piece["isLiked"] = 1
            if int(piece['piecesId']) in user_all_collected_pieces_id_list:
                # print(f'[DEBUG] enter collect')
                # 在用户喜欢的列表里
                piece["isCollected"] = 1

        except NetworkException:
            print("用户未登录或不存在，不返回点赞收藏信息")
        except Exception as e:
            raise e

    return piece


@bp.route('/getPiecesDetail', methods=['GET'])
def get_pieces_detail():
    """
    获取一章详细信息
    """
    try:
        data = request.args
        if 'piecesId' not in data:
            raise NetworkException(400, '传入数据错误，未包含piecesId')

        pieceId = int(data['piecesId'])
        piece = __get_pieces_by_id_aux(pieceId, request)
        if piece is None:
            raise NetworkException(404, '该章节不存在')
        tag_list = __get_tags_by_seriesId_sql(piece['seriesId'])
        piece['tag'] = tag_list
        piece['user'] = get_user_by_id_aux(piece['userId'], request)
        piece['series'] = {}
        if 'seriesId' in piece and piece['seriesId'] is not None:
            seriesList = __query_series_brief_sql({"seriesId": piece['seriesId']})
            if seriesList is not None and len(seriesList):
                piece['series'] = seriesList[0]
        try:
            user = check_user_before_request(request)
        except Exception as e:
            user = None
        pieces_comments = __pieces_get_comments_detail_one_depth_reply(pieceId, user)
        piece['comments'] = pieces_comments

        return build_success_response(piece)

    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)
    except Exception as e:
        check.printException(e)
        return build_error_response(code=500, msg='服务器内部错误')


@bp.route('/getSeriesDetail', methods=['GET'])
def get_series_detail():
    """
    获取系列详细信息
    """
    return build_success_response()


def __get_series_by_user_id(user_id: int) -> list:
    return execute_sql_query(pooldb, "select * from series where userId = %s", user_id)


@bp.route('/getUserSeriesList', methods=['GET'])
def get_user_series_list():
    """
    获取属于用户的所有系列的列表
    """
    try:
        user = check_user_before_request(request)
        series_list = __get_series_by_user_id(user['id'])

        return build_success_response(series_list)

    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)
    except Exception as e:
        check.printException(e)
        return build_error_response(code=500, msg='服务器内部错误')


@bp.route('/getUserPiecesList', methods=['GET'])
def get_user_pieces_list():
    """
    获取属于用户的所有篇章的列表（不包括内容）
    """
    try:
        user = check_user_before_request(request)
        rows = __query_pieces_sql({"userId": user['id'], "sortMode":"new"})

        for i in range(len(rows)):
            rows[i]['comment'] = 123
            seriesId = rows[i].get('seriesId')
            if seriesId is not None:
                series = __query_series_brief_sql({"seriesId": seriesId})
                if series is not None and len(series):
                    rows[i]['series'] = series[0]
                tag_list = __get_tags_by_seriesId_sql(seriesId)
                rows[i]['tag'] = tag_list

        return build_success_response(rows, length=len(rows))

    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)
    except Exception as e:
        check.printException(e)
        return build_error_response(code=500, msg='服务器内部错误')


def __add_pieces_sql(data: dict, trans=None) -> int:
    try:
        sql = 'insert into pieces('
        args_list = []

        # 系列id
        sql += ' seriesId '
        args_list.append(data['seriesId'])
        value_sql = ' %s '

        # 用户名
        if 'userId' in data:
            sql += ' ,userId '
            args_list.append(data['userId'])
            value_sql += ' ,%s '

        if 'content' in data:
            sql += ' ,content '
            args_list.append(data['content'])
            value_sql += ' ,%s '

        if 'status' in data:
            sql += ' ,status '
            args_list.append(data['status'])
            value_sql += ' ,%s '

        if 'piecesTitle' in data:
            sql += ' ,title '
            args_list.append(data['piecesTitle'])
            value_sql += ' ,%s '

        sql += f') values({value_sql})'

        if trans is None:
            id_ = execute_sql_write(pooldb, sql, tuple(args_list))
        else:
            id_ = trans.execute(sql, tuple(args_list))
        return id_

    except Exception as e:
        check.printException(e)
        raise e


@bp.route('/addPieces', methods=['POST'])
def add_pieces():
    """
    添加一章
    """
    try:
        msg = '发布成功'
        data = request.json
        if "piecesTitle" not in data or "content" not in data or "status" not in data:
            print(
                f'[DEBUG] 前端数据错误 piecesTitle: {"piecesTitle" in data} content: {"content" in data} status: {"status" in data}')
            raise NetworkException(400, "前端数据错误，必须包含piecesTitle、content、status")

        if 'seriesId' not in data and 'seriesName' not in data:
            print(
                f'[DEBUG] 前端数据错误 seriesId: {"seriesId" in data} seriesName: {"seriesName" in data}')
            raise NetworkException(400, "前端数据错误，必须包含seriesId、seriesName其中之一")

        # 检查权限
        if 'userId' in data:
            user = check_user_before_request(request, roles='common')
            if user['id'] != data['userId']:
                user = check_user_before_request(request, roles='manager')
        else:
            user = check_user_before_request(request, roles='common')

        data['userId'] = user['id']

        # 开启事务
        trans = SqlTransaction(pooldb)
        trans.begin()
        seriesId = data.get("seriesId")
        if seriesId is None or len(seriesId) == 0:
            # 妹有传来id说明不存在该series，是要添加series
            add_series_param = {}
            add_series_param['userId'] = user['id']
            add_series_param['seriesName'] = data['seriesName']
            seriesId = __add_series_sql(add_series_param, trans)
            data['seriesId'] = seriesId

        print(f'[DEBUG] tagNameList: {data["tagNameList"]} tagIdList: {data["tagIdList"]}')
        if 'tagNameList' in data and 'tagIdList' in data:
            tagNameList = data['tagNameList']
            tagIdList = data['tagIdList']
            for i in range(len(tagIdList)):
                tagId = tagIdList[i]
                if tagId is not None and len(tagId) > 0:
                    __add_tag_series_relation_sql(tagId, seriesId, trans)
                else:
                    tagName = tagNameList[i]
                    tagObjs = __get_tag_by_content(tagName)
                    if tagObjs is not None and len(tagObjs):
                        newAddedTagId = tagObjs[0]["tagId"]
                    else:
                        newAddedTagId = __add_tag(tagName, trans)
                    tags_relation = execute_sql_query_one(pooldb,
                                                          'select * from tag_series where tagId=%s and seriesId = %s',
                                                          (newAddedTagId, seriesId))
                    if tags_relation is None:
                        __add_tag_series_relation_sql(newAddedTagId, seriesId, trans)
                        # __update_tag_linked_times_sql(newAddedTagId, trans)
                    else:
                        msg = f'标签{tagName}重复添加'
        print(f'[DEBUG] content = {data["content"]}')

        __add_pieces_sql(data, trans)

        trans.commit()

        return build_success_response(msg=msg)
    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)

    except Exception as e:
        check.printException(e)
        return build_error_response(code=500, msg='服务器内部错误')


def __add_series_sql(data: dict, trans=None):
    try:
        sql = 'insert into series('
        args_list = []

        # 系列名
        sql += ' seriesName '
        args_list.append(data['seriesName'])
        value_sql = ' %s '

        # 用户名
        if 'userId' in data:
            sql += ' ,userId '
            args_list.append(data['userId'])
            value_sql += ' ,%s '

        if 'abstract' in data:
            sql += ' ,abstract '
            args_list.append(data['abstract'])
            value_sql += ' ,%s '
        sql += f') values({value_sql})'

        if trans is None:
            id_ = execute_sql_write(pooldb, sql, tuple(args_list))
        else:
            id_ = trans.execute(sql, tuple(args_list))
        return id_

    except Exception as e:
        check.printException(e)
        raise e


@bp.route('/addSeries', methods=['POST'])
def add_series():
    """
    添加系列
    """
    try:
        userId = request.json.get('userId')
        if userId is None:
            # 如果没有传进来userId则说明，这是用户本人想要在自己名下添加一条series
            user = check_user_before_request(request)
            userId = user['userId']
        else:
            # 如果传来了userId，则说明这是管理人员尝试向某个用户添加一条series
            user = check_user_before_request(request, roles='manager')

        seriesName = request.json.get('seriesName')
        if seriesName is None:
            raise NetworkException(code=400, msg='前端数据错误，缺少seriesName')

        trans = SqlTransaction(pooldb)
        trans.begin()
        seriesId = __add_series_sql(request.json, trans)
        # print(f'[DEBUG] seriesId = {seriesId}')

        tags_list = request.json.get('tag')
        if tags_list is not None:
            for tag in tags_list:
                __add_tag_series_relation_sql(tag['tagId'], seriesId, trans)
                # __update_tag_linked_times_sql(tag['tagId'], trans)

        trans.commit()

        return build_success_response(msg='添加系列成功')

    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)
    except Exception as e:
        check.printException(e)
        return build_error_response(code=500, msg='服务器内部错误')


def __del_pieces_sql(piecesId, trans=None):
    sql = 'delete from pieces where piecesId = %s'
    if trans is None:
        return execute_sql_write(pooldb, sql, piecesId)
    else:
        return trans.execute(sql, piecesId)


def __check_if_pieces_is_belong_to_user(piecesId, userId) -> bool:
    sql = 'select * from pieces where piecesId = %s and userId = %s'
    row = execute_sql_query_one(pooldb, sql, (piecesId, userId))
    if row is not None:
        return True
    return False


@bp.route('/delPieces', methods=['GET'])
def del_pieces():
    """
    删除一章
    """
    try:
        piecesId = request.args.get('piecesId')
        if piecesId is None:
            raise (400, "前端缺少重要参数piecesId")

        user = check_user_before_request(request, roles='common')

        if __check_if_pieces_is_belong_to_user(piecesId, user['id']):
            # 如果这个seires属于发出请求的用户，则可以操作
            __del_pieces_sql(piecesId)
        else:
            # 如果这个seires不属于发出请求的用户，则需要验证管理员身份
            check_user_before_request(request, roles='manager')
            __del_pieces_sql(piecesId)

        return build_success_response()

    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)
    except Exception as e:
        check.printException(e)
        return build_error_response(code=500, msg='服务器内部错误')


def __change_pieces_status(piecesId, trans=None):
    sql = 'select status from pieces where piecesId = %s'
    row = execute_sql_query_one(pooldb, sql, piecesId)
    if row is None:
        raise NetworkException(400, '对应的pieces不存在')

    status = int(row['status'])
    if status == 0:
        status = 1
    else:
        status = 0
    sql = 'update pieces set status = %s where piecesId = %s'

    if trans is None:
        return execute_sql_write(pooldb, sql, (status, piecesId))
    else:
        return trans.execute(sql, (status, piecesId))


@bp.route('/changePiecesStatus', methods=['GET'])
def change_pieces_status():
    """
    修改pieces状态
    """
    try:
        piecesId = request.args.get('piecesId')
        if piecesId is None:
            raise (400, "前端缺少重要参数piecesId")

        user = check_user_before_request(request, roles='common')

        if __check_if_pieces_is_belong_to_user(piecesId, user['id']):
            # 如果这个seires属于发出请求的用户，则可以操作
            __change_pieces_status(piecesId)
        else:
            # 如果这个seires不属于发出请求的用户，则需要验证管理员身份
            check_user_before_request(request, roles='manager')
            __change_pieces_status(piecesId)

        return build_success_response()

    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)
    except Exception as e:
        check.printException(e)
        return build_error_response(code=500, msg='服务器内部错误')


def __check_if_all_series_are_belong_to_user(seriesIdList: list, userId: str) -> bool:
    try:
        sql = 'select * from series where userId = %s'
        rows = execute_sql_query(pooldb, sql, userId)
        if rows is None or len(rows) <= 0:
            raise NetworkException(code=400, msg=f'没有任何系列属于ID为{userId}的用户')

        seriesIdListOriginSet = set(seriesIdList)
        seriesIdListNewSet = set(map(lambda x: x['seriesId'], rows))

        seriesCommon = list(seriesIdListOriginSet & seriesIdListNewSet)
        if len(seriesCommon) != len(seriesIdList):
            return False

        return True

    except Exception as e:
        check.printException(e)
        raise e


def __del_series_sql(seriesId: str):
    try:
        sql = 'delete from series where seriesId = %s'
        execute_sql_write(pooldb, sql, seriesId)

    except Exception as e:
        check.printException(e)
        raise e


@bp.route('/delSeries', methods=['POST'])
def del_series():
    """
    删除系列
    """
    try:
        seriesIdList = request.json.get('seriesIdList')
        if seriesIdList is None:
            raise NetworkException(code=400, msg='前端数据错误，缺少seriesIdList')
        # 先检查所有的series是不是都属于这个用户
        user = check_user_before_request(request, roles='common')

        if __check_if_all_series_are_belong_to_user(seriesIdList, user['id']):
            # 如果这个seires属于发出请求的用户，则可以操作
            for seriesId in seriesIdList:
                __del_series_sql(seriesId)
        else:
            # 如果这个seires不属于发出请求的用户，则需要验证管理员身份
            check_user_before_request(request, roles='manager')
            for seriesId in seriesIdList:
                __del_series_sql(seriesId)

        return build_success_response(msg='删除系列成功')

    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)
    except Exception as e:
        check.printException(e)
        return build_error_response(code=500, msg='服务器内部错误')


def __update_pieces_sql(piecesId, content, trans=None):
    sql = 'update pieces set content = %s, updateTime = now() where piecesId = %s'
    if trans is None:
        return execute_sql_write(pooldb, sql, (content, piecesId))
    else:
        return trans.execute(sql, (content, piecesId))


@bp.route('/updatePieces', methods=['POST'])
def update_pieces():
    """
    更新一章
    """
    try:
        piecesId = request.json.get("piecesId")
        content = request.json.get('content')
        if piecesId is None or content is None:
            raise NetworkException(400, '前端参数缺失，缺少piecesId或content')

        user = check_user_before_request(request, roles='common')

        if __check_if_pieces_is_belong_to_user(piecesId, user['id']):
            # 如果这个seires属于发出请求的用户，则可以操作
            __update_pieces_sql(piecesId, content)
        else:
            # 如果这个seires不属于发出请求的用户，则需要验证管理员身份
            check_user_before_request(request, roles='manager')
            __update_pieces_sql(piecesId, content)

        return build_success_response()

    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)
    except Exception as e:
        check.printException(e)
        return build_error_response(code=500, msg='服务器内部错误')


def __update_series_sql(data: dict):
    try:
        sql = 'update series set '
        args_list = []

        if 'seriesName' in data:
            args_list.append(['seriesName', data['seriesName']])
        if 'abstract' in data:
            args_list.append(['abstract', data['abstract']])
        if 'userId' in data:
            args_list.append(['userId', data['userId']])

        param_list = []
        if len(args_list):
            sql += f'{args_list[0][0]}=%s '
            param_list.append(args_list[0][1])
            for i in range(1, len(args_list)):
                sql += f' ,{args_list[i][0]}=%s '
                param_list.append(args_list[i][1])
        sql += ' where seriesId = %s'
        param_list.append(data['seriesId'])

        # seriesId = execute_sql_write(pooldb, sql, tuple(param_list))

        trans = SqlTransaction(pooldb)
        trans.begin()
        trans.execute(sql, tuple(param_list))

        if 'tag' in data:
            seriesId = data['seriesId']
            new_tags = data['tag']
            # 检查tag
            tags_list = __get_tags_by_seriesId_sql(seriesId)
            origin_tag_id_list = map(lambda x: x['tagId'], tags_list)
            new_tag_id_list = map(lambda x: x['tagId'], new_tags)
            origin_tag_id_set = set(origin_tag_id_list)
            new_tag_id_set = set(new_tag_id_list)
            tag_id_add_list = list(new_tag_id_set - origin_tag_id_set)
            tag_id_remove_list = list(origin_tag_id_set - new_tag_id_set)
            for tagId in tag_id_add_list:
                __add_tag_series_relation_sql(tagId, seriesId, trans)
                # __update_tag_linked_times_sql(tagId, trans)
            for tagId in tag_id_remove_list:
                __del_tag_series_relation_sql(tagId, seriesId, trans)
                # __update_tag_linked_times_sql(tagId, trans)

        trans.commit()

    except Exception as e:
        check.printException(e)
        raise e


@bp.route('/updateSeries', methods=['POST'])
def update_series():
    """
    更新系列
    """
    try:
        user = check_user_before_request(request, roles='manager')

        seriesId = request.json.get('seriesId')
        if seriesId is None:
            raise NetworkException(code=400, msg='前端数据错误，缺少seriesId')

        __update_series_sql(request.json)

        return build_success_response(msg='修改系列成功')

    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)
    except Exception as e:
        check.printException(e)
        return build_error_response(code=500, msg='服务器内部错误')


def __get_tag_sql(query_param):
    sql = ' select * from tags '
    sql_where_list = []
    if 'tagId' in query_param:
        sql_where_list.append((' tagId = %s ', query_param['tagId']))
    if 'content' in query_param:
        sql_where_list.append((' content like %s ', f'%{query_param["content"]}%'))
    args_list = []
    if len(sql_where_list):
        sql += ' where 1=1 '
        for item in sql_where_list:
            sql += f' and {item[0]} '
            args_list.append(item[1])
    if 'sortMode' in query_param:
        if query_param['sortMode'] == 'Hot':
            sql += 'order by linkedTimes desc '
        elif query_param['sortMode'] == 'New':
            sql += 'order by createTime desc '

    rows = execute_sql_query(pooldb, sql, tuple(args_list))

    return rows


# 标签管理
@bp.route('/tag/get', methods=['GET'])
def get_tag():
    """
    请求tag
    """
    try:
        check_user_before_request(request)
        rows = __get_tag_sql(request.args)
        length = len(rows)
        pageSize = request.args.get('pageSize')
        pageNum = request.args.get('pageNum')
        if pageSize is not None and pageNum is not None:
            pageSize = int(pageSize)
            pageNum = int(pageNum)
            rows = rows[pageSize * (pageNum - 1): pageSize * pageNum]

        return build_success_response(data=rows, length=length)
    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)
    except Exception as e:
        check.printException(e)
        return build_error_response(code=500, msg='服务器内部错误')


@bp.route('/tag/update', methods=['POST'])
def update_tag():
    """
    更新tag
    """
    try:
        check_user_before_request(request)
        tagId = request.json.get('tagId')
        content = request.json.get('content')
        if tagId is None or content is None:
            raise NetworkException(400, "前端数据错误，缺少tagId或content")
        sql = 'update tags set content = %s where tagId = %s'
        execute_sql_write(pooldb, sql, (content, tagId))

        return build_success_response()
    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)
    except Exception as e:
        check.printException(e)
        return build_error_response(code=500, msg='服务器内部错误')


@bp.route('/tag/del', methods=['GET'])
def del_tag():
    """
    删除tag
    """
    try:
        tagId = request.args.get('tagId')
        if tagId is None:
            raise NetworkException(400, "前端数据错误，缺少tagId")
        check_user_before_request(request, roles='manager')
        execute_sql_write(pooldb, 'delete from tags where tagId = %s', tagId)

        return build_success_response()
    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)
    except Exception as e:
        check.printException(e)
        return build_error_response(code=500, msg='服务器内部错误')


def __get_tag_by_id(tagId: str) -> Dict:
    return execute_sql_query_one(pooldb, 'select * from tags where tagId=%s', tagId)


def __get_tag_by_content(content: str) -> Dict:
    return execute_sql_query(pooldb, 'select * from tags where content=%s', content)


def __add_tag(content: str, trans=None) -> int:
    if trans is not None:
        return trans.execute('insert into tags(content) values(%s)', content)
    return execute_sql_write(pooldb, 'insert into tags(content) values(%s)', content)


@bp.route('/tag/add', methods=['POST'])
def add_tag():
    """
    添加tag
    """
    try:
        content = request.json.get('content')
        if content is None:
            raise NetworkException(400, "前端数据错误，缺少content")
        check_user_before_request(request)

        return build_success_response()
    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)
    except Exception as e:
        check.printException(e)
        return build_error_response(code=500, msg='服务器内部错误')


def __get_all_tag_series(tagId: str) -> List[Dict]:
    sql = 'select ' \
          'series.seriesId as seriesId, ' \
          'seriesName, isFinished, abstract, ' \
          'series.likes as likes, series.views as views, series.shares as shares, ' \
          'series.collect as collect, series.createTime as createTime, ' \
          'users.id as userId, users.userName as userName ' \
          'from tag_series, series, users ' \
          'where tag_series.tagId = %s ' \
          'and tag_series.seriesId = series.seriesId ' \
          'and series.userId = users.id'
    rows = execute_sql_query(pooldb, sql, tagId)
    return rows


@bp.route('/tag/getAllTagSeries', methods=['GET'])
def get_all_tag_series():
    """
    获取该tag标记过的所有series
    """
    try:
        tagId = request.args.get('tagId')
        if tagId is None:
            raise NetworkException(400, "前端数据错误，tagId")

        check_user_before_request(request)
        rows = __get_all_tag_series(tagId)

        return build_success_response(data=rows, length=len(rows))

    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)
    except Exception as e:
        check.printException(e)
        return build_error_response(code=500, msg='服务器内部错误')


def __del_tag_series_relation_sql(tagId: str, seriesId: str, trans=None):
    sql = 'delete from tag_series where tagId = %s and seriesId = %s'
    if trans is None:
        execute_sql_write(pooldb, sql, (tagId, seriesId))
    else:
        trans.execute(sql, (tagId, seriesId))


def __update_tag_linked_times_sql(tagId: str, trans=None):
    sql = 'update tags ' \
          'set linkedTimes = ' \
          '(select count(*) from tag_series where tag_series.tagId = %s) ' \
          'where tags.tagId = %s'
    # execute_sql_write(pooldb, sql, (tagId, tagId))
    if trans is None:
        execute_sql_write(pooldb, sql, (tagId, tagId))
    else:
        trans.execute(sql, (tagId, tagId))


@bp.route('/tag/delSeriesRelation', methods=['GET'])
def del_tag_series_relation():
    """
    获取与该tag标记过的series的联系
    """
    try:
        tagId = request.args.get('tagId')
        seriesId = request.args.get('seriesId')
        if tagId is None or seriesId is None:
            raise NetworkException(400, "前端数据错误，缺少tagId或seriesId")
        # 先验证登录，并查看是否具有common权限
        user = check_user_before_request(request)
        # 拿到属于该用户的所有series
        user_series_list = __get_series_by_user_id(user['id'])
        is_find = False
        # 遍历寻找前端发来的seriesId是否是用户自己的
        for series in user_series_list:
            if series['seriesId'] == seriesId:
                is_find = True
                break

        if not is_find:
            # 如果前端发来的seriesId不是用户自己的，则验证manager权限
            check_user_before_request(request, roles='manager')

        trans = SqlTransaction(pooldb)
        trans.begin()
        # 如果前端发来的seriesId是用户自己的，则直接进行操作，因为用户对自己的数据又绝对的控制权
        __del_tag_series_relation_sql(tagId, seriesId, trans)
        # 更新一下tag的被引用次数
        # __update_tag_linked_times_sql(tagId, trans)
        trans.commit()

        return build_success_response()

    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)
    except Exception as e:
        check.printException(e)
        return build_error_response(code=500, msg='服务器内部错误')


def __add_tag_series_relation_sql(tagId: str, seriesId: str, trans=None):
    sql = 'insert into tag_series(tagId, seriesId) values(%s, %s) '
    if trans is None:
        execute_sql_write(pooldb, sql, (tagId, seriesId))
    else:
        trans.execute(sql, (tagId, seriesId))


@bp.route('/tag/addSeriesRelation', methods=['GET'])
def add_tag_series_relation():
    """
    获取与该tag标记过的series的联系
    """
    try:
        tagId = request.args.get('tagId')
        seriesId = request.args.get('seriesId')
        if tagId is None or seriesId is None:
            raise NetworkException(400, "前端数据错误，缺少tagId或seriesId")
        # 先验证登录，并查看是否具有common权限
        user = check_user_before_request(request)
        # 拿到属于该用户的所有series
        user_series_list = __get_series_by_user_id(user['id'])
        is_find = False
        # 遍历寻找前端发来的seriesId是否是用户自己的
        for series in user_series_list:
            if series['seriesId'] == seriesId:
                is_find = True
                break

        if not is_find:
            # 如果前端发来的seriesId不是用户自己的，则验证manager权限
            check_user_before_request(request, roles='manager')

        trans = SqlTransaction(pooldb)
        trans.begin()
        # 如果前端发来的seriesId是用户自己的，则直接进行操作，因为用户对自己的数据又绝对的控制权
        __add_tag_series_relation_sql(tagId, seriesId, trans)
        # 更新一下tag的被引用次数
        # __update_tag_linked_times_sql(tagId, trans)
        trans.commit()

        return build_success_response()

    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)
    except Exception as e:
        check.printException(e)
        return build_error_response(code=500, msg='服务器内部错误')


# 喜欢收藏等零碎的东西

def __add_pieces_like(userId, piecesId, trans=None):
    sql = 'insert into user_pieces_like(userId, piecesId) values(%s, %s)'
    if trans is None:
        return execute_sql_write(pooldb, sql, (userId, piecesId))
    else:
        return trans.execute(sql, (userId, piecesId))


def __get_all_like_pieces_id_by_userid(userId) -> List[Dict]:
    """
    通过用户Id来获取该用户所有like的pieces的id
    """
    sql = 'select piecesId from user_pieces_like where userId=%s'
    rows = execute_sql_query(pooldb, sql, userId)
    rows = list(map(lambda x: int(x['piecesId']), rows))
    return rows


def __get_all_like_pieces_obj_by_userid(userId) -> List[Dict]:
    """
    通过用户Id来获取该用户所有like的pieces
    """
    sql = 'select pieces.piecesId as piecesId, pieces.seriesId as seriesId, ' \
          'pieces.title as title, pieces.userId as userId, pieces.content as content, ' \
          'pieces.createTime as createTime, pieces.updateTime as updateTime, ' \
          'pieces.status as status, pieces.likes as likes, pieces.views as views, ' \
          'pieces.shares as shares, pieces.collect as collect from pieces, user_pieces_like ' \
          'where pieces.piecesId = user_pieces_like.piecesId ' \
          'and user_pieces_like.userId=%s'
    return execute_sql_query(pooldb, sql, userId)


def __check_if_user_like_pieces(userId, piecesId) -> bool:
    rows = __get_all_like_pieces_id_by_userid(userId)
    piecesId = int(piecesId)
    if rows is not None and len(rows) and piecesId in rows:
        return True
    return False


@bp.route('/pieces/like/add', methods=['GET'])
def add_pieces_like():
    """
    增加pieces的点赞
    """
    try:
        piecesId = request.args.get("piecesId")
        if piecesId is None:
            raise NetworkException(400, "前端数据缺失，缺少piecesId")

        user = check_user_before_request(request)
        userId = user['id']
        if not __check_if_user_like_pieces(userId, piecesId):
            # 还没有点赞过
            __add_pieces_like(userId, piecesId)

        return build_success_response()

    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)

    except Exception as e:
        check.printException(e)
        return build_error_response(code=500, msg='服务器内部错误')


def __del_pieces_like(userId, piecesId, trans=None):
    sql = 'delete from user_pieces_like where userId=%s and piecesId=%s'
    if trans is None:
        return execute_sql_write(pooldb, sql, (userId, piecesId))
    else:
        return trans.execute(sql, (userId, piecesId))


@bp.route('/pieces/like/del', methods=['GET'])
def del_pieces_like():
    """
    取消pieces的点赞
    """
    try:
        piecesId = request.args.get("piecesId")
        if piecesId is None:
            raise NetworkException(400, "前端数据缺失，缺少piecesId")

        user = check_user_before_request(request)
        userId = user['id']
        __del_pieces_like(userId, piecesId)

        return build_success_response()

    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)

    except Exception as e:
        check.printException(e)
        return build_error_response(code=500, msg='服务器内部错误')


@bp.route('/pieces/like/get', methods=['GET'])
def get_pieces_like():
    """
    获取该用户点赞的所有pieces
    """
    try:
        user = check_user_before_request(request)
        userId = user['id']
        rows = __get_all_like_pieces_obj_by_userid(userId)

        return build_success_response(data=rows, length=len(rows))

    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)

    except Exception as e:
        check.printException(e)
        return build_error_response(code=500, msg='服务器内部错误')


def __add_pieces_collect(userId, piecesId, trans=None):
    sql = 'insert into user_pieces_collect(userId, piecesId) values(%s, %s)'
    if trans is None:
        return execute_sql_write(pooldb, sql, (userId, piecesId))
    else:
        return trans.execute(sql, (userId, piecesId))


def __get_all_collect_pieces_id_by_userid(userId) -> List[Dict]:
    """
    通过用户Id来获取该用户所有collect的pieces的id
    """
    sql = 'select piecesId from user_pieces_collect where userId=%s'
    rows = execute_sql_query(pooldb, sql, userId)
    rows = list(map(lambda x: int(x['piecesId']), rows))
    return rows


def __get_all_collect_pieces_obj_by_userid(userId) -> List[Dict]:
    """
    通过用户Id来获取该用户所有collect的pieces
    """
    sql = 'select pieces.piecesId as piecesId, pieces.seriesId as seriesId, ' \
          'pieces.title as title, pieces.userId as userId, pieces.content as content, ' \
          'pieces.createTime as createTime, pieces.updateTime as updateTime, ' \
          'pieces.status as status, pieces.likes as likes, pieces.views as views, ' \
          'pieces.shares as shares, pieces.collect as collect from pieces, user_pieces_collect ' \
          'where pieces.piecesId = user_pieces_collect.piecesId ' \
          'and user_pieces_collect.userId=%s'
    return execute_sql_query(pooldb, sql, userId)


def __check_if_user_collect_pieces(userId, piecesId) -> bool:
    rows = __get_all_collect_pieces_id_by_userid(userId)
    piecesId = int(piecesId)
    if rows is not None and len(rows) and piecesId in rows:
        return True
    return False


@bp.route('/pieces/collect/add', methods=['GET'])
def add_pieces_collect():
    """
    增加pieces的点赞
    """
    try:
        piecesId = request.args.get("piecesId")
        if piecesId is None:
            raise NetworkException(400, "前端数据缺失，缺少piecesId")

        user = check_user_before_request(request)
        userId = user['id']
        if not __check_if_user_collect_pieces(userId, piecesId):
            # 还没有点赞过
            __add_pieces_collect(userId, piecesId)

        return build_success_response()

    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)

    except Exception as e:
        check.printException(e)
        return build_error_response(code=500, msg='服务器内部错误')


def __del_pieces_collect(userId, piecesId, trans=None):
    sql = 'delete from user_pieces_collect where userId=%s and piecesId=%s'
    if trans is None:
        return execute_sql_write(pooldb, sql, (userId, piecesId))
    else:
        return trans.execute(sql, (userId, piecesId))


@bp.route('/pieces/collect/del', methods=['GET'])
def del_pieces_collect():
    """
    取消pieces的点赞
    """
    try:
        piecesId = request.args.get("piecesId")
        if piecesId is None:
            raise NetworkException(400, "前端数据缺失，缺少piecesId")

        user = check_user_before_request(request)
        userId = user['id']
        __del_pieces_collect(userId, piecesId)

        return build_success_response()

    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)

    except Exception as e:
        check.printException(e)
        return build_error_response(code=500, msg='服务器内部错误')


@bp.route('/pieces/collect/get', methods=['GET'])
def get_pieces_collect():
    """
    获取该用户点赞的所有pieces
    """
    try:
        user = check_user_before_request(request)
        userId = user['id']
        rows = __get_all_collect_pieces_obj_by_userid(userId)

        return build_success_response(data=rows, length=len(rows))

    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)

    except Exception as e:
        check.printException(e)
        return build_error_response(code=500, msg='服务器内部错误')


def __pieces_comment_add_sql(piecesId: int, userId: int, content: str, trans=None) -> int:
    # 修改 comments 表
    add_c_sql = 'insert into comments(userId, content) values(%s,%s)'
    comment_id = trans.execute(add_c_sql, (userId, content))

    # 修改 comment_pieces表
    add_cp_sql = 'insert into comment_pieces(commentId, piecesId) values(%s, %s)'
    trans.execute(add_cp_sql, (comment_id, piecesId))
    return comment_id


def __pieces_reply_comment_sql(uid: int, parent_cid: int, content: str, trans=None):
    # 修改 comments 表
    add_c_sql = 'insert into comments(userId, content,likes) values(%s,%s,%s)'
    args = uid, content, 0  # tuple
    comment_id = trans.execute( add_c_sql, args)

    # 修改 comment_replies 表
    add_cb_sql = 'insert into comment_replies(parentId, commentId) values(%s,%s)'
    args = parent_cid, comment_id
    trans.execute(add_cb_sql, args)
    return comment_id


def __pieces_update_comment_sql(uid: int, cid: int, trans=None, content=None, like=None):
    """
    更新一条评论记录，包括内容和点赞数
    注意： 数据库 comment_likes 表中设置了触发器更新 comments，所以点赞或取消只需要更新 comment_likes
    """
    if content is not None:
        # 修改 comments 表
        update_c_sql = "UPDATE comments SET content=%s WHERE commentId=%s"
        c_args = content, cid
        trans.execute(update_c_sql, c_args)

    if like is not None:
        # 修改 comment_likes 表
        if like == 1:
            # 跳过重复点赞
            if check_comment_liked(pooldb, uid, cid):
                return
            update_cl_sql = 'INSERT INTO comment_likes(userId, commentId) VALUES(%s,%s)'
            cl_args = uid, cid
            trans.execute(update_cl_sql, cl_args)
        elif like == 0:
            del_cl_sql = "DELETE FROM comment_likes WHERE userId=%s AND commentId=%s"
            d_cl_args = uid, cid
            trans.execute(del_cl_sql, d_cl_args)
        else:
            raise NetworkException(400, 'Invalid like (must be 0 or 1)')


def __pieces_del_comments_sql(uid: int, cid: int, trans=None) -> int:
    """
    删除一条评论记录和与之相关的 comment_book 记录。
    注意： 数据库 comment_book 表中设置了级联删除，仅需删除 comments 表中数据即可

    :param uid: 用户 ID。
    :param cid: 评论 ID。
    :param trans: SqlTransaction
    :return: None
    :raises NetworkException: 当执行 SQL 发生错误时，抛出此异常。
    """
    # 删除 comment
    del_c_sql = "DELETE FROM comments WHERE userId=%s AND commentId=%s"
    c_args = (uid, cid)
    if trans is None:
        return execute_sql_write(pooldb, del_c_sql, c_args)
    else:
        return trans.execute(del_c_sql, c_args)


def __pieces_del_replies_sql(uid: int, cid: int, trans=None):
    """
    删除一条回复记录和与之相关的 comment_replies 记录。
    注意： 数据库 comment_replies 表中设置了级联删除，仅需删除 comments 表中数据即可

    :param uid: 用户 ID。
    :param cid: 评论 ID。
    :param trans: SqlTransaction
    :return: None
    :raises NetworkException: 当执行 SQL 发生错误时，抛出此异常。
    """
    # 获取所有子评论的 id
    ids = get_sub_comment_ids_stack(cid)
    # 加上该评论
    ids.append(cid)

    # 删除 comment
    for cid in ids:
        del_c_sql = "DELETE FROM comments WHERE userId=%s AND commentId=%s"
        c_args = (uid, cid)
        if trans is None:
            execute_sql_write(pooldb, del_c_sql, c_args)
        else:
            trans.execute(del_c_sql, c_args)


def __get_comments_id_by_pieces_id(piecesId: int) -> List[int]:
    sql = 'select commentId from comment_pieces where piecesId=%s'
    rows = execute_sql_query(pooldb, sql, piecesId)
    ids = list(map(lambda x: int(x['commentId']), rows))
    return ids


def __check_if_comment_is_belongto_user(commentId: int, userId: int) -> bool:
    sql = 'select * from comments where commentId = %s and userId = %s '
    row = execute_sql_query_one(pooldb, sql, (commentId, userId))
    if row is None:
        return False
    return True

def __get_comment_user_id_by_comment_id_sql(commentId:int) -> int:
    sql = 'select userId from comments where commentId = %s'
    row = execute_sql_query_one(pooldb, sql, (commentId))
    if row is None:
        return None
    return int(row['userId'])

def __pieces_get_comments_detail_one_depth_reply(piecesId: int, user=None) -> List[Dict]:
    """
    获取pieces的所有评论详情，评论只有一层，包含回复
    """
    comments_ids = __get_comments_id_by_pieces_id(piecesId)
    comments_objs = []
    for comment_id in comments_ids:
        res = get_comment_details(comment_id, None, user)
        parent_user = get_user_by_id(res['userId'])
        tmp_comment_obj = {
            'commentId': res['commentId'],
            'content': res['content'],
            'user': parent_user,
            'createTime': res['createTime'],
            'likes': res['likes']
        }

        if user is not None and int(user['id']) == int(res['userId']):
            tmp_comment_obj['isYours'] = True
        else:
            tmp_comment_obj['isYours'] = False

        tmp_comment_obj['liked'] = False if user is None else check_comment_liked(pooldb, user['id'], tmp_comment_obj['commentId'])

        comments_objs.append(tmp_comment_obj)

        sub_comments = res['comments']

        for comment_obj in sub_comments:
            reply_to_user = get_user_by_id(__get_comment_user_id_by_comment_id_sql(comment_obj['reply_to']))
            tmp_sub_comments_obj = {
                'commentId': comment_obj['commentId'],
                'content': comment_obj['content'],
                'user': get_user_by_id(comment_obj['userId']),
                'toUser': reply_to_user,
                'createTime': comment_obj['createTime'],
                'likes': comment_obj['likes']
            }
            if user is not None and int(user['id']) == int(comment_obj['userId']):
                tmp_sub_comments_obj['isYours'] = True
            else:
                tmp_sub_comments_obj['isYours'] = False

            tmp_sub_comments_obj['liked'] = False if user is None else check_comment_liked(pooldb, user['id'],
                                                                                      tmp_sub_comments_obj['commentId'])

            comments_objs.append(tmp_sub_comments_obj)

    # 根据createTime逆序排序
    comments_objs.sort(key=lambda x: x['createTime'], reverse=True)

    return comments_objs


@bp.route('/pieces/comments/add', methods=['POST'])
def add_pieces_comments():
    """
    为pieces添加评价
    """
    try:
        piecesId = request.json.get("piecesId")
        content = request.json.get("content")
        if piecesId is None:
            raise NetworkException(400, '前端参数错误，未传入piecesId')
        if content is None:
            raise NetworkException(400, '请填写评论内容')

        user = check_user_before_request(request)
        userId = user['id']

        trans = SqlTransaction(pooldb)
        trans.begin()
        __pieces_comment_add_sql(piecesId, userId, content, trans)
        trans.commit()

        return build_success_response()

    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)

    except Exception as e:
        check.printException(e)
        return build_error_response(code=500, msg='服务器内部错误')

@bp.route('/pieces/comments/reply', methods=['POST'])
def add_pieces_comments_reply():
    """
    为pieces添加评价
    """
    try:
        commentId = request.json.get("commentId")
        content = request.json.get("content")
        if commentId is None:
            raise NetworkException(400, '前端参数错误，未传入commentId')
        if content is None:
            raise NetworkException(400, '请填写评论内容')
        
        
        
        user = check_user_before_request(request)
        userId = user['id']
        print(f'[DEBUG] commentId = {commentId}  content={content}  user = {user} ')
        trans = SqlTransaction(pooldb)
        trans.begin()
        __pieces_reply_comment_sql(userId, commentId, content, trans)
        trans.commit()

        return build_success_response()

    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)

    except Exception as e:
        check.printException(e)
        return build_error_response(code=500, msg='服务器内部错误')

@bp.route('/pieces/comments/update', methods=['POST'])
def update_pieces_comments():
    """
    修改评论
    """
    try:
        commentId = request.json.get("commentId")
        content = request.json.get("content")
        if commentId is None:
            raise NetworkException(400, '前端参数错误，未传入commentId')
        if content is None:
            raise NetworkException(400, '请填写评论内容')

        user = check_user_before_request(request, roles='common')

        trans = SqlTransaction(pooldb)
        trans.begin()
        if __check_if_comment_is_belongto_user(commentId, user['id']):
            # 如果这个comment属于发出请求的用户，则可以操作
            __pieces_update_comment_sql(user['id'], commentId, trans, content)
        else:
            # 如果这个seires不属于发出请求的用户，则需要验证管理员身份
            check_user_before_request(request, roles='manager')
            __pieces_update_comment_sql(user['id'], commentId, trans, content)

        trans.commit()

        return build_success_response()

    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)

    except Exception as e:
        check.printException(e)
        return build_error_response(code=500, msg='服务器内部错误')


@bp.route('/pieces/comments/del', methods=['GET'])
def del_pieces_comments():
    """
    删除Pieces的评论
    """
    try:
        commentId = request.args.get("commentId")
        if commentId is None:
            raise NetworkException(400, '前端参数错误，未传入commentId')

        user = check_user_before_request(request, roles='common')

        trans = SqlTransaction(pooldb)
        trans.begin()
        if __check_if_comment_is_belongto_user(commentId, user['id']):
            # 如果这个comment属于发出请求的用户，则可以操作
            __pieces_del_comments_sql(user['id'], commentId, trans)
        else:
            # 如果这个seires不属于发出请求的用户，则需要验证管理员身份
            check_user_before_request(request, roles='manager')
            __pieces_del_comments_sql(user['id'], commentId, trans)
        trans.commit()

        return build_success_response()

    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)

    except Exception as e:
        check.printException(e)
        return build_error_response(code=500, msg='服务器内部错误')

def __add_comment_like_sql(commentId: int, userId: int, trans=None) -> int:
    sql = 'insert into comment_likes(userId, commentId) values(%s, %s)'
    if trans is None:
        return execute_sql_write(pooldb, sql, (userId, commentId))
    else:
        return trans.execute(sql, (userId, commentId))

def __del_comment_like_sql(commentId: int, userId: int, trans=None) -> int:
    sql = 'delete from comment_likes where userId = %s and commentId = %s'
    if trans is None:
        return execute_sql_write(pooldb, sql, (userId, commentId))
    else:
        return trans.execute(sql, (userId, commentId))

@bp.route('/pieces/comments/like/add', methods=['GET'])
def add_pieces_comment_like():
    """
    为评论点赞
    """
    try:
        commentId = request.args.get("commentId")
        if commentId is None:
            raise NetworkException(400, '前端参数错误，未传入commentId')

        user = check_user_before_request(request, roles='common')
        __add_comment_like_sql(commentId, user['id'])

        return build_success_response()

    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)

    except Exception as e:
        check.printException(e)
        return build_error_response(code=500, msg='服务器内部错误')

@bp.route('/pieces/comments/like/del', methods=['GET'])
def del_pieces_comment_like():
    """
    取消评论点赞
    """
    try:
        commentId = request.args.get("commentId")
        if commentId is None:
            raise NetworkException(400, '前端参数错误，未传入commentId')

        user = check_user_before_request(request, roles='common')
        __del_comment_like_sql(commentId, user['id'])

        return build_success_response()

    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)

    except Exception as e:
        check.printException(e)
        return build_error_response(code=500, msg='服务器内部错误')
