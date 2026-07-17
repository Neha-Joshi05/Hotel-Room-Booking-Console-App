# ============================================================
# hotel_model.py — Python mirror of Java HotelApp logic
# PURPOSE: Powers the Flask web dashboard with same
#          booking rules as the Java console app.
# INTERVIEW TIP: "Same business logic — different language.
#                 This is the Model layer of MVC pattern."
# ============================================================

from datetime import date, datetime
from decimal import Decimal, ROUND_HALF_UP
from typing import Optional

# ── Room Types ────────────────────────────────────────────────
ROOM_TYPES = {
    "SINGLE": {"label": "Single",  "base": Decimal("2000.00"), "multiplier": Decimal("1.0"), "capacity": 1, "icon": "🛏️"},
    "DOUBLE": {"label": "Double",  "base": Decimal("2000.00"), "multiplier": Decimal("1.2"), "capacity": 2, "icon": "🛏️🛏️"},
    "DELUXE": {"label": "Deluxe",  "base": Decimal("2000.00"), "multiplier": Decimal("1.4"), "capacity": 2, "icon": "✨"},
    "SUITE":  {"label": "Suite",   "base": Decimal("2000.00"), "multiplier": Decimal("1.6"), "capacity": 4, "icon": "👑"},
}

TAX_RATE = Decimal("0.10")


def nightly_rate(room_type: str) -> Decimal:
    rt = ROOM_TYPES[room_type]
    return (rt["base"] * rt["multiplier"]).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)


# ── Room data (mirrors Java seedRooms) ────────────────────────
ROOMS = {
    101: {"number": 101, "type": "SINGLE", "capacity": 1, "status": "VACANT"},
    102: {"number": 102, "type": "SINGLE", "capacity": 1, "status": "VACANT"},
    103: {"number": 103, "type": "SINGLE", "capacity": 1, "status": "VACANT"},
    201: {"number": 201, "type": "DOUBLE", "capacity": 2, "status": "VACANT"},
    202: {"number": 202, "type": "DOUBLE", "capacity": 2, "status": "VACANT"},
    203: {"number": 203, "type": "DOUBLE", "capacity": 2, "status": "VACANT"},
    301: {"number": 301, "type": "DELUXE", "capacity": 2, "status": "VACANT"},
    302: {"number": 302, "type": "DELUXE", "capacity": 2, "status": "VACANT"},
    401: {"number": 401, "type": "SUITE",  "capacity": 4, "status": "VACANT"},
    402: {"number": 402, "type": "SUITE",  "capacity": 4, "status": "VACANT"},
}

# In-memory bookings store
BOOKINGS = {}
_next_id  = [1001]


def _ranges_overlap(a_start: date, a_end: date, b_start: date, b_end: date) -> bool:
    """Standard interval overlap: aStart < bEnd AND bStart < aEnd"""
    return a_start < b_end and b_start < a_end


def get_available_rooms(room_type: str, check_in: date, check_out: date) -> list:
    """Returns list of available rooms for given type and dates."""
    result = []
    for room in ROOMS.values():
        if room["type"] != room_type:
            continue
        if room["status"] == "OUT_OF_SERVICE":
            continue
        # Check for overlapping bookings
        occupied = False
        for b in BOOKINGS.values():
            if (b["room_number"] == room["number"]
                    and b["status"] not in ("CANCELLED", "COMPLETED")
                    and _ranges_overlap(check_in, check_out,
                                        b["check_in"], b["check_out"])):
                occupied = True
                break
        if not occupied:
            result.append(room)
    return result


def create_booking(room_number: int, guest_name: str, guest_phone: str,
                   check_in: date, check_out: date) -> dict:
    """Creates a new booking and returns it."""
    room   = ROOMS[room_number]
    rate   = nightly_rate(room["type"])
    nights = (check_out - check_in).days

    room_charge = (rate * nights).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
    tax         = (room_charge * TAX_RATE).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
    total       = (room_charge + tax).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)

    bid = _next_id[0]
    _next_id[0] += 1

    booking = {
        "id":           bid,
        "room_number":  room_number,
        "room_type":    room["type"],
        "guest_name":   guest_name,
        "guest_phone":  guest_phone,
        "check_in":     check_in,
        "check_out":    check_out,
        "nights":       nights,
        "nightly_rate": float(rate),
        "room_charge":  float(room_charge),
        "tax":          float(tax),
        "total":        float(total),
        "status":       "RESERVED",
    }
    BOOKINGS[bid] = booking
    return booking


def cancel_booking(booking_id: int) -> dict:
    b = BOOKINGS.get(booking_id)
    if not b:
        raise ValueError(f"Booking #{booking_id} not found")
    if b["status"] != "RESERVED":
        raise ValueError(f"Only RESERVED bookings can be cancelled. Status: {b['status']}")
    b["status"] = "CANCELLED"
    return b


def checkin_booking(booking_id: int) -> dict:
    b = BOOKINGS.get(booking_id)
    if not b:
        raise ValueError(f"Booking #{booking_id} not found")
    if b["status"] != "RESERVED":
        raise ValueError(f"Only RESERVED bookings can check in. Status: {b['status']}")
    b["status"] = "IN_HOUSE"
    ROOMS[b["room_number"]]["status"] = "OCCUPIED"
    return b


def checkout_booking(booking_id: int) -> dict:
    b = BOOKINGS.get(booking_id)
    if not b:
        raise ValueError(f"Booking #{booking_id} not found")
    if b["status"] != "IN_HOUSE":
        raise ValueError(f"Only IN_HOUSE bookings can check out. Status: {b['status']}")
    b["status"] = "COMPLETED"
    ROOMS[b["room_number"]]["status"] = "VACANT"
    return b


def get_all_rooms() -> list:
    result = []
    for room in ROOMS.values():
        rt = ROOM_TYPES[room["type"]]
        result.append({
            **room,
            "label":       rt["label"],
            "icon":        rt["icon"],
            "nightly_rate": float(nightly_rate(room["type"])),
        })
    return result


def get_all_bookings() -> list:
    result = []
    for b in BOOKINGS.values():
        result.append({
            **b,
            "check_in":  b["check_in"].strftime("%Y-%m-%d"),
            "check_out": b["check_out"].strftime("%Y-%m-%d"),
        })
    return sorted(result, key=lambda x: x["id"], reverse=True)