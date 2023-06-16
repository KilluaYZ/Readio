from setuptools import find_packages, setup
# 1234c
setup(
    name='readio',
    version='1.0.0',
    packages=[
        'readio',
        'readio.auth',
        'readio.database',
        'readio.mainpage',
        'readio.manage',
        'readio.utils',
        'readio.monitor',
        ],
    include_package_data=True,
    zip_safe=False,
    install_requires=[
        'flask',
        'PyMySql',
        'flask_cors',
        'sqlalchemy',
        'DButils',
        'wordcloud',
        'psutil',
        'flask_apscheduler'
    ],
)