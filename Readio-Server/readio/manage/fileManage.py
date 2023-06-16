"""
文件管理层，用于向上层提供透明的图片、书籍、二创等大文件的存储服务
"""
import base64
import functools
import platform
import struct
import sys
from typing import BinaryIO

from flask import request
from flask import Blueprint
from flask import redirect
from flask import url_for
from flask import send_file
from werkzeug.security import check_password_hash, generate_password_hash
from readio.utils.buildResponse import *
from readio.utils.auth import *
import readio.database.connectPool
import readio.utils.check as check
from readio.utils.executeSQL import *
from random import  randint

# appAuth = Blueprint('/auth/app', __name__)
bp = Blueprint('file', __name__, url_prefix='/file')

pooldb = readio.database.connectPool.pooldb

BASE_FILE_STORE_DIR = './readio_server_runtime_data'
PICTURE_TYPES = ['jpeg', 'jpg', 'png', 'tif', 'gif', 'bmp', 'svg']
BOOK_TYPES = ['txt', 'pdf', 'mobi', 'epub']


def __getFileInfoById(id: str) -> dict:
    """
    通过id唯一地找到对应的文件信息
    """
    try:
        conn, cursor = pooldb.get_conn()
        cursor.execute('select * from file_info where fileId=%s', (id))
        row = cursor.fetchone()
        return row
    except Exception as e:
        check.printException(e)

    finally:
        if conn is not None:
            pooldb.close_conn(conn, cursor)


def __getFilesInfoByNameExact(name: str) -> list:
    """
    通过文件名，找到文件名完全匹配的文件信息列表
    """
    try:
        conn, cursor = pooldb.get_conn()
        cursor.execute('select * from file_info where fileName=%s', (name))
        row = cursor.fetchone()
        return row

    except Exception as e:
        check.printException(e)

    finally:
        if conn is not None:
            pooldb.close_conn(conn, cursor)


def __getFilesInfoByNameFuzzy(name: str) -> list:
    """
    通过文件名，找到文件名模糊匹配的文件信息列表
    """
    try:
        conn, cursor = pooldb.get_conn()
        cursor.execute('select * from file_info where fileName LIKE %s', (f'%{name}%'))
        row = cursor.fetchone()
        return row

    except Exception as e:
        check.printException(e)

    finally:
        if conn is not None:
            pooldb.close_conn(conn, cursor)


def __loadFileByte(fileInfo: dict):
    """
    fileInfo中要求存在id、type和path，函数会读入<path>/<id>.<type>的文件
    例如: path = /home  id = 123  type = jpg
    /home/123.jpg
    返回的是byte
    """
    if 'fileId' not in fileInfo or 'filePath' not in fileInfo or 'fileType' not in fileInfo:
        raise Exception('待读取fileInfo缺少path、type或id')
    with open(
            f"{os.path.join(BASE_FILE_STORE_DIR, os.path.join(fileInfo['filePath'], fileInfo['fileId']))}.{fileInfo['fileType']}",
            "rb") as f:
        content = f.read()
    return content


def __loadFileClass(fileInfo: dict) -> bytes:
    """
    fileInfo中要求存在id、type和path，函数会读入<path>/<id>.<type>的文件
    例如: path = /home  id = 123  type = jpg
    /home/123.jpg
    返回的是一个class
    """
    content = __loadFileByte(fileInfo)
    content = struct.unpack("<4H2I", content)
    return content


def __loadFileHandle(fileInfo: dict) -> BinaryIO:
    """
    通过fileInfo获取文件句柄
    """
    if 'fileId' not in fileInfo or 'filePath' not in fileInfo or 'fileType' not in fileInfo:
        raise Exception('待读取fileInfo缺少path、type或id')
    fileHandler = open(
        f"{os.path.join(BASE_FILE_STORE_DIR, os.path.join(fileInfo['filePath'], fileInfo['fileId']))}.{fileInfo['fileType']}",
        "rb")
    return fileHandler


def __rm_file(fileInfo: dict):
    """
        通过fileInfo删除文件
    """
    print(
        f"[DEBUG] __rm_file {os.path.join(BASE_FILE_STORE_DIR, os.path.join(fileInfo['filePath'], fileInfo['fileId']))}")
    os.remove(
        f"{os.path.join(BASE_FILE_STORE_DIR, os.path.join(fileInfo['filePath'], fileInfo['fileId']))}.{fileInfo['fileType']}")


def getFilesByteByNameExact(name: str) -> list:
    """
    通过Name获取文件二进制（精确的）
    """
    res = []
    fileInfoList = __getFilesInfoByNameExact(name)
    for fileInfo in fileInfoList:
        res.append(__loadFileByte(fileInfo))
    return res


def getFilesByteByNameFuzzy(name: str) -> list:
    """
    通过Name获取文件二进制（模糊的）
    """
    res = []
    fileInfoList = __getFilesInfoByNameFuzzy(name)
    for fileInfo in fileInfoList:
        res.append(__loadFileByte(fileInfo))
    return res


def getFilesHandlerByNameExact(name: str) -> list:
    """
    通过Name获取文件句柄（精确的）
    """
    res = []
    fileInfoList = __getFilesInfoByNameExact(name)
    for fileInfo in fileInfoList:
        res.append(__loadFileHandle(fileInfo))
    return res


def getFilesHandlerByNameFuzzy(name: str) -> list:
    """
    通过Name获取文件句柄（模糊的）
    """
    res = []
    fileInfoList = __getFilesInfoByNameFuzzy(name)
    for fileInfo in fileInfoList:
        res.append(__loadFileHandle(fileInfo))
    return res


def getClassFileByNameExact(name: str) -> list:
    """
    通过Name获取二进制文件并将其转化为class（精确的）
    """
    res = []
    fileInfoList = __getFilesInfoByNameExact(name)
    for fileInfo in fileInfoList:
        res.append(__loadFileClass(fileInfo))
    return res


def getClassFileByNameFuzzy(name: str) -> list:
    """
    通过Name获取二进制文件并将其转化为class（模糊的）
    """
    res = []
    fileInfoList = __getFilesInfoByNameFuzzy(name)
    for fileInfo in fileInfoList:
        res.append(__loadFileClass(fileInfo))
    return res


def getFileHandlerById(fileId: str) -> BinaryIO:
    """
    通过id拿到文件句柄
    """
    fileInfo = __getFileInfoById(fileId)
    return __loadFileHandle(fileInfo)


def getFileByteById(fileId: str):
    """
    通过id拿到文件二进制
    """
    fileInfo = __getFileInfoById(fileId)
    return __loadFileByte(fileInfo)


def getClassFileByteById(fileId: str) -> bytes:
    """
    通过id拿到文件二进制并转化为class
    """
    fileInfo = __getFileInfoById(fileId)
    return __loadFileClass(fileInfo)


def saveFileFromByte(fileInfo: dict, content):
    if 'fileId' not in fileInfo or 'filePath' not in fileInfo or 'fileType' not in fileInfo:
        raise Exception('待写入fileInfo缺少path、type或id')
    dir_abs_path = os.path.join(BASE_FILE_STORE_DIR, fileInfo['filePath'])
    # dir_abs_path = BASE_FILE_STORE_DIR + '/' + fileInfo['filePath']
    if not os.path.exists(dir_abs_path):
        os.makedirs(dir_abs_path)
    with open(f"{os.path.join(dir_abs_path, fileInfo['fileId'])}.{fileInfo['fileType']}",
              "wb") as f:
        f.write(content)


def saveFileFromClass(fileInfo: dict, content):
    content = struct.pack("<4H2I", *content)
    saveFileFromByte(fileInfo, content)


def getFilePathById(fileId: str) -> str:
    rows = __query_res_info_sql({"fileId": fileId})
    if rows is None or len(rows) == 0:
        return None
    fileInfo = rows[0]
    file_relative_path = f"{os.path.join(BASE_FILE_STORE_DIR, os.path.join(fileInfo['filePath'], fileInfo['fileId']))}.{fileInfo['fileType']}"
    return file_relative_path

@bp.route('/downloadBinary', methods=['GET'])
def downloadFileBinary():
    try:
        data = request.args
        if 'fileId' in data and 'fileName' in data:
            return build_error_response(code=400, msg='不能同时指定文件Id和Name')

        elif 'fileId' in data:
            fileId = data['fileId']
            fileInfo = __getFileInfoById(fileId)
            print("fileInfo = ", fileInfo)
            fileContent = getFileByteById(fileId)

            response = {
                "fileId": fileInfo['fileId'],
                "fileName": fileInfo['fileName'],
                "fileType": fileInfo['fileType'],
                # "fileContent": f'data:image/{fileInfo["fileType"]};base64,{str(base64.b64encode(fileContent))}'
                "fileContent": str(base64.b64encode(fileContent))
            }
            return build_success_response(data=response, msg='获取成功')

        elif 'fileName' in data:
            fileName = data['fileName']
            if 'mode' in data and data['mode'] == 'exact':
                fileInfoList = __getFilesInfoByNameExact(fileName)
            else:
                fileInfoList = __getFilesInfoByNameFuzzy(fileName)

            response = []
            for fileInfo in fileInfoList:
                fileContent = getFileByteById(fileInfo)
                singleFileResponse = {
                    "fileId": fileInfo['fileId'],
                    "fileName": fileInfo['fileName'],
                    "fileType": fileInfo['fileType'],
                    "fileContent": base64.b64encode(fileContent)
                }
                response.append(singleFileResponse)

            return build_success_response(data=response, msg='获取成功')

        else:
            return build_error_response(code=404, msg='资源不存在')

    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)

    except Exception as e:
        check.printException(e)
        return build_error_response(code=500, msg='服务器内部错误，无法获取该资源')


@bp.route('/getFileBinaryById', methods=['GET'])
def get_file_binary_by_id():
    try:
        data = request.args
        if 'fileId' not in data:
            raise NetworkException(code=400, msg='不能同时指定文件Id和Name')

        fileId = data['fileId']
        fileInfo = __getFileInfoById(fileId)
        if fileInfo is None:
            raise NetworkException(code=404, msg='资源不存在')
        # print("fileInfo = ", fileInfo)
        fileContentHandle = getFileHandlerById(fileId)
        if fileContentHandle is None:
            raise Exception("无法获取该文件的二进制数据")

        # response = {
        #     "fileId": fileInfo['fileId'],
        #     "fileName": fileInfo['fileName'],
        #     "fileType": fileInfo['fileType'],
        #     "fileContent": f'data:image/{fileInfo["fileType"]};base64,{str(base64.b64encode(fileContent))}'
        # }
        # return build_success_response(data=response, msg='获取成功')
        return send_file(fileContentHandle, fileInfo['fileType'],
                         download_name=f"{fileInfo['fileName']}.{fileInfo['fileType']}")

    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)
    except Exception as e:
        check.printException(e)
        return build_error_response(code=500, msg='服务器内部错误，无法获取该资源')


#上传文件后返回fileId
def __upload_file_binary_sql(fileInfo: dict, trans=None) -> str:

    fileType = fileInfo['fileType'].lower()
    fileInfo['fileType'] = fileInfo['fileType'].lower()
    fileName = fileInfo['fileName']
    fileContent = base64.b64decode(fileInfo['fileContent'])
    hashObj = hashlib.sha256()
    hashObj.update(fileContent)
    fileInfo['fileId'] = hashObj.hexdigest()
    if __check_if_fileId_is_exist(fileInfo['fileId']):
        return fileInfo['fileId']
        # raise NetworkException(400, '资源文件已经存在，请勿重复添加')

    if 'filePath' not in fileInfo:
        if fileType in PICTURE_TYPES:
            filePath = 'pic'
        elif fileType in BOOK_TYPES:
            filePath = 'book'
        else:
            filePath = 'default'
        fileInfo['filePath'] = filePath

    useForHeader = 0
    if 'useForHeader' in fileInfo:
        useForHeader = bool(fileInfo['useForHeader'])
        if useForHeader:
            useForHeader = 1
        else:
            useForHeader = 0

    saveFileFromByte(fileInfo, fileContent)

    if trans is None:
        execute_sql_write(pooldb ,'insert into file_info(fileId,fileName,fileType,filePath,useForHeader) values(%s,%s,%s,%s,%s)',
                          (fileInfo['fileId'], fileName, fileType, fileInfo['filePath'], useForHeader))
    else:
        trans.execute('insert into file_info(fileId,fileName,fileType,filePath,useForHeader) values(%s,%s,%s,%s,%s)',
                          (fileInfo['fileId'], fileName, fileType, fileInfo['filePath'],useForHeader))

    return fileInfo['fileId']

def __check_if_fileId_is_exist(fileId: str) -> bool:
    row = execute_sql_query_one(pooldb, 'select * from file_info where fileId = %s', fileId)
    if row is None:
        return False
    return True


@bp.route('/uploadFile', methods=['POST'])
def uploadFileBinary():
    try:
        data = request.json
        # print(f'[DEBUG] {data}')
        if 'fileName' not in data or 'fileType' not in data or 'fileContent' not in data:
            return build_error_response(400, '上传错误，fileName,fileType,fileContent信息不全')

        print(f'[DEBUG] fileContent = {data["fileContent"][:50]}')

        __upload_file_binary_sql(data)

        return build_success_response()


    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)

    except Exception as e:
        check.printException(e)
        return build_error_response(code=500, msg='服务器内部错误')


def del_file_sql(fileInfo: dict):
    try:
        conn, cursor = pooldb.get_conn()

        __rm_file(fileInfo)

        cursor.execute('delete from file_info where fileId = %s',
                       (fileInfo['fileId']))
        conn.commit()
        pooldb.close_conn(conn, cursor)

    except Exception as e:
        if conn is not None:
            pooldb.close_conn(conn, cursor)
        raise e


@bp.route('/delFile', methods=['GET'])
def deleteFile():
    try:
        check_user_before_request(request, roles='manager')
        data = request.args
        if 'fileId' not in data:
            return build_error_response(400, '上传错误，fileName,fileType,fileContent信息不全')

        rows = __query_res_info_sql(request.args)
        if rows is None or len(rows) <= 0:
            raise NetworkException(400, '找不到对应资源文件')
        fileInfo = rows[0]
        print(f'[DEBUG] fileInfo = {fileInfo}')
        # __rmFile(fileInfo)

        del_file_sql(fileInfo)

        print('finish')
        return build_success_response()

    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)

    except Exception as e:
        check.printException(e)
        return build_error_response(code=500, msg='服务器内部错误')


def __get_res_info_by_type_sql(type=None):
    try:
        conn, cursor = pooldb.get_conn()
        if type is None:
            cursor.execute('select * from file_info')

        else:
            cursor.execute('select * from file_info where fileType = %s', type)

        rows = cursor.fetchall()
        return rows
    except Exception as e:
        raise e

    finally:
        if conn is not None:
            pooldb.close_conn(conn, cursor)


def __query_res_info_sql(query_param: dict) -> list:
    try:
        conn, cursor = pooldb.get_conn()
        sql = f'select * from file_info'
        arg_list = []
        if 'fileName' in query_param or 'fileType' in query_param or 'fileId' in query_param:
            sql = sql + ' where 1=1 '
            if 'fileName' in query_param:
                sql += f' and fileName like %s '
                arg_list.append(f'%{query_param["fileName"]}%')
            if 'fileType' in query_param:
                sql += f' and fileType=%s '
                arg_list.append(query_param['fileType'])
            if 'fileId' in query_param:
                sql += f' and fileId=%s '
                arg_list.append(query_param['fileId'])

        if 'sortMode' in query_param:
            if query_param['sortMode'] == 'Old':
                sql += ' order by createTime asc '
            else:
                sql += ' order by createTime desc '
        else:
            sql += ' order by createTime desc '

        cursor.execute(sql, tuple(arg_list))
        rows = cursor.fetchall()
        for i in range(len(rows)):
            if int(rows[i]['useForHeader']) == 1:
                rows[i]['useForHeader'] = True
            else:
                rows[i]['useForHeader'] = False

        return rows

    except Exception as e:
        raise e

    finally:
        if conn is not None:
            pooldb.close_conn(conn, cursor)


@bp.route('/getFileInfo', methods=['GET'])
def getResInfo():
    try:
        rows = __query_res_info_sql(request.args)
        length = len(rows)
        # 如果前端传来了pageSize和pageNum则说明需要分页
        pageSize = request.args.get('pageSize')
        pageNum = request.args.get('pageNum')
        if pageNum is not None and pageSize is not None:
            pageSize = int(pageSize)
            pageNum = int(pageNum)
            rows = rows[(pageNum - 1) * pageSize:pageNum * pageSize]

        return build_success_response(data=rows, length=length)

    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)

    except Exception as e:
        print(e.with_traceback())
        return build_error_response(code=500, msg='服务器内部错误，无法获取该资源')


@bp.route('/updateFileInfo', methods=['POST'])
def update_res_info():
    try:
        fileName = request.json.get('fileName')
        fileId = request.json.get('fileId')
        useForHeader = request.json.get('useForHeader')
        if fileName is None or fileId is None:
            raise NetworkException(code=400, msg='前端数据错误，缺少fileName或fileId')
        useForHeader = bool(useForHeader)
        if useForHeader:
            useForHeader = 1
        else:
            useForHeader = 0
        execute_sql_write(pooldb, 'update file_info set fileName=%s, useForHeader = %s where fileId = %s', (fileName, useForHeader, fileId))

        return build_success_response()

    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)

    except Exception as e:
        print(e.with_traceback())
        return build_error_response(code=500, msg='服务器内部错误，无法获取该资源')


@bp.route('/randomGetImgFileInfo', methods=['GET'])
def random_get_img_id():
    try:
        sql = 'select count(*) as count from file_info where fileType in ' \
              ' ("webp", "jpg", "jpeg", "png", "gif", "svg") and useForHeader=1'
        count = execute_sql_query_one(pooldb, sql)
        if count is None:
            raise Exception("无法获取count")

        count = int(count['count'])
        print(f'[DEBUG] count = {count}')
        if count == 0:
            return build_success_response(data={})

        idx = randint(0, count-1)

        sql = 'select * from file_info where fileType in ' \
              '("webp", "jpg", "jpeg", "png", "gif", "svg") and useForHeader=1 ' \
              'limit 1 offset %s '

        row = execute_sql_query_one(pooldb, sql, idx)
        if row is None:
            raise Exception("无法获取随机的图片id")

        return build_success_response(data=row)

    except NetworkException as e:
        return build_error_response(code=e.code, msg=e.msg)

    except Exception as e:
        # check.printException(e)
        print(e.with_traceback())
        return build_error_response(code=500, msg='服务器内部错误，无法获取该资源')
