import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class Chatbox {
    // ??????? ???? ??????? ??? (user ?? bot) ??? ????? ????
    private static final List<String> messages = new ArrayList<String>();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/", new UIHandler());
        server.createContext("/send", new SendMessageHandler());
        server.createContext("/messages", new GetMessagesHandler());

        server.setExecutor(null);
        System.out.println("Server running on http://localhost:8080");
        server.start();
    }

    // UI Handler
    static class UIHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String htmlResponse = 
                "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <title>Java Chat App</title>" +
                "    <style>" +
                "        body { font-family: Arial, sans-serif; background: #eef2f5; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; }" +
                "        .chat-container { width: 360px; background: white; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); overflow: hidden; display: flex; flex-direction: column; height: 480px; }" +
                "        .header-container { display: flex; align-items: center; justify-content: center; gap: 12px; background-color: #007bff; padding: 12px; color: white; border-radius: 8px 8px 0 0; }" +
                "        .logo-img { width: 45px; height: 45px; border-radius: 50%; object-fit: cover; border: 2px solid #ffffff; }" +
                "        .chat-box { flex: 1; padding: 15px; overflow-y: auto; background: #fdfdfd; display: flex; flex-direction: column; gap: 10px; }" +
                "        .msg-user { background: #007bff; color: white; padding: 8px 14px; border-radius: 15px 15px 0px 15px; align-self: flex-end; max-width: 75%; word-break: break-word; font-size: 14px; }" +
                "        .msg-bot { background: #e4e6eb; color: #050505; padding: 8px 14px; border-radius: 15px 15px 15px 0px; align-self: flex-start; max-width: 75%; word-break: break-word; font-size: 14px; }" +
                "        .chat-input { display: flex; border-top: 1px solid #ddd; padding: 10px; background: #fff; }" +
                "        .chat-input input { flex: 1; padding: 10px 14px; border: 1px solid #ccc; border-radius: 20px; outline: none; }" +
                "        .chat-input button { margin-left: 8px; padding: 10px 18px; background: #007bff; color: white; border: none; border-radius: 20px; cursor: pointer; font-weight: bold; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class='chat-container'>" +
                "        <div class='header-container'>" +
                "            <img src='https://cdn-icons-png.flaticon.com/512/4712/4712109.png' class='logo-img' alt='Logo'>" +
                "            <h2 style='margin: 0; font-size: 20px;'>Chat Box</h2>" +
                "        </div>" +
                "        <div class='chat-box' id='chatBox'></div>" +
                "        <div class='chat-input'>" +
                "            <input type='text' id='messageInput' placeholder='Type a message...' onkeypress='handleKeyPress(event)'>" +
                "            <button onclick='sendMessage()'>Send</button>" +
                "        </div>" +
                "    </div>" +

                "    <script>" +
                "        function sendMessage() {" +
                "            let input = document.getElementById('messageInput');" +
                "            let text = input.value.trim();" +
                "            if (text === '') return;" +
                "            fetch('/send', { method: 'POST', body: text })" +
                "            .then(() => {" +
                "                input.value = '';" +
                "                loadMessages();" +
                "            });" +
                "        }" +

                "        function loadMessages() {" +
                "            fetch('/messages')" +
                "            .then(res => res.json())" +
                "            .then(data => {" +
                "                let box = document.getElementById('chatBox');" +
                "                box.innerHTML = '';" +
                "                data.forEach(item => {" +
                "                    let div = document.createElement('div');" +
                "                    div.className = item.sender === 'user' ? 'msg-user' : 'msg-bot';" +
                "                    div.innerText = item.text;" +
                "                    box.appendChild(div);" +
                "                });" +
                "                box.scrollTop = box.scrollHeight;" +
                "            });" +
                "        }" +

                "        function handleKeyPress(event) {" +
                "            if (event.key === 'Enter') sendMessage();" +
                "        }" +

                "        setInterval(loadMessages, 1000);" +
                "    </script>" +
                "</body>" +
                "</html>";

            byte[] responseBytes = htmlResponse.getBytes("UTF-8");
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, responseBytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(responseBytes);
            os.close();
        }
    }

    // Backend-? ????? ????? ??? Auto-Reply Logic
    static class SendMessageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[1024];
                while ((nRead = is.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                buffer.flush();
                String msg = new String(buffer.toByteArray(), "UTF-8").trim();

                if (!msg.isEmpty()) {
                    // ?. ????? ????? ????? ??? (User)
                    messages.add("{\"sender\":\"user\", \"text\":\"" + msg + "\"}");

                    // ?. Java Backend ???????? ????? ???? ???? (Bot)
                    String reply = getBotReply(msg);
                    messages.add("{\"sender\":\"bot\", \"text\":\"" + reply + "\"}");
                }
            }
            exchange.sendResponseHeaders(200, -1);
            exchange.getResponseBody().close();
        } 

        // ?????? ?? ????
        private String getBotReply(String input) {
            input = input.toLowerCase();
            if (input.contains("hello") || input.contains("hi")) {
                return "Hello! How can I help you?";
            } else if (input.contains("fine") || input.contains("good")) {
                return "Glad to hear that!";
            } else if (input.contains("name")) {
                return "I am Java ChatBot!";
            } 
            else if (input.contains("price") || input.contains("product")) {
    return "500 dollar";
}
            else {
                return "Got it: '" + input + "'. Thanks for messaging!";
            }
        }
    }

    // ????? ????? JSON ????? ??????
    static class GetMessagesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < messages.size(); i++) {
                json.append(messages.get(i));
                if (i < messages.size() - 1) json.append(",");
            }
            json.append("]");

            byte[] responseBytes = json.toString().getBytes("UTF-8");
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(responseBytes);
            os.close();
        }
    }
}