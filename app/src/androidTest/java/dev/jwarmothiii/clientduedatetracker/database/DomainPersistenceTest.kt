package dev.jwarmothiii.clientduedatetracker.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.jwarmothiii.clientduedatetracker.database.entity.RequirementTemplateEntity
import dev.jwarmothiii.clientduedatetracker.domain.client.api.ClientCommandResult
import dev.jwarmothiii.clientduedatetracker.domain.client.data.RoomClientApi
import dev.jwarmothiii.clientduedatetracker.domain.client.model.CreateClientCommand
import dev.jwarmothiii.clientduedatetracker.domain.client.usecase.OnboardClientUseCase
import dev.jwarmothiii.clientduedatetracker.domain.client.usecase.OnboardingResult
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.api.TemplateDraft
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.data.DataIntegrityException
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.data.RoomContractDefinitionApi
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.ContractTypeId
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.ReplacementScope
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.api.TrackingCommandResult
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.data.RoomContractTrackingApi
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.model.RequirementStatus
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.usecase.DailyGenerationUseCase
import dev.jwarmothiii.clientduedatetracker.domain.contracttracking.usecase.DeadlineGenerator
import dev.jwarmothiii.clientduedatetracker.domain.notes.api.NoteCommandResult
import dev.jwarmothiii.clientduedatetracker.domain.notes.data.RoomNotesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class DomainPersistenceTest {
    private lateinit var database: ClientDueDateDatabase
    private lateinit var clientApi: RoomClientApi
    private lateinit var definitionApi: RoomContractDefinitionApi
    private lateinit var trackingApi: RoomContractTrackingApi
    private lateinit var notesApi: RoomNotesApi
    private lateinit var generation: DailyGenerationUseCase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, ClientDueDateDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        clientApi = RoomClientApi(database.clientDao())
        definitionApi = RoomContractDefinitionApi(database, database.definitionDao())
        trackingApi = RoomContractTrackingApi(database, definitionApi, database.trackingDao())
        notesApi = RoomNotesApi(database.noteDao())
        generation = DailyGenerationUseCase(definitionApi, trackingApi, DeadlineGenerator())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun onboardingIsAtomicAndGenerationIsIdempotentAndSuppressible() =
        runBlocking {
            definitionApi.ensureSeedDefinitions()
            val contractType = definitionApi.activeContractTypes().first()
            val onboard =
                OnboardClientUseCase(
                    RoomDatabaseTransactionRunner(database),
                    clientApi,
                    definitionApi,
                    trackingApi,
                    generation,
                )
            val command =
                CreateClientCommand(
                    initials = " ab ",
                    intakeDate = LocalDate.parse("2026-07-01"),
                    scheduledAssessmentDate = LocalDate.parse("2026-07-10"),
                )
            val result =
                onboard(
                    command,
                    contractType.id,
                    LocalDate.parse("2026-07-01"),
                    Instant.parse("2026-07-01T12:00:00Z"),
                    ZoneOffset.UTC,
                )
            assert(result is OnboardingResult.Success)
            val initialCount = database.trackingDao().requirementCount()
            generation(LocalDate.parse("2026-07-01"), ZoneOffset.UTC, Instant.EPOCH)
            assertEquals(initialCount, database.trackingDao().requirementCount())

            val generated = trackingApi.pendingRequirements().first()
            trackingApi.permanentlyDeleteRequirement(generated.requirement.id, Instant.EPOCH)
            generation(LocalDate.parse("2026-07-01"), ZoneOffset.UTC, Instant.EPOCH)
            assertEquals(initialCount - 1, database.trackingDao().requirementCount())
        }

    @Test
    fun oneContractIsEnforcedAndCompletionCanReopen() =
        runBlocking {
            definitionApi.ensureSeedDefinitions()
            val client =
                (
                    clientApi.create(
                        CreateClientCommand(
                            "AB",
                            LocalDate.parse("2026-07-01"),
                            LocalDate.parse("2026-07-02"),
                        ),
                        LocalDate.parse("2026-07-01"),
                    ) as ClientCommandResult.Success
                ).value
            val type = definitionApi.activeContractTypes().first()
            val contract =
                (
                    trackingApi.createContract(client.id, type.id, client.intakeDate, null) as
                        TrackingCommandResult.Success
                ).value
            assert(trackingApi.createContract(client.id, type.id, client.intakeDate, null) is TrackingCommandResult.Conflict)
            val requirement =
                (
                    trackingApi.createAdHocRequirement(
                        contract.id,
                        "Call",
                        "",
                        LocalDate.parse("2026-07-03"),
                    ) as TrackingCommandResult.Success
                ).value
            val completed =
                (
                    trackingApi.completeRequirement(requirement.id, Instant.parse("2026-07-02T12:00:00Z")) as
                        TrackingCommandResult.Success
                ).value
            assertEquals(RequirementStatus.COMPLETED, completed.status)
            val reopened = (trackingApi.reopenRequirement(requirement.id) as TrackingCommandResult.Success).value
            assertEquals(RequirementStatus.PENDING, reopened.status)
            assertNull(reopened.completedAt)
        }

    @Test
    fun deletingRequirementSetsLinkedNoteReferenceToNullAndDeletingClientCascades() =
        runBlocking {
            definitionApi.ensureSeedDefinitions()
            val client =
                (
                    clientApi.create(
                        CreateClientCommand(
                            "AB",
                            LocalDate.parse("2026-07-01"),
                            LocalDate.parse("2026-07-02"),
                        ),
                        LocalDate.parse("2026-07-01"),
                    ) as ClientCommandResult.Success
                ).value
            val type = definitionApi.activeContractTypes().first()
            val contract =
                (
                    trackingApi.createContract(client.id, type.id, client.intakeDate, null) as
                        TrackingCommandResult.Success
                ).value
            val requirement =
                (
                    trackingApi.createAdHocRequirement(
                        contract.id,
                        "Call",
                        "",
                        LocalDate.parse("2026-07-03"),
                    ) as TrackingCommandResult.Success
                ).value
            val note =
                (
                    notesApi.create(client.id, "Remember this", requirement.id, true, Instant.EPOCH) as
                        NoteCommandResult.Success
                ).value
            trackingApi.permanentlyDeleteRequirement(requirement.id, Instant.EPOCH)
            assertNull(database.noteDao().entity(note.id.value)?.requirementId)

            clientApi.permanentlyDelete(client.id)
            assertEquals(0, database.trackingDao().contractCount())
            assertEquals(
                0,
                database
                    .noteDao()
                    .observePinned(3)
                    .first()
                    .size,
            )
        }

    @Test
    fun failedOnboardingRollsBackClientInsert() =
        runBlocking {
            val onboard =
                OnboardClientUseCase(
                    RoomDatabaseTransactionRunner(database),
                    clientApi,
                    definitionApi,
                    trackingApi,
                    generation,
                )
            val result =
                onboard(
                    CreateClientCommand(
                        "AB",
                        LocalDate.parse("2026-07-01"),
                        LocalDate.parse("2026-07-02"),
                    ),
                    ContractTypeId("missing"),
                    LocalDate.parse("2026-07-01"),
                    Instant.EPOCH,
                    ZoneOffset.UTC,
                )
            assert(result is OnboardingResult.Failure)
            assertEquals(0, database.clientDao().count())
        }

    @Test
    fun corruptPolicyFailsClosed() =
        runBlocking {
            definitionApi.ensureSeedDefinitions()
            val type = database.definitionDao().activeContractTypes().first()
            val publicId = "corrupt-template"
            database
                .definitionDao()
                .insertTemplate(
                    RequirementTemplateEntity(
                        publicId = publicId,
                        contractTypeId = type.id,
                        lineageId = publicId,
                        version = 1,
                        title = "Corrupt",
                        instructions = "",
                        deadlinePolicyJson = """{"version":1,"anchor":"UNKNOWN"}""",
                        notificationLeadDays = 3,
                        active = true,
                    ),
                )
            assertThrows(DataIntegrityException::class.java) {
                kotlinx.coroutines.runBlocking {
                    definitionApi.template(
                        dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.RequirementTemplateId(
                            publicId,
                        ),
                    )
                }
            }
            Unit
        }

    @Test
    fun replacementAndDeactivationPreserveExistingOccurrences() =
        runBlocking {
            definitionApi.ensureSeedDefinitions()
            val type = definitionApi.activeContractTypes().first()
            val onboard =
                OnboardClientUseCase(
                    RoomDatabaseTransactionRunner(database),
                    clientApi,
                    definitionApi,
                    trackingApi,
                    generation,
                )
            onboard(
                CreateClientCommand(
                    "AB",
                    LocalDate.parse("2026-07-01"),
                    LocalDate.parse("2026-07-10"),
                    actualAssessmentCompletedDate = LocalDate.parse("2026-07-10"),
                ),
                type.id,
                LocalDate.parse("2026-07-01"),
                Instant.EPOCH,
                ZoneOffset.UTC,
            )
            val template = definitionApi.templatesForContractType(type.id).first()
            val beforeIds = trackingApi.pendingRequirements().map { it.requirement.id }.toSet()

            definitionApi.replaceTemplate(
                template.id,
                TemplateDraft(
                    title = "${template.title} updated",
                    instructions = template.instructions,
                    policy = template.policy,
                    notificationLeadDays = template.notificationLeadDays,
                ),
                ReplacementScope.FUTURE_AND_ACTIVE_CLIENTS,
            )
            generation(LocalDate.parse("2026-07-01"), ZoneOffset.UTC, Instant.EPOCH)
            val afterReplacementIds = trackingApi.pendingRequirements().map { it.requirement.id }.toSet()
            assert(afterReplacementIds.containsAll(beforeIds))

            val replacement = definitionApi.templatesForContractType(type.id).first { it.lineageId == template.lineageId }
            definitionApi.deactivateTemplate(replacement.id)
            generation(LocalDate.parse("2026-07-01"), ZoneOffset.UTC, Instant.EPOCH)
            val afterDeactivationIds = trackingApi.pendingRequirements().map { it.requirement.id }.toSet()
            assert(afterDeactivationIds.containsAll(beforeIds))
        }
}
