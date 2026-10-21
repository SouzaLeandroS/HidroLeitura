package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.ScreenTab
import com.example.ui.screens.ApartmentsScreen
import com.example.ui.screens.ReadingScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
                val snackBarMessage by viewModel.snackBarMessage.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(snackBarMessage) {
                    snackBarMessage?.let {
                        snackbarHostState.showSnackbar(it)
                        viewModel.clearSnackBar()
                    }
                }

                // Handle back button: return to Leitura tab if currently in another tab
                BackHandler(enabled = currentTab != ScreenTab.LEITURA) {
                    viewModel.selectTab(ScreenTab.LEITURA)
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WaterDrop,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "HidroLeitura",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp,
                            modifier = Modifier.testTag("bottom_nav_bar")
                        ) {
                            NavigationBarItem(
                                selected = currentTab == ScreenTab.LEITURA,
                                onClick = { viewModel.selectTab(ScreenTab.LEITURA) },
                                icon = {
                                    Icon(Icons.Default.CameraAlt, contentDescription = "Leitura")
                                },
                                label = { Text("Leitura") },
                                modifier = Modifier.testTag("nav_item_reading")
                            )

                            NavigationBarItem(
                                selected = currentTab == ScreenTab.APARTAMENTOS,
                                onClick = { viewModel.selectTab(ScreenTab.APARTAMENTOS) },
                                icon = {
                                    Icon(Icons.Default.Apartment, contentDescription = "Unidades")
                                },
                                label = { Text("Unidades") },
                                modifier = Modifier.testTag("nav_item_apartments")
                            )

                            NavigationBarItem(
                                selected = currentTab == ScreenTab.RELATORIOS,
                                onClick = { viewModel.selectTab(ScreenTab.RELATORIOS) },
                                icon = {
                                    Icon(Icons.Default.BarChart, contentDescription = "Relatórios")
                                },
                                label = { Text("Relatórios") },
                                modifier = Modifier.testTag("nav_item_reports")
                            )

                            NavigationBarItem(
                                selected = currentTab == ScreenTab.CONFIGURACOES,
                                onClick = { viewModel.selectTab(ScreenTab.CONFIGURACOES) },
                                icon = {
                                    Icon(Icons.Default.Settings, contentDescription = "Ajustes")
                                },
                                label = { Text("Ajustes") },
                                modifier = Modifier.testTag("nav_item_settings")
                            )
                        }
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentTab) {
                            ScreenTab.LEITURA -> ReadingScreen(viewModel = viewModel)
                            ScreenTab.APARTAMENTOS -> ApartmentsScreen(viewModel = viewModel)
                            ScreenTab.RELATORIOS -> ReportsScreen(viewModel = viewModel)
                            ScreenTab.CONFIGURACOES -> SettingsScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
