package com.example.coffeebarmobileapp.ui.landing

import com.example.coffeebarmobileapp.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun LandingScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE6D3C7)),
        contentAlignment = Alignment.TopCenter
    ) {
        BoxWithConstraints {
            val screenHeight = maxHeight
            val screenWidth = maxWidth

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Responsive image height (45% of screen height)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(screenHeight * 0.45f)
                        .clip(RoundedCornerShape(bottomStart = screenWidth * 0.5f))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.landing_image),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Welcome to Strathmore University Coffee Shop",
                    color = Color(0xFF3A322C),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "Place your order and pick your coffee within minutes...",
                    color = Color(0xFFA88C76),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons now scale
                ButtonBox("Sign In", Color(0xFF3A322C), Color.White) { onNavigateToLogin() }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Don't have an account?", color = Color(0xFF3A312C))
                Spacer(modifier = Modifier.height(12.dp))
                ButtonBox("Sign Up", Color.LightGray, Color.Black) { onNavigateToSignUp() }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ButtonBox(text: String, bg: Color, textColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.85f) // ✅ responsive width
            .height(50.dp)
            .shadow(10.dp, RoundedCornerShape(28.dp))
            .background(bg, RoundedCornerShape(28.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = textColor, fontWeight = FontWeight.Bold)
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LandingScreenPreview() {
    MaterialTheme {
        LandingScreen(
            onNavigateToLogin = {},
            onNavigateToSignUp = {}
        )
    }
}
