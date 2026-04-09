@file:Suppress(
    "MagicNumber",
    "ChainMethodContinuation",
    "FunctionSignature",
    "LongMethod",
    "ReturnCount",
    "FunctionExpressionBody",
)

package dev.crushingdigits.quotanoia.wear

import android.graphics.Color
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.degrees
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders.ARC_ANCHOR_START
import androidx.wear.protolayout.LayoutElementBuilders.Arc
import androidx.wear.protolayout.LayoutElementBuilders.ArcLine
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.FONT_WEIGHT_BOLD
import androidx.wear.protolayout.LayoutElementBuilders.FontStyle
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
import androidx.wear.protolayout.LayoutElementBuilders.Layout
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.LayoutElementBuilders.Text
import androidx.wear.protolayout.LayoutElementBuilders.VERTICAL_ALIGN_CENTER
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.TimelineBuilders.TimelineEntry
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Watch tile that displays Claude usage data synced from the phone.
 * No direct API calls — all data comes via MessageClient from phone.
 */
class UsageTileService : TileService() {

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest,
    ): ListenableFuture<TileBuilders.Tile> {
        val isRefreshing = requestParams.currentState.lastClickableId == "refresh"

        // If this is a LoadAction (user tapped refresh), request fresh data from phone
        if (isRefreshing) {
            RefreshHelper.requestRefreshFromPhone(this)
        }

        val store = WearApp.instance.store
        val entries = store.getAll()

        val root = if (entries.isEmpty()) emptyLayout() else arcLayout(entries, isRefreshing)

        val tile = TileBuilders.Tile
            .Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setFreshnessIntervalMillis(FRESHNESS_INTERVAL_MS)
            .setTileTimeline(
                Timeline
                    .Builder()
                    .addTimelineEntry(
                        TimelineEntry
                            .Builder()
                            .setLayout(Layout.Builder().setRoot(root).build())
                            .build(),
                    ).build(),
            ).build()

        return Futures.immediateFuture(tile)
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<Resources> =
        Futures
            .immediateFuture(
                Resources.Builder().setVersion(RESOURCES_VERSION).build(),
            )

    // ── Layouts ────────────────────────────────────────────

    private fun emptyLayout(): LayoutElement = Box
        .Builder()
        .setWidth(expand())
        .setHeight(expand())
        .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
        .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
        .addContent(
            Text
                .Builder()
                .setText("Open phone app\nto add accounts")
                .setFontStyle(
                    FontStyle
                        .Builder()
                        .setSize(sp(FONT_SIZE_SMALL))
                        .setColor(argb(Color.parseColor("#AAAAAA")))
                        .build(),
                ).build(),
        ).build()

    /**
     * Arc-based layout: shows usage as colored arcs around the tile edge
     * with text details in the center.
     */
    private fun arcLayout(
        entries: List<WearAccountStore.AccountWithUsage>,
        isRefreshing: Boolean = false,
    ): LayoutElement {
        val column = Column
            .Builder()
            .setWidth(expand())
            .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)

        // Title
        column.addContent(
            Text
                .Builder()
                .setText("quotanoia")
                .setFontStyle(
                    FontStyle
                        .Builder()
                        .setSize(sp(FONT_SIZE_TITLE))
                        .setColor(argb(Color.parseColor("#7B9CFF")))
                        .setWeight(FONT_WEIGHT_BOLD)
                        .build(),
                ).build(),
        )
        column.addContent(spacer(SPACER_SMALL_DP))

        for (entry in entries.take(MAX_ACCOUNTS_SHOWN)) {
            column.addContent(accountRow(entry.account.name, entry.usage))
            column.addContent(spacer(SPACER_SMALL_DP))
        }

        // Refresh hint at bottom
        val refreshText = if (isRefreshing) "Refreshing\u2026" else "\u21BB tap to refresh"
        column.addContent(
            Text
                .Builder()
                .setText(refreshText)
                .setFontStyle(
                    FontStyle
                        .Builder()
                        .setSize(sp(FONT_SIZE_REFRESH_HINT))
                        .setColor(argb(Color.parseColor("#666666")))
                        .build(),
                ).setModifiers(
                    ModifiersBuilders.Modifiers
                        .Builder()
                        .setClickable(
                            ModifiersBuilders.Clickable
                                .Builder()
                                .setId("refresh")
                                .setOnClick(
                                    ActionBuilders.LoadAction.Builder().build(),
                                ).build(),
                        ).build(),
                ).build(),
        )

        // If we have at least one account with data, wrap with arcs
        val firstWithData = entries.firstOrNull { it.usage.fiveHourPct >= 0 }
        if (firstWithData != null) {
            return arcWrappedLayout(
                column.build(),
                firstWithData.usage.fiveHourPct,
            )
        }

        return Box
            .Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
            .addContent(column.build())
            .build()
    }

    /**
     * Wraps content with an arc showing 5h usage percentage.
     */
    private fun arcWrappedLayout(
        center: LayoutElement,
        fiveHourPct: Float,
    ): LayoutElement {
        val sweep = (fiveHourPct / PCT_MAX).coerceIn(0f, 1f) * ARC_FULL_DEGREES

        val arc = Arc
            .Builder()
            .setAnchorAngle(degrees(ARC_ANCHOR_ANGLE))
            .setAnchorType(ARC_ANCHOR_START)
            .addContent(
                ArcLine
                    .Builder()
                    .setLength(degrees(sweep))
                    .setThickness(dp(ARC_THICKNESS_DP))
                    .setColor(argb(pctToArgb(fiveHourPct)))
                    .build(),
            ).addContent(
                ArcLine
                    .Builder()
                    .setLength(degrees((ARC_FULL_DEGREES - sweep).coerceAtLeast(ARC_MIN_REMAINDER)))
                    .setThickness(dp(ARC_THICKNESS_DP))
                    .setColor(argb(Color.parseColor("#1A1A2E")))
                    .build(),
            ).build()

        return Box
            .Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
            .addContent(arc)
            .addContent(
                Box
                    .Builder()
                    .setWidth(expand())
                    .setHeight(expand())
                    .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
                    .addContent(center)
                    .build(),
            ).build()
    }

    @Suppress("LongMethod")
    private fun accountRow(
        name: String,
        usage: WearAccountStore.WearUsage,
    ): LayoutElement {
        val nameColor = if (usage.hasError) {
            Color.parseColor("#E53935")
        } else {
            Color.parseColor("#E0E0E0")
        }

        val col = Column
            .Builder()
            .setWidth(expand())
            .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)

        col.addContent(
            Text
                .Builder()
                .setText(name)
                .setFontStyle(
                    FontStyle
                        .Builder()
                        .setSize(sp(FONT_SIZE_ACCOUNT_NAME))
                        .setColor(argb(nameColor))
                        .setWeight(FONT_WEIGHT_BOLD)
                        .build(),
                ).setMaxLines(1)
                .build(),
        )

        if (usage.fiveHourPct >= 0) {
            val row = Row
                .Builder()
                .setVerticalAlignment(VERTICAL_ALIGN_CENTER)

            val fiveHourLabel = coloredLabel(
                "5h:${usage.fiveHourPct.toInt()}%",
                pctToArgb(usage.fiveHourPct),
                FONT_SIZE_USAGE_LABEL,
            )
            row.addContent(fiveHourLabel)
            row.addContent(
                Text
                    .Builder()
                    .setText("  ")
                    .setFontStyle(FontStyle.Builder().setSize(sp(FONT_SIZE_USAGE_LABEL)).build())
                    .build(),
            )
            val sevenDayLabel = coloredLabel(
                "7d:${usage.sevenDayPct.toInt()}%",
                pctToArgb(usage.sevenDayPct),
                FONT_SIZE_USAGE_LABEL,
            )
            row.addContent(sevenDayLabel)

            col.addContent(row.build())

            // Reset times
            val now = System.currentTimeMillis()
            val resetRow = Row
                .Builder()
                .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
            resetRow.addContent(
                dimLabel(
                    "\u21BB${formatResetTime(usage.fiveHourResetsAtMs, now)}",
                    FONT_SIZE_RESET,
                ),
            )
            resetRow.addContent(
                Text
                    .Builder()
                    .setText("  ")
                    .setFontStyle(FontStyle.Builder().setSize(sp(FONT_SIZE_RESET)).build())
                    .build(),
            )
            resetRow.addContent(
                dimLabel(
                    "\u21BB${formatResetTime(usage.sevenDayResetsAtMs, now)}",
                    FONT_SIZE_RESET,
                ),
            )
            col.addContent(resetRow.build())
        } else if (usage.hasError) {
            col.addContent(
                Text
                    .Builder()
                    .setText(usage.errorMsg ?: "Error")
                    .setFontStyle(
                        FontStyle
                            .Builder()
                            .setSize(sp(FONT_SIZE_ERROR))
                            .setColor(argb(Color.parseColor("#E53935")))
                            .build(),
                    ).setMaxLines(1)
                    .build(),
            )
        }

        return col.build()
    }

    private fun coloredLabel(
        text: String,
        color: Int,
        sizeSp: Float,
    ): LayoutElement = Text
        .Builder()
        .setText(text)
        .setFontStyle(
            FontStyle
                .Builder()
                .setSize(sp(sizeSp))
                .setColor(argb(color))
                .setWeight(FONT_WEIGHT_BOLD)
                .build(),
        ).build()

    private fun dimLabel(
        text: String,
        sizeSp: Float,
    ): LayoutElement = Text
        .Builder()
        .setText(text)
        .setFontStyle(
            FontStyle
                .Builder()
                .setSize(sp(sizeSp))
                .setColor(argb(Color.parseColor("#888888")))
                .build(),
        ).build()

    private fun spacer(heightDp: Float): LayoutElement = Spacer.Builder().setHeight(dp(heightDp)).build()

    private fun pctToArgb(pct: Float): Int {
        val clamped = pct.coerceIn(0f, PCT_MAX)
        return when {
            clamped <= PCT_LOW -> Color.parseColor("#4CAF50")
            clamped <= PCT_MEDIUM -> Color.parseColor("#FFCA28")
            clamped <= PCT_HIGH -> Color.parseColor("#FF9800")
            else -> Color.parseColor("#E53935")
        }
    }

    private fun formatResetTime(resetMs: Long, nowMs: Long): String {
        if (resetMs <= 0) return "?"
        val diff = resetMs - nowMs
        if (diff <= 0) return "now"
        val totalSec = diff / MILLIS_PER_SECOND
        val days = totalSec / SECONDS_PER_DAY
        val hours = totalSec % SECONDS_PER_DAY / SECONDS_PER_HOUR
        val mins = totalSec % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
        return when {
            days > 0 -> "${days}d${hours}h"
            hours > 0 -> "${hours}h${mins}m"
            else -> "${mins}m"
        }
    }

    companion object {
        private const val RESOURCES_VERSION = "2"
        private const val FRESHNESS_INTERVAL_MS = 5 * 60 * 1000L
        private const val MAX_ACCOUNTS_SHOWN = 3
        private const val SPACER_SMALL_DP = 6f
        private const val FONT_SIZE_SMALL = 14f
        private const val FONT_SIZE_TITLE = 13f
        private const val FONT_SIZE_ACCOUNT_NAME = 13f
        private const val FONT_SIZE_USAGE_LABEL = 13f
        private const val FONT_SIZE_ERROR = 11f
        private const val FONT_SIZE_RESET = 10f
        private const val FONT_SIZE_REFRESH_HINT = 10f
        private const val PCT_MAX = 100f
        private const val PCT_LOW = 40f
        private const val PCT_MEDIUM = 70f
        private const val PCT_HIGH = 85f
        private const val ARC_FULL_DEGREES = 360f
        private const val ARC_ANCHOR_ANGLE = -90f
        private const val ARC_THICKNESS_DP = 6f
        private const val ARC_MIN_REMAINDER = 0.1f
        private const val MILLIS_PER_SECOND = 1000L
        private const val SECONDS_PER_DAY = 86400L
        private const val SECONDS_PER_HOUR = 3600L
        private const val SECONDS_PER_MINUTE = 60L
    }
}
