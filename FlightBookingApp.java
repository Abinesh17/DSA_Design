import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class FlightBookingApp {
    /*
    Admin
     - should be able to add users
     - Should be able to add flights
    User
     - able to search flights
     - book flights
     */

    public static class FlightBookingService
    {
        Map<String,User> userIdVsUserMap;
        Map<String,List<Flight>> flightDirectionVsFlightMap;
        Map<String,Flight> flightIdVsFlightMap;

        public FlightBookingService()
        {
            this.userIdVsUserMap = new HashMap<>();
            this.flightIdVsFlightMap = new HashMap<>();
            this.flightDirectionVsFlightMap = new HashMap<>();
        }

        public void addFlight(String flightId, String airLineName, String origin, String destination, int maxCapacity)
        {
            Flight flight = new Flight(flightId, airLineName, origin, destination, maxCapacity);
            String flightDirection = origin+"->"+destination;
            flightDirectionVsFlightMap.putIfAbsent(flightDirection, new ArrayList<>());
            flightDirectionVsFlightMap.get(flightDirection).add(flight);
            flightIdVsFlightMap.put(flightId, flight);
        }

        public void addUser(String id,String name, String email)
        {
            if(userIdVsUserMap.containsKey(id))
            {
                System.out.println("USer already exists in the system. Cannot add!");
                return;
            }
            User user = new User(id, name, email);
            this.userIdVsUserMap.put(id, user);
        }

        public List<Flight> searchFlight(String origin, String destination, boolean onlyDirect)
        {
            String flightDirection = origin + "->" + destination;
            if (!flightDirectionVsFlightMap.containsKey(flightDirection)) {
                System.out.println("No flights available on this route");
                return new ArrayList<>();
            }

            List<Flight> flights = flightDirectionVsFlightMap.get(flightDirection);
            if (onlyDirect) {
                return flights.stream().filter(Flight::isDirectFlight).collect(Collectors.toList());
            }

            return flights;
        }

        public boolean bookFlight(User user, String flightId) {
            if(user==null)
            {
                System.out.println("User information cannot be null.");
                return false;
            }

            if (!flightIdVsFlightMap.containsKey(flightId)) {
                System.out.println("Flight information not available!");
                return false;
            }

            Flight flight = flightIdVsFlightMap.get(flightId);
            return flight.book(user);
        }
        public void addConnectingFlight(String firstFlightId, String secondFlightId) {
            if (!flightIdVsFlightMap.containsKey(firstFlightId) || !flightIdVsFlightMap.containsKey(secondFlightId)) {
                System.out.println("Invalid flight IDs");
                return;
            }

            Flight firstFlight = flightIdVsFlightMap.get(firstFlightId);
            Flight secondFlight = flightIdVsFlightMap.get(secondFlightId);
//            firstFlight.addConnectingFlight(secondFlight);

            // this connecting flight shouldn't disturb the direct flights. and this should be a separate info for users
            addFlight(firstFlight.getFlightId(),firstFlight.getAirLineName(),
                    firstFlight.origin, secondFlight.destination, firstFlight.maxCapacity);
            Flight flight = flightIdVsFlightMap.get(firstFlightId);
            flight.addConnectingFlight(firstFlight);
            flight.addConnectingFlight(secondFlight);
        }
    }
    public static class Flight
    {
        private String flightId;
        private String airLineName;
        private String origin;
        private String destination;
        private int maxCapacity;
        private List<Booking> bookings;
        private List<Flight> subFlights;
        ReentrantLock lock =new ReentrantLock();
        public Flight(String flightId, String airLineName, String origin,  String destination, int maxCapacity)
        {
            this.flightId = flightId;
            this.airLineName = airLineName;
            this.origin = origin;
            this.destination = destination;
            this.maxCapacity = maxCapacity;
            this.bookings = new ArrayList<>();
            this.subFlights = new ArrayList<>();
        }

        public String getFlightId() {
            return flightId;
        }

        public String getAirLineName() {
            return airLineName;
        }

        // Method to check if flight is direct
        public boolean isDirectFlight() {
            return subFlights.isEmpty();
        }

        // Method to add a connecting flight
        public void addConnectingFlight(Flight flight) {
            subFlights.add(flight);
        }

        public boolean isAvailable()
        {
            return bookings.size() < maxCapacity;
        }

        public boolean book(User user)
        {
            lock.lock();
            try {
                if (bookings.size() < maxCapacity) {
                    String seat = "seat" + bookings.size();
                    Booking booking = new Booking(user.getId(), flightId, seat);
                    this.bookings.add(booking);
                    return true;
                }
                System.out.println("No seats are available in the flight");
                return false;
            }
            finally {
                lock.unlock();
            }
        }
    }

    public static class User
    {
        private String id;
        private String name;
        private String email;
        public User(String id, String name, String email)
        {
            this.id = id;
            this.name = name;
            this.email = email;
        }

        public String getId() {
            return id;
        }
    }

    public static class Booking
    {
        private String userId;
        private String seat;
        private String flightId;

        public Booking(String userId, String flightId, String seat)
        {
            this.userId = userId;
            this.flightId = flightId;
            this.seat = seat;
        }
    }

    public static void main(String[] args) {
        FlightBookingService service = new FlightBookingService();

// Adding direct flights
        service.addFlight("F1", "Indigo", "New York", "London", 200);
        service.addFlight("F2", "Air India", "London", "Paris", 180);
        service.addFlight("F3", "Lufthansa", "New York", "Paris", 220);

// Adding a connecting flight (New York → London → Paris)
        service.addConnectingFlight("F1", "F2");

// Searching for flights
        List<Flight> directFlights = service.searchFlight("New York", "Paris", true);
        System.out.println("Direct flights found: " + directFlights.size());

        List<Flight> allFlights = service.searchFlight("New York", "Paris", false);
        System.out.println("Total flights found (including connections): " + allFlights.size());

    }
}
