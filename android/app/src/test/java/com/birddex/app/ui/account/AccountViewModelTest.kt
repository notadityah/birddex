package com.birddex.app.ui.account

import com.birddex.app.data.account.AccountRepository
import com.birddex.app.data.account.AccountSettings
import com.birddex.app.data.auth.SessionState
import com.birddex.app.data.birds.Bird
import com.birddex.app.data.birds.BirdRepository
import com.birddex.app.data.feedback.FeedbackCategory
import com.birddex.app.data.feedback.FeedbackRepository
import com.birddex.app.data.feedback.FeedbackSent
import com.birddex.app.data.network.ApiError
import com.birddex.app.data.network.ApiResult
import com.birddex.app.data.sightings.Sighting
import com.birddex.app.data.sightings.SightingRepository
import com.birddex.app.ui.auth.FakeAuthRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {

    private class FakeAccountRepository : AccountRepository {
        var stored = false
        var getResult: (() -> ApiResult<AccountSettings>)? = null
        var setResult: ApiResult<AccountSettings>? = null
        var gate: CompletableDeferred<Unit>? = null
        val saved = mutableListOf<Boolean>()
        override suspend fun getSettings() = getResult?.invoke() ?: ApiResult.Success(AccountSettings(stored))
        override suspend fun setGalleryAnonymous(anonymous: Boolean): ApiResult<AccountSettings> {
            saved += anonymous
            gate?.await()
            return setResult ?: ApiResult.Success(AccountSettings(anonymous).also { stored = anonymous })
        }
    }

    private class FakeSightingRepository : SightingRepository {
        val saved = MutableStateFlow<List<Sighting>>(emptyList())
        var deleteAllResult: ApiResult<Int> = ApiResult.Success(3)
        var deleteAllCalls = 0
        override fun observeSightings() = saved
        override suspend fun refresh() = ApiResult.Success(Unit)
        override suspend fun setPublic(id: String, isPublic: Boolean) = ApiResult.Success(Unit)
        override suspend fun delete(id: String) = ApiResult.Success(Unit)
        override suspend fun create(birdId: Int, notes: String?, isPublic: Boolean, photoJpeg: ByteArray) = ApiResult.Success(Unit)
        override suspend fun deleteAll(): ApiResult<Int> {
            deleteAllCalls++
            if (deleteAllResult is ApiResult.Success) saved.value = emptyList()
            return deleteAllResult
        }
    }

    private class FakeBirdRepository : BirdRepository {
        val saved = MutableStateFlow<List<Bird>>(emptyList())
        override fun observeBirds() = saved
        override suspend fun refresh() = ApiResult.Success(Unit)
    }

    private class FakeFeedbackRepository : FeedbackRepository {
        data class Sent(val category: FeedbackCategory, val message: String, val page: String, val photo: ByteArray?)
        val sent = mutableListOf<Sent>()
        var result: ApiResult<FeedbackSent> = ApiResult.Success(FeedbackSent(photoUploaded = true))
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun send(category: FeedbackCategory, message: String, page: String, photoJpeg: ByteArray?): ApiResult<FeedbackSent> {
            sent += Sent(category, message, page, photoJpeg)
            gate?.await()
            return result
        }
    }

    private val auth = FakeAuthRepository().apply { state.value = SessionState.SignedIn(user) }
    private val account = FakeAccountRepository()
    private val sightings = FakeSightingRepository()
    private val birds = FakeBirdRepository()
    private val feedback = FakeFeedbackRepository()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun open() = AccountViewModel(auth, account, sightings, birds, feedback)

    private fun TestScope.messagesOf(vm: AccountViewModel): MutableList<AccountMessage> =
        mutableListOf<AccountMessage>().also { list ->
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.messages.collect { list += it } }
        }

    private fun sighting(id: String, birdId: Int, shared: Boolean) =
        Sighting(id = id, birdId = birdId, slug = "x", birdName = "x", createdAt = "2026-11-20T00:00:00Z", publicValue = shared)

    // --- gallery anonymity ---

    @Test fun loadsTheSettingOnOpen() {
        account.stored = true
        assertEquals(AnonymitySetting(anonymous = true), open().anonymity.value)
    }

    @Test fun toggleFlipsOnceTheServerSavedIt() {
        val vm = open()

        vm.toggleAnonymous()
        assertEquals(listOf(true), account.saved)
        assertEquals(true, vm.anonymity.value.anonymous)

        vm.toggleAnonymous()
        assertEquals(listOf(true, false), account.saved)
        assertEquals(false, vm.anonymity.value.anonymous)
    }

    @Test fun failedToggleLeavesTheSettingAndSaysSo() = runTest {
        account.setResult = ApiResult.Failure(ApiError.Network)
        val vm = open()
        val messages = messagesOf(vm)

        vm.toggleAnonymous()

        assertEquals(AnonymitySetting(anonymous = false), vm.anonymity.value)
        assertEquals(listOf(AccountMessage.SettingFailed), messages)
    }

    @Test fun savingIgnoresAnotherTap() {
        account.gate = CompletableDeferred()
        val vm = open()

        vm.toggleAnonymous()
        assertTrue(vm.anonymity.value.saving)
        vm.toggleAnonymous()
        account.gate!!.complete(Unit)

        assertEquals(1, account.saved.size)
        assertFalse(vm.anonymity.value.saving)
    }

    @Test fun cannotToggleBeforeTheSettingLoaded() {
        account.getResult = { ApiResult.Failure(ApiError.Network) }
        val vm = open()
        assertEquals(AnonymitySetting(loadFailed = true), vm.anonymity.value)

        vm.toggleAnonymous()
        assertTrue(account.saved.isEmpty())

        account.getResult = null
        vm.loadSettings() // "Retry"
        assertEquals(AnonymitySetting(anonymous = false), vm.anonymity.value)
    }

    // --- reset collection ---

    @Test fun resetDeletesEverythingAndSaysSo() = runTest {
        sightings.saved.value = listOf(sighting("a", 1, shared = true))
        val vm = open()
        val messages = messagesOf(vm)

        vm.resetCollection()

        assertEquals(1, sightings.deleteAllCalls)
        assertEquals(listOf(AccountMessage.ResetDone), messages)
        assertEquals(0, vm.stats.value.sightings)
        assertFalse(vm.resetting.value)
    }

    @Test fun failedResetSaysSo() = runTest {
        sightings.deleteAllResult = ApiResult.Failure(ApiError.Http(500, null))
        val vm = open()
        val messages = messagesOf(vm)

        vm.resetCollection()

        assertEquals(listOf(AccountMessage.ResetFailed), messages)
    }

    // --- profile numbers ---

    @Test fun statsCountSpeciesFoundAndSightingsShared() {
        val parrot = Bird(id = 1, name = "Australian King Parrot", scientificName = "Alisterus scapularis", slug = "australian_king_parrot")
        val magpie = Bird(id = 2, name = "Australian Magpie", scientificName = "Gymnorhina tibicen", slug = "australian_magpie")
        birds.saved.value = listOf(parrot, magpie)
        sightings.saved.value = listOf(
            sighting("a", 1, shared = true),
            sighting("b", 1, shared = false),
            sighting("c", 99, shared = true), // a bird no longer in the list
        )

        assertEquals(AccountStats(found = 1, total = 2, shared = 2, sightings = 3), open().stats.value)
    }

    // --- feedback ---

    @Test fun feedbackSendsTheCategoryAndTrimmedMessage() {
        val vm = open()
        vm.openFeedback()

        vm.sendFeedback(FeedbackCategory.Suggestion, "  Dark mode for the website please \n")

        val sent = feedback.sent.single()
        assertEquals(FeedbackCategory.Suggestion, sent.category)
        assertEquals("Dark mode for the website please", sent.message)
        assertEquals("android:/account", sent.page)
        assertNull(sent.photo)
        assertEquals(FeedbackUiState(sent = true), vm.feedback.value)
    }

    @Test fun emptyFeedbackIsNotSent() {
        val vm = open()
        vm.openFeedback()

        vm.sendFeedback(FeedbackCategory.Bug, "   ")

        assertTrue(feedback.sent.isEmpty())
        assertEquals(FeedbackUiState(error = FeedbackError.EmptyMessage), vm.feedback.value)
    }

    @Test fun failedFeedbackSaysWhyAndCanBeSentAgain() {
        feedback.result = ApiResult.Failure(ApiError.Network)
        val vm = open()
        vm.openFeedback()

        vm.sendFeedback(FeedbackCategory.Bug, "Crash when saving")
        assertEquals(FeedbackUiState(error = FeedbackError.Network), vm.feedback.value)

        feedback.result = ApiResult.Failure(ApiError.Http(400, "message is required"))
        vm.sendFeedback(FeedbackCategory.Bug, "Crash when saving")
        assertEquals(FeedbackUiState(error = FeedbackError.Failed), vm.feedback.value)

        feedback.result = ApiResult.Success(FeedbackSent(photoUploaded = true))
        vm.sendFeedback(FeedbackCategory.Bug, "Crash when saving")
        assertEquals(FeedbackUiState(sent = true), vm.feedback.value)
        assertEquals(3, feedback.sent.size)
    }

    @Test fun sendingTwiceSendsOnceAndClosingLetsItFinish() {
        feedback.gate = CompletableDeferred()
        val vm = open()
        vm.openFeedback()

        vm.sendFeedback(FeedbackCategory.Other, "Hello")
        assertEquals(FeedbackUiState(sending = true), vm.feedback.value)
        vm.sendFeedback(FeedbackCategory.Other, "Hello")
        vm.closeFeedback()
        feedback.gate!!.complete(Unit)

        assertEquals(1, feedback.sent.size)
        assertNull(vm.feedback.value) // doesn't pop open again
    }

    @Test fun nothingIsSentWithTheSheetClosed() {
        open().sendFeedback(FeedbackCategory.Bug, "Hello")
        assertTrue(feedback.sent.isEmpty())
    }

    @Test fun signOutStillWorks() {
        val vm = open()
        vm.signOut()
        assertEquals(SessionState.SignedOut, auth.state.value)
    }
}
