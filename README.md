# Tienda Demo — Proyecto de Referencia DSY1103

Proyecto demostrativo para el curso **Desarrollo FullStack 1 (DSY1103) — Duoc UC 2026**.

Contiene dos microservicios Spring Boot (`ms-productos` y `ms-pedidos`) que se comunican de forma sincrona mediante **Feign Client**. La infraestructura completa se levanta con un unico `docker compose up -d --build`.

Este repositorio es el ejemplo de referencia para el **Hito 1.5** (arquitectura con Docker) y el **Hito 2** (comunicacion entre microservicios).

---

## Indice

1. [Arquitectura del sistema](#1-arquitectura-del-sistema)
2. [Tecnologias utilizadas](#2-tecnologias-utilizadas)
3. [Instrucciones de ejecucion](#3-instrucciones-de-ejecucion)
4. [Endpoints disponibles](#4-endpoints-disponibles)
5. [Comunicacion entre microservicios — Hito 2](#5-comunicacion-entre-microservicios--hito-2)
6. [Explicacion pedagogica del proyecto](#6-explicacion-pedagogica-del-proyecto)

---

## 1. Arquitectura del sistema

El ecosistema completo corre dentro de una red Docker llamada `backend-net`. Son 4 contenedores en total:

```
                         backend-net (red Docker interna)
  ┌──────────────────────────────────────────────────────────────┐
  │                                                              │
  │   ┌───────────────────┐       ┌───────────────────┐         │
  │   │   ms-productos    │       │    ms-pedidos      │         │
  │   │   Puerto 8081     │◄──────│   Puerto 8082      │         │
  │   │   (Proveedor)     │Feign  │   (Consumidor)     │         │
  │   └────────┬──────────┘       └────────┬──────────┘         │
  │            │                           │                     │
  │   ┌────────▼──────────┐       ┌────────▼──────────┐         │
  │   │   db-productos    │       │    db-pedidos      │         │
  │   │   MySQL 8.0       │       │    MySQL 8.0       │         │
  │   │   productos_db    │       │    pedidos_db      │         │
  │   └───────────────────┘       └───────────────────┘         │
  │                                                              │
  └──────────────────────────────────────────────────────────────┘
              ▲                           ▲
              │                           │
        Postman / curl               Postman / curl
```

**Principio clave:** cada microservicio tiene su propia base de datos. `ms-pedidos` nunca accede directamente a `productos_db`. Para obtener datos de un producto, llama a la API de `ms-productos`.

---

## 2. Tecnologias utilizadas

| Tecnologia | Version | Para que se usa |
|---|---|---|
| Java | 21 | Lenguaje principal |
| Spring Boot | 3.2.5 | Framework de aplicacion |
| Spring Cloud OpenFeign | 4.x | Comunicacion entre servicios |
| Spring Data JPA / Hibernate | incluido en Boot | Acceso a base de datos |
| Spring Validation | incluido en Boot | Validacion de entrada |
| MySQL | 8.0 | Motor de base de datos |
| Lombok | incluido en Boot | Reduccion de codigo repetitivo |
| Docker y Docker Compose | 3.8 | Contenedores y orquestacion |
| SLF4J | incluido en Boot | Logs estructurados |

---

## 3. Instrucciones de ejecucion

### Pre-requisitos
- Docker Desktop instalado y corriendo
- Puerto 8081 y 8082 libres en el host

### Levantar todo el sistema

```bash
# Desde la raiz del proyecto (donde esta docker-compose.yml)
docker compose up -d --build
```

Docker construye las imagenes, levanta las bases de datos, espera a que esten saludables y luego inicia los microservicios.

### Verificar que todo esta corriendo

```bash
docker ps
```

Deben aparecer 4 contenedores en estado `Up`: `db-productos`, `db-pedidos`, `ms-productos`, `ms-pedidos`.

### Ver los logs de un servicio

```bash
docker logs ms-pedidos -f
docker logs ms-productos -f
```

### Detener sin perder datos

```bash
docker compose down
```

Los datos de las bases de datos persisten en los volumenes nombrados `vol-db-productos` y `vol-db-pedidos`.

### Detener y eliminar todo (incluyendo datos)

```bash
docker compose down -v
```

---

## 4. Endpoints disponibles

### ms-productos — `http://localhost:8081`

| Metodo | Endpoint | Descripcion | Body requerido |
|---|---|---|---|
| GET | `/api/productos` | Listar todos los productos | — |
| GET | `/api/productos/{id}` | Obtener un producto por ID | — |
| POST | `/api/productos` | Crear un producto | `ProductoCreateDTO` |
| PUT | `/api/productos/{id}` | Actualizar un producto | `ProductoCreateDTO` |
| DELETE | `/api/productos/{id}` | Eliminar un producto | — |

**Body para crear producto:**
```json
{
  "nombre": "Notebook Lenovo",
  "descripcion": "Notebook 15 pulgadas, 16GB RAM",
  "precio": 599990.00,
  "stock": 10
}
```

### ms-pedidos — `http://localhost:8082`

| Metodo | Endpoint | Descripcion | Body requerido |
|---|---|---|---|
| GET | `/api/pedidos` | Listar todos los pedidos | — |
| GET | `/api/pedidos/{id}` | Obtener un pedido por ID | — |
| POST | `/api/pedidos` | Crear un pedido | `PedidoCreateDTO` |
| PATCH | `/api/pedidos/{id}/cancelar` | Cancelar un pedido | — |

**Body para crear pedido:**
```json
{
  "productoId": 1,
  "cantidad": 2
}
```

**Respuesta al crear un pedido (ms-pedidos llama a ms-productos internamente):**
```json
{
  "id": 1,
  "productoId": 1,
  "nombreProducto": "Notebook Lenovo",
  "cantidad": 2,
  "precioUnitario": 599990.00,
  "total": 1199980.00,
  "estado": "PENDIENTE",
  "fechaCreacion": "2026-05-03T10:30:00"
}
```

---

## 5. Comunicacion entre microservicios — Hito 2

### Diagrama de dependencias

```
  ┌───────────────┐           ┌───────────────┐
  │   ms-pedidos  │──Feign───►│ ms-productos  │
  └───────────────┘           └───────────────┘
```

`ms-pedidos` (consumidor) depende de `ms-productos` (proveedor). La dependencia es unidireccional: no hay ciclo.

### Tabla de contratos

| Origen | Destino | Metodo | Endpoint | DTO de respuesta |
|---|---|---|---|---|
| ms-pedidos | ms-productos | GET | `/api/productos/{id}` | `ProductoDTO` (id, nombre, precio, stock) |

### Tecnologia utilizada para la comunicacion

- **Cliente REST:** Feign Client (`spring-cloud-starter-openfeign`)
  - Razon de eleccion: permite definir la llamada HTTP como una interfaz Java con anotaciones, sin escribir codigo de red manualmente. Es la opcion mas legible y mantenible para comunicacion sincrona entre servicios.
- **Manejo de errores:** `@RestControllerAdvice` con excepciones personalizadas (`RecursoNoEncontradoException`, `ServicioNoDisponibleException`)
- **Logs:** SLF4J en cada operacion relevante de `PedidoService` y `ProductoService`

### Escenario de despliegue

- [x] Escenario A — Todos los servicios en una sola instancia (monorepo con un unico `docker-compose.yml`)
- [ ] Escenario B — Servicios distribuidos en multiples instancias EC2

En el Escenario A, `ms-pedidos` se comunica con `ms-productos` usando el `container_name` como hostname dentro de la red Docker (`http://ms-productos:8081`). Esta URL se inyecta como variable de entorno `PRODUCTOS_URL` en el `docker-compose.yml`.

### Como probar la integracion

1. Levantar el sistema: `docker compose up -d --build`
2. Crear un producto en ms-productos:
   ```
   POST http://localhost:8081/api/productos
   ```
3. Crear un pedido en ms-pedidos usando el ID del producto creado:
   ```
   POST http://localhost:8082/api/pedidos
   Body: { "productoId": 1, "cantidad": 2 }
   ```
4. Verificar que la respuesta incluye `nombreProducto` y `total` calculado.
5. **Prueba de resiliencia:** detener ms-productos y volver a intentar crear un pedido:
   ```bash
   docker stop ms-productos
   ```
   El sistema debe responder con HTTP 503 y un mensaje claro en lugar de colgarse.

---

## 6. Explicacion pedagogica del proyecto

Esta seccion explica **por que existe cada archivo**, que problema resuelve y como se conecta con los conceptos del curso. Lean esto antes de estudiar el codigo.

---

### 6.1 Vision general: capas de una aplicacion Spring Boot

Cada microservicio sigue la misma estructura de capas. Entender para que sirve cada capa es mas importante que memorizar el codigo:

```
Controller  ──►  Service  ──►  Repository  ──►  Base de datos
   ▲                │
   │                ▼
  HTTP           Client (solo en consumidor)
  Request        (llama a otro servicio)
```

| Capa | Paquete | Responsabilidad |
|---|---|---|
| Controller | `controller/` | Recibir la peticion HTTP y devolver la respuesta |
| Service | `service/` | Logica de negocio: calculos, validaciones, coordinacion |
| Repository | `repository/` | Acceso a la base de datos |
| Model | `model/` | Representacion de la tabla en la base de datos |
| DTO | `dto/` | Objetos para entrada y salida de datos |
| Client | `client/` | Llamada HTTP a otro microservicio (solo en consumidor) |
| Exception | `exception/` | Errores personalizados y su manejo centralizado |

---

### 6.2 El modelo: Producto.java y Pedido.java

**Archivos:** `ms-productos/.../model/Producto.java`, `ms-pedidos/.../model/Pedido.java`

La clase modelo representa una **tabla en la base de datos**. Cada campo de la clase se convierte en una columna.

```java
@Entity          // Le dice a Spring: "esta clase es una tabla"
@Table(name = "productos")  // nombre de la tabla en MySQL
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;    // columna id, autoincremental

    @Column(nullable = false)
    private String nombre;  // columna nombre, no puede ser null
}
```

**Por que existe:** sin `@Entity`, Spring no sabe como guardar el objeto en MySQL. Esta clase es el puente entre Java y la tabla real.

**Nota importante sobre `Pedido`:** la entidad `Pedido` guarda `productoId` (un numero), no el nombre del producto. Eso es correcto: cada servicio guarda solo lo que le pertenece. El nombre se obtiene llamando a `ms-productos` cuando se necesita mostrarlo.

---

### 6.3 El repositorio: ProductoRepository y PedidoRepository

**Archivos:** `ms-productos/.../repository/ProductoRepository.java`, `ms-pedidos/.../repository/PedidoRepository.java`

```java
public interface ProductoRepository extends JpaRepository<Producto, Long> {
}
```

Esta interfaz esta **vacia**, pero hereda metodos completos de `JpaRepository`:
- `findAll()` → SELECT * FROM productos
- `findById(id)` → SELECT * FROM productos WHERE id = ?
- `save(producto)` → INSERT o UPDATE
- `deleteById(id)` → DELETE

**Por que existe:** Spring Data JPA genera el codigo SQL automaticamente. No hay que escribir una sola consulta SQL para las operaciones basicas. Si necesitan una consulta especial, pueden agregarla usando `@Query`.

---

### 6.4 Los DTOs: objetos de entrada y salida

**Archivos:** todos los archivos en `dto/`

Un DTO (Data Transfer Object) es un objeto que **solo transporta datos**, sin logica de negocio ni conexion a la base de datos.

En este proyecto hay tres tipos de DTO con propositos distintos:

#### DTO de entrada (lo que el cliente envia)

`ProductoCreateDTO` y `PedidoCreateDTO` reciben los datos del cliente (Postman, navegador). Incluyen validaciones:

```java
@NotBlank(message = "El nombre es obligatorio")
@Size(min = 2, max = 150)
private String nombre;

@NotNull
@DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
private BigDecimal precio;
```

Si el cliente envia un campo invalido, Spring responde automaticamente con HTTP 400 y el mensaje de error, sin necesidad de escribir ese codigo en el servicio.

#### DTO de salida (lo que el servicio devuelve)

`ProductoDTO` y `PedidoDTO` son la respuesta que se envia al cliente. Pueden tener menos campos que la entidad (por ejemplo, sin campos internos o sensibles).

#### DTO espejo (copia del contrato de otro servicio)

`ProductoDTO` en `ms-pedidos` es una copia de los campos que `ms-productos` devuelve. Cuando Feign recibe la respuesta JSON de `ms-productos`, la deserializa en este objeto.

**Por que no se usa directamente la entidad como respuesta:** si se expone la entidad `@Entity` directamente, cualquier cambio en la tabla (agregar una columna, cambiar un nombre) rompe la API publica. El DTO actua como un contrato estable independiente de la base de datos.

---

### 6.5 El servicio: ProductoService y PedidoService

**Archivos:** `ms-productos/.../service/ProductoService.java`, `ms-pedidos/.../service/PedidoService.java`

El servicio contiene la **logica de negocio**. Es donde ocurre el trabajo real.

En `PedidoService.crear()` se puede ver el flujo completo del Hito 2:

```java
public PedidoDTO crear(PedidoCreateDTO dto) {
    // 1. Llamar a ms-productos via Feign para obtener precio y nombre
    ProductoDTO producto = productoClient.obtenerProducto(dto.getProductoId());

    // 2. Calcular el total en ms-pedidos (no en ms-productos)
    BigDecimal total = producto.getPrecio().multiply(BigDecimal.valueOf(dto.getCantidad()));

    // 3. Guardar el pedido en pedidos_db con el precio y total
    Pedido pedido = new Pedido();
    pedido.setPrecioUnitario(producto.getPrecio());
    pedido.setTotal(total);
    // ...
    return toDTOConNombre(repository.save(pedido), producto.getNombre());
}
```

**Por que la logica esta en el Service y no en el Controller:** el controller solo deberia saber de HTTP (recibir peticiones, devolver respuestas). La logica de negocio en el service puede ser reutilizada, probada de forma independiente y modificada sin tocar los endpoints.

---

### 6.6 El controlador: ProductoController y PedidoController

**Archivos:** `ms-productos/.../controller/ProductoController.java`, `ms-pedidos/.../controller/PedidoController.java`

El controlador expone los **endpoints HTTP** y delega toda la logica al servicio.

```java
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    @PostMapping
    public ResponseEntity<PedidoDTO> crear(@Valid @RequestBody PedidoCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
    }
}
```

Puntos clave:
- `@RestController`: combina `@Controller` + `@ResponseBody`. Todo lo que retorna el metodo se convierte a JSON automaticamente.
- `@RequestMapping("/api/pedidos")`: prefijo de todos los endpoints de este controller.
- `@Valid`: activa las validaciones del DTO. Si el cuerpo no cumple las reglas, Spring rechaza la peticion antes de llegar al servicio.
- `ResponseEntity`: permite controlar el codigo HTTP de respuesta (201 Created, 200 OK, etc.).

---

### 6.7 El Feign Client: ProductoClient.java

**Archivo:** `ms-pedidos/.../client/ProductoClient.java`

Esta es la pieza central del Hito 2. Permite que `ms-pedidos` llame a `ms-productos` como si fuera un metodo Java normal.

```java
@FeignClient(name = "ms-productos", url = "${productos.service.url}")
public interface ProductoClient {

    @GetMapping("/api/productos/{id}")
    ProductoDTO obtenerProducto(@PathVariable("id") Long id);
}
```

**Como funciona:**
1. Se define una interfaz Java (no una clase, solo la firma del metodo).
2. La anotacion `@FeignClient` le indica a Spring que genere automaticamente el codigo HTTP.
3. Cuando el servicio llama a `productoClient.obtenerProducto(5L)`, Spring traduce eso a una peticion HTTP GET a `http://ms-productos:8081/api/productos/5`.
4. La respuesta JSON se deserializa automaticamente en un objeto `ProductoDTO`.

**Para que funcione** es obligatorio tener `@EnableFeignClients` en la clase principal `MsPedidosApplication.java`. Sin esa anotacion, Spring no activa el mecanismo de Feign y la inyeccion de `ProductoClient` falla con un error de contexto.

**Como se configura la URL:** en `application.properties` esta definida como:
```properties
productos.service.url=${PRODUCTOS_URL:http://localhost:8081}
```
El valor por defecto es `localhost` (para correr localmente sin Docker). Cuando se ejecuta con Docker Compose, la variable `PRODUCTOS_URL=http://ms-productos:8081` sobreescribe el valor, y `ms-productos` es el nombre del contenedor que Docker resuelve como hostname dentro de `backend-net`.

---

### 6.8 El manejo de errores: exception/

**Archivos:** `exception/RecursoNoEncontradoException.java`, `exception/ServicioNoDisponibleException.java`, `exception/GlobalExceptionHandler.java`

#### Las excepciones personalizadas

Son clases simples que extienden `RuntimeException`. Sirven para darle nombre al problema:

```java
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
```

**Por que no usar directamente `RuntimeException`:** si el codigo lanza `new RuntimeException("no encontrado")`, el handler global no puede distinguirlo de otros errores. Con una clase propia, el handler puede capturarla especificamente y devolver exactamente el codigo HTTP correcto.

#### El handler global: GlobalExceptionHandler.java

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(RecursoNoEncontradoException e) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)          // HTTP 404
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(ServicioNoDisponibleException.class)
    public ResponseEntity<Map<String, String>> handleServiceUnavailable(ServicioNoDisponibleException e) {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)  // HTTP 503
                .body(Map.of("error", e.getMessage()));
    }
}
```

**Por que existe:** sin este handler, cuando ocurre una excepcion, Spring devuelve una respuesta JSON con mucho detalle interno (stack trace, nombre de clases) que no deberia llegar al cliente. El handler intercepta cualquier excepcion del tipo indicado en toda la aplicacion y la convierte en una respuesta limpia con el codigo HTTP correcto.

**Como se captura el error de Feign en PedidoService:**

```java
try {
    producto = productoClient.obtenerProducto(dto.getProductoId());
} catch (FeignException.NotFound e) {
    // ms-productos respondio 404: el producto no existe
    throw new RecursoNoEncontradoException("Producto no encontrado: " + dto.getProductoId());
} catch (FeignException e) {
    // ms-productos no responde o respondio con otro error
    throw new ServicioNoDisponibleException("Servicio de productos no disponible.");
}
```

Si se omite este bloque try-catch, una excepcion de Feign no capturada llegaría al handler como un error generico 500. Con el catch especifico, el cliente recibe 404 o 503 segun el caso real.

---

### 6.9 El docker-compose.yml

**Archivo:** `docker-compose.yml` en la raiz del proyecto

Orquesta los 4 contenedores. Los puntos mas importantes a entender:

#### Healthcheck en las bases de datos

```yaml
healthcheck:
  test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-proot"]
  interval: 10s
  retries: 5
```

MySQL tarda unos segundos en estar listo despues de iniciarse. Sin el healthcheck, el microservicio intentaria conectarse a la base de datos antes de que esta estuviera disponible y fallaria. El healthcheck hace que Docker espere a que MySQL acepte conexiones antes de iniciar el microservicio.

#### depends_on con condicion

```yaml
ms-productos:
  depends_on:
    db-productos:
      condition: service_healthy   # espera el healthcheck
```

`condition: service_healthy` es mas estricto que `depends_on` simple: garantiza que la base de datos no solo este iniciada, sino que responda correctamente.

#### Variables de entorno para configurar la comunicacion

```yaml
ms-pedidos:
  environment:
    - PRODUCTOS_URL=http://ms-productos:8081
```

Esta linea es la que conecta a `ms-pedidos` con `ms-productos`. El valor `http://ms-productos:8081` usa el `container_name` del otro servicio como hostname, que Docker resuelve automaticamente dentro de `backend-net`.

#### Volumenes nombrados

```yaml
volumes:
  vol-db-productos:
  vol-db-pedidos:
```

Los datos de MySQL se guardan en estos volumenes. Si se ejecuta `docker compose down` (sin `-v`), los contenedores se eliminan pero los datos persisten. Esto replica el concepto de persistencia que se explica en la guia del Hito 1.5.

---

### 6.10 El application.properties

**Archivos:** `ms-productos/.../resources/application.properties`, `ms-pedidos/.../resources/application.properties`

```properties
# Patron: ${VARIABLE_ENTORNO:valor_por_defecto}
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3306/productos_db}
```

Este patron permite que el mismo archivo funcione en dos contextos:
- **Local (sin Docker):** si la variable de entorno no existe, usa `localhost:3306`.
- **Con Docker:** Docker Compose inyecta `SPRING_DATASOURCE_URL` apuntando al nombre del contenedor de base de datos (`db-productos:3306`).

Esto evita tener que modificar el codigo fuente al cambiar de entorno.

---

### 6.11 Flujo completo de una peticion: crear un pedido

Este es el recorrido de principio a fin cuando Postman envia `POST /api/pedidos`:

```
Postman
  │
  ▼
PedidoController.crear(@Valid @RequestBody PedidoCreateDTO)
  │  Spring valida los campos del DTO.
  │  Si son invalidos → GlobalExceptionHandler devuelve 400.
  │
  ▼
PedidoService.crear(dto)
  │  Log: "Creando pedido productoId=1, cantidad=2"
  │
  ├──► ProductoClient.obtenerProducto(1L)
  │         │
  │         │  Feign genera: GET http://ms-productos:8081/api/productos/1
  │         │
  │         ▼
  │    ProductoController en ms-productos
  │         │
  │         ▼
  │    ProductoService.findById(1L)
  │         │  Lee de productos_db
  │         │  Devuelve ProductoDTO(id=1, nombre="Notebook", precio=599990)
  │         │
  │    Feign deserializa JSON → ProductoDTO en ms-pedidos
  │
  │  Si ms-productos responde 404 → RecursoNoEncontradoException → HTTP 404
  │  Si ms-productos no responde → ServicioNoDisponibleException → HTTP 503
  │
  ├── Calcula total: 599990 × 2 = 1199980
  ├── Crea entidad Pedido y guarda en pedidos_db
  │
  ▼
PedidoController devuelve ResponseEntity con HTTP 201 y PedidoDTO
  │
  ▼
Postman recibe la respuesta con nombreProducto y total calculado
```

---

## Lista de cotejo Hito 2

- [ ] Ambos servicios levantan con `docker compose up -d --build`
- [ ] `docker ps` muestra 4 contenedores en estado `Up`
- [ ] `POST /api/pedidos` devuelve `nombreProducto` y `total` correctos
- [ ] Si se detiene `ms-productos`, el sistema responde HTTP 503 (no se cuelga)
- [ ] Si se pide un producto que no existe, el sistema responde HTTP 404
- [ ] `docker logs ms-pedidos` muestra trazas de la llamada a ms-productos
- [ ] `docker compose down` y luego `docker compose up -d` recupera los datos de la BD
- [ ] El README tiene diagrama de dependencias y tabla de contratos
- [ ] Los commits en GitHub tienen mensajes descriptivos

---

DSY1103 · Desarrollo FullStack 1 · Duoc UC 2026
