# Postgres-DB based on Docker

## How to use

run `docker-compose up -d` in this directory

## How to stop

run `docker-compose down` in this directory

## How to connect to the database

- on your local computer, run `psql -h localhost -p 5432 -U postgres` and enter the password `postgres`
- using JBDC, use the following connection string: `jdbc:postgresql://localhost:5432/postgres?user=postgres&password=postgres`

## How to connect to the database using pgAdmin

- in any browser, go to `http://localhost:5050`
- login with the credentials `admin@admin` and the password `admin`
- right click on `Servers` and select `Create` -> `Server...`
- in the `General` tab, enter `postgres` as name
- in the `Connection` tab, enter `postgres` as host name/address, `5432` as port, `postgres` as username and `postgres` as password
