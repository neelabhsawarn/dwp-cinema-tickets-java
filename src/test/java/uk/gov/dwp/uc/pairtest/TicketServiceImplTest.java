
package uk.gov.dwp.uc.pairtest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.ADULT;
import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.CHILD;
import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.INFANT;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

class TicketServiceImplTest {

    private TicketPaymentService paymentService;
    private SeatReservationService seatService;
    private TicketServiceImpl ticketService;

    @BeforeEach
    void setUp() {
        paymentService = mock(TicketPaymentService.class);
        seatService = mock(SeatReservationService.class);

        ticketService = new TicketServiceImpl(
                paymentService, seatService);
    }

    @Test
    void shouldPurchaseAdultTickets() {
        ticketService.purchaseTickets(
                1L, new TicketTypeRequest(ADULT, 2));

        verify(paymentService).makePayment(1L, 50);
        verify(seatService).reserveSeat(1L, 2);
    }

    @Test
    void shouldCalculateMixedPurchaseCorrectly() {
        ticketService.purchaseTickets(
                1L,
                new TicketTypeRequest(ADULT, 2),
                new TicketTypeRequest(CHILD, 2),
                new TicketTypeRequest(INFANT, 1));

        verify(paymentService).makePayment(1L, 80);
        verify(seatService).reserveSeat(1L, 4);

        // No other external service calls.
        verifyNoMoreInteractions(paymentService, seatService);
    }

    @Test
    void shouldNotChargeOrReserveSeatForInfants() {
        ticketService.purchaseTickets(
                1L,
                new TicketTypeRequest(ADULT, 1),
                new TicketTypeRequest(INFANT, 1));

        verify(paymentService).makePayment(1L, 25);
        verify(seatService).reserveSeat(1L, 1);
    }

    @Test
    void shouldAcceptExactly25Tickets() {
        ticketService.purchaseTickets(
                1L,
                new TicketTypeRequest(ADULT, 1),
                new TicketTypeRequest(CHILD, 24));

        verify(paymentService).makePayment(1L, 385);
        verify(seatService).reserveSeat(1L, 25);
    }

    @Test
    void shouldAggregateDuplicateTicketTypes() {
        ticketService.purchaseTickets(
                1L,
                new TicketTypeRequest(ADULT, 1),
                new TicketTypeRequest(ADULT, 2),
                new TicketTypeRequest(CHILD, 3));

        verify(paymentService).makePayment(1L, 120);
        verify(seatService).reserveSeat(1L, 6);
    }

    @Test
    void shouldRejectMoreThan25Tickets() {
        assertInvalid(
                new TicketTypeRequest(ADULT, 26));

        assertInvalid(
                new TicketTypeRequest(ADULT, 1),
                new TicketTypeRequest(CHILD, 25));

        verifyNoInteractions(paymentService, seatService);
    }

    @Test
    void shouldRejectChildTicketsWithoutAdult() {
        assertInvalid(new TicketTypeRequest(CHILD, 2));

        verifyNoInteractions(paymentService, seatService);
    }

    @Test
    void shouldRejectInfantTicketsWithoutAdult() {
        assertInvalid(new TicketTypeRequest(INFANT, 1));

        verifyNoInteractions(paymentService, seatService);
    }

    @Test
    void shouldRejectInvalidAccountIds() {
        TicketTypeRequest adult = new TicketTypeRequest(ADULT, 1);

        assertThrows(InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(null, adult));

        assertThrows(InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(0L, adult));

        assertThrows(InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(-1L, adult));

        verifyNoInteractions(paymentService, seatService);
    }

    @Test
    void shouldRejectEmptyAndNullRequests() {
        assertInvalid();

        assertInvalid((TicketTypeRequest[]) null);

        verifyNoInteractions(paymentService, seatService);
    }

    @Test
    void shouldRejectInvalidTicketQuantities() {
        assertInvalid(new TicketTypeRequest(ADULT, 0));

        assertInvalid(new TicketTypeRequest(ADULT, -1));

        verifyNoInteractions(paymentService, seatService);
    }

    @Test
    void shouldRejectNullTicketRequestsAndTypes() {
        assertInvalid((TicketTypeRequest) null);

        assertInvalid(new TicketTypeRequest(null, 1));

        verifyNoInteractions(paymentService, seatService);
    }

    @Test
    void shouldValidateEntireRequestBeforePayment() {
        assertInvalid(
                new TicketTypeRequest(ADULT, 1),
                new TicketTypeRequest(CHILD, -1));

        verifyNoInteractions(paymentService, seatService);
    }

    @Test
    void shouldAllowOneAdultWith24Infants() {
        ticketService.purchaseTickets(
                1L,
                new TicketTypeRequest(ADULT, 1),
                new TicketTypeRequest(INFANT, 24));

        verify(paymentService).makePayment(1L, 25);
        verify(seatService).reserveSeat(1L, 1);

        verifyNoMoreInteractions(paymentService, seatService);
    }

    @Test
    void shouldRejectOneAdultWith25Infants() {
        assertInvalid(
                new TicketTypeRequest(ADULT, 1),
                new TicketTypeRequest(INFANT, 25));

        verifyNoInteractions(paymentService, seatService);
    }

    private void assertInvalid(TicketTypeRequest... requests) {
        assertThrows(InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(1L, requests));
    }
}
