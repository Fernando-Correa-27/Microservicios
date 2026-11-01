-- ============================================================================
-- GLAB-S11 - Actividad: Sistema de Gestión de Citas "Salud Integral"
-- DDL de referencia. Las entidades JPA generan el esquema con ddl-auto=update;
-- este script documenta el esquema esperado y puede ejecutarse en una BD limpia.
--
-- DECISIÓN DE ARQUITECTURA:
--   cita.id_paciente e cita.id_medico NO tienen FK física porque paciente y
--   médico viven en otras bases de datos (bd_paciente, bd_medico). La
--   integridad referencial se valida por HTTP contra paciente-service y
--   medico-service. Las FK que sí pertenecen a la misma BD se mantienen:
--   medico.id_especialidad -> especialidad, horario_medico.id_medico -> medico
--   y atencion.id_cita -> cita.
-- ============================================================================

-- bd_paciente
CREATE DATABASE IF NOT EXISTS bd_paciente;
USE bd_paciente;
CREATE TABLE IF NOT EXISTS paciente (
    id_paciente INT AUTO_INCREMENT PRIMARY KEY,
    dni VARCHAR(8) UNIQUE,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    fecha_nacimiento DATE,
    sexo CHAR(1),
    telefono VARCHAR(15),
    direccion VARCHAR(200),
    correo VARCHAR(100),
    estado CHAR(1) DEFAULT 'A'
);

-- bd_medico
CREATE DATABASE IF NOT EXISTS bd_medico;
USE bd_medico;
CREATE TABLE IF NOT EXISTS especialidad (
    id_especialidad INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(200),
    estado CHAR(1) DEFAULT 'A'
);
CREATE TABLE IF NOT EXISTS medico (
    id_medico INT AUTO_INCREMENT PRIMARY KEY,
    cmp VARCHAR(20) UNIQUE,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    telefono VARCHAR(15),
    correo VARCHAR(100),
    id_especialidad INT NOT NULL,
    estado CHAR(1) DEFAULT 'A',
    CONSTRAINT fk_medico_especialidad FOREIGN KEY (id_especialidad)
        REFERENCES especialidad(id_especialidad)
);
CREATE TABLE IF NOT EXISTS horario_medico (
    id_horario INT AUTO_INCREMENT PRIMARY KEY,
    id_medico INT NOT NULL,
    dia_semana VARCHAR(15),
    hora_inicio TIME,
    hora_fin TIME,
    CONSTRAINT fk_horario_medico FOREIGN KEY (id_medico)
        REFERENCES medico(id_medico)
);

-- bd_cita
CREATE DATABASE IF NOT EXISTS bd_cita;
USE bd_cita;
CREATE TABLE IF NOT EXISTS cita (
    id_cita INT AUTO_INCREMENT PRIMARY KEY,
    fecha DATE NOT NULL,
    hora TIME NOT NULL,
    id_paciente INT NOT NULL,   -- sin FK: otra BD / otro microservicio
    id_medico INT NOT NULL,     -- sin FK: otra BD / otro microservicio
    motivo VARCHAR(300),
    estado VARCHAR(20) DEFAULT 'PROGRAMADA'
);
CREATE TABLE IF NOT EXISTS atencion (
    id_atencion INT AUTO_INCREMENT PRIMARY KEY,
    id_cita INT UNIQUE,
    diagnostico TEXT,
    tratamiento TEXT,
    observaciones TEXT,
    fecha_registro DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_atencion_cita FOREIGN KEY (id_cita)
        REFERENCES cita(id_cita)
);
