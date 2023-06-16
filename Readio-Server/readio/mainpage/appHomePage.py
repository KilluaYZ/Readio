import inspect
import random

from flask import Blueprint
from flask import request

import readio.database.connectPool
from readio.utils.buildResponse import *
from readio.utils.auth import check_user_before_request
from readio.utils.myExceptions import NetworkException
from readio.utils.executeSQL import execute_sql_query, execute_sql_query_one

# 前缀为app的蓝图
bp = Blueprint('homepage', __name__, url_prefix="/app")

pooldb = readio.database.connectPool.pooldb


def get_sentences():
    get_sql = "select * from sentences"
    sentences = execute_sql_query(pooldb, get_sql, ())
    return sentences


def __random_choose_sentences_by_args(args: dict, id_type: list, size: int):
    """
    args为dict，其中放入type和概率的键值对，可选的key有
    Anime, book, Literature, Other, Poem, Philosophy,
    Comic, Game, Original, Internet, Video, NCM, Funny,
    所有概率值之和不能大于1，如果大于一则会将前n项的和不大于1的
    键值对纳入考虑范围，如果第一项就大于1，则会完全随机
    """
    tot = 0
    res = []
    # 根据所给参数随机选择
    key_size_dict = {}
    for key, val in args.items():
        if tot + val <= 1:
            key_size_dict[key] = int(val * size)
            tot += val

    for key, val in key_size_dict.items():
        tmp_list = []
        for item in id_type:
            if item['type'] == key:
                tmp_list.append(item)
        res = res + random.sample(tmp_list, val)

    # 加入剩余的
    cnt = 0
    for val in key_size_dict.values():
        cnt += val
    if cnt < size:
        res = res + random.sample(id_type, size - cnt)
    res = list(map(lambda x: int(x['id']), res))
    return res


# 从数据库中随机选一些句子
def get_random_sentences(size):
    # count_sql = "select COUNT(*) from sentences"
    # sentences_count = execute_sql_query_one(pooldb, count_sql, ())
    # all_size = sentences_count['COUNT(*)']  # 句子总数
    # # 从 id 序列中随机选取 size 个不重复的 id
    # ids = random.sample(range(1, all_size + 1), size)
    sql = ' select id, type from sentences '
    rows = execute_sql_query(pooldb, sql)
    args = {"book": 0.2, "Literature": 0.3}
    ids = __random_choose_sentences_by_args(args, rows, size)

    # 将 id 转换成字符串并用逗号拼接
    id_string = ','.join(str(i) for i in ids)
    # 构造 SQL 语句
    # get_random_sql = f"SELECT * FROM sentences WHERE id IN ({id_string})"
    get_random_sql = f"SELECT s.*, b.bookName, b.authorName " \
                     f"FROM sentences s " \
                     f"LEFT JOIN books b ON s.bookId = b.id " \
                     f"WHERE s.id IN ({id_string})"
    sentences = execute_sql_query(pooldb, get_random_sql, ())  # 执行 SQL 语句并返回结果
    return sentences


def rec_random(all_sent, size):
    sentences = list()
    for _ in range(size):
        i = random.randint(0, len(all_sent) - 1)  # 生成随机数
        sent = all_sent[i]
        sent["createTime"] = str(sent["createTime"])
        sentences.append(sent)  # 取出句子并添加到列表中
    return sentences


def rec_sent(all_sent, size, user=None):
    if user is None:
        sentences = rec_random(all_sent, size)
    else:
        sentences = rec_random(all_sent, size)
    return sentences


@bp.route('/homepage', methods=['GET'])
def recommend():
    """ 推荐好句 """
    if request.method == 'GET':
        try:
            size = int(request.headers.get('size', 10))
            user = check_user_before_request(request, raise_exc=False)
            # sentences_all, sentences_rec -> [{},{}]
            # 拿到所有数据然后随机选，性能较差
            # sentences_all = get_sentences()
            # sentences_rec = rec_sent(sentences_all, size, user)
            # 生成随机 id，然后选
            sentences_rec = get_random_sentences(size)
            response = {
                "size": len(sentences_rec),
                "data": sentences_rec
            }
            response = build_success_response(response)
        except NetworkException as e:
            response = build_error_response(code=e.code, msg=e.msg)
        except Exception as e:
            print("[ERROR]" + __file__ + "::" + inspect.getframeinfo(inspect.currentframe().f_back)[2])
            print(e)
            response = build_error_response(msg=str(e))
        return response
