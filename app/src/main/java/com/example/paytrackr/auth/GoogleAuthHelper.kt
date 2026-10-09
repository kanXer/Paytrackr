package com.example.paytrackr.auth

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

object GoogleAuthHelper {

    fun getGoogleWebClientId(context: Context): String {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resId != 0) {
            val id = context.getString(resId)
            if (id.isNotBlank()) return id
        }
        val customResId = context.resources.getIdentifier("google_web_client_id", "string", context.packageName)
        if (customResId != 0) {
            val id = context.getString(customResId)
            if (id.isNotBlank()) return id
        }
        return ""
    }

    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val webClientId = getGoogleWebClientId(context)
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()

        if (webClientId.isNotBlank()) {
            builder.requestIdToken(webClientId)
        }

        return GoogleSignIn.getClient(context, builder.build())
    }
}

/**
 * Beautiful Google 4-Color 'G' Icon drawn using Jetpack Compose Canvas
 */
@Composable
fun GoogleLogoIcon(modifier: Modifier = Modifier.size(20.dp)) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokeWidth = size.width * 0.2f
            val radius = (size.width - strokeWidth) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Butt)

            // Red arc (top / top-left)
            drawArc(
                color = Color(0xFFEA4335),
                startAngle = 190f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                size = Size(radius * 2f, radius * 2f),
                style = stroke,
            )

            // Yellow arc (left / bottom-left)
            drawArc(
                color = Color(0xFFFBBC05),
                startAngle = 135f,
                sweepAngle = 65f,
                useCenter = false,
                topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                size = Size(radius * 2f, radius * 2f),
                style = stroke,
            )

            // Green arc (bottom / bottom-right)
            drawArc(
                color = Color(0xFF34A853),
                startAngle = 35f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                size = Size(radius * 2f, radius * 2f),
                style = stroke,
            )

            // Blue arc (right-side curve)
            drawArc(
                color = Color(0xFF4285F4),
                startAngle = 330f,
                sweepAngle = 65f,
                useCenter = false,
                topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                size = Size(radius * 2f, radius * 2f),
                style = stroke,
            )

            // Blue horizontal crossbar in 'G'
            val barStart = Offset(center.x, center.y)
            val barEnd = Offset(center.x + radius + strokeWidth / 2f, center.y)
            drawLine(
                color = Color(0xFF4285F4),
                start = barStart,
                end = barEnd,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Square,
            )
        }
    }
}
