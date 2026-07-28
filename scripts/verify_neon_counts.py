import os
import sys

import psycopg2

password = os.environ.get("NEON_PASSWORD")
if not password:
    print("NEON_PASSWORD env var required", file=sys.stderr)
    sys.exit(1)

conn = psycopg2.connect(
    host="ep-cold-dream-axlsjsbj.c-4.us-east-2.aws.neon.tech",
    dbname="neondb",
    user="neondb_owner",
    password=password,
    sslmode="require",
    options="endpoint=ep-cold-dream-axlsjsbj",
    connect_timeout=30,
)
cur = conn.cursor()
cur.execute(
    """
    SELECT relname AS table, n_live_tup AS approx_rows
    FROM pg_stat_user_tables
    ORDER BY relname
    """
)
# Prefer exact counts
tables = [
    "organization_type", "organization", "users", "roles", "permissions",
    "role_permissions", "user_roles", "files", "file_passages", "alerts",
    "alert_rules", "preconfigured_dossiers", "portal_users", "chain_templates",
    "chain_step_templates", "admin_audit_log", "refresh_token",
]
print("Exact counts:")
for t in tables:
    cur.execute(f'SELECT COUNT(*) FROM "{t}"')
    print(f"  {t}: {cur.fetchone()[0]}")
conn.close()
