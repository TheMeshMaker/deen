package io.github.themeshmaker.dunya.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.themeshmaker.dunya.core.designsystem.icon.DeenIcons
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenLook
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenMotion
import io.github.themeshmaker.dunya.core.designsystem.theme.DeenTheme
import io.github.themeshmaker.dunya.core.designsystem.theme.SquircleShape

/** A card in the current style: tinted and flat (Classic) or white, rounder and hairline-edged (Sleek). */
@Composable
fun DeenCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = DeenTheme.look.card,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val look = DeenTheme.look
    val interaction = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interaction, enabled = onClick != null && look.motion)
            .softShadow(look)
            .innerRule(look),
        shape = look.cardShape,
        color = containerColor,
        // Glass cards are see-through, so their text colour can't be derived from the fill.
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = look.cardBorder,
        onClick = onClick ?: {},
        enabled = onClick != null,
        interactionSource = interaction
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

/**
 * A pushed screen: top bar with back button, the style's page colour behind everything.
 * Top-level tabs use [DeenTabScaffold] instead.
 *
 * Sleek shows a large title that folds into the bar as the content scrolls (turn it off with
 * [largeTitle] where space matters more, e.g. the Quran reader or the Qibla compass).
 */
@Composable
fun DeenScaffold(
    title: String,
    onBack: (() -> Unit)?,
    backLabel: String,
    modifier: Modifier = Modifier,
    largeTitle: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val look = DeenTheme.look
    DeenBarScaffold(
        title = title,
        large = look.sleek && largeTitle,
        smallStyle = if (look.sleek) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
        navigationIcon = { if (onBack != null) BackButton(onBack, backLabel) },
        modifier = modifier,
        actions = actions,
        bottomBar = bottomBar,
        content = content
    )
}

/** A top-level tab: big title, no back button (in Sleek, a large title that folds away on scroll). */
@Composable
fun DeenTabScaffold(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    DeenBarScaffold(
        title = title,
        large = DeenTheme.look.sleek,
        smallStyle = MaterialTheme.typography.headlineSmall,
        navigationIcon = {},
        modifier = modifier,
        actions = actions,
        bottomBar = {},
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeenBarScaffold(
    title: String,
    large: Boolean,
    smallStyle: TextStyle,
    navigationIcon: @Composable () -> Unit,
    modifier: Modifier,
    actions: @Composable RowScope.() -> Unit,
    bottomBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val look = DeenTheme.look
    val bar = if (look.pageBrush != null) Color.Transparent else look.page
    val colors = TopAppBarDefaults.topAppBarColors(containerColor = bar, scrolledContainerColor = bar)
    if (!large) {
        Scaffold(
            modifier = modifier.pageBackground(look),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            topBar = {
                TopAppBar(
                    title = { Text(title, style = smallStyle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = navigationIcon,
                    actions = actions,
                    colors = colors
                )
            },
            bottomBar = bottomBar,
            content = content
        )
        return
    }
    // A large title: big at the top of the page, folding into the bar as the page scrolls, with a
    // hairline under the bar once it has folded.
    val behavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hairline = MaterialTheme.colorScheme.outlineVariant
    Scaffold(
        modifier = modifier
            .pageBackground(look)
            .nestedScroll(behavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            LargeTopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = navigationIcon,
                actions = actions,
                colors = colors,
                expandedHeight = 120.dp,
                scrollBehavior = behavior,
                modifier = Modifier.drawWithContent {
                    drawContent()
                    val shown = behavior.state.collapsedFraction
                    if (shown > 0f) {
                        val line = 0.5.dp.toPx()
                        drawRect(hairline.copy(alpha = 0.6f * shown), Offset(0f, size.height - line), Size(size.width, line))
                    }
                }
            )
        },
        bottomBar = bottomBar,
        content = content
    )
}

/** The style's page: a plain colour, or Glass's soft gradient (drawn in window coordinates so screens line up). */
fun Modifier.pageBackground(look: DeenLook): Modifier = look.pageBrush?.let { background(it) } ?: background(look.page)

/** Heritage: a thin second rule just inside the card's frame, like a manuscript border. */
fun Modifier.innerRule(look: DeenLook, shape: Shape = look.cardShape): Modifier {
    val color = look.cardOrnament ?: return this
    return drawWithContent {
        drawContent()
        val inset = 4.dp.toPx()
        val outline = shape.createOutline(Size(size.width - inset * 2, size.height - inset * 2), layoutDirection, this)
        translate(inset, inset) { drawOutline(outline, color, style = Stroke(0.75.dp.toPx())) }
    }
}

/** Sleek's wide, soft, theme-tinted shadow under a card (nothing in Classic or dark mode). */
fun Modifier.softShadow(look: DeenLook, shape: Shape = look.cardShape): Modifier = if (look.cardElevation > 0.dp) {
    shadow(look.cardElevation, shape, clip = false, ambientColor = look.shadowColor, spotColor = look.shadowColor)
} else {
    this
}

@Composable
private fun BackButton(onBack: () -> Unit, label: String) {
    val look = DeenTheme.look
    IconButton(
        onClick = onBack,
        colors = if (look.sleek) {
            IconButtonDefaults.iconButtonColors(containerColor = look.card)
        } else {
            IconButtonDefaults.iconButtonColors()
        },
        modifier = if (look.sleek) {
            Modifier
                .padding(start = 8.dp)
                .softShadow(look, CircleShape)
        } else {
            Modifier
        }
    ) {
        Icon(painterResource(DeenIcons.Back), contentDescription = label)
    }
}

/** Small heading above a group of cards or settings. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(top = 8.dp, bottom = 2.dp, start = 4.dp)
    )
}

/**
 * A number inside an eight-pointed star outline (the Rub el Hizb), as printed Mushafs mark
 * surah and verse numbers.
 */
@Composable
fun StarBadge(text: String, modifier: Modifier = Modifier, size: Dp = 40.dp, color: Color = MaterialTheme.colorScheme.primary) {
    val px = with(LocalDensity.current) { size.toPx() }
    val star = remember(px) { octagramPath(outerRadius = px * 0.46f) }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            translate(this.size.width / 2f, this.size.height / 2f) {
                drawPath(star, color, style = Stroke(width = 1.5.dp.toPx()))
            }
        }
        Text(
            text,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = if (text.length > 2) 10.sp else 12.sp),
            color = color,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

/** A pill-shaped single-choice control (e.g. Gregorian | Hijri), in place of Material's segmented buttons. */
@Composable
fun <T> PillChoice(options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val motion = DeenTheme.look.motion
    val index = options.indexOf(selected).coerceAtLeast(0).toFloat()
    // The highlight glides from option to option (Sleek) or jumps there (Classic).
    val position by animateFloatAsState(index, if (motion) DeenMotion.snappy() else snap(), label = "pill")
    val gap = 4.dp
    Row(
        modifier
            .clip(CircleShape)
            .background(colors.surfaceContainerHigh)
            .padding(4.dp)
            .drawBehind {
                val gapPx = gap.toPx()
                val segment = (size.width - gapPx * (options.size - 1)) / options.size
                val start = position * (segment + gapPx)
                val x = if (layoutDirection == LayoutDirection.Rtl) size.width - start - segment else start
                drawRoundRect(colors.primary, Offset(x, 0f), Size(segment, size.height), CornerRadius(size.height / 2f))
            },
        horizontalArrangement = Arrangement.spacedBy(gap)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val text = label(option)
            val textColor by animateColorAsState(if (isSelected) colors.onPrimary else colors.onSurfaceVariant, label = "pillText")
            Box(
                Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .clickable(role = Role.RadioButton) {
                        if (motion && !isSelected) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        onSelect(option)
                    }
                    .semantics {
                        this.selected = isSelected
                        contentDescription = text
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text,
                    style = MaterialTheme.typography.labelLarge,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** One row of a settings or menu list. */
@Composable
fun MenuRow(
    title: String,
    modifier: Modifier = Modifier,
    icon: Int? = null,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (icon != null) {
            val sleek = DeenTheme.look.sleek
            val primary = MaterialTheme.colorScheme.primary
            Box(
                Modifier
                    .size(if (sleek) 34.dp else 40.dp)
                    .clip(if (sleek) IconTileShape else CircleShape)
                    .background(if (sleek) DeenTheme.gradient.brush else SolidColor(primary.copy(alpha = 0.10f))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(icon),
                    contentDescription = null,
                    tint = if (sleek) DeenTheme.gradient.content else primary,
                    modifier = Modifier.size(if (sleek) 19.dp else 22.dp)
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing?.invoke()
    }
}

/** The themed gradient with the star pattern, in the style's hero shape. */
@Composable
fun GradientHero(modifier: Modifier = Modifier, shape: Shape = DeenTheme.look.heroShape, content: @Composable BoxScope.() -> Unit) {
    val gradient = DeenTheme.gradient
    Box(
        modifier
            .softShadow(DeenTheme.look, shape)
            .clip(shape)
            .background(gradient.brush)
    ) {
        GeometricPattern(color = gradient.pattern, modifier = Modifier.matchParentSize())
        content()
    }
}

private val IconTileShape = SquircleShape(10.dp)

/** A thin rounded progress bar in the primary colour. */
@Composable
fun ThinProgress(fraction: Float, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    val shown by animateFloatAsState(
        fraction.coerceIn(0f, 1f),
        if (DeenTheme.look.motion) DeenMotion.gentle() else snap(),
        label = "progress"
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f))
    ) {
        Box(
            Modifier
                .fillMaxWidth(shown)
                .height(6.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

/** A small rounded label, e.g. a hadith grade. */
@Composable
fun Tag(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = modifier
            .clip(CircleShape)
            .border(1.dp, color.copy(alpha = 0.5f), CircleShape)
            .background(color.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    )
}
