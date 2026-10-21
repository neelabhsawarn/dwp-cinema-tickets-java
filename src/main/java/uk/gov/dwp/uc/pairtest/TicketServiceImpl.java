
package uk.gov.dwp.uc.pairtest;

import java.util.Objects;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.paymentgateway.TicketPaymentServiceImpl;
import thirdparty.seatbooking.SeatReservationService;
import thirdparty.seatbooking.SeatReservationServiceImpl;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

public class TicketServiceImpl implements TicketService {

    private static final int MAX_TICKETS = 25;
    private static final int ADULT_PRICE = 25;
    private static final int CHILD_PRICE = 15;

    private final TicketPaymentService paymentService;
    private final SeatReservationService seatReservationService;

    public TicketServiceImpl() {
        this(new TicketPaymentServiceImpl(),
             new SeatReservationServiceImpl());
    }

    public TicketServiceImpl(
            TicketPaymentService paymentService,
            SeatReservationService seatReservationService) {

        this.paymentService =
                Objects.requireNonNull(paymentService);
        this.seatReservationService =
                Objects.requireNonNull(seatReservationService);
    }

    @Override
    public void purchaseTickets(
            Long accountId,
            TicketTypeRequest... ticketTypeRequests)
            throws InvalidPurchaseException {

        if (accountId == null || accountId <= 0 ||
                ticketTypeRequests == null ||
                ticketTypeRequests.length == 0) {
            throw new InvalidPurchaseException();
        }

        int totalTickets = 0;
        int adults = 0;
        int children = 0;

        for (TicketTypeRequest request : ticketTypeRequests) {

            if (request == null ||
                    request.getTicketType() == null ||
                    request.getNoOfTickets() <= 0) {
                throw new InvalidPurchaseException();
            }

            int count = request.getNoOfTickets();

            if (count > MAX_TICKETS - totalTickets) {
                throw new InvalidPurchaseException();
            }

            totalTickets += count;

            switch (request.getTicketType()) {
                case ADULT -> adults += count;
                case CHILD -> children += count;
                case INFANT -> {
                    // Infants do not require a seat.
                }
            }
        }

        if (adults == 0) {
            throw new InvalidPurchaseException();
        }

        int totalAmount =
                (adults * ADULT_PRICE) +
                (children * CHILD_PRICE);

        int totalSeats = adults + children;

        paymentService.makePayment(accountId, totalAmount);
        seatReservationService.reserveSeat(accountId, totalSeats);
    }
}
