package net.igng.mcstatus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.igng.mcstatus.data.AuthRepository
import net.igng.mcstatus.data.CustomerTicketRepository
import net.igng.mcstatus.data.InfoHallRepository
import net.igng.mcstatus.data.McManagementRepository
import net.igng.mcstatus.data.StatusRepository
import net.igng.mcstatus.data.SettingsRepository
import net.igng.mcstatus.data.ChatRepository
import net.igng.mcstatus.ui.StatusApp
import net.igng.mcstatus.ui.SettingsViewModel
import net.igng.mcstatus.ui.SettingsViewModelFactory
import net.igng.mcstatus.ui.theme.IGNGMcStatusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = StatusRepository(baseUrl = BuildConfig.MC_STATUS_BASE_URL)
        val settingsRepository = SettingsRepository(this)
        val authRepository = AuthRepository(BuildConfig.IGNG_SSO_BASE_URL)
        val ticketRepository = CustomerTicketRepository(BuildConfig.MC_STATUS_BASE_URL)
        val chatRepository = ChatRepository(BuildConfig.MC_STATUS_BASE_URL)
        val infoHallRepository = InfoHallRepository(BuildConfig.MC_STATUS_BASE_URL)
        val mcManagementRepository = McManagementRepository(BuildConfig.MC_STATUS_BASE_URL)

        setContent {
            val settingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel<SettingsViewModel>(
                factory = SettingsViewModelFactory(settingsRepository, authRepository)
            )
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
            val loginState by settingsViewModel.loginState.collectAsStateWithLifecycle()

            LaunchedEffect(settings.sessionToken) {
                settingsViewModel.restoreSavedSession(settings.sessionToken)
            }

            IGNGMcStatusTheme(settings = settings) {
                StatusApp(
                    repository = repository,
                    infoHallRepository = infoHallRepository,
                    ticketRepository = ticketRepository,
                    chatRepository = chatRepository,
                    mcManagementRepository = mcManagementRepository,
                    settings = settings,
                    onSetVibrationEnabled = settingsViewModel::setVibrationEnabled,
                    onSetUseSystemAccent = settingsViewModel::setUseSystemAccent,
                    onSetAccent = settingsViewModel::setAccent,
                    onLogin = settingsViewModel::login,
                    onLogout = settingsViewModel::logout,
                    onSwitchAccount = settingsViewModel::switchAccount,
                    loginState = loginState.copy(accountName = settings.accountName),
                )
            }
        }
    }
}
