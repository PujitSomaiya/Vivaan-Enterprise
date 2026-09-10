package com.vivaanenterprise.app.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.vivaanenterprise.app.core.designsystem.theme.AppTheme
import com.vivaanenterprise.app.domain.model.AuthState
import com.vivaanenterprise.app.domain.repository.AuthRepository
import com.vivaanenterprise.app.feature.auth.presentation.LoginRoute
import com.vivaanenterprise.app.feature.client.presentation.detail.ClientDetailRoute
import com.vivaanenterprise.app.feature.client.presentation.form.ClientFormRoute
import com.vivaanenterprise.app.feature.client.presentation.list.ClientListRoute
import com.vivaanenterprise.app.feature.dashboard.presentation.DashboardRoute
import com.vivaanenterprise.app.feature.product.presentation.detail.ProductDetailRoute
import com.vivaanenterprise.app.feature.product.presentation.form.ProductFormRoute
import com.vivaanenterprise.app.feature.product.presentation.list.ProductListRoute

@Composable
fun AppNavHost(
    authStateFlow: StateFlow<AuthState>,
    authRepository: AuthRepository,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val authState by authStateFlow.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(authState) {
        when (authState) {
            AuthState.Loading -> {
                // Auth state loading
            }
            AuthState.SignedOut -> {
                val currentRoute = navController.currentDestination?.route
                if (currentRoute != Screen.Login.route) {
                    navController.navigateToLogin()
                }
            }
            is AuthState.SignedIn -> {
                val currentRoute = navController.currentDestination?.route
                if (currentRoute == null || currentRoute == Screen.Login.route) {
                    navController.navigateToDashboard()
                }
            }
        }
    }

    when (authState) {
        AuthState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AppTheme.colorScheme.primary)
            }
        }
        else -> {
            val startDestination = if (authState is AuthState.SignedIn) {
                Screen.Dashboard.route
            } else {
                Screen.Login.route
            }

            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = modifier
            ) {
                composable(Screen.Login.route) {
                    LoginRoute()
                }

                composable(Screen.Dashboard.route) {
                    DashboardRoute(
                        onSignOutClick = {
                            coroutineScope.launch {
                                authRepository.signOut()
                            }
                        },
                        onNavigateToClients = {
                            navController.navigateToClientList()
                        },
                        onNavigateToProducts = {
                            navController.navigateToProductList()
                        },
                        onNavigateToNewInvoice = {
                            navController.navigateToAddInvoice()
                        },
                        onNavigateToNewPurchaseOrder = {
                            navController.navigateToAddPurchaseOrder()
                        },
                        onNavigateToDocuments = {
                            navController.navigateToDocuments()
                        }
                    )
                }

                composable(Screen.Documents.route) {
                    com.vivaanenterprise.app.feature.document.history.list.DocumentsRoute(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToDetail = { docId -> navController.navigateToDocumentDetail(docId) },
                        onNavigateToNewInvoice = { navController.navigateToAddInvoice() },
                        onNavigateToNewPurchaseOrder = { navController.navigateToAddPurchaseOrder() }
                    )
                }

                composable(
                    route = Screen.DocumentDetail.ROUTE_PATTERN,
                    arguments = listOf(navArgument("documentId") { type = NavType.StringType })
                ) {
                    com.vivaanenterprise.app.feature.document.history.detail.DocumentDetailRoute(
                        onNavigateBack = { navController.popBackStack() },
                        onEditDraft = { docType, docId ->
                            if (docType == com.vivaanenterprise.app.core.common.DocumentType.TAX_INVOICE) {
                                navController.navigateToEditInvoice(docId)
                            } else {
                                navController.navigateToEditPurchaseOrder(docId)
                            }
                        },
                        onViewPdf = { docId ->
                            navController.navigateToPdfViewerFromHistory(docId)
                        }
                    )
                }

                composable(Screen.AddInvoice.route) {
                    com.vivaanenterprise.app.feature.invoice.presentation.InvoiceRoute(
                        onNavigateBack = { navController.popBackStack() },
                        onSaveSuccess = { docId -> navController.navigateToPdfViewer(docId) }
                    )
                }

                composable(
                    route = Screen.EditInvoice.ROUTE_PATTERN,
                    arguments = listOf(navArgument("documentId") { type = NavType.StringType })
                ) {
                    com.vivaanenterprise.app.feature.invoice.presentation.InvoiceRoute(
                        onNavigateBack = { navController.popBackStack() },
                        onSaveSuccess = { docId -> navController.navigateToPdfViewer(docId) }
                    )
                }

                composable(Screen.AddPurchaseOrder.route) {
                    com.vivaanenterprise.app.feature.purchaseorder.presentation.PurchaseOrderRoute(
                        onNavigateBack = { navController.popBackStack() },
                        onSaveSuccess = { docId -> navController.navigateToPdfViewer(docId) }
                    )
                }

                composable(
                    route = Screen.EditPurchaseOrder.ROUTE_PATTERN,
                    arguments = listOf(navArgument("documentId") { type = NavType.StringType })
                ) {
                    com.vivaanenterprise.app.feature.purchaseorder.presentation.PurchaseOrderRoute(
                        onNavigateBack = { navController.popBackStack() },
                        onSaveSuccess = { docId -> navController.navigateToPdfViewer(docId) }
                    )
                }

                composable(
                    route = Screen.PdfViewer.ROUTE_PATTERN,
                    arguments = listOf(navArgument("documentId") { type = NavType.StringType })
                ) {
                    com.vivaanenterprise.app.feature.pdfviewer.presentation.PdfViewerRoute(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.ClientList.route) {
                    ClientListRoute(
                        onNavigateToDetail = { clientId ->
                            navController.navigateToClientDetail(clientId)
                        },
                        onNavigateToAddClient = {
                            navController.navigateToAddClient()
                        }
                    )
                }

                composable(Screen.AddClient.route) {
                    ClientFormRoute(
                        onNavigateBack = { navController.popBackStack() },
                        onSaveSuccess = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.ClientDetail.ROUTE_PATTERN,
                    arguments = listOf(navArgument("clientId") { type = NavType.StringType })
                ) {
                    ClientDetailRoute(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToEdit = { clientId ->
                            navController.navigateToEditClient(clientId)
                        },
                        onNavigateToAccount = { clientId ->
                            navController.navigateToClientAccount(clientId)
                        }
                    )
                }

                composable(
                    route = Screen.ClientAccount.ROUTE_PATTERN,
                    arguments = listOf(navArgument("clientId") { type = NavType.StringType })
                ) {
                    com.vivaanenterprise.app.feature.account.presentation.ClientAccountRoute(
                        onNavigateBack = { navController.popBackStack() },
                        onOpenDocumentDetail = { docId ->
                            navController.navigateToDocumentDetail(docId)
                        }
                    )
                }

                composable(
                    route = Screen.EditClient.ROUTE_PATTERN,
                    arguments = listOf(navArgument("clientId") { type = NavType.StringType })
                ) {
                    ClientFormRoute(
                        onNavigateBack = { navController.popBackStack() },
                        onSaveSuccess = { navController.popBackStack() }
                    )
                }

                composable(Screen.ProductList.route) {
                    ProductListRoute(
                        onNavigateToDetail = { productId ->
                            navController.navigateToProductDetail(productId)
                        },
                        onNavigateToAddProduct = {
                            navController.navigateToAddProduct()
                        },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.AddProduct.route) {
                    ProductFormRoute(
                        onNavigateBack = { navController.popBackStack() },
                        onSaveSuccess = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.ProductDetail.ROUTE_PATTERN,
                    arguments = listOf(navArgument("productId") { type = NavType.StringType })
                ) {
                    ProductDetailRoute(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToEdit = { productId ->
                            navController.navigateToEditProduct(productId)
                        }
                    )
                }

                composable(
                    route = Screen.EditProduct.ROUTE_PATTERN,
                    arguments = listOf(navArgument("productId") { type = NavType.StringType })
                ) {
                    ProductFormRoute(
                        onNavigateBack = { navController.popBackStack() },
                        onSaveSuccess = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
