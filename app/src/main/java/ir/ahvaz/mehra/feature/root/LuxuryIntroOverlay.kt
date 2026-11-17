package ir.ahvaz.mehra.feature.root

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.ahvaz.mehra.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LuxuryIntroOverlay(onFinished: () -> Unit) {
    var visible by remember { mutableStateOf(true) }
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.85f) }
    val creatorAlpha = remember { Animatable(0f) }
    val contactAlpha = remember { Animatable(0f) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { logoAlpha.animateTo(1f, tween(650, easing = FastOutSlowInEasing)) }
        launch { logoScale.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
        launch {
            delay(450)
            creatorAlpha.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        }
        launch {
            delay(900)
            contactAlpha.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        }
        launch {
            progress.animateTo(1f, tween(3600, easing = LinearEasing))
        }
        delay(3600)
        visible = false
        delay(350)
        onFinished()
    }

    AnimatedVisibility(
        visible = visible,
        exit = fadeOut(tween(350))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0A1120),
                            Color(0xFF16233D),
                            Color(0xFF0D1526)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .scale(logoScale.value)
                        .alpha(logoAlpha.value)
                        .clip(CircleShape)
                        .background(Color(0xFF142038))
                        .border(2.dp, Color(0xFFC9A24B), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_mehra_logo),
                        contentDescription = null,
                        modifier = Modifier.size(68.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(160.dp)
                        .height(2.dp)
                        .alpha(logoAlpha.value)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color(0xFFC9A24B),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier.alpha(creatorAlpha.value),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "طراحی و توسعه",
                        fontSize = 12.sp,
                        color = Color(0xFF9AA6B8)
                    )
                    Text(
                        text = "Milad Yaghoobi",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "میلاد یعقوبی",
                        fontSize = 15.sp,
                        color = Color(0xFFE8D6AE)
                    )
                }

                Surface(
                    modifier = Modifier.alpha(contactAlpha.value),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1A2845),
                    border = BorderStroke(1.dp, Color(0xFF3A4E73))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "www.Meelano.ir",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC9A24B)
                        )
                        Text(
                            text = "09120219593",
                            fontSize = 15.sp,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF1E2D4A))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.value)
                            .height(4.dp)
                            .background(Color(0xFFC9A24B))
                    )
                }
            }

            TextButton(
                onClick = {
                    visible = false
                    onFinished()
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
            ) {
                Text(text = "رد کردن", color = Color(0xFF9AA6B8))
            }
        }
    }
}
