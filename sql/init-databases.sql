-- GLAB-S11: bases de datos del laboratorio
-- MySQL 8.4, usuario root sin contraseña (localhost:3306)
CREATE DATABASE IF NOT EXISTS bd_categoria;
CREATE DATABASE IF NOT EXISTS bd_producto;
CREATE DATABASE IF NOT EXISTS bd_cliente;
CREATE DATABASE IF NOT EXISTS bd_pedido;

CREATE DATABASE IF NOT EXISTS bd_categoria_dev;
CREATE DATABASE IF NOT EXISTS bd_producto_dev;
CREATE DATABASE IF NOT EXISTS bd_cliente_dev;
CREATE DATABASE IF NOT EXISTS bd_pedido_dev;

-- Actividad: Sistema de Gestión de Citas - Clínica Salud Integral
CREATE DATABASE IF NOT EXISTS bd_paciente;
CREATE DATABASE IF NOT EXISTS bd_medico;
CREATE DATABASE IF NOT EXISTS bd_cita;

CREATE DATABASE IF NOT EXISTS bd_paciente_dev;
CREATE DATABASE IF NOT EXISTS bd_medico_dev;
CREATE DATABASE IF NOT EXISTS bd_cita_dev;

SHOW DATABASES LIKE 'bd_%';
