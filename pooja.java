
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class pooja {

    private static final int PORT = 8082;

    static class Hostel {
        String name;
        String location;
        int rent;
        String roomType;
        String facilities;

        Hostel(String name, String location, int rent,
               String roomType, String facilities) {
            this.name = name;
            this.location = location;
            this.rent = rent;
            this.roomType = roomType;
            this.facilities = facilities;
        }
    }

    private static final List<Hostel> hostels = new ArrayList<>();

    public static void main(String[] args) throws IOException {

        hostels.add(new Hostel(
                "Ashokpushp Hostel",
                "Chandwad, Maharashtra",
                4500,
                "Sharing",
                "Wi-Fi, Food, Water"
        ));

        hostels.add(new Hostel(
                "Sundarkashi Hostel",
                "Chandwad, Maharashtra",
                5000,
                "Single",
                "Wi-Fi, Security, Food"
        ));

        hostels.add(new Hostel(
                "Shivalay Niwas",
                "Chandwad, Maharashtra",
                3500,
                "Sharing",
                "Water, Food, Parking"
        ));

        hostels.add(new Hostel(
                "Mhaske Niwas",
                "Chandwad, Maharashtra",
                4000,
                "Sharing",
                "Wi-Fi, Water, Security"
        ));

        HttpServer server = HttpServer.create(
                new InetSocketAddress("0.0.0.0", PORT), 0
        );

        // Class name pooja असल्यामुळे pooja::handleHome वापरले आहे.
        server.createContext("/", pooja::handleHome);

        server.start();

        System.out.println("Hostel Finder Website Started!");
        System.out.println("Open: http://localhost:" + PORT);
    }

    private static void handleHome(HttpExchange exchange)
            throws IOException {

        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            byte[] error = "Method Not Allowed"
                    .getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders()
                    .set("Content-Type", "text/plain; charset=UTF-8");

            exchange.sendResponseHeaders(405, error.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(error);
            }

            return;
        }

        String html = htmlPage();

        byte[] response = html.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type", "text/html; charset=UTF-8"
        );

        exchange.sendResponseHeaders(200, response.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    private static String htmlPage() {

        StringBuilder cards = new StringBuilder();

        for (Hostel h : hostels) {

            cards.append("<article class='card' data-rent='")
                 .append(h.rent)
                 .append("' data-room='")
                 .append(escape(h.roomType))
                 .append("'>");

            cards.append("<div class='card-icon'>🏠</div>");

            cards.append("<h3>")
                 .append(escape(h.name))
                 .append("</h3>");

            cards.append("<p class='location'>📍 ")
                 .append(escape(h.location))
                 .append("</p>");

            cards.append("<p class='rent'>₹")
                 .append(h.rent)
                 .append(" <span>/ month</span></p>");

            cards.append("<p><b>Room:</b> ")
                 .append(escape(h.roomType))
                 .append("</p>");

            cards.append("<p><b>Facilities:</b> ")
                 .append(escape(h.facilities))
                 .append("</p>");

            cards.append("<button class='enquiry' onclick=\"enquire('")
                 .append(escape(h.name))
                 .append("')\">Enquire Now</button>");

            cards.append("</article>");
        }

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport"
                          content="width=device-width, initial-scale=1.0">

                    <title>Hostel Finder | Chandwad</title>

                    <style>
                        * {
                            box-sizing: border-box;
                        }

                        body {
                            margin: 0;
                            font-family: Arial, sans-serif;
                            background: #f5f6fa;
                            color: #222;
                        }

                        header {
                            background: linear-gradient(135deg, #6c3483, #a569bd);
                            color: white;
                            padding: 28px 15px;
                            text-align: center;
                        }

                        header h1 {
                            margin: 0 0 10px;
                            font-size: 34px;
                        }

                        header p {
                            margin: 0;
                            font-size: 16px;
                        }

                        nav {
                            background: white;
                            padding: 14px;
                            text-align: center;
                            box-shadow: 0 2px 5px #ddd;
                        }

                        nav a {
                            margin: 0 12px;
                            color: #6c3483;
                            text-decoration: none;
                            font-weight: bold;
                        }

                        .container {
                            max-width: 1100px;
                            margin: 25px auto;
                            padding: 0 15px;
                        }

                        .search-box {
                            background: white;
                            padding: 20px;
                            border-radius: 12px;
                            margin-bottom: 25px;
                            box-shadow: 0 3px 12px #ddd;
                        }

                        .search-box h2 {
                            margin-top: 0;
                        }

                        .filters {
                            display: flex;
                            flex-wrap: wrap;
                            gap: 12px;
                        }

                        input, select {
                            flex: 1;
                            min-width: 180px;
                            padding: 12px;
                            border: 1px solid #ccc;
                            border-radius: 7px;
                            font-size: 15px;
                        }

                        .hostel-grid {
                            display: grid;
                            grid-template-columns:
                                repeat(auto-fit, minmax(240px, 1fr));
                            gap: 20px;
                        }

                        .card {
                            background: white;
                            padding: 20px;
                            border-radius: 12px;
                            box-shadow: 0 3px 12px #ddd;
                            transition: transform 0.2s;
                        }

                        .card:hover {
                            transform: translateY(-4px);
                        }

                        .card-icon {
                            font-size: 40px;
                        }

                        .card h3 {
                            color: #6c3483;
                            margin-bottom: 8px;
                        }

                        .location {
                            color: #666;
                        }

                        .rent {
                            font-size: 25px;
                            font-weight: bold;
                            color: #198754;
                        }

                        .rent span {
                            font-size: 13px;
                            font-weight: normal;
                            color: #666;
                        }

                        .enquiry {
                            width: 100%;
                            padding: 12px;
                            border: none;
                            border-radius: 7px;
                            background: #6c3483;
                            color: white;
                            font-size: 15px;
                            cursor: pointer;
                            margin-top: 10px;
                        }

                        .enquiry:hover {
                            background: #512e5f;
                        }

                        #noResults {
                            display: none;
                            text-align: center;
                            padding: 25px;
                            color: #666;
                        }

                        footer {
                            margin-top: 40px;
                            background: #30213a;
                            color: white;
                            text-align: center;
                            padding: 20px;
                        }

                        @media (max-width: 600px) {
                            header h1 {
                                font-size: 27px;
                            }

                            nav a {
                                display: inline-block;
                                margin: 6px;
                            }
                        }
                    </style>
                </head>

                <body>

                    <header>
                        <h1>🏠 Hostel Finder</h1>
                        <p>Find Your Perfect Hostel in Chandwad</p>
                    </header>

                    <nav>
                        <a href="#home">Home</a>
                        <a href="#hostels">Hostels</a>
                        <a href="#contact">Contact</a>
                    </nav>

                    <main class="container" id="home">

                        <section class="search-box">
                            <h2>🔍 Search Hostels</h2>

                            <div class="filters">
                                <input
                                    type="text"
                                    id="search"
                                    placeholder="Search hostel name or location...">

                                <select id="room">
                                    <option value="all">All Room Types</option>
                                    <option value="Single">Single Room</option>
                                    <option value="Sharing">Sharing Room</option>
                                </select>

                                <select id="rent">
                                    <option value="all">Any Budget</option>
                                    <option value="3500">Up to ₹3,500</option>
                                    <option value="4000">Up to ₹4,000</option>
                                    <option value="4500">Up to ₹4,500</option>
                                    <option value="5000">Up to ₹5,000</option>
                                </select>
                            </div>
                        </section>

                        <section id="hostels">
                            <h2>Available Hostels</h2>

                            <div class="hostel-grid" id="hostelGrid">
                                __HOSTEL_CARDS__
                            </div>

                            <p id="noResults">
                                No hostels found. Try different filters.
                            </p>
                        </section>

                    </main>

                    <footer id="contact">
                        <h3>Hostel Finder</h3>
                        <p>Find a hostel that feels like home.</p>
                        <p>Chandwad, Maharashtra</p>
                        <p>© 2026 Hostel Finder</p>
                    </footer>

                    <script>
                        const searchInput = document.getElementById("search");
                        const roomSelect = document.getElementById("room");
                        const rentSelect = document.getElementById("rent");
                        const cards = document.querySelectorAll(".card");
                        const noResults = document.getElementById("noResults");

                        function filterHostels() {
                            const searchText = searchInput.value.toLowerCase();
                            const selectedRoom = roomSelect.value;
                            const maxRent = rentSelect.value;

                            let visibleCount = 0;

                            cards.forEach(card => {
                                const text = card.innerText.toLowerCase();
                                const cardRoom = card.dataset.room;
                                const cardRent = Number(card.dataset.rent);

                                const matchesSearch =
                                    text.includes(searchText);

                                const matchesRoom =
                                    selectedRoom === "all" ||
                                    cardRoom === selectedRoom;

                                const matchesRent =
                                    maxRent === "all" ||
                                    cardRent <= Number(maxRent);

                                const show =
                                    matchesSearch &&
                                    matchesRoom &&
                                    matchesRent;

                                card.style.display = show ? "block" : "none";

                                if (show) {
                                    visibleCount++;
                                }
                            });

                            noResults.style.display =
                                visibleCount === 0 ? "block" : "none";
                        }

                        searchInput.addEventListener("input", filterHostels);
                        roomSelect.addEventListener("change", filterHostels);
                        rentSelect.addEventListener("change", filterHostels);

                        function enquire(hostelName) {
                            alert(
                                "You selected " + hostelName +
                                ". Please add the owner's contact number " +
                                "to enable real enquiries."
                            );
                        }
                    </script>

                </body>
                </html>
                """.replace("__HOSTEL_CARDS__", cards.toString());
    }
}
