package com.nightpos.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nightpos.app.print.PosConfig
import com.nightpos.app.ui.theme.AmberAccent
import com.nightpos.app.ui.theme.LimeAccent
import com.nightpos.app.ui.theme.NeonCyan
import com.nightpos.app.ui.theme.NeonPink
import com.nightpos.app.ui.theme.NeonPurple
import com.nightpos.app.ui.theme.NeonVioletLight
import com.nightpos.app.ui.theme.OrangeAccent
import com.nightpos.app.ui.theme.SuccessGreen

/** A single POS outlet shown in the radial FAB menu, derived from Odoo pos.config. */
data class PosOutlet(
    val name: String,
    val color: Color,
    val url: String,
)

/** Accent colour cycle for outlet circles — assigned by index. */
private val OUTLET_COLORS = listOf(
    NeonPurple, NeonCyan, AmberAccent, SuccessGreen,
    NeonPink, LimeAccent, OrangeAccent, NeonVioletLight,
)

/** Fallback shown before Odoo responds with real outlet data. */
val FALLBACK_OUTLETS = listOf(
    PosOutlet("POS", NeonPurple, "https://soho.nightpos.com/npos/point-of-sale"),
)

/** Maps a list of [PosConfig] from Odoo to coloured [PosOutlet] entries.
 *  URL format: /pos/ui/{config_id}  (Odoo redirects to /floor if the POS has a floor plan). */
fun List<PosConfig>.toOutlets(baseUrl: String): List<PosOutlet> =
    mapIndexed { i, cfg ->
        PosOutlet(
            name = cfg.name,
            color = OUTLET_COLORS[i % OUTLET_COLORS.size],
            url = "${baseUrl.trimEnd('/')}/pos/ui/${cfg.id}/login",
        )
    }

/** Floating action button that expands into a radial menu of POS outlet shortcuts. */
@Composable
fun PosOutletFab(
    outlets: List<PosOutlet>,
    onOutletSelected: (PosOutlet) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "fab-rotate",
    )

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier,
    ) {
        // Outlet circles — staggered radial appearance
        outlets.forEachIndexed { index, outlet ->
            AnimatedVisibility(
                visible = expanded,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                        visibilityThreshold = 0.01f,
                    ),
                    initialScale = 0.2f,
                ) + fadeIn(),
                exit = scaleOut(targetScale = 0.2f) + fadeOut(),
            ) {
                OutletCircleRow(
                    outlet = outlet,
                    index = index,
                    onClick = {
                        expanded = false
                        onOutletSelected(outlet)
                    },
                )
            }
        }

        // Main FAB
        ExtendedFloatingActionButton(
            text = {
                Text(
                    text = if (expanded) "Close" else "Open POS",
                    fontWeight = FontWeight.Bold,
                )
            },
            icon = {
                Icon(
                    imageVector = if (expanded) Icons.Filled.Close else Icons.Filled.ShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.graphicsLayer { rotationZ = rotationAngle },
                )
            },
            onClick = { expanded = !expanded },
            containerColor = NeonPurple,
            contentColor = Color.White,
            modifier = Modifier.shadow(elevation = 8.dp, shape = RoundedCornerShape(32.dp)),
        )
    }
}

@Composable
private fun OutletCircleRow(outlet: PosOutlet, index: Int, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        // Label chip
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.Black.copy(alpha = 0.78f),
            modifier = Modifier.padding(end = 12.dp),
        ) {
            Text(
                text = outlet.name,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            )
        }

        // Circle button
        Surface(
            shape = CircleShape,
            color = outlet.color,
            modifier = Modifier
                .size(60.dp)
                .shadow(elevation = 6.dp, shape = CircleShape)
                .clickable(onClick = onClick),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = outlet.name
                        .split(" ")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .joinToString("") { it.first().uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                )
            }
        }
    }
}
