# 📔 Guía Técnica Universal: Proyecto WoodMatch (Kotlin App)

Este documento es una enciclopedia completa de la aplicación. Está diseñado para que entiendas cada rincón del código, desde la interfaz visual hasta la lógica más profunda en los servidores de Google.

---

## 📂 1. Anatomía del Proyecto (Carpetas y Archivos)

Cada carpeta tiene una misión específica. Si una falta, la app no funciona:

*   **`app/src/main/kotlin/`**: El **Cerebro**. Contiene los archivos `.kt`. Aquí es donde escribimos las órdenes (si pasa esto, haz aquello).
*   **`app/src/main/res/`**: El **Cuerpo**.
    *   `layout/`: Archivos XML que definen cómo se ven las pantallas.
    *   `drawable/`: Imágenes, iconos y formas (botones redondeados, fondos).
    *   `values/`: Colores, textos (strings) y estilos (temas).
*   **`AndroidManifest.xml`**: El **DNI de la App**. Aquí se registran todas las pantallas y se piden los permisos al celular (Cámara, GPS, Internet).
*   **`google-services.json`**: La **Llave de Seguridad**. Es un archivo que nos da Firebase. Sin él, la app es como un celular sin señal; no puede hablar con la base de datos.
*   **`build.gradle.kts`**: La **Lista de Compras**. Aquí anotamos qué librerías externas necesitamos (Firebase, Mapas, Cámara, etc.).

---

## 🧠 2. La Magia de Kotlin: Conceptos "Raros" Explicados

Kotlin tiene símbolos que parecen extraños pero son para evitar que la app falle:

1.  **`binding` (View Binding)**: Es el puente. En lugar de decir "busca el botón login", usamos `binding.btnLogin`. Es más rápido y seguro porque Kotlin ya sabe qué botones existen en el XML.
2.  **`?.` (Llamada Segura)**: Evita el famoso error "NullPointerException". Si algo no existe, el `?.` detiene la orden en lugar de cerrar la app.
3.  **`?:` (Operador Elvis)**: Si un valor es nulo, usa el de la derecha. Ejemplo: `nombre ?: "Anónimo"`.
4.  **`lateinit`**: Le dice al programa: "Esta variable está vacía ahora, pero te prometo que le daré un valor antes de que la uses". Se usa mucho con Firebase.
5.  **`it`**: Es un nombre por defecto. Si una función te devuelve un resultado, Kotlin lo llama `it` para que no tengas que inventar un nombre.
6.  **`Regex` (Expresiones Regulares)**: Son esos códigos raros como `^[a-zA-Z...]{2,}$`. Son **filtros de seguridad** que verifican que el usuario escriba un correo real o una contraseña fuerte.

---

## 🔐 3. Autenticación y Seguridad (`MainActivity.kt`)

Este es el archivo más importante de la app. Gestiona tres estados:

### A. Inicio de Sesión (Login)
*   Toma el correo y la clave del XML.
*   Usa `auth.signInWithEmailAndPassword`.
*   Si los datos están en Firebase, te deja pasar al "Dashboard" (`SecondActivity`).

### B. Registro (Sign Up)
*   **Validación Local**: Antes de ir a internet, revisa que no falten datos y que la clave tenga números y letras.
*   **Creación en Firebase Auth**: Crea la cuenta de acceso.
*   **Guardado en Firestore**: Una cosa es el "acceso" (email/clave) y otra el "perfil". Guardamos el nombre, teléfono, ciudad y **ROL** (admin o usuario) en una base de datos aparte llamada Firestore.

### C. Recuperación de Clave
*   No es solo enviar un correo. La app pide Correo, Teléfono y Género. Si los tres NO coinciden con lo que hay en la base de datos, no permite el reseteo. Esto es seguridad extra.

---

## 🎨 4. Interfaz Visual (XML y Temas)

### XML: Los Planos
Usamos `ConstraintLayout`. Imagina que cada botón está atado con hilos (constraints) a las esquinas de la pantalla. Esto hace que la app se adapte a cualquier tamaño de celular.

### El ThemeHelper (Modos de Color)
La app tiene un archivo llamado `ThemeHelper.kt`. Su trabajo es:
1.  Leer en la memoria del celular qué tema prefieres (Primavera, Otoño o Nieve).
2.  Aplicar el estilo visual antes de que la pantalla se dibuje (`setTheme`).
3.  Esto permite que la app sea personalizada para cada usuario.

---

## 📸 5. Funciones Especiales (Cámara y GPS)

En `ReportActivity.kt` ocurre lo siguiente:
1.  **Cámara**: La app genera una "URI" (una dirección segura) para guardar la foto que vas a tomar. No usa la galería pública por privacidad.
2.  **GPS**: Usa `FusedLocationProviderClient`. Solo pide la ubicación cuando vas a enviar el reporte, para ahorrar batería.
3.  **Firestore**: Junta la descripción, la ruta de la foto y las coordenadas GPS, y lo sube todo como un solo "paquete" a la nube.

---

## 👑 6. Roles: Admin vs Usuario

En `SecondActivity.kt`, la app hace una consulta rápida a la nube:
*   "¿Qué rol tiene este UID?".
*   Si el rol es **"admin"**, aparece un botón extra para gestionar usuarios.
*   Esto se llama **Control de Acceso Basado en Roles (RBAC)**.

---

## 🚀 Resumen para tu Presentación

> "Nuestra app es un ecosistema donde **XML** proporciona una interfaz adaptable y moderna, y **Kotlin** actúa como el cerebro lógico que valida datos mediante seguridad **Regex**, gestiona la identidad con **Firebase Auth** y persiste información geográfica y perfiles en **Firestore**. Todo esto protegido por técnicas de **Null Safety** (seguridad contra nulos) propias de Kotlin."
