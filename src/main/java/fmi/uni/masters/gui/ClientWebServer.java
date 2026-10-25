package fmi.uni.masters.gui;

import com.sun.net.httpserver.HttpServer;
import fmi.uni.masters.agent.ClientAgent;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class ClientWebServer {

    public static void start(ClientAgent clientAgent) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

            server.createContext("/api/books", exchange -> {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

                try {
                    String jsonResponse = clientAgent.getBooksFromRecommender().get();
                    byte[] bytes = jsonResponse.getBytes("UTF-8");
                    exchange.sendResponseHeaders(200, bytes.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(bytes);
                    os.close();
                } catch (Exception e) {
                    exchange.sendResponseHeaders(500, -1);
                    e.printStackTrace();
                }
            });
            server.createContext("/api/books/interact", exchange -> {

                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

                try {
                    String query = exchange.getRequestURI().getQuery();
                    Map<String, String> params = queryToMap(query);

                    String action = params.get("action");
                    String username = params.get("user");
                    String bookTitle = params.get("book");

                    if (action != null && username != null && bookTitle != null) {
                        final String finalAction = action.toUpperCase();

                        clientAgent.modifyBookLikeStatus(finalAction, username, bookTitle)
                                .thenAccept(success -> {
                                    try {
                                        String jsonResponse = "{\"success\": " + success + "}";
                                        byte[] bytes = jsonResponse.getBytes("UTF-8");
                                        exchange.sendResponseHeaders(200, bytes.length);
                                        try (OutputStream os = exchange.getResponseBody()) {
                                            os.write(bytes);
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                })
                                .exceptionally(ex -> {
                                    try {
                                        ex.printStackTrace();
                                        String jsonResponse = "{\"success\": false, \"error\": \"JADE timeout\"}";
                                        byte[] bytes = jsonResponse.getBytes("UTF-8");
                                        exchange.sendResponseHeaders(500, bytes.length);
                                        try (OutputStream os = exchange.getResponseBody()) {
                                            os.write(bytes);
                                        }
                                    } catch (Exception e) {}
                                    return null;
                                });
                    } else {
                        String jsonResponse = "{\"success\": false, \"error\": \"Missing parameters\"}";
                        byte[] bytes = jsonResponse.getBytes("UTF-8");
                        exchange.sendResponseHeaders(400, bytes.length);
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(bytes);
                        }
                    }
                } catch (Exception e) {
                    try {
                        exchange.sendResponseHeaders(500, -1);
                    } catch (StringIndexOutOfBoundsException | java.io.IOException ex) {}
                    e.printStackTrace();
                }
            });

            server.createContext("/", exchange -> {
                String path = exchange.getRequestURI().getPath();

                if (path.equals("/")) {
                    path = "/login.html";
                }

                String resourcePath = "/gui" + path;

                try (InputStream is = ClientWebServer.class.getResourceAsStream(resourcePath)) {
                    if (is != null) {
                        if (path.endsWith(".html")) {
                            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
                        } else if (path.endsWith(".css")) {
                            exchange.getResponseHeaders().add("Content-Type", "text/css; charset=UTF-8");
                        }

                        byte[] bytes = is.readAllBytes();
                        exchange.sendResponseHeaders(200, bytes.length);

                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(bytes);
                        }
                    } else {
                        String response = "404 (Not Found) - Файлът '" + resourcePath + "' не е намерен в resources/gui/";
                        exchange.sendResponseHeaders(404, response.length());
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(response.getBytes());
                        }
                    }
                } catch (Exception e) {
                    exchange.sendResponseHeaders(500, -1);
                    e.printStackTrace();
                }
            });

            server.createContext("/api/auth", exchange -> {
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

                try {
                    String query = exchange.getRequestURI().getQuery();
                    String action = "";
                    String username = "";
                    String password = "";

                    if (query != null) {
                        String[] params = query.split("&");
                        for (String param : params) {
                            String[] pair = param.split("=");
                            if (pair.length > 1) {
                                String key = pair[0];
                                String val = java.net.URLDecoder.decode(pair[1], "UTF-8");

                                if (key.equals("action")) action = val.toUpperCase(); // LOGIN или REGISTER
                                if (key.equals("username")) username = val;
                                if (key.equals("password")) password = val;
                            }
                        }
                    }

                    boolean success = false;
                    if (!action.isEmpty() && !username.isEmpty() && !password.isEmpty()) {
                        success = clientAgent.performAuth(action, username, password).get();
                    }

                    String jsonResponse = "{\"success\": " + success + "}";
                    byte[] bytes = jsonResponse.getBytes("UTF-8");
                    exchange.sendResponseHeaders(200, bytes.length);

                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                } catch (Exception e) {
                    exchange.sendResponseHeaders(500, -1);
                    e.printStackTrace();
                }
            });

            server.start();
            System.out.println("ClientAgent: http://localhost:8080/login.html");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null || query.isEmpty()) return result;
        try {
            for (String param : query.split("&")) {
                String[] entry = param.split("=");
                if (entry.length > 1) {
                    result.put(entry[0], URLDecoder.decode(entry[1], "UTF-8"));
                } else if (entry.length > 0) {
                    result.put(entry[0], "");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}