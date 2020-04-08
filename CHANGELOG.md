# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2020-04-08
### Added
- CHANGELOG, NOTICE, CONTRIBUTING, and CODE_OF_CONDUCT files
- Java based spark streaming application
- Demo producer application to generate random products
- Foxproxy plugin configuration file under source/zeppelin folder. This plugin configuration is required for Web View of EMR cluster applications like Hadoop, Zeppelin and others once dynamic port forwarding is configured through SSH tunneling
- Flow logs to VPC
- Security configuration to EMR cluster

### Changed
- Lambda function runtime to Python 3.8
- EMR version to 5.29.0
- Moved inline lambda code from CloudFormation template to its own source code repository
- Folder name of CloudFormation templates from 'cform' to 'deployment'
- Folder name for source code from 'code' folder to 'source'
- CloudFormation templates to use SSM parameters
- CloudFormation templates so that _SendAnonymousData_ is a mapping instead of a parameter
- IAM roles to use custom policies instead of managed policies
- Spark and Kinesis dependencies to use latest available versions

### Removed
- Existing Scala streaming application
- Unnecessary subnets from VPC template
- Unnecessary parameters and conditions from demo template
