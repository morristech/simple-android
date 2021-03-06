package org.simple.clinic.home.overdue.removepatient

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.inOrder
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.simple.clinic.overdue.AppointmentCancelReason.Dead
import org.simple.clinic.overdue.AppointmentCancelReason.Moved
import org.simple.clinic.overdue.AppointmentCancelReason.Other
import org.simple.clinic.overdue.AppointmentCancelReason.PatientNotResponding
import org.simple.clinic.overdue.AppointmentConfig
import org.simple.clinic.overdue.AppointmentRepository
import org.simple.clinic.patient.PatientRepository
import org.simple.clinic.widgets.UiEvent
import org.threeten.bp.Period
import java.util.UUID

@RunWith(JUnitParamsRunner::class)
class RemoveAppointmentSheetControllerTest {

  private val sheet = mock<RemoveAppointmentSheet>()
  private val repository = mock<AppointmentRepository>()
  private val patientRepository = mock<PatientRepository>()

  private val appointmentUuid = UUID.randomUUID()
  private val uiEvents = PublishSubject.create<UiEvent>()

  lateinit var controller: RemoveAppointmentSheetController
  lateinit var config: AppointmentConfig

  @Before
  fun setUp() {
    config = AppointmentConfig(
        v2ApiEnabled = false,
        minimumOverduePeriodForHighRisk = Period.ofDays(1),
        overduePeriodForLowestRiskLevel = Period.ofDays(1))
    controller = RemoveAppointmentSheetController(repository, patientRepository, Single.fromCallable { config })
    uiEvents.compose(controller).subscribe { uiChange -> uiChange(sheet) }
  }

  @Test
  fun `when done is clicked, and no reason is selected, nothing should happen`() {
    uiEvents.onNext(RemoveAppointmentSheetCreated(appointmentUuid))
    uiEvents.onNext(RemoveReasonDoneClicked)
    uiEvents.onNext(RemoveReasonDoneClicked)
    uiEvents.onNext(RemoveReasonDoneClicked)

    verify(sheet, never()).enableDoneButton()
    verify(sheet, never()).closeSheet()
  }

  @Test
  fun `when done is clicked, and reason is "already visited", then repository should update and sheet should close`() {
    whenever(repository.markAsAlreadyVisited(appointmentUuid)).thenReturn(Completable.complete())

    uiEvents.onNext(RemoveAppointmentSheetCreated(appointmentUuid))
    uiEvents.onNext(RemoveReasonDoneClicked)
    uiEvents.onNext(CancelReasonClicked(Dead))
    uiEvents.onNext(AlreadyVisitedReasonClicked)
    uiEvents.onNext(CancelReasonClicked(PatientNotResponding))
    uiEvents.onNext(CancelReasonClicked(Other))
    uiEvents.onNext(AlreadyVisitedReasonClicked)
    uiEvents.onNext(RemoveReasonDoneClicked)

    verify(repository, never()).cancelWithReason(any(), any())

    val inOrder = inOrder(sheet, repository)
    inOrder.verify(sheet, times(5)).enableDoneButton()
    inOrder.verify(repository).markAsAlreadyVisited(appointmentUuid)
    inOrder.verify(sheet).closeSheet()
  }

  @Test
  fun `when done is clicked, and reason is "Patient dead", then patient repository should be updated`() {
    whenever(repository.cancelWithReason(appointmentUuid, Dead)).thenReturn(Completable.complete())
    val patientUuid = UUID.randomUUID()
    whenever(patientRepository.updatePatientStatusToDead(patientUuid)).thenReturn(Completable.complete())

    uiEvents.onNext(RemoveAppointmentSheetCreated(appointmentUuid))
    uiEvents.onNext(AlreadyVisitedReasonClicked)
    uiEvents.onNext(CancelReasonClicked(PatientNotResponding))
    uiEvents.onNext(CancelReasonClicked(Dead))
    uiEvents.onNext(PatientDeadClicked(patientUuid))
    uiEvents.onNext(RemoveReasonDoneClicked)

    verify(repository, never()).markAsAlreadyVisited(any())

    val inOrder = inOrder(sheet, repository, patientRepository)
    inOrder.verify(sheet, times(4)).enableDoneButton()
    inOrder.verify(patientRepository).updatePatientStatusToDead(patientUuid)
    inOrder.verify(repository).cancelWithReason(appointmentUuid, Dead)
    inOrder.verify(sheet).closeSheet()
  }

  @Test
  fun `when done is clicked, and a cancel reason is selected, then repository should update and sheet should close`() {
    whenever(repository.cancelWithReason(appointmentUuid, Moved)).thenReturn(Completable.complete())

    uiEvents.onNext(RemoveAppointmentSheetCreated(appointmentUuid))
    uiEvents.onNext(RemoveReasonDoneClicked)
    uiEvents.onNext(AlreadyVisitedReasonClicked)
    uiEvents.onNext(CancelReasonClicked(PatientNotResponding))
    uiEvents.onNext(AlreadyVisitedReasonClicked)
    uiEvents.onNext(CancelReasonClicked(Moved))
    uiEvents.onNext(RemoveReasonDoneClicked)

    verify(repository, never()).markAsAlreadyVisited(any())

    val inOrder = inOrder(sheet, repository)
    inOrder.verify(sheet, times(4)).enableDoneButton()
    inOrder.verify(repository).cancelWithReason(appointmentUuid, Moved)
    inOrder.verify(sheet).closeSheet()
  }

  @Test
  @Parameters("true", "false")
  fun `v2 API reasons should only be enabled when v2 flag is enabled`(v2ApiEnabled: Boolean) {
    config = config.copy(v2ApiEnabled = v2ApiEnabled)

    uiEvents.onNext(RemoveAppointmentSheetCreated(appointmentUuid))
    verify(sheet).setV2ApiReasonsEnabled(v2ApiEnabled)
  }
}
