# Pruebas ejecutables

`PruebasClickobs.java` corre 127 casos (77 del TP3 más 50 del TP4: arreglo de 12 meses, bitácora en archivo, excepciones de persistencia, patrón DAO, ranking y patrón MVC con sus controladores) sin JUnit. Imprime OK o FALLO por caso.

```bash
# desde la raíz del repo, con la base cargada con db/CreacionTablas_CLICKOBS.sql e Insert_CLICKOBS.sql
mkdir -p out && javac -d out --module-path $PATH_TO_FX --add-modules javafx.controls $(find src pruebas -name "*.java")
export CLICKOBS_DB_URL="jdbc:mysql://HOST:3306/clickobs" CLICKOBS_DB_USER=... CLICKOBS_DB_PASSWORD=...
java -cp "out:RUTA/mysql-connector-j.jar" PruebasClickobs
```
Los casos con base de datos dejan los datos como los encontraron. Corrida de referencia del TP4 sobre MySQL 8.0.46: 127 OK, 0 FALLO (también 127 OK sobre MariaDB 10.11). Se ejecuta desde la raíz del repositorio: algunas pruebas leen `src/` para verificar la regla de dependencias del MVC; requiere un usuario con permisos de INSERT, UPDATE y DELETE sobre `clickobs`.
