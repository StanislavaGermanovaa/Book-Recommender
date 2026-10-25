package fmi.uni.masters.gui;

import com.sun.net.httpserver.HttpServer;
import fmi.uni.masters.agent.BookRecommenderAgent;
import fmi.uni.masters.BookInfo;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class AdminWebServer {
    public static void start(BookRecommenderAgent agent) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);

            server.createContext("/api/admin/createBook", exchange -> {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                try {
                    String query = exchange.getRequestURI().getQuery();
                    String id = "", format = "";
                    if (query != null) {
                        for (String param : query.split("&")) {
                            String[] pair = param.split("=");
                            if (pair.length == 2) {
                                if (pair[0].equals("id")) id = pair[1];
                                if (pair[0].equals("format")) format = pair[1];
                            }
                        }
                    }

                    String res;
                    if (agent.checkBookExists(id)) {
                        res = "{\"success\": false, \"message\": \"Book already exists!\"}";
                    } else {
                        agent.addBook(id, format);
                        res = "{\"success\": true}";
                    }

                    byte[] bytes = res.getBytes("UTF-8");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
                } catch (Exception e) {
                    e.printStackTrace();
                    exchange.sendResponseHeaders(500, -1);
                }
            });

            server.createContext("/api/admin/link", exchange -> {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                try {
                    String query = exchange.getRequestURI().getQuery();
                    String type = "", target = "", val = "";
                    if (query != null) {
                        for (String param : query.split("&")) {
                            String[] pair = param.split("=");
                            if (pair.length == 2) {
                                if (pair[0].equals("type")) type = pair[1];
                                if (pair[0].equals("target")) target = pair[1];
                                if (pair[0].equals("val")) val = pair[1];
                            }
                        }
                    }

                    if (type.equals("year")) {
                        int year = Integer.parseInt(val);
                        agent.addBookPublicationYear(target, year);
                    } else if (type.equals("order")) {
                        int order = Integer.parseInt(val);
                        agent.addBookSeriesOrder(target, order);
                    } else {
                        String propName = "";
                        if (type.equals("author")) propName = "writtenBy";
                        else if (type.equals("genre")) propName = "hasGenre";
                        else if (type.equals("rating")) propName = "hasRating";
                        else if (type.equals("publisher")) propName = "publishedBy";
                        else if (type.equals("series")) propName = "isPartOfSeries";
                        else if (type.equals("similar")) propName = "isSimilarTo";

                        agent.addPropertyToBook(target, val, propName);
                    }

                    String res = "{\"success\": true}";
                    byte[] bytes = res.getBytes("UTF-8");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
                } catch (Exception e) {
                    e.printStackTrace();
                    String res = "{\"success\": false}";
                    try {
                        byte[] bytes = res.getBytes("UTF-8");
                        exchange.sendResponseHeaders(200, bytes.length);
                        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
                    } catch (Exception ex) {
                        exchange.sendResponseHeaders(500, -1);
                    }
                }
            });

            server.createContext("/api/admin/delete", exchange -> {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                try {
                    String query = exchange.getRequestURI().getQuery();
                    String bookName = "";
                    if (query != null && query.startsWith("target=")) bookName = query.split("=")[1];

                    agent.deleteBookFromSystem(bookName);

                    String res = "{\"success\": true}";
                    exchange.sendResponseHeaders(200, res.getBytes("UTF-8").length);
                    try (OutputStream os = exchange.getResponseBody()) { os.write(res.getBytes("UTF-8")); }
                } catch (Exception e) {
                    e.printStackTrace();
                    exchange.sendResponseHeaders(500, -1);
                }
            });

            server.createContext("/api/admin/dump", exchange -> {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
                try {
                    List<String> bookNames = agent.getAllBooksNames();
                    List<String> formattedBooks = new ArrayList<>();

                    for (String bookName : bookNames) {
                        BookInfo info = agent.getBookDetails(bookName);
                        if (info != null) {
                            formattedBooks.add(info.toPrettyString());
                        }
                    }

                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    String jsonResponse = mapper.writeValueAsString(formattedBooks);

                    byte[] bytes = jsonResponse.getBytes("UTF-8");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
                } catch (Exception e) {
                    e.printStackTrace();
                    exchange.sendResponseHeaders(500, -1);
                }
            });

            server.createContext("/", exchange -> {
                String path = exchange.getRequestURI().getPath();
                if (path.equals("/")) path = "/admin.html";
                String resourcePath = "/gui" + path;

                try (InputStream is = AdminWebServer.class.getResourceAsStream(resourcePath)) {
                    if (is != null) {
                        if (path.endsWith(".html")) exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
                        else if (path.endsWith(".css")) exchange.getResponseHeaders().add("Content-Type", "text/css; charset=UTF-8");

                        byte[] bytes = is.readAllBytes();
                        exchange.sendResponseHeaders(200, bytes.length);
                        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
                    } else {
                        exchange.sendResponseHeaders(404, -1);
                    }
                } catch (Exception e) { exchange.sendResponseHeaders(500, -1); }
            });

            server.start();
            System.out.println(" BookRecommender: http://localhost:8081/admin.html");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}