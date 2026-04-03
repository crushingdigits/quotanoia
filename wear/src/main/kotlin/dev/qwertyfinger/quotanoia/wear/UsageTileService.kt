package dev.qwertyfinger.quotanoia.wear

import android.graphics.Color
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.degrees
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders.*
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.TimelineBuilders.TimelineEntry
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.TimeUnit

/**
 * Watch tile that displays Claude usage data synced from the phone.
 * No direct API calls — all data comes via MessageClient from phone.
 */
class UsageTileService : TileService() {

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest,
    ): ListenableFuture<TileBuilders.Tile> {
        // If this is a LoadAction (user tapped refresh), request fresh data from phone
        if (requestParams.currentState.lastClickableId == "refresh") {
            RefreshHelper.requestRefreshFromPhone(this)
        }

        val store = WearApp.instance.store
        val entries = store.getAll()

        val root = if (entries.isEmpty()) emptyLayout() else arcLayout(entries)

        val tile = TileBuilders.Tile.Builder()
            .setResourcesVersion("2")
            .setFreshnessIntervalMillis(5 * 60 * 1000L)
            .setTileTimeline(
                Timeline.Builder()
                    .addTimelineEntry(
                        TimelineEntry.Builder()
                            .setLayout(Layout.Builder().setRoot(root).build())
                            .build()
                    )
                    .build()
            )
            .build()

        return Futures.immediateFuture(tile)
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<Resources> {
        return Futures.immediateFuture(
            Resources.Builder().setVersion("2").build()
        )
    }

    // ── Layouts ────────────────────────────────────────────

    private fun emptyLayout(): LayoutElement =
        Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
            .addContent(
                Text.Builder()
                    .setText("Open phone app\nto add accounts")
                    .setFontStyle(
                        FontStyle.Builder()
                            .setSize(sp(14f))
                            .setColor(argb(Color.parseColor("#AAAAAA")))
                            .build()
                    )
                    .build()
            )
            .build()

    /**
     * Arc-based layout: shows usage as colored arcs around the tile edge
     * with text details in the center.
     */
    private fun arcLayout(entries: List<WearAccountStore.AccountWithUsage>): LayoutElement {
        val column = Column.Builder()
            .setWidth(expand())
            .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)

        // Title
        column.addContent(
            Text.Builder()
                .setText("quotanoia")
                .setFontStyle(
                    FontStyle.Builder()
                        .setSize(sp(13f))
                        .setColor(argb(Color.parseColor("#7B9CFF")))
                        .setWeight(FONT_WEIGHT_BOLD)
                        .build()
                )
                .build()
        )
        column.addContent(spacer(6f))

        for (entry in entries.take(3)) {
            column.addContent(accountRow(entry.account.name, entry.usage))
            column.addContent(spacer(6f))
        }

        // Refresh hint at bottom
        column.addContent(
            Text.Builder()
                .setText("↻ tap to refresh")
                .setFontStyle(
                    FontStyle.Builder()
                        .setSize(sp(10f))
                        .setColor(argb(Color.parseColor("#666666")))
                        .build()
                )
                .setModifiers(
                    ModifiersBuilders.Modifiers.Builder()
                        .setClickable(
                            ModifiersBuilders.Clickable.Builder()
                                .setId("refresh")
                                .setOnClick(
                                    ActionBuilders.LoadAction.Builder().build()
                                )
                                .build()
                        )
                        .build()
                )
                .build()
        )

        // If we have at least one account with data, wrap with arcs
        val firstWithData = entries.firstOrNull { it.usage.fiveHourPct >= 0 }
        if (firstWithData != null) {
            return arcWrappedLayout(
                column.build(),
                firstWithData.usage.fiveHourPct,
                firstWithData.usage.sevenDayPct,
            )
        }

        return Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
            .addContent(column.build())
            .build()
    }

    /**
     * Wraps content with two concentric arcs showing 5h (outer) and 7d (inner) usage.
     */
    private fun arcWrappedLayout(
        center: LayoutElement,
        fiveHourPct: Float,
        sevenDayPct: Float,
    ): LayoutElement {
        val outerSweep = (fiveHourPct / 100f).coerceIn(0f, 1f) * 360f
        val innerSweep = (sevenDayPct / 100f).coerceIn(0f, 1f) * 360f

        // Outer arc (5h usage)
        val outerArc = Arc.Builder()
            .setAnchorAngle(degrees(-90f))
            .setAnchorType(ARC_ANCHOR_START)
            .addContent(
                ArcLine.Builder()
                    .setLength(degrees(outerSweep))
                    .setThickness(dp(6f))
                    .setColor(argb(pctToArgb(fiveHourPct)))
                    .build()
            )
            .addContent(
                ArcLine.Builder()
                    .setLength(degrees((360f - outerSweep).coerceAtLeast(0.1f)))
                    .setThickness(dp(6f))
                    .setColor(argb(Color.parseColor("#1A1A2E")))
                    .build()
            )
            .build()

        return Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
            .addContent(outerArc)
            .addContent(
                Box.Builder()
                    .setWidth(expand())
                    .setHeight(expand())
                    .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
                    .addContent(center)
                    .build()
            )
            .build()
    }

    private fun accountRow(
        name: String,
        usage: WearAccountStore.WearUsage,
    ): LayoutElement {
        val nameColor = if (usage.hasError) Color.parseColor("#E53935") else Color.parseColor("#E0E0E0")

        val col = Column.Builder()
            .setWidth(expand())
            .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)

        col.addContent(
            Text.Builder()
                .setText(name)
                .setFontStyle(
                    FontStyle.Builder()
                        .setSize(sp(13f))
                        .setColor(argb(nameColor))
                        .setWeight(FONT_WEIGHT_BOLD)
                        .build()
                )
                .setMaxLines(1)
                .build()
        )

        if (usage.fiveHourPct >= 0) {
            val row = Row.Builder()
                .setVerticalAlignment(VERTICAL_ALIGN_CENTER)

            row.addContent(coloredLabel("5h:${usage.fiveHourPct.toInt()}%", pctToArgb(usage.fiveHourPct), 13f))
            row.addContent(
                Text.Builder()
                    .setText("  ")
                    .setFontStyle(FontStyle.Builder().setSize(sp(13f)).build())
                    .build()
            )
            row.addContent(coloredLabel("7d:${usage.sevenDayPct.toInt()}%", pctToArgb(usage.sevenDayPct), 13f))

            col.addContent(row.build())

            // Reset times
            val now = System.currentTimeMillis()
            val resetRow = Row.Builder()
                .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
            resetRow.addContent(dimLabel("↻${formatResetTime(usage.fiveHourResetsAtMs, now)}", 10f))
            resetRow.addContent(
                Text.Builder()
                    .setText("  ")
                    .setFontStyle(FontStyle.Builder().setSize(sp(10f)).build())
                    .build()
            )
            resetRow.addContent(dimLabel("↻${formatResetTime(usage.sevenDayResetsAtMs, now)}", 10f))
            col.addContent(resetRow.build())
        } else if (usage.hasError) {
            col.addContent(
                Text.Builder()
                    .setText(usage.errorMsg ?: "Error")
                    .setFontStyle(
                        FontStyle.Builder()
                            .setSize(sp(11f))
                            .setColor(argb(Color.parseColor("#E53935")))
                            .build()
                    )
                    .setMaxLines(1)
                    .build()
            )
        }

        return col.build()
    }

    private fun coloredLabel(text: String, color: Int, sizeSp: Float): LayoutElement =
        Text.Builder()
            .setText(text)
            .setFontStyle(
                FontStyle.Builder()
                    .setSize(sp(sizeSp))
                    .setColor(argb(color))
                    .setWeight(FONT_WEIGHT_BOLD)
                    .build()
            )
            .build()

    private fun dimLabel(text: String, sizeSp: Float): LayoutElement =
        Text.Builder()
            .setText(text)
            .setFontStyle(
                FontStyle.Builder()
                    .setSize(sp(sizeSp))
                    .setColor(argb(Color.parseColor("#888888")))
                    .build()
            )
            .build()

    private fun spacer(heightDp: Float): LayoutElement =
        Spacer.Builder().setHeight(dp(heightDp)).build()

    private fun pctToArgb(pct: Float): Int {
        val clamped = pct.coerceIn(0f, 100f)
        return when {
            clamped <= 40f -> Color.parseColor("#4CAF50")
            clamped <= 70f -> Color.parseColor("#FFCA28")
            clamped <= 85f -> Color.parseColor("#FF9800")
            else -> Color.parseColor("#E53935")
        }
    }

    private fun formatResetTime(resetMs: Long, nowMs: Long): String {
        if (resetMs <= 0) return "?"
        val diff = resetMs - nowMs
        if (diff <= 0) return "now"
        val totalSec = diff / 1000
        val days = totalSec / 86400
        val hours = (totalSec % 86400) / 3600
        val mins = (totalSec % 3600) / 60
        return when {
            days > 0 -> "${days}d${hours}h"
            hours > 0 -> "${hours}h${mins}m"
            else -> "${mins}m"
        }
    }
}
