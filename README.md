
# DB Analizer

Herramienta en **Java 8+** para comparar archivos SQL que contienen sentencias `CREATE TABLE` y detectar diferencias en:
- Tablas presentes en un archivo pero no en otro.
- Columnas que faltan o sobran entre tablas comunes.
- Tipos de datos distintos, con reglas de normalización para evitar falsos positivos.

---

## 🚀 Características principales

- Ignora diferencias de mayúsculas/minúsculas y espacios extra.
- Normaliza tipos equivalentes entre sí:
  - `NUMERIC(p,0)` ≡ `NUMBER(p)`
  - `DECIMAL` ≡ `NUMBER`
  - `INT`, `INTEGER` ≡ `NUMBER(38)`
  - `VARCHAR` ≡ `VARCHAR2`
  - `CHAR` ≡ `CHAR(1)`
  - `DATE` ≡ `TIMESTAMP(p)`
  - `VARCHAR2(n BYTE)` ≡ `VARCHAR2(n)`
- Soporta paréntesis con o sin espacios, ej.:
  - `VARCHAR2 ( 100 )` = `VARCHAR2(100)`
  - `NUMBER ( 12 , 0 )` = `NUMBER(12)`

---

## 📦 Requisitos

- Java 8 o superior
- Maven o cualquier compilador Java estándar

---

## ⚙️ Instalación

1. Clona este repositorio o descarga el código fuente.
2. Compila el proyecto:
   ```bash
   javac Comparator.java
   ```
3. Ejecuta pasando dos archivos SQL:
   ```bash
   java Comparator archivo1.sql archivo2.sql
   ```

---

## 📝 Uso

Ejemplo de ejecución:

```bash
java Comparator HRVertical_Create_Tables_Oracle_4.6.22.sql HRVertical_Create_Tables_Oracle_5.1.0.sql
```

Se tienen que remover las lineas del 26 al 29 para ejecutar por terminal:

```
   args = new String[2];
   args[0] = "C:\\Users\\Alex Villatoro\\Desktop\\HRVertical_Migration_Create_Tables_Oracle_5.1.0.sql";
   args[1] = "C:\\Users\\Alex Villatoro\\Desktop\\HRVertical_Create_Tables_Oracle_5.1.0.sql";
```

O ya sea reemplazar los directorios de los archivos sql para solo ejecutar:

```
   java Comparator.java
```

o ejecutar la clase directamente desde un IDE.

Salida esperada:

```
Tabla 'employees' encontrada solo en HRVertical_Create_Tables_Oracle_5.1.0.sql
   - Columna 'date_time' difiere de tipo: HRVertical_Create_Tables_Oracle_4.6.22.sql=DATE vs HRVertical_Create_Tables_Oracle_5.1.0.sql=TIMESTAMP(3)
   - Columna 'id_person' difiere de tipo: HRVertical_Create_Tables_Oracle_5.1.0.sql=NUMERIC(12,0) vs HRVertical_Create_Tables_Oracle_5.1.0.sql=NUMBER(12)
   - Columna 'use_callback' difiere de tipo: HRVertical_Create_Tables_Oracle_4.6.22.sql=CHAR(1) vs HRVertical_Create_Tables_Oracle_5.1.0.sql=CHAR
```

---

## 📖 Notas importantes

- Para normalizar los tipos de datos hay que descomentar el método normalizarTipo de la clase TipoDeDato.
- `VARCHAR2(n BYTE)` y `VARCHAR2(n)` son considerados equivalentes (Oracle asume **BYTE** por defecto).
- `VARCHAR2(n CHAR)` puede ser tratado como **equivalente** a `VARCHAR2(n)` si así se configura en el código. Actualmente se marcan como distintos para evitar ambigüedad.
- Solo procesa definiciones de columnas dentro de `CREATE TABLE`. Claves primarias, foráneas y otros constraints no son considerados aún.

---

## 📜 Licencia

Este proyecto es de uso libre para propósitos educativos y profesionales.
