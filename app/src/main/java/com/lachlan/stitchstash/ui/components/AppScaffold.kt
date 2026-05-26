package com.lachlan.stitchstash.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

sealed interface NavIcon {
    data object Drawer : NavIcon
    data object Back : NavIcon
    data object None : NavIcon
}

/**
 * Top-level scaffold for primary screens. Wraps content in a ModalNavigationDrawer.
 * Use [DetailScaffold] for sub-screens with a back arrow.
 */
@Composable
fun DrawerScaffold(
    title: String,
    currentRoute: String,
    onNavigateTopLevel: (TopLevelDestination) -> Unit,
    actions: @Composable () -> Unit = {},
    decorative: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                currentRoute = currentRoute,
                onNavigate = onNavigateTopLevel,
                onClose = { scope.launch { drawerState.close() } },
            )
        },
    ) {
        ScreenChrome(
            title = title,
            navIcon = NavIcon.Drawer,
            onNavClick = { scope.launch { drawerState.open() } },
            actions = actions,
            decorative = decorative,
            content = content,
        )
    }
}

/**
 * Secondary screens — back arrow, no drawer.
 */
@Composable
fun DetailScaffold(
    title: String,
    onBack: () -> Unit,
    actions: @Composable () -> Unit = {},
    decorative: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    ScreenChrome(
        title = title,
        navIcon = NavIcon.Back,
        onNavClick = onBack,
        actions = actions,
        decorative = decorative,
        content = content,
    )
}

@Composable
private fun ScreenChrome(
    title: String,
    navIcon: NavIcon,
    onNavClick: () -> Unit,
    actions: @Composable () -> Unit,
    decorative: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    val systemBars = WindowInsets.systemBars.asPaddingValues()

    Box(modifier = Modifier.fillMaxSize()) {
        if (decorative) {
            DecorativeBackground(modifier = Modifier.fillMaxSize())
        } else {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {}
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    PaddingValues(
                        top = systemBars.calculateTopPadding() + 8.dp,
                        bottom = systemBars.calculateBottomPadding() + 12.dp,
                        start = 20.dp,
                        end = 20.dp,
                    ),
                ),
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when (navIcon) {
                    NavIcon.Drawer -> NavCircleButton(onClick = onNavClick) {
                        Icon(
                            Icons.Outlined.Menu,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    NavIcon.Back -> NavCircleButton(onClick = onNavClick) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    NavIcon.None -> Spacer(Modifier.width(44.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = FontFamily.Serif,
                    modifier = Modifier.weight(1f),
                )
                actions()
            }

            content()
        }
    }
}

@Composable
private fun NavCircleButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(44.dp),
    ) {
        IconButton(onClick = onClick) { content() }
    }
}
