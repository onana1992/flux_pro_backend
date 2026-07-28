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
cur.execute("SELECT version(), current_database()")
print(cur.fetchone())
cur.execute("SELECT tablename FROM pg_tables WHERE schemaname='public' ORDER BY 1")
print("tables:", cur.fetchall())
conn.close()
print("OK")
