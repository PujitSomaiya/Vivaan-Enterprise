package com.vivaanenterprise.app.core.pdf

import com.vivaanenterprise.app.domain.pdf.BusinessDocumentPdfGenerator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PdfModule {

    @Binds
    @Singleton
    abstract fun bindBusinessDocumentPdfGenerator(
        impl: AndroidBusinessDocumentPdfGenerator
    ): BusinessDocumentPdfGenerator
}
