# Proyecto de Referencia: Microservicios con Spring Boot y Docker

Este repositorio contiene el código fuente de un proyecto demostrativo compuesto por dos microservicios (proveedor y consumidor) que se comunican de forma síncrona mediante OpenFeign. La infraestructura completa está orquestada con Docker Compose, permitiendo levantar las aplicaciones y sus bases de datos asociadas con un único comando.

## Arquitectura

El ecosistema se compone de los siguientes elementos dentro de una red Docker compartida (`backend-net`):

*   **ms-productos (Puerto 8081):** Microservicio proveedor responsable de gestionar el catálogo de productos. Se conecta a una base de datos MySQL dedicada (`productos_db`).
*   **ms-pedidos (Puerto 8082):** Microservicio consumidor que registra los pedidos. Utiliza Feign Client para consultar internamente a `ms-productos` y calcular el total de la compra. Se conecta a una base de datos MySQL dedicada (`pedidos_db`).

## Tecnologías Utilizadas

*   Java 21
*   Spring Boot 3.2.5
*   Spring Cloud OpenFeign
*   Spring Data JPA / Hibernate
*   MySQL 8.0
*   Lombok
*   Docker & Docker Compose

## Instrucciones de Ejecución

1. Clonar este repositorio.
2. Navegar a la carpeta raíz del proyecto.
3. Ejecutar el siguiente comando para compilar y levantar los contenedores en segundo plano:

docker compose up -d --build

4. Las APIs estarán disponibles en `http://localhost:8081/api/productos` y `http://localhost:8082/api/pedidos`.

Para detener la infraestructura sin perder los datos de las bases de datos, ejecute:

docker compose down