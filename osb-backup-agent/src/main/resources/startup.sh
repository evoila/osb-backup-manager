#!/bin/sh
cd /tmp
curl http://ftp.hosteurope.de/mirror/archive.mariadb.org//mariadb-10.2.6/bintar-linux-x86_64/mariadb-10.2.6-linux-x86_64.tar.gz > my.tar.gz
tar xzf my.tar.gz
cp mariadb-10.2.6-linux-x86_64/bin/mysqldump $1
cp mariadb-10.2.6-linux-x86_64/bin/mysql $1
chmod +x $1/*
rm my.tar.gz
rm -rf  mariadb-10.2.6-linux-x86_64
curl https://get.enterprisedb.com/postgresql/postgresql-9.5.7-1-linux-x64-binaries.tar.gz > pg.tar.gz
tar xfz pg.tar.gz
rm pg.tar.gz
cp pgsql/bin/pg_dump $1
cp pgsql/bin/pg_restore $1
rm -rf pqsql
curl https://fastdl.mongodb.org/linux/mongodb-linux-x86_64-3.4.7.tgz >> mongo.tgz
tar xfz mongo.tgz
cp mongodb-linux-x86_64-3.4.7/bin/mongodump $1
cp mongodb-linux-x86_64-3.4.7/bin/mongorestore $1
rm -rf mongodb-linux-x86_64-3.4.7/
rm mongo.tgz