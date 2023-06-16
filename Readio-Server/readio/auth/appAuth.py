import functools
from typing import List

from flask import request
from flask import Blueprint
from flask import redirect
from flask import url_for
from werkzeug.security import check_password_hash, generate_password_hash
# from readio.manage.userManage import __get_all_authorId_id_by_userid, __get_all_followerId_id_by_userid
from readio.auth.routerdata import admin_router_data, common_router_data, manager_router_data
from readio.manage.fileManage import __upload_file_binary_sql
from readio.utils.buildResponse import *
from readio.utils.auth import *
import readio.database.connectPool
import readio.utils.check as check
from readio.utils.executeSQL import execute_sql_query

# appAuth = Blueprint('/auth/app', __name__)
bp = Blueprint('auth', __name__, url_prefix='/app/auth')

pooldb = readio.database.connectPool.pooldb


def authorize_phoneNumber_password(phoneNumber, passWord):
    try:
        conn, cursor = pooldb.get_conn()
        cursor.execute('select * from users where phoneNumber=%s', (phoneNumber))
        user = cursor.fetchone()
        pooldb.close_conn(conn, cursor)
        if user is None:
            raise Exception('用户名不正确')

        if not check_password_hash(user['passWord'], passWord):
            raise Exception('密码不正确')

        # 都正确了，开始创建会话
        print('验证成功')
        return user
    except Exception as e:
        check.printException(e)
        pooldb.close_conn(conn, cursor) if conn is not None else None
        return None


def authorize_userId_password(userId, passWord):
    try:
        conn, cursor = pooldb.get_conn()
        cursor.execute('select * from users where id=%s', (userId))
        user = cursor.fetchone()
        pooldb.close_conn(conn, cursor)
        if user is None:
            raise Exception('用户名不正确')

        if not check_password_hash(user['passWord'], passWord):
            raise Exception('密码不正确')

        # 都正确了，开始创建会话
        print('验证成功')
        return user
    except Exception as e:
        check.printException(e)
        pooldb.close_conn(conn, cursor) if conn is not None else None
        return None


# 用户注册的sql语句, 默认的用户名随机生成，用户用phoneNumber和password注册
def register_user_sql(password, phoneNumber):
    try:
        conn, cursor = pooldb.get_conn()
        cursor.execute('insert into users(username,password,phonenumber,roles) values(%s,%s,%s,%s)',
                       (random_gen_username(), generate_password_hash(password), phoneNumber, 'common'))
        conn.commit()
        pooldb.close_conn(conn, cursor)

    except Exception as e:
        pooldb.close_conn(conn, cursor) if conn is not None else None
        raise e


# 检查phoneNumber是不是唯一的，如果是则返回True，否则返回False
def checkPhoneNumberIsUnique(phoneNumer):
    try:
        conn, cursor = pooldb.get_conn()
        cursor.execute('select * from users where phonenumber=%s', (phoneNumer))
        rows = cursor.fetchall()
        pooldb.close_conn(conn, cursor)
        if (len(rows) == 0):
            return True
        return False

    except Exception as e:
        pooldb.close_conn(conn, cursor) if conn is not None else None
        raise e


@bp.route('/register', methods=['POST'])
def register():
    try:
        data = request.json
        if 'phoneNumber' not in data or 'passWord' not in data:
            raise Exception('前端数据错误！缺少phoneNumber或passWord')
        if not checkPhoneNumberIsUnique(data['phoneNumber']):
            return build_error_response(msg='该手机号已被注册')
        register_user_sql(data['passWord'], data['phoneNumber'])
        return build_success_response()

    except Exception as e:
        check.printException(e)
        return build_error_response(msg='注册失败')


# 收到用户名密码，返回会话对应的toKen
@bp.route('/login', methods=['POST'])
def login():
    try:
        data = request.json
        if 'phoneNumber' not in data or 'passWord' not in data:
            raise Exception('前端数据不正确，phoneNumber或passWord')
        phoneNumber = data['phoneNumber']

        password = data['passWord']
        user = authorize_phoneNumber_password(phoneNumber, password)
        if user is None:
            return build_error_response(msg='用户名或密码错误')

        token = build_session(user['id'])
        print('[DEBUG] get token, token = ', token)
        # tokenList.append(token)
        return build_success_response({"token": token})

    except Exception as e:
        check.printException(e)
        return build_error_response(msg='登录失败')


# 退出登录，本质上就是删除与用户建立的对话
@bp.route('/logout', methods=['GET'])
def logout():
    try:
        token = request.headers.get('Authorization')
        if token is None:
            return build_success_response()
        conn, cursor = pooldb.get_conn()
        cursor.execute('delete from user_token where token=%s', (token))
        conn.commit()
        pooldb.close_conn(conn, cursor)
        return build_success_response()

    except Exception as e:
        check.printException(e)
        pooldb.close_conn(conn, cursor) if conn is not None else None
        return build_error_response()


# data里面应该有user的所有信息
def user_profile_update_user_sql(userId, data):
    try:
        conn, cursor = pooldb.get_conn()
        sql = 'update users set userName=%s,email=%s where id=%s'
        cursor.execute(sql, (data['userName'], data['email'], userId))
        conn.commit()
        pooldb.close_conn(conn, cursor)

    except Exception as e:
        pooldb.close_conn(conn, cursor) if conn is not None else None
        raise e


def __get_all_followerId_id_by_userid(userId) -> List[Dict]:
    """
    通过用户Id来获取该用户所有collect的pieces的id
    """
    sql = 'select followerId from user_subscribe where authorId=%s'
    rows = execute_sql_query(pooldb, sql, userId)
    rows = list(map(lambda x: int(x['followerId']), rows))
    return rows


def __get_all_authorId_id_by_userid(userId) -> List[Dict]:
    """
    通过用户Id来获取该用户所有collect的pieces的id
    """
    sql = 'select authorId from user_subscribe where followerId=%s'
    rows = execute_sql_query(pooldb, sql, userId)
    rows = list(map(lambda x: int(x['authorId']), rows))
    return rows


# 获取用户详细信息
@bp.route('/profile', methods=['GET', 'POST'])
def profile():
    try:
        if request.method == 'GET':
            user = check_user_before_request(request)
            fansNum = len(__get_all_followerId_id_by_userid(user['id']))
            subscribeNum = len(__get_all_authorId_id_by_userid(user['id']))

            response = {
                "userInfo": {
                    "userId": user['id'],
                    "userName": user['userName'],
                    "email": user['email'],
                    "phoneNumber": user['phoneNumber'],
                    "avator": user['avator'],
                    "createTime": user['createTime'],
                    "followerNum": fansNum,
                    "subscribeNum": subscribeNum,
                    "hotNum": 678,
                    "roles": [user['roles']],
                    "permissions": ["*:*:*"],
                    "roleGroup": USER_ROLE_MAP[user['roles']]
                }
            }

            return build_success_response(response)

        elif request.method == 'POST':
            token = request.headers.get('Authorization')
            if token is None:
                raise Exception('token不存在，无法修改信息')

            user = check_user_before_request(request)

            data = request.json
            user_profile_update_user_sql(user['id'], data)

            return build_success_response()

    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)
    except Exception as e:
        check.printException(e)
        return build_error_response()


def user_profile_update_user_pwd(uid, pwd):
    try:
        sql = 'update users set passWord=%s where id=%s'
        conn, cursor = pooldb.get_conn()
        cursor.execute(sql, (generate_password_hash(pwd), uid))
        conn.commit()
        pooldb.close_conn(conn, cursor)
    except Exception as e:
        check.printException(e)
        pooldb.close_conn(conn, cursor) if conn is not None else None
        raise Exception(f'用户{uid}密码修改失败')


@bp.route('/profile/updatePwd', methods=['POST'])
def updatePwd():
    try:
        data = request.json
        if 'oldPassword' not in data or 'newPassword' not in data:
            raise NetworkException(400, '前端数据错误，不存在oldPassword或newPassword')

        user = check_user_before_request(request)

        res = authorize_userId_password(user['id'], data['oldPassword'])
        if res is None:
            raise NetworkException(400, '密码不正确')

        user_profile_update_user_pwd(user['id'], data['newPassword'])

        return build_success_response()

    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)
    except Exception as e:
        check.printException(e)
        return build_error_response(500, "服务器内部错误")


@bp.route('/getRouters', methods=['GET'])
def get_routers():
    try:
        user = check_user_before_request(request)
        if user['roles'] == 'admin':
            return build_success_response(admin_router_data)
        elif user['roles'] == 'manager':
            return build_success_response(manager_router_data)
        elif user['roles'] == 'common':
            return build_success_response(common_router_data)

        return build_success_response(common_router_data)

    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)
    except Exception as e:
        check.printException(e)
        return build_error_response()


def __user_avatar_add_sql(userId, fileId, trans=None):
    sql = 'insert into user_avatar_history(userId, fileId) values(%s, %s) '
    if trans is None:
        execute_sql_write(sql, (userId, fileId))
    else:
        trans.execute(sql, (userId, fileId))


def __user_avatar_query_sql(queryParam: dict) -> List[Dict]:
    sql = 'select * from user_avatar_history '
    value_list = []
    sql_list = []
    if 'fileId' in queryParam or 'userId' in queryParam:
        sql += ' where 1=1 '
        if 'fileId' in queryParam:
            value_list.append(queryParam['fileId'])
            sql_list.append(' and fileId = %s ')
        if 'userId' in queryParam:
            value_list.append(queryParam['userId'])
            sql_list.append(' and userId = %s ')

    for sql_str in sql_list:
        sql += sql_str
    sql += ' order by updateTime desc '
    rows = execute_sql_query(pooldb, sql, tuple(value_list))
    return rows


def __check_if_history_is_exist(userId, fileId) -> bool:
    queryParam = {}
    queryParam['fileId'] = fileId
    queryParam['userId'] = userId
    rows = __user_avatar_query_sql(queryParam)
    if rows is not None and len(rows) > 0:
        return True
    return False


def __update_user_avatar_updateTime(userId, fileId, trans=None):
    sql = 'update user_avatar_history set updateTime=now() where userId=%s and fileId=%s'
    if trans is None:
        execute_sql_write(pooldb, sql, (userId, fileId))
    else:
        trans.execute(sql, (userId, fileId))

def __update_user_avatar(userId, fileId, trans=None):
    sql = 'update users set avator=%s where id = %s'
    if trans is None:
        execute_sql_write(pooldb, sql, (fileId, userId))
    else:
        trans.execute(sql, (fileId, userId))

# 用户历史头像
@bp.route('/avatar/add', methods=['POST'])
def add_user_avatar():
    try:
        data = request.json
        if 'fileName' not in data or 'fileType' not in data or 'fileContent' not in data:
            return build_error_response(400, '上传错误，fileName,fileType,fileContent信息不全')

        user = check_user_before_request(request)
        trans = SqlTransaction(pooldb)
        trans.begin()
        fileId = __upload_file_binary_sql(data, trans)
        userId = user['id']
        if __check_if_history_is_exist(userId, fileId):
            # 如果存在了，就只更新一下时间
            __update_user_avatar_updateTime(userId, fileId, trans)
        else:
            # 否则就加进去
            __user_avatar_add_sql(userId, fileId, trans)

        __update_user_avatar(userId, fileId, trans)
        trans.commit()
        return build_success_response()

    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)
    except Exception as e:
        check.printException(e)
        return build_error_response(500, "服务器内部错误")

@bp.route('/avatar/get', methods=['GET'])
def get_user_avatar():
    try:
        user = check_user_before_request(request)
        queryParam = {}
        queryParam['userId'] = user['id']
        rows = __user_avatar_query_sql(queryParam)

        return build_success_response(data=rows, length=len(rows))

    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)
    except Exception as e:
        check.printException(e)
        return build_error_response(500, "服务器内部错误")

@bp.route('/avatar/update', methods=['POST'])
def update_user_avatar():
    try:
        data = request.json
        if 'fileId' not in data:
            return build_error_response(400, '上传错误，fileId缺失')

        fileId = data['fileId']
        user = check_user_before_request(request)
        trans = SqlTransaction(pooldb)
        trans.begin()
        userId = user['id']
        if __check_if_history_is_exist(userId, fileId):
            # 如果存在了，就只更新一下时间
            __update_user_avatar_updateTime(userId, fileId, trans)
        else:
            # 否则就加进去
            __user_avatar_add_sql(userId, fileId, trans)

        __update_user_avatar(userId, fileId, trans)
        trans.commit()
        return build_success_response()

    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)
    except Exception as e:
        check.printException(e)
        return build_error_response(500, "服务器内部错误")