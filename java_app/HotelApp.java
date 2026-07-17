// ============================================================
// HotelApp.java — Hotel Room Booking Console Application
// HOW TO COMPILE: javac HotelApp.java
// HOW TO RUN:     java HotelApp
// INTERVIEW TIP:  "Single-file Java app with all classes
//                  included — easy to compile and run anywhere
//                  without IDE setup."
// ============================================================

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class HotelApp {

    // ── Room Type Enum ────────────────────────────────────────
    // INTERVIEW TIP: "Enum is perfect for fixed options like
    //                 room types — type-safe, readable, and
    //                 prevents invalid string inputs."
    enum RoomType {
        SINGLE("Single",  new BigDecimal("2000.00"), new BigDecimal("1.0")),
        DOUBLE("Double",  new BigDecimal("2000.00"), new BigDecimal("1.2")),
        DELUXE("Deluxe",  new BigDecimal("2000.00"), new BigDecimal("1.4")),
        SUITE ("Suite",   new BigDecimal("2000.00"), new BigDecimal("1.6"));

        final String     label;
        final BigDecimal base;
        final BigDecimal multiplier;

        RoomType(String label, BigDecimal base, BigDecimal multiplier) {
            this.label      = label;
            this.base       = base;
            this.multiplier = multiplier;
        }

        // Price per night = base × multiplier
        BigDecimal nightlyRate() {
            return base.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
        }

        @Override public String toString() { return label; }
    }

    // ── Room Status Enum ──────────────────────────────────────
    enum RoomStatus { VACANT, OCCUPIED, OUT_OF_SERVICE }

    // ── Booking Status Enum ───────────────────────────────────
    enum BookingStatus { RESERVED, IN_HOUSE, COMPLETED, CANCELLED }

    // ── Room Class ────────────────────────────────────────────
    // INTERVIEW TIP: "Encapsulation — fields are package-private
    //                 here for simplicity, would be private with
    //                 getters/setters in production code."
    static class Room {
        final int      number;
        final RoomType type;
        final int      capacity; // max guests
        RoomStatus     status = RoomStatus.VACANT;

        Room(int number, RoomType type, int capacity) {
            this.number   = number;
            this.type     = type;
            this.capacity = capacity;
        }

        @Override
        public String toString() {
            return String.format("Room %-4d | %-7s | Capacity: %d | Status: %-14s | Rate: %s/night",
                number, type, capacity, status,
                "Rs." + type.nightlyRate().toPlainString());
        }
    }

    // ── Booking Class ─────────────────────────────────────────
    static class Booking {
        final int           id;
        final int           roomNumber;
        final String        guestName;
        final String        guestPhone;
        final LocalDate     checkIn;
        final LocalDate     checkOut;
        final long          nights;
        final BigDecimal    nightlyRate;
        final BigDecimal    roomCharge;
        final BigDecimal    tax;
        final BigDecimal    total;
        BookingStatus       status = BookingStatus.RESERVED;

        private static final BigDecimal TAX_RATE = new BigDecimal("0.10");

        Booking(int id, int roomNumber, String guestName, String guestPhone,
                LocalDate checkIn, LocalDate checkOut, BigDecimal nightlyRate) {
            this.id         = id;
            this.roomNumber = roomNumber;
            this.guestName  = guestName;
            this.guestPhone = guestPhone;
            this.checkIn    = checkIn;
            this.checkOut   = checkOut;
            this.nightlyRate= nightlyRate.setScale(2, RoundingMode.HALF_UP);
            this.nights     = java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
            this.roomCharge = this.nightlyRate.multiply(new BigDecimal(nights))
                                              .setScale(2, RoundingMode.HALF_UP);
            this.tax        = this.roomCharge.multiply(TAX_RATE)
                                             .setScale(2, RoundingMode.HALF_UP);
            this.total      = this.roomCharge.add(this.tax)
                                             .setScale(2, RoundingMode.HALF_UP);
        }
    }

    // ── Hotel Data ────────────────────────────────────────────
    private final Map<Integer, Room>    rooms    = new LinkedHashMap<>();
    private final Map<Integer, Booking> bookings = new LinkedHashMap<>();
    private int nextBookingId = 1001;
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final Scanner sc = new Scanner(System.in);

    // ── Seed Sample Rooms ─────────────────────────────────────
    private void seedRooms() {
        // Single rooms (1 guest max)
        rooms.put(101, new Room(101, RoomType.SINGLE, 1));
        rooms.put(102, new Room(102, RoomType.SINGLE, 1));
        rooms.put(103, new Room(103, RoomType.SINGLE, 1));
        // Double rooms (2 guests max)
        rooms.put(201, new Room(201, RoomType.DOUBLE, 2));
        rooms.put(202, new Room(202, RoomType.DOUBLE, 2));
        rooms.put(203, new Room(203, RoomType.DOUBLE, 2));
        // Deluxe rooms (2 guests max)
        rooms.put(301, new Room(301, RoomType.DELUXE, 2));
        rooms.put(302, new Room(302, RoomType.DELUXE, 2));
        // Suite rooms (4 guests max)
        rooms.put(401, new Room(401, RoomType.SUITE, 4));
        rooms.put(402, new Room(402, RoomType.SUITE, 4));
    }

    // ── Main Entry Point ──────────────────────────────────────
    public static void main(String[] args) {
        HotelApp app = new HotelApp();
        app.seedRooms();
        app.printBanner();
        app.menuLoop();
    }

    private void printBanner() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║     GRAND VISTA HOTEL — BOOKING SYSTEM   ║");
        System.out.println("║         Console App  |  Java 17+          ║");
        System.out.println("╚══════════════════════════════════════════╝");
    }

    // ── Menu Loop ─────────────────────────────────────────────
    // INTERVIEW TIP: "Menu-driven app uses a while loop with
    //                 switch — standard pattern for console apps."
    private void menuLoop() {
        while (true) {
            System.out.println();
            System.out.println("══════════════ MAIN MENU ══════════════");
            System.out.println("  1. View All Rooms");
            System.out.println("  2. Search Available Rooms");
            System.out.println("  3. Create Booking");
            System.out.println("  4. View Booking Details");
            System.out.println("  5. Cancel Booking");
            System.out.println("  6. Check In");
            System.out.println("  7. Check Out & Print Bill");
            System.out.println("  8. View All Bookings");
            System.out.println("  9. Exit");
            System.out.println("═══════════════════════════════════════");

            int choice = readInt("Enter choice", 1, 9);
            System.out.println();

            switch (choice) {
                case 1 -> listAllRooms();
                case 2 -> searchAvailableRooms();
                case 3 -> createBookingFlow();
                case 4 -> viewBookingById();
                case 5 -> cancelBookingFlow();
                case 6 -> checkInFlow();
                case 7 -> checkOutFlow();
                case 8 -> viewAllBookings();
                case 9 -> {
                    System.out.println("Thank you for using Grand Vista Hotel Booking System.");
                    System.out.println("Goodbye! 👋");
                    return;
                }
            }
        }
    }

    // ── Feature 1: List All Rooms ─────────────────────────────
    private void listAllRooms() {
        System.out.println("══════════════ ALL ROOMS ══════════════");
        for (Room r : rooms.values()) {
            System.out.println("  " + r);
        }
    }

    // ── Feature 2: Search Available Rooms ────────────────────
    private void searchAvailableRooms() {
        System.out.println("══════ SEARCH AVAILABLE ROOMS ══════");
        RoomType type    = pickRoomType();
        LocalDate checkIn  = readDate("Check-in date  (yyyy-MM-dd)");
        LocalDate checkOut = readDate("Check-out date (yyyy-MM-dd)");
        if (!validateDates(checkIn, checkOut)) return;

        List<Room> available = getAvailableRooms(type, checkIn, checkOut);
        if (available.isEmpty()) {
            System.out.println("No " + type + " rooms available for selected dates.");
        } else {
            System.out.println("Available " + type + " rooms:");
            for (Room r : available) System.out.println("  " + r);
        }
    }

    // ── Feature 3: Create Booking ─────────────────────────────
    private void createBookingFlow() {
        System.out.println("══════════ CREATE BOOKING ══════════");
        RoomType  type     = pickRoomType();
        LocalDate checkIn  = readDate("Check-in date  (yyyy-MM-dd)");
        LocalDate checkOut = readDate("Check-out date (yyyy-MM-dd)");
        if (!validateDates(checkIn, checkOut)) return;

        List<Room> available = getAvailableRooms(type, checkIn, checkOut);
        if (available.isEmpty()) {
            System.out.println("No " + type + " rooms available. Try different dates.");
            return;
        }

        System.out.println("Available rooms:");
        for (Room r : available) System.out.println("  " + r);

        int roomNum = readInt("Enter room number",
            available.stream().map(r -> r.number).toList());
        Room room = rooms.get(roomNum);

        String guestName  = readNonEmpty("Guest full name");
        String guestPhone = readNonEmpty("Guest phone number");

        int id = nextBookingId++;
        Booking b = new Booking(id, room.number, guestName, guestPhone,
                                checkIn, checkOut, room.type.nightlyRate());
        bookings.put(id, b);

        System.out.println();
        System.out.println("✅ Booking confirmed!");
        printBookingDetails(b);
    }

    // ── Feature 4: View Booking by ID ────────────────────────
    private void viewBookingById() {
        int id = readInt("Enter booking ID", 1000, Integer.MAX_VALUE);
        Booking b = bookings.get(id);
        if (b == null) { System.out.println("❌ No booking found with ID " + id); return; }
        printBookingDetails(b);
    }

    // ── Feature 5: Cancel Booking ─────────────────────────────
    private void cancelBookingFlow() {
        int id = readInt("Enter booking ID to cancel", 1000, Integer.MAX_VALUE);
        Booking b = bookings.get(id);
        if (b == null) { System.out.println("❌ No booking found with ID " + id); return; }
        if (b.status != BookingStatus.RESERVED) {
            System.out.println("❌ Only RESERVED bookings can be cancelled. Current status: " + b.status);
            return;
        }
        b.status = BookingStatus.CANCELLED;
        System.out.println("✅ Booking #" + id + " has been cancelled.");
        System.out.println("   Room " + b.roomNumber + " is now available again.");
    }

    // ── Feature 6: Check In ───────────────────────────────────
    private void checkInFlow() {
        int id = readInt("Enter booking ID for check-in", 1000, Integer.MAX_VALUE);
        Booking b = bookings.get(id);
        if (b == null) { System.out.println("❌ No booking found."); return; }
        if (b.status != BookingStatus.RESERVED) {
            System.out.println("❌ Only RESERVED bookings can be checked in. Status: " + b.status);
            return;
        }
        Room r = rooms.get(b.roomNumber);
        if (r.status != RoomStatus.VACANT) {
            System.out.println("❌ Room " + r.number + " is not vacant. Status: " + r.status);
            return;
        }
        b.status = BookingStatus.IN_HOUSE;
        r.status = RoomStatus.OCCUPIED;
        System.out.println("✅ Guest " + b.guestName + " has checked in to Room " + b.roomNumber);
        System.out.println("   Enjoy your stay! 🏨");
    }

    // ── Feature 7: Check Out & Print Bill ────────────────────
    private void checkOutFlow() {
        int id = readInt("Enter booking ID for check-out", 1000, Integer.MAX_VALUE);
        Booking b = bookings.get(id);
        if (b == null) { System.out.println("❌ No booking found."); return; }
        if (b.status != BookingStatus.IN_HOUSE) {
            System.out.println("❌ Only IN_HOUSE bookings can be checked out. Status: " + b.status);
            return;
        }
        Room r = rooms.get(b.roomNumber);
        b.status = BookingStatus.COMPLETED;
        r.status = RoomStatus.VACANT;
        System.out.println("✅ Check-out successful. Thank you, " + b.guestName + "!");
        printBill(b);
    }

    // ── Feature 8: View All Bookings ──────────────────────────
    private void viewAllBookings() {
        if (bookings.isEmpty()) {
            System.out.println("No bookings found.");
            return;
        }
        System.out.println("══════════ ALL BOOKINGS ══════════");
        for (Booking b : bookings.values()) {
            System.out.printf("  [#%d] Room %-4d | %-20s | %s → %s | %-10s | Total: Rs.%s%n",
                b.id, b.roomNumber, b.guestName,
                b.checkIn.format(DTF), b.checkOut.format(DTF),
                b.status, b.total.toPlainString());
        }
    }

    // ── Print Booking Details ─────────────────────────────────
    private void printBookingDetails(Booking b) {
        System.out.println("──────────── BOOKING DETAILS ────────────");
        System.out.printf("  Booking ID   : #%d%n",            b.id);
        System.out.printf("  Guest Name   : %s%n",             b.guestName);
        System.out.printf("  Phone        : %s%n",             b.guestPhone);
        System.out.printf("  Room Number  : %d (%s)%n",        b.roomNumber, rooms.get(b.roomNumber).type);
        System.out.printf("  Check-In     : %s%n",             b.checkIn.format(DTF));
        System.out.printf("  Check-Out    : %s%n",             b.checkOut.format(DTF));
        System.out.printf("  Nights       : %d%n",             b.nights);
        System.out.printf("  Nightly Rate : Rs.%s%n",          b.nightlyRate.toPlainString());
        System.out.printf("  Room Charge  : Rs.%s%n",          b.roomCharge.toPlainString());
        System.out.printf("  Tax (10%%)    : Rs.%s%n",         b.tax.toPlainString());
        System.out.printf("  TOTAL        : Rs.%s%n",          b.total.toPlainString());
        System.out.printf("  Status       : %s%n",             b.status);
        System.out.println("──────────────────────────────────────────");
    }

    // ── Print Bill ────────────────────────────────────────────
    private void printBill(Booking b) {
        System.out.println();
        System.out.println("╔══════════ GRAND VISTA HOTEL ══════════╗");
        System.out.println("║              FINAL BILL                ║");
        System.out.println("╠════════════════════════════════════════╣");
        System.out.printf( "║  Booking ID  : %-23d ║%n", b.id);
        System.out.printf( "║  Guest       : %-23s ║%n", b.guestName);
        System.out.printf( "║  Room        : %-23s ║%n", b.roomNumber + " (" + rooms.get(b.roomNumber).type + ")");
        System.out.printf( "║  Check-In    : %-23s ║%n", b.checkIn.format(DTF));
        System.out.printf( "║  Check-Out   : %-23s ║%n", b.checkOut.format(DTF));
        System.out.printf( "║  Nights      : %-23d ║%n", b.nights);
        System.out.println("╠════════════════════════════════════════╣");
        System.out.printf( "║  Nightly Rate: Rs.%-20s ║%n", b.nightlyRate.toPlainString());
        System.out.printf( "║  Room Charge : Rs.%-20s ║%n", b.roomCharge.toPlainString());
        System.out.printf( "║  Tax (10%%)   : Rs.%-20s ║%n", b.tax.toPlainString());
        System.out.println("╠════════════════════════════════════════╣");
        System.out.printf( "║  TOTAL BILL  : Rs.%-20s ║%n", b.total.toPlainString());
        System.out.println("╚════════════════════════════════════════╝");
        System.out.println("  Thank you for staying with us! 🌟");
    }

    // ── Availability Check ────────────────────────────────────
    // INTERVIEW TIP: "Two date ranges overlap when:
    //                 aStart < bEnd AND bStart < aEnd
    //                 This is the standard interval overlap formula."
    private List<Room> getAvailableRooms(RoomType type, LocalDate checkIn, LocalDate checkOut) {
        List<Room> result = new ArrayList<>();
        outer:
        for (Room r : rooms.values()) {
            if (r.type != type || r.status == RoomStatus.OUT_OF_SERVICE) continue;
            for (Booking b : bookings.values()) {
                if (b.roomNumber == r.number
                    && b.status != BookingStatus.CANCELLED
                    && b.status != BookingStatus.COMPLETED
                    && b.checkIn.isBefore(checkOut)
                    && checkIn.isBefore(b.checkOut)) {
                    continue outer; // overlap — room not available
                }
            }
            result.add(r);
        }
        return result;
    }

    // ── Input Helpers ─────────────────────────────────────────
    private RoomType pickRoomType() {
        System.out.println("Select room type:");
        RoomType[] types = RoomType.values();
        for (int i = 0; i < types.length; i++) {
            System.out.printf("  %d. %-7s — Rs.%s/night%n",
                i + 1, types[i], types[i].nightlyRate().toPlainString());
        }
        return types[readInt("Choice", 1, types.length) - 1];
    }

    private boolean validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (!checkOut.isAfter(checkIn)) {
            System.out.println("❌ Check-out must be after check-in.");
            return false;
        }
        if (checkIn.isBefore(LocalDate.now())) {
            System.out.println("❌ Check-in cannot be in the past.");
            return false;
        }
        return true;
    }

    private int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print("  " + prompt + ": ");
            try {
                int v = Integer.parseInt(sc.nextLine().trim());
                if (v >= min && v <= max) return v;
                System.out.println("  Enter a number between " + min + " and " + max);
            } catch (NumberFormatException e) {
                System.out.println("  Enter a valid number.");
            }
        }
    }

    private int readInt(String prompt, List<Integer> allowed) {
        Set<Integer> set = new HashSet<>(allowed);
        while (true) {
            System.out.print("  " + prompt + ": ");
            try {
                int v = Integer.parseInt(sc.nextLine().trim());
                if (set.contains(v)) return v;
                System.out.println("  Choose from: " + allowed);
            } catch (NumberFormatException e) {
                System.out.println("  Enter a valid number.");
            }
        }
    }

    private String readNonEmpty(String prompt) {
        while (true) {
            System.out.print("  " + prompt + ": ");
            String s = sc.nextLine().trim();
            if (!s.isEmpty()) return s;
            System.out.println("  This field cannot be empty.");
        }
    }

    private LocalDate readDate(String prompt) {
        while (true) {
            System.out.print("  " + prompt + ": ");
            try {
                return LocalDate.parse(sc.nextLine().trim(), DTF);
            } catch (DateTimeParseException e) {
                System.out.println("  Use format: yyyy-MM-dd  e.g. 2025-12-25");
            }
        }
    }
}