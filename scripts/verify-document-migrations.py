"""Check historical Room SQL migrations and document preservation using standard SQLite.

Run from the repository root with python scripts/verify-document-migrations.py.
Native SQLCipher/Room device acceptance remains a separate check.
"""
import json
import re
import sqlite3
import textwrap
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCHEMAS = ROOT / "app/schemas/com.pims.vault.data.local.database.PimsDatabase"
SOURCE = ROOT / "app/src/main/java/com/pims/vault/data/local/database/PimsDatabase.kt"


def migration_sql():
    source = SOURCE.read_text(encoding="utf-8")
    pattern = re.compile(r"val MIGRATION_(\d+)_(\d+) = object.*?(?=\n        (?:val MIGRATION_|fun getInstance)|\Z)", re.S)
    statements = {}
    literal = re.compile(r'db\.execSQL\(\s*(""".*?"""(?:\.trimIndent\(\))?|"(?:\\.|[^"\\])*")\s*\)', re.S)
    for block in pattern.finditer(source):
        raw = block.group(0)
        expressions = literal.findall(raw)
        if len(expressions) != raw.count("db.execSQL("):
            raise AssertionError("Migration needs explicit verification for non-literal SQL")
        decoded = []
        for expression in expressions:
            if expression.startswith('"""'):
                sql = textwrap.dedent(expression[3:expression.rfind('"""')]).strip()
            else:
                sql = json.loads(expression)
            decoded.append(sql)
        statements[int(block.group(1))] = decoded
    return statements


def create_schema(connection, schema):
    for entity in schema["entities"]:
        table = entity["tableName"]
        connection.execute(entity["createSql"].replace("${TABLE_NAME}", table))
        for index in entity.get("indices", []):
            connection.execute(index["createSql"].replace("${TABLE_NAME}", table))


def insert_fixture(connection, table, supplied):
    values = dict(supplied)
    for _, name, kind, required, default, primary in connection.execute(f"PRAGMA table_info(`{table}`)"):
        if name not in values and required and default is None:
            values[name] = b"fixture" if kind == "BLOB" else 1 if kind == "INTEGER" else "fixture"
    columns = ",".join(f"`{name}`" for name in values)
    connection.execute(f"INSERT INTO `{table}` ({columns}) VALUES ({','.join('?' for _ in values)})", list(values.values()))


def structure(connection):
    tables = [row[0] for row in connection.execute("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name != 'room_master_table'")]
    return {table: sorted(connection.execute(f"PRAGMA table_info(`{table}`)").fetchall(), key=lambda row: row[1]) for table in tables}


def main():
    migrations = migration_sql()
    latest = max(migrations) + 1
    expected = sqlite3.connect(":memory:")
    if (SCHEMAS / f"{latest}.json").exists():
        create_schema(expected, json.loads((SCHEMAS / f"{latest}.json").read_text())["database"])
    else:
        create_schema(expected, json.loads((SCHEMAS / f"{latest - 1}.json").read_text())["database"])
        for statement in migrations[latest - 1]:
            expected.execute(statement)
    target = structure(expected)
    versions = sorted(int(path.stem) for path in SCHEMAS.glob("*.json") if int(path.stem) < latest)
    for version in versions:
        connection = sqlite3.connect(":memory:")
        connection.execute("PRAGMA foreign_keys=ON")
        create_schema(connection, json.loads((SCHEMAS / f"{version}.json").read_text())["database"])
        insert_fixture(connection, "persons", {"id": "fixture-owner"})
        insert_fixture(connection, "documents", {"id": "fixture-document", "person_id": "fixture-owner", "title": "Retained title", "document_number": "TEST-ID", "notes": "Retained note"})
        insert_fixture(connection, "document_versions", {"id": "fixture-version", "document_id": "fixture-document", "version_number": 1, "file_storage_path": "vault_documents/fixture/v_1.penc", "sha256_hash": "a" * 64})
        insert_fixture(connection, "addresses", {"id": "fixture-address", "person_id": "fixture-owner"})
        insert_fixture(connection, "medical_records", {"id": "fixture-medical", "person_id": "fixture-owner", "title": "Retained medical note"})
        saved = {}
        for table in ("documents", "document_versions", "addresses", "medical_records"):
            columns = [row[1] for row in connection.execute(f"PRAGMA table_info(`{table}`)")]
            saved[table] = columns, connection.execute(f"SELECT * FROM `{table}`").fetchall()
        for old in range(version, latest):
            with connection:
                for statement in migrations[old]:
                    connection.execute(statement)
        for table, (columns, values) in saved.items():
            selected = ",".join(f"`{name}`" for name in columns)
            assert connection.execute(f"SELECT {selected} FROM `{table}`").fetchall() == values, f"Document data changed upgrading schema {version}"
        actual = structure(connection)
        # Column order is intentionally irrelevant for additive ALTER TABLE upgrades.
        def normalize(rows, expected):
            defaults = {row[1]: row[4] for row in expected}
            return [(name, kind, required, default if defaults.get(name) is not None else None, primary)
                    for _, name, kind, required, default, primary in rows]
        for table, columns in target.items():
            assert table in actual, f"Missing {table} after schema {version} upgrade"
            assert normalize(actual[table], columns) == normalize(columns, columns), f"Schema {version} leaves incompatible columns in {table}"
        assert not connection.execute("PRAGMA foreign_key_check").fetchall(), f"Broken document links after schema {version} upgrade"
        connection.close()
    print(f"Preserved document IDs, metadata, versions and paths through {len(versions)} historical schema upgrades to {latest}.")


if __name__ == "__main__":
    main()
