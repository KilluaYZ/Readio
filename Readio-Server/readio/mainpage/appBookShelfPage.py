"""
书架页
"""
from flask import Blueprint
from flask import request
from flask import url_for
import inspect
from typing import List, Dict, Optional

from readio.utils.buildResponse import *
from readio.utils.auth import check_user_before_request
from readio.utils.check import check_book_added, check_book_read
import readio.database.connectPool
from readio.utils.myExceptions import NetworkException
from readio.utils.executeSQL import execute_sql_write, execute_sql_query

# 前缀为 app/books 的蓝图
bp = Blueprint('bookshelf', __name__, url_prefix='/app/books')

pooldb = readio.database.connectPool.pooldb


def get_books(user_id: int, added: int = 1) -> List[Dict[str, str]]:
    """
    获取某个用户已经阅读过的书籍信息列表
    :param user_id: 用户ID
    :param added: 是否为书架里的书（1为书架上的，0为阅读过的）
    :return: 包含书籍信息的字典列表，每个字典中包括了书籍的ID、名称、作者等信息
    """
    # SQL查询，获取用户已经阅读过的所有书籍信息
    get_sql = "SELECT info.* , books.* " \
              "FROM user_read_info AS info, books " \
              "WHERE userId=%s AND info.bookId=books.id AND info.added>=%s"
    args = user_id, int(added)
    books = execute_sql_query(pooldb, get_sql, args)
    return books


def search_books(book_name: Optional[str] = None, author_name: Optional[str] = None) -> List[dict]:
    """
    搜索书籍。根据书名或作者名进行模糊匹配搜索，并按匹配度高低及点赞数进行排序

    :param book_name: 书名
    :param author_name: 作者名
    :return: 搜索结果
    """
    sql = 'SELECT * FROM books '

    args = f'%{book_name}%', f'%{author_name}%', book_name, book_name, author_name, author_name
    if book_name is not None and author_name is not None:
        sql += "WHERE bookName LIKE %s OR authorName LIKE %s " \
               "ORDER BY instr(bookName,%s)=0, CHAR_LENGTH(bookName), instr(bookName,%s)," \
               "instr(authorName,%s)=0, CHAR_LENGTH(authorName), instr(authorName,%s), likes DESC"
    elif book_name is not None:
        sql += "WHERE bookName LIKE %s " \
               "ORDER BY instr(bookName,%s)=0, CHAR_LENGTH(bookName), instr(bookName,%s), likes DESC"
        args = f'%{book_name}%', book_name, book_name
    elif author_name is not None:
        sql += "WHERE authorName LIKE %s " \
               "ORDER BY instr(authorName,%s)=0, CHAR_LENGTH(authorName), instr(authorName,%s), likes DESC"
        args = f'%{author_name}%', author_name, author_name
    else:
        raise NetworkException(400, '请输入书名或作者名')

    results = execute_sql_query(pooldb, sql, args)
    return results


def add_book_sql(uid, bid, progress, added=1):
    """将用户书本阅读信息写入数据库，默认加入书架"""
    add_sql = 'insert into user_read_info(userId, bookId, progress,added) values(%s,%s,%s,%s)'
    args = uid, bid, progress, int(added)  # tuple
    execute_sql_write(pooldb, add_sql, args)


def read_book_sql(uid, bid, progress):
    """将用户书本阅读信息写入数据库，不加入书架"""
    add_book_sql(uid, bid, progress, added=0)


def add_bookshelf_sql(uid, bid, progress):
    """将已读书籍加入书架"""
    add_sql = 'update user_read_info set progress=%s, added=1 where userId=%s and bookId=%s'
    args = progress, uid, bid
    execute_sql_write(pooldb, add_sql, args)


def update_book_sql(uid, bid, progress):
    """ 更新用户书本阅读进度 """
    update_sql = 'update user_read_info set progress=%s where userId=%s and bookId=%s'
    args = progress, uid, bid
    execute_sql_write(pooldb, update_sql, args)


def del_book_sql(uid, bid):
    """将用户书本阅读信息写入数据库"""
    del_sql = 'delete from user_read_info where userId=%s and bookId=%s'
    args = uid, bid
    execute_sql_write(pooldb, del_sql, args)


@bp.route('/add', methods=['POST'])
def add():
    if request.method == 'POST':
        try:
            user = check_user_before_request(request)
            uid, bid = user['id'], request.json.get('bookId')
            progress = request.json.get('progress', 0)
            added = 1

            # uid bid 其中一个为空
            if not all([uid, bid]):
                raise Exception('userId 或 bookId 缺失')

            if check_book_added(pooldb, uid, bid):
                raise Exception('该书已加入书架，无需重复加入')
            elif check_book_read(pooldb, uid, bid):
                # 如果读过书，即表中有数据，放入书架即可
                add_bookshelf_sql(uid, bid, progress)
            else:
                # 将书本信息写入数据库，并加入书架
                add_book_sql(uid, bid, progress, added)
            response = build_redirect_response(f'添加书{bid}，重定向至书架页', url_for('bookshelf.index'))
        except NetworkException as e:
            response = build_error_response(code=e.code, msg=e.msg)
        except Exception as e:
            print("[ERROR]" + __file__ + "::" + inspect.getframeinfo(inspect.currentframe().f_back)[2])
            print(e)
            response = build_error_response(msg=str(e))
        return response


@bp.route('/read_try', methods=['GET', 'POST'])
def read_try():
    """
    尝试阅读，不加入书架。
    GET 获取读过的书籍信息，包括书架上的
    POST 将阅读的信息写入数据库，但不加入书架。
    注意：更新阅读进度使用 update
    """
    if request.method == 'POST':
        try:
            user = check_user_before_request(request)
            uid, bid = user['id'], request.json.get('bookId')
            progress = request.json.get('progress', 0)
            added = 0

            # uid bid 其中一个为空
            if not all([uid, bid]):
                raise Exception('userId 或 bookId 缺失')

            if check_book_read(pooldb, uid, bid):
                raise Exception('该书已经读过，请勿重复增加阅读信息')
            else:
                # 将书本阅读信息写入数据库
                add_book_sql(uid, bid, progress, added)
            response = build_success_response(f'开始阅读书{bid}')
        except NetworkException as e:
            response = build_error_response(code=e.code, msg=e.msg)
        except Exception as e:
            print("[ERROR]" + __file__ + "::" + inspect.getframeinfo(inspect.currentframe().f_back)[2])
            print(e)
            response = build_error_response(msg=str(e))
        return response
    elif request.method == 'GET':
        try:
            user = check_user_before_request(request)
            books = get_books(user['id'], added=0)
            # print(type(books[0]), books[0]) if len(books) > 0 else print('get 0 books from user:', books)
            data = {
                "size": len(books),
                "data": books
            }
            response = build_success_response(data)
        except NetworkException as e:
            response = build_error_response(code=e.code, msg=e.msg)
        except Exception as e:
            print("[ERROR]" + __file__ + "::" + inspect.getframeinfo(inspect.currentframe().f_back)[2])
            print(e)
            response = build_error_response(msg=str(e))
        return response


@bp.route('/update', methods=['POST'])
def update():
    """ 更新阅读进度 """
    if request.method == 'POST':
        try:
            user = check_user_before_request(request)
            uid, bid = user['id'], request.json.get('bookId')
            progress = request.json.get('progress', 0)

            # uid bid 其中一个为空
            if not all([uid, bid]):
                raise Exception('userId 或 bookId 缺失')

            # 已读过
            if check_book_read(pooldb, uid, bid):
                update_book_sql(uid, bid, progress)
                msg = f'书{bid}更新阅读进度'
            else:
                read_book_sql(uid, bid, progress)
                msg = f'没有读过书{bid}，现已添加'
            response = build_success_response(msg=msg)
        # check_user_before_request会抛出NetworkException，这是自定义的Exception，用于构造error_reponse
        except NetworkException as e:
            response = build_error_response(code=e.code, msg=e.msg)
        except Exception as e:
            print("[ERROR]" + __file__ + "::" + inspect.getframeinfo(inspect.currentframe().f_back)[2])
            print(e)
            response = build_error_response(msg=str(e))
        return response


@bp.route('/delete', methods=['POST'])
def delete():
    if request.method == 'POST':
        try:
            user = check_user_before_request(request)
            uid, bid = user['id'], request.json.get('bookId')

            # uid bid 其中一个为空
            if not all([uid, bid]):
                raise NetworkException(400, 'userId 或 bookId 缺失')

            # 书架上已有
            if check_book_added(pooldb, uid, bid):
                # 数据库中删除书籍信息
                del_book_sql(uid, bid)
            else:
                raise NetworkException(400, f'书架上没有书{bid}，无法删除')
            response = build_redirect_response(f'删除书{bid}，重定向至书架页', url_for('bookshelf.index'))
        except NetworkException as e:
            response = build_error_response(code=e.code, msg=e.msg)
        except Exception as e:
            print("[ERROR]" + __file__ + "::" + inspect.getframeinfo(inspect.currentframe().f_back)[2])
            print(e)
            response = build_error_response(msg=str(e))

        return response


@bp.route('/list', methods=['GET'])
def index():
    """展示用户书架"""
    # books: [{},{}]
    if request.method == 'GET':
        try:
            user = check_user_before_request(request)
            books = get_books(user['id'])
            # print(type(books[0]), books[0]) if len(books) > 0 else print('get 0 books from user:', books)
            data = {
                "size": len(books),
                "data": books
            }
            response = build_success_response(data)
        except NetworkException as e:
            response = build_error_response(code=e.code, msg=e.msg)
        except Exception as e:
            print("[ERROR]" + __file__ + "::" + inspect.getframeinfo(inspect.currentframe().f_back)[2])
            print(e)
            response = build_error_response(msg=str(e))
        return response


@bp.route('/search', methods=['GET'])
def search():
    if request.method == 'GET':
        try:
            # 检查是否有用户 token ，有返回用户，否则返回 None
            # user = check_user_before_request(request, raise_exc=False)
            book_name = request.args.get('bookName', None)
            author_name = request.args.get('authorName', None)
            results = search_books(book_name, author_name)
            data = {
                "size": len(results),
                "data": results
            }
            response = build_success_response(data, msg='搜索成功')
        except NetworkException as e:
            response = build_error_response(code=e.code, msg=e.msg)
        except Exception as e:
            print("[ERROR]" + __file__ + "::" + inspect.getframeinfo(inspect.currentframe().f_back)[2])
            print(e)
            response = build_error_response(msg=str(e))
        return response
