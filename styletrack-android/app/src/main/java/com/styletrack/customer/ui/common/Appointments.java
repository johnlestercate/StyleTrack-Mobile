package com.styletrack.customer.ui.common;

import com.styletrack.customer.data.Appointment;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/** Small appointment-list predicates shared by Home, Bookings and Booking-detail. */
public final class Appointments {
    private Appointments() {}

    /** Still open (pending/accepted) and hasn't finished yet. */
    public static boolean isUpcoming(Appointment a) {
        return a.isOpen() && Formats.parseDateTime(a.endTime).isAfter(LocalDateTime.now());
    }

    /** A booking can still be changed while it is open and hasn't started yet. */
    public static boolean canChange(Appointment a) {
        return a.isOpen() && Formats.parseDateTime(a.startTime).isAfter(LocalDateTime.now());
    }

    /** The soonest booking that is still ahead of us and not cancelled/rejected. */
    public static Appointment nextAppointment(List<Appointment> all) {
        return all.stream()
                .filter(Appointments::isUpcoming)
                .min(Comparator.comparing(a -> Formats.parseDateTime(a.startTime)))
                .orElse(null);
    }
}
