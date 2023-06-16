import pytest
from flask import g
from flask import session


def test_register(client, app):
    user_data = {
        "phoneNumber": "19800380001",
        "passWord": "123456"
    }
    # test that successful registration redirects to the login page
    response = client.post("/app/auth/register", json=user_data)
    assert response.status_code == 200


def test_login(client, auth):
    # test that successful login redirects to the index page
    response = auth.login()
    assert response.status_code == 200


# 传入两组非法测试样例
@pytest.mark.parametrize(
    ("user_data", "code"),
    (
            ({"phoneNumber": "198003800011", "passWord": "123456"}, b"400"),
            ({"phoneNumber": "19800380001", "passWord": "1"}, b"400")
    ),
)
def test_login_validate_input(auth, user_data, code):
    response = auth.login(user_data)
    assert code in response.data


def test_logout(client, auth):
    auth.login()
    with client:
        assert auth.logout().status_code == 200


def test_profile(client, auth):
    auth.login()
    with client:
        response = client.get("/app/auth/profile",
                              headers={'Authorization': auth.token})
        assert response.status_code == 200
