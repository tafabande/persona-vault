package com.pims.vault.document

import android.graphics.Bitmap
import com.pims.vault.presentation.document.DocumentPreviewLifecycle
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*

class DocumentPreviewLifecycleTest {
    @Test fun unpublishedBitmapIsRecycledWhenThePreviewCloses() {
        val bitmap: Bitmap = mock()
        val lifecycle = DocumentPreviewLifecycle()
        assertTrue(lifecycle.accept(bitmap))
        lifecycle.close()
        verify(bitmap).recycle()
    }

    @Test fun lateDecoderResultCannotOutliveThePreview() {
        val bitmap: Bitmap = mock()
        val lifecycle = DocumentPreviewLifecycle()
        lifecycle.close()
        assertFalse(lifecycle.accept(bitmap))
        verify(bitmap).recycle()
    }

    @Test fun composedBitmapLivesUntilItsImageIsRemoved() {
        val bitmap: Bitmap = mock()
        val lifecycle = DocumentPreviewLifecycle()
        lifecycle.accept(bitmap)
        lifecycle.displayed(bitmap)
        lifecycle.close()
        verify(bitmap, never()).recycle()
        lifecycle.disposed(bitmap)
        verify(bitmap).recycle()
    }
}
