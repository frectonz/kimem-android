package et.frectonz.kimem.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import et.frectonz.kimem.core.data.NoticeCenter
import et.frectonz.kimem.ui.components.MonoSnackbar
import et.frectonz.kimem.ui.components.paper
import et.frectonz.kimem.ui.screens.MenuScreen
import et.frectonz.kimem.ui.screens.ReportScreen
import et.frectonz.kimem.ui.screens.SendSmsScreen
import et.frectonz.kimem.ui.screens.SettingsScreen
import et.frectonz.kimem.ui.screens.SmsInboxScreen
import et.frectonz.kimem.ui.screens.SmsMessageScreen
import et.frectonz.kimem.ui.screens.SmsSelectScreen
import et.frectonz.kimem.ui.screens.SmsShowScreen
import et.frectonz.kimem.ui.screens.UssdScreen

@Composable
fun KimemApp(notices: NoticeCenter) {
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(notices) {
        notices.notices.collect { snackbar.showSnackbar(it.resolve(context)) }
    }

    val navigate: (Route) -> Unit = { navController.navigate(it) }
    val back: () -> Unit = { navController.popBackStack() }

    Box(
        Modifier
            .fillMaxSize()
            .background(paper())
    ) {
        NavHost(navController = navController, startDestination = Route.Menu()) {
            composable<Route.Menu> { MenuScreen(onNavigate = navigate, onBack = back) }
            composable<Route.Settings> { SettingsScreen(onBack = back) }
            composable<Route.Report> { ReportScreen(onBack = back) }
            composable<Route.SmsInbox> { SmsInboxScreen(onNavigate = navigate, onBack = back) }
            composable<Route.SmsShow> { SmsShowScreen(onNavigate = navigate, onBack = back) }
            composable<Route.SmsMessage> { SmsMessageScreen(onNavigate = navigate, onBack = back) }
            composable<Route.SendSms> { SendSmsScreen(onBack = back) }
            composable<Route.SmsSelect> { SmsSelectScreen(onBack = back) }
            composable<Route.Ussd> { UssdScreen(onBack = back) }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp),
            snackbar = { MonoSnackbar(it) },
        )
    }
}
