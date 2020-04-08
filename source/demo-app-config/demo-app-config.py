######################################################################################################################
#  Copyright 2019-2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.                                      #
#                                                                                                                    #
#  Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance    #
#  with the License. A copy of the License is located at                                                             #
#                                                                                                                    #
#      http://www.apache.org/licenses/LICENSE-2.0                                                                    #
#                                                                                                                    #
#  or in the 'license' file accompanying this file. This file is distributed on an 'AS IS' BASIS, WITHOUT WARRANTIES #
#  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific language governing permissions    #
#  and limitations under the License.                                                                                #
######################################################################################################################

import os
import boto3
import requests
import json
from botocore.client import Config

def lambda_handler(event, context):
    if event['RequestType'] == 'Create' or event['RequestType'] == 'Update':
        Bucket= os.environ['APP_BUCKET']
        try:
            s3 = boto3.resource('s3', region_name=os.environ['AWS_REGION'], config=Config(signature_version='s3v4'))

            if os.environ['DEMO_APP'] == 'TRUE':
                copy_source={'Bucket':os.environ['S3_BUCKET'],'Key': os.environ['KEY_PREFIX'] + "/kinesis-consumer.jar"}
                s3.meta.client.copy(copy_source, Bucket, 'kinesis-consumer.jar')

                copy_source2={'Bucket':os.environ['S3_BUCKET'],'Key':os.environ['KEY_PREFIX'] + "/kinesis-producer.jar"}
                s3.meta.client.copy(copy_source2, Bucket, 'kinesis-producer.jar')

                copy_source4={'Bucket':os.environ['S3_BUCKET'],'Key':os.environ['KEY_PREFIX'] + "/spark_submit.sh"}
                s3.meta.client.copy(copy_source4, Bucket, 'spark_submit.sh')

            copy_source3={'Bucket':os.environ['S3_BUCKET'],'Key':os.environ['KEY_PREFIX'] + "/zeppelin_config.sh"}
            s3.meta.client.copy(copy_source3, Bucket, 'zeppelin_config.sh')

            status = 'SUCCESS'
        except Exception as e:
            status = 'FAILED'
            print(e)
    elif event['RequestType'] == 'Delete':
        print("CustomResourceDelete")
        status = 'SUCCESS'
    else:
        status = 'SUCCESS'


    # generate response back for success
    responseUrl = event['ResponseURL']
    print(responseUrl)

    responseBody = {}
    responseBody['Status'] = status
    responseBody['Reason'] = 'See the details in CloudWatch Log Stream: ' + context.log_stream_name
    responseBody['PhysicalResourceId'] = context.log_stream_name
    responseBody['StackId'] = event['StackId']
    responseBody['RequestId'] = event['RequestId']
    responseBody['LogicalResourceId'] = event['LogicalResourceId']

    json_responseBody = json.dumps(responseBody)

    print("Response body:\n" + json_responseBody)

    headers = {
        'content-type' : '',
        'content-length' : str(len(json_responseBody))
    }

    try:
        response = requests.put(responseUrl,
                                data=json_responseBody,
                                headers=headers)
        print("Status code: " + response.reason)
    except Exception as e:
        print("send(..) failed executing requests.put(..): " + str(e))