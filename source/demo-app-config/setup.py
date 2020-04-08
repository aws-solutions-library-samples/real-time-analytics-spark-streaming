# coding: utf-8

from setuptools import setup, find_packages

setup(
    name='demo_app_config',
    version='1.1',
    description='AWS Solution Helper Custom Resource',
    author='AWS Solutions Development',
    license='ASL',
    zip_safe=False,
    packages=['demo_app_config'],
    package_dir={'demo_app_config': '.'},
    install_requires=[
        'requests>=2.22.0'
    ],
    classifiers=[
        'Programming Language :: Python :: 3.7',
    ],
)
