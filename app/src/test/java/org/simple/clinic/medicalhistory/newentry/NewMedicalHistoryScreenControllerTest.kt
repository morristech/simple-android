package org.simple.clinic.medicalhistory.newentry

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.inOrder
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Test
import org.simple.clinic.medicalhistory.MedicalHistory
import org.simple.clinic.medicalhistory.MedicalHistory.Answer.UNKNOWN
import org.simple.clinic.medicalhistory.MedicalHistoryQuestion
import org.simple.clinic.medicalhistory.MedicalHistoryQuestion.DIAGNOSED_WITH_HYPERTENSION
import org.simple.clinic.medicalhistory.MedicalHistoryQuestion.HAS_DIABETES
import org.simple.clinic.medicalhistory.MedicalHistoryQuestion.HAS_HAD_A_HEART_ATTACK
import org.simple.clinic.medicalhistory.MedicalHistoryQuestion.HAS_HAD_A_KIDNEY_DISEASE
import org.simple.clinic.medicalhistory.MedicalHistoryQuestion.HAS_HAD_A_STROKE
import org.simple.clinic.medicalhistory.MedicalHistoryQuestion.IS_ON_TREATMENT_FOR_HYPERTENSION
import org.simple.clinic.medicalhistory.MedicalHistoryRepository
import org.simple.clinic.medicalhistory.OngoingMedicalHistoryEntry
import org.simple.clinic.patient.Gender
import org.simple.clinic.patient.OngoingNewPatientEntry
import org.simple.clinic.patient.OngoingNewPatientEntry.PersonalDetails
import org.simple.clinic.patient.PatientMocker
import org.simple.clinic.patient.PatientRepository
import org.simple.clinic.widgets.ScreenCreated
import org.simple.clinic.widgets.UiEvent
import java.util.UUID

class NewMedicalHistoryScreenControllerTest {

  private val screen: NewMedicalHistoryScreen = mock()
  private val medicalHistoryRepository: MedicalHistoryRepository = mock()
  private val patientRepository: PatientRepository = mock()

  private lateinit var controller: NewMedicalHistoryScreenController
  private val uiEvents = PublishSubject.create<UiEvent>()

  @Before
  fun setUp() {
    controller = NewMedicalHistoryScreenController(medicalHistoryRepository, patientRepository)

    whenever(medicalHistoryRepository.save(any<UUID>(), any())).thenReturn(Completable.complete())
    whenever(patientRepository.ongoingEntry()).thenReturn(Single.never())

    uiEvents.compose(controller).subscribe { uiChange -> uiChange(screen) }
  }

  @Test
  fun `when screen is started then the patient's name should be shown on the toolbar`() {
    val patientName = "Ashok Kumar"
    val patientEntry = OngoingNewPatientEntry(personalDetails = PersonalDetails(
        fullName = patientName,
        dateOfBirth = null,
        age = "20",
        gender = Gender.TRANSGENDER))
    whenever(patientRepository.ongoingEntry()).thenReturn(Single.just(patientEntry))

    uiEvents.onNext(ScreenCreated())

    verify(screen).setPatientName(patientName)
  }

  @Test
  fun `when save is clicked with selected answers then patient with the answers should be saved and summary screen should be opened`() {
    val savedPatient = PatientMocker.patient(uuid = UUID.randomUUID())
    whenever(patientRepository.saveOngoingEntryAsPatient()).thenReturn(Single.just(savedPatient))

    val questionsAndAnswers = MedicalHistoryQuestion.values()
        .map { it to randomAnswer() }
        .toMap()

    uiEvents.onNext(ScreenCreated())
    questionsAndAnswers.forEach { (question, answer) ->
      uiEvents.onNext(NewMedicalHistoryAnswerToggled(question, answer))
    }
    uiEvents.onNext(SaveMedicalHistoryClicked())

    val inOrder = inOrder(medicalHistoryRepository, patientRepository, screen)
    inOrder.verify(patientRepository).saveOngoingEntryAsPatient()
    inOrder.verify(medicalHistoryRepository).save(
        patientUuid = savedPatient.uuid,
        historyEntry = OngoingMedicalHistoryEntry(
            diagnosedWithHypertension = questionsAndAnswers[DIAGNOSED_WITH_HYPERTENSION]!!,
            isOnTreatmentForHypertension = questionsAndAnswers[IS_ON_TREATMENT_FOR_HYPERTENSION]!!,
            hasHadHeartAttack = questionsAndAnswers[HAS_HAD_A_HEART_ATTACK]!!,
            hasHadStroke = questionsAndAnswers[HAS_HAD_A_STROKE]!!,
            hasHadKidneyDisease = questionsAndAnswers[HAS_HAD_A_KIDNEY_DISEASE]!!,
            hasDiabetes = questionsAndAnswers[HAS_DIABETES]!!)
    )
    inOrder.verify(screen).openPatientSummaryScreen(savedPatient.uuid)
  }

  private fun randomAnswer(): MedicalHistory.Answer {
    return MedicalHistory.Answer::class.java.enumConstants.asList().shuffled().first()
  }

  @Test
  fun `when save is clicked with no answers then patient with an empty medical history should be saved and summary screen should be opened`() {
    val savedPatient = PatientMocker.patient(uuid = UUID.randomUUID())
    whenever(patientRepository.saveOngoingEntryAsPatient()).thenReturn(Single.just(savedPatient))

    uiEvents.onNext(ScreenCreated())
    uiEvents.onNext(SaveMedicalHistoryClicked())

    val inOrder = inOrder(medicalHistoryRepository, patientRepository, screen)
    inOrder.verify(patientRepository).saveOngoingEntryAsPatient()
    inOrder.verify(medicalHistoryRepository).save(
        patientUuid = savedPatient.uuid,
        historyEntry = OngoingMedicalHistoryEntry(
            diagnosedWithHypertension = UNKNOWN,
            isOnTreatmentForHypertension = UNKNOWN,
            hasHadHeartAttack = UNKNOWN,
            hasHadStroke = UNKNOWN,
            hasHadKidneyDisease = UNKNOWN,
            hasDiabetes = UNKNOWN))
    inOrder.verify(screen).openPatientSummaryScreen(savedPatient.uuid)
  }
}
