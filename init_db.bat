@echo off
cd "C:\Program Files\MariaDB 11.8\bin"
mysql.exe -u root -ptaraji1919 < "C:\Users\trabe\OneDrive\Desktop\chosen\PFE\setup_db.sql"
echo Database initialization completed
pause

