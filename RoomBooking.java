package cache.lru;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;

public class RoomBooking {
    /*
    There are a list of rooms that can be booked.
    API to check if a room can be booked for a specific calendar event (start and end time).
    API to get all available time slots.
    */

    public static class Event implements Comparable<Event> {
        private final int id;
        private final String name;
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;

        public Event(int id, String name, LocalDateTime startTime, LocalDateTime endTime) {
            if (startTime.isAfter(endTime)) {
                throw new IllegalArgumentException("Start time must be before end time.");
            }
            this.id = id;
            this.name = name;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public LocalDateTime getEndTime() {
            return endTime;
        }

        @Override
        public int compareTo(Event other) {
            return this.startTime.compareTo(other.startTime);
        }
    }

    public static class Room {
        private final int id;
        private final String name;
        private final NavigableSet<Event> bookedEvents;
        // Working hours: 9 AM to 8 PM
        private static final LocalTime WORKING_START = LocalTime.of(9, 0);
        private static final LocalTime WORKING_END = LocalTime.of(20, 0);
        private ReentrantLock lock = new ReentrantLock();

        public Room(int id, String name) {
            this.id = id;
            this.name = name;
            this.bookedEvents = new TreeSet<>();
        }

        public void addEvent(Event event) {
            lock.lock();
            try {
                if (canBeBooked(event.getStartTime(), event.getEndTime())) {
                    bookedEvents.add(event);
                } else {
                    throw new IllegalArgumentException("Room is already booked during this time.");
                }
            }
            finally {
                lock.unlock();
            }
        }

        public boolean canBeBooked(LocalDateTime startTime, LocalDateTime endTime) {
            lock.lock();
            try {
                Event dummyEvent = new Event(-1, "Dummy", startTime, endTime);

                // Get the nearest event that starts before or overlaps
                Event lower = bookedEvents.floor(dummyEvent);
                if (lower != null && lower.getEndTime().isAfter(startTime)) {
                    return false;
                }

                // Get the nearest event that starts after or overlaps
                Event higher = bookedEvents.ceiling(dummyEvent);
                if (higher != null && higher.getStartTime().isBefore(endTime)) {
                    return false;
                }

                return true;
            }
            finally {
                lock.unlock();
            }
        }

        public List<Event> getAvailableTimeSlots(LocalDateTime day) {
            lock.lock();
            try {
                List<Event> availableSlots = new ArrayList<>();
                LocalDateTime currentStart = LocalDateTime.of(day.toLocalDate(), WORKING_START);
                LocalDateTime endOfDay = LocalDateTime.of(day.toLocalDate(), WORKING_END);

                for (Event event : bookedEvents) {
                    if (currentStart.isBefore(event.getStartTime())) {
                        availableSlots.add(new Event(-1, "Available Slot", currentStart, event.getStartTime()));
                    }
                    currentStart = event.getEndTime();
                }

                if (currentStart.isBefore(endOfDay)) {
                    availableSlots.add(new Event(-1, "Available Slot", currentStart, endOfDay));
                }

                return availableSlots;
            } finally {
                lock.unlock();
            }
        }
    }

    public static void main(String[] args) {
        Room room = new Room(1, "Conference Room");

        Event event1 = new Event(1, "Meeting A", LocalDateTime.of(2025, 2, 6, 9, 0),
                LocalDateTime.of(2025, 2, 6, 10, 0));
        Event event2 = new Event(2, "Meeting B", LocalDateTime.of(2025, 2, 6, 11, 0),
                LocalDateTime.of(2025, 2, 6, 12, 0));

        room.addEvent(event1);
        room.addEvent(event2);

        System.out.println("Can book 10:00-10:30? " + room.canBeBooked(
                LocalDateTime.of(2025, 2, 6, 10, 0),
                LocalDateTime.of(2025, 2, 6, 10, 30)
        )); // Expected: true

        System.out.println("Can book 9:30-10:30? " + room.canBeBooked(
                LocalDateTime.of(2025, 2, 6, 9, 30),
                LocalDateTime.of(2025, 2, 6, 10, 30)
        )); // Expected: false

        System.out.println("\nAvailable Time Slots:");
        for (Event slot : room.getAvailableTimeSlots(LocalDateTime.of(2025, 2, 6, 0, 0))) {
            System.out.println(slot.getStartTime() + " to " + slot.getEndTime());
        }
    }
}
