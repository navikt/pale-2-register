
DO
$$
    BEGIN
        IF EXISTS
            (SELECT 1 from pg_roles where rolname = 'pale-2-register-instance')
        THEN
            alter user "pale-2-register-instance" with replication;
        END IF;
    END
$$;
DO
$$
    BEGIN
        IF EXISTS
            (SELECT 1 from pg_roles where rolname = 'datastream-pale-2-register-user')
        THEN
            alter user "datastream-pale-2-register-user" with replication;
            GRANT SELECT ON ALL TABLES IN SCHEMA public TO "datastream-pale-2-register-user";
            GRANT USAGE ON SCHEMA public TO "datastream-pale-2-register-user";
            ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO "datastream-pale-2-register-user";
        END IF;
    END
$$;
