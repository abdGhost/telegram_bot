
package com.example.demo;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MyBot extends TelegramLongPollingBot {

    @Override
    public String getBotUsername() {
        return "Snipex1bot";
    }

    @Override
    public String getBotToken() {
        return "7140838743:AAH9WMG6z0URgRSr-kPfZOlYrRPDVPZUcVc";
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
        } else if (update.hasMessage() && update.getMessage().hasText()) {
            processMessage(update);
        }
    }

    private void processMessage(Update update) {
        String messageText = update.getMessage().getText();
        SendMessage message = new SendMessage();
        message.setChatId(update.getMessage().getChatId().toString());

        switch (messageText) {
            case "/start":
                message.setText("Welcome to " + getBotUsername() + "\nUse /tokens to see available tokens.\nUse /cryptoNews for crypto news.");
                break;
            case "/tokens":
                message.setText("Select a token:");
                message.setReplyMarkup(getInlineMessageButtons());
                break;
            case "/cryptoNews":
                fetchAndSendCryptoNews(update.getMessage().getChatId().toString());
                return;
            default:
                message.setText("You sent: " + messageText);
                break;
        }

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.getData();
        String chatId = callbackQuery.getMessage().getChatId().toString();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        if (callbackData.equals("trending")) {
            // Implement logic to fetch and display trending tokens
            String trendingTokens = "1. Bitcoin\n2. Ethereum\n3. Binance Coin\n4. Solana\n5. Cardano";
            SendMessage sendMessage = new SendMessage(chatId, "Trending Tokens:\n" + trendingTokens);
            try {
                execute(sendMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            // Assume callbackData is the token ID. We fetch the token's details again to get its name.
            RestTemplate restTemplate = new RestTemplate();
            String detailUrl = "https://api.coingecko.com/api/v3/coins/" + callbackData;
            CoinDetail coinDetail = restTemplate.getForObject(detailUrl, CoinDetail.class);
            String tokenName = (coinDetail != null) ? coinDetail.getName() : "Unknown";

            String priceUrl = "https://api.coingecko.com/api/v3/simple/price?ids=" + callbackData + "&vs_currencies=usd";
            try {
                HashMap<String, HashMap<String, Number>> response = restTemplate.getForObject(priceUrl, HashMap.class);
                if (response != null && response.get(callbackData) != null) {
                    Number price = response.get(callbackData).get("usd");
                    String answerText = String.format("Current %s price: $%s", tokenName, price.doubleValue());
                    SendMessage sendMessage = new SendMessage(chatId, answerText);
                    execute(sendMessage);

                    // Now, remove the buttons
                    EditMessageReplyMarkup editMarkup = new EditMessageReplyMarkup();
                    editMarkup.setChatId(chatId);
                    editMarkup.setMessageId(messageId);
                    editMarkup.setReplyMarkup(null);
                    execute(editMarkup);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private InlineKeyboardMarkup getInlineMessageButtons() {
        final String apiUrl = "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&order=market_cap_desc&per_page=5&page=1&sparkline=false";
        RestTemplate restTemplate = new RestTemplate();
        Coin[] response = restTemplate.getForObject(apiUrl, Coin[].class);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>(); // Used to group buttons in a row

        if (response != null) {
            int index = 0;
            for (Coin coin : response) {
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(coin.getName());
                button.setCallbackData(coin.getId());
                rowInline.add(button);

                // Assuming you want 2 buttons per row for the coins
                if (rowInline.size() == 2 || index == response.length - 1) {
                    rowsInline.add(new ArrayList<>(rowInline)); // Add the current row to rows
                    rowInline.clear(); // Clear the row for the next line of buttons
                }
                index++;
            }
        }

        // Add a button for trending tokens
        InlineKeyboardButton trendingButton = new InlineKeyboardButton();
        trendingButton.setText("Trending Tokens");
        trendingButton.setCallbackData("trending");
        rowsInline.add(List.of(trendingButton)); // Trending Tokens in its own row

        // Add a button to open dextool.io
        InlineKeyboardButton dextoolButton = new InlineKeyboardButton();
        dextoolButton.setText("View on Dextool");
        dextoolButton.setUrl("https://www.dextools.io/app/en/pairs");
        rowsInline.add(List.of(dextoolButton)); // Dextool in its own row
        
        // Add a button to join SOLTRENDING Telegram group
        InlineKeyboardButton joinGroupButton = new InlineKeyboardButton();
        joinGroupButton.setText("🚀 Join SOLTRENDING Group 🚀");
        joinGroupButton.setUrl("https://t.me/SOLTRENDING");
        rowsInline.add(List.of(joinGroupButton)); // Join SOLTRENDING group in its own row


        inlineKeyboardMarkup.setKeyboard(rowsInline);
        return inlineKeyboardMarkup;
    }

    private void fetchAndSendCryptoNews(String chatId) {
        final String apiKey = "3f2d665591714685b539e60a49892fad"; // Replace with your NewsAPI key
        final String url = "https://newsapi.org/v2/everything?q=bitcoin&sortBy=publishedAt&apiKey=" + apiKey;
        RestTemplate restTemplate = new RestTemplate();

        try {
            NewsResponse response = restTemplate.getForObject(url, NewsResponse.class);
            if (response != null && response.getArticles() != null && !response.getArticles().isEmpty()) {
                StringBuilder newsMessage = new StringBuilder("Latest Bitcoin News:\n");
                for (Article article : response.getArticles().subList(0, Math.min(response.getArticles().size(), 5))) { // Limit to top 5 articles
                    newsMessage.append("\n").append(article.getTitle()).append("\nRead more: ").append(article.getUrl()).append("\n");
                }
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText(newsMessage.toString());
                execute(message);
            } else {
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("Sorry, I couldn't fetch the latest Bitcoin news at this moment.");
                execute(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("An error occurred while fetching news.");
            try {
                execute(message);
            } catch (TelegramApiException telegramApiException) {
                telegramApiException.printStackTrace();
            }
        }
    }


    private static class Coin {
        private String id;
        private String name;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    private static class CoinDetail {
        private String id;
        private String name;
        // Other fields as needed

        // Standard getters and setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
    public static class NewsResponse {
        private List<Article> articles;

        // Getters and setters
        public List<Article> getArticles() {
            return articles;
        }

        public void setArticles(List<Article> articles) {
            this.articles = articles;
        }
    }

    public static class Article {
        private String title;
        private String url;

        // Getters and setters
        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }



}
