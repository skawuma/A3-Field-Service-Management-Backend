Production uses Flyway for schema migration management.

Add versioned SQL migrations here, for example:

```text
V2__add_work_order_indexes.sql
```

Development keeps Hibernate `ddl-auto=update` and disables Flyway so local entity iteration remains fast. Production keeps Hibernate `ddl-auto=validate` and enables Flyway so startup fails if the database schema and application model drift.
