import mysql.connector
import sys

try:
    # Connect to MariaDB
    connection = mysql.connector.connect(
        host='localhost',
        user='root',
        password='taraji1919',
        port=3306
    )

    cursor = connection.cursor()

    # Create database
    cursor.execute("CREATE DATABASE IF NOT EXISTS remoteflow CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;")
    print("✓ Database 'remoteflow' created/verified")

    # Select database and create tables
    cursor.execute("USE remoteflow;")

    # Create equipe table first (no foreign key dependencies)
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS equipe (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            name VARCHAR(255) NOT NULL
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
    """)
    print("✓ Table 'equipe' created/verified")

    # Create users table
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS users (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            nom VARCHAR(255) NOT NULL,
            email VARCHAR(255) NOT NULL UNIQUE,
            password VARCHAR(255) NOT NULL,
            equipe_id BIGINT,
            role VARCHAR(50) NOT NULL,
            FOREIGN KEY (equipe_id) REFERENCES equipe(id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
    """)
    print("✓ Table 'users' created/verified")

    # Show all tables
    cursor.execute("SHOW TABLES;")
    tables = cursor.fetchall()
    print(f"\nTables in 'remoteflow' database: {[t[0] for t in tables]}")

    connection.commit()
    cursor.close()
    connection.close()

    print("\n✓ Database initialization completed successfully!")
    sys.exit(0)

except mysql.connector.Error as err:
    print(f"✗ Error: {err}")
    sys.exit(1)
except Exception as e:
    print(f"✗ Unexpected error: {e}")
    sys.exit(1)

