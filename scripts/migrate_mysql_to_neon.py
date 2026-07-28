#!/usr/bin/env python3
"""
Migrate fluxpro data from local MySQL/MariaDB to Neon PostgreSQL.

Prerequisites:
  - Schema already created on Neon (Spring Boot with ddl-auto=update)
  - Env vars: NEON_PASSWORD (required), MYSQL_* optional

Usage:
  set NEON_PASSWORD=...
  python scripts/migrate_mysql_to_neon.py
"""

from __future__ import annotations

import os
import sys
import uuid
from datetime import date, datetime
from decimal import Decimal

import pymysql
import psycopg2
import psycopg2.extras

psycopg2.extras.register_uuid()

# FK-safe order (parents before children)
TABLES = [
    "organization_type",
    "organization",
    "permissions",
    "roles",
    "role_permissions",
    "users",
    "user_roles",
    "login_audit",
    "refresh_token",
    "admin_audit_log",
    "tenant_settings",
    "system_clock",
    "business_calendar",
    "file_types",
    "chain_templates",
    "chain_step_templates",
    "alert_types",
    "alert_rules",
    "alert_digest_recipient_role",
    "preconfigured_dossiers",
    "portal_users",
    "portal_otp_challenges",
    "file_number_sequences",
    "files",
    "file_attachments",
    "file_passages",
    "file_passage_cc",
    "alerts",
]


SELF_REF_NULLABLE = {
    "organization": ["parent_id"],
    "users": ["substitute_id"],
}


def bin_to_uuid(value):
    if value is None:
        return None
    if isinstance(value, uuid.UUID):
        return value
    if isinstance(value, str):
        # CHAR(36) UUIDs (organization_type)
        return uuid.UUID(value)
    if isinstance(value, (bytes, bytearray, memoryview)):
        raw = bytes(value)
        if len(raw) == 16:
            return uuid.UUID(bytes=raw)
        raise ValueError(f"Unexpected binary length {len(raw)}")
    raise TypeError(f"Cannot convert {type(value)} to UUID")


def convert_value(col_name: str, value, mysql_type: str):
    if value is None:
        return None

    t = mysql_type.lower()

    if t == "binary" or (t == "varbinary" and "id" in col_name):
        return bin_to_uuid(value)

    if t in ("bit", "tinyint"):
        if isinstance(value, (bytes, bytearray, memoryview)):
            return bool(bytes(value)[0])
        return bool(value)

    if t == "char" and (col_name.endswith("_id") or col_name == "id"):
        # organization_type.id / organization.organization_type_id are CHAR(36)
        if isinstance(value, str) and len(value) == 36:
            try:
                return str(uuid.UUID(value))  # keep as string for CHAR/VARCHAR targets
            except ValueError:
                return value
        return value

    if isinstance(value, datetime):
        return value
    if isinstance(value, date):
        return value
    if isinstance(value, Decimal):
        return value
    if isinstance(value, (bytes, bytearray, memoryview)):
        # leftover binary treated as UUID if 16 bytes
        raw = bytes(value)
        if len(raw) == 16:
            return uuid.UUID(bytes=raw)
        return raw.decode("utf-8", errors="replace")

    return value


def mysql_connect():
    return pymysql.connect(
        host=os.environ.get("MYSQL_HOST", "localhost"),
        port=int(os.environ.get("MYSQL_PORT", "3306")),
        user=os.environ.get("MYSQL_USER", "core"),
        password=os.environ.get("MYSQL_PASSWORD", "core2025"),
        database=os.environ.get("MYSQL_DATABASE", "fluxpro"),
        charset="utf8mb4",
        cursorclass=pymysql.cursors.Cursor,
    )


def neon_connect():
    password = os.environ.get("NEON_PASSWORD")
    if not password:
        print("NEON_PASSWORD env var required", file=sys.stderr)
        sys.exit(1)
    return psycopg2.connect(
        host=os.environ.get(
            "NEON_HOST", "ep-cold-dream-axlsjsbj.c-4.us-east-2.aws.neon.tech"
        ),
        dbname=os.environ.get("NEON_DB", "neondb"),
        user=os.environ.get("NEON_USER", "neondb_owner"),
        password=password,
        sslmode="require",
        options="endpoint=ep-cold-dream-axlsjsbj",
        connect_timeout=60,
    )


def get_mysql_columns(cur, table: str):
    cur.execute(
        """
        SELECT COLUMN_NAME, DATA_TYPE
        FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = %s
        ORDER BY ORDINAL_POSITION
        """,
        (table,),
    )
    return cur.fetchall()


def get_pg_columns(cur, table: str):
    cur.execute(
        """
        SELECT column_name
        FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = %s
        ORDER BY ordinal_position
        """,
        (table,),
    )
    return [r[0] for r in cur.fetchall()]


def truncate_all(pg):
    with pg.cursor() as cur:
        # Disable FK checks via CASCADE truncate in reverse dependency order
        for table in reversed(TABLES):
            cur.execute(
                "SELECT 1 FROM information_schema.tables WHERE table_schema='public' AND table_name=%s",
                (table,),
            )
            if cur.fetchone():
                cur.execute(f'TRUNCATE TABLE "{table}" RESTART IDENTITY CASCADE')
        pg.commit()
    print("Truncated Neon tables")


def migrate_table(mysql, pg, table: str):
    with mysql.cursor() as mcur, pg.cursor() as pcur:
        cols_meta = get_mysql_columns(mcur, table)
        if not cols_meta:
            print(f"  SKIP {table} (missing in MySQL)")
            return

        pg_cols = set(get_pg_columns(pcur, table))
        if not pg_cols:
            print(f"  SKIP {table} (missing in Neon — create schema first)")
            return

        # Only columns present on both sides
        cols = [(name, dtype) for name, dtype in cols_meta if name in pg_cols]
        if not cols:
            print(f"  SKIP {table} (no overlapping columns)")
            return

        col_names = [c[0] for c in cols]
        select_sql = "SELECT " + ", ".join(f"`{c}`" for c in col_names) + f" FROM `{table}`"
        mcur.execute(select_sql)
        rows = mcur.fetchall()

        if not rows:
            print(f"  OK   {table}: 0 rows")
            return

        converted = []
        for row in rows:
            converted.append(
                tuple(
                    convert_value(col_names[i], row[i], cols[i][1])
                    for i in range(len(cols))
                )
            )

        self_refs = [c for c in SELF_REF_NULLABLE.get(table, []) if c in col_names]
        placeholders = ", ".join(["%s"] * len(col_names))
        quoted_cols = ", ".join(f'"{c}"' for c in col_names)
        insert_sql = f'INSERT INTO "{table}" ({quoted_cols}) VALUES ({placeholders})'

        if self_refs:
            idx = {name: i for i, name in enumerate(col_names)}
            first_pass = []
            for row in converted:
                row_list = list(row)
                for col in self_refs:
                    row_list[idx[col]] = None
                first_pass.append(tuple(row_list))
            psycopg2.extras.execute_batch(pcur, insert_sql, first_pass, page_size=200)
            for col in self_refs:
                # Prefer updating by primary key "id" when present
                if "id" not in idx:
                    raise RuntimeError(f"{table}: self-ref update needs id column")
                update_sql = f'UPDATE "{table}" SET "{col}" = %s WHERE "id" = %s'
                updates = [
                    (row[idx[col]], row[idx["id"]])
                    for row in converted
                    if row[idx[col]] is not None
                ]
                if updates:
                    psycopg2.extras.execute_batch(pcur, update_sql, updates, page_size=200)
        else:
            psycopg2.extras.execute_batch(pcur, insert_sql, converted, page_size=200)

        pg.commit()
        print(f"  OK   {table}: {len(converted)} rows")


def main():
    print("Connecting MySQL…")
    mysql = mysql_connect()
    print("Connecting Neon…")
    pg = neon_connect()

    try:
        truncate_all(pg)
        print("Migrating tables…")
        for table in TABLES:
            try:
                migrate_table(mysql, pg, table)
            except Exception as e:
                pg.rollback()
                print(f"  FAIL {table}: {e}", file=sys.stderr)
                raise
        print("Done.")
    finally:
        mysql.close()
        pg.close()


if __name__ == "__main__":
    main()
