package io.github.vrcmteam.vrcm.network.api.files

import kotlin.test.Test
import kotlin.test.assertEquals

class FileApiUrlTest {
    @Test
    fun imageUrlUsesTheRequestedFileVersion() {
        assertEquals(
            "https://api.vrchat.cloud/api/1/image/file_gallery/4/256",
            FileApi.imageUrl(
                fileId = "file_gallery",
                fileVersion = 4,
                fileSize = 256,
            ),
        )
    }

    @Test
    fun legacyImageUrlConversionKeepsTheVersionFromTheSourceUrl() {
        assertEquals(
            "https://api.vrchat.cloud/api/1/image/file_gallery/4/256",
            FileApi.convertFileUrl(
                fileUrl = "https://api.vrchat.cloud/api/1/file/file_gallery/4/file",
                fileSize = 256,
            ),
        )
    }
}
