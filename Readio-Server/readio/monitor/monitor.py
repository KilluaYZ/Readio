import platform
from psutil import *
import socket
from flask import request
from flask import Blueprint
import os
import sys
import inspect

from readio.utils import check
from readio.utils.buildResponse import *
from readio.utils.auth import check_user_before_request
from readio.utils.myExceptions import NetworkException

monitor = Blueprint('monitor', __name__)
from readio.utils.auth import get_user_by_token, check_tokens_get_state


def get_host_ip():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(('8.8.8.8', 80))
        ip = s.getsockname()[0]
    finally:
        s.close()

    return ip


@monitor.route('/server', methods=['GET'])
def getPlantformInfo():
    try:
        check_user_before_request(request, "admin")

        cpu_used = cpu_percent(interval=2)
        cpu_free = 100.0 - cpu_used
        mem_info = virtual_memory()

        res = {
            "cpu": {
                "cpuType": str(platform.processor()),
                "used": str(cpu_used),
                "free": str(cpu_free)
            },
            "mem": {
                "total": "%.2f" % (mem_info[0] / (float)(1024 * 1024 * 1024)),
                "used": "%.2f" % (mem_info[3] / (float)(1024 * 1024 * 1024)),
                "free": "%.2f" % (mem_info[4] / (float)(1024 * 1024 * 1024)),
                "usage": "%.2f" % (mem_info[2])
            },
            "sys": {
                "computerName": str(platform.node()),
                "computerIp": str(get_host_ip()),
                "osName": str(platform.platform()),
                "osArch": str(platform.machine())
            }
        }
        return build_success_response(res)


    except NetworkException as e:

        return build_error_response(code=e.code, msg=e.msg)


    except Exception as e:

        check.printException(e)

        return build_error_response(code=500, msg="服务器内部错误")
