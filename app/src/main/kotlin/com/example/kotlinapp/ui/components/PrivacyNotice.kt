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

const val PRIVACY_VERSION = "2026-PRODUCCION-1.1"

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
                        text = "De conformidad con la Ley Estatutaria 1581 de 2012 y el Decreto 1074 de 2015 de la República de Colombia, WoodMatch garantiza la debida protección, confidencialidad y tratamiento legítimo de tus datos personales.",
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
                        text = "WoodMatch no comparte en vistas públicas la dirección de tu domicilio ni tus datos de contacto privados. Las coordenadas en el mapa corresponden a la localización del residuo o incidente forestal en la localidad de Suba.",
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
                                "• Contactar al canal oficial: privacidad@woodmatch.co.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = "4. Compromiso y Responsabilidad Ambiental",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "El usuario se compromete a registrar información veraz y a publicar exclusivamente residuos leñosos sobre los cuales tenga debida autorización de gestión, promoviendo la sostenibilidad y la preservación ecológica de Suba.",
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
