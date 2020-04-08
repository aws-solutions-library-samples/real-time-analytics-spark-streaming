#!/bin/bash
#
# This assumes all of the OS-level configuration has been completed and git repo has already been cloned
#
# This script should be run from the repo's deployment directory
# cd deployment
# ./build-s3-dist.sh source-bucket-base-name solution-name version-code
#
# Paramenters:
#  - source-bucket-base-name: Name for the S3 bucket location where the template will source the Lambda
#    code from. The template will append '-[region_name]' to this bucket name.
#    For example: ./build-s3-dist.sh solutions my-solution v1.0.0 template-bucket-name
#    The template will then expect the source code to be located in the solutions-[region_name] bucket
#
#  - solution-name: name of the solution for consistency
#
#  - version-code: version of the package
#
#  - bucket name where the nested templates should be copied to

[ "$DEBUG" == 'true' ] && set -x
set -e

# Check to see if input has been provided:
if [ -z "$1" ] || [ -z "$2" ] || [ -z "$3" ] || [ -z "$4" ]; then
    echo "Please provide the base source bucket name, trademark approved solution name, version where the lambda code will eventually reside and the template bucket name where the templates will be stored"
    echo "For example: ./build-s3-dist.sh solutions trademarked-solution-name v1.0.0 template-bucket-name"
    exit 1
fi

# Get reference for all important folders
template_dir="$PWD"
template_dist_dir="$template_dir/global-s3-assets"
build_dist_dir="$template_dir/regional-s3-assets"
source_dir="$template_dir/../source"

echo "------------------------------------------------------------------------------"
echo "[Init] Clean old dist, cache and target folders"
echo "------------------------------------------------------------------------------"
rm -rf $template_dist_dir
mkdir -p $template_dist_dir

rm -rf $build_dist_dir
mkdir -p $build_dist_dir

echo "------------------------------------------------------------------------------"
echo "[Packing] Templates"
echo "------------------------------------------------------------------------------"
for file in $template_dir/*.template
do
    cp $file $template_dist_dir/
done

echo "------------------------------------------------------------------------------"
echo "[Updating Source Bucket name]"
echo "------------------------------------------------------------------------------"
replace="s/%%BUCKET_NAME%%/$1/g"
for file in $template_dist_dir/*.template
do
    sed -i  -e $replace $file
done

echo "------------------------------------------------------------------------------"
echo "[Updating Template Bucket name]"
echo "------------------------------------------------------------------------------"
replace="s/%%TEMPLATE_BUCKET_NAME%%/$4/g"
for file in $template_dist_dir/*.template
do
    sed -i  -e $replace $file
done

echo "------------------------------------------------------------------------------"
echo "[Updating Solution name]"
echo "------------------------------------------------------------------------------"
replace="s/%%SOLUTION_NAME%%/$2/g"
for file in $template_dist_dir/*.template
do
    sed -i  -e $replace $file
done

echo "------------------------------------------------------------------------------"
echo "[Updating version name]"
echo "------------------------------------------------------------------------------"
replace="s/%%VERSION%%/$3/g"
for file in $template_dist_dir/*.template
do
    sed -i  -e $replace $file
done

echo "------------------------------------------------------------------------------"
echo "[Rebuild] Kinesis Producer Application"
echo "------------------------------------------------------------------------------"
cd $source_dir/kinesis-java-producer
mvn clean install
cp target/kinesis-producer-1.0-jar-with-dependencies.jar $build_dist_dir/kinesis-producer.jar

cd $template_dir

echo "------------------------------------------------------------------------------"
echo "[Rebuild] Spark Streaming Consumer"
echo "------------------------------------------------------------------------------"
cp $source_dir/appjars/spark_submit.sh $build_dist_dir/spark_submit.sh
cp $source_dir/zeppelin/zeppelin_config.sh $build_dist_dir/zeppelin_config.sh

cd $source_dir/kinesis-java-consumer
mvn clean install
cp target/kinesis-consumer-1.0-jar-with-dependencies.jar $build_dist_dir/kinesis-consumer.jar

cd $template_dir

echo "------------------------------------------------------------------------------"
echo "[Packing] Demo Lambda Function"
echo "------------------------------------------------------------------------------"
PY_PKG="demo-app-config"
echo "Building $PY_PKG zip file"
cd $build_dist_dir

# Build python resources using a virutal environment for containing all dependencies
virtualenv --python $(which python3) $build_dist_dir/env/
source env/bin/activate
pip install $source_dir/$PY_PKG/.  --target=$build_dist_dir/env/lib/python3.7/site-packages/ --upgrade --upgrade-strategy only-if-needed

# fail build if pip install fails
instl_status=$?
if [ ${instl_status} != '0' ]; then
    echo "Error occurred in pip install solution helper. pip install Error Code: ${instl_status}"
    exit ${instl_status}
fi

cd $build_dist_dir/env/lib/python3.7/site-packages/
zip -q -r9 $build_dist_dir/$PY_PKG.zip .
cd $template_dir

deactivate
rm -r $build_dist_dir/env/

echo "------------------------------------------------------------------------------"
echo "S3 Packaging Complete"
echo "------------------------------------------------------------------------------"