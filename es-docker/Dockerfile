FROM elasticsearch:8.19.10

# Install IK analyzer
# Using the online release URL. If this fails due to network issues, 
# you can download the zip manually to this directory and uncomment the COPY/RUN lines below.

RUN elasticsearch-plugin install --batch https://get.infini.cloud/elasticsearch/analysis-ik/8.19.10

# Offline installation method:
# 1. Download elasticsearch-analysis-ik-8.19.10.zip
# 2. Place it in the same directory as this Dockerfile
# 3. Uncomment the following lines and comment out the RUN command above:
# COPY elasticsearch-analysis-ik-8.19.10.zip /tmp/
# RUN elasticsearch-plugin install --batch file:///tmp/elasticsearch-analysis-ik-8.19.10.zip && rm /tmp/elasticsearch-analysis-ik-8.19.10.zip
