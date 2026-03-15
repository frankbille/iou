@file:Suppress("FunctionName")

package dk.frankbille.iou.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun HouseholdDashboard(
    state: DashboardState,
    topContent: (@Composable () -> Unit)? = null,
) {
    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    brush =
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFFF2E6D4), Color(0xFFF8F2E8), Color(0xFFE7E8E1)),
                        ),
                ),
    ) {
        val compact = maxWidth < 860.dp

        BackgroundGlow(
            alignment = Alignment.TopEnd,
            size = 320.dp,
            brush = Brush.radialGradient(listOf(ClaySoft.copy(alpha = 0.65f), Color.Transparent)),
            xOffset = 80.dp,
            yOffset = (-120).dp,
        )
        BackgroundGlow(
            alignment = Alignment.CenterStart,
            size = 280.dp,
            brush = Brush.radialGradient(listOf(PineSoft.copy(alpha = 0.55f), Color.Transparent)),
            xOffset = (-120).dp,
            yOffset = 40.dp,
        )

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .safeContentPadding()
                    .padding(horizontal = if (compact) 16.dp else 28.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .widthIn(max = 1120.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                topContent?.invoke()
                HeaderBar(state = state, compact = compact)
                HeroCard(state = state, compact = compact)

                // Stack cards on narrow screens; split into editorial and operational columns when space allows.
                if (compact) {
                    ChildrenSection(state = state)
                    TasksSection(state = state)
                    AccountsSection(state = state)
                    ActivitySection(state = state)
                    RulesSection(state = state)
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(
                            modifier = Modifier.weight(1.12f),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            ChildrenSection(state = state)
                            TasksSection(state = state)
                        }
                        Column(
                            modifier = Modifier.weight(0.88f),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            AccountsSection(state = state)
                            ActivitySection(state = state)
                            RulesSection(state = state)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.BackgroundGlow(
    alignment: Alignment,
    size: Dp,
    brush: Brush,
    xOffset: Dp,
    yOffset: Dp,
) {
    Box(
        modifier =
            Modifier
                .align(alignment)
                .offset(x = xOffset, y = yOffset)
                .size(size)
                .clip(CircleShape)
                .background(brush = brush),
    )
}

@Composable
private fun HeaderBar(
    state: DashboardState,
    compact: Boolean,
) {
    if (compact) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Pill(
                text = "Household dashboard",
                background = Color.White.copy(alpha = 0.72f),
                textColor = Pine,
            )
            Text(text = "IOU for ${state.familyName}", style = MaterialTheme.typography.headlineLarge)
            Text(
                text = state.houseNote,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill(
                    text = "Household dashboard",
                    background = Color.White.copy(alpha = 0.72f),
                    textColor = Pine,
                )
                Text(text = "IOU for ${state.familyName}", style = MaterialTheme.typography.headlineLarge)
            }
            Text(
                text = state.houseNote,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HeroCard(
    state: DashboardState,
    compact: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.55f)),
    ) {
        Box(
            modifier =
                Modifier
                    .background(
                        brush =
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFF4E7D5), Color(0xFFF8F2E8), Color(0xFFE6EEE8)),
                            ),
                    ).padding(24.dp),
        ) {
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    HeroCopy(state = state)
                    HeroMetrics(state = state)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1.12f),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        HeroCopy(state = state)
                    }
                    HeroMetrics(
                        state = state,
                        modifier = Modifier.weight(0.88f),
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroCopy(state: DashboardState) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "Allowance with a ledger, not sticky notes.",
            style = MaterialTheme.typography.displayMedium,
        )
        Text(
            text =
                "IOU should feel calm for adults and legible for kids: clear rules, named money homes, " +
                    "and chores that turn into balances without guesswork.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(
                text = "${state.children.size} kids see live balances",
                background = PineSoft,
                textColor = Pine,
            )
            Pill(
                text = "${state.accounts.size} named money homes",
                background = GoldSoft,
                textColor = Gold,
            )
        }
    }
}

@Composable
private fun HeroMetrics(
    state: DashboardState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricCard(
            title = "Tracked balance",
            value = state.formatMoney(state.totalTrackedMinor()),
            accent = Pine,
        )
        MetricCard(
            title = "Waiting approval",
            value = "${state.pendingApprovals()} chores",
            accent = Clay,
        )
        MetricCard(
            title = "Rewards queued",
            value = state.formatMoney(state.totalScheduledRewardsMinor()),
            accent = Gold,
        )
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    accent: Color,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.18f)),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = value, style = MaterialTheme.typography.titleLarge, color = accent)
        }
    }
}

@Composable
private fun ChildrenSection(state: DashboardState) {
    SectionCard(
        title = "Children and balances",
        subtitle = "Each child gets a distinct color, a current balance, and a visible savings signal.",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.children.forEach { child ->
                ChildCard(child = child, state = state)
            }
        }
    }
}

@Composable
private fun ChildCard(
    child: ChildSnapshot,
    state: DashboardState,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, child.accent.copy(alpha = 0.18f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = child.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = child.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Pill(
                    text = child.badgeLabel,
                    background = child.accent.copy(alpha = 0.12f),
                    textColor = child.accent,
                )
            }

            Text(
                text = state.formatMoney(child.balanceMinor),
                style = MaterialTheme.typography.headlineLarge,
                color = child.accent,
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Saved away",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = state.formatMoney(child.savedMinor),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                ProgressTrack(
                    progress = child.savedShare(),
                    accent = child.accent,
                )
            }

            Text(
                text = "${child.pendingTasks} tasks can change this balance this week.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TasksSection(state: DashboardState) {
    SectionCard(
        title = "Tasks shaping the week",
        subtitle = "Mix clear rewards with an approval rhythm that still feels human.",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.tasks.forEach { task ->
                TaskCard(task = task, state = state)
            }
        }
    }
}

@Composable
private fun TaskCard(
    task: TaskSnapshot,
    state: DashboardState,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, task.accent.copy(alpha = 0.16f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${task.childName}  ${task.timingLabel}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Pill(
                    text = state.formatMoney(task.rewardMinor),
                    background = task.accent.copy(alpha = 0.12f),
                    textColor = task.accent,
                )
            }

            Pill(
                text = task.status.label,
                background = task.status.badgeColor,
                textColor = task.status.textColor,
            )
        }
    }
}

@Composable
private fun AccountsSection(state: DashboardState) {
    SectionCard(
        title = "Where the money lives",
        subtitle = "Named accounts make the household feel concrete: reserve, jars, and quick payout cash.",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            state.accounts.forEach { account ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(text = account.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = account.note,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = state.formatMoney(account.amountMinor),
                            style = MaterialTheme.typography.titleMedium,
                            color = account.accent,
                        )
                    }
                    ProgressTrack(progress = account.fillRatio, accent = account.accent)
                }
            }
        }
    }
}

@Composable
private fun ActivitySection(state: DashboardState) {
    SectionCard(
        title = "Recent movement",
        subtitle = "A calm timeline keeps approvals, payouts, and transfers understandable.",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.activity.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .padding(top = 4.dp)
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(item.accent),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(text = item.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = item.detail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = item.amountLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = item.accent,
                    )
                }
            }
        }
    }
}

@Composable
private fun RulesSection(state: DashboardState) {
    SectionCard(
        title = "House rules",
        subtitle = "The product should feel friendly, but the operating model needs to stay explicit.",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.rules.forEach { rule ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = rule.accent.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, rule.accent.copy(alpha = 0.12f)),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(text = rule.title, style = MaterialTheme.typography.titleMedium, color = rule.accent)
                        Text(
                            text = rule.body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.58f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}

@Composable
private fun ProgressTrack(
    progress: Float,
    accent: Color,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.14f)),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .clip(CircleShape)
                    .background(
                        brush =
                            Brush.horizontalGradient(
                                colors = listOf(accent.copy(alpha = 0.72f), accent),
                            ),
                    ),
        )
    }
}

@Composable
private fun Pill(
    text: String,
    background: Color,
    textColor: Color,
) {
    Box(
        modifier =
            Modifier
                .clip(CircleShape)
                .background(background)
                .border(BorderStroke(1.dp, textColor.copy(alpha = 0.08f)), CircleShape)
                .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = textColor)
    }
}
