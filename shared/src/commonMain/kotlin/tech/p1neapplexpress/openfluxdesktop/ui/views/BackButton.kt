package tech.p1neapplexpress.openfluxdesktop.ui.views

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import tech.p1neapplexpress.openfluxdesktop.FluxColors

@Composable
fun BackButton(
    modifier: Modifier,
    onBackButtonClick: () -> Unit,
    color: Color = FluxColors.white,
) {
    TextButton(modifier = modifier, onClick = onBackButtonClick) {
        Text(
            text = "<",
            fontSize = 20.sp,
            color = color,
        )
    }
}