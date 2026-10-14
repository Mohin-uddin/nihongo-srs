package com.nihongo.srs.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Shared building blocks so every screen looks like one app.
 * Text is bilingual by hierarchy: English on top, Bangla smaller and muted below,
 * instead of "English • বাংলা" squeezed onto one line.
 */

val ScreenPadding = 20.dp

/** Page with a clean header (title + Bangla subtitle) and an optional back arrow. */
@Composable
fun AppScreen(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    snackbar: SnackbarHostState? = null,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { ScreenHeader(title, subtitle, onBack, actions) },
        bottomBar = bottomBar,
        snackbarHost = { snackbar?.let { SnackbarHost(it) } },
        content = content,
    )
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(start = if (onBack != null) 8.dp else ScreenPadding, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Spacer(Modifier.width(4.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = if (onBack != null) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        actions()
    }
}

/** White rounded card with a hairline border. Clickable when [onClick] is set. */
@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = MaterialTheme.colorScheme.surface,
    border: Boolean = true,
    padding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = color,
        border = if (border) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
    ) {
        Column(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

/** English line with the Bangla line under it. */
@Composable
fun Bilingual(
    en: String,
    bn: String?,
    modifier: Modifier = Modifier,
    enStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium,
    bnStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color.Unspecified,
    bnColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    align: TextAlign? = null,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
) {
    Column(modifier, horizontalAlignment = horizontalAlignment) {
        Text(en, style = enStyle, color = color, textAlign = align)
        if (!bn.isNullOrBlank()) Text(bn, style = bnStyle, color = bnColor, textAlign = align)
    }
}

/** Section title above a group of cards. */
@Composable
fun SectionTitle(en: String, bn: String? = null, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text(en, style = MaterialTheme.typography.titleLarge)
            if (bn != null) Text(bn, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing()
    }
}

/** Tall rounded primary button: English label, optional Bangla under it. */
@Composable
fun BigButton(
    en: String,
    bn: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    container: Color = MaterialTheme.colorScheme.primary,
    content: Color = MaterialTheme.colorScheme.onPrimary,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 58.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
        }
        ButtonLabel(en, bn)
    }
}

/** Outlined version of [BigButton]. */
@Composable
fun BigOutlinedButton(
    en: String,
    bn: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 58.dp),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
        }
        ButtonLabel(en, bn)
    }
}

@Composable
private fun ButtonLabel(en: String, bn: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(en, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        if (bn != null) {
            Text(bn, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center,
                color = androidx.compose.material3.LocalContentColor.current.copy(alpha = 0.8f))
        }
    }
}

/** Rounded square with an icon, used on menu tiles. */
@Composable
fun IconBadge(icon: ImageVector, tint: Color, background: Color, size: Dp = 44.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size / 3.2f)).background(background),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.52f)) }
}

/** A menu tile: icon, title, Bangla subtitle, optional detail on the right. */
@Composable
fun MenuTile(
    icon: ImageVector,
    en: String,
    bn: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    tintBackground: Color = MaterialTheme.colorScheme.primaryContainer,
    detail: String? = null,
) {
    SoftCard(modifier, onClick = onClick, padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, tint, tintBackground)
            Spacer(Modifier.width(14.dp))
            Bilingual(en, bn, Modifier.weight(1f))
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.labelLarge, color = tint)
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

/** Square tile for a 2-column grid. */
@Composable
fun GridTile(
    icon: ImageVector,
    en: String,
    bn: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    tintBackground: Color = MaterialTheme.colorScheme.primaryContainer,
) {
    SoftCard(modifier, onClick = onClick, padding = 16.dp) {
        IconBadge(icon, tint, tintBackground)
        Bilingual(en, bn, enStyle = MaterialTheme.typography.titleMedium, bnStyle = MaterialTheme.typography.bodyMedium)
    }
}

/** Small rounded label, e.g. a stage or level. */
@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primaryContainer,
    textColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    icon: ImageVector? = null,
) {
    Row(
        modifier.clip(CircleShape).background(color).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = textColor, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = textColor, fontWeight = FontWeight.SemiBold)
    }
}

/** Selectable chip used for filters (pill shaped, filled when selected). */
@Composable
fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit, enabled: Boolean = true) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Box(
        Modifier
            .clip(CircleShape)
            .background(bg)
            .then(if (selected) Modifier else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) { Text(text, style = MaterialTheme.typography.labelLarge, color = fg) }
}

/** Thick rounded progress bar. */
@Composable
fun ProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    track: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    height: Dp = 10.dp,
) {
    Box(modifier.fillMaxWidth().height(height).clip(CircleShape).background(track)) {
        Box(
            Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).clip(CircleShape).background(color)
        )
    }
}

/** Circular progress ring with content in the middle. */
@Composable
fun ProgressRing(
    fraction: Float,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    stroke: Dp = 7.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    track: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    content: @Composable () -> Unit = {},
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = stroke.toPx()
            val arcSize = Size(this.size.width - s, this.size.height - s)
            val topLeft = Offset(s / 2, s / 2)
            drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(s))
            drawArc(color, -90f, 360f * fraction.coerceIn(0f, 1f), false, topLeft, arcSize,
                style = Stroke(s, cap = StrokeCap.Round))
        }
        content()
    }
}

/** Small "1 / 10" counter + progress bar used at the top of sessions. */
@Composable
fun SessionProgress(index: Int, total: Int, modifier: Modifier = Modifier, label: String? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ProgressBar((index) / total.coerceAtLeast(1).toFloat(), Modifier.weight(1f), height = 8.dp)
        Spacer(Modifier.width(12.dp))
        Text(label ?: "${index + 1} / $total", style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Big centred message for empty states. */
@Composable
fun EmptyState(emoji: String, en: String, bn: String, modifier: Modifier = Modifier, action: @Composable () -> Unit = {}) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(emoji, style = MaterialTheme.typography.displayMedium)
        Bilingual(en, bn, align = TextAlign.Center, horizontalAlignment = Alignment.CenterHorizontally,
            enStyle = MaterialTheme.typography.titleLarge)
        action()
    }
}
