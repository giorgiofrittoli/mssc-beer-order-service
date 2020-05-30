drop database if exists beerorderservice;
drop user if exists 'beer_order_service'@'%';
create database if not exists beerorderservice character set utf8mb4 collate utf8mb4_unicode_ci;
create user if not exists 'beer_order_service'@'%' identified with mysql_native_password by 'password';
grant select,insert,update,delete,create,drop,references,index,alter,execute,create view,show view,create routine,
 alter routine,event,trigger on beerorderservice.* to 'beer_order_service'@'%';
flush privileges;