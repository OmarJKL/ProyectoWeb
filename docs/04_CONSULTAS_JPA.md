# Lambda Shield — Consultas personalizadas en JPA

## 1. Cuándo se usa cada enfoque

| Enfoque | Cuándo usarlo | Ejemplos en el proyecto |
| :--- | :--- | :--- |
| Consultas derivadas por nombre de método | Búsquedas simples por uno o varios campos | `findByCodigoCliente`, `findByEsActivaTrueOrderByFechaEvaluacionDesc` |
| `@Query` con JPQL | Agregaciones, `join fetch` y filtros opcionales | `sumarSaldoPorEstado`, `buscarDelPeriodo` |
| Filtrado en memoria | Listas pequeñas y lógica compleja | `listarFiltrado` de reportes (por ahora) |

## 2. Consultas personalizadas aplicadas

### 2.1 Suma de saldo por estado — `CuentaRepository`

```java
@Query("select coalesce(sum(c.saldoDisponible), 0) from Cuenta c where c.estadoCuenta = :estado")
BigDecimal sumarSaldoPorEstado(@Param("estado") EstadoCuenta estado);
```

**Por qué:** el saldo total de las cuentas activas es un agregado; traer todas las cuentas a Java solo para sumarlas sería ineficiente. Se ejecuta en PostgreSQL y viaja un único número.

**Dónde se usa:** `DashboardResumenService.obtenerResumen` → saldo del dashboard.

### 2.2 Suma de montos por naturaleza — `TransaccionRepository`

```java
@Query("""
        select t.tipoTransaccion.naturaleza, coalesce(sum(t.montoTransaccion), 0)
        from Transaccion t
        where t.fechaHoraTransaccion >= :desde and t.fechaHoraTransaccion < :hasta
        group by t.tipoTransaccion.naturaleza
        """)
List<Object[]> sumarMontosPorNaturaleza(@Param("desde") OffsetDateTime desde, @Param("hasta") OffsetDateTime hasta);
```

**Por qué:** ingresos (crédito) y egresos (débito) se agregan en la base. El método `sumarMonto` (default en la interfaz) extrae el valor de la naturaleza pedida.

**Dónde se usa:** `DashboardResumenService.obtenerResumen` → ingresos y egresos del periodo, y variaciones frente al periodo anterior.

### 2.3 Carga con join fetch — `TransaccionRepository.buscarDelPeriodo`

```java
@Query("""
        select t from Transaccion t
        join fetch t.cuenta c
        join fetch c.cliente
        join fetch t.tipoTransaccion
        where t.fechaHoraTransaccion >= :desde and t.fechaHoraTransaccion < :hasta
        order by t.fechaHoraTransaccion desc
        """)
List<Transaccion> buscarDelPeriodo(@Param("desde") OffsetDateTime desde, @Param("hasta") OffsetDateTime hasta);
```

**Por qué:** la serie diaria y los movimientos recientes usan `t.getCuenta().getCliente().getCodigoCliente()` y `t.getTipoTransaccion().getNaturaleza()`. Sin `join fetch`, Hibernate lanzaba `LazyInitializationException` al renderizar la vista (open-in-view está desactivado). También evita N+1.

**Dónde se usa:** `DashboardMovimientosService.generarMovimientos` y `obtenerMovimientosRecientes`.

## 3. Consultas derivadas usadas

| Método | Repositorio | Uso |
| :--- | :--- | :--- |
| `findByEsActivaTrueOrderByFechaEvaluacionDesc` | `EvaluacionFraudeRepository` | Historial y KPIs |
| `findByEsActivaTrueAndTransaccion_Cuenta_Cliente_CodigoClienteOrderByFechaEvaluacionDesc` | `EvaluacionFraudeRepository` | Portal del cliente |
| `countByEsActivaTrueAndTransaccion_Cuenta_Cliente_CodigoClienteAndFechaEvaluacionAfter` | `EvaluacionFraudeRepository` | Operaciones en la última hora (RF006) |
| `findByIdEvaluacionAndEsActivaTrue` | `EvaluacionFraudeRepository` | Baja lógica |
| `findByEvaluacion_IdEvaluacion` | `EvaluacionReglaRepository` | Desglose de reglas |
| `findByCodigoCliente` | `ClienteRepository` | Búsqueda de cliente |
| `findByNombreUsuario` | `UsuarioSistemaRepository` | Login |
| `findFirstByCliente_IdClienteAndNombreBeneficiario` | `BeneficiarioRepository` | Derivar tipo de destinatario |
| `findFirstByCliente_IdClienteOrderByIdCuentaAsc` | `CuentaRepository` | Cuenta principal del cliente |

## 4. Filtrado en memoria (a mejorar)

- `EvaluacionConsultaService.listarFiltrado`: carga todo el historial activo y filtra por fecha, nivel y cliente. Aceptable para el volumen actual; debería convertirse en una consulta con parámetros opcionales cuando crezca la tabla.
- `UsuarioAdminService`: usa `findAll()` para unicidad de correo, listado y conteo de administradores. Debería usar `existsByCorreoIgnoreCase`, `countByRol` y `join fetch`.

## 5. Regla de oro aplicada

Usar consulta personalizada cuando se cumpla alguna de estas condiciones:

1. La operación debe hacerse en la base (sumas, conteos, agrupaciones).
2. Se leen relaciones en bucle (N+1 o `LazyInitializationException`).
3. Se filtra por varios criterios opcionales sobre una tabla que crece.

En el resto de los casos, las consultas derivadas por nombre de método son suficientes y más legibles.
