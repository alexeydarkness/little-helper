package ru.littlehelper.app.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.littlehelper.app.R

/** Круглый логотип-медвежонок с тенью. */
@Composable
fun Logo(modifier: Modifier = Modifier, size: Dp = 96.dp) {
    Surface(shape = CircleShape, shadowElevation = 6.dp, modifier = modifier.size(size)) {
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "Логотип Little Helper",
            modifier = Modifier.size(size).clip(CircleShape),
        )
    }
}
