# Secuencias de servicio

Secuencias conceptuales contrastadas con `domain.mjs`, `lifecycle.mjs`, `support.mjs` y mantenimiento. Describen implementación, no pruebas físicas realizadas. [Calidad](Control-de-calidad-y-evidencias.md).

## Reserva y reprogramación

```mermaid
sequenceDiagram
    participant R as Receptor
    participant E as Edge
    participant F as Firestore
    participant G as Generador
    R->>E: Solicitar material y fecha
    E->>F: Registrar solicitud y plazo 72 h
    G->>E: Aceptar solicitud
    E->>F: Transacción de reserva exclusiva
    alt Propuesta de nueva fecha
        R->>E: Proponer reprogramación
        E->>F: Registrar propuesta y aviso
        G->>E: Aceptar o contrapropone
        E->>F: Actualizar acuerdo e invalidar pase anterior
    else Solicitud pendiente vencida
        E->>F: Marcar vencimiento sin aceptar
    end
```

## QR y doble confirmación

```mermaid
sequenceDiagram
    participant R as Receptor
    participant E as Edge
    participant F as Firestore
    participant G as Generador
    R->>E: Emitir pase autorizado
    E->>F: Guardar hash y vencimiento
    E-->>R: Pase temporal
    G->>E: Validar QR o código
    E->>F: Verificar vigencia e intentos
    alt Pase válido
        R->>E: Confirmar retiro
        G->>E: Confirmar entrega
        E->>F: Completar y crear comprobante privado
    else Pase inválido
        E-->>G: Rechazo sin completar entrega
    end
```

## Notificaciones

```mermaid
sequenceDiagram
    participant E as Edge
    participant F as Firestore
    participant M as Mantenimiento
    participant P as FCM
    E->>F: Registrar evento y aviso deduplicado
    M->>F: Reclamar entrega pendiente
    M->>P: Enviar a dispositivo habilitado
    alt Envío aceptado
        P-->>M: Resultado del proveedor
        M->>F: Guardar resultado por dispositivo
    else Fallo recuperable
        M->>F: Registrar reintento controlado
    end
    Note over F,P: Aceptación de FCM no acredita lectura en teléfono
```

## Soporte y reapertura

```mermaid
sequenceDiagram
    participant U as Usuario
    participant E as Edge
    participant F as Firestore
    participant C as Cookiestech
    U->>E: Crear incidencia
    E->>F: Caso nuevo e historial
    C->>E: Priorizar y responder
    E->>F: Validar operador y revisión esperada
    C->>E: Resolver con respuesta
    E->>F: Estado e historial más aviso
    U->>E: Reabrir con motivo
    E->>F: Validar titular y estado resuelto o cerrado
    E->>F: En progreso y evento REABIERTO
```
