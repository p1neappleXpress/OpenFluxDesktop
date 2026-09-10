package tech.p1neapplexpress.openfluxdesktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import androidx.compose.foundation.Image
import tech.p1neapplexpress.openfluxdesktop.FluxColors

@Composable
fun QrDialog(
    payloadJson: String,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(FluxColors.surfaceDark)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "SCAN TO CONNECT",
                color = FluxColors.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp,
            )

            Spacer(Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                val painter = rememberQrCodePainter(payloadJson)
                Image(
                    painter = painter,
                    contentDescription = "QR code",
                    modifier = Modifier.size(248.dp),
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Open the OpenFlux client and scan this code",
                color = FluxColors.textSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FluxColors.accent,
                    contentColor = FluxColors.background,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Close", fontWeight = FontWeight.Medium)
            }
        }
    }
}