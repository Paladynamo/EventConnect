package com.example.eventconnect

import com.example.eventconnect.data.Event
import com.example.eventconnect.data.Reservation
import com.example.eventconnect.usecase.ReservationUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReservationUseCaseTest {
    private val useCase = ReservationUseCase()

    @Test
    fun approve_withCapacity_decrements() {
        val ev = Event(id=1,title="T",description="",category="",date="",store="S",hasCapacity=true,capacityTotal=10,capacityAvailable=5)
        val res = Reservation(id=1,eventId=1,userEmail="u@x",userName="U",status="PENDING")
        val (r2,e2) = useCase.approve(res, ev)
        assertEquals("APPROVED", r2.status)
        assertEquals(4, e2.capacityAvailable)
    }

    @Test
    fun cancel_approved_restoresCapacity() {
        val ev = Event(id=1,title="T",description="",category="",date="",store="S",hasCapacity=true,capacityTotal=10,capacityAvailable=4)
        val res = Reservation(id=1,eventId=1,userEmail="u@x",userName="U",status="APPROVED")
        val (r2,e2) = useCase.cancel(res, ev)
        assertEquals("CANCELLED", r2.status)
        assertEquals(5, e2.capacityAvailable)
    }

    @Test
    fun reject_setsReason() {
        val res = Reservation(id=1,eventId=1,userEmail="u@x",userName="U",status="PENDING")
        val r2 = useCase.reject(res, "Cupos agotados")
        assertEquals("REJECTED", r2.status)
        assertEquals("Cupos agotados", r2.reason)
    }

    @Test(expected = IllegalArgumentException::class)
    fun approve_noCapacity_throws() {
        val ev = Event(id=1,title="T",description="",category="",date="",store="S",hasCapacity=true,capacityTotal=10,capacityAvailable=0)
        val res = Reservation(id=1,eventId=1,userEmail="u@x",userName="U",status="PENDING")
        useCase.approve(res, ev)
    }
}
