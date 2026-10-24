import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Executors;

public class HostelRentalWebsite {

    static class Hostel {
        String id, name, area, type, phone, description;
        int minRent, maxRent;
        boolean approved;
        Hostel(String id, String name, String area, String type, int min, int max, String phone, String desc, boolean approved) {
            this.id=id; this.name=name; this.area=area; this.type=type;
            this.minRent=min; this.maxRent=max; this.phone=phone; this.description=desc; this.approved=approved;
        }
    }

    static final List<Hostel> HOSTELS = new ArrayList<>();
    static final Object DATA_LOCK = new Object();
    static final Path DATA_FILE = Paths.get("hostels.txt");
    static final String ADMIN_PASSWORD = "admin123";
    static final String SESSION_COOKIE = "admin_session";
    static final String SESSION_TOKEN = "nashik_secret_123";

    public static void main(String[] args) throws Exception {
        loadData();
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", 8080), 0);
        server.createContext("/", HostelRentalWebsite::handle);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("Hostel Website running -> http://localhost:8080");
        System.out.println("Admin login -> http://localhost:8080/admin (password: admin123)");
    }

    static void handle(HttpExchange ex) throws IOException {
        try {
            String path = ex.getRequestURI().getPath();
            String method = ex.getRequestMethod();
            Map<String, String> query = parseForm(ex.getRequestURI().getRawQuery());
            Map<String, String> form = new HashMap<>();
            if ("POST".equals(method)) {
                String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                form = parseForm(body);
            }

            if ("/".equals(path)) {
                if ("GET".equals(method)) sendHtml(ex, 200, home(query));
            } else if ("/owner".equals(path)) {
                sendHtml(ex, 200, ownerPage(""));
            } else if ("/submit".equals(path) && "POST".equals(method)) {
                submitHostel(ex, form);
            } else if ("/admin".equals(path)) {
                if (!isAdmin(ex)) sendHtml(ex, 200, loginPage(""));
                else sendHtml(ex, 200, adminPage(""));
            } else if ("/admin-login".equals(path) && "POST".equals(method)) {
                adminLogin(ex, form);
            } else if ("/admin-action".equals(path) && "POST".equals(method)) {
                adminAction(ex, form);
            } else if ("/logout".equals(path)) {
                ex.getResponseHeaders().add("Set-Cookie", SESSION_COOKIE + "=; Path=/; Max-Age=0");
                redirect(ex, "/");
            } else {
                sendHtml(ex, 404, page("Not Found", "<div class='card'><h2>Not Found</h2><a class='btn' href='/'>Home</a></div>"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            try { sendHtml(ex, 500, page("Error", "<div class='card'><h2>Error: "+e(e.toString())+"</h2></div>")); } catch(Exception ignored){}
        } finally { ex.close(); }
    }

    // ===== Tujha code ithe pasun same — mi connect kelay =====

    private static void submitHostel(HttpExchange ex, Map<String, String> f) throws IOException {
        String name = clean(f.get("name"), 100);
        String area = clean(f.get("area"), 80);
        String type = clean(f.get("type"), 20);
        String phone = clean(f.get("phone"), 20).replaceAll("[^0-9+]", "");
        String desc = clean(f.get("description"), 1000);
        int min = parseInt(f.get("minRent"), -1);
        int max = parseInt(f.get("maxRent"), -1);

        if (name.isBlank() || area.isBlank()
                || phone.replaceAll("\\D", "").length() < 10
                || min < 0 || max < min || max > 100000
                || !Set.of("Boys", "Girls", "Co-ed").contains(type)) {
            sendHtml(ex, 400, ownerPage("Please enter valid details."));
            return;
        }
        Hostel h = new Hostel(UUID.randomUUID().toString(), name, area, type, min, max, phone, desc, false);
        synchronized (DATA_LOCK) { HOSTELS.add(h); saveData(); }
        sendHtml(ex, 200, page("Submitted","<section class='card'><h2>Listing submitted!</h2><p>Waiting for admin approval.</p><a class='btn' href='/'>Back</a></section>"));
    }

    private static void adminLogin(HttpExchange ex, Map<String, String> f) throws IOException {
        if (constantTimeEquals(ADMIN_PASSWORD, f.getOrDefault("password", ""))) {
            ex.getResponseHeaders().add("Set-Cookie", SESSION_COOKIE + "=" + SESSION_TOKEN + "; Path=/; HttpOnly; SameSite=Strict");
            redirect(ex, "/admin");
        } else { sendHtml(ex, 401, loginPage("Incorrect password.")); }
    }

    private static void adminAction(HttpExchange ex, Map<String, String> f) throws IOException {
        if (!isAdmin(ex)) { sendHtml(ex, 403, loginPage("Please log in as admin first.")); return; }
        String id = f.getOrDefault("id", ""); String action = f.getOrDefault("action", "");
        synchronized (DATA_LOCK) {
            Iterator<Hostel> it = HOSTELS.iterator();
            while (it.hasNext()) {
                Hostel h = it.next();
                if (h.id.equals(id)) {
                    if ("approve".equals(action)) h.approved = true;
                    else if ("hide".equals(action)) h.approved = false;
                    else if ("delete".equals(action)) it.remove();
                    break;
                }
            }
            saveData();
        }
        redirect(ex, "/admin");
    }

    private static boolean isAdmin(HttpExchange ex) {
        String cookie = ex.getRequestHeaders().getFirst("Cookie");
        if (cookie == null) return false;
        for (String item : cookie.split(";")) {
            String[] pair = item.trim().split("=", 2);
            if (pair.length == 2 && pair[0].equals(SESSION_COOKIE) && pair[1].equals(SESSION_TOKEN)) return true;
        }
        return false;
    }

    private static String home(Map<String, String> f) {
        String q = clean(f.get("q"), 80); String area = clean(f.get("area"), 80); String type = clean(f.get("type"), 20); int budget = parseInt(f.get("budget"), 100000);
        StringBuilder cards = new StringBuilder(); int count = 0;
        synchronized (DATA_LOCK) {
            for (Hostel h : HOSTELS) {
                if (!h.approved) continue;
                if (!q.isBlank() && !(h.name + " " + h.area + " " + h.description).toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT))) continue;
                if (!area.isBlank() && !h.area.equalsIgnoreCase(area)) continue;
                if (!type.isBlank() && !h.type.equals(type)) continue;
                if (budget >= 0 && h.minRent > budget) continue;
                cards.append(hostelCard(h)); count++;
            }
        }
        if (count == 0) cards.append("<div class='card'><h3>No hostels found</h3><p>Try changing filters.</p></div>");
        return page("Find a hostel in Nashik",
                "<section class='hero'><h1>Find your hostel in Nashik</h1><p>Search by area, budget and type.</p><a class='btn' href='/owner'>List your hostel</a></section>"
                + "<section class='card'><h2>Search hostels</h2><form method='get' action='/'><label>Search</label><input name='q' value='" + e(q) + "'><label>Area</label><input name='area' value='" + e(area) + "'><label>Type</label><select name='type'><option value=''>All</option>"+typeOption("Boys",type)+typeOption("Girls",type)+typeOption("Co-ed",type)+"</select><label>Max rent</label><input name='budget' type='number' value='"+budget+"'><button class='btn' type='submit'>Search</button> <a class='btn secondary' href='/'>Reset</a></form></section>"
                + "<h2>Available hostels</h2><p>"+count+" found</p><div class='cards'>"+cards+"</div>");
    }

    private static String typeOption(String type, String selected) { return "<option value='"+type+"'"+(type.equals(selected)?" selected":"")+">"+type+"</option>"; }
    private static String hostelCard(Hostel h) {
        String digits = h.phone.replaceAll("\\D", ""); String wa = digits.length()==10?"91"+digits:digits;
        return "<article class='card'><span class='pill'>"+e(h.type)+"</span><h3>"+e(h.name)+"</h3><p>Location: "+e(h.area)+"</p><p class='rent'>\u20B9"+h.minRent+" - \u20B9"+h.maxRent+" / month</p><p>"+e(h.description)+"</p><div class='actions'><a class='btn' href='tel:"+e(h.phone)+"'>Call</a><a class='btn whatsapp' target='_blank' href='https://wa.me/"+e(wa)+"'>WhatsApp</a></div></article>";
    }
    private static String ownerPage(String error) { return page("Register hostel","<section class='card'><h1>Register hostel</h1>"+(error.isBlank()?"":"<p class='error'>"+e(error)+"</p>")+"<form method='post' action='/submit'><label>Hostel name</label><input name='name' required><label>Area</label><input name='area' required><label>Type</label><select name='type'><option>Boys</option><option>Girls</option><option>Co-ed</option></select><label>Min rent</label><input name='minRent' type='number' required><label>Max rent</label><input name='maxRent' type='number' required><label>Phone</label><input name='phone' required><label>Description</label><textarea name='description'></textarea><button class='btn' type='submit'>Submit</button></form></section>"); }
    private static String loginPage(String error) { return page("Admin login","<section class='card'><h1>Admin login</h1>"+(error.isBlank()?"":"<p class='error'>"+e(error)+"</p>")+"<form method='post' action='/admin-login'><label>Password</label><input type='password' name='password' required><button class='btn' type='submit'>Log in</button></form></section>"); }
    private static String adminPage(String message) {
        StringBuilder rows = new StringBuilder(); synchronized (DATA_LOCK) { if (HOSTELS.isEmpty()) rows.append("<p>No listings</p>"); for (Hostel h : HOSTELS) { rows.append("<article class='card'><h3>").append(e(h.name)).append("</h3><p>").append(e(h.area)).append(" · ").append(e(h.type)).append(" · \u20B9").append(h.minRent).append("-\u20B9").append(h.maxRent).append("</p><p>Phone: ").append(e(h.phone)).append("</p><p>").append(e(h.description)).append("</p><div class='actions'>"); if (!h.approved) rows.append(actionForm(h.id,"approve","Approve")); else rows.append(actionForm(h.id,"hide","Hide")); rows.append(actionForm(h.id,"delete","Delete")).append("</div></article>"); } }
        return page("Admin","<section class='hero'><h1>Admin dashboard</h1><form method='post' action='/logout'><button class='btn'>Log out</button></form></section><div class='cards'>"+rows+"</div>");
    }
    private static String actionForm(String id, String action, String label) { return "<form class='inline' method='post' action='/admin-action'><input type='hidden' name='id' value='"+e(id)+"'><input type='hidden' name='action' value='"+e(action)+"'><button class='btn' type='submit'>"+label+"</button></form>"; }
    private static String page(String title, String body) { return "<!doctype html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width, initial-scale=1'><title>"+e(title)+"</title><style>body{font-family:Arial;background:#f4f7fb;margin:0}header{background:#123c69;color:white;padding:18px;display:flex;justify-content:space-between}header a{color:white;text-decoration:none;margin-right:12px}main{max-width:1000px;margin:24px auto;padding:0 16px}.hero{background:#dcecff;border-radius:16px;padding:24px;margin-bottom:20px}.card{background:white;border:1px solid #e1e7ef;border-radius:14px;padding:20px;margin-bottom:16px}.cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:16px}label{display:block;font-weight:bold;margin:12px 0 6px}input,select,textarea{width:100%;padding:11px;border:1px solid #bbc7d8;border-radius:8px}.btn{background:#145da0;color:white;padding:10px 14px;border:0;border-radius:8px;text-decoration:none;cursor:pointer;margin-top:12px}.secondary{background:#65758b}.whatsapp{background:#16864a}.pill{background:#e8f1ff;padding:5px 9px;border-radius:20px}.rent{font-weight:bold;color:#145da0}.error{color:#b42318}.inline{display:inline-block}footer{text-align:center;padding:24px;color:#68758a}</style></head><body><header><a href='/'>Nashik Hostel Finder</a><nav><a href='/'>Search</a><a href='/owner'>List hostel</a><a href='/admin'>Admin</a></nav></header><main>"+body+"</main><footer>Verify details before paying.</footer></body></html>"; }

    private static Map<String, String> parseForm(String raw) { Map<String,String> map=new HashMap<>(); if(raw==null||raw.isBlank()) return map; for(String part:raw.split("&")){ String[] pair=part.split("=",2); String key=decode(pair[0]); String value=pair.length>1?decode(pair[1]):""; map.put(key,value);} return map; }
    private static String decode(String s){ try{return URLDecoder.decode(s,StandardCharsets.UTF_8);}catch(Exception ex){return "";}}
    private static String clean(String value,int max){ if(value==null) return ""; String s=value.trim(); return s.length()>max?s.substring(0,max):s; }
    private static int parseInt(String s,int fallback){ try{return Integer.parseInt(s);}catch(Exception ex){return fallback;}}
    private static String e(String s){ if(s==null) return ""; return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;"); }
    private static boolean constantTimeEquals(String a,String b){ return java.security.MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8)); }
    private static void sendHtml(HttpExchange ex,int status,String html) throws IOException { byte[] bytes=html.getBytes(StandardCharsets.UTF_8); ex.getResponseHeaders().set("Content-Type","text/html; charset=UTF-8"); ex.sendResponseHeaders(status,bytes.length); ex.getResponseBody().write(bytes); }
    private static void redirect(HttpExchange ex,String location) throws IOException { ex.getResponseHeaders().set("Location",location); ex.sendResponseHeaders(303,-1); }
    private static String enc(String s){ return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8)); }
    private static String dec(String s){ return new String(Base64.getDecoder().decode(s),StandardCharsets.UTF_8); }
    private static void loadData(){ synchronized(DATA_LOCK){ HOSTELS.clear(); if(!Files.exists(DATA_FILE)) return; try{for(String line:Files.readAllLines(DATA_FILE,StandardCharsets.UTF_8)){ try{String[] p=line.split("\t",-1); if(p.length!=9) continue; HOSTELS.add(new Hostel(dec(p[0]),dec(p[1]),dec(p[2]),dec(p[3]),Integer.parseInt(p[4]),Integer.parseInt(p[5]),dec(p[6]),dec(p[7]),Boolean.parseBoolean(p[8])));}catch(Exception ignored){}}}catch(IOException ex){System.err.println("Load error: "+ex.getMessage());}}}
    private static void saveData(){ List<String> lines=new ArrayList<>(); for(Hostel h:HOSTELS){ lines.add(String.join("\t",enc(h.id),enc(h.name),enc(h.area),enc(h.type),String.valueOf(h.minRent),String.valueOf(h.maxRent),enc(h.phone),enc(h.description),String.valueOf(h.approved))); } try{Files.write(DATA_FILE,lines,StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE);}catch(IOException ex){System.err.println("Save error: "+ex.getMessage());}}
}