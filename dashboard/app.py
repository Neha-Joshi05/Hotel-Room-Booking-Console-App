# ============================================================
# app.py — Flask Web Dashboard for Hotel Booking
# HOW TO RUN: python dashboard/app.py
# Open: http://localhost:5000
# ============================================================

import sys
import os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from flask import Flask, render_template, request, jsonify
from hotel_model import (
    get_available_rooms, create_booking, cancel_booking,
    checkin_booking, checkout_booking, get_all_rooms,
    get_all_bookings, BOOKINGS, ROOM_TYPES, nightly_rate
)
from datetime import date, datetime

app = Flask(__name__)


@app.route("/")
def index():
    return render_template("index.html", room_types=ROOM_TYPES)


@app.route("/api/rooms")
def api_rooms():
    return jsonify(get_all_rooms())


@app.route("/api/rooms/available", methods=["POST"])
def api_available():
    data = request.get_json()
    try:
        room_type = data["room_type"]
        check_in  = datetime.strptime(data["check_in"],  "%Y-%m-%d").date()
        check_out = datetime.strptime(data["check_out"], "%Y-%m-%d").date()
        if check_out <= check_in:
            return jsonify({"error": "Check-out must be after check-in"}), 400
        if check_in < date.today():
            return jsonify({"error": "Check-in cannot be in the past"}), 400
        rooms = get_available_rooms(room_type, check_in, check_out)
        rt    = ROOM_TYPES[room_type]
        for r in rooms:
            r["nightly_rate"] = float(nightly_rate(room_type))
            r["label"]        = rt["label"]
            r["icon"]         = rt["icon"]
        return jsonify({"rooms": rooms, "count": len(rooms)})
    except Exception as e:
        return jsonify({"error": str(e)}), 400


@app.route("/api/bookings", methods=["POST"])
def api_create_booking():
    data = request.get_json()
    try:
        room_number  = int(data["room_number"])
        guest_name   = data["guest_name"].strip()
        guest_phone  = data["guest_phone"].strip()
        check_in     = datetime.strptime(data["check_in"],  "%Y-%m-%d").date()
        check_out    = datetime.strptime(data["check_out"], "%Y-%m-%d").date()

        if not guest_name:
            return jsonify({"error": "Guest name is required"}), 400
        if not guest_phone:
            return jsonify({"error": "Phone number is required"}), 400
        if check_out <= check_in:
            return jsonify({"error": "Check-out must be after check-in"}), 400

        booking = create_booking(room_number, guest_name, guest_phone, check_in, check_out)
        b = dict(booking)
        b["check_in"]  = b["check_in"].strftime("%Y-%m-%d")
        b["check_out"] = b["check_out"].strftime("%Y-%m-%d")
        return jsonify({"success": True, "booking": b})
    except Exception as e:
        return jsonify({"error": str(e)}), 400


@app.route("/api/bookings", methods=["GET"])
def api_get_bookings():
    return jsonify(get_all_bookings())


@app.route("/api/bookings/<int:bid>/cancel", methods=["POST"])
def api_cancel(bid):
    try:
        b = cancel_booking(bid)
        b = dict(b)
        b["check_in"]  = b["check_in"].strftime("%Y-%m-%d")
        b["check_out"] = b["check_out"].strftime("%Y-%m-%d")
        return jsonify({"success": True, "booking": b})
    except ValueError as e:
        return jsonify({"error": str(e)}), 400


@app.route("/api/bookings/<int:bid>/checkin", methods=["POST"])
def api_checkin(bid):
    try:
        b = checkin_booking(bid)
        b = dict(b)
        b["check_in"]  = b["check_in"].strftime("%Y-%m-%d")
        b["check_out"] = b["check_out"].strftime("%Y-%m-%d")
        return jsonify({"success": True, "booking": b})
    except ValueError as e:
        return jsonify({"error": str(e)}), 400


@app.route("/api/bookings/<int:bid>/checkout", methods=["POST"])
def api_checkout(bid):
    try:
        b = checkout_booking(bid)
        b = dict(b)
        b["check_in"]  = b["check_in"].strftime("%Y-%m-%d")
        b["check_out"] = b["check_out"].strftime("%Y-%m-%d")
        return jsonify({"success": True, "booking": b})
    except ValueError as e:
        return jsonify({"error": str(e)}), 400


if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5000))
    app.run(host="0.0.0.0", port=port, debug=False)