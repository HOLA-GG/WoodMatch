package com.example.kotlinapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

const val PRIVACY_VERSION = "2026-10-22-prototipo"
@Composable
fun PrivacyNotice() {
    var open by remember { mutableStateOf(false) }
    TextButton(onClick = { open = true }) { Text("Privacidad, permisos y condiciones") }
    if (open) AlertDialog(onDismissRequest = { open = false }, title = { Text("WoodMatch · versión de prueba") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            Text("Aviso $PRIVACY_VERSION. Usa únicamente datos ficticios durante las pruebas. Cookietech@woodmatch.org es un contacto de ejemplo, no un canal operativo. El responsable del tratamiento, su domicilio y canal real deben definirse antes del uso público.")
            Text("Finalidad: gestionar cuentas, publicaciones de residuos leñosos y solicitudes de retiro. Al conectar Firebase, estos datos y las fotos se almacenan en servicios de Google. Se debe informar la región y las condiciones de transmisión o transferencia internacional antes de operar con datos reales.")
            Text("Tu foto y nombre de publicación se muestran a usuarios con sesión. Una persona que reciba el enlace de descarga de una foto puede abrirlo; no publiques imágenes privadas. Publicamos referencias de sector, no tu domicilio ni teléfono. Evita imágenes de terceros, placas y rostros. El QR temporal autoriza un retiro; no certifica identidad ni habilita ingreso a propiedades por sí solo.")
            Text("La cámara se abre solo al pulsar Tomar foto, mediante la aplicación de cámara del dispositivo. Puedes cancelar o elegir una imagen. No solicitamos acceso general a almacenamiento, contactos o micrófono. El catálogo no necesita ubicación precisa.")
            Text("En Colombia, la Ley 1581 de 2012 y su reglamentación compilada en el Decreto 1074 de 2015 orientan la protección de datos personales. Puedes conocer, actualizar y rectificar tus datos, solicitar prueba e información de la autorización, revocarla y solicitar supresión cuando proceda; también acudir a la SIC después del trámite aplicable ante el responsable.")
            Text("En Configuración puedes exportar tus datos o solicitar eliminar la cuenta y sus datos mediante el servidor. El historial local antiguo permanece en esta instalación hasta borrar los datos locales. No se ha definido una política productiva de retención: no uses esta versión con datos reales.")
            Text("Las recomendaciones botánicas y de aprovechamiento son orientativas y deben confirmarse. No certifican especie, resistencia ni seguridad. Solo publica material que estés autorizado a gestionar; la aplicación no sustituye permisos ambientales, de transporte o de ingreso.")
        } }, confirmButton = { TextButton(onClick = { open = false }) { Text("Cerrar") } })
}
