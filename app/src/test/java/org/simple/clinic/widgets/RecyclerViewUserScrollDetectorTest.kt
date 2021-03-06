package org.simple.clinic.widgets

import android.support.v7.widget.RecyclerView
import com.google.common.truth.Truth.assertThat
import com.jakewharton.rxbinding2.support.v7.widget.RecyclerViewScrollEvent
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(JUnitParamsRunner::class)
class RecyclerViewUserScrollDetectorTest {

  @Test
  @Parameters(value = [
    "10, ${RecyclerView.SCROLL_STATE_IDLE}, false",
    "10, ${RecyclerView.SCROLL_STATE_DRAGGING}, true",
    "10, ${RecyclerView.SCROLL_STATE_SETTLING}, false",

    "0, ${RecyclerView.SCROLL_STATE_IDLE}, false",
    "0, ${RecyclerView.SCROLL_STATE_DRAGGING}, false",
    "0, ${RecyclerView.SCROLL_STATE_SETTLING}, false",

    "-10, ${RecyclerView.SCROLL_STATE_IDLE}, false",
    "-10, ${RecyclerView.SCROLL_STATE_DRAGGING}, true",
    "-10, ${RecyclerView.SCROLL_STATE_SETTLING}, false"
  ])
  fun `scrolls by the user should be detected correctly`(dy: Int, scrollState: Int, expectedToBeByUser: Boolean) {
    val scrollEvent = mock<RecyclerViewScrollEvent>()
    whenever(scrollEvent.dy()).thenReturn(dy)

    val scrollEvents = Observable.just(scrollEvent)
    val scrollStateChanges = Observable.just(scrollState)

    val detected = Observables.combineLatest(scrollEvents, scrollStateChanges)
        .compose(RecyclerViewUserScrollDetector.streamDetections())
        .blockingFirst()

    assertThat(detected).isEqualTo(RecyclerViewScrolled(byUser = expectedToBeByUser))
  }
}
