package com.portfolio.iptvplayer.domain.usecase

import com.portfolio.iptvplayer.domain.model.Channel
import com.portfolio.iptvplayer.domain.repository.PlaylistRepository
import javax.inject.Inject

/**
 * Each use case does exactly one thing and depends only on the repository
 * interface — ViewModels never see PlaylistRepositoryImpl, SharedPreferences,
 * or any data-layer type. This is what makes them trivially unit-testable
 * with a fake repository and no Android test runner.
 */
class GetChannelsUseCase @Inject constructor(
    private val repository: PlaylistRepository
) {
    suspend operator fun invoke(): Result<List<Channel>> = repository.loadPlaylist()
}

class GetSavedPlaylistUrlUseCase @Inject constructor(
    private val repository: PlaylistRepository
) {
    operator fun invoke(): String? = repository.getSavedPlaylistUrl()
}

class SaveCustomPlaylistUrlUseCase @Inject constructor(
    private val repository: PlaylistRepository
) {
    operator fun invoke(url: String) = repository.saveCustomPlaylistUrl(url)
}

class ClearCustomPlaylistUrlUseCase @Inject constructor(
    private val repository: PlaylistRepository
) {
    operator fun invoke() = repository.clearCustomPlaylistUrl()
}

class ImportPlaylistFromLocalFileUseCase @Inject constructor(
    private val repository: PlaylistRepository
) {
    suspend operator fun invoke(uriString: String): Result<List<Channel>> =
        repository.importPlaylistFromLocalFile(uriString)
}

class ImportPlaylistFromAppFolderUseCase @Inject constructor(
    private val repository: PlaylistRepository
) {
    suspend operator fun invoke(): Result<List<Channel>> = repository.importPlaylistFromAppFolder()
}
