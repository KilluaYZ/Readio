import json
# from dbtest.showdata10 import db # 引入其他蓝图
import re
from typing import Dict, Tuple, Optional

from apscheduler.schedulers.background import BackgroundScheduler
from flask import Flask, url_for
from flask_cors import CORS  # 跨域

# app
from readio.auth import appAuth
from readio.database.init_db import init_db
from readio.mainpage import appHomePage, appBookShelfPage, appBookDetailsPage, appBookReadPage
from readio.manage.fileManage import getFilePathById
from readio.monitor.monitor import monitor
from readio.manage import fileManage, worksManage, userManage
from readio.utils.json import CustomJSONEncoder


# 创建flask app
def create_app():
    # create and configure the app
    app = Flask(__name__, instance_relative_config=True)
    CORS(app)

    app.json_encoder = CustomJSONEncoder

    # a simple page that says hello
    @app.route('/hello')
    def hello():
        return 'Hello, World!'

    # 在应用中注册init_db
    @app.cli.command('init-db')
    def init_db_command():
        """删除现有的所有数据，并新建关系表"""
        init_db()

    app.register_blueprint(monitor, url_prefix='/monitor')
    app.register_blueprint(userManage.bp)
    app.register_blueprint(appAuth.bp)
    app.register_blueprint(appHomePage.bp)
    app.register_blueprint(appBookShelfPage.bp)
    app.register_blueprint(appBookDetailsPage.bp)
    app.register_blueprint(appBookReadPage.bp)
    app.register_blueprint(fileManage.bp)
    app.register_blueprint(worksManage.bp)

    # 配置定时任务
    # 该任务作用是每个一个小时检查一次user_token表，将超过1天未活动的token删掉（随便定的，后面改
    from readio.manage.userManage import checkSessionsAvailability
    scheduler = BackgroundScheduler()
    scheduler.add_job(func=checkSessionsAvailability,
                      id='checkSessionsAvailability',
                      trigger='interval',
                      seconds=3600,
                      replace_existing=True
                      )
    # 启动任务列表
    scheduler.start()
    """ 测试 """
    # print(f'[TEST] filePath = {getFilePathById("0658a5df12791200a99b5e0f26b03e2d53154567c759683d7b355982cff124a6")}')
    # app_test(app)

    return app


def app_test(app):
    with app.test_request_context():
        # 使用测试客户端发送请求
        client = app.test_client()
        # headers = {
        #     # 'Content-Type': 'application/json',
        # }
        """ test homepage """
        app_test_homepage(client)
        """ test auth """
        # app_test_auth(client)
        """ test bookshelf """
        app_test_bookshelf(client)
        """ test book details"""
        # app_test_book_details(client)
        """ test book reading """
        # app_test_book_reading(client)


def app_test_book_reading(client, login_data: Dict = None, headers=None):
    if login_data is None:
        login_data = {
            "phoneNumber": "19800380000",
            "passWord": "123456"
        }  # uid = 3
    if headers is None:
        headers = {}
        # login to get token
        resp_dict = client_test(client, get_url('auth.login'), 'POST', headers, login_data, print_info=False)
        token = resp_dict['data']['token']
        headers['Authorization'] = token
    resp_dict = {}
    """ ------------- book reading ------------- """
    # 1->epub 2/3->txt 4->mobi 14->pdf
    url_and_params = get_url('book_reading.index', book_id=3)
    client_test(client, url_and_params, 'GET', headers=headers)


def app_test_book_details(client, login_data: Dict = None, headers=None):
    if login_data is None:
        login_data = {
            "phoneNumber": "19800380000",
            "passWord": "123456"
        }  # uid = 3
    if headers is None:
        headers = {}
        # login to get token
        resp_dict = client_test(client, get_url('auth.login'), 'POST', headers, login_data, print_info=False)
        token = resp_dict['data']['token']
        headers['Authorization'] = token
        # headers['Authorization'] = '5cf397accdcf195bfe983af257fdbbf14e1fc83d'
        headers['depth'] = 3
    resp_dict = {}
    """ ------------- book details ------------- """
    """ show book details """
    url_and_params = get_url('book_detail.index', book_id=24)  # 3 -> 3
    client_test(client, url_and_params, 'GET', headers=headers)
    """ like """
    url_and_params = get_url('book_detail.update_like_book', book_id=3)
    like = {'bookId': 3, 'like': 1}
    # client_test(client, url_and_params, 'POST', headers=headers, json_data=like)
    """ add """
    # comment = {'content': '书很短，一下就读完。', 'bookId': 3}
    comment = {'content': '好言良劝增贤文', 'bookId': 3}
    url_and_params = get_url('book_detail.add_comments', book_id=3)
    # print(url_and_params)
    # resp_dict = client_test(client, url_and_params, 'POST', headers=headers, json_data=comment)
    """ update """
    bid, cid = 3, 1
    comment = {'bookId': bid, 'commentId': cid, 'content': '书很短，一下就读完。意很长，可受用终身。', 'like': 1}
    # comment = {'bookId': bid, 'commentId': cid, 'like': 0}
    url_and_params = get_url('book_detail.update_comments', book_id=bid, comment_id=cid)
    # client_test(client, url_and_params, 'POST', headers=headers, json_data=comment)
    """ del """
    # 新添加的，否则手动确定
    comment_id = extract_number(resp_dict['msg']) if len(resp_dict) > 0 else 10
    comment_to_del = {'bookId': 3, 'commentId': comment_id}
    url_and_params = get_url('book_detail.delete_comments', book_id=3)
    # client_test(client, url_and_params, 'POST', headers=headers, json_data=comment_to_del)
    """ ------------- comment details ------------- """
    # show comment details
    headers['easymode'] = True
    # print(headers)
    url_and_params = get_url('book_detail.index_comment', book_id=3, comment_id=1)
    # client_test(client, url_and_params, 'GET', headers=headers)
    # add
    reply_to = 1
    reply = {'content': f'对{reply_to}的回复-to-del2', 'bookId': 3, 'commentId': reply_to}
    url_and_params = get_url('book_detail.reply_comment', book_id=3)
    # client_test(client, url_and_params, 'POST', headers=headers, json_data=reply)
    # update
    bid, cid = 3, 1
    # comment = {'bookId': bid, 'commentId': cid, 'content': '书很短，一下就读完。意很长，可受用终身。', 'like': 1}
    comment = {'bookId': bid, 'commentId': cid, 'like': 1}
    url_and_params = get_url('book_detail.update_comment', book_id=bid, comment_id=cid)
    # client_test(client, url_and_params, 'POST', headers=headers, json_data=comment)
    # del test: 14 11 9
    reply_to_del = 16
    reply_del = {'bookId': 3, 'commentId': reply_to_del}
    url_and_params = get_url('book_detail.del_replies', book_id=3)
    # client_test(client, url_and_params, 'POST', headers=headers, json_data=reply_del)


def app_test_bookshelf(client, login_data: Dict = None, headers=None):
    if login_data is None:
        login_data = {
            "phoneNumber": "19800380000",
            "passWord": "123456"
        }
    read_info = dict()
    if headers is None:
        headers = {}
        # login to get token
        resp_dict = client_test(client, get_url('auth.login'), 'POST', headers, login_data, print_info=False)
        token = resp_dict['data']['token']
        headers['Authorization'] = token
        profile_dict = client_test(client, get_url('auth.profile'), 'GET', headers, print_info=False)
        uid = profile_dict['data']['userInfo']['userId']
        read_info['userId'] = uid
    # list
    client_test(client, get_url('bookshelf.index'), 'GET', headers=headers)
    # read_try
    client_test(client, get_url('bookshelf.read_try'), 'GET', headers=headers)
    read_info['bookId'] = 1
    read_info['progress'] = 6
    # client_test(client, get_url('bookshelf.read_try'), 'POST', headers=headers, json_data=read_info)
    # add
    # read_info['bookId'] = 7
    # read_info['added'] = 1
    # client_test(client, get_url('bookshelf.add'), 'POST', headers=headers, json_data=read_info)
    # update
    # client_test(client, get_url('bookshelf.update'), 'POST', headers=headers, json_data=read_info)
    # del
    # client_test(client, get_url('bookshelf.delete'), 'POST', headers=headers, json_data=read_info)
    """ ------------- search books ------------- """
    url_and_params = get_url('bookshelf.search')
    headers['bookName'] = '资本论'
    headers['authorName'] = '克'
    # client_test(client, url_and_params, 'GET', headers=headers)


def extract_number(text: str, n: int = 1) -> Optional[int]:
    """
    从字符串中提取数字，返回第 n 个匹配到的数字。默认返回第一个数字。

    :param text: 要提取数字的字符串。
    :param n: 要返回的数字在所有匹配到的数字中的位置，从 1 开始计数。默认为 1。
    :return: 返回指定位置的数字，类型为 int。如果没有找到数字，则返回 None。
    """
    pattern = r'\d+'
    matches = re.findall(pattern, text)
    if len(matches) == 0:
        return None
    return int(matches[n - 1]) if n <= len(matches) else None


# 将 response 解析为可显示中文的 dict
def response2dict(response):
    # response.data.decode('utf-8') <str>
    # json.loads(): JSON 格式的字符串转换为 dict、list、str 等
    data_str = response.data.decode('utf-8')
    try:
        return json.loads(data_str)
    except:
        return data_str


def json_append(json_old, key: str, value):
    dict_json = json.loads(json_old)
    dict_json[key] = value
    return json.dumps(dict_json)


def get_url(view_func_name: str, **kwargs) -> Tuple[str, dict]:
    """
    构造 URL 和查询参数，并返回二元组 (url, params)
    """
    url = url_for(view_func_name, **kwargs)  # _external=True
    params = {}
    return url, params


# post return dict
# def client_test(client, url_for_: str, method: str, headers, json_data=None, print_info=True) -> Dict[str, any]:
def client_test(client, url_and_params: Tuple[str, dict],
                method: str, headers, json_data=None, print_info=True) -> Dict[str, any]:
    url, params = url_and_params
    print('\t------------') if print_info else None
    print('\t| [TEST INFO] url = ', url) if print_info else None
    if method == 'POST':
        response = response2dict(client.post(url, headers=headers, json=json_data))
    elif method == 'GET':
        headers.update(json_data) if json_data is not None else None
        response = response2dict(client.get(url, headers=headers))
    else:
        response = 'test: No method!'
    print("\t| [TEST INFO] client get:", type(response), "\n\t| ", response) if print_info else None
    print('\t------------') if print_info else None
    return response


def app_test_homepage(client, headers=None):
    if headers is None:
        headers = {"size": 2}
    # 获取url: /app/homepage
    # homepage.recommend
    url_and_params = get_url('homepage.recommend')
    client_test(client, url_and_params, 'GET', headers=headers)
    # client_test(client, 'homepage.recommend', 'POST', headers)


def app_test_auth(client, headers=None, user_data=None):
    if headers is None:
        headers = {}
    if user_data is None:
        user_data = {
            "phoneNumber": "19800380000",
            "passWord": "123456"
        }
    # register
    # url_and_params = get_url('auth.register')
    # client_test(client, url_and_params, 'POST', headers=headers, data=user_data)
    # login
    url_and_params = get_url('auth.login')
    resp_dict = client_test(client, url_and_params, 'POST', headers=headers, json_data=user_data)
    # profile
    token = resp_dict['data']['token']
    # print(f'[INFO] token = {token}')
    user_data['Authorization'] = token
    # user_data['Authorization'] = 'a974075986a7519ddbeaff7dc3756c7b41a2db32'
    url_and_params = get_url('auth.profile')
    profile_dict = client_test(client, url_and_params, 'GET', headers=headers, json_data=user_data)
    uid = profile_dict['data']['userInfo']['userId'] if profile_dict is not None else None
    # print(id_)
    return token
