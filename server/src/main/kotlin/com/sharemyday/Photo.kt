package com.sharemyday

import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

sealed interface PhotoService {
    fun saveDayPhoto(userId: String, dayId: String, photoId: String, photo: ByteArray)
    fun getDayPhoto(userId: String, dayId: String, photoId: String): ByteArray
}

class LocalPhotoService(private val basePath: String): PhotoService {
    override fun saveDayPhoto(userId: String, dayId: String, photoId: String, photo: ByteArray) {
        val photoDirectory = FileSystems.getDefault().getPath(basePath, userId, dayId)
        Files.createDirectories(photoDirectory)
        val newPhotoPath = Paths.get(photoDirectory.toString(), photoId);
        Files.write(newPhotoPath, photo, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
    }

    override fun getDayPhoto(userId: String, dayId: String, photoId: String): ByteArray {
        val photoDirectory = FileSystems.getDefault().getPath(basePath, userId, dayId, photoId)
        return Files.readAllBytes(photoDirectory)
    }
}