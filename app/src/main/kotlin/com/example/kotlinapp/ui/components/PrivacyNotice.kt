package com.example.kotlinapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

const val PRIVACY_VERSION = "2026-PILOTO-2"

@Composable
fun PrivacyNotice(
    buttonText: String = "Políticas de Privacidad y Términos de Uso",
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }

    TextButton(
        onClick = { open = true },
        modifier = modifier
    ) {
        Text(
            text = buttonText,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }

    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = {
                Text(
                    text = "Términos de Servicio y Tratamiento de Datos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Marco Normativo · Ley 1581 de 2012 (Habeas Data)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "De conformidad con la Ley Estatutaria 1581 de 2012 y el Decreto 1074 de 2015 de la República de Colombia, este piloto ofrece controles para acceder, actualizar y eliminar tus datos. Antes de compartir información, revisa el alcance del proyecto y los canales de contacto facilitados por su responsable.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = "1. Finalidad del Tratamiento de Datos",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Los datos personales suministrados (nombre o razón social, correo electrónico, número telefónico y sector de ubicación en Suba) se utilizan con el propósito exclusivo de:\n" +
                                "• Gestionar tu cuenta y autenticación segura en la plataforma.\n" +
                                "• Facilitar la coordinación y trazabilidad para el aprovechamiento circular de residuos leñosos entre generadores y receptores.\n" +
                                "• Generar los pases digitales de retiro de material vegetal.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = "2. Georreferenciación y Privacidad Territorial",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "WoodMatch no comparte en vistas públicas la dirección de tu domicilio ni tus datos de contacto privados. Las coordenadas exactas del hallazgo o material se comparten con la comunidad autenticada únicamente después de tu confirmación. Las fotos se procesan para reducir su tamaño y retirar metadatos. No publiques direcciones privadas sin autorización. Los comprobantes de entrega quedan privados entre las dos personas.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = "3. Derechos del Titular (Acceso, Rectificación y Supresión)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Como usuario titular tienes derecho a:\n" +
                                "• Actualizar tus datos personales y teléfono en cualquier momento desde los Ajustes de tu cuenta.\n" +
                                "• Cambiar tu rol en la economía circular (Generador o Receptor) según tu actividad.\n" +
                                "• Solicitar la eliminación total e irreversible de tu cuenta y de tus datos desde la sección 'Eliminar cuenta y datos' en Ajustes.\n" +
                                "• Contactar al responsable del proyecto mediante el canal que te facilitó al invitarte al piloto.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text("Servicios y consultas", style = MaterialTheme.typography.labelLarge)
                    Text("Las cuentas y publicaciones se gestionan con Firebase; las fotos y operaciones del piloto con Supabase. Si solicitas ayuda, el texto se envía a Gemini; al identificar una planta, la imagen se envía a Pl@ntNet. Sus condiciones de uso y tratamiento aplican. Las sugerencias requieren revisión humana. No incluyas información personal en las consultas. Los diagnósticos de errores son opcionales y se pueden desactivar en tu perfil.", style = MaterialTheme.typography.bodySmall)

                    Text(
                        text = "4. Compromiso y Responsabilidad Ambiental",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "El usuario se compromete a registrar información veraz y a ofrecer únicamente materiales cuya entrega tenga autorizada; los hallazgos solo informan y no autorizan su retiro, promoviendo la sostenibilidad y la preservación ecológica de Suba.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { open = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Entendido")
                }
            }
        )
    }
}
