package com.colombianapps.toonizapp.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colombianapps.toonizapp.R
import kotlinx.coroutines.delay

private val BgDark = Color(0xFF071215)
private val PanelBg = Color(0xFF0D1E22)
private val ActiveCyan = Color(0xFF00CCCB)
private val TextMuted = Color(0xFF8FA8B0)

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2000L) // 2 seconds splash loading
        onTimeout()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BgDark
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Official Tooniz Logo in Studio Container
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(PanelBg)
                        .border(1.5.dp, ActiveCyan, RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_logo_tooniz),
                        contentDescription = "Tooniz Logo",
                        modifier = Modifier.size(110.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Tooniz Studio",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = "Estudio de Animación Profesional · v2.4 Pro",
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                }
            }
        }
    }
}
