package com.vivaanenterprise.app.app.navigation

import androidx.navigation.NavController

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Dashboard : Screen("dashboard")
    data object ClientList : Screen("client_list")
    data object AddClient : Screen("client_add")
    data class ClientDetail(val clientId: String) : Screen("client_detail/$clientId") {
        companion object {
            const val ROUTE_PATTERN = "client_detail/{clientId}"
        }
    }
    data class EditClient(val clientId: String) : Screen("client_edit/$clientId") {
        companion object {
            const val ROUTE_PATTERN = "client_edit/{clientId}"
        }
    }
    data object ProductList : Screen("product_list")
    data object AddProduct : Screen("product_add")
    data class ProductDetail(val productId: String) : Screen("product_detail/$productId") {
        companion object {
            const val ROUTE_PATTERN = "product_detail/{productId}"
        }
    }
    data class EditProduct(val productId: String) : Screen("product_edit/$productId") {
        companion object {
            const val ROUTE_PATTERN = "product_edit/{productId}"
        }
    }
    data object AddInvoice : Screen("invoice_add")
    data class EditInvoice(val documentId: String) : Screen("invoice_edit/$documentId") {
        companion object {
            const val ROUTE_PATTERN = "invoice_edit/{documentId}"
        }
    }
    data object AddPurchaseOrder : Screen("po_add")
    data class EditPurchaseOrder(val documentId: String) : Screen("po_edit/$documentId") {
        companion object {
            const val ROUTE_PATTERN = "po_edit/{documentId}"
        }
    }
    data object Documents : Screen("documents")
    data class DocumentDetail(val documentId: String) : Screen("document_detail/$documentId") {
        companion object {
            const val ROUTE_PATTERN = "document_detail/{documentId}"
        }
    }
    data class PdfViewer(val documentId: String) : Screen("pdf_viewer/$documentId") {
        companion object {
            const val ROUTE_PATTERN = "pdf_viewer/{documentId}"
        }
    }
}

fun NavController.navigateToAddPurchaseOrder() {
    navigate(Screen.AddPurchaseOrder.route)
}

fun NavController.navigateToEditPurchaseOrder(documentId: String) {
    navigate(Screen.EditPurchaseOrder(documentId).route)
}

fun NavController.navigateToAddInvoice() {
    navigate(Screen.AddInvoice.route)
}

fun NavController.navigateToEditInvoice(documentId: String) {
    navigate(Screen.EditInvoice(documentId).route)
}

fun NavController.navigateToDashboard() {
    navigate(Screen.Dashboard.route) {
        popUpTo(0) { inclusive = true }
        launchSingleTop = true
    }
}

fun NavController.navigateToLogin() {
    navigate(Screen.Login.route) {
        popUpTo(0) { inclusive = true }
        launchSingleTop = true
    }
}

fun NavController.navigateToClientList() {
    navigate(Screen.ClientList.route)
}

fun NavController.navigateToAddClient() {
    navigate(Screen.AddClient.route)
}

fun NavController.navigateToClientDetail(clientId: String) {
    navigate(Screen.ClientDetail(clientId).route)
}

fun NavController.navigateToEditClient(clientId: String) {
    navigate(Screen.EditClient(clientId).route)
}

fun NavController.navigateToProductList() {
    navigate(Screen.ProductList.route)
}

fun NavController.navigateToAddProduct() {
    navigate(Screen.AddProduct.route)
}

fun NavController.navigateToProductDetail(productId: String) {
    navigate(Screen.ProductDetail(productId).route)
}

fun NavController.navigateToEditProduct(productId: String) {
    navigate(Screen.EditProduct(productId).route)
}

fun NavController.navigateToDocuments() {
    navigate(Screen.Documents.route)
}

fun NavController.navigateToDocumentDetail(documentId: String) {
    navigate(Screen.DocumentDetail(documentId).route)
}

fun NavController.navigateToPdfViewerFromHistory(documentId: String) {
    navigate(Screen.PdfViewer(documentId).route)
}

fun NavController.navigateToPdfViewer(documentId: String) {
    val currentRoute = currentDestination?.route
    navigate(Screen.PdfViewer(documentId).route) {
        if (currentRoute != null && currentRoute != Screen.Dashboard.route) {
            popUpTo(currentRoute) { inclusive = true }
        } else {
            popUpTo(Screen.Dashboard.route) { inclusive = false }
        }
    }
}
