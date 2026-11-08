# Cómo correr CLICKOBS en tu computadora (paso a paso)

## 0. Qué necesitás instalado
- **JDK 21** (por ejemplo Temurin 21). Verificar: `java -version` debe decir 21.
- **Maven** (o IntelliJ IDEA, que ya trae uno). Verificar: `mvn -v`.
- **MySQL** con la base `clickobs` (la de tu servidor, o una local).
- Internet la primera vez (Maven baja JavaFX y el driver de MySQL).

## 1. Bajar el código
```
git clone https://github.com/jorgejcorrea/clickobs.git
cd clickobs
```
(o descomprimí `clickobs-repo.zip` y entrá a la carpeta `clickobs`).

## 2. Crear la base (MySQL Workbench)
Abrí y ejecutá, en este orden, los scripts de la carpeta `db/`:
1. `CreacionTablas_CLICKOBS.sql`
2. `Endurecimiento_CLICKOBS.sql` (opcional: agrega las reglas de integridad)
3. `Insert_CLICKOBS.sql`

Si todavía no existe el esquema: `CREATE DATABASE clickobs;` y `USE clickobs;` antes del paso 1.
Recomendado: un usuario propio para la app, sin permisos de más:
```sql
CREATE USER 'clickobs_app'@'%' IDENTIFIED BY 'UNA_CLAVE_TUYA';
GRANT SELECT, INSERT, UPDATE ON clickobs.* TO 'clickobs_app'@'%';
```

## 3. Configurar la conexión
En la carpeta raíz del proyecto (donde está `pom.xml`):
```
copy src\main\resources\db.properties.example db.properties        (Windows)
cp src/main/resources/db.properties.example db.properties          (Mac/Linux)
```
Editá `db.properties`: `db.url` (ej. `jdbc:mysql://192.168.139.222:3306/clickobs?useUnicode=true&characterEncoding=UTF-8`), `db.user` y `db.password`.
Ese archivo está en `.gitignore`: **no se sube a GitHub**.

## 4. Ejecutar
```
mvn javafx:run
```
Tiene que abrirse la ventana de login.

**Con IntelliJ:** File > Open > elegir `pom.xml` > "Open as Project". En la pestaña Maven: `clickobs > Plugins > javafx > javafx:run`. Importante: ejecutá desde la carpeta raíz (ahí tiene que estar `db.properties`).

## 5. Recorrido de prueba (5 minutos)
| Usuario | Qué probar |
|---|---|
| `finanzas` | Alertas: configurar un umbral, escribir `abc` (debe mostrar error rojo), «Evaluar período», «Atender siguiente» |
| `sre.infra` | Comparar Costos (período `2026-08`): ordenar, «Ubicar» una herramienta, exportar CSV; Consumo; Administración |
| `dfarias` | Recomendaciones IA (período `2026-08`); probar entrar a Comparar Costos (acceso denegado) |
| `devops.pagos` | Solo Consumo |

La contraseña no se valida todavía (solo que el usuario exista). Probá también el botón «← Volver» después de navegar por varias pantallas.

## 6. Si algo falla
| Mensaje | Causa probable |
|---|---|
| "Falta configurar la base de datos" | `db.properties` no está en la carpeta desde la que ejecutás |
| "Communications link failure" | IP/puerto incorrecto, servidor apagado o firewall |
| "Access denied for user" | usuario o clave mal; el usuario no tiene permisos sobre `clickobs` |
| "Unknown database" | falta `CREATE DATABASE clickobs;` |
| `mvn` no se reconoce | Maven no instalado o fuera del PATH |
| "release version 21 not supported" | el JDK no es 21 |
| "JavaFX runtime components are missing" | no ejecutes la clase `App` a mano: usá `mvn javafx:run` |

## 7. (Opcional) Pruebas automáticas
Ver `pruebas/README.md`: 77 casos que imprimen OK / FALLO.
