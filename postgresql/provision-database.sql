CREATE ROLE "pale-2-register-admin"  WITH LOGIN password 'test123';
CREATE ROLE "pale-2-register-user" WITH LOGIN password 'test123';
CREATE ROLE "pale-2-register-readonly" WITH LOGIN password 'test123';

GRANT CONNECT ON DATABASE "pale-2-register" to "pale-2-register-admin";
GRANT CONNECT ON DATABASE "pale-2-register" to "pale-2-register-user";
GRANT CONNECT ON DATABASE "pale-2-register" to "pale-2-register-readonly";

CREATE DATABASE "pale-2-register-admin";
CREATE DATABASE "pale-2-register-user";