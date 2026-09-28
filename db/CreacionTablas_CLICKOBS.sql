-- CLICKOBS - Creación de tablas
-- Nota de diseño: Consumo y Costo se unificaron en una sola tabla (consumo),
-- ya que tenían una relación 1 a 1 y el campo "fecha" de Costo duplicaba
-- al campo "periodo" de Consumo. Mantenerlas separadas obligaba a un JOIN
-- adicional en la consulta más frecuente del sistema (comparar costos).

CREATE TABLE tribu (
    id_tribu INT PRIMARY KEY,
    nombre VARCHAR(100),
    lead_responsable VARCHAR(150)
);

CREATE TABLE rol_permiso (
    id_rol INT PRIMARY KEY,
    nombre VARCHAR(100),
    descripcion VARCHAR(255)
);

CREATE TABLE usuario (
    id_usuario INT PRIMARY KEY,
    nombre VARCHAR(150),
    id_rol INT,
    id_tribu INT,
    FOREIGN KEY (id_rol) REFERENCES rol_permiso(id_rol),
    FOREIGN KEY (id_tribu) REFERENCES tribu(id_tribu)
);

CREATE TABLE servicio (
    id_servicio INT PRIMARY KEY,
    nombre VARCHAR(150),
    id_tribu INT,
    criticidad VARCHAR(50),
    FOREIGN KEY (id_tribu) REFERENCES tribu(id_tribu)
);

CREATE TABLE herramienta (
    id_herramienta INT PRIMARY KEY,
    nombre VARCHAR(100),
    proveedor VARCHAR(150),
    modelo_facturacion VARCHAR(100)
);

CREATE TABLE conector (
    id_conector INT PRIMARY KEY,
    id_herramienta INT,
    endpoint_api VARCHAR(255),
    estado VARCHAR(50),
    FOREIGN KEY (id_herramienta) REFERENCES herramienta(id_herramienta)
);

CREATE TABLE consumo (
    id_consumo INT PRIMARY KEY,
    id_herramienta INT,
    id_servicio INT,
    periodo VARCHAR(20),
    cantidad_usada DECIMAL(12, 2),
    unidad_medida VARCHAR(50),
    monto_usd DECIMAL(12, 2),
    FOREIGN KEY (id_herramienta) REFERENCES herramienta(id_herramienta),
    FOREIGN KEY (id_servicio) REFERENCES servicio(id_servicio)
);

CREATE TABLE umbral_alerta (
    id_umbral INT PRIMARY KEY,
    id_herramienta INT,
    monto_maximo DECIMAL(12, 2),
    periodicidad VARCHAR(50),
    FOREIGN KEY (id_herramienta) REFERENCES herramienta(id_herramienta)
);

CREATE TABLE recomendacion_ia (
    id_recomendacion INT PRIMARY KEY,
    id_herramienta INT,
    fecha DATETIME,
    modelo_usado VARCHAR(100),
    descripcion VARCHAR(500),
    ahorro_estimado DECIMAL(12, 2),
    FOREIGN KEY (id_herramienta) REFERENCES herramienta(id_herramienta)
);
