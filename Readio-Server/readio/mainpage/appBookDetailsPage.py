"""
书籍详情页
    - 返回书籍及其评论信息，标志是否已加入书架，标志评论是否已点赞
  评论子页
    - 评论的详细信息，包括子评论及其回复
"""

import inspect
from typing import List
from collections import deque
import copy

from flask import Blueprint
from flask import request
from flask import url_for

import readio.database.connectPool
from readio.utils.auth import check_user_before_request
from readio.utils.buildResponse import *
from readio.utils.check import check_book_added, check_has_comment, check_exist_comment_for_book, check_exist_comment, \
    check_comment_liked, check_book_liked
from readio.utils.executeSQL import execute_sql_write, execute_sql_query, execute_sql_query_one
from readio.utils.formatter import process_comment_time, process_comment_list_time
from readio.utils.myExceptions import NetworkException

# 前缀为 app/book 的蓝图
bp = Blueprint('book_detail', __name__, url_prefix='/app/book')

pooldb = readio.database.connectPool.pooldb


def view_book_sql(bid):
    """将表 books(id, views) 中的 views 加一"""
    view_count_sql = 'UPDATE books SET views=views+1 WHERE id=%s'
    execute_sql_write(pooldb, view_count_sql, bid)


def get_book_info_sql(bid):
    view_book_sql(bid)
    # 书籍信息
    get_book_sql = 'SELECT * FROM books WHERE books.id=%s'
    book = execute_sql_query_one(pooldb, get_book_sql, bid)
    if book is not None:
        return book
    else:
        raise Exception('查无此书')


def get_book_details(bid, user=None):
    details, comments_data = {}, {}
    book_info = get_book_info_sql(bid)
    # TODO:暂未添加的书籍封面
    book_info['bitmap'] = None
    # 评论等信息 (comments_data)
    get_comments_sql = 'SELECT c.* FROM comments AS c, comment_book AS cb ' \
                       'WHERE cb.bookId=%s AND c.commentId=cb.commentId'
    comments = execute_sql_query(pooldb, get_comments_sql, bid)
    if user is None:
        for comment in comments:
            comment = process_comment_time(comment)
            comment['liked'] = False
        details['added'] = False
        details['liked'] = False
    else:
        for comment in comments:
            comment = process_comment_time(comment)
            comment['liked'] = check_comment_liked(pooldb, user['id'], comment['commentId'])
        details['added'] = check_book_added(pooldb, user['id'], bid)
        details['liked'] = check_book_liked(pooldb, user['id'], bid)
    comments_data['size'] = len(comments)
    comments_data['data'] = comments
    # 信息汇总 -> 书籍详情 (details)
    details['book_info'] = book_info
    details['comments'] = comments_data
    return details


def update_like_book_sql(uid: int, bid: int, like=None):
    """
    更新书籍点赞
    注意： 数据库 book_likes 表中设置了触发器更新 books，所以点赞或取消只需要更新 book_likes
    """
    if like is not None:
        if like == 1:
            # 跳过重复点赞
            if check_book_liked(pooldb, uid, bid):
                return
            update_bl_sql = 'INSERT INTO book_likes(userId, bookId) VALUES(%s,%s)'
            args = uid, bid
            execute_sql_write(pooldb, update_bl_sql, args)
        elif like == 0:
            del_sql = "DELETE FROM book_likes WHERE userId=%s AND bookId=%s"
            d_args = uid, bid
            execute_sql_write(pooldb, del_sql, d_args)
        else:
            raise NetworkException(400, 'Invalid like (must be 0 or 1)')


def get_comment_sql(comment_id: int) -> dict:
    """
    通过评论 ID 查询数据库中的评论信息，并将评论字典中的 createTime 转换为字符串格式

    :param comment_id: 评论 ID。
    :return: 包含评论信息的字典对象。
    :raises NetworkException: 如果评论不存在，则抛出错误码为 400 的 NetworkException 异常。
    """
    get_sql = 'SELECT * FROM comments WHERE commentId=%s'
    # comment = execute_sql_query(pooldb, get_sql, comment_id)[0]
    comment = execute_sql_query_one(pooldb, get_sql, comment_id)
    if comment is not None:
        # 将评论的 createTime 转换为字符串格式
        return process_comment_time(comment)
    else:
        raise NetworkException(400, f'评论 {comment_id} 不存在')


def get_comment_replies_sql(comment_id: int) -> List[dict]:
    """
    查询指定评论的直接子评论并返回其列表。

    :param comment_id: int类型，表示需要查询的评论ID。
    :return: 包含子评论信息的字典列表。如果该评论没有任何子评论，则返回一个空列表。
    """
    get_sql = 'SELECT * FROM comment_replies WHERE parentId=%s'
    comment_list = execute_sql_query(pooldb, get_sql, comment_id)
    return comment_list


def get_sub_comment_ids_stack(root_id: int) -> List[int]:
    """
    使用栈遍历获取指定评论的所有子评论 id

    :param root_id: int, 指定评论的 id
    :return: list[int], 所有子评论的 id 列表
    """
    # 创建一个栈，将待处理的评论 id 加入栈中
    stack = [root_id]
    # 存储所有子评论的 id
    result = []

    # 不断从栈中弹出评论 id 并处理，直到栈为空
    while stack:
        comment_id = stack.pop()  # 弹出最新加入栈中的评论 id
        reply_list = get_comment_replies_sql(comment_id)  # 获取当前评论的所有子评论
        for reply in reply_list:
            reply_id = reply['commentId']  # 获取子评论的 id
            stack.append(reply_id)  # 将子评论 id 加入栈中，以便后续处理
            result.append(reply_id)  # 将子评论 id 加入结果列表中

    return result


def get_comment_tree_recursive(comment_id: int, depth: int, user) -> List[dict]:
    """
    递归查询指定评论的子评论树

    :param comment_id: 指定评论的 ID
    :param depth: 查询深度，即子评论的层数
    :return: 子评论树列表，每个元素为一个字典表示一个评论及其子评论
    """
    if depth <= 0:
        return []

    # 查询该评论的直接子评论
    reply_list = get_comment_replies_sql(comment_id)
    # 递归
    comment_tree = []
    for reply in reply_list:
        reply_id = reply['commentId']
        reply_cmt = get_comment_sql(reply_id)
        sub_tree = reply_cmt
        sub_tree['liked'] = False if user is None else check_comment_liked(pooldb, user['id'], reply_id)
        replies = get_comment_tree_recursive(reply_id, depth - 1, user)
        sub_tree['size'] = len(replies)
        sub_tree['replies'] = replies
        comment_tree.append(sub_tree)

    return comment_tree


def get_sub_comments_stack(comment_id: int, user) -> List[dict]:
    """
    使用栈遍历获取指定评论的所有子评论 id，包含回复

    :param comment_id: 指定评论的 ID
    :return: 子评论列表，每个元素为一个字典表示一条评论
    """
    # 创建一个栈，将待处理的评论 id 加入栈中
    stack = [comment_id]
    # 存储所有子评论的 id
    result = []

    # 不断从栈中弹出评论 id 并处理，直到栈为空
    while stack:
        comment_id = stack.pop()  # 弹出最新加入栈中的评论 id
        reply_list = get_comment_replies_sql(comment_id)  # 获取当前评论的所有子评论
        for reply in reply_list:
            reply_id = reply['commentId']  # 获取子评论的 id
            reply_cmt = get_comment_sql(reply_id)
            reply_cmt['liked'] = False if user is None else check_comment_liked(pooldb, user['id'], reply_id)
            reply_cmt['reply_to'] = comment_id
            stack.append(reply_id)  # 将子评论 id 加入栈中，以便后续处理
            result.append(reply_cmt)  # 将子评论加入结果列表中

    return result


def get_comment_details(comment_id, depth, user=None):
    """ 获取评论详细信息，包括其子评论信息 """
    # 获取评论
    comment = get_comment_sql(comment_id)

    # 获取子评论列表
    sub_comment_list = []
    if depth is not None:  # 递归模式，返回树型结构
        sub_comment_list = get_comment_tree_recursive(comment_id, depth - 1, user)
    else:  # 栈模式，返回两层结构
        sub_comment_list = get_sub_comments_stack(comment_id, user)

    # 组织结果
    details = comment
    details['size'] = len(sub_comment_list)
    details['liked'] = False if user is None else check_comment_liked(pooldb, user['id'], comment_id)
    details['comments'] = sub_comment_list
    return details


def add_comments_sql(uid: int, bid: int, content: str):
    # 修改 comments 表
    add_c_sql = 'insert into comments(userId, content,likes) values(%s,%s,%s)'
    args = uid, content, 0  # tuple
    comment_id = execute_sql_write(pooldb, add_c_sql, args)

    # 修改 comment_book 表
    add_cb_sql = 'insert into comment_book(bookId, commentId) values(%s,%s)'
    args = bid, comment_id
    execute_sql_write(pooldb, add_cb_sql, args)
    return comment_id


def reply_comment_sql(uid: int, parent_cid: int, content: str):
    # 修改 comments 表
    add_c_sql = 'insert into comments(userId, content,likes) values(%s,%s,%s)'
    args = uid, content, 0  # tuple
    comment_id = execute_sql_write(pooldb, add_c_sql, args)

    # 修改 comment_replies 表
    add_cb_sql = 'insert into comment_replies(parentId, commentId) values(%s,%s)'
    args = parent_cid, comment_id
    execute_sql_write(pooldb, add_cb_sql, args)
    return comment_id


def update_comment_sql(uid: int, cid: int, content=None, like=None):
    """
    更新一条评论记录，包括内容和点赞数
    注意： 数据库 comment_likes 表中设置了触发器更新 comments，所以点赞或取消只需要更新 comment_likes
    """
    if content is not None:
        # 修改 comments 表
        update_c_sql = "UPDATE comments SET content=%s WHERE commentId=%s"
        c_args = content, cid
        execute_sql_write(pooldb, update_c_sql, c_args)

    if like is not None:
        # 修改 comment_likes 表
        if like == 1:
            # 跳过重复点赞
            if check_comment_liked(pooldb, uid, cid):
                return
            update_cl_sql = 'INSERT INTO comment_likes(userId, commentId) VALUES(%s,%s)'
            cl_args = uid, cid
            execute_sql_write(pooldb, update_cl_sql, cl_args)
        elif like == 0:
            del_cl_sql = "DELETE FROM comment_likes WHERE userId=%s AND commentId=%s"
            d_cl_args = uid, cid
            execute_sql_write(pooldb, del_cl_sql, d_cl_args)
        else:
            raise NetworkException(400, 'Invalid like (must be 0 or 1)')


def del_comments_sql(uid: int, cid: int):
    """
    删除一条评论记录和与之相关的 comment_book 记录。
    注意： 数据库 comment_book 表中设置了级联删除，仅需删除 comments 表中数据即可

    :param uid: 用户 ID。
    :param cid: 评论 ID。

    :return: None
    :raises NetworkException: 当执行 SQL 发生错误时，抛出此异常。
    """
    # 删除 comment
    del_c_sql = "DELETE FROM comments WHERE userId=%s AND commentId=%s"
    c_args = (uid, cid)
    execute_sql_write(pooldb, del_c_sql, c_args)


def del_replies_sql(uid: int, cid: int):
    """
    删除一条回复记录和与之相关的 comment_replies 记录。
    注意： 数据库 comment_replies 表中设置了级联删除，仅需删除 comments 表中数据即可

    :param uid: 用户 ID。
    :param cid: 评论 ID。

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
        execute_sql_write(pooldb, del_c_sql, c_args)


""" ========== 针对书籍详情页的主页 ========== """


# 书本详情页，包括书籍信息和评论信息
@bp.route('/<int:book_id>', methods=['GET'])
def index(book_id):
    if request.method == 'GET':
        try:
            # 检查是否有用户 token ，有返回用户，否则返回 None
            user = check_user_before_request(request, raise_exc=False)
            # print(f"[DEBUG] user = {user}")
            details = get_book_details(book_id, user)
            data = details
            # print(f"[DEBUG] response = {data}")
            response = build_success_response(data)
        except NetworkException as e:
            response = build_error_response(code=e.code, msg=e.msg)
        except Exception as e:
            print("[ERROR]" + __file__ + "::" + inspect.getframeinfo(inspect.currentframe().f_back)[2])
            print(e)
            response = build_error_response(msg=str(e))
        return response


@bp.route('/<int:book_id>/like', methods=['POST'])
def update_like_book(book_id):
    if request.method == 'POST':
        try:
            # 检查是否有用户 token ，有返回用户，否则返回 None
            user = check_user_before_request(request)
            uid = user['id']
            bid, like = request.json.get('bookId'), request.json.get('like', None)
            # 检查 URL 与请求参数 book_id 是否一致
            if bid is None or str(bid) != str(book_id):
                raise NetworkException(400, 'Invalid bookId')
            if like is None:
                raise NetworkException(400, 'Invalid like (must be 0 or 1)')
            update_like_book_sql(uid, bid, like)
            url = url_for('book_detail.index', book_id=bid)
            response = build_redirect_response(f'成功更新书籍点赞，重定向至书籍详情页', url)
        except NetworkException as e:
            response = build_error_response(code=e.code, msg=e.msg)
        except Exception as e:
            print("[ERROR]" + __file__ + "::" + inspect.getframeinfo(inspect.currentframe().f_back)[2])
            print(e)
            response = build_error_response(msg=str(e))
        return response


# 增加书本的评论
@bp.route('/<int:book_id>/comments/add', methods=['POST'])
def add_comments(book_id):
    if request.method == 'POST':
        try:
            user = check_user_before_request(request)
            uid, bid = user['id'], request.json.get('bookId')
            content = request.json.get('content', None)
            # 检查 URL 与请求参数 book_id 是否一致
            if bid is None or str(bid) != str(book_id):
                raise NetworkException(400, 'Invalid bookId')
            if content is None:
                raise NetworkException(400, '请填写评论内容')
            cid = add_comments_sql(uid, bid, content)
            url = url_for('book_detail.index', book_id=bid)
            response = build_redirect_response(f'添加评论 {cid} 成功，重定向至书籍详情页', url)
        except NetworkException as e:
            response = build_error_response(code=e.code, msg=e.msg)
        except Exception as e:
            print("[ERROR]" + __file__ + "::" + inspect.getframeinfo(inspect.currentframe().f_back)[2])
            print(e)
            response = build_error_response(msg=str(e))
        return response


# 更新书本的评论
@bp.route('/<int:book_id>/comments/<int:comment_id>/update', methods=['POST'])
def update_comments(book_id, comment_id):
    return update_comment(book_id, comment_id)


# 删除书本的评论
@bp.route('/<int:book_id>/comments/delete', methods=['POST'])
def delete_comments(book_id):
    if request.method == 'POST':
        try:
            user = check_user_before_request(request)
            uid = user['id']
            bid, cid = request.json.get('bookId'), request.json.get('commentId')
            # 检查 URL 与请求参数 book_id 是否一致
            if bid is None or str(bid) != str(book_id):
                raise NetworkException(400, 'Invalid bookId')
            # 检查评论 ID 是否为空，前面已经保证 uid 和 bid 不为空
            if not cid:
                raise NetworkException(400, 'commentId 缺失')
            # 检查用户是否有这条评论
            if check_has_comment(pooldb, uid, cid):
                del_comments_sql(uid, cid)  # 数据库中删除该条评论
            else:
                raise NetworkException(400, f'评论 {cid} 不存在或无权删除')
            url = url_for('book_detail.index', book_id=bid)
            response = build_redirect_response(f'删除评论 {cid} 成功，重定向至书籍详情页', url)
        except NetworkException as e:
            response = build_error_response(code=e.code, msg=e.msg)
        except Exception as e:
            print("[ERROR]" + __file__ + "::" + inspect.getframeinfo(inspect.currentframe().f_back)[2])
            print(e)
            response = build_error_response(msg=str(e))
        return response


""" ========== 针对书籍详情页下的评论子页 ========== """


# 对他人的评论进行回复，可以是子评论的回复，所以不检查书籍下是否有该条评论
@bp.route('/<int:book_id>/comment/reply', methods=['POST'])
def reply_comment(book_id):
    if request.method == 'POST':
        try:
            user = check_user_before_request(request)
            uid = user['id']
            bid, cid = request.json.get('bookId'), request.json.get('commentId')
            content = request.json.get('content')
            # 检查 URL 与请求参数是否一致，且保证非空
            if bid is None or str(bid) != str(book_id):
                raise NetworkException(400, 'Invalid bookId')
            if content is None:
                raise NetworkException(400, '请填写评论内容')
            # 检查是否有这条评论
            if check_exist_comment(pooldb, cid):
                reply_comment_sql(uid, cid, content)
            else:
                raise NetworkException(400, f'评论 {cid} 不存在')
            response = build_success_response(msg=f'回复评论 {cid} 成功')
        except NetworkException as e:
            response = build_error_response(code=e.code, msg=e.msg)
        except Exception as e:
            print("[ERROR]" + __file__ + "::" + inspect.getframeinfo(inspect.currentframe().f_back)[2])
            print(e)
            response = build_error_response(msg=str(e))
        return response


@bp.route('/<int:book_id>/comment/<int:comment_id>/update', methods=['POST'])
def update_comment(book_id, comment_id):
    if request.method == 'POST':
        try:
            user = check_user_before_request(request)
            uid = user['id']
            bid, cid = request.json.get('bookId'), request.json.get('commentId')
            content, like = request.json.get('content', None), request.json.get('like', None)
            # 检查 URL 与请求参数是否一致
            if bid is None or str(bid) != str(book_id):
                raise NetworkException(400, 'Invalid bookId')
            if cid is None or str(cid) != str(comment_id):
                raise NetworkException(400, 'Invalid commentId')
            if not check_exist_comment(pooldb, comment_id):
                raise NetworkException(400, '这条评论不存在')
            # 判断是修改点赞还是修改内容，或者两个都修改
            if content is None:  # 只修改点赞
                update_comment_sql(uid, cid, like=like)
            else:  # 修改内容，需要检查用户是否有这条评论
                if check_has_comment(pooldb, uid, cid):
                    update_comment_sql(uid, cid, content=content, like=like)
                else:
                    raise NetworkException(400, f'无权修改评论 {cid}')
            response = build_success_response(f'评论修改成功')
        except NetworkException as e:
            response = build_error_response(code=e.code, msg=e.msg)
        except Exception as e:
            print("[ERROR]" + __file__ + "::" + inspect.getframeinfo(inspect.currentframe().f_back)[2])
            print(e)
            response = build_error_response(msg=str(e))
        return response


# 删除书本评论的回复
@bp.route('/<int:book_id>/comment/del', methods=['POST'])
def del_replies(book_id):
    if request.method == 'POST':
        try:
            user = check_user_before_request(request)
            uid = user['id']
            bid, cid = request.json.get('bookId'), request.json.get('commentId')
            # 检查 URL 与请求参数是否一致，且保证非空
            if bid is None or str(bid) != str(book_id):
                raise NetworkException(400, 'Invalid bookId')
            if not cid:
                raise NetworkException(400, 'commentId 缺失')
            # 检查用户是否有这条评论
            if check_has_comment(pooldb, uid, cid):
                del_replies_sql(uid, cid)
            else:
                raise NetworkException(400, f'评论 {cid} 不存在或无权删除')
            response = build_success_response(msg=f'删除回复 {cid} 成功')
        except NetworkException as e:
            response = build_error_response(code=e.code, msg=e.msg)
        except Exception as e:
            print("[ERROR]" + __file__ + "::" + inspect.getframeinfo(inspect.currentframe().f_back)[2])
            print(e)
            response = build_error_response(msg=str(e))
        return response


# 评论详情页，返回书本评论的详细内容以及子评论
@bp.route('/<int:book_id>/comment/<int:comment_id>', methods=['GET'])
def index_comment(book_id, comment_id):
    if request.method == 'GET':
        try:
            if not check_exist_comment_for_book(pooldb, book_id, comment_id):
                raise NetworkException(400, '该书没有这条评论')
            # 检查是否有用户 token ，有返回用户，否则返回 None
            user = check_user_before_request(request, raise_exc=False)
            easymode = request.headers.get('easymode', 'True').lower() == 'true'
            depth = None if easymode is True else int(request.headers.get('depth', 6))
            # print(type(easymode),easymode, type(depth),depth)
            comment_details = get_comment_details(comment_id, depth=depth, user=user)
            data = comment_details
            response = build_success_response(data)
        except NetworkException as e:
            response = build_error_response(code=e.code, msg=e.msg)
        except Exception as e:
            print("[ERROR]" + __file__ + "::" + inspect.getframeinfo(inspect.currentframe().f_back)[2])
            print(e)
            response = build_error_response(msg=str(e))
        return response
