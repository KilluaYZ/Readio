import os
import tempfile
import json
import pytest

from readio import create_app
from typing import Dict, Tuple, Optional


@pytest.fixture
def app():
    """Create and configure a new app instance for each test."""
    # create the app with common test config
    app = create_app()

    yield app


@pytest.fixture
def client(app):
    """A test client for the app."""
    return app.test_client()


@pytest.fixture
def runner(app):
    """A test runner for the app's Click commands."""
    return app.test_cli_runner()


class AuthActions:
    def __init__(self, client, token=None):
        self._client = client
        self.token = token

    def login(self, user_data=None):
        if user_data is None:
            user_data = {
                "phoneNumber": "19800380001",
                "passWord": "123456"
            }
        response = self._client.post(
            "/app/auth/login", json=user_data
        )
        self.token = response2dict(response)['data']['token']
        return response

    def logout(self):
        return self._client.get("/app/auth/logout")


@pytest.fixture
def auth(client):
    return AuthActions(client)


# 将 response 解析为可显示中文的 dict
def response2dict(response):
    # response.data.decode('utf-8') <str>
    # json.loads(): JSON 格式的字符串转换为 dict、list、str 等
    data_str = response.data.decode('utf-8')
    try:
        return json.loads(data_str)
    except:
        return data_str
